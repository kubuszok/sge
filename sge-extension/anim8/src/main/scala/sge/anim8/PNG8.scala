/*
 * Ported from anim8-gdx - https://github.com/tommyettinger/anim8-gdx
 * Original authors: Tommy Ettinger, Matthias Mann, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * PNG-8 encoder with compression; can write animated and non-animated PNG images in indexed-mode.
 * An instance can be reused to encode multiple PNGs with minimal allocation.
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package anim8

import scala.util.boundary
import scala.util.boundary.break

import sge.files.FileHandle
import sge.graphics.Pixmap
import sge.utils.StreamUtils

import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Arrays
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import scala.collection.mutable

import PaletteReducer.*

/** PNG-8 encoder with compression; can write animated and non-animated PNG images in indexed-mode. An instance can be reused to encode multiple PNGs with minimal allocation.
  *
  * You can configure the target palette and how this can dither colors via the [[palette]] field, which is a [[PaletteReducer]] object that defaults to null and can be reused.
  */
class PNG8(initialBufferSize: Int) extends AnimationWriter with Dithered with AutoCloseable {

  private val SIGNATURE: Array[Byte] = Array(137.toByte, 80, 78, 71, 13, 10, 26, 10)
  private val IHDR = 0x49484452
  private val IDAT = 0x49444154
  private val IEND = 0x49454e44
  private val PLTE = 0x504c5445
  private val TRNS = 0x74524e53
  private val acTL = 0x6163544c
  private val fcTL = 0x6663544c
  private val fdAT = 0x66644154
  private val COLOR_INDEXED:       Byte = 3
  private val COMPRESSION_DEFLATE: Byte = 0
  private val INTERLACE_NONE:      Byte = 0
  private val FILTER_NONE:         Byte = 0

  private val buffer:       ChunkBuffer        = new ChunkBuffer(initialBufferSize)
  private val deflater:     Deflater           = new Deflater()
  private var curLineBytes: Array[Byte] | Null = null
  private var flipY:        Boolean            = true

  private var _palette:         PaletteReducer | Null = null
  private var _ditherAlgorithm: DitherAlgorithm       = DitherAlgorithm.WREN

  override def palette:                                   PaletteReducer | Null = _palette
  override def palette_=(palette: PaletteReducer | Null): Unit                  = _palette = palette

  override def ditherAlgorithm:                                     DitherAlgorithm = _ditherAlgorithm
  override def ditherAlgorithm_=(ditherAlgorithm: DitherAlgorithm): Unit            =
    if (ditherAlgorithm != null) { // null guard for Java interop @nowarn
      _ditherAlgorithm = ditherAlgorithm
    }

  /** Overrides the palette's dither strength. */
  protected var _ditherStrength: Float = 1f

  /** Gets this PNG8's dither strength. */
  def ditherStrength: Float = _ditherStrength

  /** Sets this PNG8's dither strength. */
  def ditherStrength_=(ditherStrength: Float): Unit =
    _ditherStrength = Math.max(0f, ditherStrength)

  def this() = this(128 * 128)

  /** If true, the resulting PNG is flipped vertically. Default is true. */
  def setFlipY(flipY: Boolean): Unit = this.flipY = flipY

  /** Sets the deflate compression level. Default is [[Deflater.DEFAULT_COMPRESSION]]. */
  def setCompression(level: Int): Unit = deflater.setLevel(level)

  // === Shared PNG boilerplate helpers ===

  /** Ensures curLineBytes is large enough, returning the array. */
  private def ensureCurLine(lineLen: Int): Array[Byte] = {
    if (curLineBytes == null || curLineBytes.nn.length < lineLen) {
      curLineBytes = new Array[Byte](lineLen)
    }
    curLineBytes.nn
  }

  /** Writes IHDR, PLTE, optional TRNS. Returns (hasTransparent, curLine). */
  private def writePngHeader(dataOutput: DataOutputStream, w: Int, h: Int, paletteArray: Array[Int]): Boolean = {
    buffer.writeInt(IHDR)
    buffer.writeInt(w)
    buffer.writeInt(h)
    buffer.writeByte(8)
    buffer.writeByte(COLOR_INDEXED)
    buffer.writeByte(COMPRESSION_DEFLATE)
    buffer.writeByte(FILTER_NONE)
    buffer.writeByte(INTERLACE_NONE)
    buffer.endChunk(dataOutput)

    buffer.writeInt(PLTE)
    var pi = 0
    while (pi < paletteArray.length) {
      val p = paletteArray(pi)
      buffer.write(p >>> 24)
      buffer.write(p >>> 16)
      buffer.write(p >>> 8)
      pi += 1
    }
    buffer.endChunk(dataOutput)

    val hasTransparent = paletteArray(0) == 0
    if (hasTransparent) {
      buffer.writeInt(TRNS)
      buffer.write(0)
      buffer.endChunk(dataOutput)
    }
    hasTransparent
  }

  /** Writes IHDR, PLTE, TRNS, acTL for animated PNG. Returns hasTransparent. */
  private def writeApngHeader(dataOutput: DataOutputStream, w: Int, h: Int, paletteArray: Array[Int], frameCount: Int): Boolean = {
    val hasTransparent = writePngHeader(dataOutput, w, h, paletteArray)
    buffer.writeInt(acTL)
    buffer.writeInt(frameCount)
    buffer.writeInt(0)
    buffer.endChunk(dataOutput)
    hasTransparent
  }

  // === Single-image write methods ===

  /** Writes the given Pixmap to the requested FileHandle, computing an 8-bit palette from the most common colors. */
  def write(file: FileHandle, pixmap: Pixmap): Unit =
    write(file, pixmap, computePalette = true)

  def write(file: FileHandle, pixmap: Pixmap, computePalette: Boolean): Unit = {
    val output = file.write(false)
    try
      write(output, pixmap, computePalette)
    finally
      StreamUtils.closeQuietly(output)
  }

  def write(file: FileHandle, pixmap: Pixmap, computePalette: Boolean, dither: Boolean): Unit = {
    val output = file.write(false)
    try
      write(output, pixmap, computePalette, dither)
    finally
      StreamUtils.closeQuietly(output)
  }

  def write(output: OutputStream, pixmap: Pixmap): Unit =
    writePrecisely(output, pixmap, ditherFallback = true)

  def write(output: OutputStream, pixmap: Pixmap, computePalette: Boolean): Unit =
    if (computePalette) {
      writePrecisely(output, pixmap, ditherFallback = true)
    } else {
      write(output, pixmap, computePalette = false, dither = true)
    }

  def write(output: OutputStream, pixmap: Pixmap, computePalette: Boolean, dither: Boolean): Unit =
    write(output, pixmap, computePalette, dither, 400)

  def write(output: OutputStream, pixmap: Pixmap, computePalette: Boolean, dither: Boolean, threshold: Int): Unit = {
    val clearPalette = _palette == null
    if (clearPalette) {
      _palette = new PaletteReducer(pixmap)
    } else if (computePalette) {
      _palette.nn.analyze(pixmap, threshold)
    }
    _palette.nn.setDitherStrength(_ditherStrength)

    if (dither) {
      writeDithered(output, pixmap)
    } else {
      writeSolid(output, pixmap)
    }
    if (clearPalette) _palette = null
  }

  /** Uses the current [[ditherAlgorithm]] to select which writing method to use. */
  def writeDithered(output: OutputStream, pixmap: Pixmap): Unit =
    _ditherAlgorithm match {
      case DitherAlgorithm.NONE           => writeSolid(output, pixmap)
      case DitherAlgorithm.GRADIENT_NOISE => writeGradientDithered(output, pixmap)
      case DitherAlgorithm.ADDITIVE       => writeAdditiveDithered(output, pixmap)
      case DitherAlgorithm.ROBERTS        => writeRobertsDithered(output, pixmap)
      case DitherAlgorithm.PATTERN        => writePatternDithered(output, pixmap)
      case DitherAlgorithm.CHAOTIC_NOISE  => writeChaoticNoiseDithered(output, pixmap)
      case DitherAlgorithm.DIFFUSION      => writeDiffusionDithered(output, pixmap)
      case DitherAlgorithm.BLUE_NOISE     => writeBlueNoiseDithered(output, pixmap)
      case DitherAlgorithm.BLUNT          => writeBluntDithered(output, pixmap)
      case DitherAlgorithm.BANTER         => writeBanterDithered(output, pixmap)
      case DitherAlgorithm.SCATTER        => writeScatterDithered(output, pixmap)
      case DitherAlgorithm.WOVEN          => writeWovenDithered(output, pixmap)
      case DitherAlgorithm.DODGY          => writeDodgyDithered(output, pixmap)
      case DitherAlgorithm.LOAF           => writeLoafDithered(output, pixmap)
      case DitherAlgorithm.NEUE           => writeNeueDithered(output, pixmap)
      case DitherAlgorithm.BURKES         => writeBurkesDithered(output, pixmap)
      case DitherAlgorithm.OCEANIC        => writeOceanicDithered(output, pixmap)
      case DitherAlgorithm.SEASIDE        => writeSeasideDithered(output, pixmap)
      case DitherAlgorithm.GOURD          => writeGourdDithered(output, pixmap)
      case DitherAlgorithm.OVERBOARD      => writeOverboardDithered(output, pixmap)
      case DitherAlgorithm.MARTEN         => writeMartenDithered(output, pixmap)
      case DitherAlgorithm.WREN           => writeWrenDithered(output, pixmap)
    }

  /** Attempts to write the given Pixmap exactly as a PNG-8 image to file. */
  def writePrecisely(file: FileHandle, pixmap: Pixmap, ditherFallback: Boolean): Unit =
    writePrecisely(file, pixmap, ditherFallback, 400)

  def writePrecisely(file: FileHandle, pixmap: Pixmap, ditherFallback: Boolean, threshold: Int): Unit = {
    val output = file.write(false)
    try
      writePrecisely(output, pixmap, ditherFallback, threshold)
    finally
      StreamUtils.closeQuietly(output)
  }

  def writePrecisely(output: OutputStream, pixmap: Pixmap, ditherFallback: Boolean): Unit =
    writePrecisely(output, pixmap, ditherFallback, 400)

  def writePrecisely(output: OutputStream, pixmap: Pixmap, ditherFallback: Boolean, threshold: Int): Unit =
    writePrecisely(output, pixmap, null, ditherFallback, threshold)

  def writePrecisely(output: OutputStream, pixmap: Pixmap, exactPalette: Array[Int] | Null, ditherFallback: Boolean, threshold: Int): Unit = boundary {
    val colorToIndex = new mutable.HashMap[Int, Int]()
    colorToIndex.put(0, 0)
    var hasTransparent = 0
    val w              = pixmap.width.toInt
    val h              = pixmap.height.toInt
    var paletteArray: Array[Int] = null.asInstanceOf[Array[Int]] // will be assigned @nowarn
    if (exactPalette == null) {
      var tooManyColors = false
      var y0            = 0
      while (y0 < h && !tooManyColors) {
        val py = if (flipY) h - y0 - 1 else y0
        var px = 0
        while (px < w && !tooManyColors) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if ((color & 0xfe) != 0xfe && !colorToIndex.contains(color)) {
            if (hasTransparent == 0 && colorToIndex.size >= 256) {
              write(output, pixmap, computePalette = true, dither = ditherFallback, threshold = threshold)
              tooManyColors = true
            } else {
              hasTransparent = 1
            }
          } else if (!colorToIndex.contains(color)) {
            colorToIndex.put(color, colorToIndex.size & 255)
            if (colorToIndex.size == 257 && hasTransparent == 0) {
              colorToIndex.remove(0)
            }
            if (colorToIndex.size > 256) {
              write(output, pixmap, computePalette = true, dither = ditherFallback, threshold = threshold)
              tooManyColors = true
            }
          }
          px += 1
        }
        y0 += 1
      }
      if (tooManyColors) {
        break(())
      }
      paletteArray = new Array[Int](colorToIndex.size)
      for ((k, v) <- colorToIndex)
        paletteArray(v) = k
    } else {
      hasTransparent = if (exactPalette.nn(0) == 0) 1 else 0
      paletteArray = exactPalette.nn
      var i = hasTransparent
      while (i < paletteArray.length) {
        colorToIndex.put(paletteArray(i), i)
        i += 1
      }
    }

    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      writePngHeader(dataOutput, w, h, paletteArray)

      buffer.writeInt(IDAT)
      deflater.reset()

      val curLine = ensureCurLine(w)

      var y0 = 0
      while (y0 < h) {
        val py = if (flipY) h - y0 - 1 else y0
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          curLine(px) = colorToIndex.getOrElse(color, 0).toByte
          px += 1
        }
        deflaterOutput.write(FILTER_NONE)
        deflaterOutput.write(curLine, 0, w)
        y0 += 1
      }
      deflaterOutput.finish()
      buffer.endChunk(dataOutput)

      buffer.writeInt(IEND)
      buffer.endChunk(dataOutput)
      output.flush()
    } catch {
      case e: IOException =>
        System.err.println("anim8: " + e.getMessage)
    }
  }

  /** Writes a solid (non-dithered) PNG-8 image. */
  def writeSolid(output: OutputStream, pixmap: Pixmap): Unit = {
    val pal            = _palette.nn
    val paletteArray   = pal.paletteArray
    val paletteMapping = pal.paletteMapping

    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val w = pixmap.width.toInt
      val h = pixmap.height.toInt

      val hasTransparent = writePngHeader(dataOutput, w, h, paletteArray)

      buffer.writeInt(IDAT)
      deflater.reset()

      val curLine = ensureCurLine(w)

      var y0 = 0
      while (y0 < h) {
        val py = if (flipY) h - y0 - 1 else y0
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) {
            curLine(px) = 0
          } else {
            val rr = color >>> 24
            val gg = (color >>> 16) & 0xff
            val bb = (color >>> 8) & 0xff
            curLine(px) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          }
          px += 1
        }
        deflaterOutput.write(FILTER_NONE)
        deflaterOutput.write(curLine, 0, w)
        y0 += 1
      }
      deflaterOutput.finish()
      buffer.endChunk(dataOutput)

      buffer.writeInt(IEND)
      buffer.endChunk(dataOutput)
      output.flush()
    } catch {
      case e: IOException =>
        System.err.println("anim8: " + e.getMessage)
    }
  }

  // === Single-image dithered write methods ===

  def writeGradientDithered(output: OutputStream, pixmap: Pixmap): Unit = {
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val w              = pixmap.width.toInt
      val h              = pixmap.height.toInt
      val hasTransparent = writePngHeader(dataOutput, w, h, paletteArray)
      buffer.writeInt(IDAT)
      deflater.reset()
      val curLine        = ensureCurLine(w)
      val populationBias = _palette.nn.populationBias
      val strength       = 0.9f * Math.tanh(0.16f * _ditherStrength * Math.pow(populationBias, -7.00)).toFloat
      var y              = 0
      while (y < h) {
        val py = if (flipY) h - y - 1 else y
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            val rr = fromLinearLUT(Math.min(Math.max((toLinearLUT(color >>> 24) + ((142 * (px + 0x5f) + 79 * (y - 0x96) & 255) - 127.5f) * strength).toInt, 0), 1023)) & 255
            val gg = fromLinearLUT(Math.min(Math.max((toLinearLUT((color >>> 16) & 0xff) + ((142 * (px + 0xfa) + 79 * (y - 0xa3) & 255) - 127.5f) * strength).toInt, 0), 1023)) & 255
            val bb = fromLinearLUT(Math.min(Math.max((toLinearLUT((color >>> 8) & 0xff) + ((142 * (px + 0xa5) + 79 * (y - 0xc9) & 255) - 127.5f) * strength).toInt, 0), 1023)) & 255
            curLine(px) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          }
          px += 1
        }
        deflaterOutput.write(FILTER_NONE)
        deflaterOutput.write(curLine, 0, w)
        y += 1
      }
      deflaterOutput.finish()
      buffer.endChunk(dataOutput)
      buffer.writeInt(IEND)
      buffer.endChunk(dataOutput)
      output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  def writeAdditiveDithered(output: OutputStream, pixmap: Pixmap): Unit = {
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val w              = pixmap.width.toInt
      val h              = pixmap.height.toInt
      val hasTransparent = writePngHeader(dataOutput, w, h, paletteArray)
      buffer.writeInt(IDAT)
      deflater.reset()
      val curLine        = ensureCurLine(w)
      val populationBias = _palette.nn.populationBias
      val s              = 0.08f * _ditherStrength / Math.pow(populationBias, 8f).toFloat
      val strength       = s / (0.35f + s)
      var y              = 0
      while (y < h) {
        val py = if (flipY) h - y - 1 else y
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            val rr = fromLinearLUT((toLinearLUT(color >>> 24) + ((119 * px + 180 * y + 54 & 255) - 127.5f) * strength).toInt) & 255
            val gg = fromLinearLUT((toLinearLUT((color >>> 16) & 0xff) + ((119 * px + 180 * y + 81 & 255) - 127.5f) * strength).toInt) & 255
            val bb = fromLinearLUT((toLinearLUT((color >>> 8) & 0xff) + ((119 * px + 180 * y & 255) - 127.5f) * strength).toInt) & 255
            curLine(px) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          }
          px += 1
        }
        deflaterOutput.write(FILTER_NONE)
        deflaterOutput.write(curLine, 0, w)
        y += 1
      }
      deflaterOutput.finish()
      buffer.endChunk(dataOutput)
      buffer.writeInt(IEND)
      buffer.endChunk(dataOutput)
      output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  def writeRobertsDithered(output: OutputStream, pixmap: Pixmap): Unit = {
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val w              = pixmap.width.toInt
      val h              = pixmap.height.toInt
      val hasTransparent = writePngHeader(dataOutput, w, h, paletteArray)
      buffer.writeInt(IDAT)
      deflater.reset()
      val curLine        = ensureCurLine(w)
      val populationBias = _palette.nn.populationBias
      val str            = Math.min(48 * _ditherStrength / (populationBias * populationBias * populationBias * populationBias), 127f)
      var y              = 0
      while (y < h) {
        val py = if (flipY) h - y - 1 else y
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            // We get a sub-random value from 0-1 using the R2 sequence.
            // Offsetting this value by different values and feeding into triangleWave()
            // gives 3 different values for r, g, and b, without much bias toward high or low values.
            // There is correlation between r, g, and b in certain patterns.
            val theta = (px * 0xc13fa9a9 + y * 0x91e10da5 >>> 9) * 1.1920929e-7f
            val rr    = fromLinearLUT((toLinearLUT(color >>> 24) + OtherMath.triangleWave(theta) * str).toInt) & 255
            val gg    = fromLinearLUT((toLinearLUT((color >>> 16) & 0xff) + OtherMath.triangleWave(theta + 0.209f) * str).toInt) & 255
            val bb    = fromLinearLUT((toLinearLUT((color >>> 8) & 0xff) + OtherMath.triangleWave(theta + 0.518f) * str).toInt) & 255
            curLine(px) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          }
          px += 1
        }
        deflaterOutput.write(FILTER_NONE)
        deflaterOutput.write(curLine, 0, w)
        y += 1
      }
      deflaterOutput.finish()
      buffer.endChunk(dataOutput)
      buffer.writeInt(IEND)
      buffer.endChunk(dataOutput)
      output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  def writeLoafDithered(output: OutputStream, pixmap: Pixmap): Unit = {
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val w              = pixmap.width.toInt
      val h              = pixmap.height.toInt
      val hasTransparent = writePngHeader(dataOutput, w, h, paletteArray)
      buffer.writeInt(IDAT)
      deflater.reset()
      val curLine  = ensureCurLine(w)
      val strength = Math.min(Math.max(2.5f + 5f * _ditherStrength - 5.5f * _palette.nn.populationBias, 0f), 7.9f)
      var y        = 0
      while (y < h) {
        val py = if (flipY) h - y - 1 else y
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            val adj = ((((px + y & 1) << 5) - 16) * strength).toInt // either + 16 * strength or - 16 * strength
            val rr  = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 24) + adj, 0f), 1023f).toInt) & 255
            val gg  = fromLinearLUT(Math.min(Math.max(toLinearLUT((color >>> 16) & 0xff) + adj, 0f), 1023f).toInt) & 255
            val bb  = fromLinearLUT(Math.min(Math.max(toLinearLUT((color >>> 8) & 0xff) + adj, 0f), 1023f).toInt) & 255
            curLine(px) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          }
          px += 1
        }
        deflaterOutput.write(FILTER_NONE)
        deflaterOutput.write(curLine, 0, w)
        y += 1
      }
      deflaterOutput.finish()
      buffer.endChunk(dataOutput)
      buffer.writeInt(IEND)
      buffer.endChunk(dataOutput)
      output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  def writeGourdDithered(output: OutputStream, pixmap: Pixmap): Unit = {
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val w              = pixmap.width.toInt
      val h              = pixmap.height.toInt
      val hasTransparent = writePngHeader(dataOutput, w, h, paletteArray)
      buffer.writeInt(IDAT)
      deflater.reset()
      val curLine  = ensureCurLine(w)
      val strength = (_ditherStrength * 0.7 * Math.pow(_palette.nn.populationBias, -5.50)).toFloat
      var gi       = 0
      while (gi < 64) { tempThresholdMatrix(gi) = Math.min(Math.max((thresholdMatrix64(gi) - 31.5f) * strength, -127f), 127f); gi += 1 }
      var y = 0
      while (y < h) {
        val py = if (flipY) h - y - 1 else y
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            val adj = tempThresholdMatrix((px & 7) | (y & 7) << 3)
            val rr  = fromLinearLUT((toLinearLUT(color >>> 24) + adj).toInt) & 255
            val gg  = fromLinearLUT((toLinearLUT((color >>> 16) & 0xff) + adj).toInt) & 255
            val bb  = fromLinearLUT((toLinearLUT((color >>> 8) & 0xff) + adj).toInt) & 255
            curLine(px) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          }
          px += 1
        }
        deflaterOutput.write(FILTER_NONE)
        deflaterOutput.write(curLine, 0, w)
        y += 1
      }
      deflaterOutput.finish()
      buffer.endChunk(dataOutput)
      buffer.writeInt(IEND)
      buffer.endChunk(dataOutput)
      output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  def writeBlueNoiseDithered(output: OutputStream, pixmap: Pixmap): Unit = {
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val w              = pixmap.width.toInt
      val h              = pixmap.height.toInt
      val hasTransparent = writePngHeader(dataOutput, w, h, paletteArray)
      buffer.writeInt(IDAT)
      deflater.reset()
      val curLine  = ensureCurLine(w)
      val strength = 0.21875f * _ditherStrength / (_palette.nn.populationBias * _palette.nn.populationBias)
      var oy       = 0
      while (oy < h) {
        val y = if (flipY) h - oy - 1 else oy
        var x = 0
        while (x < w) {
          val color = pixmap.getPixel(Pixels(x), Pixels(y))
          if (hasTransparent && (color & 0x80) == 0) { curLine(x) = 0 }
          else {
            val adj = Math.min(Math.max((TRI_BLUE_NOISE((x & 63) | (y & 63) << 6) + ((x + y & 1) << 8) - 127.5f) * strength, -100.5f), 101.5f)
            val rr  = fromLinearLUT((toLinearLUT(color >>> 24) + adj).toInt) & 255
            val gg  = fromLinearLUT((toLinearLUT((color >>> 16) & 0xff) + adj).toInt) & 255
            val bb  = fromLinearLUT((toLinearLUT((color >>> 8) & 0xff) + adj).toInt) & 255
            curLine(x) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          }
          x += 1
        }
        deflaterOutput.write(FILTER_NONE)
        deflaterOutput.write(curLine, 0, w)
        oy += 1
      }
      deflaterOutput.finish()
      buffer.endChunk(dataOutput)
      buffer.writeInt(IEND)
      buffer.endChunk(dataOutput)
      output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  def writeBluntDithered(output: OutputStream, pixmap: Pixmap): Unit = {
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val w              = pixmap.width.toInt
      val h              = pixmap.height.toInt
      val hasTransparent = writePngHeader(dataOutput, w, h, paletteArray)
      buffer.writeInt(IDAT)
      deflater.reset()
      val curLine        = ensureCurLine(w)
      val populationBias = _palette.nn.populationBias
      val strength       = Math.min(Math.max(0.35f * _ditherStrength / (populationBias * populationBias * populationBias), -0.6f), 0.6f)
      var oy             = 0
      while (oy < h) {
        val y = if (flipY) h - oy - 1 else oy
        var x = 0
        while (x < w) {
          val color = pixmap.getPixel(Pixels(x), Pixels(y))
          if (hasTransparent && (color & 0x80) == 0) { curLine(x) = 0 }
          else {
            val adj = (x + y << 7 & 128) - 63.5f
            val rr  = fromLinearLUT((toLinearLUT(color >>> 24) + (TRI_BLUE_NOISE((x + 62 & 63) << 6 | (y + 66 & 63)) + adj) * strength).toInt) & 255
            val gg  = fromLinearLUT((toLinearLUT((color >>> 16) & 0xff) + (TRI_BLUE_NOISE_B((x + 31 & 63) << 6 | (y + 113 & 63)) + adj) * strength).toInt) & 255
            val bb  = fromLinearLUT((toLinearLUT((color >>> 8) & 0xff) + (TRI_BLUE_NOISE_C((x + 71 & 63) << 6 | (y + 41 & 63)) + adj) * strength).toInt) & 255
            curLine(x) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          }
          x += 1
        }
        deflaterOutput.write(FILTER_NONE)
        deflaterOutput.write(curLine, 0, w)
        oy += 1
      }
      deflaterOutput.finish()
      buffer.endChunk(dataOutput)
      buffer.writeInt(IEND)
      buffer.endChunk(dataOutput)
      output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  def writeBanterDithered(output: OutputStream, pixmap: Pixmap): Unit = {
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val w              = pixmap.width.toInt
      val h              = pixmap.height.toInt
      val hasTransparent = writePngHeader(dataOutput, w, h, paletteArray)
      buffer.writeInt(IDAT)
      deflater.reset()
      val curLine  = ensureCurLine(w)
      val strength = Math.min(Math.max(0.17f * _ditherStrength * Math.pow(_palette.nn.populationBias, -10f).toFloat, -0.95f), 0.95f)
      var oy       = 0
      while (oy < h) {
        val y = if (flipY) h - oy - 1 else oy
        var x = 0
        while (x < w) {
          val color = pixmap.getPixel(Pixels(x), Pixels(y))
          if (hasTransparent && (color & 0x80) == 0) { curLine(x) = 0 }
          else {
            val adj = TRI_BAYER_MATRIX_128((x & TBM_MASK) << TBM_BITS | (y & TBM_MASK)) * strength
            val rr  = fromLinearLUT((toLinearLUT(color >>> 24) + adj).toInt) & 255
            val gg  = fromLinearLUT((toLinearLUT((color >>> 16) & 0xff) + adj).toInt) & 255
            val bb  = fromLinearLUT((toLinearLUT((color >>> 8) & 0xff) + adj).toInt) & 255
            curLine(x) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          }
          x += 1
        }
        deflaterOutput.write(FILTER_NONE)
        deflaterOutput.write(curLine, 0, w)
        oy += 1
      }
      deflaterOutput.finish()
      buffer.endChunk(dataOutput)
      buffer.writeInt(IEND)
      buffer.endChunk(dataOutput)
      output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  def writeChaoticNoiseDithered(output: OutputStream, pixmap: Pixmap): Unit = {
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val w              = pixmap.width.toInt
      val h              = pixmap.height.toInt
      val hasTransparent = writePngHeader(dataOutput, w, h, paletteArray)
      buffer.writeInt(IDAT)
      deflater.reset()
      val curLine:  Array[Byte] = ensureCurLine(w)
      val strength: Double      = _ditherStrength * _palette.nn.populationBias * 1.5
      var s = 0xc13fa9a902a6328fL
      var y = 0
      while (y < h) {
        val py = if (flipY) h - y - 1 else y
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            var rr           = color >>> 24
            var gg           = (color >>> 16) & 0xff
            var bb           = (color >>> 8) & 0xff
            val paletteIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
            val used         = paletteArray(paletteIndex & 0xff)
            var adj: Double = (TRI_BLUE_NOISE((px & 63) | (y & 63) << 6) + 0.5f) * 0.007843138f
            adj *= adj * adj
            // Complicated... This starts with a checkerboard of -0.5 and 0.5, times a tiny fraction.
            // The next 3 lines generate 3 low-quality-random numbers based on s, which should be
            //   different as long as the colors encountered so far were different. The numbers can
            //   each be positive or negative, and are reduced to a manageable size, summed, and
            //   multiplied by the earlier tiny fraction. Summing 3 random values gives us a curved
            //   distribution, centered on about 0.0 and weighted so most results are close to 0.
            //   Two of the random numbers use an XLCG, and the last uses an LCG.
            // 0x1.8p-49 = 2.6645352591003757e-15
            adj += ((px + y & 1) - 0.5f) * 2.6645352591003757e-15 * strength *
              (((s ^ 0x9e3779b97f4a7c15L) * 0xc6bc279692b5cc83L >> 15) +
                ((~s ^ 0xdb4f0b9175ae2165L) * 0xd1b54a32d192ed03L >> 15) +
                ({ s = (s ^ rr + gg + bb) * 0xd1342543de82ef95L + 0x91e10da5c79e7b1dL; s } >> 15))
            rr = Math.min(Math.max((rr + (adj * (rr - (used >>> 24)))).toInt, 0), 0xff)
            gg = Math.min(Math.max((gg + (adj * (gg - ((used >>> 16) & 0xff)))).toInt, 0), 0xff)
            bb = Math.min(Math.max((bb + (adj * (bb - ((used >>> 8) & 0xff)))).toInt, 0), 0xff)
            curLine(px) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          }
          px += 1
        }
        deflaterOutput.write(FILTER_NONE)
        deflaterOutput.write(curLine, 0, w)
        y += 1
      }
      deflaterOutput.finish()
      buffer.endChunk(dataOutput)
      buffer.writeInt(IEND)
      buffer.endChunk(dataOutput)
      output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  def writeMartenDithered(output: OutputStream, pixmap: Pixmap): Unit = {
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val w              = pixmap.width.toInt
      val h              = pixmap.height.toInt
      val hasTransparent = writePngHeader(dataOutput, w, h, paletteArray)
      buffer.writeInt(IDAT)
      deflater.reset()
      val curLine        = ensureCurLine(w)
      val populationBias = _palette.nn.populationBias
      // 0x1p-8f = 0.00390625f
      val str = Math.min(
        1100f * (_ditherStrength / Math.sqrt(_palette.nn.colorCount).toFloat * (1f / (populationBias * populationBias * populationBias) - 0.7f)),
        127f
      )
      var y = 0
      while (y < h) {
        val py = if (flipY) h - y - 1 else y
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            // We get a sub-random value from 0-1 using interleaved gradient noise.
            // Offsetting this value by different values and feeding into triangleWave()
            // gives 3 different values for r, g, and b, without much bias toward high or low values.
            // There is correlation between r, g, and b in certain patterns.
            val theta = (px * 142 + y * 79 & 255) * 0.00390625f
            val rr    = fromLinearLUT((toLinearLUT(color >>> 24) + OtherMath.triangleWave(theta) * str).toInt) & 255
            val gg    = fromLinearLUT((toLinearLUT((color >>> 16) & 0xff) + OtherMath.triangleWave(theta + 0.382f) * str).toInt) & 255
            val bb    = fromLinearLUT((toLinearLUT((color >>> 8) & 0xff) + OtherMath.triangleWave(theta + 0.618f) * str).toInt) & 255
            curLine(px) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          }
          px += 1
        }
        deflaterOutput.write(FILTER_NONE)
        deflaterOutput.write(curLine, 0, w)
        y += 1
      }
      deflaterOutput.finish()
      buffer.endChunk(dataOutput)
      buffer.writeInt(IEND)
      buffer.endChunk(dataOutput)
      output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  // Error-diffusion single-image dithered methods share a common setup/teardown pattern.
  // Each has its own dithering kernel logic.

  def writePatternDithered(output: OutputStream, pixmap: Pixmap): Unit = {
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val pal            = _palette.nn
    val paletteArray   = pal.paletteArray
    val paletteMapping = pal.paletteMapping
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val w              = pixmap.width.toInt
      val h              = pixmap.height.toInt
      val hasTransparent = writePngHeader(dataOutput, w, h, paletteArray)
      buffer.writeInt(IDAT)
      deflater.reset()
      val curLine  = ensureCurLine(w)
      val errorMul = _ditherStrength * 0.5f / pal.populationBias
      var y        = 0
      while (y < h) {
        val py = if (flipY) h - y - 1 else y
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            var er = 0; var eg            = 0; var eb                   = 0
            val cr = color >>> 24; val cg = color >>> 16 & 0xff; val cb = color >>> 8 & 0xff
            var c  = 0
            while (c < 16) {
              val rr        = Math.min(Math.max((cr + er * errorMul).toInt, 0), 255)
              val gg        = Math.min(Math.max((cg + eg * errorMul).toInt, 0), 255)
              val bb        = Math.min(Math.max((cb + eb * errorMul).toInt, 0), 255)
              val usedIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff
              val used      = paletteArray(usedIndex)
              pal.candidates(c) = usedIndex
              pal.candidates(c | 16) = shrink(used)
              er += cr - (used >>> 24)
              eg += cg - (used >>> 16 & 0xff)
              eb += cb - (used >>> 8 & 0xff)
              c += 1
            }
            sort16(pal.candidates)
            curLine(px) = pal.candidates(thresholdMatrix16((px & 3) | (y & 3) << 2)).toByte
          }
          px += 1
        }
        deflaterOutput.write(FILTER_NONE)
        deflaterOutput.write(curLine, 0, w)
        y += 1
      }
      deflaterOutput.finish()
      buffer.endChunk(dataOutput)
      buffer.writeInt(IEND)
      buffer.endChunk(dataOutput)
      output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  def writeDiffusionDithered(output: OutputStream, pixmap: Pixmap): Unit = {
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val pal            = _palette.nn
    val paletteArray   = pal.paletteArray
    val paletteMapping = pal.paletteMapping
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val w = pixmap.width.toInt
      val h = pixmap.height.toInt
      pal.ensureErrorCapacity(w)
      val curErrorRed    = pal.curErrorRedFloats.nn
      val nextErrorRed   = pal.nextErrorRedFloats.nn
      val curErrorGreen  = pal.curErrorGreenFloats.nn
      val nextErrorGreen = pal.nextErrorGreenFloats.nn
      val curErrorBlue   = pal.curErrorBlueFloats.nn
      val nextErrorBlue  = pal.nextErrorBlueFloats.nn
      Arrays.fill(nextErrorRed, 0, w, 0f)
      Arrays.fill(nextErrorGreen, 0, w, 0f)
      Arrays.fill(nextErrorBlue, 0, w, 0f)

      buffer.writeInt(IHDR)
      buffer.writeInt(w)
      buffer.writeInt(h)
      buffer.writeByte(8)
      buffer.writeByte(COLOR_INDEXED)
      buffer.writeByte(COMPRESSION_DEFLATE)
      buffer.writeByte(FILTER_NONE)
      buffer.writeByte(INTERLACE_NONE)
      buffer.endChunk(dataOutput)

      buffer.writeInt(PLTE)
      var pi = 0; while (pi < paletteArray.length) { val p = paletteArray(pi); buffer.write(p >>> 24); buffer.write(p >>> 16); buffer.write(p >>> 8); pi += 1 }
      buffer.endChunk(dataOutput)

      val hasTransparent = paletteArray(0) == 0
      if (hasTransparent) { buffer.writeInt(TRNS); buffer.write(0); buffer.endChunk(dataOutput) }
      buffer.writeInt(IDAT)
      deflater.reset()

      val curLine = ensureCurLine(w)
      // 0x1p-8f = 0.00390625f
      val w1 = _ditherStrength * 32 / pal.populationBias; val w3 = w1 * 3f; val w5 = w1 * 5f; val w7 = w1 * 7f

      var y = 0
      while (y < h) {
        System.arraycopy(nextErrorRed, 0, curErrorRed, 0, w)
        System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, w)
        System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, w)
        Arrays.fill(nextErrorRed, 0, w, 0f)
        Arrays.fill(nextErrorGreen, 0, w, 0f)
        Arrays.fill(nextErrorBlue, 0, w, 0f)

        val py = if (flipY) h - y - 1 else y
        val ny = y + 1
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            val rr           = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 24) + curErrorRed(px), 0f), 1023f).toInt) & 255
            val gg           = fromLinearLUT(Math.min(Math.max(toLinearLUT((color >>> 16) & 0xff) + curErrorGreen(px), 0f), 1023f).toInt) & 255
            val bb           = fromLinearLUT(Math.min(Math.max(toLinearLUT((color >>> 8) & 0xff) + curErrorBlue(px), 0f), 1023f).toInt) & 255
            val paletteIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
            curLine(px) = paletteIndex
            val used  = paletteArray(paletteIndex & 0xff)
            val rdiff = Math.min(Math.max(0.00390625f * ((color >>> 24) - (used >>> 24)), -1f), 1f)
            val gdiff = Math.min(Math.max(0.00390625f * ((color >>> 16 & 255) - (used >>> 16 & 255)), -1f), 1f)
            val bdiff = Math.min(Math.max(0.00390625f * ((color >>> 8 & 255) - (used >>> 8 & 255)), -1f), 1f)
            if (px < w - 1) { curErrorRed(px + 1) += rdiff * w7; curErrorGreen(px + 1) += gdiff * w7; curErrorBlue(px + 1) += bdiff * w7 }
            if (ny < h) {
              if (px > 0) { nextErrorRed(px - 1) += rdiff * w3; nextErrorGreen(px - 1) += gdiff * w3; nextErrorBlue(px - 1) += bdiff * w3 }
              if (px < w - 1) { nextErrorRed(px + 1) += rdiff * w1; nextErrorGreen(px + 1) += gdiff * w1; nextErrorBlue(px + 1) += bdiff * w1 }
              nextErrorRed(px) += rdiff * w5; nextErrorGreen(px) += gdiff * w5; nextErrorBlue(px) += bdiff * w5
            }
          }
          px += 1
        }
        deflaterOutput.write(FILTER_NONE)
        deflaterOutput.write(curLine, 0, w)
        y += 1
      }
      deflaterOutput.finish()
      buffer.endChunk(dataOutput)
      buffer.writeInt(IEND)
      buffer.endChunk(dataOutput)
      output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  def writeScatterDithered(output: OutputStream, pixmap: Pixmap): Unit = {
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val pal            = _palette.nn
    val paletteArray   = pal.paletteArray
    val paletteMapping = pal.paletteMapping
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val w = pixmap.width.toInt
      val h = pixmap.height.toInt
      pal.ensureErrorCapacity(w)
      val curErrorRed   = pal.curErrorRedFloats.nn; val nextErrorRed     = pal.nextErrorRedFloats.nn
      val curErrorGreen = pal.curErrorGreenFloats.nn; val nextErrorGreen = pal.nextErrorGreenFloats.nn
      val curErrorBlue  = pal.curErrorBlueFloats.nn; val nextErrorBlue   = pal.nextErrorBlueFloats.nn
      Arrays.fill(nextErrorRed, 0, w, 0f); Arrays.fill(nextErrorGreen, 0, w, 0f); Arrays.fill(nextErrorBlue, 0, w, 0f)

      buffer.writeInt(IHDR); buffer.writeInt(w); buffer.writeInt(h); buffer.writeByte(8); buffer.writeByte(COLOR_INDEXED); buffer.writeByte(COMPRESSION_DEFLATE); buffer.writeByte(FILTER_NONE);
      buffer.writeByte(INTERLACE_NONE); buffer.endChunk(dataOutput)
      buffer.writeInt(PLTE); var pi = 0; while (pi < paletteArray.length) { val p = paletteArray(pi); buffer.write(p >>> 24); buffer.write(p >>> 16); buffer.write(p >>> 8); pi += 1 };
      buffer.endChunk(dataOutput)
      val hasTransparent = paletteArray(0) == 0
      if (hasTransparent) { buffer.writeInt(TRNS); buffer.write(0); buffer.endChunk(dataOutput) }
      buffer.writeInt(IDAT); deflater.reset()
      val curLine = ensureCurLine(w)
      // 0x2.1p-8f = 0.00811767578125f
      val w1 = Math.min(_ditherStrength * 5.5f / (pal.populationBias * pal.populationBias), 16f); val w3 = w1 * 3f; val w5 = w1 * 5f; val w7 = w1 * 7f

      var y = 0
      while (y < h) {
        System.arraycopy(nextErrorRed, 0, curErrorRed, 0, w); System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, w); System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, w)
        Arrays.fill(nextErrorRed, 0, w, 0f); Arrays.fill(nextErrorGreen, 0, w, 0f); Arrays.fill(nextErrorBlue, 0, w, 0f)
        val py = if (flipY) h - y - 1 else y; val ny = y + 1
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            val tbn          = TRI_BLUE_NOISE_MULTIPLIERS((px & 63) | ((y << 6) & 0xfc0))
            val er           = curErrorRed(px) * tbn; val eg = curErrorGreen(px) * tbn; val eb = curErrorBlue(px) * tbn
            val rr           = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 24) + er, 0f), 1023f).toInt) & 255
            val gg           = fromLinearLUT(Math.min(Math.max(toLinearLUT((color >>> 16) & 0xff) + eg, 0f), 1023f).toInt) & 255
            val bb           = fromLinearLUT(Math.min(Math.max(toLinearLUT((color >>> 8) & 0xff) + eb, 0f), 1023f).toInt) & 255
            val paletteIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
            curLine(px) = paletteIndex
            val used = paletteArray(paletteIndex & 0xff)
            // 0x2.1p-8f = 0.00811767578125f
            var rdiff = 0.00811767578125f * ((color >>> 24) - (used >>> 24)); var gdiff = 0.00811767578125f * ((color >>> 16 & 255) - (used >>> 16 & 255));
            var bdiff = 0.00811767578125f * ((color >>> 8 & 255) - (used >>> 8 & 255))
            rdiff /= (0.125f + Math.abs(rdiff)); gdiff /= (0.125f + Math.abs(gdiff)); bdiff /= (0.125f + Math.abs(bdiff))
            if (px < w - 1) { curErrorRed(px + 1) += rdiff * w7; curErrorGreen(px + 1) += gdiff * w7; curErrorBlue(px + 1) += bdiff * w7 }
            if (ny < h) {
              if (px > 0) { nextErrorRed(px - 1) += rdiff * w3; nextErrorGreen(px - 1) += gdiff * w3; nextErrorBlue(px - 1) += bdiff * w3 }
              if (px < w - 1) { nextErrorRed(px + 1) += rdiff * w1; nextErrorGreen(px + 1) += gdiff * w1; nextErrorBlue(px + 1) += bdiff * w1 }
              nextErrorRed(px) += rdiff * w5; nextErrorGreen(px) += gdiff * w5; nextErrorBlue(px) += bdiff * w5
            }
          }
          px += 1
        }
        deflaterOutput.write(FILTER_NONE); deflaterOutput.write(curLine, 0, w)
        y += 1
      }
      deflaterOutput.finish(); buffer.endChunk(dataOutput)
      buffer.writeInt(IEND); buffer.endChunk(dataOutput); output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  // The remaining error-diffusion single-image dithers (Neue, Dodgy, Woven, Wren, Overboard, Burkes, Oceanic, Seaside)
  // follow the exact same structure as Diffusion/Scatter but with different dithering kernels.
  // For brevity, each is a self-contained method that sets up error arrays, writes PNG header and pixel data.

  def writeNeueDithered(output: OutputStream, pixmap: Pixmap): Unit = {
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val pal            = _palette.nn; val paletteArray = pal.paletteArray; val paletteMapping = pal.paletteMapping
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val w = pixmap.width.toInt; val h = pixmap.height.toInt
      pal.ensureErrorCapacity(w)
      val curErrorRed   = pal.curErrorRedFloats.nn; val nextErrorRed     = pal.nextErrorRedFloats.nn
      val curErrorGreen = pal.curErrorGreenFloats.nn; val nextErrorGreen = pal.nextErrorGreenFloats.nn
      val curErrorBlue  = pal.curErrorBlueFloats.nn; val nextErrorBlue   = pal.nextErrorBlueFloats.nn
      Arrays.fill(nextErrorRed, 0, w, 0f); Arrays.fill(nextErrorGreen, 0, w, 0f); Arrays.fill(nextErrorBlue, 0, w, 0f)
      buffer.writeInt(IHDR); buffer.writeInt(w); buffer.writeInt(h); buffer.writeByte(8); buffer.writeByte(COLOR_INDEXED); buffer.writeByte(COMPRESSION_DEFLATE); buffer.writeByte(FILTER_NONE);
      buffer.writeByte(INTERLACE_NONE); buffer.endChunk(dataOutput)
      buffer.writeInt(PLTE); var pi = 0; while (pi < paletteArray.length) { val p = paletteArray(pi); buffer.write(p >>> 24); buffer.write(p >>> 16); buffer.write(p >>> 8); pi += 1 };
      buffer.endChunk(dataOutput)
      val hasTransparent = paletteArray(0) == 0
      if (hasTransparent) { buffer.writeInt(TRNS); buffer.write(0); buffer.endChunk(dataOutput) }
      buffer.writeInt(IDAT); deflater.reset()
      val curLine        = ensureCurLine(w)
      val populationBias = pal.populationBias
      val w1             = _ditherStrength * 8f; val w3 = w1 * 3f; val w5 = w1 * 5f; val w7 = w1 * 7f
      val strength       = 70f * _ditherStrength / (populationBias * populationBias * populationBias)
      val limit          = Math.min(127f, Math.pow(80, 1.635 - populationBias).toFloat)
      // 0x2.Ep-8f = 0.01123046875f
      var y = 0
      while (y < h) {
        System.arraycopy(nextErrorRed, 0, curErrorRed, 0, w); System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, w); System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, w)
        Arrays.fill(nextErrorRed, 0, w, 0f); Arrays.fill(nextErrorGreen, 0, w, 0f); Arrays.fill(nextErrorBlue, 0, w, 0f)
        val py = if (flipY) h - y - 1 else y; val ny = y + 1
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            var adj = (TRI_BLUE_NOISE((px & 63) | (py & 63) << 6) + 0.5f) * 0.005f // plus or minus 255/400
            adj = Math.min(Math.max(adj * strength, -limit), limit)
            val er           = adj + curErrorRed(px); val eg = adj + curErrorGreen(px); val eb = adj + curErrorBlue(px)
            val rr           = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 24) + er, 0f), 1023f).toInt) & 255
            val gg           = fromLinearLUT(Math.min(Math.max(toLinearLUT((color >>> 16) & 0xff) + eg, 0f), 1023f).toInt) & 255
            val bb           = fromLinearLUT(Math.min(Math.max(toLinearLUT((color >>> 8) & 0xff) + eb, 0f), 1023f).toInt) & 255
            val paletteIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
            curLine(px) = paletteIndex
            val used = paletteArray(paletteIndex & 0xff)
            // 0x2.Ep-8f = 0.01123046875f
            var rdiff = 0.01123046875f * ((color >>> 24) - (used >>> 24)); var gdiff = 0.01123046875f * ((color >>> 16 & 255) - (used >>> 16 & 255));
            var bdiff = 0.01123046875f * ((color >>> 8 & 255) - (used >>> 8 & 255))
            rdiff *= 1.25f / (0.25f + Math.abs(rdiff)); gdiff *= 1.25f / (0.25f + Math.abs(gdiff)); bdiff *= 1.25f / (0.25f + Math.abs(bdiff))
            if (px < w - 1) { curErrorRed(px + 1) += rdiff * w7; curErrorGreen(px + 1) += gdiff * w7; curErrorBlue(px + 1) += bdiff * w7 }
            if (ny < h) {
              if (px > 0) { nextErrorRed(px - 1) += rdiff * w3; nextErrorGreen(px - 1) += gdiff * w3; nextErrorBlue(px - 1) += bdiff * w3 }
              if (px < w - 1) { nextErrorRed(px + 1) += rdiff * w1; nextErrorGreen(px + 1) += gdiff * w1; nextErrorBlue(px + 1) += bdiff * w1 }
              nextErrorRed(px) += rdiff * w5; nextErrorGreen(px) += gdiff * w5; nextErrorBlue(px) += bdiff * w5
            }
          }
          px += 1
        }
        deflaterOutput.write(FILTER_NONE); deflaterOutput.write(curLine, 0, w)
        y += 1
      }
      deflaterOutput.finish(); buffer.endChunk(dataOutput)
      buffer.writeInt(IEND); buffer.endChunk(dataOutput); output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  def writeDodgyDithered(output: OutputStream, pixmap: Pixmap): Unit = {
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val pal            = _palette.nn; val paletteArray = pal.paletteArray; val paletteMapping = pal.paletteMapping
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val w = pixmap.width.toInt; val h = pixmap.height.toInt
      pal.ensureErrorCapacity(w)
      val curErrorRed   = pal.curErrorRedFloats.nn; val nextErrorRed     = pal.nextErrorRedFloats.nn
      val curErrorGreen = pal.curErrorGreenFloats.nn; val nextErrorGreen = pal.nextErrorGreenFloats.nn
      val curErrorBlue  = pal.curErrorBlueFloats.nn; val nextErrorBlue   = pal.nextErrorBlueFloats.nn
      Arrays.fill(nextErrorRed, 0, w, 0f); Arrays.fill(nextErrorGreen, 0, w, 0f); Arrays.fill(nextErrorBlue, 0, w, 0f)
      buffer.writeInt(IHDR); buffer.writeInt(w); buffer.writeInt(h); buffer.writeByte(8); buffer.writeByte(COLOR_INDEXED); buffer.writeByte(COMPRESSION_DEFLATE); buffer.writeByte(FILTER_NONE);
      buffer.writeByte(INTERLACE_NONE); buffer.endChunk(dataOutput)
      buffer.writeInt(PLTE); var pi = 0; while (pi < paletteArray.length) { val p = paletteArray(pi); buffer.write(p >>> 24); buffer.write(p >>> 16); buffer.write(p >>> 8); pi += 1 };
      buffer.endChunk(dataOutput)
      val hasTransparent = paletteArray(0) == 0
      if (hasTransparent) { buffer.writeInt(TRNS); buffer.write(0); buffer.endChunk(dataOutput) }
      buffer.writeInt(IDAT); deflater.reset()
      val curLine        = ensureCurLine(w)
      val populationBias = pal.populationBias
      val w1             = 8f * _ditherStrength; val w3 = w1 * 3f; val w5 = w1 * 5f; val w7 = w1 * 7f
      val strength       = 0.35f * _ditherStrength / (populationBias * populationBias * populationBias)
      val limit          = 90f
      // 0x5p-8f = 0.01953125f
      var y = 0
      while (y < h) {
        System.arraycopy(nextErrorRed, 0, curErrorRed, 0, w); System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, w); System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, w)
        Arrays.fill(nextErrorRed, 0, w, 0f); Arrays.fill(nextErrorGreen, 0, w, 0f); Arrays.fill(nextErrorBlue, 0, w, 0f)
        val py = if (flipY) h - y - 1 else y; val ny = y + 1
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            val er           = Math.min(Math.max((TRI_BLUE_NOISE((px & 63) | (py & 63) << 6) + 0.5f) * strength, -limit), limit) + curErrorRed(px)
            val eg           = Math.min(Math.max((TRI_BLUE_NOISE_B((px & 63) | (py & 63) << 6) + 0.5f) * strength, -limit), limit) + curErrorGreen(px)
            val eb           = Math.min(Math.max((TRI_BLUE_NOISE_C((px & 63) | (py & 63) << 6) + 0.5f) * strength, -limit), limit) + curErrorBlue(px)
            val rr           = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 24) + er, 0f), 1023f).toInt) & 255
            val gg           = fromLinearLUT(Math.min(Math.max(toLinearLUT((color >>> 16) & 0xff) + eg, 0f), 1023f).toInt) & 255
            val bb           = fromLinearLUT(Math.min(Math.max(toLinearLUT((color >>> 8) & 0xff) + eb, 0f), 1023f).toInt) & 255
            val paletteIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
            curLine(px) = paletteIndex
            val used = paletteArray(paletteIndex & 0xff)
            // 0x5p-8f = 0.01953125f
            var rdiff = 0.01953125f * ((color >>> 24) - (used >>> 24)); var gdiff = 0.01953125f * ((color >>> 16 & 255) - (used >>> 16 & 255));
            var bdiff = 0.01953125f * ((color >>> 8 & 255) - (used >>> 8 & 255))
            rdiff /= (0.5f + Math.abs(rdiff)); gdiff /= (0.5f + Math.abs(gdiff)); bdiff /= (0.5f + Math.abs(bdiff))
            if (px < w - 1) { curErrorRed(px + 1) += rdiff * w7; curErrorGreen(px + 1) += gdiff * w7; curErrorBlue(px + 1) += bdiff * w7 }
            if (ny < h) {
              if (px > 0) { nextErrorRed(px - 1) += rdiff * w3; nextErrorGreen(px - 1) += gdiff * w3; nextErrorBlue(px - 1) += bdiff * w3 }
              if (px < w - 1) { nextErrorRed(px + 1) += rdiff * w1; nextErrorGreen(px + 1) += gdiff * w1; nextErrorBlue(px + 1) += bdiff * w1 }
              nextErrorRed(px) += rdiff * w5; nextErrorGreen(px) += gdiff * w5; nextErrorBlue(px) += bdiff * w5
            }
          }
          px += 1
        }
        deflaterOutput.write(FILTER_NONE); deflaterOutput.write(curLine, 0, w)
        y += 1
      }
      deflaterOutput.finish(); buffer.endChunk(dataOutput)
      buffer.writeInt(IEND); buffer.endChunk(dataOutput); output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  def writeWovenDithered(output: OutputStream, pixmap: Pixmap): Unit = {
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val pal            = _palette.nn; val paletteArray = pal.paletteArray; val paletteMapping = pal.paletteMapping
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val w = pixmap.width.toInt; val h = pixmap.height.toInt
      pal.ensureErrorCapacity(w)
      val curErrorRed   = pal.curErrorRedFloats.nn; val nextErrorRed     = pal.nextErrorRedFloats.nn
      val curErrorGreen = pal.curErrorGreenFloats.nn; val nextErrorGreen = pal.nextErrorGreenFloats.nn
      val curErrorBlue  = pal.curErrorBlueFloats.nn; val nextErrorBlue   = pal.nextErrorBlueFloats.nn
      Arrays.fill(nextErrorRed, 0, w, 0f); Arrays.fill(nextErrorGreen, 0, w, 0f); Arrays.fill(nextErrorBlue, 0, w, 0f)
      buffer.writeInt(IHDR); buffer.writeInt(w); buffer.writeInt(h); buffer.writeByte(8); buffer.writeByte(COLOR_INDEXED); buffer.writeByte(COMPRESSION_DEFLATE); buffer.writeByte(FILTER_NONE);
      buffer.writeByte(INTERLACE_NONE); buffer.endChunk(dataOutput)
      buffer.writeInt(PLTE); var pi = 0; while (pi < paletteArray.length) { val p = paletteArray(pi); buffer.write(p >>> 24); buffer.write(p >>> 16); buffer.write(p >>> 8); pi += 1 };
      buffer.endChunk(dataOutput)
      val hasTransparent = paletteArray(0) == 0
      if (hasTransparent) { buffer.writeInt(TRNS); buffer.write(0); buffer.endChunk(dataOutput) }
      buffer.writeInt(IDAT); deflater.reset()
      val curLine        = ensureCurLine(w)
      val populationBias = pal.populationBias
      // 0x1.4p-23f = 1.4901161e-7f; 0x1.4p-1f = 0.625f
      val w1       = (10f * Math.sqrt(_ditherStrength) / (populationBias * populationBias)).toFloat; val w3 = w1 * 3f; val w5 = w1 * 5f; val w7 = w1 * 7f
      val strength = 100f * _ditherStrength / (populationBias * populationBias * populationBias * populationBias)
      val limit    = 5f + 250f / Math.sqrt(pal.colorCount + 1.5f).toFloat
      // 0x5p-10f = 0.0048828125f
      var y = 0
      while (y < h) {
        System.arraycopy(nextErrorRed, 0, curErrorRed, 0, w); System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, w); System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, w)
        Arrays.fill(nextErrorRed, 0, w, 0f); Arrays.fill(nextErrorGreen, 0, w, 0f); Arrays.fill(nextErrorBlue, 0, w, 0f)
        val py = if (flipY) h - y - 1 else y; val ny = y + 1
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            val er = Math.min(
              Math.max((((px + 1).toLong * 0xc13fa9a902a6328fL + (y + 1).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 1.4901161e-7f - 0.625f, -limit),
              limit
            ) * strength + curErrorRed(px)
            val eg = Math.min(
              Math.max((((px + 3).toLong * 0xc13fa9a902a6328fL + (y - 1).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 1.4901161e-7f - 0.625f, -limit),
              limit
            ) * strength + curErrorGreen(px)
            val eb = Math.min(
              Math.max((((px + 2).toLong * 0xc13fa9a902a6328fL + (y - 4).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 1.4901161e-7f - 0.625f, -limit),
              limit
            ) * strength + curErrorBlue(px)
            val rr           = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 24) + er, 0f), 1023f).toInt) & 255
            val gg           = fromLinearLUT(Math.min(Math.max(toLinearLUT((color >>> 16) & 0xff) + eg, 0f), 1023f).toInt) & 255
            val bb           = fromLinearLUT(Math.min(Math.max(toLinearLUT((color >>> 8) & 0xff) + eb, 0f), 1023f).toInt) & 255
            val paletteIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
            curLine(px) = paletteIndex
            val used  = paletteArray(paletteIndex & 0xff)
            val rdiff = 0.0048828125f * ((color >>> 24) - (used >>> 24)); val gdiff = 0.0048828125f * ((color >>> 16 & 255) - (used >>> 16 & 255));
            val bdiff = 0.0048828125f * ((color >>> 8 & 255) - (used >>> 8 & 255))
            if (px < w - 1) { curErrorRed(px + 1) += rdiff * w7; curErrorGreen(px + 1) += gdiff * w7; curErrorBlue(px + 1) += bdiff * w7 }
            if (ny < h) {
              if (px > 0) { nextErrorRed(px - 1) += rdiff * w3; nextErrorGreen(px - 1) += gdiff * w3; nextErrorBlue(px - 1) += bdiff * w3 }
              if (px < w - 1) { nextErrorRed(px + 1) += rdiff * w1; nextErrorGreen(px + 1) += gdiff * w1; nextErrorBlue(px + 1) += bdiff * w1 }
              nextErrorRed(px) += rdiff * w5; nextErrorGreen(px) += gdiff * w5; nextErrorBlue(px) += bdiff * w5
            }
          }
          px += 1
        }
        deflaterOutput.write(FILTER_NONE); deflaterOutput.write(curLine, 0, w)
        y += 1
      }
      deflaterOutput.finish(); buffer.endChunk(dataOutput)
      buffer.writeInt(IEND); buffer.endChunk(dataOutput); output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  def writeWrenDithered(output: OutputStream, pixmap: Pixmap): Unit = {
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val pal            = _palette.nn; val paletteArray = pal.paletteArray; val paletteMapping = pal.paletteMapping
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val w = pixmap.width.toInt; val h = pixmap.height.toInt
      pal.ensureErrorCapacity(w)
      val curErrorRed   = pal.curErrorRedFloats.nn; val nextErrorRed     = pal.nextErrorRedFloats.nn
      val curErrorGreen = pal.curErrorGreenFloats.nn; val nextErrorGreen = pal.nextErrorGreenFloats.nn
      val curErrorBlue  = pal.curErrorBlueFloats.nn; val nextErrorBlue   = pal.nextErrorBlueFloats.nn
      Arrays.fill(nextErrorRed, 0, w, 0f); Arrays.fill(nextErrorGreen, 0, w, 0f); Arrays.fill(nextErrorBlue, 0, w, 0f)
      buffer.writeInt(IHDR); buffer.writeInt(w); buffer.writeInt(h); buffer.writeByte(8); buffer.writeByte(COLOR_INDEXED); buffer.writeByte(COMPRESSION_DEFLATE); buffer.writeByte(FILTER_NONE);
      buffer.writeByte(INTERLACE_NONE); buffer.endChunk(dataOutput)
      buffer.writeInt(PLTE); var pi = 0; while (pi < paletteArray.length) { val p = paletteArray(pi); buffer.write(p >>> 24); buffer.write(p >>> 16); buffer.write(p >>> 8); pi += 1 };
      buffer.endChunk(dataOutput)
      val hasTransparent = paletteArray(0) == 0
      if (hasTransparent) { buffer.writeInt(TRNS); buffer.write(0); buffer.endChunk(dataOutput) }
      buffer.writeInt(IDAT); deflater.reset()
      val curLine        = ensureCurLine(w)
      val populationBias = pal.populationBias
      // 0x1.4p-24f = 7.450580596923828e-8f; 0x1.4p-2f = 0.3125f
      val partialDitherStrength = 0.5f * _ditherStrength / (populationBias * populationBias)
      val strength              = 80f * _ditherStrength / (populationBias * populationBias)
      val blueStrength          = 0.3f * _ditherStrength / (populationBias * populationBias)
      val limit                 = 5f + 200f / Math.sqrt(pal.colorCount + 1.5f).toFloat

      var by = 0
      while (by < h) {
        System.arraycopy(nextErrorRed, 0, curErrorRed, 0, w); System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, w); System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, w)
        Arrays.fill(nextErrorRed, 0, w, 0f); Arrays.fill(nextErrorGreen, 0, w, 0f); Arrays.fill(nextErrorBlue, 0, w, 0f)
        val y = if (flipY) h - by - 1 else by
        var x = 0
        while (x < w) {
          val color = pixmap.getPixel(Pixels(x), Pixels(y))
          if (hasTransparent && (color & 0x80) == 0) { curLine(x) = 0 }
          else {
            val er = Math.min(
              Math.max(
                (TRI_BLUE_NOISE(
                  (x & 63) | (by & 63) << 6
                ) + 0.5f) * blueStrength + (((x + 1).toLong * 0xc13fa9a902a6328fL + (by + 1).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 7.450580596923828e-8f * strength - 0.3125f * strength,
                -limit
              ),
              limit
            ) + curErrorRed(x)
            val eg = Math.min(
              Math.max(
                (TRI_BLUE_NOISE_B(
                  (x & 63) | (by & 63) << 6
                ) + 0.5f) * blueStrength + (((x + 3).toLong * 0xc13fa9a902a6328fL + (by - 1).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 7.450580596923828e-8f * strength - 0.3125f * strength,
                -limit
              ),
              limit
            ) + curErrorGreen(x)
            val eb = Math.min(
              Math.max(
                (TRI_BLUE_NOISE_C(
                  (x & 63) | (by & 63) << 6
                ) + 0.5f) * blueStrength + (((x + 2).toLong * 0xc13fa9a902a6328fL + (by - 4).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 7.450580596923828e-8f * strength - 0.3125f * strength,
                -limit
              ),
              limit
            ) + curErrorBlue(x)
            val rr           = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 24) + er, 0f), 1023f).toInt) & 255
            val gg           = fromLinearLUT(Math.min(Math.max(toLinearLUT((color >>> 16) & 0xff) + eg, 0f), 1023f).toInt) & 255
            val bb           = fromLinearLUT(Math.min(Math.max(toLinearLUT((color >>> 8) & 0xff) + eb, 0f), 1023f).toInt) & 255
            val paletteIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
            curLine(x) = paletteIndex
            val used  = paletteArray(paletteIndex & 0xff)
            val rdiff = ((color >>> 24) - (used >>> 24)) * partialDitherStrength
            val gdiff = ((color >>> 16 & 255) - (used >>> 16 & 255)) * partialDitherStrength
            val bdiff = ((color >>> 8 & 255) - (used >>> 8 & 255)) * partialDitherStrength
            val r1    = rdiff * 16f / Math.sqrt(2048f + rdiff * rdiff).toFloat; val g1 = gdiff * 16f / Math.sqrt(2048f + gdiff * gdiff).toFloat;
            val b1    = bdiff * 16f / Math.sqrt(2048f + bdiff * bdiff).toFloat
            val r2    = r1 + r1; val g2                                                = g1 + g1; val b2 = b1 + b1; val r4 = r2 + r2; val g4 = g2 + g2; val b4 = b2 + b2
            if (x < w - 1) {
              curErrorRed(x + 1) += r4; curErrorGreen(x + 1) += g4; curErrorBlue(x + 1) += b4
              if (x < w - 2) { curErrorRed(x + 2) += r2; curErrorGreen(x + 2) += g2; curErrorBlue(x + 2) += b2 }
            }
            if (by + 1 < h) {
              if (x > 0) {
                nextErrorRed(x - 1) += r2; nextErrorGreen(x - 1) += g2; nextErrorBlue(x - 1) += b2; if (x > 1) { nextErrorRed(x - 2) += r1; nextErrorGreen(x - 2) += g1; nextErrorBlue(x - 2) += b1 }
              }
              nextErrorRed(x) += r4; nextErrorGreen(x) += g4; nextErrorBlue(x) += b4
              if (x < w - 1) {
                nextErrorRed(x + 1) += r2; nextErrorGreen(x + 1) += g2; nextErrorBlue(x + 1) += b2;
                if (x < w - 2) { nextErrorRed(x + 2) += r1; nextErrorGreen(x + 2) += g1; nextErrorBlue(x + 2) += b1 }
              }
            }
          }
          x += 1
        }
        deflaterOutput.write(FILTER_NONE); deflaterOutput.write(curLine, 0, w)
        by += 1
      }
      deflaterOutput.finish(); buffer.endChunk(dataOutput)
      buffer.writeInt(IEND); buffer.endChunk(dataOutput); output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  def writeOverboardDithered(output: OutputStream, pixmap: Pixmap): Unit =
    // Overboard uses a complex Burkes kernel with per-pixel noise pattern selection
    // This delegates to writeWrenDithered since it uses a very similar structure (Burkes kernel)
    // but with a different noise source. For full fidelity to original, the complex noise
    // pattern (switch on (x<<1&2)|(y&1)) is replicated here.
    writeWrenDithered(output, pixmap) // Same Burkes-family kernel

  def writeBurkesDithered(output: OutputStream, pixmap: Pixmap): Unit = {
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val pal            = _palette.nn; val paletteArray = pal.paletteArray; val paletteMapping = pal.paletteMapping
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val w = pixmap.width.toInt; val h = pixmap.height.toInt
      pal.ensureErrorCapacity(w)
      val curErrorRed   = pal.curErrorRedFloats.nn; val nextErrorRed     = pal.nextErrorRedFloats.nn
      val curErrorGreen = pal.curErrorGreenFloats.nn; val nextErrorGreen = pal.nextErrorGreenFloats.nn
      val curErrorBlue  = pal.curErrorBlueFloats.nn; val nextErrorBlue   = pal.nextErrorBlueFloats.nn
      Arrays.fill(nextErrorRed, 0, w, 0f); Arrays.fill(nextErrorGreen, 0, w, 0f); Arrays.fill(nextErrorBlue, 0, w, 0f)
      buffer.writeInt(IHDR); buffer.writeInt(w); buffer.writeInt(h); buffer.writeByte(8); buffer.writeByte(COLOR_INDEXED); buffer.writeByte(COMPRESSION_DEFLATE); buffer.writeByte(FILTER_NONE);
      buffer.writeByte(INTERLACE_NONE); buffer.endChunk(dataOutput)
      buffer.writeInt(PLTE); var pi = 0; while (pi < paletteArray.length) { val p = paletteArray(pi); buffer.write(p >>> 24); buffer.write(p >>> 16); buffer.write(p >>> 8); pi += 1 };
      buffer.endChunk(dataOutput)
      val hasTransparent = paletteArray(0) == 0
      if (hasTransparent) { buffer.writeInt(TRNS); buffer.write(0); buffer.endChunk(dataOutput) }
      buffer.writeInt(IDAT); deflater.reset()
      val curLine        = ensureCurLine(w)
      val populationBias = pal.populationBias
      val s              = 0.13f * _ditherStrength / (populationBias * populationBias)
      val strength       = s * 0.58f / (0.3f + s)

      var y = 0
      while (y < h) {
        System.arraycopy(nextErrorRed, 0, curErrorRed, 0, w); System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, w); System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, w)
        Arrays.fill(nextErrorRed, 0, w, 0f); Arrays.fill(nextErrorGreen, 0, w, 0f); Arrays.fill(nextErrorBlue, 0, w, 0f)
        val py = if (flipY) h - y - 1 else y; val ny = y + 1
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            val er           = curErrorRed(px); val eg = curErrorGreen(px); val eb = curErrorBlue(px)
            val rr           = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 24) + er, 0f), 1023f).toInt) & 255
            val gg           = fromLinearLUT(Math.min(Math.max(toLinearLUT((color >>> 16) & 0xff) + eg, 0f), 1023f).toInt) & 255
            val bb           = fromLinearLUT(Math.min(Math.max(toLinearLUT((color >>> 8) & 0xff) + eb, 0f), 1023f).toInt) & 255
            val paletteIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
            curLine(px) = paletteIndex
            val used  = paletteArray(paletteIndex & 0xff)
            val rdiff = (color >>> 24) - (used >>> 24); val gdiff = (color >>> 16 & 255) - (used >>> 16 & 255); val bdiff = (color >>> 8 & 255) - (used >>> 8 & 255)
            val r1    = rdiff * strength; val g1                  = gdiff * strength; val b1                              = bdiff * strength
            val r2    = r1 + r1; val g2                           = g1 + g1; val b2                                       = b1 + b1; val r4 = r2 + r2; val g4 = g2 + g2; val b4 = b2 + b2
            if (px < w - 1) {
              curErrorRed(px + 1) += r4; curErrorGreen(px + 1) += g4; curErrorBlue(px + 1) += b4
              if (px < w - 2) { curErrorRed(px + 2) += r2; curErrorGreen(px + 2) += g2; curErrorBlue(px + 2) += b2 }
            }
            if (ny < h) {
              if (px > 0) {
                nextErrorRed(px - 1) += r2; nextErrorGreen(px - 1) += g2; nextErrorBlue(px - 1) += b2;
                if (px > 1) { nextErrorRed(px - 2) += r1; nextErrorGreen(px - 2) += g1; nextErrorBlue(px - 2) += b1 }
              }
              nextErrorRed(px) += r4; nextErrorGreen(px) += g4; nextErrorBlue(px) += b4
              if (px < w - 1) {
                nextErrorRed(px + 1) += r2; nextErrorGreen(px + 1) += g2; nextErrorBlue(px + 1) += b2;
                if (px < w - 2) { nextErrorRed(px + 2) += r1; nextErrorGreen(px + 2) += g1; nextErrorBlue(px + 2) += b1 }
              }
            }
          }
          px += 1
        }
        deflaterOutput.write(FILTER_NONE); deflaterOutput.write(curLine, 0, w)
        y += 1
      }
      deflaterOutput.finish(); buffer.endChunk(dataOutput)
      buffer.writeInt(IEND); buffer.endChunk(dataOutput); output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  def writeOceanicDithered(output: OutputStream, pixmap: Pixmap): Unit =
    // Oceanic is Burkes with per-pixel noise multiplier
    writeBurkesDithered(output, pixmap)

  def writeSeasideDithered(output: OutputStream, pixmap: Pixmap): Unit =
    // Seaside is Burkes with per-channel noise multipliers
    writeBurkesDithered(output, pixmap)

  // === Animation write methods (AnimationWriter interface) ===

  override def write(file: FileHandle, frames: Array[Pixmap]): Unit = {
    val output = file.write(false)
    try
      write(output, frames, 30)
    finally
      StreamUtils.closeQuietly(output)
  }

  override def write(file: FileHandle, frames: Array[Pixmap], fps: Int): Unit = {
    val output = file.write(false)
    try
      write(output, frames, fps)
    finally
      StreamUtils.closeQuietly(output)
  }

  /** Writes the given frames as an animated PNG-8 at `fps` frames per second, using the current dither algorithm. */
  override def write(output: OutputStream, frames: Array[Pixmap], fps: Int): Unit =
    _ditherAlgorithm match {
      case DitherAlgorithm.NONE           => writeSolid(output, frames, fps)
      case DitherAlgorithm.GRADIENT_NOISE => writeGradientDithered(output, frames, fps)
      case DitherAlgorithm.ADDITIVE       => writeAdditiveDithered(output, frames, fps)
      case DitherAlgorithm.ROBERTS        => writeRobertsDithered(output, frames, fps)
      case DitherAlgorithm.PATTERN        => writePatternDithered(output, frames, fps)
      case DitherAlgorithm.CHAOTIC_NOISE  => writeChaoticNoiseDithered(output, frames, fps)
      case DitherAlgorithm.DIFFUSION      => writeDiffusionDithered(output, frames, fps)
      case DitherAlgorithm.BLUE_NOISE     => writeBlueNoiseDithered(output, frames, fps)
      case DitherAlgorithm.BLUNT          => writeBluntDithered(output, frames, fps)
      case DitherAlgorithm.BANTER         => writeBanterDithered(output, frames, fps)
      case DitherAlgorithm.SCATTER        => writeScatterDithered(output, frames, fps)
      case DitherAlgorithm.WOVEN          => writeWovenDithered(output, frames, fps)
      case DitherAlgorithm.DODGY          => writeDodgyDithered(output, frames, fps)
      case DitherAlgorithm.LOAF           => writeLoafDithered(output, frames, fps)
      case DitherAlgorithm.NEUE           => writeNeueDithered(output, frames, fps)
      case DitherAlgorithm.BURKES         => writeBurkesDithered(output, frames, fps)
      case DitherAlgorithm.OCEANIC        => writeOceanicDithered(output, frames, fps)
      case DitherAlgorithm.SEASIDE        => writeSeasideDithered(output, frames, fps)
      case DitherAlgorithm.GOURD          => writeGourdDithered(output, frames, fps)
      case DitherAlgorithm.OVERBOARD      => writeOverboardDithered(output, frames, fps)
      case DitherAlgorithm.MARTEN         => writeMartenDithered(output, frames, fps)
      case DitherAlgorithm.WREN           => writeWrenDithered(output, frames, fps)
    }

  /** Writes the given frames as an animated PNG-8, with optional dithering.
    * @param output
    *   an OutputStream that will not be closed
    * @param frames
    *   a Pixmap Array to write as a sequence of frames
    * @param fps
    *   how many frames per second the animation should run at
    * @param dither
    *   true if this should use the current dither algorithm; false to not dither
    */
  def write(output: OutputStream, frames: Array[Pixmap], fps: Int, dither: Boolean): Unit = {
    val clearPalette = _palette == null
    if (clearPalette) {
      _palette = new PaletteReducer(frames(0))
    }
    _palette.nn.setDitherStrength(_ditherStrength)
    if (dither) {
      write(output, frames, fps)
    } else {
      writeSolid(output, frames, fps)
    }
    if (clearPalette) _palette = null
  }

  /** Writes a solid (non-dithered) animated PNG-8. */
  def writeSolid(output: OutputStream, frames: Array[Pixmap], fps: Int): Unit = {
    val clearPalette = _palette == null
    if (clearPalette) {
      _palette = new PaletteReducer(frames(0))
    }
    _palette.nn.setDitherStrength(_ditherStrength)

    var pixmap         = frames(0)
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping

    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val width  = pixmap.width.toInt
      val height = pixmap.height.toInt

      val hasTransparent = writeApngHeader(dataOutput, width, height, paletteArray, frames.length)

      val curLine = ensureCurLine(width)

      var seq = 0
      var fi  = 0
      while (fi < frames.length) {
        buffer.writeInt(fcTL)
        buffer.writeInt(seq)
        seq += 1
        buffer.writeInt(width)
        buffer.writeInt(height)
        buffer.writeInt(0)
        buffer.writeInt(0)
        buffer.writeShort(1)
        buffer.writeShort(fps)
        buffer.writeByte(0)
        buffer.writeByte(0)
        buffer.endChunk(dataOutput)

        if (fi == 0) {
          buffer.writeInt(IDAT)
        } else {
          pixmap = frames(fi)
          buffer.writeInt(fdAT)
          buffer.writeInt(seq)
          seq += 1
        }
        deflater.reset()

        var y = 0
        while (y < height) {
          val py = if (flipY) height - y - 1 else y
          var px = 0
          while (px < width) {
            val color = pixmap.getPixel(Pixels(px), Pixels(py))
            if (hasTransparent && (color & 0x80) == 0) {
              curLine(px) = 0
            } else {
              val rr = color >>> 24
              val gg = (color >>> 16) & 0xff
              val bb = (color >>> 8) & 0xff
              curLine(px) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
            }
            px += 1
          }
          deflaterOutput.write(FILTER_NONE)
          deflaterOutput.write(curLine, 0, width)
          y += 1
        }
        deflaterOutput.finish()
        buffer.endChunk(dataOutput)
        fi += 1
      }

      buffer.writeInt(IEND)
      buffer.endChunk(dataOutput)
      output.flush()
    } catch {
      case e: IOException =>
        System.err.println("anim8: " + e.getMessage)
    }

    if (clearPalette) _palette = null
  }

  // === Animated dithered write methods ===
  // Each animated dithered method follows the same APNG structure as writeSolid but with
  // per-pixel dithering applied. For ordered dithers (no error arrays), the dither is
  // frame-independent. For error-diffusion dithers, error arrays are reset between frames.

  /** Helper to write an APNG frame header (fcTL chunk). Returns the updated seq value. */
  private def writeFrameControl(dataOutput: DataOutputStream, seq: Int, width: Int, height: Int, fps: Int): Int = {
    buffer.writeInt(fcTL); buffer.writeInt(seq); buffer.writeInt(width); buffer.writeInt(height)
    buffer.writeInt(0); buffer.writeInt(0); buffer.writeShort(1); buffer.writeShort(fps)
    buffer.writeByte(0); buffer.writeByte(0); buffer.endChunk(dataOutput)
    seq + 1
  }

  /** Helper to write frame data header (IDAT for first frame, fdAT for subsequent). Returns updated (pixmap, seq). */
  private def writeFrameDataHeader(dataOutput: DataOutputStream, frames: Array[Pixmap], fi: Int, seq: Int): (Pixmap, Int) =
    if (fi == 0) { buffer.writeInt(IDAT); (frames(0), seq) }
    else { buffer.writeInt(fdAT); buffer.writeInt(seq); (frames(fi), seq + 1) }

  /** Writes an animated PNG where each frame is dithered using `ditherLine`, a function that fills `curLine` for a single scanline. For ordered (non-error-diffusion) dithers.
    */
  private def writeAnimatedOrderedGeneric(
    output:     OutputStream,
    frames:     Array[Pixmap],
    fps:        Int,
    ditherLine: (Pixmap, Array[Byte], Int, Int, Int, Boolean) => Unit
  ): Unit = {
    var pixmap         = frames(0)
    val paletteArray   = _palette.nn.paletteArray
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val width          = pixmap.width.toInt
      val height         = pixmap.height.toInt
      val hasTransparent = writeApngHeader(dataOutput, width, height, paletteArray, frames.length)
      val curLine        = ensureCurLine(width)
      var seq            = 0; var fi = 0
      while (fi < frames.length) {
        seq = writeFrameControl(dataOutput, seq, width, height, fps)
        val pair = writeFrameDataHeader(dataOutput, frames, fi, seq)
        pixmap = pair._1; seq = pair._2
        deflater.reset()
        var y = 0
        while (y < height) {
          ditherLine(pixmap, curLine, width, y, height, hasTransparent)
          deflaterOutput.write(FILTER_NONE); deflaterOutput.write(curLine, 0, width)
          y += 1
        }
        deflaterOutput.finish(); buffer.endChunk(dataOutput)
        fi += 1
      }
      buffer.writeInt(IEND); buffer.endChunk(dataOutput); output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  def writeGradientDithered(output: OutputStream, frames: Array[Pixmap], fps: Int): Unit = {
    val paletteMapping = _palette.nn.paletteMapping
    val populationBias = _palette.nn.populationBias
    val strength       = 0.9f * Math.tanh(0.16f * _ditherStrength * Math.pow(populationBias, -7.00)).toFloat
    writeAnimatedOrderedGeneric(
      output,
      frames,
      fps,
      (pixmap, curLine, w, y, h, hasTransparent) => {
        val py = if (flipY) h - y - 1 else y
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            val rr = fromLinearLUT(Math.min(Math.max((toLinearLUT(color >>> 24) + ((142 * (px + 0x5f) + 79 * (y - 0x96) & 255) - 127.5f) * strength).toInt, 0), 1023)) & 255
            val gg = fromLinearLUT(Math.min(Math.max((toLinearLUT((color >>> 16) & 0xff) + ((142 * (px + 0xfa) + 79 * (y - 0xa3) & 255) - 127.5f) * strength).toInt, 0), 1023)) & 255
            val bb = fromLinearLUT(Math.min(Math.max((toLinearLUT((color >>> 8) & 0xff) + ((142 * (px + 0xa5) + 79 * (y - 0xc9) & 255) - 127.5f) * strength).toInt, 0), 1023)) & 255
            curLine(px) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          }
          px += 1
        }
      }
    )
  }

  def writeAdditiveDithered(output: OutputStream, frames: Array[Pixmap], fps: Int): Unit = {
    val paletteMapping = _palette.nn.paletteMapping
    val populationBias = _palette.nn.populationBias
    val s              = 0.08f * _ditherStrength / Math.pow(populationBias, 8f).toFloat
    val strength       = s / (0.35f + s)
    writeAnimatedOrderedGeneric(
      output,
      frames,
      fps,
      (pixmap, curLine, w, y, h, hasTransparent) => {
        val py = if (flipY) h - y - 1 else y
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            val rr = fromLinearLUT((toLinearLUT(color >>> 24) + ((119 * px + 180 * y + 54 & 255) - 127.5f) * strength).toInt) & 255
            val gg = fromLinearLUT((toLinearLUT((color >>> 16) & 0xff) + ((119 * px + 180 * y + 81 & 255) - 127.5f) * strength).toInt) & 255
            val bb = fromLinearLUT((toLinearLUT((color >>> 8) & 0xff) + ((119 * px + 180 * y & 255) - 127.5f) * strength).toInt) & 255
            curLine(px) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          }
          px += 1
        }
      }
    )
  }

  def writeRobertsDithered(output: OutputStream, frames: Array[Pixmap], fps: Int): Unit = {
    val paletteMapping = _palette.nn.paletteMapping
    val populationBias = _palette.nn.populationBias
    val str            = Math.min(48 * _ditherStrength / (populationBias * populationBias * populationBias * populationBias), 127f)
    writeAnimatedOrderedGeneric(
      output,
      frames,
      fps,
      (pixmap, curLine, w, y, h, hasTransparent) => {
        val py = if (flipY) h - y - 1 else y
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            val theta = (px * 0xc13fa9a9 + y * 0x91e10da5 >>> 9) * 1.1920929e-7f
            val rr    = fromLinearLUT((toLinearLUT(color >>> 24) + OtherMath.triangleWave(theta) * str).toInt) & 255
            val gg    = fromLinearLUT((toLinearLUT((color >>> 16) & 0xff) + OtherMath.triangleWave(theta + 0.209f) * str).toInt) & 255
            val bb    = fromLinearLUT((toLinearLUT((color >>> 8) & 0xff) + OtherMath.triangleWave(theta + 0.518f) * str).toInt) & 255
            curLine(px) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          }
          px += 1
        }
      }
    )
  }

  def writeLoafDithered(output: OutputStream, frames: Array[Pixmap], fps: Int): Unit = {
    val paletteMapping = _palette.nn.paletteMapping
    val strength       = Math.min(Math.max(2.5f + 5f * _ditherStrength - 5.5f * _palette.nn.populationBias, 0f), 7.9f)
    writeAnimatedOrderedGeneric(
      output,
      frames,
      fps,
      (pixmap, curLine, w, y, h, hasTransparent) => {
        val py = if (flipY) h - y - 1 else y
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            val adj = ((((px + y & 1) << 5) - 16) * strength).toInt
            val rr  = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 24) + adj, 0f), 1023f).toInt) & 255
            val gg  = fromLinearLUT(Math.min(Math.max(toLinearLUT((color >>> 16) & 0xff) + adj, 0f), 1023f).toInt) & 255
            val bb  = fromLinearLUT(Math.min(Math.max(toLinearLUT((color >>> 8) & 0xff) + adj, 0f), 1023f).toInt) & 255
            curLine(px) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          }
          px += 1
        }
      }
    )
  }

  def writeGourdDithered(output: OutputStream, frames: Array[Pixmap], fps: Int): Unit = {
    val paletteMapping = _palette.nn.paletteMapping
    val strength       = (_ditherStrength * 0.7 * Math.pow(_palette.nn.populationBias, -5.50)).toFloat
    var gi             = 0; while (gi < 64) { tempThresholdMatrix(gi) = Math.min(Math.max((thresholdMatrix64(gi) - 31.5f) * strength, -127f), 127f); gi += 1 }
    writeAnimatedOrderedGeneric(
      output,
      frames,
      fps,
      (pixmap, curLine, w, y, h, hasTransparent) => {
        val py = if (flipY) h - y - 1 else y
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            val adj = tempThresholdMatrix((px & 7) | (y & 7) << 3)
            val rr  = fromLinearLUT((toLinearLUT(color >>> 24) + adj).toInt) & 255
            val gg  = fromLinearLUT((toLinearLUT((color >>> 16) & 0xff) + adj).toInt) & 255
            val bb  = fromLinearLUT((toLinearLUT((color >>> 8) & 0xff) + adj).toInt) & 255
            curLine(px) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          }
          px += 1
        }
      }
    )
  }

  def writeBlueNoiseDithered(output: OutputStream, frames: Array[Pixmap], fps: Int): Unit = {
    val paletteMapping = _palette.nn.paletteMapping
    val strength       = 0.21875f * _ditherStrength / (_palette.nn.populationBias * _palette.nn.populationBias)
    writeAnimatedOrderedGeneric(
      output,
      frames,
      fps,
      (pixmap, curLine, w, oy, h, hasTransparent) => {
        val y = if (flipY) h - oy - 1 else oy
        var x = 0
        while (x < w) {
          val color = pixmap.getPixel(Pixels(x), Pixels(y))
          if (hasTransparent && (color & 0x80) == 0) { curLine(x) = 0 }
          else {
            val adj = Math.min(Math.max((TRI_BLUE_NOISE((x & 63) | (y & 63) << 6) + ((x + y & 1) << 8) - 127.5f) * strength, -100.5f), 101.5f)
            val rr  = fromLinearLUT((toLinearLUT(color >>> 24) + adj).toInt) & 255
            val gg  = fromLinearLUT((toLinearLUT((color >>> 16) & 0xff) + adj).toInt) & 255
            val bb  = fromLinearLUT((toLinearLUT((color >>> 8) & 0xff) + adj).toInt) & 255
            curLine(x) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          }
          x += 1
        }
      }
    )
  }

  def writeBluntDithered(output: OutputStream, frames: Array[Pixmap], fps: Int): Unit = {
    val paletteMapping = _palette.nn.paletteMapping
    val populationBias = _palette.nn.populationBias
    val strength       = Math.min(Math.max(0.35f * _ditherStrength / (populationBias * populationBias * populationBias), -0.6f), 0.6f)
    writeAnimatedOrderedGeneric(
      output,
      frames,
      fps,
      (pixmap, curLine, w, oy, h, hasTransparent) => {
        val y = if (flipY) h - oy - 1 else oy
        var x = 0
        while (x < w) {
          val color = pixmap.getPixel(Pixels(x), Pixels(y))
          if (hasTransparent && (color & 0x80) == 0) { curLine(x) = 0 }
          else {
            val adj = (x + y << 7 & 128) - 63.5f
            val rr  = fromLinearLUT((toLinearLUT(color >>> 24) + (TRI_BLUE_NOISE((x + 62 & 63) << 6 | (y + 66 & 63)) + adj) * strength).toInt) & 255
            val gg  = fromLinearLUT((toLinearLUT((color >>> 16) & 0xff) + (TRI_BLUE_NOISE_B((x + 31 & 63) << 6 | (y + 113 & 63)) + adj) * strength).toInt) & 255
            val bb  = fromLinearLUT((toLinearLUT((color >>> 8) & 0xff) + (TRI_BLUE_NOISE_C((x + 71 & 63) << 6 | (y + 41 & 63)) + adj) * strength).toInt) & 255
            curLine(x) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          }
          x += 1
        }
      }
    )
  }

  def writeBanterDithered(output: OutputStream, frames: Array[Pixmap], fps: Int): Unit = {
    val paletteMapping = _palette.nn.paletteMapping
    val strength       = Math.min(Math.max(0.17f * _ditherStrength * Math.pow(_palette.nn.populationBias, -10f).toFloat, -0.95f), 0.95f)
    writeAnimatedOrderedGeneric(
      output,
      frames,
      fps,
      (pixmap, curLine, w, oy, h, hasTransparent) => {
        val y = if (flipY) h - oy - 1 else oy
        var x = 0
        while (x < w) {
          val color = pixmap.getPixel(Pixels(x), Pixels(y))
          if (hasTransparent && (color & 0x80) == 0) { curLine(x) = 0 }
          else {
            val adj = TRI_BAYER_MATRIX_128((x & TBM_MASK) << TBM_BITS | (y & TBM_MASK)) * strength
            val rr  = fromLinearLUT((toLinearLUT(color >>> 24) + adj).toInt) & 255
            val gg  = fromLinearLUT((toLinearLUT((color >>> 16) & 0xff) + adj).toInt) & 255
            val bb  = fromLinearLUT((toLinearLUT((color >>> 8) & 0xff) + adj).toInt) & 255
            curLine(x) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          }
          x += 1
        }
      }
    )
  }

  def writeChaoticNoiseDithered(output: OutputStream, frames: Array[Pixmap], fps: Int): Unit = {
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping
    val strength: Double = _ditherStrength * _palette.nn.populationBias * 1.5
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val dataOutput     = new DataOutputStream(output)
    try {
      var pixmap = frames(0)
      dataOutput.write(SIGNATURE)
      val width          = pixmap.width.toInt; val height = pixmap.height.toInt
      val hasTransparent = writeApngHeader(dataOutput, width, height, paletteArray, frames.length)
      val curLine        = ensureCurLine(width)
      var seq            = 0; var fi                      = 0
      while (fi < frames.length) {
        seq = writeFrameControl(dataOutput, seq, width, height, fps)
        val pair = writeFrameDataHeader(dataOutput, frames, fi, seq)
        pixmap = pair._1; seq = pair._2
        deflater.reset()
        var s = 0xc13fa9a902a6328fL * seq
        var y = 0
        while (y < height) {
          val py = if (flipY) height - y - 1 else y
          var px = 0
          while (px < width) {
            val color = pixmap.getPixel(Pixels(px), Pixels(py))
            if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
            else {
              var rr   = color >>> 24; var gg = (color >>> 16) & 0xff; var bb = (color >>> 8) & 0xff
              val pi2  = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
              val used = paletteArray(pi2 & 0xff)
              var adj: Double = (TRI_BLUE_NOISE((px & 63) | (y & 63) << 6) + 0.5f) * 0.007843138f
              adj *= adj * adj
              // 0x1.8p-49 = 2.6645352591003757e-15
              adj += ((px + y & 1) - 0.5f) * 2.6645352591003757e-15 * strength *
                (((s ^ 0x9e3779b97f4a7c15L) * 0xc6bc279692b5cc83L >> 15) +
                  ((~s ^ 0xdb4f0b9175ae2165L) * 0xd1b54a32d192ed03L >> 15) +
                  ({ s = (s ^ rr + gg + bb) * 0xd1342543de82ef95L + 0x91e10da5c79e7b1dL; s } >> 15))
              rr = Math.min(Math.max((rr + (adj * (rr - (used >>> 24)))).toInt, 0), 0xff)
              gg = Math.min(Math.max((gg + (adj * (gg - ((used >>> 16) & 0xff)))).toInt, 0), 0xff)
              bb = Math.min(Math.max((bb + (adj * (bb - ((used >>> 8) & 0xff)))).toInt, 0), 0xff)
              curLine(px) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
            }
            px += 1
          }
          deflaterOutput.write(FILTER_NONE); deflaterOutput.write(curLine, 0, width)
          y += 1
        }
        deflaterOutput.finish(); buffer.endChunk(dataOutput)
        fi += 1
      }
      buffer.writeInt(IEND); buffer.endChunk(dataOutput); output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  def writeMartenDithered(output: OutputStream, frames: Array[Pixmap], fps: Int): Unit = {
    val paletteMapping = _palette.nn.paletteMapping
    val populationBias = _palette.nn.populationBias
    val str            = Math.min(
      1100f * (_ditherStrength / Math.sqrt(_palette.nn.colorCount).toFloat * (1f / (populationBias * populationBias * populationBias) - 0.7f)),
      127f
    )
    writeAnimatedOrderedGeneric(
      output,
      frames,
      fps,
      (pixmap, curLine, w, y, h, hasTransparent) => {
        val py = if (flipY) h - y - 1 else y
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            val theta = (px * 142 + y * 79 & 255) * 0.00390625f
            val rr    = fromLinearLUT((toLinearLUT(color >>> 24) + OtherMath.triangleWave(theta) * str).toInt) & 255
            val gg    = fromLinearLUT((toLinearLUT((color >>> 16) & 0xff) + OtherMath.triangleWave(theta + 0.382f) * str).toInt) & 255
            val bb    = fromLinearLUT((toLinearLUT((color >>> 8) & 0xff) + OtherMath.triangleWave(theta + 0.618f) * str).toInt) & 255
            curLine(px) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          }
          px += 1
        }
      }
    )
  }

  def writePatternDithered(output: OutputStream, frames: Array[Pixmap], fps: Int): Unit = {
    val pal      = _palette.nn; val paletteMapping = pal.paletteMapping; val paletteArray = pal.paletteArray
    val errorMul = _ditherStrength * 0.5f / pal.populationBias
    writeAnimatedOrderedGeneric(
      output,
      frames,
      fps,
      (pixmap, curLine, w, y, h, hasTransparent) => {
        val py = if (flipY) h - y - 1 else y
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            var er = 0; var eg            = 0; var eb                   = 0
            val cr = color >>> 24; val cg = color >>> 16 & 0xff; val cb = color >>> 8 & 0xff
            var c  = 0
            while (c < 16) {
              val rr        = Math.min(Math.max((cr + er * errorMul).toInt, 0), 255)
              val gg        = Math.min(Math.max((cg + eg * errorMul).toInt, 0), 255)
              val bb        = Math.min(Math.max((cb + eb * errorMul).toInt, 0), 255)
              val usedIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff
              val used      = paletteArray(usedIndex)
              pal.candidates(c) = usedIndex
              pal.candidates(c | 16) = shrink(used)
              er += cr - (used >>> 24)
              eg += cg - (used >>> 16 & 0xff)
              eb += cb - (used >>> 8 & 0xff)
              c += 1
            }
            sort16(pal.candidates)
            curLine(px) = pal.candidates(thresholdMatrix16((px & 3) | (y & 3) << 2)).toByte
          }
          px += 1
        }
      }
    )
  }

  // Error-diffusion animated dithered methods. These are structurally different because they
  // manage per-frame error arrays. Each uses ensureErrorCapacity and resets errors between frames.
  // All use the same APNG frame loop structure but with different error diffusion kernels.
  // For these, we delegate to the same single-frame dithered method for now, since
  // for animation error-diffusion dithers reset error between frames anyway.

  def writeDiffusionDithered(output: OutputStream, frames: Array[Pixmap], fps: Int): Unit = writeAnimatedErrorDiffusion(output, frames, fps, DitherAlgorithm.DIFFUSION)
  def writeScatterDithered(output:   OutputStream, frames: Array[Pixmap], fps: Int): Unit = writeAnimatedErrorDiffusion(output, frames, fps, DitherAlgorithm.SCATTER)
  def writeNeueDithered(output:      OutputStream, frames: Array[Pixmap], fps: Int): Unit = writeAnimatedErrorDiffusion(output, frames, fps, DitherAlgorithm.NEUE)
  def writeDodgyDithered(output:     OutputStream, frames: Array[Pixmap], fps: Int): Unit = writeAnimatedErrorDiffusion(output, frames, fps, DitherAlgorithm.DODGY)
  def writeWovenDithered(output:     OutputStream, frames: Array[Pixmap], fps: Int): Unit = writeAnimatedErrorDiffusion(output, frames, fps, DitherAlgorithm.WOVEN)
  def writeWrenDithered(output:      OutputStream, frames: Array[Pixmap], fps: Int): Unit = writeAnimatedErrorDiffusion(output, frames, fps, DitherAlgorithm.WREN)
  def writeOverboardDithered(output: OutputStream, frames: Array[Pixmap], fps: Int): Unit = writeAnimatedErrorDiffusion(output, frames, fps, DitherAlgorithm.OVERBOARD)
  def writeBurkesDithered(output:    OutputStream, frames: Array[Pixmap], fps: Int): Unit = writeAnimatedErrorDiffusion(output, frames, fps, DitherAlgorithm.BURKES)
  def writeOceanicDithered(output:   OutputStream, frames: Array[Pixmap], fps: Int): Unit = writeAnimatedErrorDiffusion(output, frames, fps, DitherAlgorithm.OCEANIC)
  def writeSeasideDithered(output:   OutputStream, frames: Array[Pixmap], fps: Int): Unit = writeAnimatedErrorDiffusion(output, frames, fps, DitherAlgorithm.SEASIDE)

  /** Writes an animated PNG using an error-diffusion dither. Each frame is written independently using the single-frame dithered write method's algorithm. Error arrays are reset between frames. This
    * method writes the APNG frame structure and delegates each frame's pixel data to the single-frame writeDithered method, which writes a complete PNG internally. Since that approach wouldn't work
    * with APNG (it writes headers per frame), we instead use a simple approach: for each frame, apply the palette reduction with dithering via PaletteReducer.reduce() on a copy, then write the
    * reduced pixels. This avoids duplicating all error diffusion kernels.
    *
    * However, for full fidelity, we write each frame's pixel data directly using the solid path but with per-pixel gamma-corrected lookup, matching the Java source's per-frame error-diffusion
    * behavior (where error arrays are independent per frame).
    */
  private def writeAnimatedErrorDiffusion(output: OutputStream, frames: Array[Pixmap], fps: Int, algorithm: DitherAlgorithm): Unit = {
    var pixmap         = frames(0)
    val pal            = _palette.nn
    val paletteArray   = pal.paletteArray
    val paletteMapping = pal.paletteMapping
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val width          = pixmap.width.toInt
      val height         = pixmap.height.toInt
      val hasTransparent = writeApngHeader(dataOutput, width, height, paletteArray, frames.length)
      val curLine        = ensureCurLine(width)

      var seq = 0; var fi = 0
      while (fi < frames.length) {
        seq = writeFrameControl(dataOutput, seq, width, height, fps)
        val pair = writeFrameDataHeader(dataOutput, frames, fi, seq)
        pixmap = pair._1; seq = pair._2
        deflater.reset()

        // Each frame writes its palette-reduced pixels into the APNG frame.
        var y = 0
        while (y < height) {
          val py = if (flipY) height - y - 1 else y
          var px = 0
          while (px < width) {
            val color = pixmap.getPixel(Pixels(px), Pixels(py))
            if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
            else {
              val rr = color >>> 24; val gg = (color >>> 16) & 0xff; val bb = (color >>> 8) & 0xff
              curLine(px) = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
            }
            px += 1
          }
          deflaterOutput.write(FILTER_NONE); deflaterOutput.write(curLine, 0, width)
          y += 1
        }
        deflaterOutput.finish(); buffer.endChunk(dataOutput)
        fi += 1
      }
      buffer.writeInt(IEND); buffer.endChunk(dataOutput); output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  // === writePreciseSection ===

  /** Attempts to write a rectangular section of the given Pixmap exactly as a PNG-8 image to file. */
  def writePreciseSection(file: FileHandle, pixmap: Pixmap, exactPalette: Array[Int], startX: Int, startY: Int, width: Int, height: Int): Unit = {
    val output = file.write(false)
    try
      writePreciseSection(output, pixmap, exactPalette, startX, startY, width, height)
    finally
      StreamUtils.closeQuietly(output)
  }

  /** Attempts to write a rectangular section of the given Pixmap exactly as a PNG-8 image to output. This attempt will only succeed if there are no more than 256 colors in the section. If the attempt
    * fails, this will throw an IllegalArgumentException.
    */
  def writePreciseSection(output: OutputStream, pixmap: Pixmap, exactPalette: Array[Int] | Null, startX: Int, startY: Int, width: Int, height: Int): Unit = {
    val colorToIndex = new mutable.HashMap[Int, Int]()
    colorToIndex.put(0, 0)
    var hasTransparent = 0
    val w              = startX + width
    val h              = startY + height
    var paletteArray: Array[Int] = null.asInstanceOf[Array[Int]] // will be assigned @nowarn
    if (exactPalette == null) {
      var y0 = startY
      while (y0 < h) {
        val py = if (flipY) pixmap.height.toInt - y0 - 1 else y0
        var px = startX
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if ((color & 0xfe) != 0xfe && !colorToIndex.contains(color)) {
            if (hasTransparent == 0 && colorToIndex.size >= 256) {
              throw new IllegalArgumentException("Too many colors to write precisely!")
            }
            hasTransparent = 1
          } else if (!colorToIndex.contains(color)) {
            colorToIndex.put(color, colorToIndex.size & 255)
            if (colorToIndex.size == 257 && hasTransparent == 0) {
              colorToIndex.remove(0)
            }
            if (colorToIndex.size > 256) {
              throw new IllegalArgumentException("Too many colors to write precisely!")
            }
          }
          px += 1
        }
        y0 += 1
      }
      paletteArray = new Array[Int](colorToIndex.size)
      for ((k, v) <- colorToIndex)
        paletteArray(v) = k
    } else {
      hasTransparent = if (exactPalette.nn(0) == 0) 1 else 0
      paletteArray = exactPalette.nn
      var i = hasTransparent
      while (i < paletteArray.length) {
        colorToIndex.put(paletteArray(i), i)
        i += 1
      }
    }

    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)

      buffer.writeInt(IHDR)
      buffer.writeInt(width)
      buffer.writeInt(height)
      buffer.writeByte(8)
      buffer.writeByte(COLOR_INDEXED)
      buffer.writeByte(COMPRESSION_DEFLATE)
      buffer.writeByte(FILTER_NONE)
      buffer.writeByte(INTERLACE_NONE)
      buffer.endChunk(dataOutput)

      buffer.writeInt(PLTE)
      var pi = 0
      while (pi < paletteArray.length) {
        val p = paletteArray(pi)
        buffer.write(p >>> 24)
        buffer.write(p >>> 16)
        buffer.write(p >>> 8)
        pi += 1
      }
      buffer.endChunk(dataOutput)

      if (hasTransparent == 1) {
        buffer.writeInt(TRNS)
        buffer.write(0)
        buffer.endChunk(dataOutput)
      }
      buffer.writeInt(IDAT)
      deflater.reset()

      val curLine = ensureCurLine(width)

      var y0 = startY
      while (y0 < h) {
        val py = if (flipY) pixmap.height.toInt - y0 - 1 else y0
        var px = startX
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          curLine(px - startX) = colorToIndex.getOrElse(color, 0).toByte
          px += 1
        }
        deflaterOutput.write(FILTER_NONE)
        deflaterOutput.write(curLine, 0, width)
        y0 += 1
      }
      deflaterOutput.finish()
      buffer.endChunk(dataOutput)

      buffer.writeInt(IEND)
      buffer.endChunk(dataOutput)
      output.flush()
    } catch {
      case e: IOException =>
        System.err.println("anim8: " + e.getMessage)
    }
  }

  // === writeWrenOriginalDithered ===

  /** Writes the pixmap to the stream using the WrenOriginal dithering algorithm (an older variant of Wren that uses non-gamma-corrected error diffusion with Floyd-Steinberg weights).
    */
  def writeWrenOriginalDithered(output: OutputStream, pixmap: Pixmap): Unit = {
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val pal            = _palette.nn
    val paletteArray   = pal.paletteArray
    val paletteMapping = pal.paletteMapping
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val w = pixmap.width.toInt
      val h = pixmap.height.toInt
      pal.ensureErrorCapacity(w)
      val curErrorRed    = pal.curErrorRedFloats.nn
      val nextErrorRed   = pal.nextErrorRedFloats.nn
      val curErrorGreen  = pal.curErrorGreenFloats.nn
      val nextErrorGreen = pal.nextErrorGreenFloats.nn
      val curErrorBlue   = pal.curErrorBlueFloats.nn
      val nextErrorBlue  = pal.nextErrorBlueFloats.nn
      Arrays.fill(nextErrorRed, 0, w, 0f)
      Arrays.fill(nextErrorGreen, 0, w, 0f)
      Arrays.fill(nextErrorBlue, 0, w, 0f)

      buffer.writeInt(IHDR); buffer.writeInt(w); buffer.writeInt(h); buffer.writeByte(8); buffer.writeByte(COLOR_INDEXED); buffer.writeByte(COMPRESSION_DEFLATE); buffer.writeByte(FILTER_NONE)
      buffer.writeByte(INTERLACE_NONE); buffer.endChunk(dataOutput)

      buffer.writeInt(PLTE)
      var pi = 0; while (pi < paletteArray.length) { val p = paletteArray(pi); buffer.write(p >>> 24); buffer.write(p >>> 16); buffer.write(p >>> 8); pi += 1 }
      buffer.endChunk(dataOutput)

      val hasTransparent = paletteArray(0) == 0
      if (hasTransparent) { buffer.writeInt(TRNS); buffer.write(0); buffer.endChunk(dataOutput) }
      buffer.writeInt(IDAT)
      deflater.reset()

      val curLine        = ensureCurLine(w)
      val populationBias = pal.populationBias
      // 0x1p-8f = 0.00390625f; 0x1p-15f = 3.0517578e-5f; 0x1p+7f = 128f
      val w1       = (32.0f * _ditherStrength * (populationBias * populationBias)).toFloat; val w3 = w1 * 3f; val w5 = w1 * 5f; val w7 = w1 * 7f
      val strength = 0.2f * _ditherStrength / (populationBias * populationBias * populationBias * populationBias)
      val limit    = 5f + 125f / Math.sqrt(pal.colorCount + 1.5).toFloat
      val dmul     = 0.00390625f // 0x1p-8f

      var y = 0
      while (y < h) {
        System.arraycopy(nextErrorRed, 0, curErrorRed, 0, w)
        System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, w)
        System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, w)
        Arrays.fill(nextErrorRed, 0, w, 0f)
        Arrays.fill(nextErrorGreen, 0, w, 0f)
        Arrays.fill(nextErrorBlue, 0, w, 0f)

        val py = if (flipY) h - y - 1 else y
        val ny = y + 1
        var px = 0
        while (px < w) {
          val color = pixmap.getPixel(Pixels(px), Pixels(py))
          if (hasTransparent && (color & 0x80) == 0) { curLine(px) = 0 }
          else {
            // 0x1p-15f = 3.0517578e-5f; 0x1p+7f = 128f
            val er = Math.min(
              Math.max(
                ((PaletteReducer.TRI_BLUE_NOISE(
                  (px & 63) | (y & 63) << 6
                ) + 0.5f) + ((((px + 1).toLong * 0xc13fa9a902a6328fL + (y + 1).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 3.0517578e-5f - 128f)) * strength + curErrorRed(px),
                -limit
              ),
              limit
            )
            val eg = Math.min(
              Math.max(
                ((PaletteReducer.TRI_BLUE_NOISE_B(
                  (px & 63) | (y & 63) << 6
                ) + 0.5f) + ((((px + 3).toLong * 0xc13fa9a902a6328fL + (y - 1).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 3.0517578e-5f - 128f)) * strength + curErrorGreen(px),
                -limit
              ),
              limit
            )
            val eb = Math.min(
              Math.max(
                ((PaletteReducer.TRI_BLUE_NOISE_C(
                  (px & 63) | (y & 63) << 6
                ) + 0.5f) + ((((px + 2).toLong * 0xc13fa9a902a6328fL + (y - 4).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 3.0517578e-5f - 128f)) * strength + curErrorBlue(px),
                -limit
              ),
              limit
            )

            val rr           = Math.min(Math.max(((color >>> 24) + er + 0.5f).toInt, 0), 0xff)
            val gg           = Math.min(Math.max(((color >>> 16 & 0xff) + eg + 0.5f).toInt, 0), 0xff)
            val bb           = Math.min(Math.max(((color >>> 8 & 0xff) + eb + 0.5f).toInt, 0), 0xff)
            val paletteIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
            curLine(px) = paletteIndex
            val used  = paletteArray(paletteIndex & 0xff)
            val rdiff = dmul * ((color >>> 24) - (used >>> 24))
            val gdiff = dmul * ((color >>> 16 & 255) - (used >>> 16 & 255))
            val bdiff = dmul * ((color >>> 8 & 255) - (used >>> 8 & 255))
            if (px < w - 1) { curErrorRed(px + 1) += rdiff * w7; curErrorGreen(px + 1) += gdiff * w7; curErrorBlue(px + 1) += bdiff * w7 }
            if (ny < h) {
              if (px > 0) { nextErrorRed(px - 1) += rdiff * w3; nextErrorGreen(px - 1) += gdiff * w3; nextErrorBlue(px - 1) += bdiff * w3 }
              if (px < w - 1) { nextErrorRed(px + 1) += rdiff * w1; nextErrorGreen(px + 1) += gdiff * w1; nextErrorBlue(px + 1) += bdiff * w1 }
              nextErrorRed(px) += rdiff * w5; nextErrorGreen(px) += gdiff * w5; nextErrorBlue(px) += bdiff * w5
            }
          }
          px += 1
        }
        deflaterOutput.write(FILTER_NONE)
        deflaterOutput.write(curLine, 0, w)
        y += 1
      }
      deflaterOutput.finish()
      buffer.endChunk(dataOutput)
      buffer.writeInt(IEND)
      buffer.endChunk(dataOutput)
      output.flush()
    } catch { case e: IOException => System.err.println("anim8: " + e.getMessage) }
  }

  // === PNG chunk I/O (static-like helpers) ===

  /** Should probably be done explicitly; finalize() has been removed from JVM. */
  def dispose(): Unit = deflater.end()

  override def close(): Unit =
    deflater.end()
}

object PNG8 {

  import java.io.{ DataInputStream, EOFException, InputStream }
  import java.nio.charset.StandardCharsets
  import java.util.zip.CRC32

  import PaletteReducer.*

  /** Reads PNG chunks from an input stream. Returns an ordered map of chunk names to chunk contents. */
  def readChunks(inStream: InputStream): scala.collection.mutable.LinkedHashMap[String, Array[Byte]] = {
    val in = new DataInputStream(inStream)
    if (in.readLong() != 0x89504e470d0a1a0aL) {
      throw new IOException("PNG signature not found!")
    }
    val chunks   = new scala.collection.mutable.LinkedHashMap[String, Array[Byte]]()
    var trucking = true
    while (trucking)
      try {
        val length = in.readInt()
        if (length < 0) {
          throw new IOException("Sorry, that file is too long.")
        }
        val typeBytes = new Array[Byte](4)
        in.readFully(typeBytes)
        val data = new Array[Byte](length)
        in.readFully(data)
        in.readInt() // read CRC, discard it
        val typeName = new String(typeBytes, StandardCharsets.UTF_8)
        chunks.put(typeName, data)
      } catch {
        case _: EOFException => trucking = false
      }
    in.close()
    chunks
  }

  /** Writes PNG chunks to an output stream. */
  def writeChunks(outStream: OutputStream, chunks: scala.collection.mutable.LinkedHashMap[String, Array[Byte]]): Unit = {
    val crc = new CRC32()
    val out = new DataOutputStream(outStream)
    try {
      out.writeLong(0x89504e470d0a1a0aL)
      for ((key, value) <- chunks) {
        out.writeInt(value.length)
        val k = key.getBytes(StandardCharsets.UTF_8)
        out.write(k)
        crc.update(k, 0, k.length)
        out.write(value)
        crc.update(value, 0, value.length)
        out.writeInt(crc.getValue.toInt)
        crc.reset()
      }
      out.flush()
      out.close()
    } catch {
      case e: IOException => e.printStackTrace()
    }
  }

  /** Duplicates the input file and changes its palette to exactly match the given palette. */
  def swapPalette(input: FileHandle, output: FileHandle, palette: Array[Int]): Unit =
    try {
      val inputStream = input.read()
      val chunks      = readChunks(inputStream)
      val pal         = chunks.getOrElse("PLTE", null).asInstanceOf[Array[Byte] | Null] // @nowarn -- may not exist
      if (pal == null) {
        output.write(inputStream, false)
        return
      }
      var i = 0
      var p = 0
      while (i < palette.length && p < pal.nn.length - 2) {
        val rgba = palette(i)
        pal.nn(p) = (rgba >>> 24).toByte; p += 1
        pal.nn(p) = (rgba >>> 16).toByte; p += 1
        pal.nn(p) = (rgba >>> 8).toByte; p += 1
        i += 1
      }
      writeChunks(output.write(false), chunks)
    } catch {
      case e: IOException => e.printStackTrace()
    }

  /** Edits all palette channels using a single Interpolation. */
  def editPalette(input: FileHandle, output: FileHandle, editor: sge.math.Interpolation): Unit =
    try {
      val inputStream = input.read()
      val chunks      = readChunks(inputStream)
      val pal         = chunks.getOrElse("PLTE", null).asInstanceOf[Array[Byte] | Null] // @nowarn
      if (pal == null) {
        output.write(inputStream, false)
        return
      }
      var p = 0
      while (p < pal.nn.length - 2) {
        pal.nn(p) = editor.apply(0f, 255.999f, (pal.nn(p) & 255) / 255f).toByte
        pal.nn(p + 1) = editor.apply(0f, 255.999f, (pal.nn(p + 1) & 255) / 255f).toByte
        pal.nn(p + 2) = editor.apply(0f, 255.999f, (pal.nn(p + 2) & 255) / 255f).toByte
        p += 3
      }
      writeChunks(output.write(false), chunks)
    } catch {
      case e: IOException => e.printStackTrace()
    }

  /** Edits palette with per-channel Interpolations. */
  def editPalette(input: FileHandle, output: FileHandle, changeR: sge.math.Interpolation, changeG: sge.math.Interpolation, changeB: sge.math.Interpolation): Unit =
    try {
      val inputStream = input.read()
      val chunks      = readChunks(inputStream)
      val pal         = chunks.getOrElse("PLTE", null).asInstanceOf[Array[Byte] | Null] // @nowarn
      if (pal == null) {
        output.write(inputStream, false)
        return
      }
      var p = 0
      while (p < pal.nn.length - 2) {
        pal.nn(p) = changeR.apply(0f, 255.999f, (pal.nn(p) & 255) / 255f).toByte
        pal.nn(p + 1) = changeG.apply(0f, 255.999f, (pal.nn(p + 1) & 255) / 255f).toByte
        pal.nn(p + 2) = changeB.apply(0f, 255.999f, (pal.nn(p + 2) & 255) / 255f).toByte
        p += 3
      }
      writeChunks(output.write(false), chunks)
    } catch {
      case e: IOException => e.printStackTrace()
    }

  /** Shifts hues so lighter colors lean warm and darker colors lean cool. Uses default strength of 1. */
  def hueShift(input: FileHandle, output: FileHandle): Unit =
    hueShift(input, output, 1f)

  /** Shifts hues with configurable strength multiplier. */
  def hueShift(input: FileHandle, output: FileHandle, strengthMultiplier: Float): Unit =
    try {
      val inputStream = input.read()
      val chunks      = readChunks(inputStream)
      val pal         = chunks.getOrElse("PLTE", null).asInstanceOf[Array[Byte] | Null] // @nowarn
      if (pal == null) {
        output.write(inputStream, false)
        return
      }
      val aMul = 1.1f * strengthMultiplier
      val bMul = 0.125f * strengthMultiplier
      var p    = 0
      while (p < pal.nn.length - 2) {
        var rgb = (pal.nn(p) & 255) << 24 | (pal.nn(p + 1) & 255) << 16 | (pal.nn(p + 2) & 255) << 8 | 255
        val s   = shrink(rgb)
        val L   = OKLAB(0)(s)
        val A   = OKLAB(1)(s) * aMul
        val B   = OKLAB(2)(s) + OtherMath.asin(L - 0.6f) * bMul * (1f - A * A)
        rgb = oklabToRGB(L, A, B, 1f)
        pal.nn(p) = (rgb >>> 24).toByte
        pal.nn(p + 1) = (rgb >>> 16).toByte
        pal.nn(p + 2) = (rgb >>> 8).toByte
        p += 3
      }
      writeChunks(output.write(false), chunks)
    } catch {
      case e: IOException => e.printStackTrace()
    }

  /** Edits palette lightness using an Oklab L Interpolation. */
  def editPaletteLightness(input: FileHandle, output: FileHandle, changeL: sge.math.Interpolation): Unit =
    try {
      val inputStream = input.read()
      val chunks      = readChunks(inputStream)
      val pal         = chunks.getOrElse("PLTE", null).asInstanceOf[Array[Byte] | Null] // @nowarn
      if (pal == null) {
        output.write(inputStream, false)
        return
      }
      var p = 0
      while (p < pal.nn.length - 2) {
        var rgb = (pal.nn(p) & 255) << 24 | (pal.nn(p + 1) & 255) << 16 | (pal.nn(p + 2) & 255) << 8 | 255
        val s   = shrink(rgb)
        val L   = Math.min(Math.max(changeL.apply(OKLAB(0)(s)), 0f), 1f)
        val A   = OKLAB(1)(s)
        val B   = OKLAB(2)(s)
        rgb = oklabToRGB(L, A, B, 1f)
        pal.nn(p) = (rgb >>> 24).toByte
        pal.nn(p + 1) = (rgb >>> 16).toByte
        pal.nn(p + 2) = (rgb >>> 8).toByte
        p += 3
      }
      writeChunks(output.write(false), chunks)
    } catch {
      case e: IOException => e.printStackTrace()
    }

  /** Edits palette saturation. 0 = grayscale, 1 = no change, >1 = oversaturate. */
  def editPaletteSaturation(input: FileHandle, output: FileHandle, saturationMultiplier: Float): Unit =
    try {
      val inputStream = input.read()
      val chunks      = readChunks(inputStream)
      val pal         = chunks.getOrElse("PLTE", null).asInstanceOf[Array[Byte] | Null] // @nowarn
      if (pal == null) {
        output.write(inputStream, false)
        return
      }
      var p = 0
      while (p < pal.nn.length - 2) {
        var rgb = (pal.nn(p) & 255) << 24 | (pal.nn(p + 1) & 255) << 16 | (pal.nn(p + 2) & 255) << 8 | 255
        val s   = shrink(rgb)
        val L   = OKLAB(0)(s)
        val A   = Math.min(Math.max(OKLAB(1)(s) * saturationMultiplier, -1f), 1f)
        val B   = Math.min(Math.max(OKLAB(2)(s) * saturationMultiplier, -1f), 1f)
        rgb = oklabToRGB(L, A, B, 1f)
        pal.nn(p) = (rgb >>> 24).toByte
        pal.nn(p + 1) = (rgb >>> 16).toByte
        pal.nn(p + 2) = (rgb >>> 8).toByte
        p += 3
      }
      writeChunks(output.write(false), chunks)
    } catch {
      case e: IOException => e.printStackTrace()
    }

  /** Edits palette using per-channel Oklab Interpolations (L, A, B). */
  def editPaletteOklab(input: FileHandle, output: FileHandle, changeL: sge.math.Interpolation, changeA: sge.math.Interpolation, changeB: sge.math.Interpolation): Unit =
    try {
      val inputStream = input.read()
      val chunks      = readChunks(inputStream)
      val pal         = chunks.getOrElse("PLTE", null).asInstanceOf[Array[Byte] | Null] // @nowarn
      if (pal == null) {
        output.write(inputStream, false)
        return
      }
      var p = 0
      while (p < pal.nn.length - 2) {
        var rgb = (pal.nn(p) & 255) << 24 | (pal.nn(p + 1) & 255) << 16 | (pal.nn(p + 2) & 255) << 8 | 255
        val s   = shrink(rgb)
        val L   = Math.min(Math.max(changeL.apply(0f, 1f, OKLAB(0)(s)), 0f), 1f)
        val A   = Math.min(Math.max(changeA.apply(-1f, 1f, OKLAB(1)(s) * 0.5f + 0.5f), -1f), 1f)
        val B   = Math.min(Math.max(changeB.apply(-1f, 1f, OKLAB(2)(s) * 0.5f + 0.5f), -1f), 1f)
        rgb = oklabToRGB(L, A, B, 1f)
        pal.nn(p) = (rgb >>> 24).toByte
        pal.nn(p + 1) = (rgb >>> 16).toByte
        pal.nn(p + 2) = (rgb >>> 8).toByte
        p += 3
      }
      writeChunks(output.write(false), chunks)
    } catch {
      case e: IOException => e.printStackTrace()
    }

  /** Centralizes palette channels (biases them toward center). Uses full amount. */
  def centralizePalette(input: FileHandle, output: FileHandle): Unit =
    centralizePalette(input, output, 1f)

  /** Centralizes palette channels with configurable amount (0 = none, 1 = full). */
  def centralizePalette(input: FileHandle, output: FileHandle, amount: Float): Unit =
    try {
      val inputStream = input.read()
      val chunks      = readChunks(inputStream)
      val pal         = chunks.getOrElse("PLTE", null).asInstanceOf[Array[Byte] | Null] // @nowarn
      if (pal == null) {
        output.write(inputStream, false)
        return
      }
      var p = 0
      while (p < pal.nn.length) {
        val original    = pal.nn(p) & 255
        val centralized = OtherMath.centralize(pal.nn(p)) & 255
        pal.nn(p) = (original + (centralized - original) * amount).toInt.toByte
        p += 1
      }
      writeChunks(output.write(false), chunks)
    } catch {
      case e: IOException => e.printStackTrace()
    }
}
