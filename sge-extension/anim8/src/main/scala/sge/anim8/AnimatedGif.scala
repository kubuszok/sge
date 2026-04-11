/*
 * Ported from anim8-gdx - https://github.com/tommyettinger/anim8-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * GIF encoder using standard LZW compression; can write animated and non-animated GIF images.
 * Based on Nick Badal's Android port of Alessandro La Rossa's J2ME port of Kevin Weiner's pure Java
 * animated GIF encoder. The original has no copyright asserted.
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

import java.io.IOException
import java.io.OutputStream
import java.util.Arrays

import PaletteReducer.*

/** GIF encoder using standard LZW compression; can write animated and non-animated GIF images. An instance can be reused to encode multiple GIFs with minimal allocation.
  *
  * You can configure the target palette and how this can dither colors via the [[palette]] field, which is a [[PaletteReducer]] object that defaults to null and can be reused.
  */
class AnimatedGif extends AnimationWriter with Dithered {

  /** Writes the given Pixmap values in `frames`, in order, to an animated GIF at `file`. Always writes at 30 frames per second.
    */
  override def write(file: FileHandle, frames: Array[Pixmap]): Unit =
    write(file, frames, 30)

  /** Writes the given Pixmap values in `frames`, in order, to an animated GIF at `file`. The resulting GIF will play back at `fps` frames per second.
    */
  override def write(file: FileHandle, frames: Array[Pixmap], fps: Int): Unit = {
    val output = file.write(false)
    try
      write(output, frames, fps)
    finally
      StreamUtils.closeQuietly(output)
  }

  /** Writes the given Pixmap values in `frames`, in order, to an animated GIF in the OutputStream `output`. The resulting GIF will play back at `fps` frames per second.
    */
  override def write(output: OutputStream, frames: Array[Pixmap], fps: Int): Unit = boundary {
    if (frames == null || frames.isEmpty) {
      // noinspection: null check needed for Java interop @nowarn
      break(())
    }
    _clearPalette = _palette == null
    if (_clearPalette) {
      if (fastAnalysis && frames.length > 1) {
        _palette = new PaletteReducer()
        _palette.nn.analyzeFast(frames(0), 300, 256)
      } else {
        _palette = new PaletteReducer(frames(0))
      }
    }
    if (!start(output)) {
      break(())
    }
    setFrameRate(fps.toFloat)
    var i = 0
    while (i < frames.length) {
      addFrame(frames(i))
      i += 1
    }
    finish()
    if (_clearPalette) {
      _palette = null
    }
  }

  private var _ditherAlgorithm: DitherAlgorithm = DitherAlgorithm.WREN

  protected var width:         Int                 = 0 // image size
  protected var height:        Int                 = 0
  protected var x:             Int                 = 0
  protected var y:             Int                 = 0
  protected var flipY:         Boolean             = true
  protected var transIndex:    Int                 = -1 // transparent index in color table
  protected var repeat:        Int                 = 0 // loop repeat
  protected var delay:         Int                 = 16 // frame delay (thousandths)
  protected var started:       Boolean             = false // ready to output frames
  protected var out:           OutputStream | Null = null
  protected var image:         Pixmap | Null       = null // current frame
  protected var indexedPixels: Array[Byte] | Null  = null // converted frame indexed to palette
  protected var colorDepth:    Int                 = 0 // number of bit planes
  protected var colorTab:      Array[Byte] | Null  = null // RGB palette, 3 bytes per color
  protected var usedEntry:     Array[Boolean]      = new Array[Boolean](256) // active palette entries
  protected var palSize:       Int                 = 7 // color table size (bits-1)
  protected var dispose:       Int                 = -1 // disposal code (-1 = use default)
  protected var closeStream:   Boolean             = false // close stream when finished
  protected var firstFrame:    Boolean             = true
  protected var sizeSet:       Boolean             = false // if false, get size from first frame
  protected var seq:           Int                 = 0
  protected var _clearPalette: Boolean             = false

  /** If true (the default) and [[palette]] is null, this uses a lower-quality but much-faster algorithm to analyze the color palette in each frame.
    */
  var fastAnalysis: Boolean = true

  /** Often assigned as a field, the palette can be null (which means this may analyze each frame for its palette, based on the setting for [[fastAnalysis]]), or can be an existing PaletteReducer.
    */
  private var _palette: PaletteReducer | Null = null

  /** Overrides the palette's dither strength; see [[ditherStrength]]. */
  protected var _ditherStrength: Float = 1f

  /** Gets this AnimatedGif's dither strength. */
  def ditherStrength: Float = _ditherStrength

  /** Sets this AnimatedGif's dither strength. */
  def ditherStrength_=(ditherStrength: Float): Unit =
    _ditherStrength = Math.max(0f, ditherStrength)

  override def palette:                                   PaletteReducer | Null = _palette
  override def palette_=(palette: PaletteReducer | Null): Unit                  = _palette = palette

  /** Sets the delay time between each frame, or changes it for subsequent frames (applies to last frame added).
    * @param ms
    *   int delay time in milliseconds
    */
  def setDelay(ms: Int): Unit = delay = ms

  /** Sets the GIF frame disposal code for the last added frame and any subsequent frames. Default is 0 if no transparent color has been set, otherwise 2.
    * @param code
    *   int disposal code.
    */
  def setDispose(code: Int): Unit = if (code >= 0) dispose = code

  /** Sets the number of times the set of GIF frames should be played. Default is 1; 0 means play indefinitely. Must be invoked before the first image is added.
    * @param iter
    *   int number of iterations.
    */
  def setRepeat(iter: Int): Unit = if (iter >= 0) repeat = iter

  /** Returns true if the output is flipped top-to-bottom from the inputs (the default). */
  def isFlipY: Boolean = flipY

  /** Sets whether this should flip inputs top-to-bottom (true, the default setting), or leave as-is (false). */
  def setFlipY(flipY: Boolean): Unit = this.flipY = flipY

  override def ditherAlgorithm:                                     DitherAlgorithm = _ditherAlgorithm
  override def ditherAlgorithm_=(ditherAlgorithm: DitherAlgorithm): Unit            =
    if (ditherAlgorithm != null) { // null guard for Java interop @nowarn
      _ditherAlgorithm = ditherAlgorithm
    }

  /** Adds next GIF frame. The frame is not written immediately, but is actually deferred until the next frame is received so that timing data can be inserted.
    * @param im
    *   Pixmap containing frame to write.
    * @return
    *   true if successful.
    */
  def addFrame(im: Pixmap): Boolean =
    if ((im == null) || !started) { // null check for Java interop @nowarn
      false
    } else {
      var ok = true
      try {
        if (!sizeSet) {
          setSize(im.width.toInt, im.height.toInt)
        }
        seq += 1
        image = im
        getImagePixels()
        analyzePixels()
        if (firstFrame) {
          writeLSD()
          writePalette()
          if (repeat >= 0) {
            writeNetscapeExt()
          }
        }
        writeGraphicCtrlExt()
        writeImageDesc()
        if (!firstFrame) {
          writePalette()
        }
        writePixels()
        firstFrame = false
      } catch {
        case _: IOException => ok = false
      }
      ok
    }

  /** Flushes any pending data and closes output file. If writing to an OutputStream, the stream is not closed. */
  def finish(): Boolean =
    if (!started) {
      false
    } else {
      var ok = true
      started = false
      try {
        out.nn.write(0x3b) // gif trailer
        out.nn.flush()
        if (closeStream) {
          out.nn.close()
        }
      } catch {
        case _: IOException => ok = false
      }
      // reset for subsequent use
      transIndex = -1
      out = null
      image = null
      indexedPixels = null
      colorTab = null
      closeStream = false
      sizeSet = false
      firstFrame = true
      seq = 0
      ok
    }

  /** Sets frame rate in frames per second. Equivalent to `setDelay(1000.0f / fps)`. */
  def setFrameRate(fps: Float): Unit =
    if (fps != 0f) {
      delay = (1000f / fps).toInt
    }

  /** Sets the GIF frame size. The default size is the size of the first frame added if this method is not invoked. */
  def setSize(w: Int, h: Int): Unit = {
    width = if (w < 1) 320 else w
    height = if (h < 1) 240 else h
    sizeSet = true
  }

  /** Sets the GIF frame position. The position is 0,0 by default. */
  def setPosition(x: Int, y: Int): Unit = {
    this.x = x
    this.y = y
  }

  /** Initiates GIF file creation on the given stream. The stream is not closed automatically. */
  def start(os: OutputStream): Boolean =
    if (os == null) { // null check for Java interop @nowarn
      false
    } else {
      var ok = true
      closeStream = false
      out = os
      try
        writeString("GIF89a") // header
      catch {
        case _: IOException => ok = false
      }
      started = ok
      ok
    }

  // === Dither analysis methods ===

  protected def analyzeNone(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping
    val hasTransparent = paletteArray(0) == 0

    var y0 = 0
    var i  = 0
    while (y0 < height && i < nPix) {
      var px = 0
      while (px < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(px), Pixels(flipped + flipDir * y0))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          val idx = paletteMapping(
            (color >>> 17 & 0x7c00)
              | (color >>> 14 & 0x3e0)
              | (color >>> 11 & 0x1f)
          )
          usedEntry(idx & 255) = true
          indexedPixels.nn(i) = idx
          i += 1
        }
        px += 1
      }
      y0 += 1
    }
  }

  protected def analyzePattern(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val pal            = _palette.nn
    val paletteArray   = pal.paletteArray
    val paletteMapping = pal.paletteMapping
    val hasTransparent = paletteArray(0) == 0

    val errorMul = _ditherStrength * 0.5f / pal.populationBias
    var y0       = 0
    var i        = 0
    while (y0 < height && i < nPix) {
      var px = 0
      while (px < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(px), Pixels(flipped + flipDir * y0))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          var er = 0
          var eg = 0
          var eb = 0
          val cr = color >>> 24
          val cg = color >>> 16 & 0xff
          val cb = color >>> 8 & 0xff
          var c  = 0
          while (c < 16) {
            val rr        = Math.min(Math.max((cr + er * errorMul).toInt, 0), 255)
            val gg        = Math.min(Math.max((cg + eg * errorMul).toInt, 0), 255)
            val bb        = Math.min(Math.max((cb + eb * errorMul).toInt, 0), 255)
            val usedIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff
            pal.candidates(c) = usedIndex
            val used = paletteArray(usedIndex)
            pal.candidates(c | 16) = shrink(used)
            er += cr - (used >>> 24)
            eg += cg - (used >>> 16 & 0xff)
            eb += cb - (used >>> 8 & 0xff)
            c += 1
          }
          PaletteReducer.sort16(pal.candidates)
          val chosen = pal.candidates(thresholdMatrix16((px & 3) | (y0 & 3) << 2)).toByte
          usedEntry(chosen & 255) = true
          indexedPixels.nn(i) = chosen
          i += 1
        }
        px += 1
      }
      y0 += 1
    }
  }

  protected def analyzeGradient(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping
    val hasTransparent = paletteArray(0) == 0

    val populationBias = _palette.nn.populationBias
    val strength       = 0.9f * Math.tanh(0.16f * _ditherStrength * Math.pow(populationBias, -7.00)).toFloat
    var y0             = 0
    var i              = 0
    while (y0 < height && i < nPix) {
      var px = 0
      while (px < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(px), Pixels(flipped + flipDir * y0))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          val rr  = fromLinearLUT((toLinearLUT(color >>> 24) + ((142 * (px + 0x5f) + 79 * (y0 - 0x96) & 255) - 127.5f) * strength).toInt) & 255
          val gg  = fromLinearLUT((toLinearLUT(color >>> 16 & 0xff) + ((142 * (px + 0xfa) + 79 * (y0 - 0xa3) & 255) - 127.5f) * strength).toInt) & 255
          val bb  = fromLinearLUT((toLinearLUT(color >>> 8 & 0xff) + ((142 * (px + 0xa5) + 79 * (y0 - 0xc9) & 255) - 127.5f) * strength).toInt) & 255
          val idx = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          usedEntry(idx & 255) = true
          indexedPixels.nn(i) = idx
          i += 1
        }
        px += 1
      }
      y0 += 1
    }
  }

  protected def analyzeChaotic(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val pal            = _palette.nn
    val paletteArray   = pal.paletteArray
    val paletteMapping = pal.paletteMapping
    val hasTransparent = paletteArray(0) == 0

    val strength = _ditherStrength * pal.populationBias * 1.5
    var s        = 0xc13fa9a902a6328fL * seq
    var y0       = 0
    var i        = 0
    while (y0 < height && i < nPix) {
      var px = 0
      while (px < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(px), Pixels(flipped + flipDir * y0))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          var rr   = color >>> 24
          var gg   = (color >>> 16) & 0xff
          var bb   = (color >>> 8) & 0xff
          val used = paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff)
          var adj  = ((TRI_BLUE_NOISE((px & 63) | (y0 & 63) << 6) + 0.5f) * 0.007843138f).toDouble
          adj *= adj * adj
          // Complicated... This starts with a checkerboard of -0.5 and 0.5, times a tiny fraction.
          // The next 3 lines generate 3 low-quality-random numbers based on s, which should be
          //   different as long as the colors encountered so far were different. The numbers can
          //   each be positive or negative, and are reduced to a manageable size, summed, and
          //   multiplied by the earlier tiny fraction. Summing 3 random values gives us a curved
          //   distribution, centered on about 0.0 and weighted so most results are close to 0.
          //   Two of the random numbers use an XLCG, and the last uses an LCG.
          adj += ((px + y0 & 1) - 0.5f) * 2.6645352591003757e-15 * strength *
            (((s ^ 0x9e3779b97f4a7c15L) * 0xc6bc279692b5cc83L >> 15) +
              ((~s ^ 0xdb4f0b9175ae2165L) * 0xd1b54a32d192ed03L >> 15) +
              ({ s = (s ^ rr + gg + bb) * 0xd1342543de82ef95L + 0x91e10da5c79e7b1dL; s } >> 15))
          rr = Math.min(Math.max((rr + (adj * (rr - (used >>> 24)))).toInt, 0), 0xff)
          gg = Math.min(Math.max((gg + (adj * (gg - (used >>> 16 & 0xff)))).toInt, 0), 0xff)
          bb = Math.min(Math.max((bb + (adj * (bb - (used >>> 8 & 0xff)))).toInt, 0), 0xff)
          val idx = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          usedEntry(idx & 255) = true
          indexedPixels.nn(i) = idx
          i += 1
        }
        px += 1
      }
      y0 += 1
    }
  }

  protected def analyzeAdditive(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping
    val hasTransparent = paletteArray(0) == 0

    val populationBias = _palette.nn.populationBias
    val s              = 0.08f * _ditherStrength / Math.pow(populationBias, 8f).toFloat
    val strength       = s / (0.35f + s)
    var y0             = 0
    var i              = 0
    while (y0 < height && i < nPix) {
      var px = 0
      while (px < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(px), Pixels(flipped + flipDir * y0))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          val rr  = fromLinearLUT((toLinearLUT(color >>> 24) + ((119 * px + 180 * y0 + 54 & 255) - 127.5f) * strength).toInt) & 255
          val gg  = fromLinearLUT((toLinearLUT(color >>> 16 & 0xff) + ((119 * px + 180 * y0 + 81 & 255) - 127.5f) * strength).toInt) & 255
          val bb  = fromLinearLUT((toLinearLUT(color >>> 8 & 0xff) + ((119 * px + 180 * y0 & 255) - 127.5f) * strength).toInt) & 255
          val idx = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          usedEntry(idx & 255) = true
          indexedPixels.nn(i) = idx
          i += 1
        }
        px += 1
      }
      y0 += 1
    }
  }

  protected def analyzeRoberts(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping
    val hasTransparent = paletteArray(0) == 0

    val populationBias = _palette.nn.populationBias
    val str            = Math.min(48 * _ditherStrength / (populationBias * populationBias * populationBias * populationBias), 127f)
    var y0             = 0
    var i              = 0
    while (y0 < height && i < nPix) {
      var px = 0
      while (px < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(px), Pixels(flipped + flipDir * y0))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          // We get a sub-random value from 0-1 using the R2 sequence.
          // Offsetting this value by different values and feeding into triangleWave()
          // gives 3 different values for r, g, and b, without much bias toward high or low values.
          // There is correlation between r, g, and b in certain patterns.
          val theta = (px * 0xc13fa9a9 + y0 * 0x91e10da5 >>> 9) * 1.1920929e-7f
          val rr    = fromLinearLUT((toLinearLUT(color >>> 24) + OtherMath.triangleWave(theta) * str).toInt) & 255
          val gg    = fromLinearLUT((toLinearLUT(color >>> 16 & 0xff) + OtherMath.triangleWave(theta + 0.209f) * str).toInt) & 255
          val bb    = fromLinearLUT((toLinearLUT(color >>> 8 & 0xff) + OtherMath.triangleWave(theta + 0.518f) * str).toInt) & 255
          val idx   = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          usedEntry(idx & 255) = true
          indexedPixels.nn(i) = idx
          i += 1
        }
        px += 1
      }
      y0 += 1
    }
  }

  protected def analyzeLoaf(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping
    val hasTransparent = paletteArray(0) == 0

    val strength = Math.min(Math.max(2.5f + 5f * _ditherStrength - 5.5f * _palette.nn.populationBias, 0f), 7.9f)
    var y0       = 0
    var i        = 0
    while (y0 < height && i < nPix) {
      var px = 0
      while (px < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(px), Pixels(flipped + flipDir * y0))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          val adj = ((((px + y0 & 1) << 5) - 16) * strength).toInt // either + 16 * strength or - 16 * strength
          val rr  = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 24) + adj, 0f), 1023f).toInt) & 255
          val gg  = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 16 & 0xff) + adj, 0f), 1023f).toInt) & 255
          val bb  = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 8 & 0xff) + adj, 0f), 1023f).toInt) & 255
          val idx = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          usedEntry(idx & 255) = true
          indexedPixels.nn(i) = idx
          i += 1
        }
        px += 1
      }
      y0 += 1
    }
  }

  protected def analyzeGourd(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping
    val hasTransparent = paletteArray(0) == 0

    val strength = (_ditherStrength * 0.7 * Math.pow(_palette.nn.populationBias, -5.50)).toFloat
    var gi       = 0
    while (gi < 64) {
      tempThresholdMatrix(gi) = Math.min(Math.max((thresholdMatrix64(gi) - 31.5f) * strength, -127f), 127f)
      gi += 1
    }
    var oy = 0
    var i  = 0
    while (oy < height && i < nPix) {
      val y = flipped + flipDir * oy
      var x = 0
      while (x < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(x), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          val adj = tempThresholdMatrix((x & 7) | (oy & 7) << 3)
          val rr  = fromLinearLUT((toLinearLUT(color >>> 24) + adj).toInt) & 255
          val gg  = fromLinearLUT((toLinearLUT(color >>> 16 & 0xff) + adj).toInt) & 255
          val bb  = fromLinearLUT((toLinearLUT(color >>> 8 & 0xff) + adj).toInt) & 255
          val idx = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          usedEntry(idx & 255) = true
          indexedPixels.nn(i) = idx
          i += 1
        }
        x += 1
      }
      oy += 1
    }
  }

  protected def analyzeDiffusion(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val pal            = _palette.nn
    val paletteArray   = pal.paletteArray
    val paletteMapping = pal.paletteMapping
    val hasTransparent = paletteArray(0) == 0

    val w = width
    // 0x1p-8f = 0.00390625f
    val w1 = _ditherStrength * 32 / pal.populationBias
    val w3 = w1 * 3f
    val w5 = w1 * 5f
    val w7 = w1 * 7f

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

    var y0 = 0
    var i  = 0
    while (y0 < height && i < nPix) {
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, w)
      System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, w)
      System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, w)
      Arrays.fill(nextErrorRed, 0, w, 0f)
      Arrays.fill(nextErrorGreen, 0, w, 0f)
      Arrays.fill(nextErrorBlue, 0, w, 0f)

      val py = flipped + flipDir * y0
      val ny = y0 + 1
      var px = 0
      while (px < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(px), Pixels(py))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          val rr = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 24) + curErrorRed(px), 0f), 1023f).toInt) & 255
          val gg = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 16 & 0xff) + curErrorGreen(px), 0f), 1023f).toInt) & 255
          val bb = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 8 & 0xff) + curErrorBlue(px), 0f), 1023f).toInt) & 255

          val paletteIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          usedEntry(paletteIndex & 255) = true
          indexedPixels.nn(i) = paletteIndex
          val used  = paletteArray(paletteIndex & 0xff)
          val rdiff = Math.min(Math.max(0.00390625f * ((color >>> 24) - (used >>> 24)), -1f), 1f)
          val gdiff = Math.min(Math.max(0.00390625f * ((color >>> 16 & 255) - (used >>> 16 & 255)), -1f), 1f)
          val bdiff = Math.min(Math.max(0.00390625f * ((color >>> 8 & 255) - (used >>> 8 & 255)), -1f), 1f)

          if (px < w - 1) {
            curErrorRed(px + 1) += rdiff * w7
            curErrorGreen(px + 1) += gdiff * w7
            curErrorBlue(px + 1) += bdiff * w7
          }
          if (ny < height) {
            if (px > 0) {
              nextErrorRed(px - 1) += rdiff * w3
              nextErrorGreen(px - 1) += gdiff * w3
              nextErrorBlue(px - 1) += bdiff * w3
            }
            if (px < w - 1) {
              nextErrorRed(px + 1) += rdiff * w1
              nextErrorGreen(px + 1) += gdiff * w1
              nextErrorBlue(px + 1) += bdiff * w1
            }
            nextErrorRed(px) += rdiff * w5
            nextErrorGreen(px) += gdiff * w5
            nextErrorBlue(px) += bdiff * w5
          }
          i += 1
        }
        px += 1
      }
      y0 += 1
    }
  }

  protected def analyzeBlue(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping
    val hasTransparent = paletteArray(0) == 0

    val strength = 0.21875f * _ditherStrength / (_palette.nn.populationBias * _palette.nn.populationBias)
    var y0       = 0
    var i        = 0
    while (y0 < height && i < nPix) {
      val ny = flipped + flipDir * y0
      var x  = 0
      while (x < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(x), Pixels(ny))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          val adj = Math.min(Math.max((TRI_BLUE_NOISE((x & 63) | (ny & 63) << 6) + ((x + ny & 1) << 8) - 127.5f) * strength, -100.5f), 101.5f)
          val rr  = fromLinearLUT((toLinearLUT(color >>> 24) + adj).toInt) & 255
          val gg  = fromLinearLUT((toLinearLUT(color >>> 16 & 0xff) + adj).toInt) & 255
          val bb  = fromLinearLUT((toLinearLUT(color >>> 8 & 0xff) + adj).toInt) & 255
          val idx = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          usedEntry(idx & 255) = true
          indexedPixels.nn(i) = idx
          i += 1
        }
        x += 1
      }
      y0 += 1
    }
  }

  protected def analyzeBlunt(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping
    val hasTransparent = paletteArray(0) == 0

    val populationBias = _palette.nn.populationBias
    val strength       = Math.min(Math.max(0.35f * _ditherStrength / (populationBias * populationBias * populationBias), -0.6f), 0.6f)
    var y0             = 0
    var i              = 0
    while (y0 < height && i < nPix) {
      val ny = flipped + flipDir * y0
      var x  = 0
      while (x < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(x), Pixels(ny))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          val adj = (x + y0 << 7 & 128) - 63.5f
          val rr  = fromLinearLUT((toLinearLUT(color >>> 24) + (TRI_BLUE_NOISE((x + 62 & 63) << 6 | (y0 + 66 & 63)) + adj) * strength).toInt) & 255
          val gg  = fromLinearLUT((toLinearLUT(color >>> 16 & 0xff) + (TRI_BLUE_NOISE_B((x + 31 & 63) << 6 | (y0 + 113 & 63)) + adj) * strength).toInt) & 255
          val bb  = fromLinearLUT((toLinearLUT(color >>> 8 & 0xff) + (TRI_BLUE_NOISE_C((x + 71 & 63) << 6 | (y0 + 41 & 63)) + adj) * strength).toInt) & 255
          val idx = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          usedEntry(idx & 255) = true
          indexedPixels.nn(i) = idx
          i += 1
        }
        x += 1
      }
      y0 += 1
    }
  }

  protected def analyzeBanter(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping
    val hasTransparent = paletteArray(0) == 0

    val strength = Math.min(Math.max(0.17f * _ditherStrength * Math.pow(_palette.nn.populationBias, -10f).toFloat, -0.95f), 0.95f)
    var y0       = 0
    var i        = 0
    while (y0 < height && i < nPix) {
      val ny = flipped + flipDir * y0
      var x  = 0
      while (x < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(x), Pixels(ny))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          val adj = TRI_BAYER_MATRIX_128((x & TBM_MASK) << TBM_BITS | (y0 & TBM_MASK)) * strength
          val rr  = fromLinearLUT((toLinearLUT(color >>> 24) + adj).toInt) & 255
          val gg  = fromLinearLUT((toLinearLUT(color >>> 16 & 0xff) + adj).toInt) & 255
          val bb  = fromLinearLUT((toLinearLUT(color >>> 8 & 0xff) + adj).toInt) & 255
          val idx = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          usedEntry(idx & 255) = true
          indexedPixels.nn(i) = idx
          i += 1
        }
        x += 1
      }
      y0 += 1
    }
  }

  protected def analyzeScatter(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val pal            = _palette.nn
    val paletteArray   = pal.paletteArray
    val paletteMapping = pal.paletteMapping
    val hasTransparent = paletteArray(0) == 0

    val w = width
    // 0x2.1p-8f = 0.00805664062f
    val w1 = Math.min(_ditherStrength * 5.5f / (pal.populationBias * pal.populationBias), 16f)
    val w3 = w1 * 3f
    val w5 = w1 * 5f
    val w7 = w1 * 7f

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

    var y0 = 0
    var i  = 0
    while (y0 < height && i < nPix) {
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, w)
      System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, w)
      System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, w)
      Arrays.fill(nextErrorRed, 0, w, 0f)
      Arrays.fill(nextErrorGreen, 0, w, 0f)
      Arrays.fill(nextErrorBlue, 0, w, 0f)

      val py = flipped + flipDir * y0
      val ny = y0 + 1
      var px = 0
      while (px < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(px), Pixels(py))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          val tbn = TRI_BLUE_NOISE_MULTIPLIERS((px & 63) | ((y0 << 6) & 0xfc0))
          val er  = curErrorRed(px) * tbn
          val eg  = curErrorGreen(px) * tbn
          val eb  = curErrorBlue(px) * tbn
          val rr  = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 24) + er, 0f), 1023f).toInt) & 255
          val gg  = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 16 & 0xff) + eg, 0f), 1023f).toInt) & 255
          val bb  = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 8 & 0xff) + eb, 0f), 1023f).toInt) & 255

          val paletteIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          usedEntry(paletteIndex & 255) = true
          indexedPixels.nn(i) = paletteIndex
          val used = paletteArray(paletteIndex & 0xff)
          // 0x2.1p-8f = 0.00805664062f
          var rdiff = 0.00805664062f * ((color >>> 24) - (used >>> 24))
          var gdiff = 0.00805664062f * ((color >>> 16 & 255) - (used >>> 16 & 255))
          var bdiff = 0.00805664062f * ((color >>> 8 & 255) - (used >>> 8 & 255))
          rdiff /= (0.125f + Math.abs(rdiff))
          gdiff /= (0.125f + Math.abs(gdiff))
          bdiff /= (0.125f + Math.abs(bdiff))
          if (px < w - 1) {
            curErrorRed(px + 1) += rdiff * w7
            curErrorGreen(px + 1) += gdiff * w7
            curErrorBlue(px + 1) += bdiff * w7
          }
          if (ny < height) {
            if (px > 0) {
              nextErrorRed(px - 1) += rdiff * w3
              nextErrorGreen(px - 1) += gdiff * w3
              nextErrorBlue(px - 1) += bdiff * w3
            }
            if (px < w - 1) {
              nextErrorRed(px + 1) += rdiff * w1
              nextErrorGreen(px + 1) += gdiff * w1
              nextErrorBlue(px + 1) += bdiff * w1
            }
            nextErrorRed(px) += rdiff * w5
            nextErrorGreen(px) += gdiff * w5
            nextErrorBlue(px) += bdiff * w5
          }
          i += 1
        }
        px += 1
      }
      y0 += 1
    }
  }

  protected def analyzeWoven(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val pal            = _palette.nn
    val paletteArray   = pal.paletteArray
    val paletteMapping = pal.paletteMapping
    val hasTransparent = paletteArray(0) == 0

    val w              = width
    val populationBias = pal.populationBias
    // 0x1.4p-23f = 1.4901161e-7f; 0x1.4p-1f = 0.625f; 0x5p-10f = 0.0048828125f
    val w1       = (10f * Math.sqrt(_ditherStrength) / (populationBias * populationBias)).toFloat
    val w3       = w1 * 3f
    val w5       = w1 * 5f
    val w7       = w1 * 7f
    val strength = 100f * _ditherStrength / (populationBias * populationBias * populationBias * populationBias)
    val limit    = 5f + 250f / Math.sqrt(pal.colorCount + 1.5f).toFloat

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

    var y0 = 0
    var i  = 0
    while (y0 < height && i < nPix) {
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, w)
      System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, w)
      System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, w)
      Arrays.fill(nextErrorRed, 0, w, 0f)
      Arrays.fill(nextErrorGreen, 0, w, 0f)
      Arrays.fill(nextErrorBlue, 0, w, 0f)

      val py = flipped + flipDir * y0
      val ny = y0 + 1
      var px = 0
      while (px < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(px), Pixels(py))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          val er = Math.min(
            Math.max(
              ((((px + 1).toLong * 0xc13fa9a902a6328fL + (y0 + 1).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 1.4901161e-7f - 0.625f) * strength,
              -limit
            ),
            limit
          ) + curErrorRed(px)
          val eg = Math.min(
            Math.max(
              ((((px + 3).toLong * 0xc13fa9a902a6328fL + (y0 - 1).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 1.4901161e-7f - 0.625f) * strength,
              -limit
            ),
            limit
          ) + curErrorGreen(px)
          val eb = Math.min(
            Math.max(
              ((((px + 2).toLong * 0xc13fa9a902a6328fL + (y0 - 4).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 1.4901161e-7f - 0.625f) * strength,
              -limit
            ),
            limit
          ) + curErrorBlue(px)

          val rr = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 24) + er, 0f), 1023f).toInt) & 255
          val gg = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 16 & 0xff) + eg, 0f), 1023f).toInt) & 255
          val bb = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 8 & 0xff) + eb, 0f), 1023f).toInt) & 255

          val paletteIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          usedEntry(paletteIndex & 255) = true
          indexedPixels.nn(i) = paletteIndex
          val used = paletteArray(paletteIndex & 0xff)
          // 0x5p-10f = 0.0048828125f
          val rdiff = 0.0048828125f * ((color >>> 24) - (used >>> 24))
          val gdiff = 0.0048828125f * ((color >>> 16 & 255) - (used >>> 16 & 255))
          val bdiff = 0.0048828125f * ((color >>> 8 & 255) - (used >>> 8 & 255))
          if (px < w - 1) {
            curErrorRed(px + 1) += rdiff * w7
            curErrorGreen(px + 1) += gdiff * w7
            curErrorBlue(px + 1) += bdiff * w7
          }
          if (ny < height) {
            if (px > 0) {
              nextErrorRed(px - 1) += rdiff * w3
              nextErrorGreen(px - 1) += gdiff * w3
              nextErrorBlue(px - 1) += bdiff * w3
            }
            if (px < w - 1) {
              nextErrorRed(px + 1) += rdiff * w1
              nextErrorGreen(px + 1) += gdiff * w1
              nextErrorBlue(px + 1) += bdiff * w1
            }
            nextErrorRed(px) += rdiff * w5
            nextErrorGreen(px) += gdiff * w5
            nextErrorBlue(px) += bdiff * w5
          }
          i += 1
        }
        px += 1
      }
      y0 += 1
    }
  }

  protected def analyzeDodgy(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val pal            = _palette.nn
    val paletteArray   = pal.paletteArray
    val paletteMapping = pal.paletteMapping
    val hasTransparent = paletteArray(0) == 0

    val w              = width
    val populationBias = pal.populationBias
    val w1             = 8f * _ditherStrength
    val w3             = w1 * 3f
    val w5             = w1 * 5f
    val w7             = w1 * 7f
    val strength       = 0.35f * _ditherStrength / (populationBias * populationBias * populationBias)
    val limit          = 90f

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

    var y0 = 0
    var i  = 0
    while (y0 < height && i < nPix) {
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, w)
      System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, w)
      System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, w)
      Arrays.fill(nextErrorRed, 0, w, 0f)
      Arrays.fill(nextErrorGreen, 0, w, 0f)
      Arrays.fill(nextErrorBlue, 0, w, 0f)

      val py = flipped + flipDir * y0
      val ny = y0 + 1
      var px = 0
      while (px < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(px), Pixels(py))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          val er = Math.min(Math.max((TRI_BLUE_NOISE((px & 63) | (py & 63) << 6) + 0.5f) * strength, -limit), limit) + curErrorRed(px)
          val eg = Math.min(Math.max((TRI_BLUE_NOISE_B((px & 63) | (py & 63) << 6) + 0.5f) * strength, -limit), limit) + curErrorGreen(px)
          val eb = Math.min(Math.max((TRI_BLUE_NOISE_C((px & 63) | (py & 63) << 6) + 0.5f) * strength, -limit), limit) + curErrorBlue(px)

          val rr = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 24) + er, 0f), 1023f).toInt) & 255
          val gg = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 16 & 0xff) + eg, 0f), 1023f).toInt) & 255
          val bb = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 8 & 0xff) + eb, 0f), 1023f).toInt) & 255

          val paletteIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          usedEntry(paletteIndex & 255) = true
          indexedPixels.nn(i) = paletteIndex
          val used = paletteArray(paletteIndex & 0xff)

          // 0x5p-8f = 0.01953125f
          var rdiff = 0.01953125f * ((color >>> 24) - (used >>> 24))
          var gdiff = 0.01953125f * ((color >>> 16 & 255) - (used >>> 16 & 255))
          var bdiff = 0.01953125f * ((color >>> 8 & 255) - (used >>> 8 & 255))
          rdiff /= (0.5f + Math.abs(rdiff))
          gdiff /= (0.5f + Math.abs(gdiff))
          bdiff /= (0.5f + Math.abs(bdiff))

          if (px < w - 1) {
            curErrorRed(px + 1) += rdiff * w7
            curErrorGreen(px + 1) += gdiff * w7
            curErrorBlue(px + 1) += bdiff * w7
          }
          if (ny < height) {
            if (px > 0) {
              nextErrorRed(px - 1) += rdiff * w3
              nextErrorGreen(px - 1) += gdiff * w3
              nextErrorBlue(px - 1) += bdiff * w3
            }
            if (px < w - 1) {
              nextErrorRed(px + 1) += rdiff * w1
              nextErrorGreen(px + 1) += gdiff * w1
              nextErrorBlue(px + 1) += bdiff * w1
            }
            nextErrorRed(px) += rdiff * w5
            nextErrorGreen(px) += gdiff * w5
            nextErrorBlue(px) += bdiff * w5
          }
          i += 1
        }
        px += 1
      }
      y0 += 1
    }
  }

  protected def analyzeNeue(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val pal            = _palette.nn
    val paletteArray   = pal.paletteArray
    val paletteMapping = pal.paletteMapping
    val hasTransparent = paletteArray(0) == 0

    val w              = width
    val populationBias = pal.populationBias
    val w1             = _ditherStrength * 8f
    val w3             = w1 * 3f
    val w5             = w1 * 5f
    val w7             = w1 * 7f
    val strength       = 70f * _ditherStrength / (populationBias * populationBias * populationBias)
    val limit          = Math.min(127f, Math.pow(80.0, 1.635 - populationBias).toFloat)

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

    var y0 = 0
    var i  = 0
    while (y0 < height && i < nPix) {
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, w)
      System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, w)
      System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, w)
      Arrays.fill(nextErrorRed, 0, w, 0f)
      Arrays.fill(nextErrorGreen, 0, w, 0f)
      Arrays.fill(nextErrorBlue, 0, w, 0f)

      val py = flipped + flipDir * y0
      val ny = y0 + 1
      var px = 0
      while (px < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(px), Pixels(py))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          var adj = (TRI_BLUE_NOISE((px & 63) | (py & 63) << 6) + 0.5f) * 0.005f // plus or minus 255/400
          adj = Math.min(Math.max(adj * strength, -limit), limit)
          val er = adj + curErrorRed(px)
          val eg = adj + curErrorGreen(px)
          val eb = adj + curErrorBlue(px)
          val rr = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 24) + er, 0f), 1023f).toInt) & 255
          val gg = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 16 & 0xff) + eg, 0f), 1023f).toInt) & 255
          val bb = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 8 & 0xff) + eb, 0f), 1023f).toInt) & 255

          val paletteIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          usedEntry(paletteIndex & 255) = true
          indexedPixels.nn(i) = paletteIndex
          val used = paletteArray(paletteIndex & 0xff)
          // 0x2.Ep-8f = 0.01123046875f
          var rdiff = 0.01123046875f * ((color >>> 24) - (used >>> 24))
          var gdiff = 0.01123046875f * ((color >>> 16 & 255) - (used >>> 16 & 255))
          var bdiff = 0.01123046875f * ((color >>> 8 & 255) - (used >>> 8 & 255))
          rdiff *= 1.25f / (0.25f + Math.abs(rdiff))
          gdiff *= 1.25f / (0.25f + Math.abs(gdiff))
          bdiff *= 1.25f / (0.25f + Math.abs(bdiff))
          if (px < w - 1) {
            curErrorRed(px + 1) += rdiff * w7
            curErrorGreen(px + 1) += gdiff * w7
            curErrorBlue(px + 1) += bdiff * w7
          }
          if (ny < height) {
            if (px > 0) {
              nextErrorRed(px - 1) += rdiff * w3
              nextErrorGreen(px - 1) += gdiff * w3
              nextErrorBlue(px - 1) += bdiff * w3
            }
            if (px < w - 1) {
              nextErrorRed(px + 1) += rdiff * w1
              nextErrorGreen(px + 1) += gdiff * w1
              nextErrorBlue(px + 1) += bdiff * w1
            }
            nextErrorRed(px) += rdiff * w5
            nextErrorGreen(px) += gdiff * w5
            nextErrorBlue(px) += bdiff * w5
          }
          i += 1
        }
        px += 1
      }
      y0 += 1
    }
  }

  protected def analyzeOverboard(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val pal            = _palette.nn
    val paletteArray   = pal.paletteArray
    val paletteMapping = pal.paletteMapping
    val hasTransparent = paletteArray(0) == 0

    val w              = width
    val populationBias = pal.populationBias
    val strength       = _ditherStrength * 1.5f * (populationBias * populationBias)
    val noiseStrength  = 4f / (populationBias * populationBias)
    val limit          = 110f

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

    var by = 0
    var y  = flipped
    var i  = 0
    while (by < height && i < nPix) {
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, w)
      System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, w)
      System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, w)
      Arrays.fill(nextErrorRed, 0, w, 0f)
      Arrays.fill(nextErrorGreen, 0, w, 0f)
      Arrays.fill(nextErrorBlue, 0, w, 0f)

      var x = 0
      while (x < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(x), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          var er = 0f
          var eg = 0f
          var eb = 0f
          // 0x1p-5f = 0.03125f; 0x1p-6f = 0.015625f
          // 0x1p-20f = 9.5367431640625e-7f; 0x1.8p-20f = 1.430511474609375e-6f; 0x1.8p-21f = 7.152557373046875e-7f
          ((x << 1 & 2) | (y & 1)) match {
            case 0 =>
              er += ((x ^ y) % 9 - 4)
              er += ((x.toLong * 0xc13fa9a902a6328fL + y.toLong * 0x91e10da5c79e7b1dL) >> 41) * 9.5367431640625e-7f
              eg += (TRI_BLUE_NOISE_B((x & 63) | (y & 63) << 6) + 0.5f) * 0.03125f
              eg += ((x.toLong * -0xc13fa9a902a6328fL + y.toLong * 0x91e10da5c79e7b1dL) >> 41) * 9.5367431640625e-7f
              eb += (TRI_BLUE_NOISE_C((x & 63) | (y & 63) << 6) + 0.5f) * 0.015625f
              eb += ((y.toLong * 0xc13fa9a902a6328fL + x.toLong * -0x91e10da5c79e7b1dL) >> 41) * 1.430511474609375e-6f
            case 1 =>
              er += (TRI_BLUE_NOISE((x & 63) | (y & 63) << 6) + 0.5f) * 0.03125f
              er += ((x.toLong * -0xc13fa9a902a6328fL + y.toLong * 0x91e10da5c79e7b1dL) >> 41) * 9.5367431640625e-7f
              eg += (TRI_BLUE_NOISE_B((x & 63) | (y & 63) << 6) + 0.5f) * 0.015625f
              eg += ((y.toLong * 0xc13fa9a902a6328fL + x.toLong * -0x91e10da5c79e7b1dL) >> 41) * 1.430511474609375e-6f
              eb += ((x ^ y) % 11 - 5)
              eb += ((y.toLong * -0xc13fa9a902a6328fL + x.toLong * -0x91e10da5c79e7b1dL) >> 41) * 7.152557373046875e-7f
            case 2 =>
              er += (TRI_BLUE_NOISE((x & 63) | (y & 63) << 6) + 0.5f) * 0.015625f
              er += ((y.toLong * 0xc13fa9a902a6328fL + x.toLong * -0x91e10da5c79e7b1dL) >> 41) * 1.430511474609375e-6f
              eg += ((x ^ y) % 11 - 5)
              eg += ((y.toLong * -0xc13fa9a902a6328fL + x.toLong * -0x91e10da5c79e7b1dL) >> 41) * 7.152557373046875e-7f
              eb += ((x ^ y) % 9 - 4)
              eb += ((x.toLong * 0xc13fa9a902a6328fL + y.toLong * 0x91e10da5c79e7b1dL) >> 41) * 9.5367431640625e-7f
            case _ => // case 3
              er += ((x ^ y) % 11 - 5)
              er += ((y.toLong * -0xc13fa9a902a6328fL + x.toLong * -0x91e10da5c79e7b1dL) >> 41) * 7.152557373046875e-7f
              eg += ((x ^ y) % 9 - 4)
              eg += ((x.toLong * 0xc13fa9a902a6328fL + y.toLong * 0x91e10da5c79e7b1dL) >> 41) * 9.5367431640625e-7f
              eb += (TRI_BLUE_NOISE_C((x & 63) | (y & 63) << 6) + 0.5f) * 0.03125f
              eb += ((x.toLong * -0xc13fa9a902a6328fL + y.toLong * 0x91e10da5c79e7b1dL) >> 41) * 9.5367431640625e-7f
          }
          er = er * noiseStrength + curErrorRed(x)
          eg = eg * noiseStrength + curErrorGreen(x)
          eb = eb * noiseStrength + curErrorBlue(x)
          val rr = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 24) + Math.min(Math.max(er, -limit), limit), 0f), 1023f).toInt) & 255
          val gg = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 16 & 0xff) + Math.min(Math.max(eg, -limit), limit), 0f), 1023f).toInt) & 255
          val bb = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 8 & 0xff) + Math.min(Math.max(eb, -limit), limit), 0f), 1023f).toInt) & 255

          val paletteIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          usedEntry(paletteIndex & 255) = true
          indexedPixels.nn(i) = paletteIndex
          val used  = paletteArray(paletteIndex & 0xff)
          val rdiff = ((color >>> 24) - (used >>> 24)) * strength
          val gdiff = ((color >>> 16 & 255) - (used >>> 16 & 255)) * strength
          val bdiff = ((color >>> 8 & 255) - (used >>> 8 & 255)) * strength
          val r1    = rdiff * 16f / (45f + Math.abs(rdiff))
          val g1    = gdiff * 16f / (45f + Math.abs(gdiff))
          val b1    = bdiff * 16f / (45f + Math.abs(bdiff))
          val r2    = r1 + r1
          val g2    = g1 + g1
          val b2    = b1 + b1
          val r4    = r2 + r2
          val g4    = g2 + g2
          val b4    = b2 + b2
          if (x < w - 1) {
            curErrorRed(x + 1) += r4
            curErrorGreen(x + 1) += g4
            curErrorBlue(x + 1) += b4
            if (x < w - 2) {
              curErrorRed(x + 2) += r2
              curErrorGreen(x + 2) += g2
              curErrorBlue(x + 2) += b2
            }
          }
          if (by + 1 < height) {
            if (x > 0) {
              nextErrorRed(x - 1) += r2
              nextErrorGreen(x - 1) += g2
              nextErrorBlue(x - 1) += b2
              if (x > 1) {
                nextErrorRed(x - 2) += r1
                nextErrorGreen(x - 2) += g1
                nextErrorBlue(x - 2) += b1
              }
            }
            nextErrorRed(x) += r4
            nextErrorGreen(x) += g4
            nextErrorBlue(x) += b4
            if (x < w - 1) {
              nextErrorRed(x + 1) += r2
              nextErrorGreen(x + 1) += g2
              nextErrorBlue(x + 1) += b2
              if (x < w - 2) {
                nextErrorRed(x + 2) += r1
                nextErrorGreen(x + 2) += g1
                nextErrorBlue(x + 2) += b1
              }
            }
          }
          i += 1
        }
        x += 1
      }
      by += 1
      y += flipDir
    }
  }

  protected def analyzeBurkes(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val pal            = _palette.nn
    val paletteArray   = pal.paletteArray
    val paletteMapping = pal.paletteMapping
    val hasTransparent = paletteArray(0) == 0

    val w              = width
    val populationBias = pal.populationBias
    val s              = 0.13f * _ditherStrength / (populationBias * populationBias)
    val strength       = s * 0.58f / (0.3f + s)

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

    var y0 = 0
    var i  = 0
    while (y0 < height && i < nPix) {
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, w)
      System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, w)
      System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, w)
      Arrays.fill(nextErrorRed, 0, w, 0f)
      Arrays.fill(nextErrorGreen, 0, w, 0f)
      Arrays.fill(nextErrorBlue, 0, w, 0f)

      val py = flipped + flipDir * y0
      val ny = y0 + 1
      var px = 0
      while (px < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(px), Pixels(py))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          val er = curErrorRed(px)
          val eg = curErrorGreen(px)
          val eb = curErrorBlue(px)
          val rr = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 24) + er, 0f), 1023f).toInt) & 255
          val gg = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 16 & 0xff) + eg, 0f), 1023f).toInt) & 255
          val bb = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 8 & 0xff) + eb, 0f), 1023f).toInt) & 255

          val paletteIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          usedEntry(paletteIndex & 255) = true
          indexedPixels.nn(i) = paletteIndex
          val used     = paletteArray(paletteIndex & 0xff)
          val rdiffInt = (color >>> 24) - (used >>> 24)
          val gdiffInt = (color >>> 16 & 255) - (used >>> 16 & 255)
          val bdiffInt = (color >>> 8 & 255) - (used >>> 8 & 255)
          val r1       = rdiffInt * strength
          val g1       = gdiffInt * strength
          val b1       = bdiffInt * strength
          val r2       = r1 + r1
          val g2       = g1 + g1
          val b2       = b1 + b1
          val r4       = r2 + r2
          val g4       = g2 + g2
          val b4       = b2 + b2
          if (px < w - 1) {
            curErrorRed(px + 1) += r4
            curErrorGreen(px + 1) += g4
            curErrorBlue(px + 1) += b4
            if (px < w - 2) {
              curErrorRed(px + 2) += r2
              curErrorGreen(px + 2) += g2
              curErrorBlue(px + 2) += b2
            }
          }
          if (ny < height) {
            if (px > 0) {
              nextErrorRed(px - 1) += r2
              nextErrorGreen(px - 1) += g2
              nextErrorBlue(px - 1) += b2
              if (px > 1) {
                nextErrorRed(px - 2) += r1
                nextErrorGreen(px - 2) += g1
                nextErrorBlue(px - 2) += b1
              }
            }
            nextErrorRed(px) += r4
            nextErrorGreen(px) += g4
            nextErrorBlue(px) += b4
            if (px < w - 1) {
              nextErrorRed(px + 1) += r2
              nextErrorGreen(px + 1) += g2
              nextErrorBlue(px + 1) += b2
              if (px < w - 2) {
                nextErrorRed(px + 2) += r1
                nextErrorGreen(px + 2) += g1
                nextErrorBlue(px + 2) += b1
              }
            }
          }
          i += 1
        }
        px += 1
      }
      y0 += 1
    }
  }

  protected def analyzeOceanic(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val pal            = _palette.nn
    val paletteArray   = pal.paletteArray
    val paletteMapping = pal.paletteMapping
    val hasTransparent = paletteArray(0) == 0
    val noise          = TRI_BLUE_NOISE_MULTIPLIERS

    val w              = width
    val populationBias = pal.populationBias
    val s              = 0.13f * _ditherStrength / (populationBias * populationBias)
    val strength       = s * 0.58f / (0.3f + s)

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

    var y0 = 0
    var i  = 0
    while (y0 < height && i < nPix) {
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, w)
      System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, w)
      System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, w)
      Arrays.fill(nextErrorRed, 0, w, 0f)
      Arrays.fill(nextErrorGreen, 0, w, 0f)
      Arrays.fill(nextErrorBlue, 0, w, 0f)

      val py = flipped + flipDir * y0
      val ny = y0 + 1
      var px = 0
      while (px < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(px), Pixels(py))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          val er = curErrorRed(px)
          val eg = curErrorGreen(px)
          val eb = curErrorBlue(px)
          val rr = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 24) + er, 0f), 1023f).toInt) & 255
          val gg = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 16 & 0xff) + eg, 0f), 1023f).toInt) & 255
          val bb = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 8 & 0xff) + eb, 0f), 1023f).toInt) & 255

          val paletteIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          usedEntry(paletteIndex & 255) = true
          indexedPixels.nn(i) = paletteIndex
          val used     = paletteArray(paletteIndex & 0xff)
          val rdiffInt = (color >>> 24) - (used >>> 24)
          val gdiffInt = (color >>> 16 & 255) - (used >>> 16 & 255)
          val bdiffInt = (color >>> 8 & 255) - (used >>> 8 & 255)
          val r1       = rdiffInt * strength
          val g1       = gdiffInt * strength
          val b1       = bdiffInt * strength
          val r2       = r1 + r1
          val g2       = g1 + g1
          val b2       = b1 + b1
          val r4       = r2 + r2
          val g4       = g2 + g2
          val b4       = b2 + b2
          var modifier = 0f
          if (px < w - 1) {
            modifier = noise((px + 1 & 63) | ((py << 6) & 0xfc0))
            curErrorRed(px + 1) += r4 * modifier
            curErrorGreen(px + 1) += g4 * modifier
            curErrorBlue(px + 1) += b4 * modifier
            if (px < w - 2) {
              modifier = noise((px + 2 & 63) | ((py << 6) & 0xfc0))
              curErrorRed(px + 2) += r2 * modifier
              curErrorGreen(px + 2) += g2 * modifier
              curErrorBlue(px + 2) += b2 * modifier
            }
          }
          if (ny < height) {
            if (px > 0) {
              modifier = noise((px - 1 & 63) | ((ny << 6) & 0xfc0))
              nextErrorRed(px - 1) += r2 * modifier
              nextErrorGreen(px - 1) += g2 * modifier
              nextErrorBlue(px - 1) += b2 * modifier
              if (px > 1) {
                modifier = noise((px - 2 & 63) | ((ny << 6) & 0xfc0))
                nextErrorRed(px - 2) += r1 * modifier
                nextErrorGreen(px - 2) += g1 * modifier
                nextErrorBlue(px - 2) += b1 * modifier
              }
            }
            modifier = noise((px & 63) | ((ny << 6) & 0xfc0))
            nextErrorRed(px) += r4 * modifier
            nextErrorGreen(px) += g4 * modifier
            nextErrorBlue(px) += b4 * modifier
            if (px < w - 1) {
              modifier = noise((px + 1 & 63) | ((ny << 6) & 0xfc0))
              nextErrorRed(px + 1) += r2 * modifier
              nextErrorGreen(px + 1) += g2 * modifier
              nextErrorBlue(px + 1) += b2 * modifier
              if (px < w - 2) {
                modifier = noise((px + 2 & 63) | ((ny << 6) & 0xfc0))
                nextErrorRed(px + 2) += r1 * modifier
                nextErrorGreen(px + 2) += g1 * modifier
                nextErrorBlue(px + 2) += b1 * modifier
              }
            }
          }
          i += 1
        }
        px += 1
      }
      y0 += 1
    }
  }

  protected def analyzeSeaside(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val pal            = _palette.nn
    val paletteArray   = pal.paletteArray
    val paletteMapping = pal.paletteMapping
    val hasTransparent = paletteArray(0) == 0
    val noiseA         = TRI_BLUE_NOISE_MULTIPLIERS
    val noiseB         = TRI_BLUE_NOISE_MULTIPLIERS_B
    val noiseC         = TRI_BLUE_NOISE_MULTIPLIERS_C

    val w              = width
    val populationBias = pal.populationBias
    val s              = 0.15f * populationBias * _ditherStrength
    val strength       = s * 0.6f / (0.35f + s)

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

    var y0 = 0
    var i  = 0
    while (y0 < height && i < nPix) {
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, w)
      System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, w)
      System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, w)
      Arrays.fill(nextErrorRed, 0, w, 0f)
      Arrays.fill(nextErrorGreen, 0, w, 0f)
      Arrays.fill(nextErrorBlue, 0, w, 0f)

      val py = flipped + flipDir * y0
      val ny = y0 + 1
      var px = 0
      while (px < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(px), Pixels(py))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          val er = curErrorRed(px)
          val eg = curErrorGreen(px)
          val eb = curErrorBlue(px)
          val rr = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 24) + er, 0f), 1023f).toInt) & 255
          val gg = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 16 & 0xff) + eg, 0f), 1023f).toInt) & 255
          val bb = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 8 & 0xff) + eb, 0f), 1023f).toInt) & 255

          val paletteIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          usedEntry(paletteIndex & 255) = true
          indexedPixels.nn(i) = paletteIndex
          val used     = paletteArray(paletteIndex & 0xff)
          val rdiffInt = (color >>> 24) - (used >>> 24)
          val gdiffInt = (color >>> 16 & 255) - (used >>> 16 & 255)
          val bdiffInt = (color >>> 8 & 255) - (used >>> 8 & 255)
          var modifier = (px & 63) | (py << 6 & 0xfc0)
          val r1       = rdiffInt * strength * noiseA(modifier)
          val g1       = gdiffInt * strength * noiseB(modifier)
          val b1       = bdiffInt * strength * noiseC(modifier)
          val r2       = r1 + r1
          val g2       = g1 + g1
          val b2       = b1 + b1
          val r4       = r2 + r2
          val g4       = g2 + g2
          val b4       = b2 + b2

          if (px < w - 1) {
            modifier = (px + 1 & 63) | (py << 6 & 0xfc0)
            curErrorRed(px + 1) += r4 * noiseA(modifier)
            curErrorGreen(px + 1) += g4 * noiseB(modifier)
            curErrorBlue(px + 1) += b4 * noiseC(modifier)
            if (px < w - 2) {
              modifier = (px + 2 & 63) | ((py << 6) & 0xfc0)
              curErrorRed(px + 2) += r2 * noiseA(modifier)
              curErrorGreen(px + 2) += g2 * noiseB(modifier)
              curErrorBlue(px + 2) += b2 * noiseC(modifier)
            }
          }
          if (ny < height) {
            if (px > 0) {
              modifier = (px - 1 & 63) | ((ny << 6) & 0xfc0)
              nextErrorRed(px - 1) += r2 * noiseA(modifier)
              nextErrorGreen(px - 1) += g2 * noiseB(modifier)
              nextErrorBlue(px - 1) += b2 * noiseC(modifier)
              if (px > 1) {
                modifier = (px - 2 & 63) | ((ny << 6) & 0xfc0)
                nextErrorRed(px - 2) += r1 * noiseA(modifier)
                nextErrorGreen(px - 2) += g1 * noiseB(modifier)
                nextErrorBlue(px - 2) += b1 * noiseC(modifier)
              }
            }
            modifier = (px & 63) | ((ny << 6) & 0xfc0)
            nextErrorRed(px) += r4 * noiseA(modifier)
            nextErrorGreen(px) += g4 * noiseB(modifier)
            nextErrorBlue(px) += b4 * noiseC(modifier)
            if (px < w - 1) {
              modifier = (px + 1 & 63) | ((ny << 6) & 0xfc0)
              nextErrorRed(px + 1) += r2 * noiseA(modifier)
              nextErrorGreen(px + 1) += g2 * noiseB(modifier)
              nextErrorBlue(px + 1) += b2 * noiseC(modifier)
              if (px < w - 2) {
                modifier = (px + 2 & 63) | ((ny << 6) & 0xfc0)
                nextErrorRed(px + 2) += r1 * noiseA(modifier)
                nextErrorGreen(px + 2) += g1 * noiseB(modifier)
                nextErrorBlue(px + 2) += b1 * noiseC(modifier)
              }
            }
          }
          i += 1
        }
        px += 1
      }
      y0 += 1
    }
  }

  protected def analyzeMarten(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val paletteArray   = _palette.nn.paletteArray
    val paletteMapping = _palette.nn.paletteMapping
    val hasTransparent = paletteArray(0) == 0

    val populationBias = _palette.nn.populationBias
    val str            = Math.min(
      1100f * (_ditherStrength / Math.sqrt(_palette.nn.colorCount).toFloat * (1f / (populationBias * populationBias * populationBias) - 0.7f)),
      127f
    )
    var y0 = 0
    var i  = 0
    while (y0 < height && i < nPix) {
      var px = 0
      while (px < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(px), Pixels(flipped + flipDir * y0))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          // We get a sub-random value from 0-1 using interleaved gradient noise.
          // Offsetting this value by different values and feeding into triangleWave()
          // gives 3 different values for r, g, and b, without much bias toward high or low values.
          // There is correlation between r, g, and b in certain patterns.
          // 0x1p-8f = 0.00390625f
          val theta = (px * 142 + y0 * 79 & 255) * 0.00390625f
          val rr    = fromLinearLUT((toLinearLUT(color >>> 24) + OtherMath.triangleWave(theta) * str).toInt) & 255
          val gg    = fromLinearLUT((toLinearLUT(color >>> 16 & 0xff) + OtherMath.triangleWave(theta + 0.382f) * str).toInt) & 255
          val bb    = fromLinearLUT((toLinearLUT(color >>> 8 & 0xff) + OtherMath.triangleWave(theta + 0.618f) * str).toInt) & 255
          val idx   = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          usedEntry(idx & 255) = true
          indexedPixels.nn(i) = idx
          i += 1
        }
        px += 1
      }
      y0 += 1
    }
  }

  protected def analyzeWren(): Unit = {
    val nPix           = indexedPixels.nn.length
    val flipped        = if (flipY) height - 1 else 0
    val flipDir        = if (flipY) -1 else 1
    val pal            = _palette.nn
    val paletteArray   = pal.paletteArray
    val paletteMapping = pal.paletteMapping
    val hasTransparent = paletteArray(0) == 0

    val w                     = width
    val populationBias        = pal.populationBias
    val partialDitherStrength = 0.5f * _ditherStrength / (populationBias * populationBias)
    val strength              = 80f * _ditherStrength / (populationBias * populationBias)
    val blueStrength          = 0.3f * _ditherStrength / (populationBias * populationBias)
    val limit                 = 5f + 200f / Math.sqrt(pal.colorCount + 1.5f).toFloat

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

    var by = 0
    var i  = 0
    while (by < height && i < nPix) {
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, w)
      System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, w)
      System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, w)
      Arrays.fill(nextErrorRed, 0, w, 0f)
      Arrays.fill(nextErrorGreen, 0, w, 0f)
      Arrays.fill(nextErrorBlue, 0, w, 0f)

      val y0 = flipped + flipDir * by
      var x0 = 0
      while (x0 < width && i < nPix) {
        val color = image.nn.getPixel(Pixels(x0), Pixels(y0))
        if (hasTransparent && (color & 0x80) == 0) {
          indexedPixels.nn(i) = 0
          i += 1
        } else {
          val er = Math.min(
            Math.max(
              (TRI_BLUE_NOISE((x0 & 63) | (y0 & 63) << 6) + 0.5f) * blueStrength +
                ((((x0 + 1).toLong * 0xc13fa9a902a6328fL + (y0 + 1).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 7.450580596923828e-9f - 0.625f) * strength,
              -limit
            ),
            limit
          ) + curErrorRed(x0)
          val eg = Math.min(
            Math.max(
              (TRI_BLUE_NOISE_B((x0 & 63) | (y0 & 63) << 6) + 0.5f) * blueStrength +
                ((((x0 + 3).toLong * 0xc13fa9a902a6328fL + (y0 - 1).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 7.450580596923828e-9f - 0.625f) * strength,
              -limit
            ),
            limit
          ) + curErrorGreen(x0)
          val eb = Math.min(
            Math.max(
              (TRI_BLUE_NOISE_C((x0 & 63) | (y0 & 63) << 6) + 0.5f) * blueStrength +
                ((((x0 + 2).toLong * 0xc13fa9a902a6328fL + (y0 - 4).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 7.450580596923828e-9f - 0.625f) * strength,
              -limit
            ),
            limit
          ) + curErrorBlue(x0)

          val rr = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 24) + er, 0), 1023).toInt) & 255
          val gg = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 16 & 0xff) + eg, 0), 1023).toInt) & 255
          val bb = fromLinearLUT(Math.min(Math.max(toLinearLUT(color >>> 8 & 0xff) + eb, 0), 1023).toInt) & 255

          val paletteIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          usedEntry(paletteIndex & 255) = true
          indexedPixels.nn(i) = paletteIndex

          val used  = paletteArray(paletteIndex & 0xff)
          val rdiff = ((color >>> 24) - (used >>> 24)) * partialDitherStrength
          val gdiff = ((color >>> 16 & 255) - (used >>> 16 & 255)) * partialDitherStrength
          val bdiff = ((color >>> 8 & 255) - (used >>> 8 & 255)) * partialDitherStrength

          val r1 = rdiff * 16f / Math.sqrt(2048f + rdiff * rdiff).toFloat
          val g1 = gdiff * 16f / Math.sqrt(2048f + gdiff * gdiff).toFloat
          val b1 = bdiff * 16f / Math.sqrt(2048f + bdiff * bdiff).toFloat
          val r2 = r1 + r1
          val g2 = g1 + g1
          val b2 = b1 + b1
          val r4 = r2 + r2
          val g4 = g2 + g2
          val b4 = b2 + b2

          if (x0 < w - 1) {
            curErrorRed(x0 + 1) += r4
            curErrorGreen(x0 + 1) += g4
            curErrorBlue(x0 + 1) += b4
            if (x0 < w - 2) {
              curErrorRed(x0 + 2) += r2
              curErrorGreen(x0 + 2) += g2
              curErrorBlue(x0 + 2) += b2
            }
          }
          if (by + 1 < height) {
            if (x0 > 0) {
              nextErrorRed(x0 - 1) += r2
              nextErrorGreen(x0 - 1) += g2
              nextErrorBlue(x0 - 1) += b2
              if (x0 > 1) {
                nextErrorRed(x0 - 2) += r1
                nextErrorGreen(x0 - 2) += g1
                nextErrorBlue(x0 - 2) += b1
              }
            }
            nextErrorRed(x0) += r4
            nextErrorGreen(x0) += g4
            nextErrorBlue(x0) += b4
            if (x0 < w - 1) {
              nextErrorRed(x0 + 1) += r2
              nextErrorGreen(x0 + 1) += g2
              nextErrorBlue(x0 + 1) += b2
              if (x0 < w - 2) {
                nextErrorRed(x0 + 2) += r1
                nextErrorGreen(x0 + 2) += g1
                nextErrorBlue(x0 + 2) += b1
              }
            }
          }
          i += 1
        }
        x0 += 1
      }
      by += 1
    }
  }

  /** Analyzes image colors and creates color map. */
  protected def analyzePixels(): Unit = {
    val nPix = width * height
    indexedPixels = new Array[Byte](nPix)
    _palette.nn.setDitherStrength(_ditherStrength)
    if (seq > 1 && _clearPalette) {
      if (fastAnalysis) {
        _palette.nn.analyzeFast(image.nn, 300, 256)
      } else {
        _palette.nn.analyze(image.nn, 300, 256)
      }
    }
    val paletteArray = _palette.nn.paletteArray

    colorTab = new Array[Byte](256 * 3) // create reduced palette
    var ci = 0
    var bi = 0
    while (ci < 256) {
      val pa = paletteArray(ci)
      colorTab.nn(bi) = (pa >>> 24).toByte
      bi += 1
      colorTab.nn(bi) = (pa >>> 16).toByte
      bi += 1
      colorTab.nn(bi) = (pa >>> 8).toByte
      bi += 1
      usedEntry(ci) = false
      ci += 1
    }
    // map image pixels to new palette
    val hasTransparent = paletteArray(0) == 0
    _ditherAlgorithm match {
      case DitherAlgorithm.NONE           => analyzeNone()
      case DitherAlgorithm.PATTERN        => analyzePattern()
      case DitherAlgorithm.CHAOTIC_NOISE  => analyzeChaotic()
      case DitherAlgorithm.GRADIENT_NOISE => analyzeGradient()
      case DitherAlgorithm.ADDITIVE       => analyzeAdditive()
      case DitherAlgorithm.ROBERTS        => analyzeRoberts()
      case DitherAlgorithm.LOAF           => analyzeLoaf()
      case DitherAlgorithm.DIFFUSION      => analyzeDiffusion()
      case DitherAlgorithm.BLUE_NOISE     => analyzeBlue()
      case DitherAlgorithm.BLUNT          => analyzeBlunt()
      case DitherAlgorithm.BANTER         => analyzeBanter()
      case DitherAlgorithm.SCATTER        => analyzeScatter()
      case DitherAlgorithm.WOVEN          => analyzeWoven()
      case DitherAlgorithm.DODGY          => analyzeDodgy()
      case DitherAlgorithm.NEUE           => analyzeNeue()
      case DitherAlgorithm.OVERBOARD      => analyzeOverboard()
      case DitherAlgorithm.BURKES         => analyzeBurkes()
      case DitherAlgorithm.OCEANIC        => analyzeOceanic()
      case DitherAlgorithm.SEASIDE        => analyzeSeaside()
      case DitherAlgorithm.GOURD          => analyzeGourd()
      case DitherAlgorithm.MARTEN         => analyzeMarten()
      case DitherAlgorithm.WREN           => analyzeWren()
    }
    colorDepth = 8
    palSize = 7
    // get the closest match to transparent color if specified
    if (hasTransparent) {
      transIndex = 0
    }
  }

  /** Extracts image pixels into byte array "pixels" */
  protected def getImagePixels(): Unit = {
    val w = image.nn.width.toInt
    val h = image.nn.height.toInt
    if ((w != width) || (h != height)) {
      val temp = new Pixmap(width, height, Pixmap.Format.RGBA8888)
      temp.drawPixmap(image.nn, Pixels(0), Pixels(0))
      image = temp
    }
  }

  /** Writes Graphic Control Extension */
  @throws[IOException]
  protected def writeGraphicCtrlExt(): Unit = {
    val o = out.nn
    o.write(0x21) // extension introducer
    o.write(0xf9) // GCE label
    o.write(4) // data block size
    var transp: Int = 0
    var disp0:  Int = 0
    if (transIndex == -1) {
      transp = 0
      disp0 = 0 // dispose = no action
    } else {
      transp = 1
      disp0 = 2 // force clear if using transparent color
    }
    if (dispose >= 0) {
      disp0 = dispose & 7 // user override
    }
    disp0 <<= 2
    o.write(0 | disp0 | 0 | transp) // packed fields
    writeShort(Math.round(delay / 10f)) // delay x 1/100 sec
    o.write(transIndex) // transparent color index
    o.write(0) // block terminator
  }

  /** Writes Image Descriptor */
  @throws[IOException]
  protected def writeImageDesc(): Unit = {
    val o = out.nn
    o.write(0x2c) // image separator
    writeShort(x) // image position x,y = 0,0
    writeShort(y)
    writeShort(width) // image size
    writeShort(height)
    // packed fields
    if (firstFrame) {
      o.write(0) // no LCT - GCT is used for first (or only) frame
    } else {
      o.write(0x80 | 0 | 0 | 0 | palSize) // specify normal LCT
    }
  }

  /** Writes Logical Screen Descriptor */
  @throws[IOException]
  protected def writeLSD(): Unit = {
    val o = out.nn
    writeShort(width)
    writeShort(height)
    o.write(0x80 | 0x70 | 0x00 | palSize) // packed fields
    o.write(0) // background color index
    o.write(0) // pixel aspect ratio - assume 1:1
  }

  /** Writes Netscape application extension to define repeat count. */
  @throws[IOException]
  protected def writeNetscapeExt(): Unit = {
    val o = out.nn
    o.write(0x21) // extension introducer
    o.write(0xff) // app extension label
    o.write(11) // block size
    writeString("NETSCAPE" + "2.0") // app id + auth code
    o.write(3) // sub-block size
    o.write(1) // loop sub-block id
    writeShort(repeat) // loop count (extra iterations, 0=repeat forever)
    o.write(0) // block terminator
  }

  /** Writes color table */
  @throws[IOException]
  protected def writePalette(): Unit = {
    val o  = out.nn
    val ct = colorTab.nn
    o.write(ct, 0, ct.length)
    val n = (3 * 256) - ct.length
    var j = 0
    while (j < n) {
      o.write(0)
      j += 1
    }
  }

  /** Encodes and writes pixel data */
  @throws[IOException]
  protected def writePixels(): Unit = {
    val encoder = new LZWEncoder(width, height, indexedPixels.nn, colorDepth)
    encoder.encode(out.nn)
  }

  /** Write 16-bit value to output stream, LSB first */
  @throws[IOException]
  protected def writeShort(value: Int): Unit = {
    val o = out.nn
    o.write(value & 0xff)
    o.write((value >>> 8) & 0xff)
  }

  /** Writes string to output stream */
  @throws[IOException]
  protected def writeString(s: String): Unit = {
    var si = 0
    while (si < s.length) {
      out.nn.write(s.charAt(si).toByte)
      si += 1
    }
  }

  /** If true (the default) and [[palette]] is null, this uses a lower-quality but much-faster algorithm to analyze the color palette in each frame.
    */
  def isFastAnalysis: Boolean = fastAnalysis

  def setFastAnalysis(fastAnalysis: Boolean): Boolean = {
    this.fastAnalysis = fastAnalysis
    this.fastAnalysis
  }
}
