/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/BitmapFontSupport.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: BitmapFont.BitmapFontData → sge.graphics.g2d.BitmapFontData,
 *     FileHandle → sge.files.FileHandle, TextureRegion → sge.graphics.g2d.TextureRegion,
 *     JsonReader/JsonValue → sge.utils.Json (jsoniter-scala AST),
 *     GdxRuntimeException → RuntimeException
 *   Convention: Utility class for loading BitmapFont from Structured JSON Fonts.
 *     .json and .dat (LZB) formats fully supported. .ubj (UBJSON) and .lzma
 *     throw at runtime until UBJsonReader→Json AST bridge and Lzma decompression
 *     are ported (neither dependency exists in SGE core yet).
 *   Idiom: LZB decompression algorithm preserved for cross-platform use.
 *     boundary/break replaces return. Nullable replaces null.
 *   Merged with: decompressFromBytes is private in Java original but kept private
 *     here (LZBDecompression in textra/utils has its own copy).
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 486
 * Covenant-baseline-methods: BitmapFontSupport,JsonFontData,bits,c,cc,data,decompressFromBytes,dictSize,dictionary,done,emptyJsonArr,emptyJsonObj,enlargeIn,entry,i,index,jsonArr,jsonInt,jsonIntOr,jsonNum,jsonNumOr,jsonObj,length,load,loadStructuredJson,maxpower,numBits,position,power,regionArr,res,resb,resetValue,this,value,w
 * Covenant-source-reference: com/github/tommyettinger/textra/BitmapFontSupport.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra

import scala.collection.mutable.ArrayBuffer
import scala.language.implicitConversions
import scala.util.boundary
import scala.util.boundary.break

import sge.files.FileHandle
import sge.graphics.g2d.{ BitmapFont, BitmapFontData, TextureRegion }
import sge.math.MathUtils
import sge.utils.{ DynamicArray, Json, Nullable, given_JsonCodec_Json }

/** A utility class for loading BitmapFont instances from Structured JSON files (which use .json, .dat, .ubj, .json.lzma, or .ubj.lzma as their file extension). Font instances can already be loaded
  * using some of the constructors on Font.
  *
  * Note: While .ubj and .ubj.lzma files are supported by this on most platforms, .json.lzma is preferred because it compresses almost as well and works everywhere.
  */
object BitmapFontSupport {

  /** Creates a BitmapFont by loading it from a Structured JSON Font, which is typically a .json, .dat, .ubj, .json.lzma, or .ubj.lzma file produced by FontWriter or a related tool. This overload
    * takes a TextureRegion for the image the JSON needs; this region is often part of an atlas.
    * @param jsonFont
    *   a FileHandle with the path to a Structured JSON Font (typically a .json.lzma file)
    * @param region
    *   a TextureRegion, often part of a shared atlas, holding the image the JSON needs
    * @return
    *   a new BitmapFont loaded from `jsonFont`
    */
  def loadStructuredJson(jsonFont: FileHandle, region: TextureRegion)(using Sge): BitmapFont = {
    val data      = new JsonFontData(jsonFont)
    val regionArr = DynamicArray[TextureRegion]()
    regionArr.add(region)
    new BitmapFont(data, Nullable(regionArr), false)
  }

  /** Creates a BitmapFont by loading it from a Structured JSON Font, which is typically a .json, .dat, .ubj, .json.lzma, or .ubj.lzma file produced by FontWriter or a related tool. This overload
    * takes a TextureRegion for the image the JSON needs; this region is often part of an atlas.
    * @param jsonFont
    *   a FileHandle with the path to a Structured JSON Font (typically a .json.lzma file)
    * @param region
    *   a TextureRegion, often part of a shared atlas, holding the image the JSON needs
    * @param flip
    *   true if this BitmapFont has been flipped for use with a y-down coordinate system
    * @return
    *   a new BitmapFont loaded from `jsonFont`
    */
  def loadStructuredJson(jsonFont: FileHandle, region: TextureRegion, flip: Boolean)(using Sge): BitmapFont = {
    val data      = new JsonFontData(jsonFont, Nullable.empty, flip)
    val regionArr = DynamicArray[TextureRegion]()
    regionArr.add(region)
    new BitmapFont(data, Nullable(regionArr), false)
  }

  /** Creates a BitmapFont by loading it from a Structured JSON Font, which is typically a .json, .dat, .ubj, .json.lzma, or .ubj.lzma file produced by FontWriter or a related tool. This overload
    * takes a relative path (from `jsonFont`) to the necessary image file, with the path as a String.
    * @param jsonFont
    *   a FileHandle with the path to a Structured JSON Font (typically a .json.lzma file)
    * @param imagePath
    *   a String holding the relative path from `jsonFont` to the image file the JSON needs
    * @return
    *   a new BitmapFont loaded from `jsonFont`
    */
  def loadStructuredJson(jsonFont: FileHandle, imagePath: String)(using Sge): BitmapFont = {
    val data = new JsonFontData(jsonFont, Nullable(imagePath))
    new BitmapFont(data, Nullable.empty, false)
  }

  /** Creates a BitmapFont by loading it from a Structured JSON Font, which is typically a .json, .dat, .ubj, .json.lzma, or .ubj.lzma file produced by FontWriter or a related tool. This overload
    * takes a relative path (from `jsonFont`) to the necessary image file, with the path as a String.
    * @param jsonFont
    *   a FileHandle with the path to a Structured JSON Font (typically a .json.lzma file)
    * @param imagePath
    *   a String holding the relative path from `jsonFont` to the image file the JSON needs
    * @param flip
    *   true if this BitmapFont has been flipped for use with a y-down coordinate system
    * @return
    *   a new BitmapFont loaded from `jsonFont`
    */
  def loadStructuredJson(jsonFont: FileHandle, imagePath: String, flip: Boolean)(using Sge): BitmapFont = {
    val data = new JsonFontData(jsonFont, Nullable(imagePath), flip)
    new BitmapFont(data, Nullable.empty, false)
  }

  /** Mainly for internal use; allows loading BitmapFontData from a Structured JSON Font instead of a .fnt file. */
  class JsonFontData(jsonFont: Nullable[FileHandle], val path: Nullable[String], flip: Boolean) extends BitmapFontData(Nullable.empty, flip) {

    def this() = this(Nullable.empty, Nullable.empty, false)

    def this(jsonFont: FileHandle) = this(Nullable(jsonFont), Nullable.empty, false)

    def this(jsonFont: FileHandle, imagePath: Nullable[String]) = this(Nullable(jsonFont), imagePath, false)

    // Trigger load from constructor if jsonFont is provided
    jsonFont.foreach { jf =>
      load(jf, flip)
    }

    override def load(jsonFont: FileHandle, flip: Boolean): Unit = {
      if (imagePaths.isDefined) throw new IllegalStateException("Already loaded.")
      try {
        name = Nullable(jsonFont.nameWithoutExtension)

        val fnt: Map[String, Json] = if (jsonFont.exists()) {
          val ext = jsonFont.extension
          if ("json".equalsIgnoreCase(ext)) {
            jsonObj(sge.utils.readFromString[Json](jsonFont.readString(Nullable("UTF-8"))))
          } else if ("dat".equalsIgnoreCase(ext)) {
            jsonObj(sge.utils.readFromString[Json](decompressFromBytes(jsonFont.readBytes())))
          } else if ("ubj".equalsIgnoreCase(ext)) {
            // UBJSON: requires UBJsonReader → Json AST bridge (not yet available in SGE)
            throw new RuntimeException("UBJSON (.ubj) font loading is not yet supported: " + jsonFont.path)
          } else if ("lzma".equalsIgnoreCase(ext)) {
            // LZMA: requires sge.utils.compression.Lzma (not yet ported from LibGDX)
            throw new RuntimeException("LZMA-compressed font loading is not yet supported: " + jsonFont.path)
          } else {
            throw new RuntimeException("Not a .json, .dat, .ubj, .json.lzma, or .ubj.lzma font file: " + jsonFont.path)
          }
        } else {
          throw new RuntimeException("Missing font file: " + jsonFont.path)
        }
        if (fnt.isEmpty) throw new RuntimeException("File is empty: " + jsonFont.path)

        val atlas = jsonObj(fnt.getOrElse("atlas", emptyJsonObj))

        val size = jsonNumOr(atlas, "size", 16f)
        @scala.annotation.unused
        val width  = jsonIntOr(atlas, "width", 2048)
        val height = jsonIntOr(atlas, "height", 2048)

        padTop = 1
        padRight = 1
        padBottom = 1
        padLeft = 1
        val padY = padTop + padBottom

//        val metrics = fnt.get("metrics").map(jsonObj)

        descent = size * -0.25f // metrics.getFloat("descender", -0.25f)

//        size *= metrics.getFloat("emSize", 1f)
        lineHeight = size - descent // * metrics.getFloat("lineHeight", 1f)
//        ascent = size * metrics.getFloat("ascender", 0.8f) - lineHeight
//        val baseLine = lineHeight + descent

        path.foreach { p =>
          imagePaths = Nullable(Array(jsonFont.sibling(p).path.replaceAll("\\\\", "/")))
        }

        val glyphsJson = jsonArr(fnt.getOrElse("glyphs", emptyJsonArr))

        for (currentJson <- glyphsJson) {
          val current = jsonObj(currentJson)
          val glyph   = new BitmapFont.Glyph()
          val ch      = jsonIntOr(current, "unicode", -1)
          if (ch <= 0) {
            missingGlyph = Nullable(glyph)
          } else if (ch <= Character.MAX_VALUE) {
            setGlyph(ch, glyph)
          } else {
            // skip glyphs outside BMP — continue to next iteration
          }
          if (ch <= Character.MAX_VALUE) {
            glyph.id = ch
            glyph.xadvance = MathUtils.round(jsonNumOr(current, "advance", 1f) * size)
            val planeBounds = current.get("planeBounds").map(jsonObj)
            val atlasBounds = current.get("atlasBounds").map(jsonObj)
            atlasBounds match {
              case Some(ab) =>
                val x = jsonNumOr(ab, "left", 0f)
                glyph.srcX = x.toInt
                glyph.width = (jsonNumOr(ab, "right", 0f) - x).toInt
                val y = height - jsonNumOr(ab, "top", 0f)
                glyph.srcY = y.toInt
                glyph.height = (height - jsonNumOr(ab, "bottom", 0f) - y).toInt
              case None =>
                glyph.srcX = 0; glyph.srcY = 0; glyph.width = 0; glyph.height = 0
            }
            planeBounds match {
              case Some(pb) =>
                glyph.xoffset = MathUtils.round(jsonNumOr(pb, "left", 0f) * size)
                glyph.yoffset = if (flip) {
                  MathUtils.round(-size - jsonNumOr(pb, "top", 0f) * size)
                } else {
                  MathUtils.round(-size + jsonNumOr(pb, "bottom", 0f) * size)
                }
              case None =>
                glyph.xoffset = 0; glyph.yoffset = 0
            }
          }
        }

        val kern = fnt.get("kerning").map(jsonArr)
        kern.foreach { kernArr =>
          if (kernArr.nonEmpty) {
            for (currentJson <- kernArr) {
              val current = jsonObj(currentJson)
              val first   = jsonIntOr(current, "unicode1", -1)
              val second  = jsonIntOr(current, "unicode2", -1)
              if (first >= 0 && first <= Character.MAX_VALUE && second >= 0 && second <= Character.MAX_VALUE) {
                val glyph  = getGlyph(first.toChar)
                val amount = jsonNumOr(current, "advance", 0f)
                glyph.foreach(_.setKerning(second, MathUtils.round(amount)))
              }
            }
          }
        }

        var spaceGlyph = getGlyph(' ')
        if (spaceGlyph.isEmpty) {
          val sg = new BitmapFont.Glyph()
          sg.id = ' '
          var xadvanceGlyph = getGlyph('l')
          if (xadvanceGlyph.isEmpty) xadvanceGlyph = Nullable(firstGlyph)
          sg.xadvance = xadvanceGlyph.fold(0)(_.xadvance)
          setGlyph(' ', sg)
          spaceGlyph = Nullable(sg)
        }
        spaceGlyph.foreach { sg =>
          if (sg.width == 0) {
            sg.width = (padLeft + sg.xadvance + padRight).toInt
            sg.xoffset = (-padLeft).toInt
          }
          spaceXadvance = sg.xadvance.toFloat
        }

        var xGlyph: Nullable[BitmapFont.Glyph] = Nullable.empty
        for (xChar <- xChars if xGlyph.isEmpty)
          xGlyph = getGlyph(xChar)
        if (xGlyph.isEmpty) xGlyph = Nullable(firstGlyph)
        xHeight = xGlyph.fold(0f)(_.height.toFloat) - padY

        var capGlyph: Nullable[BitmapFont.Glyph] = Nullable.empty
        for (capChar <- capChars if capGlyph.isEmpty)
          capGlyph = getGlyph(capChar)
        if (capGlyph.isEmpty) {
          for (page <- this.glyphs if Nullable(page).isDefined)
            for (glyph <- page if Nullable(glyph).isDefined)
              if (glyph.height != 0 && glyph.width != 0) {
                capHeight = Math.max(capHeight, glyph.height.toFloat)
              }
        } else {
          capHeight = capGlyph.fold(0f)(_.height.toFloat)
        }
        capHeight -= padY

        ascent = lineHeight + descent - capHeight
        down = -lineHeight
        if (flip) {
          ascent = -ascent
          down = -down
        }

      } catch {
        case ex: Exception =>
          throw new RuntimeException("Error loading font file: " + jsonFont, ex)
      }
    }
  }

  // JSON AST helpers (mirrors the pattern used in Font.scala)
  private def jsonObj(j: Json): Map[String, Json] = j match {
    case Json.Obj(fields) => fields.toMap
    case _                => Map.empty
  }
  private def jsonArr(j: Json): Seq[Json] = j match {
    case Json.Arr(items) => items.toSeq
    case _               => Seq.empty
  }
  private def jsonNum(j: Json): Float = j match {
    case Json.Num(n) => n.toFloat.getOrElse(0f)
    case _           => 0f
  }
  private def jsonInt(j: Json): Int = j match {
    case Json.Num(n) => n.toInt.getOrElse(0)
    case _           => 0
  }
  private def jsonNumOr(m: Map[String, Json], key: String, default: Float): Float =
    m.get(key).map(jsonNum).getOrElse(default)
  private def jsonIntOr(m: Map[String, Json], key: String, default: Int): Int =
    m.get(key).map(jsonInt).getOrElse(default)
  private val emptyJsonObj: Json = Json.Obj(sge.utils.JsonObject.empty)
  private val emptyJsonArr: Json = Json.Arr(Vector.empty)

  /** Decompresses a byte array compressed with LZB, getting the original String back that was given to a compression method.
    *
    * This is private because a preferred version is present in `com.github.tommyettinger.textra.utils.LZBDecompression`; this method is only present here to make copying this class easier on its own.
    */
  private def decompressFromBytes(compressedBytes: Array[Byte]): String = boundary {
    if (compressedBytes == null) break(null) // @nowarn — Java interop boundary
    val length = compressedBytes.length
    if (length == 0) break("")
    val resetValue = 128
    val dictionary = ArrayBuffer[String]()
    var enlargeIn  = 4
    var dictSize   = 4
    var numBits    = 3
    var position   = resetValue
    var index      = 1
    var maxpower   = 0
    var power      = 0
    val res        = new StringBuilder(length)
    var bits: Char = 0
    var resb  = 0
    var w     = ""
    var c     = ""
    var entry = ""
    var cc    = 0
    var value = compressedBytes(0)

    var i = 0.toChar
    while (i < 3) {
      dictionary += String.valueOf(i)
      i = (i + 1).toChar
    }

    bits = 0
    maxpower = 2
    power = 0
    while (power != maxpower) {
      resb = value & position
      position >>>= 1
      if (position == 0) {
        position = resetValue
        value = compressedBytes(index)
        index += 1
      }
      bits = (bits | (if (resb != 0) 1 else 0) << power).toChar
      power += 1
    }

    bits.toInt match {
      case 0 =>
        bits = 0
        maxpower = 8
        power = 0
        while (power != maxpower) {
          resb = value & position
          position >>>= 1
          if (position == 0) {
            position = resetValue
            value = compressedBytes(index)
            index += 1
          }
          bits = (bits | (if (resb != 0) 1 else 0) << power).toChar
          power += 1
        }
        c = String.valueOf(bits)
      case 1 =>
        bits = 0
        maxpower = 16
        power = 0
        while (power != maxpower) {
          resb = value & position
          position >>>= 1
          if (position == 0) {
            position = resetValue
            value = compressedBytes(index)
            index += 1
          }
          bits = (bits | (if (resb != 0) 1 else 0) << power).toChar
          power += 1
        }
        c = String.valueOf(bits)
      case _ =>
        break("")
    }
    dictionary += c
    w = c
    res.append(w)

    var done = false
    while (!done) {
      if (index > length) break("")
      cc = 0
      maxpower = numBits
      power = 0
      while (power != maxpower) {
        resb = value & position
        position >>>= 1
        if (position == 0) {
          position = resetValue
          value = compressedBytes(index)
          index += 1
        }
        cc |= (if (resb != 0) 1 else 0) << power
        power += 1
      }
      cc match {
        case 0 =>
          bits = 0
          maxpower = 8
          power = 0
          while (power != maxpower) {
            resb = value & position
            position >>>= 1
            if (position == 0) {
              position = resetValue
              value = compressedBytes(index)
              index += 1
            }
            bits = (bits | (if (resb != 0) 1 else 0) << power).toChar
            power += 1
          }
          dictionary += String.valueOf(bits)
          cc = dictSize
          dictSize += 1
          enlargeIn -= 1
        case 1 =>
          bits = 0
          maxpower = 16
          power = 0
          while (power != maxpower) {
            resb = value & position
            position >>>= 1
            if (position == 0) {
              position = resetValue
              value = compressedBytes(index)
              index += 1
            }
            bits = (bits | (if (resb != 0) 1 else 0) << power).toChar
            power += 1
          }
          dictionary += String.valueOf(bits)
          cc = dictSize
          dictSize += 1
          enlargeIn -= 1
        case 2 =>
          done = true
        case _ => // continue
      }

      if (!done) {
        if (enlargeIn == 0) {
          enlargeIn = 1 << numBits
          numBits += 1
        }

        if (cc < dictionary.size && dictionary(cc) != null) {
          entry = dictionary(cc)
        } else {
          if (cc == dictSize) {
            entry = w + w.charAt(0)
          } else {
            break("")
          }
        }
        res.append(entry)

        // Add w+entry[0] to the dictionary.
        dictionary += (w + entry.charAt(0))
        dictSize += 1
        enlargeIn -= 1

        w = entry

        if (enlargeIn == 0) {
          enlargeIn = 1 << numBits
          numBits += 1
        }
      }
    }
    res.toString
  }
}
