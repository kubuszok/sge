/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite for ISS-505: SgeHttpClient.cancel() frees the request back to the
 * pool immediately, but the in-flight future's onComplete callback ALWAYS runs
 * freeRequest(request) when the backend eventually completes — freeing the
 * SAME request a second time. Pool does not guard against double-free (its own
 * scaladoc: "the same object must not be freed multiple times"), so the request
 * sits twice in the free list and two subsequent obtainRequest() calls return
 * the SAME SgeHttpRequest instance, cross-contaminating unrelated requests.
 */
package sge
package net

import java.util.concurrent.{ CountDownLatch, TimeUnit }
import munit.FunSuite
import scala.collection.mutable
import scala.concurrent.{ ExecutionContext, Future, Promise }
import lowlevel.Nullable

class SgeHttpClientDoubleFreeRedSuite extends FunSuite {

  private val dummyRequestMetadata: SttpRequestMetadata = new SttpRequestMetadata {
    val method:  SttpMethod      = SttpMethod.GET
    val uri:     SttpUri         = SttpUri.unsafeParse("https://test.invalid")
    val headers: Seq[SttpHeader] = Seq.empty
  }

  /** A controllable stub backend: every send() gets its own Promise that the test completes manually, and the dispatched sttp request is recorded for inspection. */
  private class ControllableBackendFactory extends HttpBackendFactory {
    private val sent = mutable.ArrayBuffer.empty[(SttpRequest[Array[Byte]], Promise[SttpResponse[Array[Byte]]])]

    override def send(request: SttpRequest[Array[Byte]]): Future[SttpResponse[Array[Byte]]] = {
      val p = Promise[SttpResponse[Array[Byte]]]()
      sent.synchronized {
        sent += ((request, p))
      }
      p.future
    }

    override def close(): Unit = ()

    def sentCount: Int =
      sent.synchronized {
        sent.size
      }

    def dispatchedUri(idx: Int): String =
      sent.synchronized {
        sent(idx)._1.uri.toString
      }

    /** Completes the idx-th dispatched request and waits until its callbacks (including the client's pool-freeing onComplete) have had a chance to run. */
    def completeAndSettle(idx: Int, body: String, code: Int): Unit = {
      val p = sent.synchronized {
        sent(idx)._2
      }
      p.success(SttpResponse(body.getBytes("UTF-8"), SttpStatusCode(code), "", Nil, Nil, dummyRequestMetadata))
      val latch = new CountDownLatch(1)
      p.future.onComplete(_ => latch.countDown())(using ExecutionContext.global)
      assert(latch.await(5, TimeUnit.SECONDS), "backend future callbacks did not run in time")
      // The client's own onComplete is a sibling task on the same EC; give it a moment too
      // (same pattern as SgeHttpClientTest's post-completion sleeps).
      Thread.sleep(200)
    }
  }

  /** Tolerant listener that records what happened instead of failing, so close() cleanup never trips assertions. */
  private class RecordingListener extends Net.HttpResponseListener {
    @volatile var responses: List[String]    = Nil
    @volatile var failures:  List[Throwable] = Nil
    @volatile var cancels:   Int             = 0

    def handleHttpResponse(httpResponse: Net.HttpResponse): Unit = responses = responses :+ httpResponse.resultAsString
    def failed(t:                        Throwable):        Unit = failures = failures :+ t
    def cancelled():                                        Unit = cancels += 1
  }

  /** Runs the ISS-505 trigger: send A, cancel A (first free), then complete A's backend future (buggy second free). Returns the client + backend with the pool now corrupted (request A listed twice).
    */
  private def triggerCancelThenCompletion(): (SgeHttpClient, ControllableBackendFactory, SgeHttpRequest) = {
    val backend = new ControllableBackendFactory()
    val client  = new SgeHttpClient(backend, 4, 16)

    val reqA = client.obtainRequest()
    reqA.withMethod(Net.HttpMethod.GET).withUrl("https://example.com/a")
    val listenerA = new RecordingListener()
    client.send(reqA, Nullable(listenerA))
    assert(client.isPending(reqA), "request A should be pending after send")

    // (2) cancel A -> first requestPool.free(reqA)
    client.cancel(reqA)
    assert(!client.isPending(reqA), "request A should not be pending after cancel")
    assertEquals(listenerA.cancels, 1)

    // (3) the backend eventually completes -> onComplete runs freeRequest(reqA): second free
    backend.completeAndSettle(0, "late response for A", 200)
    assertEquals(listenerA.responses, Nil, "cancelled entry must not deliver a response")

    (client, backend, reqA)
  }

  test("RED ISS-505: cancel + late completion double-frees — two subsequent obtains return the same instance") {
    val (client, _, _) = triggerCancelThenCompletion()

    // (4) obtain two new requests: with the double-free both pops return request A
    val b = client.obtainRequest()
    val c = client.obtainRequest()
    assert(b ne c, "two obtainRequest() calls after cancel+completion returned the SAME pooled instance (double-free)")

    client.close()
  }

  test(
    "RED ISS-505: double-freed request cross-contaminates B and C — B's configured URL is clobbered and the WRONG request is dispatched"
  ) {
    val (client, backend, _) = triggerCancelThenCompletion()

    val b = client.obtainRequest()
    b.withMethod(Net.HttpMethod.GET).withUrl("https://example.com/b")
    val c = client.obtainRequest()
    c.withMethod(Net.HttpMethod.GET).withUrl("https://example.com/c")

    // The user now sends B; with the double-free b eq c, so configuring C clobbered B's URL
    // and B's listener will receive the response of a request it never made.
    client.send(b, Nullable(new RecordingListener()))

    assertEquals(backend.sentCount, 2) // request A earlier + request B now
    assertEquals(backend.dispatchedUri(1), "https://example.com/b", "B must dispatch its own URL, not C's")
    assertEquals(b.url, "https://example.com/b", "configuring C must not mutate B")

    client.close()
  }

  test("RED ISS-505: double-freed request corrupts the pending map — completing B's call kills C's pending state") {
    val (client, backend, _) = triggerCancelThenCompletion()

    val b = client.obtainRequest()
    b.withMethod(Net.HttpMethod.GET).withUrl("https://example.com/b")
    val listenerB = new RecordingListener()
    client.send(b, Nullable(listenerB))

    val c = client.obtainRequest()
    c.withMethod(Net.HttpMethod.GET).withUrl("https://example.com/c")
    val listenerC = new RecordingListener()
    client.send(c, Nullable(listenerC))

    assert(client.isPending(c), "request C should be pending after send")

    // Complete ONLY B's backend call (index 1; index 0 was the cancelled request A).
    backend.completeAndSettle(1, "response for B", 200)
    assertEquals(listenerB.responses, List("response for B"))

    // With the double-free b eq c, so freeRequest(b) removed C's pending entry
    // and pushed C's in-flight request back into the pool.
    assert(client.isPending(c), "request C is still in flight — completing B must not evict C's pending entry")

    client.close()
  }

  test("control: cancel WITHOUT later completion frees exactly once — subsequent obtains are distinct") {
    val backend = new ControllableBackendFactory()
    val client  = new SgeHttpClient(backend, 4, 16)

    val reqA = client.obtainRequest()
    reqA.withMethod(Net.HttpMethod.GET).withUrl("https://example.com/a")
    client.send(reqA, Nullable(new RecordingListener()))
    client.cancel(reqA)

    // Backend never completes: single free, pool holds A exactly once.
    val b = client.obtainRequest()
    val c = client.obtainRequest()
    assert(b ne c, "single free must not duplicate the pooled instance")

    client.close()
  }
}
