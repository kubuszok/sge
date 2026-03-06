/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (replaces Lwjgl3GL20.java)
 *   Convention: JVM-only; Panama FFM downcall handles to ANGLE libGLESv2
 *   Convention: Buffer → MemorySegment.ofBuffer; Array → arena-allocated segment
 *   Convention: GLboolean → JAVA_BYTE (0/1 conversion)
 *   Idiom: split packages; no return; SgeError.GraphicsError
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import java.lang.foreign.*
import java.lang.foreign.ValueLayout.*
import java.lang.invoke.MethodHandle
import java.nio.{ Buffer, ByteBuffer, ByteOrder, FloatBuffer, IntBuffer }

/** OpenGL ES 2.0 implementation via ANGLE (libGLESv2) using Panama FFM downcall handles.
  *
  * Each GL method is backed by a lazily-initialized `MethodHandle` that calls the corresponding C function from ANGLE. Buffer parameters are passed as native pointers via `MemorySegment.ofBuffer`;
  * array parameters are copied to arena-allocated segments.
  *
  * @param lookup
  *   a `SymbolLookup` for the ANGLE libGLESv2 shared library
  */
class AngleGL20(lookup: SymbolLookup) extends GL20 {

  // ─── Shorthand layout aliases ─────────────────────────────────────────────

  private val I: ValueLayout.OfInt   = JAVA_INT
  private val F: ValueLayout.OfFloat = JAVA_FLOAT
  private val B: ValueLayout.OfByte  = JAVA_BYTE // GLboolean = unsigned char
  private val P: AddressLayout       = ADDRESS

  // ─── Helpers ──────────────────────────────────────────────────────────────

  private val linker: Linker = Linker.nativeLinker()

  private def h(name: String, desc: FunctionDescriptor): MethodHandle =
    linker.downcallHandle(
      lookup.find(name).orElseThrow(() => new UnsatisfiedLinkError(s"GL symbol not found: $name")),
      desc
    )

  private def glBool(v: Boolean): Byte = if (v) 1.toByte else 0.toByte

  /** Convert a MethodHandle result (GLboolean as Byte) to Boolean. */
  private def fromGlBool(result: AnyRef): Boolean = result.asInstanceOf[Byte] != 0

  @SuppressWarnings(Array("all"))
  private def bufAddr(buf: Buffer): MemorySegment =
    if (buf == null) MemorySegment.NULL // null is valid for some GL calls (e.g. glTexImage2D)
    else MemorySegment.ofBuffer(buf)

  /** Copy `count` floats from `v` starting at `offset` into an arena-allocated segment. */
  private def floatSeg(arena: Arena, v: Array[Float], offset: Int, count: Int): MemorySegment = {
    val seg = arena.allocate(F, count.toLong)
    MemorySegment.copy(v, offset, seg, F, 0L, count)
    seg
  }

  /** Copy `count` ints from `v` starting at `offset` into an arena-allocated segment. */
  private def intSeg(arena: Arena, v: Array[Int], offset: Int, count: Int): MemorySegment = {
    val seg = arena.allocate(I, count.toLong)
    MemorySegment.copy(v, offset, seg, I, 0L, count)
    seg
  }

  // ─── Texture state ────────────────────────────────────────────────────────

  private lazy val _glActiveTexture = h("glActiveTexture", FunctionDescriptor.ofVoid(I))
  override def glActiveTexture(texture: Int): Unit = _glActiveTexture.invoke(texture)

  private lazy val _glBindTexture = h("glBindTexture", FunctionDescriptor.ofVoid(I, I))
  override def glBindTexture(target: Int, texture: Int): Unit = _glBindTexture.invoke(target, texture)

  private lazy val _glGenTextures = h("glGenTextures", FunctionDescriptor.ofVoid(I, P))
  override def glGenTextures(n: Int, textures: IntBuffer): Unit = _glGenTextures.invoke(n, bufAddr(textures))

  override def glGenTexture(): Int = {
    val direct = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer()
    glGenTextures(1, direct)
    direct.get(0)
  }

  private lazy val _glDeleteTextures = h("glDeleteTextures", FunctionDescriptor.ofVoid(I, P))
  override def glDeleteTextures(n: Int, textures: IntBuffer): Unit = _glDeleteTextures.invoke(n, bufAddr(textures))

  override def glDeleteTexture(texture: Int): Unit = {
    val buf = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer()
    buf.put(0, texture)
    glDeleteTextures(1, buf)
  }

  private lazy val _glIsTexture = h("glIsTexture", FunctionDescriptor.of(B, I))
  override def glIsTexture(texture: Int): Boolean = fromGlBool(_glIsTexture.invoke(texture))

  private lazy val _glTexImage2D = h("glTexImage2D", FunctionDescriptor.ofVoid(I, I, I, I, I, I, I, I, P))
  override def glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, `type`: Int, pixels: Buffer): Unit =
    _glTexImage2D.invoke(target, level, internalformat, width, height, border, format, `type`, bufAddr(pixels))

  private lazy val _glTexSubImage2D = h("glTexSubImage2D", FunctionDescriptor.ofVoid(I, I, I, I, I, I, I, I, P))
  override def glTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: Buffer): Unit =
    _glTexSubImage2D.invoke(target, level, xoffset, yoffset, width, height, format, `type`, bufAddr(pixels))

  private lazy val _glCopyTexImage2D = h("glCopyTexImage2D", FunctionDescriptor.ofVoid(I, I, I, I, I, I, I, I))
  override def glCopyTexImage2D(target: Int, level: Int, internalformat: Int, x: Int, y: Int, width: Int, height: Int, border: Int): Unit =
    _glCopyTexImage2D.invoke(target, level, internalformat, x, y, width, height, border)

  private lazy val _glCopyTexSubImage2D = h("glCopyTexSubImage2D", FunctionDescriptor.ofVoid(I, I, I, I, I, I, I, I))
  override def glCopyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit =
    _glCopyTexSubImage2D.invoke(target, level, xoffset, yoffset, x, y, width, height)

  private lazy val _glCompressedTexImage2D = h("glCompressedTexImage2D", FunctionDescriptor.ofVoid(I, I, I, I, I, I, I, P))
  override def glCompressedTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, imageSize: Int, data: Buffer): Unit =
    _glCompressedTexImage2D.invoke(target, level, internalformat, width, height, border, imageSize, bufAddr(data))

  private lazy val _glCompressedTexSubImage2D = h("glCompressedTexSubImage2D", FunctionDescriptor.ofVoid(I, I, I, I, I, I, I, I, P))
  override def glCompressedTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, imageSize: Int, data: Buffer): Unit =
    _glCompressedTexSubImage2D.invoke(target, level, xoffset, yoffset, width, height, format, imageSize, bufAddr(data))

  private lazy val _glTexParameterf = h("glTexParameterf", FunctionDescriptor.ofVoid(I, I, F))
  override def glTexParameterf(target: Int, pname: Int, param: Float): Unit = _glTexParameterf.invoke(target, pname, param)

  private lazy val _glTexParameteri = h("glTexParameteri", FunctionDescriptor.ofVoid(I, I, I))
  override def glTexParameteri(target: Int, pname: Int, param: Int): Unit = _glTexParameteri.invoke(target, pname, param)

  private lazy val _glTexParameterfv = h("glTexParameterfv", FunctionDescriptor.ofVoid(I, I, P))
  override def glTexParameterfv(target: Int, pname: Int, params: FloatBuffer): Unit = _glTexParameterfv.invoke(target, pname, bufAddr(params))

  private lazy val _glTexParameteriv = h("glTexParameteriv", FunctionDescriptor.ofVoid(I, I, P))
  override def glTexParameteriv(target: Int, pname: Int, params: IntBuffer): Unit = _glTexParameteriv.invoke(target, pname, bufAddr(params))

  private lazy val _glGetTexParameterfv = h("glGetTexParameterfv", FunctionDescriptor.ofVoid(I, I, P))
  override def glGetTexParameterfv(target: Int, pname: Int, params: FloatBuffer): Unit = _glGetTexParameterfv.invoke(target, pname, bufAddr(params))

  private lazy val _glGetTexParameteriv = h("glGetTexParameteriv", FunctionDescriptor.ofVoid(I, I, P))
  override def glGetTexParameteriv(target: Int, pname: Int, params: IntBuffer): Unit = _glGetTexParameteriv.invoke(target, pname, bufAddr(params))

  private lazy val _glGenerateMipmap = h("glGenerateMipmap", FunctionDescriptor.ofVoid(I))
  override def glGenerateMipmap(target: Int): Unit = _glGenerateMipmap.invoke(target)

  // ─── Blend / Color / Depth / Stencil state ────────────────────────────────

  private lazy val _glBlendFunc = h("glBlendFunc", FunctionDescriptor.ofVoid(I, I))
  override def glBlendFunc(sfactor: Int, dfactor: Int): Unit = _glBlendFunc.invoke(sfactor, dfactor)

  private lazy val _glBlendFuncSeparate = h("glBlendFuncSeparate", FunctionDescriptor.ofVoid(I, I, I, I))
  override def glBlendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int): Unit =
    _glBlendFuncSeparate.invoke(srcRGB, dstRGB, srcAlpha, dstAlpha)

  private lazy val _glBlendColor = h("glBlendColor", FunctionDescriptor.ofVoid(F, F, F, F))
  override def glBlendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit =
    _glBlendColor.invoke(red, green, blue, alpha)

  private lazy val _glBlendEquation = h("glBlendEquation", FunctionDescriptor.ofVoid(I))
  override def glBlendEquation(mode: Int): Unit = _glBlendEquation.invoke(mode)

  private lazy val _glBlendEquationSeparate = h("glBlendEquationSeparate", FunctionDescriptor.ofVoid(I, I))
  override def glBlendEquationSeparate(modeRGB: Int, modeAlpha: Int): Unit =
    _glBlendEquationSeparate.invoke(modeRGB, modeAlpha)

  private lazy val _glClear = h("glClear", FunctionDescriptor.ofVoid(I))
  override def glClear(mask: Int): Unit = _glClear.invoke(mask)

  private lazy val _glClearColor = h("glClearColor", FunctionDescriptor.ofVoid(F, F, F, F))
  override def glClearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit =
    _glClearColor.invoke(red, green, blue, alpha)

  private lazy val _glClearDepthf = h("glClearDepthf", FunctionDescriptor.ofVoid(F))
  override def glClearDepthf(depth: Float): Unit = _glClearDepthf.invoke(depth)

  private lazy val _glClearStencil = h("glClearStencil", FunctionDescriptor.ofVoid(I))
  override def glClearStencil(s: Int): Unit = _glClearStencil.invoke(s)

  private lazy val _glColorMask = h("glColorMask", FunctionDescriptor.ofVoid(B, B, B, B))
  override def glColorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit =
    _glColorMask.invoke(glBool(red), glBool(green), glBool(blue), glBool(alpha))

  private lazy val _glDepthFunc = h("glDepthFunc", FunctionDescriptor.ofVoid(I))
  override def glDepthFunc(func: Int): Unit = _glDepthFunc.invoke(func)

  private lazy val _glDepthMask = h("glDepthMask", FunctionDescriptor.ofVoid(B))
  override def glDepthMask(flag: Boolean): Unit = _glDepthMask.invoke(glBool(flag))

  private lazy val _glDepthRangef = h("glDepthRangef", FunctionDescriptor.ofVoid(F, F))
  override def glDepthRangef(zNear: Float, zFar: Float): Unit = _glDepthRangef.invoke(zNear, zFar)

  private lazy val _glStencilFunc = h("glStencilFunc", FunctionDescriptor.ofVoid(I, I, I))
  override def glStencilFunc(func: Int, ref: Int, mask: Int): Unit = _glStencilFunc.invoke(func, ref, mask)

  private lazy val _glStencilFuncSeparate = h("glStencilFuncSeparate", FunctionDescriptor.ofVoid(I, I, I, I))
  override def glStencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int): Unit =
    _glStencilFuncSeparate.invoke(face, func, ref, mask)

  private lazy val _glStencilMask = h("glStencilMask", FunctionDescriptor.ofVoid(I))
  override def glStencilMask(mask: Int): Unit = _glStencilMask.invoke(mask)

  private lazy val _glStencilMaskSeparate = h("glStencilMaskSeparate", FunctionDescriptor.ofVoid(I, I))
  override def glStencilMaskSeparate(face: Int, mask: Int): Unit = _glStencilMaskSeparate.invoke(face, mask)

  private lazy val _glStencilOp = h("glStencilOp", FunctionDescriptor.ofVoid(I, I, I))
  override def glStencilOp(fail: Int, zfail: Int, zpass: Int): Unit = _glStencilOp.invoke(fail, zfail, zpass)

  private lazy val _glStencilOpSeparate = h("glStencilOpSeparate", FunctionDescriptor.ofVoid(I, I, I, I))
  override def glStencilOpSeparate(face: Int, fail: Int, zfail: Int, zpass: Int): Unit =
    _glStencilOpSeparate.invoke(face, fail, zfail, zpass)

  // ─── Enable / Disable / Viewport / Scissor ────────────────────────────────

  private lazy val _glEnable = h("glEnable", FunctionDescriptor.ofVoid(I))
  override def glEnable(cap: Int): Unit = _glEnable.invoke(cap)

  private lazy val _glDisable = h("glDisable", FunctionDescriptor.ofVoid(I))
  override def glDisable(cap: Int): Unit = _glDisable.invoke(cap)

  private lazy val _glIsEnabled = h("glIsEnabled", FunctionDescriptor.of(B, I))
  override def glIsEnabled(cap: Int): Boolean = fromGlBool(_glIsEnabled.invoke(cap))

  private lazy val _glViewport = h("glViewport", FunctionDescriptor.ofVoid(I, I, I, I))
  override def glViewport(x: Int, y: Int, width: Int, height: Int): Unit = _glViewport.invoke(x, y, width, height)

  private lazy val _glScissor = h("glScissor", FunctionDescriptor.ofVoid(I, I, I, I))
  override def glScissor(x: Int, y: Int, width: Int, height: Int): Unit = _glScissor.invoke(x, y, width, height)

  private lazy val _glCullFace = h("glCullFace", FunctionDescriptor.ofVoid(I))
  override def glCullFace(mode: Int): Unit = _glCullFace.invoke(mode)

  private lazy val _glFrontFace = h("glFrontFace", FunctionDescriptor.ofVoid(I))
  override def glFrontFace(mode: Int): Unit = _glFrontFace.invoke(mode)

  private lazy val _glLineWidth = h("glLineWidth", FunctionDescriptor.ofVoid(F))
  override def glLineWidth(width: Float): Unit = _glLineWidth.invoke(width)

  private lazy val _glPolygonOffset = h("glPolygonOffset", FunctionDescriptor.ofVoid(F, F))
  override def glPolygonOffset(factor: Float, units: Float): Unit = _glPolygonOffset.invoke(factor, units)

  private lazy val _glPixelStorei = h("glPixelStorei", FunctionDescriptor.ofVoid(I, I))
  override def glPixelStorei(pname: Int, param: Int): Unit = _glPixelStorei.invoke(pname, param)

  private lazy val _glHint = h("glHint", FunctionDescriptor.ofVoid(I, I))
  override def glHint(target: Int, mode: Int): Unit = _glHint.invoke(target, mode)

  private lazy val _glSampleCoverage = h("glSampleCoverage", FunctionDescriptor.ofVoid(F, B))
  override def glSampleCoverage(value: Float, invert: Boolean): Unit = _glSampleCoverage.invoke(value, glBool(invert))

  private lazy val _glFinish = h("glFinish", FunctionDescriptor.ofVoid())
  override def glFinish(): Unit = _glFinish.invoke()

  private lazy val _glFlush = h("glFlush", FunctionDescriptor.ofVoid())
  override def glFlush(): Unit = _glFlush.invoke()

  // ─── Get state ────────────────────────────────────────────────────────────

  private lazy val _glGetError = h("glGetError", FunctionDescriptor.of(I))
  override def glGetError(): Int = _glGetError.invoke().asInstanceOf[Int]

  private lazy val _glGetIntegerv = h("glGetIntegerv", FunctionDescriptor.ofVoid(I, P))
  override def glGetIntegerv(pname: Int, params: IntBuffer): Unit = _glGetIntegerv.invoke(pname, bufAddr(params))

  private lazy val _glGetFloatv = h("glGetFloatv", FunctionDescriptor.ofVoid(I, P))
  override def glGetFloatv(pname: Int, params: FloatBuffer): Unit = _glGetFloatv.invoke(pname, bufAddr(params))

  private lazy val _glGetBooleanv = h("glGetBooleanv", FunctionDescriptor.ofVoid(I, P))
  override def glGetBooleanv(pname: Int, params: Buffer): Unit = _glGetBooleanv.invoke(pname, bufAddr(params))

  private lazy val _glGetString = h("glGetString", FunctionDescriptor.of(P, I))
  override def glGetString(name: Int): String = {
    val ptr = _glGetString.invoke(name).asInstanceOf[MemorySegment]
    if (ptr == MemorySegment.NULL) null // @nowarn - GL interop boundary
    else ptr.reinterpret(Long.MaxValue).getString(0)
  }

  // ─── Draw ─────────────────────────────────────────────────────────────────

  private lazy val _glDrawArrays = h("glDrawArrays", FunctionDescriptor.ofVoid(I, I, I))
  override def glDrawArrays(mode: Int, first: Int, count: Int): Unit = _glDrawArrays.invoke(mode, first, count)

  private lazy val _glDrawElements = h("glDrawElements", FunctionDescriptor.ofVoid(I, I, I, P))

  override def glDrawElements(mode: Int, count: Int, `type`: Int, indices: Buffer): Unit =
    _glDrawElements.invoke(mode, count, `type`, bufAddr(indices))

  override def glDrawElements(mode: Int, count: Int, `type`: Int, indices: Int): Unit =
    _glDrawElements.invoke(mode, count, `type`, MemorySegment.ofAddress(indices.toLong))

  private lazy val _glReadPixels = h("glReadPixels", FunctionDescriptor.ofVoid(I, I, I, I, I, I, P))
  override def glReadPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, pixels: Buffer): Unit =
    _glReadPixels.invoke(x, y, width, height, format, `type`, bufAddr(pixels))

  // ─── Buffer objects ───────────────────────────────────────────────────────

  private lazy val _glGenBuffers = h("glGenBuffers", FunctionDescriptor.ofVoid(I, P))
  override def glGenBuffers(n: Int, buffers: IntBuffer): Unit = _glGenBuffers.invoke(n, bufAddr(buffers))

  override def glGenBuffer(): Int = {
    val buf = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer()
    glGenBuffers(1, buf)
    buf.get(0)
  }

  private lazy val _glDeleteBuffers = h("glDeleteBuffers", FunctionDescriptor.ofVoid(I, P))
  override def glDeleteBuffers(n: Int, buffers: IntBuffer): Unit = _glDeleteBuffers.invoke(n, bufAddr(buffers))

  override def glDeleteBuffer(buffer: Int): Unit = {
    val buf = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer()
    buf.put(0, buffer)
    glDeleteBuffers(1, buf)
  }

  private lazy val _glBindBuffer = h("glBindBuffer", FunctionDescriptor.ofVoid(I, I))
  override def glBindBuffer(target: Int, buffer: Int): Unit = _glBindBuffer.invoke(target, buffer)

  private lazy val _glBufferData = h("glBufferData", FunctionDescriptor.ofVoid(I, I, P, I))
  override def glBufferData(target: Int, size: Int, data: Buffer, usage: Int): Unit =
    _glBufferData.invoke(target, size, bufAddr(data), usage)

  private lazy val _glBufferSubData = h("glBufferSubData", FunctionDescriptor.ofVoid(I, I, I, P))
  override def glBufferSubData(target: Int, offset: Int, size: Int, data: Buffer): Unit =
    _glBufferSubData.invoke(target, offset, size, bufAddr(data))

  private lazy val _glIsBuffer = h("glIsBuffer", FunctionDescriptor.of(B, I))
  override def glIsBuffer(buffer: Int): Boolean = fromGlBool(_glIsBuffer.invoke(buffer))

  private lazy val _glGetBufferParameteriv = h("glGetBufferParameteriv", FunctionDescriptor.ofVoid(I, I, P))
  override def glGetBufferParameteriv(target: Int, pname: Int, params: IntBuffer): Unit =
    _glGetBufferParameteriv.invoke(target, pname, bufAddr(params))

  // ─── Framebuffer objects ──────────────────────────────────────────────────

  private lazy val _glGenFramebuffers = h("glGenFramebuffers", FunctionDescriptor.ofVoid(I, P))
  override def glGenFramebuffers(n: Int, framebuffers: IntBuffer): Unit = _glGenFramebuffers.invoke(n, bufAddr(framebuffers))

  override def glGenFramebuffer(): Int = {
    val buf = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer()
    glGenFramebuffers(1, buf)
    buf.get(0)
  }

  private lazy val _glDeleteFramebuffers = h("glDeleteFramebuffers", FunctionDescriptor.ofVoid(I, P))
  override def glDeleteFramebuffers(n: Int, framebuffers: IntBuffer): Unit = _glDeleteFramebuffers.invoke(n, bufAddr(framebuffers))

  override def glDeleteFramebuffer(framebuffer: Int): Unit = {
    val buf = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer()
    buf.put(0, framebuffer)
    glDeleteFramebuffers(1, buf)
  }

  private lazy val _glBindFramebuffer = h("glBindFramebuffer", FunctionDescriptor.ofVoid(I, I))
  override def glBindFramebuffer(target: Int, framebuffer: Int): Unit = _glBindFramebuffer.invoke(target, framebuffer)

  private lazy val _glCheckFramebufferStatus = h("glCheckFramebufferStatus", FunctionDescriptor.of(I, I))
  override def glCheckFramebufferStatus(target: Int): Int = _glCheckFramebufferStatus.invoke(target).asInstanceOf[Int]

  private lazy val _glFramebufferTexture2D = h("glFramebufferTexture2D", FunctionDescriptor.ofVoid(I, I, I, I, I))
  override def glFramebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int): Unit =
    _glFramebufferTexture2D.invoke(target, attachment, textarget, texture, level)

  private lazy val _glFramebufferRenderbuffer = h("glFramebufferRenderbuffer", FunctionDescriptor.ofVoid(I, I, I, I))
  override def glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int): Unit =
    _glFramebufferRenderbuffer.invoke(target, attachment, renderbuffertarget, renderbuffer)

  private lazy val _glGetFramebufferAttachmentParameteriv = h("glGetFramebufferAttachmentParameteriv", FunctionDescriptor.ofVoid(I, I, I, P))
  override def glGetFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: IntBuffer): Unit =
    _glGetFramebufferAttachmentParameteriv.invoke(target, attachment, pname, bufAddr(params))

  private lazy val _glIsFramebuffer = h("glIsFramebuffer", FunctionDescriptor.of(B, I))
  override def glIsFramebuffer(framebuffer: Int): Boolean = fromGlBool(_glIsFramebuffer.invoke(framebuffer))

  // ─── Renderbuffer objects ─────────────────────────────────────────────────

  private lazy val _glGenRenderbuffers = h("glGenRenderbuffers", FunctionDescriptor.ofVoid(I, P))
  override def glGenRenderbuffers(n: Int, renderbuffers: IntBuffer): Unit = _glGenRenderbuffers.invoke(n, bufAddr(renderbuffers))

  override def glGenRenderbuffer(): Int = {
    val buf = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer()
    glGenRenderbuffers(1, buf)
    buf.get(0)
  }

  private lazy val _glDeleteRenderbuffers = h("glDeleteRenderbuffers", FunctionDescriptor.ofVoid(I, P))
  override def glDeleteRenderbuffers(n: Int, renderbuffers: IntBuffer): Unit = _glDeleteRenderbuffers.invoke(n, bufAddr(renderbuffers))

  override def glDeleteRenderbuffer(renderbuffer: Int): Unit = {
    val buf = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer()
    buf.put(0, renderbuffer)
    glDeleteRenderbuffers(1, buf)
  }

  private lazy val _glBindRenderbuffer = h("glBindRenderbuffer", FunctionDescriptor.ofVoid(I, I))
  override def glBindRenderbuffer(target: Int, renderbuffer: Int): Unit = _glBindRenderbuffer.invoke(target, renderbuffer)

  private lazy val _glRenderbufferStorage = h("glRenderbufferStorage", FunctionDescriptor.ofVoid(I, I, I, I))
  override def glRenderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int): Unit =
    _glRenderbufferStorage.invoke(target, internalformat, width, height)

  private lazy val _glGetRenderbufferParameteriv = h("glGetRenderbufferParameteriv", FunctionDescriptor.ofVoid(I, I, P))
  override def glGetRenderbufferParameteriv(target: Int, pname: Int, params: IntBuffer): Unit =
    _glGetRenderbufferParameteriv.invoke(target, pname, bufAddr(params))

  private lazy val _glIsRenderbuffer = h("glIsRenderbuffer", FunctionDescriptor.of(B, I))
  override def glIsRenderbuffer(renderbuffer: Int): Boolean = fromGlBool(_glIsRenderbuffer.invoke(renderbuffer))

  // ─── Shader / Program ─────────────────────────────────────────────────────

  private lazy val _glCreateShader = h("glCreateShader", FunctionDescriptor.of(I, I))
  override def glCreateShader(`type`: Int): Int = _glCreateShader.invoke(`type`).asInstanceOf[Int]

  private lazy val _glDeleteShader = h("glDeleteShader", FunctionDescriptor.ofVoid(I))
  override def glDeleteShader(shader: Int): Unit = _glDeleteShader.invoke(shader)

  private lazy val _glIsShader = h("glIsShader", FunctionDescriptor.of(B, I))
  override def glIsShader(shader: Int): Boolean = fromGlBool(_glIsShader.invoke(shader))

  // glShaderSource: void(GLuint, GLsizei, const GLchar *const *, const GLint *)
  private lazy val _glShaderSource = h("glShaderSource", FunctionDescriptor.ofVoid(I, I, P, P))
  override def glShaderSource(shader: Int, string: String): Unit = {
    val arena = Arena.ofConfined()
    try {
      val str      = arena.allocateFrom(string)
      val strArray = arena.allocate(P)
      strArray.set(P, 0L, str)
      _glShaderSource.invoke(shader, 1, strArray, MemorySegment.NULL)
    } finally arena.close()
  }

  private lazy val _glCompileShader = h("glCompileShader", FunctionDescriptor.ofVoid(I))
  override def glCompileShader(shader: Int): Unit = _glCompileShader.invoke(shader)

  private lazy val _glGetShaderiv = h("glGetShaderiv", FunctionDescriptor.ofVoid(I, I, P))
  override def glGetShaderiv(shader: Int, pname: Int, params: IntBuffer): Unit =
    _glGetShaderiv.invoke(shader, pname, bufAddr(params))

  // glGetShaderInfoLog: void(GLuint, GLsizei, GLsizei *, GLchar *)
  private lazy val _glGetShaderInfoLog = h("glGetShaderInfoLog", FunctionDescriptor.ofVoid(I, I, P, P))
  override def glGetShaderInfoLog(shader: Int): String = {
    val arena = Arena.ofConfined()
    try {
      val lengthBuf = arena.allocate(I)
      val logBuf    = arena.allocate(JAVA_BYTE, 10240L)
      _glGetShaderInfoLog.invoke(shader, 10240, lengthBuf, logBuf)
      val len = lengthBuf.get(I, 0)
      logBuf.reinterpret(len.toLong + 1).getString(0)
    } finally arena.close()
  }

  override def glGetShaderPrecisionFormat(shadertype: Int, precisiontype: Int, range: IntBuffer, precision: IntBuffer): Unit =
    throw new UnsupportedOperationException("glGetShaderPrecisionFormat not supported on desktop")

  override def glShaderBinary(n: Int, shaders: IntBuffer, binaryformat: Int, binary: Buffer, length: Int): Unit =
    throw new UnsupportedOperationException("glShaderBinary not supported on desktop")

  override def glReleaseShaderCompiler(): Unit = () // no-op on desktop

  private lazy val _glCreateProgram = h("glCreateProgram", FunctionDescriptor.of(I))
  override def glCreateProgram(): Int = _glCreateProgram.invoke().asInstanceOf[Int]

  private lazy val _glDeleteProgram = h("glDeleteProgram", FunctionDescriptor.ofVoid(I))
  override def glDeleteProgram(program: Int): Unit = _glDeleteProgram.invoke(program)

  private lazy val _glIsProgram = h("glIsProgram", FunctionDescriptor.of(B, I))
  override def glIsProgram(program: Int): Boolean = fromGlBool(_glIsProgram.invoke(program))

  private lazy val _glAttachShader = h("glAttachShader", FunctionDescriptor.ofVoid(I, I))
  override def glAttachShader(program: Int, shader: Int): Unit = _glAttachShader.invoke(program, shader)

  private lazy val _glDetachShader = h("glDetachShader", FunctionDescriptor.ofVoid(I, I))
  override def glDetachShader(program: Int, shader: Int): Unit = _glDetachShader.invoke(program, shader)

  private lazy val _glLinkProgram = h("glLinkProgram", FunctionDescriptor.ofVoid(I))
  override def glLinkProgram(program: Int): Unit = _glLinkProgram.invoke(program)

  private lazy val _glUseProgram = h("glUseProgram", FunctionDescriptor.ofVoid(I))
  override def glUseProgram(program: Int): Unit = _glUseProgram.invoke(program)

  private lazy val _glValidateProgram = h("glValidateProgram", FunctionDescriptor.ofVoid(I))
  override def glValidateProgram(program: Int): Unit = _glValidateProgram.invoke(program)

  private lazy val _glGetProgramiv = h("glGetProgramiv", FunctionDescriptor.ofVoid(I, I, P))
  override def glGetProgramiv(program: Int, pname: Int, params: IntBuffer): Unit =
    _glGetProgramiv.invoke(program, pname, bufAddr(params))

  // glGetProgramInfoLog: void(GLuint, GLsizei, GLsizei *, GLchar *)
  private lazy val _glGetProgramInfoLog = h("glGetProgramInfoLog", FunctionDescriptor.ofVoid(I, I, P, P))
  override def glGetProgramInfoLog(program: Int): String = {
    val arena = Arena.ofConfined()
    try {
      val lengthBuf = arena.allocate(I)
      val logBuf    = arena.allocate(JAVA_BYTE, 10240L)
      _glGetProgramInfoLog.invoke(program, 10240, lengthBuf, logBuf)
      val len = lengthBuf.get(I, 0)
      logBuf.reinterpret(len.toLong + 1).getString(0)
    } finally arena.close()
  }

  private lazy val _glGetAttachedShaders = h("glGetAttachedShaders", FunctionDescriptor.ofVoid(I, I, P, P))
  override def glGetAttachedShaders(program: Int, maxcount: Int, count: Buffer, shaders: IntBuffer): Unit =
    _glGetAttachedShaders.invoke(program, maxcount, bufAddr(count), bufAddr(shaders))

  // ─── Attributes ───────────────────────────────────────────────────────────

  private lazy val _glBindAttribLocation = h("glBindAttribLocation", FunctionDescriptor.ofVoid(I, I, P))
  override def glBindAttribLocation(program: Int, index: Int, name: String): Unit = {
    val arena = Arena.ofConfined()
    try
      _glBindAttribLocation.invoke(program, index, arena.allocateFrom(name))
    finally arena.close()
  }

  private lazy val _glGetAttribLocation = h("glGetAttribLocation", FunctionDescriptor.of(I, I, P))
  override def glGetAttribLocation(program: Int, name: String): Int = {
    val arena = Arena.ofConfined()
    try
      _glGetAttribLocation.invoke(program, arena.allocateFrom(name)).asInstanceOf[Int]
    finally arena.close()
  }

  // glGetActiveAttrib: void(GLuint, GLuint, GLsizei, GLsizei *, GLint *, GLenum *, GLchar *)
  private lazy val _glGetActiveAttrib = h("glGetActiveAttrib", FunctionDescriptor.ofVoid(I, I, I, P, P, P, P))
  override def glGetActiveAttrib(program: Int, index: Int, size: IntBuffer, `type`: IntBuffer): String = {
    val arena = Arena.ofConfined()
    try {
      val lengthBuf = arena.allocate(I)
      val sizeBuf   = arena.allocate(I)
      val typeBuf   = arena.allocate(I)
      val nameBuf   = arena.allocate(JAVA_BYTE, 256L)
      _glGetActiveAttrib.invoke(program, index, 256, lengthBuf, sizeBuf, typeBuf, nameBuf)
      size.put(sizeBuf.get(I, 0))
      `type`.put(typeBuf.get(I, 0))
      val len = lengthBuf.get(I, 0)
      nameBuf.reinterpret(len.toLong + 1).getString(0)
    } finally arena.close()
  }

  private lazy val _glEnableVertexAttribArray = h("glEnableVertexAttribArray", FunctionDescriptor.ofVoid(I))
  override def glEnableVertexAttribArray(index: Int): Unit = _glEnableVertexAttribArray.invoke(index)

  private lazy val _glDisableVertexAttribArray = h("glDisableVertexAttribArray", FunctionDescriptor.ofVoid(I))
  override def glDisableVertexAttribArray(index: Int): Unit = _glDisableVertexAttribArray.invoke(index)

  private lazy val _glGetVertexAttribfv = h("glGetVertexAttribfv", FunctionDescriptor.ofVoid(I, I, P))
  override def glGetVertexAttribfv(index: Int, pname: Int, params: FloatBuffer): Unit =
    _glGetVertexAttribfv.invoke(index, pname, bufAddr(params))

  private lazy val _glGetVertexAttribiv = h("glGetVertexAttribiv", FunctionDescriptor.ofVoid(I, I, P))
  override def glGetVertexAttribiv(index: Int, pname: Int, params: IntBuffer): Unit =
    _glGetVertexAttribiv.invoke(index, pname, bufAddr(params))

  override def glGetVertexAttribPointerv(index: Int, pname: Int, pointer: Buffer): Unit =
    throw new UnsupportedOperationException("glGetVertexAttribPointerv not supported")

  // ─── Vertex attrib values ─────────────────────────────────────────────────

  private lazy val _glVertexAttrib1f = h("glVertexAttrib1f", FunctionDescriptor.ofVoid(I, F))
  override def glVertexAttrib1f(indx: Int, x: Float): Unit = _glVertexAttrib1f.invoke(indx, x)

  override def glVertexAttrib1fv(indx: Int, values: FloatBuffer): Unit =
    glVertexAttrib1f(indx, values.get())

  private lazy val _glVertexAttrib2f = h("glVertexAttrib2f", FunctionDescriptor.ofVoid(I, F, F))
  override def glVertexAttrib2f(indx: Int, x: Float, y: Float): Unit = _glVertexAttrib2f.invoke(indx, x, y)

  override def glVertexAttrib2fv(indx: Int, values: FloatBuffer): Unit =
    glVertexAttrib2f(indx, values.get(), values.get())

  private lazy val _glVertexAttrib3f = h("glVertexAttrib3f", FunctionDescriptor.ofVoid(I, F, F, F))
  override def glVertexAttrib3f(indx: Int, x: Float, y: Float, z: Float): Unit = _glVertexAttrib3f.invoke(indx, x, y, z)

  override def glVertexAttrib3fv(indx: Int, values: FloatBuffer): Unit =
    glVertexAttrib3f(indx, values.get(), values.get(), values.get())

  private lazy val _glVertexAttrib4f = h("glVertexAttrib4f", FunctionDescriptor.ofVoid(I, F, F, F, F))
  override def glVertexAttrib4f(indx: Int, x: Float, y: Float, z: Float, w: Float): Unit = _glVertexAttrib4f.invoke(indx, x, y, z, w)

  override def glVertexAttrib4fv(indx: Int, values: FloatBuffer): Unit =
    glVertexAttrib4f(indx, values.get(), values.get(), values.get(), values.get())

  // glVertexAttribPointer: void(GLuint, GLint, GLenum, GLboolean, GLsizei, const void *)
  private lazy val _glVertexAttribPointer = h("glVertexAttribPointer", FunctionDescriptor.ofVoid(I, I, I, B, I, P))

  override def glVertexAttribPointer(indx: Int, size: Int, `type`: Int, normalized: Boolean, stride: Int, ptr: Buffer): Unit =
    _glVertexAttribPointer.invoke(indx, size, `type`, glBool(normalized), stride, bufAddr(ptr))

  override def glVertexAttribPointer(indx: Int, size: Int, `type`: Int, normalized: Boolean, stride: Int, ptr: Int): Unit =
    _glVertexAttribPointer.invoke(indx, size, `type`, glBool(normalized), stride, MemorySegment.ofAddress(ptr.toLong))

  // ─── Uniforms ─────────────────────────────────────────────────────────────

  private lazy val _glGetUniformLocation = h("glGetUniformLocation", FunctionDescriptor.of(I, I, P))
  override def glGetUniformLocation(program: Int, name: String): Int = {
    val arena = Arena.ofConfined()
    try
      _glGetUniformLocation.invoke(program, arena.allocateFrom(name)).asInstanceOf[Int]
    finally arena.close()
  }

  // glGetActiveUniform: void(GLuint, GLuint, GLsizei, GLsizei *, GLint *, GLenum *, GLchar *)
  private lazy val _glGetActiveUniform = h("glGetActiveUniform", FunctionDescriptor.ofVoid(I, I, I, P, P, P, P))
  override def glGetActiveUniform(program: Int, index: Int, size: IntBuffer, `type`: IntBuffer): String = {
    val arena = Arena.ofConfined()
    try {
      val lengthBuf = arena.allocate(I)
      val sizeBuf   = arena.allocate(I)
      val typeBuf   = arena.allocate(I)
      val nameBuf   = arena.allocate(JAVA_BYTE, 256L)
      _glGetActiveUniform.invoke(program, index, 256, lengthBuf, sizeBuf, typeBuf, nameBuf)
      size.put(sizeBuf.get(I, 0))
      `type`.put(typeBuf.get(I, 0))
      val len = lengthBuf.get(I, 0)
      nameBuf.reinterpret(len.toLong + 1).getString(0)
    } finally arena.close()
  }

  private lazy val _glGetUniformfv = h("glGetUniformfv", FunctionDescriptor.ofVoid(I, I, P))
  override def glGetUniformfv(program: Int, location: Int, params: FloatBuffer): Unit =
    _glGetUniformfv.invoke(program, location, bufAddr(params))

  private lazy val _glGetUniformiv = h("glGetUniformiv", FunctionDescriptor.ofVoid(I, I, P))
  override def glGetUniformiv(program: Int, location: Int, params: IntBuffer): Unit =
    _glGetUniformiv.invoke(program, location, bufAddr(params))

  // ─── Uniform setters ──────────────────────────────────────────────────────

  private lazy val _glUniform1f = h("glUniform1f", FunctionDescriptor.ofVoid(I, F))
  override def glUniform1f(location: Int, x: Float): Unit = _glUniform1f.invoke(location, x)

  private lazy val _glUniform2f = h("glUniform2f", FunctionDescriptor.ofVoid(I, F, F))
  override def glUniform2f(location: Int, x: Float, y: Float): Unit = _glUniform2f.invoke(location, x, y)

  private lazy val _glUniform3f = h("glUniform3f", FunctionDescriptor.ofVoid(I, F, F, F))
  override def glUniform3f(location: Int, x: Float, y: Float, z: Float): Unit = _glUniform3f.invoke(location, x, y, z)

  private lazy val _glUniform4f = h("glUniform4f", FunctionDescriptor.ofVoid(I, F, F, F, F))
  override def glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float): Unit = _glUniform4f.invoke(location, x, y, z, w)

  private lazy val _glUniform1i = h("glUniform1i", FunctionDescriptor.ofVoid(I, I))
  override def glUniform1i(location: Int, x: Int): Unit = _glUniform1i.invoke(location, x)

  private lazy val _glUniform2i = h("glUniform2i", FunctionDescriptor.ofVoid(I, I, I))
  override def glUniform2i(location: Int, x: Int, y: Int): Unit = _glUniform2i.invoke(location, x, y)

  private lazy val _glUniform3i = h("glUniform3i", FunctionDescriptor.ofVoid(I, I, I, I))
  override def glUniform3i(location: Int, x: Int, y: Int, z: Int): Unit = _glUniform3i.invoke(location, x, y, z)

  private lazy val _glUniform4i = h("glUniform4i", FunctionDescriptor.ofVoid(I, I, I, I, I))
  override def glUniform4i(location: Int, x: Int, y: Int, z: Int, w: Int): Unit = _glUniform4i.invoke(location, x, y, z, w)

  // ─── Uniform vector setters (buffer variants) ─────────────────────────────

  private lazy val _glUniform1fv = h("glUniform1fv", FunctionDescriptor.ofVoid(I, I, P))
  override def glUniform1fv(location: Int, count: Int, v: FloatBuffer): Unit = _glUniform1fv.invoke(location, count, bufAddr(v))

  private lazy val _glUniform2fv = h("glUniform2fv", FunctionDescriptor.ofVoid(I, I, P))
  override def glUniform2fv(location: Int, count: Int, v: FloatBuffer): Unit = _glUniform2fv.invoke(location, count, bufAddr(v))

  private lazy val _glUniform3fv = h("glUniform3fv", FunctionDescriptor.ofVoid(I, I, P))
  override def glUniform3fv(location: Int, count: Int, v: FloatBuffer): Unit = _glUniform3fv.invoke(location, count, bufAddr(v))

  private lazy val _glUniform4fv = h("glUniform4fv", FunctionDescriptor.ofVoid(I, I, P))
  override def glUniform4fv(location: Int, count: Int, v: FloatBuffer): Unit = _glUniform4fv.invoke(location, count, bufAddr(v))

  private lazy val _glUniform1iv = h("glUniform1iv", FunctionDescriptor.ofVoid(I, I, P))
  override def glUniform1iv(location: Int, count: Int, v: IntBuffer): Unit = _glUniform1iv.invoke(location, count, bufAddr(v))

  private lazy val _glUniform2iv = h("glUniform2iv", FunctionDescriptor.ofVoid(I, I, P))
  override def glUniform2iv(location: Int, count: Int, v: IntBuffer): Unit = _glUniform2iv.invoke(location, count, bufAddr(v))

  private lazy val _glUniform3iv = h("glUniform3iv", FunctionDescriptor.ofVoid(I, I, P))
  override def glUniform3iv(location: Int, count: Int, v: IntBuffer): Unit = _glUniform3iv.invoke(location, count, bufAddr(v))

  private lazy val _glUniform4iv = h("glUniform4iv", FunctionDescriptor.ofVoid(I, I, P))
  override def glUniform4iv(location: Int, count: Int, v: IntBuffer): Unit = _glUniform4iv.invoke(location, count, bufAddr(v))

  // ─── Uniform vector setters (array variants) ──────────────────────────────

  override def glUniform1fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit = {
    val arena = Arena.ofConfined()
    try _glUniform1fv.invoke(location, count, floatSeg(arena, v, offset, count))
    finally arena.close()
  }

  override def glUniform2fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit = {
    val arena = Arena.ofConfined()
    try _glUniform2fv.invoke(location, count, floatSeg(arena, v, offset, count * 2))
    finally arena.close()
  }

  override def glUniform3fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit = {
    val arena = Arena.ofConfined()
    try _glUniform3fv.invoke(location, count, floatSeg(arena, v, offset, count * 3))
    finally arena.close()
  }

  override def glUniform4fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit = {
    val arena = Arena.ofConfined()
    try _glUniform4fv.invoke(location, count, floatSeg(arena, v, offset, count * 4))
    finally arena.close()
  }

  override def glUniform1iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit = {
    val arena = Arena.ofConfined()
    try _glUniform1iv.invoke(location, count, intSeg(arena, v, offset, count))
    finally arena.close()
  }

  override def glUniform2iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit = {
    val arena = Arena.ofConfined()
    try _glUniform2iv.invoke(location, count, intSeg(arena, v, offset, count * 2))
    finally arena.close()
  }

  override def glUniform3iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit = {
    val arena = Arena.ofConfined()
    try _glUniform3iv.invoke(location, count, intSeg(arena, v, offset, count * 3))
    finally arena.close()
  }

  override def glUniform4iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit = {
    val arena = Arena.ofConfined()
    try _glUniform4iv.invoke(location, count, intSeg(arena, v, offset, count * 4))
    finally arena.close()
  }

  // ─── Uniform matrix setters ───────────────────────────────────────────────

  private lazy val _glUniformMatrix2fv = h("glUniformMatrix2fv", FunctionDescriptor.ofVoid(I, I, B, P))
  override def glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    _glUniformMatrix2fv.invoke(location, count, glBool(transpose), bufAddr(value))

  override def glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit = {
    val arena = Arena.ofConfined()
    try _glUniformMatrix2fv.invoke(location, count, glBool(transpose), floatSeg(arena, value, offset, count * 4))
    finally arena.close()
  }

  private lazy val _glUniformMatrix3fv = h("glUniformMatrix3fv", FunctionDescriptor.ofVoid(I, I, B, P))
  override def glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    _glUniformMatrix3fv.invoke(location, count, glBool(transpose), bufAddr(value))

  override def glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit = {
    val arena = Arena.ofConfined()
    try _glUniformMatrix3fv.invoke(location, count, glBool(transpose), floatSeg(arena, value, offset, count * 9))
    finally arena.close()
  }

  private lazy val _glUniformMatrix4fv = h("glUniformMatrix4fv", FunctionDescriptor.ofVoid(I, I, B, P))
  override def glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    _glUniformMatrix4fv.invoke(location, count, glBool(transpose), bufAddr(value))

  override def glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit = {
    val arena = Arena.ofConfined()
    try _glUniformMatrix4fv.invoke(location, count, glBool(transpose), floatSeg(arena, value, offset, count * 16))
    finally arena.close()
  }
}
