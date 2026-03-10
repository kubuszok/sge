/*
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   SGE-original opaque type, no LibGDX counterpart
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * AUDIT: N/A (SGE-original opaque type, no LibGDX counterpart)
 */
package sge
package math

opaque type Radians = Float
object Radians {
  def apply(value: Float): Radians = value

  given utils.MkArray[Radians] = utils.MkArray.mkFloat.asInstanceOf[utils.MkArray[Radians]]

  inline def Pi:     Radians = 3.1415927f
  inline def TwoPi:  Radians = 6.2831855f
  inline def HalfPi: Radians = 1.5707964f
  val zero:          Radians = 0f

  extension (r: Radians) {
    inline def toFloat:   Float   = r
    inline def toDegrees: Degrees = Degrees(r * MathUtils.radiansToDegrees)
    def sin:              Float   = MathUtils.Sin.table((r * MathUtils.radToIndex).toInt & MathUtils.SIN_MASK)
    def cos:              Float   =
      MathUtils.Sin.table(((r + MathUtils.HALF_PI) * MathUtils.radToIndex).toInt & MathUtils.SIN_MASK)
    @annotation.targetName("plus")
    def +(other: Radians): Radians = r + other
    @annotation.targetName("minus")
    def -(other: Radians): Radians = r - other
    @annotation.targetName("times")
    def *(scalar: Float): Radians = r * scalar
    @annotation.targetName("negate")
    def unary_- : Radians = -r
  }
}
