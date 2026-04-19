/*
 * SGE - Scala Game Engine
 * copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (replaces LibGDX Net.HttpRequest with sttp-backed poolable request)
 *   Convention: mutable poolable object — same lifecycle as LibGDX HttpRequest
 *   Idiom: split packages, Nullable, Pool.Poolable
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 104
 * Covenant-baseline-methods: SgeHttpRequest,content,contentBytes,followRedirects,headers,method,reset,timeoutMs,url,withContent,withContentBytes,withFollowRedirects,withHeader,withMethod,withTimeoutMs,withUrl
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package net

import sge.utils.{ Nullable, Pool }
import scala.collection.mutable

/** Mutable, poolable HTTP request configuration. Obtain instances via [[SgeHttpClient.obtainRequest]], configure with fluent setters, and pass to [[SgeHttpClient.send]]. After the request completes
  * (or is cancelled), the client automatically returns the object to its pool.
  *
  * {{{
  *   val client  = sge.httpClient
  *   val request = client.obtainRequest()
  *   request.withMethod(Net.HttpMethod.POST).withUrl("https://example.com/api").withContent("{}")
  *   client.send(request, myListener)
  * }}}
  */
final class SgeHttpRequest private[net] () extends Pool.Poolable {

  /** HTTP method (default GET). */
  var method: Net.HttpMethod = Net.HttpMethod.GET

  /** Target URL. */
  var url: String = ""

  /** String body content. Takes precedence over [[contentBytes]] when both are set. */
  var content: Nullable[String] = Nullable.empty

  /** Raw byte body content. Used only when [[content]] is empty. */
  var contentBytes: Nullable[Array[Byte]] = Nullable.empty

  /** Request timeout in milliseconds. 0 means no timeout. */
  var timeoutMs: Int = 0

  /** Whether to follow 3xx redirects (default true). */
  var followRedirects: Boolean = true

  /** Request headers. */
  private[net] val headers: mutable.Map[String, String] = mutable.Map.empty

  // -- Fluent setters --

  /** Sets the HTTP method and returns this request. */
  def withMethod(method: Net.HttpMethod): SgeHttpRequest = {
    this.method = method
    this
  }

  /** Sets the target URL and returns this request. */
  def withUrl(url: String): SgeHttpRequest = {
    this.url = url
    this
  }

  /** Adds a header and returns this request. */
  def withHeader(name: String, value: String): SgeHttpRequest = {
    headers.put(name, value)
    this
  }

  /** Sets a string body and returns this request. */
  def withContent(content: String): SgeHttpRequest = {
    this.content = Nullable(content)
    this
  }

  /** Sets a byte-array body and returns this request. */
  def withContentBytes(bytes: Array[Byte]): SgeHttpRequest = {
    this.contentBytes = Nullable(bytes)
    this
  }

  /** Sets the timeout in milliseconds and returns this request. */
  def withTimeoutMs(ms: Int): SgeHttpRequest = {
    this.timeoutMs = ms
    this
  }

  /** Sets redirect-following behaviour and returns this request. */
  def withFollowRedirects(follow: Boolean): SgeHttpRequest = {
    this.followRedirects = follow
    this
  }

  /** Clears all fields to defaults. Called automatically when the pool reclaims this object. */
  override def reset(): Unit = {
    method = Net.HttpMethod.GET
    url = ""
    content = Nullable.empty
    contentBytes = Nullable.empty
    timeoutMs = 0
    followRedirects = true
    headers.clear()
  }
}
