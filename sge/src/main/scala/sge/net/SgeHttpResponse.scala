/*
 * SGE - Scala Game Engine
 * copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (wraps sttp Response into Net.HttpResponse interface)
 *   Convention: immutable, not pooled — consumed once by HttpResponseListener
 *   Idiom: split packages
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 56
 * Covenant-baseline-methods: SgeHttpResponse,bodyBytes,bodyString,getHeader,headers,result,resultAsStream,resultAsString,status
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-06-12
 */
package sge
package net

import java.io.{ ByteArrayInputStream, InputStream }
import lowlevel.Nullable

/** Wraps an sttp [[Response]] to implement [[Net.HttpResponse]].
  *
  * Not pooled — each response is created once, handed to the listener, and discarded. The body is the raw response bytes (sttp `asByteArrayAlways`); the string view is lazily decoded FROM those
  * bytes, never the other way round. This preserves the LibGDX byte-exact contract: NetJavaImpl.getResult() copies the connection InputStream straight into a byte[] with no charset
  * (NetJavaImpl.java:62-78), and NetJavaImpl.getResultAsStream() streams the connection's raw bytes (NetJavaImpl.java:98-101). Decoding bytes→String here (rather than String→bytes) means binary
  * downloads survive intact: a body that is not valid UTF-8 is no longer mangled into U+FFFD replacement characters (ISS-521).
  */
final class SgeHttpResponse private[net] (response: SttpResponse[Array[Byte]]) extends Net.HttpResponse {

  /** The response body as raw bytes, regardless of success/error status. This is the primary representation — the LibGDX getResult() contract. */
  private lazy val bodyBytes: Array[Byte] = response.body

  /** The response body as a string, lazily decoded from the raw bytes as UTF-8. */
  private lazy val bodyString: String = new String(bodyBytes, "UTF-8")

  override def result: Array[Byte] = bodyBytes

  override def resultAsString: String = bodyString

  override def resultAsStream: InputStream = new ByteArrayInputStream(bodyBytes)

  override def status: HttpStatus = HttpStatus(response.code.code)

  override def getHeader(name: String): Nullable[String] =
    Nullable.fromOption(response.header(name))

  override def headers: java.util.Map[String, java.util.List[String]] = {
    val result = new java.util.LinkedHashMap[String, java.util.List[String]]()
    for (h <- response.headers)
      result.computeIfAbsent(h.name, _ => new java.util.ArrayList[String]()).add(h.value)
    java.util.Collections.unmodifiableMap(result)
  }
}
