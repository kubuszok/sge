/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/DataInput.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils

import java.io.{ DataInputStream, IOException, InputStream }

/** Extends {@link DataInputStream} with additional convenience methods.
  * @author
  *   Nathan Sweet (original implementation)
  */
class DataInput(in: InputStream) extends DataInputStream(in) {
  private var chars = new Array[Char](32)

  /** Reads a 1-5 byte int. */
  @throws[IOException]
  def readInt(optimizePositive: Boolean): Int = {
    var b      = readByte()
    var result = b & 0x7f
    if ((b & 0x80) != 0) {
      b = readByte()
      result |= (b & 0x7f) << 7
      if ((b & 0x80) != 0) {
        b = readByte()
        result |= (b & 0x7f) << 14
        if ((b & 0x80) != 0) {
          b = readByte()
          result |= (b & 0x7f) << 21
          if ((b & 0x80) != 0) {
            b = readByte()
            result |= (b & 0x7f) << 28
          }
        }
      }
    }
    if (optimizePositive) result else (result >>> 1) ^ -(result & 1)
  }

  /** Reads the length and string of UTF8 characters, or null.
    * @return
    *   May be null.
    */
  @throws[IOException]
  def readString(): String | Null = {
    val charCount = readInt(true)
    charCount match {
      case 0 => null
      case 1 => ""
      case _ =>
        val actualCharCount = charCount - 1
        if (chars.length < actualCharCount) chars = new Array[Char](actualCharCount)
        val localChars = this.chars
        // Try to read 7 bit ASCII chars.
        var charIndex = 0
        var b         = 0
        scala.util.boundary {
          while (charIndex < actualCharCount) {
            b = readByte()
            if (b < 0) {
              b = b & 0xff
              readUtf8_slow(actualCharCount, charIndex, b)
              scala.util.boundary.break(())
            }
            localChars(charIndex) = b.toChar
            charIndex += 1
          }
        }
        new String(localChars, 0, actualCharCount)
    }
  }

  @throws[IOException]
  private def readUtf8_slow(charCount: Int, charIndex: Int, b: Int): Unit = {
    val localChars       = this.chars
    var currentCharIndex = charIndex
    var currentB         = b
    scala.util.boundary {
      while (true) {
        (currentB >> 4) match {
          case 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 =>
            localChars(currentCharIndex) = currentB.toChar
          case 12 | 13 =>
            localChars(currentCharIndex) = ((currentB & 0x1f) << 6 | readByte() & 0x3f).toChar
          case 14 =>
            localChars(currentCharIndex) = ((currentB & 0x0f) << 12 | (readByte() & 0x3f) << 6 | readByte() & 0x3f).toChar
          case _ => // ignore invalid bytes
        }
        currentCharIndex += 1
        if (currentCharIndex >= charCount) scala.util.boundary.break(())
        currentB = readByte() & 0xff
      }
    }
  }
}
