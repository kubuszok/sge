/*
 * Migration notes:
 *   SGE-original file, no LibGDX counterpart
 *   Convention: opaque type wrapping Long; replaces raw Long millisecond timestamps
 *   Idiom: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

opaque type Millis = Long
object Millis {
  def apply(value: Long): Millis = value
  val zero:               Millis = 0L

  private val nanosPerMilli: Long = 1000000L

  extension (m: Millis) {
    inline def toLong:     Long   = m
    inline def toNanos:    Nanos  = Nanos(m * nanosPerMilli)
    def +(other:  Millis): Millis = m + other
    def -(other:  Millis): Millis = m - other
    def *(scalar: Long):   Millis = m * scalar
    def /(other:  Millis): Long   = m / other
    @scala.annotation.targetName("divScalar")
    def /(scalar:  Long):   Millis  = m / scalar
    def %(other:   Millis): Millis  = m % other
    def >(other:   Millis): Boolean = m > other
    def <(other:   Millis): Boolean = m < other
    def >=(other:  Millis): Boolean = m >= other
    def <=(other:  Millis): Boolean = m <= other
    def ==(other:  Millis): Boolean = m == other
    def min(other: Millis): Millis  = scala.math.min(m, other)
    def max(other: Millis): Millis  = scala.math.max(m, other)
    inline def toInt:       Int     = m.toInt
  }
}
