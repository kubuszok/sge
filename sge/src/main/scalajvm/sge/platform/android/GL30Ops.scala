// SGE — Android GL ES 3.0 ops interface
//
// Self-contained GL30 mirror trait using only JDK types (no sge.* dependencies).
// Extends GL20Ops with additional GL ES 3.0 methods.
//
// Migration notes:
//   Source:  com.badlogic.gdx.backends.android.AndroidGL30
//   Convention: ops interface pattern; all GL enum params as Int
//   Audited: 2026-03-08

package sge
package platform
package android

import java.nio.{ Buffer, FloatBuffer, IntBuffer, LongBuffer }

/** GL ES 3.0 operations — JDK-only mirror of sge.graphics.GL30. */
trait GL30Ops extends GL20Ops {

  def glReadBuffer(mode: Int): Unit

  def glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, `type`: Int, indices: Buffer): Unit

  def glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, `type`: Int, offset: Int): Unit

  def glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, `type`: Int, offset: Int): Unit

  def glTexImage3D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: Int, `type`: Int, pixels: Buffer): Unit

  def glTexImage3D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: Int, `type`: Int, offset: Int): Unit

  def glTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, `type`: Int, offset: Int): Unit

  def glTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int, format: Int, `type`: Int, pixels: Buffer): Unit

  def glTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int, format: Int, `type`: Int, offset: Int): Unit

  def glCopyTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit

  def glGenQueries(n: Int, ids: Array[Int], offset: Int): Unit

  def glGenQueries(n: Int, ids: IntBuffer): Unit

  def glDeleteQueries(n: Int, ids: Array[Int], offset: Int): Unit

  def glDeleteQueries(n: Int, ids: IntBuffer): Unit

  def glIsQuery(id: Int): Boolean

  def glBeginQuery(target: Int, id: Int): Unit

  def glEndQuery(target: Int): Unit

  def glGetQueryiv(target: Int, pname: Int, params: IntBuffer): Unit

  def glGetQueryObjectuiv(id: Int, pname: Int, params: IntBuffer): Unit

  def glUnmapBuffer(target: Int): Boolean

  def glGetBufferPointerv(target: Int, pname: Int): Buffer

  def glDrawBuffers(n: Int, bufs: IntBuffer): Unit

  def glUniformMatrix2x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glUniformMatrix3x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glUniformMatrix2x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glUniformMatrix4x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glUniformMatrix3x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glUniformMatrix4x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glBlitFramebuffer(srcX0: Int, srcY0: Int, srcX1: Int, srcY1: Int, dstX0: Int, dstY0: Int, dstX1: Int, dstY1: Int, mask: Int, filter: Int): Unit

  def glRenderbufferStorageMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int): Unit

  def glFramebufferTextureLayer(target: Int, attachment: Int, texture: Int, level: Int, layer: Int): Unit

  def glMapBufferRange(target: Int, offset: Int, length: Int, access: Int): Buffer

  def glFlushMappedBufferRange(target: Int, offset: Int, length: Int): Unit

  def glBindVertexArray(array: Int): Unit

  def glDeleteVertexArrays(n: Int, arrays: Array[Int], offset: Int): Unit

  def glDeleteVertexArrays(n: Int, arrays: IntBuffer): Unit

  def glGenVertexArrays(n: Int, arrays: Array[Int], offset: Int): Unit

  def glGenVertexArrays(n: Int, arrays: IntBuffer): Unit

  def glIsVertexArray(array: Int): Boolean

  def glBeginTransformFeedback(primitiveMode: Int): Unit

  def glEndTransformFeedback(): Unit

  def glBindBufferRange(target: Int, index: Int, buffer: Int, offset: Int, size: Int): Unit

  def glBindBufferBase(target: Int, index: Int, buffer: Int): Unit

  def glTransformFeedbackVaryings(program: Int, varyings: Array[String], bufferMode: Int): Unit

  def glVertexAttribIPointer(index: Int, size: Int, `type`: Int, stride: Int, offset: Int): Unit

  def glGetVertexAttribIiv(index: Int, pname: Int, params: IntBuffer): Unit

  def glGetVertexAttribIuiv(index: Int, pname: Int, params: IntBuffer): Unit

  def glVertexAttribI4i(index: Int, x: Int, y: Int, z: Int, w: Int): Unit

  def glVertexAttribI4ui(index: Int, x: Int, y: Int, z: Int, w: Int): Unit

  def glGetUniformuiv(program: Int, location: Int, params: IntBuffer): Unit

  def glGetFragDataLocation(program: Int, name: String): Int

  def glUniform1uiv(location: Int, count: Int, value: IntBuffer): Unit

  def glUniform3uiv(location: Int, count: Int, value: IntBuffer): Unit

  def glUniform4uiv(location: Int, count: Int, value: IntBuffer): Unit

  def glClearBufferiv(buffer: Int, drawbuffer: Int, value: IntBuffer): Unit

  def glClearBufferuiv(buffer: Int, drawbuffer: Int, value: IntBuffer): Unit

  def glClearBufferfv(buffer: Int, drawbuffer: Int, value: FloatBuffer): Unit

  def glClearBufferfi(buffer: Int, drawbuffer: Int, depth: Float, stencil: Int): Unit

  def glGetStringi(name: Int, index: Int): String

  def glCopyBufferSubData(readTarget: Int, writeTarget: Int, readOffset: Int, writeOffset: Int, size: Int): Unit

  def glGetUniformIndices(program: Int, uniformNames: Array[String], uniformIndices: IntBuffer): Unit

  def glGetActiveUniformsiv(program: Int, uniformCount: Int, uniformIndices: IntBuffer, pname: Int, params: IntBuffer): Unit

  def glGetUniformBlockIndex(program: Int, uniformBlockName: String): Int

  def glGetActiveUniformBlockiv(program: Int, uniformBlockIndex: Int, pname: Int, params: IntBuffer): Unit

  def glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int, length: Buffer, uniformBlockName: Buffer): Unit

  def glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int): String

  def glUniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int): Unit

  def glDrawArraysInstanced(mode: Int, first: Int, count: Int, instanceCount: Int): Unit

  def glDrawElementsInstanced(mode: Int, count: Int, `type`: Int, indicesOffset: Int, instanceCount: Int): Unit

  def glGetInteger64v(pname: Int, params: LongBuffer): Unit

  def glGetBufferParameteri64v(target: Int, pname: Int, params: LongBuffer): Unit

  def glGenSamplers(count: Int, samplers: Array[Int], offset: Int): Unit

  def glGenSamplers(count: Int, samplers: IntBuffer): Unit

  def glDeleteSamplers(count: Int, samplers: Array[Int], offset: Int): Unit

  def glDeleteSamplers(count: Int, samplers: IntBuffer): Unit

  def glIsSampler(sampler: Int): Boolean

  def glBindSampler(unit: Int, sampler: Int): Unit

  def glSamplerParameteri(sampler: Int, pname: Int, param: Int): Unit

  def glSamplerParameteriv(sampler: Int, pname: Int, param: IntBuffer): Unit

  def glSamplerParameterf(sampler: Int, pname: Int, param: Float): Unit

  def glSamplerParameterfv(sampler: Int, pname: Int, param: FloatBuffer): Unit

  def glGetSamplerParameteriv(sampler: Int, pname: Int, params: IntBuffer): Unit

  def glGetSamplerParameterfv(sampler: Int, pname: Int, params: FloatBuffer): Unit

  def glVertexAttribDivisor(index: Int, divisor: Int): Unit

  def glBindTransformFeedback(target: Int, id: Int): Unit

  def glDeleteTransformFeedbacks(n: Int, ids: Array[Int], offset: Int): Unit

  def glDeleteTransformFeedbacks(n: Int, ids: IntBuffer): Unit

  def glGenTransformFeedbacks(n: Int, ids: Array[Int], offset: Int): Unit

  def glGenTransformFeedbacks(n: Int, ids: IntBuffer): Unit

  def glIsTransformFeedback(id: Int): Boolean

  def glPauseTransformFeedback(): Unit

  def glResumeTransformFeedback(): Unit

  def glProgramParameteri(program: Int, pname: Int, value: Int): Unit

  def glInvalidateFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer): Unit

  def glInvalidateSubFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer, x: Int, y: Int, width: Int, height: Int): Unit
}
