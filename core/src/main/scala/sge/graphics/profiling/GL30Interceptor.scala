/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/profiling/GL30Interceptor.java
 * Original authors: Daniel Holderbaum, Jan Polák
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: gl30 promoted from protected final field to val constructor param (public)
 *   Convention: check() promoted from private to override protected (abstract in GLInterceptor)
 *   Convention: vertexCount.put(count) → vertexCount.put(count.toFloat) (FloatCounter takes Float)
 *   Idiom: split packages
 *   TODO: typed GL enums -- all GL30 opaque types (passthrough interceptor) -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-03
 */
package sge
package graphics
package profiling

import java.nio.{ Buffer, FloatBuffer, IntBuffer, LongBuffer }

/** @author
  *   Daniel Holderbaum (original implementation)
  * @author
  *   Jan Polák (original implementation)
  */
class GL30Interceptor(glProfiler: GLProfiler, val gl30: GL30) extends GLInterceptor(glProfiler) with GL30 {

  override protected def check(): Unit = {
    var error = gl30.glGetError()
    while (error != GL20.GL_NO_ERROR) {
      glProfiler.listener.onError(error)
      error = gl30.glGetError()
    }
  }

  override def glActiveTexture(texture: Int): Unit = {
    calls += 1
    gl30.glActiveTexture(texture)
    check()
  }

  override def glBindTexture(target: Int, texture: Int): Unit = {
    textureBindings += 1
    calls += 1
    gl30.glBindTexture(target, texture)
    check()
  }

  override def glBlendFunc(sfactor: Int, dfactor: Int): Unit = {
    calls += 1
    gl30.glBlendFunc(sfactor, dfactor)
    check()
  }

  override def glClear(mask: Int): Unit = {
    calls += 1
    gl30.glClear(mask)
    check()
  }

  override def glClearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = {
    calls += 1
    gl30.glClearColor(red, green, blue, alpha)
    check()
  }

  override def glClearDepthf(depth: Float): Unit = {
    calls += 1
    gl30.glClearDepthf(depth)
    check()
  }

  override def glClearStencil(s: Int): Unit = {
    calls += 1
    gl30.glClearStencil(s)
    check()
  }

  override def glColorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit = {
    calls += 1
    gl30.glColorMask(red, green, blue, alpha)
    check()
  }

  override def glCompressedTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, imageSize: Int, data: Buffer): Unit = {
    calls += 1
    gl30.glCompressedTexImage2D(target, level, internalformat, width, height, border, imageSize, data)
    check()
  }

  override def glCompressedTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, imageSize: Int, data: Buffer): Unit = {
    calls += 1
    gl30.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, imageSize, data)
    check()
  }

  override def glCopyTexImage2D(target: Int, level: Int, internalformat: Int, x: Int, y: Int, width: Int, height: Int, border: Int): Unit = {
    calls += 1
    gl30.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border)
    check()
  }

  override def glCopyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit = {
    calls += 1
    gl30.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height)
    check()
  }

  override def glCullFace(mode: Int): Unit = {
    calls += 1
    gl30.glCullFace(mode)
    check()
  }

  override def glDeleteTextures(n: Int, textures: IntBuffer): Unit = {
    calls += 1
    gl30.glDeleteTextures(n, textures)
    check()
  }

  override def glDeleteTexture(texture: Int): Unit = {
    calls += 1
    gl30.glDeleteTexture(texture)
    check()
  }

  override def glDepthFunc(func: Int): Unit = {
    calls += 1
    gl30.glDepthFunc(func)
    check()
  }

  override def glDepthMask(flag: Boolean): Unit = {
    calls += 1
    gl30.glDepthMask(flag)
    check()
  }

  override def glDepthRangef(zNear: Float, zFar: Float): Unit = {
    calls += 1
    gl30.glDepthRangef(zNear, zFar)
    check()
  }

  override def glDisable(cap: Int): Unit = {
    calls += 1
    gl30.glDisable(cap)
    check()
  }

  override def glDrawArrays(mode: Int, first: Int, count: Int): Unit = {
    vertexCount.put(count.toFloat)
    drawCalls += 1
    calls += 1
    gl30.glDrawArrays(mode, first, count)
    check()
  }

  override def glDrawElements(mode: Int, count: Int, `type`: Int, indices: Buffer): Unit = {
    vertexCount.put(count.toFloat)
    drawCalls += 1
    calls += 1
    gl30.glDrawElements(mode, count, `type`, indices)
    check()
  }

  override def glEnable(cap: Int): Unit = {
    calls += 1
    gl30.glEnable(cap)
    check()
  }

  override def glFinish(): Unit = {
    calls += 1
    gl30.glFinish()
    check()
  }

  override def glFlush(): Unit = {
    calls += 1
    gl30.glFlush()
    check()
  }

  override def glFrontFace(mode: Int): Unit = {
    calls += 1
    gl30.glFrontFace(mode)
    check()
  }

  override def glGenTextures(n: Int, textures: IntBuffer): Unit = {
    calls += 1
    gl30.glGenTextures(n, textures)
    check()
  }

  override def glGenTexture(): Int = {
    calls += 1
    val result = gl30.glGenTexture()
    check()
    result
  }

  override def glGetError(): Int = {
    calls += 1
    // Errors by glGetError are undetectable
    gl30.glGetError()
  }

  override def glGetIntegerv(pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl30.glGetIntegerv(pname, params)
    check()
  }

  override def glGetString(name: Int): String = {
    calls += 1
    val result = gl30.glGetString(name)
    check()
    result
  }

  override def glHint(target: Int, mode: Int): Unit = {
    calls += 1
    gl30.glHint(target, mode)
    check()
  }

  override def glLineWidth(width: Float): Unit = {
    calls += 1
    gl30.glLineWidth(width)
    check()
  }

  override def glPixelStorei(pname: Int, param: Int): Unit = {
    calls += 1
    gl30.glPixelStorei(pname, param)
    check()
  }

  override def glPolygonOffset(factor: Float, units: Float): Unit = {
    calls += 1
    gl30.glPolygonOffset(factor, units)
    check()
  }

  override def glReadPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: Buffer): Unit = {
    calls += 1
    gl30.glReadPixels(x, y, width, height, format, `type`, pixels)
    check()
  }

  override def glScissor(x: Int, y: Int, width: Int, height: Int): Unit = {
    calls += 1
    gl30.glScissor(x, y, width, height)
    check()
  }

  override def glStencilFunc(func: Int, ref: Int, mask: Int): Unit = {
    calls += 1
    gl30.glStencilFunc(func, ref, mask)
    check()
  }

  override def glStencilMask(mask: Int): Unit = {
    calls += 1
    gl30.glStencilMask(mask)
    check()
  }

  override def glStencilOp(fail: Int, zfail: Int, zpass: Int): Unit = {
    calls += 1
    gl30.glStencilOp(fail, zfail, zpass)
    check()
  }

  override def glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, `type`: Int, pixels: Buffer): Unit = {
    calls += 1
    gl30.glTexImage2D(target, level, internalformat, width, height, border, format, `type`, pixels)
    check()
  }

  override def glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, `type`: Int, offset: Int): Unit = {
    calls += 1
    gl30.glTexImage2D(target, level, internalformat, width, height, border, format, `type`, offset)
    check()
  }

  override def glTexParameterf(target: Int, pname: Int, param: Float): Unit = {
    calls += 1
    gl30.glTexParameterf(target, pname, param)
    check()
  }

  override def glTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: Buffer): Unit = {
    calls += 1
    gl30.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, `type`, pixels)
    check()
  }

  override def glTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, `type`: Int, offset: Int): Unit = {
    calls += 1
    gl30.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, `type`, offset)
    check()
  }

  override def glViewport(x: Int, y: Int, width: Int, height: Int): Unit = {
    calls += 1
    gl30.glViewport(x, y, width, height)
    check()
  }

  override def glAttachShader(program: Int, shader: Int): Unit = {
    calls += 1
    gl30.glAttachShader(program, shader)
    check()
  }

  override def glBindAttribLocation(program: Int, index: Int, name: String): Unit = {
    calls += 1
    gl30.glBindAttribLocation(program, index, name)
    check()
  }

  override def glBindBuffer(target: Int, buffer: Int): Unit = {
    calls += 1
    gl30.glBindBuffer(target, buffer)
    check()
  }

  override def glBindFramebuffer(target: Int, framebuffer: Int): Unit = {
    calls += 1
    gl30.glBindFramebuffer(target, framebuffer)
    check()
  }

  override def glBindRenderbuffer(target: Int, renderbuffer: Int): Unit = {
    calls += 1
    gl30.glBindRenderbuffer(target, renderbuffer)
    check()
  }

  override def glBlendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = {
    calls += 1
    gl30.glBlendColor(red, green, blue, alpha)
    check()
  }

  override def glBlendEquation(mode: Int): Unit = {
    calls += 1
    gl30.glBlendEquation(mode)
    check()
  }

  override def glBlendEquationSeparate(modeRGB: Int, modeAlpha: Int): Unit = {
    calls += 1
    gl30.glBlendEquationSeparate(modeRGB, modeAlpha)
    check()
  }

  override def glBlendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int): Unit = {
    calls += 1
    gl30.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)
    check()
  }

  override def glBufferData(target: Int, size: Int, data: Buffer, usage: Int): Unit = {
    calls += 1
    gl30.glBufferData(target, size, data, usage)
    check()
  }

  override def glBufferSubData(target: Int, offset: Int, size: Int, data: Buffer): Unit = {
    calls += 1
    gl30.glBufferSubData(target, offset, size, data)
    check()
  }

  override def glCheckFramebufferStatus(target: Int): Int = {
    calls += 1
    val result = gl30.glCheckFramebufferStatus(target)
    check()
    result
  }

  override def glCompileShader(shader: Int): Unit = {
    calls += 1
    gl30.glCompileShader(shader)
    check()
  }

  override def glCreateProgram(): Int = {
    calls += 1
    val result = gl30.glCreateProgram()
    check()
    result
  }

  override def glCreateShader(`type`: Int): Int = {
    calls += 1
    val result = gl30.glCreateShader(`type`)
    check()
    result
  }

  override def glDeleteBuffer(buffer: Int): Unit = {
    calls += 1
    gl30.glDeleteBuffer(buffer)
    check()
  }

  override def glDeleteBuffers(n: Int, buffers: IntBuffer): Unit = {
    calls += 1
    gl30.glDeleteBuffers(n, buffers)
    check()
  }

  override def glDeleteFramebuffer(framebuffer: Int): Unit = {
    calls += 1
    gl30.glDeleteFramebuffer(framebuffer)
    check()
  }

  override def glDeleteFramebuffers(n: Int, framebuffers: IntBuffer): Unit = {
    calls += 1
    gl30.glDeleteFramebuffers(n, framebuffers)
    check()
  }

  override def glDeleteProgram(program: Int): Unit = {
    calls += 1
    gl30.glDeleteProgram(program)
    check()
  }

  override def glDeleteRenderbuffer(renderbuffer: Int): Unit = {
    calls += 1
    gl30.glDeleteRenderbuffer(renderbuffer)
    check()
  }

  override def glDeleteRenderbuffers(n: Int, renderbuffers: IntBuffer): Unit = {
    calls += 1
    gl30.glDeleteRenderbuffers(n, renderbuffers)
    check()
  }

  override def glDeleteShader(shader: Int): Unit = {
    calls += 1
    gl30.glDeleteShader(shader)
    check()
  }

  override def glDetachShader(program: Int, shader: Int): Unit = {
    calls += 1
    gl30.glDetachShader(program, shader)
    check()
  }

  override def glDisableVertexAttribArray(index: Int): Unit = {
    calls += 1
    gl30.glDisableVertexAttribArray(index)
    check()
  }

  override def glDrawElements(mode: Int, count: Int, `type`: Int, indices: Int): Unit = {
    vertexCount.put(count.toFloat)
    drawCalls += 1
    calls += 1
    gl30.glDrawElements(mode, count, `type`, indices)
    check()
  }

  override def glEnableVertexAttribArray(index: Int): Unit = {
    calls += 1
    gl30.glEnableVertexAttribArray(index)
    check()
  }

  override def glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int): Unit = {
    calls += 1
    gl30.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)
    check()
  }

  override def glFramebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int): Unit = {
    calls += 1
    gl30.glFramebufferTexture2D(target, attachment, textarget, texture, level)
    check()
  }

  override def glGenBuffer(): Int = {
    calls += 1
    val result = gl30.glGenBuffer()
    check()
    result
  }

  override def glGenBuffers(n: Int, buffers: IntBuffer): Unit = {
    calls += 1
    gl30.glGenBuffers(n, buffers)
    check()
  }

  override def glGenerateMipmap(target: Int): Unit = {
    calls += 1
    gl30.glGenerateMipmap(target)
    check()
  }

  override def glGenFramebuffer(): Int = {
    calls += 1
    val result = gl30.glGenFramebuffer()
    check()
    result
  }

  override def glGenFramebuffers(n: Int, framebuffers: IntBuffer): Unit = {
    calls += 1
    gl30.glGenFramebuffers(n, framebuffers)
    check()
  }

  override def glGenRenderbuffer(): Int = {
    calls += 1
    val result = gl30.glGenRenderbuffer()
    check()
    result
  }

  override def glGenRenderbuffers(n: Int, renderbuffers: IntBuffer): Unit = {
    calls += 1
    gl30.glGenRenderbuffers(n, renderbuffers)
    check()
  }

  override def glGetActiveAttrib(program: Int, index: Int, size: IntBuffer, `type`: IntBuffer): String = {
    calls += 1
    val result = gl30.glGetActiveAttrib(program, index, size, `type`)
    check()
    result
  }

  override def glGetActiveUniform(program: Int, index: Int, size: IntBuffer, `type`: IntBuffer): String = {
    calls += 1
    val result = gl30.glGetActiveUniform(program, index, size, `type`)
    check()
    result
  }

  override def glGetAttachedShaders(program: Int, maxcount: Int, count: Buffer, shaders: IntBuffer): Unit = {
    calls += 1
    gl30.glGetAttachedShaders(program, maxcount, count, shaders)
    check()
  }

  override def glGetAttribLocation(program: Int, name: String): Int = {
    calls += 1
    val result = gl30.glGetAttribLocation(program, name)
    check()
    result
  }

  override def glGetBooleanv(pname: Int, params: Buffer): Unit = {
    calls += 1
    gl30.glGetBooleanv(pname, params)
    check()
  }

  override def glGetBufferParameteriv(target: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl30.glGetBufferParameteriv(target, pname, params)
    check()
  }

  override def glGetFloatv(pname: Int, params: FloatBuffer): Unit = {
    calls += 1
    gl30.glGetFloatv(pname, params)
    check()
  }

  override def glGetFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl30.glGetFramebufferAttachmentParameteriv(target, attachment, pname, params)
    check()
  }

  override def glGetProgramiv(program: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl30.glGetProgramiv(program, pname, params)
    check()
  }

  override def glGetProgramInfoLog(program: Int): String = {
    calls += 1
    val result = gl30.glGetProgramInfoLog(program)
    check()
    result
  }

  override def glGetRenderbufferParameteriv(target: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl30.glGetRenderbufferParameteriv(target, pname, params)
    check()
  }

  override def glGetShaderiv(shader: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl30.glGetShaderiv(shader, pname, params)
    check()
  }

  override def glGetShaderInfoLog(shader: Int): String = {
    calls += 1
    val result = gl30.glGetShaderInfoLog(shader)
    check()
    result
  }

  override def glGetShaderPrecisionFormat(shadertype: Int, precisiontype: Int, range: IntBuffer, precision: IntBuffer): Unit = {
    calls += 1
    gl30.glGetShaderPrecisionFormat(shadertype, precisiontype, range, precision)
    check()
  }

  override def glGetTexParameterfv(target: Int, pname: Int, params: FloatBuffer): Unit = {
    calls += 1
    gl30.glGetTexParameterfv(target, pname, params)
    check()
  }

  override def glGetTexParameteriv(target: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl30.glGetTexParameteriv(target, pname, params)
    check()
  }

  override def glGetUniformfv(program: Int, location: Int, params: FloatBuffer): Unit = {
    calls += 1
    gl30.glGetUniformfv(program, location, params)
    check()
  }

  override def glGetUniformiv(program: Int, location: Int, params: IntBuffer): Unit = {
    calls += 1
    gl30.glGetUniformiv(program, location, params)
    check()
  }

  override def glGetUniformLocation(program: Int, name: String): Int = {
    calls += 1
    val result = gl30.glGetUniformLocation(program, name)
    check()
    result
  }

  override def glGetVertexAttribfv(index: Int, pname: Int, params: FloatBuffer): Unit = {
    calls += 1
    gl30.glGetVertexAttribfv(index, pname, params)
    check()
  }

  override def glGetVertexAttribiv(index: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl30.glGetVertexAttribiv(index, pname, params)
    check()
  }

  override def glGetVertexAttribPointerv(index: Int, pname: Int, pointer: Buffer): Unit = {
    calls += 1
    gl30.glGetVertexAttribPointerv(index, pname, pointer)
    check()
  }

  override def glIsBuffer(buffer: Int): Boolean = {
    calls += 1
    val result = gl30.glIsBuffer(buffer)
    check()
    result
  }

  override def glIsEnabled(cap: Int): Boolean = {
    calls += 1
    val result = gl30.glIsEnabled(cap)
    check()
    result
  }

  override def glIsFramebuffer(framebuffer: Int): Boolean = {
    calls += 1
    val result = gl30.glIsFramebuffer(framebuffer)
    check()
    result
  }

  override def glIsProgram(program: Int): Boolean = {
    calls += 1
    val result = gl30.glIsProgram(program)
    check()
    result
  }

  override def glIsRenderbuffer(renderbuffer: Int): Boolean = {
    calls += 1
    val result = gl30.glIsRenderbuffer(renderbuffer)
    check()
    result
  }

  override def glIsShader(shader: Int): Boolean = {
    calls += 1
    val result = gl30.glIsShader(shader)
    check()
    result
  }

  override def glIsTexture(texture: Int): Boolean = {
    calls += 1
    val result = gl30.glIsTexture(texture)
    check()
    result
  }

  override def glLinkProgram(program: Int): Unit = {
    calls += 1
    gl30.glLinkProgram(program)
    check()
  }

  override def glReleaseShaderCompiler(): Unit = {
    calls += 1
    gl30.glReleaseShaderCompiler()
    check()
  }

  override def glRenderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int): Unit = {
    calls += 1
    gl30.glRenderbufferStorage(target, internalformat, width, height)
    check()
  }

  override def glSampleCoverage(value: Float, invert: Boolean): Unit = {
    calls += 1
    gl30.glSampleCoverage(value, invert)
    check()
  }

  override def glShaderBinary(n: Int, shaders: IntBuffer, binaryformat: Int, binary: Buffer, length: Int): Unit = {
    calls += 1
    gl30.glShaderBinary(n, shaders, binaryformat, binary, length)
    check()
  }

  override def glShaderSource(shader: Int, string: String): Unit = {
    calls += 1
    gl30.glShaderSource(shader, string)
    check()
  }

  override def glStencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int): Unit = {
    calls += 1
    gl30.glStencilFuncSeparate(face, func, ref, mask)
    check()
  }

  override def glStencilMaskSeparate(face: Int, mask: Int): Unit = {
    calls += 1
    gl30.glStencilMaskSeparate(face, mask)
    check()
  }

  override def glStencilOpSeparate(face: Int, fail: Int, zfail: Int, zpass: Int): Unit = {
    calls += 1
    gl30.glStencilOpSeparate(face, fail, zfail, zpass)
    check()
  }

  override def glTexParameterfv(target: Int, pname: Int, params: FloatBuffer): Unit = {
    calls += 1
    gl30.glTexParameterfv(target, pname, params)
    check()
  }

  override def glTexParameteri(target: Int, pname: Int, param: Int): Unit = {
    calls += 1
    gl30.glTexParameteri(target, pname, param)
    check()
  }

  override def glTexParameteriv(target: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl30.glTexParameteriv(target, pname, params)
    check()
  }

  override def glUniform1f(location: Int, x: Float): Unit = {
    calls += 1
    gl30.glUniform1f(location, x)
    check()
  }

  override def glUniform1fv(location: Int, count: Int, v: FloatBuffer): Unit = {
    calls += 1
    gl30.glUniform1fv(location, count, v)
    check()
  }

  override def glUniform1fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit = {
    calls += 1
    gl30.glUniform1fv(location, count, v, offset)
    check()
  }

  override def glUniform1i(location: Int, x: Int): Unit = {
    calls += 1
    gl30.glUniform1i(location, x)
    check()
  }

  override def glUniform1iv(location: Int, count: Int, v: IntBuffer): Unit = {
    calls += 1
    gl30.glUniform1iv(location, count, v)
    check()
  }

  override def glUniform1iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit = {
    calls += 1
    gl30.glUniform1iv(location, count, v, offset)
    check()
  }

  override def glUniform2f(location: Int, x: Float, y: Float): Unit = {
    calls += 1
    gl30.glUniform2f(location, x, y)
    check()
  }

  override def glUniform2fv(location: Int, count: Int, v: FloatBuffer): Unit = {
    calls += 1
    gl30.glUniform2fv(location, count, v)
    check()
  }

  override def glUniform2fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit = {
    calls += 1
    gl30.glUniform2fv(location, count, v, offset)
    check()
  }

  override def glUniform2i(location: Int, x: Int, y: Int): Unit = {
    calls += 1
    gl30.glUniform2i(location, x, y)
    check()
  }

  override def glUniform2iv(location: Int, count: Int, v: IntBuffer): Unit = {
    calls += 1
    gl30.glUniform2iv(location, count, v)
    check()
  }

  override def glUniform2iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit = {
    calls += 1
    gl30.glUniform2iv(location, count, v, offset)
    check()
  }

  override def glUniform3f(location: Int, x: Float, y: Float, z: Float): Unit = {
    calls += 1
    gl30.glUniform3f(location, x, y, z)
    check()
  }

  override def glUniform3fv(location: Int, count: Int, v: FloatBuffer): Unit = {
    calls += 1
    gl30.glUniform3fv(location, count, v)
    check()
  }

  override def glUniform3fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit = {
    calls += 1
    gl30.glUniform3fv(location, count, v, offset)
    check()
  }

  override def glUniform3i(location: Int, x: Int, y: Int, z: Int): Unit = {
    calls += 1
    gl30.glUniform3i(location, x, y, z)
    check()
  }

  override def glUniform3iv(location: Int, count: Int, v: IntBuffer): Unit = {
    calls += 1
    gl30.glUniform3iv(location, count, v)
    check()
  }

  override def glUniform3iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit = {
    calls += 1
    gl30.glUniform3iv(location, count, v, offset)
    check()
  }

  override def glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float): Unit = {
    calls += 1
    gl30.glUniform4f(location, x, y, z, w)
    check()
  }

  override def glUniform4fv(location: Int, count: Int, v: FloatBuffer): Unit = {
    calls += 1
    gl30.glUniform4fv(location, count, v)
    check()
  }

  override def glUniform4fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit = {
    calls += 1
    gl30.glUniform4fv(location, count, v, offset)
    check()
  }

  override def glUniform4i(location: Int, x: Int, y: Int, z: Int, w: Int): Unit = {
    calls += 1
    gl30.glUniform4i(location, x, y, z, w)
    check()
  }

  override def glUniform4iv(location: Int, count: Int, v: IntBuffer): Unit = {
    calls += 1
    gl30.glUniform4iv(location, count, v)
    check()
  }

  override def glUniform4iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit = {
    calls += 1
    gl30.glUniform4iv(location, count, v, offset)
    check()
  }

  override def glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = {
    calls += 1
    gl30.glUniformMatrix2fv(location, count, transpose, value)
    check()
  }

  override def glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit = {
    calls += 1
    gl30.glUniformMatrix2fv(location, count, transpose, value, offset)
    check()
  }

  override def glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = {
    calls += 1
    gl30.glUniformMatrix3fv(location, count, transpose, value)
    check()
  }

  override def glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit = {
    calls += 1
    gl30.glUniformMatrix3fv(location, count, transpose, value, offset)
    check()
  }

  override def glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = {
    calls += 1
    gl30.glUniformMatrix4fv(location, count, transpose, value)
    check()
  }

  override def glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit = {
    calls += 1
    gl30.glUniformMatrix4fv(location, count, transpose, value, offset)
    check()
  }

  override def glUseProgram(program: Int): Unit = {
    shaderSwitches += 1
    calls += 1
    gl30.glUseProgram(program)
    check()
  }

  override def glValidateProgram(program: Int): Unit = {
    calls += 1
    gl30.glValidateProgram(program)
    check()
  }

  override def glVertexAttrib1f(indx: Int, x: Float): Unit = {
    calls += 1
    gl30.glVertexAttrib1f(indx, x)
    check()
  }

  override def glVertexAttrib1fv(indx: Int, values: FloatBuffer): Unit = {
    calls += 1
    gl30.glVertexAttrib1fv(indx, values)
    check()
  }

  override def glVertexAttrib2f(indx: Int, x: Float, y: Float): Unit = {
    calls += 1
    gl30.glVertexAttrib2f(indx, x, y)
    check()
  }

  override def glVertexAttrib2fv(indx: Int, values: FloatBuffer): Unit = {
    calls += 1
    gl30.glVertexAttrib2fv(indx, values)
    check()
  }

  override def glVertexAttrib3f(indx: Int, x: Float, y: Float, z: Float): Unit = {
    calls += 1
    gl30.glVertexAttrib3f(indx, x, y, z)
    check()
  }

  override def glVertexAttrib3fv(indx: Int, values: FloatBuffer): Unit = {
    calls += 1
    gl30.glVertexAttrib3fv(indx, values)
    check()
  }

  override def glVertexAttrib4f(indx: Int, x: Float, y: Float, z: Float, w: Float): Unit = {
    calls += 1
    gl30.glVertexAttrib4f(indx, x, y, z, w)
    check()
  }

  override def glVertexAttrib4fv(indx: Int, values: FloatBuffer): Unit = {
    calls += 1
    gl30.glVertexAttrib4fv(indx, values)
    check()
  }

  override def glVertexAttribPointer(indx: Int, size: Int, `type`: Int, normalized: Boolean, stride: Int, ptr: Buffer): Unit = {
    calls += 1
    gl30.glVertexAttribPointer(indx, size, `type`, normalized, stride, ptr)
    check()
  }

  override def glVertexAttribPointer(indx: Int, size: Int, `type`: Int, normalized: Boolean, stride: Int, ptr: Int): Unit = {
    calls += 1
    gl30.glVertexAttribPointer(indx, size, `type`, normalized, stride, ptr)
    check()
  }

  // GL30 Unique

  override def glReadBuffer(mode: Int): Unit = {
    calls += 1
    gl30.glReadBuffer(mode)
    check()
  }

  override def glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, `type`: Int, indices: Buffer): Unit = {
    vertexCount.put(count.toFloat)
    drawCalls += 1
    calls += 1
    gl30.glDrawRangeElements(mode, start, end, count, `type`, indices)
    check()
  }

  override def glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, `type`: Int, offset: Int): Unit = {
    vertexCount.put(count.toFloat)
    drawCalls += 1
    calls += 1
    gl30.glDrawRangeElements(mode, start, end, count, `type`, offset)
    check()
  }

  override def glTexImage3D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: Int, `type`: Int, pixels: Buffer): Unit = {
    calls += 1
    gl30.glTexImage3D(target, level, internalformat, width, height, depth, border, format, `type`, pixels)
    check()
  }

  override def glTexImage3D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: Int, `type`: Int, offset: Int): Unit = {
    calls += 1
    gl30.glTexImage3D(target, level, internalformat, width, height, depth, border, format, `type`, offset)
    check()
  }

  override def glTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int, format: Int, `type`: Int, pixels: Buffer): Unit = {
    calls += 1
    gl30.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, `type`, pixels)
    check()
  }

  override def glTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int, format: Int, `type`: Int, offset: Int): Unit = {
    calls += 1
    gl30.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, `type`, offset)
    check()
  }

  override def glCopyTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit = {
    calls += 1
    gl30.glCopyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height)
    check()
  }

  override def glGenQueries(n: Int, ids: Array[Int], offset: Int): Unit = {
    calls += 1
    gl30.glGenQueries(n, ids, offset)
    check()
  }

  override def glGenQueries(n: Int, ids: IntBuffer): Unit = {
    calls += 1
    gl30.glGenQueries(n, ids)
    check()
  }

  override def glDeleteQueries(n: Int, ids: Array[Int], offset: Int): Unit = {
    calls += 1
    gl30.glDeleteQueries(n, ids, offset)
    check()
  }

  override def glDeleteQueries(n: Int, ids: IntBuffer): Unit = {
    calls += 1
    gl30.glDeleteQueries(n, ids)
    check()
  }

  override def glIsQuery(id: Int): Boolean = {
    calls += 1
    val result = gl30.glIsQuery(id)
    check()
    result
  }

  override def glBeginQuery(target: Int, id: Int): Unit = {
    calls += 1
    gl30.glBeginQuery(target, id)
    check()
  }

  override def glEndQuery(target: Int): Unit = {
    calls += 1
    gl30.glEndQuery(target)
    check()
  }

  override def glGetQueryiv(target: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl30.glGetQueryiv(target, pname, params)
    check()
  }

  override def glGetQueryObjectuiv(id: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl30.glGetQueryObjectuiv(id, pname, params)
    check()
  }

  override def glUnmapBuffer(target: Int): Boolean = {
    calls += 1
    val result = gl30.glUnmapBuffer(target)
    check()
    result
  }

  override def glGetBufferPointerv(target: Int, pname: Int): Buffer = {
    calls += 1
    val result = gl30.glGetBufferPointerv(target, pname)
    check()
    result
  }

  override def glDrawBuffers(n: Int, bufs: IntBuffer): Unit = {
    drawCalls += 1
    calls += 1
    gl30.glDrawBuffers(n, bufs)
    check()
  }

  override def glUniformMatrix2x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = {
    calls += 1
    gl30.glUniformMatrix2x3fv(location, count, transpose, value)
    check()
  }

  override def glUniformMatrix3x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = {
    calls += 1
    gl30.glUniformMatrix3x2fv(location, count, transpose, value)
    check()
  }

  override def glUniformMatrix2x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = {
    calls += 1
    gl30.glUniformMatrix2x4fv(location, count, transpose, value)
    check()
  }

  override def glUniformMatrix4x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = {
    calls += 1
    gl30.glUniformMatrix4x2fv(location, count, transpose, value)
    check()
  }

  override def glUniformMatrix3x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = {
    calls += 1
    gl30.glUniformMatrix3x4fv(location, count, transpose, value)
    check()
  }

  override def glUniformMatrix4x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = {
    calls += 1
    gl30.glUniformMatrix4x3fv(location, count, transpose, value)
    check()
  }

  override def glBlitFramebuffer(srcX0: Int, srcY0: Int, srcX1: Int, srcY1: Int, dstX0: Int, dstY0: Int, dstX1: Int, dstY1: Int, mask: Int, filter: Int): Unit = {
    calls += 1
    gl30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter)
    check()
  }

  override def glRenderbufferStorageMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int): Unit = {
    calls += 1
    gl30.glRenderbufferStorageMultisample(target, samples, internalformat, width, height)
    check()
  }

  override def glFramebufferTextureLayer(target: Int, attachment: Int, texture: Int, level: Int, layer: Int): Unit = {
    calls += 1
    gl30.glFramebufferTextureLayer(target, attachment, texture, level, layer)
    check()
  }

  override def glMapBufferRange(target: Int, offset: Int, length: Int, access: Int): Buffer = {
    calls += 1
    val result = gl30.glMapBufferRange(target, offset, length, access)
    check()
    result
  }

  override def glFlushMappedBufferRange(target: Int, offset: Int, length: Int): Unit = {
    calls += 1
    gl30.glFlushMappedBufferRange(target, offset, length)
    check()
  }

  override def glBindVertexArray(array: Int): Unit = {
    calls += 1
    gl30.glBindVertexArray(array)
    check()
  }

  override def glDeleteVertexArrays(n: Int, arrays: Array[Int], offset: Int): Unit = {
    calls += 1
    gl30.glDeleteVertexArrays(n, arrays, offset)
    check()
  }

  override def glDeleteVertexArrays(n: Int, arrays: IntBuffer): Unit = {
    calls += 1
    gl30.glDeleteVertexArrays(n, arrays)
    check()
  }

  override def glGenVertexArrays(n: Int, arrays: Array[Int], offset: Int): Unit = {
    calls += 1
    gl30.glGenVertexArrays(n, arrays, offset)
    check()
  }

  override def glGenVertexArrays(n: Int, arrays: IntBuffer): Unit = {
    calls += 1
    gl30.glGenVertexArrays(n, arrays)
    check()
  }

  override def glIsVertexArray(array: Int): Boolean = {
    calls += 1
    val result = gl30.glIsVertexArray(array)
    check()
    result
  }

  override def glBeginTransformFeedback(primitiveMode: Int): Unit = {
    calls += 1
    gl30.glBeginTransformFeedback(primitiveMode)
    check()
  }

  override def glEndTransformFeedback(): Unit = {
    calls += 1
    gl30.glEndTransformFeedback()
    check()
  }

  override def glBindBufferRange(target: Int, index: Int, buffer: Int, offset: Int, size: Int): Unit = {
    calls += 1
    gl30.glBindBufferRange(target, index, buffer, offset, size)
    check()
  }

  override def glBindBufferBase(target: Int, index: Int, buffer: Int): Unit = {
    calls += 1
    gl30.glBindBufferBase(target, index, buffer)
    check()
  }

  override def glTransformFeedbackVaryings(program: Int, varyings: Array[String], bufferMode: Int): Unit = {
    calls += 1
    gl30.glTransformFeedbackVaryings(program, varyings, bufferMode)
    check()
  }

  override def glVertexAttribIPointer(index: Int, size: Int, `type`: Int, stride: Int, offset: Int): Unit = {
    calls += 1
    gl30.glVertexAttribIPointer(index, size, `type`, stride, offset)
    check()
  }

  override def glGetVertexAttribIiv(index: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl30.glGetVertexAttribIiv(index, pname, params)
    check()
  }

  override def glGetVertexAttribIuiv(index: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl30.glGetVertexAttribIuiv(index, pname, params)
    check()
  }

  override def glVertexAttribI4i(index: Int, x: Int, y: Int, z: Int, w: Int): Unit = {
    calls += 1
    gl30.glVertexAttribI4i(index, x, y, z, w)
    check()
  }

  override def glVertexAttribI4ui(index: Int, x: Int, y: Int, z: Int, w: Int): Unit = {
    calls += 1
    gl30.glVertexAttribI4ui(index, x, y, z, w)
    check()
  }

  override def glGetUniformuiv(program: Int, location: Int, params: IntBuffer): Unit = {
    calls += 1
    gl30.glGetUniformuiv(program, location, params)
    check()
  }

  override def glGetFragDataLocation(program: Int, name: String): Int = {
    calls += 1
    val result = gl30.glGetFragDataLocation(program, name)
    check()
    result
  }

  override def glUniform1uiv(location: Int, count: Int, value: IntBuffer): Unit = {
    calls += 1
    gl30.glUniform1uiv(location, count, value)
    check()
  }

  override def glUniform3uiv(location: Int, count: Int, value: IntBuffer): Unit = {
    calls += 1
    gl30.glUniform3uiv(location, count, value)
    check()
  }

  override def glUniform4uiv(location: Int, count: Int, value: IntBuffer): Unit = {
    calls += 1
    gl30.glUniform4uiv(location, count, value)
    check()
  }

  override def glClearBufferiv(buffer: Int, drawbuffer: Int, value: IntBuffer): Unit = {
    calls += 1
    gl30.glClearBufferiv(buffer, drawbuffer, value)
    check()
  }

  override def glClearBufferuiv(buffer: Int, drawbuffer: Int, value: IntBuffer): Unit = {
    calls += 1
    gl30.glClearBufferuiv(buffer, drawbuffer, value)
    check()
  }

  override def glClearBufferfv(buffer: Int, drawbuffer: Int, value: FloatBuffer): Unit = {
    calls += 1
    gl30.glClearBufferfv(buffer, drawbuffer, value)
    check()
  }

  override def glClearBufferfi(buffer: Int, drawbuffer: Int, depth: Float, stencil: Int): Unit = {
    calls += 1
    gl30.glClearBufferfi(buffer, drawbuffer, depth, stencil)
    check()
  }

  override def glGetStringi(name: Int, index: Int): String = {
    calls += 1
    val result = gl30.glGetStringi(name, index)
    check()
    result
  }

  override def glCopyBufferSubData(readTarget: Int, writeTarget: Int, readOffset: Int, writeOffset: Int, size: Int): Unit = {
    calls += 1
    gl30.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size)
    check()
  }

  override def glGetUniformIndices(program: Int, uniformNames: Array[String], uniformIndices: IntBuffer): Unit = {
    calls += 1
    gl30.glGetUniformIndices(program, uniformNames, uniformIndices)
    check()
  }

  override def glGetActiveUniformsiv(program: Int, uniformCount: Int, uniformIndices: IntBuffer, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl30.glGetActiveUniformsiv(program, uniformCount, uniformIndices, pname, params)
    check()
  }

  override def glGetUniformBlockIndex(program: Int, uniformBlockName: String): Int = {
    calls += 1
    val result = gl30.glGetUniformBlockIndex(program, uniformBlockName)
    check()
    result
  }

  override def glGetActiveUniformBlockiv(program: Int, uniformBlockIndex: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl30.glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, params)
    check()
  }

  override def glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int, length: Buffer, uniformBlockName: Buffer): Unit = {
    calls += 1
    gl30.glGetActiveUniformBlockName(program, uniformBlockIndex, length, uniformBlockName)
    check()
  }

  override def glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int): String = {
    calls += 1
    val result = gl30.glGetActiveUniformBlockName(program, uniformBlockIndex)
    check()
    result
  }

  override def glUniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int): Unit = {
    calls += 1
    gl30.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding)
    check()
  }

  override def glDrawArraysInstanced(mode: Int, first: Int, count: Int, instanceCount: Int): Unit = {
    vertexCount.put(count.toFloat)
    drawCalls += 1
    calls += 1
    gl30.glDrawArraysInstanced(mode, first, count, instanceCount)
    check()
  }

  override def glDrawElementsInstanced(mode: Int, count: Int, `type`: Int, indicesOffset: Int, instanceCount: Int): Unit = {
    vertexCount.put(count.toFloat)
    drawCalls += 1
    calls += 1
    gl30.glDrawElementsInstanced(mode, count, `type`, indicesOffset, instanceCount)
    check()
  }

  override def glGetInteger64v(pname: Int, params: LongBuffer): Unit = {
    calls += 1
    gl30.glGetInteger64v(pname, params)
    check()
  }

  override def glGetBufferParameteri64v(target: Int, pname: Int, params: LongBuffer): Unit = {
    calls += 1
    gl30.glGetBufferParameteri64v(target, pname, params)
    check()
  }

  override def glGenSamplers(count: Int, samplers: Array[Int], offset: Int): Unit = {
    calls += 1
    gl30.glGenSamplers(count, samplers, offset)
    check()
  }

  override def glGenSamplers(count: Int, samplers: IntBuffer): Unit = {
    calls += 1
    gl30.glGenSamplers(count, samplers)
    check()
  }

  override def glDeleteSamplers(count: Int, samplers: Array[Int], offset: Int): Unit = {
    calls += 1
    gl30.glDeleteSamplers(count, samplers, offset)
    check()
  }

  override def glDeleteSamplers(count: Int, samplers: IntBuffer): Unit = {
    calls += 1
    gl30.glDeleteSamplers(count, samplers)
    check()
  }

  override def glIsSampler(sampler: Int): Boolean = {
    calls += 1
    val result = gl30.glIsSampler(sampler)
    check()
    result
  }

  override def glBindSampler(unit: Int, sampler: Int): Unit = {
    calls += 1
    gl30.glBindSampler(unit, sampler)
    check()
  }

  override def glSamplerParameteri(sampler: Int, pname: Int, param: Int): Unit = {
    calls += 1
    gl30.glSamplerParameteri(sampler, pname, param)
    check()
  }

  override def glSamplerParameteriv(sampler: Int, pname: Int, param: IntBuffer): Unit = {
    calls += 1
    gl30.glSamplerParameteriv(sampler, pname, param)
    check()
  }

  override def glSamplerParameterf(sampler: Int, pname: Int, param: Float): Unit = {
    calls += 1
    gl30.glSamplerParameterf(sampler, pname, param)
    check()
  }

  override def glSamplerParameterfv(sampler: Int, pname: Int, param: FloatBuffer): Unit = {
    calls += 1
    gl30.glSamplerParameterfv(sampler, pname, param)
    check()
  }

  override def glGetSamplerParameteriv(sampler: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl30.glGetSamplerParameteriv(sampler, pname, params)
    check()
  }

  override def glGetSamplerParameterfv(sampler: Int, pname: Int, params: FloatBuffer): Unit = {
    calls += 1
    gl30.glGetSamplerParameterfv(sampler, pname, params)
    check()
  }

  override def glVertexAttribDivisor(index: Int, divisor: Int): Unit = {
    calls += 1
    gl30.glVertexAttribDivisor(index, divisor)
    check()
  }

  override def glBindTransformFeedback(target: Int, id: Int): Unit = {
    calls += 1
    gl30.glBindTransformFeedback(target, id)
    check()
  }

  override def glDeleteTransformFeedbacks(n: Int, ids: Array[Int], offset: Int): Unit = {
    calls += 1
    gl30.glDeleteTransformFeedbacks(n, ids, offset)
    check()
  }

  override def glDeleteTransformFeedbacks(n: Int, ids: IntBuffer): Unit = {
    calls += 1
    gl30.glDeleteTransformFeedbacks(n, ids)
    check()
  }

  override def glGenTransformFeedbacks(n: Int, ids: Array[Int], offset: Int): Unit = {
    calls += 1
    gl30.glGenTransformFeedbacks(n, ids, offset)
    check()
  }

  override def glGenTransformFeedbacks(n: Int, ids: IntBuffer): Unit = {
    calls += 1
    gl30.glGenTransformFeedbacks(n, ids)
    check()
  }

  override def glIsTransformFeedback(id: Int): Boolean = {
    calls += 1
    val result = gl30.glIsTransformFeedback(id)
    check()
    result
  }

  override def glPauseTransformFeedback(): Unit = {
    calls += 1
    gl30.glPauseTransformFeedback()
    check()
  }

  override def glResumeTransformFeedback(): Unit = {
    calls += 1
    gl30.glResumeTransformFeedback()
    check()
  }

  override def glProgramParameteri(program: Int, pname: Int, value: Int): Unit = {
    calls += 1
    gl30.glProgramParameteri(program, pname, value)
    check()
  }

  override def glInvalidateFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer): Unit = {
    calls += 1
    gl30.glInvalidateFramebuffer(target, numAttachments, attachments)
    check()
  }

  override def glInvalidateSubFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer, x: Int, y: Int, width: Int, height: Int): Unit = {
    calls += 1
    gl30.glInvalidateSubFramebuffer(target, numAttachments, attachments, x, y, width, height)
    check()
  }
}
