/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
// SGE — JS request→response roundtrip for the Fetch HTTP path (ISS-860).
//
// BrowserNet delegates all HTTP to SgeHttpClient over sttp's Fetch backend. A LIVE fetch cannot be
// unit-tested in jsdom (no loopback server), so this suite exercises the honest substitute the task
// asks for: an INJECTED mock backend. It drives the exact SgeHttpClient that BrowserNet uses,
// linked for JS, proving on Scala.js that:
//   * request construction maps SgeHttpRequest → sttp Request (method, URL, headers)
//   * a mock backend response is mapped back to Net.HttpResponse (status, body, headers)
//   * backend failures reach listener.failed
// This is NOT a live-fetch test. Live fetch against a real server stays a follow-up (browser IT
// smoke marker + a future loopback IT). The mock-backend send/cancel/close lifecycle also has JVM
// coverage in sge.net.SgeHttpClientTest; this suite re-pins the response-mapping slice on JS, where
// Thread.sleep is unavailable and completion runs on the microtask queue (async via Future).
package sge
package net

import munit.FunSuite
import scala.concurrent.{ ExecutionContext, Future, Promise }
import lowlevel.Nullable

class BrowserNetFetchRoundtripJsSuite extends FunSuite {

  private given ExecutionContext = ExecutionContext.global

  private val dummyRequestMetadata: SttpRequestMetadata = new SttpRequestMetadata {
    val method:  SttpMethod      = SttpMethod.GET
    val uri:     SttpUri         = SttpUri.unsafeParse("https://test.invalid")
    val headers: Seq[SttpHeader] = Seq.empty
  }

  /** A mock HTTP backend: instead of hitting the network it captures the built request and replies with a canned response, so the request-construction and response-mapping slices are exercised
    * without a live server.
    */
  private class MockBackendFactory(
    body:        Array[Byte],
    code:        Int,
    respHeaders: Seq[SttpHeader] = Seq.empty
  ) extends HttpBackendFactory {
    @volatile var lastRequest: SttpRequest[Array[Byte]] = scala.compiletime.uninitialized
    @volatile var closed:      Boolean                  = false

    override def send(request: SttpRequest[Array[Byte]]): Future[SttpResponse[Array[Byte]]] = {
      lastRequest = request
      Future.successful(SttpResponse(body, SttpStatusCode(code), "", respHeaders, Nil, dummyRequestMetadata))
    }

    override def close(): Unit = closed = true
  }

  test("request→response roundtrip maps status and body back through SgeHttpResponse") {
    val backend = new MockBackendFactory("pong".getBytes("UTF-8"), 200)
    val client  = new SgeHttpClient(backend, 4, 16)

    val req = client.obtainRequest()
    req.withMethod(Net.HttpMethod.GET).withUrl("https://example.com/ping")

    val done     = Promise[Net.HttpResponse]()
    val listener = new Net.HttpResponseListener {
      def handleHttpResponse(r: Net.HttpResponse): Unit = done.success(r)
      def failed(t:             Throwable):        Unit = done.failure(t)
      def cancelled():                             Unit = done.failure(new AssertionError("unexpected cancel"))
    }

    client.send(req, Nullable(listener))

    done.future.map { resp =>
      assertEquals(resp.status.statusCode, 200)
      assertEquals(resp.resultAsString, "pong")
      // the request the backend actually received carries the configured method + URL
      assertEquals(backend.lastRequest.method, SttpMethod.GET)
      assertEquals(backend.lastRequest.uri.toString, "https://example.com/ping")
      client.close()
    }
  }

  test("configured request headers and method reach the built sttp request") {
    val backend = new MockBackendFactory("{}".getBytes("UTF-8"), 201)
    val client  = new SgeHttpClient(backend, 4, 16)

    val req = client.obtainRequest()
    req.withMethod(Net.HttpMethod.POST).withUrl("https://example.com/api/items").withHeader("X-Custom", "abc123").withContent("""{"name":"sword"}""")

    val done     = Promise[Net.HttpResponse]()
    val listener = new Net.HttpResponseListener {
      def handleHttpResponse(r: Net.HttpResponse): Unit = done.success(r)
      def failed(t:             Throwable):        Unit = done.failure(t)
      def cancelled():                             Unit = done.failure(new AssertionError("unexpected cancel"))
    }

    client.send(req, Nullable(listener))

    done.future.map { resp =>
      assertEquals(resp.status.statusCode, 201)
      assertEquals(backend.lastRequest.method, SttpMethod.POST)
      assertEquals(backend.lastRequest.uri.toString, "https://example.com/api/items")
      assert(
        backend.lastRequest.headers.exists(h => h.name == "X-Custom" && h.value == "abc123"),
        s"custom header must be forwarded, got ${backend.lastRequest.headers}"
      )
      client.close()
    }
  }

  test("response headers are exposed through Net.HttpResponse") {
    val backend = new MockBackendFactory(
      "body".getBytes("UTF-8"),
      200,
      Seq(SttpHeader("Content-Type", "text/plain"))
    )
    val client = new SgeHttpClient(backend, 4, 16)

    val req = client.obtainRequest()
    req.withUrl("https://example.com/with-headers")

    val done     = Promise[Net.HttpResponse]()
    val listener = new Net.HttpResponseListener {
      def handleHttpResponse(r: Net.HttpResponse): Unit = done.success(r)
      def failed(t:             Throwable):        Unit = done.failure(t)
      def cancelled():                             Unit = done.failure(new AssertionError("unexpected cancel"))
    }

    client.send(req, Nullable(listener))

    done.future.map { resp =>
      assertEquals(resp.getHeader("Content-Type").toOption, Some("text/plain"))
      client.close()
    }
  }

  test("backend failure is delivered to listener.failed with the cause message") {
    val backend = new HttpBackendFactory {
      override def send(request: SttpRequest[Array[Byte]]): Future[SttpResponse[Array[Byte]]] =
        Future.failed(new RuntimeException("connection refused"))
      override def close(): Unit = ()
    }
    val client = new SgeHttpClient(backend, 4, 16)

    val req = client.obtainRequest()
    req.withUrl("https://example.com/fail")

    val done     = Promise[Throwable]()
    val listener = new Net.HttpResponseListener {
      def handleHttpResponse(r: Net.HttpResponse): Unit = done.failure(new AssertionError("unexpected success"))
      def failed(t:             Throwable):        Unit = done.success(t)
      def cancelled():                             Unit = done.failure(new AssertionError("unexpected cancel"))
    }

    client.send(req, Nullable(listener))

    done.future.map { err =>
      assert(err.getMessage.contains("connection refused"), s"expected the backend cause, got: ${err.getMessage}")
      client.close()
    }
  }
}
