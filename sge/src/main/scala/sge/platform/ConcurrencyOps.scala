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
 * Covenant-baseline-loc: 28
 * Covenant-baseline-methods: ConcurrencyOps,executor,shutdown,yieldThread
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package platform

import scala.concurrent.ExecutionContext

/** Platform-specific provider of an ExecutionContext for background work (e.g. asset loading). */
private[sge] trait ConcurrencyOps {

  /** An ExecutionContext suitable for async loading tasks. */
  def executor: ExecutionContext

  /** Shut down the backing thread pool (if any). No-op on single-threaded platforms. */
  def shutdown(): Unit

  /** Yield the current thread to other runnable threads. No-op on single-threaded platforms. */
  def yieldThread(): Unit
}
