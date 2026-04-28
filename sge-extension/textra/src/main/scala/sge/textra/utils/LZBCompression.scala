/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/utils/LZBCompression.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 202
 * Covenant-baseline-methods: LZBCompression,bitsPerChar,compressToByteArray,compressToBytes,contextData,contextDataPosition,contextDataVal,contextDictSize,contextDictionary,contextDictionaryToCreate,contextEnlargeIn,contextNumBits,contextW,done,i,ii,ucl,value
 * Covenant-source-reference: com/github/tommyettinger/textra/utils/LZBCompression.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra
package utils

import scala.collection.mutable.{ ArrayBuffer, HashMap, HashSet }

/** Compresses Strings to byte arrays using a type of LZ-compression. */
object LZBCompression {

  /** Compresses a String using LZB compression and returns it as a byte array. */
  def compressToBytes(uncompressedStr: String): Array[Byte] =
    if (uncompressedStr == null) null // @nowarn — Java interop boundary
    else if (uncompressedStr.isEmpty) Array.emptyByteArray
    else compressToByteArray(uncompressedStr)

  private def compressToByteArray(uncompressedStr: String): Array[Byte] = {
    val bitsPerChar               = 8
    val contextDictionary         = new HashMap[String, Int](1024, 0.5)
    val contextDictionaryToCreate = new HashSet[String](1024, 0.5)
    var contextW                  = ""
    var contextEnlargeIn          = 2
    var contextDictSize           = 3
    var contextNumBits            = 2
    val contextData               = new ArrayBuffer[Byte](uncompressedStr.length >>> 1)
    var contextDataVal: Byte = 0
    var contextDataPosition = 0

    var ii  = 0
    val ucl = uncompressedStr.length
    while (ii < ucl) {
      val contextC = String.valueOf(uncompressedStr.charAt(ii))
      if (!contextDictionary.contains(contextC)) {
        contextDictionary.put(contextC, contextDictSize)
        contextDictSize += 1
        contextDictionaryToCreate.add(contextC)
      }
      val contextWc = contextW + contextC
      if (contextDictionary.contains(contextWc)) {
        contextW = contextWc
      } else {
        if (contextDictionaryToCreate.contains(contextW)) {
          var value = contextW.charAt(0).toInt
          if (value < 256) {
            var i = 0
            while (i < contextNumBits) {
              contextDataVal = (contextDataVal << 1).toByte
              if (contextDataPosition == bitsPerChar - 1) {
                contextDataPosition = 0; contextData += contextDataVal; contextDataVal = 0
              } else contextDataPosition += 1
              i += 1
            }
            i = 0
            while (i < 8) {
              contextDataVal = (contextDataVal << 1 | (value & 1)).toByte
              if (contextDataPosition == bitsPerChar - 1) {
                contextDataPosition = 0; contextData += contextDataVal; contextDataVal = 0
              } else contextDataPosition += 1
              value >>>= 1
              i += 1
            }
          } else {
            value = 1
            var i = 0
            while (i < contextNumBits) {
              contextDataVal = ((contextDataVal << 1) | value).toByte
              if (contextDataPosition == bitsPerChar - 1) {
                contextDataPosition = 0; contextData += contextDataVal; contextDataVal = 0
              } else contextDataPosition += 1
              value = 0
              i += 1
            }
            value = contextW.charAt(0).toInt
            i = 0
            while (i < 16) {
              contextDataVal = ((contextDataVal << 1) | (value & 1)).toByte
              if (contextDataPosition == bitsPerChar - 1) {
                contextDataPosition = 0; contextData += contextDataVal; contextDataVal = 0
              } else contextDataPosition += 1
              value >>>= 1
              i += 1
            }
          }
          contextEnlargeIn -= 1
          if (contextEnlargeIn == 0) {
            contextEnlargeIn = 1 << contextNumBits
            contextNumBits += 1
          }
          contextDictionaryToCreate.remove(contextW)
        } else {
          var value = contextDictionary(contextW)
          var i     = 0
          while (i < contextNumBits) {
            contextDataVal = ((contextDataVal << 1) | (value & 1)).toByte
            if (contextDataPosition == bitsPerChar - 1) {
              contextDataPosition = 0; contextData += contextDataVal; contextDataVal = 0
            } else contextDataPosition += 1
            value >>>= 1
            i += 1
          }
        }
        contextEnlargeIn -= 1
        if (contextEnlargeIn == 0) {
          contextEnlargeIn = 1 << contextNumBits
          contextNumBits += 1
        }
        contextDictionary.put(contextWc, contextDictSize)
        contextDictSize += 1
        contextW = contextC
      }
      ii += 1
    }

    // Output the code for w
    if (contextW.nonEmpty) {
      if (contextDictionaryToCreate.contains(contextW)) {
        var value = contextW.charAt(0).toInt
        if (value < 256) {
          var i = 0
          while (i < contextNumBits) {
            contextDataVal = (contextDataVal << 1).toByte
            if (contextDataPosition == bitsPerChar - 1) {
              contextDataPosition = 0; contextData += contextDataVal; contextDataVal = 0
            } else contextDataPosition += 1
            i += 1
          }
          i = 0
          while (i < 8) {
            contextDataVal = ((contextDataVal << 1) | (value & 1)).toByte
            if (contextDataPosition == bitsPerChar - 1) {
              contextDataPosition = 0; contextData += contextDataVal; contextDataVal = 0
            } else contextDataPosition += 1
            value >>>= 1
            i += 1
          }
        } else {
          value = 1
          var i = 0
          while (i < contextNumBits) {
            contextDataVal = ((contextDataVal << 1) | value).toByte
            if (contextDataPosition == bitsPerChar - 1) {
              contextDataPosition = 0; contextData += contextDataVal; contextDataVal = 0
            } else contextDataPosition += 1
            value = 0
            i += 1
          }
          value = contextW.charAt(0).toInt
          i = 0
          while (i < 16) {
            contextDataVal = ((contextDataVal << 1) | (value & 1)).toByte
            if (contextDataPosition == bitsPerChar - 1) {
              contextDataPosition = 0; contextData += contextDataVal; contextDataVal = 0
            } else contextDataPosition += 1
            value >>>= 1
            i += 1
          }
        }
        contextDictionaryToCreate.remove(contextW)
      } else {
        var value = contextDictionary(contextW)
        var i     = 0
        while (i < contextNumBits) {
          contextDataVal = ((contextDataVal << 1) | (value & 1)).toByte
          if (contextDataPosition == bitsPerChar - 1) {
            contextDataPosition = 0; contextData += contextDataVal; contextDataVal = 0
          } else contextDataPosition += 1
          value >>>= 1
          i += 1
        }
      }
    }

    // Mark the end of the stream
    var value = 2
    var i     = 0
    while (i < contextNumBits) {
      contextDataVal = ((contextDataVal << 1) | (value & 1)).toByte
      if (contextDataPosition == bitsPerChar - 1) {
        contextDataPosition = 0; contextData += contextDataVal; contextDataVal = 0
      } else contextDataPosition += 1
      value >>>= 1
      i += 1
    }

    // Flush the last char
    var done = false
    while (!done) {
      contextDataVal = (contextDataVal << 1).toByte
      if (contextDataPosition == bitsPerChar - 1) {
        contextData += contextDataVal
        done = true
      } else contextDataPosition += 1
    }
    contextData.toArray
  }
}
