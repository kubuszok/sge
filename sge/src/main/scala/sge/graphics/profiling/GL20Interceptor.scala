/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/profiling/GL20Interceptor.java
 * Original authors: Daniel Holderbaum, Jan Polák
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: gl20 promoted from protected final field to val constructor param (public)
 *   Convention: check() promoted from private to override protected (abstract in GLInterceptor)
 *   Convention: vertexCount.put(count) → vertexCount.put(count.toFloat) (FloatCounter takes Float)
 *   Idiom: split packages
 *   Idiom: opaque GL enum types (TextureTarget, BlendFactor, BlendEquation, CompareFunc, StencilOp, PrimitiveMode, BufferTarget, BufferUsage, ShaderType, PixelFormat, DataType, ClearMask, CullFace, EnableCap) for method params -- see GLEnum.scala
 *   Audited: 2026-03-03
 */
package sge
package graphics
package profiling

import java.nio.{ Buffer, FloatBuffer, IntBuffer }

/** @author
  *   Daniel Holderbaum (original implementation)
  * @author
  *   Jan Polák (original implementation)
  */
class GL20Interceptor(glProfiler: GLProfiler, val gl20: GL20) extends GLInterceptor(glProfiler) with GL20 {

  override protected def check(): Unit = {
    var error = gl20.glGetError()
    while (error != GL20.GL_NO_ERROR) {
      glProfiler.listener.onError(error)
      error = gl20.glGetError()
    }
  }

  override def glActiveTexture(texture: Int): Unit = {
    calls += 1
    gl20.glActiveTexture(texture)
    check()
  }

  override def glBindTexture(target: TextureTarget, texture: Int): Unit = {
    textureBindings += 1
    calls += 1
    gl20.glBindTexture(target, texture)
    check()
  }

  override def glBlendFunc(sfactor: BlendFactor, dfactor: BlendFactor): Unit = {
    calls += 1
    gl20.glBlendFunc(sfactor, dfactor)
    check()
  }

  override def glClear(mask: ClearMask): Unit = {
    calls += 1
    gl20.glClear(mask)
    check()
  }

  override def glClearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = {
    calls += 1
    gl20.glClearColor(red, green, blue, alpha)
    check()
  }

  override def glClearDepthf(depth: Float): Unit = {
    calls += 1
    gl20.glClearDepthf(depth)
    check()
  }

  override def glClearStencil(s: Int): Unit = {
    calls += 1
    gl20.glClearStencil(s)
    check()
  }

  override def glColorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit = {
    calls += 1
    gl20.glColorMask(red, green, blue, alpha)
    check()
  }

  override def glCompressedTexImage2D(target: TextureTarget, level: Int, internalformat: Int, width: Pixels, height: Pixels, border: Int, imageSize: Int, data: Buffer): Unit = {
    calls += 1
    gl20.glCompressedTexImage2D(target, level, internalformat, width, height, border, imageSize, data)
    check()
  }

  override def glCompressedTexSubImage2D(
    target:    TextureTarget,
    level:     Int,
    xoffset:   Pixels,
    yoffset:   Pixels,
    width:     Pixels,
    height:    Pixels,
    format:    PixelFormat,
    imageSize: Int,
    data:      Buffer
  ): Unit = {
    calls += 1
    gl20.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, imageSize, data)
    check()
  }

  override def glCopyTexImage2D(target: TextureTarget, level: Int, internalformat: Int, x: Pixels, y: Pixels, width: Pixels, height: Pixels, border: Int): Unit = {
    calls += 1
    gl20.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border)
    check()
  }

  override def glCopyTexSubImage2D(target: TextureTarget, level: Int, xoffset: Pixels, yoffset: Pixels, x: Pixels, y: Pixels, width: Pixels, height: Pixels): Unit = {
    calls += 1
    gl20.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height)
    check()
  }

  override def glCullFace(mode: CullFace): Unit = {
    calls += 1
    gl20.glCullFace(mode)
    check()
  }

  override def glDeleteTextures(n: Int, textures: IntBuffer): Unit = {
    calls += 1
    gl20.glDeleteTextures(n, textures)
    check()
  }

  override def glDeleteTexture(texture: Int): Unit = {
    calls += 1
    gl20.glDeleteTexture(texture)
    check()
  }

  override def glDepthFunc(func: CompareFunc): Unit = {
    calls += 1
    gl20.glDepthFunc(func)
    check()
  }

  override def glDepthMask(flag: Boolean): Unit = {
    calls += 1
    gl20.glDepthMask(flag)
    check()
  }

  override def glDepthRangef(zNear: Float, zFar: Float): Unit = {
    calls += 1
    gl20.glDepthRangef(zNear, zFar)
    check()
  }

  override def glDisable(cap: EnableCap): Unit = {
    calls += 1
    gl20.glDisable(cap)
    check()
  }

  override def glDrawArrays(mode: PrimitiveMode, first: Int, count: Int): Unit = {
    vertexCount.put(count.toFloat)
    drawCalls += 1
    calls += 1
    gl20.glDrawArrays(mode, first, count)
    check()
  }

  override def glDrawElements(mode: PrimitiveMode, count: Int, `type`: DataType, indices: Buffer): Unit = {
    vertexCount.put(count.toFloat)
    drawCalls += 1
    calls += 1
    gl20.glDrawElements(mode, count, `type`, indices)
    check()
  }

  override def glEnable(cap: EnableCap): Unit = {
    calls += 1
    gl20.glEnable(cap)
    check()
  }

  override def glFinish(): Unit = {
    calls += 1
    gl20.glFinish()
    check()
  }

  override def glFlush(): Unit = {
    calls += 1
    gl20.glFlush()
    check()
  }

  override def glFrontFace(mode: Int): Unit = {
    calls += 1
    gl20.glFrontFace(mode)
    check()
  }

  override def glGenTextures(n: Int, textures: IntBuffer): Unit = {
    calls += 1
    gl20.glGenTextures(n, textures)
    check()
  }

  override def glGenTexture(): Int = {
    calls += 1
    val result = gl20.glGenTexture()
    check()
    result
  }

  override def glGetError(): Int = {
    calls += 1
    // Errors by glGetError are undetectable
    gl20.glGetError()
  }

  override def glGetIntegerv(pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl20.glGetIntegerv(pname, params)
    check()
  }

  override def glGetString(name: Int): String = {
    calls += 1
    val result = gl20.glGetString(name)
    check()
    result
  }

  override def glHint(target: Int, mode: Int): Unit = {
    calls += 1
    gl20.glHint(target, mode)
    check()
  }

  override def glLineWidth(width: Float): Unit = {
    calls += 1
    gl20.glLineWidth(width)
    check()
  }

  override def glPixelStorei(pname: Int, param: Int): Unit = {
    calls += 1
    gl20.glPixelStorei(pname, param)
    check()
  }

  override def glPolygonOffset(factor: Float, units: Float): Unit = {
    calls += 1
    gl20.glPolygonOffset(factor, units)
    check()
  }

  override def glReadPixels(x: Pixels, y: Pixels, width: Pixels, height: Pixels, format: PixelFormat, `type`: DataType, pixels: Buffer): Unit = {
    calls += 1
    gl20.glReadPixels(x, y, width, height, format, `type`, pixels)
    check()
  }

  override def glScissor(x: Pixels, y: Pixels, width: Pixels, height: Pixels): Unit = {
    calls += 1
    gl20.glScissor(x, y, width, height)
    check()
  }

  override def glStencilFunc(func: CompareFunc, ref: Int, mask: Int): Unit = {
    calls += 1
    gl20.glStencilFunc(func, ref, mask)
    check()
  }

  override def glStencilMask(mask: Int): Unit = {
    calls += 1
    gl20.glStencilMask(mask)
    check()
  }

  override def glStencilOp(fail: StencilOp, zfail: StencilOp, zpass: StencilOp): Unit = {
    calls += 1
    gl20.glStencilOp(fail, zfail, zpass)
    check()
  }

  override def glTexImage2D(target: TextureTarget, level: Int, internalformat: Int, width: Pixels, height: Pixels, border: Int, format: PixelFormat, `type`: DataType, pixels: Buffer): Unit = {
    calls += 1
    gl20.glTexImage2D(target, level, internalformat, width, height, border, format, `type`, pixels)
    check()
  }

  override def glTexParameterf(target: TextureTarget, pname: Int, param: Float): Unit = {
    calls += 1
    gl20.glTexParameterf(target, pname, param)
    check()
  }

  override def glTexSubImage2D(target: TextureTarget, level: Int, xoffset: Pixels, yoffset: Pixels, width: Pixels, height: Pixels, format: PixelFormat, `type`: DataType, pixels: Buffer): Unit = {
    calls += 1
    gl20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, `type`, pixels)
    check()
  }

  override def glViewport(x: Pixels, y: Pixels, width: Pixels, height: Pixels): Unit = {
    calls += 1
    gl20.glViewport(x, y, width, height)
    check()
  }

  override def glAttachShader(program: Int, shader: Int): Unit = {
    calls += 1
    gl20.glAttachShader(program, shader)
    check()
  }

  override def glBindAttribLocation(program: Int, index: Int, name: String): Unit = {
    calls += 1
    gl20.glBindAttribLocation(program, index, name)
    check()
  }

  override def glBindBuffer(target: BufferTarget, buffer: Int): Unit = {
    calls += 1
    gl20.glBindBuffer(target, buffer)
    check()
  }

  override def glBindFramebuffer(target: Int, framebuffer: Int): Unit = {
    calls += 1
    gl20.glBindFramebuffer(target, framebuffer)
    check()
  }

  override def glBindRenderbuffer(target: Int, renderbuffer: Int): Unit = {
    calls += 1
    gl20.glBindRenderbuffer(target, renderbuffer)
    check()
  }

  override def glBlendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = {
    calls += 1
    gl20.glBlendColor(red, green, blue, alpha)
    check()
  }

  override def glBlendEquation(mode: BlendEquation): Unit = {
    calls += 1
    gl20.glBlendEquation(mode)
    check()
  }

  override def glBlendEquationSeparate(modeRGB: BlendEquation, modeAlpha: BlendEquation): Unit = {
    calls += 1
    gl20.glBlendEquationSeparate(modeRGB, modeAlpha)
    check()
  }

  override def glBlendFuncSeparate(srcRGB: BlendFactor, dstRGB: BlendFactor, srcAlpha: BlendFactor, dstAlpha: BlendFactor): Unit = {
    calls += 1
    gl20.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)
    check()
  }

  override def glBufferData(target: BufferTarget, size: Int, data: Buffer, usage: BufferUsage): Unit = {
    calls += 1
    gl20.glBufferData(target, size, data, usage)
    check()
  }

  override def glBufferSubData(target: BufferTarget, offset: Int, size: Int, data: Buffer): Unit = {
    calls += 1
    gl20.glBufferSubData(target, offset, size, data)
    check()
  }

  override def glCheckFramebufferStatus(target: Int): Int = {
    calls += 1
    val result = gl20.glCheckFramebufferStatus(target)
    check()
    result
  }

  override def glCompileShader(shader: Int): Unit = {
    calls += 1
    gl20.glCompileShader(shader)
    check()
  }

  override def glCreateProgram(): Int = {
    calls += 1
    val result = gl20.glCreateProgram()
    check()
    result
  }

  override def glCreateShader(`type`: ShaderType): Int = {
    calls += 1
    val result = gl20.glCreateShader(`type`)
    check()
    result
  }

  override def glDeleteBuffer(buffer: Int): Unit = {
    calls += 1
    gl20.glDeleteBuffer(buffer)
    check()
  }

  override def glDeleteBuffers(n: Int, buffers: IntBuffer): Unit = {
    calls += 1
    gl20.glDeleteBuffers(n, buffers)
    check()
  }

  override def glDeleteFramebuffer(framebuffer: Int): Unit = {
    calls += 1
    gl20.glDeleteFramebuffer(framebuffer)
    check()
  }

  override def glDeleteFramebuffers(n: Int, framebuffers: IntBuffer): Unit = {
    calls += 1
    gl20.glDeleteFramebuffers(n, framebuffers)
    check()
  }

  override def glDeleteProgram(program: Int): Unit = {
    calls += 1
    gl20.glDeleteProgram(program)
    check()
  }

  override def glDeleteRenderbuffer(renderbuffer: Int): Unit = {
    calls += 1
    gl20.glDeleteRenderbuffer(renderbuffer)
    check()
  }

  override def glDeleteRenderbuffers(n: Int, renderbuffers: IntBuffer): Unit = {
    calls += 1
    gl20.glDeleteRenderbuffers(n, renderbuffers)
    check()
  }

  override def glDeleteShader(shader: Int): Unit = {
    calls += 1
    gl20.glDeleteShader(shader)
    check()
  }

  override def glDetachShader(program: Int, shader: Int): Unit = {
    calls += 1
    gl20.glDetachShader(program, shader)
    check()
  }

  override def glDisableVertexAttribArray(index: Int): Unit = {
    calls += 1
    gl20.glDisableVertexAttribArray(index)
    check()
  }

  override def glDrawElements(mode: PrimitiveMode, count: Int, `type`: DataType, indices: Int): Unit = {
    vertexCount.put(count.toFloat)
    drawCalls += 1
    calls += 1
    gl20.glDrawElements(mode, count, `type`, indices)
    check()
  }

  override def glEnableVertexAttribArray(index: Int): Unit = {
    calls += 1
    gl20.glEnableVertexAttribArray(index)
    check()
  }

  override def glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int): Unit = {
    calls += 1
    gl20.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)
    check()
  }

  override def glFramebufferTexture2D(target: Int, attachment: Int, textarget: TextureTarget, texture: Int, level: Int): Unit = {
    calls += 1
    gl20.glFramebufferTexture2D(target, attachment, textarget, texture, level)
    check()
  }

  override def glGenBuffer(): Int = {
    calls += 1
    val result = gl20.glGenBuffer()
    check()
    result
  }

  override def glGenBuffers(n: Int, buffers: IntBuffer): Unit = {
    calls += 1
    gl20.glGenBuffers(n, buffers)
    check()
  }

  override def glGenerateMipmap(target: TextureTarget): Unit = {
    calls += 1
    gl20.glGenerateMipmap(target)
    check()
  }

  override def glGenFramebuffer(): Int = {
    calls += 1
    val result = gl20.glGenFramebuffer()
    check()
    result
  }

  override def glGenFramebuffers(n: Int, framebuffers: IntBuffer): Unit = {
    calls += 1
    gl20.glGenFramebuffers(n, framebuffers)
    check()
  }

  override def glGenRenderbuffer(): Int = {
    calls += 1
    val result = gl20.glGenRenderbuffer()
    check()
    result
  }

  override def glGenRenderbuffers(n: Int, renderbuffers: IntBuffer): Unit = {
    calls += 1
    gl20.glGenRenderbuffers(n, renderbuffers)
    check()
  }

  override def glGetActiveAttrib(program: Int, index: Int, size: IntBuffer, `type`: IntBuffer): String = {
    calls += 1
    val result = gl20.glGetActiveAttrib(program, index, size, `type`)
    check()
    result
  }

  override def glGetActiveUniform(program: Int, index: Int, size: IntBuffer, `type`: IntBuffer): String = {
    calls += 1
    val result = gl20.glGetActiveUniform(program, index, size, `type`)
    check()
    result
  }

  override def glGetAttachedShaders(program: Int, maxcount: Int, count: Buffer, shaders: IntBuffer): Unit = {
    calls += 1
    gl20.glGetAttachedShaders(program, maxcount, count, shaders)
    check()
  }

  override def glGetAttribLocation(program: Int, name: String): Int = {
    calls += 1
    val result = gl20.glGetAttribLocation(program, name)
    check()
    result
  }

  override def glGetBooleanv(pname: Int, params: Buffer): Unit = {
    calls += 1
    gl20.glGetBooleanv(pname, params)
    check()
  }

  override def glGetBufferParameteriv(target: BufferTarget, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl20.glGetBufferParameteriv(target, pname, params)
    check()
  }

  override def glGetFloatv(pname: Int, params: FloatBuffer): Unit = {
    calls += 1
    gl20.glGetFloatv(pname, params)
    check()
  }

  override def glGetFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl20.glGetFramebufferAttachmentParameteriv(target, attachment, pname, params)
    check()
  }

  override def glGetProgramiv(program: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl20.glGetProgramiv(program, pname, params)
    check()
  }

  override def glGetProgramInfoLog(program: Int): String = {
    calls += 1
    val result = gl20.glGetProgramInfoLog(program)
    check()
    result
  }

  override def glGetRenderbufferParameteriv(target: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl20.glGetRenderbufferParameteriv(target, pname, params)
    check()
  }

  override def glGetShaderiv(shader: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl20.glGetShaderiv(shader, pname, params)
    check()
  }

  override def glGetShaderInfoLog(shader: Int): String = {
    calls += 1
    val result = gl20.glGetShaderInfoLog(shader)
    check()
    result
  }

  override def glGetShaderPrecisionFormat(shadertype: ShaderType, precisiontype: Int, range: IntBuffer, precision: IntBuffer): Unit = {
    calls += 1
    gl20.glGetShaderPrecisionFormat(shadertype, precisiontype, range, precision)
    check()
  }

  override def glGetTexParameterfv(target: TextureTarget, pname: Int, params: FloatBuffer): Unit = {
    calls += 1
    gl20.glGetTexParameterfv(target, pname, params)
    check()
  }

  override def glGetTexParameteriv(target: TextureTarget, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl20.glGetTexParameteriv(target, pname, params)
    check()
  }

  override def glGetUniformfv(program: Int, location: Int, params: FloatBuffer): Unit = {
    calls += 1
    gl20.glGetUniformfv(program, location, params)
    check()
  }

  override def glGetUniformiv(program: Int, location: Int, params: IntBuffer): Unit = {
    calls += 1
    gl20.glGetUniformiv(program, location, params)
    check()
  }

  override def glGetUniformLocation(program: Int, name: String): Int = {
    calls += 1
    val result = gl20.glGetUniformLocation(program, name)
    check()
    result
  }

  override def glGetVertexAttribfv(index: Int, pname: Int, params: FloatBuffer): Unit = {
    calls += 1
    gl20.glGetVertexAttribfv(index, pname, params)
    check()
  }

  override def glGetVertexAttribiv(index: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl20.glGetVertexAttribiv(index, pname, params)
    check()
  }

  override def glGetVertexAttribPointerv(index: Int, pname: Int, pointer: Buffer): Unit = {
    calls += 1
    gl20.glGetVertexAttribPointerv(index, pname, pointer)
    check()
  }

  override def glIsBuffer(buffer: Int): Boolean = {
    calls += 1
    val result = gl20.glIsBuffer(buffer)
    check()
    result
  }

  override def glIsEnabled(cap: EnableCap): Boolean = {
    calls += 1
    val result = gl20.glIsEnabled(cap)
    check()
    result
  }

  override def glIsFramebuffer(framebuffer: Int): Boolean = {
    calls += 1
    val result = gl20.glIsFramebuffer(framebuffer)
    check()
    result
  }

  override def glIsProgram(program: Int): Boolean = {
    calls += 1
    val result = gl20.glIsProgram(program)
    check()
    result
  }

  override def glIsRenderbuffer(renderbuffer: Int): Boolean = {
    calls += 1
    val result = gl20.glIsRenderbuffer(renderbuffer)
    check()
    result
  }

  override def glIsShader(shader: Int): Boolean = {
    calls += 1
    val result = gl20.glIsShader(shader)
    check()
    result
  }

  override def glIsTexture(texture: Int): Boolean = {
    calls += 1
    val result = gl20.glIsTexture(texture)
    check()
    result
  }

  override def glLinkProgram(program: Int): Unit = {
    calls += 1
    gl20.glLinkProgram(program)
    check()
  }

  override def glReleaseShaderCompiler(): Unit = {
    calls += 1
    gl20.glReleaseShaderCompiler()
    check()
  }

  override def glRenderbufferStorage(target: Int, internalformat: Int, width: Pixels, height: Pixels): Unit = {
    calls += 1
    gl20.glRenderbufferStorage(target, internalformat, width, height)
    check()
  }

  override def glSampleCoverage(value: Float, invert: Boolean): Unit = {
    calls += 1
    gl20.glSampleCoverage(value, invert)
    check()
  }

  override def glShaderBinary(n: Int, shaders: IntBuffer, binaryformat: Int, binary: Buffer, length: Int): Unit = {
    calls += 1
    gl20.glShaderBinary(n, shaders, binaryformat, binary, length)
    check()
  }

  override def glShaderSource(shader: Int, string: String): Unit = {
    calls += 1
    gl20.glShaderSource(shader, string)
    check()
  }

  override def glStencilFuncSeparate(face: CullFace, func: CompareFunc, ref: Int, mask: Int): Unit = {
    calls += 1
    gl20.glStencilFuncSeparate(face, func, ref, mask)
    check()
  }

  override def glStencilMaskSeparate(face: CullFace, mask: Int): Unit = {
    calls += 1
    gl20.glStencilMaskSeparate(face, mask)
    check()
  }

  override def glStencilOpSeparate(face: CullFace, fail: StencilOp, zfail: StencilOp, zpass: StencilOp): Unit = {
    calls += 1
    gl20.glStencilOpSeparate(face, fail, zfail, zpass)
    check()
  }

  override def glTexParameterfv(target: TextureTarget, pname: Int, params: FloatBuffer): Unit = {
    calls += 1
    gl20.glTexParameterfv(target, pname, params)
    check()
  }

  override def glTexParameteri(target: TextureTarget, pname: Int, param: Int): Unit = {
    calls += 1
    gl20.glTexParameteri(target, pname, param)
    check()
  }

  override def glTexParameteriv(target: TextureTarget, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl20.glTexParameteriv(target, pname, params)
    check()
  }

  override def glUniform1f(location: Int, x: Float): Unit = {
    calls += 1
    gl20.glUniform1f(location, x)
    check()
  }

  override def glUniform1fv(location: Int, count: Int, v: FloatBuffer): Unit = {
    calls += 1
    gl20.glUniform1fv(location, count, v)
    check()
  }

  override def glUniform1fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit = {
    calls += 1
    gl20.glUniform1fv(location, count, v, offset)
    check()
  }

  override def glUniform1i(location: Int, x: Int): Unit = {
    calls += 1
    gl20.glUniform1i(location, x)
    check()
  }

  override def glUniform1iv(location: Int, count: Int, v: IntBuffer): Unit = {
    calls += 1
    gl20.glUniform1iv(location, count, v)
    check()
  }

  override def glUniform1iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit = {
    calls += 1
    gl20.glUniform1iv(location, count, v, offset)
    check()
  }

  override def glUniform2f(location: Int, x: Float, y: Float): Unit = {
    calls += 1
    gl20.glUniform2f(location, x, y)
    check()
  }

  override def glUniform2fv(location: Int, count: Int, v: FloatBuffer): Unit = {
    calls += 1
    gl20.glUniform2fv(location, count, v)
    check()
  }

  override def glUniform2fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit = {
    calls += 1
    gl20.glUniform2fv(location, count, v, offset)
    check()
  }

  override def glUniform2i(location: Int, x: Int, y: Int): Unit = {
    calls += 1
    gl20.glUniform2i(location, x, y)
    check()
  }

  override def glUniform2iv(location: Int, count: Int, v: IntBuffer): Unit = {
    calls += 1
    gl20.glUniform2iv(location, count, v)
    check()
  }

  override def glUniform2iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit = {
    calls += 1
    gl20.glUniform2iv(location, count, v, offset)
    check()
  }

  override def glUniform3f(location: Int, x: Float, y: Float, z: Float): Unit = {
    calls += 1
    gl20.glUniform3f(location, x, y, z)
    check()
  }

  override def glUniform3fv(location: Int, count: Int, v: FloatBuffer): Unit = {
    calls += 1
    gl20.glUniform3fv(location, count, v)
    check()
  }

  override def glUniform3fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit = {
    calls += 1
    gl20.glUniform3fv(location, count, v, offset)
    check()
  }

  override def glUniform3i(location: Int, x: Int, y: Int, z: Int): Unit = {
    calls += 1
    gl20.glUniform3i(location, x, y, z)
    check()
  }

  override def glUniform3iv(location: Int, count: Int, v: IntBuffer): Unit = {
    calls += 1
    gl20.glUniform3iv(location, count, v)
    check()
  }

  override def glUniform3iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit = {
    calls += 1
    gl20.glUniform3iv(location, count, v, offset)
    check()
  }

  override def glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float): Unit = {
    calls += 1
    gl20.glUniform4f(location, x, y, z, w)
    check()
  }

  override def glUniform4fv(location: Int, count: Int, v: FloatBuffer): Unit = {
    calls += 1
    gl20.glUniform4fv(location, count, v)
    check()
  }

  override def glUniform4fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit = {
    calls += 1
    gl20.glUniform4fv(location, count, v, offset)
    check()
  }

  override def glUniform4i(location: Int, x: Int, y: Int, z: Int, w: Int): Unit = {
    calls += 1
    gl20.glUniform4i(location, x, y, z, w)
    check()
  }

  override def glUniform4iv(location: Int, count: Int, v: IntBuffer): Unit = {
    calls += 1
    gl20.glUniform4iv(location, count, v)
    check()
  }

  override def glUniform4iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit = {
    calls += 1
    gl20.glUniform4iv(location, count, v, offset)
    check()
  }

  override def glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = {
    calls += 1
    gl20.glUniformMatrix2fv(location, count, transpose, value)
    check()
  }

  override def glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit = {
    calls += 1
    gl20.glUniformMatrix2fv(location, count, transpose, value, offset)
    check()
  }

  override def glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = {
    calls += 1
    gl20.glUniformMatrix3fv(location, count, transpose, value)
    check()
  }

  override def glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit = {
    calls += 1
    gl20.glUniformMatrix3fv(location, count, transpose, value, offset)
    check()
  }

  override def glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = {
    calls += 1
    gl20.glUniformMatrix4fv(location, count, transpose, value)
    check()
  }

  override def glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit = {
    calls += 1
    gl20.glUniformMatrix4fv(location, count, transpose, value, offset)
    check()
  }

  override def glUseProgram(program: Int): Unit = {
    shaderSwitches += 1
    calls += 1
    gl20.glUseProgram(program)
    check()
  }

  override def glValidateProgram(program: Int): Unit = {
    calls += 1
    gl20.glValidateProgram(program)
    check()
  }

  override def glVertexAttrib1f(indx: Int, x: Float): Unit = {
    calls += 1
    gl20.glVertexAttrib1f(indx, x)
    check()
  }

  override def glVertexAttrib1fv(indx: Int, values: FloatBuffer): Unit = {
    calls += 1
    gl20.glVertexAttrib1fv(indx, values)
    check()
  }

  override def glVertexAttrib2f(indx: Int, x: Float, y: Float): Unit = {
    calls += 1
    gl20.glVertexAttrib2f(indx, x, y)
    check()
  }

  override def glVertexAttrib2fv(indx: Int, values: FloatBuffer): Unit = {
    calls += 1
    gl20.glVertexAttrib2fv(indx, values)
    check()
  }

  override def glVertexAttrib3f(indx: Int, x: Float, y: Float, z: Float): Unit = {
    calls += 1
    gl20.glVertexAttrib3f(indx, x, y, z)
    check()
  }

  override def glVertexAttrib3fv(indx: Int, values: FloatBuffer): Unit = {
    calls += 1
    gl20.glVertexAttrib3fv(indx, values)
    check()
  }

  override def glVertexAttrib4f(indx: Int, x: Float, y: Float, z: Float, w: Float): Unit = {
    calls += 1
    gl20.glVertexAttrib4f(indx, x, y, z, w)
    check()
  }

  override def glVertexAttrib4fv(indx: Int, values: FloatBuffer): Unit = {
    calls += 1
    gl20.glVertexAttrib4fv(indx, values)
    check()
  }

  override def glVertexAttribPointer(indx: Int, size: Int, `type`: DataType, normalized: Boolean, stride: Int, ptr: Buffer): Unit = {
    calls += 1
    gl20.glVertexAttribPointer(indx, size, `type`, normalized, stride, ptr)
    check()
  }

  override def glVertexAttribPointer(indx: Int, size: Int, `type`: DataType, normalized: Boolean, stride: Int, ptr: Int): Unit = {
    calls += 1
    gl20.glVertexAttribPointer(indx, size, `type`, normalized, stride, ptr)
    check()
  }
}
