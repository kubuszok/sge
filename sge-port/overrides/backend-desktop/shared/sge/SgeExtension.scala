/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 32
 * Covenant-baseline-methods: SgeExtension,load,loadAll,name
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-06-17
 */
package sge

/** A loadable SGE extension. Extensions (e.g. the physics extension, which on JS must asynchronously load a Rapier WASM module) register on the platform application config and have their dependencies
  * loaded exactly once at application startup — before the game listener's `create()` runs.
  */
trait SgeExtension {

  /** Human-readable extension name (for logging/diagnostics). */
  def name: String

  /** Load this extension's dependencies. Default: nothing to load.
    *
    * On JS an extension may return a Future that completes asynchronously (e.g. after a WASM module finishes loading); on JVM/Native it is typically an already-completed Future.
    */
  def load()(using Sge): scala.concurrent.Future[Unit] = scala.concurrent.Future.unit
}

object SgeExtension {

  /** Load all extensions SEQUENTIALLY in order; the returned Future completes when every extension's `load()` has completed. Each `load()` starts only after the previous extension's Future has
    * resolved.
    */
  def loadAll(
    extensions: Seq[SgeExtension]
  )(using Sge, scala.concurrent.ExecutionContext): scala.concurrent.Future[Unit] =
    extensions.foldLeft(scala.concurrent.Future.successful(()))((acc, ext) => acc.flatMap(_ => ext.load()))
}
