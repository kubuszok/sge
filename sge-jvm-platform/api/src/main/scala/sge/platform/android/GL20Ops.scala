// SGE — Android GL ES 2.0 ops interface
//
// Self-contained GL20 mirror trait using only JDK types (no sge.* dependencies).
// All opaque types (TextureTarget, BlendFactor, Pixels, etc.) are represented as
// plain Int. Implementations live in sge-jvm-platform-android; sge core bridges
// to sge.graphics.GL20 via an adapter.
//
// Migration notes:
//   Source:  com.badlogic.gdx.backends.android.AndroidGL20
//   Convention: ops interface pattern; all GL enum params as Int
//   Audited: 2026-03-08

package sge
package platform
package android

import java.nio.{ Buffer, FloatBuffer, IntBuffer }

/** GL ES 2.0 operations — JDK-only mirror of sge.graphics.GL20. */
trait GL20Ops {

  def glActiveTexture(texture: Int): Unit

  def glBindTexture(target: Int, texture: Int): Unit

  def glBlendFunc(sfactor: Int, dfactor: Int): Unit

  def glClear(mask: Int): Unit

  def glClearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit

  def glClearDepthf(depth: Float): Unit

  def glClearStencil(s: Int): Unit

  def glColorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit

  def glCompressedTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, imageSize: Int, data: Buffer): Unit

  def glCompressedTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, imageSize: Int, data: Buffer): Unit

  def glCopyTexImage2D(target: Int, level: Int, internalformat: Int, x: Int, y: Int, width: Int, height: Int, border: Int): Unit

  def glCopyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit

  def glCullFace(mode: Int): Unit

  def glDeleteTextures(n: Int, textures: IntBuffer): Unit

  def glDeleteTexture(texture: Int): Unit

  def glDepthFunc(func: Int): Unit

  def glDepthMask(flag: Boolean): Unit

  def glDepthRangef(zNear: Float, zFar: Float): Unit

  def glDisable(cap: Int): Unit

  def glDrawArrays(mode: Int, first: Int, count: Int): Unit

  def glDrawElements(mode: Int, count: Int, `type`: Int, indices: Buffer): Unit

  def glEnable(cap: Int): Unit

  def glFinish(): Unit

  def glFlush(): Unit

  def glFrontFace(mode: Int): Unit

  def glGenTextures(n: Int, textures: IntBuffer): Unit

  def glGenTexture(): Int

  def glGetError(): Int

  def glGetIntegerv(pname: Int, params: IntBuffer): Unit

  def glGetString(name: Int): String

  def glHint(target: Int, mode: Int): Unit

  def glLineWidth(width: Float): Unit

  def glPixelStorei(pname: Int, param: Int): Unit

  def glPolygonOffset(factor: Float, units: Float): Unit

  def glReadPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: Buffer): Unit

  def glScissor(x: Int, y: Int, width: Int, height: Int): Unit

  def glStencilFunc(func: Int, ref: Int, mask: Int): Unit

  def glStencilMask(mask: Int): Unit

  def glStencilOp(fail: Int, zfail: Int, zpass: Int): Unit

  def glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, `type`: Int, pixels: Buffer): Unit

  def glTexParameterf(target: Int, pname: Int, param: Float): Unit

  def glTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: Buffer): Unit

  def glViewport(x: Int, y: Int, width: Int, height: Int): Unit

  def glAttachShader(program: Int, shader: Int): Unit

  def glBindAttribLocation(program: Int, index: Int, name: String): Unit

  def glBindBuffer(target: Int, buffer: Int): Unit

  def glBindFramebuffer(target: Int, framebuffer: Int): Unit

  def glBindRenderbuffer(target: Int, renderbuffer: Int): Unit

  def glBlendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit

  def glBlendEquation(mode: Int): Unit

  def glBlendEquationSeparate(modeRGB: Int, modeAlpha: Int): Unit

  def glBlendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int): Unit

  def glBufferData(target: Int, size: Int, data: Buffer, usage: Int): Unit

  def glBufferSubData(target: Int, offset: Int, size: Int, data: Buffer): Unit

  def glCheckFramebufferStatus(target: Int): Int

  def glCompileShader(shader: Int): Unit

  def glCreateProgram(): Int

  def glCreateShader(`type`: Int): Int

  def glDeleteBuffer(buffer: Int): Unit

  def glDeleteBuffers(n: Int, buffers: IntBuffer): Unit

  def glDeleteFramebuffer(framebuffer: Int): Unit

  def glDeleteFramebuffers(n: Int, framebuffers: IntBuffer): Unit

  def glDeleteProgram(program: Int): Unit

  def glDeleteRenderbuffer(renderbuffer: Int): Unit

  def glDeleteRenderbuffers(n: Int, renderbuffers: IntBuffer): Unit

  def glDeleteShader(shader: Int): Unit

  def glDetachShader(program: Int, shader: Int): Unit

  def glDisableVertexAttribArray(index: Int): Unit

  def glDrawElements(mode: Int, count: Int, `type`: Int, indices: Int): Unit

  def glEnableVertexAttribArray(index: Int): Unit

  def glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int): Unit

  def glFramebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int): Unit

  def glGenBuffer(): Int

  def glGenBuffers(n: Int, buffers: IntBuffer): Unit

  def glGenerateMipmap(target: Int): Unit

  def glGenFramebuffer(): Int

  def glGenFramebuffers(n: Int, framebuffers: IntBuffer): Unit

  def glGenRenderbuffer(): Int

  def glGenRenderbuffers(n: Int, renderbuffers: IntBuffer): Unit

  def glGetActiveAttrib(program: Int, index: Int, size: IntBuffer, `type`: IntBuffer): String

  def glGetActiveUniform(program: Int, index: Int, size: IntBuffer, `type`: IntBuffer): String

  def glGetAttachedShaders(program: Int, maxcount: Int, count: Buffer, shaders: IntBuffer): Unit

  def glGetAttribLocation(program: Int, name: String): Int

  def glGetBooleanv(pname: Int, params: Buffer): Unit

  def glGetBufferParameteriv(target: Int, pname: Int, params: IntBuffer): Unit

  def glGetFloatv(pname: Int, params: FloatBuffer): Unit

  def glGetFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: IntBuffer): Unit

  def glGetProgramiv(program: Int, pname: Int, params: IntBuffer): Unit

  def glGetProgramInfoLog(program: Int): String

  def glGetRenderbufferParameteriv(target: Int, pname: Int, params: IntBuffer): Unit

  def glGetShaderiv(shader: Int, pname: Int, params: IntBuffer): Unit

  def glGetShaderInfoLog(shader: Int): String

  def glGetShaderPrecisionFormat(shadertype: Int, precisiontype: Int, range: IntBuffer, precision: IntBuffer): Unit

  def glGetTexParameterfv(target: Int, pname: Int, params: FloatBuffer): Unit

  def glGetTexParameteriv(target: Int, pname: Int, params: IntBuffer): Unit

  def glGetUniformfv(program: Int, location: Int, params: FloatBuffer): Unit

  def glGetUniformiv(program: Int, location: Int, params: IntBuffer): Unit

  def glGetUniformLocation(program: Int, name: String): Int

  def glGetVertexAttribfv(index: Int, pname: Int, params: FloatBuffer): Unit

  def glGetVertexAttribiv(index: Int, pname: Int, params: IntBuffer): Unit

  def glGetVertexAttribPointerv(index: Int, pname: Int, pointer: Buffer): Unit

  def glIsBuffer(buffer: Int): Boolean

  def glIsEnabled(cap: Int): Boolean

  def glIsFramebuffer(framebuffer: Int): Boolean

  def glIsProgram(program: Int): Boolean

  def glIsRenderbuffer(renderbuffer: Int): Boolean

  def glIsShader(shader: Int): Boolean

  def glIsTexture(texture: Int): Boolean

  def glLinkProgram(program: Int): Unit

  def glReleaseShaderCompiler(): Unit

  def glRenderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int): Unit

  def glSampleCoverage(value: Float, invert: Boolean): Unit

  def glShaderBinary(n: Int, shaders: IntBuffer, binaryformat: Int, binary: Buffer, length: Int): Unit

  def glShaderSource(shader: Int, string: String): Unit

  def glStencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int): Unit

  def glStencilMaskSeparate(face: Int, mask: Int): Unit

  def glStencilOpSeparate(face: Int, fail: Int, zfail: Int, zpass: Int): Unit

  def glTexParameterfv(target: Int, pname: Int, params: FloatBuffer): Unit

  def glTexParameteri(target: Int, pname: Int, param: Int): Unit

  def glTexParameteriv(target: Int, pname: Int, params: IntBuffer): Unit

  def glUniform1f(location: Int, x: Float): Unit

  def glUniform1fv(location: Int, count: Int, v: FloatBuffer): Unit

  def glUniform1fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit

  def glUniform1i(location: Int, x: Int): Unit

  def glUniform1iv(location: Int, count: Int, v: IntBuffer): Unit

  def glUniform1iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit

  def glUniform2f(location: Int, x: Float, y: Float): Unit

  def glUniform2fv(location: Int, count: Int, v: FloatBuffer): Unit

  def glUniform2fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit

  def glUniform2i(location: Int, x: Int, y: Int): Unit

  def glUniform2iv(location: Int, count: Int, v: IntBuffer): Unit

  def glUniform2iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit

  def glUniform3f(location: Int, x: Float, y: Float, z: Float): Unit

  def glUniform3fv(location: Int, count: Int, v: FloatBuffer): Unit

  def glUniform3fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit

  def glUniform3i(location: Int, x: Int, y: Int, z: Int): Unit

  def glUniform3iv(location: Int, count: Int, v: IntBuffer): Unit

  def glUniform3iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit

  def glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float): Unit

  def glUniform4fv(location: Int, count: Int, v: FloatBuffer): Unit

  def glUniform4fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit

  def glUniform4i(location: Int, x: Int, y: Int, z: Int, w: Int): Unit

  def glUniform4iv(location: Int, count: Int, v: IntBuffer): Unit

  def glUniform4iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit

  def glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit

  def glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit

  def glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit

  def glUseProgram(program: Int): Unit

  def glValidateProgram(program: Int): Unit

  def glVertexAttrib1f(indx: Int, x: Float): Unit

  def glVertexAttrib1fv(indx: Int, values: FloatBuffer): Unit

  def glVertexAttrib2f(indx: Int, x: Float, y: Float): Unit

  def glVertexAttrib2fv(indx: Int, values: FloatBuffer): Unit

  def glVertexAttrib3f(indx: Int, x: Float, y: Float, z: Float): Unit

  def glVertexAttrib3fv(indx: Int, values: FloatBuffer): Unit

  def glVertexAttrib4f(indx: Int, x: Float, y: Float, z: Float, w: Float): Unit

  def glVertexAttrib4fv(indx: Int, values: FloatBuffer): Unit

  def glVertexAttribPointer(indx: Int, size: Int, `type`: Int, normalized: Boolean, stride: Int, ptr: Buffer): Unit

  def glVertexAttribPointer(indx: Int, size: Int, `type`: Int, normalized: Boolean, stride: Int, ptr: Int): Unit
}
