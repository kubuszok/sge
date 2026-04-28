/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 376
 * Covenant-baseline-methods: FloatColors,b,bits,c,ch,chroma,cv,d,decoded,e,end,g,hcl2rgbInt,hsl2rgb,hsl2rgbInt,hslColor,l,lerpFloatColors,lerpFloatColorsBlended,lv,max,min,mix,multiplyAlpha,r,rgb2hclInt,rgb2hsl,rgb2hslInt,s,setAlpha,sz,unevenMix,v,vScaled,x,y,ye,ys,z
 * Covenant-source-reference: com/github/tommyettinger/colorful/FloatColors.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: e4a5fd960eef746ca5aa826063432fb79666d74f
 */
package sge
package colorful

import sge.graphics.Color
import sge.math.MathUtils

/** Various utility methods for working with colors encoded as packed floats in any of the formats this can use.
  */
object FloatColors {

  /** Converts the four HSLA components, each in the 0.0 to 1.0 range, to a packed float in RGBA format.
    * @param h
    *   hue, from 0.0 to 1.0
    * @param s
    *   saturation, from 0.0 to 1.0
    * @param l
    *   lightness, from 0.0 to 1.0
    * @param a
    *   alpha, from 0.0 to 1.0
    * @return
    *   an RGBA-format packed float
    */
  def hsl2rgb(h: Float, s: Float, l: Float, a: Float): Float = {
    val x = Math.min(Math.max(Math.abs(h * 6f - 3f) - 1f, 0f), 1f)
    var y = h + (2f / 3f)
    var z = h + (1f / 3f)
    y -= y.toInt
    z -= z.toInt
    y = Math.min(Math.max(Math.abs(y * 6f - 3f) - 1f, 0f), 1f)
    z = Math.min(Math.max(Math.abs(z * 6f - 3f) - 1f, 0f), 1f)
    val v       = l + s * Math.min(l, 1f - l)
    val d       = 2f * (1f - l / (v + 1e-10f))
    val vScaled = v * 255f
    java.lang.Float.intBitsToFloat(
      (a * 127f).toInt << 25
        | (vScaled * MathUtils.lerp(1f, z, d)).toInt << 16
        | (vScaled * MathUtils.lerp(1f, y, d)).toInt << 8
        | (vScaled * MathUtils.lerp(1f, x, d)).toInt
    )
  }

  /** Converts a packed float in HSLA format to a packed float in RGBA format.
    */
  def hsl2rgb(hsla: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(hsla)
    hsl2rgb((decoded & 0xff) / 255f, (decoded >>> 8 & 0xff) / 255f, (decoded >>> 16 & 0xff) / 255f, (decoded >>> 25) / 127f)
  }

  /** Converts an int in HSLA format (hue, saturation, lightness, alpha) to an int in RGBA8888 format.
    * @param hsla
    *   an HSLA-format int
    * @return
    *   an RGBA8888-format int
    */
  def hsl2rgbInt(hsla: Int): Int =
    hsl2rgbInt((hsla >>> 24) * (1f / 255f), (hsla >>> 16 & 0xff) * (1f / 255f), (hsla >>> 16 & 0xff) * (1f / 255f), (hsla & 0xfe) * (1f / 254f))

  /** Converts the four HSLA components, each in the 0.0 to 1.0 range, to an int in RGBA8888 format. [[https://stackoverflow.com/a/64090995 From this StackOverflow answer by Kamil Kiełczewski]]
    * @param h
    *   hue, from 0.0 to 1.0
    * @param s
    *   saturation, from 0.0 to 1.0
    * @param l
    *   lightness, from 0.0 to 1.0
    * @param a
    *   alpha, from 0.0 to 1.0
    * @return
    *   an RGBA8888-format int
    */
  def hsl2rgbInt(h: Float, s: Float, l: Float, a: Float): Int = {
    val c  = s * Math.min(l, 1f - l) * 3071.999f /* 12 * 256, minus epsilon */
    val lv = l * 255.999f
    var r  = h
    r = lv - c * Math.max(Math.min(Math.min(r - 0.25f, 0.75f - r), 1f / 12f), -1f / 12f)
    var g = h + (2f / 3f); g -= g.toInt
    g = lv - c * Math.max(Math.min(Math.min(g - 0.25f, 0.75f - g), 1f / 12f), -1f / 12f)
    var b = h + (1f / 3f); b -= b.toInt
    b = lv - c * Math.max(Math.min(Math.min(b - 0.25f, 0.75f - b), 1f / 12f), -1f / 12f)
    r.toInt << 24 | g.toInt << 16 | b.toInt << 8 | (a * 255.999f).toInt
  }

  /** Converts an int in RGBA8888 format to an int in "HSLA format" (hue, saturation, lightness, alpha).
    * @param rgba
    *   an RGBA8888-format int
    * @return
    *   an "HSLA-format" int
    */
  def rgb2hslInt(rgba: Int): Int =
    rgb2hslInt((rgba >>> 24) * (1f / 255f), (rgba >>> 16 & 0xff) * (1f / 255f), (rgba >>> 16 & 0xff) * (1f / 255f), (rgba & 0xfe) * (1f / 254f))

  /** Converts the four RGBA components, each in the 0.0 to 1.0 range, to an int in "HSLA format". [[https://stackoverflow.com/a/64090995 From this StackOverflow answer by Kamil Kiełczewski]]
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
  def rgb2hslInt(r: Float, g: Float, b: Float, a: Float): Int = {
    val max    = Math.max(Math.max(r, g), b)
    val min    = Math.min(Math.min(r, g), b)
    val chroma = max - min
    if (chroma < (1f / 256f)) {
      (max * 255.999f).toInt << 8 | (a * 255.999f).toInt
    } else {
      val iDelta = 0.16666667f / chroma
      val hue    = 1f + (if (max == r) (g - b) * iDelta else if (max == g) (1f / 3f) + (b - r) * iDelta else (2f / 3f) + (r - g) * iDelta)
      ((hue - hue.toInt) * 255.999f).toInt << 24 |
        (chroma / (1f - Math.abs(max + min - 1f)) * 255.999f).toInt << 16 |
        ((max + min) * 127.25f + 0.5f).toInt << 8 |
        (a * 255.999f).toInt
    }
  }

  /** Converts an int in HCLA format (hue, chroma, lightness, alpha) to an int in RGBA8888 format. Here, chroma is similar and related to saturation, but isn't scaled to fit in the 0.0 to 1.0 range
    * for all lightness values; instead, 0.5 lightness permits chroma up to 1.0, and as lightness approaches 0.0 or 1.0, chroma has its maximum value shrink down to 0.
    * @param hsla
    *   an HCLA-format int
    * @return
    *   an RGBA8888-format int
    */
  def hcl2rgbInt(hsla: Int): Int =
    hcl2rgbInt((hsla >>> 24) * (1f / 255f), (hsla >>> 16 & 0xff) * (1f / 255f), (hsla >>> 16 & 0xff) * (1f / 255f), (hsla & 0xfe) * (1f / 254f))

  /** Converts the four HCLA components, each in the 0.0 to 1.0 range, to an int in RGBA8888 format. Here, chroma `c` is similar and related to saturation, but isn't scaled to fit in the 0.0 to 1.0
    * range for all lightness values; instead, 0.5 lightness permits chroma up to 1.0, and as lightness approaches 0.0 or 1.0, chroma has its maximum value shrink down to 0.
    * [[https://stackoverflow.com/a/64090995 From this StackOverflow answer by Kamil Kiełczewski]]
    * @param h
    *   hue, from 0.0 to 1.0
    * @param c
    *   chroma, from 0.0 to 1.0
    * @param l
    *   lightness, from 0.0 to 1.0
    * @param a
    *   alpha, from 0.0 to 1.0
    * @return
    *   an RGBA8888-format int
    */
  def hcl2rgbInt(h: Float, c: Float, l: Float, a: Float): Int = {
    val cv = Math.min(c, 2f * Math.min(l, 1f - l)) * 1535.999f /* 12 * 256 / 2.0f, minus epsilon */
    val lv = l * 255.999f
    var r  = h
    r = lv - cv * Math.max(Math.min(Math.min(r - 0.25f, 0.75f - r), 1f / 12f), -1f / 12f)
    var g = h + (2f / 3f); g -= g.toInt
    g = lv - cv * Math.max(Math.min(Math.min(g - 0.25f, 0.75f - g), 1f / 12f), -1f / 12f)
    var b = h + (1f / 3f); b -= b.toInt
    b = lv - cv * Math.max(Math.min(Math.min(b - 0.25f, 0.75f - b), 1f / 12f), -1f / 12f)
    r.toInt << 24 | g.toInt << 16 | b.toInt << 8 | (a * 255.999f).toInt
  }

  /** Converts an int in RGBA8888 format to an int in "HCLA format" (hue, chroma, lightness, alpha). Here, chroma is similar and related to saturation, but isn't scaled to fit in the 0.0 to 1.0 range
    * for all lightness values; instead, 0.5 lightness permits chroma up to 1.0, and as lightness approaches 0.0 or 1.0, chroma has its maximum value shrink down to 0.
    * @param rgba
    *   an RGBA8888-format int
    * @return
    *   an "HCLA-format" int
    */
  def rgb2hclInt(rgba: Int): Int =
    rgb2hclInt((rgba >>> 24) * (1f / 255f), (rgba >>> 16 & 0xff) * (1f / 255f), (rgba >>> 16 & 0xff) * (1f / 255f), (rgba & 0xfe) * (1f / 254f))

  /** Converts the four RGBA components, each in the 0.0 to 1.0 range, to an int in "HCLA format" (hue, chroma, lightness, alpha). Here, chroma is similar and related to saturation, but isn't scaled
    * to fit in the 0.0 to 1.0 range for all lightness values; instead, 0.5 lightness permits chroma up to 1.0, and as lightness approaches 0.0 or 1.0, chroma has its maximum value shrink down to 0.
    * [[https://stackoverflow.com/a/64090995 From this StackOverflow answer by Kamil Kiełczewski]]
    * @param r
    *   red, from 0.0 to 1.0
    * @param g
    *   green, from 0.0 to 1.0
    * @param b
    *   blue, from 0.0 to 1.0
    * @param a
    *   alpha, from 0.0 to 1.0
    * @return
    *   an "HCLA-format" int
    */
  def rgb2hclInt(r: Float, g: Float, b: Float, a: Float): Int = {
    val max    = Math.max(Math.max(r, g), b)
    val min    = Math.min(Math.min(r, g), b)
    val chroma = max - min
    if (chroma < (1f / 256f)) {
      (max * 255.999f).toInt << 8 | (a * 255.999f).toInt
    } else {
      val iDelta = 0.16666667f / chroma
      val hue    = 1f + (if (max == r) (g - b) * iDelta else if (max == g) (1f / 3f) + (b - r) * iDelta else (2f / 3f) + (r - g) * iDelta)
      ((hue - hue.toInt) * 255.999f).toInt << 24 |
        (chroma * 255.999f).toInt << 16 |
        ((max + min) * 127.25f + 0.5f).toInt << 8 |
        (a * 255.999f).toInt
    }
  }

  /** Converts the four HSLA components, each in the 0.0 to 1.0 range, to RGBA and assigns them into changing.
    * @param changing
    *   a non-null Color that will be modified
    * @param h
    *   hue, from 0.0 to 1.0
    * @param s
    *   saturation, from 0.0 to 1.0
    * @param l
    *   lightness, from 0.0 to 1.0
    * @param a
    *   alpha, from 0.0 to 1.0
    * @return
    *   changing, after assignment
    */
  def hslColor(changing: Color, h: Float, s: Float, l: Float, a: Float): Color = {
    val x = Math.min(Math.max(Math.abs(h * 6f - 3f) - 1f, 0f), 1f)
    var y = h + (2f / 3f)
    var z = h + (1f / 3f)
    y -= y.toInt
    z -= z.toInt
    y = Math.min(Math.max(Math.abs(y * 6f - 3f) - 1f, 0f), 1f)
    z = Math.min(Math.max(Math.abs(z * 6f - 3f) - 1f, 0f), 1f)
    val v = l + s * Math.min(l, 1f - l)
    val d = 2f * (1f - l / (v + 1e-10f))
    changing.set(v * MathUtils.lerp(1f, x, d), v * MathUtils.lerp(1f, y, d), v * MathUtils.lerp(1f, z, d), a)
    changing
  }

  /** Given a color stored as a packed float and an alpha multiplier, this makes a packed float color that has the same channels but has its own alpha multiplied by `alpha`.
    */
  def multiplyAlpha(encodedColor: Float, alpha: Float): Float = {
    val bits = java.lang.Float.floatToRawIntBits(encodedColor)
    java.lang.Float.intBitsToFloat(
      bits & 0xffffff
        | (Math.min(Math.max(((bits >>> 24) * alpha).toInt, 0), 255) << 24 & 0xfe000000)
    )
  }

  /** Given a color stored as a packed float, and a desired alpha to set for that color, this makes a new float that has the same channels but has been set to the given alpha.
    */
  def setAlpha(encodedColor: Float, alpha: Float): Float =
    java.lang.Float.intBitsToFloat(
      java.lang.Float.floatToRawIntBits(encodedColor) & 0xffffff
        | (Math.min(Math.max((255f * alpha).toInt, 0), 255) << 24 & 0xfe000000)
    )

  /** Interpolates from the packed float color start towards end by change. Both start and end should be packed colors, and change can be between 0f (keep start) and 1f (only use end).
    */
  def lerpFloatColors(start: Float, end: Float, change: Float): Float = {
    val s  = java.lang.Float.floatToRawIntBits(start)
    val e  = java.lang.Float.floatToRawIntBits(end)
    val ys = s & 0xff; val cws = (s >>> 8) & 0xff; val cms = (s >>> 16) & 0xff; val as = s >>> 25
    val ye = e & 0xff; val cwe = (e >>> 8) & 0xff; val cme = (e >>> 16) & 0xff; val ae = e >>> 25
    java.lang.Float.intBitsToFloat(
      ((ys + change * (ye - ys)).toInt & 0xff)
        | (((cws + change * (cwe - cws)).toInt & 0xff) << 8)
        | (((cms + change * (cme - cms)).toInt & 0xff) << 16)
        | (((as + change * (ae - as)).toInt & 0x7f) << 25)
    )
  }

  /** Interpolates from start towards end by change, but keeps the alpha of start and uses the alpha of end as an extra factor that can affect how much to change.
    */
  def lerpFloatColorsBlended(start: Float, end: Float, change: Float): Float = {
    val s  = java.lang.Float.floatToRawIntBits(start)
    val e  = java.lang.Float.floatToRawIntBits(end)
    val ys = s & 0xff; val cws = (s >>> 8) & 0xff; val cms = (s >>> 16) & 0xff; val as = s & 0xfe000000
    val ye = e & 0xff; val cwe = (e >>> 8) & 0xff; val cme = (e >>> 16) & 0xff
    val ch = change * (e >>> 25) * 0.007874016f
    java.lang.Float.intBitsToFloat(
      ((ys + ch * (ye - ys)).toInt & 0xff)
        | (((cws + ch * (cwe - cws)).toInt & 0xff) << 8)
        | (((cms + ch * (cme - cms)).toInt & 0xff) << 16)
        | as
    )
  }

  /** Returns a 1:1 mix of color0 and color1. */
  def mix(color0: Float, color1: Float): Float =
    lerpFloatColors(color0, color1, 0.5f)

  /** Returns a 1:1:1 mix of color0, color1, and color2. */
  def mix(color0: Float, color1: Float, color2: Float): Float =
    lerpFloatColors(lerpFloatColors(color0, color1, 0.5f), color2, 0.33333f)

  /** Returns a 1:1:1:1 mix of color0, color1, color2, and color3. */
  def mix(color0: Float, color1: Float, color2: Float, color3: Float): Float =
    lerpFloatColors(lerpFloatColors(lerpFloatColors(color0, color1, 0.5f), color2, 0.33333f), color3, 0.25f)

  /** Given several colors, gets an even mix of all colors in equal measure. */
  def mix(colors: Float*): Float =
    if (colors.isEmpty) 0f
    else {
      var result = colors(0)
      var i      = 1
      while (i < colors.length) {
        result = lerpFloatColors(result, colors(i), 1f / (i + 1f))
        i += 1
      }
      result
    }

  /** Given several colors in an array, gets an even mix of all colors from offset to offset+size. */
  def mix(colors: Array[Float], offset: Int, size: Int): Float = {
    val end = offset + size
    if (colors == null || colors.length < end || offset < 0 || size <= 0) 0f
    else {
      var result = colors(offset)
      var i      = offset + 1
      var denom  = 2
      while (i < end) {
        result = lerpFloatColors(result, colors(i), 1f / denom)
        i += 1
        denom += 1
      }
      result
    }
  }

  /** Mixes any number of colors with arbitrary weights per-color. Takes an array of alternating floats representing colors and weights, as with `color, weight, color, weight...`.
    */
  def unevenMix(colors: Float*): Float =
    if (colors.isEmpty) 0f
    else if (colors.length <= 2) colors(0)
    else unevenMix(colors.toArray, 0, colors.length)

  /** Mixes any number of colors with arbitrary weights per-color from an array.
    */
  def unevenMix(colors: Array[Float], offset: Int, size: Int): Float = {
    val sz  = size & -2
    val end = offset + sz
    if (colors == null || colors.length < end || offset < 0 || sz <= 0) 0f
    else {
      var result  = colors(offset)
      var current = colors(offset + 1)
      var total   = current
      var i       = offset + 3
      while (i < end) {
        total += colors(i)
        i += 2
      }
      val invTotal = 1f / total
      current *= invTotal
      i = offset + 3
      while (i < end) {
        val mixColor = colors(i - 1)
        val weight   = colors(i) * invTotal
        current += weight
        result = lerpFloatColors(result, mixColor, weight / current)
        i += 2
      }
      result
    }
  }

  /** Converts the four RGBA components to a packed float in "HSLA format" (hue, saturation, lightness, alpha).
    */
  def rgb2hsl(r: Float, g: Float, b: Float, a: Float): Float = {
    var x = 0f; var y = 0f; var z = 0f; var w = 0f
    if (g < b) { x = b; y = g; z = -1f; w = 2f / 3f }
    else { x = g; y = b; z = 0f; w = -1f / 3f }
    if (r < x) { z = w; w = r }
    else { w = x; x = r }
    val d = x - Math.min(w, y)
    val l = x * (1f - 0.5f * d / (x + 1e-10f))
    Color.toFloatBits(Math.abs(z + (w - y) / (6f * d + 1e-10f)), (x - l) / (Math.min(l, 1f - l) + 1e-10f), l, a)
  }

  /** Converts a packed float in RGBA format to a packed float in "HSLA format".
    */
  def rgb2hsl(rgba: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(rgba)
    rgb2hsl((decoded & 0xff) / 255f, (decoded >>> 8 & 0xff) / 255f, (decoded >>> 16 & 0xff) / 255f, (decoded >>> 25) / 127f)
  }
}
