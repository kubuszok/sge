/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../GwtGL20.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: GwtGL20 -> WebGL20
 *   Convention: Scala.js only; GWT JSNI -> js.Dynamic calls
 *   Convention: IntMap<JSO> -> js.Array[js.Dynamic] for WebGL handle mapping
 *   Convention: GWT HasArrayBufferView -> manual buffer-to-typed-array copy
 *   Convention: GWT WebGLRenderingContext typed methods -> js.Dynamic WebGL API calls
 *   Idiom: GdxRuntimeException -> SgeError.GraphicsError
 *   TODO: Pixmap-based texImage2D/texSubImage2D path (requires browser Pixmap implementation)
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import java.nio.{ Buffer, ByteBuffer, FloatBuffer, IntBuffer, ShortBuffer }
import scala.scalajs.js
import scala.scalajs.js.typedarray._
import scala.util.boundary
import scala.util.boundary.break

/** WebGL 1.0 implementation of [[GL20]] for the browser backend.
  *
  * WebGL returns JavaScript objects (WebGLProgram, WebGLShader, etc.) but the [[GL20]] trait uses integer handles. This class maintains `js.Array[js.Dynamic]` maps that assign integer IDs to JS
  * objects. Index 0 is always `undefined` (sentinel for "no object").
  *
  * @param gl
  *   the WebGLRenderingContext obtained from a canvas element
  */
class WebGL20(val gl: js.Dynamic) extends GL20 {

  // ------ Handle maps: index 0 = undefined sentinel, real IDs start at 1 ------

  protected val programs:      js.Array[js.Dynamic]           = js.Array(js.undefined.asInstanceOf[js.Dynamic])
  protected val shaders:       js.Array[js.Dynamic]           = js.Array(js.undefined.asInstanceOf[js.Dynamic])
  protected val buffers:       js.Array[js.Dynamic]           = js.Array(js.undefined.asInstanceOf[js.Dynamic])
  protected val frameBuffers:  js.Array[js.Dynamic]           = js.Array(js.undefined.asInstanceOf[js.Dynamic])
  protected val renderBuffers: js.Array[js.Dynamic]           = js.Array(js.undefined.asInstanceOf[js.Dynamic])
  protected val textures:      js.Array[js.Dynamic]           = js.Array(js.undefined.asInstanceOf[js.Dynamic])
  protected val uniforms:      js.Array[js.Array[js.Dynamic]] = js.Array(null.asInstanceOf[js.Array[js.Dynamic]])
  protected var currProgram:   Int                            = 0

  // ------ Scratch typed arrays for buffer→TypedArray conversion ------

  private var floatBuf: Float32Array = new Float32Array(2000 * 20)
  protected var intBuf: Int32Array   = new Int32Array(2000 * 6)
  private var shortBuf: Int16Array   = new Int16Array(2000 * 6)
  private var byteBuf:  Int8Array    = new Int8Array(2000 * 6)

  // ------ Init: disable premultiplied alpha ------
  locally {
    gl.pixelStorei(0x9241, 0) // UNPACK_PREMULTIPLY_ALPHA_WEBGL
  }

  // ------ Handle map helpers ------

  protected def addHandle(arr: js.Array[js.Dynamic], value: js.Dynamic): Int = {
    arr.push(value)
    arr.length - 1
  }

  protected def removeHandle(arr: js.Array[js.Dynamic], id: Int): js.Dynamic = {
    val old = arr(id)
    arr(id) = js.undefined.asInstanceOf[js.Dynamic]
    old
  }

  protected def findKey(arr: js.Array[js.Dynamic], value: js.Dynamic): Int = boundary {
    var i = 0
    while (i < arr.length) {
      if (arr(i) == value) break(i)
      i += 1
    }
    0
  }

  // ------ Buffer copy methods ------

  protected def copy(buffer: FloatBuffer): Float32Array = {
    val remaining = buffer.remaining()
    if (remaining > floatBuf.length) floatBuf = new Float32Array(remaining)
    var i = buffer.position()
    var j = 0
    while (i < buffer.limit()) {
      floatBuf(j) = buffer.get(i)
      i += 1
      j += 1
    }
    floatBuf.subarray(0, remaining)
  }

  protected def copy(buffer: IntBuffer): Int32Array = {
    val remaining = buffer.remaining()
    if (remaining > intBuf.length) intBuf = new Int32Array(remaining)
    var i = buffer.position()
    var j = 0
    while (i < buffer.limit()) {
      intBuf(j) = buffer.get(i)
      i += 1
      j += 1
    }
    intBuf.subarray(0, remaining)
  }

  private def copy(buffer: ShortBuffer): Int16Array = {
    val remaining = buffer.remaining()
    if (remaining > shortBuf.length) shortBuf = new Int16Array(remaining)
    var i = buffer.position()
    var j = 0
    while (i < buffer.limit()) {
      shortBuf(j) = (buffer.get(i) & 0xffff).toShort
      i += 1
      j += 1
    }
    shortBuf.subarray(0, remaining)
  }

  protected def copy(buffer: ByteBuffer): Int8Array = {
    val remaining = buffer.remaining()
    if (remaining > byteBuf.length) byteBuf = new Int8Array(remaining)
    var i = buffer.position()
    var j = 0
    while (i < buffer.limit()) {
      byteBuf(j) = buffer.get(i)
      i += 1
      j += 1
    }
    byteBuf.subarray(0, remaining)
  }

  // ------ Uniform location helper ------

  protected def getUniformLocation(location: Int): js.Dynamic =
    uniforms(currProgram)(location)

  // ======================================================================
  // GL20 method implementations (alphabetical order)
  // ======================================================================

  override def glActiveTexture(texture: Int): Unit =
    gl.activeTexture(texture)

  override def glAttachShader(program: Int, shader: Int): Unit =
    gl.attachShader(programs(program), shaders(shader))

  override def glBindAttribLocation(program: Int, index: Int, name: String): Unit =
    gl.bindAttribLocation(programs(program), index, name)

  override def glBindBuffer(target: BufferTarget, buffer: Int): Unit =
    gl.bindBuffer(target.toInt, buffers(buffer))

  override def glBindFramebuffer(target: Int, framebuffer: Int): Unit =
    gl.bindFramebuffer(target, frameBuffers(framebuffer))

  override def glBindRenderbuffer(target: Int, renderbuffer: Int): Unit =
    gl.bindRenderbuffer(target, renderBuffers(renderbuffer))

  override def glBindTexture(target: TextureTarget, texture: Int): Unit =
    gl.bindTexture(target.toInt, textures(texture))

  override def glBlendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit =
    gl.blendColor(red, green, blue, alpha)

  override def glBlendEquation(mode: BlendEquation): Unit =
    gl.blendEquation(mode.toInt)

  override def glBlendEquationSeparate(modeRGB: BlendEquation, modeAlpha: BlendEquation): Unit =
    gl.blendEquationSeparate(modeRGB.toInt, modeAlpha.toInt)

  override def glBlendFunc(sfactor: BlendFactor, dfactor: BlendFactor): Unit =
    gl.blendFunc(sfactor.toInt, dfactor.toInt)

  override def glBlendFuncSeparate(srcRGB: BlendFactor, dstRGB: BlendFactor, srcAlpha: BlendFactor, dstAlpha: BlendFactor): Unit =
    gl.blendFuncSeparate(srcRGB.toInt, dstRGB.toInt, srcAlpha.toInt, dstAlpha.toInt)

  override def glBufferData(target: BufferTarget, size: Int, data: Buffer, usage: BufferUsage): Unit =
    data match {
      case fb: FloatBuffer => gl.bufferData(target.toInt, copy(fb), usage.toInt)
      case sb: ShortBuffer => gl.bufferData(target.toInt, copy(sb), usage.toInt)
      case bb: ByteBuffer  => gl.bufferData(target.toInt, copy(bb), usage.toInt)
      case null => gl.bufferData(target.toInt, size, usage.toInt)
      case _    => throw utils.SgeError.GraphicsError("Can only cope with FloatBuffer, ShortBuffer, and ByteBuffer")
    }

  override def glBufferSubData(target: BufferTarget, offset: Int, size: Int, data: Buffer): Unit =
    data match {
      case fb: FloatBuffer => gl.bufferSubData(target.toInt, offset, copy(fb))
      case sb: ShortBuffer => gl.bufferSubData(target.toInt, offset, copy(sb))
      case bb: ByteBuffer  => gl.bufferSubData(target.toInt, offset, copy(bb))
      case _ => throw utils.SgeError.GraphicsError("Can only cope with FloatBuffer, ShortBuffer, and ByteBuffer")
    }

  override def glCheckFramebufferStatus(target: Int): Int =
    gl.checkFramebufferStatus(target).asInstanceOf[Int]

  override def glClear(mask: ClearMask): Unit =
    gl.clear(mask.toInt)

  override def glClearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit =
    gl.clearColor(red, green, blue, alpha)

  override def glClearDepthf(depth: Float): Unit =
    gl.clearDepth(depth)

  override def glClearStencil(s: Int): Unit =
    gl.clearStencil(s)

  override def glColorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit =
    gl.colorMask(red, green, blue, alpha)

  override def glCompileShader(shader: Int): Unit =
    gl.compileShader(shaders(shader))

  override def glCompressedTexImage2D(
    target:         TextureTarget,
    level:          Int,
    internalformat: Int,
    width:          Pixels,
    height:         Pixels,
    border:         Int,
    imageSize:      Int,
    data:           Buffer
  ): Unit =
    throw utils.SgeError.GraphicsError("compressed textures not supported by WebGL backend")

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
  ): Unit =
    throw utils.SgeError.GraphicsError("compressed textures not supported by WebGL backend")

  override def glCopyTexImage2D(target: TextureTarget, level: Int, internalformat: Int, x: Pixels, y: Pixels, width: Pixels, height: Pixels, border: Int): Unit =
    gl.copyTexImage2D(target.toInt, level, internalformat, x.toInt, y.toInt, width.toInt, height.toInt, border)

  override def glCopyTexSubImage2D(target: TextureTarget, level: Int, xoffset: Pixels, yoffset: Pixels, x: Pixels, y: Pixels, width: Pixels, height: Pixels): Unit =
    gl.copyTexSubImage2D(target.toInt, level, xoffset.toInt, yoffset.toInt, x.toInt, y.toInt, width.toInt, height.toInt)

  override def glCreateProgram(): Int = {
    val program = gl.createProgram()
    addHandle(programs, program)
  }

  override def glCreateShader(`type`: ShaderType): Int = {
    val shader = gl.createShader(`type`.toInt)
    addHandle(shaders, shader)
  }

  override def glCullFace(mode: CullFace): Unit =
    gl.cullFace(mode.toInt)

  override def glDeleteBuffer(buffer: Int): Unit = {
    val buf = removeHandle(buffers, buffer)
    gl.deleteBuffer(buf)
  }

  override def glDeleteBuffers(n: Int, buffers: IntBuffer): Unit = {
    var i = 0
    while (i < n) {
      val id  = buffers.get()
      val buf = removeHandle(this.buffers, id)
      gl.deleteBuffer(buf)
      i += 1
    }
  }

  override def glDeleteFramebuffer(framebuffer: Int): Unit = {
    val fb = removeHandle(frameBuffers, framebuffer)
    gl.deleteFramebuffer(fb)
  }

  override def glDeleteFramebuffers(n: Int, framebuffers: IntBuffer): Unit = {
    var i = 0
    while (i < n) {
      val id = framebuffers.get()
      val fb = removeHandle(frameBuffers, id)
      gl.deleteFramebuffer(fb)
      i += 1
    }
  }

  override def glDeleteProgram(program: Int): Unit = {
    val prog = programs(program)
    removeHandle(programs, program)
    uniforms(program) = null.asInstanceOf[js.Array[js.Dynamic]]
    gl.deleteProgram(prog)
  }

  override def glDeleteRenderbuffer(renderbuffer: Int): Unit = {
    val rb = removeHandle(renderBuffers, renderbuffer)
    gl.deleteRenderbuffer(rb)
  }

  override def glDeleteRenderbuffers(n: Int, renderbuffers: IntBuffer): Unit = {
    var i = 0
    while (i < n) {
      val id = renderbuffers.get()
      val rb = removeHandle(renderBuffers, id)
      gl.deleteRenderbuffer(rb)
      i += 1
    }
  }

  override def glDeleteShader(shader: Int): Unit = {
    val sh = removeHandle(shaders, shader)
    gl.deleteShader(sh)
  }

  override def glDeleteTexture(texture: Int): Unit = {
    val tex = removeHandle(textures, texture)
    gl.deleteTexture(tex)
  }

  override def glDeleteTextures(n: Int, textures: IntBuffer): Unit = {
    var i = 0
    while (i < n) {
      val id  = textures.get()
      val tex = removeHandle(this.textures, id)
      gl.deleteTexture(tex)
      i += 1
    }
  }

  override def glDepthFunc(func: CompareFunc): Unit =
    gl.depthFunc(func.toInt)

  override def glDepthMask(flag: Boolean): Unit =
    gl.depthMask(flag)

  override def glDepthRangef(zNear: Float, zFar: Float): Unit =
    gl.depthRange(zNear, zFar)

  override def glDetachShader(program: Int, shader: Int): Unit =
    gl.detachShader(programs(program), shaders(shader))

  override def glDisable(cap: EnableCap): Unit =
    gl.disable(cap.toInt)

  override def glDisableVertexAttribArray(index: Int): Unit =
    gl.disableVertexAttribArray(index)

  override def glDrawArrays(mode: PrimitiveMode, first: Int, count: Int): Unit =
    gl.drawArrays(mode.toInt, first, count)

  override def glDrawElements(mode: PrimitiveMode, count: Int, `type`: DataType, indices: Buffer): Unit =
    gl.drawElements(mode.toInt, count, `type`.toInt, indices.position()) // WebGL uses buffer offset, not client pointer

  override def glDrawElements(mode: PrimitiveMode, count: Int, `type`: DataType, indices: Int): Unit =
    gl.drawElements(mode.toInt, count, `type`.toInt, indices)

  override def glEnable(cap: EnableCap): Unit =
    gl.enable(cap.toInt)

  override def glEnableVertexAttribArray(index: Int): Unit =
    gl.enableVertexAttribArray(index)

  override def glFinish(): Unit =
    gl.finish()

  override def glFlush(): Unit =
    gl.flush()

  override def glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int): Unit =
    gl.framebufferRenderbuffer(target, attachment, renderbuffertarget, renderBuffers(renderbuffer))

  override def glFramebufferTexture2D(target: Int, attachment: Int, textarget: TextureTarget, texture: Int, level: Int): Unit =
    gl.framebufferTexture2D(target, attachment, textarget.toInt, textures(texture), level)

  override def glFrontFace(mode: Int): Unit =
    gl.frontFace(mode)

  override def glGenBuffer(): Int = {
    val buffer = gl.createBuffer()
    addHandle(buffers, buffer)
  }

  override def glGenBuffers(n: Int, buffers: IntBuffer): Unit = {
    var i = 0
    while (i < n) {
      val buffer = gl.createBuffer()
      val id     = addHandle(this.buffers, buffer)
      buffers.put(id)
      i += 1
    }
    buffers.flip()
  }

  override def glGenerateMipmap(target: TextureTarget): Unit =
    gl.generateMipmap(target.toInt)

  override def glGenFramebuffer(): Int = {
    val fb = gl.createFramebuffer()
    addHandle(frameBuffers, fb)
  }

  override def glGenFramebuffers(n: Int, framebuffers: IntBuffer): Unit = {
    var i = 0
    while (i < n) {
      val fb = gl.createFramebuffer()
      val id = addHandle(frameBuffers, fb)
      framebuffers.put(id)
      i += 1
    }
    framebuffers.flip()
  }

  override def glGenRenderbuffer(): Int = {
    val rb = gl.createRenderbuffer()
    addHandle(renderBuffers, rb)
  }

  override def glGenRenderbuffers(n: Int, renderbuffers: IntBuffer): Unit = {
    var i = 0
    while (i < n) {
      val rb = gl.createRenderbuffer()
      val id = addHandle(renderBuffers, rb)
      renderbuffers.put(id)
      i += 1
    }
    renderbuffers.flip()
  }

  override def glGenTexture(): Int = {
    val texture = gl.createTexture()
    addHandle(textures, texture)
  }

  override def glGenTextures(n: Int, textures: IntBuffer): Unit = {
    var i = 0
    while (i < n) {
      val texture = gl.createTexture()
      val id      = addHandle(this.textures, texture)
      textures.put(id)
      i += 1
    }
    textures.flip()
  }

  override def glGetActiveAttrib(program: Int, index: Int, size: IntBuffer, `type`: IntBuffer): String = {
    val info = gl.getActiveAttrib(programs(program), index)
    size.put(info.size.asInstanceOf[Int])
    size.flip()
    `type`.put(info.selectDynamic("type").asInstanceOf[Int])
    `type`.flip()
    info.name.asInstanceOf[String]
  }

  override def glGetActiveUniform(program: Int, index: Int, size: IntBuffer, `type`: IntBuffer): String = {
    val info = gl.getActiveUniform(programs(program), index)
    size.put(info.size.asInstanceOf[Int])
    size.flip()
    `type`.put(info.selectDynamic("type").asInstanceOf[Int])
    `type`.flip()
    info.name.asInstanceOf[String]
  }

  override def glGetAttachedShaders(program: Int, maxcount: Int, count: Buffer, shaders: IntBuffer): Unit =
    throw utils.SgeError.GraphicsError("glGetAttachedShaders not implemented")

  override def glGetAttribLocation(program: Int, name: String): Int =
    gl.getAttribLocation(programs(program), name).asInstanceOf[Int]

  override def glGetBooleanv(pname: Int, params: Buffer): Unit =
    throw utils.SgeError.GraphicsError("glGetBooleanv not supported by WebGL backend")

  override def glGetBufferParameteriv(target: BufferTarget, pname: Int, params: IntBuffer): Unit =
    throw utils.SgeError.GraphicsError("glGetBufferParameteriv not implemented")

  override def glGetError(): Int =
    gl.getError().asInstanceOf[Int]

  override def glGetFloatv(pname: Int, params: FloatBuffer): Unit =
    if (
      pname == GL20.GL_DEPTH_CLEAR_VALUE || pname == GL20.GL_LINE_WIDTH || pname == GL20.GL_POLYGON_OFFSET_FACTOR
      || pname == GL20.GL_POLYGON_OFFSET_UNITS || pname == GL20.GL_SAMPLE_COVERAGE_VALUE
    ) {
      params.put(0, gl.getParameter(pname).asInstanceOf[Double].toFloat)
      params.flip()
    } else {
      throw utils.SgeError.GraphicsError("glGetFloatv not supported by WebGL backend for this pname")
    }

  override def glGetFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: IntBuffer): Unit =
    pname match {
      case GL20.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE | GL20.GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL | GL20.GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE =>
        params.put(0, gl.getFramebufferAttachmentParameter(target, attachment, pname).asInstanceOf[Int])
        params.flip()
      case GL20.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME =>
        val tex = gl.getFramebufferAttachmentParameter(target, attachment, pname)
        if (tex == null) params.put(0)
        else params.put(findKey(textures, tex))
        params.flip()
      case _ =>
        throw utils.SgeError.GraphicsError("glGetFramebufferAttachmentParameteriv: invalid pname for WebGL backend")
    }

  override def glGetIntegerv(pname: Int, params: IntBuffer): Unit =
    if (
      pname == GL20.GL_ACTIVE_TEXTURE || pname == GL20.GL_ALPHA_BITS || pname == GL20.GL_BLEND_DST_ALPHA
      || pname == GL20.GL_BLEND_DST_RGB || pname == GL20.GL_BLEND_EQUATION_ALPHA || pname == GL20.GL_BLEND_EQUATION_RGB
      || pname == GL20.GL_BLEND_SRC_ALPHA || pname == GL20.GL_BLEND_SRC_RGB || pname == GL20.GL_BLUE_BITS
      || pname == GL20.GL_CULL_FACE_MODE || pname == GL20.GL_DEPTH_BITS || pname == GL20.GL_DEPTH_FUNC
      || pname == GL20.GL_FRONT_FACE || pname == GL20.GL_GENERATE_MIPMAP_HINT || pname == GL20.GL_GREEN_BITS
      || pname == GL20.GL_IMPLEMENTATION_COLOR_READ_FORMAT || pname == GL20.GL_IMPLEMENTATION_COLOR_READ_TYPE
      || pname == GL20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS || pname == GL20.GL_MAX_CUBE_MAP_TEXTURE_SIZE
      || pname == GL20.GL_MAX_FRAGMENT_UNIFORM_VECTORS || pname == GL20.GL_MAX_RENDERBUFFER_SIZE
      || pname == GL20.GL_MAX_TEXTURE_IMAGE_UNITS || pname == GL20.GL_MAX_TEXTURE_SIZE || pname == GL20.GL_MAX_VARYING_VECTORS
      || pname == GL20.GL_MAX_VERTEX_ATTRIBS || pname == GL20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS
      || pname == GL20.GL_MAX_VERTEX_UNIFORM_VECTORS || pname == GL20.GL_NUM_COMPRESSED_TEXTURE_FORMATS
      || pname == GL20.GL_PACK_ALIGNMENT || pname == GL20.GL_RED_BITS || pname == GL20.GL_SAMPLE_BUFFERS
      || pname == GL20.GL_SAMPLES || pname == GL20.GL_STENCIL_BACK_FAIL || pname == GL20.GL_STENCIL_BACK_FUNC
      || pname == GL20.GL_STENCIL_BACK_PASS_DEPTH_FAIL || pname == GL20.GL_STENCIL_BACK_PASS_DEPTH_PASS
      || pname == GL20.GL_STENCIL_BACK_REF || pname == GL20.GL_STENCIL_BACK_VALUE_MASK
      || pname == GL20.GL_STENCIL_BACK_WRITEMASK || pname == GL20.GL_STENCIL_BITS || pname == GL20.GL_STENCIL_CLEAR_VALUE
      || pname == GL20.GL_STENCIL_FAIL || pname == GL20.GL_STENCIL_FUNC || pname == GL20.GL_STENCIL_PASS_DEPTH_FAIL
      || pname == GL20.GL_STENCIL_PASS_DEPTH_PASS || pname == GL20.GL_STENCIL_REF || pname == GL20.GL_STENCIL_VALUE_MASK
      || pname == GL20.GL_STENCIL_WRITEMASK || pname == GL20.GL_SUBPIXEL_BITS || pname == GL20.GL_UNPACK_ALIGNMENT
    ) {
      params.put(0, gl.getParameter(pname).asInstanceOf[Int])
      params.flip()
    } else if (pname == GL20.GL_VIEWPORT || pname == GL20.GL_SCISSOR_BOX) {
      val array = gl.getParameter(pname).asInstanceOf[Int32Array]
      params.put(0, array(0))
      params.put(1, array(1))
      params.put(2, array(2))
      params.put(3, array(3))
      params.flip()
    } else if (pname == GL20.GL_FRAMEBUFFER_BINDING) {
      val fbo = gl.getParameter(pname)
      if (fbo == null) params.put(0)
      else params.put(findKey(frameBuffers, fbo))
      params.flip()
    } else {
      throw utils.SgeError.GraphicsError("glGetIntegerv not supported by WebGL backend for this pname")
    }

  override def glGetProgramInfoLog(program: Int): String =
    gl.getProgramInfoLog(programs(program)).asInstanceOf[String]

  override def glGetProgramiv(program: Int, pname: Int, params: IntBuffer): Unit = {
    if (pname == GL20.GL_DELETE_STATUS || pname == GL20.GL_LINK_STATUS || pname == GL20.GL_VALIDATE_STATUS) {
      val result = gl.getProgramParameter(programs(program), pname).asInstanceOf[Boolean]
      params.put(if (result) GL20.GL_TRUE else GL20.GL_FALSE)
    } else {
      params.put(gl.getProgramParameter(programs(program), pname).asInstanceOf[Int])
    }
    params.flip()
  }

  override def glGetRenderbufferParameteriv(target: Int, pname: Int, params: IntBuffer): Unit =
    throw utils.SgeError.GraphicsError("glGetRenderbufferParameteriv not implemented")

  override def glGetShaderInfoLog(shader: Int): String =
    gl.getShaderInfoLog(shaders(shader)).asInstanceOf[String]

  override def glGetShaderiv(shader: Int, pname: Int, params: IntBuffer): Unit = {
    if (pname == GL20.GL_COMPILE_STATUS || pname == GL20.GL_DELETE_STATUS) {
      val result = gl.getShaderParameter(shaders(shader), pname).asInstanceOf[Boolean]
      params.put(if (result) GL20.GL_TRUE else GL20.GL_FALSE)
    } else {
      params.put(gl.getShaderParameter(shaders(shader), pname).asInstanceOf[Int])
    }
    params.flip()
  }

  override def glGetShaderPrecisionFormat(shadertype: ShaderType, precisiontype: Int, range: IntBuffer, precision: IntBuffer): Unit =
    throw utils.SgeError.GraphicsError("glGetShaderPrecisionFormat not supported by WebGL backend")

  override def glGetString(name: Int): String =
    gl.getParameter(name).asInstanceOf[String]

  override def glGetTexParameterfv(target: TextureTarget, pname: Int, params: FloatBuffer): Unit =
    throw utils.SgeError.GraphicsError("glGetTexParameterfv not supported by WebGL backend")

  override def glGetTexParameteriv(target: TextureTarget, pname: Int, params: IntBuffer): Unit =
    throw utils.SgeError.GraphicsError("glGetTexParameteriv not supported by WebGL backend")

  override def glGetUniformfv(program: Int, location: Int, params: FloatBuffer): Unit =
    throw utils.SgeError.GraphicsError("glGetUniformfv not implemented")

  override def glGetUniformiv(program: Int, location: Int, params: IntBuffer): Unit =
    throw utils.SgeError.GraphicsError("glGetUniformiv not implemented")

  override def glGetUniformLocation(program: Int, name: String): Int = {
    val location = gl.getUniformLocation(programs(program), name)
    if (location == null) -1
    else {
      var progUniforms = uniforms(program)
      if (progUniforms == null) {
        progUniforms = js.Array(js.undefined.asInstanceOf[js.Dynamic])
        uniforms(program) = progUniforms
      }
      addHandle(progUniforms, location)
    }
  }

  override def glGetVertexAttribfv(index: Int, pname: Int, params: FloatBuffer): Unit =
    throw utils.SgeError.GraphicsError("glGetVertexAttribfv not implemented")

  override def glGetVertexAttribiv(index: Int, pname: Int, params: IntBuffer): Unit =
    throw utils.SgeError.GraphicsError("glGetVertexAttribiv not implemented")

  override def glGetVertexAttribPointerv(index: Int, pname: Int, pointer: Buffer): Unit =
    throw utils.SgeError.GraphicsError("glGetVertexAttribPointerv not supported by WebGL backend")

  override def glHint(target: Int, mode: Int): Unit =
    gl.hint(target, mode)

  override def glIsBuffer(buffer: Int): Boolean =
    gl.isBuffer(buffers(buffer)).asInstanceOf[Boolean]

  override def glIsEnabled(cap: EnableCap): Boolean =
    gl.isEnabled(cap.toInt).asInstanceOf[Boolean]

  override def glIsFramebuffer(framebuffer: Int): Boolean =
    gl.isFramebuffer(frameBuffers(framebuffer)).asInstanceOf[Boolean]

  override def glIsProgram(program: Int): Boolean =
    gl.isProgram(programs(program)).asInstanceOf[Boolean]

  override def glIsRenderbuffer(renderbuffer: Int): Boolean =
    gl.isRenderbuffer(renderBuffers(renderbuffer)).asInstanceOf[Boolean]

  override def glIsShader(shader: Int): Boolean =
    gl.isShader(shaders(shader)).asInstanceOf[Boolean]

  override def glIsTexture(texture: Int): Boolean =
    gl.isTexture(textures(texture)).asInstanceOf[Boolean]

  override def glLineWidth(width: Float): Unit =
    gl.lineWidth(width)

  override def glLinkProgram(program: Int): Unit =
    gl.linkProgram(programs(program))

  override def glPixelStorei(pname: Int, param: Int): Unit =
    gl.pixelStorei(pname, param)

  override def glPolygonOffset(factor: Float, units: Float): Unit =
    gl.polygonOffset(factor, units)

  override def glReadPixels(x: Pixels, y: Pixels, width: Pixels, height: Pixels, format: PixelFormat, `type`: DataType, pixels: Buffer): Unit = {
    if (format.toInt != GL20.GL_RGBA || `type`.toInt != GL20.GL_UNSIGNED_BYTE) {
      throw utils.SgeError.GraphicsError(
        "Only format RGBA and type UNSIGNED_BYTE are currently supported for glReadPixels"
      )
    }
    pixels match {
      case bb: ByteBuffer =>
        val size   = 4 * width.toInt * height.toInt
        val buffer = new Uint8Array(size)
        gl.readPixels(x.toInt, y.toInt, width.toInt, height.toInt, format.toInt, `type`.toInt, buffer)
        var i = 0
        while (i < size) {
          bb.put(buffer(i).toByte)
          i += 1
        }
        bb.flip()
      case _ =>
        throw utils.SgeError.GraphicsError("Pixels buffer must be a ByteBuffer for glReadPixels")
    }
  }

  override def glReleaseShaderCompiler(): Unit =
    throw utils.SgeError.GraphicsError("glReleaseShaderCompiler not implemented")

  override def glRenderbufferStorage(target: Int, internalformat: Int, width: Pixels, height: Pixels): Unit =
    gl.renderbufferStorage(target, internalformat, width.toInt, height.toInt)

  override def glSampleCoverage(value: Float, invert: Boolean): Unit =
    gl.sampleCoverage(value, invert)

  override def glScissor(x: Pixels, y: Pixels, width: Pixels, height: Pixels): Unit =
    gl.scissor(x.toInt, y.toInt, width.toInt, height.toInt)

  override def glShaderBinary(n: Int, shaders: IntBuffer, binaryformat: Int, binary: Buffer, length: Int): Unit =
    throw utils.SgeError.GraphicsError("glShaderBinary not supported by WebGL backend")

  override def glShaderSource(shader: Int, string: String): Unit =
    gl.shaderSource(shaders(shader), string)

  override def glStencilFunc(func: CompareFunc, ref: Int, mask: Int): Unit =
    gl.stencilFunc(func.toInt, ref, mask)

  override def glStencilFuncSeparate(face: CullFace, func: CompareFunc, ref: Int, mask: Int): Unit =
    gl.stencilFuncSeparate(face.toInt, func.toInt, ref, mask)

  override def glStencilMask(mask: Int): Unit =
    gl.stencilMask(mask)

  override def glStencilMaskSeparate(face: CullFace, mask: Int): Unit =
    gl.stencilMaskSeparate(face.toInt, mask)

  override def glStencilOp(fail: StencilOp, zfail: StencilOp, zpass: StencilOp): Unit =
    gl.stencilOp(fail.toInt, zfail.toInt, zpass.toInt)

  override def glStencilOpSeparate(face: CullFace, fail: StencilOp, zfail: StencilOp, zpass: StencilOp): Unit =
    gl.stencilOpSeparate(face.toInt, fail.toInt, zfail.toInt, zpass.toInt)

  override def glTexImage2D(
    target:         TextureTarget,
    level:          Int,
    internalformat: Int,
    width:          Pixels,
    height:         Pixels,
    border:         Int,
    format:         PixelFormat,
    `type`:         DataType,
    pixels:         Buffer
  ): Unit = {
    val t = target.toInt; val w = width.toInt; val h = height.toInt; val f = format.toInt; val dt = `type`.toInt
    if (pixels == null) {
      gl.texImage2D(t, level, internalformat, w, h, border, f, dt, null)
    } else {
      pixels match {
        case fb: FloatBuffer =>
          gl.texImage2D(t, level, internalformat, w, h, border, f, dt, copy(fb))
        case ib: IntBuffer =>
          // Reinterpret int data as byte array for pixel upload
          val int32 = copy(ib)
          val uint8 = new Uint8Array(int32.buffer, int32.byteOffset, int32.byteLength)
          gl.texImage2D(t, level, internalformat, w, h, border, f, dt, uint8)
        case bb: ByteBuffer =>
          val int8  = copy(bb)
          val uint8 = new Uint8Array(int8.buffer, int8.byteOffset, int8.byteLength)
          gl.texImage2D(t, level, internalformat, w, h, border, f, dt, uint8)
        case _ =>
          throw utils.SgeError.GraphicsError("Unsupported Buffer type for glTexImage2D")
      }
    }
  }

  override def glTexParameterf(target: TextureTarget, pname: Int, param: Float): Unit =
    gl.texParameterf(target.toInt, pname, param)

  override def glTexParameterfv(target: TextureTarget, pname: Int, params: FloatBuffer): Unit =
    gl.texParameterf(target.toInt, pname, params.get())

  override def glTexParameteri(target: TextureTarget, pname: Int, param: Int): Unit =
    gl.texParameterf(target.toInt, pname, param)

  override def glTexParameteriv(target: TextureTarget, pname: Int, params: IntBuffer): Unit =
    gl.texParameterf(target.toInt, pname, params.get())

  override def glTexSubImage2D(
    target:  TextureTarget,
    level:   Int,
    xoffset: Pixels,
    yoffset: Pixels,
    width:   Pixels,
    height:  Pixels,
    format:  PixelFormat,
    `type`:  DataType,
    pixels:  Buffer
  ): Unit = {
    val t = target.toInt; val xo = xoffset.toInt; val yo = yoffset.toInt
    val w = width.toInt; val h   = height.toInt; val f   = format.toInt; val dt = `type`.toInt
    pixels match {
      case fb: FloatBuffer =>
        gl.texSubImage2D(t, level, xo, yo, w, h, f, dt, copy(fb))
      case ib: IntBuffer =>
        val int32 = copy(ib)
        val uint8 = new Uint8Array(int32.buffer, int32.byteOffset, int32.byteLength)
        gl.texSubImage2D(t, level, xo, yo, w, h, f, dt, uint8)
      case bb: ByteBuffer =>
        val int8  = copy(bb)
        val uint8 = new Uint8Array(int8.buffer, int8.byteOffset, int8.byteLength)
        gl.texSubImage2D(t, level, xo, yo, w, h, f, dt, uint8)
      case _ =>
        throw utils.SgeError.GraphicsError("Unsupported Buffer type for glTexSubImage2D")
    }
  }

  override def glUniform1f(location: Int, x: Float): Unit =
    gl.uniform1f(getUniformLocation(location), x)

  override def glUniform1fv(location: Int, count: Int, v: FloatBuffer): Unit =
    gl.uniform1fv(getUniformLocation(location), copy(v))

  override def glUniform1fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit =
    gl.uniform1fv(getUniformLocation(location), v.asInstanceOf[js.Any])

  override def glUniform1i(location: Int, x: Int): Unit =
    gl.uniform1i(getUniformLocation(location), x)

  override def glUniform1iv(location: Int, count: Int, v: IntBuffer): Unit =
    gl.uniform1iv(getUniformLocation(location), copy(v))

  override def glUniform1iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit =
    gl.uniform1iv(getUniformLocation(location), v.asInstanceOf[js.Any])

  override def glUniform2f(location: Int, x: Float, y: Float): Unit =
    gl.uniform2f(getUniformLocation(location), x, y)

  override def glUniform2fv(location: Int, count: Int, v: FloatBuffer): Unit =
    gl.uniform2fv(getUniformLocation(location), copy(v))

  override def glUniform2fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit =
    gl.uniform2fv(getUniformLocation(location), v.asInstanceOf[js.Any])

  override def glUniform2i(location: Int, x: Int, y: Int): Unit =
    gl.uniform2i(getUniformLocation(location), x, y)

  override def glUniform2iv(location: Int, count: Int, v: IntBuffer): Unit =
    gl.uniform2iv(getUniformLocation(location), copy(v))

  override def glUniform2iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit =
    gl.uniform2iv(getUniformLocation(location), v.asInstanceOf[js.Any])

  override def glUniform3f(location: Int, x: Float, y: Float, z: Float): Unit =
    gl.uniform3f(getUniformLocation(location), x, y, z)

  override def glUniform3fv(location: Int, count: Int, v: FloatBuffer): Unit =
    gl.uniform3fv(getUniformLocation(location), copy(v))

  override def glUniform3fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit =
    gl.uniform3fv(getUniformLocation(location), v.asInstanceOf[js.Any])

  override def glUniform3i(location: Int, x: Int, y: Int, z: Int): Unit =
    gl.uniform3i(getUniformLocation(location), x, y, z)

  override def glUniform3iv(location: Int, count: Int, v: IntBuffer): Unit =
    gl.uniform3iv(getUniformLocation(location), copy(v))

  override def glUniform3iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit =
    gl.uniform3iv(getUniformLocation(location), v.asInstanceOf[js.Any])

  override def glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float): Unit =
    gl.uniform4f(getUniformLocation(location), x, y, z, w)

  override def glUniform4fv(location: Int, count: Int, v: FloatBuffer): Unit =
    gl.uniform4fv(getUniformLocation(location), copy(v))

  override def glUniform4fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit =
    gl.uniform4fv(getUniformLocation(location), v.asInstanceOf[js.Any])

  override def glUniform4i(location: Int, x: Int, y: Int, z: Int, w: Int): Unit =
    gl.uniform4i(getUniformLocation(location), x, y, z, w)

  override def glUniform4iv(location: Int, count: Int, v: IntBuffer): Unit =
    gl.uniform4iv(getUniformLocation(location), copy(v))

  override def glUniform4iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit =
    gl.uniform4iv(getUniformLocation(location), v.asInstanceOf[js.Any])

  override def glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    gl.uniformMatrix2fv(getUniformLocation(location), transpose, copy(value))

  override def glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit =
    gl.uniformMatrix2fv(getUniformLocation(location), transpose, value.asInstanceOf[js.Any])

  override def glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    gl.uniformMatrix3fv(getUniformLocation(location), transpose, copy(value))

  override def glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit =
    gl.uniformMatrix3fv(getUniformLocation(location), transpose, value.asInstanceOf[js.Any])

  override def glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    gl.uniformMatrix4fv(getUniformLocation(location), transpose, copy(value))

  override def glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit =
    gl.uniformMatrix4fv(getUniformLocation(location), transpose, value.asInstanceOf[js.Any])

  override def glUseProgram(program: Int): Unit = {
    currProgram = program
    gl.useProgram(programs(program))
  }

  override def glValidateProgram(program: Int): Unit =
    gl.validateProgram(programs(program))

  override def glVertexAttrib1f(indx: Int, x: Float): Unit =
    gl.vertexAttrib1f(indx, x)

  override def glVertexAttrib1fv(indx: Int, values: FloatBuffer): Unit =
    gl.vertexAttrib1fv(indx, copy(values))

  override def glVertexAttrib2f(indx: Int, x: Float, y: Float): Unit =
    gl.vertexAttrib2f(indx, x, y)

  override def glVertexAttrib2fv(indx: Int, values: FloatBuffer): Unit =
    gl.vertexAttrib2fv(indx, copy(values))

  override def glVertexAttrib3f(indx: Int, x: Float, y: Float, z: Float): Unit =
    gl.vertexAttrib3f(indx, x, y, z)

  override def glVertexAttrib3fv(indx: Int, values: FloatBuffer): Unit =
    gl.vertexAttrib3fv(indx, copy(values))

  override def glVertexAttrib4f(indx: Int, x: Float, y: Float, z: Float, w: Float): Unit =
    gl.vertexAttrib4f(indx, x, y, z, w)

  override def glVertexAttrib4fv(indx: Int, values: FloatBuffer): Unit =
    gl.vertexAttrib4fv(indx, copy(values))

  override def glVertexAttribPointer(indx: Int, size: Int, `type`: DataType, normalized: Boolean, stride: Int, ptr: Buffer): Unit =
    throw utils.SgeError.GraphicsError("Vertex arrays from client memory not supported in WebGL")

  override def glVertexAttribPointer(indx: Int, size: Int, `type`: DataType, normalized: Boolean, stride: Int, ptr: Int): Unit =
    gl.vertexAttribPointer(indx, size, `type`.toInt, normalized, stride, ptr)

  override def glViewport(x: Pixels, y: Pixels, width: Pixels, height: Pixels): Unit =
    gl.viewport(x.toInt, y.toInt, width.toInt, height.toInt)
}
