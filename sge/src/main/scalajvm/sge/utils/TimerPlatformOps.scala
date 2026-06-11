/*
 * Covenant: full-port
 * Covenant-baseline-loc: 68
 * Covenant-baseline-methods: LoopHandle,runLoop,wakeUp
 * Covenant-source-reference: com/badlogic/gdx/utils/Timer.java
 * Covenant-verified: 2026-06-11
 */
// SGE — JVM timer loop (daemon thread + monitor wait/notify)
//
// Faithful port of the original libGDX TimerThread (Timer.java): a daemon
// thread blocks in `threadLock.wait(waitMillis)` between steps and is woken by
// `threadLock.notifyAll()`. This is the JVM design verbatim — a newly scheduled
// task wakes the idle loop instead of waiting out the 5s idle cap (ISS-504).

package sge
package utils

/** Platform-specific timer loop for JVM.
  *
  * Runs the background loop on a daemon thread that waits on the supplied lock between steps, exactly as the original libGDX `TimerThread` waits on `threadLock` (Timer.java:307). `wakeUp()` performs
  * `lock.notifyAll()` (Timer.java:94, 117, 347, 354, 365) so a task scheduled while the loop idles fires promptly.
  */
private[utils] object TimerPlatformOps {

  /** Handle to the running loop's wait primitive. */
  trait LoopHandle {

    /** Wakes the loop if it is currently waiting. */
    def wakeUp(): Unit
  }

  /** Start a background timer loop.
    *
    * @param lock
    *   the monitor the loop waits on between steps; `wakeUp()` notifies it. The same object the caller synchronizes on when mutating timer state, so the step + wait happen under one monitor
    *   acquisition, as in the original.
    * @param step
    *   Called repeatedly under `lock`. Returns millis to sleep before the next call. Negative value stops the loop.
    * @param onDone
    *   Called once after the loop exits (on the background thread).
    * @return
    *   a [[LoopHandle]] whose `wakeUp()` interrupts the loop's wait.
    */
  def runLoop(lock: Object, step: () => Long, onDone: () => Unit): LoopHandle = {
    val thread = new Thread(() =>
      try {
        var running = true
        while (running)
          lock.synchronized {
            val waitMillis = step()
            if (waitMillis < 0) running = false
            else if (waitMillis > 0)
              try lock.wait(waitMillis)
              catch { case _: InterruptedException => () } // Timer.java:308 — ignored
          }
      } finally
        onDone()
    )
    thread.setDaemon(true)
    thread.setName("SGE-Timer")
    thread.start()
    new LoopHandle {
      def wakeUp(): Unit = lock.synchronized {
        lock.notifyAll()
      }
    }
  }
}
