/*
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   SGE-original opaque type, no LibGDX counterpart
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * AUDIT: N/A (SGE-original opaque type, no LibGDX counterpart)
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 40
 * Covenant-baseline-methods: Degrees,Full,Half,Quarter,apply,cos,sin,toFloat,toRadians,unary_,zero
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package math

import lowlevel.math.MathUtils

opaque type Degrees = Float
object Degrees {
  def apply(value: Float): Degrees = value

  given lowlevel.MkArray.OfFloats[Degrees] = lowlevel.MkArray.ofFloatAs[Degrees]

  inline def Full:    Degrees = 360f
  inline def Half:    Degrees = 180f
  inline def Quarter: Degrees = 90f
  val zero:           Degrees = 0f

  extension (d: Degrees) {
    inline def toFloat:   Float   = d
    inline def toRadians: Radians = Radians(d * MathUtils.degreesToRadians)
    def sin:              Float   = MathUtils.sinDeg(d)
    def cos:              Float   = MathUtils.cosDeg(d)
    @annotation.targetName("plus")
    def +(other: Degrees): Degrees = d + other
    @annotation.targetName("minus")
    def -(other: Degrees): Degrees = d - other
    @annotation.targetName("times")
    def *(scalar: Float): Degrees = d * scalar
    @annotation.targetName("negate")
    def unary_- : Degrees = -d
  }
}
