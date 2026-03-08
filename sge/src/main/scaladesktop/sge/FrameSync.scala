/*
 * Copyright (c) 2002-2012 LWJGL Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of 'LWJGL' nor the names of its contributors may be used
 *   to endorse or promote products derived from this software without specific
 *   prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Migration notes:
 *   Renames: Sync -> FrameSync
 *   Convention: glfwGetTime() replaced with System.nanoTime() (no FFI dependency)
 *   Convention: inner class RunningAvg -> private class
 *   Idiom: no return, split packages
 *   Audited: 2026-03-08
 *
 * Ported to Scala 3 for SGE
 */
package sge

/** A highly accurate sync method that continually adapts to the system it runs on to provide reliable results.
  *
  * @author
  *   Riven (original implementation)
  * @author
  *   kappaOne (original implementation)
  */
private[sge] class FrameSync {

  /** number of nanoseconds in a second */
  private val NanosInSecond: Long = 1000L * 1000L * 1000L

  /** The time to sleep/yield until the next frame */
  private var nextFrame: Long = 0L

  /** whether the initialisation code has run */
  private var initialised: Boolean = false

  /** for calculating the averages the previous sleep/yield times are stored */
  private val sleepDurations: FrameSync.RunningAvg = FrameSync.RunningAvg(10)
  private val yieldDurations: FrameSync.RunningAvg = FrameSync.RunningAvg(10)

  /** An accurate sync method that will attempt to run at a constant frame rate. It should be called once every frame.
    *
    * @param fps
    *   the desired frame rate, in frames per second
    */
  def sync(fps: Int): Unit =
    if (fps > 0) {
      if (!initialised) initialise()

      try {
        // sleep until the average sleep time is greater than the time remaining till nextFrame
        var t0 = getTime()
        while ((nextFrame - t0) > sleepDurations.avg()) {
          Thread.sleep(1)
          val t1 = getTime()
          sleepDurations.add(t1 - t0) // update average sleep time
          t0 = t1
        }

        // slowly dampen sleep average if too high to avoid yielding too much
        sleepDurations.dampenForLowResTicker()

        // yield until the average yield time is greater than the time remaining till nextFrame
        t0 = getTime()
        while ((nextFrame - t0) > yieldDurations.avg()) {
          Thread.`yield`()
          val t1 = getTime()
          yieldDurations.add(t1 - t0) // update average yield time
          t0 = t1
        }
      } catch {
        case _: InterruptedException => // ignored
      }

      // schedule next frame, drop frame(s) if already too late for next frame
      nextFrame = Math.max(nextFrame + NanosInSecond / fps, getTime())
    }

  /** This method will initialise the sync method by setting initial values for sleepDurations/yieldDurations and nextFrame.
    *
    * If running on windows it will start the sleep timer fix.
    */
  private def initialise(): Unit = {
    initialised = true

    sleepDurations.init(1000 * 1000)
    yieldDurations.init((-(getTime() - getTime()) * 1.333).toInt)

    nextFrame = getTime()

    val osName = System.getProperty("os.name")

    if (osName.startsWith("Win")) {
      // On windows the sleep functions can be highly inaccurate by
      // over 10ms making in unusable. However it can be forced to
      // be a bit more accurate by running a separate sleeping daemon
      // thread.
      val timerAccuracyThread = Thread(() =>
        try
          Thread.sleep(Long.MaxValue)
        catch {
          case _: Exception => // ignored
        }
      )

      timerAccuracyThread.setName("SGE Timer")
      timerAccuracyThread.setDaemon(true)
      timerAccuracyThread.start()
    }
  }

  /** Get the system time in nanoseconds.
    *
    * @return
    *   will return the current time in nanos
    */
  private def getTime(): Long = System.nanoTime()
}

private[sge] object FrameSync {

  private class RunningAvg(slotCount: Int) {
    private val slots:  Array[Long] = new Array[Long](slotCount)
    private var offset: Int         = 0

    private val DampenThreshold: Long  = 10 * 1000L * 1000L // 10ms
    private val DampenFactor:    Float = 0.9f // don't change: 0.9f is exactly right!

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
      if (avg() > DampenThreshold) {
        var i = 0
        while (i < slots.length) {
          slots(i) = (slots(i) * DampenFactor).toLong
          i += 1
        }
      }
  }
}
