/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (JVM HTTP backend via sttp + Java HttpClient)
 *   Convention: platform-specific impl in scalajvm/
 *   Idiom: split packages
 */
package sge
package net

import scala.concurrent.Future
import sttp.client4.{ DefaultFutureBackend, Response }

/** JVM implementation: uses sttp's `DefaultFutureBackend` which delegates to Java's built-in `java.net.http.HttpClient` with `Future`-based async dispatch.
  */
private[net] object HttpBackendFactoryImpl extends HttpBackendFactory {

  private val backend = DefaultFutureBackend()

  override def send(request: sttp.client4.Request[Either[String, String]]): Future[Response[Either[String, String]]] =
    backend.send(request)

  override def close(): Unit =
    backend.close()
}
