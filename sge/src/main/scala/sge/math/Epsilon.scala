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

opaque type Epsilon = Float
object Epsilon {
  @scala.annotation.targetName("summon")
  inline def apply()(using epsilon: Epsilon): Float = epsilon

  inline def apply(value: Float): Epsilon = value

  given utils.MkArray[Epsilon] = utils.MkArray.mkFloat.asInstanceOf[utils.MkArray[Epsilon]]

  extension (epsilon: Epsilon) {
    inline def toFloat: Float = epsilon
  }
}
