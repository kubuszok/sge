/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite for ISS-521: HTTP bodies round-trip through a UTF-8 String.
 * SgeHttpClient.buildSttpRequest produces SttpRequest[Either[String, String]]
 * (SgeHttpClient.scala), so every backend decodes the raw response bytes into
 * a String, and SgeHttpResponse re-encodes that String via getBytes("UTF-8")
 * for `result`, with `resultAsStream` being a ByteArrayInputStream over that
 * re-encoding (SgeHttpResponse.scala). Any response byte sequence that is not
 * valid UTF-8 is mangled into U+FFFD replacement characters, so binary
 * downloads corrupt and the "stream" is fake.
 *
 * The original LibGDX contract is byte-exact:
 *   - NetJavaImpl.java:62-78  — getResult() copies the connection InputStream
 *     straight into a byte[] (StreamUtils.copyStreamToByteArray, no charset).
 *   - NetJavaImpl.java:98-101 — getResultAsStream() returns the connection's
 *     raw InputStream (true streaming, no String round-trip).
 *   - NetJavaImpl.java:211-219 — request content streams are copied raw to
 *     the connection OutputStream (upload direction is byte-exact too).
 *
 * JVM-only: the corruption happens where the REAL backend applies the
 * String response description to the raw transport bytes, so proving it
 * requires real bytes over a real transport (in-process com.sun.net.httpserver
 * + the real JVM sttp backend). A stub backend would have to fabricate the
 * String body in the test itself and would prove nothing; the sttp stub
 * artifact is not on the classpath anyway, and JS/Native have no in-process
 * HTTP server harness in this test infrastructure.
 */
package sge
package net

import com.sun.net.httpserver.{ HttpExchange, HttpServer }
import java.net.InetSocketAddress
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import munit.FunSuite
import lowlevel.Nullable

class HttpBinaryBodyRedSuite extends FunSuite {

  /** Bytes that are invalid as UTF-8: 0xFF and 0xFE never appear in well-formed UTF-8, 0x80 is a continuation byte without a lead byte. */
  private val invalidUtf8Bytes: Array[Byte] =
    Array[Byte](0xff.toByte, 0xfe.toByte, 0x00, 0x80.toByte)

  /** A 256-byte ramp 0..255 — covers every possible byte value. */
  private val rampBytes: Array[Byte] =
    Array.tabulate[Byte](256)(i => i.toByte)

  private val controlText = "héllo SGE ✓ żółć"

  private var server:                 HttpServer    = scala.compiletime.uninitialized
  private var client:                 SgeHttpClient = scala.compiletime.uninitialized
  @volatile private var uploadedBody: Array[Byte]   = Array.emptyByteArray

  private def baseUrl: String = s"http://127.0.0.1:${server.getAddress.getPort}"

  private def respondWith(exchange: HttpExchange, contentType: String, body: Array[Byte]): Unit = {
    exchange.getResponseHeaders.set("Content-Type", contentType)
    exchange.sendResponseHeaders(200, body.length.toLong)
    val os = exchange.getResponseBody
    try os.write(body)
    finally os.close()
  }

  override def beforeAll(): Unit = {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/binary", (exchange: HttpExchange) => respondWith(exchange, "application/octet-stream", invalidUtf8Bytes))
    server.createContext("/ramp", (exchange: HttpExchange) => respondWith(exchange, "application/octet-stream", rampBytes))
    server.createContext("/text", (exchange: HttpExchange) => respondWith(exchange, "text/plain; charset=utf-8", controlText.getBytes("UTF-8")))
    server.createContext(
      "/upload",
      (exchange: HttpExchange) => {
        uploadedBody = exchange.getRequestBody.readAllBytes()
        respondWith(exchange, "text/plain; charset=utf-8", "ok".getBytes("UTF-8"))
      }
    )
    server.start()
    // Real client → real JVM sttp backend (java.net.http.HttpClient), the same
    // path a game uses; this is where the byte→String decode happens.
    client = SgeHttpClient()
  }

  override def afterAll(): Unit = {
    if (client != null) client.close() // test-only null check: uninitialized lifecycle guard
    if (server != null) server.stop(0) // test-only null check: uninitialized lifecycle guard
  }

  /** Sends the request via the public API and waits for the listener callback. */
  private def fetch(configure: SgeHttpRequest => SgeHttpRequest): Net.HttpResponse = {
    val latch = new CountDownLatch(1)
    // Plain vars are safe here: CountDownLatch.countDown/await establishes happens-before.
    var response: Option[Net.HttpResponse] = None
    var error:    Option[Throwable]        = None

    val listener = new Net.HttpResponseListener {
      def handleHttpResponse(httpResponse: Net.HttpResponse): Unit = {
        response = Some(httpResponse)
        latch.countDown()
      }
      def failed(t: Throwable): Unit = {
        error = Some(t)
        latch.countDown()
      }
      def cancelled(): Unit =
        latch.countDown()
    }

    client.send(configure(client.obtainRequest()), Nullable(listener))
    assert(latch.await(15, TimeUnit.SECONDS), "request did not complete within 15s")
    error.foreach(t => fail(s"request failed: $t"))
    response.getOrElse(fail("no response received"))
  }

  private def hex(bytes: Array[Byte]): String =
    bytes.map(b => f"${b & 0xff}%02X").mkString(" ")

  test("control: UTF-8 text body round-trips intact") {
    val response = fetch(_.withMethod(Net.HttpMethod.GET).withUrl(s"$baseUrl/text"))
    assertEquals(response.status.statusCode, 200)
    assertEquals(response.resultAsString, controlText)
    assertEquals(hex(response.result), hex(controlText.getBytes("UTF-8")))
  }

  test("binary download: result returns the exact bytes sent (FF FE 00 80)") {
    val response = fetch(_.withMethod(Net.HttpMethod.GET).withUrl(s"$baseUrl/binary"))
    assertEquals(response.status.statusCode, 200)
    // LibGDX contract (NetJavaImpl.java:62-78): getResult() is the raw response
    // bytes. Today the UTF-8 String round-trip mangles FF FE 80 into EF BF BD
    // (U+FFFD replacement) sequences.
    assertEquals(
      hex(response.result),
      hex(invalidUtf8Bytes),
      "response.result must be the exact bytes the server sent — UTF-8 String round-trip corrupted them"
    )
  }

  test("binary download: resultAsStream yields the exact bytes sent (FF FE 00 80)") {
    val response = fetch(_.withMethod(Net.HttpMethod.GET).withUrl(s"$baseUrl/binary"))
    assertEquals(response.status.statusCode, 200)
    // LibGDX contract (NetJavaImpl.java:98-101): getResultAsStream() streams the
    // connection's raw InputStream. Today it is a ByteArrayInputStream over the
    // UTF-8 re-encoding of the mangled String.
    val streamed = response.resultAsStream.readAllBytes()
    assertEquals(
      hex(streamed),
      hex(invalidUtf8Bytes),
      "resultAsStream must yield the exact bytes the server sent — UTF-8 String round-trip corrupted them"
    )
  }

  test("binary download: a 256-byte 0..255 ramp round-trips byte-exact") {
    val response = fetch(_.withMethod(Net.HttpMethod.GET).withUrl(s"$baseUrl/ramp"))
    assertEquals(response.status.statusCode, 200)
    val got = response.result
    assertEquals(
      hex(got),
      hex(rampBytes),
      s"every byte value 0..255 must survive the download (got ${got.length} bytes, sent ${rampBytes.length})"
    )
  }

  test("binary upload: contentBytes arrive intact at the server") {
    // LibGDX contract (NetJavaImpl.java:211-219): the request content stream is
    // copied raw to the connection OutputStream.
    val response = fetch(_.withMethod(Net.HttpMethod.POST).withUrl(s"$baseUrl/upload").withContentBytes(invalidUtf8Bytes ++ rampBytes))
    assertEquals(response.status.statusCode, 200)
    assertEquals(
      hex(uploadedBody),
      hex(invalidUtf8Bytes ++ rampBytes),
      "request body bytes must arrive at the server exactly as set via withContentBytes"
    )
  }
}
