/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package net

import munit.FunSuite
import scala.concurrent.{ Future, Promise }
import sge.utils.Nullable

class SgeHttpClientTest extends FunSuite {

  private val dummyRequestMetadata: SttpRequestMetadata = new SttpRequestMetadata {
    val method:  SttpMethod      = SttpMethod.GET
    val uri:     SttpUri         = SttpUri.unsafeParse("https://test.invalid")
    val headers: Seq[SttpHeader] = Seq.empty
  }

  /** A mock backend that captures sent requests and allows completing them manually. */
  private class MockBackendFactory extends HttpBackendFactory {
    @volatile var lastRequest: SttpRequest[Either[String, String]]           = scala.compiletime.uninitialized
    @volatile var lastPromise: Promise[SttpResponse[Either[String, String]]] = scala.compiletime.uninitialized
    @volatile var closed:      Boolean                                       = false

    override def send(request: SttpRequest[Either[String, String]]): Future[SttpResponse[Either[String, String]]] = {
      lastRequest = request
      val p = Promise[SttpResponse[Either[String, String]]]()
      lastPromise = p
      p.future
    }

    override def close(): Unit =
      closed = true

    def completeWith(body: String, code: Int): Unit =
      lastPromise.success(SttpResponse(Right(body), SttpStatusCode(code), "", Nil, Nil, dummyRequestMetadata))
  }

  test("obtainRequest returns fresh request with defaults") {
    val client = SgeHttpClient.noop()
    val req    = client.obtainRequest()
    assertEquals(req.method, Net.HttpMethod.GET)
    assertEquals(req.url, "")
    client.close()
  }

  test("send dispatches request and calls listener on success") {
    val backend = new MockBackendFactory()
    val client  = new SgeHttpClient(backend, 4, 16)

    val req = client.obtainRequest()
    req.withMethod(Net.HttpMethod.POST).withUrl("https://example.com/api")

    @volatile var receivedResponse: Net.HttpResponse = null.asInstanceOf[Net.HttpResponse] // test-only local
    val listener = new Net.HttpResponseListener {
      def handleHttpResponse(httpResponse: Net.HttpResponse): Unit = receivedResponse = httpResponse
      def failed(t:                        Throwable):        Unit = fail(s"unexpected failure: $t")
      def cancelled():                                        Unit = fail("unexpected cancel")
    }

    client.send(req, Nullable(listener))
    assert(client.isPending(req), "request should be pending after send")

    // Complete the backend future
    backend.completeWith("response body", 200)

    // Give the future callback a moment to execute
    Thread.sleep(100)

    assert(!client.isPending(req), "request should not be pending after completion")
    assertNotEquals(receivedResponse, null)
    assertEquals(receivedResponse.resultAsString, "response body")
    assertEquals(receivedResponse.status.statusCode, 200)

    client.close()
  }

  test("send calls listener.failed on backend error") {
    val backend = new MockBackendFactory()
    val client  = new SgeHttpClient(backend, 4, 16)

    val req = client.obtainRequest()
    req.withUrl("https://example.com/fail")

    @volatile var receivedError: Throwable = null.asInstanceOf[Throwable] // test-only local
    val listener = new Net.HttpResponseListener {
      def handleHttpResponse(httpResponse: Net.HttpResponse): Unit = fail("unexpected success")
      def failed(t:                        Throwable):        Unit = receivedError = t
      def cancelled():                                        Unit = fail("unexpected cancel")
    }

    client.send(req, Nullable(listener))
    backend.lastPromise.failure(new RuntimeException("connection refused"))

    Thread.sleep(100)

    assertNotEquals(receivedError, null)
    assert(receivedError.getMessage.contains("connection refused"))

    client.close()
  }

  test("cancel marks request as cancelled and calls listener") {
    val backend = new MockBackendFactory()
    val client  = new SgeHttpClient(backend, 4, 16)

    val req = client.obtainRequest()
    req.withUrl("https://example.com/cancel")

    @volatile var wasCancelled = false
    val listener               = new Net.HttpResponseListener {
      def handleHttpResponse(httpResponse: Net.HttpResponse): Unit = fail("unexpected success")
      def failed(t:                        Throwable):        Unit = fail(s"unexpected failure: $t")
      def cancelled():                                        Unit = wasCancelled = true
    }

    client.send(req, Nullable(listener))
    assert(client.isPending(req))

    client.cancel(req)
    assert(!client.isPending(req), "request should not be pending after cancel")
    assert(wasCancelled, "listener.cancelled() should have been called")

    client.close()
  }

  test("close cancels all pending requests") {
    val backend = new MockBackendFactory()
    val client  = new SgeHttpClient(backend, 4, 16)

    val req = client.obtainRequest()
    req.withUrl("https://example.com/close")

    @volatile var wasCancelled = false
    val listener               = new Net.HttpResponseListener {
      def handleHttpResponse(httpResponse: Net.HttpResponse): Unit = ()
      def failed(t:                        Throwable):        Unit = ()
      def cancelled():                                        Unit = wasCancelled = true
    }

    client.send(req, Nullable(listener))
    client.close()

    assert(wasCancelled, "close should cancel pending requests")
    assert(backend.closed, "close should close the backend")
  }

  test("noop client obtains requests but fails on send") {
    val client = SgeHttpClient.noop()
    val req    = client.obtainRequest()
    req.withUrl("https://example.com")

    @volatile var receivedError: Throwable = null.asInstanceOf[Throwable] // test-only local
    val listener = new Net.HttpResponseListener {
      def handleHttpResponse(httpResponse: Net.HttpResponse): Unit = ()
      def failed(t:                        Throwable):        Unit = receivedError = t
      def cancelled():                                        Unit = ()
    }

    client.send(req, Nullable(listener))

    Thread.sleep(100)

    assertNotEquals(receivedError, null)
    assert(receivedError.isInstanceOf[UnsupportedOperationException])

    client.close()
  }
}
