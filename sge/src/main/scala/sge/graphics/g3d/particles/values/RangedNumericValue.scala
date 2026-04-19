/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/values/RangedNumericValue.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (2026-03-03):
 * - Json.Serializable write/read methods intentionally omitted
 * - All public methods ported: newLowValue, setLow (x2), lowMin/lowMax (public vars), load
 * - Fixes (2026-03-04): getLowMin/setLowMin/getLowMax/setLowMax → public vars
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 50
 * Covenant-baseline-methods: RangedNumericValue,load,lowMax,lowMin,newLowValue,setLow
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/particles/values/RangedNumericValue.java
 * Covenant-verified: 2026-04-19
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
  var lowMin: Float = 0f
  var lowMax: Float = 0f

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

  def load(value: RangedNumericValue): Unit = {
    super.load(value)
    lowMax = value.lowMax
    lowMin = value.lowMin
  }
}
