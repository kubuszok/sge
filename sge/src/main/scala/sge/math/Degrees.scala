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

opaque type Degrees = Float
object Degrees {
  def apply(value: Float): Degrees = value

  given utils.MkArray[Degrees] = utils.MkArray.mkFloat.asInstanceOf[utils.MkArray[Degrees]]

  inline def Full:    Degrees = 360f
  inline def Half:    Degrees = 180f
  inline def Quarter: Degrees = 90f
  val zero:           Degrees = 0f

  extension (d: Degrees) {
    inline def toFloat:   Float   = d
    inline def toRadians: Radians = Radians(d * MathUtils.degreesToRadians)
    def sin:              Float   = MathUtils.Sin.table((d * MathUtils.degToIndex).toInt & MathUtils.SIN_MASK)
    def cos:              Float   =
      MathUtils.Sin.table(((d + 90) * MathUtils.degToIndex).toInt & MathUtils.SIN_MASK)
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
