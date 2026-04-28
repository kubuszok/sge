/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/utils/ColorUtils.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: RegExodus Pattern/Matcher -> java.util.regex.Pattern/Matcher
 *   Convention: IntArray -> DynamicArray[Int]
 *   Idiom: Java switch fall-through -> cumulative if/else length checks
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 803
 * Covenant-baseline-methods: ColorUtils,_lightness,_saturation,a,adjMultiplier4,adjMultiplier5,adjMultiplier6,adjMultiplierPale,bi,bits,c,ch,channel,channelInt,current,d,darken,describe,dullen,eA,eB,ei,end,enrich,h,hsb2rgb,hsl2rgb,hue,i,l,lerpColors,lerpColorsMultiplyAlpha,light,lighten,lookupInColors,mix,mixing,multiplyAllAlpha,multiplyAlpha,n,nonTermPattern,off,offset,offsetLightness,process,r,rc,result,rgb2hsb,rgb2hsl,s,sA,sz,total,u,unevenMix,v,x,y,y0,z,z0
 * Covenant-source-reference: com/github/tommyettinger/textra/utils/ColorUtils.java
 * Covenant-verified: 2026-04-19
 *
 * Partial-port debt:
 *   - parseDescription: only handles single color name lookup, not multi-word descriptions
 *     like "peach red" or "DARK DULLEST GREEN".
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra
package utils

import java.util.regex.Pattern
import scala.util.boundary
import scala.util.boundary.break
import sge.graphics.{ Color, Colors }
import sge.math.MathUtils
import sge.utils.{ DynamicArray, NumberUtils }

/** A few static methods for commonly-used color handling tasks. This has methods to convert from HSLA colors to RGBA and back again, for hue-changing effects mainly. It also has
  * [[lerpColors(Int,Int,Float):Int]] to blend RGBA colors, and [[multiplyAlpha(Int,Float):Int]] to alter only the alpha channel on an RGBA or HSLA int color.
  */
object ColorUtils {

  /** Converts the four HSLA components, each in the 0.0 to 1.0 range, to an int in RGBA8888 format. I brought this over from colorful-gdx's FloatColors class. I can't recall where I got the original
    * HSL(A) code from, but there's a strong chance it was written by cypherdare/cyphercove for their color space comparison. The `h` parameter for hue can be lower than 0.0 or higher than 1.0 because
    * the hue "wraps around;" only the fractional part of h is used. The other parameters must be between 0.0 and 1.0 (inclusive) to make sense.
    *
    * @param h
    *   hue, usually from 0.0 to 1.0, but only the fractional part is used
    * @param s
    *   saturation, from 0.0 to 1.0
    * @param l
    *   lightness, from 0.0 to 1.0
    * @param a
    *   alpha, from 0.0 to 1.0
    * @return
    *   an RGBA8888-format int
    */
  def hsl2rgb(h: Float, s: Float, l: Float, a: Float): Int = {
    val hue = h - MathUtils.floor(h)
    val x   = Math.min(Math.max(Math.abs(hue * 6f - 3f) - 1f, 0f), 1f)
    val y0  = hue + (2f / 3f)
    val z0  = hue + (1f / 3f)
    val y   = Math.min(Math.max(Math.abs((y0 - y0.toInt) * 6f - 3f) - 1f, 0f), 1f)
    val z   = Math.min(Math.max(Math.abs((z0 - z0.toInt) * 6f - 3f) - 1f, 0f), 1f)
    val v   = l + s * Math.min(l, 1f - l)
    val d   = 2f * (1f - l / (v + 1e-10f))
    Color.rgba8888(v * MathUtils.lerp(1f, x, d), v * MathUtils.lerp(1f, y, d), v * MathUtils.lerp(1f, z, d), a)
  }

  /** Converts the four RGBA components, each in the 0.0 to 1.0 range, to an int in HSLA format (hue, saturation, lightness, alpha). This format is exactly like RGBA8888 but treats what would normally
    * be red as hue, green as saturation, and blue as lightness; alpha is the same.
    *
    * @param r
    *   red, from 0.0 to 1.0
    * @param g
    *   green, from 0.0 to 1.0
    * @param b
    *   blue, from 0.0 to 1.0
    * @param a
    *   alpha, from 0.0 to 1.0
    * @return
    *   an "HSLA-format" int
    */
  def rgb2hsl(r: Float, g: Float, b: Float, a: Float): Int = {
    var x, y, z, w = 0f
    if (g < b) { x = b; y = g; z = -1f; w = 2f / 3f }
    else { x = g; y = b; z = 0f; w = -1f / 3f }
    if (r < x) { z = w; w = r }
    else { w = x; x = r }
    val d = x - Math.min(w, y)
    val l = x * (1f - 0.5f * d / (x + 1e-10f))
    Color.rgba8888(Math.abs(z + (w - y) / (6f * d + 1e-10f)), (x - l) / (Math.min(l, 1f - l) + 1e-10f), l, a)
  }

  /** Converts the four HSBA/HSVA components, each in the 0.0 to 1.0 range, to an int in RGBA8888 format. I brought this over from colorful-gdx's FloatColors class. I can't recall where I got the
    * original HSL(A) code from, but there's a strong chance it was written by cypherdare/cyphercove for their color space comparison. HSV and HSB are synonyms; it makes a little more sense to call
    * the third channel brightness. The `h` parameter for hue can be lower than 0.0 or higher than 1.0 because the hue "wraps around;" only the fractional part of h is used. The other parameters must
    * be between 0.0 and 1.0 (inclusive) to make sense.
    *
    * @param h
    *   hue, from 0.0 to 1.0
    * @param s
    *   saturation, from 0.0 to 1.0
    * @param b
    *   brightness, from 0.0 to 1.0
    * @param a
    *   alpha, from 0.0 to 1.0
    * @return
    *   an RGBA8888-format int
    */
  def hsb2rgb(h: Float, s: Float, b: Float, a: Float): Int = {
    val hue = h - MathUtils.floor(h)
    val x   = Math.min(Math.max(Math.abs(hue * 6f - 3f) - 1f, 0f), 1f)
    val y0  = hue + (2f / 3f)
    val z0  = hue + (1f / 3f)
    val y   = Math.min(Math.max(Math.abs((y0 - y0.toInt) * 6f - 3f) - 1f, 0f), 1f)
    val z   = Math.min(Math.max(Math.abs((z0 - z0.toInt) * 6f - 3f) - 1f, 0f), 1f)
    Color.rgba8888(b * MathUtils.lerp(1f, x, s), b * MathUtils.lerp(1f, y, s), b * MathUtils.lerp(1f, z, s), a)
  }

  /** Converts the four RGBA components, each in the 0.0 to 1.0 range, to an int in HSBA/HSVA format (hue, saturation, brightness/value, alpha). This format is exactly like RGBA8888 but treats what
    * would normally be red as hue, green as saturation, and blue as brightness/value; alpha is the same. HSV and HSB are synonyms; it makes a little more sense to call the third channel brightness.
    *
    * @param r
    *   red, from 0.0 to 1.0
    * @param g
    *   green, from 0.0 to 1.0
    * @param b
    *   blue, from 0.0 to 1.0
    * @param a
    *   alpha, from 0.0 to 1.0
    * @return
    *   an "HSBA/HSVA-format" int
    */
  def rgb2hsb(r: Float, g: Float, b: Float, a: Float): Int = {
    val v = Math.max(Math.max(r, g), b)
    val n = Math.min(Math.min(r, g), b)
    val c = v - n
    val h =
      if (c == 0) 0f
      else if (v == r) ((g - b) / c) / 6f
      else if (v == g) ((b - r) / c + 2f) / 6f
      else ((r - g) / c + 4f) / 6f
    Color.rgba8888(h, if (v == 0) 0f else c / v, v, a)
  }

  /** Given a packed int color and a channel value from 0 to 3, gets the value of that channel as a float from 0.0f to 1.0f . Channel 0 refers to R in RGBA8888 and H in HSLA ints, channel 1 refers to
    * G or S, 2 refers to B or L, and 3 always refers to A.
    *
    * @param color
    *   a packed int color in any 32-bit, 4-channel format
    * @param channel
    *   which channel to access, as an index from 0 to 3 inclusive
    * @return
    *   the non-packed float value of the requested channel, from 0.0f to 1.0f inclusive
    */
  def channel(color: Int, channel: Int): Float =
    (color >>> 24 - ((channel & 3) << 3) & 255) / 255f

  /** Given a packed int color and a channel value from 0 to 3, gets the value of that channel as an int from 0 to 255 . Channel 0 refers to R in RGBA8888 and H in HSLA ints, channel 1 refers to G or
    * S, 2 refers to B or L, and 3 always refers to A.
    *
    * @param color
    *   a packed int color in any 32-bit, 4-channel format
    * @param channel
    *   which channel to access, as an index from 0 to 3 inclusive
    * @return
    *   the int value of the requested channel, from 0 to 255 inclusive
    */
  def channelInt(color: Int, channel: Int): Int =
    color >>> 24 - ((channel & 3) << 3) & 255

  /** Interpolates from the RGBA8888 int color start towards end by change. Both start and end should be RGBA8888 ints, and change can be between 0f (keep start) and 1f (only use end). This is a good
    * way to reduce allocations of temporary Colors.
    *
    * @param s
    *   the starting color as a packed int
    * @param e
    *   the end/target color as a packed int
    * @param change
    *   how much to go from start toward end, as a float between 0 and 1; higher means closer to end
    * @return
    *   an RGBA8888 int that represents a color between start and end
    */
  def lerpColors(s: Int, e: Int, change: Float): Int = {
    val sA = s & 0xfe; val sB = (s >>> 8) & 0xff; val sG = (s >>> 16) & 0xff; val sR = (s >>> 24) & 0xff
    val eA = e & 0xfe; val eB = (e >>> 8) & 0xff; val eG = (e >>> 16) & 0xff; val eR = (e >>> 24) & 0xff
    (((sR + change * (eR - sR)).toInt & 0xff) << 24) |
      (((sG + change * (eG - sG)).toInt & 0xff) << 16) |
      (((sB + change * (eB - sB)).toInt & 0xff) << 8) |
      ((sA + change * (eA - sA)).toInt & 0xfe)
  }

  /** Interpolates from the RGBA8888 packed float color start towards end by change. Both start and end should be ABGR8888 packed floats, and change can be between 0f (keep start) and 1f (only use
    * end).
    *
    * @param start
    *   the starting color as a packed float
    * @param end
    *   the end/target color as a packed float
    * @param change
    *   how much to go from start toward end, as a float between 0 and 1; higher means closer to end
    * @return
    *   a packed float that represents an ABGR8888 color between start and end
    */
  def lerpColors(start: Float, end: Float, change: Float): Float = {
    val s  = NumberUtils.floatToIntBits(start); val e = NumberUtils.floatToIntBits(end)
    val sA = s >>> 25; val sB                         = (s >>> 16) & 0xff; val sG = (s >>> 8) & 0xff; val sR = s & 0xff
    val eA = e >>> 25; val eB                         = (e >>> 16) & 0xff; val eG = (e >>> 8) & 0xff; val eR = e & 0xff
    NumberUtils.intBitsToFloat(
      ((sR + change * (eR - sR)).toInt & 0xff) |
        (((sG + change * (eG - sG)).toInt & 0xff) << 8) |
        (((sB + change * (eB - sB)).toInt & 0xff) << 16) |
        (((sA + change * (eA - sA)).toInt & 0x7f) << 25)
    )
  }

  /** Interpolates from the RGBA8888 packed float color start towards end by change. Both start and end should be ABGR8888 packed floats, and change can be between 0f (keep start) and 1f (only use
    * end). This does not use the alpha of `end`, and instead multiplies the alpha of `start` by `alphaMultiplier` in the result. This is a specialized method meant to reduce the number of conversions
    * needed between packed floats and int bits.
    *
    * @param start
    *   the starting color as a packed float
    * @param end
    *   the end/target color as a packed float
    * @param change
    *   how much to go from start toward end, as a float between 0 and 1; higher means closer to end
    * @param alphaMultiplier
    *   between 0f and 1f; will be multiplied by the alpha of start and used in the final color
    * @return
    *   a packed float that represents an ABGR8888 color between start and end
    */
  def lerpColorsMultiplyAlpha(start: Float, end: Float, change: Float, alphaMultiplier: Float): Float = {
    val s  = NumberUtils.floatToIntBits(start); val e = NumberUtils.floatToIntBits(end)
    val sA = s >>> 25; val sB                         = (s >>> 16) & 0xff; val sG = (s >>> 8) & 0xff; val sR = s & 0xff
    val eB = (e >>> 16) & 0xff; val eG                = (e >>> 8) & 0xff; val eR  = e & 0xff
    NumberUtils.intBitsToFloat(
      ((sR + change * (eR - sR)).toInt & 0xff) |
        (((sG + change * (eG - sG)).toInt & 0xff) << 8) |
        (((sB + change * (eB - sB)).toInt & 0xff) << 16) |
        (((sA * alphaMultiplier).toInt & 0x7f) << 25)
    )
  }

  /** Given several colors, this gets an even mix of all colors in equal measure. If `colors` is null or has no items, this returns 256 (a transparent placeholder used by ColorLookup for "no color
    * found"). This is mostly useful in conjunction with DynamicArray, using its `items` for colors, typically 0 for offset, and its `size` for size.
    * @param colors
    *   an array of RGBA8888 int colors; all should use the same color space
    * @param offset
    *   the index of the first item in `colors` to use
    * @param size
    *   how many items from `colors` to use
    * @return
    *   an even mix of all colors given, as an RGBA8888 int color
    */
  def mix(colors: Array[Int], offset: Int, size: Int): Int = {
    val end = offset + size
    if (colors == null || colors.length < end || offset < 0 || size <= 0) 256
    else {
      var off = offset
      while (off < end && colors(off) == 256) off += 1
      if (off >= end) 256
      else {
        var result = colors(off)
        var i      = off + 1
        var denom  = 2
        while (i < end) {
          if (colors(i) != 256) { result = lerpColors(result, colors(i), 1f / denom) }
          else denom -= 1
          i += 1
          denom += 1
        }
        result
      }
    }
  }

  /** Mixes any number of colors with arbitrary weights per-color. Takes an array of varargs of alternating ints representing colors and weights, as with `color, weight, color, weight...`. If `colors`
    * is null or has no items, this returns 0 (fully-transparent black). Each color should be an RGBA8888 int, and each weight should be greater than 0.
    * @param colors
    *   an array or varargs that should contain alternating `color, weight, color, weight...` ints
    * @return
    *   the mixed color, as an RGBA8888 int
    */
  def unevenMix(colors: Int*): Int =
    if (colors == null || colors.isEmpty) 0
    else if (colors.length <= 2) colors(0)
    else unevenMix(colors.toArray, 0, colors.length)

  /** Mixes any number of colors with arbitrary weights per-color. Takes an array of alternating ints representing colors and weights, as with `color, weight, color, weight...`, starting at `offset`
    * in the array and continuing for `size` indices in the array. The `size` should be an even number 2 or greater, otherwise it will be reduced by 1. The weights can be any non-negative int values;
    * this method handles normalizing them internally. Each color should an RGBA8888 int, and each weight should be greater than 0. If `colors` is null or has no items, or if size <= 0, this returns 0
    * (fully-transparent black).
    *
    * @param colors
    *   starting at `offset`, this should contain alternating `color, weight, color, weight...` ints
    * @param offset
    *   where to start reading from in `colors`
    * @param size
    *   how many indices to read from `colors`; must be an even number
    * @return
    *   the mixed color, as an RGBA8888 int
    */
  def unevenMix(colors: Array[Int], offset: Int, size: Int): Int = boundary {
    val sz  = size & -2
    val end = offset + sz
    if (colors == null || colors.length < end || offset < 0 || sz <= 0) {
      break(256) // placeholder color
    }
    var off = offset
    while (colors(off) == 256) {
      off += 2
      if (off >= end) {
        break(256) // all colors were placeholders
      }
    }
    var result  = colors(off)
    var current = colors(off + 1).toFloat
    var total   = current
    var i       = off + 3
    while (i < end) {
      if (colors(i - 1) != 256) {
        total += colors(i)
      }
      i += 2
    }
    total = 1f / total
    current *= total
    i = off + 3
    while (i < end) {
      val mixColor = colors(i - 1)
      if (mixColor != 256) {
        val weight = colors(i) * total
        current += weight
        result = lerpColors(result, mixColor, weight / current)
      }
      i += 2
    }
    result
  }

  /** Interpolates from the int color start towards white by change. While change should be between 0f (return start as-is) and 1f (return white), start should be an RGBA8888 color. This is a good way
    * to reduce allocations of temporary Colors, and is a little more efficient and clear than using [[lerpColors(Int,Int,Float):Int]] to lerp towards white. Unlike [[lerpColors(Int,Int,Float):Int]],
    * this keeps the alpha of start as-is.
    * @see
    *   [[darken(Int,Float):Int]] the counterpart method that darkens an int color
    * @param start
    *   the starting color as an RGBA8888 int
    * @param change
    *   how much to go from start toward white, as a float between 0 and 1; higher means closer to white
    * @return
    *   an RGBA8888 int that represents a color between start and white
    */
  def lighten(start: Int, change: Float): Int = {
    val r = start >>> 24; val g = start >>> 16 & 0xff; val b = start >>> 8 & 0xff
    val a = start & 0x000000fe
    ((r + (0xff - r) * change).toInt & 0xff) << 24 |
      ((g + (0xff - g) * change).toInt & 0xff) << 16 |
      ((b + (0xff - b) * change).toInt & 0xff) << 8 | a
  }

  /** Interpolates from the packed float color start towards white by change. While change should be between 0f (return start as-is) and 1f (return white), start should be a packed float (ABGR8888)
    * color. The alpha will not change.
    *
    * @see
    *   [[darken(Float,Float):Float]] the counterpart method that darkens a packed float color
    * @param start
    *   the starting color as an ABGR8888 packed float color
    * @param change
    *   how much to go from start toward white, as a float between 0 and 1; higher means closer to white
    * @return
    *   a packed float that represents a color between start and white
    */
  def lighten(start: Float, change: Float): Float = {
    val u = NumberUtils.floatToIntBits(start)
    val r = u & 0xff; val g = u >>> 8 & 0xff; val b = u >>> 16 & 0xff
    val a = u & 0xfe000000
    NumberUtils.intBitsToFloat(
      ((r + (0xff - r) * change).toInt & 0xff) |
        ((g + (0xff - g) * change).toInt & 0xff) << 8 |
        ((b + (0xff - b) * change).toInt & 0xff) << 16 |
        a
    )
  }

  /** Interpolates from the int color start towards black by change. While change should be between 0f (return start as-is) and 1f (return black), start should be an RGBA8888 color. This is a good way
    * to reduce allocations of temporary Colors, and is a little more efficient and clear than using [[lerpColors(Int,Int,Float):Int]] to lerp towards black. Unlike [[lerpColors(Int,Int,Float):Int]],
    * this keeps the alpha of start as-is.
    * @see
    *   [[lighten(Int,Float):Int]] the counterpart method that lightens an int color
    * @param start
    *   the starting color as an RGBA8888 int
    * @param change
    *   how much to go from start toward black, as a float between 0 and 1; higher means closer to black
    * @return
    *   an RGBA8888 int that represents a color between start and black
    */
  def darken(start: Int, change: Float): Int = {
    val r  = start >>> 24; val g = start >>> 16 & 0xff; val b = start >>> 8 & 0xff
    val a  = start & 0x000000fe
    val ch = 1f - change
    ((r * ch).toInt & 0xff) << 24 | ((g * ch).toInt & 0xff) << 16 | ((b * ch).toInt & 0xff) << 8 | a
  }

  /** Interpolates from the packed float color start towards black by change. While change should be between 0f (return start as-is) and 1f (return black), start should be a packed float (ABGR8888)
    * color. The alpha will not change.
    *
    * @see
    *   [[lighten(Float,Float):Float]] the counterpart method that lightens a packed float color
    * @param start
    *   the starting color as an ABGR8888 packed float color
    * @param change
    *   how much to go from start toward black, as a float between 0 and 1; higher means closer to black
    * @return
    *   a packed float that represents a color between start and black
    */
  def darken(start: Float, change: Float): Float = {
    val u  = NumberUtils.floatToIntBits(start)
    val r  = u & 0xff; val g = u >>> 8 & 0xff; val b = u >>> 16 & 0xff
    val a  = u & 0xfe000000
    val ch = 1f - change
    NumberUtils.intBitsToFloat(
      ((r * ch).toInt & 0xff) |
        ((g * ch).toInt & 0xff) << 8 |
        ((b * ch).toInt & 0xff) << 16 |
        a
    )
  }

  /** Brings the chromatic components of `start` closer to grayscale by `change` (desaturating them). While change should be between 0f (return start as-is) and 1f (return fully gray), start should be
    * an RGBA8888 int color. This leaves alpha alone.
    *
    * [[http://www.graficaobscura.com/matrix/index.html The algorithm used is from here]].
    * @see
    *   [[enrich]] the counterpart method that makes an int color more saturated
    * @param start
    *   the starting color as an RGBA8888 int
    * @param change
    *   how much to change start to a desaturated color, as a float between 0 and 1; higher means a less saturated result
    * @return
    *   an RGBA8888 int that represents a color between start and a desaturated color
    */
  def dullen(start: Int, change: Float): Int = {
    val rc = 0.32627f; val gc    = 0.3678f; val bc            = 0.30593001f
    val r  = start >>> 24; val g = start >>> 16 & 0xff; val b = start >>> 8 & 0xff
    val a  = start & 0x000000fe
    val ch = 1f - change; val rw = change * rc; val gw        = change * gc; val bw = change * bc
    Math.min(Math.max(r * (rw + ch) + g * rw + b * rw, 0), 255).toInt << 24 |
      Math.min(Math.max(r * gw + g * (gw + ch) + b * gw, 0), 255).toInt << 16 |
      Math.min(Math.max(r * bw + g * bw + b * (bw + ch), 0), 255).toInt << 8 |
      a
  }

  /** Pushes the chromatic components of `start` away from grayscale by change (saturating them). While change should be between 0f (return start as-is) and 1f (return maximally saturated), start
    * should be an RGBA8888 int color.
    *
    * [[http://www.graficaobscura.com/matrix/index.html The algorithm used is from here]].
    * @see
    *   [[dullen]] the counterpart method that makes an int color less saturated
    * @param start
    *   the starting color as an RGBA8888 int
    * @param change
    *   how much to change start to a saturated color, as a float between 0 and 1; higher means a more saturated result
    * @return
    *   an RGBA8888 int that represents a color between start and a saturated color
    */
  def enrich(start: Int, change: Float): Int = {
    val rc = -0.32627f; val gc   = -0.3678f; val bc           = -0.30593001f
    val r  = start >>> 24; val g = start >>> 16 & 0xff; val b = start >>> 8 & 0xff
    val a  = start & 0x000000fe
    val ch = 1f + change; val rw = change * rc; val gw        = change * gc; val bw = change * bc
    Math.min(Math.max(r * (rw + ch) + g * rw + b * rw, 0), 255).toInt << 24 |
      Math.min(Math.max(r * gw + g * (gw + ch) + b * gw, 0), 255).toInt << 16 |
      Math.min(Math.max(r * bw + g * bw + b * (bw + ch), 0), 255).toInt << 8 |
      a
  }

  /** Gets an "offset color" for the original `color` where high red, green, or blue channels become low values in that same channel, and vice versa, then blends the original with that offset, using
    * more of the offset if `power` is higher (closer to 1.0f). It is usually fine for `power` to be 0.5f . This can look... pretty strange for some input colors, and you may want [[offsetLightness]]
    * instead.
    * @param color
    *   the original color as an RGBA8888 int
    * @param power
    *   between 0.0f and 1.0f, this is how heavily the offset color should factor in to the result
    * @return
    *   a mix between `color` and its offset, with higher `power` using more of the offset
    */
  def offset(color: Int, power: Float): Int =
    lerpColors(color, color ^ 0x80808000, power)

  /** Gets an "offset color" for the original `color`, lightening it if it is perceptually dark (under 40% luma by a simplistic measurement) or darkening it if it is perceptually light. This
    * essentially uses the lightness to determine whether to call `lighten(color, power)` or `darken(color, power)`. It is usually fine for `power` to be 0.5f . This leaves hue alone, and doesn't
    * change saturation much. The lightness measurement is effectively `red * 3/8 + green * 1/2 + blue * 1/8`.
    * @param color
    *   the original color as an RGBA8888 int
    * @param power
    *   between 0.0f and 1.0f, this is how much this should either lighten or darken the result
    * @return
    *   a variant on `color`, either lighter or darker depending on its original lightness
    */
  def offsetLightness(color: Int, power: Float): Int = {
    val light = (color >>> 24) * 3 + (color >>> 14 & 0x3fc) + (color >>> 8 & 0xff) // ranges from 0 to 2020
    if (light < 808) lighten(color, power) // under 40% luma
    else darken(color, power)
  }

  /** Given an RGBA8888 or HSLA color as an int, this multiplies the alpha of that color by multiplier and returns another int color of the same format passed in. This clamps the alpha if it would go
    * below 0 or above 255, and leaves the RGB or HSL channels alone.
    *
    * @param color
    *   an RGBA8888 or HSLA color
    * @param multiplier
    *   a multiplier to apply to color's alpha
    * @return
    *   another color of the same format as the one given, with alpha multiplied
    */
  def multiplyAlpha(color: Int, multiplier: Float): Int =
    (color & 0xffffff00) | Math.min(Math.max(((color & 0xff) * multiplier).toInt, 0), 255)

  /** Given a packed ABGR float color, this multiplies the alpha of that color by multiplier and returns another float color of the same format passed in. This clamps the alpha if it would go below 0
    * or above 255, and leaves the RGB channels alone.
    *
    * @param color
    *   an RGBA8888 or HSLA color
    * @param multiplier
    *   a multiplier to apply to color's alpha
    * @return
    *   another color of the same format as the one given, with alpha multiplied
    */
  def multiplyAlpha(color: Float, multiplier: Float): Float = {
    val bits = NumberUtils.floatToIntBits(color)
    NumberUtils.intBitsToFloat((bits & 0x00ffffff) | Math.min(Math.max(((bits >>> 25) * multiplier).toInt, 0), 127) << 25)
  }

  /** Given any purely-non-null 2D int array representing RGBA or HSLA colors, this multiplies the alpha channel of each color by multiplier, modifying the given array, and returns the changed array
    * for chaining. This uses [[multiplyAlpha(Int,Float):Int]] internally, so its documentation applies.
    *
    * @param colors
    *   a 2D int array of RGBA or HSLA colors, none of which can include null arrays
    * @param multiplier
    *   a multiplier to apply to each color's alpha
    * @return
    *   colors, after having each item's alpha multiplied
    */
  def multiplyAllAlpha(colors: Array[Array[Int]], multiplier: Float): Array[Array[Int]] = {
    var x = 0
    while (x < colors.length) {
      var y = 0
      while (y < colors(x).length) {
        colors(x)(y) = multiplyAlpha(colors(x)(y), multiplier)
        y += 1
      }
      x += 1
    }
    colors
  }

  /** Looks up a color name from libGDX Colors. Returns 256 if not found.
    *
    * This simply looks up `key` in Colors, returning 256 (fully transparent, extremely dark blue) if no Color exists by that exact name (case-sensitive), or returning the RGBA8888 value of the color
    * otherwise. All color names are `ALL_CAPS` in libGDX's Colors collection by default.
    * @param key
    *   a color name, typically in `ALL_CAPS`
    * @return
    *   the RGBA8888 int color matching the name, or 256 if the name was not found
    */
  def lookupInColors(key: String, beginIndex: Int, endIndex: Int): Int = {
    val c = Colors.get(StringUtils.safeSubstring(key, beginIndex, endIndex))
    c.fold(256)(Color.rgba8888(_))
  }

  // --- describe() support ---

  private val mixing         = DynamicArray[Int](8)
  private val nonTermPattern = Pattern.compile("[^a-zA-Z0-9_]+")
  private var _lightness     = 0f
  private var _saturation    = 0f

  /** Parses a color description and returns the approximate color it describes, as an RGBA8888 int color. Color descriptions consist of one or more alphabetical words, separated by non-alphanumeric
    * characters (typically spaces and/or hyphens, though the underscore is treated as a letter). Any word that is the name of a color in Palette will be looked up in Palette.NAMED and tracked; if
    * there is more than one of these color names, the colors will be mixed using [[unevenMix(Array[Int],Int,Int):Int]], or if there is just one color name, then the corresponding color will be used.
    * A number can be present after a color name (separated by any non-alphanumeric character(s) other than the underscore); if so, it acts as a positive weight for that color when mixed with other
    * named colors. The recommended separator between a color name and its weight is the space `' '`, but other punctuation like `':'`, or whitespace, is usually valid. Note that in some contexts,
    * color descriptions shouldn't contain square brackets, curly braces, or the chars `@%?^=.` , because they can have unintended effects on the behavior of markup. You can also repeat a color name
    * to increase its weight, as in "red red blue".
    *
    * The special adjectives "light" and "dark" change the lightness of the described color; likewise, "rich" and "dull" change the saturation (how different the color is from grayscale). All of these
    * adjectives can have "-er" or "-est" appended to make their effect twice or three times as strong. Technically, the chars appended to an adjective don't matter, only their count, so "lightaa" is
    * the same as "lighter" and "richcat" is the same as "richest". There's an unofficial fourth level as well, used when any 4 characters are appended to an adjective (as in "darkmost"); it has four
    * times the effect of the original adjective. There are also the adjectives "bright" (equivalent to "light rich"), "pale" ("light dull"), "deep" ("dark rich"), and "weak" ("dark dull"). These can
    * be amplified like the other four, except that "pale" goes to "paler", "palest", and then to "palemax" or (its equivalent) "palemost", where only the word length is checked.
    *
    * Note that while adjectives are case-insensitive, color names are not. Because the colors defined in libGDX Colors use ALL_CAPS, and the colors additionally defined by Palette use lower case and
    * are always one word, there are a few places where two different colors are defined by names that only differ in case. Examples include Palette.orange and Palette.ORANGE, or Palette.salmon and
    * Palette.SALMON.
    *
    * If part of a color name or adjective is invalid, it is not considered; if the description is empty or fully invalid, this returns the RGBA8888 int value 256 (used as a placeholder by
    * ColorLookup).
    *
    * Examples of valid descriptions include "blue", "dark green", "DULLER RED", "peach pink", "indigo purple mauve", "lightest, richer apricot-olive", "BRIGHT GOLD", "palest cyan blue", "Deep fern
    * black", "weakmost celery", "LIGHTMOST rich MAROON 2 indigo 3", "red:3 orange", and "dark deep (blue 7) (cyan 3)".
    * @param description
    *   a color description, as a String matching the above format
    * @return
    *   an RGBA8888 int color as described
    */
  def describe(description: String): Int =
    describe(description, 0, description.length)

  /** Parses a subsection of a color description and returns the general color it describes, as an RGBA8888 int color. Color descriptions consist of one or more alphabetical words, separated by
    * non-alphanumeric characters (typically spaces and/or hyphens, though the underscore is treated as a letter). Any word that is the name of a color in Palette will be looked up in Palette.NAMED
    * and tracked; if there is more than one of these color names, the colors will be mixed using [[unevenMix(Array[Int],Int,Int):Int]], or if there is just one color name, then the corresponding
    * color will be used. A number can be present after a color name (separated by any non-alphanumeric character(s) other than the underscore); if so, it acts as a positive weight for that color when
    * mixed with other named colors. The recommended separator between a color name and its weight is the space `' '`, but other punctuation like `':'`, or whitespace, is usually valid. Note that in
    * some contexts, color descriptions shouldn't contain square brackets, curly braces, or the chars `@%?^=.` , because they can have unintended effects on the behavior of markup. You can also repeat
    * a color name to increase its weight, as in "red red blue".
    *
    * The special adjectives "light" and "dark" change the lightness of the described color; likewise, "rich" and "dull" change the saturation (how different the color is from grayscale). All of these
    * adjectives can have "-er" or "-est" appended to make their effect twice or three times as strong. Technically, the chars appended to an adjective don't matter, only their count, so "lightaa" is
    * the same as "lighter" and "richcat" is the same as "richest". There's an unofficial fourth level as well, used when any 4 characters are appended to an adjective (as in "darkmost"); it has four
    * times the effect of the original adjective. There are also the adjectives "bright" (equivalent to "light rich"), "pale" ("light dull"), "deep" ("dark rich"), and "weak" ("dark dull"). These can
    * be amplified like the other four, except that "pale" goes to "paler", "palest", and then to "palemax" or (its equivalent) "palemost", where only the word length is checked.
    *
    * Note that while adjectives are case-insensitive, color names are not. Because the colors defined in libGDX Colors use ALL_CAPS, and the colors additionally defined by Palette use lower case and
    * are always one word, there are a few places where two different colors are defined by names that only differ in case. Examples include Palette.orange and Palette.ORANGE, or Palette.salmon and
    * Palette.SALMON.
    *
    * If part of a color name or adjective is invalid, it is not considered; if the description is empty or fully invalid, this returns the RGBA8888 int value 256 (used as a placeholder by
    * ColorLookup).
    *
    * Examples of valid descriptions include "blue", "dark green", "DULLER RED", "peach pink", "indigo purple mauve", "lightest, richer apricot-olive", "BRIGHT GOLD", "palest cyan blue", "Deep fern
    * black", "weakmost celery", "LIGHTMOST rich MAROON 2 indigo 3", "red:3 orange", and "dark deep (blue 7) (cyan 3)".
    * @param description
    *   a color description, as a String matching the above format
    * @param beginIndex
    *   the starting index in description, inclusive
    * @param endIndex
    *   the ending index in description, exclusive
    * @return
    *   an RGBA8888 int color as described
    */
  def describe(description: String, beginIndex: Int, endIndex: Int): Int = {
    val bi = Math.max(beginIndex, 0)
    val ei = Math.min(endIndex, description.length)
    if (ei <= bi) {
      256
    } else {
      _lightness = 0f
      _saturation = 0f
      mixing.clear()

      val sub   = description.substring(bi, ei)
      var index = 0
      val m     = nonTermPattern.matcher(sub)

      // Add segments before each match found
      while (m.find()) {
        if (m.start() > index) {
          process(sub.substring(index, m.start()))
        }
        index = m.end()
      }

      // If no match was found, process the whole substring
      if (index == 0) {
        process(sub)
      }

      // Add remaining segment
      if (index > 0 && index < sub.length) {
        process(sub.substring(index))
      }

      if (mixing.size < 2) {
        256
      } else {
        var result = unevenMix(mixing.items, 0, mixing.size)
        if (result == 256) {
          result
        } else {
          if (_lightness > 0) result = lighten(result, _lightness)
          else if (_lightness < 0) result = darken(result, -_lightness)

          if (_saturation > 0) result = enrich(result, _saturation)
          else if (_saturation < 0) result = dullen(result, -_saturation)

          result
        }
      }
    }
  }

  /** Processes a single term from a color description: either an adjective (modifying lightness/saturation), a numeric weight, or a color name.
    *
    * The Java original uses switch fall-through to accumulate effects. For base-4 adjectives (dark, dull, rich, deep, weak), the valid lengths are 4, 6, 7, 8 (skipping 5). For base-5 (light), lengths
    * are 5, 7, 8, 9 (skipping 6). For base-6 (bright), lengths are 6, 8, 9, 10 (skipping 7). For pale, all lengths 4-8 are valid (no gaps). Effect multiplier = number of valid lengths <= the actual
    * length.
    */
  private def process(term: String): Unit =
    if (term == null || term.isEmpty) {
      () // nothing to do
    } else {
      val len = term.length
      term.charAt(0) match {
        case 'L' | 'l' =>
          if (len > 2 && (term.charAt(2) == 'g' || term.charAt(2) == 'G')) {
            // light: base=5, +er=7, +est=8, +most=9 (gap at 6)
            val n = adjMultiplier5(len)
            _lightness += 0.20f * n
          } else {
            mixing.add(Palette.NAMED.getOrElse(term, 256), 1)
          }
        case 'B' | 'b' =>
          if (len > 3 && (term.charAt(3) == 'g' || term.charAt(3) == 'G')) {
            // bright: base=6, +er=8, +est=9, +most=10 (gap at 7)
            val n = adjMultiplier6(len)
            _lightness += 0.20f * n
            _saturation += 0.200f * n
          } else {
            mixing.add(Palette.NAMED.getOrElse(term, 256), 1)
          }
        case 'P' | 'p' =>
          if (len > 2 && (term.charAt(2) == 'l' || term.charAt(2) == 'L')) {
            // pale: base=4, +r=5, +st=6, +xxx=7, +most=8 (no gaps; case 8 falls to 7)
            val n = adjMultiplierPale(len)
            _lightness += 0.20f * n
            _saturation -= 0.200f * n
          } else {
            mixing.add(Palette.NAMED.getOrElse(term, 256), 1)
          }
        case 'W' | 'w' =>
          if (len > 3 && (term.charAt(3) == 'k' || term.charAt(3) == 'K')) {
            // weak: base=4, +er=6, +est=7, +most=8 (gap at 5)
            val n = adjMultiplier4(len)
            _lightness -= 0.20f * n
            _saturation -= 0.200f * n
          } else {
            mixing.add(Palette.NAMED.getOrElse(term, 256), 1)
          }
        case 'R' | 'r' =>
          if (len > 1 && (term.charAt(1) == 'i' || term.charAt(1) == 'I')) {
            // rich: base=4, +er=6, +est=7, +most=8 (gap at 5)
            val n = adjMultiplier4(len)
            _saturation += 0.200f * n
          } else {
            mixing.add(Palette.NAMED.getOrElse(term, 256), 1)
          }
        case 'D' | 'd' =>
          if (len > 1 && (term.charAt(1) == 'a' || term.charAt(1) == 'A')) {
            // dark: base=4, +er=6, +est=7, +most=8 (gap at 5)
            val n = adjMultiplier4(len)
            _lightness -= 0.20f * n
          } else if (len > 1 && (term.charAt(1) == 'u' || term.charAt(1) == 'U')) {
            // dull: base=4, +er=6, +est=7, +most=8 (gap at 5)
            val n = adjMultiplier4(len)
            _saturation -= 0.200f * n
          } else if (len > 3 && (term.charAt(3) == 'p' || term.charAt(3) == 'P')) {
            // deep: base=4, +er=6, +est=7, +most=8 (gap at 5)
            val n = adjMultiplier4(len)
            _lightness -= 0.20f * n
            _saturation += 0.200f * n
          } else {
            mixing.add(Palette.NAMED.getOrElse(term, 256), 1)
          }
        case '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' =>
          if (mixing.size >= 2) {
            mixing((mixing.size & -2) - 1) = StringUtils.intFromDec(term, 0, term.length)
          }
        case _ =>
          mixing.add(Palette.NAMED.getOrElse(term, 256), 1)
      }
    }

  /** Fall-through multiplier for base-4 adjectives (dark, dull, rich, deep, weak). Valid lengths: 4, 6, 7, 8. Gap at 5.
    */
  private def adjMultiplier4(len: Int): Int =
    if (len >= 8) 4
    else if (len == 7) 3
    else if (len == 6) 2
    else if (len == 4) 1
    else 0

  /** Fall-through multiplier for base-5 adjectives (light). Valid lengths: 5, 7, 8, 9. Gap at 6.
    */
  private def adjMultiplier5(len: Int): Int =
    if (len >= 9) 4
    else if (len == 8) 3
    else if (len == 7) 2
    else if (len == 5) 1
    else 0

  /** Fall-through multiplier for base-6 adjectives (bright). Valid lengths: 6, 8, 9, 10. Gap at 7.
    */
  private def adjMultiplier6(len: Int): Int =
    if (len >= 10) 4
    else if (len == 9) 3
    else if (len == 8) 2
    else if (len == 6) 1
    else 0

  /** Fall-through multiplier for "pale" — consecutive lengths 4-7, with 8 equivalent to 7. Valid lengths: 4, 5, 6, 7, 8 (8 maps to same as 7 = 4x).
    */
  private def adjMultiplierPale(len: Int): Int =
    if (len >= 7) 4
    else if (len == 6) 3
    else if (len == 5) 2
    else if (len == 4) 1
    else 0
}
