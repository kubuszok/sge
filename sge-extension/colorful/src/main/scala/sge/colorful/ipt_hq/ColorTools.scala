/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful
package ipt_hq

import scala.util.boundary
import scala.util.boundary.break

import sge.colorful.FloatColors
import sge.graphics.Color
import sge.math.MathUtils

/** Contains code for manipulating colors as `int` and packed `float` values in the IPT color space. IPT has more perceptually-uniform handling of hue than some other color spaces, like YCwCm, and
  * this version goes further than the IPT package by performing gamma correction and all the complex exponential adjustments to various components that the original IPT paper used.
  */
object ColorTools {

  /** Used when converting from RGB to IPT, as an intermediate step.
    * @param component
    *   one of the LMS channels to be converted to LMS Prime
    * @return
    *   an LMS Prime channel value, which can be converted to IPT
    */
  private def forwardTransform(component: Float): Float =
    Math.pow(component, 0.43f).toFloat

  /** Used when converting from IPT to RGB, as an intermediate step.
    * @param component
    *   one of the LMS Prime channels to be converted to LMS
    * @return
    *   an LMS channel value, which can be converted to RGB
    */
  private def reverseTransform(component: Float): Float =
    Math.copySign(Math.pow(Math.abs(component), 2.3256f).toFloat, component)

  /** Used when given non-linear sRGB inputs to make them linear, approximating with gamma 2.0.
    * @param component
    *   any non-linear channel of a color, to be made linear
    * @return
    *   a linear version of component
    */
  private def forwardGamma(component: Float): Float =
    component * component

  /** Used to return from a linear, gamma-corrected input to an sRGB, non-linear output, using gamma 2.0.
    * @param component
    *   a linear channel of a color, to be made non-linear
    * @return
    *   a non-linear version of component
    */
  private def reverseGamma(component: Float): Float =
    Math.sqrt(component).toFloat

  /** Gets a packed float representation of a color given as 4 float components: I (intensity/lightness), P (protan), T (tritan), and alpha.
    */
  def ipt(intens: Float, protan: Float, tritan: Float, alpha: Float): Float =
    java.lang.Float.intBitsToFloat(
      ((alpha * 255).toInt << 24 & 0xfe000000) | ((tritan * 255).toInt << 16 & 0xff0000)
        | ((protan * 255).toInt << 8 & 0xff00) | ((intens * 255).toInt & 0xff)
    )

  /** Converts a packed float IPT_HQ color to an RGBA8888 int. */
  def toRGBA8888(packed: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val l       = reverseTransform(i + 0.097569f * p + 0.205226f * t)
    val m       = reverseTransform(i + -0.11388f * p + 0.133217f * t)
    val s       = reverseTransform(i + 0.032615f * p + -0.67689f * t)
    val r       = (reverseGamma(Math.min(Math.max(5.432622f * l + -4.67910f * m + 0.246257f * s, 0f), 1f)) * 255.999f).toInt
    val g       = (reverseGamma(Math.min(Math.max(-1.10517f * l + 2.311198f * m + -0.20588f * s, 0f), 1f)) * 255.999f).toInt
    val b       = (reverseGamma(Math.min(Math.max(0.028104f * l + -0.19466f * m + 1.166325f * s, 0f), 1f)) * 255.999f).toInt
    r << 24 | g << 16 | b << 8 | (decoded & 0xfe000000) >>> 24 | decoded >>> 31
  }

  /** Converts a packed float IPT_HQ color to a packed float in RGBA format. */
  def toRGBA(packed: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val l       = reverseTransform(i + 0.097569f * p + 0.205226f * t)
    val m       = reverseTransform(i + -0.11388f * p + 0.133217f * t)
    val s       = reverseTransform(i + 0.032615f * p + -0.67689f * t)
    val r       = (reverseGamma(Math.min(Math.max(5.432622f * l + -4.67910f * m + 0.246257f * s, 0f), 1f)) * 255.999f).toInt
    val g       = (reverseGamma(Math.min(Math.max(-1.10517f * l + 2.311198f * m + -0.20588f * s, 0f), 1f)) * 255.999f).toInt
    val b       = (reverseGamma(Math.min(Math.max(0.028104f * l + -0.19466f * m + 1.166325f * s, 0f), 1f)) * 255.999f).toInt
    java.lang.Float.intBitsToFloat(r | g << 8 | b << 16 | (decoded & 0xfe000000))
  }

  /** Writes an IPT_HQ packed float color into an RGBA8888 Color. */
  def toColor(editing: Color, packed: Float): Color = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val l       = reverseTransform(i + 0.097569f * p + 0.205226f * t)
    val m       = reverseTransform(i + -0.11388f * p + 0.133217f * t)
    val s       = reverseTransform(i + 0.032615f * p + -0.67689f * t)
    editing.r = reverseGamma(Math.min(Math.max(5.432622f * l + -4.67910f * m + 0.246257f * s, 0f), 1f))
    editing.g = reverseGamma(Math.min(Math.max(-1.10517f * l + 2.311198f * m + -0.20588f * s, 0f), 1f))
    editing.b = reverseGamma(Math.min(Math.max(0.028104f * l + -0.19466f * m + 1.166325f * s, 0f), 1f))
    // 0x1.020408p-7f is 1/127 as a float = 0.007874016f
    editing.a = (decoded >>> 25) * 0.007874016f
    editing.clamp()
  }

  /** Writes an IPT_HQ-format packed float color into an IPT_HQ-format Color called `editing`. This is mostly useful if the rest of your application expects colors in IPT_HQ format.
    *
    * Internally, this simply calls `Color.abgr8888ToColor` and returns the edited Color.
    */
  def toIPTColor(editing: Color, iptColor: Float): Color = {
    Color.abgr8888ToColor(editing, iptColor)
    editing
  }

  /** Takes an RGBA8888 int and converts to a packed float in IPT_HQ format. */
  def fromRGBA8888(rgba: Int): Float = {
    // 0x1.010101010101p-8f = 0.003921569f (approximately 1/255)
    val r = forwardGamma((rgba >>> 24) * 0.003921569f)
    val g = forwardGamma((rgba >>> 16 & 0xff) * 0.003921569f)
    val b = forwardGamma((rgba >>> 8 & 0xff) * 0.003921569f)
    val l = forwardTransform(0.313921f * r + 0.639468f * g + 0.0465970f * b)
    val m = forwardTransform(0.151693f * r + 0.748209f * g + 0.1000044f * b)
    val s = forwardTransform(0.017753f * r + 0.109468f * g + 0.8729690f * b)
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max(((0.4000f * l + 0.4000f * m + 0.2000f * s) * 255.999f).toInt, 0), 255)
        | Math.min(Math.max(((2.2275f * l - 2.4255f * m + 0.1980f * s + 0.5f) * 255.999f).toInt, 0), 255) << 8
        | Math.min(Math.max(((0.4028f * l + 0.1786f * m - 0.5814f * s + 0.5f) * 255.999f).toInt, 0), 255) << 16
        | (rgba & 0xfe) << 24
    )
  }

  /** Takes a packed float RGBA color and converts to a packed float in IPT_HQ format. */
  def fromRGBA(packed: Float): Float = {
    val abgr = java.lang.Float.floatToRawIntBits(packed)
    // 0x1.010101010101p-8f = 0.003921569f
    val r = forwardGamma((abgr & 0xff) * 0.003921569f)
    val g = forwardGamma((abgr >>> 8 & 0xff) * 0.003921569f)
    val b = forwardGamma((abgr >>> 16 & 0xff) * 0.003921569f)
    val l = forwardTransform(0.313921f * r + 0.639468f * g + 0.0465970f * b)
    val m = forwardTransform(0.151693f * r + 0.748209f * g + 0.1000044f * b)
    val s = forwardTransform(0.017753f * r + 0.109468f * g + 0.8729690f * b)
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max(((0.4000f * l + 0.4000f * m + 0.2000f * s) * 255.999f).toInt, 0), 255)
        | Math.min(Math.max(((2.2275f * l - 2.4255f * m + 0.1980f * s + 0.5f) * 255.999f).toInt, 0), 255) << 8
        | Math.min(Math.max(((0.4028f * l + 0.1786f * m - 0.5814f * s + 0.5f) * 255.999f).toInt, 0), 255) << 16
        | (abgr & 0xfe000000)
    )
  }

  /** Takes a Color (RGBA8888) and converts to a packed float in IPT_HQ format. */
  def fromColor(color: Color): Float = {
    val r = forwardGamma(color.r)
    val g = forwardGamma(color.g)
    val b = forwardGamma(color.b)
    val l = forwardTransform(0.313921f * r + 0.639468f * g + 0.0465970f * b)
    val m = forwardTransform(0.151693f * r + 0.748209f * g + 0.1000044f * b)
    val s = forwardTransform(0.017753f * r + 0.109468f * g + 0.8729690f * b)
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max(((0.4000f * l + 0.4000f * m + 0.2000f * s) * 255.999f).toInt, 0), 255)
        | Math.min(Math.max(((2.2275f * l - 2.4255f * m + 0.1980f * s + 0.5f) * 255.999f).toInt, 0), 255) << 8
        | Math.min(Math.max(((0.4028f * l + 0.1786f * m - 0.5814f * s + 0.5f) * 255.999f).toInt, 0), 255) << 16
        | ((color.a * 255f).toInt << 24 & 0xfe000000)
    )
  }

  /** Takes RGBA components from 0.0 to 1.0 and converts to a packed float in IPT_HQ format. */
  def fromRGBA(r: Float, g: Float, b: Float, a: Float): Float = {
    val rg = forwardGamma(r)
    val gg = forwardGamma(g)
    val bg = forwardGamma(b)
    val l  = forwardTransform(0.313921f * rg + 0.639468f * gg + 0.0465970f * bg)
    val m  = forwardTransform(0.151693f * rg + 0.748209f * gg + 0.1000044f * bg)
    val s  = forwardTransform(0.017753f * rg + 0.109468f * gg + 0.8729690f * bg)
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max(((0.4000f * l + 0.4000f * m + 0.2000f * s) * 255.999f).toInt, 0), 255)
        | Math.min(Math.max(((2.2275f * l - 2.4255f * m + 0.1980f * s + 0.5f) * 255.999f).toInt, 0), 255) << 8
        | Math.min(Math.max(((0.4028f * l + 0.1786f * m - 0.5814f * s + 0.5f) * 255.999f).toInt, 0), 255) << 16
        | ((a * 255f).toInt << 24 & 0xfe000000)
    )
  }

  /** Gets the red channel value of the given encoded color, as an int ranging from 0 to 255, inclusive. */
  def redInt(encoded: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val l       = reverseTransform(i + 0.097569f * p + 0.205226f * t)
    val m       = reverseTransform(i + -0.11388f * p + 0.133217f * t)
    val s       = reverseTransform(i + 0.032615f * p + -0.67689f * t)
    (reverseGamma(Math.min(Math.max(5.432622f * l + -4.67910f * m + 0.246257f * s, 0f), 1f)) * 255.999f).toInt
  }

  /** Gets the green channel value of the given encoded color, as an int ranging from 0 to 255, inclusive. */
  def greenInt(encoded: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val l       = reverseTransform(i + 0.097569f * p + 0.205226f * t)
    val m       = reverseTransform(i + -0.11388f * p + 0.133217f * t)
    val s       = reverseTransform(i + 0.032615f * p + -0.67689f * t)
    (reverseGamma(Math.min(Math.max(-1.10517f * l + 2.311198f * m + -0.20588f * s, 0f), 1f)) * 255.999f).toInt
  }

  /** Gets the blue channel value of the given encoded color, as an int ranging from 0 to 255, inclusive. */
  def blueInt(encoded: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val l       = reverseTransform(i + 0.097569f * p + 0.205226f * t)
    val m       = reverseTransform(i + -0.11388f * p + 0.133217f * t)
    val s       = reverseTransform(i + 0.032615f * p + -0.67689f * t)
    (reverseGamma(Math.min(Math.max(0.028104f * l + -0.19466f * m + 1.166325f * s, 0f), 1f)) * 255.999f).toInt
  }

  /** Gets the red channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive. */
  def red(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val l       = reverseTransform(i + 0.097569f * p + 0.205226f * t)
    val m       = reverseTransform(i + -0.11388f * p + 0.133217f * t)
    val s       = reverseTransform(i + 0.032615f * p + -0.67689f * t)
    reverseGamma(Math.min(Math.max(5.432622f * l + -4.67910f * m + 0.246257f * s, 0f), 1f))
  }

  /** Gets the green channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive. */
  def green(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val l       = reverseTransform(i + 0.097569f * p + 0.205226f * t)
    val m       = reverseTransform(i + -0.11388f * p + 0.133217f * t)
    val s       = reverseTransform(i + 0.032615f * p + -0.67689f * t)
    reverseGamma(Math.min(Math.max(-1.10517f * l + 2.311198f * m + -0.20588f * s, 0f), 1f))
  }

  /** Gets the blue channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive. */
  def blue(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val l       = reverseTransform(i + 0.097569f * p + 0.205226f * t)
    val m       = reverseTransform(i + -0.11388f * p + 0.133217f * t)
    val s       = reverseTransform(i + 0.032615f * p + -0.67689f * t)
    reverseGamma(Math.min(Math.max(0.028104f * l + -0.19466f * m + 1.166325f * s, 0f), 1f))
  }

  /** Gets the alpha channel value as an even int from 0 to 254. */
  def alphaInt(encoded: Float): Int =
    (java.lang.Float.floatToRawIntBits(encoded) & 0xfe000000) >>> 24

  /** Gets the alpha channel as a float from 0.0f to 1.0f. 0x1.020408p-8f = 0.003937008f (approximately 1/254)
    */
  def alpha(encoded: Float): Float =
    ((java.lang.Float.floatToRawIntBits(encoded) & 0xfe000000) >>> 24) * 0.003937008f

  /** The "intensity" of the given packed float in IPT_HQ format (lightness, 0 to 1). */
  def intensity(encoded: Float): Float =
    (java.lang.Float.floatToRawIntBits(encoded) & 0xff) / 255f

  /** The "protan" of the given packed float in IPT_HQ format (0 to 1). */
  def protan(encoded: Float): Float =
    (java.lang.Float.floatToRawIntBits(encoded) >>> 8 & 0xff) / 255f

  /** The "tritan" of the given packed float in IPT_HQ format (0 to 1). */
  def tritan(encoded: Float): Float =
    (java.lang.Float.floatToRawIntBits(encoded) >>> 16 & 0xff) / 255f

  /** Gets a color as an IPT_HQ packed float given floats for HSL and opacity. */
  def floatGetHSL(hue: Float, saturation: Float, lightness: Float, opacity: Float): Float =
    if (lightness <= 0.001f) {
      java.lang.Float.intBitsToFloat(((opacity * 255f).toInt << 24 & 0xfe000000) | 0x7f7f00)
    } else {
      fromRGBA(FloatColors.hsl2rgb(hue, saturation, lightness, opacity))
    }

  /** Gets the saturation of the given encoded color, as a float ranging from 0.0f to 1.0f, inclusive. */
  def saturation(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val i       = (decoded & 0xff) / 255f
    if (Math.abs(i - 0.5) > 0.495f) 0f
    else {
      val p = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
      val t = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
      val l = reverseTransform(i + 0.097569f * p + 0.205226f * t)
      val m = reverseTransform(i + -0.11388f * p + 0.133217f * t)
      val s = reverseTransform(i + 0.032615f * p + -0.67689f * t)
      val r = reverseGamma(Math.min(Math.max(5.432622f * l + -4.67910f * m + 0.246257f * s, 0f), 1f))
      val g = reverseGamma(Math.min(Math.max(-1.10517f * l + 2.311198f * m + -0.20588f * s, 0f), 1f))
      val b = reverseGamma(Math.min(Math.max(0.028104f * l + -0.19466f * m + 1.166325f * s, 0f), 1f))
      var x = 0f
      var y = 0f
      var w = 0f
      if (g < b) {
        x = b
        y = g
      } else {
        x = g
        y = b
      }
      if (r < x) {
        w = r
      } else {
        w = x
        x = r
      }
      x - Math.min(w, y)
    }
  }

  /** Gets the lightness of the given encoded color, as a float ranging from 0.0f to 1.0f, inclusive. */
  def lightness(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val l       = reverseTransform(i + 0.097569f * p + 0.205226f * t)
    val m       = reverseTransform(i + -0.11388f * p + 0.133217f * t)
    val s       = reverseTransform(i + 0.032615f * p + -0.67689f * t)
    val r       = reverseGamma(Math.min(Math.max(5.432622f * l + -4.67910f * m + 0.246257f * s, 0f), 1f))
    val g       = reverseGamma(Math.min(Math.max(-1.10517f * l + 2.311198f * m + -0.20588f * s, 0f), 1f))
    val b       = reverseGamma(Math.min(Math.max(0.028104f * l + -0.19466f * m + 1.166325f * s, 0f), 1f))
    var x       = 0f
    var y       = 0f
    var w       = 0f
    if (g < b) {
      x = b
      y = g
    } else {
      x = g
      y = b
    }
    if (r < x) {
      w = r
    } else {
      w = x
      x = r
    }
    val d = x - Math.min(w, y)
    x * (1f - 0.5f * d / (x + 1e-10f))
  }

  /** Gets the hue of the given encoded color, as a float from 0f (inclusive, red and approaching orange if increased) to 1f (exclusive, red and approaching purple if decreased).
    */
  def hue(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val l       = reverseTransform(i + 0.097569f * p + 0.205226f * t)
    val m       = reverseTransform(i + -0.11388f * p + 0.133217f * t)
    val s       = reverseTransform(i + 0.032615f * p + -0.67689f * t)
    val r       = reverseGamma(Math.min(Math.max(5.432622f * l + -4.67910f * m + 0.246257f * s, 0f), 1f))
    val g       = reverseGamma(Math.min(Math.max(-1.10517f * l + 2.311198f * m + -0.20588f * s, 0f), 1f))
    val b       = reverseGamma(Math.min(Math.max(0.028104f * l + -0.19466f * m + 1.166325f * s, 0f), 1f))
    var x       = 0f
    var y       = 0f
    var z       = 0f
    var w       = 0f
    if (g < b) {
      x = b
      y = g
      z = -1f
      w = 2f / 3f
    } else {
      x = g
      y = b
      z = 0f
      w = -1f / 3f
    }
    if (r < x) {
      z = w
      w = r
    } else {
      w = x
      x = r
    }
    val d = x - Math.min(w, y)
    Math.abs(z + (w - y) / (6f * d + 1e-10f))
  }

  /** Gets a variation on the packed float color basis as another packed float that has its hue, saturation, lightness, and opacity adjusted by the specified amounts. Note that this edits the color in
    * HSL space, not IPT_HQ! Takes floats representing the amounts of change to apply to hue, saturation, lightness, and opacity; these can be between -1f and 1f. Returns a float that can be used as a
    * packed or encoded color. The float is likely to be different than the result of `ipt` unless hue, saturation, lightness, and opacity are all 0. This won't allocate any objects.
    *
    * The parameters this takes all specify additive changes for a color component, clamping the final values so they can't go above 1 or below 0, with an exception for hue, which can rotate around if
    * lower or higher hues would be used.
    */
  def toEditedFloat(basis: Float, hue: Float, saturation: Float, light: Float, opacity: Float): Float = {
    val e = java.lang.Float.floatToRawIntBits(basis)
    val i = Math.min(Math.max(light + (e & 0xff) / 255f, 0f), 1f)
    // 0x1.020408p-8f = 0.003937008f
    val op = Math.min(Math.max(opacity + (e >>> 24 & 0xfe) * 0.003937008f, 0f), 1f)
    if (i <= 0.001f) {
      java.lang.Float.intBitsToFloat(((op * 255f).toInt << 24 & 0xfe000000) | 0x808000)
    } else {
      val p = ((e >>> 7 & 0x1fe) - 0xff) / 255f
      val t = ((e >>> 15 & 0x1fe) - 0xff) / 255f
      val l = reverseTransform(i + 0.097569f * p + 0.205226f * t)
      val m = reverseTransform(i + -0.11388f * p + 0.133217f * t)
      val s = reverseTransform(i + 0.032615f * p + -0.67689f * t)
      val r = reverseGamma(Math.min(Math.max(5.432622f * l + -4.67910f * m + 0.246257f * s, 0f), 1f))
      val g = reverseGamma(Math.min(Math.max(-1.10517f * l + 2.311198f * m + -0.20588f * s, 0f), 1f))
      val b = reverseGamma(Math.min(Math.max(0.028104f * l + -0.19466f * m + 1.166325f * s, 0f), 1f))
      var x = 0f
      var y = 0f
      var z = 0f
      var w = 0f
      if (g < b) {
        x = b
        y = g
        z = -1f
        w = 2f / 3f
      } else {
        x = g
        y = b
        z = 0f
        w = -1f / 3f
      }
      if (r < x) {
        z = w
        w = r
      } else {
        w = x
        x = r
      }
      val d   = x - Math.min(w, y)
      val lum = x * (1f - 0.5f * d / (x + 1e-10f))
      val h   = hue + Math.abs(z + (w - y) / (6f * d + 1e-10f)) + 1f
      val sat = saturation + (x - lum) / (Math.min(lum, 1f - lum) + 1e-10f)
      fromRGBA(FloatColors.hsl2rgb(h - h.toInt, Math.min(Math.max(sat, 0f), 1f), lum, op))
    }
  }

  /** Interpolates from start towards white by change. */
  def lighten(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val i = s & 0xff; val other = s & 0xfeffff00
    java.lang.Float.intBitsToFloat(((i + (0xff - i) * change).toInt & 0xff) | other)
  }

  /** Interpolates from start towards black by change. */
  def darken(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val i = s & 0xff; val other = s & 0xfeffff00
    java.lang.Float.intBitsToFloat(((i * (1f - change)).toInt & 0xff) | other)
  }

  /** Interpolates from start towards a warmer color (orange to magenta) by change. */
  def protanUp(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val p = s >>> 8 & 0xff; val other = s & 0xfeff00ff
    java.lang.Float.intBitsToFloat(((p + (0xff - p) * change).toInt << 8 & 0xff00) | other)
  }

  /** Interpolates from start towards a cooler color (green to blue) by change. */
  def protanDown(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val p = s >>> 8 & 0xff; val other = s & 0xfeff00ff
    java.lang.Float.intBitsToFloat(((p * (1f - change)).toInt & 0xff) << 8 | other)
  }

  /** Interpolates from start towards a "natural" color (between green and orange) by change. */
  def tritanUp(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val t = s >>> 16 & 0xff; val other = s & 0xfe00ffff
    java.lang.Float.intBitsToFloat(((t + (0xff - t) * change).toInt << 16 & 0xff0000) | other)
  }

  /** Interpolates from start towards an "artificial" color (between blue and purple) by change. */
  def tritanDown(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val t = s >>> 16 & 0xff; val other = s & 0xfe00ffff
    java.lang.Float.intBitsToFloat(((t * (1f - change)).toInt & 0xff) << 16 | other)
  }

  /** Interpolates from start towards opaque by change. */
  def blot(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val opacity = s >>> 24 & 0xfe; val other = s & 0x00ffffff
    java.lang.Float.intBitsToFloat(((opacity + (0xfe - opacity) * change).toInt & 0xfe) << 24 | other)
  }

  /** Interpolates from start towards transparent by change. */
  def fade(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val opacity = s >>> 24 & 0xfe; val other = s & 0x00ffffff
    java.lang.Float.intBitsToFloat(((opacity * (1f - change)).toInt & 0xfe) << 24 | other)
  }

  /** Brings the chromatic components of start closer to grayscale by change (desaturating). */
  def dullen(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start)
    ipt((s & 0xff) / 255f, ((s >>> 8 & 0xff) / 255f - 0.5f) * (1f - change) + 0.5f, ((s >>> 16 & 0xff) / 255f - 0.5f) * (1f - change) + 0.5f, (s >>> 25) / 127f)
  }

  /** Pushes the chromatic components of start away from grayscale by change (saturating). */
  def enrich(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start)
    limitToGamut((s & 0xff) / 255f, ((s >>> 8 & 0xff) / 255f - 0.5f) * (1f + change) + 0.5f, ((s >>> 16 & 0xff) / 255f - 0.5f) * (1f + change) + 0.5f, (s >>> 25) / 127f)
  }

  /** Given a packed float IPT_HQ color `mainColor` and another IPT_HQ color that it should be made to contrast with, gets a packed float IPT_HQ color with roughly inverted intensity but the same
    * chromatic channels and opacity (P and T are likely to be clamped if the result gets close to white or black). This won't ever produce black or other very dark colors, and also has a gap in the
    * range it produces for intensity values between 0.5 and 0.55. That allows most of the colors this method produces to contrast well as a foreground when displayed on a background of
    * `contrastingColor`, or vice versa. This will leave the intensity unchanged if the chromatic channels of the contrastingColor and those of the mainColor are already very different.
    */
  def inverseLightness(mainColor: Float, contrastingColor: Float): Float = {
    val bits         = java.lang.Float.floatToRawIntBits(mainColor)
    val contrastBits = java.lang.Float.floatToRawIntBits(contrastingColor)
    val i            = bits & 0xff
    val p            = bits >>> 8 & 0xff
    val t            = bits >>> 16 & 0xff
    val ci           = contrastBits & 0xff
    val cp           = contrastBits >>> 8 & 0xff
    val ct           = contrastBits >>> 16 & 0xff
    if ((p - cp) * (p - cp) + (t - ct) * (t - ct) >= 0x10000) {
      mainColor
    } else {
      // 0x1.0p-8f = 0.00390625f (1/256)
      ipt(if (ci < 128) i * (0.45f / 255f) + 0.55f else 0.5f - i * (0.45f / 255f), p / 255f, t / 255f, 0.00390625f * (bits >>> 24))
    }
  }

  /** Given a packed float IPT_HQ color `mainColor` and another IPT_HQ color that it should be made to contrast with, gets a packed float IPT_HQ color with I that should be quite different from
    * `contrastingColor`'s I, but the same chromatic channels and opacity (P and T are likely to be clamped if the result gets close to white or black). This allows most of the colors this method
    * produces to contrast well as a foreground when displayed on a background of `contrastingColor`, or vice versa.
    *
    * This is similar to `inverseLightness`, but is considerably simpler, and this method will change the lightness of mainColor when the two given colors have close lightness but distant chroma.
    * Because it averages the original I of mainColor with the modified one, this tends to not produce harsh color changes.
    */
  def differentiateLightness(mainColor: Float, contrastingColor: Float): Float = {
    val main     = java.lang.Float.floatToRawIntBits(mainColor)
    val contrast = java.lang.Float.floatToRawIntBits(contrastingColor)
    limitToGamut(java.lang.Float.intBitsToFloat((main & 0xfeffff00) | (contrast + 128 & 0xff) + (main & 0xff) >>> 1))
  }

  /** Pretty simple; adds 0.5 to the given color's I and wraps it around if it would go above 1.0, then averages that with the original I. This means light colors become darker, and dark colors become
    * lighter, with almost all results in the middle-range of possible lightness.
    */
  def offsetLightness(mainColor: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(mainColor)
    limitToGamut(java.lang.Float.intBitsToFloat((decoded & 0xfeffff00) | (decoded + 128 & 0xff) + (decoded & 0xff) >>> 1))
  }

  /** Makes the additive IPT_HQ color cause less of a change when used as a tint. */
  def lessenChange(color: Float, fraction: Float): Float = {
    val e  = java.lang.Float.floatToRawIntBits(color)
    val sI = 0x80; val sP     = 0x80; val sT             = 0x80
    val eI = e & 0xff; val eP = (e >>> 8) & 0xff; val eT = (e >>> 16) & 0xff; val eAlpha = e >>> 24 & 0xfe
    java.lang.Float.intBitsToFloat(
      ((sI + fraction * (eI - sI)).toInt & 0xff)
        | (((sP + fraction * (eP - sP)).toInt & 0xff) << 8)
        | (((sT + fraction * (eT - sT)).toInt & 0xff) << 16)
        | (eAlpha << 24)
    )
  }

  /** Makes a quasi-randomly-edited variant on the given color, allowing typically a small amount of variance (such as 0.05 to 0.25) between the given color and what this can return. The seed should
    * be different each time this is called, and can be obtained from a random number generator to make the colors more random, or can be incremented on each call. If the seed is only incremented or
    * decremented, then this shouldn't produce two similar colors in a row unless variance is very small. The variance affects the I, P, and T of the generated color, and each of those channels can go
    * up or down by the given variance as long as the total distance isn't greater than the variance.
    */
  def randomEdit(color: Float, seed: Long, variance: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(color)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val limit   = variance * variance
    var sd      = seed
    // 0x7FFFFFp-1f = 4194303.5f, 0x1p-22f = 2.3841858e-7f
    boundary[Float] {
      var j = 0
      while (j < 50) {
        val x = (((sd * 0xd1b54a32d192ed03L >>> 41) - 4194303.5f) * 2.3841858e-7f) * variance
        val y = (((sd * 0xabc98388fb8fac03L >>> 41) - 4194303.5f) * 2.3841858e-7f) * variance
        val z = (((sd * 0x8cb92ba72f3d8dd7L >>> 41) - 4194303.5f) * 2.3841858e-7f) * variance
        sd += 0x9e3779b97f4a7c15L
        val dist = x * x + y * y + z * z
        if (dist <= limit) {
          val nx = x + i
          val ny = (p + y) * 0.5f + 0.5f
          val nz = (t + z) * 0.5f + 0.5f
          if (inGamut(nx, ny, nz)) {
            break(
              java.lang.Float.intBitsToFloat(
                (decoded & 0xfe000000) | ((nz * 255.5f).toInt << 16 & 0xff0000)
                  | ((ny * 255.5f).toInt << 8 & 0xff00) | (nx * 255.5f).toInt
              )
            )
          }
        }
        j += 1
      }
      color
    }
  }

  /** Returns true if the given packed float color, as IPT_HQ, is valid to convert losslessly back to RGBA. */
  def inGamut(packed: Float): Boolean = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val l       = reverseTransform(i + 0.097569f * p + 0.205226f * t)
    val m       = reverseTransform(i + -0.11388f * p + 0.133217f * t)
    val s       = reverseTransform(i + 0.032615f * p + -0.67689f * t)
    val r       = 5.432622f * l + -4.67910f * m + 0.246257f * s
    if (r < 0f || r > 1.0f) false
    else {
      val g = -1.10517f * l + 2.311198f * m + -0.20588f * s
      if (g < 0f || g > 1.0f) false
      else {
        val b = 0.028104f * l + -0.19466f * m + 1.166325f * s
        b >= 0f && b <= 1.0f
      }
    }
  }

  /** Returns true if the given IPT_HQ values are valid to convert losslessly back to RGBA.
    * @param i
    *   intensity channel, as a float from 0 to 1
    * @param p
    *   protan channel, as a float from 0 to 1
    * @param t
    *   tritan channel, as a float from 0 to 1
    */
  def inGamut(i: Float, p: Float, t: Float): Boolean = {
    val p2 = (p - 0.5f) * 2f
    val t2 = (t - 0.5f) * 2f
    val l  = reverseTransform(i + 0.097569f * p2 + 0.205226f * t2)
    val m  = reverseTransform(i + -0.11388f * p2 + 0.133217f * t2)
    val s  = reverseTransform(i + 0.032615f * p2 + -0.67689f * t2)
    val r  = 5.432622f * l + -4.67910f * m + 0.246257f * s
    if (r < 0f || r > 1.0f) false
    else {
      val g = -1.10517f * l + 2.311198f * m + -0.20588f * s
      if (g < 0f || g > 1.0f) false
      else {
        val b = 0.028104f * l + -0.19466f * m + 1.166325f * s
        b >= 0f && b <= 1.0f
      }
    }
  }

  /** Iteratively checks whether the given IPT_HQ color is in-gamut, and either brings the color closer to grayscale if it isn't in-gamut, or returns it as soon as it is in-gamut. Maintains the
    * intensity of the color, only bringing protan and tritan closer to grayscale.
    */
  def limitToGamut(packed: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    var p2      = p
    var t2      = t
    // 0x1p-5f = 0.03125f (1/32)
    var attempt = 31
    while (attempt >= 0) {
      val l = reverseTransform(i + 0.097569f * p2 + 0.205226f * t2)
      val m = reverseTransform(i + -0.11388f * p2 + 0.133217f * t2)
      val s = reverseTransform(i + 0.032615f * p2 + -0.67689f * t2)
      val r = 5.432622f * l + -4.67910f * m + 0.246257f * s
      val g = -1.10517f * l + 2.311198f * m + -0.20588f * s
      val b = 0.028104f * l + -0.19466f * m + 1.166325f * s
      if (r >= 0f && r <= 1f && g >= 0f && g <= 1f && b >= 0f && b <= 1f) {
        attempt = -1 // break out
      } else {
        val progress = attempt * 0.03125f
        p2 = MathUtils.lerp(0, p, progress)
        t2 = MathUtils.lerp(0, t, progress)
        attempt -= 1
      }
    }
    ipt(i, p2 * 0.5f + 0.5f, t2 * 0.5f + 0.5f, (decoded >>> 25) / 127f)
  }

  /** Iteratively checks whether the given IPT_HQ color is in-gamut, and either brings the color closer to grayscale if it isn't in-gamut, or returns it as soon as it is in-gamut. This always produces
    * an opaque color.
    */
  def limitToGamut(i: Float, p: Float, t: Float): Float =
    limitToGamut(i, p, t, 1f)

  /** Iteratively checks whether the given IPT_HQ color is in-gamut, and either brings the color closer to grayscale if it isn't in-gamut, or returns it as soon as it is in-gamut.
    */
  def limitToGamut(I: Float, P: Float, T: Float, alpha: Float): Float = {
    val i2 = Math.min(Math.max(I, 0f), 1f)
    var p  = Math.min(Math.max((P - 0.5f) * 2f, -1f), 1f)
    var t  = Math.min(Math.max((T - 0.5f) * 2f, -1f), 1f)
    val a  = Math.min(Math.max(alpha, 0f), 1f)
    val pO = p
    val tO = t
    // 0x1p-5f = 0.03125f (1/32)
    var attempt = 31
    while (attempt >= 0) {
      val l = reverseTransform(i2 + 0.097569f * p + 0.205226f * t)
      val m = reverseTransform(i2 + -0.11388f * p + 0.133217f * t)
      val s = reverseTransform(i2 + 0.032615f * p + -0.67689f * t)
      val r = 5.432622f * l + -4.67910f * m + 0.246257f * s
      val g = -1.10517f * l + 2.311198f * m + -0.20588f * s
      val b = 0.028104f * l + -0.19466f * m + 1.166325f * s
      if (r >= 0f && r <= 1f && g >= 0f && g <= 1f && b >= 0f && b <= 1f) {
        attempt = -1 // break out
      } else {
        val progress = attempt * 0.03125f
        p = MathUtils.lerp(0, pO, progress)
        t = MathUtils.lerp(0, tO, progress)
        attempt -= 1
      }
    }
    ipt(i2, p * 0.5f + 0.5f, t * 0.5f + 0.5f, a)
  }

  /** Given a packed float IPT_HQ color, this edits its intensity, protan, tritan, and alpha channels by adding the corresponding "add" parameter and then clamping. This returns a different float
    * value. You can give a value of 0 for any "add" parameter you want to stay unchanged. This clamps the resulting color to remain in-gamut, so it should be safe to convert it back to RGBA.
    */
  def editIPT(encoded: Float, addI: Float, addP: Float, addT: Float, addAlpha: Float): Float =
    editIPT(encoded, addI, addP, addT, addAlpha, 1f, 1f, 1f, 1f)

  /** Given a packed float IPT_HQ color, this edits its intensity, protan, tritan, and alpha channels by first multiplying each channel by the corresponding "mul" parameter and then adding the
    * corresponding "add" parameter, before clamping. This means the intensity value is multiplied by `mulI`, then has `addI` added, and then is clamped to the normal range for intensity (0 to 1).
    * This returns a different float value. You can give a value of 0 for any "add" parameter you want to stay unchanged, or a value of 1 for any "mul" parameter that shouldn't change. Note that this
    * manipulates protan and tritan in the -1 to 1 range, so if you multiply by a small number like 0.25f, then this will produce a less-saturated color, and if you multiply by a larger number like
    * 4f, then you will get a much more-saturated color. This clamps the resulting color to remain in-gamut, so it should be safe to convert it back to RGBA.
    */
  def editIPT(
    encoded:  Float,
    addI:     Float,
    addP:     Float,
    addT:     Float,
    addAlpha: Float,
    mulI:     Float,
    mulP:     Float,
    mulT:     Float,
    mulAlpha: Float
  ): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val i       = (decoded & 0xff) / 255f
    val pRaw    = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val tRaw    = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    var al      = (decoded >>> 25) / 127f

    val i2 = Math.min(Math.max(i * mulI + addI, 0f), 1f)
    var p  = Math.min(Math.max(pRaw * mulP + addP, -1f), 1f)
    var t  = Math.min(Math.max(tRaw * mulT + addT, -1f), 1f)
    al = Math.min(Math.max(al * mulAlpha + addAlpha, 0f), 1f)
    val pO = p
    val tO = t
    // 0x1p-5f = 0.03125f (1/32)
    var attempt = 31
    while (attempt >= 0) {
      val l = reverseTransform(i2 + 0.097569f * p + 0.205226f * t)
      val m = reverseTransform(i2 + -0.11388f * p + 0.133217f * t)
      val s = reverseTransform(i2 + 0.032615f * p + -0.67689f * t)
      val r = 5.432622f * l + -4.67910f * m + 0.246257f * s
      val g = -1.10517f * l + 2.311198f * m + -0.20588f * s
      val b = 0.028104f * l + -0.19466f * m + 1.166325f * s
      if (r >= 0f && r <= 1f && g >= 0f && g <= 1f && b >= 0f && b <= 1f) {
        attempt = -1 // break out
      } else {
        val progress = attempt * 0.03125f
        p = MathUtils.lerp(0, pO, progress)
        t = MathUtils.lerp(0, tO, progress)
        attempt -= 1
      }
    }
    ipt(i2, p * 0.5f + 0.5f, t * 0.5f + 0.5f, al)
  }

  /** Produces a random packed float color that is always in-gamut and should be uniformly distributed. */
  def randomColor(random: java.util.Random): Float = {
    var i = random.nextFloat()
    var p = random.nextFloat()
    var t = random.nextFloat()
    while (!inGamut(i, p, t)) {
      i = random.nextFloat()
      p = random.nextFloat()
      t = random.nextFloat()
    }
    ipt(i, p, t, 1f)
  }
}
