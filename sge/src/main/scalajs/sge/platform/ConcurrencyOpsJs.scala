// SGE Concurrency Ops — Scala.js implementation
//
// Uses ExecutionContext.global since JS is single-threaded.
// Shutdown is a no-op.
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: extracted from AssetManager for cross-platform support
//   Idiom: boundary/break (0 return), Nullable (0 null), split packages
//   Audited: 2026-03-17

package sge
package platform

import scala.concurrent.ExecutionContext

private[sge] object ConcurrencyOpsJs extends ConcurrencyOps {

  // JS is single-threaded: every owned executor runs on ExecutionContext.global and its
  // shutdown is a safe no-op (there is no per-instance thread pool to terminate).
  private object JsOwnedExecutor extends OwnedExecutor {
    override val executor:   ExecutionContext = ExecutionContext.global
    override def shutdown(): Unit             = () // no-op on JS
  }

  override def createExecutor(): OwnedExecutor = JsOwnedExecutor

  override def yieldThread(): Unit = () // no-op on JS (single-threaded)
}
