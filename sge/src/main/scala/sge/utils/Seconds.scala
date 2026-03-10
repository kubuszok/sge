/*
 * Migration notes:
 *   SGE-original file, no LibGDX counterpart
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

opaque type Seconds = Float
object Seconds {
  def apply(value: Float): Seconds = value
  val zero:                Seconds = 0f

  given MkArray[Seconds] = MkArray.mkFloat.asInstanceOf[MkArray[Seconds]]

  extension (s: Seconds) {
    inline def toFloat:     Float   = s
    inline def toMillis:    Long    = (s * 1000).toLong
    def +(other:  Seconds): Seconds = s + other
    def -(other:  Seconds): Seconds = s - other
    def *(scalar: Float):   Seconds = s * scalar
    def /(other:  Seconds): Float   = s / other
    @scala.annotation.targetName("divScalar")
    def /(scalar: Float):   Seconds = s / scalar
    def %(other:  Seconds): Seconds = s % other
    def unary_-           : Seconds = -s
    def >(other:  Seconds): Boolean = s > other
    def >=(other: Seconds): Boolean = s >= other
    def <(other:  Seconds): Boolean = s < other
    def <=(other: Seconds): Boolean = s <= other
    inline def abs:         Seconds = Math.abs(s)
  }
}
