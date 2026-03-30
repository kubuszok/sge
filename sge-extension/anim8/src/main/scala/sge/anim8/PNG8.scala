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

import sge.files.FileHandle
import sge.graphics.Pixmap
import sge.utils.StreamUtils

import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import scala.collection.mutable

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

  /** Uses the current [[ditherAlgorithm]] to select which writing method to use. For now, all algorithms delegate to writeSolid as a baseline. Individual dither methods will be added as needed.
    */
  def writeDithered(output: OutputStream, pixmap: Pixmap): Unit =
    // TODO: Implement individual dithered write methods
    writeSolid(output, pixmap)

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

  def writePrecisely(output: OutputStream, pixmap: Pixmap, exactPalette: Array[Int] | Null, ditherFallback: Boolean, threshold: Int): Unit = {
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
        return // early exit already handled @nowarn
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

      if (hasTransparent == 1) {
        buffer.writeInt(TRNS)
        buffer.write(0)
        buffer.endChunk(dataOutput)
      }
      buffer.writeInt(IDAT)
      deflater.reset()

      val lineLen = w
      if (curLineBytes == null || curLineBytes.nn.length < lineLen) {
        curLineBytes = new Array[Byte](lineLen)
      }
      val curLine = curLineBytes.nn

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
        deflaterOutput.write(curLine, 0, lineLen)
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
      buffer.writeInt(IDAT)
      deflater.reset()

      val lineLen = w
      if (curLineBytes == null || curLineBytes.nn.length < lineLen) {
        curLineBytes = new Array[Byte](lineLen)
      }
      val curLine = curLineBytes.nn

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
        deflaterOutput.write(curLine, 0, lineLen)
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

  /** Writes the given frames as an animated PNG-8 at `fps` frames per second. */
  override def write(output: OutputStream, frames: Array[Pixmap], fps: Int): Unit =
    // For animation, delegate to solid write (APNG format)
    writeSolid(output, frames, fps)

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

      val hasTransparent = paletteArray(0) == 0
      if (hasTransparent) {
        buffer.writeInt(TRNS)
        buffer.write(0)
        buffer.endChunk(dataOutput)
      }

      buffer.writeInt(acTL)
      buffer.writeInt(frames.length)
      buffer.writeInt(0)
      buffer.endChunk(dataOutput)

      if (curLineBytes == null || curLineBytes.nn.length < width) {
        curLineBytes = new Array[Byte](width)
      }
      val curLine = curLineBytes.nn

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

  override def close(): Unit =
    deflater.end()
}
