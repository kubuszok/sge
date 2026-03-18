/*
 * Ported from LWJGL Project (BSD license)
 * Original source: backends/gdx-backend-lwjgl3/.../Sync.java
 * Original authors: Riven, kappaOne
 *
 * Migration notes:
 *   Convention: Java class -> Scala class; inner RunningAvg -> private class
 *   Convention: glfwGetTime() abstracted via WindowingOps.getTime()
 *   Idiom: split packages
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.platform.WindowingOps

/** A highly accurate sync method that continually adapts to the system it runs on to provide reliable results.
  *
  * @param windowing
  *   the windowing ops for time queries
  * @author
  *   Riven, kappaOne (original implementation)
  */
private[sge] class Sync(private val windowing: WindowingOps) {

  final private val NANOS_IN_SECOND = 1000L * 1000L * 1000L

  private var nextFrame:   Long    = 0L
  private var initialised: Boolean = false

  private val sleepDurations = new RunningAvg(10)
  private val yieldDurations = new RunningAvg(10)

  /** An accurate sync method that will attempt to run at a constant frame rate. It should be called once every frame.
    *
    * @param fps
    *   the desired frame rate, in frames per second
    */
  def sync(fps: Int): Unit =
    if (fps <= 0) {
      // do nothing
    } else {
      if (!initialised) initialise()

      try {
        // sleep until the average sleep time is greater than the time remaining till nextFrame
        var t0 = time
        while ((nextFrame - t0) > sleepDurations.avg()) {
          Thread.sleep(1)
          val t1 = time
          sleepDurations.add(t1 - t0)
          t0 = t1
        }

        // slowly dampen sleep average if too high to avoid yielding too much
        sleepDurations.dampenForLowResTicker()

        // yield until the average yield time is greater than the time remaining till nextFrame
        t0 = time
        while ((nextFrame - t0) > yieldDurations.avg()) {
          Thread.`yield`()
          val t1 = time
          yieldDurations.add(t1 - t0)
          t0 = t1
        }
      } catch {
        case _: InterruptedException => ()
      }

      // schedule next frame, drop frame(s) if already too late for next frame
      nextFrame = scala.math.max(nextFrame + NANOS_IN_SECOND / fps, time)
    }

  private def initialise(): Unit = {
    initialised = true

    sleepDurations.init(1000 * 1000)
    yieldDurations.init((-(time - time) * 1.333).toInt)

    nextFrame = time

    val osName = System.getProperty("os.name")
    if (osName.startsWith("Win")) {
      // On windows the sleep functions can be highly inaccurate by
      // over 10ms making in unusable. However it can be forced to
      // be a bit more accurate by running a separate sleeping daemon
      // thread.
      val timerAccuracyThread = new Thread(() =>
        try Thread.sleep(Long.MaxValue)
        catch { case _: Exception => () }
      )
      timerAccuracyThread.setName("SGE Timer")
      timerAccuracyThread.setDaemon(true)
      timerAccuracyThread.start()
    }
  }

  private def time: Long =
    (windowing.time * NANOS_IN_SECOND).toLong

  private class RunningAvg(slotCount: Int) {
    private val slots  = new Array[Long](slotCount)
    private var offset = 0

    final private val DAMPEN_THRESHOLD = 10 * 1000L * 1000L // 10ms
    final private val DAMPEN_FACTOR    = 0.9f

    def init(value: Long): Unit =
      while (offset < slots.length) {
        slots(offset) = value
        offset += 1
      }

    def add(value: Long): Unit = {
      slots(offset % slots.length) = value
      offset += 1
      offset %= slots.length
    }

    def avg(): Long = {
      var sum = 0L
      var i   = 0
      while (i < slots.length) {
        sum += slots(i)
        i += 1
      }
      sum / slots.length
    }

    def dampenForLowResTicker(): Unit =
      if (avg() > DAMPEN_THRESHOLD) {
        var i = 0
        while (i < slots.length) {
          slots(i) = (slots(i) * DAMPEN_FACTOR).toLong
          i += 1
        }
      }
  }
}
