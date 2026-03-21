/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (Native HTTP backend via sttp + curl)
 *   Convention: platform-specific impl in scalanative/
 *   Idiom: split packages
 *   Audited: 2026-03-08
 */
package sge
package net

import scala.concurrent.{ ExecutionContext, Future }
import sttp.client4.DefaultSyncBackend

/** Native implementation: uses sttp's `DefaultSyncBackend` (curl) wrapped in a `Future` for API uniformity with JVM/JS backends.
  */
private[net] object HttpBackendFactoryImpl extends HttpBackendFactory {

  private val backend            = DefaultSyncBackend()
  private given ExecutionContext = ExecutionContext.global

  override def send(request: SttpRequest[Either[String, String]]): Future[SttpResponse[Either[String, String]]] =
    Future {
      backend.send(request)
    }

  override def close(): Unit =
    backend.close()
}
