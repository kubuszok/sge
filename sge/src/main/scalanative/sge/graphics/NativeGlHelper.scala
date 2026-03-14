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

  // Byte offset from object start to Buffer._rawAddress field.
  // Detected at init time by probing a known direct ByteBuffer.
  private val RawAddressOffset: Int = detectRawAddressOffset()

  /** Probe a direct ByteBuffer to find the offset of _rawAddress within the object.
    *
    * We allocate a direct ByteBuffer of known size, then scan the object memory for a value that looks like a valid native heap pointer (non-zero, large, plausible address range). The first
    * 8-byte-aligned offset after the header that contains such a pointer is the _rawAddress field.
    */
  private def detectRawAddressOffset(): Int = {
    // Create a direct ByteBuffer — its _rawAddress points to malloc'd memory
    val probe  = ByteBuffer.allocateDirect(64)
    val rawObj = Intrinsics.castObjectToRawPtr(probe)
    // Scan offsets 8, 16, 24, 32, 40 — _rawAddress must be 8-byte aligned
    // and should be a non-zero pointer value that doesn't look like a small int.
    var offset = 8
    while (offset <= 48) {
      val fieldPtr = Intrinsics.elemRawPtr(rawObj, Intrinsics.castIntToRawSizeUnsigned(offset))
      val value    = Intrinsics.castRawPtrToLong(Intrinsics.loadRawPtr(fieldPtr))
      // A valid native heap address is non-zero and > 4GB (above any reasonable int field value)
      if (value > 0x100000000L) return offset
      offset += 8
    }
    // Fallback: try the documented layout (16-byte header + 8 = 24)
    24
  }

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
      // We extract the _rawAddress field from the Buffer object via raw memory access.
      //
      // Scala Native 0.5.x javalib Buffer layout (from Buffer.scala source):
      //   Constructor: Buffer(_capacity: Int, _address: CVoidPtr)
      //   Fields in declaration order:
      //     val _capacity: Int                              (4 bytes)
      //     protected val _rawAddress = toRawPtr(_address)  (RawPtr = 8 bytes, 8-byte aligned)
      //     private var _limit: Int                         (4 bytes)
      //     private var _position: Int                      (4 bytes)
      //     private[nio] var _mark: Int                     (4 bytes)
      //
      // Object header: 16 bytes when multithreading is enabled (rtti + lock word),
      // 8 bytes when single-threaded. _rawAddress offset = header + 4 (_capacity)
      // + 4 (alignment padding) = header + 8.
      val rawObj     = Intrinsics.castObjectToRawPtr(buf)
      val fieldPtr   = Intrinsics.elemRawPtr(rawObj, Intrinsics.castIntToRawSizeUnsigned(RawAddressOffset))
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
