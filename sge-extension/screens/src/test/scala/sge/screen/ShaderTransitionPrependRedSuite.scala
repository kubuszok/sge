/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-528 clause 1 (ShaderTransition ignorePrepend is DEAD).
 *
 * The original de/eskalon/commons/screen/transition/impl/ShaderTransition.java
 * (original-src/libgdx-screenmanager, lines 114-139) builds its ShaderProgram
 * via `ShaderProgramFactory.fromString(vert, frag, true, ignorePrepend)`. The
 * guacamole ShaderProgramFactory's `ignorePrepend` contract is: when true, the
 * global ShaderProgram.prependVertexCode / prependFragmentCode are NOT applied
 * to the compiled shader source (the factory saves the two static fields,
 * clears them around construction, then restores them) — used because the
 * supplied shaders (e.g. the GLTransitions shaders, and any caller that has
 * already set a global #version prepend via colorful/gltf) carry their own
 * header and would be corrupted by a second prepend.
 *
 * The SGE port (sge-extension/screens/.../impl/ShaderTransition.scala, ctor
 * param `ignorePrepend` at ~line 45 and `new ShaderProgram(vert, frag)` at
 * ~line 51) passes the source straight to `new ShaderProgram(...)`, which
 * UNCONDITIONALLY applies the global prepends in its constructor
 * (ShaderProgram.scala lines 56-63):
 *     actualVertexShader =
 *       if (ShaderProgram.prependVertexCode.nonEmpty)
 *         ShaderProgram.prependVertexCode + vertexShader else vertexShader
 * `ignorePrepend` is never read, so it is DEAD: setting it true does not stop
 * the prepend.
 *
 * Testable seam: ShaderProgram exposes the *actual* (prepend-applied) source
 * via vertexShaderSource / fragmentShaderSource (ShaderProgram.scala lines
 * 970, 974); these are assigned in the constructor regardless of whether the
 * shader actually compiles, so they are inspectable headlessly. The
 * ShaderTransition exposes its program via getProgram.
 *
 * Correct behaviour encoded below:
 *   - ignorePrepend = true  -> compiled vertex/fragment source must NOT contain
 *                              the global prepend text.
 *   - ignorePrepend = false -> compiled source MUST contain it (discriminating
 *                              guard; this is the existing, correct path and
 *                              passes today).
 *
 * Headless fixture: glCreateShader returns 0, so ShaderProgram's loadShader
 * short-circuits to compiledSuccessfully = false and skips
 * fetchAttributes/fetchUniforms — construction completes without a real GL
 * context while still populating vertexShaderSource/fragmentShaderSource. The
 * sge core test fixture (sge/src/test/.../NoopGL20.scala) is NOT on this
 * module's test classpath (sge-screens.dependsOn(sge) is Compile-only), so the
 * minimal no-op GL20 is replicated here, matching the sibling precedent in
 * sge-extension/visui/src/test/scalajvm/sge/visui/VisUITestFixture.scala.
 *
 * Written by the reproducer agent; the fixer MUST NOT modify these tests.
 */
package sge
package screen

import java.nio.{ Buffer, FloatBuffer, IntBuffer }

import sge.graphics._
import sge.graphics.glutils.ShaderProgram
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }
import sge.screen.transition.impl.ShaderTransition

class ShaderTransitionPrependRedSuite extends munit.FunSuite {

  // -- Headless fixture -------------------------------------------------------

  private given Sge = Sge(
    application = StubApplication,
    graphics = new NoopGraphics() {
      override def gl20: GL20 = ShaderTransitionNoopGL20
    },
    audio = new NoopAudio(),
    files = null.asInstanceOf[Files], // @nowarn — not touched by these tests
    input = new NoopInput(),
    net = null.asInstanceOf[Net] // @nowarn — not touched by these tests
  )

  /** Distinctive prepend markers that must never appear verbatim in a real shader body. */
  private val vertPrepend = "#version 130 // SGE-ISS528-VERT-PREPEND-MARKER\n"
  private val fragPrepend = "#version 130 // SGE-ISS528-FRAG-PREPEND-MARKER\n"

  private val vertBody = "// already-headed vertex shader\nvoid main(){ gl_Position = vec4(0.0); }"
  private val fragBody = "// already-headed fragment shader\nvoid main(){ gl_FragColor = vec4(1.0); }"

  /** Runs `body` with the two global prepends set, restoring whatever was there before (the prepend fields are app-global mutable state). */
  private def withGlobalPrepends[A](body: => A): A = {
    val savedVert = ShaderProgram.prependVertexCode
    val savedFrag = ShaderProgram.prependFragmentCode
    ShaderProgram.prependVertexCode = vertPrepend
    ShaderProgram.prependFragmentCode = fragPrepend
    try body
    finally {
      ShaderProgram.prependVertexCode = savedVert
      ShaderProgram.prependFragmentCode = savedFrag
    }
  }

  // -- ISS-528: ignorePrepend = true must suppress the global prepend ---------

  test("ISS-528: with ignorePrepend = true the compiled vertex source must NOT contain the global prependVertexCode") {
    withGlobalPrepends {
      val transition = new ShaderTransition(vertBody, fragBody, ignorePrepend = true, duration = 1.0f)
      val src        = transition.getProgram.vertexShaderSource
      assert(
        !src.contains("SGE-ISS528-VERT-PREPEND-MARKER"),
        s"ignorePrepend=true must build the ShaderProgram WITHOUT prependVertexCode, but the compiled vertex source was:\n$src"
      )
    }
  }

  test("ISS-528: with ignorePrepend = true the compiled fragment source must NOT contain the global prependFragmentCode") {
    withGlobalPrepends {
      val transition = new ShaderTransition(vertBody, fragBody, ignorePrepend = true, duration = 1.0f)
      val src        = transition.getProgram.fragmentShaderSource
      assert(
        !src.contains("SGE-ISS528-FRAG-PREPEND-MARKER"),
        s"ignorePrepend=true must build the ShaderProgram WITHOUT prependFragmentCode, but the compiled fragment source was:\n$src"
      )
    }
  }

  test("ISS-528: with ignorePrepend = true the compiled vertex source must equal the supplied body verbatim") {
    withGlobalPrepends {
      val transition = new ShaderTransition(vertBody, fragBody, ignorePrepend = true, duration = 1.0f)
      assertEquals(
        transition.getProgram.vertexShaderSource,
        vertBody,
        "ignorePrepend=true must leave the supplied vertex shader untouched"
      )
    }
  }

  // -- Discriminating guard: ignorePrepend = false keeps the prepend ----------
  // (This path is correct today and must keep passing after the fix; it proves
  //  the test distinguishes the two modes rather than blanket-asserting.)

  test("ISS-528 guard: with ignorePrepend = false the compiled vertex source MUST contain the global prependVertexCode") {
    withGlobalPrepends {
      val transition = new ShaderTransition(vertBody, fragBody, ignorePrepend = false, duration = 1.0f)
      val src        = transition.getProgram.vertexShaderSource
      assert(
        src.contains("SGE-ISS528-VERT-PREPEND-MARKER"),
        s"ignorePrepend=false must apply prependVertexCode, but the compiled vertex source was:\n$src"
      )
    }
  }

  test("ISS-528 guard: with ignorePrepend = false the compiled fragment source MUST contain the global prependFragmentCode") {
    withGlobalPrepends {
      val transition = new ShaderTransition(vertBody, fragBody, ignorePrepend = false, duration = 1.0f)
      val src        = transition.getProgram.fragmentShaderSource
      assert(
        src.contains("SGE-ISS528-FRAG-PREPEND-MARKER"),
        s"ignorePrepend=false must apply prependFragmentCode, but the compiled fragment source was:\n$src"
      )
    }
  }

  // -- Sanity: with no global prepend set, both modes leave the body verbatim --

  test("ISS-528 sanity: with no global prepend, ignorePrepend = true leaves the fragment body verbatim") {
    val savedFrag = ShaderProgram.prependFragmentCode
    val savedVert = ShaderProgram.prependVertexCode
    ShaderProgram.prependVertexCode = ""
    ShaderProgram.prependFragmentCode = ""
    try {
      val transition = new ShaderTransition(vertBody, fragBody, ignorePrepend = true, duration = 1.0f)
      assertEquals(transition.getProgram.fragmentShaderSource, fragBody)
    } finally {
      ShaderProgram.prependVertexCode = savedVert
      ShaderProgram.prependFragmentCode = savedFrag
    }
  }
}

/** No-op Application stub; members a headless shader-construction test never touches throw. */
private object StubApplication extends Application {
  private def unused(member: String): Nothing =
    throw new IllegalStateException(s"$member must not be touched by the headless ShaderTransition prepend tests")

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

/** No-op GL20 — minimal replica of sge/src/test/scala/sge/NoopGL20.scala (not on this module's test classpath; see suite header). glCreateShader returns 0 so ShaderProgram construction completes
  * without a real GL context while still populating vertexShaderSource/fragmentShaderSource.
  */
private object ShaderTransitionNoopGL20 extends GL20 {

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
