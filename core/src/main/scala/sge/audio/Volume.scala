/*
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: SGE-original opaque type wrapping Float; replaces raw float volume parameters
 *     in Music, Sound, AudioDevice; validated range [0, 1]
 *   Audited: 2026-03-03
 */
package sge
package audio

opaque type Volume = Float
object Volume {

  def parse(value: Float): Either[String, Volume] =
    if (value < 0 || value > 1) Left(s"Volume must be between 0 and 1, got $value")
    else Right(value)

  def unsafeMake(value: Float): Volume = value

  val min: Volume = 0
  val max: Volume = 1

  extension (volume: Volume) {
    inline def toFloat: Float = volume
  }
}
