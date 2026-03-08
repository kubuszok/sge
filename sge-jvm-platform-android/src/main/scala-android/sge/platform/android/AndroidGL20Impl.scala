// SGE — Android GL ES 2.0 implementation
//
// Thin wrapper around android.opengl.GLES20 static methods.
//
// Migration notes:
//   Source:  com.badlogic.gdx.backends.android.AndroidGL20
//   Renames: AndroidGL20 → AndroidGL20Impl
//   Convention: ops interface pattern; _root_.android.* imports
//   Audited: 2026-03-08

package sge
package platform
package android

import _root_.android.opengl.GLES20
import java.nio.{ Buffer, FloatBuffer, IntBuffer }

class AndroidGL20Impl extends GL20Ops {

  // Temp arrays for single-value gen/delete calls
  private val ints  = new Array[Int](1)
  private val ints2 = new Array[Int](1)
  private val ints3 = new Array[Int](1)
  private val buf   = new Array[Byte](512)

  // ── Texture state ─────────────────────────────────────────────────────

  override def glActiveTexture(texture: Int): Unit = GLES20.glActiveTexture(texture)

  override def glBindTexture(target: Int, texture: Int): Unit = GLES20.glBindTexture(target, texture)

  override def glBlendFunc(sfactor: Int, dfactor: Int): Unit = GLES20.glBlendFunc(sfactor, dfactor)

  override def glClear(mask: Int): Unit = GLES20.glClear(mask)

  override def glClearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit =
    GLES20.glClearColor(red, green, blue, alpha)

  override def glClearDepthf(depth: Float): Unit = GLES20.glClearDepthf(depth)

  override def glClearStencil(s: Int): Unit = GLES20.glClearStencil(s)

  override def glColorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit =
    GLES20.glColorMask(red, green, blue, alpha)

  override def glCompressedTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, imageSize: Int, data: Buffer): Unit =
    GLES20.glCompressedTexImage2D(target, level, internalformat, width, height, border, imageSize, data)

  override def glCompressedTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, imageSize: Int, data: Buffer): Unit =
    GLES20.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, imageSize, data)

  override def glCopyTexImage2D(target: Int, level: Int, internalformat: Int, x: Int, y: Int, width: Int, height: Int, border: Int): Unit =
    GLES20.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border)

  override def glCopyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit =
    GLES20.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height)

  override def glCullFace(mode: Int): Unit = GLES20.glCullFace(mode)

  override def glDeleteTextures(n: Int, textures: IntBuffer): Unit = GLES20.glDeleteTextures(n, textures)

  override def glDeleteTexture(texture: Int): Unit = {
    ints(0) = texture
    GLES20.glDeleteTextures(1, ints, 0)
  }

  override def glDepthFunc(func: Int): Unit = GLES20.glDepthFunc(func)

  override def glDepthMask(flag: Boolean): Unit = GLES20.glDepthMask(flag)

  override def glDepthRangef(zNear: Float, zFar: Float): Unit = GLES20.glDepthRangef(zNear, zFar)

  override def glDisable(cap: Int): Unit = GLES20.glDisable(cap)

  override def glDrawArrays(mode: Int, first: Int, count: Int): Unit = GLES20.glDrawArrays(mode, first, count)

  override def glDrawElements(mode: Int, count: Int, `type`: Int, indices: Buffer): Unit =
    GLES20.glDrawElements(mode, count, `type`, indices)

  override def glEnable(cap: Int): Unit = GLES20.glEnable(cap)

  override def glFinish(): Unit = GLES20.glFinish()

  override def glFlush(): Unit = GLES20.glFlush()

  override def glFrontFace(mode: Int): Unit = GLES20.glFrontFace(mode)

  override def glGenTextures(n: Int, textures: IntBuffer): Unit = GLES20.glGenTextures(n, textures)

  override def glGenTexture(): Int = {
    GLES20.glGenTextures(1, ints, 0)
    ints(0)
  }

  override def glGetError(): Int = GLES20.glGetError()

  override def glGetIntegerv(pname: Int, params: IntBuffer): Unit = GLES20.glGetIntegerv(pname, params)

  override def glGetString(name: Int): String = GLES20.glGetString(name)

  override def glHint(target: Int, mode: Int): Unit = GLES20.glHint(target, mode)

  override def glLineWidth(width: Float): Unit = GLES20.glLineWidth(width)

  override def glPixelStorei(pname: Int, param: Int): Unit = GLES20.glPixelStorei(pname, param)

  override def glPolygonOffset(factor: Float, units: Float): Unit = GLES20.glPolygonOffset(factor, units)

  override def glReadPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: Buffer): Unit =
    GLES20.glReadPixels(x, y, width, height, format, `type`, pixels)

  override def glScissor(x: Int, y: Int, width: Int, height: Int): Unit = GLES20.glScissor(x, y, width, height)

  override def glStencilFunc(func: Int, ref: Int, mask: Int): Unit = GLES20.glStencilFunc(func, ref, mask)

  override def glStencilMask(mask: Int): Unit = GLES20.glStencilMask(mask)

  override def glStencilOp(fail: Int, zfail: Int, zpass: Int): Unit = GLES20.glStencilOp(fail, zfail, zpass)

  override def glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, `type`: Int, pixels: Buffer): Unit =
    GLES20.glTexImage2D(target, level, internalformat, width, height, border, format, `type`, pixels)

  override def glTexParameterf(target: Int, pname: Int, param: Float): Unit =
    GLES20.glTexParameterf(target, pname, param)

  override def glTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: Buffer): Unit =
    GLES20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, `type`, pixels)

  override def glViewport(x: Int, y: Int, width: Int, height: Int): Unit = GLES20.glViewport(x, y, width, height)

  // ── Shader / program ──────────────────────────────────────────────────

  override def glAttachShader(program: Int, shader: Int): Unit = GLES20.glAttachShader(program, shader)

  override def glBindAttribLocation(program: Int, index: Int, name: String): Unit =
    GLES20.glBindAttribLocation(program, index, name)

  override def glBindBuffer(target: Int, buffer: Int): Unit = GLES20.glBindBuffer(target, buffer)

  override def glBindFramebuffer(target: Int, framebuffer: Int): Unit = GLES20.glBindFramebuffer(target, framebuffer)

  override def glBindRenderbuffer(target: Int, renderbuffer: Int): Unit = GLES20.glBindRenderbuffer(target, renderbuffer)

  override def glBlendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit =
    GLES20.glBlendColor(red, green, blue, alpha)

  override def glBlendEquation(mode: Int): Unit = GLES20.glBlendEquation(mode)

  override def glBlendEquationSeparate(modeRGB: Int, modeAlpha: Int): Unit =
    GLES20.glBlendEquationSeparate(modeRGB, modeAlpha)

  override def glBlendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int): Unit =
    GLES20.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)

  override def glBufferData(target: Int, size: Int, data: Buffer, usage: Int): Unit =
    GLES20.glBufferData(target, size, data, usage)

  override def glBufferSubData(target: Int, offset: Int, size: Int, data: Buffer): Unit =
    GLES20.glBufferSubData(target, offset, size, data)

  override def glCheckFramebufferStatus(target: Int): Int = GLES20.glCheckFramebufferStatus(target)

  override def glCompileShader(shader: Int): Unit = GLES20.glCompileShader(shader)

  override def glCreateProgram(): Int = GLES20.glCreateProgram()

  override def glCreateShader(`type`: Int): Int = GLES20.glCreateShader(`type`)

  override def glDeleteBuffer(buffer: Int): Unit = {
    ints(0) = buffer
    GLES20.glDeleteBuffers(1, ints, 0)
  }

  override def glDeleteBuffers(n: Int, buffers: IntBuffer): Unit = GLES20.glDeleteBuffers(n, buffers)

  override def glDeleteFramebuffer(framebuffer: Int): Unit = {
    ints(0) = framebuffer
    GLES20.glDeleteFramebuffers(1, ints, 0)
  }

  override def glDeleteFramebuffers(n: Int, framebuffers: IntBuffer): Unit =
    GLES20.glDeleteFramebuffers(n, framebuffers)

  override def glDeleteProgram(program: Int): Unit = GLES20.glDeleteProgram(program)

  override def glDeleteRenderbuffer(renderbuffer: Int): Unit = {
    ints(0) = renderbuffer
    GLES20.glDeleteRenderbuffers(1, ints, 0)
  }

  override def glDeleteRenderbuffers(n: Int, renderbuffers: IntBuffer): Unit =
    GLES20.glDeleteRenderbuffers(n, renderbuffers)

  override def glDeleteShader(shader: Int): Unit = GLES20.glDeleteShader(shader)

  override def glDetachShader(program: Int, shader: Int): Unit = GLES20.glDetachShader(program, shader)

  override def glDisableVertexAttribArray(index: Int): Unit = GLES20.glDisableVertexAttribArray(index)

  override def glDrawElements(mode: Int, count: Int, `type`: Int, indices: Int): Unit =
    GLES20.glDrawElements(mode, count, `type`, indices)

  override def glEnableVertexAttribArray(index: Int): Unit = GLES20.glEnableVertexAttribArray(index)

  override def glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int): Unit =
    GLES20.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)

  override def glFramebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int): Unit =
    GLES20.glFramebufferTexture2D(target, attachment, textarget, texture, level)

  override def glGenBuffer(): Int = {
    GLES20.glGenBuffers(1, ints, 0)
    ints(0)
  }

  override def glGenBuffers(n: Int, buffers: IntBuffer): Unit = GLES20.glGenBuffers(n, buffers)

  override def glGenerateMipmap(target: Int): Unit = GLES20.glGenerateMipmap(target)

  override def glGenFramebuffer(): Int = {
    GLES20.glGenFramebuffers(1, ints, 0)
    ints(0)
  }

  override def glGenFramebuffers(n: Int, framebuffers: IntBuffer): Unit = GLES20.glGenFramebuffers(n, framebuffers)

  override def glGenRenderbuffer(): Int = {
    GLES20.glGenRenderbuffers(1, ints, 0)
    ints(0)
  }

  override def glGenRenderbuffers(n: Int, renderbuffers: IntBuffer): Unit = GLES20.glGenRenderbuffers(n, renderbuffers)

  // Deviating: returns name String, puts size/type into buffers
  override def glGetActiveAttrib(program: Int, index: Int, size: IntBuffer, `type`: IntBuffer): String = {
    GLES20.glGetActiveAttrib(program, index, buf.length, ints, 0, ints2, 0, ints3, 0, buf, 0)
    size.put(ints2(0))
    `type`.put(ints3(0))
    new String(buf, 0, ints(0))
  }

  // Deviating: returns name String, puts size/type into buffers
  override def glGetActiveUniform(program: Int, index: Int, size: IntBuffer, `type`: IntBuffer): String = {
    GLES20.glGetActiveUniform(program, index, buf.length, ints, 0, ints2, 0, ints3, 0, buf, 0)
    size.put(ints2(0))
    `type`.put(ints3(0))
    new String(buf, 0, ints(0))
  }

  override def glGetAttachedShaders(program: Int, maxcount: Int, count: Buffer, shaders: IntBuffer): Unit =
    GLES20.glGetAttachedShaders(program, maxcount, count.asInstanceOf[IntBuffer], shaders)

  override def glGetAttribLocation(program: Int, name: String): Int = GLES20.glGetAttribLocation(program, name)

  override def glGetBooleanv(pname: Int, params: Buffer): Unit =
    GLES20.glGetBooleanv(pname, params.asInstanceOf[IntBuffer])

  override def glGetBufferParameteriv(target: Int, pname: Int, params: IntBuffer): Unit =
    GLES20.glGetBufferParameteriv(target, pname, params)

  override def glGetFloatv(pname: Int, params: FloatBuffer): Unit = GLES20.glGetFloatv(pname, params)

  override def glGetFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: IntBuffer): Unit =
    GLES20.glGetFramebufferAttachmentParameteriv(target, attachment, pname, params)

  override def glGetProgramiv(program: Int, pname: Int, params: IntBuffer): Unit =
    GLES20.glGetProgramiv(program, pname, params)

  override def glGetProgramInfoLog(program: Int): String = GLES20.glGetProgramInfoLog(program)

  override def glGetRenderbufferParameteriv(target: Int, pname: Int, params: IntBuffer): Unit =
    GLES20.glGetRenderbufferParameteriv(target, pname, params)

  override def glGetShaderiv(shader: Int, pname: Int, params: IntBuffer): Unit =
    GLES20.glGetShaderiv(shader, pname, params)

  override def glGetShaderInfoLog(shader: Int): String = GLES20.glGetShaderInfoLog(shader)

  override def glGetShaderPrecisionFormat(shadertype: Int, precisiontype: Int, range: IntBuffer, precision: IntBuffer): Unit =
    GLES20.glGetShaderPrecisionFormat(shadertype, precisiontype, range, precision)

  override def glGetTexParameterfv(target: Int, pname: Int, params: FloatBuffer): Unit =
    GLES20.glGetTexParameterfv(target, pname, params)

  override def glGetTexParameteriv(target: Int, pname: Int, params: IntBuffer): Unit =
    GLES20.glGetTexParameteriv(target, pname, params)

  override def glGetUniformfv(program: Int, location: Int, params: FloatBuffer): Unit =
    GLES20.glGetUniformfv(program, location, params)

  override def glGetUniformiv(program: Int, location: Int, params: IntBuffer): Unit =
    GLES20.glGetUniformiv(program, location, params)

  override def glGetUniformLocation(program: Int, name: String): Int = GLES20.glGetUniformLocation(program, name)

  override def glGetVertexAttribfv(index: Int, pname: Int, params: FloatBuffer): Unit =
    GLES20.glGetVertexAttribfv(index, pname, params)

  override def glGetVertexAttribiv(index: Int, pname: Int, params: IntBuffer): Unit =
    GLES20.glGetVertexAttribiv(index, pname, params)

  // Not implemented on Android (same as LibGDX)
  override def glGetVertexAttribPointerv(index: Int, pname: Int, pointer: Buffer): Unit = ()

  override def glIsBuffer(buffer: Int): Boolean = GLES20.glIsBuffer(buffer)

  override def glIsEnabled(cap: Int): Boolean = GLES20.glIsEnabled(cap)

  override def glIsFramebuffer(framebuffer: Int): Boolean = GLES20.glIsFramebuffer(framebuffer)

  override def glIsProgram(program: Int): Boolean = GLES20.glIsProgram(program)

  override def glIsRenderbuffer(renderbuffer: Int): Boolean = GLES20.glIsRenderbuffer(renderbuffer)

  override def glIsShader(shader: Int): Boolean = GLES20.glIsShader(shader)

  override def glIsTexture(texture: Int): Boolean = GLES20.glIsTexture(texture)

  override def glLinkProgram(program: Int): Unit = GLES20.glLinkProgram(program)

  override def glReleaseShaderCompiler(): Unit = GLES20.glReleaseShaderCompiler()

  override def glRenderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int): Unit =
    GLES20.glRenderbufferStorage(target, internalformat, width, height)

  override def glSampleCoverage(value: Float, invert: Boolean): Unit = GLES20.glSampleCoverage(value, invert)

  override def glShaderBinary(n: Int, shaders: IntBuffer, binaryformat: Int, binary: Buffer, length: Int): Unit =
    GLES20.glShaderBinary(n, shaders, binaryformat, binary, length)

  override def glShaderSource(shader: Int, string: String): Unit = GLES20.glShaderSource(shader, string)

  override def glStencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int): Unit =
    GLES20.glStencilFuncSeparate(face, func, ref, mask)

  override def glStencilMaskSeparate(face: Int, mask: Int): Unit = GLES20.glStencilMaskSeparate(face, mask)

  override def glStencilOpSeparate(face: Int, fail: Int, zfail: Int, zpass: Int): Unit =
    GLES20.glStencilOpSeparate(face, fail, zfail, zpass)

  override def glTexParameterfv(target: Int, pname: Int, params: FloatBuffer): Unit =
    GLES20.glTexParameterfv(target, pname, params)

  override def glTexParameteri(target: Int, pname: Int, param: Int): Unit =
    GLES20.glTexParameteri(target, pname, param)

  override def glTexParameteriv(target: Int, pname: Int, params: IntBuffer): Unit =
    GLES20.glTexParameteriv(target, pname, params)

  // ── Uniforms ──────────────────────────────────────────────────────────

  override def glUniform1f(location: Int, x: Float): Unit = GLES20.glUniform1f(location, x)

  override def glUniform1fv(location: Int, count: Int, v: FloatBuffer): Unit = GLES20.glUniform1fv(location, count, v)

  override def glUniform1fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit =
    GLES20.glUniform1fv(location, count, v, offset)

  override def glUniform1i(location: Int, x: Int): Unit = GLES20.glUniform1i(location, x)

  override def glUniform1iv(location: Int, count: Int, v: IntBuffer): Unit = GLES20.glUniform1iv(location, count, v)

  override def glUniform1iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit =
    GLES20.glUniform1iv(location, count, v, offset)

  override def glUniform2f(location: Int, x: Float, y: Float): Unit = GLES20.glUniform2f(location, x, y)

  override def glUniform2fv(location: Int, count: Int, v: FloatBuffer): Unit = GLES20.glUniform2fv(location, count, v)

  override def glUniform2fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit =
    GLES20.glUniform2fv(location, count, v, offset)

  override def glUniform2i(location: Int, x: Int, y: Int): Unit = GLES20.glUniform2i(location, x, y)

  override def glUniform2iv(location: Int, count: Int, v: IntBuffer): Unit = GLES20.glUniform2iv(location, count, v)

  override def glUniform2iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit =
    GLES20.glUniform2iv(location, count, v, offset)

  override def glUniform3f(location: Int, x: Float, y: Float, z: Float): Unit = GLES20.glUniform3f(location, x, y, z)

  override def glUniform3fv(location: Int, count: Int, v: FloatBuffer): Unit = GLES20.glUniform3fv(location, count, v)

  override def glUniform3fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit =
    GLES20.glUniform3fv(location, count, v, offset)

  override def glUniform3i(location: Int, x: Int, y: Int, z: Int): Unit = GLES20.glUniform3i(location, x, y, z)

  override def glUniform3iv(location: Int, count: Int, v: IntBuffer): Unit = GLES20.glUniform3iv(location, count, v)

  override def glUniform3iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit =
    GLES20.glUniform3iv(location, count, v, offset)

  override def glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float): Unit =
    GLES20.glUniform4f(location, x, y, z, w)

  override def glUniform4fv(location: Int, count: Int, v: FloatBuffer): Unit = GLES20.glUniform4fv(location, count, v)

  override def glUniform4fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit =
    GLES20.glUniform4fv(location, count, v, offset)

  override def glUniform4i(location: Int, x: Int, y: Int, z: Int, w: Int): Unit =
    GLES20.glUniform4i(location, x, y, z, w)

  override def glUniform4iv(location: Int, count: Int, v: IntBuffer): Unit = GLES20.glUniform4iv(location, count, v)

  override def glUniform4iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit =
    GLES20.glUniform4iv(location, count, v, offset)

  override def glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GLES20.glUniformMatrix2fv(location, count, transpose, value)

  override def glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit =
    GLES20.glUniformMatrix2fv(location, count, transpose, value, offset)

  override def glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GLES20.glUniformMatrix3fv(location, count, transpose, value)

  override def glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit =
    GLES20.glUniformMatrix3fv(location, count, transpose, value, offset)

  override def glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GLES20.glUniformMatrix4fv(location, count, transpose, value)

  override def glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit =
    GLES20.glUniformMatrix4fv(location, count, transpose, value, offset)

  override def glUseProgram(program: Int): Unit = GLES20.glUseProgram(program)

  override def glValidateProgram(program: Int): Unit = GLES20.glValidateProgram(program)

  // ── Vertex attributes ─────────────────────────────────────────────────

  override def glVertexAttrib1f(indx: Int, x: Float): Unit = GLES20.glVertexAttrib1f(indx, x)

  override def glVertexAttrib1fv(indx: Int, values: FloatBuffer): Unit = GLES20.glVertexAttrib1fv(indx, values)

  override def glVertexAttrib2f(indx: Int, x: Float, y: Float): Unit = GLES20.glVertexAttrib2f(indx, x, y)

  override def glVertexAttrib2fv(indx: Int, values: FloatBuffer): Unit = GLES20.glVertexAttrib2fv(indx, values)

  override def glVertexAttrib3f(indx: Int, x: Float, y: Float, z: Float): Unit =
    GLES20.glVertexAttrib3f(indx, x, y, z)

  override def glVertexAttrib3fv(indx: Int, values: FloatBuffer): Unit = GLES20.glVertexAttrib3fv(indx, values)

  override def glVertexAttrib4f(indx: Int, x: Float, y: Float, z: Float, w: Float): Unit =
    GLES20.glVertexAttrib4f(indx, x, y, z, w)

  override def glVertexAttrib4fv(indx: Int, values: FloatBuffer): Unit = GLES20.glVertexAttrib4fv(indx, values)

  override def glVertexAttribPointer(indx: Int, size: Int, `type`: Int, normalized: Boolean, stride: Int, ptr: Buffer): Unit =
    GLES20.glVertexAttribPointer(indx, size, `type`, normalized, stride, ptr)

  override def glVertexAttribPointer(indx: Int, size: Int, `type`: Int, normalized: Boolean, stride: Int, ptr: Int): Unit =
    GLES20.glVertexAttribPointer(indx, size, `type`, normalized, stride, ptr)
}
