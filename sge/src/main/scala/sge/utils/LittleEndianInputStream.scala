/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/LittleEndianInputStream.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: `readLine()` annotated with `@scala.annotation.nowarn` for deprecation
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

import java.io.{ DataInput, DataInputStream, FilterInputStream, IOException, InputStream }

/** Taken from http://www.javafaq.nu/java-example-code-1079.html
  * @author
  *   mzechner
  */
final class LittleEndianInputStream(in: InputStream) extends FilterInputStream(in) with DataInput {

  private val din = new DataInputStream(in)

  @throws[IOException]
  def readFully(b: Array[Byte]): Unit =
    din.readFully(b)

  @throws[IOException]
  def readFully(b: Array[Byte], off: Int, len: Int): Unit =
    din.readFully(b, off, len)

  @throws[IOException]
  def skipBytes(n: Int): Int =
    din.skipBytes(n)

  @throws[IOException]
  def readBoolean(): Boolean =
    din.readBoolean()

  @throws[IOException]
  def readByte(): Byte =
    din.readByte()

  @throws[IOException]
  def readUnsignedByte(): Int =
    din.readUnsignedByte()

  @throws[IOException]
  def readShort(): Short = {
    val low  = din.read()
    val high = din.read()
    ((high << 8) | (low & 0xff)).toShort
  }

  @throws[IOException]
  def readUnsignedShort(): Int = {
    val low  = din.read()
    val high = din.read()
    ((high & 0xff) << 8) | (low & 0xff)
  }

  @throws[IOException]
  def readChar(): Char =
    din.readChar()

  @throws[IOException]
  def readInt(): Int = {
    val res = new Array[Int](4)
    for (i <- 3 to 0 by -1)
      res(i) = din.read()
    ((res(0) & 0xff) << 24) | ((res(1) & 0xff) << 16) | ((res(2) & 0xff) << 8) | (res(3) & 0xff)
  }

  @throws[IOException]
  def readLong(): Long = {
    val res = new Array[Int](8)
    for (i <- 7 to 0 by -1)
      res(i) = din.read()
    (((res(0) & 0xff).toLong << 56) | ((res(1) & 0xff).toLong << 48) | ((res(2) & 0xff).toLong << 40)
      | ((res(3) & 0xff).toLong << 32) | ((res(4) & 0xff).toLong << 24) | ((res(5) & 0xff).toLong << 16)
      | ((res(6) & 0xff).toLong << 8) | ((res(7) & 0xff).toLong))
  }

  @throws[IOException]
  def readFloat(): Float =
    java.lang.Float.intBitsToFloat(readInt())

  @throws[IOException]
  def readDouble(): Double =
    java.lang.Double.longBitsToDouble(readLong())

  @scala.annotation.nowarn("msg=deprecated")
  @throws[IOException]
  def readLine(): String =
    din.readLine()

  @throws[IOException]
  def readUTF(): String =
    din.readUTF()
}
