/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 379
 * Covenant-baseline-methods: ColorTools,a,alpha,alphaInt,b,blot,blue,blueInt,ch,contrast,d,darken,decoded,differentiateLightness,dullen,e,editRGB,enrich,fade,floatGetHSL,fromColor,fromRGBA,fromRGBA8888,g,green,greenInt,h,hue,inverseLightness,lessenChange,lighten,lightness,limit,lowerB,lowerG,lowerR,lum,main,offsetLightness,oklab,op,r,raiseB,raiseG,raiseR,randomColor,randomEdit,rc,re,red,redInt,rgb,rs,s,sat,saturation,sd,subrandomColor,toColor,toEditedFloat,toRGBA8888,x
 * Covenant-source-reference: com/github/tommyettinger/colorful/rgb/ColorTools.java
 * Covenant-verified: 2026-04-19
 */
package sge
package colorful
package rgb

import scala.util.boundary
import scala.util.boundary.break

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

  /** Gets a variation on basis with HSL adjustments applied. Note that this edits the color in HSL space, not RGB! Takes floats representing the amounts of change to apply to hue, saturation,
    * lightness, and opacity; these can be between -1f and 1f.
    */
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

  /** Given a packed float RGBA color mainColor and another RGBA color that it should be made to contrast with, gets a packed float RGBA color with roughly inverted lightness (how the Oklab color
    * space interprets it), but the same general hue and saturation unless the lightness gets too close to white or black. This won't ever produce black or other very dark colors, and also has a gap
    * in the range it produces for lightness values between 0.5 and 0.55. That allows most of the colors this method produces to contrast well as a foreground when displayed on a background of
    * contrastingColor, or vice versa.
    */
  def inverseLightness(mainColor: Float, contrastingColor: Float): Float =
    oklab.ColorTools.toRGBA(
      oklab.ColorTools.inverseLightness(
        oklab.ColorTools.fromRGBA(mainColor),
        oklab.ColorTools.fromRGBA(contrastingColor)
      )
    )

  /** Given a packed float RGBA color mainColor and another RGBA color that it should be made to contrast with, gets a packed float RGBA color with lightness that should be quite different from
    * contrastingColor's lightness, but the same chromatic channels and opacity. This goes through Oklab as an intermediate step.
    */
  def differentiateLightness(mainColor: Float, contrastingColor: Float): Float = {
    val main     = java.lang.Float.floatToRawIntBits(oklab.ColorTools.fromRGBA(mainColor))
    val contrast = java.lang.Float.floatToRawIntBits(oklab.ColorTools.fromRGBA(contrastingColor))
    oklab.ColorTools.toRGBA(java.lang.Float.intBitsToFloat((main & 0xfeffff00) | (contrast + 128 & 0xff) + (main & 0xff) >>> 1))
  }

  /** Pretty simple; adds 0.5 to the given color's lightness (calculated by converting it to Oklab internally) and wraps it around if it would go above 1.0, then averages that with the original
    * lightness. This means light colors become darker, and dark colors become lighter, with almost all results in the middle-range of possible lightness.
    */
  def offsetLightness(mainColor: Float): Float = {
    val oklab = java.lang.Float.floatToRawIntBits(mainColor)
    sge.colorful.oklab.ColorTools.toRGBA(java.lang.Float.intBitsToFloat((oklab & 0xfeffff00) | (oklab + 128 & 0xff) + (oklab & 0xff) >>> 1))
  }

  /** Makes the additive RGBA color stored in color cause less of a change when used as a tint, as if it were mixed with neutral gray. When fraction is 1.0, this returns color unchanged; when fraction
    * is 0.0, it returns gray, and when it is in-between 0.0 and 1.0 it returns something between the two.
    */
  def lessenChange(color: Float, fraction: Float): Float = {
    val e  = java.lang.Float.floatToRawIntBits(color)
    val rs = 0x80; val gs     = 0x80; val bs             = 0x80
    val re = e & 0xff; val ge = (e >>> 8) & 0xff; val be = (e >>> 16) & 0xff; val ae = e >>> 24 & 0xfe
    java.lang.Float.intBitsToFloat(
      ((rs + fraction * (re - rs)).toInt & 0xff)
        | ((gs + fraction * (ge - gs)).toInt & 0xff) << 8
        | ((bs + fraction * (be - bs)).toInt & 0xff) << 16
        | (ae << 24)
    )
  }

  /** Given a packed float RGB color, this edits its red, green, blue, and alpha channels by adding the corresponding "add" parameter and then clamping. Each value is considered in the 0 to 1 range.
    * You can give a value of 0 for any "add" parameter you want to stay unchanged.
    */
  def editRGB(encoded: Float, addR: Float, addG: Float, addB: Float, addAlpha: Float): Float =
    editRGB(encoded, addR, addG, addB, addAlpha, 1f, 1f, 1f, 1f)

  /** Given a packed float RGB color, this edits its red, green, blue, and alpha channels by first multiplying each channel by the corresponding "mul" parameter and then adding the corresponding "add"
    * parameter, before clamping. You can give a value of 0 for any "add" parameter you want to stay unchanged, or a value of 1 for any "mul" parameter that shouldn't change.
    */
  def editRGB(
    encoded:  Float,
    addR:     Float,
    addG:     Float,
    addB:     Float,
    addAlpha: Float,
    mulR:     Float,
    mulG:     Float,
    mulB:     Float,
    mulAlpha: Float
  ): Float = {
    val s = java.lang.Float.floatToRawIntBits(encoded)
    val r = s & 0xff; val g = s >>> 8 & 0xff; val b = s >>> 16 & 0xff
    val a = s >>> 25
    java.lang.Float.intBitsToFloat(
      Math.max(0, Math.min(255, (r * mulR + addR * 255.999f).toInt)) |
        Math.max(0, Math.min(255, (g * mulG + addG * 255.999f).toInt)) << 8 |
        Math.max(0, Math.min(255, (b * mulB + addB * 255.999f).toInt)) << 16 |
        Math.max(0, Math.min(127, (a * mulAlpha + addAlpha * 127.999f).toInt)) << 25
    )
  }

  /** Makes a quasi-randomly-edited variant on the given color, allowing typically a small amount of variance (such as 0.05 to 0.25) between the given color and what this can return. The seed should
    * be different each time this is called, and can be obtained from a random number generator to make the colors more random, or can be incremented on each call.
    */
  def randomEdit(color: Float, seed: Long, variance: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(color)
    val r       = (decoded & 0xff) / 255f
    val g       = (decoded >>> 8 & 0xff) / 255f
    val b       = (decoded >>> 16 & 0xff) / 255f
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
        val nx   = x + r
        val ny   = y + g
        val nz   = z + b
        if (dist <= limit) {
          break(
            java.lang.Float.intBitsToFloat(
              (decoded & 0xfe000000) | ((nz * 255.999f).toInt << 16 & 0xff0000)
                | ((ny * 255.999f).toInt << 8 & 0xff00) | (nx * 255.999f).toInt
            )
          )
        }
        j += 1
      }
      color
    }
  }

  /** Produces a random packed float color that is always opaque and should be uniformly distributed.
    */
  def randomColor(random: java.util.Random): Float = {
    val r = random.nextFloat(); val g = random.nextFloat(); val b = random.nextFloat()
    java.lang.Float.intBitsToFloat(
      0xfe000000
        | ((b * 256f).toInt << 16 & 0xff0000)
        | ((g * 256f).toInt << 8 & 0xff00)
        | ((r * 256f).toInt & 0xff)
    )
  }

  /** Limited-use; like randomColor but for cases where you already have three floats (r, g, and b) distributed how you want. This can be somewhat useful if you are using a "subrandom" or
    * "quasi-random" sequence to get 3D points and map them to colors.
    */
  def subrandomColor(r: Float, g: Float, b: Float): Float =
    java.lang.Float.intBitsToFloat(
      0xfe000000
        | (Math.min(Math.max(b * 256f, 0), 255.999f).toInt << 16 & 0xff0000)
        | (Math.min(Math.max(g * 256f, 0), 255.999f).toInt << 8 & 0xff00)
        | (Math.min(Math.max(r * 256f, 0), 255.999f).toInt & 0xff)
    )
}
