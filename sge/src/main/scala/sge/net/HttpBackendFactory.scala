/*
 * SGE - Scala Game Engine
 * copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (platform abstraction for sttp HTTP backends)
 *   Convention: private[net] trait — same pattern as PlatformOps/BufferOps
 *   Idiom: split packages
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 35
 * Covenant-baseline-methods: HttpBackendFactory,close,send
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-06-12
 */
package sge
package net

import scala.concurrent.Future

/** Platform abstraction for HTTP backends. Each platform (JVM, JS, Native) provides an `HttpBackendFactoryImpl` object that creates the appropriate sttp backend and exposes a uniform `Future`-based
  * send method.
  */
private[net] trait HttpBackendFactory {

  /** Sends an sttp request and returns a Future of the response. The body is the raw response bytes on both success and error paths (sttp `asByteArrayAlways`), mirroring NetJavaImpl.getResult() which
    * copies the raw connection stream with no charset (NetJavaImpl.java:62-78); the String view is decoded from those bytes downstream in [[SgeHttpResponse]] (ISS-521).
    */
  def send(request: SttpRequest[Array[Byte]]): Future[SttpResponse[Array[Byte]]]

  /** Closes the underlying sttp backend and releases resources. */
  def close(): Unit
}
