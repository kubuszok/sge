/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package noop

import java.nio.{ Buffer, ByteBuffer, FloatBuffer, IntBuffer, LongBuffer }

import sge.graphics._

/** No-op [[sge.graphics.GL32]] test fake, mirroring the production [[NoopGL20]] object but reaching all the way up the GL interface chain (GL32 extends GL31 extends GL30 extends GL20). Every method
  * is a no-op returning zero/false/empty/Unit.
  *
  * Exists so the GL profiling tests can drive the GL30/GL31/GL32 interceptors: `GLProfiler` selects the interceptor for the highest GL level a `Graphics` exposes, and `NoopGraphics` only exposes GL20
  * (`gl30`/`gl31`/`gl32` are `Nullable.empty`). Feeding this fake as the wrapped delegate (directly, or via a `Graphics` that reports it as `gl3x`) routes calls through the GL30+ interceptors so
  * their draw-counting paths (instanced / range / draw-buffers / indirect / base-vertex) can be characterized.
  */
class NoopGL32 extends GL32 {

  private val emptyBuffer: ByteBuffer = ByteBuffer.allocate(0)

  // ---- GL20 ----

  def glActiveTexture(texture: Int): Unit = {}

  def glBindTexture(target: TextureTarget, texture: Int): Unit = {}

  def glBlendFunc(sfactor: BlendFactor, dfactor: BlendFactor): Unit = {}

  def glClear(mask: ClearMask): Unit = {}

  def glClearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = {}

  def glClearDepthf(depth: Float): Unit = {}

  def glClearStencil(s: Int): Unit = {}

  def glColorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit = {}

  def glCompressedTexImage2D(target: TextureTarget, level: Int, internalformat: Int, width: Pixels, height: Pixels, border: Int, imageSize: Int, data: Buffer): Unit = {}

  def glCompressedTexSubImage2D(target: TextureTarget, level: Int, xoffset: Pixels, yoffset: Pixels, width: Pixels, height: Pixels, format: PixelFormat, imageSize: Int, data: Buffer): Unit = {}

  def glCopyTexImage2D(target: TextureTarget, level: Int, internalformat: Int, x: Pixels, y: Pixels, width: Pixels, height: Pixels, border: Int): Unit = {}

  def glCopyTexSubImage2D(target: TextureTarget, level: Int, xoffset: Pixels, yoffset: Pixels, x: Pixels, y: Pixels, width: Pixels, height: Pixels): Unit = {}

  def glCullFace(mode: CullFace): Unit = {}

  def glDeleteTextures(n: Int, textures: IntBuffer): Unit = {}

  def glDeleteTexture(texture: Int): Unit = {}

  def glDepthFunc(func: CompareFunc): Unit = {}

  def glDepthMask(flag: Boolean): Unit = {}

  def glDepthRangef(zNear: Float, zFar: Float): Unit = {}

  def glDisable(cap: EnableCap): Unit = {}

  def glDrawArrays(mode: PrimitiveMode, first: Int, count: Int): Unit = {}

  def glDrawElements(mode: PrimitiveMode, count: Int, `type`: DataType, indices: Buffer): Unit = {}

  def glEnable(cap: EnableCap): Unit = {}

  def glFinish(): Unit = {}

  def glFlush(): Unit = {}

  def glFrontFace(mode: Int): Unit = {}

  def glGenTextures(n: Int, textures: IntBuffer): Unit = {}

  def glGenTexture(): Int = 0

  def glGetError(): Int = 0

  def glGetIntegerv(pname: Int, params: IntBuffer): Unit = {}

  def glGetString(name: Int): String = ""

  def glHint(target: Int, mode: Int): Unit = {}

  def glLineWidth(width: Float): Unit = {}

  def glPixelStorei(pname: Int, param: Int): Unit = {}

  def glPolygonOffset(factor: Float, units: Float): Unit = {}

  def glReadPixels(x: Pixels, y: Pixels, width: Pixels, height: Pixels, format: PixelFormat, `type`: DataType, pixels: Buffer): Unit = {}

  def glScissor(x: Pixels, y: Pixels, width: Pixels, height: Pixels): Unit = {}

  def glStencilFunc(func: CompareFunc, ref: Int, mask: Int): Unit = {}

  def glStencilMask(mask: Int): Unit = {}

  def glStencilOp(fail: StencilOp, zfail: StencilOp, zpass: StencilOp): Unit = {}

  def glTexImage2D(target: TextureTarget, level: Int, internalformat: Int, width: Pixels, height: Pixels, border: Int, format: PixelFormat, `type`: DataType, pixels: Buffer): Unit = {}

  def glTexParameterf(target: TextureTarget, pname: Int, param: Float): Unit = {}

  def glTexSubImage2D(target: TextureTarget, level: Int, xoffset: Pixels, yoffset: Pixels, width: Pixels, height: Pixels, format: PixelFormat, `type`: DataType, pixels: Buffer): Unit = {}

  def glViewport(x: Pixels, y: Pixels, width: Pixels, height: Pixels): Unit = {}

  def glAttachShader(program: Int, shader: Int): Unit = {}

  def glBindAttribLocation(program: Int, index: Int, name: String): Unit = {}

  def glBindBuffer(target: BufferTarget, buffer: Int): Unit = {}

  def glBindFramebuffer(target: Int, framebuffer: Int): Unit = {}

  def glBindRenderbuffer(target: Int, renderbuffer: Int): Unit = {}

  def glBlendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = {}

  def glBlendEquation(mode: BlendEquation): Unit = {}

  def glBlendEquationSeparate(modeRGB: BlendEquation, modeAlpha: BlendEquation): Unit = {}

  def glBlendFuncSeparate(srcRGB: BlendFactor, dstRGB: BlendFactor, srcAlpha: BlendFactor, dstAlpha: BlendFactor): Unit = {}

  def glBufferData(target: BufferTarget, size: Int, data: Buffer, usage: BufferUsage): Unit = {}

  def glBufferSubData(target: BufferTarget, offset: Int, size: Int, data: Buffer): Unit = {}

  def glCheckFramebufferStatus(target: Int): Int = 0

  def glCompileShader(shader: Int): Unit = {}

  def glCreateProgram(): Int = 0

  def glCreateShader(`type`: ShaderType): Int = 0

  def glDeleteBuffer(buffer: Int): Unit = {}

  def glDeleteBuffers(n: Int, buffers: IntBuffer): Unit = {}

  def glDeleteFramebuffer(framebuffer: Int): Unit = {}

  def glDeleteFramebuffers(n: Int, framebuffers: IntBuffer): Unit = {}

  def glDeleteProgram(program: Int): Unit = {}

  def glDeleteRenderbuffer(renderbuffer: Int): Unit = {}

  def glDeleteRenderbuffers(n: Int, renderbuffers: IntBuffer): Unit = {}

  def glDeleteShader(shader: Int): Unit = {}

  def glDetachShader(program: Int, shader: Int): Unit = {}

  def glDisableVertexAttribArray(index: Int): Unit = {}

  def glDrawElements(mode: PrimitiveMode, count: Int, `type`: DataType, indices: Int): Unit = {}

  def glEnableVertexAttribArray(index: Int): Unit = {}

  def glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int): Unit = {}

  def glFramebufferTexture2D(target: Int, attachment: Int, textarget: TextureTarget, texture: Int, level: Int): Unit = {}

  def glGenBuffer(): Int = 0

  def glGenBuffers(n: Int, buffers: IntBuffer): Unit = {}

  def glGenerateMipmap(target: TextureTarget): Unit = {}

  def glGenFramebuffer(): Int = 0

  def glGenFramebuffers(n: Int, framebuffers: IntBuffer): Unit = {}

  def glGenRenderbuffer(): Int = 0

  def glGenRenderbuffers(n: Int, renderbuffers: IntBuffer): Unit = {}

  def glGetActiveAttrib(program: Int, index: Int, size: IntBuffer, `type`: IntBuffer): String = ""

  def glGetActiveUniform(program: Int, index: Int, size: IntBuffer, `type`: IntBuffer): String = ""

  def glGetAttachedShaders(program: Int, maxcount: Int, count: Buffer, shaders: IntBuffer): Unit = {}

  def glGetAttribLocation(program: Int, name: String): Int = 0

  def glGetBooleanv(pname: Int, params: Buffer): Unit = {}

  def glGetBufferParameteriv(target: BufferTarget, pname: Int, params: IntBuffer): Unit = {}

  def glGetFloatv(pname: Int, params: FloatBuffer): Unit = {}

  def glGetFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: IntBuffer): Unit = {}

  def glGetProgramiv(program: Int, pname: Int, params: IntBuffer): Unit = {}

  def glGetProgramInfoLog(program: Int): String = ""

  def glGetRenderbufferParameteriv(target: Int, pname: Int, params: IntBuffer): Unit = {}

  def glGetShaderiv(shader: Int, pname: Int, params: IntBuffer): Unit = {}

  def glGetShaderInfoLog(shader: Int): String = ""

  def glGetShaderPrecisionFormat(shadertype: ShaderType, precisiontype: Int, range: IntBuffer, precision: IntBuffer): Unit = {}

  def glGetTexParameterfv(target: TextureTarget, pname: Int, params: FloatBuffer): Unit = {}

  def glGetTexParameteriv(target: TextureTarget, pname: Int, params: IntBuffer): Unit = {}

  def glGetUniformfv(program: Int, location: Int, params: FloatBuffer): Unit = {}

  def glGetUniformiv(program: Int, location: Int, params: IntBuffer): Unit = {}

  def glGetUniformLocation(program: Int, name: String): Int = 0

  def glGetVertexAttribfv(index: Int, pname: Int, params: FloatBuffer): Unit = {}

  def glGetVertexAttribiv(index: Int, pname: Int, params: IntBuffer): Unit = {}

  def glGetVertexAttribPointerv(index: Int, pname: Int, pointer: Buffer): Unit = {}

  def glIsBuffer(buffer: Int): Boolean = false

  def glIsEnabled(cap: EnableCap): Boolean = false

  def glIsFramebuffer(framebuffer: Int): Boolean = false

  def glIsProgram(program: Int): Boolean = false

  def glIsRenderbuffer(renderbuffer: Int): Boolean = false

  def glIsShader(shader: Int): Boolean = false

  def glIsTexture(texture: Int): Boolean = false

  def glLinkProgram(program: Int): Unit = {}

  def glReleaseShaderCompiler(): Unit = {}

  def glRenderbufferStorage(target: Int, internalformat: Int, width: Pixels, height: Pixels): Unit = {}

  def glSampleCoverage(value: Float, invert: Boolean): Unit = {}

  def glShaderBinary(n: Int, shaders: IntBuffer, binaryformat: Int, binary: Buffer, length: Int): Unit = {}

  def glShaderSource(shader: Int, string: String): Unit = {}

  def glStencilFuncSeparate(face: CullFace, func: CompareFunc, ref: Int, mask: Int): Unit = {}

  def glStencilMaskSeparate(face: CullFace, mask: Int): Unit = {}

  def glStencilOpSeparate(face: CullFace, fail: StencilOp, zfail: StencilOp, zpass: StencilOp): Unit = {}

  def glTexParameterfv(target: TextureTarget, pname: Int, params: FloatBuffer): Unit = {}

  def glTexParameteri(target: TextureTarget, pname: Int, param: Int): Unit = {}

  def glTexParameteriv(target: TextureTarget, pname: Int, params: IntBuffer): Unit = {}

  def glUniform1f(location: Int, x: Float): Unit = {}

  def glUniform1fv(location: Int, count: Int, v: FloatBuffer): Unit = {}

  def glUniform1fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit = {}

  def glUniform1i(location: Int, x: Int): Unit = {}

  def glUniform1iv(location: Int, count: Int, v: IntBuffer): Unit = {}

  def glUniform1iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit = {}

  def glUniform2f(location: Int, x: Float, y: Float): Unit = {}

  def glUniform2fv(location: Int, count: Int, v: FloatBuffer): Unit = {}

  def glUniform2fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit = {}

  def glUniform2i(location: Int, x: Int, y: Int): Unit = {}

  def glUniform2iv(location: Int, count: Int, v: IntBuffer): Unit = {}

  def glUniform2iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit = {}

  def glUniform3f(location: Int, x: Float, y: Float, z: Float): Unit = {}

  def glUniform3fv(location: Int, count: Int, v: FloatBuffer): Unit = {}

  def glUniform3fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit = {}

  def glUniform3i(location: Int, x: Int, y: Int, z: Int): Unit = {}

  def glUniform3iv(location: Int, count: Int, v: IntBuffer): Unit = {}

  def glUniform3iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit = {}

  def glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float): Unit = {}

  def glUniform4fv(location: Int, count: Int, v: FloatBuffer): Unit = {}

  def glUniform4fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit = {}

  def glUniform4i(location: Int, x: Int, y: Int, z: Int, w: Int): Unit = {}

  def glUniform4iv(location: Int, count: Int, v: IntBuffer): Unit = {}

  def glUniform4iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit = {}

  def glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = {}

  def glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit = {}

  def glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = {}

  def glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit = {}

  def glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = {}

  def glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit = {}

  def glUseProgram(program: Int): Unit = {}

  def glValidateProgram(program: Int): Unit = {}

  def glVertexAttrib1f(indx: Int, x: Float): Unit = {}

  def glVertexAttrib1fv(indx: Int, values: FloatBuffer): Unit = {}

  def glVertexAttrib2f(indx: Int, x: Float, y: Float): Unit = {}

  def glVertexAttrib2fv(indx: Int, values: FloatBuffer): Unit = {}

  def glVertexAttrib3f(indx: Int, x: Float, y: Float, z: Float): Unit = {}

  def glVertexAttrib3fv(indx: Int, values: FloatBuffer): Unit = {}

  def glVertexAttrib4f(indx: Int, x: Float, y: Float, z: Float, w: Float): Unit = {}

  def glVertexAttrib4fv(indx: Int, values: FloatBuffer): Unit = {}

  def glVertexAttribPointer(indx: Int, size: Int, `type`: DataType, normalized: Boolean, stride: Int, ptr: Buffer): Unit = {}

  def glVertexAttribPointer(indx: Int, size: Int, `type`: DataType, normalized: Boolean, stride: Int, ptr: Int): Unit = {}

  // ---- GL30 ----

  def glReadBuffer(mode: Int): Unit = {}

  def glDrawRangeElements(mode: PrimitiveMode, start: Int, end: Int, count: Int, `type`: DataType, indices: Buffer): Unit = {}

  def glDrawRangeElements(mode: PrimitiveMode, start: Int, end: Int, count: Int, `type`: DataType, offset: Int): Unit = {}

  def glTexImage2D(target: TextureTarget, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: PixelFormat, `type`: DataType, offset: Int): Unit = {}

  def glTexImage3D(target: TextureTarget, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: PixelFormat, `type`: DataType, pixels: Buffer): Unit = {}

  def glTexImage3D(target: TextureTarget, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: PixelFormat, `type`: DataType, offset: Int): Unit = {}

  def glTexSubImage2D(target: TextureTarget, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: PixelFormat, `type`: DataType, offset: Int): Unit = {}

  def glTexSubImage3D(
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
  ): Unit = {}

  def glTexSubImage3D(target: TextureTarget, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int, format: PixelFormat, `type`: DataType, offset: Int): Unit = {}

  def glCopyTexSubImage3D(target: TextureTarget, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit = {}

  def glGenQueries(n: Int, ids: Array[Int], offset: Int): Unit = {}

  def glGenQueries(n: Int, ids: IntBuffer): Unit = {}

  def glDeleteQueries(n: Int, ids: Array[Int], offset: Int): Unit = {}

  def glDeleteQueries(n: Int, ids: IntBuffer): Unit = {}

  def glIsQuery(id: Int): Boolean = false

  def glBeginQuery(target: Int, id: Int): Unit = {}

  def glEndQuery(target: Int): Unit = {}

  def glGetQueryiv(target: Int, pname: Int, params: IntBuffer): Unit = {}

  def glGetQueryObjectuiv(id: Int, pname: Int, params: IntBuffer): Unit = {}

  def glUnmapBuffer(target: BufferTarget): Boolean = false

  def glGetBufferPointerv(target: BufferTarget, pname: Int): Buffer = emptyBuffer

  def glDrawBuffers(n: Int, bufs: IntBuffer): Unit = {}

  def glUniformMatrix2x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = {}

  def glUniformMatrix3x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = {}

  def glUniformMatrix2x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = {}

  def glUniformMatrix4x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = {}

  def glUniformMatrix3x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = {}

  def glUniformMatrix4x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = {}

  def glBlitFramebuffer(srcX0: Int, srcY0: Int, srcX1: Int, srcY1: Int, dstX0: Int, dstY0: Int, dstX1: Int, dstY1: Int, mask: ClearMask, filter: Int): Unit = {}

  def glRenderbufferStorageMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int): Unit = {}

  def glFramebufferTextureLayer(target: Int, attachment: Int, texture: Int, level: Int, layer: Int): Unit = {}

  def glMapBufferRange(target: BufferTarget, offset: Int, length: Int, access: Int): Buffer = emptyBuffer

  def glFlushMappedBufferRange(target: BufferTarget, offset: Int, length: Int): Unit = {}

  def glBindVertexArray(array: Int): Unit = {}

  def glDeleteVertexArrays(n: Int, arrays: Array[Int], offset: Int): Unit = {}

  def glDeleteVertexArrays(n: Int, arrays: IntBuffer): Unit = {}

  def glGenVertexArrays(n: Int, arrays: Array[Int], offset: Int): Unit = {}

  def glGenVertexArrays(n: Int, arrays: IntBuffer): Unit = {}

  def glIsVertexArray(array: Int): Boolean = false

  def glBeginTransformFeedback(primitiveMode: PrimitiveMode): Unit = {}

  def glEndTransformFeedback(): Unit = {}

  def glBindBufferRange(target: BufferTarget, index: Int, buffer: Int, offset: Int, size: Int): Unit = {}

  def glBindBufferBase(target: BufferTarget, index: Int, buffer: Int): Unit = {}

  def glTransformFeedbackVaryings(program: Int, varyings: Array[String], bufferMode: Int): Unit = {}

  def glVertexAttribIPointer(index: Int, size: Int, `type`: DataType, stride: Int, offset: Int): Unit = {}

  def glGetVertexAttribIiv(index: Int, pname: Int, params: IntBuffer): Unit = {}

  def glGetVertexAttribIuiv(index: Int, pname: Int, params: IntBuffer): Unit = {}

  def glVertexAttribI4i(index: Int, x: Int, y: Int, z: Int, w: Int): Unit = {}

  def glVertexAttribI4ui(index: Int, x: Int, y: Int, z: Int, w: Int): Unit = {}

  def glGetUniformuiv(program: Int, location: Int, params: IntBuffer): Unit = {}

  def glGetFragDataLocation(program: Int, name: String): Int = 0

  def glUniform1uiv(location: Int, count: Int, value: IntBuffer): Unit = {}

  def glUniform3uiv(location: Int, count: Int, value: IntBuffer): Unit = {}

  def glUniform4uiv(location: Int, count: Int, value: IntBuffer): Unit = {}

  def glClearBufferiv(buffer: Int, drawbuffer: Int, value: IntBuffer): Unit = {}

  def glClearBufferuiv(buffer: Int, drawbuffer: Int, value: IntBuffer): Unit = {}

  def glClearBufferfv(buffer: Int, drawbuffer: Int, value: FloatBuffer): Unit = {}

  def glClearBufferfi(buffer: Int, drawbuffer: Int, depth: Float, stencil: Int): Unit = {}

  def glGetStringi(name: Int, index: Int): String = ""

  def glCopyBufferSubData(readTarget: BufferTarget, writeTarget: BufferTarget, readOffset: Int, writeOffset: Int, size: Int): Unit = {}

  def glGetUniformIndices(program: Int, uniformNames: Array[String], uniformIndices: IntBuffer): Unit = {}

  def glGetActiveUniformsiv(program: Int, uniformCount: Int, uniformIndices: IntBuffer, pname: Int, params: IntBuffer): Unit = {}

  def glGetUniformBlockIndex(program: Int, uniformBlockName: String): Int = 0

  def glGetActiveUniformBlockiv(program: Int, uniformBlockIndex: Int, pname: Int, params: IntBuffer): Unit = {}

  def glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int, length: Buffer, uniformBlockName: Buffer): Unit = {}

  def glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int): String = ""

  def glUniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int): Unit = {}

  def glDrawArraysInstanced(mode: PrimitiveMode, first: Int, count: Int, instanceCount: Int): Unit = {}

  def glDrawElementsInstanced(mode: PrimitiveMode, count: Int, `type`: DataType, indicesOffset: Int, instanceCount: Int): Unit = {}

  def glGetInteger64v(pname: Int, params: LongBuffer): Unit = {}

  def glGetBufferParameteri64v(target: BufferTarget, pname: Int, params: LongBuffer): Unit = {}

  def glGenSamplers(count: Int, samplers: Array[Int], offset: Int): Unit = {}

  def glGenSamplers(count: Int, samplers: IntBuffer): Unit = {}

  def glDeleteSamplers(count: Int, samplers: Array[Int], offset: Int): Unit = {}

  def glDeleteSamplers(count: Int, samplers: IntBuffer): Unit = {}

  def glIsSampler(sampler: Int): Boolean = false

  def glBindSampler(unit: Int, sampler: Int): Unit = {}

  def glSamplerParameteri(sampler: Int, pname: Int, param: Int): Unit = {}

  def glSamplerParameteriv(sampler: Int, pname: Int, param: IntBuffer): Unit = {}

  def glSamplerParameterf(sampler: Int, pname: Int, param: Float): Unit = {}

  def glSamplerParameterfv(sampler: Int, pname: Int, param: FloatBuffer): Unit = {}

  def glGetSamplerParameteriv(sampler: Int, pname: Int, params: IntBuffer): Unit = {}

  def glGetSamplerParameterfv(sampler: Int, pname: Int, params: FloatBuffer): Unit = {}

  def glVertexAttribDivisor(index: Int, divisor: Int): Unit = {}

  def glBindTransformFeedback(target: Int, id: Int): Unit = {}

  def glDeleteTransformFeedbacks(n: Int, ids: Array[Int], offset: Int): Unit = {}

  def glDeleteTransformFeedbacks(n: Int, ids: IntBuffer): Unit = {}

  def glGenTransformFeedbacks(n: Int, ids: Array[Int], offset: Int): Unit = {}

  def glGenTransformFeedbacks(n: Int, ids: IntBuffer): Unit = {}

  def glIsTransformFeedback(id: Int): Boolean = false

  def glPauseTransformFeedback(): Unit = {}

  def glResumeTransformFeedback(): Unit = {}

  def glProgramParameteri(program: Int, pname: Int, value: Int): Unit = {}

  def glInvalidateFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer): Unit = {}

  def glInvalidateSubFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer, x: Int, y: Int, width: Int, height: Int): Unit = {}

  // ---- GL31 ----

  def glDispatchCompute(num_groups_x: Int, num_groups_y: Int, num_groups_z: Int): Unit = {}

  def glDispatchComputeIndirect(indirect: Long): Unit = {}

  def glDrawArraysIndirect(mode: PrimitiveMode, indirect: Long): Unit = {}

  def glDrawElementsIndirect(mode: PrimitiveMode, `type`: DataType, indirect: Long): Unit = {}

  def glFramebufferParameteri(target: Int, pname: Int, param: Int): Unit = {}

  def glGetFramebufferParameteriv(target: Int, pname: Int, params: IntBuffer): Unit = {}

  def glGetProgramInterfaceiv(program: Int, programInterface: Int, pname: Int, params: IntBuffer): Unit = {}

  def glGetProgramResourceIndex(program: Int, programInterface: Int, name: String): Int = 0

  def glGetProgramResourceName(program: Int, programInterface: Int, index: Int): String = ""

  def glGetProgramResourceiv(program: Int, programInterface: Int, index: Int, props: IntBuffer, length: IntBuffer, params: IntBuffer): Unit = {}

  def glGetProgramResourceLocation(program: Int, programInterface: Int, name: String): Int = 0

  def glUseProgramStages(pipeline: Int, stages: Int, program: Int): Unit = {}

  def glActiveShaderProgram(pipeline: Int, program: Int): Unit = {}

  def glCreateShaderProgramv(`type`: ShaderType, strings: Array[String]): Int = 0

  def glBindProgramPipeline(pipeline: Int): Unit = {}

  def glDeleteProgramPipelines(n: Int, pipelines: IntBuffer): Unit = {}

  def glGenProgramPipelines(n: Int, pipelines: IntBuffer): Unit = {}

  def glIsProgramPipeline(pipeline: Int): Boolean = false

  def glGetProgramPipelineiv(pipeline: Int, pname: Int, params: IntBuffer): Unit = {}

  def glProgramUniform1i(program: Int, location: Int, v0: Int): Unit = {}

  def glProgramUniform2i(program: Int, location: Int, v0: Int, v1: Int): Unit = {}

  def glProgramUniform3i(program: Int, location: Int, v0: Int, v1: Int, v2: Int): Unit = {}

  def glProgramUniform4i(program: Int, location: Int, v0: Int, v1: Int, v2: Int, v3: Int): Unit = {}

  def glProgramUniform1ui(program: Int, location: Int, v0: Int): Unit = {}

  def glProgramUniform2ui(program: Int, location: Int, v0: Int, v1: Int): Unit = {}

  def glProgramUniform3ui(program: Int, location: Int, v0: Int, v1: Int, v2: Int): Unit = {}

  def glProgramUniform4ui(program: Int, location: Int, v0: Int, v1: Int, v2: Int, v3: Int): Unit = {}

  def glProgramUniform1f(program: Int, location: Int, v0: Float): Unit = {}

  def glProgramUniform2f(program: Int, location: Int, v0: Float, v1: Float): Unit = {}

  def glProgramUniform3f(program: Int, location: Int, v0: Float, v1: Float, v2: Float): Unit = {}

  def glProgramUniform4f(program: Int, location: Int, v0: Float, v1: Float, v2: Float, v3: Float): Unit = {}

  def glProgramUniform1iv(program: Int, location: Int, value: IntBuffer): Unit = {}

  def glProgramUniform2iv(program: Int, location: Int, value: IntBuffer): Unit = {}

  def glProgramUniform3iv(program: Int, location: Int, value: IntBuffer): Unit = {}

  def glProgramUniform4iv(program: Int, location: Int, value: IntBuffer): Unit = {}

  def glProgramUniform1uiv(program: Int, location: Int, value: IntBuffer): Unit = {}

  def glProgramUniform2uiv(program: Int, location: Int, value: IntBuffer): Unit = {}

  def glProgramUniform3uiv(program: Int, location: Int, value: IntBuffer): Unit = {}

  def glProgramUniform4uiv(program: Int, location: Int, value: IntBuffer): Unit = {}

  def glProgramUniform1fv(program: Int, location: Int, value: FloatBuffer): Unit = {}

  def glProgramUniform2fv(program: Int, location: Int, value: FloatBuffer): Unit = {}

  def glProgramUniform3fv(program: Int, location: Int, value: FloatBuffer): Unit = {}

  def glProgramUniform4fv(program: Int, location: Int, value: FloatBuffer): Unit = {}

  def glProgramUniformMatrix2fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit = {}

  def glProgramUniformMatrix3fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit = {}

  def glProgramUniformMatrix4fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit = {}

  def glProgramUniformMatrix2x3fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit = {}

  def glProgramUniformMatrix3x2fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit = {}

  def glProgramUniformMatrix2x4fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit = {}

  def glProgramUniformMatrix4x2fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit = {}

  def glProgramUniformMatrix3x4fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit = {}

  def glProgramUniformMatrix4x3fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit = {}

  def glValidateProgramPipeline(pipeline: Int): Unit = {}

  def glGetProgramPipelineInfoLog(program: Int): String = ""

  def glBindImageTexture(unit: Int, texture: Int, level: Int, layered: Boolean, layer: Int, access: Int, format: Int): Unit = {}

  def glGetBooleani_v(target: Int, index: Int, data: IntBuffer): Unit = {}

  def glMemoryBarrier(barriers: Int): Unit = {}

  def glMemoryBarrierByRegion(barriers: Int): Unit = {}

  def glTexStorage2DMultisample(target: TextureTarget, samples: Int, internalformat: Int, width: Int, height: Int, fixedsamplelocations: Boolean): Unit = {}

  def glGetMultisamplefv(pname: Int, index: Int, value: FloatBuffer): Unit = {}

  def glSampleMaski(maskNumber: Int, mask: Int): Unit = {}

  def glGetTexLevelParameteriv(target: TextureTarget, level: Int, pname: Int, params: IntBuffer): Unit = {}

  def glGetTexLevelParameterfv(target: TextureTarget, level: Int, pname: Int, params: FloatBuffer): Unit = {}

  def glBindVertexBuffer(bindingindex: Int, buffer: Int, offset: Long, stride: Int): Unit = {}

  def glVertexAttribFormat(attribindex: Int, size: Int, `type`: DataType, normalized: Boolean, relativeoffset: Int): Unit = {}

  def glVertexAttribIFormat(attribindex: Int, size: Int, `type`: DataType, relativeoffset: Int): Unit = {}

  def glVertexAttribBinding(attribindex: Int, bindingindex: Int): Unit = {}

  def glVertexBindingDivisor(bindingindex: Int, divisor: Int): Unit = {}

  // ---- GL32 ----

  def glBlendBarrier(): Unit = {}

  def glCopyImageSubData(
    srcName:   Int,
    srcTarget: Int,
    srcLevel:  Int,
    srcX:      Int,
    srcY:      Int,
    srcZ:      Int,
    dstName:   Int,
    dstTarget: Int,
    dstLevel:  Int,
    dstX:      Int,
    dstY:      Int,
    dstZ:      Int,
    srcWidth:  Int,
    srcHeight: Int,
    srcDepth:  Int
  ): Unit = {}

  def glDebugMessageControl(source: Int, `type`: Int, severity: Int, ids: IntBuffer, enabled: Boolean): Unit = {}

  def glDebugMessageInsert(source: Int, `type`: Int, id: Int, severity: Int, buf: String): Unit = {}

  def glDebugMessageCallback(callback: DebugProc): Unit = {}

  def glGetDebugMessageLog(count: Int, sources: IntBuffer, types: IntBuffer, ids: IntBuffer, severities: IntBuffer, lengths: IntBuffer, messageLog: java.nio.ByteBuffer): Int = 0

  def glPushDebugGroup(source: Int, id: Int, message: String): Unit = {}

  def glPopDebugGroup(): Unit = {}

  def glObjectLabel(identifier: Int, name: Int, label: String): Unit = {}

  def glGetObjectLabel(identifier: Int, name: Int): String = ""

  def glGetPointerv(pname: Int): Long = 0L

  def glEnablei(target: Int, index: Int): Unit = {}

  def glDisablei(target: Int, index: Int): Unit = {}

  def glBlendEquationi(buf: Int, mode: BlendEquation): Unit = {}

  def glBlendEquationSeparatei(buf: Int, modeRGB: BlendEquation, modeAlpha: BlendEquation): Unit = {}

  def glBlendFunci(buf: Int, src: BlendFactor, dst: BlendFactor): Unit = {}

  def glBlendFuncSeparatei(buf: Int, srcRGB: BlendFactor, dstRGB: BlendFactor, srcAlpha: BlendFactor, dstAlpha: BlendFactor): Unit = {}

  def glColorMaski(index: Int, r: Boolean, g: Boolean, b: Boolean, a: Boolean): Unit = {}

  def glIsEnabledi(target: Int, index: Int): Boolean = false

  def glDrawElementsBaseVertex(mode: PrimitiveMode, count: Int, `type`: DataType, indices: Buffer, basevertex: Int): Unit = {}

  def glDrawRangeElementsBaseVertex(mode: PrimitiveMode, start: Int, end: Int, count: Int, `type`: DataType, indices: Buffer, basevertex: Int): Unit = {}

  def glDrawElementsInstancedBaseVertex(mode: PrimitiveMode, count: Int, `type`: DataType, indices: Buffer, instanceCount: Int, basevertex: Int): Unit = {}

  def glDrawElementsInstancedBaseVertex(mode: PrimitiveMode, count: Int, `type`: DataType, indicesOffset: Int, instanceCount: Int, basevertex: Int): Unit = {}

  def glFramebufferTexture(target: Int, attachment: Int, texture: Int, level: Int): Unit = {}

  def glGetGraphicsResetStatus(): Int = 0

  def glReadnPixels(x: Int, y: Int, width: Int, height: Int, format: PixelFormat, `type`: DataType, bufSize: Int, data: Buffer): Unit = {}

  def glGetnUniformfv(program: Int, location: Int, params: FloatBuffer): Unit = {}

  def glGetnUniformiv(program: Int, location: Int, params: IntBuffer): Unit = {}

  def glGetnUniformuiv(program: Int, location: Int, params: IntBuffer): Unit = {}

  def glMinSampleShading(value: Float): Unit = {}

  def glPatchParameteri(pname: Int, value: Int): Unit = {}

  def glTexParameterIiv(target: TextureTarget, pname: Int, params: IntBuffer): Unit = {}

  def glTexParameterIuiv(target: TextureTarget, pname: Int, params: IntBuffer): Unit = {}

  def glGetTexParameterIiv(target: TextureTarget, pname: Int, params: IntBuffer): Unit = {}

  def glGetTexParameterIuiv(target: TextureTarget, pname: Int, params: IntBuffer): Unit = {}

  def glSamplerParameterIiv(sampler: Int, pname: Int, param: IntBuffer): Unit = {}

  def glSamplerParameterIuiv(sampler: Int, pname: Int, param: IntBuffer): Unit = {}

  def glGetSamplerParameterIiv(sampler: Int, pname: Int, params: IntBuffer): Unit = {}

  def glGetSamplerParameterIuiv(sampler: Int, pname: Int, params: IntBuffer): Unit = {}

  def glTexBuffer(target: TextureTarget, internalformat: Int, buffer: Int): Unit = {}

  def glTexBufferRange(target: TextureTarget, internalformat: Int, buffer: Int, offset: Int, size: Int): Unit = {}

  def glTexStorage3DMultisample(target: TextureTarget, samples: Int, internalformat: Int, width: Int, height: Int, depth: Int, fixedsamplelocations: Boolean): Unit = {}
}
