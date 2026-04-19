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
 * Covenant-baseline-loc: 26
 * Covenant-baseline-methods: Epsilon,apply,toFloat
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
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
