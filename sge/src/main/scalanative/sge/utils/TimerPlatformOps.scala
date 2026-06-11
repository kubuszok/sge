/*
 * Covenant: full-port
 * Covenant-baseline-loc: 77
 * Covenant-baseline-methods: LoopHandle,runLoop,wakeUp
 * Covenant-source-reference: com/badlogic/gdx/utils/Timer.java
 * Covenant-verified: 2026-06-11
 */
// SGE — Scala Native timer loop (daemon thread + monitor wait/notify)
//
// Faithful port of the original libGDX TimerThread (Timer.java): a daemon
// thread blocks in `threadLock.wait(waitMillis)` between steps and is woken by
// `threadLock.notifyAll()`, so a newly scheduled task wakes the idle loop
// instead of waiting out the 5s idle cap (ISS-504).
//
// Scala Native 0.5 ships a real multithreaded runtime with working object
// monitors: `scala.scalanative.runtime.monitor.{BasicMonitor, ObjectMonitor}`
// back `synchronized` / `Object.wait` / `Object.notifyAll`, and
// `java.lang.Thread` runs arbitrary runnables on OS threads (the engine's
// desktop target enables multithreading — the previous implementation already
// relied on `new Thread(...).setDaemon(true).start()`). This differs from
// Scala Native 0.4, whose `runtime.Monitor` did nothing (a no-op) and whose
// `java.lang.Thread` rejected non-main runnables, so wait/notify were
// unavailable there.

package sge
package utils

/** Platform-specific timer loop for Scala Native.
  *
  * Runs the background loop on a daemon thread that waits on the supplied lock between steps, mirroring the original libGDX `TimerThread`. `wakeUp()` performs `lock.notifyAll()` so a task scheduled
  * while the loop idles fires promptly.
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
    *   the monitor the loop waits on between steps; `wakeUp()` notifies it.
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
