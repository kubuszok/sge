/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Headless test fixture for the VisUI extension (ISS-515 red suite).
 *
 * The sge core test fixture (sge/src/test/scala/sge/SgeTestFixture.scala and
 * sge/src/test/scala/sge/NoopGL20.scala) is NOT on this module's test
 * classpath — build.sbt wires `sge-visui`.dependsOn(sge) in the Compile
 * configuration only (no test->test), matching every other extension module.
 * Following the established sibling precedent (the per-platform fixture
 * replication in sge/src/test/scalajvm and sge/src/test/scalanative red
 * suites), the minimal pieces are replicated here:
 *
 *   - HeadlessNoopGL20 mirrors sge/src/test/scala/sge/NoopGL20.scala: every
 *     GL call is a no-op returning zero/false/empty/Unit. Texture and
 *     BitmapFont construction during Skin loading only needs glGenTexture /
 *     glBindTexture / glTexImage2D / glTexParameter* — all safe no-ops.
 *     (No ShaderProgram is constructed during VisUI.load, so the
 *     compile-status faking of the ShaderCompilingGL20 variant used by
 *     SpriteCacheRedSuite/DecalSortRedSuite is not needed here.)
 *   - headlessSge() mirrors SgeTestFixture.testSge but uses the REAL
 *     DesktopFiles (sge/src/main/scaladesktop/sge/files/DesktopFiles.scala)
 *     so that Sge().files.classpath(...) genuinely resolves against the test
 *     runtime classpath — which is exactly what ISS-515 is about: the
 *     resources VisUI.load()/Locales/PickerCommons request are missing from
 *     sge-extension/visui/src/main/resources.
 *   - PNG decoding on the JVM goes through ImageIO
 *     (sge/src/main/scalajvm/sge/platform/Gdx2dOpsJvm.scala), so once the
 *     resources exist the whole skin pipeline works headlessly.
 *
 * The Application/Net stubs throw IllegalStateException from members that a
 * headless resource-loading test must never touch; no code path exercised by
 * the red suite reaches them.
 */
package sge
package visui

import java.nio.{ Buffer, FloatBuffer, IntBuffer }

import sge.files.DesktopFiles
import sge.graphics._
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }

object VisUITestFixture {

  /** Creates a headless [[Sge]] whose `files` is the real [[DesktopFiles]] (classpath resolution works) and whose GL is a no-op. */
  def headlessSge(): Sge =
    Sge(
      application = StubApplication,
      graphics = new NoopGraphics() {
        override def gl20: GL20 = HeadlessNoopGL20
      },
      audio = new NoopAudio(),
      files = new DesktopFiles(),
      input = new NoopInput(),
      net = StubNet
    )

  private def unused(member: String): Nothing =
    throw new IllegalStateException(s"$member must not be touched by the headless VisUI resource tests")

  private object StubApplication extends Application {
    def applicationListener:                                  ApplicationListener         = unused("Application.applicationListener")
    def graphics:                                             Graphics                    = unused("Application.graphics")
    def audio:                                                Audio                       = unused("Application.audio")
    def input:                                                Input                       = unused("Application.input")
    def files:                                                Files                       = unused("Application.files")
    def net:                                                  Net                         = unused("Application.net")
    def applicationType:                                      Application.ApplicationType = Application.ApplicationType.HeadlessDesktop
    def version:                                              Int                         = 0
    def javaHeap:                                             Long                        = 0L
    def nativeHeap:                                           Long                        = 0L
    def getPreferences(name:              String):            Preferences                 = unused("Application.getPreferences")
    def clipboard:                                            sge.utils.Clipboard         = unused("Application.clipboard")
    def postRunnable(runnable:            Runnable):          Unit                        = ()
    def exit():                                               Unit                        = ()
    def addLifecycleListener(listener:    LifecycleListener): Unit                        = ()
    def removeLifecycleListener(listener: LifecycleListener): Unit                        = ()
  }

  private object StubNet extends Net {
    import Net._
    def httpClient:                                                                                     net.SgeHttpClient = net.SgeHttpClient.noop()
    def newServerSocket(protocol: Protocol, hostname: String, port: Int, hints: net.ServerSocketHints): net.ServerSocket  = unused("Net.newServerSocket")
    def newServerSocket(protocol: Protocol, port:     Int, hints:   net.ServerSocketHints):             net.ServerSocket  = unused("Net.newServerSocket")
    def newClientSocket(protocol: Protocol, host:     String, port: Int, hints: net.SocketHints):       net.Socket        = unused("Net.newClientSocket")
    def openURI(URI:              String):                                                              Boolean           = false
  }
}

/** No-op GL20 — replica of sge/src/test/scala/sge/NoopGL20.scala (unreachable from this module's test classpath, see header). */
object HeadlessNoopGL20 extends GL20 {

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
}
