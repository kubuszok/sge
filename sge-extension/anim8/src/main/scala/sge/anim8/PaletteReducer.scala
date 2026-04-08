/*
 * Ported from anim8-gdx - https://github.com/tommyettinger/anim8-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * STUB: This file contains only the public API surface needed by AnimatedGif, PNG8, etc.
 * The full implementation will be ported separately due to its size.
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: partial-port
 * Covenant-source-reference: anim8-gdx/src/main/java/com/github/tommyettinger/anim8/PaletteReducer.java
 * Covenant-verified: 2026-04-08
 *
 * Partial-port debt:
 *   - Color analysis (analyze, analyzeFast, analyzeReductive) — STUB delegates to default palette
 *   - Hue-wise analyze variant — STUB delegates to analyze
 *   - Full implementation tracked separately; this file ports only the public API surface
 *     consumed by AnimatedGif, PNG8, FastGif, AnimatedPNG.
 */
package sge
package anim8

import sge.files.FileHandle
import sge.graphics.{ Color, Pixmap }

/** Data that can be used to limit the colors present in a Pixmap or other image, here with the goal of using 256 or fewer colors in the image (for saving indexed-mode images). Can be used
  * independently of classes like [[AnimatedGif]] and [[PNG8]], but it is meant to help with intelligently reducing the color count to fit under the maximum palette size for those formats.
  *
  * STUB: This is a forward-reference stub. The full implementation will be ported separately.
  */
class PaletteReducer {

  // === Public fields used by AnimatedGif, PNG8, etc. ===

  /** Maps 15-bit RGB555 colors to palette indices. */
  val paletteMapping: Array[Byte] = new Array[Byte](0x8000)

  /** The RGBA8888 colors in the current palette. */
  val paletteArray: Array[Int] = new Array[Int](256)

  /** The number of colors in the current palette. */
  var colorCount: Int = 0

  /** A bias value based on population analysis; affects dithering strength. */
  var populationBias: Float = 0.5f

  /** Dither strength, typically between 0 and 2. */
  private var _ditherStrength: Float = 1f

  /** Candidates array used by Pattern dither; has 32 entries. */
  val candidates: Array[Int] = new Array[Int](32)

  // Error diffusion float arrays, used by various dithering algorithms in AnimatedGif/PNG8.
  var curErrorRedFloats:    Array[Float] | Null = null
  var nextErrorRedFloats:   Array[Float] | Null = null
  var curErrorGreenFloats:  Array[Float] | Null = null
  var nextErrorGreenFloats: Array[Float] | Null = null
  var curErrorBlueFloats:   Array[Float] | Null = null
  var nextErrorBlueFloats:  Array[Float] | Null = null

  // === Constructors ===

  /** Creates a PaletteReducer that analyzes the given Pixmap for its palette. */
  def this(pixmap: Pixmap) = {
    this()
    analyze(pixmap)
  }

  /** Creates a PaletteReducer with the given RGBA8888 palette. */
  def this(rgbaPalette: Array[Int]) = {
    this()
    exact(rgbaPalette)
  }

  /** Creates a PaletteReducer that analyzes the given Pixmap with the specified threshold. */
  def this(pixmap: Pixmap, threshold: Double) = {
    this()
    analyze(pixmap, threshold)
  }

  // === Public methods ===

  def getDitherStrength: Float = _ditherStrength

  def setDitherStrength(ditherStrength: Float): Unit =
    _ditherStrength = Math.max(0f, ditherStrength)

  def getPopulationBias: Float = populationBias

  def setPopulationBias(populationBias: Float): Unit =
    this.populationBias = populationBias

  /** Sets the default Snuggly palette. */
  def setDefaultPalette(): Unit =
    exact(PaletteReducer.SNUGGLY, ConstantData.ENCODED_SNUGGLY)

  /** Sets an exact RGBA8888 palette. */
  def exact(rgbaPalette: Array[Int]): Unit =
    exact(rgbaPalette, rgbaPalette.length)

  /** Sets an exact RGBA8888 palette with a limit on the number of colors. */
  def exact(rgbaPalette: Array[Int], limit: Int): Unit = {
    // STUB: minimal implementation - copy palette and create basic mapping
    colorCount = Math.min(limit, Math.min(rgbaPalette.length, 256))
    System.arraycopy(rgbaPalette, 0, paletteArray, 0, colorCount)
    populationBias = Math.exp(-1.375 / colorCount).toFloat
    // Build a simple nearest-color mapping for RGB555
    var i = 0
    while (i < 0x8000) {
      val r        = ((i & 0x7c00) >>> 7) | ((i & 0x7c00) >>> 12)
      val g        = ((i & 0x3e0) >>> 2) | ((i & 0x3e0) >>> 7)
      val b        = ((i & 0x1f) << 3) | ((i & 0x1f) >>> 2)
      var bestDist = Int.MaxValue
      var bestIdx  = 0
      var j        = 0
      while (j < colorCount) {
        val pa = paletteArray(j)
        if ((pa & 0xff) >= 0x80) { // only consider opaque colors
          val dr   = r - (pa >>> 24)
          val dg   = g - (pa >>> 16 & 0xff)
          val db   = b - (pa >>> 8 & 0xff)
          val dist = dr * dr + dg * dg + db * db
          if (dist < bestDist) {
            bestDist = dist
            bestIdx = j
          }
        }
        j += 1
      }
      paletteMapping(i) = bestIdx.toByte
      i += 1
    }
  }

  /** Sets an exact palette with preloaded mapping data. */
  def exact(palette: Array[Int], preload: Array[Byte]): Unit = {
    colorCount = Math.min(palette.length, 256)
    System.arraycopy(palette, 0, paletteArray, 0, colorCount)
    populationBias = Math.exp(-1.375 / colorCount).toFloat
    val mappingLen = Math.min(preload.length, 0x8000)
    System.arraycopy(preload, 0, paletteMapping, 0, mappingLen)
  }

  /** Analyzes the given Pixmap to build a palette. */
  def analyze(pixmap: Pixmap): Unit =
    analyze(pixmap, 400)

  /** Analyzes the given Pixmap with a threshold to build a palette. */
  def analyze(pixmap: Pixmap, threshold: Double): Unit =
    analyze(pixmap, threshold, 256)

  /** Analyzes the given Pixmap with a threshold and color limit to build a palette. STUB: This is a simplified implementation that picks colors by frequency.
    */
  def analyze(pixmap: Pixmap, threshold: Double, limit: Int): Unit =
    // STUB: For now, just set the default palette
    // TODO: Full implementation of color analysis
    setDefaultPalette()

  /** Analyzes the given Pixmaps to build a palette. */
  def analyze(pixmaps: Array[Pixmap]): Unit =
    if (pixmaps.nonEmpty) analyze(pixmaps(0))

  /** Fast analysis of a Pixmap for palette generation. */
  def analyzeFast(pixmap: Pixmap, threshold: Double, limit: Int): Unit =
    // STUB: delegates to analyze
    analyze(pixmap, threshold, limit.min(256))

  /** Sets an exact palette from an array of Color objects. */
  def exact(colorPalette: Array[Color]): Unit =
    exact(colorPalette, colorPalette.length)

  /** Sets an exact palette from an array of Color objects, with a limit. */
  def exact(colorPalette: Array[Color], limit: Int): Unit = {
    val count = Math.min(limit, Math.min(colorPalette.length, 256))
    val rgba  = new Array[Int](count)
    var i     = 0
    while (i < count) {
      rgba(i) = Color.rgba8888(colorPalette(i))
      i += 1
    }
    exact(rgba, count)
  }

  /** Analyzes the given Pixmap with hue-wise analysis. STUB: delegates to analyze. */
  def analyzeHueWise(pixmap: Pixmap, threshold: Double, limit: Int): Unit =
    analyze(pixmap, threshold, limit)

  /** Writes preload data for this palette reducer to a file. The preload file stores the paletteMapping array (0x8000 bytes) for fast re-loading.
    */
  def writePreloadFile(file: FileHandle): Unit = {
    val out = file.write(false)
    try
      out.write(paletteMapping, 0, 0x8000)
    finally
      out.close()
  }

  /** Loads preload data from a file, returning the paletteMapping byte array. Returns an array of 0x8000 bytes read from the file.
    */
  def loadPreloadFile(file: FileHandle): Array[Byte] = {
    val data = new Array[Byte](0x8000)
    val in   = file.read()
    try {
      var offset    = 0
      var remaining = 0x8000
      while (remaining > 0) {
        val read = in.read(data, offset, remaining)
        if (read <= 0) {
          // short read — fill rest with zeros (already zeroed)
          remaining = 0
        } else {
          offset += read
          remaining -= read
        }
      }
    } finally
      in.close()
    data
  }

  /** Ensures error arrays have capacity for at least `size` elements. */
  def ensureErrorCapacity(size: Int): Unit =
    if (curErrorRedFloats == null || curErrorRedFloats.nn.length < size) {
      curErrorRedFloats = new Array[Float](size)
      nextErrorRedFloats = new Array[Float](size)
      curErrorGreenFloats = new Array[Float](size)
      nextErrorGreenFloats = new Array[Float](size)
      curErrorBlueFloats = new Array[Float](size)
      nextErrorBlueFloats = new Array[Float](size)
    }
}

object PaletteReducer {

  // === Static constants ===

  /** The default 255-color Snuggly palette, plus transparent at index 0. */
  val SNUGGLY: Array[Int] = Array(
    0x00000000, 0x1b1b1bff, 0x131313ff, 0x3b3b3bff, 0x4b4b4bff, 0x555555ff, 0x646464ff, 0x6d6d6dff, 0x7e7e7eff, 0x8c8c8cff, 0x9b9b9bff, 0xa7a7a7ff, 0xb6b6b6ff, 0xc7c7c7ff, 0xd5d5d5ff, 0xe5e5e5ff,
    0xf4f4f4ff, 0x261813ff, 0x33221cff, 0x3b2c22ff, 0x4c3b2eff, 0x3b3127ff, 0x55453aff, 0x5e4f42ff, 0x685b4fff, 0x796b5eff, 0x7e7163ff, 0x857a6fff, 0x92877cff, 0xa0978cff, 0xaca49aff, 0xbdb4aaff,
    0xcfc7beff, 0xe1d9cfff, 0x403026ff, 0x271c22ff, 0x372b31ff, 0x463a3fff, 0x554b4eff, 0x655b5dff, 0x756b6cff, 0x857b7cff, 0x988d8cff, 0xa79d9bff, 0xc7bcbaff, 0xdad0cdff, 0x4c1f13ff, 0x572b1cff,
    0x603522ff, 0x6d422cff, 0x7b5237ff, 0x885d44ff, 0x956a4eff, 0xa27758ff, 0xb08466ff, 0xbd9375ff, 0xc8a084ff, 0xd3ae92ff, 0xdec0a6ff, 0xe9d0b7ff, 0xf4e4cdff, 0x641c13ff, 0x75251cff, 0x832d22ff,
    0x913a2cff, 0xa14737ff, 0xa65644ff, 0xb76553ff, 0xc37463ff, 0xcd8474ff, 0xd59484ff, 0xdca495ff, 0xe4b5a5ff, 0xecc7b8ff, 0x572244ff, 0x422740ff, 0x6e2b55ff, 0x59304dff, 0x803a64ff, 0x8d4a71ff,
    0x9d5a82ff, 0xaa6b90ff, 0xb67b9fff, 0xc38dacff, 0xcf9dbbff, 0xd9aec8ff, 0xe4bfd5ff, 0xefd2e3ff, 0x3b1346ff, 0x4b1c59ff, 0x582468ff, 0x662b77ff, 0x743587ff, 0x804295ff, 0x8c50a2ff, 0x9a60afff,
    0xa671bbff, 0xb282c6ff, 0xbf93d1ff, 0xcaa4dbff, 0xd5b6e4ff, 0xe1c9edff, 0xeddcf5ff, 0x1b1346ff, 0x251c58ff, 0x2d2466ff, 0x372c77ff, 0x423586ff, 0x4f4293ff, 0x5d50a0ff, 0x6b5eadff, 0x7a6ebaff,
    0x8a7ec5ff, 0x9a8ed0ff, 0xa99fdaff, 0xb9b1e3ff, 0xc9c2ecff, 0xdad4f4ff, 0x131b4bff, 0x1b2458ff, 0x222b66ff, 0x2b3577ff, 0x354286ff, 0x425095ff, 0x505ea2ff, 0x5e6eafff, 0x6e7cbbff, 0x7e8dc7ff,
    0x8f9dd2ff, 0x9faddbff, 0xb1bfe4ff, 0xc2d0edff, 0xd4e2f5ff, 0x132a4bff, 0x1b375bff, 0x22426aff, 0x2a507aff, 0x345e89ff, 0x3f6d98ff, 0x4c7ba5ff, 0x5b8ab2ff, 0x6b98bdff, 0x7da8c8ff, 0x8eb7d2ff,
    0x9fc6dcff, 0xb2d4e6ff, 0xc5e3efff, 0x133b4bff, 0x1b4a5bff, 0x225869ff, 0x2c6879ff, 0x377788ff, 0x448697ff, 0x5295a5ff, 0x62a3b2ff, 0x73b2bdff, 0x86c0c8ff, 0x99cdd3ff, 0xacdaddff, 0xc0e7e8ff,
    0x133b35ff, 0x1b4b44ff, 0x225a52ff, 0x2d6962ff, 0x397871ff, 0x468780ff, 0x54968eff, 0x64a49bff, 0x75b2a8ff, 0x87bfb5ff, 0x9accc1ff, 0xadd8cdff, 0xc1e4d9ff, 0xd5f0e6ff, 0x133b21ff, 0x1b4b2bff,
    0x225b35ff, 0x2c6b42ff, 0x387a50ff, 0x44895fff, 0x52976eff, 0x62a57cff, 0x73b28bff, 0x85bf9aff, 0x98cca8ff, 0xacd8b8ff, 0xc0e5c7ff, 0xd5f1d7ff, 0x1a4413ff, 0x24551cff, 0x2d6422ff, 0x37752bff,
    0x438537ff, 0x509444ff, 0x5ea352ff, 0x6eb262ff, 0x7fbf73ff, 0x91cb84ff, 0xa3d696ff, 0xb6e1a8ff, 0xcaecbbff, 0xdef6cfff, 0x354413ff, 0x44551cff, 0x536622ff, 0x62772bff, 0x728737ff, 0x819644ff,
    0x90a452ff, 0x9fb362ff, 0xafc073ff, 0xbecd84ff, 0xccd996ff, 0xdae4a8ff, 0xe7efbbff, 0xf4f9cfff, 0x4b4b13ff, 0x5e5e1cff, 0x6f6f24ff, 0x80802dff, 0x8f8f38ff, 0x9d9d44ff, 0xabab52ff, 0xb9b962ff,
    0xc6c674ff, 0xd2d286ff, 0xdede99ff, 0xe9e9acff, 0xf3f3c0ff, 0x4c3e13ff, 0x5e4d1cff, 0x6f5d24ff, 0x806c2cff, 0x8f7c38ff, 0x9d8b44ff, 0xab9a52ff, 0xb9a862ff, 0xc6b774ff, 0xd2c486ff, 0xded198ff,
    0xe9deacff, 0xf3ebc0ff, 0x4c3513ff, 0x5e421cff, 0x6f4f24ff, 0x805d2cff, 0x8f6c38ff, 0x9d7c44ff, 0xab8c52ff, 0xb99c62ff, 0xc6ac74ff, 0xd2bc86ff, 0xdecb98ff, 0xe9daacff, 0xf3eac0ff
  )

  /** Blue noise data forwarded from ConstantData. */
  val TRI_BLUE_NOISE:   Array[Byte] = ConstantData.TRI_BLUE_NOISE
  val TRI_BLUE_NOISE_B: Array[Byte] = ConstantData.TRI_BLUE_NOISE_B
  val TRI_BLUE_NOISE_C: Array[Byte] = ConstantData.TRI_BLUE_NOISE_C

  /** Blue noise multiplier data forwarded from ConstantData. */
  val TRI_BLUE_NOISE_MULTIPLIERS:   Array[Float] = ConstantData.TRI_BLUE_NOISE_MULTIPLIERS
  val TRI_BLUE_NOISE_MULTIPLIERS_B: Array[Float] = ConstantData.TRI_BLUE_NOISE_MULTIPLIERS_B
  val TRI_BLUE_NOISE_MULTIPLIERS_C: Array[Float] = ConstantData.TRI_BLUE_NOISE_MULTIPLIERS_C

  /** Bit size for Triangular Bayer Matrix. */
  val TBM_BITS: Int = 7
  val TBM_MASK: Int = (1 << TBM_BITS) - 1

  /** Triangular Bayer Matrix 128x128. */
  val TRI_BAYER_MATRIX_128: Array[Byte] = {
    val arr = new Array[Byte](1 << (TBM_BITS + TBM_BITS))
    var i   = 0
    while (i < arr.length) {
      val x = i & TBM_MASK
      val y = i >>> TBM_BITS
      arr(i) = reverseShortBits(interleaveBytes(x ^ y, y)).>>>(16 - TBM_BITS - TBM_BITS).toByte
      i += 1
    }
    arr
  }

  /** Lookup table for sRGB to linear conversion (0-255 -> 0-1023 approximately). */
  val toLinearLUT: Array[Float] = {
    val arr = new Array[Float](256)
    var i   = 0
    while (i < 256) {
      val x = i / 255.0
      arr(i) = (if (x <= 0.04045) x / 12.92 else Math.pow((x + 0.055) / 1.055, 2.4)).toFloat * 1023f
      i += 1
    }
    arr
  }

  /** Lookup table for linear to sRGB conversion (0-1023 -> 0-255). */
  val fromLinearLUT: Array[Byte] = {
    val arr = new Array[Byte](1024)
    var i   = 0
    while (i < 1024) {
      val x = i / 1023.0
      arr(i) = (Math.max(0.0, Math.min(1.0, if (x <= 0.0031308) x * 12.92 else 1.055 * Math.pow(x, 1.0 / 2.4) - 0.055)) * 255.0 + 0.5).toInt.toByte
      i += 1
    }
    arr
  }

  /** Primarily used to avoid allocating arrays that copy [[thresholdMatrix64]], this has length 64. */
  val tempThresholdMatrix: Array[Float] = new Array[Float](64)

  /** Threshold matrix for 4x4 Pattern dither. */
  val thresholdMatrix16: Array[Int] = Array(
    0, 12, 3, 15, 8, 4, 11, 7, 2, 14, 1, 13, 10, 6, 9, 5
  )

  /** Threshold matrix for 8x8 Gourd dither. */
  val thresholdMatrix64: Array[Int] = Array(
    1, 49, 13, 61, 4, 52, 16, 64, 33, 17, 45, 29, 36, 20, 48, 32, 9, 57, 5, 53, 12, 60, 8, 56, 41, 25, 37, 21, 44, 28, 40, 24, 3, 51, 15, 63, 2, 50, 14, 62, 35, 19, 47, 31, 34, 18, 46, 30, 11, 59, 7,
    55, 10, 58, 6, 54, 43, 27, 39, 23, 42, 26, 38, 22
  )

  /** Shrinks an RGBA8888 color to a 15-bit RGB555 value. */
  def shrink(color: Int): Int =
    (color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f)

  /** Sorts the first 16 entries of the given int array (used for Pattern dither). */
  def sort16(i16: Array[Int]): Unit = {
    // Simple insertion sort for 16 elements, sorting by values at i|16
    var i = 1
    while (i < 16) {
      val key     = i16(i)
      val keyData = i16(i | 16)
      var j       = i - 1
      while (j >= 0 && i16(j | 16) > keyData) {
        i16(j + 1) = i16(j)
        i16((j + 1) | 16) = i16(j | 16)
        j -= 1
      }
      i16(j + 1) = key
      i16((j + 1) | 16) = keyData
      i += 1
    }
  }

  private def reverseShortBits(v0: Int): Int = {
    var v = v0
    v = ((v >>> 1) & 0x5555) | ((v & 0x5555) << 1)
    v = ((v >>> 2) & 0x3333) | ((v & 0x3333) << 2)
    v = ((v >>> 4) & 0x0f0f) | ((v & 0x0f0f) << 4)
    v = ((v >>> 8) & 0x00ff) | ((v & 0x00ff) << 8)
    v
  }

  private def interleaveBytes(a: Int, b: Int): Int = {
    var x = a & 0xff
    var y = b & 0xff
    x = (x | (x << 4)) & 0x0f0f
    y = (y | (y << 4)) & 0x0f0f
    x = (x | (x << 2)) & 0x3333
    y = (y | (y << 2)) & 0x3333
    x = (x | (x << 1)) & 0x5555
    y = (y | (y << 1)) & 0x5555
    x | (y << 1)
  }
}
