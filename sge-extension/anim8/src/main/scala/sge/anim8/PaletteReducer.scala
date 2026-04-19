/*
 * Ported from anim8-gdx - https://github.com/tommyettinger/anim8-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-source-reference: anim8-gdx/src/main/java/com/github/tommyettinger/anim8/PaletteReducer.java
 * Covenant-verified: 2026-04-19
 */
package sge
package anim8

import sge.files.FileHandle
import sge.graphics.{ Color, Pixmap }
import sge.math.Interpolation

import java.util.Arrays
import scala.collection.mutable
import scala.util.boundary
import scala.util.boundary.break

/** Data that can be used to limit the colors present in a Pixmap or other image, here with the goal of using 256 or fewer colors in the image (for saving indexed-mode images). Can be used
  * independently of classes like [[AnimatedGif]] and [[PNG8]], but it is meant to help with intelligently reducing the color count to fit under the maximum palette size for those formats.
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

  /** Builds the palette information this PaletteReducer stores from the RGBA8888 ints in rgbaPalette, up to 256 colors or limit, whichever is less. */
  def exact(rgbaPalette: Array[Int], limit: Int): Unit = {
    if (rgbaPalette == null || rgbaPalette.length < 2 || limit < 2) {
      exact(PaletteReducer.SNUGGLY, ConstantData.ENCODED_SNUGGLY)
      return // safe in this context -- simple guard clause
    }
    Arrays.fill(paletteArray, 0)
    Arrays.fill(paletteMapping, 0.toByte)
    val plen = Math.min(Math.min(256, limit), rgbaPalette.length)
    colorCount = plen
    populationBias = Math.exp(-1.375 / colorCount).toFloat
    var color = 0
    var i     = 0
    while (i < plen) {
      color = rgbaPalette(i)
      if ((color & 0x80) != 0) {
        paletteArray(i) = color
        paletteMapping((color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f)) = i.toByte
      }
      i += 1
    }
    var rr = 0
    var gg = 0
    var bb = 0
    var r  = 0
    while (r < 32) {
      rr = r << 3 | r >>> 2
      var g = 0
      while (g < 32) {
        gg = g << 3 | g >>> 2
        var b = 0
        while (b < 32) {
          val c2 = r << 10 | g << 5 | b
          if (paletteMapping(c2) == 0) {
            bb = b << 3 | b >>> 2
            var dist: Double = 1e100
            var j = 1
            while (j < plen) {
              val newDist = Math.min(dist, differenceMatch(paletteArray(j), rr, gg, bb))
              if (dist > newDist) {
                dist = newDist
                paletteMapping(c2) = j.toByte
              }
              dist = newDist
              j += 1
            }
          }
          b += 1
        }
        g += 1
      }
      r += 1
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

  /** Analyzes pixmap for color count and frequency, building a palette with at most limit colors. */
  def analyze(pixmap: Pixmap, threshold0: Double, limit0: Int): Unit = {
    Arrays.fill(paletteArray, 0)
    Arrays.fill(paletteMapping, 0.toByte)
    var color     = 0
    val limit     = Math.min(Math.max(limit0, 2), 256)
    val threshold = threshold0 / Math.min(0.5625, Math.pow(limit + 16, 1.45) * 0.00025)
    val width     = pixmap.width.toInt
    val height    = pixmap.height.toInt
    val counts    = mutable.HashMap[Int, Int]()
    var y         = 0
    while (y < height) {
      var x = 0
      while (x < width) {
        color = pixmap.getPixel(Pixels(x), Pixels(y)) & 0xf8f8f880
        if ((color & 0x80) != 0) {
          color |= (color >>> 5 & 0x07070700) | 0xff
          counts(color) = counts.getOrElse(color, 0) + 1
        }
        x += 1
      }
      y += 1
    }
    val cs     = counts.size
    val sorted = counts.toArray.sortBy(-_._2)
    if (cs < limit) {
      var i = 1
      for ((c, _) <- sorted) {
        color = c
        paletteArray(i) = color
        paletteMapping((color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f)) = i.toByte
        i += 1
      }
      colorCount = i
      populationBias = Math.exp(-1.375 / colorCount).toFloat
    } else {
      var i = 1
      var c = 0
      boundary {
        while (i < limit && c < cs) {
          color = sorted(c)._1
          c += 1
          var tooClose = false
          boundary {
            var j = 1
            while (j < i) {
              if (differenceAnalyzing(color, paletteArray(j)) < threshold) {
                tooClose = true
                break(())
              }
              j += 1
            }
          }
          if (!tooClose) {
            paletteArray(i) = color
            paletteMapping((color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f)) = i.toByte
            i += 1
          }
        }
      }
      colorCount = i
      populationBias = Math.exp(-1.375 / colorCount).toFloat
    }

    var r = 0
    while (r < 32) {
      val rr = r << 3 | r >>> 2
      var g  = 0
      while (g < 32) {
        val gg = g << 3 | g >>> 2
        var b  = 0
        while (b < 32) {
          val c2 = r << 10 | g << 5 | b
          if (paletteMapping(c2) == 0) {
            val bb   = b << 3 | b >>> 2
            var dist = Double.MaxValue
            var i    = 1
            while (i < colorCount) {
              val newDist = Math.min(dist, differenceAnalyzing(paletteArray(i), rr, gg, bb))
              if (dist > newDist) {
                dist = newDist
                paletteMapping(c2) = i.toByte
              }
              dist = newDist
              i += 1
            }
          }
          b += 1
        }
        g += 1
      }
      r += 1
    }
  }

  /** Analyzes the given Pixmaps to build a palette. */
  def analyze(pixmaps: Array[Pixmap]): Unit =
    if (pixmaps != null && pixmaps.nonEmpty) analyze(pixmaps, pixmaps.length, 100, 256)

  /** Analyzes the given Pixmaps to build a palette with a specified threshold. */
  def analyze(pixmaps: Array[Pixmap], threshold: Double): Unit =
    if (pixmaps != null && pixmaps.nonEmpty) analyze(pixmaps, pixmaps.length, threshold, 256)

  /** Analyzes the given Pixmaps to build a palette with a specified threshold and limit. */
  def analyze(pixmaps: Array[Pixmap], threshold: Double, limit: Int): Unit =
    if (pixmaps != null && pixmaps.nonEmpty) analyze(pixmaps, pixmaps.length, threshold, limit)

  /** Analyzes all pixmaps for color count and frequency (as if they are one image), building a palette with at most `limit` colors. Iterates ALL pixmaps and aggregates color frequencies across
    * frames.
    */
  def analyze(pixmaps: Array[Pixmap], pixmapCount: Int, threshold0: Double, limit0: Int): Unit = {
    Arrays.fill(paletteArray, 0)
    Arrays.fill(paletteMapping, 0.toByte)
    var color     = 0
    val limit     = Math.min(Math.max(limit0, 2), 256)
    val threshold = threshold0 / Math.min(0.5625, Math.pow(limit + 16, 1.45) * 0.00025)
    val counts    = mutable.HashMap[Int, Int]()
    val reds      = new Array[Int](limit)
    val greens    = new Array[Int](limit)
    val blues     = new Array[Int](limit)
    var pi        = 0
    while (pi < pixmapCount && pi < pixmaps.length) {
      val pixmap = pixmaps(pi)
      val width  = pixmap.width.toInt
      val height = pixmap.height.toInt
      var y      = 0
      while (y < height) {
        var x = 0
        while (x < width) {
          color = pixmap.getPixel(Pixels(x), Pixels(y)) & 0xf8f8f880
          if ((color & 0x80) != 0) {
            color |= (color >>> 5 & 0x07070700) | 0xff
            counts(color) = counts.getOrElse(color, 0) + 1
          }
          x += 1
        }
        y += 1
      }
      pi += 1
    }
    val cs     = counts.size
    val sorted = counts.toArray.sortBy(-_._2)
    if (cs < limit) {
      var i = 1
      for ((c, _) <- sorted) {
        color = c
        paletteArray(i) = color
        paletteMapping((color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f)) = i.toByte
        reds(i) = color >>> 24
        greens(i) = color >>> 16 & 255
        blues(i) = color >>> 8 & 255
        i += 1
      }
      colorCount = i
      populationBias = Math.exp(-1.375 / colorCount).toFloat
    } else {
      var i = 1
      var c = 0
      boundary {
        while (i < limit && c < cs) {
          color = sorted(c)._1
          c += 1
          var tooClose = false
          boundary {
            var j = 1
            while (j < i) {
              val diff = differenceAnalyzing(color, paletteArray(j))
              if (diff < threshold) {
                tooClose = true
                break(())
              }
              j += 1
            }
          }
          if (!tooClose) {
            paletteArray(i) = color
            paletteMapping((color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f)) = i.toByte
            reds(i) = color >>> 24
            greens(i) = color >>> 16 & 255
            blues(i) = color >>> 8 & 255
            i += 1
          }
        }
      }
      colorCount = i
      populationBias = Math.exp(-1.375 / colorCount).toFloat
    }

    var r = 0
    while (r < 32) {
      val rr = r << 3 | r >>> 2
      var g  = 0
      while (g < 32) {
        val gg = g << 3 | g >>> 2
        var b  = 0
        while (b < 32) {
          val c2 = r << 10 | g << 5 | b
          if (paletteMapping(c2) == 0) {
            val bb   = b << 3 | b >>> 2
            var dist = Double.MaxValue
            var i    = 1
            while (i < colorCount) {
              val newDist = Math.min(dist, differenceAnalyzing(reds(i), greens(i), blues(i), rr, gg, bb))
              if (dist > newDist) {
                dist = newDist
                paletteMapping(c2) = i.toByte
              }
              dist = newDist
              i += 1
            }
          }
          b += 1
        }
        g += 1
      }
      r += 1
    }
  }

  /** Fast analysis of a Pixmap for palette generation. Uses neighbor-propagation instead of full RGB555 scan. */
  def analyzeFast(pixmap: Pixmap, threshold0: Double, limit0: Int): Unit = {
    Arrays.fill(paletteArray, 0)
    Arrays.fill(paletteMapping, 0.toByte)
    var color     = 0
    val limit     = Math.min(Math.max(limit0, 2), 256)
    val threshold = threshold0 / Math.min(0.5625, Math.pow(limit + 16, 1.45) * 0.00025)
    val width     = pixmap.width.toInt
    val height    = pixmap.height.toInt
    val counts    = mutable.HashMap[Int, Int]()
    var y         = 0
    while (y < height) {
      var x = 0
      while (x < width) {
        color = pixmap.getPixel(Pixels(x), Pixels(y)) & 0xf8f8f880
        if ((color & 0x80) != 0) {
          color |= (color >>> 5 & 0x07070700) | 0xff
          counts(color) = counts.getOrElse(color, 0) + 1
        }
        x += 1
      }
      y += 1
    }
    val cs     = counts.size
    val sorted = counts.toArray.sortBy(-_._2)
    if (cs < limit) {
      var i = 1
      for ((c, _) <- sorted) {
        color = c
        paletteArray(i) = color
        paletteMapping((color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f)) = i.toByte
        i += 1
      }
      colorCount = i
      populationBias = Math.exp(-1.375 / colorCount).toFloat
    } else {
      var i = 1
      var c = 0
      boundary {
        while (i < limit && c < cs) {
          color = sorted(c)._1
          c += 1
          var tooClose = false
          boundary {
            var j = 1
            while (j < i) {
              if (differenceAnalyzing(color, paletteArray(j)) < threshold) {
                tooClose = true
                break(())
              }
              j += 1
            }
          }
          if (!tooClose) {
            paletteArray(i) = color
            paletteMapping((color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f)) = i.toByte
            i += 1
          }
        }
      }
      colorCount = i
      populationBias = Math.exp(-1.375 / colorCount).toFloat
    }

    if (colorCount <= 1) { return }
    var numUnassigned = 1
    var iterations    = 0
    val buffer        = Arrays.copyOf(paletteMapping, 0x8000)
    while (numUnassigned != 0) {
      numUnassigned = 0
      var r = 0
      while (r < 32) {
        var g = 0
        while (g < 32) {
          var b = 0
          while (b < 32) {
            val c2 = r << 10 | g << 5 | b
            if (buffer(c2) == 0) {
              var bt: Byte = 0
              if (iterations != 2) {
                iterations += 1
                if (b < 31 && { bt = paletteMapping(c2 + 1); bt } != 0) buffer(c2) = bt
                else if (g < 31 && { bt = paletteMapping(c2 + 32); bt } != 0) buffer(c2) = bt
                else if (r < 31 && { bt = paletteMapping(c2 + 1024); bt } != 0) buffer(c2) = bt
                else if (b > 0 && { bt = paletteMapping(c2 - 1); bt } != 0) buffer(c2) = bt
                else if (g > 0 && { bt = paletteMapping(c2 - 32); bt } != 0) buffer(c2) = bt
                else if (r > 0 && { bt = paletteMapping(c2 - 1024); bt } != 0) buffer(c2) = bt
                else numUnassigned += 1
              } else {
                iterations = 0
                if (b < 31 && { bt = paletteMapping(c2 + 1); bt } != 0) buffer(c2) = bt
                else if (g < 31 && { bt = paletteMapping(c2 + 32); bt } != 0) buffer(c2) = bt
                else if (r < 31 && { bt = paletteMapping(c2 + 1024); bt } != 0) buffer(c2) = bt
                else if (b > 0 && { bt = paletteMapping(c2 - 1); bt } != 0) buffer(c2) = bt
                else if (g > 0 && { bt = paletteMapping(c2 - 32); bt } != 0) buffer(c2) = bt
                else if (r > 0 && { bt = paletteMapping(c2 - 1024); bt } != 0) buffer(c2) = bt
                else if (b < 31 && g < 31 && { bt = paletteMapping(c2 + 1 + 32); bt } != 0) buffer(c2) = bt
                else if (b < 31 && r < 31 && { bt = paletteMapping(c2 + 1 + 1024); bt } != 0) buffer(c2) = bt
                else if (g < 31 && r < 31 && { bt = paletteMapping(c2 + 32 + 1024); bt } != 0) buffer(c2) = bt
                else if (b > 0 && g > 0 && { bt = paletteMapping(c2 - 1 - 32); bt } != 0) buffer(c2) = bt
                else if (b > 0 && r > 0 && { bt = paletteMapping(c2 - 1 - 1024); bt } != 0) buffer(c2) = bt
                else if (g > 0 && r > 0 && { bt = paletteMapping(c2 - 32 - 1024); bt } != 0) buffer(c2) = bt
                else if (b < 31 && g > 0 && { bt = paletteMapping(c2 + 1 - 32); bt } != 0) buffer(c2) = bt
                else if (b < 31 && r > 0 && { bt = paletteMapping(c2 + 1 - 1024); bt } != 0) buffer(c2) = bt
                else if (g < 31 && r > 0 && { bt = paletteMapping(c2 + 32 - 1024); bt } != 0) buffer(c2) = bt
                else if (b > 0 && g < 31 && { bt = paletteMapping(c2 - 1 + 32); bt } != 0) buffer(c2) = bt
                else if (b > 0 && r < 31 && { bt = paletteMapping(c2 - 1 + 1024); bt } != 0) buffer(c2) = bt
                else if (g > 0 && r < 31 && { bt = paletteMapping(c2 - 32 + 1024); bt } != 0) buffer(c2) = bt
                else numUnassigned += 1
              }
            }
            b += 1
          }
          g += 1
        }
        r += 1
      }
      System.arraycopy(buffer, 0, paletteMapping, 0, 0x8000)
    }
  }

  /** Sets an exact palette from an array of Color objects. */
  def exact(colorPalette: Array[Color]): Unit =
    exact(colorPalette, colorPalette.length)

  /** Builds the palette information from Color objects, up to 256 or limit, whichever is less. */
  def exact(colorPalette: Array[Color], limit: Int): Unit = {
    if (colorPalette == null || colorPalette.length < 2 || limit < 2) {
      exact(PaletteReducer.SNUGGLY, ConstantData.ENCODED_SNUGGLY)
      return // safe in this context -- simple guard clause
    }
    Arrays.fill(paletteArray, 0)
    Arrays.fill(paletteMapping, 0.toByte)
    val plen = Math.min(Math.min(256, colorPalette.length), limit)
    colorCount = plen
    populationBias = Math.exp(-1.375 / colorCount).toFloat
    var color = 0
    var i     = 0
    while (i < plen) {
      color = Color.rgba8888(colorPalette(i))
      paletteArray(i) = color
      paletteMapping((color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f)) = i.toByte
      i += 1
    }
    var rr = 0
    var gg = 0
    var bb = 0
    var r  = 0
    while (r < 32) {
      rr = r << 3 | r >>> 2
      var g = 0
      while (g < 32) {
        gg = g << 3 | g >>> 2
        var b = 0
        while (b < 32) {
          val c2 = r << 10 | g << 5 | b
          if (paletteMapping(c2) == 0) {
            bb = b << 3 | b >>> 2
            var dist: Double = 0x7fffffff.toDouble
            var j = 1
            while (j < plen) {
              val newDist = Math.min(dist, differenceMatch(paletteArray(j), rr, gg, bb))
              if (dist > newDist) {
                dist = newDist
                paletteMapping(c2) = j.toByte
              }
              dist = newDist
              j += 1
            }
          }
          b += 1
        }
        g += 1
      }
      r += 1
    }
  }

  /** Analyzes the given Pixmap with hue-wise segmentation. Builds separate frequency maps per hue segment, using a distinct algorithm that segments colors by hue and lightness to build a
    * representative palette.
    */
  def analyzeHueWise(pixmap: Pixmap, threshold0: Double, limit0: Int): Unit = {
    Arrays.fill(paletteArray, 0)
    Arrays.fill(paletteMapping, 0.toByte)
    var color     = 0
    val limit     = Math.min(Math.max(limit0, 3), 256)
    var threshold = threshold0 / (Math.pow(limit, 1.35) * 0.00005375)
    val width     = pixmap.width.toInt
    val height    = pixmap.height.toInt
    val counts    = mutable.HashMap[Int, Int]()
    val enc       = new mutable.ArrayBuffer[Int](width * height)
    var y         = 0
    while (y < height) {
      var x = 0
      while (x < width) {
        color = pixmap.getPixel(Pixels(x), Pixels(y)) & 0xf8f8f880
        if ((color & 0x80) != 0) {
          color |= (color >>> 5 & 0x07070700) | 0xff
          counts(color) = counts.getOrElse(color, 0) + 1
          if (((x & y) * 5 & 31) < 3) {
            enc += PaletteReducer.shrink(color)
          }
        }
        x += 1
      }
      y += 1
    }
    val cs = counts.size
    if (cs < limit) {
      val sorted = counts.toArray.sortBy(-_._2)
      var i      = 1
      for ((c, _) <- sorted) {
        color = c
        paletteArray(i) = color
        paletteMapping((color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f)) = i.toByte
        i += 1
      }
      colorCount = i
      populationBias = Math.exp(-1.375 / colorCount).toFloat
    } else {
      // generate colors using hue-segmented algorithm
      val ei   = enc.toArray
      val encs = ei.length
      PaletteReducer.sortInts(ei, 0, encs, PaletteReducer.hueComparator)
      paletteArray(1) = -1 // white
      paletteArray(2) = 255 // black
      var i           = 3
      var segments    = (Math.min(encs, limit - 3) + 1) >> 1
      var e           = 0
      var lightPieces = Math.ceil(Math.log(limit.toDouble))
      boundary {
        while (i < limit) {
          val ePrev = e
          e %= encs
          if (e < ePrev) {
            // wrapped around
            segments += 1
            lightPieces += 1
            threshold *= 0.9
          }
          val segStart = e
          val segEnd   = Math.min(segStart + Math.ceil(encs / segments.toDouble).toInt, encs)
          val segLen   = segEnd - segStart
          PaletteReducer.sortInts(ei, segStart, segLen, PaletteReducer.lightnessComparator)
          var li = 0
          while (li < lightPieces && li < segLen && i < limit) {
            val end = Math.min(encs, e + Math.ceil(segLen / lightPieces).toInt)
            val len = end - e

            var totalL = 0.0f
            var totalA = 0.0f
            var totalB = 0.0f
            while (e < end) {
              val index = ei(e)
              totalL += PaletteReducer.OKLAB(0)(index)
              totalA += PaletteReducer.OKLAB(1)(index)
              totalB += PaletteReducer.OKLAB(2)(index)
              e += 1
            }
            totalA /= len
            totalB /= len
            color = PaletteReducer.oklabToRGB(
              OtherMath.barronSpline(totalL / len, 3f, 0.5f),
              totalA,
              totalB,
              1f
            )

            var tooClose = false
            boundary {
              var j = 3
              while (j < i) {
                if (differenceHW(color, paletteArray(j)) < threshold) {
                  tooClose = true
                  break(())
                }
                j += 1
              }
            }
            if (!tooClose) {
              paletteArray(i) = color
              paletteMapping((color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f)) = i.toByte
              i += 1
            }
            li += 1
          }
        }
      }
      colorCount = i
      populationBias = Math.exp(-1.375 / colorCount).toFloat
    }

    var r = 0
    while (r < 32) {
      val rr = r << 3 | r >>> 2
      var g  = 0
      while (g < 32) {
        val gg = g << 3 | g >>> 2
        var b  = 0
        while (b < 32) {
          val c2 = r << 10 | g << 5 | b
          if (paletteMapping(c2) == 0) {
            val bb   = b << 3 | b >>> 2
            var dist = Double.MaxValue
            var i    = 1
            while (i < colorCount) {
              val newDist = Math.min(dist, differenceHW(paletteArray(i), rr, gg, bb))
              if (dist > newDist) {
                dist = newDist
                paletteMapping(c2) = i.toByte
              }
              dist = newDist
              i += 1
            }
          }
          b += 1
        }
        g += 1
      }
      r += 1
    }
  }

  /** Analyzes the given Pixmaps with hue-wise segmentation. */
  def analyzeHueWise(pixmaps: Array[Pixmap]): Unit =
    if (pixmaps != null && pixmaps.nonEmpty) analyzeHueWise(pixmaps, pixmaps.length, 100, 256)

  /** Analyzes the given Pixmaps with hue-wise segmentation and a specified threshold. */
  def analyzeHueWise(pixmaps: Array[Pixmap], threshold: Double): Unit =
    if (pixmaps != null && pixmaps.nonEmpty) analyzeHueWise(pixmaps, pixmaps.length, threshold, 256)

  /** Analyzes the given Pixmaps with hue-wise segmentation, threshold, and limit. */
  def analyzeHueWise(pixmaps: Array[Pixmap], threshold: Double, limit: Int): Unit =
    if (pixmaps != null && pixmaps.nonEmpty) analyzeHueWise(pixmaps, pixmaps.length, threshold, limit)

  /** Analyzes all pixmaps for color count and frequency with hue-wise segmentation, aggregating color frequencies across ALL frames. Uses separate hue and lightness segments to build a representative
    * palette.
    */
  def analyzeHueWise(pixmaps: Array[Pixmap], pixmapCount: Int, threshold0: Double, limit0: Int): Unit = {
    Arrays.fill(paletteArray, 0)
    Arrays.fill(paletteMapping, 0.toByte)
    var color     = 0
    val limit     = Math.min(Math.max(limit0, 3), 256)
    var threshold = threshold0 / (Math.pow(limit, 1.35) * 0.00005375)
    val w0        = pixmaps(0).width.toInt
    val h0        = pixmaps(0).height.toInt
    val counts    = mutable.HashMap[Int, Int]()
    val enc       = new mutable.ArrayBuffer[Int](w0 * h0 * pixmapCount / 10)
    val reds      = new Array[Int](limit)
    val greens    = new Array[Int](limit)
    val blues     = new Array[Int](limit)
    var pi        = 0
    while (pi < pixmapCount && pi < pixmaps.length) {
      val pixmap = pixmaps(pi)
      val width  = pixmap.width.toInt
      val height = pixmap.height.toInt
      var y0     = 0
      while (y0 < height) {
        var x = 0
        while (x < width) {
          color = pixmap.getPixel(Pixels(x), Pixels(y0)) & 0xf8f8f880
          if ((color & 0x80) != 0) {
            color |= (color >>> 5 & 0x07070700) | 0xff
            counts(color) = counts.getOrElse(color, 0) + 1
            if (((x & y0) * 5 - pi & 31) < 3) {
              enc += PaletteReducer.shrink(color)
            }
          }
          x += 1
        }
        y0 += 1
      }
      pi += 1
    }
    val cs = counts.size
    if (cs < limit) {
      val sorted = counts.toArray.sortBy(-_._2)
      var i      = 1
      for ((c, _) <- sorted) {
        color = c
        paletteArray(i) = color
        paletteMapping((color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f)) = i.toByte
        reds(i) = color >>> 24
        greens(i) = color >>> 16 & 255
        blues(i) = color >>> 8 & 255
        i += 1
      }
      colorCount = i
    } else {
      // generate colors using hue-segmented algorithm
      val ei   = enc.toArray
      val encs = ei.length
      PaletteReducer.sortInts(ei, 0, encs, PaletteReducer.hueComparator)
      paletteArray(1) = -1 // white
      reds(1) = 255; greens(1) = 255; blues(1) = 255
      paletteArray(2) = 255 // black
      reds(2) = 0; greens(2) = 0; blues(2) = 0
      var i           = 3
      var segments    = (Math.min(encs, limit - 3) + 1) >> 1
      var e           = 0
      var lightPieces = Math.ceil(Math.log(limit.toDouble))
      boundary {
        while (i < limit) {
          val ePrev = e
          e %= encs
          if (e < ePrev) {
            // wrapped around
            segments += 1
            lightPieces += 1
            threshold *= 0.9
          }
          val segStart = e
          val segEnd   = Math.min(segStart + Math.ceil(encs / segments.toDouble).toInt, encs)
          val segLen   = segEnd - segStart
          PaletteReducer.sortInts(ei, segStart, segLen, PaletteReducer.lightnessComparator)
          var li = 0
          while (li < lightPieces && li < segLen && i < limit) {
            val end = Math.min(encs, e + Math.ceil(segLen / lightPieces).toInt)
            val len = end - e

            var totalL = 0.0f
            var totalA = 0.0f
            var totalB = 0.0f
            while (e < end) {
              val index = ei(e)
              totalL += PaletteReducer.OKLAB(0)(index)
              totalA += PaletteReducer.OKLAB(1)(index)
              totalB += PaletteReducer.OKLAB(2)(index)
              e += 1
            }
            totalA /= len
            totalB /= len
            color = PaletteReducer.oklabToRGB(
              OtherMath.barronSpline(totalL / len, 3f, 0.5f),
              totalA,
              totalB,
              1f
            )

            var tooClose = false
            boundary {
              var j = 3
              while (j < i) {
                if (differenceHW(color, paletteArray(j)) < threshold) {
                  tooClose = true
                  break(())
                }
                j += 1
              }
            }
            if (!tooClose) {
              paletteArray(i) = color
              paletteMapping((color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f)) = i.toByte
              reds(i) = color >>> 24
              greens(i) = color >>> 16 & 255
              blues(i) = color >>> 8 & 255
              i += 1
            }
            li += 1
          }
        }
      }
      colorCount = i
    }
    populationBias = Math.exp(-1.375 / colorCount).toFloat

    var r = 0
    while (r < 32) {
      val rr = r << 3 | r >>> 2
      var g  = 0
      while (g < 32) {
        val gg = g << 3 | g >>> 2
        var b  = 0
        while (b < 32) {
          val c2 = r << 10 | g << 5 | b
          if (paletteMapping(c2) == 0) {
            val bb   = b << 3 | b >>> 2
            var dist = Double.MaxValue
            var i    = 1
            while (i < colorCount) {
              val newDist = Math.min(dist, differenceHW(reds(i), greens(i), blues(i), rr, gg, bb))
              if (dist > newDist) {
                dist = newDist
                paletteMapping(c2) = i.toByte
              }
              dist = newDist
              i += 1
            }
          }
          b += 1
        }
        g += 1
      }
      r += 1
    }
  }

  /** Writes preload data for this palette reducer to a file. */
  def writePreloadFile(file: FileHandle): Unit = {
    val out = file.write(false)
    try
      out.write(paletteMapping, 0, 0x8000)
    finally
      out.close()
  }

  /** Loads preload data from a file, returning the paletteMapping byte array. */
  def loadPreloadFile(file: FileHandle): Array[Byte] = {
    val data = new Array[Byte](0x8000)
    val in   = file.read()
    try {
      var offset    = 0
      var remaining = 0x8000
      while (remaining > 0) {
        val read = in.read(data, offset, remaining)
        if (read <= 0) { remaining = 0 }
        else { offset += read; remaining -= read }
      }
    } finally
      in.close()
    data
  }

  /** Given a non-null Pixmap, finds the up-to-255 most-frequently-used colors and returns them as an array of RGBA8888 ints. */
  def colorsFrom(pixmap: Pixmap): Array[Int] =
    colorsFrom(pixmap, 256)

  /** Given a non-null Pixmap, finds the up-to-`limit - 1` most-frequently-used colors. */
  def colorsFrom(pixmap: Pixmap, limit: Int): Array[Int] = {
    val w      = pixmap.width.toInt
    val h      = pixmap.height.toInt
    val counts = mutable.HashMap[Int, Int]()
    var y      = 0
    while (y < h) {
      var x = 0
      while (x < w) {
        var color = pixmap.getPixel(Pixels(x), Pixels(y)) & 0xf8f8f880
        if ((color & 0x80) != 0) {
          color |= (color >>> 5 & 0x07070700) | 0xff
          counts(color) = counts.getOrElse(color, 0) + 1
        }
        x += 1
      }
      y += 1
    }
    val sorted     = counts.toArray.sortBy(-_._2)
    val colorCount = Math.min(limit, sorted.length + 1)
    val colorArray = new Array[Int](colorCount)
    var i          = 1
    boundary {
      for ((color, _) <- sorted) {
        colorArray(i) = color
        i += 1
        if (i >= limit) break(())
      }
    }
    colorArray
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

  // === Color difference methods ===
  // All difference methods are overrideable for subclasses like FastPalette and QualityPalette.

  private def oklabDiff(idx1: Int, idx2: Int, lWeight: Double): Double = {
    val dL = (PaletteReducer.OKLAB(0)(idx1) - PaletteReducer.OKLAB(0)(idx2)) * 512.0
    val dA = (PaletteReducer.OKLAB(1)(idx1) - PaletteReducer.OKLAB(1)(idx2)) * 512.0
    val dB = (PaletteReducer.OKLAB(2)(idx1) - PaletteReducer.OKLAB(2)(idx2)) * 512.0
    dL * dL * lWeight + dA * dA + dB * dB
  }

  private def rgb555(r: Int, g: Int, b: Int): Int =
    (r << 7 & 0x7c00) | (g << 2 & 0x3e0) | (b >>> 3)

  def differenceMatch(color1: Int, color2: Int): Double = {
    if (((color1 ^ color2) & 0x80) == 0x80) return Double.MaxValue
    oklabDiff(PaletteReducer.shrink(color1), PaletteReducer.shrink(color2), 1.7)
  }

  def differenceMatch(rgba1: Int, r2: Int, g2: Int, b2: Int): Double = {
    if ((rgba1 & 0x80) == 0) return Double.MaxValue
    oklabDiff(PaletteReducer.shrink(rgba1), rgb555(r2, g2, b2), 1.7)
  }

  def differenceMatch(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Double =
    oklabDiff(rgb555(r1, g1, b1), rgb555(r2, g2, b2), 1.7)

  def differenceAnalyzing(color1: Int, color2: Int): Double = {
    if (((color1 ^ color2) & 0x80) == 0x80) return Double.MaxValue
    oklabDiff(PaletteReducer.shrink(color1), PaletteReducer.shrink(color2), 1.0)
  }

  def differenceAnalyzing(rgba1: Int, r2: Int, g2: Int, b2: Int): Double = {
    if ((rgba1 & 0x80) == 0) return Double.MaxValue
    oklabDiff(PaletteReducer.shrink(rgba1), rgb555(r2, g2, b2), 1.0)
  }

  def differenceAnalyzing(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Double =
    oklabDiff(rgb555(r1, g1, b1), rgb555(r2, g2, b2), 1.0)

  def differenceHW(color1: Int, color2: Int): Double = {
    if (((color1 ^ color2) & 0x80) == 0x80) return Double.MaxValue
    oklabDiff(PaletteReducer.shrink(color1), PaletteReducer.shrink(color2), 1.0)
  }

  def differenceHW(rgba1: Int, r2: Int, g2: Int, b2: Int): Double = {
    if ((rgba1 & 0x80) == 0) return Double.MaxValue
    oklabDiff(PaletteReducer.shrink(rgba1), rgb555(r2, g2, b2), 1.0)
  }

  def differenceHW(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Double =
    oklabDiff(rgb555(r1, g1, b1), rgb555(r2, g2, b2), 1.0)

  /** Blends two RGBA8888 colors in Oklab space. */
  def blend(rgba1: Int, rgba2: Int, preference: Float): Int = {
    val a1 = rgba1 & 255
    val a2 = rgba2 & 255
    if ((a1 & 0x80) == 0) rgba2
    else if ((a2 & 0x80) == 0) rgba1
    else {
      val s1 = PaletteReducer.shrink(rgba1)
      val s2 = PaletteReducer.shrink(rgba2)
      val L  = PaletteReducer.OKLAB(0)(s1) + (PaletteReducer.OKLAB(0)(s2) - PaletteReducer.OKLAB(0)(s1)) * preference
      val A  = PaletteReducer.OKLAB(1)(s1) + (PaletteReducer.OKLAB(1)(s2) - PaletteReducer.OKLAB(1)(s1)) * preference
      val B  = PaletteReducer.OKLAB(2)(s1) + (PaletteReducer.OKLAB(2)(s2) - PaletteReducer.OKLAB(2)(s1)) * preference
      PaletteReducer.oklabToRGB(L, A, B, (a1 + (a2 - a1) * preference) * (1f / 255f))
    }
  }

  /** Given by Joel Yliluoma. Must not be modified. */
  val thresholdMatrix8: Array[Int] = Array(
    0, 4, 2, 6, 3, 7, 1, 5
  )

  // === Error array management ===

  /** Ensures that the error diffusion arrays are at least `lineLen` in length. */
  private def ensureErrorArrays(lineLen: Int): (Array[Float], Array[Float], Array[Float], Array[Float], Array[Float], Array[Float]) = {
    if (curErrorRedFloats == null || curErrorRedFloats.nn.length < lineLen) {
      curErrorRedFloats = new Array[Float](lineLen)
      nextErrorRedFloats = new Array[Float](lineLen)
      curErrorGreenFloats = new Array[Float](lineLen)
      nextErrorGreenFloats = new Array[Float](lineLen)
      curErrorBlueFloats = new Array[Float](lineLen)
      nextErrorBlueFloats = new Array[Float](lineLen)
    } else {
      Arrays.fill(nextErrorRedFloats.nn, 0, lineLen, 0f)
      Arrays.fill(nextErrorGreenFloats.nn, 0, lineLen, 0f)
      Arrays.fill(nextErrorBlueFloats.nn, 0, lineLen, 0f)
    }
    (curErrorRedFloats.nn, nextErrorRedFloats.nn, curErrorGreenFloats.nn, nextErrorGreenFloats.nn, curErrorBlueFloats.nn, nextErrorBlueFloats.nn)
  }

  // === Dithering / Reduction methods ===

  /** Reduces the given Pixmap to the palette this knows, using the default dithering algorithm (Wren). */
  def reduce(pixmap: Pixmap): Pixmap = reduceWren(pixmap)

  /** Uses the given [[DitherAlgorithm]] to decide how to dither `pixmap`. */
  def reduce(pixmap: Pixmap, dither: DitherAlgorithm): Pixmap =
    dither match {
      case DitherAlgorithm.NONE           => reduceSolid(pixmap)
      case DitherAlgorithm.GRADIENT_NOISE => reduceJimenez(pixmap)
      case DitherAlgorithm.ADDITIVE       => reduceAdditive(pixmap)
      case DitherAlgorithm.PATTERN        => reduceKnoll(pixmap)
      case DitherAlgorithm.CHAOTIC_NOISE  => reduceChaoticNoise(pixmap)
      case DitherAlgorithm.DIFFUSION      => reduceFloydSteinberg(pixmap)
      case DitherAlgorithm.BLUE_NOISE     => reduceBlueNoise(pixmap)
      case DitherAlgorithm.BLUNT          => reduceBlunt(pixmap)
      case DitherAlgorithm.BANTER         => reduceBanter(pixmap)
      case DitherAlgorithm.SCATTER        => reduceScatter(pixmap)
      case DitherAlgorithm.ROBERTS        => reduceRoberts(pixmap)
      case DitherAlgorithm.WOVEN          => reduceWoven(pixmap)
      case DitherAlgorithm.DODGY          => reduceDodgy(pixmap)
      case DitherAlgorithm.LOAF           => reduceLoaf(pixmap)
      case DitherAlgorithm.NEUE           => reduceNeue(pixmap)
      case DitherAlgorithm.BURKES         => reduceBurkes(pixmap)
      case DitherAlgorithm.OCEANIC        => reduceOceanic(pixmap)
      case DitherAlgorithm.SEASIDE        => reduceSeaside(pixmap)
      case DitherAlgorithm.GOURD          => reduceGourd(pixmap)
      case DitherAlgorithm.OVERBOARD      => reduceOverboard(pixmap)
      case DitherAlgorithm.MARTEN         => reduceMarten(pixmap)
      case DitherAlgorithm.WREN           => reduceWren(pixmap)
    }

  /** No dithering -- solid color reduction. */
  def reduceSolid(pixmap: Pixmap): Pixmap = {
    val hasTransparent = paletteArray(0) == 0
    val lineLen        = pixmap.width.toInt; val h = pixmap.height.toInt
    val oldBlending    = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    var y              = 0;
    while (y < h) {
      var px = 0;
      while (px < lineLen) {
        val color = pixmap.getPixel(Pixels(px), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(px), Pixels(y), 0)
        else {
          val rr = color >>> 24; val gg = (color >>> 16) & 0xff; val bb = (color >>> 8) & 0xff
          pixmap.drawPixel(Pixels(px), Pixels(y), paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff))
        };
        px += 1
      };
      y += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Floyd-Steinberg error-diffusion dithering. */
  def reduceFloydSteinberg(pixmap: Pixmap): Pixmap = {
    val hasTransparent                                                                          = paletteArray(0) == 0
    val lineLen                                                                                 = pixmap.width.toInt; val h                      = pixmap.height.toInt
    val (curErrorRed, nextErrorRed, curErrorGreen, nextErrorGreen, curErrorBlue, nextErrorBlue) = ensureErrorArrays(lineLen)
    val oldBlending                                                                             = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    val w1                                                                                      = _ditherStrength * 32f / populationBias; val w3 = w1 * 3f; val w5 = w1 * 5f; val w7 = w1 * 7f
    var y                                                                                       = 0;
    while (y < h) {
      val ny = y + 1
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, lineLen); System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, lineLen); System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, lineLen)
      Arrays.fill(nextErrorRed, 0, lineLen, 0f); Arrays.fill(nextErrorGreen, 0, lineLen, 0f); Arrays.fill(nextErrorBlue, 0, lineLen, 0f)
      var px = 0;
      while (px < lineLen) {
        val color = pixmap.getPixel(Pixels(px), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(px), Pixels(y), 0)
        else {
          val rr   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT(color >>> 24) + curErrorRed(px), 0f), 1023f).toInt) & 255
          val gg   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 16) & 0xff) + curErrorGreen(px), 0f), 1023f).toInt) & 255
          val bb   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 8) & 0xff) + curErrorBlue(px), 0f), 1023f).toInt) & 255
          val used = paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff)
          pixmap.drawPixel(Pixels(px), Pixels(y), used)
          // 0x1p-8f = 0.00390625f
          val rdiff = Math.min(Math.max(0.00390625f * ((color >>> 24) - (used >>> 24)), -1f), 1f)
          val gdiff = Math.min(Math.max(0.00390625f * (((color >>> 16) & 255) - ((used >>> 16) & 255)), -1f), 1f)
          val bdiff = Math.min(Math.max(0.00390625f * (((color >>> 8) & 255) - ((used >>> 8) & 255)), -1f), 1f)
          if (px < lineLen - 1) { curErrorRed(px + 1) += rdiff * w7; curErrorGreen(px + 1) += gdiff * w7; curErrorBlue(px + 1) += bdiff * w7 }
          if (ny < h) {
            if (px > 0) { nextErrorRed(px - 1) += rdiff * w3; nextErrorGreen(px - 1) += gdiff * w3; nextErrorBlue(px - 1) += bdiff * w3 }
            if (px < lineLen - 1) { nextErrorRed(px + 1) += rdiff * w1; nextErrorGreen(px + 1) += gdiff * w1; nextErrorBlue(px + 1) += bdiff * w1 }
            nextErrorRed(px) += rdiff * w5; nextErrorGreen(px) += gdiff * w5; nextErrorBlue(px) += bdiff * w5
          }
        };
        px += 1
      };
      y += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Sierra Lite error-diffusion dithering. */
  def reduceSierraLite(pixmap: Pixmap): Pixmap = {
    val hasTransparent                                                                          = paletteArray(0) == 0
    val lineLen                                                                                 = pixmap.width.toInt; val h                = pixmap.height.toInt
    val (curErrorRed, nextErrorRed, curErrorGreen, nextErrorGreen, curErrorBlue, nextErrorBlue) = ensureErrorArrays(lineLen)
    val oldBlending                                                                             = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    val ditherStr                                                                               = _ditherStrength * 20f; val halfDitherStr = ditherStr * 0.5f
    var y                                                                                       = 0;
    while (y < h) {
      val ny = y + 1
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, lineLen); System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, lineLen); System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, lineLen)
      Arrays.fill(nextErrorRed, 0, lineLen, 0f); Arrays.fill(nextErrorGreen, 0, lineLen, 0f); Arrays.fill(nextErrorBlue, 0, lineLen, 0f)
      var px = 0;
      while (px < lineLen) {
        val color = pixmap.getPixel(Pixels(px), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(px), Pixels(y), 0)
        else {
          val rr   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT(color >>> 24) + curErrorRed(px), 0f), 1023f).toInt) & 255
          val gg   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 16) & 0xff) + curErrorGreen(px), 0f), 1023f).toInt) & 255
          val bb   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 8) & 0xff) + curErrorBlue(px), 0f), 1023f).toInt) & 255
          val used = paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff)
          pixmap.drawPixel(Pixels(px), Pixels(y), used)
          // 0x2.4p-8f = 0.00878906f
          var rdiff = 0.00878906f * ((color >>> 24) - (used >>> 24)); var gdiff = 0.00878906f * (((color >>> 16) & 255) - ((used >>> 16) & 255));
          var bdiff = 0.00878906f * (((color >>> 8) & 255) - ((used >>> 8) & 255))
          rdiff *= 1.25f / (0.25f + Math.abs(rdiff)); gdiff *= 1.25f / (0.25f + Math.abs(gdiff)); bdiff *= 1.25f / (0.25f + Math.abs(bdiff))
          if (px < lineLen - 1) { curErrorRed(px + 1) += rdiff * ditherStr; curErrorGreen(px + 1) += gdiff * ditherStr; curErrorBlue(px + 1) += bdiff * ditherStr }
          if (ny < h) {
            if (px > 0) { nextErrorRed(px - 1) += rdiff * halfDitherStr; nextErrorGreen(px - 1) += gdiff * halfDitherStr; nextErrorBlue(px - 1) += bdiff * halfDitherStr }
            nextErrorRed(px) += rdiff * halfDitherStr; nextErrorGreen(px) += gdiff * halfDitherStr; nextErrorBlue(px) += bdiff * halfDitherStr
          }
        };
        px += 1
      };
      y += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Interleaved gradient noise dither (Jorge Jimenez). */
  def reduceJimenez(pixmap: Pixmap): Pixmap = {
    val hasTransparent = paletteArray(0) == 0
    val lineLen        = pixmap.width.toInt; val h = pixmap.height.toInt
    val oldBlending    = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    val strength       = 0.9f * Math.tanh(0.16f * _ditherStrength * Math.pow(populationBias, -7.00)).toFloat
    var y              = 0;
    while (y < h) {
      var px = 0;
      while (px < lineLen) {
        val color = pixmap.getPixel(Pixels(px), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(px), Pixels(y), 0)
        else {
          val rr = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT(color >>> 24) + ((142 * (px + 0x5f) + 79 * (y - 0x96) & 255) - 127.5f) * strength).toInt) & 255
          val gg = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT((color >>> 16) & 0xff) + ((142 * (px + 0xfa) + 79 * (y - 0xa3) & 255) - 127.5f) * strength).toInt) & 255
          val bb = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT((color >>> 8) & 0xff) + ((142 * (px + 0xa5) + 79 * (y - 0xc9) & 255) - 127.5f) * strength).toInt) & 255
          pixmap.drawPixel(Pixels(px), Pixels(y), paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff))
        };
        px += 1
      };
      y += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Additive ordered dither by Oyvind Kolas. */
  def reduceAdditive(pixmap: Pixmap): Pixmap = {
    val hasTransparent = paletteArray(0) == 0
    val lineLen        = pixmap.width.toInt; val h                                                    = pixmap.height.toInt
    val oldBlending    = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    val s              = 0.08f * _ditherStrength / Math.pow(populationBias, 8f).toFloat; val strength = s / (0.35f + s)
    var y              = 0;
    while (y < h) {
      var px = 0;
      while (px < lineLen) {
        val color = pixmap.getPixel(Pixels(px), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(px), Pixels(y), 0)
        else {
          val rr = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT(color >>> 24) + ((119 * px + 180 * y + 54 & 255) - 127.5f) * strength).toInt) & 255
          val gg = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT((color >>> 16) & 0xff) + ((119 * px + 180 * y + 81 & 255) - 127.5f) * strength).toInt) & 255
          val bb = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT((color >>> 8) & 0xff) + ((119 * px + 180 * y & 255) - 127.5f) * strength).toInt) & 255
          pixmap.drawPixel(Pixels(px), Pixels(y), paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff))
        };
        px += 1
      };
      y += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Intentionally low-fidelity 2x2 ordered dither. */
  def reduceLoaf(pixmap: Pixmap): Pixmap = {
    val hasTransparent = paletteArray(0) == 0
    val lineLen        = pixmap.width.toInt; val h = pixmap.height.toInt
    val oldBlending    = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    val strength       = Math.min(Math.max(2.5f + 5f * _ditherStrength - 5.5f * populationBias, 0f), 7.9f)
    var y              = 0;
    while (y < h) {
      var px = 0;
      while (px < lineLen) {
        val color = pixmap.getPixel(Pixels(px), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(px), Pixels(y), 0)
        else {
          val adj = (((px + y & 1) << 5) - 16) * strength
          val rr  = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT(color >>> 24) + adj, 0f), 1023f).toInt) & 255
          val gg  = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 16) & 0xff) + adj, 0f), 1023f).toInt) & 255
          val bb  = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 8) & 0xff) + adj, 0f), 1023f).toInt) & 255
          pixmap.drawPixel(Pixels(px), Pixels(y), paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff))
        };
        px += 1
      };
      y += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** 8x8 ordered dither with gamma correction. */
  def reduceGourd(pixmap: Pixmap): Pixmap = {
    val hasTransparent = paletteArray(0) == 0
    val lineLen        = pixmap.width.toInt; val h = pixmap.height.toInt
    val oldBlending    = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    val strength       = (_ditherStrength * 0.7 * Math.pow(populationBias, -5.50)).toFloat
    var i              = 0; while (i < 64) { PaletteReducer.tempThresholdMatrix(i) = Math.min(Math.max((PaletteReducer.thresholdMatrix64(i) - 31.5f) * strength, -127f), 127f); i += 1 }
    var y              = 0;
    while (y < h) {
      var px = 0;
      while (px < lineLen) {
        val color = pixmap.getPixel(Pixels(px), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(px), Pixels(y), 0)
        else {
          val idx = (px & 7) ^ (y << 3 & 56)
          val rr  = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT(color >>> 24) + PaletteReducer.tempThresholdMatrix(idx ^ 0x2e)).toInt) & 255
          val gg  = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT((color >>> 16) & 0xff) + PaletteReducer.tempThresholdMatrix(idx ^ 0x33)).toInt) & 255
          val bb  = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT((color >>> 8) & 0xff) + PaletteReducer.tempThresholdMatrix(idx ^ 0x27)).toInt) & 255
          pixmap.drawPixel(Pixels(px), Pixels(y), paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff))
        };
        px += 1
      };
      y += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Three-texture blue noise ordered dither. */
  def reduceBlunt(pixmap: Pixmap): Pixmap = {
    val hasTransparent = paletteArray(0) == 0; val w = pixmap.width.toInt; val h = pixmap.height.toInt
    val oldBlending    = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    val strength       = Math.min(Math.max(0.35f * _ditherStrength / (populationBias * populationBias * populationBias), -0.6f), 0.6f)
    var y              = 0;
    while (y < h) {
      var x = 0;
      while (x < w) {
        val color = pixmap.getPixel(Pixels(x), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(x), Pixels(y), 0)
        else {
          val adj = ((x + y << 7) & 128) - 63.5f
          val rr  = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT(color >>> 24) + (PaletteReducer.TRI_BLUE_NOISE((x + 62 & 63) << 6 | (y + 66 & 63)) + adj) * strength).toInt) & 255
          val gg  = PaletteReducer.fromLinearLUT(
            (PaletteReducer.toLinearLUT((color >>> 16) & 0xff) + (PaletteReducer.TRI_BLUE_NOISE_B((x + 31 & 63) << 6 | (y + 113 & 63)) + adj) * strength).toInt
          ) & 255
          val bb = PaletteReducer.fromLinearLUT(
            (PaletteReducer.toLinearLUT((color >>> 8) & 0xff) + (PaletteReducer.TRI_BLUE_NOISE_C((x + 71 & 63) << 6 | (y + 41 & 63)) + adj) * strength).toInt
          ) & 255
          pixmap.drawPixel(Pixels(x), Pixels(y), paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff))
        };
        x += 1
      };
      y += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Triangular-mapped 128x128 Bayer Matrix ordered dither. */
  def reduceBanter(pixmap: Pixmap): Pixmap = {
    val hasTransparent = paletteArray(0) == 0; val w = pixmap.width.toInt; val h = pixmap.height.toInt
    val oldBlending    = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    val strength       = Math.min(Math.max(0.17f * _ditherStrength * Math.pow(populationBias, -10f).toFloat, -0.95f), 0.95f)
    var y              = 0;
    while (y < h) {
      var x = 0;
      while (x < w) {
        val color = pixmap.getPixel(Pixels(x), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(x), Pixels(y), 0)
        else {
          val adj = PaletteReducer.TRI_BAYER_MATRIX_128((x & PaletteReducer.TBM_MASK) << PaletteReducer.TBM_BITS | (y & PaletteReducer.TBM_MASK)) * strength
          val rr  = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT(color >>> 24) + adj).toInt) & 255
          val gg  = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT((color >>> 16) & 0xff) + adj).toInt) & 255
          val bb  = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT((color >>> 8) & 0xff) + adj).toInt) & 255
          pixmap.drawPixel(Pixels(x), Pixels(y), paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff))
        };
        x += 1
      };
      y += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Roberts R2-sequence ordered dither with triangle wave. */
  def reduceRoberts(pixmap: Pixmap): Pixmap = {
    val hasTransparent = paletteArray(0) == 0
    val lineLen        = pixmap.width.toInt; val h = pixmap.height.toInt
    val oldBlending    = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    val str            = Math.min(48f * _ditherStrength / (populationBias * populationBias * populationBias * populationBias), 127f)
    var y              = 0;
    while (y < h) {
      var px = 0;
      while (px < lineLen) {
        val color = pixmap.getPixel(Pixels(px), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(px), Pixels(y), 0)
        else {
          val theta = (px * 0xc13fa9a9 + y * 0x91e10da5 >>> 9) * 1.1920929e-7f
          val rr    = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT(color >>> 24) + OtherMath.triangleWave(theta) * str).toInt) & 255
          val gg    = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT((color >>> 16) & 0xff) + OtherMath.triangleWave(theta + 0.209f) * str).toInt) & 255
          val bb    = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT((color >>> 8) & 0xff) + OtherMath.triangleWave(theta + 0.518f) * str).toInt) & 255
          pixmap.drawPixel(Pixels(px), Pixels(y), paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff))
        };
        px += 1
      };
      y += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Marten ordered dither -- less error for larger palettes. */
  def reduceMarten(pixmap: Pixmap): Pixmap = {
    val hasTransparent = paletteArray(0) == 0
    val lineLen        = pixmap.width.toInt; val h = pixmap.height.toInt
    val oldBlending    = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    val str            = Math.min(1100f * (_ditherStrength / Math.sqrt(colorCount.toFloat) * (1f / (populationBias * populationBias * populationBias) - 0.7f)), 127f).toFloat
    var y              = 0;
    while (y < h) {
      var px = 0;
      while (px < lineLen) {
        val color = pixmap.getPixel(Pixels(px), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(px), Pixels(y), 0)
        else {
          val theta = (px * 142 + y * 79 & 255) * 0.00390625f
          val rr    = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT(color >>> 24) + OtherMath.triangleWave(theta) * str).toInt) & 255
          val gg    = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT((color >>> 16) & 0xff) + OtherMath.triangleWave(theta + 0.382f) * str).toInt) & 255
          val bb    = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT((color >>> 8) & 0xff) + OtherMath.triangleWave(theta + 0.618f) * str).toInt) & 255
          pixmap.drawPixel(Pixels(px), Pixels(y), paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff))
        };
        px += 1
      };
      y += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Blue noise ordered dither with checkerboard. */
  def reduceBlueNoise(pixmap: Pixmap): Pixmap = {
    val hasTransparent = paletteArray(0) == 0; val w = pixmap.width.toInt; val h = pixmap.height.toInt
    val oldBlending    = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    val strength       = 0.21875f * _ditherStrength / (populationBias * populationBias)
    var y              = 0;
    while (y < h) {
      var x = 0;
      while (x < w) {
        val color = pixmap.getPixel(Pixels(x), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(x), Pixels(y), 0)
        else {
          val adj = Math.min(Math.max((PaletteReducer.TRI_BLUE_NOISE((x & 63) | (y & 63) << 6) + ((x + y & 1) << 8) - 127.5f) * strength, -100.5f), 101.5f)
          val rr  = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT(color >>> 24) + adj).toInt) & 255
          val gg  = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT((color >>> 16) & 0xff) + adj).toInt) & 255
          val bb  = PaletteReducer.fromLinearLUT((PaletteReducer.toLinearLUT((color >>> 8) & 0xff) + adj).toInt) & 255
          pixmap.drawPixel(Pixels(x), Pixels(y), paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff))
        };
        x += 1
      };
      y += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Chaotic noise dither using pseudo-random state. */
  def reduceChaoticNoise(pixmap: Pixmap): Pixmap = {
    val hasTransparent = paletteArray(0) == 0
    val lineLen        = pixmap.width.toInt; val h = pixmap.height.toInt
    val oldBlending    = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    val strength: Double = _ditherStrength * populationBias * 1.5
    var s:        Long   = 0xc13fa9a902a6328fL
    var y = 0;
    while (y < h) {
      var px = 0;
      while (px < lineLen) {
        val color = pixmap.getPixel(Pixels(px), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(px), Pixels(y), 0)
        else {
          var rr   = color >>> 24; var gg = (color >>> 16) & 0xff; var bb = (color >>> 8) & 0xff
          val used = paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff)
          var adj: Double = (PaletteReducer.TRI_BLUE_NOISE((px & 63) | (y & 63) << 6) + 0.5f) * 0.007843138f
          adj *= adj * adj
          // 0x1.8p-49 = 2.6645352591003757e-15
          adj += ((px + y & 1) - 0.5f) * 2.6645352591003757e-15 * strength *
            (((s ^ 0x9e3779b97f4a7c15L) * 0xc6bc279692b5cc83L >> 15) +
              ((~s ^ 0xdb4f0b9175ae2165L) * 0xd1b54a32d192ed03L >> 15) +
              ({ s = (s ^ rr + gg + bb) * 0xd1342543de82ef95L + 0x91e10da5c79e7b1dL; s } >> 15))
          rr = Math.min(Math.max((rr + (adj * (rr - (used >>> 24)))).toInt, 0), 0xff)
          gg = Math.min(Math.max((gg + (adj * (gg - ((used >>> 16) & 0xff)))).toInt, 0), 0xff)
          bb = Math.min(Math.max((bb + (adj * (bb - ((used >>> 8) & 0xff)))).toInt, 0), 0xff)
          pixmap.drawPixel(Pixels(px), Pixels(y), paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff))
        };
        px += 1
      };
      y += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Scatter error-diffusion dither with blue noise multiplier. */
  def reduceScatter(pixmap: Pixmap): Pixmap = {
    val hasTransparent                                                                          = paletteArray(0) == 0; val lineLen = pixmap.width.toInt; val h = pixmap.height.toInt
    val (curErrorRed, nextErrorRed, curErrorGreen, nextErrorGreen, curErrorBlue, nextErrorBlue) = ensureErrorArrays(lineLen)
    val oldBlending                                                                             = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    val w1 = Math.min(_ditherStrength * 5.5f / (populationBias * populationBias), 16f); val w3 = w1 * 3f; val w5 = w1 * 5f; val w7 = w1 * 7f
    var y  = 0;
    while (y < h) {
      val ny = y + 1
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, lineLen); System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, lineLen); System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, lineLen)
      Arrays.fill(nextErrorRed, 0, lineLen, 0f); Arrays.fill(nextErrorGreen, 0, lineLen, 0f); Arrays.fill(nextErrorBlue, 0, lineLen, 0f)
      var px = 0;
      while (px < lineLen) {
        val color = pixmap.getPixel(Pixels(px), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(px), Pixels(y), 0)
        else {
          val tbn  = PaletteReducer.TRI_BLUE_NOISE_MULTIPLIERS((px & 63) | ((y << 6) & 0xfc0))
          val er   = curErrorRed(px) * tbn; val eg = curErrorGreen(px) * tbn; val eb = curErrorBlue(px) * tbn
          val rr   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT(color >>> 24) + er, 0f), 1023f).toInt) & 255
          val gg   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 16) & 0xff) + eg, 0f), 1023f).toInt) & 255
          val bb   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 8) & 0xff) + eb, 0f), 1023f).toInt) & 255
          val used = paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff)
          pixmap.drawPixel(Pixels(px), Pixels(y), used)
          // 0x2.1p-8f = 0.0080566406f
          var rdiff = 0.0080566406f * ((color >>> 24) - (used >>> 24)); var gdiff = 0.0080566406f * (((color >>> 16) & 255) - ((used >>> 16) & 255));
          var bdiff = 0.0080566406f * (((color >>> 8) & 255) - ((used >>> 8) & 255))
          rdiff /= (0.125f + Math.abs(rdiff)); gdiff /= (0.125f + Math.abs(gdiff)); bdiff /= (0.125f + Math.abs(bdiff))
          if (px < lineLen - 1) { curErrorRed(px + 1) += rdiff * w7; curErrorGreen(px + 1) += gdiff * w7; curErrorBlue(px + 1) += bdiff * w7 }
          if (ny < h) {
            if (px > 0) { nextErrorRed(px - 1) += rdiff * w3; nextErrorGreen(px - 1) += gdiff * w3; nextErrorBlue(px - 1) += bdiff * w3 }
            if (px < lineLen - 1) { nextErrorRed(px + 1) += rdiff * w1; nextErrorGreen(px + 1) += gdiff * w1; nextErrorBlue(px + 1) += bdiff * w1 }
            nextErrorRed(px) += rdiff * w5; nextErrorGreen(px) += gdiff * w5; nextErrorBlue(px) += bdiff * w5
          }
        };
        px += 1
      };
      y += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Neue error-diffusion dither with blue noise additive adjustment. */
  def reduceNeue(pixmap: Pixmap): Pixmap = {
    val hasTransparent                                                                          = paletteArray(0) == 0; val lineLen = pixmap.width.toInt; val h = pixmap.height.toInt
    val (curErrorRed, nextErrorRed, curErrorGreen, nextErrorGreen, curErrorBlue, nextErrorBlue) = ensureErrorArrays(lineLen)
    val oldBlending                                                                             = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    val w1                                                                                      = _ditherStrength * 8f; val w3      = w1 * 3f; val w5           = w1 * 5f; val w7 = w1 * 7f
    val strength                                                                                = 70f * _ditherStrength / (populationBias * populationBias * populationBias)
    val limit                                                                                   = Math.min(127f, Math.pow(80.0, 1.635 - populationBias).toFloat)
    var py                                                                                      = 0;
    while (py < h) {
      val ny = py + 1
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, lineLen); System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, lineLen); System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, lineLen)
      Arrays.fill(nextErrorRed, 0, lineLen, 0f); Arrays.fill(nextErrorGreen, 0, lineLen, 0f); Arrays.fill(nextErrorBlue, 0, lineLen, 0f)
      var px = 0;
      while (px < lineLen) {
        val color = pixmap.getPixel(Pixels(px), Pixels(py))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(px), Pixels(py), 0)
        else {
          val adj  = Math.min(Math.max((PaletteReducer.TRI_BLUE_NOISE((px & 63) | (py & 63) << 6) + 0.5f) * 0.005f * strength, -limit), limit)
          val er   = adj + curErrorRed(px); val eg = adj + curErrorGreen(px); val eb = adj + curErrorBlue(px)
          val rr   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT(color >>> 24) + er, 0f), 1023f).toInt) & 255
          val gg   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 16) & 0xff) + eg, 0f), 1023f).toInt) & 255
          val bb   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 8) & 0xff) + eb, 0f), 1023f).toInt) & 255
          val used = paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff)
          pixmap.drawPixel(Pixels(px), Pixels(py), used)
          // 0x2.Ep-8f = 0.01123047f
          var rdiff = 0.01123047f * ((color >>> 24) - (used >>> 24)); var gdiff = 0.01123047f * (((color >>> 16) & 255) - ((used >>> 16) & 255));
          var bdiff = 0.01123047f * (((color >>> 8) & 255) - ((used >>> 8) & 255))
          rdiff *= 1.25f / (0.25f + Math.abs(rdiff)); gdiff *= 1.25f / (0.25f + Math.abs(gdiff)); bdiff *= 1.25f / (0.25f + Math.abs(bdiff))
          if (px < lineLen - 1) { curErrorRed(px + 1) += rdiff * w7; curErrorGreen(px + 1) += gdiff * w7; curErrorBlue(px + 1) += bdiff * w7 }
          if (ny < h) {
            if (px > 0) { nextErrorRed(px - 1) += rdiff * w3; nextErrorGreen(px - 1) += gdiff * w3; nextErrorBlue(px - 1) += bdiff * w3 }
            if (px < lineLen - 1) { nextErrorRed(px + 1) += rdiff * w1; nextErrorGreen(px + 1) += gdiff * w1; nextErrorBlue(px + 1) += bdiff * w1 }
            nextErrorRed(px) += rdiff * w5; nextErrorGreen(px) += gdiff * w5; nextErrorBlue(px) += bdiff * w5
          }
        };
        px += 1
      };
      py += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Burkes error-diffusion dither. */
  def reduceBurkes(pixmap: Pixmap): Pixmap = {
    val hasTransparent = paletteArray(0) == 0; val lineLen                                         = pixmap.width.toInt; val h = pixmap.height.toInt
    val s              = 0.13f * _ditherStrength / (populationBias * populationBias); val strength = s * 0.58f / (0.3f + s)
    val (curErrorRed, nextErrorRed, curErrorGreen, nextErrorGreen, curErrorBlue, nextErrorBlue) = ensureErrorArrays(lineLen)
    val oldBlending                                                                             = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    var py                                                                                      = 0;
    while (py < h) {
      val ny = py + 1
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, lineLen); System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, lineLen); System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, lineLen)
      Arrays.fill(nextErrorRed, 0, lineLen, 0f); Arrays.fill(nextErrorGreen, 0, lineLen, 0f); Arrays.fill(nextErrorBlue, 0, lineLen, 0f)
      var px = 0;
      while (px < lineLen) {
        val color = pixmap.getPixel(Pixels(px), Pixels(py))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(px), Pixels(py), 0)
        else {
          val rr   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT(color >>> 24) + curErrorRed(px), 0f), 1023f).toInt) & 255
          val gg   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 16) & 0xff) + curErrorGreen(px), 0f), 1023f).toInt) & 255
          val bb   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 8) & 0xff) + curErrorBlue(px), 0f), 1023f).toInt) & 255
          val used = paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff)
          pixmap.drawPixel(Pixels(px), Pixels(py), used)
          val r1 = ((color >>> 24) - (used >>> 24)) * strength; val g1 = (((color >>> 16) & 255) - ((used >>> 16) & 255)) * strength; val b1 = (((color >>> 8) & 255) - ((used >>> 8) & 255)) * strength
          val r2 = r1 + r1; val g2 = g1 + g1; val b2 = b1 + b1; val r4 = r2 + r2; val g4 = g2 + g2; val b4 = b2 + b2
          if (px < lineLen - 1) {
            curErrorRed(px + 1) += r4; curErrorGreen(px + 1) += g4; curErrorBlue(px + 1) += b4
            if (px < lineLen - 2) { curErrorRed(px + 2) += r2; curErrorGreen(px + 2) += g2; curErrorBlue(px + 2) += b2 }
          }
          if (ny < h) {
            if (px > 0) {
              nextErrorRed(px - 1) += r2; nextErrorGreen(px - 1) += g2; nextErrorBlue(px - 1) += b2
              if (px > 1) { nextErrorRed(px - 2) += r1; nextErrorGreen(px - 2) += g1; nextErrorBlue(px - 2) += b1 }
            }
            nextErrorRed(px) += r4; nextErrorGreen(px) += g4; nextErrorBlue(px) += b4
            if (px < lineLen - 1) {
              nextErrorRed(px + 1) += r2; nextErrorGreen(px + 1) += g2; nextErrorBlue(px + 1) += b2
              if (px < lineLen - 2) { nextErrorRed(px + 2) += r1; nextErrorGreen(px + 2) += g1; nextErrorBlue(px + 2) += b1 }
            }
          }
        };
        px += 1
      };
      py += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Oceanic: Burkes with blue noise multiplier on error. */
  def reduceOceanic(pixmap: Pixmap): Pixmap = {
    val hasTransparent = paletteArray(0) == 0; val w                                               = pixmap.width.toInt; val h = pixmap.height.toInt
    val noise          = PaletteReducer.TRI_BLUE_NOISE_MULTIPLIERS
    val s              = 0.13f * _ditherStrength / (populationBias * populationBias); val strength = s * 0.58f / (0.3f + s)
    val (curErrorRed, nextErrorRed, curErrorGreen, nextErrorGreen, curErrorBlue, nextErrorBlue) = ensureErrorArrays(w)
    val oldBlending                                                                             = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    var py                                                                                      = 0;
    while (py < h) {
      val ny = py + 1
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, w); System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, w); System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, w)
      Arrays.fill(nextErrorRed, 0, w, 0f); Arrays.fill(nextErrorGreen, 0, w, 0f); Arrays.fill(nextErrorBlue, 0, w, 0f)
      var px = 0;
      while (px < w) {
        val color = pixmap.getPixel(Pixels(px), Pixels(py))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(px), Pixels(py), 0)
        else {
          val rr   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT(color >>> 24) + curErrorRed(px), 0f), 1023f).toInt) & 255
          val gg   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 16) & 0xff) + curErrorGreen(px), 0f), 1023f).toInt) & 255
          val bb   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 8) & 0xff) + curErrorBlue(px), 0f), 1023f).toInt) & 255
          val used = paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff)
          pixmap.drawPixel(Pixels(px), Pixels(py), used)
          val r1 = ((color >>> 24) - (used >>> 24)) * strength; val g1 = (((color >>> 16) & 255) - ((used >>> 16) & 255)) * strength; val b1 = (((color >>> 8) & 255) - ((used >>> 8) & 255)) * strength
          val r2 = r1 + r1; val g2 = g1 + g1; val b2 = b1 + b1; val r4 = r2 + r2; val g4 = g2 + g2; val b4 = b2 + b2
          if (px < w - 1) {
            val m = noise((px + 1 & 63) | ((py << 6) & 0xfc0)); curErrorRed(px + 1) += r4 * m; curErrorGreen(px + 1) += g4 * m; curErrorBlue(px + 1) += b4 * m
            if (px < w - 2) { val m2 = noise((px + 2 & 63) | ((py << 6) & 0xfc0)); curErrorRed(px + 2) += r2 * m2; curErrorGreen(px + 2) += g2 * m2; curErrorBlue(px + 2) += b2 * m2 }
          }
          if (ny < h) {
            if (px > 0) {
              val m = noise((px - 1 & 63) | ((ny << 6) & 0xfc0)); nextErrorRed(px - 1) += r2 * m; nextErrorGreen(px - 1) += g2 * m; nextErrorBlue(px - 1) += b2 * m
              if (px > 1) { val m2 = noise((px - 2 & 63) | ((ny << 6) & 0xfc0)); nextErrorRed(px - 2) += r1 * m2; nextErrorGreen(px - 2) += g1 * m2; nextErrorBlue(px - 2) += b1 * m2 }
            }
            val mc = noise((px & 63) | ((ny << 6) & 0xfc0)); nextErrorRed(px) += r4 * mc; nextErrorGreen(px) += g4 * mc; nextErrorBlue(px) += b4 * mc
            if (px < w - 1) {
              val m = noise((px + 1 & 63) | ((ny << 6) & 0xfc0)); nextErrorRed(px + 1) += r2 * m; nextErrorGreen(px + 1) += g2 * m; nextErrorBlue(px + 1) += b2 * m
              if (px < w - 2) { val m2 = noise((px + 2 & 63) | ((ny << 6) & 0xfc0)); nextErrorRed(px + 2) += r1 * m2; nextErrorGreen(px + 2) += g1 * m2; nextErrorBlue(px + 2) += b1 * m2 }
            }
          }
        };
        px += 1
      };
      py += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Seaside: Burkes with per-channel blue noise multipliers. */
  def reduceSeaside(pixmap: Pixmap): Pixmap = {
    val hasTransparent = paletteArray(0) == 0; val w                            = pixmap.width.toInt; val h                           = pixmap.height.toInt
    val nA             = PaletteReducer.TRI_BLUE_NOISE_MULTIPLIERS; val nB      = PaletteReducer.TRI_BLUE_NOISE_MULTIPLIERS_B; val nC = PaletteReducer.TRI_BLUE_NOISE_MULTIPLIERS_C
    val s              = 0.15f * populationBias * _ditherStrength; val strength = s * 0.6f / (0.35f + s)
    val (curErrorRed, nextErrorRed, curErrorGreen, nextErrorGreen, curErrorBlue, nextErrorBlue) = ensureErrorArrays(w)
    val oldBlending                                                                             = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    var py                                                                                      = 0;
    while (py < h) {
      val ny = py + 1
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, w); System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, w); System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, w)
      Arrays.fill(nextErrorRed, 0, w, 0f); Arrays.fill(nextErrorGreen, 0, w, 0f); Arrays.fill(nextErrorBlue, 0, w, 0f)
      var px = 0;
      while (px < w) {
        val color = pixmap.getPixel(Pixels(px), Pixels(py))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(px), Pixels(py), 0)
        else {
          val rr   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT(color >>> 24) + curErrorRed(px), 0f), 1023f).toInt) & 255
          val gg   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 16) & 0xff) + curErrorGreen(px), 0f), 1023f).toInt) & 255
          val bb   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 8) & 0xff) + curErrorBlue(px), 0f), 1023f).toInt) & 255
          val used = paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff)
          pixmap.drawPixel(Pixels(px), Pixels(py), used)
          val rdiff = (color >>> 24) - (used >>> 24); val gdiff = ((color >>> 16) & 255) - ((used >>> 16) & 255); val bdiff = ((color >>> 8) & 255) - ((used >>> 8) & 255)
          var mi    = (px & 63) | (py << 6 & 0xfc0)
          val r1    = rdiff * strength * nA(mi); val g1         = gdiff * strength * nB(mi); val b1                         = bdiff * strength * nC(mi)
          val r2    = r1 + r1; val g2                           = g1 + g1; val b2                                           = b1 + b1; val r4 = r2 + r2; val g4 = g2 + g2; val b4 = b2 + b2
          if (px < w - 1) {
            mi = (px + 1 & 63) | (py << 6 & 0xfc0); curErrorRed(px + 1) += r4 * nA(mi); curErrorGreen(px + 1) += g4 * nB(mi); curErrorBlue(px + 1) += b4 * nC(mi)
            if (px < w - 2) { mi = (px + 2 & 63) | ((py << 6) & 0xfc0); curErrorRed(px + 2) += r2 * nA(mi); curErrorGreen(px + 2) += g2 * nB(mi); curErrorBlue(px + 2) += b2 * nC(mi) }
          }
          if (ny < h) {
            if (px > 0) {
              mi = (px - 1 & 63) | ((ny << 6) & 0xfc0); nextErrorRed(px - 1) += r2 * nA(mi); nextErrorGreen(px - 1) += g2 * nB(mi); nextErrorBlue(px - 1) += b2 * nC(mi)
              if (px > 1) { mi = (px - 2 & 63) | ((ny << 6) & 0xfc0); nextErrorRed(px - 2) += r1 * nA(mi); nextErrorGreen(px - 2) += g1 * nB(mi); nextErrorBlue(px - 2) += b1 * nC(mi) }
            }
            mi = (px & 63) | ((ny << 6) & 0xfc0); nextErrorRed(px) += r4 * nA(mi); nextErrorGreen(px) += g4 * nB(mi); nextErrorBlue(px) += b4 * nC(mi)
            if (px < w - 1) {
              mi = (px + 1 & 63) | ((ny << 6) & 0xfc0); nextErrorRed(px + 1) += r2 * nA(mi); nextErrorGreen(px + 1) += g2 * nB(mi); nextErrorBlue(px + 1) += b2 * nC(mi)
              if (px < w - 2) { mi = (px + 2 & 63) | ((ny << 6) & 0xfc0); nextErrorRed(px + 2) += r1 * nA(mi); nextErrorGreen(px + 2) += g1 * nB(mi); nextErrorBlue(px + 2) += b1 * nC(mi) }
            }
          }
        };
        px += 1
      };
      py += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Woven error-diffusion dither with R2 per-channel noise. */
  def reduceWoven(pixmap: Pixmap): Pixmap = {
    val hasTransparent                                                                          = paletteArray(0) == 0; val lineLen = pixmap.width.toInt; val h = pixmap.height.toInt
    val (curErrorRed, nextErrorRed, curErrorGreen, nextErrorGreen, curErrorBlue, nextErrorBlue) = ensureErrorArrays(lineLen)
    val oldBlending                                                                             = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    val w1       = (10f * Math.sqrt(_ditherStrength) / (populationBias * populationBias)).toFloat; val w3 = w1 * 3f; val w5 = w1 * 5f; val w7 = w1 * 7f
    val strength = 100f * _ditherStrength / (populationBias * populationBias * populationBias * populationBias)
    val limit    = 5f + 250f / Math.sqrt(colorCount + 1.5f).toFloat
    var y        = 0;
    while (y < h) {
      val ny = y + 1
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, lineLen); System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, lineLen); System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, lineLen)
      Arrays.fill(nextErrorRed, 0, lineLen, 0f); Arrays.fill(nextErrorGreen, 0, lineLen, 0f); Arrays.fill(nextErrorBlue, 0, lineLen, 0f)
      var px = 0;
      while (px < lineLen) {
        val color = pixmap.getPixel(Pixels(px), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(px), Pixels(y), 0)
        else {
          // 0x1.4p-23f = 1.4901161e-7f; 0x1.4p-1f = 0.625f
          val er = Math.min(
            Math.max(((((px + 1).toLong * 0xc13fa9a902a6328fL + (y + 1).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 1.4901161e-7f - 0.625f) * strength, -limit),
            limit
          ) + curErrorRed(px)
          val eg = Math.min(
            Math.max(((((px + 3).toLong * 0xc13fa9a902a6328fL + (y - 1).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 1.4901161e-7f - 0.625f) * strength, -limit),
            limit
          ) + curErrorGreen(px)
          val eb = Math.min(
            Math.max(((((px - 4).toLong * 0xc13fa9a902a6328fL + (y + 2).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 1.4901161e-7f - 0.625f) * strength, -limit),
            limit
          ) + curErrorBlue(px)
          val rr   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT(color >>> 24) + er, 0f), 1023f).toInt) & 255
          val gg   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 16) & 0xff) + eg, 0f), 1023f).toInt) & 255
          val bb   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 8) & 0xff) + eb, 0f), 1023f).toInt) & 255
          val used = paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff)
          pixmap.drawPixel(Pixels(px), Pixels(y), used)
          // 0x5p-10f = 0.0048828125f
          val rdiff = 0.0048828125f * ((color >>> 24) - (used >>> 24)); val gdiff = 0.0048828125f * (((color >>> 16) & 255) - ((used >>> 16) & 255));
          val bdiff = 0.0048828125f * (((color >>> 8) & 255) - ((used >>> 8) & 255))
          if (px < lineLen - 1) { curErrorRed(px + 1) += rdiff * w7; curErrorGreen(px + 1) += gdiff * w7; curErrorBlue(px + 1) += bdiff * w7 }
          if (ny < h) {
            if (px > 0) { nextErrorRed(px - 1) += rdiff * w3; nextErrorGreen(px - 1) += gdiff * w3; nextErrorBlue(px - 1) += bdiff * w3 }
            if (px < lineLen - 1) { nextErrorRed(px + 1) += rdiff * w1; nextErrorGreen(px + 1) += gdiff * w1; nextErrorBlue(px + 1) += bdiff * w1 }
            nextErrorRed(px) += rdiff * w5; nextErrorGreen(px) += gdiff * w5; nextErrorBlue(px) += bdiff * w5
          }
        };
        px += 1
      };
      y += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Dodgy error-diffusion dither with per-channel blue noise. */
  def reduceDodgy(pixmap: Pixmap): Pixmap = {
    val hasTransparent                                                                          = paletteArray(0) == 0; val lineLen = pixmap.width.toInt; val h = pixmap.height.toInt
    val (curErrorRed, nextErrorRed, curErrorGreen, nextErrorGreen, curErrorBlue, nextErrorBlue) = ensureErrorArrays(lineLen)
    val oldBlending                                                                             = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    val w1                                                                                      = 8f * _ditherStrength; val w3      = w1 * 3f; val w5           = w1 * 5f; val w7 = w1 * 7f
    val strength = 0.35f * _ditherStrength / (populationBias * populationBias * populationBias); val limit = 90f
    var py       = 0;
    while (py < h) {
      val ny = py + 1
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, lineLen); System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, lineLen); System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, lineLen)
      Arrays.fill(nextErrorRed, 0, lineLen, 0f); Arrays.fill(nextErrorGreen, 0, lineLen, 0f); Arrays.fill(nextErrorBlue, 0, lineLen, 0f)
      var px = 0;
      while (px < lineLen) {
        val color = pixmap.getPixel(Pixels(px), Pixels(py))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(px), Pixels(py), 0)
        else {
          val er   = Math.min(Math.max((PaletteReducer.TRI_BLUE_NOISE((px & 63) | (py & 63) << 6) + 0.5f) * strength, -limit), limit) + curErrorRed(px)
          val eg   = Math.min(Math.max((PaletteReducer.TRI_BLUE_NOISE_B((px & 63) | (py & 63) << 6) + 0.5f) * strength, -limit), limit) + curErrorGreen(px)
          val eb   = Math.min(Math.max((PaletteReducer.TRI_BLUE_NOISE_C((px & 63) | (py & 63) << 6) + 0.5f) * strength, -limit), limit) + curErrorBlue(px)
          val rr   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT(color >>> 24) + er, 0f), 1023f).toInt) & 255
          val gg   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 16) & 0xff) + eg, 0f), 1023f).toInt) & 255
          val bb   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 8) & 0xff) + eb, 0f), 1023f).toInt) & 255
          val used = paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff)
          pixmap.drawPixel(Pixels(px), Pixels(py), used)
          // 0x5p-8f = 0.01953125f
          var rdiff = 0.01953125f * ((color >>> 24) - (used >>> 24)); var gdiff = 0.01953125f * (((color >>> 16) & 255) - ((used >>> 16) & 255));
          var bdiff = 0.01953125f * (((color >>> 8) & 255) - ((used >>> 8) & 255))
          rdiff /= (0.5f + Math.abs(rdiff)); gdiff /= (0.5f + Math.abs(gdiff)); bdiff /= (0.5f + Math.abs(bdiff))
          if (px < lineLen - 1) { curErrorRed(px + 1) += rdiff * w7; curErrorGreen(px + 1) += gdiff * w7; curErrorBlue(px + 1) += bdiff * w7 }
          if (ny < h) {
            if (px > 0) { nextErrorRed(px - 1) += rdiff * w3; nextErrorGreen(px - 1) += gdiff * w3; nextErrorBlue(px - 1) += bdiff * w3 }
            if (px < lineLen - 1) { nextErrorRed(px + 1) += rdiff * w1; nextErrorGreen(px + 1) += gdiff * w1; nextErrorBlue(px + 1) += bdiff * w1 }
            nextErrorRed(px) += rdiff * w5; nextErrorGreen(px) += gdiff * w5; nextErrorBlue(px) += bdiff * w5
          }
        };
        px += 1
      };
      py += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Overboard: Burkes dither with various extra error patterns. */
  def reduceOverboard(pixmap: Pixmap): Pixmap = {
    val hasTransparent = paletteArray(0) == 0; val lineLen                                             = pixmap.width.toInt; val h                         = pixmap.height.toInt
    val strength       = _ditherStrength * 1.5f * (populationBias * populationBias); val noiseStrength = 4f / (populationBias * populationBias); val limit = 110f
    val (curErrorRed, nextErrorRed, curErrorGreen, nextErrorGreen, curErrorBlue, nextErrorBlue) = ensureErrorArrays(lineLen)
    val oldBlending                                                                             = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    var y                                                                                       = 0;
    while (y < h) {
      val ny = y + 1
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, lineLen); System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, lineLen); System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, lineLen)
      Arrays.fill(nextErrorRed, 0, lineLen, 0f); Arrays.fill(nextErrorGreen, 0, lineLen, 0f); Arrays.fill(nextErrorBlue, 0, lineLen, 0f)
      var x = 0;
      while (x < lineLen) {
        val color = pixmap.getPixel(Pixels(x), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(x), Pixels(y), 0)
        else {
          var er    = 0f; var eg = 0f; var eb = 0f
          val bnIdx = (x & 63) | (y & 63) << 6
          ((x << 1) & 2) | (y & 1) match {
            case 0 => er += ((x ^ y) % 9 - 4); eg += (PaletteReducer.TRI_BLUE_NOISE_B(bnIdx) + 0.5f) * 0.03125f; eb += (PaletteReducer.TRI_BLUE_NOISE_C(bnIdx) + 0.5f) * 0.015625f
            case 1 => er += (PaletteReducer.TRI_BLUE_NOISE(bnIdx) + 0.5f) * 0.03125f; eg += (PaletteReducer.TRI_BLUE_NOISE_B(bnIdx) + 0.5f) * 0.015625f; eb += ((x ^ y) % 11 - 5)
            case 2 => er += (PaletteReducer.TRI_BLUE_NOISE(bnIdx) + 0.5f) * 0.015625f; eg += ((x ^ y) % 11 - 5); eb += ((x ^ y) % 9 - 4)
            case _ => er += ((x ^ y) % 11 - 5); eg += ((x ^ y) % 9 - 4); eb += (PaletteReducer.TRI_BLUE_NOISE_C(bnIdx) + 0.5f) * 0.03125f
          }
          er = er * noiseStrength + curErrorRed(x); eg = eg * noiseStrength + curErrorGreen(x); eb = eb * noiseStrength + curErrorBlue(x)
          val rr   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT(color >>> 24) + Math.min(Math.max(er, -limit), limit), 0f), 1023f).toInt) & 255
          val gg   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 16) & 0xff) + Math.min(Math.max(eg, -limit), limit), 0f), 1023f).toInt) & 255
          val bb   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 8) & 0xff) + Math.min(Math.max(eb, -limit), limit), 0f), 1023f).toInt) & 255
          val used = paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff)
          pixmap.drawPixel(Pixels(x), Pixels(y), used)
          val rdiff = ((color >>> 24) - (used >>> 24)) * strength; val gdiff = (((color >>> 16) & 255) - ((used >>> 16) & 255)) * strength;
          val bdiff = (((color >>> 8) & 255) - ((used >>> 8) & 255)) * strength
          val r1    = rdiff * 16f / (45f + Math.abs(rdiff)); val g1          = gdiff * 16f / (45f + Math.abs(gdiff)); val b1 = bdiff * 16f / (45f + Math.abs(bdiff))
          val r2    = r1 + r1; val g2                                        = g1 + g1; val b2                               = b1 + b1; val r4 = r2 + r2; val g4 = g2 + g2; val b4 = b2 + b2
          if (x < lineLen - 1) {
            curErrorRed(x + 1) += r4; curErrorGreen(x + 1) += g4; curErrorBlue(x + 1) += b4
            if (x < lineLen - 2) { curErrorRed(x + 2) += r2; curErrorGreen(x + 2) += g2; curErrorBlue(x + 2) += b2 }
          }
          if (ny < h) {
            if (x > 0) {
              nextErrorRed(x - 1) += r2; nextErrorGreen(x - 1) += g2; nextErrorBlue(x - 1) += b2
              if (x > 1) { nextErrorRed(x - 2) += r1; nextErrorGreen(x - 2) += g1; nextErrorBlue(x - 2) += b1 }
            }
            nextErrorRed(x) += r4; nextErrorGreen(x) += g4; nextErrorBlue(x) += b4
            if (x < lineLen - 1) {
              nextErrorRed(x + 1) += r2; nextErrorGreen(x + 1) += g2; nextErrorBlue(x + 1) += b2
              if (x < lineLen - 2) { nextErrorRed(x + 2) += r1; nextErrorGreen(x + 2) += g1; nextErrorBlue(x + 2) += b1 }
            }
          }
        };
        x += 1
      };
      y += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Igneous error-diffusion dither with interleaved gradient noise. */
  def reduceIgneous(pixmap: Pixmap): Pixmap = {
    val hasTransparent                                                                          = paletteArray(0) == 0; val lineLen = pixmap.width.toInt; val h = pixmap.height.toInt
    val (curErrorRed, nextErrorRed, curErrorGreen, nextErrorGreen, curErrorBlue, nextErrorBlue) = ensureErrorArrays(lineLen)
    val oldBlending                                                                             = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    val w1       = 6f * _ditherStrength * populationBias * populationBias; val w3 = w1 * 3f; val w5 = w1 * 5f; val w7 = w1 * 7f
    val strength = 60f * _ditherStrength / (populationBias * populationBias)
    var y        = 0;
    while (y < h) {
      val ny = y + 1
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, lineLen); System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, lineLen); System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, lineLen)
      Arrays.fill(nextErrorRed, 0, lineLen, 0f); Arrays.fill(nextErrorGreen, 0, lineLen, 0f); Arrays.fill(nextErrorBlue, 0, lineLen, 0f)
      var px = 0;
      while (px < lineLen) {
        val color = pixmap.getPixel(Pixels(px), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(px), Pixels(y), 0)
        else {
          var adj  = px * 0.06711056f + y * 0.00583715f; adj -= adj.toInt; adj *= 52.9829189f; adj -= adj.toInt; adj -= 0.5f; adj *= strength
          val rr   = Math.min(Math.max(((color >>> 24) + adj + curErrorRed(px) + 0.5f).toInt, 0), 0xff)
          val gg   = Math.min(Math.max((((color >>> 16) & 0xff) + adj + curErrorGreen(px) + 0.5f).toInt, 0), 0xff)
          val bb   = Math.min(Math.max((((color >>> 8) & 0xff) + adj + curErrorBlue(px) + 0.5f).toInt, 0), 0xff)
          val used = paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff)
          pixmap.drawPixel(Pixels(px), Pixels(y), used)
          // 0x3p-10f = 0.0029296875f
          val rdiff = 0.0029296875f * ((color >>> 24) - (used >>> 24)); val gdiff = 0.0029296875f * (((color >>> 16) & 255) - ((used >>> 16) & 255));
          val bdiff = 0.0029296875f * (((color >>> 8) & 255) - ((used >>> 8) & 255))
          if (px < lineLen - 1) { curErrorRed(px + 1) += rdiff * w7; curErrorGreen(px + 1) += gdiff * w7; curErrorBlue(px + 1) += bdiff * w7 }
          if (ny < h) {
            if (px > 0) { nextErrorRed(px - 1) += rdiff * w3; nextErrorGreen(px - 1) += gdiff * w3; nextErrorBlue(px - 1) += bdiff * w3 }
            if (px < lineLen - 1) { nextErrorRed(px + 1) += rdiff * w1; nextErrorGreen(px + 1) += gdiff * w1; nextErrorBlue(px + 1) += bdiff * w1 }
            nextErrorRed(px) += rdiff * w5; nextErrorGreen(px) += gdiff * w5; nextErrorBlue(px) += bdiff * w5
          }
        };
        px += 1
      };
      y += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Wren: Burkes dither with R2 + per-channel blue noise. Default dither. */
  def reduceWren(pixmap: Pixmap): Pixmap = {
    val hasTransparent                                                                          = paletteArray(0) == 0; val lineLen = pixmap.width.toInt; val h = pixmap.height.toInt
    val (curErrorRed, nextErrorRed, curErrorGreen, nextErrorGreen, curErrorBlue, nextErrorBlue) = ensureErrorArrays(lineLen)
    val oldBlending                                                                             = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    val partialDitherStrength                                                                   = 0.5f * _ditherStrength / (populationBias * populationBias)
    val strength                                                                                = 80f * _ditherStrength / (populationBias * populationBias)
    val blueStrength                                                                            = 0.3f * _ditherStrength / (populationBias * populationBias)
    val limit                                                                                   = 5f + 200f / Math.sqrt(colorCount + 1.5f).toFloat
    // 0x1.4p-24f = 7.450581e-8f; 0x1.4p-2f = 0.3125f
    var y = 0;
    while (y < h) {
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, lineLen); System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, lineLen); System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, lineLen)
      Arrays.fill(nextErrorRed, 0, lineLen, 0f); Arrays.fill(nextErrorGreen, 0, lineLen, 0f); Arrays.fill(nextErrorBlue, 0, lineLen, 0f)
      var x = 0;
      while (x < lineLen) {
        val color = pixmap.getPixel(Pixels(x), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(x), Pixels(y), 0)
        else {
          val er = Math.min(
            Math.max(
              (PaletteReducer.TRI_BLUE_NOISE(
                (x & 63) | (y & 63) << 6
              ) + 0.5f) * blueStrength + ((((x + 1).toLong * 0xc13fa9a902a6328fL + (y + 1).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 7.450581e-8f - 0.3125f) * strength,
              -limit
            ),
            limit
          ) + curErrorRed(x)
          val eg = Math.min(
            Math.max(
              (PaletteReducer.TRI_BLUE_NOISE_B(
                (x & 63) | (y & 63) << 6
              ) + 0.5f) * blueStrength + ((((x + 3).toLong * 0xc13fa9a902a6328fL + (y - 1).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 7.450581e-8f - 0.3125f) * strength,
              -limit
            ),
            limit
          ) + curErrorGreen(x)
          val eb = Math.min(
            Math.max(
              (PaletteReducer.TRI_BLUE_NOISE_C(
                (x & 63) | (y & 63) << 6
              ) + 0.5f) * blueStrength + ((((x + 2).toLong * 0xc13fa9a902a6328fL + (y - 4).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 7.450581e-8f - 0.3125f) * strength,
              -limit
            ),
            limit
          ) + curErrorBlue(x)
          val rr   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT(color >>> 24) + er, 0f), 1023f).toInt) & 255
          val gg   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 16) & 0xff) + eg, 0f), 1023f).toInt) & 255
          val bb   = PaletteReducer.fromLinearLUT(Math.min(Math.max(PaletteReducer.toLinearLUT((color >>> 8) & 0xff) + eb, 0f), 1023f).toInt) & 255
          val used = paletteArray(paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff)
          pixmap.drawPixel(Pixels(x), Pixels(y), used)
          val rdiff = ((color >>> 24) - (used >>> 24)) * partialDitherStrength; val gdiff = (((color >>> 16) & 255) - ((used >>> 16) & 255)) * partialDitherStrength;
          val bdiff = (((color >>> 8) & 255) - ((used >>> 8) & 255)) * partialDitherStrength
          val r1    = rdiff * 16f / Math.sqrt(2048f + rdiff * rdiff).toFloat; val g1      = gdiff * 16f / Math.sqrt(2048f + gdiff * gdiff).toFloat;
          val b1    = bdiff * 16f / Math.sqrt(2048f + bdiff * bdiff).toFloat
          val r2    = r1 + r1; val g2                                                     = g1 + g1; val b2 = b1 + b1; val r4 = r2 + r2; val g4 = g2 + g2; val b4 = b2 + b2
          if (x < lineLen - 1) {
            curErrorRed(x + 1) += r4; curErrorGreen(x + 1) += g4; curErrorBlue(x + 1) += b4
            if (x < lineLen - 2) { curErrorRed(x + 2) += r2; curErrorGreen(x + 2) += g2; curErrorBlue(x + 2) += b2 }
          }
          if (y + 1 < h) {
            if (x > 0) {
              nextErrorRed(x - 1) += r2; nextErrorGreen(x - 1) += g2; nextErrorBlue(x - 1) += b2
              if (x > 1) { nextErrorRed(x - 2) += r1; nextErrorGreen(x - 2) += g1; nextErrorBlue(x - 2) += b1 }
            }
            nextErrorRed(x) += r4; nextErrorGreen(x) += g4; nextErrorBlue(x) += b4
            if (x < lineLen - 1) {
              nextErrorRed(x + 1) += r2; nextErrorGreen(x + 1) += g2; nextErrorBlue(x + 1) += b2
              if (x < lineLen - 2) { nextErrorRed(x + 2) += r1; nextErrorGreen(x + 2) += g1; nextErrorBlue(x + 2) += b1 }
            }
          }
        };
        x += 1
      };
      y += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Knoll pattern dither with 4x4 matrix. */
  def reduceKnoll(pixmap: Pixmap): Pixmap = {
    val hasTransparent = paletteArray(0) == 0; val lineLen = pixmap.width.toInt; val h = pixmap.height.toInt
    val oldBlending    = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    val errorMul       = _ditherStrength * 0.5f / populationBias
    var y              = 0;
    while (y < h) {
      var px = 0;
      while (px < lineLen) {
        val color = pixmap.getPixel(Pixels(px), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(px), Pixels(y), 0)
        else {
          var er = 0; var eg = 0; var eb = 0; val cr = color >>> 24; val cg = (color >>> 16) & 0xff; val cb = (color >>> 8) & 0xff
          var i  = 0;
          while (i < 16) {
            val rr        = Math.min(Math.max((cr + er * errorMul).toInt, 0), 255); val gg = Math.min(Math.max((cg + eg * errorMul).toInt, 0), 255);
            val bb        = Math.min(Math.max((cb + eb * errorMul).toInt, 0), 255)
            val usedIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff
            val used      = paletteArray(usedIndex); candidates(i | 16) = PaletteReducer.shrink(used); candidates(i) = used
            er += cr - (used >>> 24); eg += cg - ((used >>> 16) & 0xff); eb += cb - ((used >>> 8) & 0xff); i += 1
          }
          PaletteReducer.sort16(candidates)
          pixmap.drawPixel(Pixels(px), Pixels(y), candidates(PaletteReducer.thresholdMatrix16((px & 3) | (y & 3) << 2)))
        };
        px += 1
      };
      y += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  /** Knoll-Roberts skewed pattern dither with R2 sequence. */
  def reduceKnollRoberts(pixmap: Pixmap): Pixmap = {
    val hasTransparent = paletteArray(0) == 0; val lineLen = pixmap.width.toInt; val h = pixmap.height.toInt
    val oldBlending    = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    val errorMul       = _ditherStrength * populationBias * 1.25f
    // 0x1.C13FA9A902A6328Fp3 = 14.039735539770483; 0x1.9E3779B97F4A7C15p-2 = 0.40614353195788
    var y = 0;
    while (y < h) {
      var px = 0;
      while (px < lineLen) {
        val color = pixmap.getPixel(Pixels(px), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(px), Pixels(y), 0)
        else {
          var er = 0; var eg = 0; var eb = 0; val cr = color >>> 24; val cg = (color >>> 16) & 0xff; val cb = (color >>> 8) & 0xff
          var c  = 0;
          while (c < 8) {
            val rr        = Math.min(Math.max((cr + er * errorMul).toInt, 0), 255); val gg = Math.min(Math.max((cg + eg * errorMul).toInt, 0), 255);
            val bb        = Math.min(Math.max((cb + eb * errorMul).toInt, 0), 255)
            val usedIndex = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3)) & 0xff
            val used      = paletteArray(usedIndex); candidates(c | 16) = PaletteReducer.shrink(used); candidates(c) = used
            er += cr - (used >>> 24); eg += cg - ((used >>> 16) & 0xff); eb += cb - ((used >>> 8) & 0xff); c += 1
          }
          PaletteReducer.sort8(candidates)
          pixmap.drawPixel(
            Pixels(px),
            Pixels(y),
            candidates(thresholdMatrix8(((px * 14.039735539770483 + y * 0.40614353195788).toInt & 3) ^ ((px & 3) | (y & 1) << 2)))
          )
        };
        px += 1
      };
      y += 1
    }
    pixmap.setBlending(oldBlending); pixmap
  }

  // === Reduce utility methods ===

  /** Returns a byte index for the given RGBA8888 color. */
  def reduceIndex(color: Int): Byte = {
    if ((color & 0x80) == 0) return 0.toByte
    paletteMapping((color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f))
  }

  /** Reduces a packed float color to the nearest palette color. */
  def reduceFloat(packedColor: Float): Float = {
    val color = java.lang.Float.floatToIntBits(packedColor)
    if (color >= 0) return 0f
    java.lang.Float.intBitsToFloat(Integer.reverseBytes(paletteArray(paletteMapping((color << 7 & 0x7c00) | (color >>> 6 & 0x3e0) | (color >>> 19)) & 0xff) & 0xfffffffe))
  }

  /** Modifies `color` in-place to match the nearest palette color. */
  def reduceInPlace(color: Color): Color = {
    if (color.a < 0.5f) return color.set(0)
    color.set(
      paletteArray(paletteMapping(((color.r * 32256f).toInt & 0x7c00) | ((color.g * 1008f).toInt & 0x3e0) | (color.r * 31.5f).toInt) & 0xff)
    )
  }

  // === Color alteration methods ===

  /** Adjusts lightness of palette colors using an Interpolation. */
  def alterColorsLightness(lightness: Interpolation): PaletteReducer = {
    var idx = 0;
    while (idx < colorCount) {
      val s = PaletteReducer.shrink(paletteArray(idx))
      paletteArray(idx) = PaletteReducer.oklabToRGB(
        lightness.apply(PaletteReducer.OKLAB(0)(s)),
        PaletteReducer.OKLAB(1)(s),
        PaletteReducer.OKLAB(2)(s),
        (paletteArray(idx) & 0xfe) / 254f
      )
      idx += 1
    };
    this
  }

  /** Adjusts RGB channels of palette colors using per-channel Interpolations. */
  def alterColors(changeR: Interpolation, changeG: Interpolation, changeB: Interpolation): PaletteReducer = {
    var idx = 0;
    while (idx < colorCount) {
      val p = paletteArray(idx)
      paletteArray(idx) = Math.min(Math.max(changeR.apply(0f, 255.999f, (p >>> 24) / 255f).toInt, 0), 255) << 24 |
        Math.min(Math.max(changeG.apply(0f, 255.999f, ((p >>> 16) & 255) / 255f).toInt, 0), 255) << 16 |
        Math.min(Math.max(changeB.apply(0f, 255.999f, ((p >>> 8) & 255) / 255f).toInt, 0), 255) << 8 | (p & 255)
      idx += 1
    };
    this
  }

  /** Adjusts Oklab channels of palette colors using per-channel Interpolations. */
  def alterColorsOklab(lightness: Interpolation, greenToRed: Interpolation, blueToYellow: Interpolation): PaletteReducer = {
    var idx = 0;
    while (idx < colorCount) {
      val s = PaletteReducer.shrink(paletteArray(idx))
      paletteArray(idx) = PaletteReducer.oklabToRGB(
        lightness.apply(PaletteReducer.OKLAB(0)(s)),
        greenToRed.apply(-1f, 1f, PaletteReducer.OKLAB(1)(s) * 0.5f + 0.5f),
        blueToYellow.apply(-1f, 1f, PaletteReducer.OKLAB(2)(s) * 0.5f + 0.5f),
        (paletteArray(idx) & 0xfe) / 254f
      )
      idx += 1
    };
    this
  }

  /** Multiplies chroma of palette colors by `saturationMultiplier`. */
  def alterColorsSaturation(saturationMultiplier: Float): PaletteReducer = {
    var idx = 0;
    while (idx < colorCount) {
      val s = PaletteReducer.shrink(paletteArray(idx))
      paletteArray(idx) = PaletteReducer.oklabToRGB(
        PaletteReducer.OKLAB(0)(s),
        Math.min(Math.max(PaletteReducer.OKLAB(1)(s) * saturationMultiplier, -1f), 1f),
        Math.min(Math.max(PaletteReducer.OKLAB(2)(s) * saturationMultiplier, -1f), 1f),
        (paletteArray(idx) & 0xfe) / 254f
      )
      idx += 1
    };
    this
  }

  /** Shifts hues so lighter colors lean warm and darker colors lean cool. */
  def hueShift(): PaletteReducer = hueShift(1f)

  /** Shifts hues with configurable strength. */
  def hueShift(strengthMultiplier: Float): PaletteReducer = {
    val aMul = 1.1f * strengthMultiplier; val bMul = 0.125f * strengthMultiplier
    var idx  = 0;
    while (idx < colorCount) {
      val s = PaletteReducer.shrink(paletteArray(idx)); val L = PaletteReducer.OKLAB(0)(s); val A = PaletteReducer.OKLAB(1)(s) * aMul
      val B = PaletteReducer.OKLAB(2)(s) + OtherMath.asin(L - 0.6f) * bMul * (1f - A * A)
      paletteArray(idx) = PaletteReducer.oklabToRGB(L, A, B, (paletteArray(idx) & 0xfe) / 254f)
      idx += 1
    };
    this
  }

  // === Median-Cut Analysis ===

  /** Analyzes the given pixmap using median-cut color quantization to build a palette with at most `limit` colors. */
  def analyzeMC(pixmap: Pixmap, limit0: Int): Unit = {
    Arrays.fill(paletteArray, 0)
    Arrays.fill(paletteMapping, 0.toByte)
    var color          = 0
    val width          = pixmap.width.toInt
    val height         = pixmap.height.toInt
    val bin            = new scala.collection.mutable.ArrayBuffer[Int](width * height)
    val counts         = new mutable.HashMap[Int, Int]()
    var hasTransparent = 0
    var rangeR         = 0
    var rangeG         = 0
    var rangeB         = 0
    var y              = 0
    while (y < height) {
      var x = 0
      while (x < width) {
        color = pixmap.getPixel(Pixels(x), Pixels(y)) & 0xf8f8f880
        if ((color & 0x80) != 0) {
          color |= (color >>> 5 & 0x07070700) | 0xff
          bin += color
          counts(color) = counts.getOrElse(color, 0) + 1
        } else {
          hasTransparent = 1
        }
        x += 1
      }
      y += 1
    }
    val limit = Math.max(2 - hasTransparent, Math.min(limit0 - hasTransparent, 256))
    if (counts.size > limit) {
      val numCuts = 32 - Integer.numberOfLeadingZeros(limit - 1)
      var in      = bin.toArray
      var out     = new Array[Int](in.length)
      val bufR    = new Array[Int](32)
      val bufG    = new Array[Int](32)
      val bufB    = new Array[Int](32)
      var stage   = 0
      while (stage < numCuts) {
        val size   = in.length >>> stage
        var offset = 0
        var end    = 0
        var part   = 1 << stage
        while (part > 0) {
          if (part == 1) {
            end = in.length
          } else {
            end += size
          }
          Arrays.fill(bufR, 0)
          Arrays.fill(bufG, 0)
          Arrays.fill(bufB, 0)
          var i = offset
          while (i < end) {
            val ii = in(i)
            bufR(ii >>> 27) += 1
            bufG(ii >>> 19 & 31) += 1
            bufB(ii >>> 11 & 31) += 1
            i += 1
          }
          rangeR = 32
          while (rangeR > 0 && bufR(rangeR - 1) == 0) rangeR -= 1
          locally { var r2 = 0; while (r2 < rangeR && bufR(r2) == 0) { r2 += 1; rangeR -= 1 } }
          rangeG = 32
          while (rangeG > 0 && bufG(rangeG - 1) == 0) rangeG -= 1
          locally { var r2 = 0; while (r2 < rangeG && bufG(r2) == 0) { r2 += 1; rangeG -= 1 } }
          rangeB = 32
          while (rangeB > 0 && bufB(rangeB - 1) == 0) rangeB -= 1
          locally { var r2 = 0; while (r2 < rangeB && bufB(r2) == 0) { r2 += 1; rangeB -= 1 } }

          if (rangeG >= rangeR && rangeG >= rangeB) {
            { var j = 1; while (j < 32) { bufG(j) += bufG(j - 1); j += 1 } }
            { var j = end - 1; while (j >= offset) { bufG(in(j) >>> 19 & 31) -= 1; out(offset + bufG(in(j) >>> 19 & 31)) = in(j); j -= 1 } }
          } else if (rangeR >= rangeG && rangeR >= rangeB) {
            { var j = 1; while (j < 32) { bufR(j) += bufR(j - 1); j += 1 } }
            { var j = end - 1; while (j >= offset) { bufR(in(j) >>> 27) -= 1; out(offset + bufR(in(j) >>> 27)) = in(j); j -= 1 } }
          } else {
            { var j = 1; while (j < 32) { bufB(j) += bufB(j - 1); j += 1 } }
            { var j = end - 1; while (j >= offset) { bufB(in(j) >>> 11 & 31) -= 1; out(offset + bufB(in(j) >>> 11 & 31)) = in(j); j -= 1 } }
          }
          offset += size
          part -= 1
        }
        // Swap in and out for next stage
        val tmp = in; in = out; out = tmp
        stage += 1
      }
      // Now `in` holds the sorted data after all stages
      val jump     = in.length >>> numCuts
      var mid      = 0
      var assigned = 0
      val fr       = 270.0 / (jump * 31.0)
      val n        = (1 << numCuts) - 1
      while (assigned < n) {
        var r  = 0.0
        var g  = 0.0
        var b  = 0.0
        var i2 = mid + jump - 1
        while (i2 >= mid) {
          color = in(i2)
          r += color >>> 27
          g += color >>> 19 & 31
          b += color >>> 11 & 31
          i2 -= 1
        }
        paletteArray(assigned) = Math.min(Math.max(((r - 7.0) * fr).toInt, 0), 255) << 24 |
          Math.min(Math.max(((g - 7.0) * fr).toInt, 0), 255) << 16 |
          Math.min(Math.max(((b - 7.0) * fr).toInt, 0), 255) << 8 | 0xff
        assigned += 1
        mid += jump
      }
      // Handle the last bin (which may differ in size)
      locally {
        val j2  = in.length - (mid - jump)
        var r   = 0.0
        var g   = 0.0
        var b   = 0.0
        val fr2 = 270.0 / (j2 * 31.0)
        var i2  = in.length - 1
        while (i2 >= mid) {
          color = in(i2)
          r += color >>> 27
          g += color >>> 19 & 31
          b += color >>> 11 & 31
          i2 -= 1
        }
        paletteArray(assigned) = Math.min(Math.max(((r - 7.0) * fr2).toInt, 0), 255) << 24 |
          Math.min(Math.max(((g - 7.0) * fr2).toInt, 0), 255) << 16 |
          Math.min(Math.max(((b - 7.0) * fr2).toInt, 0), 255) << 8 | 0xff
        assigned += 1
      }
      // Swap less-frequent colors past limit with more-frequent ones in limit range
      var ci = limit
      while (ci < assigned) {
        val currentCount = counts.getOrElse(paletteArray(ci), 0)
        var j            = 0
        boundary {
          while (j < limit) {
            if (counts.getOrElse(paletteArray(j), 0) < currentCount) {
              val temp = paletteArray(j)
              paletteArray(j) = paletteArray(ci)
              paletteArray(ci) = temp
              break(())
            }
            j += 1
          }
        }
        ci += 1
      }
      if (hasTransparent == 1) {
        var min   = Int.MaxValue
        var worst = 0
        var i2    = 0
        while (i2 < limit) {
          val currentCount = counts.getOrElse(paletteArray(i2), 0)
          if (currentCount < min) {
            min = currentCount
            worst = i2
          }
          i2 += 1
        }
        if (worst != 0) {
          paletteArray(worst) = paletteArray(0)
        }
        paletteArray(0) = 0
      }
      var i2 = hasTransparent
      while (i2 < limit) {
        color = paletteArray(i2)
        paletteMapping((color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f)) = i2.toByte
        i2 += 1
      }
      colorCount = limit
      populationBias = Math.exp(-1.375 / colorCount).toFloat
    } else {
      Arrays.fill(paletteArray, 0)
      val it = counts.keysIterator
      var i  = hasTransparent
      while (i < limit && it.hasNext) {
        color = it.next()
        paletteArray(i) = color
        paletteMapping((color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f)) = i.toByte
        i += 1
      }
      colorCount = counts.size + hasTransparent
      populationBias = Math.exp(-1.375 / colorCount).toFloat
    }

    // Fill rest of palette mapping
    var r = 0
    while (r < 32) {
      val rr = r << 3 | r >>> 2
      var g  = 0
      while (g < 32) {
        val gg = g << 3 | g >>> 2
        var b  = 0
        while (b < 32) {
          val c2 = r << 10 | g << 5 | b
          if (paletteMapping(c2) == 0) {
            val bb   = b << 3 | b >>> 2
            var dist = Double.MaxValue
            var i2   = 1
            while (i2 < colorCount) {
              val newDist = Math.min(dist, differenceAnalyzing(paletteArray(i2), rr, gg, bb))
              if (dist > newDist) {
                dist = newDist
                paletteMapping(c2) = i2.toByte
              }
              dist = newDist
              i2 += 1
            }
          }
          b += 1
        }
        g += 1
      }
      r += 1
    }
  }

  // === Big Palette / Reductive Analysis ===

  /** Builds the mapping from RGB555 colors to their closest match in BIG_PALETTE by calculating the closest match for every color. The palette should have 1024 colors, if possible, but can be
    * smaller.
    */
  def alterBigPalette(palette: Array[Int]): Unit = {
    if (PaletteReducer.bigPaletteMapping.isEmpty) {
      PaletteReducer.bigPaletteMapping = new Array[Char](0x8000)
    }
    val plen = palette.length
    if (!(palette eq PaletteReducer.BIG_PALETTE)) {
      System.arraycopy(palette, 0, PaletteReducer.BIG_PALETTE, 0, Math.min(plen, PaletteReducer.BIG_PALETTE.length))
    }
    if (plen < PaletteReducer.BIG_PALETTE.length) {
      Arrays.fill(PaletteReducer.BIG_PALETTE, plen, 1024, 0)
    }
    var i = 0
    while (i < plen) {
      val color = palette(i)
      if ((color & 0x80) != 0) {
        PaletteReducer.bigPaletteMapping((color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f)) = i.toChar
      }
      i += 1
    }
    var r = 0
    while (r < 32) {
      val rr = r << 3 | r >>> 2
      var g  = 0
      while (g < 32) {
        val gg = g << 3 | g >>> 2
        var b  = 0
        while (b < 32) {
          val c2 = r << 10 | g << 5 | b
          if (PaletteReducer.bigPaletteMapping(c2) == 0) {
            val bb   = b << 3 | b >>> 2
            var dist = 1e100
            var j    = 1
            while (j < plen) {
              val newDist = Math.min(dist, differenceMatch(PaletteReducer.BIG_PALETTE(j), rr, gg, bb))
              if (dist > newDist) {
                dist = newDist
                PaletteReducer.bigPaletteMapping(c2) = j.toChar
              }
              dist = newDist
              j += 1
            }
          }
          b += 1
        }
        g += 1
      }
      r += 1
    }
    PaletteReducer.bigPaletteLoaded = true
  }

  /** Writes the current bigPaletteMapping to the given FileHandle. */
  def writeBigPalette(filename: FileHandle | Null): Unit =
    if (PaletteReducer.bigPaletteMapping.nonEmpty) {
      val target =
        if (filename != null) filename.nn
        else {
          // Cannot use Gdx.files in SGE; require an explicit file handle
          return
        }
      target.writeString(new String(PaletteReducer.bigPaletteMapping), false, sge.utils.Nullable("UTF8"))
    }

  /** Loads the bigPaletteMapping from the given file, using the provided palette. */
  def loadBigPalette(file: FileHandle, palette: Array[Int]): Unit = {
    val plen = palette.length
    System.arraycopy(palette, 0, PaletteReducer.BIG_PALETTE, 0, Math.min(plen, PaletteReducer.BIG_PALETTE.length))
    if (plen < PaletteReducer.BIG_PALETTE.length) {
      Arrays.fill(PaletteReducer.BIG_PALETTE, plen, 1024, 0)
    }
    if (PaletteReducer.bigPaletteMapping.isEmpty) {
      PaletteReducer.bigPaletteMapping = new Array[Char](0x8000)
    }
    file.readString(sge.utils.Nullable("UTF8")).getChars(0, 0x8000, PaletteReducer.bigPaletteMapping, 0)
    PaletteReducer.bigPaletteLoaded = true
  }

  /** Builds the BIG_PALETTE mapping. If already loaded, this does nothing. */
  def buildBigPalette(): Unit = {
    if (PaletteReducer.bigPaletteLoaded) return
    if (PaletteReducer.bigPaletteMapping.isEmpty) {
      PaletteReducer.bigPaletteMapping = new Array[Char](0x8000)
    }
    // In SGE we don't have Gdx.files.classpath, so we always build from scratch.
    alterBigPalette(PaletteReducer.BIG_PALETTE)
  }

  /** Analyzes pixmap for color count and frequency using the Reductive algorithm with defaults. */
  def analyzeReductive(pixmap: Pixmap): Unit =
    analyzeReductive(pixmap, 100)

  /** Analyzes pixmap using the Reductive algorithm with the given threshold. */
  def analyzeReductive(pixmap: Pixmap, threshold: Double): Unit =
    analyzeReductive(pixmap, threshold, 256)

  /** Analyzes pixmap for color count and frequency using the Reductive algorithm. This uses the BIG_PALETTE (1024 colors) and maps image colors through it before selecting a final palette.
    */
  def analyzeReductive(pixmap: Pixmap, threshold0: Double, limit0: Int): Unit = {
    buildBigPalette()
    Arrays.fill(paletteArray, 0)
    Arrays.fill(paletteMapping, 0.toByte)
    var color     = 0
    val limit     = Math.min(Math.max(limit0, 2), 256)
    val threshold = threshold0 / Math.min(0.3, Math.pow(limit + 16, 1.45) * 0.00013333)
    val width     = pixmap.width.toInt
    val height    = pixmap.height.toInt
    val counts    = new mutable.HashMap[Int, Int]()
    val reds      = new Array[Int](limit)
    val greens    = new Array[Int](limit)
    val blues     = new Array[Int](limit)
    var y         = 0
    while (y < height) {
      var x = 0
      while (x < width) {
        color = pixmap.getPixel(Pixels(x), Pixels(y)) & 0xf8f8f880
        if ((color & 0x80) != 0) {
          color = PaletteReducer.BIG_PALETTE(PaletteReducer.bigPaletteMapping(PaletteReducer.shrink(color)))
          counts(color) = counts.getOrElse(color, 0) + 1
        }
        x += 1
      }
      y += 1
    }
    val cs     = counts.size
    val sorted = new scala.collection.mutable.ArrayBuffer[(Int, Int)](cs)
    for ((k, v) <- counts) sorted += ((k, v))
    sorted.sortInPlaceBy(-_._2)
    var thresholdVar = threshold
    if (cs < limit) {
      var i = 1
      for ((c, _) <- sorted) {
        color = c
        paletteArray(i) = color
        paletteMapping((color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f)) = i.toByte
        reds(i) = color >>> 24
        greens(i) = color >>> 16 & 255
        blues(i) = color >>> 8 & 255
        i += 1
      }
      colorCount = i
      populationBias = Math.exp(-1.375 / colorCount).toFloat
    } else {
      var i = 1
      boundary {
        var iterations = 0
        while (iterations < 4) {
          var c = 0
          while (c < sorted.size) {
            color = sorted(c)._1
            c += 1
            var tooClose = false
            boundary {
              var j = 1
              while (j < i) {
                if (differenceAnalyzing(color, paletteArray(j)) < thresholdVar) {
                  tooClose = true
                  break(())
                }
                j += 1
              }
            }
            if (!tooClose) {
              sorted.remove(c - 1)
              c -= 1
              paletteArray(i) = color
              paletteMapping((color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f)) = i.toByte
              reds(i) = color >>> 24
              greens(i) = color >>> 16 & 255
              blues(i) = color >>> 8 & 255
              i += 1
              if (i >= limit) break(())
            }
          }
          thresholdVar *= 0.65
          iterations += 1
        }
      }
      colorCount = i
      populationBias = Math.exp(-1.375 / colorCount).toFloat
    }

    var r = 0
    while (r < 32) {
      val rr = r << 3 | r >>> 2
      var g  = 0
      while (g < 32) {
        val gg = g << 3 | g >>> 2
        var b  = 0
        while (b < 32) {
          val c2 = r << 10 | g << 5 | b
          if (paletteMapping(c2) == 0) {
            val bb   = b << 3 | b >>> 2
            var dist = Double.MaxValue
            var i2   = 1
            while (i2 < colorCount) {
              val newDist = Math.min(dist, differenceAnalyzing(reds(i2), greens(i2), blues(i2), rr, gg, bb))
              if (dist > newDist) {
                dist = newDist
                paletteMapping(c2) = i2.toByte
              }
              dist = newDist
              i2 += 1
            }
          }
          b += 1
        }
        g += 1
      }
      r += 1
    }
  }

  /** Analyzes multiple pixmaps using the Reductive algorithm. */
  def analyzeReductive(pixmaps: Array[Pixmap]): Unit =
    if (pixmaps != null && pixmaps.nonEmpty) analyzeReductive(pixmaps, pixmaps.length, 100, 256)

  /** Analyzes multiple pixmaps using the Reductive algorithm with the given threshold. */
  def analyzeReductive(pixmaps: Array[Pixmap], threshold: Double): Unit =
    if (pixmaps != null && pixmaps.nonEmpty) analyzeReductive(pixmaps, pixmaps.length, threshold, 256)

  /** Analyzes multiple pixmaps using the Reductive algorithm with the given threshold and limit. */
  def analyzeReductive(pixmaps: Array[Pixmap], threshold: Double, limit: Int): Unit =
    if (pixmaps != null && pixmaps.nonEmpty) analyzeReductive(pixmaps, pixmaps.length, threshold, limit)

  /** Analyzes all pixmaps for color count and frequency using the Reductive algorithm, aggregating color frequencies across ALL frames. Uses BIG_PALETTE (1024 colors) to map image colors before
    * selecting a final palette.
    */
  def analyzeReductive(pixmaps: Array[Pixmap], pixmapCount: Int, threshold0: Double, limit0: Int): Unit = {
    buildBigPalette()
    Arrays.fill(paletteArray, 0)
    Arrays.fill(paletteMapping, 0.toByte)
    var color     = 0
    val limit     = Math.min(Math.max(limit0, 2), 256)
    val threshold = threshold0 / Math.min(0.3, Math.pow(limit + 16, 1.45) * 0.00013333)
    val counts    = new mutable.HashMap[Int, Int]()
    val reds      = new Array[Int](limit)
    val greens    = new Array[Int](limit)
    val blues     = new Array[Int](limit)
    var pi        = 0
    while (pi < pixmapCount && pi < pixmaps.length) {
      val pixmap = pixmaps(pi)
      val width  = pixmap.width.toInt
      val height = pixmap.height.toInt
      var y      = 0
      while (y < height) {
        var x = 0
        while (x < width) {
          color = pixmap.getPixel(Pixels(x), Pixels(y)) & 0xf8f8f880
          if ((color & 0x80) != 0) {
            color = PaletteReducer.BIG_PALETTE(PaletteReducer.bigPaletteMapping(PaletteReducer.shrink(color)))
            counts(color) = counts.getOrElse(color, 0) + 1
          }
          x += 1
        }
        y += 1
      }
      pi += 1
    }
    val cs     = counts.size
    val sorted = new scala.collection.mutable.ArrayBuffer[(Int, Int)](cs)
    for ((k, v) <- counts) sorted += ((k, v))
    sorted.sortInPlaceBy(-_._2)
    var thresholdVar = threshold
    if (cs < limit) {
      var i = 1
      for ((c, _) <- sorted) {
        color = c
        paletteArray(i) = color
        paletteMapping((color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f)) = i.toByte
        reds(i) = color >>> 24
        greens(i) = color >>> 16 & 255
        blues(i) = color >>> 8 & 255
        i += 1
      }
      colorCount = i
      populationBias = Math.exp(-1.375 / colorCount).toFloat
    } else {
      var i = 1
      boundary {
        var iterations = 0
        while (iterations < 4) {
          var c = 0
          while (c < sorted.size) {
            color = sorted(c)._1
            c += 1
            var tooClose = false
            boundary {
              var j = 1
              while (j < i) {
                if (differenceAnalyzing(color, paletteArray(j)) < thresholdVar) {
                  tooClose = true
                  break(())
                }
                j += 1
              }
            }
            if (!tooClose) {
              sorted.remove(c - 1)
              c -= 1
              paletteArray(i) = color
              paletteMapping((color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f)) = i.toByte
              reds(i) = color >>> 24
              greens(i) = color >>> 16 & 255
              blues(i) = color >>> 8 & 255
              i += 1
              if (i >= limit) break(())
            }
          }
          thresholdVar *= 0.65
          iterations += 1
        }
      }
      colorCount = i
      populationBias = Math.exp(-1.375 / colorCount).toFloat
    }

    var r = 0
    while (r < 32) {
      val rr = r << 3 | r >>> 2
      var g  = 0
      while (g < 32) {
        val gg = g << 3 | g >>> 2
        var b  = 0
        while (b < 32) {
          val c2 = r << 10 | g << 5 | b
          if (paletteMapping(c2) == 0) {
            val bb   = b << 3 | b >>> 2
            var dist = Double.MaxValue
            var i2   = 1
            while (i2 < colorCount) {
              val newDist = Math.min(dist, differenceAnalyzing(reds(i2), greens(i2), blues(i2), rr, gg, bb))
              if (dist > newDist) {
                dist = newDist
                paletteMapping(c2) = i2.toByte
              }
              dist = newDist
              i2 += 1
            }
          }
          b += 1
        }
        g += 1
      }
      r += 1
    }
  }

  // === Random Color Selection ===

  /** Retrieves a random non-0 color index for the palette this would reduce to. The index has a higher likelihood for colors that are used more often in reductions.
    */
  def randomColorIndex(random: java.util.Random): Byte =
    paletteMapping(random.nextInt() >>> 17)

  /** Retrieves a random non-transparent color from the palette this would reduce to. Returns an RGBA8888 int.
    */
  def randomColor(random: java.util.Random): Int =
    paletteArray(paletteMapping(random.nextInt() >>> 17) & 255)

  /** Looks up color and finds the closest color in the palette. Both parameter and return are RGBA8888 ints. */
  def reduceSingle(color: Int): Int = {
    if ((color & 0x80) == 0) return 0
    paletteArray(paletteMapping((color >>> 17 & 0x7c00) | (color >>> 14 & 0x3e0) | (color >>> 11 & 0x1f)) & 0xff)
  }

  // === WrenOriginal Dither ===

  /** WrenOriginal: an older variant of Wren dither that uses per-channel blue noise + R2 hash with non-gamma-corrected error diffusion. Uses Floyd-Steinberg weights.
    */
  def reduceWrenOriginal(pixmap: Pixmap): Pixmap = {
    val hasTransparent                                                                          = paletteArray(0) == 0
    val lineLen                                                                                 = pixmap.width.toInt; val h = pixmap.height.toInt
    val (curErrorRed, nextErrorRed, curErrorGreen, nextErrorGreen, curErrorBlue, nextErrorBlue) = ensureErrorArrays(lineLen)
    val oldBlending                                                                             = pixmap.blending; pixmap.setBlending(Pixmap.Blending.None)
    // 0x1p-8f = 0.00390625f
    val w1       = (32.0f * _ditherStrength * (populationBias * populationBias)).toFloat; val w3 = w1 * 3f; val w5 = w1 * 5f; val w7 = w1 * 7f
    val strength = 0.2f * _ditherStrength / (populationBias * populationBias * populationBias * populationBias)
    val limit    = 5f + 125f / Math.sqrt(colorCount + 1.5).toFloat
    val dmul     = 0.00390625f // 0x1p-8f
    // 0x1p-16f = 1.5258789e-5f; 0x1p+6f = 64f; 0x1p-15f = 3.0517578e-5f; 0x1p+7f = 128f
    var y = 0;
    while (y < h) {
      val ny = y + 1
      System.arraycopy(nextErrorRed, 0, curErrorRed, 0, lineLen); System.arraycopy(nextErrorGreen, 0, curErrorGreen, 0, lineLen); System.arraycopy(nextErrorBlue, 0, curErrorBlue, 0, lineLen)
      Arrays.fill(nextErrorRed, 0, lineLen, 0f); Arrays.fill(nextErrorGreen, 0, lineLen, 0f); Arrays.fill(nextErrorBlue, 0, lineLen, 0f)
      var px = 0;
      while (px < lineLen) {
        val color = pixmap.getPixel(Pixels(px), Pixels(y))
        if (hasTransparent && (color & 0x80) == 0) pixmap.drawPixel(Pixels(px), Pixels(y), 0)
        else {
          val er = Math.min(
            Math.max(
              ((PaletteReducer.TRI_BLUE_NOISE(
                (px & 63) | (y & 63) << 6
              ) + 0.5f) + ((((px + 1).toLong * 0xc13fa9a902a6328fL + (y + 1).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 1.5258789e-5f - 64f)) * strength + curErrorRed(px),
              -limit
            ),
            limit
          )
          val eg = Math.min(
            Math.max(
              ((PaletteReducer.TRI_BLUE_NOISE_B(
                (px & 63) | (y & 63) << 6
              ) + 0.5f) + ((((px + 3).toLong * 0xc13fa9a902a6328fL + (y - 1).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 1.5258789e-5f - 64f)) * strength + curErrorGreen(px),
              -limit
            ),
            limit
          )
          val eb = Math.min(
            Math.max(
              ((PaletteReducer.TRI_BLUE_NOISE_C(
                (px & 63) | (y & 63) << 6
              ) + 0.5f) + ((((px + 2).toLong * 0xc13fa9a902a6328fL + (y - 4).toLong * 0x91e10da5c79e7b1dL) >>> 41) * 1.5258789e-5f - 64f)) * strength + curErrorBlue(px),
              -limit
            ),
            limit
          )
          val rr   = Math.min(Math.max(((color >>> 24) + er + 0.5f).toInt, 0), 0xff)
          val gg   = Math.min(Math.max(((color >>> 16 & 0xff) + eg + 0.5f).toInt, 0), 0xff)
          val bb   = Math.min(Math.max(((color >>> 8 & 0xff) + eb + 0.5f).toInt, 0), 0xff)
          val pidx = paletteMapping(((rr << 7) & 0x7c00) | ((gg << 2) & 0x3e0) | (bb >>> 3))
          val used = paletteArray(pidx & 0xff)
          pixmap.drawPixel(Pixels(px), Pixels(y), used)
          val rdiff = dmul * ((color >>> 24) - (used >>> 24))
          val gdiff = dmul * ((color >>> 16 & 255) - (used >>> 16 & 255))
          val bdiff = dmul * ((color >>> 8 & 255) - (used >>> 8 & 255))
          if (px < lineLen - 1) { curErrorRed(px + 1) += rdiff * w7; curErrorGreen(px + 1) += gdiff * w7; curErrorBlue(px + 1) += bdiff * w7 }
          if (ny < h) {
            if (px > 0) { nextErrorRed(px - 1) += rdiff * w3; nextErrorGreen(px - 1) += gdiff * w3; nextErrorBlue(px - 1) += bdiff * w3 }
            if (px < lineLen - 1) { nextErrorRed(px + 1) += rdiff * w1; nextErrorGreen(px + 1) += gdiff * w1; nextErrorBlue(px + 1) += bdiff * w1 }
            nextErrorRed(px) += rdiff * w5; nextErrorGreen(px) += gdiff * w5; nextErrorBlue(px) += bdiff * w5
          }
        };
        px += 1
      };
      y += 1
    }
    pixmap.setBlending(oldBlending); pixmap
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
      val x = i & TBM_MASK; val y = i >>> TBM_BITS
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

  // === IntComparator-based sorting (from FastUtil, used by hue-wise analysis) ===

  /** Compares shrunken indices (RGB555) by lightness as Oklab knows it. */
  val lightnessComparator: (Int, Int) => Int = (k1, k2) => java.lang.Float.floatToIntBits(OKLAB(0)(k2) - OKLAB(0)(k1))

  /** Compares shrunken indices (RGB555) by hue as Oklab knows it. */
  val hueComparator: (Int, Int) => Int = (k1, k2) => java.lang.Float.floatToIntBits(OKLAB(3)(k2) - OKLAB(3)(k1))

  private def swapInts(items: Array[Int], first: Int, second: Int): Unit = {
    val firstValue = items(first)
    items(first) = items(second)
    items(second) = firstValue
  }

  /** Transforms two consecutive sorted ranges into a single sorted range. Based on FastUtil (Apache License 2.0).
    */
  private def inPlaceMerge(items: Array[Int], from: Int, mid0: Int, to: Int, comp: (Int, Int) => Int): Unit = boundary {
    if (from >= mid0 || mid0 >= to) { break(()) }
    if (to - from == 2) {
      if (comp(items(mid0), items(from)) < 0) { swapInts(items, from, mid0) }
      break(())
    }

    var firstCut  = 0
    var secondCut = 0

    if (mid0 - from > to - mid0) {
      firstCut = from + (mid0 - from) / 2
      secondCut = lowerBound(items, mid0, to, firstCut, comp)
    } else {
      secondCut = mid0 + (to - mid0) / 2
      firstCut = upperBound(items, from, mid0, secondCut, comp)
    }

    val first2  = firstCut
    val middle2 = mid0
    val last2   = secondCut
    if (middle2 != first2 && middle2 != last2) {
      var first1 = first2; var last1 = middle2
      while (first1 < { last1 -= 1; last1 }) swapInts(items, { val r = first1; first1 += 1; r }, last1)
      first1 = middle2; last1 = last2
      while (first1 < { last1 -= 1; last1 }) swapInts(items, { val r = first1; first1 += 1; r }, last1)
      first1 = first2; last1 = last2
      while (first1 < { last1 -= 1; last1 }) swapInts(items, { val r = first1; first1 += 1; r }, last1)
    }

    val mid = firstCut + secondCut - mid0
    inPlaceMerge(items, from, firstCut, mid, comp)
    inPlaceMerge(items, mid, secondCut, to, comp)
  }

  private def lowerBound(items: Array[Int], from0: Int, to: Int, pos: Int, comp: (Int, Int) => Int): Int = {
    var from = from0
    var len  = to - from
    while (len > 0) {
      val half   = len / 2
      val middle = from + half
      if (comp(items(middle), items(pos)) < 0) {
        from = middle + 1
        len -= half + 1
      } else {
        len = half
      }
    }
    from
  }

  private def upperBound(items: Array[Int], from0: Int, to: Int, pos: Int, comp: (Int, Int) => Int): Int = {
    var from = from0
    var len  = to - from
    while (len > 0) {
      val half   = len / 2
      val middle = from + half
      if (comp(items(pos), items(middle)) < 0) {
        len = half
      } else {
        from = middle + 1
        len -= half + 1
      }
    }
    from
  }

  /** Sorts the specified range [from, to) of elements using an in-place mergesort with a custom comparator. Based on FastUtil (Apache License 2.0). `to` is the exclusive end index.
    */
  def sortInts(items: Array[Int], from: Int, to: Int, c: (Int, Int) => Int): Unit = boundary {
    if (to <= 0) { break(()) }
    if (from < 0 || from >= items.length || to > items.length) {
      throw new IllegalArgumentException("The given from/to range in sortInts() is invalid.")
    }
    val length = to - from

    // Insertion sort on smallest arrays, less than 16 items
    if (length < 16) {
      var i = from
      while (i < to) {
        var j = i
        while (j > from && c(items(j - 1), items(j)) > 0) {
          swapInts(items, j, j - 1)
          j -= 1
        }
        i += 1
      }
      break(())
    }

    // Recursively sort halves
    val mid = (from + to) >>> 1
    sortInts(items, from, mid, c)
    sortInts(items, mid, to, c)

    // If list is already sorted, nothing left to do.
    if (c(items(mid - 1), items(mid)) <= 0) { break(()) }

    // Merge sorted halves
    inPlaceMerge(items, from, mid, to, c)
  }

  /** Compares items by lightness using OKLAB and swaps if out of order. */
  def compareSwap(ints: Array[Int], a: Int, b: Int): Unit =
    if (OKLAB(0)(ints(a | 16)) > OKLAB(0)(ints(b | 16))) {
      val t = ints(a); val st = ints(a | 16); ints(a) = ints(b); ints(a | 16) = ints(b | 16); ints(b) = t; ints(b | 16) = st
    }

  /** Sorting network for 8 elements. */
  def sort8(i8: Array[Int]): Unit = {
    compareSwap(i8, 0, 1); compareSwap(i8, 2, 3); compareSwap(i8, 0, 2); compareSwap(i8, 1, 3); compareSwap(i8, 1, 2)
    compareSwap(i8, 4, 5); compareSwap(i8, 6, 7); compareSwap(i8, 4, 6); compareSwap(i8, 5, 7); compareSwap(i8, 5, 6)
    compareSwap(i8, 0, 4); compareSwap(i8, 1, 5); compareSwap(i8, 1, 4); compareSwap(i8, 2, 6); compareSwap(i8, 3, 7)
    compareSwap(i8, 3, 6); compareSwap(i8, 2, 4); compareSwap(i8, 3, 5); compareSwap(i8, 3, 4)
  }

  /** Sorting network for 16 elements. */
  def sort16(i16: Array[Int]): Unit = {
    compareSwap(i16, 0, 1); compareSwap(i16, 2, 3); compareSwap(i16, 4, 5); compareSwap(i16, 6, 7); compareSwap(i16, 8, 9); compareSwap(i16, 10, 11); compareSwap(i16, 12, 13); compareSwap(i16, 14, 15)
    compareSwap(i16, 0, 2); compareSwap(i16, 4, 6); compareSwap(i16, 8, 10); compareSwap(i16, 12, 14); compareSwap(i16, 1, 3); compareSwap(i16, 5, 7); compareSwap(i16, 9, 11); compareSwap(i16, 13, 15)
    compareSwap(i16, 0, 4); compareSwap(i16, 8, 12); compareSwap(i16, 1, 5); compareSwap(i16, 9, 13); compareSwap(i16, 2, 6); compareSwap(i16, 10, 14); compareSwap(i16, 3, 7); compareSwap(i16, 11, 15)
    compareSwap(i16, 0, 8); compareSwap(i16, 1, 9); compareSwap(i16, 2, 10); compareSwap(i16, 3, 11); compareSwap(i16, 4, 12); compareSwap(i16, 5, 13); compareSwap(i16, 6, 14); compareSwap(i16, 7, 15)
    compareSwap(i16, 5, 10); compareSwap(i16, 6, 9); compareSwap(i16, 3, 12); compareSwap(i16, 13, 14); compareSwap(i16, 7, 11); compareSwap(i16, 1, 2); compareSwap(i16, 4, 8)
    compareSwap(i16, 1, 4); compareSwap(i16, 7, 13); compareSwap(i16, 2, 8); compareSwap(i16, 11, 14); compareSwap(i16, 2, 4); compareSwap(i16, 5, 6); compareSwap(i16, 9, 10); compareSwap(i16, 11, 13)
    compareSwap(i16, 3, 8); compareSwap(i16, 7, 12); compareSwap(i16, 6, 8); compareSwap(i16, 10, 12); compareSwap(i16, 3, 5); compareSwap(i16, 7, 9)
    compareSwap(i16, 3, 4); compareSwap(i16, 5, 6); compareSwap(i16, 7, 8); compareSwap(i16, 9, 10); compareSwap(i16, 11, 12); compareSwap(i16, 6, 7); compareSwap(i16, 8, 9)
  }

  private def reverseShortBits(v0: Int): Int = {
    var v = v0; v = ((v >>> 1) & 0x5555) | ((v & 0x5555) << 1); v = ((v >>> 2) & 0x3333) | ((v & 0x3333) << 2); v = ((v >>> 4) & 0x0f0f) | ((v & 0x0f0f) << 4);
    v = ((v >>> 8) & 0x00ff) | ((v & 0x00ff) << 8); v
  }

  private def interleaveBytes(a: Int, b: Int): Int = {
    var x = a & 0xff; var y = b & 0xff; x = (x | (x << 4)) & 0x0f0f; y = (y | (y << 4)) & 0x0f0f; x = (x | (x << 2)) & 0x3333; y = (y | (y << 2)) & 0x3333; x = (x | (x << 1)) & 0x5555;
    y = (y | (y << 1)) & 0x5555; x | (y << 1)
  }

  def forwardLight(L: Float): Float = Math.sqrt((L * L * L).toDouble).toFloat

  def reverseLight(L0: Float): Float = {
    val ix  = java.lang.Float.floatToRawIntBits(L0); val x0 = L0
    var ix2 = (ix >>> 2) + (ix >>> 4); ix2 += (ix2 >>> 4); ix2 += (ix2 >>> 8) + 0x2a5137a0
    var L   = java.lang.Float.intBitsToFloat(ix2); L = 0.33333334f * (2f * L + x0 / (L * L)); L = 0.33333334f * (1.9999999f * L + x0 / (L * L)); L * L
  }

  /** Stores Oklab components corresponding to RGB555 indices. */
  val OKLAB: Array[Array[Float]] = {
    val result = Array.ofDim[Float](4, 0x8000); var idx = 0; var ri = 0
    while (ri < 32) {
      val rf = (ri * ri * 0.0010405827263267429).toFloat; var gi = 0
      while (gi < 32) {
        val gf = (gi * gi * 0.0010405827263267429).toFloat; var bi = 0
        while (bi < 32) {
          val bf = (bi * bi * 0.0010405827263267429).toFloat
          val lf = OtherMath.cbrtPositive(0.4121656120f * rf + 0.5362752080f * gf + 0.0514575653f * bf)
          val mf = OtherMath.cbrtPositive(0.2118591070f * rf + 0.6807189584f * gf + 0.1074065790f * bf)
          val sf = OtherMath.cbrtPositive(0.0883097947f * rf + 0.2818474174f * gf + 0.6302613616f * bf)
          result(0)(idx) = forwardLight(0.2104542553f * lf + 0.7936177850f * mf - 0.0040720468f * sf)
          result(1)(idx) = 1.9779984951f * lf - 2.4285922050f * mf + 0.4505937099f * sf
          result(2)(idx) = 0.0259040371f * lf + 0.7827717662f * mf - 0.8086757660f * sf
          result(3)(idx) = OtherMath.atan2(result(2)(idx), result(1)(idx))
          idx += 1; bi += 1
        };
        gi += 1
      };
      ri += 1
    }
    result
  }

  /** Converts Oklab color components to an RGBA8888 int color. */
  def oklabToRGB(L: Float, A: Float, B: Float, alpha: Float): Int = {
    val lRev = reverseLight(L); var l = lRev + 0.3963377774f * A + 0.2158037573f * B; var m = lRev - 0.1055613458f * A - 0.0638541728f * B; var s = lRev - 0.0894841775f * A - 1.2914855480f * B
    l *= l * l; m *= m * m; s *= s * s
    val r = (Math.sqrt(Math.min(Math.max(+4.0767245293f * l - 3.3072168827f * m + 0.2307590544f * s, 0.0f), 1.0f)) * 255.999f).toInt
    val g = (Math.sqrt(Math.min(Math.max(-1.2681437731f * l + 2.6093323231f * m - 0.3411344290f * s, 0.0f), 1.0f)) * 255.999f).toInt
    val b = (Math.sqrt(Math.min(Math.max(-0.0041119885f * l - 0.7034763098f * m + 1.7068625689f * s, 0.0f), 1.0f)) * 255.999f).toInt
    r << 24 | g << 16 | b << 8 | (alpha * 255.999f).toInt
  }

  /** Converts RGB555 to approximated RGBA8888. */
  def stretch(color: Int): Int =
    (color << 17 & 0xf8000000) | (color << 12 & 0x07000000) | (color << 14 & 0xf80000) | (color << 9 & 0x070000) | (color << 11 & 0xf800) | (color << 6 & 0x0700) | 0xff

  def bayer(x: Int, y: Int, bits: Int): Int =
    reverseShortBits(interleaveBytes(x ^ y, y)) >>> (16 - bits - bits)

  /** A 256-entry lookup table computed via [[OtherMath.triangularRemap]]. */
  val TRIANGULAR_BYTE_LOOKUP: Array[Float] = {
    val arr = new Array[Float](256); var i = 0
    while (i < 256) { arr(i) = OtherMath.triangularRemap(i + 0.5f, 256); i += 1 }
    arr
  }

  // === Big Palette for Reductive Analysis ===

  /** Whether the big palette mapping has been loaded or built. */
  var bigPaletteLoaded:  Boolean     = false
  var bigPaletteMapping: Array[Char] = Array.emptyCharArray // lazily initialized to 0x8000 elements

  /** An expanded palette with 1024 colors for reductive analysis. */
  val BIG_PALETTE: Array[Int] = Array(
    0x00000000, 0x000000ff, 0xffffffff, 0x080808ff, 0x101010ff, 0x181818ff, 0x202020ff, 0x292929ff, 0x313131ff, 0x393939ff, 0x414141ff, 0x4a4a4aff, 0x525252ff, 0x5a5a5aff, 0x626262ff, 0x6a6a6aff,
    0x737373ff, 0x7b7b7bff, 0x838383ff, 0x8b8b8bff, 0x949494ff, 0x9c9c9cff, 0xa4a4a4ff, 0xacacacff, 0xb4b4b4ff, 0xbdbdbdff, 0xc5c5c5ff, 0xcdcdcdff, 0xd5d5d5ff, 0xdededeff, 0xe6e6e6ff, 0xeeeeeeff,
    0xf6f6f6ff, 0xf225b9ff, 0xe3db38ff, 0xad8a65ff, 0x77377bff, 0x33d8c9ff, 0x2b7cc9ff, 0xf5b43dff, 0xc85b67ff, 0x379b2eff, 0xf32fedff, 0xacf7a5ff, 0x194847ff, 0x909dceff, 0x7041c7ff, 0xcd1d7dff,
    0x78752cff, 0xf2c2a4ff, 0x4c173dff, 0xbf68c0ff, 0xcd9327ff, 0x95403aff, 0xcb20c5ff, 0x76d973ff, 0x53859aff, 0x3a1e7eff, 0xde5c23ff, 0xe7f83eff, 0xb6a66bff, 0x2b0b16ff, 0x8d4f87ff, 0x3ef0e9ff,
    0xe01a2cff, 0x398cf1ff, 0x4a1ce2ff, 0xcd717cff, 0x9b1e90ff, 0x6ab634ff, 0x2b6d54ff, 0xa5b8eeff, 0x110f3aff, 0x8a5fccff, 0xef2584ff, 0x8e8c2eff, 0xecdea6ff, 0x63344dff, 0xc885d4ff, 0x991fc6ff,
    0xd7ad2aff, 0xa75744ff, 0xe559c6ff, 0x64f888ff, 0x619ca3ff, 0x655194ff, 0xed7228ff, 0xbb224cff, 0xc9bd89ff, 0x452515ff, 0xb36286ff, 0xf5332bff, 0x44a4f3ff, 0x5244eaff, 0x84451fff, 0xf1897eff,
    0xb64c8fff, 0x70cd38ff, 0x568a70ff, 0x8cd6ecff, 0x1d3665ff, 0x937deaff, 0xf3668bff, 0x97a536ff, 0x8f5a72ff, 0xe98deeff, 0xb024eeff, 0x1f359cff, 0xb5704cff, 0x8b1c63ff, 0xeb6edcff, 0x256323ff,
    0x4fbfb0ff, 0x6864a0ff, 0xd24059ff, 0xa0c4aeff, 0x1c1ee8ff, 0x26491bff, 0xa67faaff, 0x823da9ff, 0x37ccf1ff, 0x3771edff, 0x965b20ff, 0xf1a385ff, 0x52111aff, 0xa9a385ff, 0x4af53bff, 0x39a67bff,
    0x94f3eaff, 0x215175ff, 0x828cf3ff, 0x7521e8ff, 0x9a2418ff, 0x9fbc3aff, 0xa27a83ff, 0xdeafebff, 0x511671ff, 0xa95cecff, 0x2751b4ff, 0xcf8a58ff, 0x7d5054ff, 0x297b27ff, 0x77d6b5ff, 0x6073bbff,
    0x4b1ba9ff, 0xe87461ff, 0xe5f182ff, 0x525324ff, 0xc396beff, 0x8b52a8ff, 0x9f6d24ff, 0x74181fff, 0xc581a7ff, 0x3bb678ff, 0x27648bff, 0x7c51ebff, 0xb13f25ff, 0xdf4c9eff, 0xa0dd3aff, 0x7e946dff,
    0xebc4eaff, 0x5b3883ff, 0xb383f2ff, 0x286bbbff, 0xdda562ff, 0x8d6562ff, 0x378a33ff, 0xd548ecff, 0x5af6c2ff, 0x708ec7ff, 0x543cb3ff, 0xf06468ff, 0xa8246bff, 0x596d2bff, 0xcba4bcff, 0x35134aff,
    0xa762afff, 0xf42e5cff, 0xb38427ff, 0x803b47ff, 0xef78b9ff, 0xaf45b2ff, 0x3acd85ff, 0x297894ff, 0x1b1988ff, 0xc05924ff, 0xed8aa7ff, 0xa7f73fff, 0x82a77bff, 0x844a6fff, 0xbe2123ff, 0x3393cbff,
    0xd0ba67ff, 0x1d1ab9ff, 0xae6d80ff, 0x6e456cff, 0x31ad2dff, 0x246161ff, 0x7aafe2ff, 0x5e5ac9ff, 0xb7466fff, 0x6a8b32ff, 0xafdcb8ff, 0x453863ff, 0x9c73d1ff, 0x731eb9ff, 0xb4a62eff, 0x99685cff,
    0xf2a1c5ff, 0xaa7fc5ff, 0x38e996ff, 0x2693a1ff, 0x31488bff, 0x111212ff, 0x951d3fff, 0x91bc7fff, 0x182e3fff, 0x87668bff, 0xd2442bff, 0x3da9d0ff, 0xdcd17aff, 0x243fdeff, 0x61381bff, 0xc98a8bff,
    0x857081ff, 0x2ec534ff, 0x2d7f6bff, 0x74c5deff, 0x151764ff, 0x7371eaff, 0xdb5b84ff, 0x75a33dff, 0xddf8c6ff, 0x55567dff, 0xaf9cefff, 0x9645e3ff, 0xc6c634ff, 0x8a684eff, 0x6c1755ff, 0xc970ebff,
    0x32aeadff, 0x486597ff, 0xf29032ff, 0xb0565cff, 0xa3d385ff, 0x1b3216ff, 0x8674acff, 0x6f1a8cff, 0xf06041ff, 0x32b8e5ff, 0x265ae8ff, 0x725a24ff, 0xd1a391ff, 0x948c7fff, 0x38de39ff, 0x29936aff,
    0x5c1ad9ff, 0x711e1aff, 0xf07095ff, 0x85b557ff, 0x5c6365ff, 0xc7b7ebff, 0x2c054dff, 0x9c59e5ff, 0xfc2091ff, 0xdad818ff, 0xa9885bff, 0x70365eff, 0xe682e6ff, 0x60c8b0ff, 0x4a71aeff, 0xfaa737ff,
    0xbd565bff, 0xf93cdfff, 0xbeef9dff, 0x31432fff, 0x989badff, 0x6d41a3ff, 0x3079fdff, 0x7c681bff, 0xe9bfa0ff, 0x3f1a28ff, 0xb569a8ff, 0x33fb52ff, 0x25a775ff, 0x165271ff, 0x84a9ffff, 0x6943f0ff,
    0x8a3923ff, 0xc426a0ff, 0x90cf5dff, 0x6b7d72ff, 0xd5d2f8ff, 0x3f2864ff, 0xac76f7ff, 0xe9f211ff, 0xbaa163ff, 0x854f6dff, 0xf89df4ff, 0xc232edff, 0x68e3bbff, 0x548cbfff, 0x3d29a9ff, 0xd27066ff,
    0x8e0765ff, 0x3f5c3bff, 0xa6b5baff, 0x130b27ff, 0x7d5db6ff, 0xde2e65ff, 0x8d8120ff, 0xf9d9a8ff, 0x3c17f7ff, 0x553336ff, 0xc884b6ff, 0x8f1cabff, 0x2ac27fff, 0x1d6d82ff, 0x170562ff, 0xa1532bff,
    0xdb49b0ff, 0x9ce962ff, 0x78967eff, 0x4f4278ff, 0x9013f8ff, 0xa7082eff, 0xcabb6bff, 0x98697bff, 0x6ffec5ff, 0x5ca7cfff, 0x4849c0ff, 0xe6896fff, 0xa63175ff, 0x4c7645ff, 0xb3cfc5ff, 0x24253dff,
    0x8c78c7ff, 0xf74e70ff, 0x9d9b24ff, 0x694c43ff, 0xd99ec3ff, 0xa440beff, 0x2fdc88ff, 0x248791ff, 0x1c2c7cff, 0xb66c31ff, 0x71173dff, 0xf066bfff, 0x85b089ff, 0x5f5c89ff, 0xc13337ff, 0xd9d571ff,
    0x1028c5ff, 0x3b2c0cff, 0xa98288ff, 0x72297fff, 0x64c1ddff, 0x5166d4ff, 0xfaa377ff, 0xbd4e83ff, 0x578f4fff, 0xbfead0ff, 0x323e4fff, 0x9b92d7ff, 0x712dc7ff, 0xacb426ff, 0x7b654fff, 0xe9b8cfff,
    0x401042ff, 0xb75ed0ff, 0x33f790ff, 0x2aa29fff, 0x224992ff, 0xc98637ff, 0x8a344bff, 0x90ca93ff, 0x041d1aff, 0x6d7699ff, 0x431382ff, 0xd9503fff, 0xe8ef76ff, 0x4e4515ff, 0xba9c93ff, 0x864691ff,
    0x6cdceaff, 0x5a81e7ff, 0x561817ff, 0xd26991ff, 0x62a957ff, 0x40575fff, 0xa8ade6ff, 0x814edcff, 0xde1d8bff, 0xbace27ff, 0x8c7e59ff, 0xf9d3dbff, 0x562c55ff, 0xc979e0ff, 0x2fbcacff, 0x2864a6ff,
    0xdc9f3cff, 0xa04e58ff, 0x338607ff, 0xdc35d8ff, 0x9be59cff, 0x123629ff, 0x7a90a8ff, 0x533599ff, 0xf06b46ff, 0x605e1dff, 0xcab69eff, 0x26111fff, 0x9960a1ff, 0x72f7f6ff, 0xfb1a4aff, 0x639df9ff,
    0x5133e5ff, 0x6e3221ff, 0xe6839dff, 0xa71f98ff, 0x6cc45eff, 0x4c716dff, 0xb5c8f4ff, 0x281a58ff, 0x906befff, 0xf74499ff, 0xc8e926ff, 0x9c9762ff, 0x694666ff, 0xdb94efff, 0xa726e4ff, 0x33d7b8ff,
    0x2d7fb8ff, 0xeeb93fff, 0x28149cff, 0xb56863ff, 0x3ca00cff, 0xf257e8ff, 0xa5ffa4ff, 0x1e5036ff, 0x86aab6ff, 0x6251adff, 0xc12b60ff, 0x707723ff, 0xd9d0a7ff, 0x3b292fff, 0xab7bb1ff, 0x750ba0ff,
    0x5a55fbff, 0x844b2aff, 0xf99da8ff, 0xbe42a9ff, 0x76de64ff, 0x588b7aff, 0x36366eff, 0x8a032bff, 0xabb16bff, 0x7c5f75ff, 0xebaffdff, 0xba4cf7ff, 0x36f2c3ff, 0x339ac8ff, 0xffd341ff, 0x2e3ab4ff,
    0xc9816dff, 0x8a2a6dff, 0x44bb0cff, 0x286942ff, 0x92c5c2ff, 0x0b1731ff, 0x706cc0ff, 0xd9496cff, 0x7f9029ff, 0xe7eab0ff, 0x4e413eff, 0xbb95bfff, 0x8936b5ff, 0x996432ff, 0x561036ff, 0xd35fb9ff,
    0x7ff96aff, 0x63a586ff, 0x435081ff, 0xa42e35ff, 0xb9cb72ff, 0x232109ff, 0x8d7983ff, 0x581f74ff, 0x37b5d8ff, 0x3458cbff, 0xdb9b76ff, 0xa0477dff, 0x4bd509ff, 0x32834cff, 0x9cdfceff, 0x163146ff,
    0x7d87d1ff, 0x591cbbff, 0xf06577ff, 0x8caa2dff, 0x5f5a4bff, 0xcbafccff, 0x9b54c8ff, 0xac7d39ff, 0x6e2d45ff, 0xe77ac8ff, 0x6dbf91ff, 0x4f6a92ff, 0xbc4a3eff, 0xf832c1ff, 0xc7e578ff, 0x343914ff,
    0x9c9290ff, 0x6c3c88ff, 0x3bd0e6ff, 0x3974dfff, 0xedb57eff, 0x3b1012ff, 0xb5618cff, 0x3b9e56ff, 0xa6fad8ff, 0x214b57ff, 0x89a2e0ff, 0x6841d2ff, 0xc11985ff, 0x9ac430ff, 0x6f7456ff, 0xdac9d8ff,
    0x3d224cff, 0xad70d9ff, 0xbe963fff, 0x844653ff, 0xfa95d5ff, 0xc02ccfff, 0x76da9bff, 0x5b85a2ff, 0x3c278dff, 0xd36446ff, 0xd3ff7eff, 0x44521dff, 0xabac9bff, 0x0f0513ff, 0x7d569aff, 0x3debf3ff,
    0xdc1a48ff, 0xfecf86ff, 0x3b1cd8ff, 0x53291eff, 0xc97b99ff, 0x8c158fff, 0x42b85eff, 0x2b6567ff, 0x95bdefff, 0x140649ff, 0x755fe7ff, 0xd93f94ff, 0xa6de31ff, 0x7e8d60ff, 0xe8e4e3ff, 0x4f3b5eff,
    0xbd8be9ff, 0x8d14d9ff, 0xcfb043ff, 0x98605fff, 0xd54fe1ff, 0x7ef4a4ff, 0x659fb1ff, 0x4945a3ff, 0xe87e4dff, 0xa4265bff, 0x526c25ff, 0xb9c6a5ff, 0x231e27ff, 0x8e71aaff, 0xf64051ff, 0x4145f0ff,
    0x694228ff, 0xdb95a5ff, 0xa23aa2ff, 0x4ad265ff, 0x347f75ff, 0x9fd7fdff, 0x1d2862ff, 0x827bf9ff, 0xf05ca2ff, 0xb2f931ff, 0x8ca76aff, 0xf5feedff, 0x60556fff, 0xcda6f8ff, 0x9f40eeff, 0xbf210eff,
    0xdfca47ff, 0x1428a8ff, 0xac796aff, 0x6f2265ff, 0xe96df2ff, 0x6fb9beff, 0x5460b7ff, 0xfc9853ff, 0xbc4468ff, 0x60852bff, 0xc6e0afff, 0x0a0ff5ff, 0x343637ff, 0x9e8bb9ff, 0x6f2aabff, 0x7d5b31ff,
    0xedafb0ff, 0x3b062cff, 0xb757b3ff, 0x50ed6cff, 0x3c9982ff, 0x274377ff, 0x6f22f8ff, 0x872932ff, 0x99c172ff, 0x0b1405ff, 0x706e7eff, 0x401068ff, 0xd84213ff, 0xeee44aff, 0x0b49c0ff, 0xbd9274ff,
    0x853e76ff, 0x78d4caff, 0x5f7bc9ff, 0xd25f74ff, 0x6c9f31ff, 0xd3fbb8ff, 0x444f45ff, 0xada5c7ff, 0x8149bfff, 0x8f7439ff, 0xfec9baff, 0x53243eff, 0xca72c2ff, 0x44b48eff, 0x305e8aff, 0x9f433cff,
    0xda2cbaff, 0xa5db79ff, 0x192d11ff, 0x7f888bff, 0x52307eff, 0xf05e16ff, 0xfdfe4bff, 0xceac7eff, 0x21070cff, 0x995886ff, 0x80efd6ff, 0x6a96daff, 0x5032c7ff, 0xe7797fff, 0xa4127dff, 0x78b935ff,
    0x526852ff, 0xbabfd4ff, 0x251640ff, 0x9165d1ff, 0xf5367dff, 0xa08d40ff, 0x693d4dff, 0xdc8dd0ff, 0xa422c6ff, 0x4ace99ff, 0x39789bff, 0x271580ff, 0xb55d45ff, 0xf14fcbff, 0xb0f580ff, 0x27461bff,
    0x8ca298ff, 0x634c92ff, 0xbe1845ff, 0xdec686ff, 0x392019ff, 0xac7294ff, 0x73b1eaff, 0x5b52ddff, 0xfb9388ff, 0xbc398eff, 0x83d438ff, 0x5f825eff, 0xc8dae0ff, 0x363055ff, 0xa181e3ff, 0xb0a746ff,
    0x7d565bff, 0xeea7deff, 0xb946d9ff, 0x50e9a2ff, 0x4093abff, 0x2f3798ff, 0xca774dff, 0x872054ff, 0x336024ff, 0x99bca3ff, 0x0c111cff, 0x7266a3ff, 0xd83c4fff, 0xeee08dff, 0x2832e4ff, 0x4e3825ff,
    0xbe8ca1ff, 0x873199ff, 0x7cccf8ff, 0x666ff1ff, 0xd2569cff, 0x8dee3aff, 0x6c9c68ff, 0xd4f4ebff, 0x454967ff, 0xaf9cf2ff, 0x8533e4ff, 0xa11e12ff, 0xc0c04bff, 0x8f7067ff, 0xfec2eaff, 0x55195bff,
    0xcc65ebff, 0x48adb9ff, 0x3853aeff, 0xde9054ff, 0x9f3d63ff, 0x3e7a2cff, 0xa5d6adff, 0x1a2a2fff, 0x8180b3ff, 0x571b9fff, 0xef5958ff, 0xfcfa93ff, 0x2655fbff, 0x61512fff, 0xcfa6adff, 0x9b4eabff,
    0x6b222dff, 0xe871aaff, 0x77b671ff, 0x536377ff, 0x9655f9ff, 0xbb3d18ff, 0xf524a5ff, 0xceda4fff, 0xa08972ff, 0x6a356eff, 0xdf81fbff, 0x4ec8c6ff, 0x406fc1ff, 0xf0aa5aff, 0xb55870ff, 0x499433ff,
    0xf33af3ff, 0xb0f0b7ff, 0x27433fff, 0x8e9bc2ff, 0x673db5ff, 0x736a38ff, 0xdfc0b8ff, 0x3a1b35ff, 0xae69bcff, 0x833c39ff, 0xfc8cb6ff, 0xbe25b3ff, 0x82d079ff, 0x617d86ff, 0x3a2473ff, 0xd3581dff,
    0xdcf552ff, 0xb0a37cff, 0x7e4f7fff, 0x54e3d3ff, 0x478ad3ff, 0x3a1fbaff, 0xca727bff, 0x880575ff, 0x53ae39ff, 0x335c4dff, 0x9bb5d0ff, 0x110431ff, 0x765ac9ff, 0xd73378ff, 0x838340ff, 0xeedac2ff,
    0x4f3446ff, 0xbf84cbff, 0x8a12bcff, 0x995543ff, 0xd448c4ff, 0x8cea81ff, 0x6d9793ff, 0x494088ff, 0xe97221ff, 0xa11541ff, 0xbfbc85ff, 0x211613ff, 0x90698eff, 0x59fedeff, 0xf42e2fff, 0x4ea5e3ff,
    0x4243d2ff, 0xdd8b86ff, 0xa03286ff, 0x5cc83dff, 0x3f765aff, 0xa7cfddff, 0x1e234aff, 0x8476dbff, 0xef5185ff, 0x929d48ff, 0xfcf5cbff, 0x614c55ff, 0xd09ed9ff, 0x9e3cd1ff, 0x16268bff, 0xad6f4cff,
    0x6b194cff, 0xe966d4ff, 0x78b1a0ff, 0x575a9bff, 0xfe8c23ff, 0xba384cff, 0xcdd68dff, 0x0e18d6ff, 0x352d20ff, 0xa1839cff, 0x6c268fff, 0x54c0f3ff, 0x4961e7ff, 0xf0a590ff, 0xb64f96ff, 0x64e341ff,
    0x499065ff, 0xb2eae8ff, 0x2a3d5eff, 0x9291ecff, 0x6d23d9ff, 0x841913ff, 0xa0b64eff, 0x736662ff, 0xe0b9e6ff, 0x3c0c4fff, 0xb05be3ff, 0x1745a3ff, 0xc08854ff, 0x83365cff, 0xfd82e2ff, 0x0e6d2bff,
    0x82cbabff, 0x6475acff, 0xd25357ff, 0xdbf194ff, 0x46462cff, 0xb19da9ff, 0x8044a3ff, 0x517efbff, 0x501b27ff, 0xca6aa5ff, 0x6bfd43ff, 0x52aa6fff, 0x365770ff, 0x9eacfcff, 0x7c48efff, 0x9e371bff,
    0xd81f9eff, 0xadd053ff, 0x837f6eff, 0xefd3f3ff, 0x502b64ff, 0xc278f5ff, 0x1861b8ff, 0xd2a25bff, 0x99506bff, 0x188733ff, 0xd631ebff, 0x8ce6b5ff, 0x6f8fbcff, 0x4f30aaff, 0xe86e60ff, 0x565f36ff,
    0xc0b7b5ff, 0x22102aff, 0x925fb4ff, 0xf32460ff, 0xa48203ff, 0x4d26f7ff, 0x683434ff, 0xde84b2ff, 0xa21baaff, 0x5bc578ff, 0x417180ff, 0x241365ff, 0xb55221ff, 0xf045adff, 0xbaeb57ff, 0x929979ff,
    0xfdeefeff, 0x634577ff, 0xa21af7ff, 0x197dcbff, 0xe3bb62ff, 0xad6977ff, 0x20a23aff, 0xeb55fdff, 0x7baacbff, 0x5c4ebfff, 0xfd8868ff, 0xba2e72ff, 0x657840ff, 0xced1c0ff, 0x35293dff, 0xa37ac5ff,
    0xb59c00ff, 0x7d4d40ff, 0xf09ebfff, 0xb740bcff, 0x63df81ff, 0x4b8b8eff, 0x30337dff, 0xcb6b26ff, 0x84103cff, 0xa0b283ff, 0x0a0a0aff, 0x745f88ff, 0xd62c30ff, 0x1998ddff, 0xf3d567ff, 0x2932c6ff,
    0xc08383ff, 0x842a7eff, 0x27bc40ff, 0x116a55ff, 0x85c4d8ff, 0x04143cff, 0x686ad3ff, 0xd24c80ff, 0x739248ff, 0xdbebcaff, 0x47424eff, 0xb294d4ff, 0x8331c7ff, 0x90664aff, 0x511043ff, 0xcc5ecdff,
    0x54a59bff, 0x3b4e92ff, 0xe0852aff, 0x9d3248ff, 0xadcc8cff, 0x1c221aff, 0x847997ff, 0x541984ff, 0xef4b36ff, 0x18b4edff, 0x2b53ddff, 0x624707ff, 0xd29d8eff, 0x9a468fff, 0x2cd745ff, 0x1a8461ff,
    0x8edfe5ff, 0x092f53ff, 0x7485e5ff, 0x681412ff, 0xe8678cff, 0x80ac4fff, 0x575b5dff, 0xc1afe2ff, 0x9551dbff, 0xf30888ff, 0xa37f53ff, 0x682d55ff, 0xdf7addff, 0x5dc0a8ff, 0x4669a5ff, 0xf49f2dff,
    0xb54d54ff, 0xf131d5ff, 0xb9e795ff, 0x2b3a27ff, 0x9392a4ff, 0x663999ff, 0x16cffcff, 0x2d70f2ff, 0x755f0fff, 0xe3b798ff, 0x361220ff, 0xae619fff, 0x31f249ff, 0x219e6dff, 0x97faf1ff, 0x114a67ff,
    0x7fa0f6ff, 0x633ae4ff, 0x81311bff, 0xfd8298ff, 0xbb1997ff, 0x8cc655ff, 0x65746aff, 0xcfc9efff, 0x381f59ff, 0xa66dedff, 0xb4995bff, 0x7d4764ff, 0xf295ebff, 0xba26e3ff, 0x64dab3ff, 0x4f83b6ff,
    0x381f9dff, 0xca675eff, 0x3a5333ff, 0xa1adb1ff, 0x7755acff, 0xd5225dff, 0x877914ff, 0xf3d1a0ff, 0x4d2b2eff, 0xc17cadff, 0x860ca0ff, 0x27b977ff, 0x6dfa89ff, 0x196479ff, 0x3e91f1ff, 0x6f5afaff
  )
}
