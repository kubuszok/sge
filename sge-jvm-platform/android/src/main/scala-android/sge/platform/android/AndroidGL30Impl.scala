// SGE — Android GL ES 3.0 implementation
//
// Extends AndroidGL20Impl with GLES30 static method delegation.
//
// Migration notes:
//   Source:  com.badlogic.gdx.backends.android.AndroidGL30
//   Renames: AndroidGL30 → AndroidGL30Impl
//   Convention: ops interface pattern; _root_.android.* imports
//   Audited: 2026-03-08

package sge
package platform
package android

import _root_.android.opengl.{ GLES20, GLES30 }
import java.nio.{ Buffer, FloatBuffer, IntBuffer, LongBuffer }

class AndroidGL30Impl extends AndroidGL20Impl with GL30Ops {

  override def glReadBuffer(mode: Int): Unit = GLES30.glReadBuffer(mode)

  override def glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, `type`: Int, indices: Buffer): Unit =
    GLES30.glDrawRangeElements(mode, start, end, count, `type`, indices)

  override def glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, `type`: Int, offset: Int): Unit =
    GLES30.glDrawRangeElements(mode, start, end, count, `type`, offset)

  override def glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, `type`: Int, offset: Int): Unit = {
    if (offset != 0) throw new RuntimeException("non zero offset is not supported")
    GLES20.glTexImage2D(target, level, internalformat, width, height, border, format, `type`, null)
  }

  override def glTexImage3D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: Int, `type`: Int, pixels: Buffer): Unit =
    if (pixels == null)
      GLES30.glTexImage3D(target, level, internalformat, width, height, depth, border, format, `type`, 0)
    else
      GLES30.glTexImage3D(target, level, internalformat, width, height, depth, border, format, `type`, pixels)

  override def glTexImage3D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: Int, `type`: Int, offset: Int): Unit =
    GLES30.glTexImage3D(target, level, internalformat, width, height, depth, border, format, `type`, offset)

  override def glTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, `type`: Int, offset: Int): Unit = {
    if (offset != 0) throw new RuntimeException("non zero offset is not supported")
    GLES20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, `type`, null)
  }

  override def glTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int, format: Int, `type`: Int, pixels: Buffer): Unit =
    GLES30.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, `type`, pixels)

  override def glTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int, format: Int, `type`: Int, offset: Int): Unit =
    GLES30.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, `type`, offset)

  override def glCopyTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit =
    GLES30.glCopyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height)

  // ── Queries ───────────────────────────────────────────────────────────

  override def glGenQueries(n: Int, ids: Array[Int], offset: Int): Unit = GLES30.glGenQueries(n, ids, offset)

  override def glGenQueries(n: Int, ids: IntBuffer): Unit = GLES30.glGenQueries(n, ids)

  override def glDeleteQueries(n: Int, ids: Array[Int], offset: Int): Unit = GLES30.glDeleteQueries(n, ids, offset)

  override def glDeleteQueries(n: Int, ids: IntBuffer): Unit = GLES30.glDeleteQueries(n, ids)

  override def glIsQuery(id: Int): Boolean = GLES30.glIsQuery(id)

  override def glBeginQuery(target: Int, id: Int): Unit = GLES30.glBeginQuery(target, id)

  override def glEndQuery(target: Int): Unit = GLES30.glEndQuery(target)

  override def glGetQueryiv(target: Int, pname: Int, params: IntBuffer): Unit =
    GLES30.glGetQueryiv(target, pname, params)

  override def glGetQueryObjectuiv(id: Int, pname: Int, params: IntBuffer): Unit =
    GLES30.glGetQueryObjectuiv(id, pname, params)

  // ── Buffer mapping ────────────────────────────────────────────────────

  override def glUnmapBuffer(target: Int): Boolean = GLES30.glUnmapBuffer(target)

  override def glGetBufferPointerv(target: Int, pname: Int): Buffer = GLES30.glGetBufferPointerv(target, pname)

  override def glDrawBuffers(n: Int, bufs: IntBuffer): Unit = GLES30.glDrawBuffers(n, bufs)

  // ── Uniform matrices (non-square) ─────────────────────────────────────

  override def glUniformMatrix2x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GLES30.glUniformMatrix2x3fv(location, count, transpose, value)

  override def glUniformMatrix3x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GLES30.glUniformMatrix3x2fv(location, count, transpose, value)

  override def glUniformMatrix2x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GLES30.glUniformMatrix2x4fv(location, count, transpose, value)

  override def glUniformMatrix4x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GLES30.glUniformMatrix4x2fv(location, count, transpose, value)

  override def glUniformMatrix3x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GLES30.glUniformMatrix3x4fv(location, count, transpose, value)

  override def glUniformMatrix4x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GLES30.glUniformMatrix4x3fv(location, count, transpose, value)

  // ── Framebuffer / renderbuffer ────────────────────────────────────────

  override def glBlitFramebuffer(srcX0: Int, srcY0: Int, srcX1: Int, srcY1: Int, dstX0: Int, dstY0: Int, dstX1: Int, dstY1: Int, mask: Int, filter: Int): Unit =
    GLES30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter)

  override def glRenderbufferStorageMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int): Unit =
    GLES30.glRenderbufferStorageMultisample(target, samples, internalformat, width, height)

  override def glFramebufferTextureLayer(target: Int, attachment: Int, texture: Int, level: Int, layer: Int): Unit =
    GLES30.glFramebufferTextureLayer(target, attachment, texture, level, layer)

  override def glMapBufferRange(target: Int, offset: Int, length: Int, access: Int): Buffer =
    GLES30.glMapBufferRange(target, offset, length, access)

  override def glFlushMappedBufferRange(target: Int, offset: Int, length: Int): Unit =
    GLES30.glFlushMappedBufferRange(target, offset, length)

  // ── Vertex array objects ──────────────────────────────────────────────

  override def glBindVertexArray(array: Int): Unit = GLES30.glBindVertexArray(array)

  override def glDeleteVertexArrays(n: Int, arrays: Array[Int], offset: Int): Unit =
    GLES30.glDeleteVertexArrays(n, arrays, offset)

  override def glDeleteVertexArrays(n: Int, arrays: IntBuffer): Unit = GLES30.glDeleteVertexArrays(n, arrays)

  override def glGenVertexArrays(n: Int, arrays: Array[Int], offset: Int): Unit =
    GLES30.glGenVertexArrays(n, arrays, offset)

  override def glGenVertexArrays(n: Int, arrays: IntBuffer): Unit = GLES30.glGenVertexArrays(n, arrays)

  override def glIsVertexArray(array: Int): Boolean = GLES30.glIsVertexArray(array)

  // ── Transform feedback ────────────────────────────────────────────────

  override def glBeginTransformFeedback(primitiveMode: Int): Unit = GLES30.glBeginTransformFeedback(primitiveMode)

  override def glEndTransformFeedback(): Unit = GLES30.glEndTransformFeedback()

  override def glBindBufferRange(target: Int, index: Int, buffer: Int, offset: Int, size: Int): Unit =
    GLES30.glBindBufferRange(target, index, buffer, offset, size)

  override def glBindBufferBase(target: Int, index: Int, buffer: Int): Unit =
    GLES30.glBindBufferBase(target, index, buffer)

  override def glTransformFeedbackVaryings(program: Int, varyings: Array[String], bufferMode: Int): Unit =
    GLES30.glTransformFeedbackVaryings(program, varyings, bufferMode)

  // ── Vertex attrib integer ─────────────────────────────────────────────

  override def glVertexAttribIPointer(index: Int, size: Int, `type`: Int, stride: Int, offset: Int): Unit =
    GLES30.glVertexAttribIPointer(index, size, `type`, stride, offset)

  override def glGetVertexAttribIiv(index: Int, pname: Int, params: IntBuffer): Unit =
    GLES30.glGetVertexAttribIiv(index, pname, params)

  override def glGetVertexAttribIuiv(index: Int, pname: Int, params: IntBuffer): Unit =
    GLES30.glGetVertexAttribIuiv(index, pname, params)

  override def glVertexAttribI4i(index: Int, x: Int, y: Int, z: Int, w: Int): Unit =
    GLES30.glVertexAttribI4i(index, x, y, z, w)

  override def glVertexAttribI4ui(index: Int, x: Int, y: Int, z: Int, w: Int): Unit =
    GLES30.glVertexAttribI4ui(index, x, y, z, w)

  // ── Unsigned integer uniforms ─────────────────────────────────────────

  override def glGetUniformuiv(program: Int, location: Int, params: IntBuffer): Unit =
    GLES30.glGetUniformuiv(program, location, params)

  override def glGetFragDataLocation(program: Int, name: String): Int = GLES30.glGetFragDataLocation(program, name)

  override def glUniform1uiv(location: Int, count: Int, value: IntBuffer): Unit =
    GLES30.glUniform1uiv(location, count, value)

  override def glUniform3uiv(location: Int, count: Int, value: IntBuffer): Unit =
    GLES30.glUniform3uiv(location, count, value)

  override def glUniform4uiv(location: Int, count: Int, value: IntBuffer): Unit =
    GLES30.glUniform4uiv(location, count, value)

  // ── Clear buffers ─────────────────────────────────────────────────────

  override def glClearBufferiv(buffer: Int, drawbuffer: Int, value: IntBuffer): Unit =
    GLES30.glClearBufferiv(buffer, drawbuffer, value)

  override def glClearBufferuiv(buffer: Int, drawbuffer: Int, value: IntBuffer): Unit =
    GLES30.glClearBufferuiv(buffer, drawbuffer, value)

  override def glClearBufferfv(buffer: Int, drawbuffer: Int, value: FloatBuffer): Unit =
    GLES30.glClearBufferfv(buffer, drawbuffer, value)

  override def glClearBufferfi(buffer: Int, drawbuffer: Int, depth: Float, stencil: Int): Unit =
    GLES30.glClearBufferfi(buffer, drawbuffer, depth, stencil)

  override def glGetStringi(name: Int, index: Int): String = GLES30.glGetStringi(name, index)

  // ── Buffer copy ───────────────────────────────────────────────────────

  override def glCopyBufferSubData(readTarget: Int, writeTarget: Int, readOffset: Int, writeOffset: Int, size: Int): Unit =
    GLES30.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size)

  // ── Uniform blocks ────────────────────────────────────────────────────

  override def glGetUniformIndices(program: Int, uniformNames: Array[String], uniformIndices: IntBuffer): Unit =
    GLES30.glGetUniformIndices(program, uniformNames, uniformIndices)

  override def glGetActiveUniformsiv(program: Int, uniformCount: Int, uniformIndices: IntBuffer, pname: Int, params: IntBuffer): Unit =
    GLES30.glGetActiveUniformsiv(program, uniformCount, uniformIndices, pname, params)

  override def glGetUniformBlockIndex(program: Int, uniformBlockName: String): Int =
    GLES30.glGetUniformBlockIndex(program, uniformBlockName)

  override def glGetActiveUniformBlockiv(program: Int, uniformBlockIndex: Int, pname: Int, params: IntBuffer): Unit =
    GLES30.glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, params)

  override def glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int, length: Buffer, uniformBlockName: Buffer): Unit =
    GLES30.glGetActiveUniformBlockName(program, uniformBlockIndex, length, uniformBlockName)

  override def glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int): String =
    GLES30.glGetActiveUniformBlockName(program, uniformBlockIndex)

  override def glUniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int): Unit =
    GLES30.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding)

  // ── Instanced drawing ─────────────────────────────────────────────────

  override def glDrawArraysInstanced(mode: Int, first: Int, count: Int, instanceCount: Int): Unit =
    GLES30.glDrawArraysInstanced(mode, first, count, instanceCount)

  override def glDrawElementsInstanced(mode: Int, count: Int, `type`: Int, indicesOffset: Int, instanceCount: Int): Unit =
    GLES30.glDrawElementsInstanced(mode, count, `type`, indicesOffset, instanceCount)

  // ── 64-bit queries ────────────────────────────────────────────────────

  override def glGetInteger64v(pname: Int, params: LongBuffer): Unit = GLES30.glGetInteger64v(pname, params)

  override def glGetBufferParameteri64v(target: Int, pname: Int, params: LongBuffer): Unit =
    GLES30.glGetBufferParameteri64v(target, pname, params)

  // ── Samplers ──────────────────────────────────────────────────────────

  override def glGenSamplers(count: Int, samplers: Array[Int], offset: Int): Unit =
    GLES30.glGenSamplers(count, samplers, offset)

  override def glGenSamplers(count: Int, samplers: IntBuffer): Unit = GLES30.glGenSamplers(count, samplers)

  override def glDeleteSamplers(count: Int, samplers: Array[Int], offset: Int): Unit =
    GLES30.glDeleteSamplers(count, samplers, offset)

  override def glDeleteSamplers(count: Int, samplers: IntBuffer): Unit = GLES30.glDeleteSamplers(count, samplers)

  override def glIsSampler(sampler: Int): Boolean = GLES30.glIsSampler(sampler)

  override def glBindSampler(unit: Int, sampler: Int): Unit = GLES30.glBindSampler(unit, sampler)

  override def glSamplerParameteri(sampler: Int, pname: Int, param: Int): Unit =
    GLES30.glSamplerParameteri(sampler, pname, param)

  override def glSamplerParameteriv(sampler: Int, pname: Int, param: IntBuffer): Unit =
    GLES30.glSamplerParameteriv(sampler, pname, param)

  override def glSamplerParameterf(sampler: Int, pname: Int, param: Float): Unit =
    GLES30.glSamplerParameterf(sampler, pname, param)

  override def glSamplerParameterfv(sampler: Int, pname: Int, param: FloatBuffer): Unit =
    GLES30.glSamplerParameterfv(sampler, pname, param)

  override def glGetSamplerParameteriv(sampler: Int, pname: Int, params: IntBuffer): Unit =
    GLES30.glGetSamplerParameteriv(sampler, pname, params)

  override def glGetSamplerParameterfv(sampler: Int, pname: Int, params: FloatBuffer): Unit =
    GLES30.glGetSamplerParameterfv(sampler, pname, params)

  override def glVertexAttribDivisor(index: Int, divisor: Int): Unit = GLES30.glVertexAttribDivisor(index, divisor)

  // ── Transform feedback objects ────────────────────────────────────────

  override def glBindTransformFeedback(target: Int, id: Int): Unit = GLES30.glBindTransformFeedback(target, id)

  override def glDeleteTransformFeedbacks(n: Int, ids: Array[Int], offset: Int): Unit =
    GLES30.glDeleteTransformFeedbacks(n, ids, offset)

  override def glDeleteTransformFeedbacks(n: Int, ids: IntBuffer): Unit =
    GLES30.glDeleteTransformFeedbacks(n, ids)

  override def glGenTransformFeedbacks(n: Int, ids: Array[Int], offset: Int): Unit =
    GLES30.glGenTransformFeedbacks(n, ids, offset)

  override def glGenTransformFeedbacks(n: Int, ids: IntBuffer): Unit = GLES30.glGenTransformFeedbacks(n, ids)

  override def glIsTransformFeedback(id: Int): Boolean = GLES30.glIsTransformFeedback(id)

  override def glPauseTransformFeedback(): Unit = GLES30.glPauseTransformFeedback()

  override def glResumeTransformFeedback(): Unit = GLES30.glResumeTransformFeedback()

  override def glProgramParameteri(program: Int, pname: Int, value: Int): Unit =
    GLES30.glProgramParameteri(program, pname, value)

  // ── Framebuffer invalidation ──────────────────────────────────────────

  override def glInvalidateFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer): Unit =
    GLES30.glInvalidateFramebuffer(target, numAttachments, attachments)

  override def glInvalidateSubFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer, x: Int, y: Int, width: Int, height: Int): Unit =
    GLES30.glInvalidateSubFramebuffer(target, numAttachments, attachments, x, y, width, height)
}
