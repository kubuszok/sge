/*
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: SGE-original opaque type wrapping Float; replaces raw float pitch parameters
 *     in Sound; validated range [0.5, 2.0]
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 30
 * Covenant-baseline-methods: Pitch,max,min,normal,parse,toFloat,unsafeMake
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package audio

opaque type Pitch = Float
object Pitch {

  given utils.MkArray[Pitch] = utils.MkArray.mkFloat.asInstanceOf[utils.MkArray[Pitch]]

  def parse(value: Float): Either[String, Pitch] =
    if (value < 0.5 || value > 2.0) Left(s"Pitch must be between 0.5 and 2.0, got $value")
    else Right(value)

  def unsafeMake(value: Float): Pitch = value

  val min:    Pitch = 0.5f
  val normal: Pitch = 1.0f
  val max:    Pitch = 2.0f

  extension (pitch: Pitch) {
    inline def toFloat: Float = pitch
  }
}
