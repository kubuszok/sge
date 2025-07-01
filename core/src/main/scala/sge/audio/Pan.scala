package sge
package audio

opaque type Pan = Float
object Pan {

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
