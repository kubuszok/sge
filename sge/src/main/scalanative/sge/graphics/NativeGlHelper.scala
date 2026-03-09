/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (Scala Native GL buffer/string helpers)
 *   Convention: Scala Native @extern; buffer address via reflection
 *   Idiom: split packages
 *   Audited: 2026-03-08
 */
package sge
package graphics

import java.nio.{ Buffer, ByteBuffer, ByteOrder, FloatBuffer, IntBuffer, LongBuffer }

import scala.scalanative.runtime.{ Intrinsics, fromRawPtr }
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

/** Shared helpers for Scala Native OpenGL ES implementations.
  *
  * Provides buffer-to-pointer conversion, GLboolean helpers, and string conversion utilities used by AngleGL20Native through AngleGL32Native.
  */
private[graphics] object NativeGlHelper {

  // ─── Buffer address extraction ────────────────────────────────────────────
  // Direct NIO buffers on Scala Native are backed by malloc'd memory.
  // On the JVM, the `address` field is accessed via reflection, but
  // java.lang.reflect.Field is not available on Scala Native.
  // Instead, we use scala.scalanative.runtime to get the native pointer directly.

  private def elementSize(buf: Buffer): Int = buf match {
    case _: ByteBuffer  => 1
    case _: FloatBuffer => 4
    case _: IntBuffer   => 4
    case _: LongBuffer  => 8
    case _ => 1
  }

  /** Get a native pointer to the buffer's current position.
    *
    * For direct buffers on Scala Native, we extract the underlying ByteBuffer and use its array-backed or native-backed data pointer. For heap buffers, we pin the backing array and compute the
    * offset.
    *
    * @return
    *   Ptr[Byte] to buffer data at the current position, or null if buf is null
    */
  @SuppressWarnings(Array("all"))
  def bufPtr(buf: Buffer): Ptr[Byte] =
    if (buf == null) null
    else {
      // On Scala Native, all direct NIO buffers ultimately wrap a native memory region.
      // We can obtain the pointer by going through the underlying ByteBuffer's array
      // or by reading the address from the object's memory layout.
      //
      // The Scala Native javalib stores the native address as a CVoidPtr in the Buffer
      // constructor. We read it by casting the object to raw memory.
      // Object layout: [header (8 bytes on 64-bit)] [fields...]
      // Buffer fields: _capacity (Int, 4B), then _address (CVoidPtr = Ptr = 8B)
      // With alignment, _address starts at offset 16 from the object start.
      val rawObj     = Intrinsics.castObjectToRawPtr(buf)
      val fieldPtr   = Intrinsics.elemRawPtr(rawObj, Intrinsics.castIntToRawSizeUnsigned(16))
      val baseAddr   = Intrinsics.castRawPtrToLong(Intrinsics.loadRawPtr(fieldPtr))
      val byteOffset = buf.position().toLong * elementSize(buf).toLong
      fromRawPtr[Byte](Intrinsics.castLongToRawPtr(baseAddr + byteOffset))
    }

  /** Get a native pointer to the buffer at a specific byte offset (for GL offset parameters). */
  def offsetPtr(offset: Int): Ptr[Byte] =
    if (offset == 0) null
    else fromRawPtr[Byte](Intrinsics.castLongToRawPtr(offset.toLong))

  // ─── GLboolean helpers ────────────────────────────────────────────────────

  def glBool(v: Boolean): CUnsignedChar =
    if (v) 1.toUByte else 0.toUByte

  def fromGlBool(v: CUnsignedChar): Boolean = v != 0.toUByte

  // ─── Direct buffer allocation helpers ─────────────────────────────────────

  def allocDirectInt(n: Int): IntBuffer =
    ByteBuffer.allocateDirect(n * 4).order(ByteOrder.nativeOrder()).asIntBuffer()

  def allocDirectFloat(n: Int): FloatBuffer =
    ByteBuffer.allocateDirect(n * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
}
