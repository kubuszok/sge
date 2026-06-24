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

import java.nio.{ Buffer, ByteBuffer, ByteOrder, CharBuffer, DoubleBuffer, FloatBuffer, IntBuffer, LongBuffer, ShortBuffer }

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
    * We allocate a direct ByteBuffer, stamp a unique marker into its data, then
    * scan the object's 8-byte-aligned fields for the one whose value — treated as
    * a pointer and dereferenced — reads the marker back. That field is the
    * `_rawAddress` pointer to the malloc'd data.
    *
    * A magnitude-only heuristic ("address > 4GB") is NOT portable: on Windows the
    * native heap address can be below 4GB, so it mis-detects the field (the
    * Windows-native NativeGlHelperElementSize failures). The marker is
    * deterministic regardless of address range. We still guard the dereference
    * with a `> 64KB, 8-byte-aligned` check so small int fields (capacity, limit,
    * position, ...) are never dereferenced as pointers.
    */
  private def detectRawAddressOffset(): Int = {
    val probe = ByteBuffer.allocateDirect(64)
    // Stamp in native byte order so the marker bytes match a native raw load.
    probe.order(ByteOrder.nativeOrder())
    val marker = 0x5347_4548_5052_4245L // 'SGEHPRBE'
    probe.putLong(0, marker)
    val rawObj = Intrinsics.castObjectToRawPtr(probe)
    var offset = 8
    var found  = -1
    while (offset <= 48 && found < 0) {
      val fieldPtr  = Intrinsics.elemRawPtr(rawObj, Intrinsics.castIntToRawSizeUnsigned(offset))
      val candidate = Intrinsics.loadRawPtr(fieldPtr)
      val addr      = Intrinsics.castRawPtrToLong(candidate)
      // Only dereference plausible, aligned pointers (skip small int fields).
      if (addr > 0x10000L && (addr & 0x7L) == 0L) {
        val derefed = Intrinsics.castRawPtrToLong(Intrinsics.loadRawPtr(candidate))
        if (derefed == marker) found = offset
      }
      offset += 8
    }
    // Fallback: the documented layout (16-byte header + 8 = 24).
    if (found >= 0) found else 24
  }

  private def elementSize(buf: Buffer): Int = buf match {
    case _: ByteBuffer   => 1
    case _: ShortBuffer  => 2
    case _: CharBuffer   => 2
    case _: FloatBuffer  => 4
    case _: IntBuffer    => 4
    case _: LongBuffer   => 8
    case _: DoubleBuffer => 8
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

  /** Wrap a native pointer as a direct ByteBuffer of the given length.
    *
    * Creates a direct ByteBuffer and overwrites its internal `_rawAddress` field to point to the given native pointer instead of the originally malloc'd memory. The original malloc'd memory is leaked
    * — this is acceptable for GL-mapped buffers which are short-lived and unmapped by the caller.
    *
    * @return
    *   ByteBuffer wrapping the native pointer, or null if ptr is null
    */
  def wrapPtr(ptr: Ptr[Byte], length: Int): ByteBuffer =
    if (ptr == null) null
    else {
      val buf    = ByteBuffer.allocateDirect(length).order(ByteOrder.nativeOrder())
      val rawObj = Intrinsics.castObjectToRawPtr(buf)
      val field  = Intrinsics.elemRawPtr(rawObj, Intrinsics.castIntToRawSizeUnsigned(RawAddressOffset))
      Intrinsics.storeRawPtr(field, scala.scalanative.runtime.toRawPtr(ptr))
      buf
    }

  /** Get a native pointer to the buffer at a specific byte offset (for GL offset parameters). */
  def offsetPtr(offset: Int): Ptr[Byte] =
    if (offset == 0) null
    else fromRawPtr[Byte](Intrinsics.castLongToRawPtr(offset.toLong))

  /** Get a native pointer to the buffer at a specific byte offset, preserving the full 64-bit offset (for GL indirect-draw parameters whose offset is a long pointer into the bound buffer).
    */
  def offsetPtr(offset: Long): Ptr[Byte] =
    if (offset == 0L) null
    else fromRawPtr[Byte](Intrinsics.castLongToRawPtr(offset))

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
