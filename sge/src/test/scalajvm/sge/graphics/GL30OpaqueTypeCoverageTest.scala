/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */

// SGE — Unit test: GL30 opaque type coverage
//
// Tests that AndroidGL30Adapter correctly converts opaque types (PrimitiveMode,
// DataType, TextureTarget, PixelFormat, BufferTarget, ClearMask) to plain Int
// when delegating to GL30Ops.

package sge
package graphics

import java.nio.{ Buffer, ByteBuffer, FloatBuffer, IntBuffer, LongBuffer }
import munit.FunSuite
import sge.platform.android.GL30Ops

class GL30OpaqueTypeCoverageTest extends FunSuite {

  // Minimal stub that records last call and selected args for assertion.
  // Extends GL30Ops which extends GL20Ops, so ALL methods from both must be implemented.
  private class StubGL30Ops extends GL30Ops {
    var lastMethod: String   = ""
    var lastArgs:   Seq[Any] = Seq.empty

    private def record(method: String, args: Any*): Unit = {
      lastMethod = method
      lastArgs = args
    }

    // -------------------------------------------------------------------------
    // GL20Ops methods
    // -------------------------------------------------------------------------

    override def glActiveTexture(texture: Int):                                                    Unit = record("glActiveTexture", texture)
    override def glBindTexture(target:    Int, texture:   Int):                                    Unit = record("glBindTexture", target, texture)
    override def glBlendFunc(sfactor:     Int, dfactor:   Int):                                    Unit = record("glBlendFunc", sfactor, dfactor)
    override def glClear(mask:            Int):                                                    Unit = record("glClear", mask)
    override def glClearColor(red:        Float, green:   Float, blue:   Float, alpha:   Float):   Unit = record("glClearColor")
    override def glClearDepthf(depth:     Float):                                                  Unit = record("glClearDepthf")
    override def glClearStencil(s:        Int):                                                    Unit = record("glClearStencil")
    override def glColorMask(red:         Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit = record("glColorMask")
    override def glCompressedTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, imageSize: Int, data: Buffer): Unit = record("glCompressedTexImage2D")
    override def glCompressedTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, imageSize: Int, data: Buffer): Unit = record(
      "glCompressedTexSubImage2D"
    )
    override def glCopyTexImage2D(target:    Int, level:    Int, internalformat: Int, x:       Int, y: Int, width: Int, height: Int, border: Int): Unit = record("glCopyTexImage2D")
    override def glCopyTexSubImage2D(target: Int, level:    Int, xoffset:        Int, yoffset: Int, x: Int, y:     Int, width:  Int, height: Int): Unit = record("glCopyTexSubImage2D")
    override def glCullFace(mode:            Int):                                                                                                 Unit = record("glCullFace", mode)
    override def glDeleteTextures(n:         Int, textures: IntBuffer):                                                                            Unit = record("glDeleteTextures")
    override def glDeleteTexture(texture:    Int):                                                                                                 Unit = record("glDeleteTexture")
    override def glDepthFunc(func:           Int):                                                                                                 Unit = record("glDepthFunc", func)
    override def glDepthMask(flag:           Boolean):                                                                                             Unit = record("glDepthMask")
    override def glDepthRangef(zNear:        Float, zFar:   Float):                                                                                Unit = record("glDepthRangef")
    override def glDisable(cap:              Int):                                                                                                 Unit = record("glDisable", cap)
    override def glDrawArrays(mode:          Int, first:    Int, count:          Int):                                                             Unit = record("glDrawArrays", mode, first, count)
    override def glDrawElements(mode:    Int, count:    Int, `type`: Int, indices: Buffer):                                        Unit   = record("glDrawElements-buffer", mode, count, `type`)
    override def glEnable(cap:           Int):                                                                                     Unit   = record("glEnable", cap)
    override def glFinish():                                                                                                       Unit   = record("glFinish")
    override def glFlush():                                                                                                        Unit   = record("glFlush")
    override def glFrontFace(mode:       Int):                                                                                     Unit   = record("glFrontFace")
    override def glGenTextures(n:        Int, textures: IntBuffer):                                                                Unit   = record("glGenTextures")
    override def glGenTexture():                                                                                                   Int    = { record("glGenTexture"); 42 }
    override def glGetError():                                                                                                     Int    = { record("glGetError"); 0 }
    override def glGetIntegerv(pname:    Int, params:   IntBuffer):                                                                Unit   = record("glGetIntegerv")
    override def glGetString(name:       Int):                                                                                     String = { record("glGetString", name); "" }
    override def glHint(target:          Int, mode:     Int):                                                                      Unit   = record("glHint")
    override def glLineWidth(width:      Float):                                                                                   Unit   = record("glLineWidth")
    override def glPixelStorei(pname:    Int, param:    Int):                                                                      Unit   = record("glPixelStorei")
    override def glPolygonOffset(factor: Float, units:  Float):                                                                    Unit   = record("glPolygonOffset")
    override def glReadPixels(x:         Int, y:        Int, width:  Int, height:  Int, format: Int, `type`: Int, pixels: Buffer): Unit   = record("glReadPixels", format, `type`)
    override def glScissor(x:            Int, y:        Int, width:  Int, height:  Int):                                           Unit   = record("glScissor")
    override def glStencilFunc(func:     Int, ref:      Int, mask:   Int):                                                         Unit   = record("glStencilFunc", func, ref, mask)
    override def glStencilMask(mask:     Int):                                                                                     Unit   = record("glStencilMask")
    override def glStencilOp(fail:       Int, zfail:    Int, zpass:  Int):                                                         Unit   = record("glStencilOp", fail, zfail, zpass)
    override def glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, `type`: Int, pixels: Buffer): Unit =
      record("glTexImage2D-buffer", target, format, `type`)
    override def glTexParameterf(target: Int, pname: Int, param: Float):                                                                                  Unit = record("glTexParameterf")
    override def glTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: Buffer): Unit =
      record("glTexSubImage2D-buffer", target, format, `type`)
    override def glViewport(x:                      Int, y:             Int, width:              Int, height:       Int):    Unit = record("glViewport")
    override def glAttachShader(program:            Int, shader:        Int):                                                Unit = record("glAttachShader")
    override def glBindAttribLocation(program:      Int, index:         Int, name:               String):                    Unit = record("glBindAttribLocation")
    override def glBindBuffer(target:               Int, buffer:        Int):                                                Unit = record("glBindBuffer", target, buffer)
    override def glBindFramebuffer(target:          Int, framebuffer:   Int):                                                Unit = record("glBindFramebuffer")
    override def glBindRenderbuffer(target:         Int, renderbuffer:  Int):                                                Unit = record("glBindRenderbuffer")
    override def glBlendColor(red:                  Float, green:       Float, blue:             Float, alpha:      Float):  Unit = record("glBlendColor")
    override def glBlendEquation(mode:              Int):                                                                    Unit = record("glBlendEquation", mode)
    override def glBlendEquationSeparate(modeRGB:   Int, modeAlpha:     Int):                                                Unit = record("glBlendEquationSeparate", modeRGB, modeAlpha)
    override def glBlendFuncSeparate(srcRGB:        Int, dstRGB:        Int, srcAlpha:           Int, dstAlpha:     Int):    Unit = record("glBlendFuncSeparate", srcRGB, dstRGB, srcAlpha, dstAlpha)
    override def glBufferData(target:               Int, size:          Int, data:               Buffer, usage:     Int):    Unit = record("glBufferData", target, usage)
    override def glBufferSubData(target:            Int, offset:        Int, size:               Int, data:         Buffer): Unit = record("glBufferSubData", target)
    override def glCheckFramebufferStatus(target:   Int):                                                                    Int  = { record("glCheckFramebufferStatus"); 0 }
    override def glCompileShader(shader:            Int):                                                                    Unit = record("glCompileShader")
    override def glCreateProgram():                                                                                          Int  = { record("glCreateProgram"); 1 }
    override def glCreateShader(`type`:             Int):                                                                    Int  = { record("glCreateShader", `type`); 2 }
    override def glDeleteBuffer(buffer:             Int):                                                                    Unit = record("glDeleteBuffer")
    override def glDeleteBuffers(n:                 Int, buffers:       IntBuffer):                                          Unit = record("glDeleteBuffers")
    override def glDeleteFramebuffer(framebuffer:   Int):                                                                    Unit = record("glDeleteFramebuffer")
    override def glDeleteFramebuffers(n:            Int, framebuffers:  IntBuffer):                                          Unit = record("glDeleteFramebuffers")
    override def glDeleteProgram(program:           Int):                                                                    Unit = record("glDeleteProgram")
    override def glDeleteRenderbuffer(renderbuffer: Int):                                                                    Unit = record("glDeleteRenderbuffer")
    override def glDeleteRenderbuffers(n:           Int, renderbuffers: IntBuffer):                                          Unit = record("glDeleteRenderbuffers")
    override def glDeleteShader(shader:             Int):                                                                    Unit = record("glDeleteShader")
    override def glDetachShader(program:            Int, shader:        Int):                                                Unit = record("glDetachShader")
    override def glDisableVertexAttribArray(index:  Int):                                                                    Unit = record("glDisableVertexAttribArray")
    override def glDrawElements(mode:               Int, count:         Int, `type`:             Int, indices:      Int):    Unit = record("glDrawElements-int", mode, count, `type`, indices)
    override def glEnableVertexAttribArray(index:   Int):                                                                    Unit = record("glEnableVertexAttribArray")
    override def glFramebufferRenderbuffer(target:  Int, attachment:    Int, renderbuffertarget: Int, renderbuffer: Int):    Unit = record("glFramebufferRenderbuffer")
    override def glFramebufferTexture2D(target:                Int, attachment:     Int, textarget: Int, texture:         Int, level: Int): Unit    = record("glFramebufferTexture2D", textarget)
    override def glGenBuffer():                                                                                                             Int     = { record("glGenBuffer"); 3 }
    override def glGenBuffers(n:                               Int, buffers:        IntBuffer):                                             Unit    = record("glGenBuffers")
    override def glGenerateMipmap(target:                      Int):                                                                        Unit    = record("glGenerateMipmap", target)
    override def glGenFramebuffer():                                                                                                        Int     = { record("glGenFramebuffer"); 4 }
    override def glGenFramebuffers(n:                          Int, framebuffers:   IntBuffer):                                             Unit    = record("glGenFramebuffers")
    override def glGenRenderbuffer():                                                                                                       Int     = { record("glGenRenderbuffer"); 5 }
    override def glGenRenderbuffers(n:                         Int, renderbuffers:  IntBuffer):                                             Unit    = record("glGenRenderbuffers")
    override def glGetActiveAttrib(program:                    Int, index:          Int, size:      IntBuffer, `type`:    IntBuffer):       String  = { record("glGetActiveAttrib"); "attr" }
    override def glGetActiveUniform(program:                   Int, index:          Int, size:      IntBuffer, `type`:    IntBuffer):       String  = { record("glGetActiveUniform"); "uniform" }
    override def glGetAttachedShaders(program:                 Int, maxcount:       Int, count:     Buffer, shaders:      IntBuffer):       Unit    = record("glGetAttachedShaders")
    override def glGetAttribLocation(program:                  Int, name:           String):                                                Int     = { record("glGetAttribLocation"); 0 }
    override def glGetBooleanv(pname:                          Int, params:         Buffer):                                                Unit    = record("glGetBooleanv")
    override def glGetBufferParameteriv(target:                Int, pname:          Int, params:    IntBuffer):                             Unit    = record("glGetBufferParameteriv", target)
    override def glGetFloatv(pname:                            Int, params:         FloatBuffer):                                           Unit    = record("glGetFloatv")
    override def glGetFramebufferAttachmentParameteriv(target: Int, attachment:     Int, pname:     Int, params:          IntBuffer):       Unit    = record("glGetFramebufferAttachmentParameteriv")
    override def glGetProgramiv(program:                       Int, pname:          Int, params:    IntBuffer):                             Unit    = record("glGetProgramiv")
    override def glGetProgramInfoLog(program:                  Int):                                                                        String  = { record("glGetProgramInfoLog"); "" }
    override def glGetRenderbufferParameteriv(target:          Int, pname:          Int, params:    IntBuffer):                             Unit    = record("glGetRenderbufferParameteriv")
    override def glGetShaderiv(shader:                         Int, pname:          Int, params:    IntBuffer):                             Unit    = record("glGetShaderiv")
    override def glGetShaderInfoLog(shader:                    Int):                                                                        String  = { record("glGetShaderInfoLog"); "" }
    override def glGetShaderPrecisionFormat(shadertype:        Int, precisiontype:  Int, range:     IntBuffer, precision: IntBuffer):       Unit    = record("glGetShaderPrecisionFormat")
    override def glGetTexParameterfv(target:                   Int, pname:          Int, params:    FloatBuffer):                           Unit    = record("glGetTexParameterfv")
    override def glGetTexParameteriv(target:                   Int, pname:          Int, params:    IntBuffer):                             Unit    = record("glGetTexParameteriv")
    override def glGetUniformfv(program:                       Int, location:       Int, params:    FloatBuffer):                           Unit    = record("glGetUniformfv")
    override def glGetUniformiv(program:                       Int, location:       Int, params:    IntBuffer):                             Unit    = record("glGetUniformiv")
    override def glGetUniformLocation(program:                 Int, name:           String):                                                Int     = { record("glGetUniformLocation"); 0 }
    override def glGetVertexAttribfv(index:                    Int, pname:          Int, params:    FloatBuffer):                           Unit    = record("glGetVertexAttribfv")
    override def glGetVertexAttribiv(index:                    Int, pname:          Int, params:    IntBuffer):                             Unit    = record("glGetVertexAttribiv")
    override def glGetVertexAttribPointerv(index:              Int, pname:          Int, pointer:   Buffer):                                Unit    = record("glGetVertexAttribPointerv")
    override def glIsBuffer(buffer:                            Int):                                                                        Boolean = { record("glIsBuffer"); false }
    override def glIsEnabled(cap:                              Int):                                                                        Boolean = { record("glIsEnabled", cap); true }
    override def glIsFramebuffer(framebuffer:                  Int):                                                                        Boolean = { record("glIsFramebuffer"); false }
    override def glIsProgram(program:                          Int):                                                                        Boolean = { record("glIsProgram"); false }
    override def glIsRenderbuffer(renderbuffer:                Int):                                                                        Boolean = { record("glIsRenderbuffer"); false }
    override def glIsShader(shader:                            Int):                                                                        Boolean = { record("glIsShader"); false }
    override def glIsTexture(texture:                          Int):                                                                        Boolean = { record("glIsTexture"); false }
    override def glLinkProgram(program:                        Int):                                                                        Unit    = record("glLinkProgram")
    override def glReleaseShaderCompiler():                                                                                                 Unit    = record("glReleaseShaderCompiler")
    override def glRenderbufferStorage(target:                 Int, internalformat: Int, width:     Int, height:          Int):             Unit    = record("glRenderbufferStorage")
    override def glSampleCoverage(value:                       Float, invert:       Boolean):                                               Unit    = record("glSampleCoverage")
    override def glShaderBinary(n:            Int, shaders: IntBuffer, binaryformat: Int, binary:          Buffer, length:       Int):   Unit = record("glShaderBinary")
    override def glShaderSource(shader:       Int, string:  String):                                                                     Unit = record("glShaderSource")
    override def glStencilFuncSeparate(face:  Int, func:    Int, ref:                Int, mask:            Int):                         Unit = record("glStencilFuncSeparate", face, func, ref, mask)
    override def glStencilMaskSeparate(face:  Int, mask:    Int):                                                                        Unit = record("glStencilMaskSeparate")
    override def glStencilOpSeparate(face:    Int, fail:    Int, zfail:              Int, zpass:           Int):                         Unit = record("glStencilOpSeparate", face, fail, zfail, zpass)
    override def glTexParameterfv(target:     Int, pname:   Int, params:             FloatBuffer):                                       Unit = record("glTexParameterfv")
    override def glTexParameteri(target:      Int, pname:   Int, param:              Int):                                               Unit = record("glTexParameteri", target, pname, param)
    override def glTexParameteriv(target:     Int, pname:   Int, params:             IntBuffer):                                         Unit = record("glTexParameteriv")
    override def glUniform1f(location:        Int, x:       Float):                                                                      Unit = record("glUniform1f")
    override def glUniform1fv(location:       Int, count:   Int, v:                  FloatBuffer):                                       Unit = record("glUniform1fv")
    override def glUniform1fv(location:       Int, count:   Int, v:                  Array[Float], offset: Int):                         Unit = record("glUniform1fv-arr")
    override def glUniform1i(location:        Int, x:       Int):                                                                        Unit = record("glUniform1i")
    override def glUniform1iv(location:       Int, count:   Int, v:                  IntBuffer):                                         Unit = record("glUniform1iv")
    override def glUniform1iv(location:       Int, count:   Int, v:                  Array[Int], offset:   Int):                         Unit = record("glUniform1iv-arr")
    override def glUniform2f(location:        Int, x:       Float, y:                Float):                                             Unit = record("glUniform2f")
    override def glUniform2fv(location:       Int, count:   Int, v:                  FloatBuffer):                                       Unit = record("glUniform2fv")
    override def glUniform2fv(location:       Int, count:   Int, v:                  Array[Float], offset: Int):                         Unit = record("glUniform2fv-arr")
    override def glUniform2i(location:        Int, x:       Int, y:                  Int):                                               Unit = record("glUniform2i")
    override def glUniform2iv(location:       Int, count:   Int, v:                  IntBuffer):                                         Unit = record("glUniform2iv")
    override def glUniform2iv(location:       Int, count:   Int, v:                  Array[Int], offset:   Int):                         Unit = record("glUniform2iv-arr")
    override def glUniform3f(location:        Int, x:       Float, y:                Float, z:             Float):                       Unit = record("glUniform3f")
    override def glUniform3fv(location:       Int, count:   Int, v:                  FloatBuffer):                                       Unit = record("glUniform3fv")
    override def glUniform3fv(location:       Int, count:   Int, v:                  Array[Float], offset: Int):                         Unit = record("glUniform3fv-arr")
    override def glUniform3i(location:        Int, x:       Int, y:                  Int, z:               Int):                         Unit = record("glUniform3i")
    override def glUniform3iv(location:       Int, count:   Int, v:                  IntBuffer):                                         Unit = record("glUniform3iv")
    override def glUniform3iv(location:       Int, count:   Int, v:                  Array[Int], offset:   Int):                         Unit = record("glUniform3iv-arr")
    override def glUniform4f(location:        Int, x:       Float, y:                Float, z:             Float, w:             Float): Unit = record("glUniform4f")
    override def glUniform4fv(location:       Int, count:   Int, v:                  FloatBuffer):                                       Unit = record("glUniform4fv")
    override def glUniform4fv(location:       Int, count:   Int, v:                  Array[Float], offset: Int):                         Unit = record("glUniform4fv-arr")
    override def glUniform4i(location:        Int, x:       Int, y:                  Int, z:               Int, w:               Int):   Unit = record("glUniform4i")
    override def glUniform4iv(location:       Int, count:   Int, v:                  IntBuffer):                                         Unit = record("glUniform4iv")
    override def glUniform4iv(location:       Int, count:   Int, v:                  Array[Int], offset:   Int):                         Unit = record("glUniform4iv-arr")
    override def glUniformMatrix2fv(location: Int, count:   Int, transpose:          Boolean, value:       FloatBuffer):                 Unit = record("glUniformMatrix2fv")
    override def glUniformMatrix2fv(location: Int, count:   Int, transpose:          Boolean, value:       Array[Float], offset: Int):   Unit = record("glUniformMatrix2fv-arr")
    override def glUniformMatrix3fv(location: Int, count:   Int, transpose:          Boolean, value:       FloatBuffer):                 Unit = record("glUniformMatrix3fv")
    override def glUniformMatrix3fv(location: Int, count:   Int, transpose:          Boolean, value:       Array[Float], offset: Int):   Unit = record("glUniformMatrix3fv-arr")
    override def glUniformMatrix4fv(location: Int, count:   Int, transpose:          Boolean, value:       FloatBuffer):                 Unit = record("glUniformMatrix4fv")
    override def glUniformMatrix4fv(location: Int, count:   Int, transpose:          Boolean, value:       Array[Float], offset: Int):   Unit = record("glUniformMatrix4fv-arr")
    override def glUseProgram(program:        Int):                                                                                      Unit = record("glUseProgram")
    override def glValidateProgram(program:   Int):                                                                                      Unit = record("glValidateProgram")
    override def glVertexAttrib1f(indx:       Int, x:       Float):                                                                      Unit = record("glVertexAttrib1f")
    override def glVertexAttrib1fv(indx:      Int, values:  FloatBuffer):                                                                Unit = record("glVertexAttrib1fv")
    override def glVertexAttrib2f(indx:       Int, x:       Float, y:                Float):                                             Unit = record("glVertexAttrib2f")
    override def glVertexAttrib2fv(indx:      Int, values:  FloatBuffer):                                                                Unit = record("glVertexAttrib2fv")
    override def glVertexAttrib3f(indx:       Int, x:       Float, y:                Float, z:             Float):                       Unit = record("glVertexAttrib3f")
    override def glVertexAttrib3fv(indx:      Int, values:  FloatBuffer):                                                                Unit = record("glVertexAttrib3fv")
    override def glVertexAttrib4f(indx:       Int, x:       Float, y:                Float, z:             Float, w:             Float): Unit = record("glVertexAttrib4f")
    override def glVertexAttrib4fv(indx:      Int, values:  FloatBuffer):                                                                Unit = record("glVertexAttrib4fv")
    override def glVertexAttribPointer(indx: Int, size: Int, `type`: Int, normalized: Boolean, stride: Int, ptr: Buffer): Unit = record("glVertexAttribPointer-buffer", `type`)
    override def glVertexAttribPointer(indx: Int, size: Int, `type`: Int, normalized: Boolean, stride: Int, ptr: Int):    Unit = record("glVertexAttribPointer-int", `type`)

    // -------------------------------------------------------------------------
    // GL30Ops methods
    // -------------------------------------------------------------------------

    override def glReadBuffer(mode: Int): Unit = record("glReadBuffer", mode)

    override def glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, `type`: Int, indices: Buffer): Unit =
      record("glDrawRangeElements-buffer", mode, `type`)

    override def glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, `type`: Int, offset: Int): Unit =
      record("glDrawRangeElements-offset", mode, `type`)

    override def glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, `type`: Int, offset: Int): Unit =
      record("glTexImage2D-offset", target, format, `type`)

    override def glTexImage3D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: Int, `type`: Int, pixels: Buffer): Unit =
      record("glTexImage3D-buffer", target, format, `type`)

    override def glTexImage3D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: Int, `type`: Int, offset: Int): Unit =
      record("glTexImage3D-offset", target, format, `type`)

    override def glTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, `type`: Int, offset: Int): Unit =
      record("glTexSubImage2D-offset", target, format, `type`)

    override def glTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int, format: Int, `type`: Int, pixels: Buffer): Unit =
      record("glTexSubImage3D-buffer", target, format, `type`)

    override def glTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int, format: Int, `type`: Int, offset: Int): Unit =
      record("glTexSubImage3D-offset", target, format, `type`)

    override def glCopyTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit =
      record("glCopyTexSubImage3D", target)

    override def glGenQueries(n: Int, ids: Array[Int], offset: Int): Unit = record("glGenQueries-arr")
    override def glGenQueries(n: Int, ids: IntBuffer):               Unit = record("glGenQueries")

    override def glDeleteQueries(n: Int, ids: Array[Int], offset: Int): Unit = record("glDeleteQueries-arr")
    override def glDeleteQueries(n: Int, ids: IntBuffer):               Unit = record("glDeleteQueries")

    override def glIsQuery(id: Int): Boolean = { record("glIsQuery"); false }

    override def glBeginQuery(target: Int, id: Int): Unit = record("glBeginQuery")
    override def glEndQuery(target:   Int):          Unit = record("glEndQuery")

    override def glGetQueryiv(target:    Int, pname: Int, params: IntBuffer): Unit = record("glGetQueryiv")
    override def glGetQueryObjectuiv(id: Int, pname: Int, params: IntBuffer): Unit = record("glGetQueryObjectuiv")

    override def glUnmapBuffer(target: Int): Boolean = { record("glUnmapBuffer", target); false }

    override def glGetBufferPointerv(target: Int, pname: Int): Buffer = { record("glGetBufferPointerv", target); ByteBuffer.allocate(0) }

    override def glDrawBuffers(n: Int, bufs: IntBuffer): Unit = record("glDrawBuffers")

    override def glUniformMatrix2x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = record("glUniformMatrix2x3fv")
    override def glUniformMatrix3x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = record("glUniformMatrix3x2fv")
    override def glUniformMatrix2x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = record("glUniformMatrix2x4fv")
    override def glUniformMatrix4x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = record("glUniformMatrix4x2fv")
    override def glUniformMatrix3x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = record("glUniformMatrix3x4fv")
    override def glUniformMatrix4x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit = record("glUniformMatrix4x3fv")

    override def glBlitFramebuffer(srcX0: Int, srcY0: Int, srcX1: Int, srcY1: Int, dstX0: Int, dstY0: Int, dstX1: Int, dstY1: Int, mask: Int, filter: Int): Unit =
      record("glBlitFramebuffer", mask)

    override def glRenderbufferStorageMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int): Unit =
      record("glRenderbufferStorageMultisample")

    override def glFramebufferTextureLayer(target: Int, attachment: Int, texture: Int, level: Int, layer: Int): Unit =
      record("glFramebufferTextureLayer")

    override def glMapBufferRange(target: Int, offset: Int, length: Int, access: Int): Buffer = { record("glMapBufferRange", target); ByteBuffer.allocate(0) }

    override def glFlushMappedBufferRange(target: Int, offset: Int, length: Int): Unit = record("glFlushMappedBufferRange", target)

    override def glBindVertexArray(array: Int): Unit = record("glBindVertexArray")

    override def glDeleteVertexArrays(n: Int, arrays: Array[Int], offset: Int): Unit = record("glDeleteVertexArrays-arr")
    override def glDeleteVertexArrays(n: Int, arrays: IntBuffer):               Unit = record("glDeleteVertexArrays")

    override def glGenVertexArrays(n: Int, arrays: Array[Int], offset: Int): Unit = record("glGenVertexArrays-arr")
    override def glGenVertexArrays(n: Int, arrays: IntBuffer):               Unit = record("glGenVertexArrays")

    override def glIsVertexArray(array: Int): Boolean = { record("glIsVertexArray"); false }

    override def glBeginTransformFeedback(primitiveMode: Int): Unit = record("glBeginTransformFeedback", primitiveMode)

    override def glEndTransformFeedback(): Unit = record("glEndTransformFeedback")

    override def glBindBufferRange(target: Int, index: Int, buffer: Int, offset: Int, size: Int): Unit =
      record("glBindBufferRange", target)

    override def glBindBufferBase(target: Int, index: Int, buffer: Int): Unit = record("glBindBufferBase", target)

    override def glTransformFeedbackVaryings(program: Int, varyings: Array[String], bufferMode: Int): Unit =
      record("glTransformFeedbackVaryings")

    override def glVertexAttribIPointer(index: Int, size: Int, `type`: Int, stride: Int, offset: Int): Unit =
      record("glVertexAttribIPointer", `type`)

    override def glGetVertexAttribIiv(index:  Int, pname: Int, params: IntBuffer): Unit = record("glGetVertexAttribIiv")
    override def glGetVertexAttribIuiv(index: Int, pname: Int, params: IntBuffer): Unit = record("glGetVertexAttribIuiv")

    override def glVertexAttribI4i(index:  Int, x: Int, y: Int, z: Int, w: Int): Unit = record("glVertexAttribI4i")
    override def glVertexAttribI4ui(index: Int, x: Int, y: Int, z: Int, w: Int): Unit = record("glVertexAttribI4ui")

    override def glGetUniformuiv(program: Int, location: Int, params: IntBuffer): Unit = record("glGetUniformuiv")

    override def glGetFragDataLocation(program: Int, name: String): Int = { record("glGetFragDataLocation"); 0 }

    override def glUniform1uiv(location: Int, count: Int, value: IntBuffer): Unit = record("glUniform1uiv")
    override def glUniform3uiv(location: Int, count: Int, value: IntBuffer): Unit = record("glUniform3uiv")
    override def glUniform4uiv(location: Int, count: Int, value: IntBuffer): Unit = record("glUniform4uiv")

    override def glClearBufferiv(buffer:  Int, drawbuffer: Int, value: IntBuffer):           Unit = record("glClearBufferiv")
    override def glClearBufferuiv(buffer: Int, drawbuffer: Int, value: IntBuffer):           Unit = record("glClearBufferuiv")
    override def glClearBufferfv(buffer:  Int, drawbuffer: Int, value: FloatBuffer):         Unit = record("glClearBufferfv")
    override def glClearBufferfi(buffer:  Int, drawbuffer: Int, depth: Float, stencil: Int): Unit = record("glClearBufferfi")

    override def glGetStringi(name: Int, index: Int): String = { record("glGetStringi"); "" }

    override def glCopyBufferSubData(readTarget: Int, writeTarget: Int, readOffset: Int, writeOffset: Int, size: Int): Unit =
      record("glCopyBufferSubData", readTarget, writeTarget)

    override def glGetUniformIndices(program: Int, uniformNames: Array[String], uniformIndices: IntBuffer): Unit =
      record("glGetUniformIndices")

    override def glGetActiveUniformsiv(program: Int, uniformCount: Int, uniformIndices: IntBuffer, pname: Int, params: IntBuffer): Unit =
      record("glGetActiveUniformsiv")

    override def glGetUniformBlockIndex(program: Int, uniformBlockName: String): Int = { record("glGetUniformBlockIndex"); 0 }

    override def glGetActiveUniformBlockiv(program: Int, uniformBlockIndex: Int, pname: Int, params: IntBuffer): Unit =
      record("glGetActiveUniformBlockiv")

    override def glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int, length: Buffer, uniformBlockName: Buffer): Unit =
      record("glGetActiveUniformBlockName-buf")

    override def glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int): String = { record("glGetActiveUniformBlockName"); "" }

    override def glUniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int): Unit =
      record("glUniformBlockBinding")

    override def glDrawArraysInstanced(mode: Int, first: Int, count: Int, instanceCount: Int): Unit =
      record("glDrawArraysInstanced", mode)

    override def glDrawElementsInstanced(mode: Int, count: Int, `type`: Int, indicesOffset: Int, instanceCount: Int): Unit =
      record("glDrawElementsInstanced", mode, `type`)

    override def glGetInteger64v(pname: Int, params: LongBuffer): Unit = record("glGetInteger64v")

    override def glGetBufferParameteri64v(target: Int, pname: Int, params: LongBuffer): Unit =
      record("glGetBufferParameteri64v", target)

    override def glGenSamplers(count: Int, samplers: Array[Int], offset: Int): Unit = record("glGenSamplers-arr")
    override def glGenSamplers(count: Int, samplers: IntBuffer):               Unit = record("glGenSamplers")

    override def glDeleteSamplers(count: Int, samplers: Array[Int], offset: Int): Unit = record("glDeleteSamplers-arr")
    override def glDeleteSamplers(count: Int, samplers: IntBuffer):               Unit = record("glDeleteSamplers")

    override def glIsSampler(sampler: Int): Boolean = { record("glIsSampler"); false }

    override def glBindSampler(unit: Int, sampler: Int): Unit = record("glBindSampler")

    override def glSamplerParameteri(sampler:  Int, pname: Int, param: Int):         Unit = record("glSamplerParameteri")
    override def glSamplerParameteriv(sampler: Int, pname: Int, param: IntBuffer):   Unit = record("glSamplerParameteriv")
    override def glSamplerParameterf(sampler:  Int, pname: Int, param: Float):       Unit = record("glSamplerParameterf")
    override def glSamplerParameterfv(sampler: Int, pname: Int, param: FloatBuffer): Unit = record("glSamplerParameterfv")

    override def glGetSamplerParameteriv(sampler: Int, pname: Int, params: IntBuffer):   Unit = record("glGetSamplerParameteriv")
    override def glGetSamplerParameterfv(sampler: Int, pname: Int, params: FloatBuffer): Unit = record("glGetSamplerParameterfv")

    override def glVertexAttribDivisor(index: Int, divisor: Int): Unit = record("glVertexAttribDivisor")

    override def glBindTransformFeedback(target: Int, id: Int): Unit = record("glBindTransformFeedback")

    override def glDeleteTransformFeedbacks(n: Int, ids: Array[Int], offset: Int): Unit = record("glDeleteTransformFeedbacks-arr")
    override def glDeleteTransformFeedbacks(n: Int, ids: IntBuffer):               Unit = record("glDeleteTransformFeedbacks")

    override def glGenTransformFeedbacks(n: Int, ids: Array[Int], offset: Int): Unit = record("glGenTransformFeedbacks-arr")
    override def glGenTransformFeedbacks(n: Int, ids: IntBuffer):               Unit = record("glGenTransformFeedbacks")

    override def glIsTransformFeedback(id: Int): Boolean = { record("glIsTransformFeedback"); false }

    override def glPauseTransformFeedback():  Unit = record("glPauseTransformFeedback")
    override def glResumeTransformFeedback(): Unit = record("glResumeTransformFeedback")

    override def glProgramParameteri(program: Int, pname: Int, value: Int): Unit = record("glProgramParameteri")

    override def glInvalidateFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer): Unit =
      record("glInvalidateFramebuffer")

    override def glInvalidateSubFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer, x: Int, y: Int, width: Int, height: Int): Unit =
      record("glInvalidateSubFramebuffer")
  }

  // Opaque type test values
  private val triangles:     PrimitiveMode = PrimitiveMode(0x0004) // GL_TRIANGLES → raw 4
  private val unsignedShort: DataType      = DataType(0x1403) // GL_UNSIGNED_SHORT → raw 5123
  private val texture2D:     TextureTarget = TextureTarget(0x0de1) // GL_TEXTURE_2D → raw 3553
  private val rgba:          PixelFormat   = PixelFormat(0x1908) // GL_RGBA → raw 6408
  private val arrayBuffer:   BufferTarget  = BufferTarget(0x8892) // GL_ARRAY_BUFFER → raw 34962
  private val colorBit:      ClearMask     = ClearMask(0x4000) // GL_COLOR_BUFFER_BIT → raw 16384

  private val dummyBuffer: Buffer = ByteBuffer.allocate(16)

  // ---------------------------------------------------------------------------
  // 1. glDrawRangeElements (Buffer overload) — PrimitiveMode + DataType
  // ---------------------------------------------------------------------------

  test("glDrawRangeElements(Buffer): PrimitiveMode and DataType convert to plain Int") {
    val stub = StubGL30Ops()
    val gl   = AndroidGL30Adapter(stub)
    gl.glDrawRangeElements(triangles, 0, 10, 6, unsignedShort, dummyBuffer)
    assertEquals(stub.lastMethod, "glDrawRangeElements-buffer")
    assertEquals(stub.lastArgs, Seq(4, 5123))
  }

  // ---------------------------------------------------------------------------
  // 2. glDrawRangeElements (offset overload) — PrimitiveMode + DataType
  // ---------------------------------------------------------------------------

  test("glDrawRangeElements(offset): PrimitiveMode and DataType convert to plain Int") {
    val stub = StubGL30Ops()
    val gl   = AndroidGL30Adapter(stub)
    gl.glDrawRangeElements(triangles, 0, 10, 6, unsignedShort, 0)
    assertEquals(stub.lastMethod, "glDrawRangeElements-offset")
    assertEquals(stub.lastArgs, Seq(4, 5123))
  }

  // ---------------------------------------------------------------------------
  // 3. glTexImage2D (GL30 offset overload) — TextureTarget + PixelFormat + DataType
  // ---------------------------------------------------------------------------

  test("glTexImage2D(offset): TextureTarget, PixelFormat, DataType convert to plain Int") {
    val stub = StubGL30Ops()
    val gl   = AndroidGL30Adapter(stub)
    gl.glTexImage2D(texture2D, 0, 0x1908, 256, 256, 0, rgba, unsignedShort, 0)
    assertEquals(stub.lastMethod, "glTexImage2D-offset")
    assertEquals(stub.lastArgs, Seq(3553, 6408, 5123))
  }

  // ---------------------------------------------------------------------------
  // 4. glTexImage3D (Buffer) — TextureTarget + PixelFormat + DataType
  // ---------------------------------------------------------------------------

  test("glTexImage3D(Buffer): TextureTarget, PixelFormat, DataType convert to plain Int") {
    val stub = StubGL30Ops()
    val gl   = AndroidGL30Adapter(stub)
    gl.glTexImage3D(texture2D, 0, 0x1908, 64, 64, 4, 0, rgba, unsignedShort, dummyBuffer)
    assertEquals(stub.lastMethod, "glTexImage3D-buffer")
    assertEquals(stub.lastArgs, Seq(3553, 6408, 5123))
  }

  // ---------------------------------------------------------------------------
  // 5. glTexImage3D (offset) — TextureTarget + PixelFormat + DataType
  // ---------------------------------------------------------------------------

  test("glTexImage3D(offset): TextureTarget, PixelFormat, DataType convert to plain Int") {
    val stub = StubGL30Ops()
    val gl   = AndroidGL30Adapter(stub)
    gl.glTexImage3D(texture2D, 0, 0x1908, 64, 64, 4, 0, rgba, unsignedShort, 0)
    assertEquals(stub.lastMethod, "glTexImage3D-offset")
    assertEquals(stub.lastArgs, Seq(3553, 6408, 5123))
  }

  // ---------------------------------------------------------------------------
  // 6. glTexSubImage2D (GL30 offset overload) — TextureTarget + PixelFormat + DataType
  // ---------------------------------------------------------------------------

  test("glTexSubImage2D(offset): TextureTarget, PixelFormat, DataType convert to plain Int") {
    val stub = StubGL30Ops()
    val gl   = AndroidGL30Adapter(stub)
    gl.glTexSubImage2D(texture2D, 0, 0, 0, 128, 128, rgba, unsignedShort, 0)
    assertEquals(stub.lastMethod, "glTexSubImage2D-offset")
    assertEquals(stub.lastArgs, Seq(3553, 6408, 5123))
  }

  // ---------------------------------------------------------------------------
  // 7. glTexSubImage3D (Buffer) — TextureTarget + PixelFormat + DataType
  // ---------------------------------------------------------------------------

  test("glTexSubImage3D(Buffer): TextureTarget, PixelFormat, DataType convert to plain Int") {
    val stub = StubGL30Ops()
    val gl   = AndroidGL30Adapter(stub)
    gl.glTexSubImage3D(texture2D, 0, 0, 0, 0, 64, 64, 4, rgba, unsignedShort, dummyBuffer)
    assertEquals(stub.lastMethod, "glTexSubImage3D-buffer")
    assertEquals(stub.lastArgs, Seq(3553, 6408, 5123))
  }

  // ---------------------------------------------------------------------------
  // 8. glTexSubImage3D (offset) — TextureTarget + PixelFormat + DataType
  // ---------------------------------------------------------------------------

  test("glTexSubImage3D(offset): TextureTarget, PixelFormat, DataType convert to plain Int") {
    val stub = StubGL30Ops()
    val gl   = AndroidGL30Adapter(stub)
    gl.glTexSubImage3D(texture2D, 0, 0, 0, 0, 64, 64, 4, rgba, unsignedShort, 0)
    assertEquals(stub.lastMethod, "glTexSubImage3D-offset")
    assertEquals(stub.lastArgs, Seq(3553, 6408, 5123))
  }

  // ---------------------------------------------------------------------------
  // 9. glCopyTexSubImage3D — TextureTarget
  // ---------------------------------------------------------------------------

  test("glCopyTexSubImage3D: TextureTarget converts to plain Int") {
    val stub = StubGL30Ops()
    val gl   = AndroidGL30Adapter(stub)
    gl.glCopyTexSubImage3D(texture2D, 0, 0, 0, 0, 0, 0, 64, 64)
    assertEquals(stub.lastMethod, "glCopyTexSubImage3D")
    assertEquals(stub.lastArgs, Seq(3553))
  }

  // ---------------------------------------------------------------------------
  // 10. glUnmapBuffer — BufferTarget
  // ---------------------------------------------------------------------------

  test("glUnmapBuffer: BufferTarget converts to plain Int") {
    val stub = StubGL30Ops()
    val gl   = AndroidGL30Adapter(stub)
    gl.glUnmapBuffer(arrayBuffer)
    assertEquals(stub.lastMethod, "glUnmapBuffer")
    assertEquals(stub.lastArgs, Seq(34962))
  }

  // ---------------------------------------------------------------------------
  // 11. glGetBufferPointerv — BufferTarget
  // ---------------------------------------------------------------------------

  test("glGetBufferPointerv: BufferTarget converts to plain Int") {
    val stub = StubGL30Ops()
    val gl   = AndroidGL30Adapter(stub)
    gl.glGetBufferPointerv(arrayBuffer, 0x88bd)
    assertEquals(stub.lastMethod, "glGetBufferPointerv")
    assertEquals(stub.lastArgs, Seq(34962))
  }

  // ---------------------------------------------------------------------------
  // 12. glBlitFramebuffer — ClearMask
  // ---------------------------------------------------------------------------

  test("glBlitFramebuffer: ClearMask converts to plain Int") {
    val stub = StubGL30Ops()
    val gl   = AndroidGL30Adapter(stub)
    gl.glBlitFramebuffer(0, 0, 800, 600, 0, 0, 800, 600, colorBit, 0x2600)
    assertEquals(stub.lastMethod, "glBlitFramebuffer")
    assertEquals(stub.lastArgs, Seq(16384))
  }

  // ---------------------------------------------------------------------------
  // 13. glMapBufferRange — BufferTarget
  // ---------------------------------------------------------------------------

  test("glMapBufferRange: BufferTarget converts to plain Int") {
    val stub = StubGL30Ops()
    val gl   = AndroidGL30Adapter(stub)
    gl.glMapBufferRange(arrayBuffer, 0, 1024, 1)
    assertEquals(stub.lastMethod, "glMapBufferRange")
    assertEquals(stub.lastArgs, Seq(34962))
  }

  // ---------------------------------------------------------------------------
  // 14. glFlushMappedBufferRange — BufferTarget
  // ---------------------------------------------------------------------------

  test("glFlushMappedBufferRange: BufferTarget converts to plain Int") {
    val stub = StubGL30Ops()
    val gl   = AndroidGL30Adapter(stub)
    gl.glFlushMappedBufferRange(arrayBuffer, 0, 512)
    assertEquals(stub.lastMethod, "glFlushMappedBufferRange")
    assertEquals(stub.lastArgs, Seq(34962))
  }

  // ---------------------------------------------------------------------------
  // 15. glBeginTransformFeedback — PrimitiveMode
  // ---------------------------------------------------------------------------

  test("glBeginTransformFeedback: PrimitiveMode converts to plain Int") {
    val stub = StubGL30Ops()
    val gl   = AndroidGL30Adapter(stub)
    gl.glBeginTransformFeedback(triangles)
    assertEquals(stub.lastMethod, "glBeginTransformFeedback")
    assertEquals(stub.lastArgs, Seq(4))
  }

  // ---------------------------------------------------------------------------
  // 16. glBindBufferRange — BufferTarget
  // ---------------------------------------------------------------------------

  test("glBindBufferRange: BufferTarget converts to plain Int") {
    val stub = StubGL30Ops()
    val gl   = AndroidGL30Adapter(stub)
    gl.glBindBufferRange(arrayBuffer, 0, 1, 0, 256)
    assertEquals(stub.lastMethod, "glBindBufferRange")
    assertEquals(stub.lastArgs, Seq(34962))
  }

  // ---------------------------------------------------------------------------
  // 17. glBindBufferBase — BufferTarget
  // ---------------------------------------------------------------------------

  test("glBindBufferBase: BufferTarget converts to plain Int") {
    val stub = StubGL30Ops()
    val gl   = AndroidGL30Adapter(stub)
    gl.glBindBufferBase(arrayBuffer, 0, 1)
    assertEquals(stub.lastMethod, "glBindBufferBase")
    assertEquals(stub.lastArgs, Seq(34962))
  }

  // ---------------------------------------------------------------------------
  // 18. glVertexAttribIPointer — DataType
  // ---------------------------------------------------------------------------

  test("glVertexAttribIPointer: DataType converts to plain Int") {
    val stub = StubGL30Ops()
    val gl   = AndroidGL30Adapter(stub)
    gl.glVertexAttribIPointer(0, 4, unsignedShort, 0, 0)
    assertEquals(stub.lastMethod, "glVertexAttribIPointer")
    assertEquals(stub.lastArgs, Seq(5123))
  }

  // ---------------------------------------------------------------------------
  // 19. glDrawArraysInstanced — PrimitiveMode
  // ---------------------------------------------------------------------------

  test("glDrawArraysInstanced: PrimitiveMode converts to plain Int") {
    val stub = StubGL30Ops()
    val gl   = AndroidGL30Adapter(stub)
    gl.glDrawArraysInstanced(triangles, 0, 36, 10)
    assertEquals(stub.lastMethod, "glDrawArraysInstanced")
    assertEquals(stub.lastArgs, Seq(4))
  }

  // ---------------------------------------------------------------------------
  // 20. glDrawElementsInstanced — PrimitiveMode + DataType
  // ---------------------------------------------------------------------------

  test("glDrawElementsInstanced: PrimitiveMode and DataType convert to plain Int") {
    val stub = StubGL30Ops()
    val gl   = AndroidGL30Adapter(stub)
    gl.glDrawElementsInstanced(triangles, 36, unsignedShort, 0, 10)
    assertEquals(stub.lastMethod, "glDrawElementsInstanced")
    assertEquals(stub.lastArgs, Seq(4, 5123))
  }

  // ---------------------------------------------------------------------------
  // 21. glGetBufferParameteri64v — BufferTarget
  // ---------------------------------------------------------------------------

  test("glGetBufferParameteri64v: BufferTarget converts to plain Int") {
    val stub   = StubGL30Ops()
    val gl     = AndroidGL30Adapter(stub)
    val params = LongBuffer.allocate(1)
    gl.glGetBufferParameteri64v(arrayBuffer, 0x8764, params)
    assertEquals(stub.lastMethod, "glGetBufferParameteri64v")
    assertEquals(stub.lastArgs, Seq(34962))
  }

  // ---------------------------------------------------------------------------
  // 22. glCopyBufferSubData — BufferTarget x 2
  // ---------------------------------------------------------------------------

  test("glCopyBufferSubData: both BufferTargets convert to plain Int") {
    val stub       = StubGL30Ops()
    val gl         = AndroidGL30Adapter(stub)
    val elemBuffer = BufferTarget(0x8893) // GL_ELEMENT_ARRAY_BUFFER → raw 34963
    gl.glCopyBufferSubData(arrayBuffer, elemBuffer, 0, 0, 1024)
    assertEquals(stub.lastMethod, "glCopyBufferSubData")
    assertEquals(stub.lastArgs, Seq(34962, 34963))
  }
}
