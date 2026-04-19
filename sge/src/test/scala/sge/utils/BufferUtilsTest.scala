/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

import java.nio.{ Buffer, ByteOrder }

class BufferUtilsTest extends munit.FunSuite {

  // ─── Buffer allocation ────────────────────────────────────────────────

  test("newByteBuffer creates direct buffer with native order") {
    val buf = BufferUtils.newByteBuffer(16)
    assertEquals(buf.capacity(), 16)
    assert(buf.isDirect)
    assertEquals(buf.order(), ByteOrder.nativeOrder())
  }

  test("newFloatBuffer creates buffer with correct capacity") {
    val buf = BufferUtils.newFloatBuffer(8)
    assertEquals(buf.capacity(), 8)
  }

  test("newIntBuffer creates buffer with correct capacity") {
    val buf = BufferUtils.newIntBuffer(4)
    assertEquals(buf.capacity(), 4)
  }

  test("newShortBuffer creates buffer with correct capacity") {
    val buf = BufferUtils.newShortBuffer(6)
    assertEquals(buf.capacity(), 6)
  }

  test("newCharBuffer creates buffer with correct capacity") {
    val buf = BufferUtils.newCharBuffer(10)
    assertEquals(buf.capacity(), 10)
  }

  test("newLongBuffer creates buffer with correct capacity") {
    val buf = BufferUtils.newLongBuffer(3)
    assertEquals(buf.capacity(), 3)
  }

  test("newDoubleBuffer creates buffer with correct capacity") {
    val buf = BufferUtils.newDoubleBuffer(5)
    assertEquals(buf.capacity(), 5)
  }

  // ─── copy: float array to FloatBuffer ─────────────────────────────────

  test("copy float array to FloatBuffer") {
    val src = Array(1.0f, 2.0f, 3.0f, 4.0f)
    val dst = BufferUtils.newFloatBuffer(4)
    BufferUtils.copy(src, dst.asInstanceOf[Buffer], 4, 0)
    assertEquals(dst.asInstanceOf[Buffer].position(), 0)
    assertEquals(dst.asInstanceOf[Buffer].limit(), 4)
    val out = new Array[Float](4)
    dst.get(out)
    assertEquals(out.toSeq, src.toSeq)
  }

  test("copy float array to FloatBuffer with offset") {
    val src = Array(10.0f, 20.0f, 30.0f, 40.0f, 50.0f)
    val dst = BufferUtils.newFloatBuffer(3)
    BufferUtils.copy(src, dst.asInstanceOf[Buffer], 3, 2)
    dst.asInstanceOf[Buffer].position(0)
    val out = new Array[Float](3)
    dst.get(out)
    assertEquals(out.toSeq, Seq(30.0f, 40.0f, 50.0f))
  }

  test("copy float array to ByteBuffer") {
    val src = Array(1.0f, 2.0f, 3.0f)
    val dst = BufferUtils.newByteBuffer(12) // 3 floats * 4 bytes
    BufferUtils.copy(src, dst.asInstanceOf[Buffer], 3, 0)
    assertEquals(dst.asInstanceOf[Buffer].position(), 0)
    assertEquals(dst.asInstanceOf[Buffer].limit(), 12)
    val fb = dst.asFloatBuffer()
    val out = new Array[Float](3)
    fb.get(out)
    assertEquals(out.toSeq, src.toSeq)
  }

  // ─── copy: byte array to ByteBuffer ───────────────────────────────────

  test("copy byte array to ByteBuffer") {
    val src = Array[Byte](1, 2, 3, 4, 5)
    val dst = BufferUtils.newByteBuffer(8)
    BufferUtils.copy(src, 0, dst.asInstanceOf[Buffer], 5)
    dst.asInstanceOf[Buffer].position(0)
    val out = new Array[Byte](5)
    dst.get(out)
    assertEquals(out.toSeq, src.toSeq)
  }

  test("copy byte array with srcOffset") {
    val src = Array[Byte](10, 20, 30, 40, 50)
    val dst = BufferUtils.newByteBuffer(8)
    BufferUtils.copy(src, 2, dst.asInstanceOf[Buffer], 3)
    dst.asInstanceOf[Buffer].position(0)
    val out = new Array[Byte](3)
    dst.get(out)
    assertEquals(out.toSeq, Seq[Byte](30, 40, 50))
  }

  // ─── copy: Buffer to Buffer ───────────────────────────────────────────

  test("copy ByteBuffer to ByteBuffer") {
    val src = BufferUtils.newByteBuffer(4)
    src.put(Array[Byte](1, 2, 3, 4))
    src.asInstanceOf[Buffer].position(0)
    val dst = BufferUtils.newByteBuffer(8)
    BufferUtils.copy(src.asInstanceOf[Buffer], dst.asInstanceOf[Buffer], 4)
    dst.asInstanceOf[Buffer].position(0)
    val out = new Array[Byte](4)
    dst.get(out)
    assertEquals(out.toSeq, Seq[Byte](1, 2, 3, 4))
  }

  // ─── clear ────────────────────────────────────────────────────────────

  test("clear zeros out bytes") {
    val buf = BufferUtils.newByteBuffer(8)
    buf.put(Array[Byte](1, 2, 3, 4, 5, 6, 7, 8))
    buf.asInstanceOf[Buffer].position(0)
    BufferUtils.clear(buf, 4)
    buf.asInstanceOf[Buffer].position(0)
    for (i <- 0 until 4)
      assertEquals(buf.get(i), 0.toByte, s"Byte at index $i should be zero")
    // Remaining bytes should be unchanged
    for (i <- 4 until 8)
      assert(buf.get(i) != 0, s"Byte at index $i should not be zero")
  }

  test("clear entire buffer") {
    val buf = BufferUtils.newByteBuffer(4)
    buf.put(Array[Byte](9, 8, 7, 6))
    buf.asInstanceOf[Buffer].position(0)
    BufferUtils.clear(buf, 4)
    for (i <- 0 until 4)
      assertEquals(buf.get(i), 0.toByte)
  }

  // ─── unsafe buffer management (tests that don't require native libs) ──

  test("disposeUnsafeByteBuffer throws on non-unsafe buffer") {
    val buf = BufferUtils.newByteBuffer(16)
    intercept[IllegalArgumentException] {
      BufferUtils.disposeUnsafeByteBuffer(buf)
    }
  }

  test("isUnsafeByteBuffer returns false for regular buffer") {
    val buf = BufferUtils.newByteBuffer(8)
    assert(!BufferUtils.isUnsafeByteBuffer(buf))
  }
}
