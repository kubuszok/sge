/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (adapts GL30Ops plain-Int mirror to sge.graphics.GL30 opaque types)
 *   Convention: JVM-only; extends AndroidGL20Adapter, adds GL30 methods
 *   Convention: Opaque types erase to Int at runtime — adapter casts via .toInt / apply(raw)
 *   Idiom: split packages
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import java.nio.{ Buffer, FloatBuffer, IntBuffer, LongBuffer }
import sge.platform.android.GL30Ops

/** Adapts [[GL30Ops]] (JDK-only, plain Int params) to [[GL30]] (opaque type params).
  *
  * Extends [[AndroidGL20Adapter]] — all GL ES 2.0 methods are inherited.
  *
  * @param gl30Ops
  *   the underlying GL ES 3.0 operations (from Android platform implementation)
  */
class AndroidGL30Adapter(gl30Ops: GL30Ops) extends AndroidGL20Adapter(gl30Ops) with GL30 {

  override def glReadBuffer(mode: Int): Unit = gl30Ops.glReadBuffer(mode)

  override def glDrawRangeElements(mode: PrimitiveMode, start: Int, end: Int, count: Int, `type`: DataType, indices: Buffer): Unit =
    gl30Ops.glDrawRangeElements(mode.toInt, start, end, count, `type`.toInt, indices)

  override def glDrawRangeElements(mode: PrimitiveMode, start: Int, end: Int, count: Int, `type`: DataType, offset: Int): Unit =
    gl30Ops.glDrawRangeElements(mode.toInt, start, end, count, `type`.toInt, offset)

  override def glTexImage2D(target: TextureTarget, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: PixelFormat, `type`: DataType, offset: Int): Unit =
    gl30Ops.glTexImage2D(target.toInt, level, internalformat, width, height, border, format.toInt, `type`.toInt, offset)

  override def glTexImage3D(target: TextureTarget, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: PixelFormat, `type`: DataType, pixels: Buffer): Unit =
    gl30Ops.glTexImage3D(target.toInt, level, internalformat, width, height, depth, border, format.toInt, `type`.toInt, pixels)

  override def glTexImage3D(target: TextureTarget, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: PixelFormat, `type`: DataType, offset: Int): Unit =
    gl30Ops.glTexImage3D(target.toInt, level, internalformat, width, height, depth, border, format.toInt, `type`.toInt, offset)

  override def glTexSubImage2D(target: TextureTarget, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: PixelFormat, `type`: DataType, offset: Int): Unit =
    gl30Ops.glTexSubImage2D(target.toInt, level, xoffset, yoffset, width, height, format.toInt, `type`.toInt, offset)

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
    gl30Ops.glTexSubImage3D(target.toInt, level, xoffset, yoffset, zoffset, width, height, depth, format.toInt, `type`.toInt, pixels)

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
    gl30Ops.glTexSubImage3D(target.toInt, level, xoffset, yoffset, zoffset, width, height, depth, format.toInt, `type`.toInt, offset)

  override def glCopyTexSubImage3D(target: TextureTarget, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit =
    gl30Ops.glCopyTexSubImage3D(target.toInt, level, xoffset, yoffset, zoffset, x, y, width, height)

  override def glGenQueries(n: Int, ids: Array[Int], offset: Int): Unit = gl30Ops.glGenQueries(n, ids, offset)

  override def glGenQueries(n: Int, ids: IntBuffer): Unit = gl30Ops.glGenQueries(n, ids)

  override def glDeleteQueries(n: Int, ids: Array[Int], offset: Int): Unit = gl30Ops.glDeleteQueries(n, ids, offset)

  override def glDeleteQueries(n: Int, ids: IntBuffer): Unit = gl30Ops.glDeleteQueries(n, ids)

  override def glIsQuery(id: Int): Boolean = gl30Ops.glIsQuery(id)

  override def glBeginQuery(target: Int, id: Int): Unit = gl30Ops.glBeginQuery(target, id)

  override def glEndQuery(target: Int): Unit = gl30Ops.glEndQuery(target)

  override def glGetQueryiv(target: Int, pname: Int, params: IntBuffer): Unit =
    gl30Ops.glGetQueryiv(target, pname, params)

  override def glGetQueryObjectuiv(id: Int, pname: Int, params: IntBuffer): Unit =
    gl30Ops.glGetQueryObjectuiv(id, pname, params)

  override def glUnmapBuffer(target: BufferTarget): Boolean = gl30Ops.glUnmapBuffer(target.toInt)

  override def glGetBufferPointerv(target: BufferTarget, pname: Int): Buffer =
    gl30Ops.glGetBufferPointerv(target.toInt, pname)

  override def glDrawBuffers(n: Int, bufs: IntBuffer): Unit = gl30Ops.glDrawBuffers(n, bufs)

  override def glUniformMatrix2x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    gl30Ops.glUniformMatrix2x3fv(location, count, transpose, value)

  override def glUniformMatrix3x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    gl30Ops.glUniformMatrix3x2fv(location, count, transpose, value)

  override def glUniformMatrix2x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    gl30Ops.glUniformMatrix2x4fv(location, count, transpose, value)

  override def glUniformMatrix4x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    gl30Ops.glUniformMatrix4x2fv(location, count, transpose, value)

  override def glUniformMatrix3x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    gl30Ops.glUniformMatrix3x4fv(location, count, transpose, value)

  override def glUniformMatrix4x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    gl30Ops.glUniformMatrix4x3fv(location, count, transpose, value)

  override def glBlitFramebuffer(srcX0: Int, srcY0: Int, srcX1: Int, srcY1: Int, dstX0: Int, dstY0: Int, dstX1: Int, dstY1: Int, mask: ClearMask, filter: Int): Unit =
    gl30Ops.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask.toInt, filter)

  override def glRenderbufferStorageMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int): Unit =
    gl30Ops.glRenderbufferStorageMultisample(target, samples, internalformat, width, height)

  override def glFramebufferTextureLayer(target: Int, attachment: Int, texture: Int, level: Int, layer: Int): Unit =
    gl30Ops.glFramebufferTextureLayer(target, attachment, texture, level, layer)

  override def glMapBufferRange(target: BufferTarget, offset: Int, length: Int, access: Int): Buffer =
    gl30Ops.glMapBufferRange(target.toInt, offset, length, access)

  override def glFlushMappedBufferRange(target: BufferTarget, offset: Int, length: Int): Unit =
    gl30Ops.glFlushMappedBufferRange(target.toInt, offset, length)

  override def glBindVertexArray(array: Int): Unit = gl30Ops.glBindVertexArray(array)

  override def glDeleteVertexArrays(n: Int, arrays: Array[Int], offset: Int): Unit =
    gl30Ops.glDeleteVertexArrays(n, arrays, offset)

  override def glDeleteVertexArrays(n: Int, arrays: IntBuffer): Unit = gl30Ops.glDeleteVertexArrays(n, arrays)

  override def glGenVertexArrays(n: Int, arrays: Array[Int], offset: Int): Unit =
    gl30Ops.glGenVertexArrays(n, arrays, offset)

  override def glGenVertexArrays(n: Int, arrays: IntBuffer): Unit = gl30Ops.glGenVertexArrays(n, arrays)

  override def glIsVertexArray(array: Int): Boolean = gl30Ops.glIsVertexArray(array)

  override def glBeginTransformFeedback(primitiveMode: PrimitiveMode): Unit =
    gl30Ops.glBeginTransformFeedback(primitiveMode.toInt)

  override def glEndTransformFeedback(): Unit = gl30Ops.glEndTransformFeedback()

  override def glBindBufferRange(target: BufferTarget, index: Int, buffer: Int, offset: Int, size: Int): Unit =
    gl30Ops.glBindBufferRange(target.toInt, index, buffer, offset, size)

  override def glBindBufferBase(target: BufferTarget, index: Int, buffer: Int): Unit =
    gl30Ops.glBindBufferBase(target.toInt, index, buffer)

  override def glTransformFeedbackVaryings(program: Int, varyings: Array[String], bufferMode: Int): Unit =
    gl30Ops.glTransformFeedbackVaryings(program, varyings, bufferMode)

  override def glVertexAttribIPointer(index: Int, size: Int, `type`: DataType, stride: Int, offset: Int): Unit =
    gl30Ops.glVertexAttribIPointer(index, size, `type`.toInt, stride, offset)

  override def glGetVertexAttribIiv(index: Int, pname: Int, params: IntBuffer): Unit =
    gl30Ops.glGetVertexAttribIiv(index, pname, params)

  override def glGetVertexAttribIuiv(index: Int, pname: Int, params: IntBuffer): Unit =
    gl30Ops.glGetVertexAttribIuiv(index, pname, params)

  override def glVertexAttribI4i(index: Int, x: Int, y: Int, z: Int, w: Int): Unit =
    gl30Ops.glVertexAttribI4i(index, x, y, z, w)

  override def glVertexAttribI4ui(index: Int, x: Int, y: Int, z: Int, w: Int): Unit =
    gl30Ops.glVertexAttribI4ui(index, x, y, z, w)

  override def glGetUniformuiv(program: Int, location: Int, params: IntBuffer): Unit =
    gl30Ops.glGetUniformuiv(program, location, params)

  override def glGetFragDataLocation(program: Int, name: String): Int = gl30Ops.glGetFragDataLocation(program, name)

  override def glUniform1uiv(location: Int, count: Int, value: IntBuffer): Unit =
    gl30Ops.glUniform1uiv(location, count, value)

  override def glUniform3uiv(location: Int, count: Int, value: IntBuffer): Unit =
    gl30Ops.glUniform3uiv(location, count, value)

  override def glUniform4uiv(location: Int, count: Int, value: IntBuffer): Unit =
    gl30Ops.glUniform4uiv(location, count, value)

  override def glClearBufferiv(buffer: Int, drawbuffer: Int, value: IntBuffer): Unit =
    gl30Ops.glClearBufferiv(buffer, drawbuffer, value)

  override def glClearBufferuiv(buffer: Int, drawbuffer: Int, value: IntBuffer): Unit =
    gl30Ops.glClearBufferuiv(buffer, drawbuffer, value)

  override def glClearBufferfv(buffer: Int, drawbuffer: Int, value: FloatBuffer): Unit =
    gl30Ops.glClearBufferfv(buffer, drawbuffer, value)

  override def glClearBufferfi(buffer: Int, drawbuffer: Int, depth: Float, stencil: Int): Unit =
    gl30Ops.glClearBufferfi(buffer, drawbuffer, depth, stencil)

  override def glGetStringi(name: Int, index: Int): String = gl30Ops.glGetStringi(name, index)

  override def glCopyBufferSubData(readTarget: BufferTarget, writeTarget: BufferTarget, readOffset: Int, writeOffset: Int, size: Int): Unit =
    gl30Ops.glCopyBufferSubData(readTarget.toInt, writeTarget.toInt, readOffset, writeOffset, size)

  override def glGetUniformIndices(program: Int, uniformNames: Array[String], uniformIndices: IntBuffer): Unit =
    gl30Ops.glGetUniformIndices(program, uniformNames, uniformIndices)

  override def glGetActiveUniformsiv(program: Int, uniformCount: Int, uniformIndices: IntBuffer, pname: Int, params: IntBuffer): Unit =
    gl30Ops.glGetActiveUniformsiv(program, uniformCount, uniformIndices, pname, params)

  override def glGetUniformBlockIndex(program: Int, uniformBlockName: String): Int =
    gl30Ops.glGetUniformBlockIndex(program, uniformBlockName)

  override def glGetActiveUniformBlockiv(program: Int, uniformBlockIndex: Int, pname: Int, params: IntBuffer): Unit =
    gl30Ops.glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, params)

  override def glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int, length: Buffer, uniformBlockName: Buffer): Unit =
    gl30Ops.glGetActiveUniformBlockName(program, uniformBlockIndex, length, uniformBlockName)

  override def glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int): String =
    gl30Ops.glGetActiveUniformBlockName(program, uniformBlockIndex)

  override def glUniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int): Unit =
    gl30Ops.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding)

  override def glDrawArraysInstanced(mode: PrimitiveMode, first: Int, count: Int, instanceCount: Int): Unit =
    gl30Ops.glDrawArraysInstanced(mode.toInt, first, count, instanceCount)

  override def glDrawElementsInstanced(mode: PrimitiveMode, count: Int, `type`: DataType, indicesOffset: Int, instanceCount: Int): Unit =
    gl30Ops.glDrawElementsInstanced(mode.toInt, count, `type`.toInt, indicesOffset, instanceCount)

  override def glGetInteger64v(pname: Int, params: LongBuffer): Unit = gl30Ops.glGetInteger64v(pname, params)

  override def glGetBufferParameteri64v(target: BufferTarget, pname: Int, params: LongBuffer): Unit =
    gl30Ops.glGetBufferParameteri64v(target.toInt, pname, params)

  override def glGenSamplers(count: Int, samplers: Array[Int], offset: Int): Unit =
    gl30Ops.glGenSamplers(count, samplers, offset)

  override def glGenSamplers(count: Int, samplers: IntBuffer): Unit = gl30Ops.glGenSamplers(count, samplers)

  override def glDeleteSamplers(count: Int, samplers: Array[Int], offset: Int): Unit =
    gl30Ops.glDeleteSamplers(count, samplers, offset)

  override def glDeleteSamplers(count: Int, samplers: IntBuffer): Unit = gl30Ops.glDeleteSamplers(count, samplers)

  override def glIsSampler(sampler: Int): Boolean = gl30Ops.glIsSampler(sampler)

  override def glBindSampler(unit: Int, sampler: Int): Unit = gl30Ops.glBindSampler(unit, sampler)

  override def glSamplerParameteri(sampler: Int, pname: Int, param: Int): Unit =
    gl30Ops.glSamplerParameteri(sampler, pname, param)

  override def glSamplerParameteriv(sampler: Int, pname: Int, param: IntBuffer): Unit =
    gl30Ops.glSamplerParameteriv(sampler, pname, param)

  override def glSamplerParameterf(sampler: Int, pname: Int, param: Float): Unit =
    gl30Ops.glSamplerParameterf(sampler, pname, param)

  override def glSamplerParameterfv(sampler: Int, pname: Int, param: FloatBuffer): Unit =
    gl30Ops.glSamplerParameterfv(sampler, pname, param)

  override def glGetSamplerParameteriv(sampler: Int, pname: Int, params: IntBuffer): Unit =
    gl30Ops.glGetSamplerParameteriv(sampler, pname, params)

  override def glGetSamplerParameterfv(sampler: Int, pname: Int, params: FloatBuffer): Unit =
    gl30Ops.glGetSamplerParameterfv(sampler, pname, params)

  override def glVertexAttribDivisor(index: Int, divisor: Int): Unit =
    gl30Ops.glVertexAttribDivisor(index, divisor)

  override def glBindTransformFeedback(target: Int, id: Int): Unit = gl30Ops.glBindTransformFeedback(target, id)

  override def glDeleteTransformFeedbacks(n: Int, ids: Array[Int], offset: Int): Unit =
    gl30Ops.glDeleteTransformFeedbacks(n, ids, offset)

  override def glDeleteTransformFeedbacks(n: Int, ids: IntBuffer): Unit =
    gl30Ops.glDeleteTransformFeedbacks(n, ids)

  override def glGenTransformFeedbacks(n: Int, ids: Array[Int], offset: Int): Unit =
    gl30Ops.glGenTransformFeedbacks(n, ids, offset)

  override def glGenTransformFeedbacks(n: Int, ids: IntBuffer): Unit = gl30Ops.glGenTransformFeedbacks(n, ids)

  override def glIsTransformFeedback(id: Int): Boolean = gl30Ops.glIsTransformFeedback(id)

  override def glPauseTransformFeedback(): Unit = gl30Ops.glPauseTransformFeedback()

  override def glResumeTransformFeedback(): Unit = gl30Ops.glResumeTransformFeedback()

  override def glProgramParameteri(program: Int, pname: Int, value: Int): Unit =
    gl30Ops.glProgramParameteri(program, pname, value)

  override def glInvalidateFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer): Unit =
    gl30Ops.glInvalidateFramebuffer(target, numAttachments, attachments)

  override def glInvalidateSubFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer, x: Int, y: Int, width: Int, height: Int): Unit =
    gl30Ops.glInvalidateSubFramebuffer(target, numAttachments, attachments, x, y, width, height)

  override def glVertexAttribPointer(indx: Int, size: Int, `type`: DataType, normalized: Boolean, stride: Int, ptr: Buffer): Unit =
    ops.glVertexAttribPointer(indx, size, `type`.toInt, normalized, stride, ptr)
}
