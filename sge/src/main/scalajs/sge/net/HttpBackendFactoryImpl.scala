/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (JS HTTP backend via sttp + Fetch API)
 *   Convention: platform-specific impl in scalajs/
 *   Idiom: split packages
 *   Audited: 2026-03-08
 */
package sge
package net

import scala.concurrent.Future
import sttp.client4.{ DefaultFutureBackend, Response }

/** JS implementation: uses sttp's `DefaultFutureBackend` which delegates to the browser Fetch API with `Future`-based async dispatch.
  */
private[net] object HttpBackendFactoryImpl extends HttpBackendFactory {

  private val backend = DefaultFutureBackend()

  override def send(request: sttp.client4.Request[Either[String, String]]): Future[Response[Either[String, String]]] =
    backend.send(request)

  override def close(): Unit =
    backend.close()
}
