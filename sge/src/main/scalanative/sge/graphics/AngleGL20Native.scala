/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (replaces Lwjgl3GL20.java)
 *   Convention: Scala Native @extern bindings to ANGLE libGLESv2
 *   Convention: Buffer -> Ptr[Byte] via NativeGlHelper.bufPtr
 *   Convention: String -> CString via Zone; GLboolean -> CUnsignedChar
 *   Idiom: split packages; no return
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import java.nio.{ Buffer, FloatBuffer, IntBuffer }

import scala.scalanative.unsafe.*

import NativeGlHelper.*

// ─── C extern declarations ──────────────────────────────────────────────────

@link("GLESv2")
@extern
private[graphics] object GL20C {
  // Texture state
  def glActiveTexture(texture:          CInt):                                                                                                            Unit          = extern
  def glBindTexture(target:             CInt, texture:  CInt):                                                                                            Unit          = extern
  def glGenTextures(n:                  CInt, textures: Ptr[CInt]):                                                                                       Unit          = extern
  def glDeleteTextures(n:               CInt, textures: Ptr[CInt]):                                                                                       Unit          = extern
  def glIsTexture(texture:              CInt):                                                                                                            CUnsignedChar = extern
  def glTexImage2D(target:              CInt, level:    CInt, intfmt: CInt, w:  CInt, h: CInt, border: CInt, fmt: CInt, tp:     CInt, pixels: Ptr[Byte]): Unit          = extern
  def glTexSubImage2D(target:           CInt, level:    CInt, xo:     CInt, yo: CInt, w: CInt, h:      CInt, fmt: CInt, tp:     CInt, pixels: Ptr[Byte]): Unit          = extern
  def glCopyTexImage2D(target:          CInt, level:    CInt, intfmt: CInt, x:  CInt, y: CInt, w:      CInt, h:   CInt, border: CInt):                    Unit          = extern
  def glCopyTexSubImage2D(target:       CInt, level:    CInt, xo:     CInt, yo: CInt, x: CInt, y:      CInt, w:   CInt, h:      CInt):                    Unit          = extern
  def glCompressedTexImage2D(target:    CInt, level:    CInt, intfmt: CInt, w:  CInt, h: CInt, border: CInt, sz:  CInt, data:   Ptr[Byte]):               Unit          = extern
  def glCompressedTexSubImage2D(target: CInt, level:    CInt, xo:     CInt, yo: CInt, w: CInt, h:      CInt, fmt: CInt, sz:     CInt, data:   Ptr[Byte]): Unit          = extern
  def glTexParameterf(target:           CInt, pname:    CInt, param:  CFloat):                                                                            Unit          = extern
  def glTexParameteri(target:           CInt, pname:    CInt, param:  CInt):                                                                              Unit          = extern
  def glTexParameterfv(target:          CInt, pname:    CInt, params: Ptr[CFloat]):                                                                       Unit          = extern
  def glTexParameteriv(target:          CInt, pname:    CInt, params: Ptr[CInt]):                                                                         Unit          = extern
  def glGetTexParameterfv(target:       CInt, pname:    CInt, params: Ptr[CFloat]):                                                                       Unit          = extern
  def glGetTexParameteriv(target:       CInt, pname:    CInt, params: Ptr[CInt]):                                                                         Unit          = extern
  def glGenerateMipmap(target:          CInt):                                                                                                            Unit          = extern

  // Blend / Color / Depth / Stencil
  def glBlendFunc(sfactor:             CInt, dfactor:    CInt):                                              Unit = extern
  def glBlendFuncSeparate(srcRGB:      CInt, dstRGB:     CInt, srcA:       CInt, dstA:       CInt):          Unit = extern
  def glBlendColor(r:                  CFloat, g:        CFloat, b:        CFloat, a:        CFloat):        Unit = extern
  def glBlendEquation(mode:            CInt):                                                                Unit = extern
  def glBlendEquationSeparate(modeRGB: CInt, modeA:      CInt):                                              Unit = extern
  def glClear(mask:                    CInt):                                                                Unit = extern
  def glClearColor(r:                  CFloat, g:        CFloat, b:        CFloat, a:        CFloat):        Unit = extern
  def glClearDepthf(depth:             CFloat):                                                              Unit = extern
  def glClearStencil(s:                CInt):                                                                Unit = extern
  def glColorMask(r:                   CUnsignedChar, g: CUnsignedChar, b: CUnsignedChar, a: CUnsignedChar): Unit = extern
  def glDepthFunc(func:                CInt):                                                                Unit = extern
  def glDepthMask(flag:                CUnsignedChar):                                                       Unit = extern
  def glDepthRangef(zNear:             CFloat, zFar:     CFloat):                                            Unit = extern
  def glStencilFunc(func:              CInt, ref:        CInt, mask:       CInt):                            Unit = extern
  def glStencilFuncSeparate(face:      CInt, func:       CInt, ref:        CInt, mask:       CInt):          Unit = extern
  def glStencilMask(mask:              CInt):                                                                Unit = extern
  def glStencilMaskSeparate(face:      CInt, mask:       CInt):                                              Unit = extern
  def glStencilOp(fail:                CInt, zfail:      CInt, zpass:      CInt):                            Unit = extern
  def glStencilOpSeparate(face:        CInt, fail:       CInt, zfail:      CInt, zpass:      CInt):          Unit = extern

  // Enable / Disable / Viewport
  def glEnable(cap:           CInt):                                   Unit          = extern
  def glDisable(cap:          CInt):                                   Unit          = extern
  def glIsEnabled(cap:        CInt):                                   CUnsignedChar = extern
  def glViewport(x:           CInt, y:        CInt, w: CInt, h: CInt): Unit          = extern
  def glScissor(x:            CInt, y:        CInt, w: CInt, h: CInt): Unit          = extern
  def glCullFace(mode:        CInt):                                   Unit          = extern
  def glFrontFace(mode:       CInt):                                   Unit          = extern
  def glLineWidth(width:      CFloat):                                 Unit          = extern
  def glPolygonOffset(factor: CFloat, units:  CFloat):                 Unit          = extern
  def glPixelStorei(pname:    CInt, param:    CInt):                   Unit          = extern
  def glHint(target:          CInt, mode:     CInt):                   Unit          = extern
  def glSampleCoverage(value: CFloat, invert: CUnsignedChar):          Unit          = extern
  def glFinish():                                                      Unit          = extern
  def glFlush():                                                       Unit          = extern

  // Get state
  def glGetError():                                    CInt    = extern
  def glGetIntegerv(pname: CInt, params: Ptr[CInt]):   Unit    = extern
  def glGetFloatv(pname:   CInt, params: Ptr[CFloat]): Unit    = extern
  def glGetBooleanv(pname: CInt, params: Ptr[Byte]):   Unit    = extern
  def glGetString(name:    CInt):                      CString = extern

  // Draw
  def glDrawArrays(mode:   CInt, first: CInt, count: CInt):                                                        Unit = extern
  def glDrawElements(mode: CInt, count: CInt, tp:    CInt, indices: Ptr[Byte]):                                    Unit = extern
  def glReadPixels(x:      CInt, y:     CInt, w:     CInt, h:       CInt, fmt: CInt, tp: CInt, pixels: Ptr[Byte]): Unit = extern

  // Buffer objects
  def glGenBuffers(n:                CInt, buffers: Ptr[CInt]):                                 Unit          = extern
  def glDeleteBuffers(n:             CInt, buffers: Ptr[CInt]):                                 Unit          = extern
  def glBindBuffer(target:           CInt, buffer:  CInt):                                      Unit          = extern
  def glBufferData(target:           CInt, size:    CInt, data:   Ptr[Byte], usage: CInt):      Unit          = extern
  def glBufferSubData(target:        CInt, offset:  CInt, size:   CInt, data:       Ptr[Byte]): Unit          = extern
  def glIsBuffer(buffer:             CInt):                                                     CUnsignedChar = extern
  def glGetBufferParameteriv(target: CInt, pname:   CInt, params: Ptr[CInt]):                   Unit          = extern

  // Framebuffer objects
  def glGenFramebuffers(n:                          CInt, fbs:    Ptr[CInt]):                                         Unit          = extern
  def glDeleteFramebuffers(n:                       CInt, fbs:    Ptr[CInt]):                                         Unit          = extern
  def glBindFramebuffer(target:                     CInt, fb:     CInt):                                              Unit          = extern
  def glCheckFramebufferStatus(target:              CInt):                                                            CInt          = extern
  def glFramebufferTexture2D(target:                CInt, attach: CInt, textarget: CInt, texture: CInt, level: CInt): Unit          = extern
  def glFramebufferRenderbuffer(target:             CInt, attach: CInt, rbtarget:  CInt, rb:      CInt):              Unit          = extern
  def glGetFramebufferAttachmentParameteriv(target: CInt, attach: CInt, pname:     CInt, params:  Ptr[CInt]):         Unit          = extern
  def glIsFramebuffer(fb:                           CInt):                                                            CUnsignedChar = extern

  // Renderbuffer objects
  def glGenRenderbuffers(n:                CInt, rbs:    Ptr[CInt]):                   Unit          = extern
  def glDeleteRenderbuffers(n:             CInt, rbs:    Ptr[CInt]):                   Unit          = extern
  def glBindRenderbuffer(target:           CInt, rb:     CInt):                        Unit          = extern
  def glRenderbufferStorage(target:        CInt, intfmt: CInt, w:      CInt, h: CInt): Unit          = extern
  def glGetRenderbufferParameteriv(target: CInt, pname:  CInt, params: Ptr[CInt]):     Unit          = extern
  def glIsRenderbuffer(rb:                 CInt):                                      CUnsignedChar = extern

  // Shader / Program
  def glCreateShader(tp:            CInt):                                                          CInt          = extern
  def glDeleteShader(shader:        CInt):                                                          Unit          = extern
  def glIsShader(shader:            CInt):                                                          CUnsignedChar = extern
  def glShaderSource(shader:        CInt, count:    CInt, string: Ptr[CString], length: Ptr[CInt]): Unit          = extern
  def glCompileShader(shader:       CInt):                                                          Unit          = extern
  def glGetShaderiv(shader:         CInt, pname:    CInt, params: Ptr[CInt]):                       Unit          = extern
  def glGetShaderInfoLog(shader:    CInt, bufSize:  CInt, length: Ptr[CInt], infoLog:   CString):   Unit          = extern
  def glCreateProgram():                                                                            CInt          = extern
  def glDeleteProgram(program:      CInt):                                                          Unit          = extern
  def glIsProgram(program:          CInt):                                                          CUnsignedChar = extern
  def glAttachShader(program:       CInt, shader:   CInt):                                          Unit          = extern
  def glDetachShader(program:       CInt, shader:   CInt):                                          Unit          = extern
  def glLinkProgram(program:        CInt):                                                          Unit          = extern
  def glUseProgram(program:         CInt):                                                          Unit          = extern
  def glValidateProgram(program:    CInt):                                                          Unit          = extern
  def glGetProgramiv(program:       CInt, pname:    CInt, params: Ptr[CInt]):                       Unit          = extern
  def glGetProgramInfoLog(program:  CInt, bufSize:  CInt, length: Ptr[CInt], infoLog:   CString):   Unit          = extern
  def glGetAttachedShaders(program: CInt, maxcount: CInt, count:  Ptr[CInt], shaders:   Ptr[CInt]): Unit          = extern

  // Attributes
  def glBindAttribLocation(program:     CInt, index: CInt, name:    CString):                                                                Unit = extern
  def glGetAttribLocation(program:      CInt, name:  CString):                                                                               CInt = extern
  def glGetActiveAttrib(program:        CInt, index: CInt, bufSize: CInt, length: Ptr[CInt], size: Ptr[CInt], tp: Ptr[CInt], name: CString): Unit = extern
  def glEnableVertexAttribArray(index:  CInt):                                                                                               Unit = extern
  def glDisableVertexAttribArray(index: CInt):                                                                                               Unit = extern
  def glGetVertexAttribfv(index:        CInt, pname: CInt, params:  Ptr[CFloat]):                                                            Unit = extern
  def glGetVertexAttribiv(index:        CInt, pname: CInt, params:  Ptr[CInt]):                                                              Unit = extern

  // Vertex attrib values
  def glVertexAttrib1f(indx:      CInt, x:    CFloat):                                                             Unit = extern
  def glVertexAttrib2f(indx:      CInt, x:    CFloat, y: CFloat):                                                  Unit = extern
  def glVertexAttrib3f(indx:      CInt, x:    CFloat, y: CFloat, z:  CFloat):                                      Unit = extern
  def glVertexAttrib4f(indx:      CInt, x:    CFloat, y: CFloat, z:  CFloat, w:             CFloat):               Unit = extern
  def glVertexAttribPointer(indx: CInt, size: CInt, tp:  CInt, norm: CUnsignedChar, stride: CInt, ptr: Ptr[Byte]): Unit = extern

  // Uniforms
  def glGetUniformLocation(program: CInt, name:     CString):                                                                               CInt = extern
  def glGetActiveUniform(program:   CInt, index:    CInt, bufSize: CInt, length: Ptr[CInt], size: Ptr[CInt], tp: Ptr[CInt], name: CString): Unit = extern
  def glGetUniformfv(program:       CInt, location: CInt, params:  Ptr[CFloat]):                                                            Unit = extern
  def glGetUniformiv(program:       CInt, location: CInt, params:  Ptr[CInt]):                                                              Unit = extern

  // Uniform setters
  def glUniform1f(loc:  CInt, x:     CFloat):                                  Unit = extern
  def glUniform2f(loc:  CInt, x:     CFloat, y: CFloat):                       Unit = extern
  def glUniform3f(loc:  CInt, x:     CFloat, y: CFloat, z: CFloat):            Unit = extern
  def glUniform4f(loc:  CInt, x:     CFloat, y: CFloat, z: CFloat, w: CFloat): Unit = extern
  def glUniform1i(loc:  CInt, x:     CInt):                                    Unit = extern
  def glUniform2i(loc:  CInt, x:     CInt, y:   CInt):                         Unit = extern
  def glUniform3i(loc:  CInt, x:     CInt, y:   CInt, z:   CInt):              Unit = extern
  def glUniform4i(loc:  CInt, x:     CInt, y:   CInt, z:   CInt, w:   CInt):   Unit = extern
  def glUniform1fv(loc: CInt, count: CInt, v:   Ptr[CFloat]):                  Unit = extern
  def glUniform2fv(loc: CInt, count: CInt, v:   Ptr[CFloat]):                  Unit = extern
  def glUniform3fv(loc: CInt, count: CInt, v:   Ptr[CFloat]):                  Unit = extern
  def glUniform4fv(loc: CInt, count: CInt, v:   Ptr[CFloat]):                  Unit = extern
  def glUniform1iv(loc: CInt, count: CInt, v:   Ptr[CInt]):                    Unit = extern
  def glUniform2iv(loc: CInt, count: CInt, v:   Ptr[CInt]):                    Unit = extern
  def glUniform3iv(loc: CInt, count: CInt, v:   Ptr[CInt]):                    Unit = extern
  def glUniform4iv(loc: CInt, count: CInt, v:   Ptr[CInt]):                    Unit = extern

  // Uniform matrices
  def glUniformMatrix2fv(loc: CInt, count: CInt, transpose: CUnsignedChar, value: Ptr[CFloat]): Unit = extern
  def glUniformMatrix3fv(loc: CInt, count: CInt, transpose: CUnsignedChar, value: Ptr[CFloat]): Unit = extern
  def glUniformMatrix4fv(loc: CInt, count: CInt, transpose: CUnsignedChar, value: Ptr[CFloat]): Unit = extern
}

// ─── GL20 wrapper ─────────────────────────────────────────────────────────────

/** OpenGL ES 2.0 implementation via ANGLE (libGLESv2) using Scala Native @extern bindings.
  *
  * Each GL method delegates to the corresponding C function via the GL20C @extern object. Buffer parameters are converted to native pointers via NativeGlHelper.bufPtr.
  */
class AngleGL20Native extends GL20 {

  // ─── Texture state ────────────────────────────────────────────────────────

  override def glActiveTexture(texture: Int): Unit = GL20C.glActiveTexture(texture)

  override def glBindTexture(target: TextureTarget, texture: Int): Unit = GL20C.glBindTexture(target.toInt, texture)

  override def glGenTextures(n: Int, textures: IntBuffer): Unit =
    GL20C.glGenTextures(n, bufPtr(textures).asInstanceOf[Ptr[CInt]])

  override def glGenTexture(): Int = {
    val buf = allocDirectInt(1)
    glGenTextures(1, buf)
    buf.get(0)
  }

  override def glDeleteTextures(n: Int, textures: IntBuffer): Unit =
    GL20C.glDeleteTextures(n, bufPtr(textures).asInstanceOf[Ptr[CInt]])

  override def glDeleteTexture(texture: Int): Unit = {
    val buf = allocDirectInt(1)
    buf.put(0, texture)
    glDeleteTextures(1, buf)
  }

  override def glIsTexture(texture: Int): Boolean = fromGlBool(GL20C.glIsTexture(texture))

  override def glTexImage2D(target: TextureTarget, level: Int, internalformat: Int, width: Pixels, height: Pixels, border: Int, format: PixelFormat, `type`: DataType, pixels: Buffer): Unit =
    GL20C.glTexImage2D(target.toInt, level, internalformat, width.toInt, height.toInt, border, format.toInt, `type`.toInt, bufPtr(pixels))

  override def glTexSubImage2D(target: TextureTarget, level: Int, xoffset: Pixels, yoffset: Pixels, width: Pixels, height: Pixels, format: PixelFormat, `type`: DataType, pixels: Buffer): Unit =
    GL20C.glTexSubImage2D(target.toInt, level, xoffset.toInt, yoffset.toInt, width.toInt, height.toInt, format.toInt, `type`.toInt, bufPtr(pixels))

  override def glCopyTexImage2D(target: TextureTarget, level: Int, internalformat: Int, x: Pixels, y: Pixels, width: Pixels, height: Pixels, border: Int): Unit =
    GL20C.glCopyTexImage2D(target.toInt, level, internalformat, x.toInt, y.toInt, width.toInt, height.toInt, border)

  override def glCopyTexSubImage2D(target: TextureTarget, level: Int, xoffset: Pixels, yoffset: Pixels, x: Pixels, y: Pixels, width: Pixels, height: Pixels): Unit =
    GL20C.glCopyTexSubImage2D(target.toInt, level, xoffset.toInt, yoffset.toInt, x.toInt, y.toInt, width.toInt, height.toInt)

  override def glCompressedTexImage2D(target: TextureTarget, level: Int, internalformat: Int, width: Pixels, height: Pixels, border: Int, imageSize: Int, data: Buffer): Unit =
    GL20C.glCompressedTexImage2D(target.toInt, level, internalformat, width.toInt, height.toInt, border, imageSize, bufPtr(data))

  override def glCompressedTexSubImage2D(target: TextureTarget, level: Int, xoffset: Pixels, yoffset: Pixels, width: Pixels, height: Pixels, format: PixelFormat, imageSize: Int, data: Buffer): Unit =
    GL20C.glCompressedTexSubImage2D(target.toInt, level, xoffset.toInt, yoffset.toInt, width.toInt, height.toInt, format.toInt, imageSize, bufPtr(data))

  override def glTexParameterf(target: TextureTarget, pname: Int, param: Float): Unit = GL20C.glTexParameterf(target.toInt, pname, param)

  override def glTexParameteri(target: TextureTarget, pname: Int, param: Int): Unit = GL20C.glTexParameteri(target.toInt, pname, param)

  override def glTexParameterfv(target: TextureTarget, pname: Int, params: FloatBuffer): Unit =
    GL20C.glTexParameterfv(target.toInt, pname, bufPtr(params).asInstanceOf[Ptr[CFloat]])

  override def glTexParameteriv(target: TextureTarget, pname: Int, params: IntBuffer): Unit =
    GL20C.glTexParameteriv(target.toInt, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])

  override def glGetTexParameterfv(target: TextureTarget, pname: Int, params: FloatBuffer): Unit =
    GL20C.glGetTexParameterfv(target.toInt, pname, bufPtr(params).asInstanceOf[Ptr[CFloat]])

  override def glGetTexParameteriv(target: TextureTarget, pname: Int, params: IntBuffer): Unit =
    GL20C.glGetTexParameteriv(target.toInt, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])

  override def glGenerateMipmap(target: TextureTarget): Unit = GL20C.glGenerateMipmap(target.toInt)

  // ─── Blend / Color / Depth / Stencil state ────────────────────────────────

  override def glBlendFunc(sfactor: BlendFactor, dfactor: BlendFactor): Unit = GL20C.glBlendFunc(sfactor.toInt, dfactor.toInt)

  override def glBlendFuncSeparate(srcRGB: BlendFactor, dstRGB: BlendFactor, srcAlpha: BlendFactor, dstAlpha: BlendFactor): Unit =
    GL20C.glBlendFuncSeparate(srcRGB.toInt, dstRGB.toInt, srcAlpha.toInt, dstAlpha.toInt)

  override def glBlendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit =
    GL20C.glBlendColor(red, green, blue, alpha)

  override def glBlendEquation(mode: BlendEquation): Unit = GL20C.glBlendEquation(mode.toInt)

  override def glBlendEquationSeparate(modeRGB: BlendEquation, modeAlpha: BlendEquation): Unit =
    GL20C.glBlendEquationSeparate(modeRGB.toInt, modeAlpha.toInt)

  override def glClear(mask: ClearMask): Unit = GL20C.glClear(mask.toInt)

  override def glClearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit =
    GL20C.glClearColor(red, green, blue, alpha)

  override def glClearDepthf(depth: Float): Unit = GL20C.glClearDepthf(depth)

  override def glClearStencil(s: Int): Unit = GL20C.glClearStencil(s)

  override def glColorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit =
    GL20C.glColorMask(glBool(red), glBool(green), glBool(blue), glBool(alpha))

  override def glDepthFunc(func: CompareFunc): Unit = GL20C.glDepthFunc(func.toInt)

  override def glDepthMask(flag: Boolean): Unit = GL20C.glDepthMask(glBool(flag))

  override def glDepthRangef(zNear: Float, zFar: Float): Unit = GL20C.glDepthRangef(zNear, zFar)

  override def glStencilFunc(func: CompareFunc, ref: Int, mask: Int): Unit = GL20C.glStencilFunc(func.toInt, ref, mask)

  override def glStencilFuncSeparate(face: CullFace, func: CompareFunc, ref: Int, mask: Int): Unit =
    GL20C.glStencilFuncSeparate(face.toInt, func.toInt, ref, mask)

  override def glStencilMask(mask: Int): Unit = GL20C.glStencilMask(mask)

  override def glStencilMaskSeparate(face: CullFace, mask: Int): Unit = GL20C.glStencilMaskSeparate(face.toInt, mask)

  override def glStencilOp(fail: StencilOp, zfail: StencilOp, zpass: StencilOp): Unit = GL20C.glStencilOp(fail.toInt, zfail.toInt, zpass.toInt)

  override def glStencilOpSeparate(face: CullFace, fail: StencilOp, zfail: StencilOp, zpass: StencilOp): Unit =
    GL20C.glStencilOpSeparate(face.toInt, fail.toInt, zfail.toInt, zpass.toInt)

  // ─── Enable / Disable / Viewport / Scissor ────────────────────────────────

  override def glEnable(cap: EnableCap): Unit = GL20C.glEnable(cap.toInt)

  override def glDisable(cap: EnableCap): Unit = GL20C.glDisable(cap.toInt)

  override def glIsEnabled(cap: EnableCap): Boolean = fromGlBool(GL20C.glIsEnabled(cap.toInt))

  override def glViewport(x: Pixels, y: Pixels, width: Pixels, height: Pixels): Unit = GL20C.glViewport(x.toInt, y.toInt, width.toInt, height.toInt)

  override def glScissor(x: Pixels, y: Pixels, width: Pixels, height: Pixels): Unit = GL20C.glScissor(x.toInt, y.toInt, width.toInt, height.toInt)

  override def glCullFace(mode: CullFace): Unit = GL20C.glCullFace(mode.toInt)

  override def glFrontFace(mode: Int): Unit = GL20C.glFrontFace(mode)

  override def glLineWidth(width: Float): Unit = GL20C.glLineWidth(width)

  override def glPolygonOffset(factor: Float, units: Float): Unit = GL20C.glPolygonOffset(factor, units)

  override def glPixelStorei(pname: Int, param: Int): Unit = GL20C.glPixelStorei(pname, param)

  override def glHint(target: Int, mode: Int): Unit = GL20C.glHint(target, mode)

  override def glSampleCoverage(value: Float, invert: Boolean): Unit = GL20C.glSampleCoverage(value, glBool(invert))

  override def glFinish(): Unit = GL20C.glFinish()

  override def glFlush(): Unit = GL20C.glFlush()

  // ─── Get state ────────────────────────────────────────────────────────────

  override def glGetError(): Int = GL20C.glGetError()

  override def glGetIntegerv(pname: Int, params: IntBuffer): Unit =
    GL20C.glGetIntegerv(pname, bufPtr(params).asInstanceOf[Ptr[CInt]])

  override def glGetFloatv(pname: Int, params: FloatBuffer): Unit =
    GL20C.glGetFloatv(pname, bufPtr(params).asInstanceOf[Ptr[CFloat]])

  override def glGetBooleanv(pname: Int, params: Buffer): Unit =
    GL20C.glGetBooleanv(pname, bufPtr(params))

  override def glGetString(name: Int): String = {
    val ptr = GL20C.glGetString(name)
    if (ptr == null) null // @nowarn - GL interop boundary
    else fromCString(ptr)
  }

  // ─── Draw ─────────────────────────────────────────────────────────────────

  override def glDrawArrays(mode: PrimitiveMode, first: Int, count: Int): Unit = GL20C.glDrawArrays(mode.toInt, first, count)

  override def glDrawElements(mode: PrimitiveMode, count: Int, `type`: DataType, indices: Buffer): Unit =
    GL20C.glDrawElements(mode.toInt, count, `type`.toInt, bufPtr(indices))

  override def glDrawElements(mode: PrimitiveMode, count: Int, `type`: DataType, indices: Int): Unit =
    GL20C.glDrawElements(mode.toInt, count, `type`.toInt, offsetPtr(indices))

  override def glReadPixels(x: Pixels, y: Pixels, width: Pixels, height: Pixels, format: PixelFormat, `type`: DataType, pixels: Buffer): Unit =
    GL20C.glReadPixels(x.toInt, y.toInt, width.toInt, height.toInt, format.toInt, `type`.toInt, bufPtr(pixels))

  // ─── Buffer objects ───────────────────────────────────────────────────────

  override def glGenBuffers(n: Int, buffers: IntBuffer): Unit =
    GL20C.glGenBuffers(n, bufPtr(buffers).asInstanceOf[Ptr[CInt]])

  override def glGenBuffer(): Int = {
    val buf = allocDirectInt(1)
    glGenBuffers(1, buf)
    buf.get(0)
  }

  override def glDeleteBuffers(n: Int, buffers: IntBuffer): Unit =
    GL20C.glDeleteBuffers(n, bufPtr(buffers).asInstanceOf[Ptr[CInt]])

  override def glDeleteBuffer(buffer: Int): Unit = {
    val buf = allocDirectInt(1)
    buf.put(0, buffer)
    glDeleteBuffers(1, buf)
  }

  override def glBindBuffer(target: BufferTarget, buffer: Int): Unit = GL20C.glBindBuffer(target.toInt, buffer)

  override def glBufferData(target: BufferTarget, size: Int, data: Buffer, usage: BufferUsage): Unit =
    GL20C.glBufferData(target.toInt, size, bufPtr(data), usage.toInt)

  override def glBufferSubData(target: BufferTarget, offset: Int, size: Int, data: Buffer): Unit =
    GL20C.glBufferSubData(target.toInt, offset, size, bufPtr(data))

  override def glIsBuffer(buffer: Int): Boolean = fromGlBool(GL20C.glIsBuffer(buffer))

  override def glGetBufferParameteriv(target: BufferTarget, pname: Int, params: IntBuffer): Unit =
    GL20C.glGetBufferParameteriv(target.toInt, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])

  // ─── Framebuffer objects ──────────────────────────────────────────────────

  override def glGenFramebuffers(n: Int, framebuffers: IntBuffer): Unit =
    GL20C.glGenFramebuffers(n, bufPtr(framebuffers).asInstanceOf[Ptr[CInt]])

  override def glGenFramebuffer(): Int = {
    val buf = allocDirectInt(1)
    glGenFramebuffers(1, buf)
    buf.get(0)
  }

  override def glDeleteFramebuffers(n: Int, framebuffers: IntBuffer): Unit =
    GL20C.glDeleteFramebuffers(n, bufPtr(framebuffers).asInstanceOf[Ptr[CInt]])

  override def glDeleteFramebuffer(framebuffer: Int): Unit = {
    val buf = allocDirectInt(1)
    buf.put(0, framebuffer)
    glDeleteFramebuffers(1, buf)
  }

  override def glBindFramebuffer(target: Int, framebuffer: Int): Unit = GL20C.glBindFramebuffer(target, framebuffer)

  override def glCheckFramebufferStatus(target: Int): Int = GL20C.glCheckFramebufferStatus(target)

  override def glFramebufferTexture2D(target: Int, attachment: Int, textarget: TextureTarget, texture: Int, level: Int): Unit =
    GL20C.glFramebufferTexture2D(target, attachment, textarget.toInt, texture, level)

  override def glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int): Unit =
    GL20C.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)

  override def glGetFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: IntBuffer): Unit =
    GL20C.glGetFramebufferAttachmentParameteriv(target, attachment, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])

  override def glIsFramebuffer(framebuffer: Int): Boolean = fromGlBool(GL20C.glIsFramebuffer(framebuffer))

  // ─── Renderbuffer objects ─────────────────────────────────────────────────

  override def glGenRenderbuffers(n: Int, renderbuffers: IntBuffer): Unit =
    GL20C.glGenRenderbuffers(n, bufPtr(renderbuffers).asInstanceOf[Ptr[CInt]])

  override def glGenRenderbuffer(): Int = {
    val buf = allocDirectInt(1)
    glGenRenderbuffers(1, buf)
    buf.get(0)
  }

  override def glDeleteRenderbuffers(n: Int, renderbuffers: IntBuffer): Unit =
    GL20C.glDeleteRenderbuffers(n, bufPtr(renderbuffers).asInstanceOf[Ptr[CInt]])

  override def glDeleteRenderbuffer(renderbuffer: Int): Unit = {
    val buf = allocDirectInt(1)
    buf.put(0, renderbuffer)
    glDeleteRenderbuffers(1, buf)
  }

  override def glBindRenderbuffer(target: Int, renderbuffer: Int): Unit = GL20C.glBindRenderbuffer(target, renderbuffer)

  override def glRenderbufferStorage(target: Int, internalformat: Int, width: Pixels, height: Pixels): Unit =
    GL20C.glRenderbufferStorage(target, internalformat, width.toInt, height.toInt)

  override def glGetRenderbufferParameteriv(target: Int, pname: Int, params: IntBuffer): Unit =
    GL20C.glGetRenderbufferParameteriv(target, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])

  override def glIsRenderbuffer(renderbuffer: Int): Boolean = fromGlBool(GL20C.glIsRenderbuffer(renderbuffer))

  // ─── Shader / Program ─────────────────────────────────────────────────────

  override def glCreateShader(`type`: ShaderType): Int = GL20C.glCreateShader(`type`.toInt)

  override def glDeleteShader(shader: Int): Unit = GL20C.glDeleteShader(shader)

  override def glIsShader(shader: Int): Boolean = fromGlBool(GL20C.glIsShader(shader))

  override def glShaderSource(shader: Int, string: String): Unit = {
    val zone = Zone.open()
    try {
      val cstr   = toCString(string)(using zone)
      val strPtr = stackalloc[CString]()
      !strPtr = cstr
      GL20C.glShaderSource(shader, 1, strPtr, null)
    } finally zone.close()
  }

  override def glCompileShader(shader: Int): Unit = GL20C.glCompileShader(shader)

  override def glGetShaderiv(shader: Int, pname: Int, params: IntBuffer): Unit =
    GL20C.glGetShaderiv(shader, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])

  override def glGetShaderInfoLog(shader: Int): String = {
    val logBuf    = stackalloc[Byte](10240)
    val lengthBuf = stackalloc[CInt]()
    GL20C.glGetShaderInfoLog(shader, 10240, lengthBuf, logBuf)
    val len = !lengthBuf
    if (len <= 0) ""
    else fromCString(logBuf)
  }

  override def glGetShaderPrecisionFormat(shadertype: ShaderType, precisiontype: Int, range: IntBuffer, precision: IntBuffer): Unit =
    throw new UnsupportedOperationException("glGetShaderPrecisionFormat not supported on desktop")

  override def glShaderBinary(n: Int, shaders: IntBuffer, binaryformat: Int, binary: Buffer, length: Int): Unit =
    throw new UnsupportedOperationException("glShaderBinary not supported on desktop")

  override def glReleaseShaderCompiler(): Unit = () // no-op on desktop

  override def glCreateProgram(): Int = GL20C.glCreateProgram()

  override def glDeleteProgram(program: Int): Unit = GL20C.glDeleteProgram(program)

  override def glIsProgram(program: Int): Boolean = fromGlBool(GL20C.glIsProgram(program))

  override def glAttachShader(program: Int, shader: Int): Unit = GL20C.glAttachShader(program, shader)

  override def glDetachShader(program: Int, shader: Int): Unit = GL20C.glDetachShader(program, shader)

  override def glLinkProgram(program: Int): Unit = GL20C.glLinkProgram(program)

  override def glUseProgram(program: Int): Unit = GL20C.glUseProgram(program)

  override def glValidateProgram(program: Int): Unit = GL20C.glValidateProgram(program)

  override def glGetProgramiv(program: Int, pname: Int, params: IntBuffer): Unit =
    GL20C.glGetProgramiv(program, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])

  override def glGetProgramInfoLog(program: Int): String = {
    val logBuf    = stackalloc[Byte](10240)
    val lengthBuf = stackalloc[CInt]()
    GL20C.glGetProgramInfoLog(program, 10240, lengthBuf, logBuf)
    val len = !lengthBuf
    if (len <= 0) ""
    else fromCString(logBuf)
  }

  override def glGetAttachedShaders(program: Int, maxcount: Int, count: Buffer, shaders: IntBuffer): Unit =
    GL20C.glGetAttachedShaders(program, maxcount, bufPtr(count).asInstanceOf[Ptr[CInt]], bufPtr(shaders).asInstanceOf[Ptr[CInt]])

  // ─── Attributes ───────────────────────────────────────────────────────────

  override def glBindAttribLocation(program: Int, index: Int, name: String): Unit = {
    val zone = Zone.open()
    try GL20C.glBindAttribLocation(program, index, toCString(name)(using zone))
    finally zone.close()
  }

  override def glGetAttribLocation(program: Int, name: String): Int = {
    val zone = Zone.open()
    try GL20C.glGetAttribLocation(program, toCString(name)(using zone))
    finally zone.close()
  }

  override def glGetActiveAttrib(program: Int, index: Int, size: IntBuffer, `type`: IntBuffer): String = {
    val nameBuf   = stackalloc[Byte](256)
    val lengthBuf = stackalloc[CInt]()
    val sizeBuf   = stackalloc[CInt]()
    val typeBuf   = stackalloc[CInt]()
    GL20C.glGetActiveAttrib(program, index, 256, lengthBuf, sizeBuf, typeBuf, nameBuf)
    size.put(!sizeBuf)
    `type`.put(!typeBuf)
    fromCString(nameBuf)
  }

  override def glEnableVertexAttribArray(index: Int): Unit = GL20C.glEnableVertexAttribArray(index)

  override def glDisableVertexAttribArray(index: Int): Unit = GL20C.glDisableVertexAttribArray(index)

  override def glGetVertexAttribfv(index: Int, pname: Int, params: FloatBuffer): Unit =
    GL20C.glGetVertexAttribfv(index, pname, bufPtr(params).asInstanceOf[Ptr[CFloat]])

  override def glGetVertexAttribiv(index: Int, pname: Int, params: IntBuffer): Unit =
    GL20C.glGetVertexAttribiv(index, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])

  override def glGetVertexAttribPointerv(index: Int, pname: Int, pointer: Buffer): Unit =
    throw new UnsupportedOperationException("glGetVertexAttribPointerv not supported")

  // ─── Vertex attrib values ─────────────────────────────────────────────────

  override def glVertexAttrib1f(indx: Int, x: Float): Unit = GL20C.glVertexAttrib1f(indx, x)

  override def glVertexAttrib1fv(indx: Int, values: FloatBuffer): Unit =
    glVertexAttrib1f(indx, values.get())

  override def glVertexAttrib2f(indx: Int, x: Float, y: Float): Unit = GL20C.glVertexAttrib2f(indx, x, y)

  override def glVertexAttrib2fv(indx: Int, values: FloatBuffer): Unit =
    glVertexAttrib2f(indx, values.get(), values.get())

  override def glVertexAttrib3f(indx: Int, x: Float, y: Float, z: Float): Unit = GL20C.glVertexAttrib3f(indx, x, y, z)

  override def glVertexAttrib3fv(indx: Int, values: FloatBuffer): Unit =
    glVertexAttrib3f(indx, values.get(), values.get(), values.get())

  override def glVertexAttrib4f(indx: Int, x: Float, y: Float, z: Float, w: Float): Unit = GL20C.glVertexAttrib4f(indx, x, y, z, w)

  override def glVertexAttrib4fv(indx: Int, values: FloatBuffer): Unit =
    glVertexAttrib4f(indx, values.get(), values.get(), values.get(), values.get())

  override def glVertexAttribPointer(indx: Int, size: Int, `type`: DataType, normalized: Boolean, stride: Int, ptr: Buffer): Unit =
    GL20C.glVertexAttribPointer(indx, size, `type`.toInt, glBool(normalized), stride, bufPtr(ptr))

  override def glVertexAttribPointer(indx: Int, size: Int, `type`: DataType, normalized: Boolean, stride: Int, ptr: Int): Unit =
    GL20C.glVertexAttribPointer(indx, size, `type`.toInt, glBool(normalized), stride, offsetPtr(ptr))

  // ─── Uniforms ─────────────────────────────────────────────────────────────

  override def glGetUniformLocation(program: Int, name: String): Int = {
    val zone = Zone.open()
    try GL20C.glGetUniformLocation(program, toCString(name)(using zone))
    finally zone.close()
  }

  override def glGetActiveUniform(program: Int, index: Int, size: IntBuffer, `type`: IntBuffer): String = {
    val nameBuf   = stackalloc[Byte](256)
    val lengthBuf = stackalloc[CInt]()
    val sizeBuf   = stackalloc[CInt]()
    val typeBuf   = stackalloc[CInt]()
    GL20C.glGetActiveUniform(program, index, 256, lengthBuf, sizeBuf, typeBuf, nameBuf)
    size.put(!sizeBuf)
    `type`.put(!typeBuf)
    fromCString(nameBuf)
  }

  override def glGetUniformfv(program: Int, location: Int, params: FloatBuffer): Unit =
    GL20C.glGetUniformfv(program, location, bufPtr(params).asInstanceOf[Ptr[CFloat]])

  override def glGetUniformiv(program: Int, location: Int, params: IntBuffer): Unit =
    GL20C.glGetUniformiv(program, location, bufPtr(params).asInstanceOf[Ptr[CInt]])

  // ─── Uniform setters ──────────────────────────────────────────────────────

  override def glUniform1f(location: Int, x: Float):                               Unit = GL20C.glUniform1f(location, x)
  override def glUniform2f(location: Int, x: Float, y: Float):                     Unit = GL20C.glUniform2f(location, x, y)
  override def glUniform3f(location: Int, x: Float, y: Float, z: Float):           Unit = GL20C.glUniform3f(location, x, y, z)
  override def glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float): Unit = GL20C.glUniform4f(location, x, y, z, w)
  override def glUniform1i(location: Int, x: Int):                                 Unit = GL20C.glUniform1i(location, x)
  override def glUniform2i(location: Int, x: Int, y:   Int):                       Unit = GL20C.glUniform2i(location, x, y)
  override def glUniform3i(location: Int, x: Int, y:   Int, z:   Int):             Unit = GL20C.glUniform3i(location, x, y, z)
  override def glUniform4i(location: Int, x: Int, y:   Int, z:   Int, w:   Int):   Unit = GL20C.glUniform4i(location, x, y, z, w)

  // ─── Uniform vector setters (buffer variants) ─────────────────────────────

  override def glUniform1fv(location: Int, count: Int, v: FloatBuffer): Unit = GL20C.glUniform1fv(location, count, bufPtr(v).asInstanceOf[Ptr[CFloat]])
  override def glUniform2fv(location: Int, count: Int, v: FloatBuffer): Unit = GL20C.glUniform2fv(location, count, bufPtr(v).asInstanceOf[Ptr[CFloat]])
  override def glUniform3fv(location: Int, count: Int, v: FloatBuffer): Unit = GL20C.glUniform3fv(location, count, bufPtr(v).asInstanceOf[Ptr[CFloat]])
  override def glUniform4fv(location: Int, count: Int, v: FloatBuffer): Unit = GL20C.glUniform4fv(location, count, bufPtr(v).asInstanceOf[Ptr[CFloat]])
  override def glUniform1iv(location: Int, count: Int, v: IntBuffer):   Unit = GL20C.glUniform1iv(location, count, bufPtr(v).asInstanceOf[Ptr[CInt]])
  override def glUniform2iv(location: Int, count: Int, v: IntBuffer):   Unit = GL20C.glUniform2iv(location, count, bufPtr(v).asInstanceOf[Ptr[CInt]])
  override def glUniform3iv(location: Int, count: Int, v: IntBuffer):   Unit = GL20C.glUniform3iv(location, count, bufPtr(v).asInstanceOf[Ptr[CInt]])
  override def glUniform4iv(location: Int, count: Int, v: IntBuffer):   Unit = GL20C.glUniform4iv(location, count, bufPtr(v).asInstanceOf[Ptr[CInt]])

  // ─── Uniform vector setters (array variants) ──────────────────────────────

  override def glUniform1fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit =
    GL20C.glUniform1fv(location, count, v.at(offset))

  override def glUniform2fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit =
    GL20C.glUniform2fv(location, count, v.at(offset))

  override def glUniform3fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit =
    GL20C.glUniform3fv(location, count, v.at(offset))

  override def glUniform4fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit =
    GL20C.glUniform4fv(location, count, v.at(offset))

  override def glUniform1iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit =
    GL20C.glUniform1iv(location, count, v.at(offset))

  override def glUniform2iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit =
    GL20C.glUniform2iv(location, count, v.at(offset))

  override def glUniform3iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit =
    GL20C.glUniform3iv(location, count, v.at(offset))

  override def glUniform4iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit =
    GL20C.glUniform4iv(location, count, v.at(offset))

  // ─── Uniform matrix setters ───────────────────────────────────────────────

  override def glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GL20C.glUniformMatrix2fv(location, count, glBool(transpose), bufPtr(value).asInstanceOf[Ptr[CFloat]])

  override def glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit =
    GL20C.glUniformMatrix2fv(location, count, glBool(transpose), value.at(offset))

  override def glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GL20C.glUniformMatrix3fv(location, count, glBool(transpose), bufPtr(value).asInstanceOf[Ptr[CFloat]])

  override def glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit =
    GL20C.glUniformMatrix3fv(location, count, glBool(transpose), value.at(offset))

  override def glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GL20C.glUniformMatrix4fv(location, count, glBool(transpose), bufPtr(value).asInstanceOf[Ptr[CFloat]])

  override def glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit =
    GL20C.glUniformMatrix4fv(location, count, glBool(transpose), value.at(offset))
}
