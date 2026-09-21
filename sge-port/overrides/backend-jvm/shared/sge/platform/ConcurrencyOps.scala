/*
 * SGE Concurrency Ops — platform abstraction for background execution
 *
 * Provides a platform-specific ExecutionContext for async asset loading.
 * JVM/Native: single-thread ExecutorService. JS: ExecutionContext.global (single-threaded by nature).
 *
 * Migration notes:
 *   Origin: SGE-original (platform abstraction)
 *   Convention: extracted from AssetManager to support cross-platform builds
 *   Idiom: boundary/break (0 return), Nullable (0 null), split packages
 *   Audited: 2026-03-17
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 51
 * Covenant-baseline-methods: OwnedExecutor,executor,shutdown,ConcurrencyOps,createExecutor,yieldThread
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-06-19
 */
package sge
package platform

import scala.concurrent.ExecutionContext

/** A single, independently-shutdownable execution context owned by one caller.
  *
  * Mirrors LibGDX's per-instance `AsyncExecutor` (e.g. AssetManager owns its own `new AsyncExecutor(1, "AssetManager")` and `dispose()`s only that one). Shutting one `OwnedExecutor` down must never
  * affect any other `OwnedExecutor` handed out by the same platform `ConcurrencyOps`.
  */
private[sge] trait OwnedExecutor {

  /** An ExecutionContext suitable for async loading tasks. */
  def executor: ExecutionContext

  /** Shut down only this executor's backing thread pool (if any). No-op on single-threaded platforms. */
  def shutdown(): Unit
}

/** Platform-specific provider of an ExecutionContext for background work (e.g. asset loading). */
private[sge] trait ConcurrencyOps {

  /** Create a fresh, independently-shutdownable executor.
    *
    * Each call returns a NEW [[OwnedExecutor]] (JVM/Native: a new single-thread daemon ExecutorService). Shutting one down does not affect any other. This replaces the previous single shared
    * `executor`/`shutdown()` pair, which let one caller (e.g. a disposed AssetManager) terminate the process-wide executor used by every other caller.
    */
  def createExecutor(): OwnedExecutor

  /** Yield the current thread to other runnable threads. No-op on single-threaded platforms. */
  def yieldThread(): Unit
}
