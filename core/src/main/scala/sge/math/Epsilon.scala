package sge
package math

opaque type Epsilon = Float
object Epsilon {
  @scala.annotation.targetName("summon")
  inline def apply()(using epsilon: Epsilon): Float = epsilon

  inline def apply(value: Float): Epsilon = value

  extension (epsilon: Epsilon) {
    inline def toFloat: Float = epsilon
  }
}
