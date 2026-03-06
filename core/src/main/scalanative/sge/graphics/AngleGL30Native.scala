/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (replaces Lwjgl3GL30.java)
 *   Convention: Scala Native @extern bindings to ANGLE libGLESv2
 *   Convention: Extends AngleGL20Native — inherits all GL ES 2.0 bindings
 *   Idiom: split packages; no return
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import java.nio.{ Buffer, ByteBuffer, ByteOrder, FloatBuffer, IntBuffer, LongBuffer }

import scala.scalanative.unsafe.*

import NativeGlHelper.*

// ─── C extern declarations for GL ES 3.0 ────────────────────────────────────

@link("GLESv2")
@extern
private[graphics] object GL30C {
  def glReadBuffer(mode:          CInt):                                                                                                                                         Unit = extern
  def glDrawRangeElements(mode:   CInt, start: CInt, end:    CInt, count: CInt, tp: CInt, indices: Ptr[Byte]):                                                                   Unit = extern
  def glTexImage3D(target:        CInt, level: CInt, intfmt: CInt, w:     CInt, h:  CInt, depth:   CInt, border: CInt, fmt:   CInt, tp:  CInt, pixels: Ptr[Byte]):               Unit = extern
  def glTexSubImage3D(target:     CInt, level: CInt, xo:     CInt, yo:    CInt, zo: CInt, w:       CInt, h:      CInt, depth: CInt, fmt: CInt, tp:     CInt, pixels: Ptr[Byte]): Unit = extern
  def glCopyTexSubImage3D(target: CInt, level: CInt, xo:     CInt, yo:    CInt, zo: CInt, x:       CInt, y:      CInt, w:     CInt, h:   CInt):                                  Unit = extern

  // Queries
  def glGenQueries(n:         CInt, ids:   Ptr[CInt]):               Unit          = extern
  def glDeleteQueries(n:      CInt, ids:   Ptr[CInt]):               Unit          = extern
  def glIsQuery(id:           CInt):                                 CUnsignedChar = extern
  def glBeginQuery(target:    CInt, id:    CInt):                    Unit          = extern
  def glEndQuery(target:      CInt):                                 Unit          = extern
  def glGetQueryiv(target:    CInt, pname: CInt, params: Ptr[CInt]): Unit          = extern
  def glGetQueryObjectuiv(id: CInt, pname: CInt, params: Ptr[CInt]): Unit          = extern

  // Buffer mapping
  def glUnmapBuffer(target:            CInt):                                           CUnsignedChar = extern
  def glMapBufferRange(target:         CInt, offset: CInt, length: CInt, access: CInt): Ptr[Byte]     = extern
  def glFlushMappedBufferRange(target: CInt, offset: CInt, length: CInt):               Unit          = extern
  def glDrawBuffers(n:                 CInt, bufs:   Ptr[CInt]):                        Unit          = extern

  // Non-square uniform matrices
  def glUniformMatrix2x3fv(loc: CInt, count: CInt, transpose: CUnsignedChar, value: Ptr[CFloat]): Unit = extern
  def glUniformMatrix3x2fv(loc: CInt, count: CInt, transpose: CUnsignedChar, value: Ptr[CFloat]): Unit = extern
  def glUniformMatrix2x4fv(loc: CInt, count: CInt, transpose: CUnsignedChar, value: Ptr[CFloat]): Unit = extern
  def glUniformMatrix4x2fv(loc: CInt, count: CInt, transpose: CUnsignedChar, value: Ptr[CFloat]): Unit = extern
  def glUniformMatrix3x4fv(loc: CInt, count: CInt, transpose: CUnsignedChar, value: Ptr[CFloat]): Unit = extern
  def glUniformMatrix4x3fv(loc: CInt, count: CInt, transpose: CUnsignedChar, value: Ptr[CFloat]): Unit = extern

  // Framebuffer blit / multisample
  def glBlitFramebuffer(sx0:                   CInt, sy0:     CInt, sx1:     CInt, sy1:   CInt, dx0:   CInt, dy0: CInt, dx1: CInt, dy1: CInt, mask: CInt, filter: CInt): Unit = extern
  def glRenderbufferStorageMultisample(target: CInt, samples: CInt, intfmt:  CInt, w:     CInt, h:     CInt):                                                            Unit = extern
  def glFramebufferTextureLayer(target:        CInt, attach:  CInt, texture: CInt, level: CInt, layer: CInt):                                                            Unit = extern

  // VAOs
  def glGenVertexArrays(n:     CInt, arrays: Ptr[CInt]): Unit          = extern
  def glDeleteVertexArrays(n:  CInt, arrays: Ptr[CInt]): Unit          = extern
  def glBindVertexArray(array: CInt):                    Unit          = extern
  def glIsVertexArray(array:   CInt):                    CUnsignedChar = extern

  // Transform feedback
  def glBeginTransformFeedback(primitiveMode: CInt):                                                                    Unit          = extern
  def glEndTransformFeedback():                                                                                         Unit          = extern
  def glBindBufferRange(target:               CInt, index: CInt, buffer:   CInt, offset:             CInt, size: CInt): Unit          = extern
  def glBindBufferBase(target:                CInt, index: CInt, buffer:   CInt):                                       Unit          = extern
  def glTransformFeedbackVaryings(program:    CInt, count: CInt, varyings: Ptr[CString], bufferMode: CInt):             Unit          = extern
  def glBindTransformFeedback(target:         CInt, id:    CInt):                                                       Unit          = extern
  def glGenTransformFeedbacks(n:              CInt, ids:   Ptr[CInt]):                                                  Unit          = extern
  def glDeleteTransformFeedbacks(n:           CInt, ids:   Ptr[CInt]):                                                  Unit          = extern
  def glIsTransformFeedback(id:               CInt):                                                                    CUnsignedChar = extern
  def glPauseTransformFeedback():                                                                                       Unit          = extern
  def glResumeTransformFeedback():                                                                                      Unit          = extern

  // Vertex attrib integer
  def glVertexAttribIPointer(index: CInt, size:    CInt, tp:     CInt, stride: CInt, pointer: Ptr[Byte]): Unit = extern
  def glGetVertexAttribIiv(index:   CInt, pname:   CInt, params: Ptr[CInt]):                              Unit = extern
  def glGetVertexAttribIuiv(index:  CInt, pname:   CInt, params: Ptr[CInt]):                              Unit = extern
  def glVertexAttribI4i(index:      CInt, x:       CInt, y:      CInt, z:      CInt, w:       CInt):      Unit = extern
  def glVertexAttribI4ui(index:     CInt, x:       CInt, y:      CInt, z:      CInt, w:       CInt):      Unit = extern
  def glVertexAttribDivisor(index:  CInt, divisor: CInt):                                                 Unit = extern

  // Unsigned int uniforms
  def glGetUniformuiv(program:       CInt, location: CInt, params: Ptr[CInt]): Unit = extern
  def glGetFragDataLocation(program: CInt, name:     CString):                 CInt = extern
  def glUniform1uiv(loc:             CInt, count:    CInt, value:  Ptr[CInt]): Unit = extern
  def glUniform3uiv(loc:             CInt, count:    CInt, value:  Ptr[CInt]): Unit = extern
  def glUniform4uiv(loc:             CInt, count:    CInt, value:  Ptr[CInt]): Unit = extern

  // Clear buffer
  def glClearBufferiv(buffer:  CInt, drawbuffer: CInt, value: Ptr[CInt]):             Unit = extern
  def glClearBufferuiv(buffer: CInt, drawbuffer: CInt, value: Ptr[CInt]):             Unit = extern
  def glClearBufferfv(buffer:  CInt, drawbuffer: CInt, value: Ptr[CFloat]):           Unit = extern
  def glClearBufferfi(buffer:  CInt, drawbuffer: CInt, depth: CFloat, stencil: CInt): Unit = extern

  // String query
  def glGetStringi(name: CInt, index: CInt): CString = extern

  // Buffer copy
  def glCopyBufferSubData(readTarget: CInt, writeTarget: CInt, readOffset: CInt, writeOffset: CInt, size: CInt): Unit = extern

  // Uniform blocks
  def glGetUniformIndices(program:         CInt, count: CInt, names:   Ptr[CString], indices: Ptr[CInt]):                  Unit = extern
  def glGetActiveUniformsiv(program:       CInt, count: CInt, indices: Ptr[CInt], pname:      CInt, params:    Ptr[CInt]): Unit = extern
  def glGetUniformBlockIndex(program:      CInt, name:  CString):                                                          CInt = extern
  def glGetActiveUniformBlockiv(program:   CInt, index: CInt, pname:   CInt, params:          Ptr[CInt]):                  Unit = extern
  def glGetActiveUniformBlockName(program: CInt, index: CInt, bufSize: CInt, length:          Ptr[CInt], name: CString):   Unit = extern
  def glUniformBlockBinding(program:       CInt, index: CInt, binding: CInt):                                              Unit = extern

  // Instancing
  def glDrawArraysInstanced(mode:   CInt, first: CInt, count: CInt, instanceCount: CInt):                           Unit = extern
  def glDrawElementsInstanced(mode: CInt, count: CInt, tp:    CInt, indices:       Ptr[Byte], instanceCount: CInt): Unit = extern

  // 64-bit queries
  def glGetInteger64v(pname:           CInt, params: Ptr[CLong]):               Unit = extern
  def glGetBufferParameteri64v(target: CInt, pname:  CInt, params: Ptr[CLong]): Unit = extern

  // Samplers
  def glGenSamplers(count:             CInt, samplers: Ptr[CInt]):                 Unit          = extern
  def glDeleteSamplers(count:          CInt, samplers: Ptr[CInt]):                 Unit          = extern
  def glIsSampler(sampler:             CInt):                                      CUnsignedChar = extern
  def glBindSampler(unit:              CInt, sampler:  CInt):                      Unit          = extern
  def glSamplerParameteri(sampler:     CInt, pname:    CInt, param:  CInt):        Unit          = extern
  def glSamplerParameteriv(sampler:    CInt, pname:    CInt, param:  Ptr[CInt]):   Unit          = extern
  def glSamplerParameterf(sampler:     CInt, pname:    CInt, param:  CFloat):      Unit          = extern
  def glSamplerParameterfv(sampler:    CInt, pname:    CInt, param:  Ptr[CFloat]): Unit          = extern
  def glGetSamplerParameteriv(sampler: CInt, pname:    CInt, params: Ptr[CInt]):   Unit          = extern
  def glGetSamplerParameterfv(sampler: CInt, pname:    CInt, params: Ptr[CFloat]): Unit          = extern

  // Program params / framebuffer invalidation
  def glProgramParameteri(program:       CInt, pname:          CInt, value:       CInt):                                          Unit = extern
  def glInvalidateFramebuffer(target:    CInt, numAttachments: CInt, attachments: Ptr[CInt]):                                     Unit = extern
  def glInvalidateSubFramebuffer(target: CInt, numAttachments: CInt, attachments: Ptr[CInt], x: CInt, y: CInt, w: CInt, h: CInt): Unit = extern
}

// ─── GL30 wrapper ─────────────────────────────────────────────────────────────

/** OpenGL ES 3.0 implementation via ANGLE (libGLESv2) using Scala Native @extern bindings.
  *
  * Extends [[AngleGL20Native]] with the additional GL ES 3.0 methods.
  */
class AngleGL30Native extends AngleGL20Native with GL30 {

  override def glReadBuffer(mode: Int): Unit = GL30C.glReadBuffer(mode)

  override def glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, `type`: Int, indices: Buffer): Unit =
    GL30C.glDrawRangeElements(mode, start, end, count, `type`, bufPtr(indices))

  override def glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, `type`: Int, offset: Int): Unit =
    GL30C.glDrawRangeElements(mode, start, end, count, `type`, offsetPtr(offset))

  // ─── Texture 3D ───────────────────────────────────────────────────────────

  override def glTexImage3D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: Int, `type`: Int, pixels: Buffer): Unit =
    GL30C.glTexImage3D(target, level, internalformat, width, height, depth, border, format, `type`, bufPtr(pixels))

  override def glTexImage3D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: Int, `type`: Int, offset: Int): Unit =
    GL30C.glTexImage3D(target, level, internalformat, width, height, depth, border, format, `type`, offsetPtr(offset))

  override def glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, `type`: Int, offset: Int): Unit =
    GL20C.glTexImage2D(target, level, internalformat, width, height, border, format, `type`, offsetPtr(offset))

  override def glTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int, format: Int, `type`: Int, pixels: Buffer): Unit =
    GL30C.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, `type`, bufPtr(pixels))

  override def glTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int, format: Int, `type`: Int, offset: Int): Unit =
    GL30C.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, `type`, offsetPtr(offset))

  override def glTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, `type`: Int, offset: Int): Unit =
    GL20C.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, `type`, offsetPtr(offset))

  override def glCopyTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit =
    GL30C.glCopyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height)

  // ─── Queries ──────────────────────────────────────────────────────────────

  override def glGenQueries(n: Int, ids: IntBuffer): Unit = GL30C.glGenQueries(n, bufPtr(ids).asInstanceOf[Ptr[CInt]])

  override def glGenQueries(n: Int, ids: Array[Int], offset: Int): Unit = {
    val buf = allocDirectInt(n); glGenQueries(n, buf)
    var i   = 0; while (i < n) { ids(offset + i) = buf.get(i); i += 1 }
  }

  override def glDeleteQueries(n: Int, ids: IntBuffer): Unit = GL30C.glDeleteQueries(n, bufPtr(ids).asInstanceOf[Ptr[CInt]])

  override def glDeleteQueries(n: Int, ids: Array[Int], offset: Int): Unit = {
    val buf = allocDirectInt(n)
    var i   = 0; while (i < n) { buf.put(i, ids(offset + i)); i += 1 }
    glDeleteQueries(n, buf)
  }

  override def glIsQuery(id:           Int):                                Boolean = fromGlBool(GL30C.glIsQuery(id))
  override def glBeginQuery(target:    Int, id:    Int):                    Unit    = GL30C.glBeginQuery(target, id)
  override def glEndQuery(target:      Int):                                Unit    = GL30C.glEndQuery(target)
  override def glGetQueryiv(target:    Int, pname: Int, params: IntBuffer): Unit    = GL30C.glGetQueryiv(target, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])
  override def glGetQueryObjectuiv(id: Int, pname: Int, params: IntBuffer): Unit    = GL30C.glGetQueryObjectuiv(id, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])

  // ─── Buffer mapping ───────────────────────────────────────────────────────

  override def glUnmapBuffer(target: Int): Boolean = fromGlBool(GL30C.glUnmapBuffer(target))

  override def glGetBufferPointerv(target: Int, pname: Int): Buffer =
    throw new UnsupportedOperationException("glGetBufferPointerv not implemented")

  override def glMapBufferRange(target: Int, offset: Int, length: Int, access: Int): Buffer = {
    val ptr = GL30C.glMapBufferRange(target, offset, length, access)
    if (ptr == null) null.asInstanceOf[Buffer] // @nowarn - GL interop boundary
    else {
      // Wrap native pointer as a ByteBuffer — on Scala Native, this needs special handling
      // For now, create a direct buffer and note this may need refinement
      val buf = ByteBuffer.allocateDirect(length).order(ByteOrder.nativeOrder())
      // TODO: The buffer should point to ptr directly; for now returns fresh buffer
      buf
    }
  }

  override def glFlushMappedBufferRange(target: Int, offset: Int, length: Int): Unit =
    GL30C.glFlushMappedBufferRange(target, offset, length)

  override def glDrawBuffers(n: Int, bufs: IntBuffer): Unit = GL30C.glDrawBuffers(n, bufPtr(bufs).asInstanceOf[Ptr[CInt]])

  // ─── Non-square uniform matrices ──────────────────────────────────────────

  override def glUniformMatrix2x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GL30C.glUniformMatrix2x3fv(location, count, glBool(transpose), bufPtr(value).asInstanceOf[Ptr[CFloat]])

  override def glUniformMatrix3x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GL30C.glUniformMatrix3x2fv(location, count, glBool(transpose), bufPtr(value).asInstanceOf[Ptr[CFloat]])

  override def glUniformMatrix2x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GL30C.glUniformMatrix2x4fv(location, count, glBool(transpose), bufPtr(value).asInstanceOf[Ptr[CFloat]])

  override def glUniformMatrix4x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GL30C.glUniformMatrix4x2fv(location, count, glBool(transpose), bufPtr(value).asInstanceOf[Ptr[CFloat]])

  override def glUniformMatrix3x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GL30C.glUniformMatrix3x4fv(location, count, glBool(transpose), bufPtr(value).asInstanceOf[Ptr[CFloat]])

  override def glUniformMatrix4x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GL30C.glUniformMatrix4x3fv(location, count, glBool(transpose), bufPtr(value).asInstanceOf[Ptr[CFloat]])

  // ─── Framebuffer blit / multisample / layer ───────────────────────────────

  override def glBlitFramebuffer(srcX0: Int, srcY0: Int, srcX1: Int, srcY1: Int, dstX0: Int, dstY0: Int, dstX1: Int, dstY1: Int, mask: Int, filter: Int): Unit =
    GL30C.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter)

  override def glRenderbufferStorageMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int): Unit =
    GL30C.glRenderbufferStorageMultisample(target, samples, internalformat, width, height)

  override def glFramebufferTextureLayer(target: Int, attachment: Int, texture: Int, level: Int, layer: Int): Unit =
    GL30C.glFramebufferTextureLayer(target, attachment, texture, level, layer)

  // ─── VAOs ─────────────────────────────────────────────────────────────────

  override def glGenVertexArrays(n: Int, arrays: IntBuffer): Unit = GL30C.glGenVertexArrays(n, bufPtr(arrays).asInstanceOf[Ptr[CInt]])

  override def glGenVertexArrays(n: Int, arrays: Array[Int], offset: Int): Unit = {
    val buf = allocDirectInt(n); glGenVertexArrays(n, buf)
    var i   = 0; while (i < n) { arrays(offset + i) = buf.get(i); i += 1 }
  }

  override def glDeleteVertexArrays(n: Int, arrays: IntBuffer): Unit = GL30C.glDeleteVertexArrays(n, bufPtr(arrays).asInstanceOf[Ptr[CInt]])

  override def glDeleteVertexArrays(n: Int, arrays: Array[Int], offset: Int): Unit = {
    val buf = allocDirectInt(n)
    var i   = 0; while (i < n) { buf.put(i, arrays(offset + i)); i += 1 }
    glDeleteVertexArrays(n, buf)
  }

  override def glBindVertexArray(array: Int): Unit    = GL30C.glBindVertexArray(array)
  override def glIsVertexArray(array:   Int): Boolean = fromGlBool(GL30C.glIsVertexArray(array))

  // ─── Transform feedback ───────────────────────────────────────────────────

  override def glBeginTransformFeedback(primitiveMode: Int):                                                  Unit = GL30C.glBeginTransformFeedback(primitiveMode)
  override def glEndTransformFeedback():                                                                      Unit = GL30C.glEndTransformFeedback()
  override def glBindBufferRange(target:               Int, index: Int, buffer: Int, offset: Int, size: Int): Unit = GL30C.glBindBufferRange(target, index, buffer, offset, size)
  override def glBindBufferBase(target:                Int, index: Int, buffer: Int):                         Unit = GL30C.glBindBufferBase(target, index, buffer)

  override def glTransformFeedbackVaryings(program: Int, varyings: Array[String], bufferMode: Int): Unit = {
    val zone = Zone.open()
    try {
      val ptrs = stackalloc[CString](varyings.length)
      var i    = 0
      while (i < varyings.length) { ptrs(i) = toCString(varyings(i))(using zone); i += 1 }
      GL30C.glTransformFeedbackVaryings(program, varyings.length, ptrs, bufferMode)
    } finally zone.close()
  }

  override def glBindTransformFeedback(target: Int, id: Int): Unit = GL30C.glBindTransformFeedback(target, id)

  override def glGenTransformFeedbacks(n: Int, ids: IntBuffer): Unit = GL30C.glGenTransformFeedbacks(n, bufPtr(ids).asInstanceOf[Ptr[CInt]])

  override def glGenTransformFeedbacks(n: Int, ids: Array[Int], offset: Int): Unit = {
    val buf = allocDirectInt(n); glGenTransformFeedbacks(n, buf)
    var i   = 0; while (i < n) { ids(offset + i) = buf.get(i); i += 1 }
  }

  override def glDeleteTransformFeedbacks(n: Int, ids: IntBuffer): Unit = GL30C.glDeleteTransformFeedbacks(n, bufPtr(ids).asInstanceOf[Ptr[CInt]])

  override def glDeleteTransformFeedbacks(n: Int, ids: Array[Int], offset: Int): Unit = {
    val buf = allocDirectInt(n)
    var i   = 0; while (i < n) { buf.put(i, ids(offset + i)); i += 1 }
    glDeleteTransformFeedbacks(n, buf)
  }

  override def glIsTransformFeedback(id: Int): Boolean = fromGlBool(GL30C.glIsTransformFeedback(id))
  override def glPauseTransformFeedback():     Unit    = GL30C.glPauseTransformFeedback()
  override def glResumeTransformFeedback():    Unit    = GL30C.glResumeTransformFeedback()

  // ─── Vertex attrib integer ────────────────────────────────────────────────

  override def glVertexAttribIPointer(index: Int, size: Int, `type`: Int, stride: Int, offset: Int): Unit =
    GL30C.glVertexAttribIPointer(index, size, `type`, stride, offsetPtr(offset))

  override def glGetVertexAttribIiv(index:  Int, pname:   Int, params: IntBuffer):           Unit = GL30C.glGetVertexAttribIiv(index, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])
  override def glGetVertexAttribIuiv(index: Int, pname:   Int, params: IntBuffer):           Unit = GL30C.glGetVertexAttribIuiv(index, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])
  override def glVertexAttribI4i(index:     Int, x:       Int, y:      Int, z: Int, w: Int): Unit = GL30C.glVertexAttribI4i(index, x, y, z, w)
  override def glVertexAttribI4ui(index:    Int, x:       Int, y:      Int, z: Int, w: Int): Unit = GL30C.glVertexAttribI4ui(index, x, y, z, w)
  override def glVertexAttribDivisor(index: Int, divisor: Int):                              Unit = GL30C.glVertexAttribDivisor(index, divisor)

  // ─── Unsigned int uniforms ────────────────────────────────────────────────

  override def glGetUniformuiv(program: Int, location: Int, params: IntBuffer): Unit = GL30C.glGetUniformuiv(program, location, bufPtr(params).asInstanceOf[Ptr[CInt]])

  override def glGetFragDataLocation(program: Int, name: String): Int = {
    val zone = Zone.open()
    try GL30C.glGetFragDataLocation(program, toCString(name)(using zone))
    finally zone.close()
  }

  override def glUniform1uiv(location: Int, count: Int, value: IntBuffer): Unit = GL30C.glUniform1uiv(location, count, bufPtr(value).asInstanceOf[Ptr[CInt]])
  override def glUniform3uiv(location: Int, count: Int, value: IntBuffer): Unit = GL30C.glUniform3uiv(location, count, bufPtr(value).asInstanceOf[Ptr[CInt]])
  override def glUniform4uiv(location: Int, count: Int, value: IntBuffer): Unit = GL30C.glUniform4uiv(location, count, bufPtr(value).asInstanceOf[Ptr[CInt]])

  // ─── Clear buffer ─────────────────────────────────────────────────────────

  override def glClearBufferiv(buffer:  Int, drawbuffer: Int, value: IntBuffer):           Unit = GL30C.glClearBufferiv(buffer, drawbuffer, bufPtr(value).asInstanceOf[Ptr[CInt]])
  override def glClearBufferuiv(buffer: Int, drawbuffer: Int, value: IntBuffer):           Unit = GL30C.glClearBufferuiv(buffer, drawbuffer, bufPtr(value).asInstanceOf[Ptr[CInt]])
  override def glClearBufferfv(buffer:  Int, drawbuffer: Int, value: FloatBuffer):         Unit = GL30C.glClearBufferfv(buffer, drawbuffer, bufPtr(value).asInstanceOf[Ptr[CFloat]])
  override def glClearBufferfi(buffer:  Int, drawbuffer: Int, depth: Float, stencil: Int): Unit = GL30C.glClearBufferfi(buffer, drawbuffer, depth, stencil)

  // ─── String query ─────────────────────────────────────────────────────────

  override def glGetStringi(name: Int, index: Int): String = {
    val ptr = GL30C.glGetStringi(name, index)
    if (ptr == null) null // @nowarn - GL interop boundary
    else fromCString(ptr)
  }

  // ─── Buffer copy ──────────────────────────────────────────────────────────

  override def glCopyBufferSubData(readTarget: Int, writeTarget: Int, readOffset: Int, writeOffset: Int, size: Int): Unit =
    GL30C.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size)

  // ─── Uniform blocks ───────────────────────────────────────────────────────

  override def glGetUniformIndices(program: Int, uniformNames: Array[String], uniformIndices: IntBuffer): Unit = {
    val zone = Zone.open()
    try {
      val ptrs = stackalloc[CString](uniformNames.length)
      var i    = 0
      while (i < uniformNames.length) { ptrs(i) = toCString(uniformNames(i))(using zone); i += 1 }
      GL30C.glGetUniformIndices(program, uniformNames.length, ptrs, bufPtr(uniformIndices).asInstanceOf[Ptr[CInt]])
    } finally zone.close()
  }

  override def glGetActiveUniformsiv(program: Int, uniformCount: Int, uniformIndices: IntBuffer, pname: Int, params: IntBuffer): Unit =
    GL30C.glGetActiveUniformsiv(program, uniformCount, bufPtr(uniformIndices).asInstanceOf[Ptr[CInt]], pname, bufPtr(params).asInstanceOf[Ptr[CInt]])

  override def glGetUniformBlockIndex(program: Int, uniformBlockName: String): Int = {
    val zone = Zone.open()
    try GL30C.glGetUniformBlockIndex(program, toCString(uniformBlockName)(using zone))
    finally zone.close()
  }

  override def glGetActiveUniformBlockiv(program: Int, uniformBlockIndex: Int, pname: Int, params: IntBuffer): Unit =
    GL30C.glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])

  override def glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int, length: Buffer, uniformBlockName: Buffer): Unit =
    GL30C.glGetActiveUniformBlockName(program, uniformBlockIndex, 1024, bufPtr(length).asInstanceOf[Ptr[CInt]], bufPtr(uniformBlockName).asInstanceOf[CString])

  override def glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int): String = {
    val nameBuf   = stackalloc[Byte](1024)
    val lengthBuf = stackalloc[CInt]()
    GL30C.glGetActiveUniformBlockName(program, uniformBlockIndex, 1024, lengthBuf, nameBuf)
    fromCString(nameBuf)
  }

  override def glUniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int): Unit =
    GL30C.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding)

  // ─── Instancing ───────────────────────────────────────────────────────────

  override def glDrawArraysInstanced(mode: Int, first: Int, count: Int, instanceCount: Int): Unit =
    GL30C.glDrawArraysInstanced(mode, first, count, instanceCount)

  override def glDrawElementsInstanced(mode: Int, count: Int, `type`: Int, indicesOffset: Int, instanceCount: Int): Unit =
    GL30C.glDrawElementsInstanced(mode, count, `type`, offsetPtr(indicesOffset), instanceCount)

  // ─── 64-bit queries ───────────────────────────────────────────────────────

  override def glGetInteger64v(pname:           Int, params: LongBuffer):              Unit = GL30C.glGetInteger64v(pname, bufPtr(params).asInstanceOf[Ptr[CLong]])
  override def glGetBufferParameteri64v(target: Int, pname:  Int, params: LongBuffer): Unit = GL30C.glGetBufferParameteri64v(target, pname, bufPtr(params).asInstanceOf[Ptr[CLong]])

  // ─── Samplers ─────────────────────────────────────────────────────────────

  override def glGenSamplers(count: Int, samplers: IntBuffer): Unit = GL30C.glGenSamplers(count, bufPtr(samplers).asInstanceOf[Ptr[CInt]])

  override def glGenSamplers(count: Int, samplers: Array[Int], offset: Int): Unit = {
    val buf = allocDirectInt(count); glGenSamplers(count, buf)
    var i   = 0; while (i < count) { samplers(offset + i) = buf.get(i); i += 1 }
  }

  override def glDeleteSamplers(count: Int, samplers: IntBuffer): Unit = GL30C.glDeleteSamplers(count, bufPtr(samplers).asInstanceOf[Ptr[CInt]])

  override def glDeleteSamplers(count: Int, samplers: Array[Int], offset: Int): Unit = {
    val buf = allocDirectInt(count)
    var i   = 0; while (i < count) { buf.put(i, samplers(offset + i)); i += 1 }
    glDeleteSamplers(count, buf)
  }

  override def glIsSampler(sampler:             Int):                                    Boolean = fromGlBool(GL30C.glIsSampler(sampler))
  override def glBindSampler(unit:              Int, sampler: Int):                      Unit    = GL30C.glBindSampler(unit, sampler)
  override def glSamplerParameteri(sampler:     Int, pname:   Int, param:  Int):         Unit    = GL30C.glSamplerParameteri(sampler, pname, param)
  override def glSamplerParameteriv(sampler:    Int, pname:   Int, param:  IntBuffer):   Unit    = GL30C.glSamplerParameteriv(sampler, pname, bufPtr(param).asInstanceOf[Ptr[CInt]])
  override def glSamplerParameterf(sampler:     Int, pname:   Int, param:  Float):       Unit    = GL30C.glSamplerParameterf(sampler, pname, param)
  override def glSamplerParameterfv(sampler:    Int, pname:   Int, param:  FloatBuffer): Unit    = GL30C.glSamplerParameterfv(sampler, pname, bufPtr(param).asInstanceOf[Ptr[CFloat]])
  override def glGetSamplerParameteriv(sampler: Int, pname:   Int, params: IntBuffer):   Unit    = GL30C.glGetSamplerParameteriv(sampler, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])
  override def glGetSamplerParameterfv(sampler: Int, pname:   Int, params: FloatBuffer): Unit    = GL30C.glGetSamplerParameterfv(sampler, pname, bufPtr(params).asInstanceOf[Ptr[CFloat]])

  // ─── Program parameters / framebuffer invalidation ────────────────────────

  override def glProgramParameteri(program: Int, pname: Int, value: Int): Unit = GL30C.glProgramParameteri(program, pname, value)

  override def glInvalidateFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer): Unit =
    GL30C.glInvalidateFramebuffer(target, numAttachments, bufPtr(attachments).asInstanceOf[Ptr[CInt]])

  override def glInvalidateSubFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer, x: Int, y: Int, width: Int, height: Int): Unit =
    GL30C.glInvalidateSubFramebuffer(target, numAttachments, bufPtr(attachments).asInstanceOf[Ptr[CInt]], x, y, width, height)
}
