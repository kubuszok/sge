/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/utils/LZBDecompression.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 133
 * Covenant-baseline-methods: LZBDecompression,bits,cc,decompressFromBytes,dictionary,enlargeIn,entry,i,position,res,resb,resetValue,value,w
 * Covenant-source-reference: com/github/tommyettinger/textra/utils/LZBDecompression.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra
package utils

import scala.collection.mutable.ArrayBuffer
import scala.util.boundary
import scala.util.boundary.break

/** Decompresses byte arrays to Strings using a type of LZ-compression. */
object LZBDecompression {

  /** Decompresses a byte array compressed with LZB. */
  def decompressFromBytes(compressedBytes: Array[Byte]): String =
    decompressFromBytes(compressedBytes, 0, compressedBytes.length)

  /** Decompresses a byte array compressed with LZB. */
  def decompressFromBytes(compressedBytes: Array[Byte], offset: Int, length: Int): String = boundary {
    if (compressedBytes == null) break(null) // @nowarn — Java interop boundary
    if (length <= 0) break("")
    val resetValue = 128
    val dictionary = new ArrayBuffer[String](256)
    var enlargeIn  = 4; var dictSize       = 4; var numBits = 3
    var position   = resetValue; var index = offset + 1
    var w          = ""; var c             = ""
    val res        = new StringBuilder(length)
    var bits: Char = 0
    var resb  = 0; var maxpower = 0; var power = 0
    var cc    = 0
    var entry = ""
    var value = compressedBytes(offset).toInt

    var i = 0
    while (i < 3) { dictionary += String.valueOf(i.toChar); i += 1 }

    bits = 0; maxpower = 2; power = 0
    while (power != maxpower) {
      resb = value & position; position >>>= 1
      if (position == 0) { position = resetValue; value = compressedBytes(index); index += 1 }
      bits = (bits | (if (resb != 0) 1 << power else 0)).toChar
      power += 1
    }

    bits.toInt match {
      case 0 =>
        bits = 0; maxpower = 8; power = 0
        while (power != maxpower) {
          resb = value & position; position >>>= 1
          if (position == 0) { position = resetValue; value = compressedBytes(index); index += 1 }
          bits = (bits | (if (resb != 0) 1 << power else 0)).toChar
          power += 1
        }
        c = String.valueOf(bits)
      case 1 =>
        bits = 0; maxpower = 16; power = 0
        while (power != maxpower) {
          resb = value & position; position >>>= 1
          if (position == 0) { position = resetValue; value = compressedBytes(index); index += 1 }
          bits = (bits | (if (resb != 0) 1 << power else 0)).toChar
          power += 1
        }
        c = String.valueOf(bits)
      case _ => break("")
    }
    dictionary += c
    w = c
    res.append(w)

    boundary {
      while (true) {
        if (index - offset > length) break("")

        cc = 0; maxpower = numBits; power = 0
        while (power != maxpower) {
          resb = value & position; position >>>= 1
          if (position == 0) { position = resetValue; value = compressedBytes(index); index += 1 }
          cc |= (if (resb != 0) 1 << power else 0)
          power += 1
        }
        cc match {
          case 0 =>
            bits = 0; maxpower = 8; power = 0
            while (power != maxpower) {
              resb = value & position; position >>>= 1
              if (position == 0) { position = resetValue; value = compressedBytes(index); index += 1 }
              bits = (bits | (if (resb != 0) 1 << power else 0)).toChar
              power += 1
            }
            dictionary += String.valueOf(bits)
            cc = dictSize; dictSize += 1; enlargeIn -= 1
          case 1 =>
            bits = 0; maxpower = 16; power = 0
            while (power != maxpower) {
              resb = value & position; position >>>= 1
              if (position == 0) { position = resetValue; value = compressedBytes(index); index += 1 }
              bits = (bits | (if (resb != 0) 1 << power else 0)).toChar
              power += 1
            }
            dictionary += String.valueOf(bits)
            cc = dictSize; dictSize += 1; enlargeIn -= 1
          case 2 =>
            break(res.toString)
          case _ => ()
        }

        if (enlargeIn == 0) { enlargeIn = 1 << numBits; numBits += 1 }

        if (cc < dictionary.size && dictionary(cc) != null) {
          entry = dictionary(cc)
        } else if (cc == dictSize) {
          entry = w + w.charAt(0)
        } else {
          break("")
        }
        res.append(entry)

        dictionary += (w + entry.charAt(0))
        dictSize += 1; enlargeIn -= 1

        w = entry

        if (enlargeIn == 0) { enlargeIn = 1 << numBits; numBits += 1 }
      }
      res.toString
    }
  }
}
