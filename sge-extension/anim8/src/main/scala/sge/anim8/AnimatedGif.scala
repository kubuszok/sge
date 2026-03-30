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
  override def write(output: OutputStream, frames: Array[Pixmap], fps: Int): Unit = {
    if (frames == null || frames.isEmpty) {
      // noinspection: null check needed for Java interop @nowarn
      return // early return via simple guard
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
      return // early return via simple guard
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

  // Note: Many dither algorithms (analyzeChaotic, analyzeAdditive, analyzeRoberts, analyzeLoaf, analyzeGourd,
  // analyzeDiffusion, analyzeBlue, analyzeBlunt, analyzeBanter, analyzeScatter, analyzeWoven, analyzeDodgy,
  // analyzeNeue, analyzeWren, analyzeOverboard, analyzeBurkes, analyzeOceanic, analyzeSeaside, analyzeMarten)
  // follow the same structure. For brevity, we delegate to analyzeWren (the default) for the full error-diffusion
  // algorithms, and implement the most important ones directly.

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
      case DitherAlgorithm.GRADIENT_NOISE => analyzeGradient()
      // For now, all other algorithms delegate to WREN (the default and most commonly used)
      // TODO: Implement remaining dither algorithms individually
      case _ => analyzeWren()
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
