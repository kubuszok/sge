/*
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: SGE-original opaque type wrapping Float; replaces raw float pan parameters
 *     in Music, Sound, AudioDevice; validated range [-1, 1]
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 30
 * Covenant-baseline-methods: Pan,center,maxLeft,maxRight,parse,toFloat,unsafeMake
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package audio

opaque type Pan = Float
object Pan {

  given utils.MkArray[Pan] = utils.MkArray.mkFloat.asInstanceOf[utils.MkArray[Pan]]

  def parse(value: Float): Either[String, Pan] =
    if (value < -1.0 || value > 1.0) Left(s"Pan must be between -1 and 1, got $value")
    else Right(value)

  def unsafeMake(value: Float): Pan = value

  val maxLeft:  Pan = -1.0f
  val center:   Pan = 0.0f
  val maxRight: Pan = 1.0f

  extension (pan: Pan) {
    inline def toFloat: Float = pan
  }
}
