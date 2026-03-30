/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful
package hsluv

import sge.colorful.{ FloatColors, TrigTools }
import sge.graphics.Color
import sge.math.MathUtils

/** Contains code for manipulating colors as `int` and packed `float` values in the HSLuv color space. IPT has more color manipulation in the HSLuv color space.
  */
object ColorTools {

  /** Gets a packed float representation of a color given as 4 float components: I (intensity/lightness), P (protan), T (tritan), and alpha.
    */
  def hsluv(h: Float, s: Float, l: Float, alpha: Float): Float =
    java.lang.Float.intBitsToFloat(
      ((alpha * 255.999f).toInt << 24 & 0xfe000000) | ((l * 255.999f).toInt << 16 & 0xff0000)
        | ((s * 255.999f).toInt << 8 & 0xff00) | ((h * 255.999f).toInt & 0xff)
    )

  /** Converts a packed float HSLuv color to an RGBA8888 int. */
  def toRGBA8888(packed: Float): Int = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val r       = Math.min(Math.max(((0.999779f * i + 1.0709400f * p + 0.324891f * t) * 256.0).toInt, 0), 255)
    val g       = Math.min(Math.max(((1.000150f * i - 0.3777440f * p + 0.220439f * t) * 256.0).toInt, 0), 255)
    val b       = Math.min(Math.max(((0.999769f * i + 0.0629496f * p - 0.809638f * t) * 256.0).toInt, 0), 255)
    r << 24 | g << 16 | b << 8 | (decoded & 0xfe000000) >>> 24 | decoded >>> 31
  }

  /** Converts a packed float HSLuv color to a packed float in RGBA format. */
  def toRGBA(packed: Float): Float = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    val r       = Math.min(Math.max(((0.999779f * i + 1.0709400f * p + 0.324891f * t) * 256.0).toInt, 0), 255)
    val g       = Math.min(Math.max(((1.000150f * i - 0.3777440f * p + 0.220439f * t) * 256.0).toInt, 0), 255)
    val b       = Math.min(Math.max(((0.999769f * i + 0.0629496f * p - 0.809638f * t) * 256.0).toInt, 0), 255)
    java.lang.Float.intBitsToFloat(r | g << 8 | b << 16 | (decoded & 0xfe000000))
  }

  /** Writes an HSLuv packed float color into an RGBA8888 Color. */
  def toColor(editing: Color, packed: Float): Color = {
    val decoded = java.lang.Float.floatToRawIntBits(packed)
    val i       = (decoded & 0xff) / 255f
    val p       = ((decoded >>> 8 & 0xff) - 127.5f) / 127.5f
    val t       = ((decoded >>> 16 & 0xff) - 127.5f) / 127.5f
    editing.r = 0.999779f * i + 1.0709400f * p + 0.324891f * t
    editing.g = 1.000150f * i - 0.3777440f * p + 0.220439f * t
    editing.b = 0.999769f * i + 0.0629496f * p - 0.809638f * t
    editing.a = (decoded >>> 25) * 0.007874016f
    editing.clamp()
  }

  /** Takes an RGBA8888 int and converts to a packed float in HSLuv format. */
  def fromRGBA8888(rgba: Int): Float = {
    val r = (rgba >>> 24) * 0.003921569f
    val g = (rgba >>> 16 & 0xff) * 0.003921569f
    val b = (rgba >>> 8 & 0xff) * 0.003921569f
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max(((0.189786f * r + 0.576951f * g + 0.233221f * b) * 255.0f + 0.500f).toInt, 0), 255)
        | Math.min(Math.max(((0.669665f * r - 0.73741f * g + 0.0681367f * b) * 127.5f + 127.5f).toInt, 0), 255) << 8
        | Math.min(Math.max(((0.286498f * r + 0.655205f * g - 0.941748f * b) * 127.5f + 127.5f).toInt, 0), 255) << 16
        | (rgba & 0xfe) << 24
    )
  }

  /** Takes a packed float RGBA color and converts to a packed float in HSLuv format. */
  def fromRGBA(packed: Float): Float = {
    val abgr = java.lang.Float.floatToRawIntBits(packed)
    val r    = (abgr & 0xff) * 0.003921569f
    val g    = (abgr >>> 8 & 0xff) * 0.003921569f
    val b    = (abgr >>> 16 & 0xff) * 0.003921569f
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max(((0.189786f * r + 0.576951f * g + 0.233221f * b) * 255.999f).toInt, 0), 255)
        | Math.min(Math.max(((0.669665f * r - 0.73741f * g + 0.0681367f * b) * 127.5f + 127.5f).toInt, 0), 255) << 8
        | Math.min(Math.max(((0.286498f * r + 0.655205f * g - 0.941748f * b) * 127.5f + 127.5f).toInt, 0), 255) << 16
        | (abgr & 0xfe000000)
    )
  }

  /** Takes a Color (RGBA8888) and converts to a packed float in HSLuv format. */
  def fromColor(color: Color): Float =
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max(((0.189786f * color.r + 0.576951f * color.g + 0.233221f * color.b) * 255.0f + 0.500f).toInt, 0), 255)
        | Math.min(Math.max(((0.669665f * color.r - 0.73741f * color.g + 0.0681367f * color.b) * 127.5f + 127.5f).toInt, 0), 255) << 8
        | Math.min(Math.max(((0.286498f * color.r + 0.655205f * color.g - 0.941748f * color.b) * 127.5f + 127.5f).toInt, 0), 255) << 16
        | ((color.a * 255f).toInt << 24 & 0xfe000000)
    )

  /** Takes RGBA components from 0.0 to 1.0 and converts to a packed float in HSLuv format. */
  def fromRGBA(r: Float, g: Float, b: Float, a: Float): Float =
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max(((0.189786f * r + 0.576951f * g + 0.233221f * b) * 255.0f + 0.500f).toInt, 0), 255)
        | Math.min(Math.max(((0.669665f * r - 0.73741f * g + 0.0681367f * b) * 127.5f + 127.5f).toInt, 0), 255) << 8
        | Math.min(Math.max(((0.286498f * r + 0.655205f * g - 0.941748f * b) * 127.5f + 127.5f).toInt, 0), 255) << 16
        | ((a * 255f).toInt << 24 & 0xfe000000)
    )

  /** Gets the alpha channel value as an even int from 0 to 254. */
  def alphaInt(encoded: Float): Int =
    (java.lang.Float.floatToRawIntBits(encoded) & 0xfe000000) >>> 24

  /** Gets the alpha channel as a float from 0.0f to 1.0f. */
  def alpha(encoded: Float): Float =
    ((java.lang.Float.floatToRawIntBits(encoded) & 0xfe000000) >>> 24) * 0.003937008f

  /** The "intensity" of the given packed float in HSLuv format (lightness, 0 to 1). */
  def channelH(encoded: Float): Float =
    (java.lang.Float.floatToRawIntBits(encoded) & 0xff) / 255f

  /** The "protan" of the given packed float in HSLuv format (0 to 1). */
  def channelS(encoded: Float): Float =
    (java.lang.Float.floatToRawIntBits(encoded) >>> 8 & 0xff) / 255f

  /** The "tritan" of the given packed float in HSLuv format (0 to 1). */
  def channelL(encoded: Float): Float =
    (java.lang.Float.floatToRawIntBits(encoded) >>> 16 & 0xff) / 255f

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
    hsluv((s & 0xff) / 255f, ((s >>> 8 & 0xff) / 255f - 0.5f) * (1f - change) + 0.5f, ((s >>> 16 & 0xff) / 255f - 0.5f) * (1f - change) + 0.5f, (s >>> 25) / 127f)
  }

  /** Pushes the chromatic components of start away from grayscale by change (saturating). */
  def enrich(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start)
    limitToGamut((s & 0xff) / 255f, ((s >>> 8 & 0xff) / 255f - 0.5f) * (1f + change) + 0.5f, ((s >>> 16 & 0xff) / 255f - 0.5f) * (1f + change) + 0.5f, (s >>> 25) / 127f)
  }

  /** Gets a color as an HSLuv packed float given floats for HSL and opacity. */
  def floatGetHSL(hue: Float, saturation: Float, lightness: Float, opacity: Float): Float =
    if (lightness <= 0.001f) {
      java.lang.Float.intBitsToFloat(((opacity * 255f).toInt << 24 & 0xfe000000) | 0x7f7f00)
    } else {
      fromRGBA(FloatColors.hsl2rgb(hue, saturation, lightness, opacity))
    }

  /** Checks whether the given HSLuv color (I, P, T, alpha) is in-gamut; if not, brings it inside. */
  def limitToGamut(I: Float, P: Float, T: Float, alpha: Float): Float =
    hsluv(Math.min(Math.max(I, 0f), 1f), Math.min(Math.max(P, 0f), 1f), Math.min(Math.max(T, 0f), 1f), Math.min(Math.max(alpha, 0f), 1f))

  /** Checks whether the given HSLuv packed float is in-gamut; if not, brings it inside. */
  def limitToGamut(packed: Float): Float =
    packed

  /** Makes the additive HSLuv color cause less of a change when used as a tint. */
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

  // HSLuv-specific constants
  private val m: Array[Array[Float]] = Array(
    Array(+3.240969941904521f, -1.537383177570093f, -0.498610760293000f),
    Array(-0.969243636280870f, +1.875967501507720f, +0.041555057407175f),
    Array(+0.055630079696993f, -0.203976958888970f, +1.056971514242878f)
  )
  @scala.annotation.nowarn("msg=unused") // used in extended version
  private val refU: Float = 0.19783000664283f
  @scala.annotation.nowarn("msg=unused") // used in extended version
  private val refV:    Float = 0.46831999493879f
  private val kappa:   Float = 9.032962962f
  private val epsilon: Float = 0.0088564516f

  private def intersectLength(sin: Float, cos: Float, line1: Float, line2: Float): Float =
    line2 / (sin - line1 * cos)

  /** Gets the approximate maximum chroma for a given hue and lightness in HSLuv. */
  def chromaLimit(hue: Float, lightness: Float): Float = {
    val h    = hue - MathUtils.floor(hue)
    val sin  = TrigTools.sinTurns(h)
    val cos  = TrigTools.cosTurns(h)
    var sub1 = (lightness + 0.16f) / 1.16f
    sub1 *= sub1 * sub1
    val sub2 = if (sub1 > epsilon) sub1 else lightness / kappa
    var min  = Float.MaxValue
    var i    = 0
    while (i < 3) {
      val m1 = m(i)(0) * sub2; var m2 = m(i)(1) * sub2; val m3 = m(i)(2) * sub2
      var t  = 0
      while (t < 2) {
        m2 -= t
        val top1   = 2845.17f * m1 - 948.39f * m3
        val top2   = (8384.22f * m3 + 7698.60f * m2 + 7317.18f * m1) * lightness
        val bottom = 6322.60f * m3 - 1264.52f * m2
        val length = intersectLength(sin, cos, top1 / bottom, top2 / bottom)
        if (length >= 0) min = Math.min(min, length)
        // Reset m2 for next iteration
        if (t == 0) m2 = m(i)(1) * sub2
        t += 1
      }
      i += 1
    }
    min
  }

  /** Interpolates from the packed float HSLuv color start towards end by change, handling hue wrapping correctly by converting through LUV space.
    */
  def lerpFloatColors(start: Float, end: Float, change: Float): Float = {
    val s  = java.lang.Float.floatToRawIntBits(start); val e = java.lang.Float.floatToRawIntBits(end)
    val hs = s & 0xff; val ss                                = (s >>> 8) & 0xff; val ls = (s >>> 16) & 0xff; val as = s >>> 24 & 0xfe
    val he = e & 0xff; val se                                = (e >>> 8) & 0xff; val le = (e >>> 16) & 0xff; val ae = e >>> 24 & 0xfe

    val Hs = hs / 255f; var Cs = 0f; var Ls = 0f
    if (ls == 255) { Ls = 1f; Cs = 0f }
    else if (ls == 0) { Ls = 0f; Cs = 0f }
    else { Ls = ls / 255f; Cs = chromaLimit(Hs, Ls) * (ss / 255f) }
    val Us = TrigTools.cosTurns(Hs) * Cs
    val Vs = TrigTools.sinTurns(Hs) * Cs

    val He = he / 255f; var Ce = 0f; var Le = 0f
    if (le == 255) { Le = 1f; Ce = 0f }
    else if (le == 0) { Le = 0f; Ce = 0f }
    else { Le = le / 255f; Ce = chromaLimit(He, Le) * (se / 255f) }
    val Ue = TrigTools.cosTurns(He) * Ce
    val Ve = TrigTools.sinTurns(He) * Ce

    val L = Ls + change * (Le - Ls)
    val U = Us + change * (Ue - Us)
    val V = Vs + change * (Ve - Vs)

    val H = TrigTools.atan2Turns(V, U)

    if (L > 0.99999f) {
      Palette.WHITE
    } else if (L < 0.00001f) {
      Palette.BLACK
    } else {
      val S = Math.min(Math.sqrt(U * U + V * V).toFloat / chromaLimit(H, L), 1f)
      java.lang.Float.intBitsToFloat(
        Math.min(Math.max((H * 255.999f).toInt, 0), 255)
          | Math.min(Math.max((S * 255.999f).toInt, 0), 255) << 8
          | Math.min(Math.max((L * 255.999f).toInt, 0), 255) << 16
          | (((as + change * (ae - as)).toInt & 0xfe) << 24)
      )
    }
  }
}
