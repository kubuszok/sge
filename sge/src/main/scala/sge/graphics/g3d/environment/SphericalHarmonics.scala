/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/environment/SphericalHarmonics.java
 * Original authors: (see AUTHORS file)
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Audit: pass (2026-03-03)
 *   - All 4 methods ported: set(float[]), set(AmbientCubemap), set(Color), set(r,g,b)
 *   - Constructors: no-arg + array validated via companion apply
 *   - coeff array commented out (unused in upstream LibGDX)
 *   - clamp method omitted (unused in upstream LibGDX)
 *   - GdxRuntimeException → SgeError.InvalidInput
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 72
 * Covenant-baseline-methods: NUM_VALUES,SphericalHarmonics,apply,i,idx,set,this
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/environment/SphericalHarmonics.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package graphics
package g3d
package environment

import sge.utils.SgeError

class SphericalHarmonics private[environment] (val data: Array[Float]) {

  def this() =
    this(new Array[Float](SphericalHarmonics.NUM_VALUES))

  def set(values: Array[Float]): SphericalHarmonics = {
    var i = 0
    while (i < data.length) {
      data(i) = values(i)
      i += 1
    }
    this
  }

  def set(other: AmbientCubemap): SphericalHarmonics =
    set(other.data)

  def set(color: Color): SphericalHarmonics =
    set(color.r, color.g, color.b)

  def set(r: Float, g: Float, b: Float): SphericalHarmonics = {
    var idx = 0
    while (idx < data.length) {
      data(idx) = r
      idx += 1
      data(idx) = g
      idx += 1
      data(idx) = b
      idx += 1
    }
    this
  }
}

object SphericalHarmonics {
  private val NUM_VALUES = 9 * 3

  // <kalle_h> last term is no x*x * y*y but x*x - y*y
  // private val coeff: Array[Float] = Array(0.282095f, 0.488603f, 0.488603f, 0.488603f, 1.092548f, 1.092548f, 1.092548f, 0.315392f, 0.546274f) // unused in upstream LibGDX

  /** Creates a SphericalHarmonics from an array of float values.
    * @throws SgeError.InvalidInput
    *   if the array size is not 27 (9 * 3)
    */
  def apply(copyFrom: Array[Float]): SphericalHarmonics = {
    if (copyFrom.length != NUM_VALUES) throw SgeError.InvalidInput("Incorrect array size")
    SphericalHarmonics(copyFrom.clone())
  }
}
