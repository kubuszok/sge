/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/values/NumericValue.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes (2026-03-03):
 * - Json.Serializable write/read methods intentionally omitted
 * - All public methods ported: getValue, setValue, load
 * - Status: pass
 */

package sge
package graphics
package g3d
package particles
package values

/** A value which contains a single float variable.
  * @author
  *   Inferno
  */
class NumericValue extends ParticleValue {
  private var value: Float = 0f

  def getValue(): Float =
    value

  def setValue(value: Float): Unit =
    this.value = value

  def load(value: NumericValue): Unit = {
    super.load(value)
    this.value = value.value
  }
}
