/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/values/GradientColorValue.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (2026-03-03):
 * - Json.Serializable write/read methods intentionally omitted
 * - All public methods ported: getTimeline, setTimeline, getColors, setColors, getColor (x2), load
 * - static temp field moved to companion object (correct pattern)
 * - getColor uses boundary/break instead of return (correct pattern)
 * - Status: pass
 */

package sge
package graphics
package g3d
package particles
package values

import scala.util.boundary
import scala.util.boundary.break

/** Defines a variation of red, green and blue on a given time line.
  * @author
  *   Inferno
  */
class GradientColorValue extends ParticleValue {

  private var colors: Array[Float] = Array(1f, 1f, 1f)
  var timeline:       Array[Float] = Array(0f)

  def getTimeline(): Array[Float] =
    timeline

  def setTimeline(timeline: Array[Float]): Unit =
    this.timeline = timeline

  def getColors(): Array[Float] =
    colors

  def setColors(colors: Array[Float]): Unit =
    this.colors = colors

  def getColor(percent: Float): Array[Float] = {
    getColor(percent, GradientColorValue.temp, 0)
    GradientColorValue.temp
  }

  def getColor(percent: Float, out: Array[Float], index: Int): Unit = boundary {
    var startIndex = 0
    var endIndex   = -1
    val timeline   = this.timeline
    val n          = timeline.length
    boundary[Unit] {
      var i = 1
      while (i < n) {
        val t = timeline(i)
        if (t > percent) {
          endIndex = i
          break(())
        }
        startIndex = i
        i += 1
      }
    }
    val startTime = timeline(startIndex)
    startIndex *= 3
    val r1 = colors(startIndex)
    val g1 = colors(startIndex + 1)
    val b1 = colors(startIndex + 2)
    if (endIndex == -1) {
      out(index) = r1
      out(index + 1) = g1
      out(index + 2) = b1
      break(())
    }
    val factor = (percent - startTime) / (timeline(endIndex) - startTime)
    endIndex *= 3
    out(index) = r1 + (colors(endIndex) - r1) * factor
    out(index + 1) = g1 + (colors(endIndex + 1) - g1) * factor
    out(index + 2) = b1 + (colors(endIndex + 2) - b1) * factor
  }

  def load(value: GradientColorValue): Unit = {
    super.load(value)
    colors = new Array[Float](value.colors.length)
    Array.copy(value.colors, 0, colors, 0, colors.length)
    timeline = new Array[Float](value.timeline.length)
    Array.copy(value.timeline, 0, timeline, 0, timeline.length)
  }
}

object GradientColorValue {
  private val temp: Array[Float] = new Array[Float](3)
}
