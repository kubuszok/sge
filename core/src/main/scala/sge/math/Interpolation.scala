/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/Interpolation.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package math

/** Takes a linear value in the range of 0-1 and outputs a (usually) non-linear, interpolated value.
  * @author
  *   Nathan Sweet (original implementation)
  */
abstract class Interpolation {

  /** @param a Alpha value between 0 and 1. */
  def apply(a: Float): Float

  /** @param a Alpha value between 0 and 1. */
  def apply(start: Float, end: Float, a: Float): Float =
    start + (end - start) * apply(a)
}

object Interpolation {
  val linear: Interpolation = new Interpolation {
    def apply(a: Float): Float = a
  }

  /** Aka "smoothstep". */
  val smooth: Interpolation = new Interpolation {
    def apply(a: Float): Float = a * a * (3 - 2 * a)
  }

  val smooth2: Interpolation = new Interpolation {
    def apply(a: Float): Float = {
      val smoothed = a * a * (3 - 2 * a)
      smoothed * smoothed * (3 - 2 * smoothed)
    }
  }

  /** By Ken Perlin. */
  val smoother: Interpolation = new Interpolation {
    def apply(a: Float): Float = a * a * a * (a * (a * 6 - 15) + 10)
  }
  val fade: Interpolation = smoother

  val pow2 = new Pow(2)

  /** Slow, then fast. */
  val pow2In = new PowIn(2)
  val slowFast: Interpolation = pow2In

  /** Fast, then slow. */
  val pow2Out = new PowOut(2)
  val fastSlow:      Interpolation = pow2Out
  val pow2InInverse: Interpolation = new Interpolation {
    def apply(a: Float): Float =
      if (a < MathUtils.FLOAT_ROUNDING_ERROR) 0f
      else Math.sqrt(a).toFloat
  }
  val pow2OutInverse: Interpolation = new Interpolation {
    def apply(a: Float): Float =
      if (a < MathUtils.FLOAT_ROUNDING_ERROR) 0f
      else if (a > 1) 1f
      else 1 - Math.sqrt(-(a - 1)).toFloat
  }

  val pow3    = new Pow(3)
  val pow3In  = new PowIn(3)
  val pow3Out = new PowOut(3)
  val pow3InInverse: Interpolation = new Interpolation {
    def apply(a: Float): Float = Math.cbrt(a).toFloat
  }
  val pow3OutInverse: Interpolation = new Interpolation {
    def apply(a: Float): Float = 1 - Math.cbrt(-(a - 1)).toFloat
  }

  val pow4    = new Pow(4)
  val pow4In  = new PowIn(4)
  val pow4Out = new PowOut(4)

  val pow5    = new Pow(5)
  val pow5In  = new PowIn(5)
  val pow5Out = new PowOut(5)

  val sine: Interpolation = new Interpolation {
    def apply(a: Float): Float = (1 - MathUtils.cos(a * MathUtils.PI)) / 2
  }

  val sineIn: Interpolation = new Interpolation {
    def apply(a: Float): Float = 1 - MathUtils.cos(a * MathUtils.HALF_PI)
  }

  val sineOut: Interpolation = new Interpolation {
    def apply(a: Float): Float = MathUtils.sin(a * MathUtils.HALF_PI)
  }

  val exp10    = new Exp(2, 10)
  val exp10In  = new ExpIn(2, 10)
  val exp10Out = new ExpOut(2, 10)

  val exp5    = new Exp(2, 5)
  val exp5In  = new ExpIn(2, 5)
  val exp5Out = new ExpOut(2, 5)

  val circle: Interpolation = new Interpolation {
    def apply(a: Float): Float =
      if (a <= 0.5f) {
        val a2 = a * 2
        (1 - Math.sqrt(1 - a2 * a2).toFloat) / 2
      } else {
        val a2 = (a - 1) * 2
        (Math.sqrt(1 - a2 * a2).toFloat + 1) / 2
      }
  }

  val circleIn: Interpolation = new Interpolation {
    def apply(a: Float): Float = 1 - Math.sqrt(1 - a * a).toFloat
  }

  val circleOut: Interpolation = new Interpolation {
    def apply(a: Float): Float = {
      val a2 = a - 1
      Math.sqrt(1 - a2 * a2).toFloat
    }
  }

  val elastic    = new Elastic(2, 10, 7, 1)
  val elasticIn  = new ElasticIn(2, 10, 6, 1)
  val elasticOut = new ElasticOut(2, 10, 7, 1)

  val swing    = new Swing(1.5f)
  val swingIn  = new SwingIn(2f)
  val swingOut = new SwingOut(2f)

  val bounce    = new Bounce(4)
  val bounceIn  = new BounceIn(4)
  val bounceOut = new BounceOut(4)

  class Pow(val power: Int) extends Interpolation {
    def apply(a: Float): Float =
      if (a <= 0.5f) Math.pow(a * 2, power).toFloat / 2
      else Math.pow((a - 1) * 2, power).toFloat / (if (power % 2 == 0) -2 else 2) + 1
  }

  class PowIn(power: Int) extends Pow(power) {
    override def apply(a: Float): Float = Math.pow(a, power).toFloat
  }

  class PowOut(power: Int) extends Pow(power) {
    override def apply(a: Float): Float =
      Math.pow(a - 1, power).toFloat * (if (power % 2 == 0) -1 else 1) + 1
  }

  class Exp(val value: Float, val power: Float) extends Interpolation {
    val min:   Float = Math.pow(value, -power).toFloat
    val scale: Float = 1 / (1 - min)

    def apply(a: Float): Float =
      if (a <= 0.5f) (Math.pow(value, power * (a * 2 - 1)).toFloat - min) * scale / 2
      else (2 - (Math.pow(value, -power * (a * 2 - 1)).toFloat - min) * scale) / 2
  }

  class ExpIn(value: Float, power: Float) extends Exp(value, power) {
    override def apply(a: Float): Float =
      (Math.pow(value, power * (a - 1)).toFloat - min) * scale
  }

  class ExpOut(value: Float, power: Float) extends Exp(value, power) {
    override def apply(a: Float): Float =
      1 - (Math.pow(value, -power * a).toFloat - min) * scale
  }

  class Elastic(val value: Float, val power: Float, bounces: Int, val scale: Float) extends Interpolation {
    val bouncesFinal: Float = bounces * MathUtils.PI * (if (bounces % 2 == 0) 1 else -1)

    def apply(a: Float): Float =
      if (a <= 0.5f) {
        val a2 = a * 2
        Math.pow(value, power * (a2 - 1)).toFloat * MathUtils.sin(a2 * bouncesFinal) * scale / 2
      } else {
        val a2 = (1 - a) * 2
        1 - Math.pow(value, power * (a2 - 1)).toFloat * MathUtils.sin(a2 * bouncesFinal) * scale / 2
      }
  }

  class ElasticIn(value: Float, power: Float, bounces: Int, scale: Float) extends Elastic(value, power, bounces, scale) {
    override def apply(a: Float): Float =
      if (a >= 0.99f) 1f
      else Math.pow(value, power * (a - 1)).toFloat * MathUtils.sin(a * bouncesFinal) * scale
  }

  class ElasticOut(value: Float, power: Float, bounces: Int, scale: Float) extends Elastic(value, power, bounces, scale) {
    override def apply(a: Float): Float =
      if (a == 0) 0f
      else {
        val a2 = 1 - a
        1 - Math.pow(value, power * (a2 - 1)).toFloat * MathUtils.sin(a2 * bouncesFinal) * scale
      }
  }

  class Bounce(widths: Array[Float], heights: Array[Float]) extends BounceOut(widths, heights) {
    def this(bounces: Int) = this(Array.empty, Array.empty) // Will be set in BounceOut constructor

    private def out(a: Float): Float = {
      val test = a + widths(0) / 2
      if (test < widths(0)) test / (widths(0) / 2) - 1
      else super.apply(a)
    }

    override def apply(a: Float): Float =
      if (a <= 0.5f) (1 - out(1 - a * 2)) / 2
      else out(a * 2 - 1) / 2 + 0.5f
  }

  class BounceOut(val widths: Array[Float], val heights: Array[Float]) extends Interpolation {
    def this(bounces: Int) = {
      this(
        if (bounces < 2 || bounces > 5) throw new IllegalArgumentException(s"bounces cannot be < 2 or > 5: $bounces")
        else {
          val w = new Array[Float](bounces)
          val h = new Array[Float](bounces)
          h(0) = 1
          bounces match {
            case 2 =>
              w(0) = 0.6f
              w(1) = 0.4f
              h(1) = 0.33f
            case 3 =>
              w(0) = 0.4f
              w(1) = 0.4f
              w(2) = 0.2f
              h(1) = 0.33f
              h(2) = 0.1f
            case 4 =>
              w(0) = 0.34f
              w(1) = 0.34f
              w(2) = 0.2f
              w(3) = 0.15f
              h(1) = 0.26f
              h(2) = 0.11f
              h(3) = 0.03f
            case 5 =>
              w(0) = 0.3f
              w(1) = 0.3f
              w(2) = 0.2f
              w(3) = 0.1f
              w(4) = 0.1f
              h(1) = 0.45f
              h(2) = 0.3f
              h(3) = 0.15f
              h(4) = 0.06f
          }
          w(0) *= 2
          w
        }, {
          val h = new Array[Float](bounces)
          h(0) = 1
          bounces match {
            case 2 => h(1) = 0.33f
            case 3 => h(1) = 0.33f; h(2) = 0.1f
            case 4 => h(1) = 0.26f; h(2) = 0.11f; h(3) = 0.03f
            case 5 => h(1) = 0.45f; h(2) = 0.3f; h(3) = 0.15f; h(4) = 0.06f
          }
          h
        }
      )
    }

    def apply(a: Float): Float =
      if (a == 1) 1f
      else
        scala.util.boundary {
          var aVar   = a + widths(0) / 2
          var width  = 0f
          var height = 0f

          for (i <- widths.indices) {
            width = widths(i)
            if (aVar <= width) {
              height = heights(i)
              scala.util.boundary.break {
                aVar /= width
                val z = 4 / width * height * aVar
                1 - (z - z * aVar) * width
              }
            }
            aVar -= width
          }
          // Fallback (shouldn't reach here)
          aVar /= width
          val z = 4 / width * height * aVar
          1 - (z - z * aVar) * width
        }
  }

  class BounceIn(widths: Array[Float], heights: Array[Float]) extends BounceOut(widths, heights) {
    def this(bounces: Int) = this(Array.empty, Array.empty) // Will be handled by BounceOut constructor

    override def apply(a: Float): Float = 1 - super.apply(1 - a)
  }

  class Swing(scale: Float) extends Interpolation {
    private val scaleValue = scale * 2

    def apply(a: Float): Float =
      if (a <= 0.5f) {
        val a2 = a * 2
        a2 * a2 * ((scaleValue + 1) * a2 - scaleValue) / 2
      } else {
        val a2 = (a - 1) * 2
        a2 * a2 * ((scaleValue + 1) * a2 + scaleValue) / 2 + 1
      }
  }

  class SwingOut(val scale: Float) extends Interpolation {
    def apply(a: Float): Float = {
      val a2 = a - 1
      a2 * a2 * ((scale + 1) * a2 + scale) + 1
    }
  }

  class SwingIn(val scale: Float) extends Interpolation {
    def apply(a: Float): Float =
      a * a * ((scale + 1) * a - scale)
  }
}
