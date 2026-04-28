/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/values/NumericValue.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (2026-03-03):
 * - Json.Serializable write/read methods intentionally omitted
 * - All public methods ported: value (public var), load
 * - Fixes (2026-03-04): getValue()/setValue() → public var value
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 33
 * Covenant-baseline-methods: NumericValue,load,value
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/particles/values/NumericValue.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: f087ba76cac7ff2cc4ded91efc009f52bf0987c2
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
  var value: Float = 0f

  def load(numericValue: NumericValue): Unit = {
    super.load(numericValue)
    this.value = numericValue.value
  }
}
