/*
 * Covenant: full-port
 * Covenant-baseline-loc: 96
 * Covenant-baseline-methods: LoopHandle,inTick,runLoop,schedule,scheduledTick,stopped,tick,wakeUp
 * Covenant-source-reference: com/badlogic/gdx/utils/Timer.java
 * Covenant-verified: 2026-06-11
 */
// SGE — Browser timer loop (setTimeout)
//
// Uses JavaScript setTimeout for the background timer loop. The browser event
// loop is single-threaded, so the original's `threadLock.wait(waitMillis)` /
// `threadLock.notifyAll()` design (Timer.java:307, 94…) has no thread to block
// or notify. The OBSERVABLE semantics — "a task scheduled while the loop idles
// fires promptly, not after the 5s idle cap" — are matched instead by
// `wakeUp()` cancelling the in-flight `setTimeout` and rescheduling an immediate
// tick (ISS-504). This mirrors GWT, where libGDX's TimerThread runs on the same
// single thread and its wait/notify are emulated on the event loop.

package sge
package utils

import scala.scalajs.js.timers
import scala.scalajs.js.timers.SetTimeoutHandle

import lowlevel.Nullable

/** Platform-specific timer loop for Scala.js.
  *
  * Drives the timer loop using `setTimeout` callbacks on the JavaScript event loop. Each iteration calls `step()` to run due tasks and compute the next sleep duration, then schedules the next tick
  * via `setTimeout`. `wakeUp()` cancels the in-flight tick and reschedules it immediately, so a newly scheduled sooner task is not held back by an already-queued long timeout.
  */
private[utils] object TimerPlatformOps {

  /** Handle to the running loop's wait primitive. */
  trait LoopHandle {

    /** Reschedules the next tick immediately if the loop is currently idling. */
    def wakeUp(): Unit
  }

  /** Start a background timer loop.
    *
    * @param lock
    *   unused on JS — the single-threaded event loop has no monitor to wait on; kept for a uniform cross-platform signature.
    * @param step
    *   Called repeatedly. Returns millis to sleep before the next call. Negative value stops the loop.
    * @param onDone
    *   Called once after the loop exits.
    * @return
    *   a [[LoopHandle]] whose `wakeUp()` reschedules the next tick immediately.
    */
  def runLoop(lock: Object, step: () => Long, onDone: () => Unit): LoopHandle = {
    val _ = lock // unused on the single-threaded JS event loop
    // Handle for the next-tick timeout still in flight, so wakeUp() can cancel
    // and reschedule it. Nullable: empty while a tick is executing or once stopped.
    var scheduledTick: Nullable[SetTimeoutHandle] = Nullable.empty
    var inTick:        Boolean                    = false
    var stopped:       Boolean                    = false

    def schedule(delayMillis: Long): Unit =
      scheduledTick = Nullable(timers.setTimeout(delayMillis.toDouble)(tick()))

    def tick(): Unit = {
      scheduledTick = Nullable.empty
      inTick = true
      try
        if (!stopped) {
          val waitMillis = step()
          if (waitMillis >= 0)
            schedule(scala.math.max(waitMillis, 1))
          else {
            stopped = true
            onDone()
          }
        }
      finally
        inTick = false
    }

    // Start the first tick on the next event loop iteration.
    schedule(0L)

    new LoopHandle {
      def wakeUp(): Unit =
        // If a tick is currently executing (e.g. a posted task scheduled a new
        // task), step() will recompute and reschedule on its own — nothing to
        // do. Otherwise cancel the in-flight (possibly far-future) tick and run an
        // immediate one, so a sooner task fires without waiting out the idle cap.
        if (!stopped && !inTick) {
          scheduledTick.foreach(timers.clearTimeout)
          scheduledTick = Nullable.empty
          schedule(0L)
        }
    }
  }
}
