/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package net

import munit.FunSuite

class SgeHttpResponseTest extends FunSuite {

  private val dummyRequestMetadata: SttpRequestMetadata = new SttpRequestMetadata {
    val method:  SttpMethod      = SttpMethod.GET
    val uri:     SttpUri         = SttpUri.unsafeParse("https://test.invalid")
    val headers: Seq[SttpHeader] = Seq.empty
  }

  private def makeResponse(
    body:    Either[String, String],
    code:    Int,
    headers: Seq[SttpHeader] = Seq.empty
  ): SgeHttpResponse = {
    val sttpResponse = SttpResponse(
      body = body,
      code = SttpStatusCode(code),
      statusText = "",
      headers = headers,
      history = Nil,
      request = dummyRequestMetadata
    )
    new SgeHttpResponse(sttpResponse)
  }

  test("getStatus returns correct HttpStatus") {
    val resp = makeResponse(Right("ok"), 200)
    assertEquals(resp.status.statusCode, 200)
  }

  test("getStatus for error code") {
    val resp = makeResponse(Left("not found"), 404)
    assertEquals(resp.status.statusCode, 404)
  }

  test("getResultAsString returns body on success") {
    val resp = makeResponse(Right("hello world"), 200)
    assertEquals(resp.resultAsString, "hello world")
  }

  test("getResultAsString returns error body on failure") {
    val resp = makeResponse(Left("error message"), 500)
    assertEquals(resp.resultAsString, "error message")
  }

  test("getResult returns UTF-8 bytes") {
    val resp  = makeResponse(Right("abc"), 200)
    val bytes = resp.result
    assertEquals(new String(bytes, "UTF-8"), "abc")
  }

  test("getResultAsStream is readable") {
    val resp   = makeResponse(Right("stream data"), 200)
    val stream = resp.resultAsStream
    val bytes  = stream.readAllBytes()
    assertEquals(new String(bytes, "UTF-8"), "stream data")
  }

  test("getHeader returns value when present") {
    val resp = makeResponse(
      Right(""),
      200,
      Seq(SttpHeader("Content-Type", "application/json"))
    )
    assertEquals(resp.getHeader("Content-Type").get, "application/json")
  }

  test("getHeader returns empty when absent") {
    val resp = makeResponse(Right(""), 200)
    assert(resp.getHeader("X-Missing").isEmpty)
  }

  test("getHeaders groups multiple values for same header") {
    val resp = makeResponse(
      Right(""),
      200,
      Seq(
        SttpHeader("Set-Cookie", "a=1"),
        SttpHeader("Set-Cookie", "b=2"),
        SttpHeader("Content-Type", "text/html")
      )
    )
    val headers = resp.headers
    assertEquals(headers.get("Set-Cookie").size(), 2)
    assertEquals(headers.get("Content-Type").size(), 1)
  }
}
