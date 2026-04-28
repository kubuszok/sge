/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/values/ScaledNumericValue.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (2026-03-03):
 * - Json.Serializable write/read methods intentionally omitted
 * - All public methods ported: newHighValue, setHigh (x2), highMin/highMax/scaling/relative
 *   (public vars), getScale, load
 * - getScale uses boundary/break instead of return (correct pattern)
 * - Array.copy used instead of System.arraycopy (correct Scala equivalent)
 * - Fixes (2026-03-04): removed 8 redundant getters/setters (highMin/Max, scaling, timeline,
 *   relative); made fields public vars
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 87
 * Covenant-baseline-methods: ScaledNumericValue,endIndex,getScale,highMax,highMin,load,n,newHighValue,relative,scaling,setHigh,startIndex,startTime,startValue,timeline
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/particles/values/ScaledNumericValue.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: f087ba76cac7ff2cc4ded91efc009f52bf0987c2
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
  var scaling:  Array[Float] = Array(1f)
  var timeline: Array[Float] = Array(0f)
  var highMin:  Float        = 0f
  var highMax:  Float        = 0f
  var relative: Boolean      = false

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

  def getScale(percent: Float): Float = boundary {
    var endIndex = -1
    val n        = timeline.length
    if (percent >= timeline(n - 1)) break(scaling(n - 1))
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
