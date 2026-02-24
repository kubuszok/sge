/*
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils

opaque type Seconds = Float
object Seconds {
  def apply(value: Float): Seconds = value
  val zero:                Seconds = 0f

  extension (s: Seconds) {
    inline def toFloat:     Float   = s
    inline def toMillis:    Long    = (s * 1000).toLong
    def +(other:  Seconds): Seconds = s + other
    def -(other:  Seconds): Seconds = s - other
    def *(scalar: Float):   Seconds = s * scalar
    def >(other:  Seconds): Boolean = s > other
    def <(other:  Seconds): Boolean = s < other
  }
}
