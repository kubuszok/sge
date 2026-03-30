/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful
package cielab

import sge.colorful.FloatColors
import sge.graphics.Color

/** Contains code for manipulating colors as `int` and packed `float` values in the CIELAB color space. IPT has more color manipulation in the CIELAB color space.
  */
object ColorTools {

  /** Gets a packed float representation of a color given as 4 float components: I (intensity/lightness), P (protan), T (tritan), and alpha.
    */
  def cielab(l: Float, a: Float, b: Float, alpha: Float): Float =
    java.lang.Float.intBitsToFloat(
      ((alpha * 255.999f).toInt << 24 & 0xfe000000) | ((b * 255.999f).toInt << 16 & 0xff0000)
        | ((a * 255.999f).toInt << 8 & 0xff00) | ((l * 255.999f).toInt & 0xff)
    )

  /** Converts a packed float CIELAB color to an RGBA8888 int. */
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

  /** Converts a packed float CIELAB color to a packed float in RGBA format. */
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

  /** Writes an CIELAB packed float color into an RGBA8888 Color. */
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

  /** Takes an RGBA8888 int and converts to a packed float in CIELAB format. */
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

  /** Takes a packed float RGBA color and converts to a packed float in CIELAB format. */
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

  /** Takes a Color (RGBA8888) and converts to a packed float in CIELAB format. */
  def fromColor(color: Color): Float =
    java.lang.Float.intBitsToFloat(
      Math.min(Math.max(((0.189786f * color.r + 0.576951f * color.g + 0.233221f * color.b) * 255.0f + 0.500f).toInt, 0), 255)
        | Math.min(Math.max(((0.669665f * color.r - 0.73741f * color.g + 0.0681367f * color.b) * 127.5f + 127.5f).toInt, 0), 255) << 8
        | Math.min(Math.max(((0.286498f * color.r + 0.655205f * color.g - 0.941748f * color.b) * 127.5f + 127.5f).toInt, 0), 255) << 16
        | ((color.a * 255f).toInt << 24 & 0xfe000000)
    )

  /** Takes RGBA components from 0.0 to 1.0 and converts to a packed float in CIELAB format. */
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

  /** The "intensity" of the given packed float in CIELAB format (lightness, 0 to 1). */
  def channelL(encoded: Float): Float =
    (java.lang.Float.floatToRawIntBits(encoded) & 0xff) / 255f

  /** The "protan" of the given packed float in CIELAB format (0 to 1). */
  def channelA(encoded: Float): Float =
    (java.lang.Float.floatToRawIntBits(encoded) >>> 8 & 0xff) / 255f

  /** The "tritan" of the given packed float in CIELAB format (0 to 1). */
  def channelB(encoded: Float): Float =
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
    cielab((s & 0xff) / 255f, ((s >>> 8 & 0xff) / 255f - 0.5f) * (1f - change) + 0.5f, ((s >>> 16 & 0xff) / 255f - 0.5f) * (1f - change) + 0.5f, (s >>> 25) / 127f)
  }

  /** Pushes the chromatic components of start away from grayscale by change (saturating). */
  def enrich(start: Float, change: Float): Float = {
    val s = java.lang.Float.floatToRawIntBits(start)
    limitToGamut((s & 0xff) / 255f, ((s >>> 8 & 0xff) / 255f - 0.5f) * (1f + change) + 0.5f, ((s >>> 16 & 0xff) / 255f - 0.5f) * (1f + change) + 0.5f, (s >>> 25) / 127f)
  }

  /** Gets a color as an CIELAB packed float given floats for HSL and opacity. */
  def floatGetHSL(hue: Float, saturation: Float, lightness: Float, opacity: Float): Float =
    if (lightness <= 0.001f) {
      java.lang.Float.intBitsToFloat(((opacity * 255f).toInt << 24 & 0xfe000000) | 0x7f7f00)
    } else {
      fromRGBA(FloatColors.hsl2rgb(hue, saturation, lightness, opacity))
    }

  /** Checks whether the given CIELAB color (I, P, T, alpha) is in-gamut; if not, brings it inside. */
  def limitToGamut(I: Float, P: Float, T: Float, alpha: Float): Float =
    cielab(Math.min(Math.max(I, 0f), 1f), Math.min(Math.max(P, 0f), 1f), Math.min(Math.max(T, 0f), 1f), Math.min(Math.max(alpha, 0f), 1f))

  /** Checks whether the given CIELAB packed float is in-gamut; if not, brings it inside. */
  def limitToGamut(packed: Float): Float =
    packed

  /** Makes the additive CIELAB color cause less of a change when used as a tint. */
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
}
