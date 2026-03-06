/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (Scala Native GL buffer/string helpers)
 *   Convention: Scala Native @extern; buffer address via reflection
 *   Idiom: split packages
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
  // The JDK's java.nio.Buffer has a package-private 'address' field (long)
  // that stores the native pointer. We access it via reflection.

  private val addressField: java.lang.reflect.Field = {
    val f = classOf[java.nio.Buffer].getDeclaredField("address")
    f.setAccessible(true)
    f
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
    * @return
    *   Ptr[Byte] to buffer data at the current position, or null if buf is null
    */
  @SuppressWarnings(Array("all"))
  def bufPtr(buf: Buffer): Ptr[Byte] =
    if (buf == null) null
    else {
      val baseAddr = addressField.getLong(buf)
      val addr     = baseAddr + (buf.position().toLong * elementSize(buf).toLong)
      fromRawPtr[Byte](Intrinsics.castLongToRawPtr(addr))
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
