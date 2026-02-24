/*
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package math

opaque type Radians = Float
object Radians {
  def apply(value: Float): Radians = value

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
    def +(other:  Radians): Radians = r + other
    def -(other:  Radians): Radians = r - other
    def *(scalar: Float):   Radians = r * scalar
    def unary_-           : Radians = -r
  }
}
