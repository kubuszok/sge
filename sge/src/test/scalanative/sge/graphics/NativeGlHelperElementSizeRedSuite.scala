/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-535 (Scala Native NativeGlHelper.bufPtr corrupts the data
 * pointer for non-Byte/Float/Int/Long buffers).
 *
 * Root cause being reproduced: NativeGlHelper.elementSize (sge/src/main/
 * scalanative/sge/graphics/NativeGlHelper.scala lines ~60-66) maps
 *   ByteBuffer  -> 1
 *   FloatBuffer -> 4
 *   IntBuffer   -> 4
 *   LongBuffer  -> 8
 *   case _      -> 1
 * so ShortBuffer, CharBuffer and DoubleBuffer all fall into `case _ => 1`.
 *
 * bufPtr (lines ~76-100) computes the native data pointer at the buffer's
 * current position as
 *   byteOffset = buf.position() * elementSize(buf)
 *   dataPtr    = baseAddress + byteOffset
 * Therefore for a ShortBuffer (2 bytes/element) positioned at N the pointer is
 * computed as base + N*1 instead of base + N*2 — off by a factor of the element
 * size. This is the corruption that breaks glDrawElements on Native, where Mesh
 * index buffers are ShortBuffers consumed at a non-zero offset (Mesh.scala
 * 653-657, vertex-array branch with offset != 0).
 *
 * The contract these tests pin: for a buffer of element size S, advancing the
 * position from 0 to N must advance the native data pointer by exactly N*S
 * bytes. That is the only address arithmetic that lets the GL driver read the
 * correct element. We compute bufPtr(position 0) and bufPtr(position N) for each
 * java.nio buffer type and assert the byte distance equals N * expectedSize.
 *
 * ByteBuffer/IntBuffer/FloatBuffer/LongBuffer already pass (sanity controls);
 * ShortBuffer/CharBuffer/DoubleBuffer FAIL today (distance N*1, expected N*2 /
 * N*2 / N*8).
 *
 * This suite is Scala-Native only: NativeGlHelper lives under src/main/
 * scalanative and has no JVM/JS counterpart. It is written by the reproducer
 * agent and MUST NOT be modified by the fixer.
 */
package sge
package graphics

import java.nio.{ Buffer, ByteBuffer, ByteOrder }

import scala.scalanative.runtime.toRawPtr
import scala.scalanative.runtime.Intrinsics
import scala.scalanative.unsafe.Ptr

class NativeGlHelperElementSizeRedSuite extends munit.FunSuite {

  // Number of elements to advance the position by before measuring the pointer.
  private val N = 3

  /** Native address of a Ptr[Byte] as a Long, using the same intrinsics
    * NativeGlHelper itself uses internally to materialise the pointer.
    */
  private def addrOf(ptr: Ptr[Byte]): Long =
    Intrinsics.castRawPtrToLong(toRawPtr(ptr))

  /** Byte distance the data pointer moves when the buffer position advances from
    * 0 to N. The buffer must be a fresh direct buffer at position 0.
    */
  private def pointerDelta(buf: Buffer): Long = {
    buf.position(0)
    val base = addrOf(NativeGlHelper.bufPtr(buf))
    buf.position(N)
    val at = addrOf(NativeGlHelper.bufPtr(buf))
    at - base
  }

  private def assertElementSize(buf: Buffer, expectedSize: Int): Unit =
    assertEquals(
      pointerDelta(buf),
      (N.toLong * expectedSize.toLong),
      s"advancing ${buf.getClass.getSimpleName} by $N elements must move the native " +
        s"data pointer by ${N * expectedSize} bytes (element size $expectedSize); " +
        "elementSize's `case _ => 1` collapses this to N bytes for non Byte/Int/Float/Long buffers"
    )

  // Direct buffers backing each typed view. asXBuffer() produces a typed view
  // over native memory, which is exactly what bufPtr is handed in the GL paths.
  private def directBytes(elements: Int, bytesPerElement: Int): ByteBuffer =
    ByteBuffer.allocateDirect(elements * bytesPerElement).order(ByteOrder.nativeOrder())

  // --- Sanity controls (already correct) -------------------------------------

  test("ByteBuffer element size is 1 byte") {
    assertElementSize(directBytes(N + 1, 1), 1)
  }

  test("IntBuffer element size is 4 bytes") {
    assertElementSize(directBytes(N + 1, 4).asIntBuffer(), 4)
  }

  test("FloatBuffer element size is 4 bytes") {
    assertElementSize(directBytes(N + 1, 4).asFloatBuffer(), 4)
  }

  test("LongBuffer element size is 8 bytes") {
    assertElementSize(directBytes(N + 1, 8).asLongBuffer(), 8)
  }

  // --- The bug: these FAIL today (case _ => 1) -------------------------------

  test("ShortBuffer element size is 2 bytes (RED: bufPtr uses 1)") {
    assertElementSize(directBytes(N + 1, 2).asShortBuffer(), 2)
  }

  test("CharBuffer element size is 2 bytes (RED: bufPtr uses 1)") {
    assertElementSize(directBytes(N + 1, 2).asCharBuffer(), 2)
  }

  test("DoubleBuffer element size is 8 bytes (RED: bufPtr uses 1)") {
    assertElementSize(directBytes(N + 1, 8).asDoubleBuffer(), 8)
  }
}
