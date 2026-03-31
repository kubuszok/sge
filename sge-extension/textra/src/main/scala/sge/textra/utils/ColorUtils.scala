/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/utils/ColorUtils.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package textra
package utils

import sge.graphics.{ Color, Colors }
import sge.math.MathUtils
import sge.utils.NumberUtils

/** A few static methods for commonly-used color handling tasks. */
object ColorUtils {

  /** Converts the four HSLA components, each in the 0.0 to 1.0 range, to an int in RGBA8888 format. */
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

  /** Converts the four RGBA components to an int in HSLA format. */
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

  /** Converts HSBA/HSVA components to RGBA8888. */
  def hsb2rgb(h: Float, s: Float, b: Float, a: Float): Int = {
    val hue = h - MathUtils.floor(h)
    val x   = Math.min(Math.max(Math.abs(hue * 6f - 3f) - 1f, 0f), 1f)
    val y0  = hue + (2f / 3f)
    val z0  = hue + (1f / 3f)
    val y   = Math.min(Math.max(Math.abs((y0 - y0.toInt) * 6f - 3f) - 1f, 0f), 1f)
    val z   = Math.min(Math.max(Math.abs((z0 - z0.toInt) * 6f - 3f) - 1f, 0f), 1f)
    Color.rgba8888(b * MathUtils.lerp(1f, x, s), b * MathUtils.lerp(1f, y, s), b * MathUtils.lerp(1f, z, s), a)
  }

  def channel(color: Int, channel: Int): Float =
    (color >>> 24 - ((channel & 3) << 3) & 255) / 255f

  def channelInt(color: Int, channel: Int): Int =
    color >>> 24 - ((channel & 3) << 3) & 255

  /** Interpolates from the RGBA8888 int color start towards end by change. */
  def lerpColors(s: Int, e: Int, change: Float): Int = {
    val sA = s & 0xfe; val sB = (s >>> 8) & 0xff; val sG = (s >>> 16) & 0xff; val sR = (s >>> 24) & 0xff
    val eA = e & 0xfe; val eB = (e >>> 8) & 0xff; val eG = (e >>> 16) & 0xff; val eR = (e >>> 24) & 0xff
    (((sR + change * (eR - sR)).toInt & 0xff) << 24) |
      (((sG + change * (eG - sG)).toInt & 0xff) << 16) |
      (((sB + change * (eB - sB)).toInt & 0xff) << 8) |
      ((sA + change * (eA - sA)).toInt & 0xfe)
  }

  /** Interpolates from the ABGR8888 packed float color start towards end by change. */
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

  def multiplyAlpha(color: Int, alphaMultiplier: Float): Int =
    (color & 0xffffff00) | (((color & 0xff) * alphaMultiplier).toInt & 0xfe)

  def lighten(start: Int, change: Float): Int = {
    val r = start >>> 24; val g = start >>> 16 & 0xff; val b = start >>> 8 & 0xff
    val a = start & 0x000000fe
    ((r + (0xff - r) * change).toInt & 0xff) << 24 |
      ((g + (0xff - g) * change).toInt & 0xff) << 16 |
      ((b + (0xff - b) * change).toInt & 0xff) << 8 | a
  }

  def darken(start: Int, change: Float): Int = {
    val r  = start >>> 24; val g = start >>> 16 & 0xff; val b = start >>> 8 & 0xff
    val a  = start & 0x000000fe
    val ch = 1f - change
    ((r * ch).toInt & 0xff) << 24 | ((g * ch).toInt & 0xff) << 16 | ((b * ch).toInt & 0xff) << 8 | a
  }

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

  /** Looks up a color name from libGDX Colors. Returns 256 if not found. */
  def lookupInColors(key: String, beginIndex: Int, endIndex: Int): Int = {
    val name = key.substring(beginIndex, endIndex)
    Colors.get(name).fold(256)(c => Color.rgba8888(c))
  }

  /** Parses a description such as "peach red" or "DARK DULLEST GREEN". Stub for now. */
  def describe(key: String, beginIndex: Int, endIndex: Int): Int = {
    // Simplified: just try looking up as a single color name first
    val name   = key.substring(beginIndex, endIndex).trim
    val result = lookupInColors(name, 0, name.length)
    if (result != 256) result
    else {
      // Try in Palette
      val paletteColor = Palette.NAMED.get(name)
      if (paletteColor.isDefined) paletteColor.get
      else 256
    }
  }
}
