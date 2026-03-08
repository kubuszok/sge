/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (replaces Lwjgl3GL32.java)
 *   Convention: Scala Native @extern bindings to ANGLE libGLESv2
 *   Convention: Extends AngleGL31Native — inherits all GL ES 2.0/3.0/3.1 bindings
 *   Convention: glDebugMessageCallback uses CFuncPtr for native callback (vs Panama upcall on JVM)
 *   Idiom: split packages; no return
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import java.nio.{ Buffer, ByteBuffer, FloatBuffer, IntBuffer }

import scala.scalanative.unsafe.*
// unsigned.* imported via NativeGlHelper

import NativeGlHelper.*

// ─── C extern declarations for GL ES 3.2 ────────────────────────────────────

@link("GLESv2")
@extern
private[graphics] object GL32C {
  // Blend barrier
  def glBlendBarrier(): Unit = extern

  // Copy image sub data
  def glCopyImageSubData(
    srcName:   CInt,
    srcTarget: CInt,
    srcLevel:  CInt,
    srcX:      CInt,
    srcY:      CInt,
    srcZ:      CInt,
    dstName:   CInt,
    dstTarget: CInt,
    dstLevel:  CInt,
    dstX:      CInt,
    dstY:      CInt,
    dstZ:      CInt,
    srcWidth:  CInt,
    srcHeight: CInt,
    srcDepth:  CInt
  ): Unit = extern

  // Debug messages
  def glDebugMessageControl(source:    CInt, tp:             CInt, severity: CInt, count:      CInt, ids:      Ptr[CInt], enabled:    CUnsignedChar):                                      Unit = extern
  def glDebugMessageInsert(source:     CInt, tp:             CInt, id:       CInt, severity:   CInt, length:   CInt, buf:             CString):                                            Unit = extern
  def glDebugMessageCallback(callback: Ptr[Byte], userParam: Ptr[Byte]):                                                                                                                   Unit = extern
  def glGetDebugMessageLog(count:      CInt, bufSize:        CInt, sources:  Ptr[CInt], types: Ptr[CInt], ids: Ptr[CInt], severities: Ptr[CInt], lengths: Ptr[CInt], messageLog: CString): CInt = extern

  // Debug groups
  def glPushDebugGroup(source: CInt, id: CInt, length: CInt, message: CString): Unit = extern
  def glPopDebugGroup():                                                        Unit = extern

  // Object labels
  def glObjectLabel(identifier:    CInt, name: CInt, length:  CInt, label:  CString):                   Unit = extern
  def glGetObjectLabel(identifier: CInt, name: CInt, bufSize: CInt, length: Ptr[CInt], label: CString): Unit = extern

  // Pointer queries
  def glGetPointerv(pname: CInt, params: Ptr[Ptr[Byte]]): Unit = extern

  // Indexed enable/disable
  def glEnablei(target:  CInt, index: CInt): Unit = extern
  def glDisablei(target: CInt, index: CInt): Unit = extern

  // Indexed blend
  def glBlendEquationi(buf:         CInt, mode:    CInt):                                                  Unit = extern
  def glBlendEquationSeparatei(buf: CInt, modeRGB: CInt, modeAlpha: CInt):                                 Unit = extern
  def glBlendFunci(buf:             CInt, src:     CInt, dst:       CInt):                                 Unit = extern
  def glBlendFuncSeparatei(buf:     CInt, srcRGB:  CInt, dstRGB:    CInt, srcAlpha: CInt, dstAlpha: CInt): Unit = extern

  // Indexed color mask
  def glColorMaski(index:  CInt, r:     CUnsignedChar, g: CUnsignedChar, b: CUnsignedChar, a: CUnsignedChar): Unit          = extern
  def glIsEnabledi(target: CInt, index: CInt):                                                                CUnsignedChar = extern

  // Draw elements with base vertex
  def glDrawElementsBaseVertex(mode:          CInt, count: CInt, tp:  CInt, indices: Ptr[Byte], basevertex:    CInt):                                          Unit = extern
  def glDrawRangeElementsBaseVertex(mode:     CInt, start: CInt, end: CInt, count:   CInt, tp:                 CInt, indices:    Ptr[Byte], basevertex: CInt): Unit = extern
  def glDrawElementsInstancedBaseVertex(mode: CInt, count: CInt, tp:  CInt, indices: Ptr[Byte], instanceCount: CInt, basevertex: CInt):                        Unit = extern

  // Framebuffer texture
  def glFramebufferTexture(target: CInt, attachment: CInt, texture: CInt, level: CInt): Unit = extern

  // Reset status
  def glGetGraphicsResetStatus(): CInt = extern

  // Robust readback
  def glReadnPixels(x: CInt, y: CInt, w: CInt, h: CInt, format: CInt, tp: CInt, bufSize: CInt, data: Ptr[Byte]): Unit = extern

  // Robust uniform getters
  def glGetnUniformfv(program:  CInt, location: CInt, bufSize: CInt, params: Ptr[CFloat]): Unit = extern
  def glGetnUniformiv(program:  CInt, location: CInt, bufSize: CInt, params: Ptr[CInt]):   Unit = extern
  def glGetnUniformuiv(program: CInt, location: CInt, bufSize: CInt, params: Ptr[CInt]):   Unit = extern

  // Sample shading / patches
  def glMinSampleShading(value: CFloat):            Unit = extern
  def glPatchParameteri(pname:  CInt, value: CInt): Unit = extern

  // Texture parameter integer variants
  def glTexParameterIiv(target:     CInt, pname: CInt, params: Ptr[CInt]): Unit = extern
  def glTexParameterIuiv(target:    CInt, pname: CInt, params: Ptr[CInt]): Unit = extern
  def glGetTexParameterIiv(target:  CInt, pname: CInt, params: Ptr[CInt]): Unit = extern
  def glGetTexParameterIuiv(target: CInt, pname: CInt, params: Ptr[CInt]): Unit = extern

  // Sampler parameter integer variants
  def glSamplerParameterIiv(sampler:     CInt, pname: CInt, param:  Ptr[CInt]): Unit = extern
  def glSamplerParameterIuiv(sampler:    CInt, pname: CInt, param:  Ptr[CInt]): Unit = extern
  def glGetSamplerParameterIiv(sampler:  CInt, pname: CInt, params: Ptr[CInt]): Unit = extern
  def glGetSamplerParameterIuiv(sampler: CInt, pname: CInt, params: Ptr[CInt]): Unit = extern

  // Texture buffer
  def glTexBuffer(target:      CInt, internalformat: CInt, buffer: CInt):                           Unit = extern
  def glTexBufferRange(target: CInt, internalformat: CInt, buffer: CInt, offset: CInt, size: CInt): Unit = extern

  // 3D multisample texture storage
  def glTexStorage3DMultisample(target: CInt, samples: CInt, intfmt: CInt, w: CInt, h: CInt, depth: CInt, fixed: CUnsignedChar): Unit = extern
}

// ─── Static debug callback holder (CFuncPtr requires statically reachable symbols) ─

private[graphics] object AngleGL32Native {
  var debugCallback: GL32#DebugProc = scala.compiletime.uninitialized
}

// ─── GL32 wrapper ─────────────────────────────────────────────────────────────

/** OpenGL ES 3.2 implementation via ANGLE (libGLESv2) using Scala Native @extern bindings.
  *
  * Extends [[AngleGL31Native]] with the additional GL ES 3.2 methods.
  */
class AngleGL32Native extends AngleGL31Native with GL32 {

  // ─── Blend barrier ────────────────────────────────────────────────────────

  override def glBlendBarrier(): Unit = GL32C.glBlendBarrier()

  // ─── Copy image sub data ──────────────────────────────────────────────────

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
    GL32C.glCopyImageSubData(srcName, srcTarget, srcLevel, srcX, srcY, srcZ, dstName, dstTarget, dstLevel, dstX, dstY, dstZ, srcWidth, srcHeight, srcDepth)

  // ─── Debug messages ───────────────────────────────────────────────────────

  override def glDebugMessageControl(
    source:   Int,
    `type`:   Int,
    severity: Int,
    ids:      IntBuffer,
    enabled:  Boolean
  ): Unit = {
    val count  = if (ids == null) 0 else ids.remaining()
    val idsPtr = if (ids == null) null.asInstanceOf[Ptr[CInt]] else bufPtr(ids).asInstanceOf[Ptr[CInt]]
    GL32C.glDebugMessageControl(source, `type`, severity, count, idsPtr, glBool(enabled))
  }

  override def glDebugMessageInsert(source: Int, `type`: Int, id: Int, severity: Int, buf: String): Unit = {
    val zone = Zone.open()
    try GL32C.glDebugMessageInsert(source, `type`, id, severity, buf.length, toCString(buf)(using zone))
    finally zone.close()
  }

  // Debug callback: static function pointer required by Scala Native CFuncPtr
  override def glDebugMessageCallback(callback: DebugProc): Unit = {
    AngleGL32Native.debugCallback = callback
    if (callback == null) {
      GL32C.glDebugMessageCallback(null.asInstanceOf[Ptr[Byte]], null.asInstanceOf[Ptr[Byte]])
    } else {
      val fp = CFuncPtr.toPtr(
        CFuncPtr7.fromScalaFunction { (source: CInt, tp: CInt, id: CInt, severity: CInt, length: CInt, message: CString, _userParam: Ptr[Byte]) =>
          val cb = AngleGL32Native.debugCallback
          if (cb != null) {
            val msg = if (length > 0 && message != null) fromCString(message) else ""
            cb.onMessage(source, tp, id, severity, msg)
          }
        }
      )
      GL32C.glDebugMessageCallback(fp.asInstanceOf[Ptr[Byte]], null.asInstanceOf[Ptr[Byte]])
    }
  }

  override def glGetDebugMessageLog(
    count:      Int,
    sources:    IntBuffer,
    types:      IntBuffer,
    ids:        IntBuffer,
    severities: IntBuffer,
    lengths:    IntBuffer,
    messageLog: ByteBuffer
  ): Int =
    GL32C.glGetDebugMessageLog(
      count,
      if (messageLog != null) messageLog.remaining() else 0,
      bufPtr(sources).asInstanceOf[Ptr[CInt]],
      bufPtr(types).asInstanceOf[Ptr[CInt]],
      bufPtr(ids).asInstanceOf[Ptr[CInt]],
      bufPtr(severities).asInstanceOf[Ptr[CInt]],
      bufPtr(lengths).asInstanceOf[Ptr[CInt]],
      bufPtr(messageLog).asInstanceOf[CString]
    )

  // ─── Debug groups ─────────────────────────────────────────────────────────

  override def glPushDebugGroup(source: Int, id: Int, message: String): Unit = {
    val zone = Zone.open()
    try GL32C.glPushDebugGroup(source, id, message.length, toCString(message)(using zone))
    finally zone.close()
  }

  override def glPopDebugGroup(): Unit = GL32C.glPopDebugGroup()

  // ─── Object labels ────────────────────────────────────────────────────────

  override def glObjectLabel(identifier: Int, name: Int, label: String): Unit = {
    val zone = Zone.open()
    try GL32C.glObjectLabel(identifier, name, label.length, toCString(label)(using zone))
    finally zone.close()
  }

  override def glGetObjectLabel(identifier: Int, name: Int): String = {
    val labelBuf  = stackalloc[Byte](1024)
    val lengthBuf = stackalloc[CInt]()
    GL32C.glGetObjectLabel(identifier, name, 1024, lengthBuf, labelBuf)
    val len = !lengthBuf
    if (len <= 0) ""
    else fromCString(labelBuf)
  }

  // ─── Pointer queries ──────────────────────────────────────────────────────

  override def glGetPointerv(pname: Int): Long = {
    val ptrBuf = stackalloc[Ptr[Byte]]()
    GL32C.glGetPointerv(pname, ptrBuf)
    scala.scalanative.runtime.Intrinsics.castRawPtrToLong((!ptrBuf).asInstanceOf[scala.scalanative.runtime.RawPtr])
  }

  // ─── Indexed enable/disable ───────────────────────────────────────────────

  override def glEnablei(target:  Int, index: Int): Unit = GL32C.glEnablei(target, index)
  override def glDisablei(target: Int, index: Int): Unit = GL32C.glDisablei(target, index)

  // ─── Indexed blend ────────────────────────────────────────────────────────

  override def glBlendEquationi(buf:         Int, mode:    BlendEquation):                           Unit = GL32C.glBlendEquationi(buf, mode.toInt)
  override def glBlendEquationSeparatei(buf: Int, modeRGB: BlendEquation, modeAlpha: BlendEquation): Unit = GL32C.glBlendEquationSeparatei(buf, modeRGB.toInt, modeAlpha.toInt)
  override def glBlendFunci(buf:             Int, src:     BlendFactor, dst:         BlendFactor):   Unit = GL32C.glBlendFunci(buf, src.toInt, dst.toInt)
  override def glBlendFuncSeparatei(buf: Int, srcRGB: BlendFactor, dstRGB: BlendFactor, srcAlpha: BlendFactor, dstAlpha: BlendFactor): Unit =
    GL32C.glBlendFuncSeparatei(buf, srcRGB.toInt, dstRGB.toInt, srcAlpha.toInt, dstAlpha.toInt)

  // ─── Indexed color mask ───────────────────────────────────────────────────

  override def glColorMaski(index: Int, r: Boolean, g: Boolean, b: Boolean, a: Boolean): Unit =
    GL32C.glColorMaski(index, glBool(r), glBool(g), glBool(b), glBool(a))

  override def glIsEnabledi(target: Int, index: Int): Boolean =
    fromGlBool(GL32C.glIsEnabledi(target, index))

  // ─── Draw elements with base vertex ───────────────────────────────────────

  override def glDrawElementsBaseVertex(mode: PrimitiveMode, count: Int, `type`: DataType, indices: Buffer, basevertex: Int): Unit =
    GL32C.glDrawElementsBaseVertex(mode.toInt, count, `type`.toInt, bufPtr(indices), basevertex)

  override def glDrawRangeElementsBaseVertex(
    mode:       PrimitiveMode,
    start:      Int,
    end:        Int,
    count:      Int,
    `type`:     DataType,
    indices:    Buffer,
    basevertex: Int
  ): Unit =
    GL32C.glDrawRangeElementsBaseVertex(mode.toInt, start, end, count, `type`.toInt, bufPtr(indices), basevertex)

  override def glDrawElementsInstancedBaseVertex(
    mode:          PrimitiveMode,
    count:         Int,
    `type`:        DataType,
    indices:       Buffer,
    instanceCount: Int,
    basevertex:    Int
  ): Unit =
    GL32C.glDrawElementsInstancedBaseVertex(mode.toInt, count, `type`.toInt, bufPtr(indices), instanceCount, basevertex)

  override def glDrawElementsInstancedBaseVertex(
    mode:          PrimitiveMode,
    count:         Int,
    `type`:        DataType,
    indicesOffset: Int,
    instanceCount: Int,
    basevertex:    Int
  ): Unit =
    GL32C.glDrawElementsInstancedBaseVertex(mode.toInt, count, `type`.toInt, offsetPtr(indicesOffset), instanceCount, basevertex)

  // ─── Framebuffer texture ──────────────────────────────────────────────────

  override def glFramebufferTexture(target: Int, attachment: Int, texture: Int, level: Int): Unit =
    GL32C.glFramebufferTexture(target, attachment, texture, level)

  // ─── Reset status ─────────────────────────────────────────────────────────

  override def glGetGraphicsResetStatus(): Int = GL32C.glGetGraphicsResetStatus()

  // ─── Robust readback ──────────────────────────────────────────────────────

  override def glReadnPixels(
    x:       Int,
    y:       Int,
    width:   Int,
    height:  Int,
    format:  PixelFormat,
    `type`:  DataType,
    bufSize: Int,
    data:    Buffer
  ): Unit =
    GL32C.glReadnPixels(x, y, width, height, format.toInt, `type`.toInt, bufSize, bufPtr(data))

  // ─── Robust uniform getters ───────────────────────────────────────────────

  override def glGetnUniformfv(program: Int, location: Int, params: FloatBuffer): Unit =
    GL32C.glGetnUniformfv(program, location, params.remaining() * 4, bufPtr(params).asInstanceOf[Ptr[CFloat]])

  override def glGetnUniformiv(program: Int, location: Int, params: IntBuffer): Unit =
    GL32C.glGetnUniformiv(program, location, params.remaining() * 4, bufPtr(params).asInstanceOf[Ptr[CInt]])

  override def glGetnUniformuiv(program: Int, location: Int, params: IntBuffer): Unit =
    GL32C.glGetnUniformuiv(program, location, params.remaining() * 4, bufPtr(params).asInstanceOf[Ptr[CInt]])

  // ─── Sample shading / patches ─────────────────────────────────────────────

  override def glMinSampleShading(value: Float):           Unit = GL32C.glMinSampleShading(value)
  override def glPatchParameteri(pname:  Int, value: Int): Unit = GL32C.glPatchParameteri(pname, value)

  // ─── Texture parameter integer variants ───────────────────────────────────

  override def glTexParameterIiv(target: TextureTarget, pname: Int, params: IntBuffer): Unit =
    GL32C.glTexParameterIiv(target.toInt, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])
  override def glTexParameterIuiv(target: TextureTarget, pname: Int, params: IntBuffer): Unit =
    GL32C.glTexParameterIuiv(target.toInt, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])
  override def glGetTexParameterIiv(target: TextureTarget, pname: Int, params: IntBuffer): Unit =
    GL32C.glGetTexParameterIiv(target.toInt, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])
  override def glGetTexParameterIuiv(target: TextureTarget, pname: Int, params: IntBuffer): Unit =
    GL32C.glGetTexParameterIuiv(target.toInt, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])

  // ─── Sampler parameter integer variants ───────────────────────────────────

  override def glSamplerParameterIiv(sampler: Int, pname: Int, param: IntBuffer): Unit =
    GL32C.glSamplerParameterIiv(sampler, pname, bufPtr(param).asInstanceOf[Ptr[CInt]])
  override def glSamplerParameterIuiv(sampler: Int, pname: Int, param: IntBuffer): Unit =
    GL32C.glSamplerParameterIuiv(sampler, pname, bufPtr(param).asInstanceOf[Ptr[CInt]])
  override def glGetSamplerParameterIiv(sampler: Int, pname: Int, params: IntBuffer): Unit =
    GL32C.glGetSamplerParameterIiv(sampler, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])
  override def glGetSamplerParameterIuiv(sampler: Int, pname: Int, params: IntBuffer): Unit =
    GL32C.glGetSamplerParameterIuiv(sampler, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])

  // ─── Texture buffer ───────────────────────────────────────────────────────

  override def glTexBuffer(target: TextureTarget, internalformat: Int, buffer: Int): Unit =
    GL32C.glTexBuffer(target.toInt, internalformat, buffer)
  override def glTexBufferRange(target: TextureTarget, internalformat: Int, buffer: Int, offset: Int, size: Int): Unit =
    GL32C.glTexBufferRange(target.toInt, internalformat, buffer, offset, size)

  // ─── 3D multisample texture storage ───────────────────────────────────────

  override def glTexStorage3DMultisample(
    target:               TextureTarget,
    samples:              Int,
    internalformat:       Int,
    width:                Int,
    height:               Int,
    depth:                Int,
    fixedsamplelocations: Boolean
  ): Unit =
    GL32C.glTexStorage3DMultisample(target.toInt, samples, internalformat, width, height, depth, glBool(fixedsamplelocations))
}
