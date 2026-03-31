/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/BitmapFontSupport.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: BitmapFont.BitmapFontData → deferred (requires libGDX BitmapFont),
 *     FileHandle → deferred, TextureRegion → deferred,
 *     JsonReader/UBJsonReader/Lzma → deferred,
 *     GdxRuntimeException → RuntimeException
 *   Convention: Utility class for loading BitmapFont from Structured JSON Fonts.
 *     Full loading deferred until rendering layer is wired up.
 *   Idiom: LZB decompression algorithm preserved for cross-platform use.
 */
package sge
package textra

import scala.collection.mutable.ArrayBuffer
import scala.util.boundary
import scala.util.boundary.break

/** A utility class for loading BitmapFont instances from Structured JSON files (which use .json, .dat, .ubj, .json.lzma, or .ubj.lzma as their file extension). Font instances can already be loaded
  * using some of the constructors on Font.
  *
  * Note: While .ubj and .ubj.lzma files are supported by this on most platforms, .json.lzma is preferred because it compresses almost as well and works everywhere.
  */
object BitmapFontSupport {

  /** Decompresses a byte array compressed with LZB, getting the original String back that was given to a compression method. */
  def decompressFromBytes(compressedBytes: Array[Byte]): String = boundary {
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
      if (index > length) return ""
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
