/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

import java.io.ByteArrayInputStream

class LittleEndianInputStreamTest extends munit.FunSuite {

  /** Helper: writes big-endian data with DataOutputStream, then reads it with LittleEndianInputStream to test the byte-swap behavior.
    */
  private def makeLittleEndianStream(bytes: Array[Byte]): LittleEndianInputStream =
    LittleEndianInputStream(new ByteArrayInputStream(bytes))

  // ---- readByte ----

  test("readByte returns correct value") {
    val in = makeLittleEndianStream(Array[Byte](42, -1, 0, 127))
    assertEquals(in.readByte(), 42.toByte)
    assertEquals(in.readByte(), -1.toByte)
    assertEquals(in.readByte(), 0.toByte)
    assertEquals(in.readByte(), 127.toByte)
  }

  // ---- readBoolean ----

  test("readBoolean returns false for 0 and true for non-zero") {
    val in = makeLittleEndianStream(Array[Byte](0, 1, 42))
    assertEquals(in.readBoolean(), false)
    assertEquals(in.readBoolean(), true)
    assertEquals(in.readBoolean(), true)
  }

  // ---- readUnsignedByte ----

  test("readUnsignedByte returns value in 0..255") {
    val in = makeLittleEndianStream(Array[Byte](0, 127, -128.toByte, -1.toByte))
    assertEquals(in.readUnsignedByte(), 0)
    assertEquals(in.readUnsignedByte(), 127)
    assertEquals(in.readUnsignedByte(), 128)
    assertEquals(in.readUnsignedByte(), 255)
  }

  // ---- readShort (little-endian) ----

  test("readShort reads bytes in little-endian order") {
    // Little-endian: low byte first, high byte second
    // Value 0x0102 = 258 -> bytes: 0x02, 0x01
    val in = makeLittleEndianStream(Array[Byte](0x02, 0x01))
    assertEquals(in.readShort(), 0x0102.toShort)
  }

  test("readShort negative value") {
    // Value -1 = 0xFFFF -> bytes: 0xFF, 0xFF
    val in = makeLittleEndianStream(Array[Byte](0xff.toByte, 0xff.toByte))
    assertEquals(in.readShort(), -1.toShort)
  }

  test("readShort zero") {
    val in = makeLittleEndianStream(Array[Byte](0, 0))
    assertEquals(in.readShort(), 0.toShort)
  }

  // ---- readUnsignedShort (little-endian) ----

  test("readUnsignedShort reads bytes in little-endian order") {
    // Value 0x0102 = 258 -> bytes: 0x02, 0x01
    val in = makeLittleEndianStream(Array[Byte](0x02, 0x01))
    assertEquals(in.readUnsignedShort(), 258)
  }

  test("readUnsignedShort max value") {
    // 0xFFFF = 65535 -> bytes: 0xFF, 0xFF
    val in = makeLittleEndianStream(Array[Byte](0xff.toByte, 0xff.toByte))
    assertEquals(in.readUnsignedShort(), 65535)
  }

  // ---- readInt (little-endian) ----

  test("readInt reads bytes in little-endian order") {
    // Value 0x01020304 -> little-endian bytes: 0x04, 0x03, 0x02, 0x01
    val in = makeLittleEndianStream(Array[Byte](0x04, 0x03, 0x02, 0x01))
    assertEquals(in.readInt(), 0x01020304)
  }

  test("readInt zero") {
    val in = makeLittleEndianStream(Array[Byte](0, 0, 0, 0))
    assertEquals(in.readInt(), 0)
  }

  test("readInt negative one") {
    val in = makeLittleEndianStream(Array[Byte](0xff.toByte, 0xff.toByte, 0xff.toByte, 0xff.toByte))
    assertEquals(in.readInt(), -1)
  }

  test("readInt value 1") {
    // 1 in little-endian: 0x01, 0x00, 0x00, 0x00
    val in = makeLittleEndianStream(Array[Byte](0x01, 0x00, 0x00, 0x00))
    assertEquals(in.readInt(), 1)
  }

  // ---- readLong (little-endian) ----

  test("readLong reads bytes in little-endian order") {
    // Value 0x0102030405060708L -> little-endian bytes: 08,07,06,05,04,03,02,01
    val in = makeLittleEndianStream(Array[Byte](0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01))
    assertEquals(in.readLong(), 0x0102030405060708L)
  }

  test("readLong zero") {
    val in = makeLittleEndianStream(new Array[Byte](8))
    assertEquals(in.readLong(), 0L)
  }

  test("readLong negative one") {
    val bytes = Array.fill[Byte](8)(0xff.toByte)
    val in    = makeLittleEndianStream(bytes)
    assertEquals(in.readLong(), -1L)
  }

  // ---- readFloat (little-endian) ----

  test("readFloat reads little-endian float") {
    // 1.0f = 0x3F800000 -> little-endian: 0x00, 0x00, 0x80, 0x3F
    val in = makeLittleEndianStream(Array[Byte](0x00, 0x00, 0x80.toByte, 0x3f))
    assertEquals(in.readFloat(), 1.0f)
  }

  test("readFloat zero") {
    val in = makeLittleEndianStream(new Array[Byte](4))
    assertEquals(in.readFloat(), 0.0f)
  }

  // ---- readDouble (little-endian) ----

  test("readDouble reads little-endian double") {
    // 1.0d = 0x3FF0000000000000L -> little-endian: 00,00,00,00,00,00,F0,3F
    val in = makeLittleEndianStream(Array[Byte](0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xf0.toByte, 0x3f))
    assertEquals(in.readDouble(), 1.0d)
  }

  test("readDouble zero") {
    val in = makeLittleEndianStream(new Array[Byte](8))
    assertEquals(in.readDouble(), 0.0d)
  }

  // ---- readFully ----

  test("readFully reads entire byte array") {
    val data = Array[Byte](1, 2, 3, 4, 5)
    val in   = makeLittleEndianStream(data)
    val buf  = new Array[Byte](5)
    in.readFully(buf)
    assertEquals(buf.toSeq, data.toSeq)
  }

  test("readFully with offset and length") {
    val data = Array[Byte](1, 2, 3, 4, 5)
    val in   = makeLittleEndianStream(data)
    val buf  = new Array[Byte](7)
    in.readFully(buf, 1, 5)
    assertEquals(buf(0), 0.toByte)
    assertEquals(buf(1), 1.toByte)
    assertEquals(buf(5), 5.toByte)
    assertEquals(buf(6), 0.toByte)
  }

  // ---- skipBytes ----

  test("skipBytes skips correct number of bytes") {
    val in      = makeLittleEndianStream(Array[Byte](1, 2, 3, 4, 5))
    val skipped = in.skipBytes(3)
    assertEquals(skipped, 3)
    assertEquals(in.readByte(), 4.toByte)
  }

  // ---- Multiple reads ----

  test("mixed reads from same stream") {
    // Build a stream with: short(LE) + int(LE) + byte
    // short 0x0102 -> LE: 02, 01
    // int 0x01020304 -> LE: 04, 03, 02, 01
    // byte 42
    val data = Array[Byte](0x02, 0x01, 0x04, 0x03, 0x02, 0x01, 42)
    val in   = makeLittleEndianStream(data)
    assertEquals(in.readShort(), 0x0102.toShort)
    assertEquals(in.readInt(), 0x01020304)
    assertEquals(in.readByte(), 42.toByte)
  }
}
