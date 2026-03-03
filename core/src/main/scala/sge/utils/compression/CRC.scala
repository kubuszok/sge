/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/compression/CRC.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Java static Table + static initializer -> companion object val + initializer block
 * - All public methods (Init, Update x2, UpdateByte, GetDigest) faithfully ported
 * - Method names lowercased: Init->init, Update->update, UpdateByte->updateByte, GetDigest->getDigest
 * - _value field: package-private in Java, private in Scala (correct)
 * - No convention issues
 */
package sge
package utils
package compression

// SevenZip/CRC.java
class CRC {
  private var _value: Int = -1

  def init(): Unit =
    _value = -1

  def update(data: Array[Byte], offset: Int, size: Int): Unit =
    for (i <- 0 until size)
      _value = CRC.Table((_value ^ data(offset + i)) & 0xff) ^ (_value >>> 8)

  def update(data: Array[Byte]): Unit = {
    val size = data.length
    for (i <- 0 until size)
      _value = CRC.Table((_value ^ data(i)) & 0xff) ^ (_value >>> 8)
  }

  def updateByte(b: Int): Unit =
    _value = CRC.Table((_value ^ b) & 0xff) ^ (_value >>> 8)

  def getDigest(): Int =
    _value ^ -1
}

object CRC {
  val Table: Array[Int] = Array.ofDim[Int](256)

  // Static initializer block
  {
    var i = 0
    while (i < 256) {
      var r = i
      var j = 0
      while (j < 8) {
        if ((r & 1) != 0)
          r = (r >>> 1) ^ 0xedb88320
        else
          r >>>= 1
        j += 1
      }
      Table(i) = r
      i += 1
    }
  }
}
