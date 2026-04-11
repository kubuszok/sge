/*
 * Ported from anim8-gdx - https://github.com/tommyettinger/anim8-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package anim8

import sge.graphics.{ Color, Pixmap }

import ConstantData.ENCODED_SNUGGLY

/** This is just like [[PaletteReducer]], except that it uses a higher-quality, slower color difference calculation when creating a palette. This calculates the difference between colors using
  * Euclidean distance in the Oklab color space, rather than what PaletteReducer uses, which is Euclidean distance in RGB.
  *
  * A quirk of how this calculates the color difference between colors A and B is that it avoids converting both A and B to Oklab. Instead, it gets the absolute value of the difference between the RGB
  * channels, and converts that to Oklab, then just gets its magnitude.
  *
  * This tends to use fewer very-dark colors than PaletteReducer or [[FastPalette]], but seems to avoid the problem case for those two where if a maximum of n colors are requested, fewer than n unique
  * colors might be found for the palette.
  */
class QualityPalette extends PaletteReducer {

  /** Constructs a default QualityPalette that uses the "Snuggly" 255-color-plus-transparent palette. */
  exact(PaletteReducer.SNUGGLY, ENCODED_SNUGGLY)

  /** Constructs a QualityPalette that uses the given array of RGBA8888 ints as a palette.
    *
    * @param rgbaPalette
    *   an array of RGBA8888 ints to use as a palette
    */
  def this(rgbaPalette: Array[Int]) = {
    this()
    if (rgbaPalette == null) { // null guard for Java interop @nowarn
      exact(PaletteReducer.SNUGGLY, ENCODED_SNUGGLY)
    } else {
      exact(rgbaPalette)
    }
  }

  /** Constructs a QualityPalette that uses the given array of RGBA8888 ints as a palette.
    *
    * @param rgbaPalette
    *   an array of RGBA8888 ints to use as a palette
    * @param limit
    *   how many int items to use from rgbaPalette (this always starts at index 0)
    */
  def this(rgbaPalette: Array[Int], limit: Int) = {
    this()
    if (rgbaPalette == null) { // null guard for Java interop @nowarn
      exact(PaletteReducer.SNUGGLY, ENCODED_SNUGGLY)
    } else {
      exact(rgbaPalette, limit)
    }
  }

  /** Constructs a QualityPalette that uses the given array of Color objects as a palette.
    *
    * @param colorPalette
    *   an array of Color objects to use as a palette
    */
  def this(colorPalette: Array[Color]) = {
    this()
    if (colorPalette == null) { // null guard for Java interop @nowarn
      exact(PaletteReducer.SNUGGLY, ENCODED_SNUGGLY)
    } else {
      exact(colorPalette)
    }
  }

  /** Constructs a QualityPalette that uses the given array of Color objects as a palette.
    *
    * @param colorPalette
    *   an array of Color objects to use as a palette
    * @param limit
    *   how many Color items to use
    */
  def this(colorPalette: Array[Color], limit: Int) = {
    this()
    if (colorPalette == null) { // null guard for Java interop @nowarn
      exact(PaletteReducer.SNUGGLY, ENCODED_SNUGGLY)
    } else {
      exact(colorPalette, limit)
    }
  }

  /** Constructs a QualityPalette that analyzes the given Pixmap for color count and frequency to generate a palette.
    *
    * @param pixmap
    *   a Pixmap to analyze in detail to produce a palette
    */
  def this(pixmap: Pixmap) = {
    this()
    if (pixmap == null) { // null guard for Java interop @nowarn
      exact(PaletteReducer.SNUGGLY, ENCODED_SNUGGLY)
    } else {
      analyze(pixmap)
    }
  }

  /** Constructs a QualityPalette that analyzes the given Pixmaps for color count and frequency to generate a palette.
    *
    * @param pixmaps
    *   an Array of Pixmap to analyze in detail to produce a palette
    */
  def this(pixmaps: Array[Pixmap]) = {
    this()
    if (pixmaps == null) { // null guard for Java interop @nowarn
      exact(PaletteReducer.SNUGGLY, ENCODED_SNUGGLY)
    } else {
      analyze(pixmaps)
    }
  }

  /** Constructs a QualityPalette with pre-loaded color data.
    *
    * @param palette
    *   an array of RGBA8888 ints to use as a palette
    * @param preload
    *   a byte array containing preload data
    */
  def this(palette: Array[Int], preload: Array[Byte]) = {
    this()
    exact(palette, preload)
  }

  /** Constructs a QualityPalette that analyzes the given Pixmap with a threshold.
    *
    * @param pixmap
    *   a Pixmap to analyze in detail to produce a palette
    * @param threshold
    *   the minimum difference between colors required to put them in the palette (default 300)
    */
  def this(pixmap: Pixmap, threshold: Double) = {
    this()
    analyze(pixmap, threshold)
  }

  // === Color difference methods using Oklab distance ===

  override def differenceMatch(color1: Int, color2: Int): Double =
    if (((color1 ^ color2) & 0x80) == 0x80) Double.MaxValue
    else differenceMatch(color1 >>> 24, color1 >>> 16 & 0xff, color1 >>> 8 & 0xff, color2 >>> 24, color2 >>> 16 & 0xff, color2 >>> 8 & 0xff)

  override def differenceAnalyzing(color1: Int, color2: Int): Double =
    if (((color1 ^ color2) & 0x80) == 0x80) Double.MaxValue
    else differenceAnalyzing(color1 >>> 24, color1 >>> 16 & 0xff, color1 >>> 8 & 0xff, color2 >>> 24, color2 >>> 16 & 0xff, color2 >>> 8 & 0xff)

  override def differenceHW(color1: Int, color2: Int): Double =
    if (((color1 ^ color2) & 0x80) == 0x80) Double.MaxValue
    else differenceHW(color1 >>> 24, color1 >>> 16 & 0xff, color1 >>> 8 & 0xff, color2 >>> 24, color2 >>> 16 & 0xff, color2 >>> 8 & 0xff)

  override def differenceMatch(color1: Int, r2: Int, g2: Int, b2: Int): Double =
    if ((color1 & 0x80) == 0) Double.MaxValue
    else differenceMatch(color1 >>> 24, color1 >>> 16 & 0xff, color1 >>> 8 & 0xff, r2, g2, b2)

  override def differenceAnalyzing(color1: Int, r2: Int, g2: Int, b2: Int): Double =
    if ((color1 & 0x80) == 0) Double.MaxValue
    else differenceAnalyzing(color1 >>> 24, color1 >>> 16 & 0xff, color1 >>> 8 & 0xff, r2, g2, b2)

  override def differenceHW(color1: Int, r2: Int, g2: Int, b2: Int): Double =
    if ((color1 & 0x80) == 0) Double.MaxValue
    else differenceHW(color1 >>> 24, color1 >>> 16 & 0xff, color1 >>> 8 & 0xff, r2, g2, b2)

  /** Core color difference using Euclidean distance in Oklab, delegated to [[difference(Int,Int,Int,Int,Int,Int)*]]. */
  override def differenceMatch(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Double =
    difference(r1, g1, b1, r2, g2, b2)

  override def differenceAnalyzing(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Double =
    difference(r1, g1, b1, r2, g2, b2)

  override def differenceHW(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Double =
    difference(r1, g1, b1, r2, g2, b2)

  // === Lightness curve helpers ===

  /** Changes the curve of a requested L value so that it matches the internally-used curve. Internally, this is just `Math.pow(L, 1.5)`. */
  private def forwardLight(L: Float): Float =
    Math.sqrt(L.toDouble * L.toDouble * L.toDouble).toFloat

  def difference(color1: Int, color2: Int): Double =
    if (((color1 ^ color2) & 0x80) == 0x80) Double.MaxValue
    else difference(color1 >>> 24, color1 >>> 16 & 0xff, color1 >>> 8 & 0xff, color2 >>> 24, color2 >>> 16 & 0xff, color2 >>> 8 & 0xff)

  def difference(color1: Int, r2: Int, g2: Int, b2: Int): Double =
    if ((color1 & 0x80) == 0) Double.MaxValue
    else difference(color1 >>> 24, color1 >>> 16 & 0xff, color1 >>> 8 & 0xff, r2, g2, b2)

  /** Computes the squared Oklab distance between two colors given as RGB component values (0-255). */
  def difference(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Double = {
    var r: Float = r1 * 0.00392156862745098f; r *= r
    var g: Float = g1 * 0.00392156862745098f; g *= g
    var b: Float = b1 * 0.00392156862745098f; b *= b

    var l = OtherMath.cbrtPositive(0.4121656120f * r + 0.5362752080f * g + 0.0514575653f * b)
    var m = OtherMath.cbrtPositive(0.2118591070f * r + 0.6807189584f * g + 0.1074065790f * b)
    var s = OtherMath.cbrtPositive(0.0883097947f * r + 0.2818474174f * g + 0.6302613616f * b)

    val lA = forwardLight(0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s)
    val aA = 1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s
    val bA = 0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s

    r = r2 * 0.00392156862745098f; r *= r
    g = g2 * 0.00392156862745098f; g *= g
    b = b2 * 0.00392156862745098f; b *= b

    l = OtherMath.cbrtPositive(0.4121656120f * r + 0.5362752080f * g + 0.0514575653f * b)
    m = OtherMath.cbrtPositive(0.2118591070f * r + 0.6807189584f * g + 0.1074065790f * b)
    s = OtherMath.cbrtPositive(0.0883097947f * r + 0.2818474174f * g + 0.6302613616f * b)

    val lB = forwardLight(0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s)
    val aB = 1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s
    val bB = 0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s

    val dL = (lA - lB).toDouble * 512.0
    val dA = (aA - aB).toDouble * 512.0
    val dB = (bA - bB).toDouble * 512.0

    dL * dL + dA * dA + dB * dB
  }
}
