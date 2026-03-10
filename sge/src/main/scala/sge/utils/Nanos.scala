/*
 * Migration notes:
 *   SGE-original file, no LibGDX counterpart
 *   Convention: opaque type wrapping Long; replaces raw Long nanosecond timestamps
 *   Idiom: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

opaque type Nanos = Long
object Nanos {
  def apply(value: Long): Nanos = value
  val zero:               Nanos = 0L

  given MkArray[Nanos] = MkArray.mkLong.asInstanceOf[MkArray[Nanos]]

  private val nanosPerMilli: Long = 1000000L

  extension (n: Nanos) {
    inline def toLong:    Long   = n
    inline def toMillis:  Millis = Millis(n / nanosPerMilli)
    def +(other:  Nanos): Nanos  = n + other
    def -(other:  Nanos): Nanos  = n - other
    def *(scalar: Long):  Nanos  = n * scalar
    def /(other:  Nanos): Long   = n / other
    @scala.annotation.targetName("divScalar")
    def /(scalar: Long):  Nanos   = n / scalar
    def %(other:  Nanos): Nanos   = n % other
    def >(other:  Nanos): Boolean = n > other
    def <(other:  Nanos): Boolean = n < other
    def >=(other: Nanos): Boolean = n >= other
    def <=(other: Nanos): Boolean = n <= other
    def ==(other: Nanos): Boolean = n == other
    inline def toFloat:   Float   = n.toFloat
  }
}
