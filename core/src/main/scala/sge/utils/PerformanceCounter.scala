package sge.utils

import sge.math.{ FloatCounter, MathUtils }

/** Class to keep track of the time and load (percentage of total time) a specific task takes. Call start() just before starting the task and stop() right after. You can do this multiple times if
  * required. Every render or update call tick() to update the values. The time FloatCounter provides access to the minimum, maximum, average, total and current time (in seconds) the task takes.
  * Likewise for the load value, which is the percentage of the total time.
  * @author
  *   xoppa (original implementation)
  */
class PerformanceCounter(val name: String, windowSize: Int = 5) {
  private val nano2seconds = MathUtils.nanoToSec
  private var startTime    = 0L
  private var lastTick     = 0L

  /** The time value of this counter (seconds) */
  val time: FloatCounter = new FloatCounter(windowSize)

  /** The load value of this counter */
  val load: FloatCounter = new FloatCounter(1)

  /** The current value in seconds, you can manually increase this using your own timing mechanism if needed, if you do so, you also need to update valid.
    */
  var current = 0f

  /** Flag to indicate that the current value is valid, you need to set this to true if using your own timing mechanism */
  var valid = false

  /** Updates the time and load counters and resets the time. Call start() to begin a new count. The values are only valid after at least two calls to this method.
    */
  def tick(): Unit = {
    val t = TimeUtils.nanoTime()
    if (lastTick > 0L) tick((t - lastTick) * nano2seconds)
    lastTick = t
  }

  /** Updates the time and load counters and resets the time. Call start() to begin a new count.
    * @param delta
    *   The time since the last call to this method
    */
  def tick(delta: Float): Unit = {
    if (!valid) {
      throw SgeError.InvalidInput("Invalid data, check if you called PerformanceCounter#stop()")
    }

    time.put(current)

    val currentLoad = if (delta == 0f) 0f else current / delta
    load.put(if (delta > 1f) currentLoad else delta * currentLoad + (1f - delta) * load.latest)

    current = 0f
    valid = false
  }

  /** Start counting, call this method just before performing the task you want to keep track of. Call stop() when done.
    */
  def start(): Unit = {
    startTime = TimeUtils.nanoTime()
    valid = false
  }

  /** Stop counting, call this method right after you performed the task you want to keep track of. Call start() again when you perform more of that task.
    */
  def stop(): Unit =
    if (startTime > 0L) {
      current += (TimeUtils.nanoTime() - startTime) * nano2seconds
      startTime = 0L
      valid = true
    }

  /** Resets this performance counter to its defaults values. */
  def reset(): Unit = {
    time.reset()
    load.reset()
    startTime = 0L
    lastTick = 0L
    current = 0f
    valid = false
  }

  override def toString: String = {
    val sb = new StringBuilder()
    toString(sb).toString
  }

  /** Creates a string in the form of "name [time: value, load: value]" */
  def toString(sb: StringBuilder): StringBuilder = {
    sb.append(name).append(": [time: ").append(time.value).append(", load: ").append(load.value).append("]")
    sb
  }
}
