/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (replaces Lwjgl3GL31.java)
 *   Convention: Scala Native @extern bindings to ANGLE libGLESv2
 *   Convention: Extends AngleGL30Native — inherits all GL ES 2.0/3.0 bindings
 *   Idiom: split packages; no return
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import java.nio.{ FloatBuffer, IntBuffer }

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

import NativeGlHelper.*

// ─── C extern declarations for GL ES 3.1 ────────────────────────────────────

@link("GLESv2")
@extern
private[graphics] object GL31C {
  // Compute shaders
  def glDispatchCompute(x:                CInt, y: CInt, z: CInt): Unit = extern
  def glDispatchComputeIndirect(indirect: CLong):                  Unit = extern

  // Indirect draw
  def glDrawArraysIndirect(mode:   CInt, indirect: Ptr[Byte]):                 Unit = extern
  def glDrawElementsIndirect(mode: CInt, tp:       CInt, indirect: Ptr[Byte]): Unit = extern

  // Framebuffer parameters
  def glFramebufferParameteri(target:     CInt, pname: CInt, param:  CInt):      Unit = extern
  def glGetFramebufferParameteriv(target: CInt, pname: CInt, params: Ptr[CInt]): Unit = extern

  // Program interface queries
  def glGetProgramInterfaceiv(program:      CInt, iface: CInt, pname: CInt, params:    Ptr[CInt]):                                                                    Unit = extern
  def glGetProgramResourceIndex(program:    CInt, iface: CInt, name:  CString):                                                                                       CInt = extern
  def glGetProgramResourceName(program:     CInt, iface: CInt, index: CInt, bufSize:   CInt, length: Ptr[CInt], name:    CString):                                    Unit = extern
  def glGetProgramResourceiv(program:       CInt, iface: CInt, index: CInt, propCount: CInt, props:  Ptr[CInt], bufSize: CInt, length: Ptr[CInt], params: Ptr[CInt]): Unit = extern
  def glGetProgramResourceLocation(program: CInt, iface: CInt, name:  CString):                                                                                       CInt = extern

  // Program pipelines
  def glUseProgramStages(pipeline:     CInt, stages:    CInt, program: CInt):         Unit          = extern
  def glActiveShaderProgram(pipeline:  CInt, program:   CInt):                        Unit          = extern
  def glCreateShaderProgramv(tp:       CInt, count:     CInt, strings: Ptr[CString]): CInt          = extern
  def glBindProgramPipeline(pipeline:  CInt):                                         Unit          = extern
  def glDeleteProgramPipelines(n:      CInt, pipelines: Ptr[CInt]):                   Unit          = extern
  def glGenProgramPipelines(n:         CInt, pipelines: Ptr[CInt]):                   Unit          = extern
  def glIsProgramPipeline(pipeline:    CInt):                                         CUnsignedChar = extern
  def glGetProgramPipelineiv(pipeline: CInt, pname:     CInt, params:  Ptr[CInt]):    Unit          = extern

  // Program uniforms (scalar)
  def glProgramUniform1i(program:  CInt, location: CInt, v0: CInt):                                       Unit = extern
  def glProgramUniform2i(program:  CInt, location: CInt, v0: CInt, v1:   CInt):                           Unit = extern
  def glProgramUniform3i(program:  CInt, location: CInt, v0: CInt, v1:   CInt, v2:   CInt):               Unit = extern
  def glProgramUniform4i(program:  CInt, location: CInt, v0: CInt, v1:   CInt, v2:   CInt, v3:   CInt):   Unit = extern
  def glProgramUniform1ui(program: CInt, location: CInt, v0: CInt):                                       Unit = extern
  def glProgramUniform2ui(program: CInt, location: CInt, v0: CInt, v1:   CInt):                           Unit = extern
  def glProgramUniform3ui(program: CInt, location: CInt, v0: CInt, v1:   CInt, v2:   CInt):               Unit = extern
  def glProgramUniform4ui(program: CInt, location: CInt, v0: CInt, v1:   CInt, v2:   CInt, v3:   CInt):   Unit = extern
  def glProgramUniform1f(program:  CInt, location: CInt, v0: CFloat):                                     Unit = extern
  def glProgramUniform2f(program:  CInt, location: CInt, v0: CFloat, v1: CFloat):                         Unit = extern
  def glProgramUniform3f(program:  CInt, location: CInt, v0: CFloat, v1: CFloat, v2: CFloat):             Unit = extern
  def glProgramUniform4f(program:  CInt, location: CInt, v0: CFloat, v1: CFloat, v2: CFloat, v3: CFloat): Unit = extern

  // Program uniforms (buffer/vector)
  def glProgramUniform1iv(program:  CInt, location: CInt, count: CInt, value: Ptr[CInt]):   Unit = extern
  def glProgramUniform2iv(program:  CInt, location: CInt, count: CInt, value: Ptr[CInt]):   Unit = extern
  def glProgramUniform3iv(program:  CInt, location: CInt, count: CInt, value: Ptr[CInt]):   Unit = extern
  def glProgramUniform4iv(program:  CInt, location: CInt, count: CInt, value: Ptr[CInt]):   Unit = extern
  def glProgramUniform1uiv(program: CInt, location: CInt, count: CInt, value: Ptr[CInt]):   Unit = extern
  def glProgramUniform2uiv(program: CInt, location: CInt, count: CInt, value: Ptr[CInt]):   Unit = extern
  def glProgramUniform3uiv(program: CInt, location: CInt, count: CInt, value: Ptr[CInt]):   Unit = extern
  def glProgramUniform4uiv(program: CInt, location: CInt, count: CInt, value: Ptr[CInt]):   Unit = extern
  def glProgramUniform1fv(program:  CInt, location: CInt, count: CInt, value: Ptr[CFloat]): Unit = extern
  def glProgramUniform2fv(program:  CInt, location: CInt, count: CInt, value: Ptr[CFloat]): Unit = extern
  def glProgramUniform3fv(program:  CInt, location: CInt, count: CInt, value: Ptr[CFloat]): Unit = extern
  def glProgramUniform4fv(program:  CInt, location: CInt, count: CInt, value: Ptr[CFloat]): Unit = extern

  // Program uniform matrices
  def glProgramUniformMatrix2fv(program:   CInt, location: CInt, count: CInt, transpose: CUnsignedChar, value: Ptr[CFloat]): Unit = extern
  def glProgramUniformMatrix3fv(program:   CInt, location: CInt, count: CInt, transpose: CUnsignedChar, value: Ptr[CFloat]): Unit = extern
  def glProgramUniformMatrix4fv(program:   CInt, location: CInt, count: CInt, transpose: CUnsignedChar, value: Ptr[CFloat]): Unit = extern
  def glProgramUniformMatrix2x3fv(program: CInt, location: CInt, count: CInt, transpose: CUnsignedChar, value: Ptr[CFloat]): Unit = extern
  def glProgramUniformMatrix3x2fv(program: CInt, location: CInt, count: CInt, transpose: CUnsignedChar, value: Ptr[CFloat]): Unit = extern
  def glProgramUniformMatrix2x4fv(program: CInt, location: CInt, count: CInt, transpose: CUnsignedChar, value: Ptr[CFloat]): Unit = extern
  def glProgramUniformMatrix4x2fv(program: CInt, location: CInt, count: CInt, transpose: CUnsignedChar, value: Ptr[CFloat]): Unit = extern
  def glProgramUniformMatrix3x4fv(program: CInt, location: CInt, count: CInt, transpose: CUnsignedChar, value: Ptr[CFloat]): Unit = extern
  def glProgramUniformMatrix4x3fv(program: CInt, location: CInt, count: CInt, transpose: CUnsignedChar, value: Ptr[CFloat]): Unit = extern

  // Pipeline validation
  def glValidateProgramPipeline(pipeline:  CInt):                                                     Unit = extern
  def glGetProgramPipelineInfoLog(program: CInt, bufSize: CInt, length: Ptr[CInt], infoLog: CString): Unit = extern

  // Image textures
  def glBindImageTexture(unit: CInt, texture: CInt, level: CInt, layered: CUnsignedChar, layer: CInt, access: CInt, format: CInt): Unit = extern

  // Boolean indexed queries
  def glGetBooleani_v(target: CInt, index: CInt, data: Ptr[CUnsignedChar]): Unit = extern

  // Memory barriers
  def glMemoryBarrier(barriers:         CInt): Unit = extern
  def glMemoryBarrierByRegion(barriers: CInt): Unit = extern

  // Multisample textures
  def glTexStorage2DMultisample(target: CInt, samples: CInt, intfmt: CInt, w: CInt, h: CInt, fixed: CUnsignedChar): Unit = extern
  def glGetMultisamplefv(pname:         CInt, index:   CInt, value:  Ptr[CFloat]):                                  Unit = extern
  def glSampleMaski(maskNumber:         CInt, mask:    CInt):                                                       Unit = extern

  // Texture level parameters
  def glGetTexLevelParameteriv(target: CInt, level: CInt, pname: CInt, params: Ptr[CInt]):   Unit = extern
  def glGetTexLevelParameterfv(target: CInt, level: CInt, pname: CInt, params: Ptr[CFloat]): Unit = extern

  // Vertex attribute binding
  def glBindVertexBuffer(bindingindex:     CInt, buffer:       CInt, offset: CLong, stride:        CInt):                                Unit = extern
  def glVertexAttribFormat(attribindex:    CInt, size:         CInt, tp:     CInt, normalized:     CUnsignedChar, relativeoffset: CInt): Unit = extern
  def glVertexAttribIFormat(attribindex:   CInt, size:         CInt, tp:     CInt, relativeoffset: CInt):                                Unit = extern
  def glVertexAttribBinding(attribindex:   CInt, bindingindex: CInt):                                                                    Unit = extern
  def glVertexBindingDivisor(bindingindex: CInt, divisor:      CInt):                                                                    Unit = extern
}

// ─── GL31 wrapper ─────────────────────────────────────────────────────────────

/** OpenGL ES 3.1 implementation via ANGLE (libGLESv2) using Scala Native @extern bindings.
  *
  * Extends [[AngleGL30Native]] with the additional GL ES 3.1 methods.
  */
class AngleGL31Native extends AngleGL30Native with GL31 {

  // ─── Compute shaders ──────────────────────────────────────────────────────

  override def glDispatchCompute(num_groups_x: Int, num_groups_y: Int, num_groups_z: Int): Unit =
    GL31C.glDispatchCompute(num_groups_x, num_groups_y, num_groups_z)

  override def glDispatchComputeIndirect(indirect: Long): Unit =
    GL31C.glDispatchComputeIndirect(indirect.asInstanceOf[CLong])

  // ─── Indirect draw ────────────────────────────────────────────────────────

  override def glDrawArraysIndirect(mode: PrimitiveMode, indirect: Long): Unit =
    GL31C.glDrawArraysIndirect(mode.toInt, offsetPtr(indirect.toInt))

  override def glDrawElementsIndirect(mode: PrimitiveMode, `type`: DataType, indirect: Long): Unit =
    GL31C.glDrawElementsIndirect(mode.toInt, `type`.toInt, offsetPtr(indirect.toInt))

  // ─── Framebuffer parameters ───────────────────────────────────────────────

  override def glFramebufferParameteri(target: Int, pname: Int, param: Int): Unit =
    GL31C.glFramebufferParameteri(target, pname, param)

  override def glGetFramebufferParameteriv(target: Int, pname: Int, params: IntBuffer): Unit =
    GL31C.glGetFramebufferParameteriv(target, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])

  // ─── Program interface queries ────────────────────────────────────────────

  override def glGetProgramInterfaceiv(program: Int, programInterface: Int, pname: Int, params: IntBuffer): Unit =
    GL31C.glGetProgramInterfaceiv(program, programInterface, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])

  override def glGetProgramResourceIndex(program: Int, programInterface: Int, name: String): Int = {
    val zone = Zone.open()
    try GL31C.glGetProgramResourceIndex(program, programInterface, toCString(name)(using zone))
    finally zone.close()
  }

  override def glGetProgramResourceName(program: Int, programInterface: Int, index: Int): String = {
    val nameBuf   = stackalloc[Byte](1024)
    val lengthBuf = stackalloc[CInt]()
    GL31C.glGetProgramResourceName(program, programInterface, index, 1024, lengthBuf, nameBuf)
    fromCString(nameBuf)
  }

  override def glGetProgramResourceiv(
    program:          Int,
    programInterface: Int,
    index:            Int,
    props:            IntBuffer,
    length:           IntBuffer,
    params:           IntBuffer
  ): Unit =
    GL31C.glGetProgramResourceiv(
      program,
      programInterface,
      index,
      props.remaining(),
      bufPtr(props).asInstanceOf[Ptr[CInt]],
      params.remaining(),
      bufPtr(length).asInstanceOf[Ptr[CInt]],
      bufPtr(params).asInstanceOf[Ptr[CInt]]
    )

  override def glGetProgramResourceLocation(program: Int, programInterface: Int, name: String): Int = {
    val zone = Zone.open()
    try GL31C.glGetProgramResourceLocation(program, programInterface, toCString(name)(using zone))
    finally zone.close()
  }

  // ─── Program pipelines ────────────────────────────────────────────────────

  override def glUseProgramStages(pipeline: Int, stages: Int, program: Int): Unit =
    GL31C.glUseProgramStages(pipeline, stages, program)

  override def glActiveShaderProgram(pipeline: Int, program: Int): Unit =
    GL31C.glActiveShaderProgram(pipeline, program)

  override def glCreateShaderProgramv(`type`: ShaderType, strings: Array[String]): Int = {
    val zone = Zone.open()
    try {
      val ptrs = stackalloc[CString](strings.length)
      var i    = 0
      while (i < strings.length) { ptrs(i) = toCString(strings(i))(using zone); i += 1 }
      GL31C.glCreateShaderProgramv(`type`.toInt, strings.length, ptrs)
    } finally zone.close()
  }

  override def glBindProgramPipeline(pipeline: Int): Unit =
    GL31C.glBindProgramPipeline(pipeline)

  override def glDeleteProgramPipelines(n: Int, pipelines: IntBuffer): Unit =
    GL31C.glDeleteProgramPipelines(n, bufPtr(pipelines).asInstanceOf[Ptr[CInt]])

  override def glGenProgramPipelines(n: Int, pipelines: IntBuffer): Unit =
    GL31C.glGenProgramPipelines(n, bufPtr(pipelines).asInstanceOf[Ptr[CInt]])

  override def glIsProgramPipeline(pipeline: Int): Boolean =
    fromGlBool(GL31C.glIsProgramPipeline(pipeline))

  override def glGetProgramPipelineiv(pipeline: Int, pname: Int, params: IntBuffer): Unit =
    GL31C.glGetProgramPipelineiv(pipeline, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])

  // ─── Program uniforms (scalar) ────────────────────────────────────────────

  override def glProgramUniform1i(program: Int, location: Int, v0: Int): Unit =
    GL31C.glProgramUniform1i(program, location, v0)
  override def glProgramUniform2i(program: Int, location: Int, v0: Int, v1: Int): Unit =
    GL31C.glProgramUniform2i(program, location, v0, v1)
  override def glProgramUniform3i(program: Int, location: Int, v0: Int, v1: Int, v2: Int): Unit =
    GL31C.glProgramUniform3i(program, location, v0, v1, v2)
  override def glProgramUniform4i(program: Int, location: Int, v0: Int, v1: Int, v2: Int, v3: Int): Unit =
    GL31C.glProgramUniform4i(program, location, v0, v1, v2, v3)

  override def glProgramUniform1ui(program: Int, location: Int, v0: Int): Unit =
    GL31C.glProgramUniform1ui(program, location, v0)
  override def glProgramUniform2ui(program: Int, location: Int, v0: Int, v1: Int): Unit =
    GL31C.glProgramUniform2ui(program, location, v0, v1)
  override def glProgramUniform3ui(program: Int, location: Int, v0: Int, v1: Int, v2: Int): Unit =
    GL31C.glProgramUniform3ui(program, location, v0, v1, v2)
  override def glProgramUniform4ui(program: Int, location: Int, v0: Int, v1: Int, v2: Int, v3: Int): Unit =
    GL31C.glProgramUniform4ui(program, location, v0, v1, v2, v3)

  override def glProgramUniform1f(program: Int, location: Int, v0: Float): Unit =
    GL31C.glProgramUniform1f(program, location, v0)
  override def glProgramUniform2f(program: Int, location: Int, v0: Float, v1: Float): Unit =
    GL31C.glProgramUniform2f(program, location, v0, v1)
  override def glProgramUniform3f(program: Int, location: Int, v0: Float, v1: Float, v2: Float): Unit =
    GL31C.glProgramUniform3f(program, location, v0, v1, v2)
  override def glProgramUniform4f(program: Int, location: Int, v0: Float, v1: Float, v2: Float, v3: Float): Unit =
    GL31C.glProgramUniform4f(program, location, v0, v1, v2, v3)

  // ─── Program uniforms (buffer/vector) ─────────────────────────────────────

  override def glProgramUniform1iv(program: Int, location: Int, value: IntBuffer): Unit =
    GL31C.glProgramUniform1iv(program, location, value.remaining(), bufPtr(value).asInstanceOf[Ptr[CInt]])
  override def glProgramUniform2iv(program: Int, location: Int, value: IntBuffer): Unit =
    GL31C.glProgramUniform2iv(program, location, value.remaining() / 2, bufPtr(value).asInstanceOf[Ptr[CInt]])
  override def glProgramUniform3iv(program: Int, location: Int, value: IntBuffer): Unit =
    GL31C.glProgramUniform3iv(program, location, value.remaining() / 3, bufPtr(value).asInstanceOf[Ptr[CInt]])
  override def glProgramUniform4iv(program: Int, location: Int, value: IntBuffer): Unit =
    GL31C.glProgramUniform4iv(program, location, value.remaining() / 4, bufPtr(value).asInstanceOf[Ptr[CInt]])

  override def glProgramUniform1uiv(program: Int, location: Int, value: IntBuffer): Unit =
    GL31C.glProgramUniform1uiv(program, location, value.remaining(), bufPtr(value).asInstanceOf[Ptr[CInt]])
  override def glProgramUniform2uiv(program: Int, location: Int, value: IntBuffer): Unit =
    GL31C.glProgramUniform2uiv(program, location, value.remaining() / 2, bufPtr(value).asInstanceOf[Ptr[CInt]])
  override def glProgramUniform3uiv(program: Int, location: Int, value: IntBuffer): Unit =
    GL31C.glProgramUniform3uiv(program, location, value.remaining() / 3, bufPtr(value).asInstanceOf[Ptr[CInt]])
  override def glProgramUniform4uiv(program: Int, location: Int, value: IntBuffer): Unit =
    GL31C.glProgramUniform4uiv(program, location, value.remaining() / 4, bufPtr(value).asInstanceOf[Ptr[CInt]])

  override def glProgramUniform1fv(program: Int, location: Int, value: FloatBuffer): Unit =
    GL31C.glProgramUniform1fv(program, location, value.remaining(), bufPtr(value).asInstanceOf[Ptr[CFloat]])
  override def glProgramUniform2fv(program: Int, location: Int, value: FloatBuffer): Unit =
    GL31C.glProgramUniform2fv(program, location, value.remaining() / 2, bufPtr(value).asInstanceOf[Ptr[CFloat]])
  override def glProgramUniform3fv(program: Int, location: Int, value: FloatBuffer): Unit =
    GL31C.glProgramUniform3fv(program, location, value.remaining() / 3, bufPtr(value).asInstanceOf[Ptr[CFloat]])
  override def glProgramUniform4fv(program: Int, location: Int, value: FloatBuffer): Unit =
    GL31C.glProgramUniform4fv(program, location, value.remaining() / 4, bufPtr(value).asInstanceOf[Ptr[CFloat]])

  // ─── Program uniform matrices ─────────────────────────────────────────────

  override def glProgramUniformMatrix2fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GL31C.glProgramUniformMatrix2fv(program, location, value.remaining() / 4, glBool(transpose), bufPtr(value).asInstanceOf[Ptr[CFloat]])

  override def glProgramUniformMatrix3fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GL31C.glProgramUniformMatrix3fv(program, location, value.remaining() / 9, glBool(transpose), bufPtr(value).asInstanceOf[Ptr[CFloat]])

  override def glProgramUniformMatrix4fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GL31C.glProgramUniformMatrix4fv(program, location, value.remaining() / 16, glBool(transpose), bufPtr(value).asInstanceOf[Ptr[CFloat]])

  override def glProgramUniformMatrix2x3fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GL31C.glProgramUniformMatrix2x3fv(program, location, value.remaining() / 6, glBool(transpose), bufPtr(value).asInstanceOf[Ptr[CFloat]])

  override def glProgramUniformMatrix3x2fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GL31C.glProgramUniformMatrix3x2fv(program, location, value.remaining() / 6, glBool(transpose), bufPtr(value).asInstanceOf[Ptr[CFloat]])

  override def glProgramUniformMatrix2x4fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GL31C.glProgramUniformMatrix2x4fv(program, location, value.remaining() / 8, glBool(transpose), bufPtr(value).asInstanceOf[Ptr[CFloat]])

  override def glProgramUniformMatrix4x2fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GL31C.glProgramUniformMatrix4x2fv(program, location, value.remaining() / 8, glBool(transpose), bufPtr(value).asInstanceOf[Ptr[CFloat]])

  override def glProgramUniformMatrix3x4fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GL31C.glProgramUniformMatrix3x4fv(program, location, value.remaining() / 12, glBool(transpose), bufPtr(value).asInstanceOf[Ptr[CFloat]])

  override def glProgramUniformMatrix4x3fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit =
    GL31C.glProgramUniformMatrix4x3fv(program, location, value.remaining() / 12, glBool(transpose), bufPtr(value).asInstanceOf[Ptr[CFloat]])

  // ─── Pipeline validation ──────────────────────────────────────────────────

  override def glValidateProgramPipeline(pipeline: Int): Unit =
    GL31C.glValidateProgramPipeline(pipeline)

  override def glGetProgramPipelineInfoLog(program: Int): String = {
    val logBuf    = stackalloc[Byte](10240)
    val lengthBuf = stackalloc[CInt]()
    GL31C.glGetProgramPipelineInfoLog(program, 10240, lengthBuf, logBuf)
    val len = !lengthBuf
    if (len <= 0) ""
    else fromCString(logBuf)
  }

  // ─── Image textures ───────────────────────────────────────────────────────

  override def glBindImageTexture(
    unit:    Int,
    texture: Int,
    level:   Int,
    layered: Boolean,
    layer:   Int,
    access:  Int,
    format:  Int
  ): Unit =
    GL31C.glBindImageTexture(unit, texture, level, glBool(layered), layer, access, format)

  // ─── Boolean indexed queries ──────────────────────────────────────────────

  override def glGetBooleani_v(target: Int, index: Int, data: IntBuffer): Unit = {
    val tmpBuf = stackalloc[CUnsignedChar](16)
    GL31C.glGetBooleani_v(target, index, tmpBuf)
    val remaining = scala.math.min(data.remaining(), 16)
    var i         = 0
    while (i < remaining) {
      data.put(if (tmpBuf(i) != 0.toUByte) 1 else 0)
      i += 1
    }
  }

  // ─── Memory barriers ─────────────────────────────────────────────────────

  override def glMemoryBarrier(barriers:         Int): Unit = GL31C.glMemoryBarrier(barriers)
  override def glMemoryBarrierByRegion(barriers: Int): Unit = GL31C.glMemoryBarrierByRegion(barriers)

  // ─── Multisample textures ─────────────────────────────────────────────────

  override def glTexStorage2DMultisample(
    target:               TextureTarget,
    samples:              Int,
    internalformat:       Int,
    width:                Int,
    height:               Int,
    fixedsamplelocations: Boolean
  ): Unit =
    GL31C.glTexStorage2DMultisample(target.toInt, samples, internalformat, width, height, glBool(fixedsamplelocations))

  override def glGetMultisamplefv(pname: Int, index: Int, value: FloatBuffer): Unit =
    GL31C.glGetMultisamplefv(pname, index, bufPtr(value).asInstanceOf[Ptr[CFloat]])

  override def glSampleMaski(maskNumber: Int, mask: Int): Unit =
    GL31C.glSampleMaski(maskNumber, mask)

  // ─── Texture level parameters ─────────────────────────────────────────────

  override def glGetTexLevelParameteriv(target: TextureTarget, level: Int, pname: Int, params: IntBuffer): Unit =
    GL31C.glGetTexLevelParameteriv(target.toInt, level, pname, bufPtr(params).asInstanceOf[Ptr[CInt]])

  override def glGetTexLevelParameterfv(target: TextureTarget, level: Int, pname: Int, params: FloatBuffer): Unit =
    GL31C.glGetTexLevelParameterfv(target.toInt, level, pname, bufPtr(params).asInstanceOf[Ptr[CFloat]])

  // ─── Vertex attribute binding ─────────────────────────────────────────────

  override def glBindVertexBuffer(bindingindex: Int, buffer: Int, offset: Long, stride: Int): Unit =
    GL31C.glBindVertexBuffer(bindingindex, buffer, offset.asInstanceOf[CLong], stride)

  override def glVertexAttribFormat(
    attribindex:    Int,
    size:           Int,
    `type`:         DataType,
    normalized:     Boolean,
    relativeoffset: Int
  ): Unit =
    GL31C.glVertexAttribFormat(attribindex, size, `type`.toInt, glBool(normalized), relativeoffset)

  override def glVertexAttribIFormat(attribindex: Int, size: Int, `type`: DataType, relativeoffset: Int): Unit =
    GL31C.glVertexAttribIFormat(attribindex, size, `type`.toInt, relativeoffset)

  override def glVertexAttribBinding(attribindex: Int, bindingindex: Int): Unit =
    GL31C.glVertexAttribBinding(attribindex, bindingindex)

  override def glVertexBindingDivisor(bindingindex: Int, divisor: Int): Unit =
    GL31C.glVertexBindingDivisor(bindingindex, divisor)
}
