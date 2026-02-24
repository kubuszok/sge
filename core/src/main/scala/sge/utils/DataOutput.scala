/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/DataOutput.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils

import java.io.{ DataOutputStream, IOException, OutputStream }
import scala.util.boundary

/** Extends {@link DataOutputStream} with additional convenience methods.
  * @author
  *   Nathan Sweet (original implementation)
  */
class DataOutput(out: OutputStream) extends DataOutputStream(out) {

  /** Writes a 1-5 byte int.
    * @param optimizePositive
    *   If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be inefficient (5 bytes).
    */
  @throws[IOException]
  def writeInt(value: Int, optimizePositive: Boolean): Int = boundary {
    var v = if (!optimizePositive) (value << 1) ^ (value >> 31) else value
    if (v >>> 7 == 0) {
      write(v.toByte)
      boundary.break(1)
    }
    write(((v & 0x7f) | 0x80).toByte)
    if (v >>> 14 == 0) {
      write((v >>> 7).toByte)
      boundary.break(2)
    }
    write(((v >>> 7) | 0x80).toByte)
    if (v >>> 21 == 0) {
      write((v >>> 14).toByte)
      boundary.break(3)
    }
    write(((v >>> 14) | 0x80).toByte)
    if (v >>> 28 == 0) {
      write((v >>> 21).toByte)
      boundary.break(4)
    }
    write(((v >>> 21) | 0x80).toByte)
    write((v >>> 28).toByte)
    5
  }

  /** Writes a length and then the string as UTF8.
    * @param value
    *   May be null.
    */
  @throws[IOException]
  def writeString(value: String | Null): Unit = boundary {
    if (value == null) {
      write(0)
      boundary.break(())
    }
    val charCount = value.length()
    if (charCount == 0) {
      writeByte(1)
      boundary.break(())
    }
    writeInt(charCount + 1, true)
    // Try to write 8 bit chars.
    var charIndex = 0
    while (charIndex < charCount) {
      val c = value.charAt(charIndex)
      if (c > 127) {
        writeString_slow(value, charCount, charIndex)
        boundary.break(())
      }
      write(c.toByte)
      charIndex += 1
    }
  }

  @throws[IOException]
  private def writeString_slow(value: String, charCount: Int, charIndex: Int): Unit = {
    var i = charIndex
    while (i < charCount) {
      val c = value.charAt(i).toInt
      if (c <= 0x007f) {
        write(c.toByte)
      } else if (c > 0x07ff) {
        write((0xe0 | c >> 12 & 0x0f).toByte)
        write((0x80 | c >> 6 & 0x3f).toByte)
        write((0x80 | c & 0x3f).toByte)
      } else {
        write((0xc0 | c >> 6 & 0x1f).toByte)
        write((0x80 | c & 0x3f).toByte)
      }
      i += 1
    }
  }
}
