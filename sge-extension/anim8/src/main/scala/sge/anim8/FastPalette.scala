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
  * Euclidean distance in RGB color space, rather than what PaletteReducer uses, which is Euclidean distance in Oklab.
  */
class FastPalette extends PaletteReducer {

  /** Constructs a default FastPalette that uses the "Snuggly" 255-color-plus-transparent palette. Note that this uses a more-detailed and higher-quality metric than you would get by just specifying
    * `new FastPalette(PaletteReducer.SNUGGLY)`; this metric would be too slow to calculate at runtime, but as pre-calculated data it works very well.
    */
  exact(PaletteReducer.SNUGGLY, ENCODED_SNUGGLY)

  /** Constructs a FastPalette that uses the given array of RGBA8888 ints as a palette (see [[PaletteReducer.exact(Array[Int])* exact]] for more info).
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

  /** Constructs a FastPalette that uses the given array of RGBA8888 ints as a palette (see [[PaletteReducer.exact(Array[Int],Int)* exact]] for more info).
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

  /** Constructs a FastPalette that uses the given array of Color objects as a palette.
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

  /** Constructs a FastPalette that uses the given array of Color objects as a palette.
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

  /** Constructs a FastPalette that analyzes the given Pixmap for color count and frequency to generate a palette.
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

  /** Constructs a FastPalette that analyzes the given Pixmaps for color count and frequency to generate a palette.
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

  /** Constructs a FastPalette with pre-loaded color data.
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

  /** Constructs a FastPalette that analyzes the given Pixmap with a threshold.
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

  // === Color difference methods using Euclidean RGB distance ===

  /** Gets a squared estimate of how different two colors are, with noticeable differences typically at least 25. */
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

  /** Gets a squared estimate of how different two colors are, using Euclidean distance in the RGB color cube. This is the core metric for FastPalette -- used by match, analyzing, and HW variants.
    */
  override def differenceMatch(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Double =
    difference(r1, g1, b1, r2, g2, b2)

  override def differenceAnalyzing(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Double =
    difference(r1, g1, b1, r2, g2, b2)

  override def differenceHW(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Double =
    difference(r1, g1, b1, r2, g2, b2)

  def difference(color1: Int, color2: Int): Double =
    if (((color1 ^ color2) & 0x80) == 0x80) Double.MaxValue
    else difference(color1 >>> 24, color1 >>> 16 & 0xff, color1 >>> 8 & 0xff, color2 >>> 24, color2 >>> 16 & 0xff, color2 >>> 8 & 0xff)

  def difference(color1: Int, r2: Int, g2: Int, b2: Int): Double =
    if ((color1 & 0x80) == 0) Double.MaxValue
    else difference(color1 >>> 24, color1 >>> 16 & 0xff, color1 >>> 8 & 0xff, r2, g2, b2)

  def difference(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Double = {
    val rf = r1 - r2
    val gf = g1 - g2
    val bf = b1 - b2
    (rf * rf + gf * gf + bf * bf).toDouble
  }
}
