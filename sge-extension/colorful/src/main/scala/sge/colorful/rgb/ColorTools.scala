/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful
package rgb

import sge.colorful.FloatColors
import sge.graphics.Color

/** Contains code for manipulating colors as `int`, packed `float`, and [[Color]] values in the RGB color space. RGB is the standard mode for colors in SGE/libGDX.
  */
object ColorTools {

  /** Gets a packed float representation of a color given as 4 float components: R, G, B, A. All channels range from 0.0 to 1.0, inclusive. Alpha is multiplicative opacity.
    */
  def rgb(red: Float, green: Float, blue: Float, alpha: Float): Float =
    java.lang.Float.intBitsToFloat(
      ((alpha * 255).toInt << 24 & 0xfe000000) | ((blue * 255).toInt << 16 & 0xff0000)
        | ((green * 255).toInt << 8 & 0xff00) | ((red * 255).toInt & 0xff)
    )

  /** Writes an RGB-format packed float color into an RGBA8888 Color. */
  def toColor(editing: Color, packed: Float): Color = {
    Color.abgr8888ToColor(editing, packed)
    editing
  }

  /** Converts a packed float RGB color to an RGBA8888 int. */
  def toRGBA8888(packed: Float): Int =
    Integer.reverseBytes(java.lang.Float.floatToIntBits(packed))

  /** Takes an RGBA8888 int and converts to a packed float in RGB format. */
  def fromRGBA8888(rgba: Int): Float =
    java.lang.Float.intBitsToFloat(Integer.reverseBytes(rgba) & 0xfeffffff)

  /** Takes a libGDX Color (RGBA8888) and converts to a packed float in RGB format. */
  def fromColor(color: Color): Float =
    color.toFloatBits()

  /** Takes RGBA components from 0.0 to 1.0 each and converts to a packed float in RGBA format. */
  def fromRGBA(r: Float, g: Float, b: Float, a: Float): Float =
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max((r * 255.999f).toInt, 0), 255)
        | Math.min(Math.max((g * 255.999f).toInt, 0), 255) << 8
        | Math.min(Math.max((b * 255.999f).toInt, 0), 255) << 16
        | ((a * 255f).toInt << 24 & 0xfe000000)
    )

  /** Gets the red channel as an int from 0 to 255. */
  def redInt(encoded: Float): Int = java.lang.Float.floatToRawIntBits(encoded) & 0xff

  /** Gets the green channel as an int from 0 to 255. */
  def greenInt(encoded: Float): Int = java.lang.Float.floatToRawIntBits(encoded) >>> 8 & 0xff

  /** Gets the blue channel as an int from 0 to 255. */
  def blueInt(encoded: Float): Int = java.lang.Float.floatToRawIntBits(encoded) >>> 16 & 0xff

  /** Gets the alpha channel as an even int from 0 to 254. */
  def alphaInt(encoded: Float): Int = (java.lang.Float.floatToRawIntBits(encoded) & 0xfe000000) >>> 24

  /** Gets the red channel as a float from 0.0f to 1.0f. */
  def red(encoded: Float): Float = (java.lang.Float.floatToRawIntBits(encoded) & 0xff) / 255f

  /** Gets the green channel as a float from 0.0f to 1.0f. */
  def green(encoded: Float): Float = (java.lang.Float.floatToRawIntBits(encoded) >>> 8 & 0xff) / 255f

  /** Gets the blue channel as a float from 0.0f to 1.0f. */
  def blue(encoded: Float): Float = (java.lang.Float.floatToRawIntBits(encoded) >>> 16 & 0xff) / 255f

  /** Gets the alpha channel as a float from 0.0f to 1.0f. */
  def alpha(encoded: Float): Float =
    ((java.lang.Float.floatToRawIntBits(encoded) & 0xfe000000) >>> 24) * 0.003937008f

  /** Gets a color as an RGBA packed float given floats for HSL and opacity. */
  def floatGetHSL(hue: Float, saturation: Float, lightness: Float, opacity: Float): Float =
    if (lightness <= 0.001f) {
      java.lang.Float.intBitsToFloat(((opacity * 255f).toInt << 24) & 0xfe000000)
    } else if (lightness >= 0.999f) {
      java.lang.Float.intBitsToFloat(((opacity * 255f).toInt << 24 & 0xfe000000) | 0x00ffffff)
    } else {
      FloatColors.hsl2rgb(hue, saturation, lightness, opacity)
    }

  /** Gets the hue of the given encoded color, from 0f (inclusive, red) to 1f (exclusive, red). */
  def hue(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val r       = (decoded & 0xff) / 255f
    val g       = (decoded >>> 8 & 0xff) / 255f
    val b       = (decoded >>> 16 & 0xff) / 255f
    var x       = 0f; var y = 0f; var z = 0f; var w = 0f
    if (g < b) { x = b; y = g; z = -1f; w = 2f / 3f }
    else { x = g; y = b; z = 0f; w = -1f / 3f }
    if (r < x) { z = w; w = r }
    else { w = x; x = r }
    val d = x - Math.min(w, y)
    Math.abs(z + (w - y) / (6f * d + 1e-10f))
  }

  /** Gets the saturation of the given encoded color, from 0.0f to 1.0f. */
  def saturation(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val r       = (decoded & 0xff) / 255f
    val g       = (decoded >>> 8 & 0xff) / 255f
    val b       = (decoded >>> 16 & 0xff) / 255f
    var x       = 0f; var y = 0f; var w = 0f
    if (g < b) { x = b; y = g }
    else { x = g; y = b }
    if (r < x) { w = r }
    else { w = x; x = r }
    x - Math.min(w, y)
  }

  /** Gets the lightness of the given encoded color. */
  def lightness(encoded: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(encoded)
    val r       = (decoded & 0xff) / 255f
    val g       = (decoded >>> 8 & 0xff) / 255f
    val b       = (decoded >>> 16 & 0xff) / 255f
    var x       = 0f; var y = 0f; var w = 0f
    if (g < b) { x = b; y = g }
    else { x = g; y = b }
    if (r < x) { w = r }
    else { w = x; x = r }
    val d = x - Math.min(w, y)
    x * (1f - 0.5f * d / (x + 1e-10f))
  }

  /** Interpolates from start towards white by change. Keeps alpha of start. */
  def lighten(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start)
    val r = s & 0xff; val g = s >>> 8 & 0xff; val b = s >>> 16 & 0xff; val a = s & 0xfe000000
    java.lang.Float.intBitsToFloat(
      ((r + (0xff - r) * change).toInt & 0xff) |
        ((g + (0xff - g) * change).toInt & 0xff) << 8 |
        ((b + (0xff - b) * change).toInt & 0xff) << 16 | a
    )
  }

  /** Interpolates from start towards black by change. Keeps alpha of start. */
  def darken(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start)
    val r = s & 0xff; val g = s >>> 8 & 0xff; val b = s >>> 16 & 0xff; val a = s & 0xfe000000
    java.lang.Float.intBitsToFloat(
      ((r * (1f - change)).toInt & 0xff) |
        ((g * (1f - change)).toInt & 0xff) << 8 |
        ((b * (1f - change)).toInt & 0xff) << 16 | a
    )
  }

  /** Brings the chromatic components closer to grayscale by change (desaturating). */
  def dullen(start: Float, change: Float): Float = {
    val rc = 0.32627f; val gc    = 0.3678f; val bc       = 0.30593001f
    val s  = java.lang.Float.floatToRawIntBits(start)
    val r  = s & 0xff; val g     = s >>> 8 & 0xff; val b = s >>> 16 & 0xff; val a = s & 0xfe000000
    val ch = 1f - change; val rw = change * rc; val gw   = change * gc; val bw    = change * bc
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max((r * (rw + ch) + g * rw + b * rw).toInt, 0), 255) |
        Math.min(Math.max((r * gw + g * (gw + ch) + b * gw).toInt, 0), 255) << 8 |
        Math.min(Math.max((r * bw + g * bw + b * (bw + ch)).toInt, 0), 255) << 16 | a
    )
  }

  /** Pushes the chromatic components away from grayscale by change (saturating). */
  def enrich(start: Float, change: Float): Float = {
    val rc = 0.32627f; val gc    = 0.3678f; val bc        = 0.30593001f
    val s  = java.lang.Float.floatToRawIntBits(start)
    val r  = s & 0xff; val g     = s >>> 8 & 0xff; val b  = s >>> 16 & 0xff; val a = s & 0xfe000000
    val ch = 1f + change; val rw = (-change) * rc; val gw = (-change) * gc; val bw = (-change) * bc
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max((r * (rw + ch) + g * rw + b * rw).toInt, 0), 255) |
        Math.min(Math.max((r * gw + g * (gw + ch) + b * gw).toInt, 0), 255) << 8 |
        Math.min(Math.max((r * bw + g * bw + b * (bw + ch)).toInt, 0), 255) << 16 | a
    )
  }

  def raiseR(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val t = s & 0xff; val other = s & 0xfeffff00
    java.lang.Float.intBitsToFloat(((t + (0xff - t) * change).toInt & 0xff) | other)
  }

  def lowerR(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val t = s & 0xff; val other = s & 0xfeffff00
    java.lang.Float.intBitsToFloat(((t * (1f - change)).toInt & 0xff) | other)
  }

  def raiseG(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val t = s >>> 8 & 0xff; val other = s & 0xfeff00ff
    java.lang.Float.intBitsToFloat(((t + (0xff - t) * change).toInt << 8 & 0xff00) | other)
  }

  def lowerG(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val t = s >>> 8 & 0xff; val other = s & 0xfeff00ff
    java.lang.Float.intBitsToFloat(((t * (1f - change)).toInt & 0xff) << 8 | other)
  }

  def raiseB(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val t = s >>> 16 & 0xff; val other = s & 0xfe00ffff
    java.lang.Float.intBitsToFloat(((t + (0xff - t) * change).toInt << 16 & 0xff0000) | other)
  }

  def lowerB(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val t = s >>> 16 & 0xff; val other = s & 0xfe00ffff
    java.lang.Float.intBitsToFloat(((t * (1f - change)).toInt & 0xff) << 16 | other)
  }

  def blot(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val opacity = s >>> 24 & 0xfe; val other = s & 0x00ffffff
    java.lang.Float.intBitsToFloat(((opacity + (0xfe - opacity) * change).toInt & 0xfe) << 24 | other)
  }

  def fade(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start); val opacity = s & 0xfe; val other = s & 0x00ffffff
    java.lang.Float.intBitsToFloat(((opacity * (1f - change)).toInt & 0xfe) << 24 | other)
  }

  /** Gets a variation on basis with HSL adjustments applied. */
  def toEditedFloat(basis: Float, hue: Float, saturation: Float, light: Float, opacity: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(basis)
    val op      = Math.min(Math.max(opacity + (decoded >>> 25) * (1f / 127f), 0f), 1f)
    val r       = (decoded & 0xff) / 255f
    val g       = (decoded >>> 8 & 0xff) / 255f
    val b       = (decoded >>> 16 & 0xff) / 255f
    var x       = 0f; var y = 0f; var z = 0f; var w = 0f
    if (g < b) { x = b; y = g; z = -1f; w = 2f / 3f }
    else { x = g; y = b; z = 0f; w = -1f / 3f }
    if (r < x) { z = w; w = r }
    else { w = x; x = r }
    val d   = x - Math.min(w, y)
    val lum = x * (1f - 0.5f * d / (x + 1e-10f))
    val h   = Math.abs(z + (w - y) / (6f * d + 1e-10f)) + hue + 1f
    val sat = (x - lum) / (Math.min(lum, 1f - lum) + 1e-10f) + saturation
    FloatColors.hsl2rgb(h - h.toInt, Math.min(Math.max(sat, 0f), 1f), Math.min(Math.max(lum + light, 0f), 1f), op)
  }
}
