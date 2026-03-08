/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (adapts GL20Ops plain-Int mirror to sge.graphics.GL20 opaque types)
 *   Convention: JVM-only; delegates to GL20Ops from sge.platform.android
 *   Convention: Opaque types erase to Int at runtime — adapter casts via .toInt / apply(raw)
 *   Idiom: split packages
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import java.nio.{ Buffer, FloatBuffer, IntBuffer }
import sge.platform.android.GL20Ops

/** Adapts [[GL20Ops]] (JDK-only, plain Int params) to [[GL20]] (opaque type params).
  *
  * Since all opaque GL enum types (`TextureTarget`, `BlendFactor`, etc.) erase to `Int` at runtime, this adapter simply delegates every call, converting opaque params via `.toInt` and wrapping return
  * values where needed.
  *
  * @param ops
  *   the underlying GL ES 2.0 operations (from Android platform implementation)
  */
class AndroidGL20Adapter(protected val ops: GL20Ops) extends GL20 {

  override def glActiveTexture(texture: Int): Unit = ops.glActiveTexture(texture)

  override def glBindTexture(target: TextureTarget, texture: Int): Unit = ops.glBindTexture(target.toInt, texture)

  override def glBlendFunc(sfactor: BlendFactor, dfactor: BlendFactor): Unit =
    ops.glBlendFunc(sfactor.toInt, dfactor.toInt)

  override def glClear(mask: ClearMask): Unit = ops.glClear(mask.toInt)

  override def glClearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit =
    ops.glClearColor(red, green, blue, alpha)

  override def glClearDepthf(depth: Float): Unit = ops.glClearDepthf(depth)

  override def glClearStencil(s: Int): Unit = ops.glClearStencil(s)

  override def glColorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit =
    ops.glColorMask(red, green, blue, alpha)

  override def glCompressedTexImage2D(target: TextureTarget, level: Int, internalformat: Int, width: Pixels, height: Pixels, border: Int, imageSize: Int, data: Buffer): Unit =
    ops.glCompressedTexImage2D(target.toInt, level, internalformat, width.toInt, height.toInt, border, imageSize, data)

  override def glCompressedTexSubImage2D(target: TextureTarget, level: Int, xoffset: Pixels, yoffset: Pixels, width: Pixels, height: Pixels, format: PixelFormat, imageSize: Int, data: Buffer): Unit =
    ops.glCompressedTexSubImage2D(target.toInt, level, xoffset.toInt, yoffset.toInt, width.toInt, height.toInt, format.toInt, imageSize, data)

  override def glCopyTexImage2D(target: TextureTarget, level: Int, internalformat: Int, x: Pixels, y: Pixels, width: Pixels, height: Pixels, border: Int): Unit =
    ops.glCopyTexImage2D(target.toInt, level, internalformat, x.toInt, y.toInt, width.toInt, height.toInt, border)

  override def glCopyTexSubImage2D(target: TextureTarget, level: Int, xoffset: Pixels, yoffset: Pixels, x: Pixels, y: Pixels, width: Pixels, height: Pixels): Unit =
    ops.glCopyTexSubImage2D(target.toInt, level, xoffset.toInt, yoffset.toInt, x.toInt, y.toInt, width.toInt, height.toInt)

  override def glCullFace(mode: CullFace): Unit = ops.glCullFace(mode.toInt)

  override def glDeleteTextures(n: Int, textures: IntBuffer): Unit = ops.glDeleteTextures(n, textures)

  override def glDeleteTexture(texture: Int): Unit = ops.glDeleteTexture(texture)

  override def glDepthFunc(func: CompareFunc): Unit = ops.glDepthFunc(func.toInt)

  override def glDepthMask(flag: Boolean): Unit = ops.glDepthMask(flag)

  override def glDepthRangef(zNear: Float, zFar: Float): Unit = ops.glDepthRangef(zNear, zFar)

  override def glDisable(cap: EnableCap): Unit = ops.glDisable(cap.toInt)

  override def glDrawArrays(mode: PrimitiveMode, first: Int, count: Int): Unit =
    ops.glDrawArrays(mode.toInt, first, count)

  override def glDrawElements(mode: PrimitiveMode, count: Int, `type`: DataType, indices: Buffer): Unit =
    ops.glDrawElements(mode.toInt, count, `type`.toInt, indices)

  override def glEnable(cap: EnableCap): Unit = ops.glEnable(cap.toInt)

  override def glFinish(): Unit = ops.glFinish()

  override def glFlush(): Unit = ops.glFlush()

  override def glFrontFace(mode: Int): Unit = ops.glFrontFace(mode)

  override def glGenTextures(n: Int, textures: IntBuffer): Unit = ops.glGenTextures(n, textures)

  override def glGenTexture(): Int = ops.glGenTexture()

  override def glGetError(): Int = ops.glGetError()

  override def glGetIntegerv(pname: Int, params: IntBuffer): Unit = ops.glGetIntegerv(pname, params)

  override def glGetString(name: Int): String = ops.glGetString(name)

  override def glHint(target: Int, mode: Int): Unit = ops.glHint(target, mode)

  override def glLineWidth(width: Float): Unit = ops.glLineWidth(width)

  override def glPixelStorei(pname: Int, param: Int): Unit = ops.glPixelStorei(pname, param)

  override def glPolygonOffset(factor: Float, units: Float): Unit = ops.glPolygonOffset(factor, units)

  override def glReadPixels(x: Pixels, y: Pixels, width: Pixels, height: Pixels, format: PixelFormat, `type`: DataType, pixels: Buffer): Unit =
    ops.glReadPixels(x.toInt, y.toInt, width.toInt, height.toInt, format.toInt, `type`.toInt, pixels)

  override def glScissor(x: Pixels, y: Pixels, width: Pixels, height: Pixels): Unit =
    ops.glScissor(x.toInt, y.toInt, width.toInt, height.toInt)

  override def glStencilFunc(func: CompareFunc, ref: Int, mask: Int): Unit =
    ops.glStencilFunc(func.toInt, ref, mask)

  override def glStencilMask(mask: Int): Unit = ops.glStencilMask(mask)

  override def glStencilOp(fail: StencilOp, zfail: StencilOp, zpass: StencilOp): Unit =
    ops.glStencilOp(fail.toInt, zfail.toInt, zpass.toInt)

  override def glTexImage2D(target: TextureTarget, level: Int, internalformat: Int, width: Pixels, height: Pixels, border: Int, format: PixelFormat, `type`: DataType, pixels: Buffer): Unit =
    ops.glTexImage2D(target.toInt, level, internalformat, width.toInt, height.toInt, border, format.toInt, `type`.toInt, pixels)

  override def glTexParameterf(target: TextureTarget, pname: Int, param: Float): Unit =
    ops.glTexParameterf(target.toInt, pname, param)

  override def glTexSubImage2D(target: TextureTarget, level: Int, xoffset: Pixels, yoffset: Pixels, width: Pixels, height: Pixels, format: PixelFormat, `type`: DataType, pixels: Buffer): Unit =
    ops.glTexSubImage2D(target.toInt, level, xoffset.toInt, yoffset.toInt, width.toInt, height.toInt, format.toInt, `type`.toInt, pixels)

  override def glViewport(x: Pixels, y: Pixels, width: Pixels, height: Pixels): Unit =
    ops.glViewport(x.toInt, y.toInt, width.toInt, height.toInt)

  override def glAttachShader(program: Int, shader: Int): Unit = ops.glAttachShader(program, shader)

  override def glBindAttribLocation(program: Int, index: Int, name: String): Unit =
    ops.glBindAttribLocation(program, index, name)

  override def glBindBuffer(target: BufferTarget, buffer: Int): Unit = ops.glBindBuffer(target.toInt, buffer)

  override def glBindFramebuffer(target: Int, framebuffer: Int): Unit = ops.glBindFramebuffer(target, framebuffer)

  override def glBindRenderbuffer(target: Int, renderbuffer: Int): Unit = ops.glBindRenderbuffer(target, renderbuffer)

  override def glBlendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit =
    ops.glBlendColor(red, green, blue, alpha)

  override def glBlendEquation(mode: BlendEquation): Unit = ops.glBlendEquation(mode.toInt)

  override def glBlendEquationSeparate(modeRGB: BlendEquation, modeAlpha: BlendEquation): Unit =
    ops.glBlendEquationSeparate(modeRGB.toInt, modeAlpha.toInt)

  override def glBlendFuncSeparate(srcRGB: BlendFactor, dstRGB: BlendFactor, srcAlpha: BlendFactor, dstAlpha: BlendFactor): Unit =
    ops.glBlendFuncSeparate(srcRGB.toInt, dstRGB.toInt, srcAlpha.toInt, dstAlpha.toInt)

  override def glBufferData(target: BufferTarget, size: Int, data: Buffer, usage: BufferUsage): Unit =
    ops.glBufferData(target.toInt, size, data, usage.toInt)

  override def glBufferSubData(target: BufferTarget, offset: Int, size: Int, data: Buffer): Unit =
    ops.glBufferSubData(target.toInt, offset, size, data)

  override def glCheckFramebufferStatus(target: Int): Int = ops.glCheckFramebufferStatus(target)

  override def glCompileShader(shader: Int): Unit = ops.glCompileShader(shader)

  override def glCreateProgram(): Int = ops.glCreateProgram()

  override def glCreateShader(`type`: ShaderType): Int = ops.glCreateShader(`type`.toInt)

  override def glDeleteBuffer(buffer: Int): Unit = ops.glDeleteBuffer(buffer)

  override def glDeleteBuffers(n: Int, buffers: IntBuffer): Unit = ops.glDeleteBuffers(n, buffers)

  override def glDeleteFramebuffer(framebuffer: Int): Unit = ops.glDeleteFramebuffer(framebuffer)

  override def glDeleteFramebuffers(n: Int, framebuffers: IntBuffer): Unit = ops.glDeleteFramebuffers(n, framebuffers)

  override def glDeleteProgram(program: Int): Unit = ops.glDeleteProgram(program)

  override def glDeleteRenderbuffer(renderbuffer: Int): Unit = ops.glDeleteRenderbuffer(renderbuffer)

  override def glDeleteRenderbuffers(n: Int, renderbuffers: IntBuffer): Unit =
    ops.glDeleteRenderbuffers(n, renderbuffers)

  override def glDeleteShader(shader: Int): Unit = ops.glDeleteShader(shader)

  override def glDetachShader(program: Int, shader: Int): Unit = ops.glDetachShader(program, shader)

  override def glDisableVertexAttribArray(index: Int): Unit = ops.glDisableVertexAttribArray(index)

  override def glDrawElements(mode: PrimitiveMode, count: Int, `type`: DataType, indices: Int): Unit =
    ops.glDrawElements(mode.toInt, count, `type`.toInt, indices)

  override def glEnableVertexAttribArray(index: Int): Unit = ops.glEnableVertexAttribArray(index)

  override def glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int): Unit =
    ops.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)

  override def glFramebufferTexture2D(target: Int, attachment: Int, textarget: TextureTarget, texture: Int, level: Int): Unit =
    ops.glFramebufferTexture2D(target, attachment, textarget.toInt, texture, level)

  override def glGenBuffer(): Int = ops.glGenBuffer()

  override def glGenBuffers(n: Int, buffers: IntBuffer): Unit = ops.glGenBuffers(n, buffers)

  override def glGenerateMipmap(target: TextureTarget): Unit = ops.glGenerateMipmap(target.toInt)

  override def glGenFramebuffer(): Int = ops.glGenFramebuffer()

  override def glGenFramebuffers(n: Int, framebuffers: IntBuffer): Unit = ops.glGenFramebuffers(n, framebuffers)

  override def glGenRenderbuffer(): Int = ops.glGenRenderbuffer()

  override def glGenRenderbuffers(n: Int, renderbuffers: IntBuffer): Unit = ops.glGenRenderbuffers(n, renderbuffers)

  override def glGetActiveAttrib(program: Int, index: Int, size: IntBuffer, `type`: IntBuffer): String =
    ops.glGetActiveAttrib(program, index, size, `type`)

  override def glGetActiveUniform(program: Int, index: Int, size: IntBuffer, `type`: IntBuffer): String =
    ops.glGetActiveUniform(program, index, size, `type`)

  override def glGetAttachedShaders(program: Int, maxcount: Int, count: Buffer, shaders: IntBuffer): Unit =
    ops.glGetAttachedShaders(program, maxcount, count, shaders)

  override def glGetAttribLocation(program: Int, name: String): Int = ops.glGetAttribLocation(program, name)

  override def glGetBooleanv(pname: Int, params: Buffer): Unit = ops.glGetBooleanv(pname, params)

  override def glGetBufferParameteriv(target: BufferTarget, pname: Int, params: IntBuffer): Unit =
    ops.glGetBufferParameteriv(target.toInt, pname, params)

  override def glGetFloatv(pname: Int, params: FloatBuffer): Unit = ops.glGetFloatv(pname, params)

  override def glGetFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: IntBuffer): Unit =
    ops.glGetFramebufferAttachmentParameteriv(target, attachment, pname, params)

  override def glGetProgramiv(program: Int, pname: Int, params: IntBuffer): Unit =
    ops.glGetProgramiv(program, pname, params)

  override def glGetProgramInfoLog(program: Int): String = ops.glGetProgramInfoLog(program)

  override def glGetRenderbufferParameteriv(target: Int, pname: Int, params: IntBuffer): Unit =
    ops.glGetRenderbufferParameteriv(target, pname, params)

  override def glGetShaderiv(shader: Int, pname: Int, params: IntBuffer): Unit =
    ops.glGetShaderiv(shader, pname, params)

  override def glGetShaderInfoLog(shader: Int): String = ops.glGetShaderInfoLog(shader)

  override def glGetShaderPrecisionFormat(shadertype: ShaderType, precisiontype: Int, range: IntBuffer, precision: IntBuffer): Unit =
    ops.glGetShaderPrecisionFormat(shadertype.toInt, precisiontype, range, precision)

  override def glGetTexParameterfv(target: TextureTarget, pname: Int, params: FloatBuffer): Unit =
    ops.glGetTexParameterfv(target.toInt, pname, params)

  override def glGetTexParameteriv(target: TextureTarget, pname: Int, params: IntBuffer): Unit =
    ops.glGetTexParameteriv(target.toInt, pname, params)

  override def glGetUniformfv(program: Int, location: Int, params: FloatBuffer): Unit =
    ops.glGetUniformfv(program, location, params)

  override def glGetUniformiv(program: Int, location: Int, params: IntBuffer): Unit =
    ops.glGetUniformiv(program, location, params)

  override def glGetUniformLocation(program: Int, name: String): Int = ops.glGetUniformLocation(program, name)

  override def glGetVertexAttribfv(index: Int, pname: Int, params: FloatBuffer): Unit =
    ops.glGetVertexAttribfv(index, pname, params)

  override def glGetVertexAttribiv(index: Int, pname: Int, params: IntBuffer): Unit =
    ops.glGetVertexAttribiv(index, pname, params)

  override def glGetVertexAttribPointerv(index: Int, pname: Int, pointer: Buffer): Unit =
    ops.glGetVertexAttribPointerv(index, pname, pointer)

  override def glIsBuffer(buffer: Int): Boolean = ops.glIsBuffer(buffer)

  override def glIsEnabled(cap: EnableCap): Boolean = ops.glIsEnabled(cap.toInt)

  override def glIsFramebuffer(framebuffer: Int): Boolean = ops.glIsFramebuffer(framebuffer)

  override def glIsProgram(program: Int): Boolean = ops.glIsProgram(program)

  override def glIsRenderbuffer(renderbuffer: Int): Boolean = ops.glIsRenderbuffer(renderbuffer)

  override def glIsShader(shader: Int): Boolean = ops.glIsShader(shader)

  override def glIsTexture(texture: Int): Boolean = ops.glIsTexture(texture)

  override def glLinkProgram(program: Int): Unit = ops.glLinkProgram(program)

  override def glReleaseShaderCompiler(): Unit = ops.glReleaseShaderCompiler()

  override def glRenderbufferStorage(target: Int, internalformat: Int, width: Pixels, height: Pixels): Unit =
    ops.glRenderbufferStorage(target, internalformat, width.toInt, height.toInt)

  override def glSampleCoverage(value: Float, invert: Boolean): Unit = ops.glSampleCoverage(value, invert)

  override def glShaderBinary(n: Int, shaders: IntBuffer, binaryformat: Int, binary: Buffer, length: Int): Unit =
    ops.glShaderBinary(n, shaders, binaryformat, binary, length)

  override def glShaderSource(shader: Int, string: String): Unit = ops.glShaderSource(shader, string)

  override def glStencilFuncSeparate(face: CullFace, func: CompareFunc, ref: Int, mask: Int): Unit =
    ops.glStencilFuncSeparate(face.toInt, func.toInt, ref, mask)

  override def glStencilMaskSeparate(face: CullFace, mask: Int): Unit =
    ops.glStencilMaskSeparate(face.toInt, mask)

  override def glStencilOpSeparate(face: CullFace, fail: StencilOp, zfail: StencilOp, zpass: StencilOp): Unit =
    ops.glStencilOpSeparate(face.toInt, fail.toInt, zfail.toInt, zpass.toInt)

  override def glTexParameterfv(target: TextureTarget, pname: Int, params: FloatBuffer): Unit =
    ops.glTexParameterfv(target.toInt, pname, params)

  override def glTexParameteri(target: TextureTarget, pname: Int, param: Int): Unit =
    ops.glTexParameteri(target.toInt, pname, param)

  override def glTexParameteriv(target: TextureTarget, pname: Int, params: IntBuffer): Unit =
    ops.glTexParameteriv(target.toInt, pname, params)

  override def glUniform1f(location: Int, x: Float): Unit = ops.glUniform1f(location, x)

  override def glUniform1fv(location: Int, count: Int, v: FloatBuffer): Unit = ops.glUniform1fv(location, count, v)

  override def glUniform1fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit =
    ops.glUniform1fv(location, count, v, offset)

  override def glUniform1i(location: Int, x: Int): Unit = ops.glUniform1i(location, x)

  override def glUniform1iv(location: Int, count: Int, v: IntBuffer): Unit = ops.glUniform1iv(location, count, v)

  override def glUniform1iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit =
    ops.glUniform1iv(location, count, v, offset)

  override def glUniform2f(location: Int, x: Float, y: Float): Unit = ops.glUniform2f(location, x, y)

  override def glUniform2fv(location: Int, count: Int, v: FloatBuffer): Unit = ops.glUniform2fv(location, count, v)

  override def glUniform2fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit =
    ops.glUniform2fv(location, count, v, offset)

  override def glUniform2i(location: Int, x: Int, y: Int): Unit = ops.glUniform2i(location, x, y)

  override def glUniform2iv(location: Int, count: Int, v: IntBuffer): Unit = ops.glUniform2iv(location, count, v)

  override def glUniform2iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit =
    ops.glUniform2iv(location, count, v, offset)

  override def glUniform3f(location: Int, x: Float, y: Float, z: Float): Unit = ops.glUniform3f(location, x, y, z)

  override def glUniform3fv(location: Int, count: Int, v: FloatBuffer): Unit = ops.glUniform3fv(location, count, v)

  override def glUniform3fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit =
    ops.glUniform3fv(location, count, v, offset)

  override def glUniform3i(location: Int, x: Int, y: Int, z: Int): Unit = ops.glUniform3i(location, x, y, z)

  override def glUniform3iv(location: Int, count: Int, v: IntBuffer): Unit = ops.glUniform3iv(location, count, v)

  override def glUniform3iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit =
    ops.glUniform3iv(location, count, v, offset)

  override def glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float): Unit =
    ops.glUniform4f(location, x, y, z, w)

  override def glUniform4fv(location: Int, count: Int, v: FloatBuffer): Unit = ops.glUniform4fv(location, count, v)

  override def glUniform4fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit =
    ops.glUniform4fv(location, count, v, offset)

  override def glUniform4i(location: Int, x: Int, y: Int, z: Int, w: Int): Unit =
    ops.glUniform4i(location, x, y, z, w)

  override def glUniform4iv(location: Int, count: Int, v: IntBuffer): Unit = ops.glUniform4iv(location, count, v)

  override def glUniform4iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit =
    ops.glUniform4iv(location, count, v, offset)

  override def glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    ops.glUniformMatrix2fv(location, count, transpose, value)

  override def glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit =
    ops.glUniformMatrix2fv(location, count, transpose, value, offset)

  override def glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    ops.glUniformMatrix3fv(location, count, transpose, value)

  override def glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit =
    ops.glUniformMatrix3fv(location, count, transpose, value, offset)

  override def glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    ops.glUniformMatrix4fv(location, count, transpose, value)

  override def glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit =
    ops.glUniformMatrix4fv(location, count, transpose, value, offset)

  override def glUseProgram(program: Int): Unit = ops.glUseProgram(program)

  override def glValidateProgram(program: Int): Unit = ops.glValidateProgram(program)

  override def glVertexAttrib1f(indx: Int, x: Float): Unit = ops.glVertexAttrib1f(indx, x)

  override def glVertexAttrib1fv(indx: Int, values: FloatBuffer): Unit = ops.glVertexAttrib1fv(indx, values)

  override def glVertexAttrib2f(indx: Int, x: Float, y: Float): Unit = ops.glVertexAttrib2f(indx, x, y)

  override def glVertexAttrib2fv(indx: Int, values: FloatBuffer): Unit = ops.glVertexAttrib2fv(indx, values)

  override def glVertexAttrib3f(indx: Int, x: Float, y: Float, z: Float): Unit =
    ops.glVertexAttrib3f(indx, x, y, z)

  override def glVertexAttrib3fv(indx: Int, values: FloatBuffer): Unit = ops.glVertexAttrib3fv(indx, values)

  override def glVertexAttrib4f(indx: Int, x: Float, y: Float, z: Float, w: Float): Unit =
    ops.glVertexAttrib4f(indx, x, y, z, w)

  override def glVertexAttrib4fv(indx: Int, values: FloatBuffer): Unit = ops.glVertexAttrib4fv(indx, values)

  override def glVertexAttribPointer(indx: Int, size: Int, `type`: DataType, normalized: Boolean, stride: Int, ptr: Buffer): Unit =
    ops.glVertexAttribPointer(indx, size, `type`.toInt, normalized, stride, ptr)

  override def glVertexAttribPointer(indx: Int, size: Int, `type`: DataType, normalized: Boolean, stride: Int, ptr: Int): Unit =
    ops.glVertexAttribPointer(indx, size, `type`.toInt, normalized, stride, ptr)
}
