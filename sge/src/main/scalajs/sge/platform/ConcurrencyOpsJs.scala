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

  override val executor: ExecutionContext = ExecutionContext.global

  override def shutdown(): Unit = () // no-op on JS

  override def yieldThread(): Unit = () // no-op on JS (single-threaded)
}
