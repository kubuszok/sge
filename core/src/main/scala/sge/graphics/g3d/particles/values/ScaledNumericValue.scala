/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/values/ScaledNumericValue.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */

package sge
package graphics
package g3d
package particles
package values

import scala.util.boundary
import scala.util.boundary.break

import sge.math.MathUtils

/** A value which has a defined minimum and maximum upper and lower bounds. Defines the variations of the value on a time line.
  * @author
  *   Inferno
  */
class ScaledNumericValue extends RangedNumericValue {
  private var scaling:  Array[Float] = Array(1f)
  var timeline:         Array[Float] = Array(0f)
  private var highMin:  Float        = 0f
  private var highMax:  Float        = 0f
  private var relative: Boolean      = false

  def newHighValue(): Float =
    highMin + (highMax - highMin) * MathUtils.random()

  def setHigh(value: Float): Unit = {
    highMin = value
    highMax = value
  }

  def setHigh(min: Float, max: Float): Unit = {
    highMin = min
    highMax = max
  }

  def getHighMin(): Float =
    highMin

  def setHighMin(highMin: Float): Unit =
    this.highMin = highMin

  def getHighMax(): Float =
    highMax

  def setHighMax(highMax: Float): Unit =
    this.highMax = highMax

  def getScaling(): Array[Float] =
    scaling

  def setScaling(values: Array[Float]): Unit =
    this.scaling = values

  def getTimeline(): Array[Float] =
    timeline

  def setTimeline(timeline: Array[Float]): Unit =
    this.timeline = timeline

  def isRelative(): Boolean =
    relative

  def setRelative(relative: Boolean): Unit =
    this.relative = relative

  def getScale(percent: Float): Float = boundary {
    var endIndex = -1
    val n        = timeline.length
    // if (percent >= timeline[n-1])
    // return scaling[n - 1];
    boundary[Unit] {
      var i = 1
      while (i < n) {
        val t = timeline(i)
        if (t > percent) {
          endIndex = i
          break(())
        }
        i += 1
      }
    }
    if (endIndex == -1) break(scaling(n - 1))
    val startIndex = endIndex - 1
    val startValue = scaling(startIndex)
    val startTime  = timeline(startIndex)
    startValue + (scaling(endIndex) - startValue) * ((percent - startTime) / (timeline(endIndex) - startTime))
  }

  def load(value: ScaledNumericValue): Unit = {
    super.load(value)
    highMax = value.highMax
    highMin = value.highMin
    scaling = new Array[Float](value.scaling.length)
    Array.copy(value.scaling, 0, scaling, 0, scaling.length)
    timeline = new Array[Float](value.timeline.length)
    Array.copy(value.timeline, 0, timeline, 0, timeline.length)
    relative = value.relative
  }
}
