// SGE — Browser timer loop (setTimeout)
//
// Uses JavaScript setTimeout for the background timer loop.
// Single-threaded — no real concurrency, but matches JS event loop model.

package sge
package utils

import scala.scalajs.js.timers

/** Platform-specific timer loop for Scala.js.
  *
  * Drives the timer loop using `setTimeout` callbacks on the JavaScript event loop. Each iteration calls `step()` to check tasks and compute the next sleep duration, then schedules the next tick via
  * `setTimeout`.
  */
private[utils] object TimerPlatformOps {

  /** Start a background timer loop.
    *
    * @param step
    *   Called repeatedly. Returns millis to sleep before next call. Negative value stops the loop.
    * @param onDone
    *   Called once after the loop exits.
    */
  def runLoop(step: () => Long, onDone: () => Unit): Unit = {
    def tick(): Unit = {
      val waitMillis = step()
      if (waitMillis >= 0)
        timers.setTimeout(scala.math.max(waitMillis, 1).toDouble)(tick())
      else
        onDone()
    }
    // Start the first tick on the next event loop iteration
    timers.setTimeout(0)(tick())
  }
}
