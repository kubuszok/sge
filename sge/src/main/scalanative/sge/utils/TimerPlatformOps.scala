// SGE — Scala Native timer loop (Thread-based)
//
// Uses a plain background thread with Thread.sleep for timing.
// Scala Native supports Java threads natively.

package sge
package utils

/** Platform-specific timer loop for Scala Native.
  *
  * Runs the background loop on a daemon thread using `Thread.sleep` for delays.
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
    val thread = new Thread(() =>
      try {
        var waitMillis = step()
        while (waitMillis >= 0) {
          if (waitMillis > 0) Thread.sleep(waitMillis)
          waitMillis = step()
        }
      } finally
        onDone()
    )
    thread.setDaemon(true)
    thread.setName("SGE-Timer")
    thread.start()
    ()
  }
}
