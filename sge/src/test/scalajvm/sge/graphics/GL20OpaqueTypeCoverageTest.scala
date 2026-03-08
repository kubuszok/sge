/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */

// SGE — Unit test: GL20 opaque type coverage
//
// Tests the opaque types NOT covered by AndroidGL20AdapterTest:
//   StencilOp, CompareFunc, BlendEquation, PixelFormat, DataType, CullFace, BufferUsage
// Also tests additional methods for already-tested types:
//   TextureTarget (glGenerateMipmap, glFramebufferTexture2D, glTexParameteri)
//   EnableCap (glDisable, glIsEnabled)
//   BufferTarget (glBufferSubData, glGetBufferParameteriv)
//   BlendFactor (glBlendFuncSeparate)

package sge
package graphics

import java.nio.{ Buffer, ByteBuffer, FloatBuffer, IntBuffer }
import munit.FunSuite
import sge.platform.android.GL20Ops

class GL20OpaqueTypeCoverageTest extends FunSuite {

  // Minimal stub that records last call and selected args for assertion
  private class StubGL20Ops extends GL20Ops {
    var lastMethod: String   = ""
    var lastArgs:   Seq[Any] = Seq.empty

    private def record(method: String, args: Any*): Unit = {
      lastMethod = method
      lastArgs = args
    }

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
    override def glGetString(name:       Int):                                                                                     String = { record("glGetString", name); "test" }
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
      record("glTexImage2D", target, format, `type`)
    override def glTexParameterf(target: Int, pname: Int, param: Float):                                                                                  Unit = record("glTexParameterf")
    override def glTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: Buffer): Unit =
      record("glTexSubImage2D", target, format, `type`)
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
  }

  // ---------------------------------------------------------------------------
  // StencilOp
  // ---------------------------------------------------------------------------

  test("StencilOp: glStencilOp passes all three ops as plain Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glStencilOp(StencilOp.Keep, StencilOp.Replace, StencilOp.IncrWrap)
    assertEquals(stub.lastMethod, "glStencilOp")
    assertEquals(stub.lastArgs, Seq(0x1e00, 0x1e01, 0x8507))
  }

  test("StencilOp: glStencilOpSeparate passes face and ops as plain Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glStencilOpSeparate(CullFace.Front, StencilOp.Zero, StencilOp.Decr, StencilOp.Invert)
    assertEquals(stub.lastMethod, "glStencilOpSeparate")
    assertEquals(stub.lastArgs, Seq(0x0404, 0, 0x1e03, 0x150a))
  }

  test("StencilOp: DecrWrap constant value is correct") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glStencilOp(StencilOp.DecrWrap, StencilOp.Incr, StencilOp.Keep)
    assertEquals(stub.lastArgs, Seq(0x8508, 0x1e02, 0x1e00))
  }

  // ---------------------------------------------------------------------------
  // CompareFunc
  // ---------------------------------------------------------------------------

  test("CompareFunc: glDepthFunc passes func as plain Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glDepthFunc(CompareFunc.Lequal)
    assertEquals(stub.lastMethod, "glDepthFunc")
    assertEquals(stub.lastArgs, Seq(0x0203))
  }

  test("CompareFunc: glStencilFunc passes func, ref, mask") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glStencilFunc(CompareFunc.Always, 1, 0xff)
    assertEquals(stub.lastMethod, "glStencilFunc")
    assertEquals(stub.lastArgs, Seq(0x0207, 1, 0xff))
  }

  test("CompareFunc: glStencilFuncSeparate passes face and func as plain Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glStencilFuncSeparate(CullFace.Back, CompareFunc.Greater, 0, 0xffffffff)
    assertEquals(stub.lastMethod, "glStencilFuncSeparate")
    assertEquals(stub.lastArgs, Seq(0x0405, 0x0204, 0, 0xffffffff))
  }

  test("CompareFunc: all constant values are distinct and correct") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glDepthFunc(CompareFunc.Never)
    assertEquals(stub.lastArgs, Seq(0x0200))
    gl.glDepthFunc(CompareFunc.Less)
    assertEquals(stub.lastArgs, Seq(0x0201))
    gl.glDepthFunc(CompareFunc.Equal)
    assertEquals(stub.lastArgs, Seq(0x0202))
    gl.glDepthFunc(CompareFunc.Notequal)
    assertEquals(stub.lastArgs, Seq(0x0205))
    gl.glDepthFunc(CompareFunc.Gequal)
    assertEquals(stub.lastArgs, Seq(0x0206))
  }

  // ---------------------------------------------------------------------------
  // BlendEquation
  // ---------------------------------------------------------------------------

  test("BlendEquation: glBlendEquation passes mode as plain Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glBlendEquation(BlendEquation.FuncAdd)
    assertEquals(stub.lastMethod, "glBlendEquation")
    assertEquals(stub.lastArgs, Seq(0x8006))
  }

  test("BlendEquation: glBlendEquationSeparate passes both modes as plain Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glBlendEquationSeparate(BlendEquation.FuncSubtract, BlendEquation.FuncReverseSubtract)
    assertEquals(stub.lastMethod, "glBlendEquationSeparate")
    assertEquals(stub.lastArgs, Seq(0x800a, 0x800b))
  }

  test("BlendEquation: Min and Max constants (GL30+) have correct values") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glBlendEquation(BlendEquation.Min)
    assertEquals(stub.lastArgs, Seq(0x8007))
    gl.glBlendEquation(BlendEquation.Max)
    assertEquals(stub.lastArgs, Seq(0x8008))
  }

  // ---------------------------------------------------------------------------
  // CullFace
  // ---------------------------------------------------------------------------

  test("CullFace: glCullFace passes mode as plain Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glCullFace(CullFace.Back)
    assertEquals(stub.lastMethod, "glCullFace")
    assertEquals(stub.lastArgs, Seq(0x0405))
  }

  test("CullFace: Front and FrontAndBack constants") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glCullFace(CullFace.Front)
    assertEquals(stub.lastArgs, Seq(0x0404))
    gl.glCullFace(CullFace.FrontAndBack)
    assertEquals(stub.lastArgs, Seq(0x0408))
  }

  test("CullFace: used as face param in glStencilMaskSeparate via glStencilFuncSeparate") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glStencilFuncSeparate(CullFace.FrontAndBack, CompareFunc.Never, 0, 0)
    assertEquals(stub.lastArgs(0), 0x0408)
  }

  // ---------------------------------------------------------------------------
  // DataType
  // ---------------------------------------------------------------------------

  test("DataType: glDrawElements (Buffer overload) passes type as plain Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    val buf  = ByteBuffer.allocate(4)
    gl.glDrawElements(PrimitiveMode.Triangles, 3, DataType.UnsignedShort, buf)
    assertEquals(stub.lastMethod, "glDrawElements-buffer")
    assertEquals(stub.lastArgs, Seq(0x0004, 3, 0x1403))
  }

  test("DataType: glDrawElements (Int offset overload) passes type as plain Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glDrawElements(PrimitiveMode.Lines, 6, DataType.UnsignedInt, 0)
    assertEquals(stub.lastMethod, "glDrawElements-int")
    assertEquals(stub.lastArgs, Seq(0x0001, 6, 0x1405, 0))
  }

  test("DataType: glVertexAttribPointer (Buffer overload) passes type as plain Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    val buf  = ByteBuffer.allocate(16)
    gl.glVertexAttribPointer(0, 3, DataType.Float, false, 12, buf)
    assertEquals(stub.lastMethod, "glVertexAttribPointer-buffer")
    assertEquals(stub.lastArgs, Seq(0x1406))
  }

  test("DataType: glVertexAttribPointer (Int offset overload) passes type as plain Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glVertexAttribPointer(1, 2, DataType.Short, true, 8, 0)
    assertEquals(stub.lastMethod, "glVertexAttribPointer-int")
    assertEquals(stub.lastArgs, Seq(0x1402))
  }

  test("DataType: additional constant values are correct") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glVertexAttribPointer(0, 1, DataType.Byte, false, 0, 0)
    assertEquals(stub.lastArgs, Seq(0x1400))
    gl.glVertexAttribPointer(0, 1, DataType.UnsignedByte, false, 0, 0)
    assertEquals(stub.lastArgs, Seq(0x1401))
    gl.glVertexAttribPointer(0, 1, DataType.Int, false, 0, 0)
    assertEquals(stub.lastArgs, Seq(0x1404))
    gl.glVertexAttribPointer(0, 1, DataType.Fixed, false, 0, 0)
    assertEquals(stub.lastArgs, Seq(0x140c))
    gl.glVertexAttribPointer(0, 1, DataType.HalfFloat, false, 0, 0)
    assertEquals(stub.lastArgs, Seq(0x140b))
  }

  // ---------------------------------------------------------------------------
  // PixelFormat
  // ---------------------------------------------------------------------------

  test("PixelFormat: glReadPixels passes format and type as plain Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    val buf  = ByteBuffer.allocate(16)
    gl.glReadPixels(Pixels(0), Pixels(0), Pixels(4), Pixels(4), PixelFormat.RGBA, DataType.UnsignedByte, buf)
    assertEquals(stub.lastMethod, "glReadPixels")
    assertEquals(stub.lastArgs, Seq(0x1908, 0x1401))
  }

  test("PixelFormat: glTexImage2D passes target, format, and type as plain Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    val buf  = ByteBuffer.allocate(16)
    gl.glTexImage2D(TextureTarget.Texture2D, 0, GL20.GL_RGBA, Pixels(4), Pixels(4), 0, PixelFormat.RGBA, DataType.UnsignedByte, buf)
    assertEquals(stub.lastMethod, "glTexImage2D")
    assertEquals(stub.lastArgs, Seq(0x0de1, 0x1908, 0x1401))
  }

  test("PixelFormat: glTexSubImage2D passes target, format, and type as plain Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    val buf  = ByteBuffer.allocate(16)
    gl.glTexSubImage2D(TextureTarget.TextureCubeMapPositiveX, 0, Pixels(0), Pixels(0), Pixels(2), Pixels(2), PixelFormat.RGB, DataType.Float, buf)
    assertEquals(stub.lastMethod, "glTexSubImage2D")
    assertEquals(stub.lastArgs, Seq(0x8515, 0x1907, 0x1406))
  }

  test("PixelFormat: various format constants are correct") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    val buf  = ByteBuffer.allocate(16)
    gl.glReadPixels(Pixels(0), Pixels(0), Pixels(1), Pixels(1), PixelFormat.Alpha, DataType.UnsignedByte, buf)
    assertEquals(stub.lastArgs(0), 0x1906)
    gl.glReadPixels(Pixels(0), Pixels(0), Pixels(1), Pixels(1), PixelFormat.Luminance, DataType.UnsignedByte, buf)
    assertEquals(stub.lastArgs(0), 0x1909)
    gl.glReadPixels(Pixels(0), Pixels(0), Pixels(1), Pixels(1), PixelFormat.LuminanceAlpha, DataType.UnsignedByte, buf)
    assertEquals(stub.lastArgs(0), 0x190a)
    gl.glReadPixels(Pixels(0), Pixels(0), Pixels(1), Pixels(1), PixelFormat.DepthComponent, DataType.UnsignedByte, buf)
    assertEquals(stub.lastArgs(0), 0x1902)
  }

  test("PixelFormat: GL30+ format constants are correct") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    val buf  = ByteBuffer.allocate(16)
    gl.glReadPixels(Pixels(0), Pixels(0), Pixels(1), Pixels(1), PixelFormat.Red, DataType.UnsignedByte, buf)
    assertEquals(stub.lastArgs(0), 0x1903)
    gl.glReadPixels(Pixels(0), Pixels(0), Pixels(1), Pixels(1), PixelFormat.RG, DataType.UnsignedByte, buf)
    assertEquals(stub.lastArgs(0), 0x8227)
    gl.glReadPixels(Pixels(0), Pixels(0), Pixels(1), Pixels(1), PixelFormat.RGBAInteger, DataType.UnsignedByte, buf)
    assertEquals(stub.lastArgs(0), 0x8d99)
    gl.glReadPixels(Pixels(0), Pixels(0), Pixels(1), Pixels(1), PixelFormat.DepthStencil, DataType.UnsignedByte, buf)
    assertEquals(stub.lastArgs(0), 0x84f9)
  }

  // ---------------------------------------------------------------------------
  // BufferUsage
  // ---------------------------------------------------------------------------

  test("BufferUsage: glBufferData passes target and usage as plain Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    val buf  = ByteBuffer.allocate(64)
    gl.glBufferData(BufferTarget.ArrayBuffer, 64, buf, BufferUsage.StaticDraw)
    assertEquals(stub.lastMethod, "glBufferData")
    assertEquals(stub.lastArgs, Seq(0x8892, 0x88e4))
  }

  test("BufferUsage: StreamDraw and DynamicDraw constant values") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    val buf  = ByteBuffer.allocate(8)
    gl.glBufferData(BufferTarget.ElementArrayBuffer, 8, buf, BufferUsage.StreamDraw)
    assertEquals(stub.lastArgs, Seq(0x8893, 0x88e0))
    gl.glBufferData(BufferTarget.ArrayBuffer, 8, buf, BufferUsage.DynamicDraw)
    assertEquals(stub.lastArgs, Seq(0x8892, 0x88e8))
  }

  test("BufferUsage: GL30+ constant values are correct") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    val buf  = ByteBuffer.allocate(8)
    gl.glBufferData(BufferTarget.ArrayBuffer, 8, buf, BufferUsage.StreamRead)
    assertEquals(stub.lastArgs(1), 0x88e1)
    gl.glBufferData(BufferTarget.ArrayBuffer, 8, buf, BufferUsage.StreamCopy)
    assertEquals(stub.lastArgs(1), 0x88e2)
    gl.glBufferData(BufferTarget.ArrayBuffer, 8, buf, BufferUsage.StaticRead)
    assertEquals(stub.lastArgs(1), 0x88e5)
    gl.glBufferData(BufferTarget.ArrayBuffer, 8, buf, BufferUsage.StaticCopy)
    assertEquals(stub.lastArgs(1), 0x88e6)
    gl.glBufferData(BufferTarget.ArrayBuffer, 8, buf, BufferUsage.DynamicRead)
    assertEquals(stub.lastArgs(1), 0x88e9)
    gl.glBufferData(BufferTarget.ArrayBuffer, 8, buf, BufferUsage.DynamicCopy)
    assertEquals(stub.lastArgs(1), 0x88ea)
  }

  // ---------------------------------------------------------------------------
  // Additional TextureTarget coverage
  // ---------------------------------------------------------------------------

  test("TextureTarget: glGenerateMipmap passes target as plain Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glGenerateMipmap(TextureTarget.Texture2D)
    assertEquals(stub.lastMethod, "glGenerateMipmap")
    assertEquals(stub.lastArgs, Seq(0x0de1))
  }

  test("TextureTarget: glGenerateMipmap with CubeMap") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glGenerateMipmap(TextureTarget.TextureCubeMap)
    assertEquals(stub.lastArgs, Seq(0x8513))
  }

  test("TextureTarget: glFramebufferTexture2D passes textarget as plain Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_COLOR_ATTACHMENT0, TextureTarget.Texture2D, 1, 0)
    assertEquals(stub.lastMethod, "glFramebufferTexture2D")
    assertEquals(stub.lastArgs, Seq(0x0de1))
  }

  test("TextureTarget: glFramebufferTexture2D with cube map face") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_COLOR_ATTACHMENT0, TextureTarget.TextureCubeMapNegativeZ, 1, 0)
    assertEquals(stub.lastArgs, Seq(0x851a))
  }

  test("TextureTarget: glTexParameteri passes target as plain Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glTexParameteri(TextureTarget.Texture2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_LINEAR)
    assertEquals(stub.lastMethod, "glTexParameteri")
    assertEquals(stub.lastArgs, Seq(0x0de1, 0x2801, 0x2601))
  }

  test("TextureTarget: GL30+ targets have correct values") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glGenerateMipmap(TextureTarget.Texture3D)
    assertEquals(stub.lastArgs, Seq(0x806f))
    gl.glGenerateMipmap(TextureTarget.Texture2DArray)
    assertEquals(stub.lastArgs, Seq(0x8c1a))
  }

  // ---------------------------------------------------------------------------
  // Additional EnableCap coverage
  // ---------------------------------------------------------------------------

  test("EnableCap: glDisable passes cap as plain Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glDisable(EnableCap.Blend)
    assertEquals(stub.lastMethod, "glDisable")
    assertEquals(stub.lastArgs, Seq(0x0be2))
  }

  test("EnableCap: glIsEnabled passes cap as plain Int and returns result") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    val res  = gl.glIsEnabled(EnableCap.ScissorTest)
    assertEquals(stub.lastMethod, "glIsEnabled")
    assertEquals(stub.lastArgs, Seq(0x0c11))
    assertEquals(res, true)
  }

  test("EnableCap: additional capability constants") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glEnable(EnableCap.CullFace)
    assertEquals(stub.lastArgs, Seq(0x0b44))
    gl.glEnable(EnableCap.StencilTest)
    assertEquals(stub.lastArgs, Seq(0x0b90))
    gl.glEnable(EnableCap.Dither)
    assertEquals(stub.lastArgs, Seq(0x0bd0))
    gl.glEnable(EnableCap.PolygonOffsetFill)
    assertEquals(stub.lastArgs, Seq(0x8037))
    gl.glEnable(EnableCap.SampleAlphaToCoverage)
    assertEquals(stub.lastArgs, Seq(0x809e))
    gl.glEnable(EnableCap.SampleCoverage)
    assertEquals(stub.lastArgs, Seq(0x80a0))
    gl.glEnable(EnableCap.VertexProgramPointSize)
    assertEquals(stub.lastArgs, Seq(0x8642))
  }

  // ---------------------------------------------------------------------------
  // Additional BufferTarget coverage
  // ---------------------------------------------------------------------------

  test("BufferTarget: glBufferSubData passes target as plain Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    val buf  = ByteBuffer.allocate(32)
    gl.glBufferSubData(BufferTarget.ElementArrayBuffer, 0, 32, buf)
    assertEquals(stub.lastMethod, "glBufferSubData")
    assertEquals(stub.lastArgs, Seq(0x8893))
  }

  test("BufferTarget: glGetBufferParameteriv passes target as plain Int") {
    val stub   = StubGL20Ops()
    val gl     = AndroidGL20Adapter(stub)
    val params = IntBuffer.allocate(1)
    gl.glGetBufferParameteriv(BufferTarget.ArrayBuffer, GL20.GL_BUFFER_SIZE, params)
    assertEquals(stub.lastMethod, "glGetBufferParameteriv")
    assertEquals(stub.lastArgs, Seq(0x8892))
  }

  test("BufferTarget: GL30+ buffer target constants") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    val buf  = ByteBuffer.allocate(8)
    gl.glBufferSubData(BufferTarget.PixelPackBuffer, 0, 8, buf)
    assertEquals(stub.lastArgs, Seq(0x88eb))
    gl.glBufferSubData(BufferTarget.PixelUnpackBuffer, 0, 8, buf)
    assertEquals(stub.lastArgs, Seq(0x88ec))
    gl.glBufferSubData(BufferTarget.CopyReadBuffer, 0, 8, buf)
    assertEquals(stub.lastArgs, Seq(0x8f36))
    gl.glBufferSubData(BufferTarget.CopyWriteBuffer, 0, 8, buf)
    assertEquals(stub.lastArgs, Seq(0x8f37))
    gl.glBufferSubData(BufferTarget.TransformFeedbackBuffer, 0, 8, buf)
    assertEquals(stub.lastArgs, Seq(0x8c8e))
    gl.glBufferSubData(BufferTarget.UniformBuffer, 0, 8, buf)
    assertEquals(stub.lastArgs, Seq(0x8a11))
  }

  // ---------------------------------------------------------------------------
  // Additional BlendFactor coverage
  // ---------------------------------------------------------------------------

  test("BlendFactor: glBlendFuncSeparate passes all four factors as plain Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glBlendFuncSeparate(BlendFactor.SrcAlpha, BlendFactor.OneMinusSrcAlpha, BlendFactor.One, BlendFactor.Zero)
    assertEquals(stub.lastMethod, "glBlendFuncSeparate")
    assertEquals(stub.lastArgs, Seq(0x0302, 0x0303, 1, 0))
  }

  test("BlendFactor: additional blend factor constants") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glBlendFuncSeparate(BlendFactor.DstColor, BlendFactor.OneMinusDstColor, BlendFactor.DstAlpha, BlendFactor.OneMinusDstAlpha)
    assertEquals(stub.lastArgs, Seq(0x0306, 0x0307, 0x0304, 0x0305))
  }

  test("BlendFactor: constant/saturate blend factor constants") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glBlendFuncSeparate(
      BlendFactor.ConstantColor,
      BlendFactor.OneMinusConstantColor,
      BlendFactor.ConstantAlpha,
      BlendFactor.OneMinusConstantAlpha
    )
    assertEquals(stub.lastArgs, Seq(0x8001, 0x8002, 0x8003, 0x8004))
  }

  test("BlendFactor: SrcColor, OneMinusSrcColor, SrcAlphaSaturate constants") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glBlendFuncSeparate(BlendFactor.SrcColor, BlendFactor.OneMinusSrcColor, BlendFactor.SrcAlphaSaturate, BlendFactor.One)
    assertEquals(stub.lastArgs, Seq(0x0300, 0x0301, 0x0308, 1))
  }
}
