/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/WindowedMean.java
 * Original authors: badlogicgames@gmail.com
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package math

/** A simple class keeping track of the mean of a stream of values within a certain window. the WindowedMean will only return a value in case enough data has been sampled. After enough data has been
  * sampled the oldest sample will be replaced by the newest in case a new sample is added.
  *
  * @author
  *   badlogicgames@gmail.com (original implementation)
  */
class WindowedMean(windowSize: Int) {
  private val values      = new Array[Float](windowSize)
  private var addedValues = 0
  private var lastValue   = 0
  private var mean        = 0f
  private var dirty       = true

  /** @return whether the value returned will be meaningful */
  def hasEnoughData(): Boolean =
    addedValues >= values.length

  /** clears this WindowedMean. The class will only return meaningful values after enough data has been added again. */
  def clear(): Unit = {
    addedValues = 0
    lastValue = 0
    for (i <- values.indices)
      values(i) = 0f
    dirty = true
  }

  /** adds a new sample to this mean. In case the window is full the oldest value will be replaced by this new value.
    *
    * @param value
    *   The value to add
    */
  def addValue(value: Float): Unit = {
    if (addedValues < values.length) addedValues += 1
    values(lastValue) = value
    lastValue += 1
    if (lastValue > values.length - 1) lastValue = 0
    dirty = true
  }

  /** returns the mean of the samples added to this instance. Only returns meaningful results when at least window_size samples as specified in the constructor have been added.
    * @return
    *   the mean
    */
  def getMean(): Float =
    if (hasEnoughData()) {
      if (dirty) {
        var meanSum = 0f
        for (i <- values.indices)
          meanSum += values(i)

        this.mean = meanSum / values.length
        dirty = false
      }
      this.mean
    } else
      0f

  /** @return the oldest value in the window */
  def getOldest(): Float =
    if (addedValues < values.length) values(0) else values(lastValue)

  /** @return the value last added */
  def getLatest(): Float = {
    val index = if (lastValue - 1 == -1) values.length - 1 else lastValue - 1
    values(index)
  }

  /** @return The standard deviation */
  def standardDeviation(): Float = {
    if (!hasEnoughData()) return 0f

    val meanValue = getMean()
    var sum       = 0f
    for (i <- values.indices)
      sum += (values(i) - meanValue) * (values(i) - meanValue)

    scala.math.sqrt(sum / values.length).toFloat
  }

  def getLowest(): Float = {
    var lowest = Float.MaxValue
    for (i <- values.indices)
      lowest = scala.math.min(lowest, values(i))
    lowest
  }

  def getHighest(): Float = {
    var highest = Float.MinValue
    for (i <- values.indices)
      highest = scala.math.max(highest, values(i))
    highest
  }

  def getValueCount(): Int =
    addedValues

  def getWindowSize(): Int =
    values.length

  /** @return
    *   A new Array[Float] containing all values currently in the window of the stream, in order from oldest to latest. The length of the array is smaller than the window size if not enough data has
    *   been added.
    */
  def getWindowValues(): Array[Float] = {
    val windowValues = new Array[Float](addedValues)
    if (hasEnoughData()) {
      for (i <- windowValues.indices)
        windowValues(i) = values((i + lastValue) % values.length)
    } else {
      Array.copy(values, 0, windowValues, 0, addedValues)
    }
    windowValues
  }
}
