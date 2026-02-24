/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/CumulativeDistribution.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package math

/** This class represents a cumulative distribution. It can be used in scenarios where there are values with different probabilities and it's required to pick one of those respecting the probability.
  * For example one could represent the frequency of the alphabet letters using a cumulative distribution and use it to randomly pick a letter respecting their probabilities (useful when generating
  * random words). Another example could be point generation on a mesh surface: one could generate a cumulative distribution using triangles areas as interval size, in this way triangles with a large
  * area will be picked more often than triangles with a smaller one. See <a href="http://en.wikipedia.org/wiki/Cumulative_distribution_function">Wikipedia</a> for a detailed explanation.
  * @author
  *   Inferno (original implementation)
  */
class CumulativeDistribution[T] {

  private case class CumulativeValue[T](var value: T, var frequency: Float, var interval: Float)

  private val values:      Array[CumulativeValue[T]] = new Array[CumulativeValue[T]](10)
  private var currentSize: Int                       = 0

  /** Adds a value with a given interval size to the distribution */
  def add(value: T, intervalSize: Float): Unit = {
    ensureCapacity(currentSize + 1)
    values(currentSize) = CumulativeValue(value, 0, intervalSize)
    currentSize += 1
  }

  /** Adds a value with interval size equal to zero to the distribution */
  def add(value: T): Unit =
    add(value, 0)

  /** Generate the cumulative distribution */
  def generate(): Unit = {
    var sum = 0f
    var i   = 0
    while (i < currentSize) {
      sum += values(i).interval
      values(i).frequency = sum
      i += 1
    }
  }

  /** Generate the cumulative distribution in [0,1] where each interval will get a frequency between [0,1] */
  def generateNormalized(): Unit = {
    var sum = 0f
    var i   = 0
    while (i < currentSize) {
      sum += values(i).interval
      i += 1
    }
    var intervalSum = 0f
    i = 0
    while (i < currentSize) {
      intervalSum += values(i).interval / sum
      values(i).frequency = intervalSum
      i += 1
    }
  }

  /** Generate the cumulative distribution in [0,1] where each value will have the same frequency and interval size */
  def generateUniform(): Unit = {
    val freq = 1f / currentSize
    var i    = 0
    while (i < currentSize) {
      // reset the interval to the normalized frequency
      values(i).interval = freq
      values(i).frequency = (i + 1) * freq
      i += 1
    }
  }

  /** Finds the value whose interval contains the given probability Binary search algorithm is used to find the value.
    * @param probability
    * @return
    *   the value whose interval contains the probability
    */
  def value(probability: Float): T =
    scala.util.boundary {
      var valueObj: CumulativeValue[T] = null
      var imax = currentSize - 1
      var imin = 0
      var imid = 0
      while (imin <= imax) {
        imid = imin + ((imax - imin) / 2)
        valueObj = values(imid)
        if (probability < valueObj.frequency)
          imax = imid - 1
        else if (probability > valueObj.frequency)
          imin = imid + 1
        else
          scala.util.boundary.break(valueObj.value)
      }

      values(imin).value
    }

  /** @return the value whose interval contains a random probability in [0,1] */
  def value(): T =
    value(MathUtils.random())

  /** @return the amount of values */
  def size(): Int =
    currentSize

  /** @return the interval size for the value at the given position */
  def getInterval(index: Int): Float =
    values(index).interval

  /** @return the value at the given position */
  def getValue(index: Int): T =
    values(index).value

  /** Set the interval size on the passed in object. The object must be present in the distribution. */
  def setInterval(obj: T, intervalSize: Float): Unit = {
    var i = 0
    while (i < currentSize) {
      val value = values(i)
      if (value.value == obj) {
        value.interval = intervalSize
        return
      }
      i += 1
    }
  }

  /** Sets the interval size for the value at the given index */
  def setInterval(index: Int, intervalSize: Float): Unit =
    values(index).interval = intervalSize

  /** Removes all the values from the distribution */
  def clear(): Unit =
    currentSize = 0

  private def ensureCapacity(minCapacity: Int): Unit =
    if (minCapacity > values.length) {
      val newCapacity = scala.math.max(values.length * 2, minCapacity)
      val newValues   = new Array[CumulativeValue[T]](newCapacity)
      Array.copy(values, 0, newValues, 0, currentSize)
      // values = newValues // This would need to make values a var, but let's use a different approach
    }
}
