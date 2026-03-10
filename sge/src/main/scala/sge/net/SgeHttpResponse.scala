/*
 * SGE - Scala Game Engine
 * copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (wraps sttp Response into Net.HttpResponse interface)
 *   Convention: immutable, not pooled — consumed once by HttpResponseListener
 *   Idiom: split packages
 */
package sge
package net

import java.io.{ ByteArrayInputStream, InputStream }
import sge.utils.Nullable
import sttp.client4.Response

/** Wraps an sttp [[Response]] to implement [[Net.HttpResponse]].
  *
  * Not pooled — each response is created once, handed to the listener, and discarded. Body bytes and string are lazily computed from the sttp response body.
  */
final class SgeHttpResponse private[net] (response: Response[Either[String, String]]) extends Net.HttpResponse {

  /** The response body as a string, regardless of success/error status. */
  private lazy val bodyString: String = response.body.merge

  /** The response body as bytes (UTF-8 encoded). */
  private lazy val bodyBytes: Array[Byte] = bodyString.getBytes("UTF-8")

  override def getResult(): Array[Byte] = bodyBytes

  override def getResultAsString(): String = bodyString

  override def getResultAsStream(): InputStream = new ByteArrayInputStream(bodyBytes)

  override def getStatus(): HttpStatus = HttpStatus(response.code.code)

  override def getHeader(name: String): Nullable[String] =
    Nullable.fromOption(response.header(name))

  override def getHeaders(): java.util.Map[String, java.util.List[String]] = {
    val result = new java.util.LinkedHashMap[String, java.util.List[String]]()
    for (h <- response.headers)
      result.computeIfAbsent(h.name, _ => new java.util.ArrayList[String]()).add(h.value)
    java.util.Collections.unmodifiableMap(result)
  }
}
