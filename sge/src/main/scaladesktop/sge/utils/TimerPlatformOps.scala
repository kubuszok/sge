// SGE — Desktop + Native timer loop (Gears async)
//
// Uses Gears structured concurrency for the background timer loop.
// JVM: virtual threads (JDK 21+)
// Scala Native: delimited continuations

package sge
package utils

import gears.async.*
import gears.async.default.given
import scala.concurrent.duration.*

/** Platform-specific timer loop for JVM + Scala Native.
  *
  * Runs the background loop inside a Gears `Async.blocking` context, using `AsyncOperations.sleep` for non-blocking delays. The loop is launched on `ExecutionContext.global` to avoid blocking the
  * calling thread.
  */
private[utils] object TimerPlatformOps {

  /** Start a background timer loop.
    *
    * @param step
    *   Called repeatedly. Returns millis to sleep before next call. Negative value stops the loop.
    * @param onDone
    *   Called once after the loop exits (on the background thread).
    */
  def runLoop(step: () => Long, onDone: () => Unit): Unit = {
    scala.concurrent.Future {
      Async.blocking {
        var waitMillis = step()
        while (waitMillis >= 0) {
          if (waitMillis > 0) AsyncOperations.sleep(waitMillis.millis)
          waitMillis = step()
        }
      }
      onDone()
    }(using scala.concurrent.ExecutionContext.global)
    ()
  }
}
