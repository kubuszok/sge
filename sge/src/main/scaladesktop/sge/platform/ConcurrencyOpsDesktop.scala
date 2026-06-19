// SGE Concurrency Ops — JVM/Native implementation
//
// Uses a single daemon thread for async asset loading, matching LibGDX's original behavior.
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: extracted from AssetManager for cross-platform support
//   Idiom: boundary/break (0 return), Nullable (0 null), split packages
//   Audited: 2026-03-17

package sge
package platform

import scala.concurrent.ExecutionContext

private[sge] object ConcurrencyOpsDesktop extends ConcurrencyOps {

  // A per-instance owned executor: one single-thread daemon ExecutorService, shut down
  // independently of every other. Mirrors LibGDX's `new AsyncExecutor(1, "AssetManager")`.
  final private class DesktopOwnedExecutor extends OwnedExecutor {

    private val executorService: java.util.concurrent.ExecutorService =
      java.util.concurrent.Executors.newSingleThreadExecutor { (r: Runnable) =>
        val t = new Thread(r, "AssetManager")
        t.setDaemon(true)
        t
      }

    override val executor: ExecutionContext = ExecutionContext.fromExecutorService(executorService)

    override def shutdown(): Unit = executorService.shutdown()
  }

  override def createExecutor(): OwnedExecutor = new DesktopOwnedExecutor

  override def yieldThread(): Unit = Thread.`yield`()
}
