// SGE — Unit test: AndroidGL20Adapter
//
// Tests that the adapter correctly bridges GL20Ops (plain Int) to GL20 (opaque types).

package sge
package graphics

import java.nio.{ Buffer, FloatBuffer, IntBuffer }
import munit.FunSuite
import sge.platform.android.GL20Ops

class AndroidGL20AdapterTest extends FunSuite {

  // Minimal stub that records the last call
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
    override def glClearColor(red:        Float, green:   Float, blue:   Float, alpha:   Float):   Unit = record("glClearColor", red, green, blue, alpha)
    override def glClearDepthf(depth:     Float):                                                  Unit = record("glClearDepthf", depth)
    override def glClearStencil(s:        Int):                                                    Unit = record("glClearStencil", s)
    override def glColorMask(red:         Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit = record("glColorMask")
    override def glCompressedTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, imageSize: Int, data: Buffer): Unit = record("glCompressedTexImage2D")
    override def glCompressedTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, imageSize: Int, data: Buffer): Unit = record(
      "glCompressedTexSubImage2D"
    )
    override def glCopyTexImage2D(target:    Int, level:    Int, internalformat: Int, x:       Int, y: Int, width: Int, height: Int, border: Int): Unit = record("glCopyTexImage2D")
    override def glCopyTexSubImage2D(target: Int, level:    Int, xoffset:        Int, yoffset: Int, x: Int, y:     Int, width:  Int, height: Int): Unit = record("glCopyTexSubImage2D")
    override def glCullFace(mode:            Int):                                                                                                 Unit = record("glCullFace", mode)
    override def glDeleteTextures(n:         Int, textures: IntBuffer):                                                                            Unit = record("glDeleteTextures")
    override def glDeleteTexture(texture:    Int):                                                                                                 Unit = record("glDeleteTexture", texture)
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
    override def glReadPixels(x:         Int, y:        Int, width:  Int, height:  Int, format: Int, `type`: Int, pixels: Buffer): Unit   = record("glReadPixels")
    override def glScissor(x:            Int, y:        Int, width:  Int, height:  Int):                                           Unit   = record("glScissor", x, y, width, height)
    override def glStencilFunc(func:     Int, ref:      Int, mask:   Int):                                                         Unit   = record("glStencilFunc", func)
    override def glStencilMask(mask:     Int):                                                                                     Unit   = record("glStencilMask")
    override def glStencilOp(fail:       Int, zfail:    Int, zpass:  Int):                                                         Unit   = record("glStencilOp", fail, zfail, zpass)
    override def glTexImage2D(target:    Int, level: Int, internalformat: Int, width:   Int, height: Int, border: Int, format: Int, `type`: Int, pixels: Buffer): Unit = record("glTexImage2D")
    override def glTexParameterf(target: Int, pname: Int, param:          Float):                                                                                 Unit = record("glTexParameterf")
    override def glTexSubImage2D(target: Int, level: Int, xoffset:        Int, yoffset: Int, width:  Int, height: Int, format: Int, `type`: Int, pixels: Buffer): Unit = record("glTexSubImage2D")
    override def glViewport(x:                      Int, y:             Int, width:              Int, height:       Int):             Unit = record("glViewport", x, y, width, height)
    override def glAttachShader(program:            Int, shader:        Int):                                                         Unit = record("glAttachShader")
    override def glBindAttribLocation(program:      Int, index:         Int, name:               String):                             Unit = record("glBindAttribLocation")
    override def glBindBuffer(target:               Int, buffer:        Int):                                                         Unit = record("glBindBuffer", target, buffer)
    override def glBindFramebuffer(target:          Int, framebuffer:   Int):                                                         Unit = record("glBindFramebuffer")
    override def glBindRenderbuffer(target:         Int, renderbuffer:  Int):                                                         Unit = record("glBindRenderbuffer")
    override def glBlendColor(red:                  Float, green:       Float, blue:             Float, alpha:      Float):           Unit = record("glBlendColor")
    override def glBlendEquation(mode:              Int):                                                                             Unit = record("glBlendEquation", mode)
    override def glBlendEquationSeparate(modeRGB:   Int, modeAlpha:     Int):                                                         Unit = record("glBlendEquationSeparate")
    override def glBlendFuncSeparate(srcRGB:        Int, dstRGB:        Int, srcAlpha:           Int, dstAlpha:     Int):             Unit = record("glBlendFuncSeparate")
    override def glBufferData(target:               Int, size:          Int, data:               Buffer, usage:     Int):             Unit = record("glBufferData", target, usage)
    override def glBufferSubData(target:            Int, offset:        Int, size:               Int, data:         Buffer):          Unit = record("glBufferSubData")
    override def glCheckFramebufferStatus(target:   Int):                                                                             Int  = { record("glCheckFramebufferStatus"); 0 }
    override def glCompileShader(shader:            Int):                                                                             Unit = record("glCompileShader")
    override def glCreateProgram():                                                                                                   Int  = { record("glCreateProgram"); 1 }
    override def glCreateShader(`type`:             Int):                                                                             Int  = { record("glCreateShader", `type`); 2 }
    override def glDeleteBuffer(buffer:             Int):                                                                             Unit = record("glDeleteBuffer")
    override def glDeleteBuffers(n:                 Int, buffers:       IntBuffer):                                                   Unit = record("glDeleteBuffers")
    override def glDeleteFramebuffer(framebuffer:   Int):                                                                             Unit = record("glDeleteFramebuffer")
    override def glDeleteFramebuffers(n:            Int, framebuffers:  IntBuffer):                                                   Unit = record("glDeleteFramebuffers")
    override def glDeleteProgram(program:           Int):                                                                             Unit = record("glDeleteProgram")
    override def glDeleteRenderbuffer(renderbuffer: Int):                                                                             Unit = record("glDeleteRenderbuffer")
    override def glDeleteRenderbuffers(n:           Int, renderbuffers: IntBuffer):                                                   Unit = record("glDeleteRenderbuffers")
    override def glDeleteShader(shader:             Int):                                                                             Unit = record("glDeleteShader")
    override def glDetachShader(program:            Int, shader:        Int):                                                         Unit = record("glDetachShader")
    override def glDisableVertexAttribArray(index:  Int):                                                                             Unit = record("glDisableVertexAttribArray")
    override def glDrawElements(mode:               Int, count:         Int, `type`:             Int, indices:      Int):             Unit = record("glDrawElements-int", mode, count, `type`, indices)
    override def glEnableVertexAttribArray(index:   Int):                                                                             Unit = record("glEnableVertexAttribArray")
    override def glFramebufferRenderbuffer(target:  Int, attachment:    Int, renderbuffertarget: Int, renderbuffer: Int):             Unit = record("glFramebufferRenderbuffer")
    override def glFramebufferTexture2D(target:     Int, attachment:    Int, textarget:          Int, texture:      Int, level: Int): Unit = record("glFramebufferTexture2D", textarget)
    override def glGenBuffer():                                                                                                       Int  = { record("glGenBuffer"); 3 }
    override def glGenBuffers(n:                    Int, buffers:       IntBuffer):                                                   Unit = record("glGenBuffers")
    override def glGenerateMipmap(target:           Int):                                                                             Unit = record("glGenerateMipmap", target)
    override def glGenFramebuffer():                                                                                                  Int  = { record("glGenFramebuffer"); 4 }
    override def glGenFramebuffers(n:               Int, framebuffers:  IntBuffer):                                                   Unit = record("glGenFramebuffers")
    override def glGenRenderbuffer():                                                                                                 Int  = { record("glGenRenderbuffer"); 5 }
    override def glGenRenderbuffers(n:              Int, renderbuffers: IntBuffer):                                                   Unit = record("glGenRenderbuffers")
    override def glGetActiveAttrib(program:                    Int, index:          Int, size:    IntBuffer, `type`:    IntBuffer): String  = { record("glGetActiveAttrib"); "attr" }
    override def glGetActiveUniform(program:                   Int, index:          Int, size:    IntBuffer, `type`:    IntBuffer): String  = { record("glGetActiveUniform"); "uniform" }
    override def glGetAttachedShaders(program:                 Int, maxcount:       Int, count:   Buffer, shaders:      IntBuffer): Unit    = record("glGetAttachedShaders")
    override def glGetAttribLocation(program:                  Int, name:           String):                                        Int     = { record("glGetAttribLocation"); 0 }
    override def glGetBooleanv(pname:                          Int, params:         Buffer):                                        Unit    = record("glGetBooleanv")
    override def glGetBufferParameteriv(target:                Int, pname:          Int, params:  IntBuffer):                       Unit    = record("glGetBufferParameteriv")
    override def glGetFloatv(pname:                            Int, params:         FloatBuffer):                                   Unit    = record("glGetFloatv")
    override def glGetFramebufferAttachmentParameteriv(target: Int, attachment:     Int, pname:   Int, params:          IntBuffer): Unit    = record("glGetFramebufferAttachmentParameteriv")
    override def glGetProgramiv(program:                       Int, pname:          Int, params:  IntBuffer):                       Unit    = record("glGetProgramiv")
    override def glGetProgramInfoLog(program:                  Int):                                                                String  = { record("glGetProgramInfoLog"); "" }
    override def glGetRenderbufferParameteriv(target:          Int, pname:          Int, params:  IntBuffer):                       Unit    = record("glGetRenderbufferParameteriv")
    override def glGetShaderiv(shader:                         Int, pname:          Int, params:  IntBuffer):                       Unit    = record("glGetShaderiv")
    override def glGetShaderInfoLog(shader:                    Int):                                                                String  = { record("glGetShaderInfoLog"); "" }
    override def glGetShaderPrecisionFormat(shadertype:        Int, precisiontype:  Int, range:   IntBuffer, precision: IntBuffer): Unit    = record("glGetShaderPrecisionFormat", shadertype)
    override def glGetTexParameterfv(target:                   Int, pname:          Int, params:  FloatBuffer):                     Unit    = record("glGetTexParameterfv")
    override def glGetTexParameteriv(target:                   Int, pname:          Int, params:  IntBuffer):                       Unit    = record("glGetTexParameteriv")
    override def glGetUniformfv(program:                       Int, location:       Int, params:  FloatBuffer):                     Unit    = record("glGetUniformfv")
    override def glGetUniformiv(program:                       Int, location:       Int, params:  IntBuffer):                       Unit    = record("glGetUniformiv")
    override def glGetUniformLocation(program:                 Int, name:           String):                                        Int     = { record("glGetUniformLocation"); 0 }
    override def glGetVertexAttribfv(index:                    Int, pname:          Int, params:  FloatBuffer):                     Unit    = record("glGetVertexAttribfv")
    override def glGetVertexAttribiv(index:                    Int, pname:          Int, params:  IntBuffer):                       Unit    = record("glGetVertexAttribiv")
    override def glGetVertexAttribPointerv(index:              Int, pname:          Int, pointer: Buffer):                          Unit    = record("glGetVertexAttribPointerv")
    override def glIsBuffer(buffer:                            Int):                                                                Boolean = { record("glIsBuffer"); false }
    override def glIsEnabled(cap:                              Int):                                                                Boolean = { record("glIsEnabled", cap); false }
    override def glIsFramebuffer(framebuffer:                  Int):                                                                Boolean = { record("glIsFramebuffer"); false }
    override def glIsProgram(program:                          Int):                                                                Boolean = { record("glIsProgram"); false }
    override def glIsRenderbuffer(renderbuffer:                Int):                                                                Boolean = { record("glIsRenderbuffer"); false }
    override def glIsShader(shader:                            Int):                                                                Boolean = { record("glIsShader"); false }
    override def glIsTexture(texture:                          Int):                                                                Boolean = { record("glIsTexture"); false }
    override def glLinkProgram(program:                        Int):                                                                Unit    = record("glLinkProgram")
    override def glReleaseShaderCompiler():                                                                                         Unit    = record("glReleaseShaderCompiler")
    override def glRenderbufferStorage(target:                 Int, internalformat: Int, width:   Int, height:          Int):       Unit    = record("glRenderbufferStorage", width, height)
    override def glSampleCoverage(value:                       Float, invert:       Boolean):                                       Unit    = record("glSampleCoverage")
    override def glShaderBinary(n:            Int, shaders: IntBuffer, binaryformat: Int, binary:          Buffer, length:       Int):   Unit = record("glShaderBinary")
    override def glShaderSource(shader:       Int, string:  String):                                                                     Unit = record("glShaderSource")
    override def glStencilFuncSeparate(face:  Int, func:    Int, ref:                Int, mask:            Int):                         Unit = record("glStencilFuncSeparate", face, func)
    override def glStencilMaskSeparate(face:  Int, mask:    Int):                                                                        Unit = record("glStencilMaskSeparate", face)
    override def glStencilOpSeparate(face:    Int, fail:    Int, zfail:              Int, zpass:           Int):                         Unit = record("glStencilOpSeparate", face, fail)
    override def glTexParameterfv(target:     Int, pname:   Int, params:             FloatBuffer):                                       Unit = record("glTexParameterfv")
    override def glTexParameteri(target:      Int, pname:   Int, param:              Int):                                               Unit = record("glTexParameteri")
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

  test("opaque TextureTarget is passed as plain Int to ops") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glBindTexture(TextureTarget.Texture2D, 1)
    assertEquals(stub.lastMethod, "glBindTexture")
    assertEquals(stub.lastArgs, Seq(0x0de1, 1))
  }

  test("opaque BlendFactor is passed as plain Int to ops") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glBlendFunc(BlendFactor.SrcAlpha, BlendFactor.OneMinusSrcAlpha)
    assertEquals(stub.lastMethod, "glBlendFunc")
    assertEquals(stub.lastArgs, Seq(0x0302, 0x0303))
  }

  test("opaque ClearMask is passed as plain Int to ops") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glClear(ClearMask.ColorBufferBit | ClearMask.DepthBufferBit)
    assertEquals(stub.lastMethod, "glClear")
    assertEquals(stub.lastArgs, Seq(0x00004000 | 0x00000100))
  }

  test("opaque PrimitiveMode is passed as plain Int to ops") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glDrawArrays(PrimitiveMode.Triangles, 0, 6)
    assertEquals(stub.lastMethod, "glDrawArrays")
    assertEquals(stub.lastArgs, Seq(0x0004, 0, 6))
  }

  test("Pixels params are unwrapped to Int") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glViewport(Pixels(10), Pixels(20), Pixels(640), Pixels(480))
    assertEquals(stub.lastMethod, "glViewport")
    assertEquals(stub.lastArgs, Seq(10, 20, 640, 480))
  }

  test("opaque ShaderType is passed as plain Int to ops") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    val id   = gl.glCreateShader(ShaderType.Fragment)
    assertEquals(stub.lastMethod, "glCreateShader")
    assertEquals(stub.lastArgs, Seq(0x8b30))
    assertEquals(id, 2)
  }

  test("opaque BufferTarget is passed as plain Int to ops") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glBindBuffer(BufferTarget.ArrayBuffer, 7)
    assertEquals(stub.lastMethod, "glBindBuffer")
    assertEquals(stub.lastArgs, Seq(0x8892, 7))
  }

  test("opaque EnableCap is passed as plain Int to ops") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    gl.glEnable(EnableCap.DepthTest)
    assertEquals(stub.lastMethod, "glEnable")
    assertEquals(stub.lastArgs, Seq(0x0b71))
  }

  test("return values pass through correctly") {
    val stub = StubGL20Ops()
    val gl   = AndroidGL20Adapter(stub)
    assertEquals(gl.glGenTexture(), 42)
    assertEquals(gl.glGetError(), 0)
    assertEquals(gl.glGetString(GL20.GL_VERSION), "test")
  }
}
