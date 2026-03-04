/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package net

import munit.FunSuite
import sge.utils.Pool

class SgeHttpRequestTest extends FunSuite {

  test("default values after construction") {
    val req = new SgeHttpRequest()
    assertEquals(req.method, Net.HttpMethod.GET)
    assertEquals(req.url, "")
    assert(req.content.isEmpty)
    assert(req.contentBytes.isEmpty)
    assertEquals(req.timeoutMs, 0)
    assertEquals(req.followRedirects, true)
  }

  test("fluent setters return this") {
    val req    = new SgeHttpRequest()
    val result = req
      .withMethod(Net.HttpMethod.POST)
      .withUrl("https://example.com")
      .withHeader("Content-Type", "application/json")
      .withContent("""{"key":"value"}""")
      .withTimeoutMs(5000)
      .withFollowRedirects(false)

    assert(result eq req, "fluent setters should return the same instance")
    assertEquals(req.method, Net.HttpMethod.POST)
    assertEquals(req.url, "https://example.com")
    assertEquals(req.content.getOrElse(""), """{"key":"value"}""")
    assertEquals(req.timeoutMs, 5000)
    assertEquals(req.followRedirects, false)
  }

  test("withContentBytes sets byte body") {
    val req   = new SgeHttpRequest()
    val bytes = Array[Byte](1, 2, 3)
    req.withContentBytes(bytes)
    assert(req.contentBytes.isDefined)
  }

  test("headers are mutable and accessible") {
    val req = new SgeHttpRequest()
    req.withHeader("Authorization", "Bearer token123")
    req.withHeader("Accept", "application/json")
    assertEquals(req.headers.size, 2)
    assertEquals(req.headers("Authorization"), "Bearer token123")
    assertEquals(req.headers("Accept"), "application/json")
  }

  test("reset clears all fields to defaults") {
    val req = new SgeHttpRequest()
    req
      .withMethod(Net.HttpMethod.PUT)
      .withUrl("https://example.com/api")
      .withHeader("X-Custom", "value")
      .withContent("body")
      .withContentBytes(Array[Byte](1))
      .withTimeoutMs(3000)
      .withFollowRedirects(false)

    req.reset()

    assertEquals(req.method, Net.HttpMethod.GET)
    assertEquals(req.url, "")
    assert(req.content.isEmpty)
    assert(req.contentBytes.isEmpty)
    assertEquals(req.timeoutMs, 0)
    assertEquals(req.followRedirects, true)
    assert(req.headers.isEmpty)
  }

  test("pool round-trip: obtain, configure, free, re-obtain") {
    val pool = Pool.Default[SgeHttpRequest](() => new SgeHttpRequest(), 2, 4)

    val req1 = pool.obtain()
    req1.withMethod(Net.HttpMethod.DELETE).withUrl("https://example.com/resource/1")
    assertEquals(req1.method, Net.HttpMethod.DELETE)

    pool.free(req1) // triggers reset()

    val req2 = pool.obtain()
    // Should be the same instance, reset to defaults
    assert(req2 eq req1, "pool should return the same instance after free")
    assertEquals(req2.method, Net.HttpMethod.GET)
    assertEquals(req2.url, "")
  }

  test("all HttpMethod values are settable") {
    val req = new SgeHttpRequest()
    for (m <- Net.HttpMethod.values) {
      req.withMethod(m)
      assertEquals(req.method, m)
    }
  }
}
