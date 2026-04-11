/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful
package ycwcm

import scala.util.boundary
import scala.util.boundary.break

import sge.colorful.FloatColors
import sge.graphics.Color
import sge.math.MathUtils

/** Contains code for manipulating colors as `int` and packed `float` values in the YCwCm color space.
  */
object ColorTools {

  /** Gets a packed float representation of a color given as 4 float components, here, Y (luma or lightness), Cw (chromatic warmth), Cm (chromatic mildness), and A (alpha or opacity). Luma should be
    * between 0 and 1, inclusive, with 0 used for very dark colors (almost only black), and 1 used for very light colors (almost only white). The two chroma values range from 0.0 to 1.0, and there's
    * some aesthetic value in changing just one chroma value. When warm is high and mild is low, the color is more reddish, when both are low it is more bluish, when mild is high and warm is low, the
    * color tends to be greenish, and when both are high it tends to be brown or yellow. When warm and mild are both near 0.5f, the color is closer to gray. Alpha is the multiplicative opacity of the
    * color, and acts like RGBA's alpha.
    *
    * This method bit-masks the resulting color's byte values, so any values can technically be given to this as luma, warm, and mild, but they will only be reversible from the returned float color to
    * the original Y, Cw, and Cm values if the original values were a valid YCwCm color and not an "imaginary color." `fromRGBA(Float)` should always produce a valid YCwCm color.
    *
    * @param luma
    *   0f to 1f, luma or Y component of YCwCm, with 0.5f meaning "no change" and 1f brightening
    * @param warm
    *   0f to 1f, "chroma warm" or Cw component of YCwCm, with 1f more red or yellow
    * @param mild
    *   0f to 1f, "chroma mild" or Cm component of YCwCm, with 1f more green or yellow
    * @param alpha
    *   0f to 1f, 0f makes the color transparent and 1f makes it opaque
    * @return
    *   a float encoding a color with the given properties
    */
  def ycwcm(luma: Float, warm: Float, mild: Float, alpha: Float): Float =
    java.lang.Float.intBitsToFloat(
      ((alpha * 255).toInt << 24 & 0xfe000000) | ((mild * 255).toInt << 16 & 0xff0000)
        | ((warm * 255).toInt << 8 & 0xff00) | ((luma * 255).toInt & 0xff)
    )

  /** Converts a packed float color in the format produced by `ycwcm(Float, Float, Float, Float)` to an RGBA8888 int. This format of int can be used with Pixmap and in some other places in libGDX.
    * @param packed
    *   a packed float color, as produced by `ycwcm(Float, Float, Float, Float)`
    * @return
    *   an RGBA8888 int color
    */
  def toRGBA8888(packed: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val y       = decoded & 0xff
    val cw      = (decoded >>> 7 & 0x1fe) - 0xff
    val cm      = ((decoded >>> 15 & 0x1fe) - 0xff) >> 1
    Math.min(Math.max(y + (cw * 5 >> 3) - cm, 0), 0xff) << 24 |
      Math.min(Math.max(y - (cw * 3 >> 3) + cm, 0), 0xff) << 16 |
      Math.min(Math.max(y - (cw * 3 >> 3) - cm, 0), 0xff) << 8 |
      (decoded & 0xfe000000) >>> 24 | decoded >>> 31
  }

  /** Writes a YCwCm-format packed float color into an RGBA8888 Color as used by libGDX (called `editing`).
    * @param editing
    *   a libGDX color that will be filled in-place with an RGBA conversion of `packed`
    * @param packed
    *   a packed float color, as produced by `ycwcm(Float, Float, Float, Float)`
    * @return
    *   the editing Color, filled with RGBA values
    */
  def toColor(editing: Color, packed: Float): Color = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    // 0x1.010102p-8f = 0.003921569f, 0x1.414142p-9f = 0.0024509805f, 0x1.010102p-9f = 0.0019607844f, 0x1.818184p-10f = 0.0014705883f
    editing.set(
      Math.min(
        Math.max((decoded & 0xff) * 0.003921569f + ((decoded >>> 8 & 0xff) - 127.5f) * 0.0024509805f - ((decoded >>> 16 & 0xff) - 127.5f) * 0.0019607844f, 0f),
        1f
      ),
      Math.min(
        Math.max((decoded & 0xff) * 0.003921569f - ((decoded >>> 8 & 0xff) - 127.5f) * 0.0014705883f + ((decoded >>> 16 & 0xff) - 127.5f) * 0.0019607844f, 0f),
        1f
      ),
      Math.min(
        Math.max((decoded & 0xff) * 0.003921569f - ((decoded >>> 8 & 0xff) - 127.5f) * 0.0014705883f - ((decoded >>> 16 & 0xff) - 127.5f) * 0.0019607844f, 0f),
        1f
      ),
      ((decoded & 0xfe000000) >>> 24) * 0.003937008f
    )
    editing
  }

  /** Writes a YCwCm-format packed float color into a YCwCm-format Color called `editing`. This is mostly useful if the rest of your application expects colors in YCwCm format.
    *
    * Internally, this simply calls `Color.abgr8888ToColor(Color, Float)` and returns the edited Color.
    * @param editing
    *   a libGDX Color that will be filled in-place with the color `ycwcmColor`, unchanged from its color space
    * @param ycwcmColor
    *   a packed float color, as produced by `ycwcm(Float, Float, Float, Float)`
    * @return
    *   the editing Color
    */
  def toYCwCmColor(editing: Color, ycwcmColor: Float): Color = {
    Color.abgr8888ToColor(editing, ycwcmColor)
    editing
  }

  /** Converts a packed float color in the format produced by `ycwcm(Float, Float, Float, Float)` to a packed float in RGBA format. This format of float can be used with the standard SpriteBatch and
    * in some other places in libGDX.
    * @param packed
    *   a packed float color, as produced by `ycwcm(Float, Float, Float, Float)`
    * @return
    *   a packed float color as RGBA
    */
  def toRGBA(packed: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val y       = decoded & 0xff
    val cw      = (decoded >>> 7 & 0x1fe) - 0xff
    val cm      = ((decoded >>> 15 & 0x1fe) - 0xff) >> 1
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max(y + (cw * 5 >> 3) - cm, 0), 0xff)
        | Math.min(Math.max(y - (cw * 3 >> 3) + cm, 0), 0xff) << 8
        | Math.min(Math.max(y - (cw * 3 >> 3) - cm, 0), 0xff) << 16
        | (decoded & 0xfe000000)
    )
  }

  /** Takes a color encoded as an RGBA8888 int and converts to a packed float in the YCwCm this uses.
    * @param rgba
    *   an int with the channels (in order) red, green, blue, alpha; should have 8 bits per channel
    * @return
    *   a packed float as YCwCm, which this class can use
    */
  def fromRGBA8888(rgba: Int): Float =
    java.lang.Float.intBitsToFloat(
      ((rgba >>> 24) * 3 + (rgba >>> 16 & 0xff) * 4 + (rgba >>> 8 & 0xff) >> 3)
        | (0xff + (rgba >>> 24) - (rgba >>> 8 & 0xff) & 0x1fe) << 7
        | (0xff + (rgba >>> 16 & 0xff) - (rgba >>> 8 & 0xff) & 0x1fe) << 15
        | (rgba & 0xfe) << 24
    )

  /** Takes a color encoded as an RGBA8888 packed float and converts to a packed float in the YCwCm this uses.
    * @param packed
    *   a packed float in RGBA8888 format, with A in the MSB and R in the LSB
    * @return
    *   a packed float as YCwCm, which this class can use
    */
  def fromRGBA(packed: Float): Float = {
    val rgba = java.lang.Float.floatToRawIntBits(packed)
    java.lang.Float.intBitsToFloat(
      ((rgba & 0xff) * 3 + (rgba >>> 8 & 0xff) * 4 + (rgba >>> 16 & 0xff) >> 3)
        | (0xff + (rgba & 0xff) - (rgba >>> 16 & 0xff) & 0x1fe) << 7
        | (0xff + (rgba >>> 8 & 0xff) - (rgba >>> 16 & 0xff) & 0x1fe) << 15
        | (rgba >>> 24 & 0xfe) << 24
    )
  }

  /** Takes a libGDX Color that uses RGBA8888 channels and converts to a packed float in the YCwCm this uses.
    * @param color
    *   a libGDX RGBA8888 Color
    * @return
    *   a packed float as YCwCm, which this class can use
    */
  def fromColor(color: Color): Float =
    java.lang.Float.intBitsToFloat(
      (255 * (color.r * 0.375f + color.g * 0.5f + color.b * 0.125f)).toInt & 0xff
        | ((color.r - color.b + 1f) * 127.5f).toInt << 8 & 0xff00
        | ((color.g - color.b + 1f) * 127.5f).toInt << 16 & 0xff0000
        | ((color.a * 255f).toInt << 24 & 0xfe000000)
    )

  /** Takes RGBA components from 0.0 to 1.0 each and converts to a packed float in the YCwCm this uses.
    * @param r
    *   red, from 0.0 to 1.0 (both inclusive)
    * @param g
    *   green, from 0.0 to 1.0 (both inclusive)
    * @param b
    *   blue, from 0.0 to 1.0 (both inclusive)
    * @param a
    *   alpha, from 0.0 to 1.0 (both inclusive)
    * @return
    *   a packed float as YCwCm, which this class can use
    */
  def fromRGBA(r: Float, g: Float, b: Float, a: Float): Float =
    java.lang.Float.intBitsToFloat(
      (255 * (r * 0.375f + g * 0.5f + b * 0.125f)).toInt & 0xff
        | ((r - b + 1f) * 127.5f).toInt << 8 & 0xff00
        | ((g - b + 1f) * 127.5f).toInt << 16 & 0xff0000
        | ((a * 255f).toInt << 24 & 0xfe000000)
    )

  /** Gets the red channel value of the given encoded color, as an int ranging from 0 to 255, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by `ycwcm(Float, Float, Float, Float)`
    * @return
    *   an int from 0 to 255, inclusive, representing the red channel value of the given encoded color
    */
  def redInt(encoded: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    (decoded & 0xff) + (((decoded >>> 7 & 0x1fe) - 0xff) * 5 >>> 4) - (((decoded >>> 15 & 0x1fe) - 0xff) >>> 2)
  }

  /** Gets the green channel value of the given encoded color, as an int ranging from 0 to 255, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by `ycwcm(Float, Float, Float, Float)`
    * @return
    *   an int from 0 to 255, inclusive, representing the green channel value of the given encoded color
    */
  def greenInt(encoded: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    (decoded & 0xff) - (((decoded >>> 7 & 0x1fe) - 0xff) * 3 >> 4) + (((decoded >>> 15 & 0x1fe) - 0xff) >> 2)
  }

  /** Gets the blue channel value of the given encoded color, as an int ranging from 0 to 255, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by `ycwcm(Float, Float, Float, Float)`
    * @return
    *   an int from 0 to 255, inclusive, representing the blue channel value of the given encoded color
    */
  def blueInt(encoded: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    (decoded & 0xff) - (((decoded >>> 7 & 0x1fe) - 0xff) * 3 >> 4) - (((decoded >>> 15 & 0x1fe) - 0xff) >> 2)
  }

  /** Gets the alpha channel value of the given encoded color, as an even int ranging from 0 to 254, inclusive. Because of how alpha is stored in libGDX, no odd-number values are possible for alpha.
    * @param encoded
    *   a color as a packed float that can be obtained by `ycwcm(Float, Float, Float, Float)`
    * @return
    *   an even int from 0 to 254, inclusive, representing the alpha channel value of the given encoded color
    */
  def alphaInt(encoded: Float): Int =
    (java.lang.Float.floatToRawIntBits(encoded) & 0xfe000000) >>> 24

  /** Gets the red channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by `ycwcm(Float, Float, Float, Float)`
    * @return
    *   a float from 0.0f to 1.0f, inclusive, representing the red channel value of the given encoded color
    */
  def red(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    // 0x1.010102p-8f = 0.003921569f, 0x1.414142p-9f = 0.0024509805f, 0x1.010102p-9f = 0.0019607844f
    Math.min(
      Math.max((decoded & 0xff) * 0.003921569f + ((decoded >>> 8 & 0xff) - 127.5f) * 0.0024509805f - ((decoded >>> 16 & 0xff) - 127.5f) * 0.0019607844f, 0f),
      1f
    )
  }

  /** Gets the green channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by `ycwcm(Float, Float, Float, Float)`
    * @return
    *   a float from 0.0f to 1.0f, inclusive, representing the green channel value of the given encoded color
    */
  def green(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    // 0x1.010102p-8f = 0.003921569f, 0x1.818184p-10f = 0.0014705883f, 0x1.010102p-9f = 0.0019607844f
    Math.min(
      Math.max((decoded & 0xff) * 0.003921569f - ((decoded >>> 8 & 0xff) - 127.5f) * 0.0014705883f + ((decoded >>> 16 & 0xff) - 127.5f) * 0.0019607844f, 0f),
      1f
    )
  }

  /** Gets the blue channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by `ycwcm(Float, Float, Float, Float)`
    * @return
    *   a float from 0.0f to 1.0f, inclusive, representing the blue channel value of the given encoded color
    */
  def blue(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    // 0x1.010102p-8f = 0.003921569f, 0x1.818184p-10f = 0.0014705883f, 0x1.010102p-9f = 0.0019607844f
    Math.min(
      Math.max((decoded & 0xff) * 0.003921569f - ((decoded >>> 8 & 0xff) - 127.5f) * 0.0014705883f - ((decoded >>> 16 & 0xff) - 127.5f) * 0.0019607844f, 0f),
      1f
    )
  }

  /** Gets the alpha channel value of the given encoded color, as a float from 0.0f to 1.0f, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by `ycwcm(Float, Float, Float, Float)`
    * @return
    *   a float from 0.0f to 1.0f, inclusive, representing the alpha channel value of the given encoded color
    */
  def alpha(encoded: Float): Float =
    ((java.lang.Float.floatToRawIntBits(encoded) & 0xfe000000) >>> 24) * 0.003937008f

  /** Gets a color as a YCwCm packed float given floats representing hue, saturation, lightness, and opacity. All parameters should normally be between 0 and 1 inclusive, though any hue is tolerated
    * (precision loss may affect the color if the hue is too large). A hue of 0 is red, progressively higher hue values go to orange, yellow, green, blue, and purple before wrapping around to red as
    * it approaches 1. A saturation of 0 is grayscale, a saturation of 1 is brightly colored, and values close to 1 will usually appear more distinct than values close to 0, especially if the hue is
    * different. A lightness of 0.001f or less is always black (also using a shortcut if this is the case, respecting opacity), while a lightness of 1f is white. Very bright colors are mostly in a
    * band of high-saturation where lightness is 0.5f.
    *
    * @param hue
    *   0f to 1f, color wheel position
    * @param saturation
    *   0f to 1f, 0f is grayscale and 1f is brightly colored
    * @param lightness
    *   0f to 1f, 0f is black and 1f is white
    * @param opacity
    *   0f to 1f, 0f is fully transparent and 1f is opaque
    * @return
    *   a float encoding a color with the given properties
    */
  def floatGetHSL(hue: Float, saturation: Float, lightness: Float, opacity: Float): Float =
    if (lightness <= 0.001f) {
      java.lang.Float.intBitsToFloat(((opacity * 255f).toInt << 24 & 0xfe000000) | 0x7f7f00)
    } else {
      fromRGBA(FloatColors.hsl2rgb(hue, saturation, lightness, opacity))
    }

  /** Gets the saturation of the given encoded color, as a float ranging from 0.0f to 1.0f, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by `ycwcm(Float, Float, Float, Float)`
    * @return
    *   the saturation of the color from 0.0 (a grayscale color; inclusive) to 1.0 (a bright color, inclusive)
    */
  def saturation(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val lu      = decoded & 0xff
    val cw      = (decoded >>> 7 & 0x1fe) - 0xff
    val cm      = ((decoded >>> 15 & 0x1fe) - 0xff) >> 1
    // 0x1.010102p-8f = 0.003921569f
    val r = Math.min(Math.max(lu + (cw * 5 >> 3) - cm, 0), 0xff) * 0.003921569f
    val g = Math.min(Math.max(lu - (cw * 3 >> 3) + cm, 0), 0xff) * 0.003921569f
    val b = Math.min(Math.max(lu - (cw * 3 >> 3) - cm, 0), 0xff) * 0.003921569f
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
    val d  = x - Math.min(w, y)
    val li = x * (1f - 0.5f * d / (x + 1e-10f))
    (x - li) / (Math.min(li, 1f - li) + 1e-10f)
  }

  /** Gets the lightness of the given encoded color, as a float ranging from 0.0f to 1.0f, inclusive.
    * @param encoded
    *   a color as a packed float that can be obtained by `ycwcm(Float, Float, Float, Float)`
    * @return
    *   the lightness of the color from 0.0 (black, inclusive) to 1.0 (white, inclusive)
    */
  def lightness(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val lu      = decoded & 0xff
    val cw      = (decoded >>> 7 & 0x1fe) - 0xff
    val cm      = ((decoded >>> 15 & 0x1fe) - 0xff) >> 1
    // 0x1.010102p-8f = 0.003921569f
    val r = Math.min(Math.max(lu + (cw * 5 >> 3) - cm, 0), 0xff) * 0.003921569f
    val g = Math.min(Math.max(lu - (cw * 3 >> 3) + cm, 0), 0xff) * 0.003921569f
    val b = Math.min(Math.max(lu - (cw * 3 >> 3) - cm, 0), 0xff) * 0.003921569f
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
    val d = x - Math.min(w, y)
    x * (1f - 0.5f * d / (x + 1e-10f))
  }

  /** Gets the hue of the given encoded color, as a float from 0f (inclusive, red and approaching orange if increased) to 1f (exclusive, red and approaching purple if decreased).
    * @param encoded
    *   a color as a packed float that can be obtained by `ycwcm(Float, Float, Float, Float)`
    * @return
    *   The hue of the color from 0.0 (red, inclusive) towards orange, then yellow, and eventually to purple before looping back to almost the same red (1.0, exclusive)
    */
  def hue(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val lu      = decoded & 0xff
    val cw      = (decoded >>> 7 & 0x1fe) - 0xff
    val cm      = ((decoded >>> 15 & 0x1fe) - 0xff) >> 1
    // 0x1.010102p-8f = 0.003921569f
    val r = Math.min(Math.max(lu + (cw * 5 >> 3) - cm, 0), 0xff) * 0.003921569f
    val g = Math.min(Math.max(lu - (cw * 3 >> 3) + cm, 0), 0xff) * 0.003921569f
    val b = Math.min(Math.max(lu - (cw * 3 >> 3) - cm, 0), 0xff) * 0.003921569f
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
    val d = x - Math.min(w, y)
    Math.abs(z + (w - y) / (6f * d + 1e-10f))
  }

  /** The "luma" of the given packed float in YCwCm format, which is like its lightness; ranges from 0.0f to 1.0f . YCwCm is useful for modifications to colors: you can get a grayscale version of a
    * color by setting Cw and Cm to 0.5, you can desaturate by subtracting 0.5, multiplying Cw and Cm by a number between 0 and 1, and adding 0.5 afterwards, you can oversaturate by subtracting 0.5,
    * multiplying Cw and Cm by a number greater than 1, and adding 0.5 afterwards, you can lighten or darken by increasing or decreasing luma, and so on and so forth.
    * @param encoded
    *   a packed float
    * @return
    *   the luma as a float from 0.0f to 1.0f
    */
  def luma(encoded: Float): Float =
    // 0x1.010102p-8f = 0.003921569f
    (java.lang.Float.floatToRawIntBits(encoded) & 0xff) * 0.003921569f

  /** The "chroma warm" of the given packed float in YCwCm format, which when combined with chroma mild describes the shade and saturation of a color; ranges from 0f to 1f . YCwCm is useful for
    * modifications to colors: you can get a grayscale version of a color by setting Cw and Cm to 0.5, you can desaturate by subtracting 0.5, multiplying Cw and Cm by a number between 0 and 1, and
    * adding 0.5 afterwards, you can oversaturate by subtracting 0.5, multiplying Cw and Cm by a number greater than 1, and adding 0.5 afterwards, you can lighten or darken by increasing or decreasing
    * luma, and so on and so forth.
    * @param encoded
    *   a color encoded as a packed float, as by `ycwcm(Float, Float, Float, Float)`
    * @return
    *   the chroma warm as a float from 0f to 1f
    */
  def chromaWarm(encoded: Float): Float =
    // 0x1.010102p-8f = 0.003921569f
    (java.lang.Float.floatToRawIntBits(encoded) >>> 8 & 0xff) * 0.003921569f

  /** The "chroma mild" of the given packed float in YCwCm format, which when combined with chroma warm describes the shade and saturation of a color; ranges from 0f to 1f . YCwCm is useful for
    * modifications to colors: you can get a grayscale version of a color by setting Cw and Cm to 0.5, you can desaturate by subtracting 0.5, multiplying Cw and Cm by a number between 0 and 1, and
    * adding 0.5 afterwards, you can oversaturate by subtracting 0.5, multiplying Cw and Cm by a number greater than 1, and adding 0.5 afterwards, you can lighten or darken by increasing or decreasing
    * luma, and so on and so forth.
    * @param encoded
    *   a color encoded as a packed float, as by `ycwcm(Float, Float, Float, Float)`
    * @return
    *   the chroma mild as a float from 0f to 1f
    */
  def chromaMild(encoded: Float): Float =
    // 0x1.010102p-8f = 0.003921569f
    (java.lang.Float.floatToRawIntBits(encoded) >>> 16 & 0xff) * 0.003921569f

  /** Gets a variation on the packed float color basis as another packed float that has its hue, saturation, lightness, and opacity adjusted by the specified amounts. Note that this edits the color in
    * HSL space, not YCwCm! Takes floats representing the amounts of change to apply to hue, saturation, lightness, and opacity; these can be between -1f and 1f. Returns a float that can be used as a
    * packed or encoded color. The float is likely to be different than the result of `ycwcm(Float, Float, Float, Float)` unless hue, saturation, lightness, and opacity are all 0. This won't allocate
    * any objects.
    *
    * The parameters this takes all specify additive changes for a color component, clamping the final values so they can't go above 1 or below 0, with an exception for hue, which can rotate around if
    * lower or higher hues would be used. As an example, if you give this 0.4f for saturation, and the current color has saturation 0.7f, then the resulting color will have 1f for saturation. If you
    * gave this -0.1f for saturation and the current color again has saturation 0.7f, then resulting color will have 0.6f for saturation.
    *
    * @param basis
    *   a packed float color that will be used as the starting point to make the next color
    * @param hue
    *   -1f to 1f, the hue change that can be applied to the new float color (not clamped, wraps)
    * @param saturation
    *   -1f to 1f, the saturation change that can be applied to the new float color
    * @param light
    *   -1f to 1f, the light/brightness change that can be applied to the new float color
    * @param opacity
    *   -1f to 1f, the opacity/alpha change that can be applied to the new float color
    * @return
    *   a float encoding a variation of basis with the given changes
    */
  def toEditedFloat(basis: Float, hue: Float, saturation: Float, light: Float, opacity: Float): Float = {
    val e = java.lang.Float.floatToRawIntBits(basis)
    // 0x1.020408p-8f = 0.003937008f, 0x1.010102p-8f = 0.003921569f
    val op = Math.min(Math.max(opacity + (e >>> 24 & 0xfe) * 0.003937008f, 0f), 1f)
    if (light + (e & 0xff) * 0.003921569f <= 0.001f) {
      java.lang.Float.intBitsToFloat(((op * 255f).toInt << 24 & 0xfe000000) | 0x7f7f00)
    } else {
      val lu = e & 0xff
      val cw = (e >>> 7 & 0x1fe) - 0xff
      val cm = ((e >>> 15 & 0x1fe) - 0xff) >> 1
      // 0x1.010102p-8f = 0.003921569f
      val r = Math.min(Math.max(lu + light + cw * 0.625f - cm, 0), 255f) * 0.003921569f
      val g = Math.min(Math.max(lu + light - cw * 0.375f + cm, 0), 255f) * 0.003921569f
      val b = Math.min(Math.max(lu + light - cw * 0.375f - cm, 0), 255f) * 0.003921569f
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

  /** Interpolates from the packed float color start towards white by change. While change should be between 0f (return start as-is) and 1f (return white), start should be a packed color, as from
    * `ycwcm(Float, Float, Float, Float)`. Unlike `FloatColors.lerpFloatColors(Float, Float, Float)`, this keeps the alpha and both chroma of start as-is.
    * @see
    *   `darken(Float, Float)` the counterpart method that darkens a float color
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to go from start toward white, as a float between 0 and 1; higher means closer to white
    * @return
    *   a packed float that represents a color between start and white
    */
  def lighten(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val i = s & 0xff; val other = s & 0xfeffff00
    java.lang.Float.intBitsToFloat(((i + (0xff - i) * change).toInt & 0xff) | other)
  }

  /** Interpolates from the packed float color start towards black by change. While change should be between 0f (return start as-is) and 1f (return black), start should be a packed color, as from
    * `ycwcm(Float, Float, Float, Float)`. Unlike `FloatColors.lerpFloatColors(Float, Float, Float)`, this keeps the alpha and both chroma of start as-is.
    * @see
    *   `lighten(Float, Float)` the counterpart method that lightens a float color
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to go from start toward black, as a float between 0 and 1; higher means closer to black
    * @return
    *   a packed float that represents a color between start and black
    */
  def darken(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val i = s & 0xff; val other = s & 0xfeffff00
    java.lang.Float.intBitsToFloat(((i * (1f - change)).toInt & 0xff) | other)
  }

  /** Interpolates from the packed float color start towards a warmer color (yellow to red) by change. While change should be between 0f (return start as-is) and 1f (return fully warmed), start should
    * be a packed color, as from `ycwcm(Float, Float, Float, Float)`. Unlike `FloatColors.lerpFloatColors(Float, Float, Float)`, this keeps the alpha and luma of start as-is.
    * @see
    *   `cool(Float, Float)` the counterpart method that cools a float color
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to warm start, as a float between 0 and 1; higher means a warmer result
    * @return
    *   a packed float that represents a color between start and a warmer color
    */
  def warm(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val warmth = s >>> 8 & 0xff; val other = s & 0xfeff00ff
    java.lang.Float.intBitsToFloat(((warmth + (0xff - warmth) * change).toInt << 8 & 0xff) | other)
  }

  /** Interpolates from the packed float color start towards a cooler color (green to blue) by change. While change should be between 0f (return start as-is) and 1f (return fully cooled), start should
    * be a packed color, as from `ycwcm(Float, Float, Float, Float)`. Unlike `FloatColors.lerpFloatColors(Float, Float, Float)`, this keeps the alpha and luma of start as-is.
    * @see
    *   `warm(Float, Float)` the counterpart method that warms a float color
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to cool start, as a float between 0 and 1; higher means a cooler result
    * @return
    *   a packed float that represents a color between start and a cooler color
    */
  def cool(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val warmth = s >>> 8 & 0xff; val other = s & 0xfeff00ff
    java.lang.Float.intBitsToFloat(((warmth * (1f - change)).toInt & 0xff) << 8 | other)
  }

  /** Interpolates from the packed float color start towards a milder color (between green and yellow) by change. While change should be between 0f (return start as-is) and 1f (return fully mild),
    * start should be a packed color, as from `ycwcm(Float, Float, Float, Float)`. Unlike `FloatColors.lerpFloatColors(Float, Float, Float)`, this keeps the alpha and luma of start as-is.
    * @see
    *   `strengthen(Float, Float)` the counterpart method that makes a float color more bold
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to change start to a milder color, as a float between 0 and 1; higher means a milder result
    * @return
    *   a packed float that represents a color between start and a milder color
    */
  def weaken(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val warmth = s >>> 8 & 0xff; val other = s & 0xfeff00ff
    java.lang.Float.intBitsToFloat(((warmth + (0xff - warmth) * change).toInt << 8 & 0xff) | other)
  }

  /** Interpolates from the packed float color start towards a bolder color (between blue and red) by change. While change should be between 0f (return start as-is) and 1f (return fully cooled), start
    * should be a packed color, as from `ycwcm(Float, Float, Float, Float)`. Unlike `FloatColors.lerpFloatColors(Float, Float, Float)`, this keeps the alpha and luma of start as-is.
    * @see
    *   `weaken(Float, Float)` the counterpart method that makes a float color more mild
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to change start to a bolder color, as a float between 0 and 1; higher means a bolder result
    * @return
    *   a packed float that represents a color between start and a bolder color
    */
  def strengthen(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val warmth = s >>> 8 & 0xff; val other = s & 0xfeff00ff
    java.lang.Float.intBitsToFloat(((warmth * (1f - change)).toInt & 0xff) << 8 | other)
  }

  /** Interpolates from the packed float color start towards that color made opaque by change. While change should be between 0f (return start as-is) and 1f (return start with full alpha), start
    * should be a packed color, as from `ycwcm(Float, Float, Float, Float)`. This won't change the luma, chroma warm, or chroma mild of the color.
    * @see
    *   `fade(Float, Float)` the counterpart method that makes a float color more translucent
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to go from start toward opaque, as a float between 0 and 1; higher means closer to opaque
    * @return
    *   a packed float that represents a color between start and its opaque version
    */
  def blot(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val opacity = s >>> 24 & 0xfe; val other = s & 0x00ffffff
    java.lang.Float.intBitsToFloat(((opacity + (0xfe - opacity) * change).toInt & 0xfe) << 24 | other)
  }

  /** Interpolates from the packed float color start towards transparent by change. While change should be between 0 (return start as-is) and 1f (return the color with 0 alpha), start should be a
    * packed color, as from `ycwcm(Float, Float, Float, Float)`. This won't change the luma, chroma warm, or chroma mild of the color.
    * @see
    *   `blot(Float, Float)` the counterpart method that makes a float color more opaque
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to go from start toward transparent, as a float between 0 and 1; higher means closer to transparent
    * @return
    *   a packed float that represents a color between start and transparent
    */
  def fade(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val opacity = s & 0xfe; val other = s & 0x00ffffff
    java.lang.Float.intBitsToFloat(((opacity * (1f - change)).toInt & 0xfe) << 24 | other)
  }

  /** Brings the chromatic components of `start` closer to grayscale by `change` (desaturating them). While change should be between 0f (return start as-is) and 1f (return fully gray), start should be
    * a packed color, as from `ycwcm(Float, Float, Float, Float)`. This only changes Cw and Cm; it leaves Y and alpha alone, unlike `lessenChange(Float, Float)`, which usually changes Y.
    * @see
    *   `enrich(Float, Float)` the counterpart method that makes a float color more saturated
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to change start to a desaturated color, as a float between 0 and 1; higher means a less saturated result
    * @return
    *   a packed float that represents a color between start and a desaturated color
    */
  def dullen(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start)
    ycwcm(
      (s & 0xff) / 255f,
      ((s >>> 8 & 0xff) / 255f - 0.5f) * (1f - change) + 0.5f,
      ((s >>> 16 & 0xff) / 255f - 0.5f) * (1f - change) + 0.5f,
      (s >>> 25) / 127f
    )
  }

  /** Pushes the chromatic components of `start` away from grayscale by change (saturating them). While change should be between 0f (return start as-is) and 1f (return maximally saturated), start
    * should be a packed color, as from `ycwcm(Float, Float, Float, Float)`. This usually changes only Cw and Cm, but higher values for `change` can force the color out of the gamut, which this
    * corrects using `limitToGamut(Float, Float, Float, Float)` (and that can change Y somewhat). If the color stays in-gamut, then Y won't change; alpha never changes.
    * @see
    *   `dullen(Float, Float)` the counterpart method that makes a float color less saturated
    * @param start
    *   the starting color as a packed float
    * @param change
    *   how much to change start to a saturated color, as a float between 0 and 1; higher means a more saturated result
    * @return
    *   a packed float that represents a color between start and a saturated color
    */
  def enrich(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start)
    limitToGamut(
      (s & 0xff) / 255f,
      ((s >>> 8 & 0xff) / 255f - 0.5f) * (1f + change) + 0.5f,
      ((s >>> 16 & 0xff) / 255f - 0.5f) * (1f + change) + 0.5f,
      (s >>> 25) / 127f
    )
  }

  /** Given a packed float YCwCm color `mainColor` and another YCwCm color that it should be made to contrast with, gets a packed float YCwCm color with roughly inverted luma but the same chromatic
    * channels and opacity (Cw and Cm are likely to be clamped if the result gets close to white or black). This won't ever produce black or other very dark colors, and also has a gap in the range it
    * produces for luma values between 0.5 and 0.55. That allows most of the colors this method produces to contrast well as a foreground when displayed on a background of `contrastingColor`, or vice
    * versa. This will leave the luma unchanged if the chromatic channels of the contrastingColor and those of the mainColor are already very different. This has nothing to do with the contrast
    * channel of the tweak in ColorfulBatch; where that part of the tweak can make too-similar lightness values further apart by just a little, this makes a modification on `mainColor` to maximize its
    * lightness difference from `contrastingColor` without losing its other qualities.
    * @param mainColor
    *   a packed float color, as produced by `ycwcm(Float, Float, Float, Float)`; this is the color that will be adjusted
    * @param contrastingColor
    *   a packed float color, as produced by `ycwcm(Float, Float, Float, Float)`; the adjusted mainColor will contrast with this
    * @return
    *   a different packed float color, based on mainColor but with potentially very different lightness
    */
  def inverseLightness(mainColor: Float, contrastingColor: Float): Float = {
    val bits         = java.lang.Float.floatToRawIntBits(mainColor)
    val contrastBits = java.lang.Float.floatToRawIntBits(contrastingColor)
    val lumaVal      = bits & 0xff
    val warmVal      = bits >>> 8 & 0xff
    val mildVal      = bits >>> 16 & 0xff
    val cLuma        = contrastBits & 0xff
    val cWarm        = contrastBits >>> 8 & 0xff
    val cMild        = contrastBits >>> 16 & 0xff
    if ((warmVal - cWarm) * (warmVal - cWarm) + (mildVal - cMild) * (mildVal - cMild) >= 0x10000) {
      mainColor
    } else {
      // 0x1.010102p-8f = 0.003921569f
      ycwcm(
        if (cLuma < 128) lumaVal * (0.45f / 255f) + 0.55f else 0.5f - lumaVal * (0.45f / 255f),
        warmVal / 255f,
        mildVal / 255f,
        0.003921569f * (bits >>> 24)
      )
    }
  }

  /** Given a packed float YCwCm color `mainColor` and another YCwCm color that it should be made to contrast with, gets a packed float YCwCm color with Y that should be quite different from
    * `contrastingColor`'s Y, but the same chromatic channels and opacity (Cw and Cm are likely to be clamped if the result gets close to white or black). This allows most of the colors this method
    * produces to contrast well as a foreground when displayed on a background of `contrastingColor`, or vice versa.
    *
    * This is similar to `inverseLightness(Float, Float)`, but is considerably simpler, and this method will change the lightness of mainColor when the two given colors have close lightness but
    * distant chroma. Because it averages the original Y of mainColor with the modified one, this tends to not produce harsh color changes.
    * @param mainColor
    *   a packed YCwCm float color; this is the color that will be adjusted
    * @param contrastingColor
    *   a packed YCwCm float color; the adjusted mainColor will contrast with the Y of this
    * @return
    *   a different packed YCwCm float color, based on mainColor but typically with different lightness
    */
  def differentiateLightness(mainColor: Float, contrastingColor: Float): Float = {
    val main     = java.lang.Float.floatToRawIntBits(mainColor)
    val contrast = java.lang.Float.floatToRawIntBits(contrastingColor)
    limitToGamut(java.lang.Float.intBitsToFloat((main & 0xfeffff00) | (contrast + 128 & 0xff) + (main & 0xff) >>> 1))
  }

  /** Pretty simple; adds 0.5 to the given color's Y and wraps it around if it would go above 1.0, then averages that with the original Y. This means light colors become darker, and dark colors become
    * lighter, with almost all results in the middle-range of possible lightness.
    * @param mainColor
    *   a packed YCwCm float color
    * @return
    *   a different packed YCwCm float color, with its Y channel changed and limited to the correct gamut
    */
  def offsetLightness(mainColor: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(mainColor)
    limitToGamut(java.lang.Float.intBitsToFloat((decoded & 0xfeffff00) | (decoded + 128 & 0xff) + (decoded & 0xff) >>> 1))
  }

  /** Makes the additive YCwCm color stored in `color` cause less of a change when used as a tint, as if it were mixed with neutral gray. When `fraction` is 1.0, this returns color unchanged; when
    * fraction is 0.0, it returns `Palette.GRAY`, and when it is in-between 0.0 and 1.0 it returns something between the two. This is meant for things like area of effect abilities that make smaller
    * color changes toward their periphery.
    * @param color
    *   a color that should have its tinting effect potentially weakened
    * @param fraction
    *   how much of `color` should be kept, from 0.0 to 1.0
    * @return
    *   a YCwCm float color between gray and `color`
    */
  def lessenChange(color: Float, fraction: Float): Float = {
    val e  = java.lang.Float.floatToRawIntBits(color)
    val ys = 0x7f; val cws     = 0x7f; val cms             = 0x7f
    val ye = e & 0xff; val cwe = (e >>> 8) & 0xff; val cme = (e >>> 16) & 0xff; val ae = e >>> 24 & 0xfe
    java.lang.Float.intBitsToFloat(
      ((ys + fraction * (ye - ys)).toInt & 0xff)
        | (((cws + fraction * (cwe - cws)).toInt & 0xff) << 8)
        | (((cms + fraction * (cme - cms)).toInt & 0xff) << 16)
        | (ae << 24)
    )
  }

  /** Makes a quasi-randomly-edited variant on the given `color`, allowing typically a small amount of `variance` (such as 0.05 to 0.25) between the given color and what this can return. The `seed`
    * should be different each time this is called, and can be obtained from a random number generator to make the colors more random, or can be incremented on each call. If the seed is only
    * incremented or decremented, then this shouldn't produce two similar colors in a row unless variance is very small. The variance affects the Y, Cw, and Cm of the generated color, and each of
    * those channels can go up or down by the given variance as long as the total distance isn't greater than the variance (this considers Cw and Cm extra-wide, going from -1 to 1, while Y goes from 0
    * to 1, but only internally for measuring distance).
    * @param color
    *   a packed float color, as produced by `ycwcm(Float, Float, Float, Float)`
    * @param seed
    *   a long seed that should be different on each call; should not be 0
    * @param variance
    *   max amount of difference between the given color and the generated color; always less than 1
    * @return
    *   a generated packed float color that should be at least somewhat different from `color`
    */
  def randomEdit(color: Float, seed: Long, variance: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(color)
    val y       = (decoded & 0xff) / 255f
    val cw      = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val cm      = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val limit   = variance * variance
    var sd      = seed
    var dist    = limit + 1f
    var a       = 0f
    var b       = 0f
    var c       = 0f
    // 0x7FFFFFp-1f = 4194303.5f, 0x1p-22f = 2.3841858e-7f
    while (dist > limit) {
      a = (((sd * 0xd1b54a32d192ed03L >>> 41) - 4194303.5f) * 2.3841858e-7f) * variance
      b = (((sd * 0xabc98388fb8fac03L >>> 41) - 4194303.5f) * 2.3841858e-7f) * variance
      c = (((sd * 0x8cb92ba72f3d8dd7L >>> 41) - 4194303.5f) * 2.3841858e-7f) * variance
      sd += 0x9e3779b97f4a7c15L
      dist = a * a + b * b + c * c
    }
    java.lang.Float.intBitsToFloat(
      (decoded & 0xfe000000) | ((Math.min(Math.max(cm + c, -1), 1) * 127.5f + 128f).toInt << 16 & 0xff0000)
        | ((Math.min(Math.max(cw + b, -1), 1) * 127.5f + 128f).toInt << 8 & 0xff00) | (Math.min(Math.max(y + a, 0), 1) * 255f).toInt
    )
  }

  /** Returns true if the given packed float color, as YCwCm, is valid to convert losslessly back to RGBA.
    * @param packed
    *   a packed float color as YCwCm
    * @return
    *   true if the given packed float color can be converted back and forth to RGBA
    */
  def inGamut(packed: Float): Boolean = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val yi      = decoded & 0xff
    val cwi     = (decoded >>> 7 & 0x1fe) - 0xff
    val cmi     = ((decoded >>> 15 & 0x1fe) - 0xff) / 2
    val r       = yi + (cwi * 5 / 8) - cmi
    if (r < 0 || r > 255) {
      false
    } else {
      val g = yi - (cwi * 3 / 8) + cmi
      if (g < 0 || g > 255) {
        false
      } else {
        val b = yi - (cwi * 3 / 8) - cmi
        (b >= 0) && (b <= 255)
      }
    }
  }

  /** Returns true if the given YCwCm values are valid to convert losslessly back to RGBA.
    * @param y
    *   luma channel, as a float from 0 to 1
    * @param cw
    *   chromatic warmth channel, as a float from 0 to 1
    * @param cm
    *   chromatic mildness channel, as a float from 0 to 1
    * @return
    *   true if the given packed float color can be converted back and forth to RGBA
    */
  def inGamut(y: Float, cw: Float, cm: Float): Boolean = {
    val yi  = (y * 255.999f).toInt
    val cwi = ((cw - 0.5f) * 511.999f).toInt
    val cmi = ((cm - 0.5f) * 255.999f).toInt
    val r   = yi + (cwi * 5 / 8) - cmi
    if (r < 0 || r > 255) {
      false
    } else {
      val g = yi - (cwi * 3 / 8) + cmi
      if (g < 0 || g > 255) {
        false
      } else {
        val b = yi - (cwi * 3 / 8) - cmi
        (b >= 0) && (b <= 255)
      }
    }
  }

  /** Iteratively checks whether the given YCwCm color is in-gamut, and either brings the color closer to 50% gray if it isn't in-gamut, or returns it as soon as it is in-gamut.
    * @param packed
    *   a packed float color in YCwCm format; often this color is not in-gamut
    * @return
    *   the first color this finds that is between the given YCwCm color and 50% gray, and is in-gamut
    * @see
    *   `inGamut(Float)` You can use inGamut() if you just want to check whether a color is in-gamut.
    */
  def limitToGamut(packed: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val y       = (decoded & 0xff) / 255f
    val cw      = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val cm      = ((decoded >>> 16 & 0xff) - 127.5f) / 255f
    var cw2     = cw
    var cm2     = cm
    // 0x1p-5f = 0.03125f
    boundary[Float] {
      var attempt = 31
      while (attempt >= 0) {
        val r = y + 0.625f * cw2 - cm2
        val g = y - 0.375f * cw2 + cm2
        val b = y - 0.375f * cw2 - cm2
        if (r >= 0f && r <= 1f && g >= 0f && g <= 1f && b >= 0f && b <= 1f) {
          break(ycwcm(y, cw2 * 0.5f + 0.5f, cm2 * 0.5f + 0.5f, (decoded >>> 25) / 127f))
        }
        val progress = attempt * 0.03125f
        cw2 = MathUtils.lerp(0, cw, progress)
        cm2 = MathUtils.lerp(0, cm, progress)
        attempt -= 1
      }
      ycwcm(y, cw2 * 0.5f + 0.5f, cm2 * 0.5f + 0.5f, (decoded >>> 25) / 127f)
    }
  }

  /** Iteratively checks whether the given YCwCm color is in-gamut, and either brings the color closer to 50% gray if it isn't in-gamut, or returns it as soon as it is in-gamut. This always produces
    * an opaque color.
    * @param y
    *   luma component; will be clamped between 0 and 1 if it isn't already
    * @param cw
    *   chromatic warmth component; will be clamped between 0 and 1 if it isn't already
    * @param cm
    *   chromatic mildness component; will be clamped between 0 and 1 if it isn't already
    * @return
    *   the first color this finds that is between the given YCwCm color and 50% gray, and is in-gamut
    * @see
    *   `inGamut(Float, Float, Float)` You can use inGamut() if you just want to check whether a color is in-gamut.
    */
  def limitToGamut(y: Float, cw: Float, cm: Float): Float =
    limitToGamut(y, cw, cm, 1f)

  /** Iteratively checks whether the given YCwCm color is in-gamut, and either brings the color closer to 50% gray if it isn't in-gamut, or returns it as soon as it is in-gamut.
    * @param y
    *   luma component; will be clamped between 0 and 1 if it isn't already
    * @param cw
    *   chromatic warmth component; will be clamped between 0 and 1 if it isn't already
    * @param cm
    *   chromatic mildness component; will be clamped between 0 and 1 if it isn't already
    * @param a
    *   alpha component; will be clamped between 0 and 1 if it isn't already
    * @return
    *   the first color this finds that is between the given YCwCm color and 50% gray, and is in-gamut
    * @see
    *   `inGamut(Float, Float, Float)` You can use inGamut() if you just want to check whether a color is in-gamut.
    */
  def limitToGamut(y: Float, cw: Float, cm: Float, a: Float): Float = {
    val y2     = Math.min(Math.max(y, 0f), 1f)
    var cwCurr = Math.min(Math.max((cw - 0.5f) * 2f, -1f), 1f)
    var cmCurr = Math.min(Math.max((cm - 0.5f) * 2f, -1f), 1f)
    val cwOrig = cwCurr
    val cmOrig = cmCurr
    // 0x1p-5f = 0.03125f
    boundary[Float] {
      var attempt = 31
      while (attempt >= 0) {
        val r = y2 + 0.625f * cwCurr - cmCurr
        val g = y2 - 0.375f * cwCurr + cmCurr
        val b = y2 - 0.375f * cwCurr - cmCurr
        if (r >= 0f && r <= 1f && g >= 0f && g <= 1f && b >= 0f && b <= 1f) {
          break(ycwcm(y2, cwCurr * 0.5f + 0.5f, cmCurr * 0.5f + 0.5f, Math.min(Math.max(a, 0f), 1f)))
        }
        val progress = attempt * 0.03125f
        cwCurr = MathUtils.lerp(0, cwOrig, progress)
        cmCurr = MathUtils.lerp(0, cmOrig, progress)
        attempt -= 1
      }
      ycwcm(y2, cwCurr * 0.5f + 0.5f, cmCurr * 0.5f + 0.5f, Math.min(Math.max(a, 0f), 1f))
    }
  }

  /** Given a packed float YCwCm color, this edits its luma (Y), chromatic warmth (Cw), chromatic mildness (Cm), and alpha channels by adding the corresponding "add" parameter and then clamping. This
    * returns a different float value (of course, the given float can't be edited in-place). You can give a value of 0 for any "add" parameter you want to stay unchanged. This clamps the resulting
    * color to remain in-gamut, so it should be safe to convert it back to RGBA.
    * @param encoded
    *   a packed float YCwCm color
    * @param addY
    *   how much to add to the luma channel; typically in the -1 to 1 range
    * @param addCw
    *   how much to add to the chromatic warmth channel; typically in the -2 to 2 range
    * @param addCm
    *   how much to add to the chromatic mildness channel; typically in the -2 to 2 range
    * @param addAlpha
    *   how much to add to the alpha channel; typically in the -1 to 1 range
    * @return
    *   a packed float YCwCm color with the requested edits applied to `encoded`
    */
  def editYCwCm(encoded: Float, addY: Float, addCw: Float, addCm: Float, addAlpha: Float): Float =
    editYCwCm(encoded, addY, addCw, addCm, addAlpha, 1f, 1f, 1f, 1f)

  /** Given a packed float YCwCm color, this edits its luma (Y), chromatic warmth (Cw), chromatic mildness (Cm), and alpha channels by first multiplying each channel by the corresponding "mul"
    * parameter and then adding the corresponding "add" parameter, before clamping. This means the luma value is multiplied by `mulY`, then has `addY` added, and then is clamped to the normal range
    * for luma (0 to 1). This returns a different float value (of course, the given float can't be edited in-place). You can give a value of 0 for any "add" parameter you want to stay unchanged, or a
    * value of 1 for any "mul" parameter that shouldn't change. Note that this manipulates chromatic warmth and mildness in the -1 to 1 range, so if you multiply by a small number like `0.25f`, then
    * this will produce a less-saturated color, and if you multiply by a larger number like `4f`, then you will get a much more-saturated color. This clamps the resulting color to remain in-gamut, so
    * it should be safe to convert it back to RGBA.
    * @param encoded
    *   a packed float YCwCm color
    * @param addY
    *   how much to add to the luma channel; typically in the -1 to 1 range
    * @param addCw
    *   how much to add to the chromatic warmth channel; typically in the -2 to 2 range
    * @param addCm
    *   how much to add to the chromatic mildness channel; typically in the -2 to 2 range
    * @param addAlpha
    *   how much to add to the alpha channel; typically in the -1 to 1 range
    * @param mulY
    *   how much to multiply the luma channel by; should be non-negative
    * @param mulCw
    *   how much to multiply the chromatic warmth channel by; usually non-negative (not always)
    * @param mulCm
    *   how much to multiply the chromatic mildness channel by; usually non-negative (not always)
    * @param mulAlpha
    *   how much to multiply the alpha channel by; should be non-negative
    * @return
    *   a packed float YCwCm color with the requested edits applied to `encoded`
    */
  def editYCwCm(
    encoded:  Float,
    addY:     Float,
    addCw:    Float,
    addCm:    Float,
    addAlpha: Float,
    mulY:     Float,
    mulCw:    Float,
    mulCm:    Float,
    mulAlpha: Float
  ): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val yVal    = (decoded & 0xff) / 255f
    val cwVal   = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val cmVal   = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    var al      = (decoded >>> 25) / 127f

    val y2     = Math.min(Math.max(yVal * mulY + addY, 0f), 1f)
    var cwCurr = Math.min(Math.max(cwVal * mulCw + addCw, -1f), 1f)
    var cmCurr = Math.min(Math.max(cmVal * mulCm + addCm, -1f), 1f)
    val cwOrig = cwCurr
    val cmOrig = cmCurr
    al = Math.min(Math.max(al * mulAlpha + addAlpha, 0f), 1f)
    // 0x1p-5f = 0.03125f
    boundary[Float] {
      var attempt = 31
      while (attempt >= 0) {
        val r = y2 + 0.625f * cwCurr - cmCurr
        val g = y2 - 0.375f * cwCurr + cmCurr
        val b = y2 - 0.375f * cwCurr - cmCurr
        if (r >= 0f && r <= 1f && g >= 0f && g <= 1f && b >= 0f && b <= 1f) {
          break(ycwcm(y2, cwCurr * 0.5f + 0.5f, cmCurr * 0.5f + 0.5f, al))
        }
        val progress = attempt * 0.03125f
        cwCurr = MathUtils.lerp(0, cwOrig, progress)
        cmCurr = MathUtils.lerp(0, cmOrig, progress)
        attempt -= 1
      }
      ycwcm(y2, cwCurr * 0.5f + 0.5f, cmCurr * 0.5f + 0.5f, al)
    }
  }

  /** Produces a random packed float color that is always in-gamut and should be uniformly distributed.
    * @param random
    *   a Random object (or preferably a subclass of Random)
    * @return
    *   a random opaque packed float color that is always in-gamut
    */
  def randomColor(random: java.util.Random): Float = {
    val yr = +0.375f; val wr           = +0.5f; val mr             = +0.0f
    val yg = +0.500f; val wg           = +0.0f; val mg             = +0.5f
    val yb = +0.125f; val wb           = -0.5f; val mb             = -0.5f
    val r  = random.nextFloat(); val g = random.nextFloat(); val b = random.nextFloat()
    java.lang.Float.intBitsToFloat(
      0xfe000000
        | ((mr * r + mg * g + mb * b) * 128f + 128f).toInt << 16 & 0xff0000
        | ((wr * r + wg * g + wb * b) * 128f + 128f).toInt << 8 & 0xff00
        | ((yr * r + yg * g + yb * b) * 256f).toInt & 0xff
    )
  }
}
