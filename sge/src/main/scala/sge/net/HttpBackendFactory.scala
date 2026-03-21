/*
 * SGE - Scala Game Engine
 * copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (platform abstraction for sttp HTTP backends)
 *   Convention: private[net] trait — same pattern as PlatformOps/BufferOps
 *   Idiom: split packages
 */
package sge
package net

import scala.concurrent.Future

/** Platform abstraction for HTTP backends. Each platform (JVM, JS, Native) provides an `HttpBackendFactoryImpl` object that creates the appropriate sttp backend and exposes a uniform `Future`-based
  * send method.
  */
private[net] trait HttpBackendFactory {

  /** Sends an sttp request and returns a Future of the response. The body is decoded as a String on both success and error paths.
    */
  def send(request: SttpRequest[Either[String, String]]): Future[SttpResponse[Either[String, String]]]

  /** Closes the underlying sttp backend and releases resources. */
  def close(): Unit
}
