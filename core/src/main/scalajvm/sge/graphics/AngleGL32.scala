/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (replaces Lwjgl3GL32.java)
 *   Convention: JVM-only; Panama FFM downcall handles to ANGLE libGLESv2
 *   Convention: Extends AngleGL31 — inherits all GL ES 2.0/3.0/3.1 bindings
 *   Convention: glDebugMessageCallback uses Panama upcall stub for native callback
 *   Idiom: split packages; no return; SgeError.GraphicsError
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import java.lang.foreign.*
import java.lang.foreign.ValueLayout.*
import java.lang.invoke.{ MethodHandle, MethodHandles, MethodType }
import java.nio.{ Buffer, ByteBuffer, FloatBuffer, IntBuffer }

/** OpenGL ES 3.2 implementation via ANGLE (libGLESv2) using Panama FFM downcall handles.
  *
  * Extends [[AngleGL31]] with the additional GL ES 3.2 methods. All GL20/GL30/GL31 methods are inherited.
  *
  * @param lookup
  *   a `SymbolLookup` for the ANGLE libGLESv2 shared library
  */
class AngleGL32(lookup: SymbolLookup) extends AngleGL31(lookup) with GL32 {

  // ─── Shorthand layout aliases (inherited from parents are private) ─────────
  private val I32: ValueLayout.OfInt   = JAVA_INT
  private val F32: ValueLayout.OfFloat = JAVA_FLOAT
  private val B32: ValueLayout.OfByte  = JAVA_BYTE
  private val P32: AddressLayout       = ADDRESS

  private val linker32: Linker = Linker.nativeLinker()

  private def h32(name: String, desc: FunctionDescriptor): MethodHandle =
    linker32.downcallHandle(
      lookup.find(name).orElseThrow(() => new UnsatisfiedLinkError(s"GL symbol not found: $name")),
      desc
    )

  private def glBool32(v:          Boolean): Byte    = if (v) 1.toByte else 0.toByte
  private def fromGlBool32(result: AnyRef):  Boolean = result.asInstanceOf[Byte] != 0

  @SuppressWarnings(Array("all"))
  private def bufAddr32(buf: Buffer): MemorySegment =
    if (buf == null) MemorySegment.NULL else MemorySegment.ofBuffer(buf)

  // ─── Blend barrier ────────────────────────────────────────────────────────

  private lazy val _glBlendBarrier =
    h32("glBlendBarrier", FunctionDescriptor.ofVoid())
  override def glBlendBarrier(): Unit =
    _glBlendBarrier.invoke()

  // ─── Copy image sub data ──────────────────────────────────────────────────

  private lazy val _glCopyImageSubData = h32(
    "glCopyImageSubData",
    FunctionDescriptor.ofVoid(I32, I32, I32, I32, I32, I32, I32, I32, I32, I32, I32, I32, I32, I32, I32)
  )
  override def glCopyImageSubData(
    srcName:   Int,
    srcTarget: Int,
    srcLevel:  Int,
    srcX:      Int,
    srcY:      Int,
    srcZ:      Int,
    dstName:   Int,
    dstTarget: Int,
    dstLevel:  Int,
    dstX:      Int,
    dstY:      Int,
    dstZ:      Int,
    srcWidth:  Int,
    srcHeight: Int,
    srcDepth:  Int
  ): Unit =
    _glCopyImageSubData.invoke(
      srcName,
      srcTarget,
      srcLevel,
      srcX,
      srcY,
      srcZ,
      dstName,
      dstTarget,
      dstLevel,
      dstX,
      dstY,
      dstZ,
      srcWidth,
      srcHeight,
      srcDepth
    )

  // ─── Debug messages ───────────────────────────────────────────────────────

  private lazy val _glDebugMessageControl =
    h32("glDebugMessageControl", FunctionDescriptor.ofVoid(I32, I32, I32, I32, P32, B32))
  override def glDebugMessageControl(
    source:   Int,
    `type`:   Int,
    severity: Int,
    ids:      IntBuffer,
    enabled:  Boolean
  ): Unit = {
    val count = if (ids == null) 0 else ids.remaining()
    _glDebugMessageControl.invoke(source, `type`, severity, count, bufAddr32(ids), glBool32(enabled))
  }

  private lazy val _glDebugMessageInsert =
    h32("glDebugMessageInsert", FunctionDescriptor.ofVoid(I32, I32, I32, I32, I32, P32))
  override def glDebugMessageInsert(source: Int, `type`: Int, id: Int, severity: Int, buf: String): Unit = {
    val arena = Arena.ofConfined()
    try {
      val str = arena.allocateFrom(buf)
      _glDebugMessageInsert.invoke(source, `type`, id, severity, buf.length, str)
    } finally arena.close()
  }

  // Debug callback: Panama upcall stub
  // C signature: void callback(GLenum source, GLenum type, GLuint id, GLenum severity, GLsizei length, const GLchar *message, const void *userParam)
  private var debugCallbackArena: Arena = scala.compiletime.uninitialized

  private lazy val _glDebugMessageCallback =
    h32("glDebugMessageCallback", FunctionDescriptor.ofVoid(P32, P32))

  override def glDebugMessageCallback(callback: DebugProc): Unit = {
    // Close previous callback arena if any
    if (debugCallbackArena != null) {
      debugCallbackArena.close()
      debugCallbackArena = null.asInstanceOf[Arena]
    }

    if (callback == null) {
      _glDebugMessageCallback.invoke(MemorySegment.NULL, MemorySegment.NULL)
    } else {
      debugCallbackArena = Arena.ofShared()
      val stubDesc = FunctionDescriptor.ofVoid(I32, I32, I32, I32, I32, P32, P32)
      // We need a MethodHandle for the dispatch method
      val dispatchHandle = MethodHandles
        .lookup()
        .bind(
          new DebugCallbackDispatcher(callback),
          "dispatch",
          MethodType.methodType(
            classOf[Unit],
            classOf[Int],
            classOf[Int],
            classOf[Int],
            classOf[Int],
            classOf[Int],
            classOf[MemorySegment],
            classOf[MemorySegment]
          )
        )
      val stub = linker32.upcallStub(dispatchHandle, stubDesc, debugCallbackArena)
      _glDebugMessageCallback.invoke(stub, MemorySegment.NULL)
    }
  }

  private class DebugCallbackDispatcher(callback: DebugProc) {
    def dispatch(source: Int, `type`: Int, id: Int, severity: Int, length: Int, message: MemorySegment, userParam: MemorySegment): Unit = {
      val msg =
        if (length > 0) message.reinterpret(length.toLong + 1).getString(0)
        else ""
      callback.onMessage(source, `type`, id, severity, msg)
    }
  }

  private lazy val _glGetDebugMessageLog =
    h32("glGetDebugMessageLog", FunctionDescriptor.of(I32, I32, I32, P32, P32, P32, P32, P32, P32))
  override def glGetDebugMessageLog(
    count:      Int,
    sources:    IntBuffer,
    types:      IntBuffer,
    ids:        IntBuffer,
    severities: IntBuffer,
    lengths:    IntBuffer,
    messageLog: ByteBuffer
  ): Int =
    _glGetDebugMessageLog
      .invoke(
        count,
        if (messageLog != null) messageLog.remaining() else 0,
        bufAddr32(sources),
        bufAddr32(types),
        bufAddr32(ids),
        bufAddr32(severities),
        bufAddr32(lengths),
        bufAddr32(messageLog)
      )
      .asInstanceOf[Int]

  // ─── Debug groups ─────────────────────────────────────────────────────────

  private lazy val _glPushDebugGroup =
    h32("glPushDebugGroup", FunctionDescriptor.ofVoid(I32, I32, I32, P32))
  override def glPushDebugGroup(source: Int, id: Int, message: String): Unit = {
    val arena = Arena.ofConfined()
    try _glPushDebugGroup.invoke(source, id, message.length, arena.allocateFrom(message))
    finally arena.close()
  }

  private lazy val _glPopDebugGroup =
    h32("glPopDebugGroup", FunctionDescriptor.ofVoid())
  override def glPopDebugGroup(): Unit =
    _glPopDebugGroup.invoke()

  // ─── Object labels ────────────────────────────────────────────────────────

  private lazy val _glObjectLabel =
    h32("glObjectLabel", FunctionDescriptor.ofVoid(I32, I32, I32, P32))
  override def glObjectLabel(identifier: Int, name: Int, label: String): Unit = {
    val arena = Arena.ofConfined()
    try _glObjectLabel.invoke(identifier, name, label.length, arena.allocateFrom(label))
    finally arena.close()
  }

  private lazy val _glGetObjectLabel =
    h32("glGetObjectLabel", FunctionDescriptor.ofVoid(I32, I32, I32, P32, P32))
  override def glGetObjectLabel(identifier: Int, name: Int): String = {
    val arena = Arena.ofConfined()
    try {
      val maxLen   = 1024
      val labelBuf = arena.allocate(maxLen.toLong)
      val lenBuf   = arena.allocate(I32)
      _glGetObjectLabel.invoke(identifier, name, maxLen, lenBuf, labelBuf)
      val len = lenBuf.get(I32, 0L)
      if (len <= 0) ""
      else labelBuf.reinterpret(len.toLong + 1).getString(0)
    } finally arena.close()
  }

  // ─── Pointer queries ──────────────────────────────────────────────────────

  private lazy val _glGetPointerv =
    h32("glGetPointerv", FunctionDescriptor.ofVoid(I32, P32))
  override def glGetPointerv(pname: Int): Long = {
    val arena = Arena.ofConfined()
    try {
      val ptrBuf = arena.allocate(P32)
      _glGetPointerv.invoke(pname, ptrBuf)
      ptrBuf.get(P32, 0L).address()
    } finally arena.close()
  }

  // ─── Indexed enable/disable ───────────────────────────────────────────────

  private lazy val _glEnablei =
    h32("glEnablei", FunctionDescriptor.ofVoid(I32, I32))
  override def glEnablei(target: Int, index: Int): Unit =
    _glEnablei.invoke(target, index)

  private lazy val _glDisablei =
    h32("glDisablei", FunctionDescriptor.ofVoid(I32, I32))
  override def glDisablei(target: Int, index: Int): Unit =
    _glDisablei.invoke(target, index)

  // ─── Indexed blend ────────────────────────────────────────────────────────

  private lazy val _glBlendEquationi =
    h32("glBlendEquationi", FunctionDescriptor.ofVoid(I32, I32))
  override def glBlendEquationi(buf: Int, mode: Int): Unit =
    _glBlendEquationi.invoke(buf, mode)

  private lazy val _glBlendEquationSeparatei =
    h32("glBlendEquationSeparatei", FunctionDescriptor.ofVoid(I32, I32, I32))
  override def glBlendEquationSeparatei(buf: Int, modeRGB: Int, modeAlpha: Int): Unit =
    _glBlendEquationSeparatei.invoke(buf, modeRGB, modeAlpha)

  private lazy val _glBlendFunci =
    h32("glBlendFunci", FunctionDescriptor.ofVoid(I32, I32, I32))
  override def glBlendFunci(buf: Int, src: Int, dst: Int): Unit =
    _glBlendFunci.invoke(buf, src, dst)

  private lazy val _glBlendFuncSeparatei =
    h32("glBlendFuncSeparatei", FunctionDescriptor.ofVoid(I32, I32, I32, I32, I32))
  override def glBlendFuncSeparatei(buf: Int, srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int): Unit =
    _glBlendFuncSeparatei.invoke(buf, srcRGB, dstRGB, srcAlpha, dstAlpha)

  // ─── Indexed color mask ───────────────────────────────────────────────────

  private lazy val _glColorMaski =
    h32("glColorMaski", FunctionDescriptor.ofVoid(I32, B32, B32, B32, B32))
  override def glColorMaski(index: Int, r: Boolean, g: Boolean, b: Boolean, a: Boolean): Unit =
    _glColorMaski.invoke(index, glBool32(r), glBool32(g), glBool32(b), glBool32(a))

  private lazy val _glIsEnabledi =
    h32("glIsEnabledi", FunctionDescriptor.of(B32, I32, I32))
  override def glIsEnabledi(target: Int, index: Int): Boolean =
    fromGlBool32(_glIsEnabledi.invoke(target, index))

  // ─── Draw elements with base vertex ───────────────────────────────────────

  // Buffer variant: pass pointer to index data
  private lazy val _glDrawElementsBaseVertex =
    h32("glDrawElementsBaseVertex", FunctionDescriptor.ofVoid(I32, I32, I32, P32, I32))
  override def glDrawElementsBaseVertex(mode: Int, count: Int, `type`: Int, indices: Buffer, basevertex: Int): Unit =
    _glDrawElementsBaseVertex.invoke(mode, count, `type`, bufAddr32(indices), basevertex)

  private lazy val _glDrawRangeElementsBaseVertex =
    h32("glDrawRangeElementsBaseVertex", FunctionDescriptor.ofVoid(I32, I32, I32, I32, I32, P32, I32))
  override def glDrawRangeElementsBaseVertex(
    mode:       Int,
    start:      Int,
    end:        Int,
    count:      Int,
    `type`:     Int,
    indices:    Buffer,
    basevertex: Int
  ): Unit =
    _glDrawRangeElementsBaseVertex.invoke(mode, start, end, count, `type`, bufAddr32(indices), basevertex)

  // Buffer variant
  private lazy val _glDrawElementsInstancedBaseVertex =
    h32("glDrawElementsInstancedBaseVertex", FunctionDescriptor.ofVoid(I32, I32, I32, P32, I32, I32))
  override def glDrawElementsInstancedBaseVertex(
    mode:          Int,
    count:         Int,
    `type`:        Int,
    indices:       Buffer,
    instanceCount: Int,
    basevertex:    Int
  ): Unit =
    _glDrawElementsInstancedBaseVertex.invoke(mode, count, `type`, bufAddr32(indices), instanceCount, basevertex)

  // Offset variant
  override def glDrawElementsInstancedBaseVertex(
    mode:          Int,
    count:         Int,
    `type`:        Int,
    indicesOffset: Int,
    instanceCount: Int,
    basevertex:    Int
  ): Unit =
    _glDrawElementsInstancedBaseVertex.invoke(
      mode,
      count,
      `type`,
      MemorySegment.ofAddress(indicesOffset.toLong),
      instanceCount,
      basevertex
    )

  // ─── Framebuffer texture ──────────────────────────────────────────────────

  private lazy val _glFramebufferTexture =
    h32("glFramebufferTexture", FunctionDescriptor.ofVoid(I32, I32, I32, I32))
  override def glFramebufferTexture(target: Int, attachment: Int, texture: Int, level: Int): Unit =
    _glFramebufferTexture.invoke(target, attachment, texture, level)

  // ─── Reset status ─────────────────────────────────────────────────────────

  private lazy val _glGetGraphicsResetStatus =
    h32("glGetGraphicsResetStatus", FunctionDescriptor.of(I32))
  override def glGetGraphicsResetStatus(): Int =
    _glGetGraphicsResetStatus.invoke().asInstanceOf[Int]

  // ─── Robust readback ──────────────────────────────────────────────────────

  private lazy val _glReadnPixels =
    h32("glReadnPixels", FunctionDescriptor.ofVoid(I32, I32, I32, I32, I32, I32, I32, P32))
  override def glReadnPixels(
    x:       Int,
    y:       Int,
    width:   Int,
    height:  Int,
    format:  Int,
    `type`:  Int,
    bufSize: Int,
    data:    Buffer
  ): Unit =
    _glReadnPixels.invoke(x, y, width, height, format, `type`, bufSize, bufAddr32(data))

  // ─── Robust uniform getters ───────────────────────────────────────────────

  private lazy val _glGetnUniformfv =
    h32("glGetnUniformfv", FunctionDescriptor.ofVoid(I32, I32, I32, P32))
  override def glGetnUniformfv(program: Int, location: Int, params: FloatBuffer): Unit =
    _glGetnUniformfv.invoke(program, location, params.remaining() * 4, bufAddr32(params))

  private lazy val _glGetnUniformiv =
    h32("glGetnUniformiv", FunctionDescriptor.ofVoid(I32, I32, I32, P32))
  override def glGetnUniformiv(program: Int, location: Int, params: IntBuffer): Unit =
    _glGetnUniformiv.invoke(program, location, params.remaining() * 4, bufAddr32(params))

  private lazy val _glGetnUniformuiv =
    h32("glGetnUniformuiv", FunctionDescriptor.ofVoid(I32, I32, I32, P32))
  override def glGetnUniformuiv(program: Int, location: Int, params: IntBuffer): Unit =
    _glGetnUniformuiv.invoke(program, location, params.remaining() * 4, bufAddr32(params))

  // ─── Sample shading / patches ─────────────────────────────────────────────

  private lazy val _glMinSampleShading =
    h32("glMinSampleShading", FunctionDescriptor.ofVoid(F32))
  override def glMinSampleShading(value: Float): Unit =
    _glMinSampleShading.invoke(value)

  private lazy val _glPatchParameteri =
    h32("glPatchParameteri", FunctionDescriptor.ofVoid(I32, I32))
  override def glPatchParameteri(pname: Int, value: Int): Unit =
    _glPatchParameteri.invoke(pname, value)

  // ─── Texture parameter integer variants ───────────────────────────────────

  private lazy val _glTexParameterIiv =
    h32("glTexParameterIiv", FunctionDescriptor.ofVoid(I32, I32, P32))
  override def glTexParameterIiv(target: Int, pname: Int, params: IntBuffer): Unit =
    _glTexParameterIiv.invoke(target, pname, bufAddr32(params))

  private lazy val _glTexParameterIuiv =
    h32("glTexParameterIuiv", FunctionDescriptor.ofVoid(I32, I32, P32))
  override def glTexParameterIuiv(target: Int, pname: Int, params: IntBuffer): Unit =
    _glTexParameterIuiv.invoke(target, pname, bufAddr32(params))

  private lazy val _glGetTexParameterIiv =
    h32("glGetTexParameterIiv", FunctionDescriptor.ofVoid(I32, I32, P32))
  override def glGetTexParameterIiv(target: Int, pname: Int, params: IntBuffer): Unit =
    _glGetTexParameterIiv.invoke(target, pname, bufAddr32(params))

  private lazy val _glGetTexParameterIuiv =
    h32("glGetTexParameterIuiv", FunctionDescriptor.ofVoid(I32, I32, P32))
  override def glGetTexParameterIuiv(target: Int, pname: Int, params: IntBuffer): Unit =
    _glGetTexParameterIuiv.invoke(target, pname, bufAddr32(params))

  // ─── Sampler parameter integer variants ───────────────────────────────────

  private lazy val _glSamplerParameterIiv =
    h32("glSamplerParameterIiv", FunctionDescriptor.ofVoid(I32, I32, P32))
  override def glSamplerParameterIiv(sampler: Int, pname: Int, param: IntBuffer): Unit =
    _glSamplerParameterIiv.invoke(sampler, pname, bufAddr32(param))

  private lazy val _glSamplerParameterIuiv =
    h32("glSamplerParameterIuiv", FunctionDescriptor.ofVoid(I32, I32, P32))
  override def glSamplerParameterIuiv(sampler: Int, pname: Int, param: IntBuffer): Unit =
    _glSamplerParameterIuiv.invoke(sampler, pname, bufAddr32(param))

  private lazy val _glGetSamplerParameterIiv =
    h32("glGetSamplerParameterIiv", FunctionDescriptor.ofVoid(I32, I32, P32))
  override def glGetSamplerParameterIiv(sampler: Int, pname: Int, params: IntBuffer): Unit =
    _glGetSamplerParameterIiv.invoke(sampler, pname, bufAddr32(params))

  private lazy val _glGetSamplerParameterIuiv =
    h32("glGetSamplerParameterIuiv", FunctionDescriptor.ofVoid(I32, I32, P32))
  override def glGetSamplerParameterIuiv(sampler: Int, pname: Int, params: IntBuffer): Unit =
    _glGetSamplerParameterIuiv.invoke(sampler, pname, bufAddr32(params))

  // ─── Texture buffer ───────────────────────────────────────────────────────

  private lazy val _glTexBuffer =
    h32("glTexBuffer", FunctionDescriptor.ofVoid(I32, I32, I32))
  override def glTexBuffer(target: Int, internalformat: Int, buffer: Int): Unit =
    _glTexBuffer.invoke(target, internalformat, buffer)

  private lazy val _glTexBufferRange =
    h32("glTexBufferRange", FunctionDescriptor.ofVoid(I32, I32, I32, I32, I32))
  override def glTexBufferRange(target: Int, internalformat: Int, buffer: Int, offset: Int, size: Int): Unit =
    _glTexBufferRange.invoke(target, internalformat, buffer, offset, size)

  // ─── 3D multisample texture storage ───────────────────────────────────────

  private lazy val _glTexStorage3DMultisample =
    h32("glTexStorage3DMultisample", FunctionDescriptor.ofVoid(I32, I32, I32, I32, I32, I32, B32))
  override def glTexStorage3DMultisample(
    target:               Int,
    samples:              Int,
    internalformat:       Int,
    width:                Int,
    height:               Int,
    depth:                Int,
    fixedsamplelocations: Boolean
  ): Unit =
    _glTexStorage3DMultisample.invoke(
      target,
      samples,
      internalformat,
      width,
      height,
      depth,
      glBool32(fixedsamplelocations)
    )
}
