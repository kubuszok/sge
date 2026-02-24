/*
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package audio

opaque type Position = Float
object Position {

  def parse(seconds: Float): Either[String, Position] =
    if (seconds < 0.0) Left(s"Position must be greater than 0, got $seconds")
    else Right(seconds)

  def unsafeMake(seconds: Float): Position = seconds

  extension (position: Position) {
    inline def toFloatSeconds: Float = position
  }
}
