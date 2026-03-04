/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package net

import munit.FunSuite
import sttp.client4.Response
import sttp.model.{ Header, Method, RequestMetadata, StatusCode, Uri }

class SgeHttpResponseTest extends FunSuite {

  private val dummyRequestMetadata: RequestMetadata = new RequestMetadata {
    val method:  Method      = Method.GET
    val uri:     Uri         = Uri.unsafeParse("https://test.invalid")
    val headers: Seq[Header] = Seq.empty
  }

  private def makeResponse(
    body:    Either[String, String],
    code:    Int,
    headers: Seq[Header] = Seq.empty
  ): SgeHttpResponse = {
    val sttpResponse = Response(
      body = body,
      code = StatusCode(code),
      statusText = "",
      headers = headers,
      history = Nil,
      request = dummyRequestMetadata
    )
    new SgeHttpResponse(sttpResponse)
  }

  test("getStatus returns correct HttpStatus") {
    val resp = makeResponse(Right("ok"), 200)
    assertEquals(resp.getStatus().getStatusCode(), 200)
  }

  test("getStatus for error code") {
    val resp = makeResponse(Left("not found"), 404)
    assertEquals(resp.getStatus().getStatusCode(), 404)
  }

  test("getResultAsString returns body on success") {
    val resp = makeResponse(Right("hello world"), 200)
    assertEquals(resp.getResultAsString(), "hello world")
  }

  test("getResultAsString returns error body on failure") {
    val resp = makeResponse(Left("error message"), 500)
    assertEquals(resp.getResultAsString(), "error message")
  }

  test("getResult returns UTF-8 bytes") {
    val resp  = makeResponse(Right("abc"), 200)
    val bytes = resp.getResult()
    assertEquals(new String(bytes, "UTF-8"), "abc")
  }

  test("getResultAsStream is readable") {
    val resp   = makeResponse(Right("stream data"), 200)
    val stream = resp.getResultAsStream()
    val bytes  = stream.readAllBytes()
    assertEquals(new String(bytes, "UTF-8"), "stream data")
  }

  test("getHeader returns value when present") {
    val resp = makeResponse(
      Right(""),
      200,
      Seq(Header("Content-Type", "application/json"))
    )
    assertEquals(resp.getHeader("Content-Type"), "application/json")
  }

  test("getHeader returns null when absent") {
    val resp = makeResponse(Right(""), 200)
    assertEquals(resp.getHeader("X-Missing"), null)
  }

  test("getHeaders groups multiple values for same header") {
    val resp = makeResponse(
      Right(""),
      200,
      Seq(
        Header("Set-Cookie", "a=1"),
        Header("Set-Cookie", "b=2"),
        Header("Content-Type", "text/html")
      )
    )
    val headers = resp.getHeaders()
    assertEquals(headers.get("Set-Cookie").size(), 2)
    assertEquals(headers.get("Content-Type").size(), 1)
  }
}
