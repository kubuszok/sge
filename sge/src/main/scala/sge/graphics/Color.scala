/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/Color.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Convention: mutable class retained for compatibility; opaque types (Pixels, WorldUnits, Seconds,
 *     Align, GL enums/handles) used elsewhere in SGE but Color itself remains a mutable RGBA container
 *     since it's a low-level primitive shared across the rendering pipeline
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import sge.math.MathUtils
import sge.utils.NumberUtils

/** A color class, holding the r, g, b and alpha component as floats in the range [0,1]. All methods perform clamping on the internal values after execution.
  *
  * @author
  *   mzechner (original implementation)
  */
class Color(var r: Float = 0f, var g: Float = 0f, var b: Float = 0f, var a: Float = 0f) {

  /** @see #rgba8888ToColor(Color, int) */
  def this(rgba8888: Int) = {
    this()
    Color.rgba8888ToColor(this, rgba8888)
  }

  /** Constructs a new color using the given color
    *
    * @param color
    *   the color
    */
  def this(color: Color) = {
    this()
    set(color)
  }

  clamp()

  /** Sets this color to the given color.
    *
    * @param color
    *   the Color
    * @return
    *   this color.
    */
  def set(color: Color): Color = {
    this.r = color.r
    this.g = color.g
    this.b = color.b
    this.a = color.a
    this
  }

  /** Sets this color to the red, green and blue components of the provided Color and a deviating alpha value.
    *
    * @param rgb
    *   the desired red, green and blue values (alpha of that Color is ignored)
    * @param alpha
    *   the desired alpha value (will be clamped to the range [0, 1])
    * @return
    *   this color.
    */
  def set(rgb: Color, alpha: Float): Color = {
    this.r = rgb.r
    this.g = rgb.g
    this.b = rgb.b
    this.a = MathUtils.clamp(alpha, 0f, 1f)
    this
  }

  /** Multiplies this color and the given color
    *
    * @param color
    *   the color
    * @return
    *   this color.
    */
  def mul(color: Color): Color = {
    this.r *= color.r
    this.g *= color.g
    this.b *= color.b
    this.a *= color.a
    clamp()
  }

  /** Multiplies all components of this Color with the given value.
    *
    * @param value
    *   the value
    * @return
    *   this color
    */
  def mul(value: Float): Color = {
    this.r *= value
    this.g *= value
    this.b *= value
    this.a *= value
    clamp()
  }

  /** Adds the given color to this color.
    *
    * @param color
    *   the color
    * @return
    *   this color
    */
  def add(color: Color): Color = {
    this.r += color.r
    this.g += color.g
    this.b += color.b
    this.a += color.a
    clamp()
  }

  /** Subtracts the given color from this color
    *
    * @param color
    *   the color
    * @return
    *   this color
    */
  def sub(color: Color): Color = {
    this.r -= color.r
    this.g -= color.g
    this.b -= color.b
    this.a -= color.a
    clamp()
  }

  /** Clamps this Color's components to a valid range [0 - 1]
    * @return
    *   this Color for chaining
    */
  def clamp(): Color = {
    if (r < 0)
      r = 0
    else if (r > 1) r = 1

    if (g < 0)
      g = 0
    else if (g > 1) g = 1

    if (b < 0)
      b = 0
    else if (b > 1) b = 1

    if (a < 0)
      a = 0
    else if (a > 1) a = 1
    this
  }

  /** Sets this Color's component values.
    *
    * @param r
    *   Red component
    * @param g
    *   Green component
    * @param b
    *   Blue component
    * @param a
    *   Alpha component
    *
    * @return
    *   this Color for chaining
    */
  def set(r: Float, g: Float, b: Float, a: Float): Color = {
    this.r = r
    this.g = g
    this.b = b
    this.a = a
    clamp()
  }

  /** Sets this color's component values through an integer representation.
    *
    * @return
    *   this Color for chaining
    * @see
    *   #rgba8888ToColor(Color, int)
    */
  def set(rgba: Int): Color = {
    Color.rgba8888ToColor(this, rgba)
    this
  }

  /** Adds the given color component values to this Color's values.
    *
    * @param r
    *   Red component
    * @param g
    *   Green component
    * @param b
    *   Blue component
    * @param a
    *   Alpha component
    *
    * @return
    *   this Color for chaining
    */
  def add(r: Float, g: Float, b: Float, a: Float): Color = {
    this.r += r
    this.g += g
    this.b += b
    this.a += a
    clamp()
  }

  /** Subtracts the given values from this Color's component values.
    *
    * @param r
    *   Red component
    * @param g
    *   Green component
    * @param b
    *   Blue component
    * @param a
    *   Alpha component
    *
    * @return
    *   this Color for chaining
    */
  def sub(r: Float, g: Float, b: Float, a: Float): Color = {
    this.r -= r
    this.g -= g
    this.b -= b
    this.a -= a
    clamp()
  }

  /** Multiplies this Color's color components by the given ones.
    *
    * @param r
    *   Red component
    * @param g
    *   Green component
    * @param b
    *   Blue component
    * @param a
    *   Alpha component
    *
    * @return
    *   this Color for chaining
    */
  def mul(r: Float, g: Float, b: Float, a: Float): Color = {
    this.r *= r
    this.g *= g
    this.b *= b
    this.a *= a
    clamp()
  }

  /** Linearly interpolates between this color and the target color by t which is in the range [0,1]. The result is stored in this color.
    * @param target
    *   The target color
    * @param t
    *   The interpolation coefficient
    * @return
    *   This color for chaining.
    */
  def lerp(target: Color, t: Float): Color = {
    this.r += t * (target.r - this.r)
    this.g += t * (target.g - this.g)
    this.b += t * (target.b - this.b)
    this.a += t * (target.a - this.a)
    clamp()
  }

  /** Linearly interpolates between this color and the target color by t which is in the range [0,1]. The result is stored in this color.
    * @param r
    *   The red component of the target color
    * @param g
    *   The green component of the target color
    * @param b
    *   The blue component of the target color
    * @param a
    *   The alpha component of the target color
    * @param t
    *   The interpolation coefficient
    * @return
    *   This color for chaining.
    */
  def lerp(r: Float, g: Float, b: Float, a: Float, t: Float): Color = {
    this.r += t * (r - this.r)
    this.g += t * (g - this.g)
    this.b += t * (b - this.b)
    this.a += t * (a - this.a)
    clamp()
  }

  /** Multiplies the RGB values by the alpha. */
  def premultiplyAlpha(): Color = {
    r *= a
    g *= a
    b *= a
    this
  }

  override def equals(o: Any): Boolean = o match {
    case color: Color => (this eq color) || toIntBits() == color.toIntBits()
    case _ => false
  }

  override def hashCode(): Int = {
    var result = if (r != +0.0f) NumberUtils.floatToIntBits(r) else 0
    result = 31 * result + (if (g != +0.0f) NumberUtils.floatToIntBits(g) else 0)
    result = 31 * result + (if (b != +0.0f) NumberUtils.floatToIntBits(b) else 0)
    result = 31 * result + (if (a != +0.0f) NumberUtils.floatToIntBits(a) else 0)
    result
  }

  /** Packs the color components into a 32-bit integer with the format ABGR and then converts it to a float. Alpha is compressed from 0-255 to use only even numbers between 0-254 to avoid using float
    * bits in the NaN range (see {@link NumberUtils#intToFloatColor(int)} ). Converting a color to a float and back can be lossy for alpha.
    * @return
    *   the packed color as a 32-bit float
    */
  def toFloatBits(): Float = {
    val color = ((a * 255).toInt << 24) | ((b * 255).toInt << 16) | ((g * 255).toInt << 8) | (r * 255).toInt
    NumberUtils.intToFloatColor(color)
  }

  /** Packs the color components into a 32-bit integer with the format ABGR.
    * @return
    *   the packed color as a 32-bit int.
    */
  def toIntBits(): Int =
    ((a * 255).toInt << 24) | ((b * 255).toInt << 16) | ((g * 255).toInt << 8) | (r * 255).toInt

  /** Returns the color encoded as hex string with the format RRGGBBAA. */
  override def toString: String = {
    var value = Integer.toHexString(((r * 255).toInt << 24) | ((g * 255).toInt << 16) | ((b * 255).toInt << 8) | (a * 255).toInt)
    while (value.length < 8)
      value = "0" + value
    value
  }

  /** Sets RGB components using the specified Hue-Saturation-Value. Note that HSV components are voluntary not clamped to preserve high range color and can range beyond typical values.
    * @param h
    *   The Hue in degree from 0 to 360
    * @param s
    *   The Saturation from 0 to 1
    * @param v
    *   The Value (brightness) from 0 to 1
    * @return
    *   The modified Color for chaining.
    */
  def fromHsv(h: Float, s: Float, v: Float): Color = {
    val x = (h / 60f + 6) % 6
    val i = x.toInt
    val f = x - i
    val p = v * (1 - s)
    val q = v * (1 - s * f)
    val t = v * (1 - s * (1 - f))
    i match {
      case 0 =>
        r = v
        g = t
        b = p
      case 1 =>
        r = q
        g = v
        b = p
      case 2 =>
        r = p
        g = v
        b = t
      case 3 =>
        r = p
        g = q
        b = v
      case 4 =>
        r = t
        g = p
        b = v
      case _ =>
        r = v
        g = p
        b = q
    }

    clamp()
  }

  /** Sets RGB components using the specified Hue-Saturation-Value. This is a convenient method for {@link #fromHsv(float, float, float)} . This is the inverse of {@link #toHsv(float[])} .
    * @param hsv
    *   The Hue, Saturation and Value components in that order.
    * @return
    *   The modified Color for chaining.
    */
  def fromHsv(hsv: Array[Float]): Color =
    fromHsv(hsv(0), hsv(1), hsv(2))

  /** Extract Hue-Saturation-Value. This is the inverse of {@link #fromHsv(float[])} .
    * @param hsv
    *   The HSV array to be modified.
    * @return
    *   HSV components for chaining.
    */
  def toHsv(hsv: Array[Float]): Array[Float] = {
    val max   = Math.max(Math.max(r, g), b)
    val min   = Math.min(Math.min(r, g), b)
    val range = max - min
    if (range == 0) {
      hsv(0) = 0
    } else if (max == r) {
      hsv(0) = (60 * (g - b) / range + 360) % 360
    } else if (max == g) {
      hsv(0) = 60 * (b - r) / range + 120
    } else {
      hsv(0) = 60 * (r - g) / range + 240
    }

    if (max > 0) {
      hsv(1) = 1 - min / max
    } else {
      hsv(1) = 0
    }

    hsv(2) = max

    hsv
  }

  /** @return a copy of this color */
  def cpy(): Color =
    Color(this)
}

object Color {
  val WHITE      = Color(1, 1, 1, 1)
  val LIGHT_GRAY = Color(0xbfbfbfff)
  val GRAY       = Color(0x7f7f7fff)
  val DARK_GRAY  = Color(0x3f3f3fff)
  val BLACK      = Color(0, 0, 0, 1)

  /** Convenience for frequently used <code>WHITE.toFloatBits()</code> */
  val WHITE_FLOAT_BITS = WHITE.toFloatBits()

  val CLEAR       = Color(0, 0, 0, 0)
  val CLEAR_WHITE = Color(1, 1, 1, 0)

  val BLUE  = Color(0, 0, 1, 1)
  val NAVY  = Color(0, 0, 0.5f, 1)
  val ROYAL = Color(0x4169e1ff)
  val SLATE = Color(0x708090ff)
  val SKY   = Color(0x87ceebff)
  val CYAN  = Color(0, 1, 1, 1)
  val TEAL  = Color(0, 0.5f, 0.5f, 1)

  val GREEN      = Color(0x00ff00ff)
  val CHARTREUSE = Color(0x7fff00ff)
  val LIME       = Color(0x32cd32ff)
  val FOREST     = Color(0x228b22ff)
  val OLIVE      = Color(0x6b8e23ff)

  val YELLOW    = Color(0xffff00ff)
  val GOLD      = Color(0xffd700ff)
  val GOLDENROD = Color(0xdaa520ff)
  val ORANGE    = Color(0xffa500ff)

  val BROWN     = Color(0x8b4513ff)
  val TAN       = Color(0xd2b48cff)
  val FIREBRICK = Color(0xb22222ff)

  val RED     = Color(0xff0000ff)
  val SCARLET = Color(0xff341cff)
  val CORAL   = Color(0xff7f50ff)
  val SALMON  = Color(0xfa8072ff)
  val PINK    = Color(0xff69b4ff)
  val MAGENTA = Color(1, 0, 1, 1)

  val PURPLE = Color(0xa020f0ff)
  val VIOLET = Color(0xee82eeff)
  val MAROON = Color(0xb03060ff)

  /** Returns a new color from a hex string with the format RRGGBBAA.
    * @see
    *   #toString()
    */
  def valueOf(hex: String): Color =
    valueOf(hex, Color())

  /** Sets the specified color from a hex string with the format RRGGBBAA.
    * @see
    *   #toString()
    */
  def valueOf(hex: String, color: Color): Color = {
    val hexValue = if (hex.charAt(0) == '#') hex.substring(1) else hex
    color.r = Integer.parseInt(hexValue.substring(0, 2), 16) / 255f
    color.g = Integer.parseInt(hexValue.substring(2, 4), 16) / 255f
    color.b = Integer.parseInt(hexValue.substring(4, 6), 16) / 255f
    color.a = if (hexValue.length != 8) 1 else Integer.parseInt(hexValue.substring(6, 8), 16) / 255f
    color
  }

  /** Packs the color components into a 32-bit integer with the format ABGR and then converts it to a float. Note that no range checking is performed for higher performance.
    * @param r
    *   the red component, 0 - 255
    * @param g
    *   the green component, 0 - 255
    * @param b
    *   the blue component, 0 - 255
    * @param a
    *   the alpha component, 0 - 255
    * @return
    *   the packed color as a float
    * @see
    *   NumberUtils#intToFloatColor(int)
    */
  def toFloatBits(r: Int, g: Int, b: Int, a: Int): Float = {
    val color = (a << 24) | (b << 16) | (g << 8) | r
    NumberUtils.intToFloatColor(color)
  }

  /** Packs the color components into a 32-bit integer with the format ABGR and then converts it to a float.
    * @return
    *   the packed color as a 32-bit float
    * @see
    *   NumberUtils#intToFloatColor(int)
    */
  def toFloatBits(r: Float, g: Float, b: Float, a: Float): Float = {
    val color = ((a * 255).toInt << 24) | ((b * 255).toInt << 16) | ((g * 255).toInt << 8) | (r * 255).toInt
    NumberUtils.intToFloatColor(color)
  }

  /** Packs the color components into a 32-bit integer with the format ABGR. Note that no range checking is performed for higher performance.
    * @param r
    *   the red component, 0 - 255
    * @param g
    *   the green component, 0 - 255
    * @param b
    *   the blue component, 0 - 255
    * @param a
    *   the alpha component, 0 - 255
    * @return
    *   the packed color as a 32-bit int
    */
  def toIntBits(r: Int, g: Int, b: Int, a: Int): Int =
    (a << 24) | (b << 16) | (g << 8) | r

  def alpha(alpha: Float): Int =
    (alpha * 255.0f).toInt

  def luminanceAlpha(luminance: Float, alpha: Float): Int =
    ((luminance * 255.0f).toInt << 8) | (alpha * 255).toInt

  def rgb565(r: Float, g: Float, b: Float): Int =
    ((r * 31).toInt << 11) | ((g * 63).toInt << 5) | (b * 31).toInt

  def rgba4444(r: Float, g: Float, b: Float, a: Float): Int =
    ((r * 15).toInt << 12) | ((g * 15).toInt << 8) | ((b * 15).toInt << 4) | (a * 15).toInt

  def rgb888(r: Float, g: Float, b: Float): Int =
    ((r * 255).toInt << 16) | ((g * 255).toInt << 8) | (b * 255).toInt

  def rgba8888(r: Float, g: Float, b: Float, a: Float): Int =
    ((r * 255).toInt << 24) | ((g * 255).toInt << 16) | ((b * 255).toInt << 8) | (a * 255).toInt

  def argb8888(a: Float, r: Float, g: Float, b: Float): Int =
    ((a * 255).toInt << 24) | ((r * 255).toInt << 16) | ((g * 255).toInt << 8) | (b * 255).toInt

  def rgb565(color: Color): Int =
    ((color.r * 31).toInt << 11) | ((color.g * 63).toInt << 5) | (color.b * 31).toInt

  def rgba4444(color: Color): Int =
    ((color.r * 15).toInt << 12) | ((color.g * 15).toInt << 8) | ((color.b * 15).toInt << 4) | (color.a * 15).toInt

  def rgb888(color: Color): Int =
    ((color.r * 255).toInt << 16) | ((color.g * 255).toInt << 8) | (color.b * 255).toInt

  def rgba8888(color: Color): Int =
    ((color.r * 255).toInt << 24) | ((color.g * 255).toInt << 16) | ((color.b * 255).toInt << 8) | (color.a * 255).toInt

  def argb8888(color: Color): Int =
    ((color.a * 255).toInt << 24) | ((color.r * 255).toInt << 16) | ((color.g * 255).toInt << 8) | (color.b * 255).toInt

  /** Sets the Color components using the specified integer value in the format RGB565. This is inverse to the rgb565(r, g, b) method.
    *
    * @param color
    *   The Color to be modified.
    * @param value
    *   An integer color value in RGB565 format.
    */
  def rgb565ToColor(color: Color, value: Int): Unit = {
    color.r = ((value & 0x0000f800) >>> 11) / 31f
    color.g = ((value & 0x000007e0) >>> 5) / 63f
    color.b = ((value & 0x0000001f) >>> 0) / 31f
  }

  /** Sets the Color components using the specified integer value in the format RGBA4444. This is inverse to the rgba4444(r, g, b, a) method.
    *
    * @param color
    *   The Color to be modified.
    * @param value
    *   An integer color value in RGBA4444 format.
    */
  def rgba4444ToColor(color: Color, value: Int): Unit = {
    color.r = ((value & 0x0000f000) >>> 12) / 15f
    color.g = ((value & 0x00000f00) >>> 8) / 15f
    color.b = ((value & 0x000000f0) >>> 4) / 15f
    color.a = (value & 0x0000000f) / 15f
  }

  /** Sets the Color components using the specified integer value in the format RGB888. This is inverse to the rgb888(r, g, b) method.
    *
    * @param color
    *   The Color to be modified.
    * @param value
    *   An integer color value in RGB888 format.
    */
  def rgb888ToColor(color: Color, value: Int): Unit = {
    color.r = ((value & 0x00ff0000) >>> 16) / 255f
    color.g = ((value & 0x0000ff00) >>> 8) / 255f
    color.b = (value & 0x000000ff) / 255f
  }

  /** Sets the Color components using the specified integer value in the format RGBA8888. This is inverse to the rgba8888(r, g, b, a) method.
    *
    * @param color
    *   The Color to be modified.
    * @param value
    *   An integer color value in RGBA8888 format.
    */
  def rgba8888ToColor(color: Color, value: Int): Unit = {
    color.r = ((value & 0xff000000) >>> 24) / 255f
    color.g = ((value & 0x00ff0000) >>> 16) / 255f
    color.b = ((value & 0x0000ff00) >>> 8) / 255f
    color.a = (value & 0x000000ff) / 255f
  }

  /** Sets the Color components using the specified integer value in the format ARGB8888. This is the inverse to the argb8888(a, r, g, b) method
    *
    * @param color
    *   The Color to be modified.
    * @param value
    *   An integer color value in ARGB8888 format.
    */
  def argb8888ToColor(color: Color, value: Int): Unit = {
    color.a = ((value & 0xff000000) >>> 24) / 255f
    color.r = ((value & 0x00ff0000) >>> 16) / 255f
    color.g = ((value & 0x0000ff00) >>> 8) / 255f
    color.b = (value & 0x000000ff) / 255f
  }

  /** Sets the Color components using the specified integer value in the format ABGR8888.
    * @param color
    *   The Color to be modified.
    */
  def abgr8888ToColor(color: Color, value: Int): Unit = {
    color.a = ((value & 0xff000000) >>> 24) / 255f
    color.b = ((value & 0x00ff0000) >>> 16) / 255f
    color.g = ((value & 0x0000ff00) >>> 8) / 255f
    color.r = (value & 0x000000ff) / 255f
  }

  /** Sets the Color components using the specified float value in the format ABGR8888.
    * @param color
    *   The Color to be modified.
    */
  def abgr8888ToColor(color: Color, value: Float): Unit = {
    val c = NumberUtils.floatToIntColor(value)
    color.a = ((c & 0xff000000) >>> 24) / 255f
    color.b = ((c & 0x00ff0000) >>> 16) / 255f
    color.g = ((c & 0x0000ff00) >>> 8) / 255f
    color.r = (c & 0x000000ff) / 255f
  }
}
