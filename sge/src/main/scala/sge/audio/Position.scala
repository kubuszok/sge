/*
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: SGE-original opaque type wrapping Float; replaces raw float position (seconds)
 *     parameters in Music; validated range [0, +inf)
 *   Audited: 2026-03-03
 */
package sge
package audio

opaque type Position = Float
object Position {

  given utils.MkArray[Position] = utils.MkArray.mkFloat.asInstanceOf[utils.MkArray[Position]]

  def parse(seconds: Float): Either[String, Position] =
    if (seconds < 0.0) Left(s"Position must be greater than 0, got $seconds")
    else Right(seconds)

  def unsafeMake(seconds: Float): Position = seconds

  extension (position: Position) {
    inline def toFloatSeconds: Float = position
  }
}
