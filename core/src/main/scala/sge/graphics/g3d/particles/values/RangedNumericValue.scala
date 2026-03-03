/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/values/RangedNumericValue.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes (2026-03-03):
 * - Json.Serializable write/read methods intentionally omitted
 * - All public methods ported: newLowValue, setLow (x2), getLowMin, setLowMin, getLowMax, setLowMax, load
 * - Status: pass
 * TODO: Java-style getters/setters — getLowMin/setLowMin, getLowMax/setLowMax
 */

package sge
package graphics
package g3d
package particles
package values

import sge.math.MathUtils

/** A value which has a defined minimum and maximum bounds.
  * @author
  *   Inferno
  */
class RangedNumericValue extends ParticleValue {
  private var lowMin: Float = 0f
  private var lowMax: Float = 0f

  def newLowValue(): Float =
    lowMin + (lowMax - lowMin) * MathUtils.random()

  def setLow(value: Float): Unit = {
    lowMin = value
    lowMax = value
  }

  def setLow(min: Float, max: Float): Unit = {
    lowMin = min
    lowMax = max
  }

  def getLowMin(): Float =
    lowMin

  def setLowMin(lowMin: Float): Unit =
    this.lowMin = lowMin

  def getLowMax(): Float =
    lowMax

  def setLowMax(lowMax: Float): Unit =
    this.lowMax = lowMax

  def load(value: RangedNumericValue): Unit = {
    super.load(value)
    lowMax = value.lowMax
    lowMin = value.lowMin
  }
}
