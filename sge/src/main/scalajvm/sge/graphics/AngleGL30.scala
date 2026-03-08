/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (replaces Lwjgl3GL30.java)
 *   Convention: JVM-only; Panama FFM downcall handles to ANGLE libGLESv2
 *   Convention: Extends AngleGL20 — inherits all GL ES 2.0 bindings
 *   Convention: Array+offset variants loop with single-element calls
 *   Idiom: split packages; no return; SgeError.GraphicsError
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import java.lang.foreign.*
import java.lang.foreign.ValueLayout.*
import java.lang.invoke.MethodHandle
import java.nio.{ Buffer, ByteBuffer, ByteOrder, FloatBuffer, IntBuffer, LongBuffer }

/** OpenGL ES 3.0 implementation via ANGLE (libGLESv2) using Panama FFM downcall handles.
  *
  * Extends [[AngleGL20]] with the additional GL ES 3.0 methods. All GL20 methods are inherited.
  *
  * @param lookup
  *   a `SymbolLookup` for the ANGLE libGLESv2 shared library
  */
class AngleGL30(lookup: SymbolLookup) extends AngleGL20(lookup) with GL30 {

  // ─── Shorthand layout aliases (inherited I, F, B, P from AngleGL20 are private) ─
  private val I30: ValueLayout.OfInt   = JAVA_INT
  private val F30: ValueLayout.OfFloat = JAVA_FLOAT
  private val B30: ValueLayout.OfByte  = JAVA_BYTE
  private val P30: AddressLayout       = ADDRESS

  private val linker30: Linker = Linker.nativeLinker()

  private def h30(name: String, desc: FunctionDescriptor): MethodHandle =
    linker30.downcallHandle(
      lookup.find(name).orElseThrow(() => new UnsatisfiedLinkError(s"GL symbol not found: $name")),
      desc
    )

  private def glBool30(v:          Boolean): Byte    = if (v) 1.toByte else 0.toByte
  private def fromGlBool30(result: AnyRef):  Boolean = result.asInstanceOf[Byte] != 0

  @SuppressWarnings(Array("all"))
  private def bufAddr30(buf: Buffer): MemorySegment =
    if (buf == null) MemorySegment.NULL else MemorySegment.ofBuffer(buf)

  // ─── Read buffer / Draw ───────────────────────────────────────────────────

  private lazy val _glReadBuffer = h30("glReadBuffer", FunctionDescriptor.ofVoid(I30))
  override def glReadBuffer(mode: Int): Unit = _glReadBuffer.invoke(mode)

  private lazy val _glDrawRangeElements = h30("glDrawRangeElements", FunctionDescriptor.ofVoid(I30, I30, I30, I30, I30, P30))

  override def glDrawRangeElements(mode: PrimitiveMode, start: Int, end: Int, count: Int, `type`: DataType, indices: Buffer): Unit =
    _glDrawRangeElements.invoke(mode, start, end, count, `type`, bufAddr30(indices))

  override def glDrawRangeElements(mode: PrimitiveMode, start: Int, end: Int, count: Int, `type`: DataType, offset: Int): Unit =
    _glDrawRangeElements.invoke(mode, start, end, count, `type`, MemorySegment.ofAddress(offset.toLong))

  // ─── Texture 3D / offset variants ─────────────────────────────────────────

  private lazy val _glTexImage3D = h30("glTexImage3D", FunctionDescriptor.ofVoid(I30, I30, I30, I30, I30, I30, I30, I30, I30, P30))

  override def glTexImage3D(target: TextureTarget, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: PixelFormat, `type`: DataType, pixels: Buffer): Unit =
    _glTexImage3D.invoke(target, level, internalformat, width, height, depth, border, format, `type`, bufAddr30(pixels))

  override def glTexImage3D(target: TextureTarget, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: PixelFormat, `type`: DataType, offset: Int): Unit =
    _glTexImage3D.invoke(target, level, internalformat, width, height, depth, border, format, `type`, MemorySegment.ofAddress(offset.toLong))

  // GL30 adds offset variants for existing GL20 methods
  private lazy val _glTexImage2D_offset = h30("glTexImage2D", FunctionDescriptor.ofVoid(I30, I30, I30, I30, I30, I30, I30, I30, P30))
  override def glTexImage2D(target: TextureTarget, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: PixelFormat, `type`: DataType, offset: Int): Unit =
    _glTexImage2D_offset.invoke(target, level, internalformat, width, height, border, format, `type`, MemorySegment.ofAddress(offset.toLong))

  private lazy val _glTexSubImage3D = h30("glTexSubImage3D", FunctionDescriptor.ofVoid(I30, I30, I30, I30, I30, I30, I30, I30, I30, I30, P30))

  override def glTexSubImage3D(
    target:  TextureTarget,
    level:   Int,
    xoffset: Int,
    yoffset: Int,
    zoffset: Int,
    width:   Int,
    height:  Int,
    depth:   Int,
    format:  PixelFormat,
    `type`:  DataType,
    pixels:  Buffer
  ): Unit =
    _glTexSubImage3D.invoke(target, level, xoffset, yoffset, zoffset, width, height, depth, format, `type`, bufAddr30(pixels))

  override def glTexSubImage3D(
    target:  TextureTarget,
    level:   Int,
    xoffset: Int,
    yoffset: Int,
    zoffset: Int,
    width:   Int,
    height:  Int,
    depth:   Int,
    format:  PixelFormat,
    `type`:  DataType,
    offset:  Int
  ): Unit =
    _glTexSubImage3D.invoke(target, level, xoffset, yoffset, zoffset, width, height, depth, format, `type`, MemorySegment.ofAddress(offset.toLong))

  private lazy val _glTexSubImage2D_offset = h30("glTexSubImage2D", FunctionDescriptor.ofVoid(I30, I30, I30, I30, I30, I30, I30, I30, P30))
  override def glTexSubImage2D(target: TextureTarget, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: PixelFormat, `type`: DataType, offset: Int): Unit =
    _glTexSubImage2D_offset.invoke(target, level, xoffset, yoffset, width, height, format, `type`, MemorySegment.ofAddress(offset.toLong))

  private lazy val _glCopyTexSubImage3D = h30("glCopyTexSubImage3D", FunctionDescriptor.ofVoid(I30, I30, I30, I30, I30, I30, I30, I30, I30))
  override def glCopyTexSubImage3D(target: TextureTarget, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit =
    _glCopyTexSubImage3D.invoke(target, level, xoffset, yoffset, zoffset, x, y, width, height)

  // ─── Queries ──────────────────────────────────────────────────────────────

  private lazy val _glGenQueries = h30("glGenQueries", FunctionDescriptor.ofVoid(I30, P30))

  override def glGenQueries(n: Int, ids: IntBuffer): Unit = _glGenQueries.invoke(n, bufAddr30(ids))

  override def glGenQueries(n: Int, ids: Array[Int], offset: Int): Unit = {
    val buf = ByteBuffer.allocateDirect(n * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
    glGenQueries(n, buf)
    var i = 0
    while (i < n) { ids(offset + i) = buf.get(i); i += 1 }
  }

  private lazy val _glDeleteQueries = h30("glDeleteQueries", FunctionDescriptor.ofVoid(I30, P30))

  override def glDeleteQueries(n: Int, ids: IntBuffer): Unit = _glDeleteQueries.invoke(n, bufAddr30(ids))

  override def glDeleteQueries(n: Int, ids: Array[Int], offset: Int): Unit = {
    val buf = ByteBuffer.allocateDirect(n * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
    var i   = 0
    while (i < n) { buf.put(i, ids(offset + i)); i += 1 }
    glDeleteQueries(n, buf)
  }

  private lazy val _glIsQuery = h30("glIsQuery", FunctionDescriptor.of(B30, I30))
  override def glIsQuery(id: Int): Boolean = fromGlBool30(_glIsQuery.invoke(id))

  private lazy val _glBeginQuery = h30("glBeginQuery", FunctionDescriptor.ofVoid(I30, I30))
  override def glBeginQuery(target: Int, id: Int): Unit = _glBeginQuery.invoke(target, id)

  private lazy val _glEndQuery = h30("glEndQuery", FunctionDescriptor.ofVoid(I30))
  override def glEndQuery(target: Int): Unit = _glEndQuery.invoke(target)

  private lazy val _glGetQueryiv = h30("glGetQueryiv", FunctionDescriptor.ofVoid(I30, I30, P30))
  override def glGetQueryiv(target: Int, pname: Int, params: IntBuffer): Unit = _glGetQueryiv.invoke(target, pname, bufAddr30(params))

  private lazy val _glGetQueryObjectuiv = h30("glGetQueryObjectuiv", FunctionDescriptor.ofVoid(I30, I30, P30))
  override def glGetQueryObjectuiv(id: Int, pname: Int, params: IntBuffer): Unit = _glGetQueryObjectuiv.invoke(id, pname, bufAddr30(params))

  // ─── Buffer mapping ───────────────────────────────────────────────────────

  private lazy val _glUnmapBuffer = h30("glUnmapBuffer", FunctionDescriptor.of(B30, I30))
  override def glUnmapBuffer(target: BufferTarget): Boolean = fromGlBool30(_glUnmapBuffer.invoke(target))

  override def glGetBufferPointerv(target: BufferTarget, pname: Int): Buffer =
    throw new UnsupportedOperationException("glGetBufferPointerv not implemented")

  private lazy val _glMapBufferRange = h30("glMapBufferRange", FunctionDescriptor.of(P30, I30, I30, I30, I30))
  override def glMapBufferRange(target: BufferTarget, offset: Int, length: Int, access: Int): Buffer = {
    val ptr = _glMapBufferRange.invoke(target, offset, length, access).asInstanceOf[MemorySegment]
    if (ptr == MemorySegment.NULL) null.asInstanceOf[Buffer] // @nowarn - GL interop boundary
    else ptr.reinterpret(length.toLong).asByteBuffer().order(ByteOrder.nativeOrder())
  }

  private lazy val _glFlushMappedBufferRange = h30("glFlushMappedBufferRange", FunctionDescriptor.ofVoid(I30, I30, I30))
  override def glFlushMappedBufferRange(target: BufferTarget, offset: Int, length: Int): Unit =
    _glFlushMappedBufferRange.invoke(target, offset, length)

  // ─── Draw buffers ─────────────────────────────────────────────────────────

  private lazy val _glDrawBuffers = h30("glDrawBuffers", FunctionDescriptor.ofVoid(I30, P30))
  override def glDrawBuffers(n: Int, bufs: IntBuffer): Unit = _glDrawBuffers.invoke(n, bufAddr30(bufs))

  // ─── Non-square uniform matrices ──────────────────────────────────────────

  private lazy val _glUniformMatrix2x3fv = h30("glUniformMatrix2x3fv", FunctionDescriptor.ofVoid(I30, I30, B30, P30))
  override def glUniformMatrix2x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    _glUniformMatrix2x3fv.invoke(location, count, glBool30(transpose), bufAddr30(value))

  private lazy val _glUniformMatrix3x2fv = h30("glUniformMatrix3x2fv", FunctionDescriptor.ofVoid(I30, I30, B30, P30))
  override def glUniformMatrix3x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    _glUniformMatrix3x2fv.invoke(location, count, glBool30(transpose), bufAddr30(value))

  private lazy val _glUniformMatrix2x4fv = h30("glUniformMatrix2x4fv", FunctionDescriptor.ofVoid(I30, I30, B30, P30))
  override def glUniformMatrix2x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    _glUniformMatrix2x4fv.invoke(location, count, glBool30(transpose), bufAddr30(value))

  private lazy val _glUniformMatrix4x2fv = h30("glUniformMatrix4x2fv", FunctionDescriptor.ofVoid(I30, I30, B30, P30))
  override def glUniformMatrix4x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    _glUniformMatrix4x2fv.invoke(location, count, glBool30(transpose), bufAddr30(value))

  private lazy val _glUniformMatrix3x4fv = h30("glUniformMatrix3x4fv", FunctionDescriptor.ofVoid(I30, I30, B30, P30))
  override def glUniformMatrix3x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    _glUniformMatrix3x4fv.invoke(location, count, glBool30(transpose), bufAddr30(value))

  private lazy val _glUniformMatrix4x3fv = h30("glUniformMatrix4x3fv", FunctionDescriptor.ofVoid(I30, I30, B30, P30))
  override def glUniformMatrix4x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    _glUniformMatrix4x3fv.invoke(location, count, glBool30(transpose), bufAddr30(value))

  // ─── Framebuffer blit / multisample / layer ───────────────────────────────

  private lazy val _glBlitFramebuffer = h30("glBlitFramebuffer", FunctionDescriptor.ofVoid(I30, I30, I30, I30, I30, I30, I30, I30, I30, I30))
  override def glBlitFramebuffer(srcX0: Int, srcY0: Int, srcX1: Int, srcY1: Int, dstX0: Int, dstY0: Int, dstX1: Int, dstY1: Int, mask: ClearMask, filter: Int): Unit =
    _glBlitFramebuffer.invoke(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask.toInt, filter)

  private lazy val _glRenderbufferStorageMultisample = h30("glRenderbufferStorageMultisample", FunctionDescriptor.ofVoid(I30, I30, I30, I30, I30))
  override def glRenderbufferStorageMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int): Unit =
    _glRenderbufferStorageMultisample.invoke(target, samples, internalformat, width, height)

  private lazy val _glFramebufferTextureLayer = h30("glFramebufferTextureLayer", FunctionDescriptor.ofVoid(I30, I30, I30, I30, I30))
  override def glFramebufferTextureLayer(target: Int, attachment: Int, texture: Int, level: Int, layer: Int): Unit =
    _glFramebufferTextureLayer.invoke(target, attachment, texture, level, layer)

  // ─── Vertex array objects ─────────────────────────────────────────────────

  private lazy val _glGenVertexArrays = h30("glGenVertexArrays", FunctionDescriptor.ofVoid(I30, P30))

  override def glGenVertexArrays(n: Int, arrays: IntBuffer): Unit = _glGenVertexArrays.invoke(n, bufAddr30(arrays))

  override def glGenVertexArrays(n: Int, arrays: Array[Int], offset: Int): Unit = {
    val buf = ByteBuffer.allocateDirect(n * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
    glGenVertexArrays(n, buf)
    var i = 0
    while (i < n) { arrays(offset + i) = buf.get(i); i += 1 }
  }

  private lazy val _glDeleteVertexArrays = h30("glDeleteVertexArrays", FunctionDescriptor.ofVoid(I30, P30))

  override def glDeleteVertexArrays(n: Int, arrays: IntBuffer): Unit = _glDeleteVertexArrays.invoke(n, bufAddr30(arrays))

  override def glDeleteVertexArrays(n: Int, arrays: Array[Int], offset: Int): Unit = {
    val buf = ByteBuffer.allocateDirect(n * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
    var i   = 0
    while (i < n) { buf.put(i, arrays(offset + i)); i += 1 }
    glDeleteVertexArrays(n, buf)
  }

  private lazy val _glBindVertexArray = h30("glBindVertexArray", FunctionDescriptor.ofVoid(I30))
  override def glBindVertexArray(array: Int): Unit = _glBindVertexArray.invoke(array)

  private lazy val _glIsVertexArray = h30("glIsVertexArray", FunctionDescriptor.of(B30, I30))
  override def glIsVertexArray(array: Int): Boolean = fromGlBool30(_glIsVertexArray.invoke(array))

  // ─── Transform feedback ───────────────────────────────────────────────────

  private lazy val _glBeginTransformFeedback = h30("glBeginTransformFeedback", FunctionDescriptor.ofVoid(I30))
  override def glBeginTransformFeedback(primitiveMode: PrimitiveMode): Unit = _glBeginTransformFeedback.invoke(primitiveMode.toInt)

  private lazy val _glEndTransformFeedback = h30("glEndTransformFeedback", FunctionDescriptor.ofVoid())
  override def glEndTransformFeedback(): Unit = _glEndTransformFeedback.invoke()

  private lazy val _glBindBufferRange = h30("glBindBufferRange", FunctionDescriptor.ofVoid(I30, I30, I30, I30, I30))
  override def glBindBufferRange(target: BufferTarget, index: Int, buffer: Int, offset: Int, size: Int): Unit =
    _glBindBufferRange.invoke(target.toInt, index, buffer, offset, size)

  private lazy val _glBindBufferBase = h30("glBindBufferBase", FunctionDescriptor.ofVoid(I30, I30, I30))
  override def glBindBufferBase(target: BufferTarget, index: Int, buffer: Int): Unit =
    _glBindBufferBase.invoke(target.toInt, index, buffer)

  // glTransformFeedbackVaryings: void(GLuint, GLsizei, const GLchar *const *, GLenum)
  private lazy val _glTransformFeedbackVaryings = h30("glTransformFeedbackVaryings", FunctionDescriptor.ofVoid(I30, I30, P30, I30))
  override def glTransformFeedbackVaryings(program: Int, varyings: Array[String], bufferMode: Int): Unit = {
    val arena = Arena.ofConfined()
    try {
      val ptrs = arena.allocate(P30, varyings.length.toLong)
      var i    = 0
      while (i < varyings.length) {
        ptrs.setAtIndex(P30, i.toLong, arena.allocateFrom(varyings(i)))
        i += 1
      }
      _glTransformFeedbackVaryings.invoke(program, varyings.length, ptrs, bufferMode)
    } finally arena.close()
  }

  private lazy val _glBindTransformFeedback = h30("glBindTransformFeedback", FunctionDescriptor.ofVoid(I30, I30))
  override def glBindTransformFeedback(target: Int, id: Int): Unit = _glBindTransformFeedback.invoke(target, id)

  private lazy val _glGenTransformFeedbacks = h30("glGenTransformFeedbacks", FunctionDescriptor.ofVoid(I30, P30))

  override def glGenTransformFeedbacks(n: Int, ids: IntBuffer): Unit = _glGenTransformFeedbacks.invoke(n, bufAddr30(ids))

  override def glGenTransformFeedbacks(n: Int, ids: Array[Int], offset: Int): Unit = {
    val buf = ByteBuffer.allocateDirect(n * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
    glGenTransformFeedbacks(n, buf)
    var i = 0
    while (i < n) { ids(offset + i) = buf.get(i); i += 1 }
  }

  private lazy val _glDeleteTransformFeedbacks = h30("glDeleteTransformFeedbacks", FunctionDescriptor.ofVoid(I30, P30))

  override def glDeleteTransformFeedbacks(n: Int, ids: IntBuffer): Unit = _glDeleteTransformFeedbacks.invoke(n, bufAddr30(ids))

  override def glDeleteTransformFeedbacks(n: Int, ids: Array[Int], offset: Int): Unit = {
    val buf = ByteBuffer.allocateDirect(n * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
    var i   = 0
    while (i < n) { buf.put(i, ids(offset + i)); i += 1 }
    glDeleteTransformFeedbacks(n, buf)
  }

  private lazy val _glIsTransformFeedback = h30("glIsTransformFeedback", FunctionDescriptor.of(B30, I30))
  override def glIsTransformFeedback(id: Int): Boolean = fromGlBool30(_glIsTransformFeedback.invoke(id))

  private lazy val _glPauseTransformFeedback = h30("glPauseTransformFeedback", FunctionDescriptor.ofVoid())
  override def glPauseTransformFeedback(): Unit = _glPauseTransformFeedback.invoke()

  private lazy val _glResumeTransformFeedback = h30("glResumeTransformFeedback", FunctionDescriptor.ofVoid())
  override def glResumeTransformFeedback(): Unit = _glResumeTransformFeedback.invoke()

  // ─── Vertex attrib integer ────────────────────────────────────────────────

  private lazy val _glVertexAttribIPointer = h30("glVertexAttribIPointer", FunctionDescriptor.ofVoid(I30, I30, I30, I30, P30))
  override def glVertexAttribIPointer(index: Int, size: Int, `type`: DataType, stride: Int, offset: Int): Unit =
    _glVertexAttribIPointer.invoke(index, size, `type`, stride, MemorySegment.ofAddress(offset.toLong))

  private lazy val _glGetVertexAttribIiv = h30("glGetVertexAttribIiv", FunctionDescriptor.ofVoid(I30, I30, P30))
  override def glGetVertexAttribIiv(index: Int, pname: Int, params: IntBuffer): Unit =
    _glGetVertexAttribIiv.invoke(index, pname, bufAddr30(params))

  private lazy val _glGetVertexAttribIuiv = h30("glGetVertexAttribIuiv", FunctionDescriptor.ofVoid(I30, I30, P30))
  override def glGetVertexAttribIuiv(index: Int, pname: Int, params: IntBuffer): Unit =
    _glGetVertexAttribIuiv.invoke(index, pname, bufAddr30(params))

  private lazy val _glVertexAttribI4i = h30("glVertexAttribI4i", FunctionDescriptor.ofVoid(I30, I30, I30, I30, I30))
  override def glVertexAttribI4i(index: Int, x: Int, y: Int, z: Int, w: Int): Unit =
    _glVertexAttribI4i.invoke(index, x, y, z, w)

  private lazy val _glVertexAttribI4ui = h30("glVertexAttribI4ui", FunctionDescriptor.ofVoid(I30, I30, I30, I30, I30))
  override def glVertexAttribI4ui(index: Int, x: Int, y: Int, z: Int, w: Int): Unit =
    _glVertexAttribI4ui.invoke(index, x, y, z, w)

  private lazy val _glVertexAttribDivisor = h30("glVertexAttribDivisor", FunctionDescriptor.ofVoid(I30, I30))
  override def glVertexAttribDivisor(index: Int, divisor: Int): Unit = _glVertexAttribDivisor.invoke(index, divisor)

  // ─── Unsigned int uniforms ────────────────────────────────────────────────

  private lazy val _glGetUniformuiv = h30("glGetUniformuiv", FunctionDescriptor.ofVoid(I30, I30, P30))
  override def glGetUniformuiv(program: Int, location: Int, params: IntBuffer): Unit =
    _glGetUniformuiv.invoke(program, location, bufAddr30(params))

  private lazy val _glGetFragDataLocation = h30("glGetFragDataLocation", FunctionDescriptor.of(I30, I30, P30))
  override def glGetFragDataLocation(program: Int, name: String): Int = {
    val arena = Arena.ofConfined()
    try _glGetFragDataLocation.invoke(program, arena.allocateFrom(name)).asInstanceOf[Int]
    finally arena.close()
  }

  private lazy val _glUniform1uiv = h30("glUniform1uiv", FunctionDescriptor.ofVoid(I30, I30, P30))
  override def glUniform1uiv(location: Int, count: Int, value: IntBuffer): Unit =
    _glUniform1uiv.invoke(location, count, bufAddr30(value))

  private lazy val _glUniform3uiv = h30("glUniform3uiv", FunctionDescriptor.ofVoid(I30, I30, P30))
  override def glUniform3uiv(location: Int, count: Int, value: IntBuffer): Unit =
    _glUniform3uiv.invoke(location, count, bufAddr30(value))

  private lazy val _glUniform4uiv = h30("glUniform4uiv", FunctionDescriptor.ofVoid(I30, I30, P30))
  override def glUniform4uiv(location: Int, count: Int, value: IntBuffer): Unit =
    _glUniform4uiv.invoke(location, count, bufAddr30(value))

  // ─── Clear buffer ─────────────────────────────────────────────────────────

  private lazy val _glClearBufferiv = h30("glClearBufferiv", FunctionDescriptor.ofVoid(I30, I30, P30))
  override def glClearBufferiv(buffer: Int, drawbuffer: Int, value: IntBuffer): Unit =
    _glClearBufferiv.invoke(buffer, drawbuffer, bufAddr30(value))

  private lazy val _glClearBufferuiv = h30("glClearBufferuiv", FunctionDescriptor.ofVoid(I30, I30, P30))
  override def glClearBufferuiv(buffer: Int, drawbuffer: Int, value: IntBuffer): Unit =
    _glClearBufferuiv.invoke(buffer, drawbuffer, bufAddr30(value))

  private lazy val _glClearBufferfv = h30("glClearBufferfv", FunctionDescriptor.ofVoid(I30, I30, P30))
  override def glClearBufferfv(buffer: Int, drawbuffer: Int, value: FloatBuffer): Unit =
    _glClearBufferfv.invoke(buffer, drawbuffer, bufAddr30(value))

  private lazy val _glClearBufferfi = h30("glClearBufferfi", FunctionDescriptor.ofVoid(I30, I30, F30, I30))
  override def glClearBufferfi(buffer: Int, drawbuffer: Int, depth: Float, stencil: Int): Unit =
    _glClearBufferfi.invoke(buffer, drawbuffer, depth, stencil)

  // ─── String query ─────────────────────────────────────────────────────────

  private lazy val _glGetStringi = h30("glGetStringi", FunctionDescriptor.of(P30, I30, I30))
  override def glGetStringi(name: Int, index: Int): String = {
    val ptr = _glGetStringi.invoke(name, index).asInstanceOf[MemorySegment]
    if (ptr == MemorySegment.NULL) null // @nowarn - GL interop boundary
    else ptr.reinterpret(Long.MaxValue).getString(0)
  }

  // ─── Buffer copy ──────────────────────────────────────────────────────────

  private lazy val _glCopyBufferSubData = h30("glCopyBufferSubData", FunctionDescriptor.ofVoid(I30, I30, I30, I30, I30))
  override def glCopyBufferSubData(readTarget: BufferTarget, writeTarget: BufferTarget, readOffset: Int, writeOffset: Int, size: Int): Unit =
    _glCopyBufferSubData.invoke(readTarget, writeTarget, readOffset, writeOffset, size)

  // ─── Uniform blocks ───────────────────────────────────────────────────────

  // glGetUniformIndices: void(GLuint, GLsizei, const GLchar *const *, GLuint *)
  private lazy val _glGetUniformIndices = h30("glGetUniformIndices", FunctionDescriptor.ofVoid(I30, I30, P30, P30))
  override def glGetUniformIndices(program: Int, uniformNames: Array[String], uniformIndices: IntBuffer): Unit = {
    val arena = Arena.ofConfined()
    try {
      val ptrs = arena.allocate(P30, uniformNames.length.toLong)
      var i    = 0
      while (i < uniformNames.length) {
        ptrs.setAtIndex(P30, i.toLong, arena.allocateFrom(uniformNames(i)))
        i += 1
      }
      _glGetUniformIndices.invoke(program, uniformNames.length, ptrs, bufAddr30(uniformIndices))
    } finally arena.close()
  }

  private lazy val _glGetActiveUniformsiv = h30("glGetActiveUniformsiv", FunctionDescriptor.ofVoid(I30, I30, P30, I30, P30))
  override def glGetActiveUniformsiv(program: Int, uniformCount: Int, uniformIndices: IntBuffer, pname: Int, params: IntBuffer): Unit =
    _glGetActiveUniformsiv.invoke(program, uniformCount, bufAddr30(uniformIndices), pname, bufAddr30(params))

  private lazy val _glGetUniformBlockIndex = h30("glGetUniformBlockIndex", FunctionDescriptor.of(I30, I30, P30))
  override def glGetUniformBlockIndex(program: Int, uniformBlockName: String): Int = {
    val arena = Arena.ofConfined()
    try _glGetUniformBlockIndex.invoke(program, arena.allocateFrom(uniformBlockName)).asInstanceOf[Int]
    finally arena.close()
  }

  private lazy val _glGetActiveUniformBlockiv = h30("glGetActiveUniformBlockiv", FunctionDescriptor.ofVoid(I30, I30, I30, P30))
  override def glGetActiveUniformBlockiv(program: Int, uniformBlockIndex: Int, pname: Int, params: IntBuffer): Unit =
    _glGetActiveUniformBlockiv.invoke(program, uniformBlockIndex, pname, bufAddr30(params))

  // glGetActiveUniformBlockName: void(GLuint, GLuint, GLsizei, GLsizei *, GLchar *)
  private lazy val _glGetActiveUniformBlockName = h30("glGetActiveUniformBlockName", FunctionDescriptor.ofVoid(I30, I30, I30, P30, P30))

  override def glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int, length: Buffer, uniformBlockName: Buffer): Unit =
    _glGetActiveUniformBlockName.invoke(program, uniformBlockIndex, 1024, bufAddr30(length), bufAddr30(uniformBlockName))

  override def glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int): String = {
    val arena = Arena.ofConfined()
    try {
      val lengthBuf = arena.allocate(I30)
      val nameBuf   = arena.allocate(JAVA_BYTE, 1024L)
      _glGetActiveUniformBlockName.invoke(program, uniformBlockIndex, 1024, lengthBuf, nameBuf)
      val len = lengthBuf.get(I30, 0)
      nameBuf.reinterpret(len.toLong + 1).getString(0)
    } finally arena.close()
  }

  private lazy val _glUniformBlockBinding = h30("glUniformBlockBinding", FunctionDescriptor.ofVoid(I30, I30, I30))
  override def glUniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int): Unit =
    _glUniformBlockBinding.invoke(program, uniformBlockIndex, uniformBlockBinding)

  // ─── Instancing ───────────────────────────────────────────────────────────

  private lazy val _glDrawArraysInstanced = h30("glDrawArraysInstanced", FunctionDescriptor.ofVoid(I30, I30, I30, I30))
  override def glDrawArraysInstanced(mode: PrimitiveMode, first: Int, count: Int, instanceCount: Int): Unit =
    _glDrawArraysInstanced.invoke(mode, first, count, instanceCount)

  private lazy val _glDrawElementsInstanced = h30("glDrawElementsInstanced", FunctionDescriptor.ofVoid(I30, I30, I30, P30, I30))
  override def glDrawElementsInstanced(mode: PrimitiveMode, count: Int, `type`: DataType, indicesOffset: Int, instanceCount: Int): Unit =
    _glDrawElementsInstanced.invoke(mode, count, `type`, MemorySegment.ofAddress(indicesOffset.toLong), instanceCount)

  // ─── 64-bit queries ───────────────────────────────────────────────────────

  private lazy val _glGetInteger64v = h30("glGetInteger64v", FunctionDescriptor.ofVoid(I30, P30))
  override def glGetInteger64v(pname: Int, params: LongBuffer): Unit = _glGetInteger64v.invoke(pname, bufAddr30(params))

  private lazy val _glGetBufferParameteri64v = h30("glGetBufferParameteri64v", FunctionDescriptor.ofVoid(I30, I30, P30))
  override def glGetBufferParameteri64v(target: BufferTarget, pname: Int, params: LongBuffer): Unit =
    _glGetBufferParameteri64v.invoke(target, pname, bufAddr30(params))

  // ─── Samplers ─────────────────────────────────────────────────────────────

  private lazy val _glGenSamplers = h30("glGenSamplers", FunctionDescriptor.ofVoid(I30, P30))

  override def glGenSamplers(count: Int, samplers: IntBuffer): Unit = _glGenSamplers.invoke(count, bufAddr30(samplers))

  override def glGenSamplers(count: Int, samplers: Array[Int], offset: Int): Unit = {
    val buf = ByteBuffer.allocateDirect(count * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
    glGenSamplers(count, buf)
    var i = 0
    while (i < count) { samplers(offset + i) = buf.get(i); i += 1 }
  }

  private lazy val _glDeleteSamplers = h30("glDeleteSamplers", FunctionDescriptor.ofVoid(I30, P30))

  override def glDeleteSamplers(count: Int, samplers: IntBuffer): Unit = _glDeleteSamplers.invoke(count, bufAddr30(samplers))

  override def glDeleteSamplers(count: Int, samplers: Array[Int], offset: Int): Unit = {
    val buf = ByteBuffer.allocateDirect(count * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
    var i   = 0
    while (i < count) { buf.put(i, samplers(offset + i)); i += 1 }
    glDeleteSamplers(count, buf)
  }

  private lazy val _glIsSampler = h30("glIsSampler", FunctionDescriptor.of(B30, I30))
  override def glIsSampler(sampler: Int): Boolean = fromGlBool30(_glIsSampler.invoke(sampler))

  private lazy val _glBindSampler = h30("glBindSampler", FunctionDescriptor.ofVoid(I30, I30))
  override def glBindSampler(unit: Int, sampler: Int): Unit = _glBindSampler.invoke(unit, sampler)

  private lazy val _glSamplerParameteri = h30("glSamplerParameteri", FunctionDescriptor.ofVoid(I30, I30, I30))
  override def glSamplerParameteri(sampler: Int, pname: Int, param: Int): Unit = _glSamplerParameteri.invoke(sampler, pname, param)

  private lazy val _glSamplerParameteriv = h30("glSamplerParameteriv", FunctionDescriptor.ofVoid(I30, I30, P30))
  override def glSamplerParameteriv(sampler: Int, pname: Int, param: IntBuffer): Unit = _glSamplerParameteriv.invoke(sampler, pname, bufAddr30(param))

  private lazy val _glSamplerParameterf = h30("glSamplerParameterf", FunctionDescriptor.ofVoid(I30, I30, F30))
  override def glSamplerParameterf(sampler: Int, pname: Int, param: Float): Unit = _glSamplerParameterf.invoke(sampler, pname, param)

  private lazy val _glSamplerParameterfv = h30("glSamplerParameterfv", FunctionDescriptor.ofVoid(I30, I30, P30))
  override def glSamplerParameterfv(sampler: Int, pname: Int, param: FloatBuffer): Unit = _glSamplerParameterfv.invoke(sampler, pname, bufAddr30(param))

  private lazy val _glGetSamplerParameteriv = h30("glGetSamplerParameteriv", FunctionDescriptor.ofVoid(I30, I30, P30))
  override def glGetSamplerParameteriv(sampler: Int, pname: Int, params: IntBuffer): Unit = _glGetSamplerParameteriv.invoke(sampler, pname, bufAddr30(params))

  private lazy val _glGetSamplerParameterfv = h30("glGetSamplerParameterfv", FunctionDescriptor.ofVoid(I30, I30, P30))
  override def glGetSamplerParameterfv(sampler: Int, pname: Int, params: FloatBuffer): Unit = _glGetSamplerParameterfv.invoke(sampler, pname, bufAddr30(params))

  // ─── Program parameters / framebuffer invalidation ────────────────────────

  private lazy val _glProgramParameteri = h30("glProgramParameteri", FunctionDescriptor.ofVoid(I30, I30, I30))
  override def glProgramParameteri(program: Int, pname: Int, value: Int): Unit = _glProgramParameteri.invoke(program, pname, value)

  private lazy val _glInvalidateFramebuffer = h30("glInvalidateFramebuffer", FunctionDescriptor.ofVoid(I30, I30, P30))
  override def glInvalidateFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer): Unit =
    _glInvalidateFramebuffer.invoke(target, numAttachments, bufAddr30(attachments))

  private lazy val _glInvalidateSubFramebuffer = h30("glInvalidateSubFramebuffer", FunctionDescriptor.ofVoid(I30, I30, P30, I30, I30, I30, I30))
  override def glInvalidateSubFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer, x: Int, y: Int, width: Int, height: Int): Unit =
    _glInvalidateSubFramebuffer.invoke(target, numAttachments, bufAddr30(attachments), x, y, width, height)
}
