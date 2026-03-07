/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (replaces Lwjgl3GL31.java)
 *   Convention: JVM-only; Panama FFM downcall handles to ANGLE libGLESv2
 *   Convention: Extends AngleGL30 — inherits all GL ES 2.0/3.0 bindings
 *   Idiom: split packages; no return; SgeError.GraphicsError
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import java.lang.foreign.*
import java.lang.foreign.ValueLayout.*
import java.lang.invoke.MethodHandle
import java.nio.{ Buffer, FloatBuffer, IntBuffer }

/** OpenGL ES 3.1 implementation via ANGLE (libGLESv2) using Panama FFM downcall handles.
  *
  * Extends [[AngleGL30]] with the additional GL ES 3.1 methods. All GL20/GL30 methods are inherited.
  *
  * @param lookup
  *   a `SymbolLookup` for the ANGLE libGLESv2 shared library
  */
class AngleGL31(lookup: SymbolLookup) extends AngleGL30(lookup) with GL31 {

  // ─── Shorthand layout aliases (inherited from parents are private) ─────────
  private val I31: ValueLayout.OfInt   = JAVA_INT
  private val F31: ValueLayout.OfFloat = JAVA_FLOAT
  private val B31: ValueLayout.OfByte  = JAVA_BYTE
  private val L31: ValueLayout.OfLong  = JAVA_LONG
  private val P31: AddressLayout       = ADDRESS

  private val linker31: Linker = Linker.nativeLinker()

  private def h31(name: String, desc: FunctionDescriptor): MethodHandle =
    linker31.downcallHandle(
      lookup.find(name).orElseThrow(() => new UnsatisfiedLinkError(s"GL symbol not found: $name")),
      desc
    )

  private def glBool31(v:          Boolean): Byte    = if (v) 1.toByte else 0.toByte
  private def fromGlBool31(result: AnyRef):  Boolean = result.asInstanceOf[Byte] != 0

  @SuppressWarnings(Array("all"))
  private def bufAddr31(buf: Buffer): MemorySegment =
    if (buf == null) MemorySegment.NULL else MemorySegment.ofBuffer(buf)

  // ─── Compute shaders ──────────────────────────────────────────────────────

  private lazy val _glDispatchCompute =
    h31("glDispatchCompute", FunctionDescriptor.ofVoid(I31, I31, I31))
  override def glDispatchCompute(num_groups_x: Int, num_groups_y: Int, num_groups_z: Int): Unit =
    _glDispatchCompute.invoke(num_groups_x, num_groups_y, num_groups_z)

  private lazy val _glDispatchComputeIndirect =
    h31("glDispatchComputeIndirect", FunctionDescriptor.ofVoid(L31))
  override def glDispatchComputeIndirect(indirect: Long): Unit =
    _glDispatchComputeIndirect.invoke(indirect)

  // ─── Indirect draw ────────────────────────────────────────────────────────

  private lazy val _glDrawArraysIndirect =
    h31("glDrawArraysIndirect", FunctionDescriptor.ofVoid(I31, P31))
  override def glDrawArraysIndirect(mode: PrimitiveMode, indirect: Long): Unit =
    _glDrawArraysIndirect.invoke(mode, MemorySegment.ofAddress(indirect))

  private lazy val _glDrawElementsIndirect =
    h31("glDrawElementsIndirect", FunctionDescriptor.ofVoid(I31, I31, P31))
  override def glDrawElementsIndirect(mode: PrimitiveMode, `type`: DataType, indirect: Long): Unit =
    _glDrawElementsIndirect.invoke(mode, `type`, MemorySegment.ofAddress(indirect))

  // ─── Framebuffer parameters ───────────────────────────────────────────────

  private lazy val _glFramebufferParameteri =
    h31("glFramebufferParameteri", FunctionDescriptor.ofVoid(I31, I31, I31))
  override def glFramebufferParameteri(target: Int, pname: Int, param: Int): Unit =
    _glFramebufferParameteri.invoke(target, pname, param)

  private lazy val _glGetFramebufferParameteriv =
    h31("glGetFramebufferParameteriv", FunctionDescriptor.ofVoid(I31, I31, P31))
  override def glGetFramebufferParameteriv(target: Int, pname: Int, params: IntBuffer): Unit =
    _glGetFramebufferParameteriv.invoke(target, pname, bufAddr31(params))

  // ─── Program interface queries ────────────────────────────────────────────

  private lazy val _glGetProgramInterfaceiv =
    h31("glGetProgramInterfaceiv", FunctionDescriptor.ofVoid(I31, I31, I31, P31))
  override def glGetProgramInterfaceiv(program: Int, programInterface: Int, pname: Int, params: IntBuffer): Unit =
    _glGetProgramInterfaceiv.invoke(program, programInterface, pname, bufAddr31(params))

  private lazy val _glGetProgramResourceIndex =
    h31("glGetProgramResourceIndex", FunctionDescriptor.of(I31, I31, I31, P31))
  override def glGetProgramResourceIndex(program: Int, programInterface: Int, name: String): Int = {
    val arena = Arena.ofConfined()
    try _glGetProgramResourceIndex.invoke(program, programInterface, arena.allocateFrom(name)).asInstanceOf[Int]
    finally arena.close()
  }

  private lazy val _glGetProgramResourceName =
    h31("glGetProgramResourceName", FunctionDescriptor.ofVoid(I31, I31, I31, I31, P31, P31))
  override def glGetProgramResourceName(program: Int, programInterface: Int, index: Int): String = {
    val arena = Arena.ofConfined()
    try {
      val maxLen  = 1024
      val nameBuf = arena.allocate(maxLen.toLong)
      val lenBuf  = arena.allocate(I31)
      _glGetProgramResourceName.invoke(program, programInterface, index, maxLen, lenBuf, nameBuf)
      val len = lenBuf.get(I31, 0L)
      nameBuf.reinterpret(len.toLong + 1).getString(0)
    } finally arena.close()
  }

  private lazy val _glGetProgramResourceiv =
    h31("glGetProgramResourceiv", FunctionDescriptor.ofVoid(I31, I31, I31, I31, P31, I31, P31, P31))
  override def glGetProgramResourceiv(
    program:          Int,
    programInterface: Int,
    index:            Int,
    props:            IntBuffer,
    length:           IntBuffer,
    params:           IntBuffer
  ): Unit =
    _glGetProgramResourceiv.invoke(
      program,
      programInterface,
      index,
      props.remaining(),
      bufAddr31(props),
      params.remaining(),
      bufAddr31(length),
      bufAddr31(params)
    )

  private lazy val _glGetProgramResourceLocation =
    h31("glGetProgramResourceLocation", FunctionDescriptor.of(I31, I31, I31, P31))
  override def glGetProgramResourceLocation(program: Int, programInterface: Int, name: String): Int = {
    val arena = Arena.ofConfined()
    try _glGetProgramResourceLocation.invoke(program, programInterface, arena.allocateFrom(name)).asInstanceOf[Int]
    finally arena.close()
  }

  // ─── Program pipelines ────────────────────────────────────────────────────

  private lazy val _glUseProgramStages =
    h31("glUseProgramStages", FunctionDescriptor.ofVoid(I31, I31, I31))
  override def glUseProgramStages(pipeline: Int, stages: Int, program: Int): Unit =
    _glUseProgramStages.invoke(pipeline, stages, program)

  private lazy val _glActiveShaderProgram =
    h31("glActiveShaderProgram", FunctionDescriptor.ofVoid(I31, I31))
  override def glActiveShaderProgram(pipeline: Int, program: Int): Unit =
    _glActiveShaderProgram.invoke(pipeline, program)

  private lazy val _glCreateShaderProgramv =
    h31("glCreateShaderProgramv", FunctionDescriptor.of(I31, I31, I31, P31))
  override def glCreateShaderProgramv(`type`: ShaderType, strings: Array[String]): Int = {
    val arena = Arena.ofConfined()
    try {
      val ptrs = arena.allocate(P31, strings.length.toLong)
      var i    = 0
      while (i < strings.length) {
        ptrs.setAtIndex(P31, i.toLong, arena.allocateFrom(strings(i)))
        i += 1
      }
      _glCreateShaderProgramv.invoke(`type`, strings.length, ptrs).asInstanceOf[Int]
    } finally arena.close()
  }

  private lazy val _glBindProgramPipeline =
    h31("glBindProgramPipeline", FunctionDescriptor.ofVoid(I31))
  override def glBindProgramPipeline(pipeline: Int): Unit =
    _glBindProgramPipeline.invoke(pipeline)

  private lazy val _glDeleteProgramPipelines =
    h31("glDeleteProgramPipelines", FunctionDescriptor.ofVoid(I31, P31))
  override def glDeleteProgramPipelines(n: Int, pipelines: IntBuffer): Unit =
    _glDeleteProgramPipelines.invoke(n, bufAddr31(pipelines))

  private lazy val _glGenProgramPipelines =
    h31("glGenProgramPipelines", FunctionDescriptor.ofVoid(I31, P31))
  override def glGenProgramPipelines(n: Int, pipelines: IntBuffer): Unit =
    _glGenProgramPipelines.invoke(n, bufAddr31(pipelines))

  private lazy val _glIsProgramPipeline =
    h31("glIsProgramPipeline", FunctionDescriptor.of(B31, I31))
  override def glIsProgramPipeline(pipeline: Int): Boolean =
    fromGlBool31(_glIsProgramPipeline.invoke(pipeline))

  private lazy val _glGetProgramPipelineiv =
    h31("glGetProgramPipelineiv", FunctionDescriptor.ofVoid(I31, I31, P31))
  override def glGetProgramPipelineiv(pipeline: Int, pname: Int, params: IntBuffer): Unit =
    _glGetProgramPipelineiv.invoke(pipeline, pname, bufAddr31(params))

  // ─── Program uniforms (scalar) ────────────────────────────────────────────

  private lazy val _glProgramUniform1i =
    h31("glProgramUniform1i", FunctionDescriptor.ofVoid(I31, I31, I31))
  override def glProgramUniform1i(program: Int, location: Int, v0: Int): Unit =
    _glProgramUniform1i.invoke(program, location, v0)

  private lazy val _glProgramUniform2i =
    h31("glProgramUniform2i", FunctionDescriptor.ofVoid(I31, I31, I31, I31))
  override def glProgramUniform2i(program: Int, location: Int, v0: Int, v1: Int): Unit =
    _glProgramUniform2i.invoke(program, location, v0, v1)

  private lazy val _glProgramUniform3i =
    h31("glProgramUniform3i", FunctionDescriptor.ofVoid(I31, I31, I31, I31, I31))
  override def glProgramUniform3i(program: Int, location: Int, v0: Int, v1: Int, v2: Int): Unit =
    _glProgramUniform3i.invoke(program, location, v0, v1, v2)

  private lazy val _glProgramUniform4i =
    h31("glProgramUniform4i", FunctionDescriptor.ofVoid(I31, I31, I31, I31, I31, I31))
  override def glProgramUniform4i(program: Int, location: Int, v0: Int, v1: Int, v2: Int, v3: Int): Unit =
    _glProgramUniform4i.invoke(program, location, v0, v1, v2, v3)

  private lazy val _glProgramUniform1ui =
    h31("glProgramUniform1ui", FunctionDescriptor.ofVoid(I31, I31, I31))
  override def glProgramUniform1ui(program: Int, location: Int, v0: Int): Unit =
    _glProgramUniform1ui.invoke(program, location, v0)

  private lazy val _glProgramUniform2ui =
    h31("glProgramUniform2ui", FunctionDescriptor.ofVoid(I31, I31, I31, I31))
  override def glProgramUniform2ui(program: Int, location: Int, v0: Int, v1: Int): Unit =
    _glProgramUniform2ui.invoke(program, location, v0, v1)

  private lazy val _glProgramUniform3ui =
    h31("glProgramUniform3ui", FunctionDescriptor.ofVoid(I31, I31, I31, I31, I31))
  override def glProgramUniform3ui(program: Int, location: Int, v0: Int, v1: Int, v2: Int): Unit =
    _glProgramUniform3ui.invoke(program, location, v0, v1, v2)

  private lazy val _glProgramUniform4ui =
    h31("glProgramUniform4ui", FunctionDescriptor.ofVoid(I31, I31, I31, I31, I31, I31))
  override def glProgramUniform4ui(program: Int, location: Int, v0: Int, v1: Int, v2: Int, v3: Int): Unit =
    _glProgramUniform4ui.invoke(program, location, v0, v1, v2, v3)

  private lazy val _glProgramUniform1f =
    h31("glProgramUniform1f", FunctionDescriptor.ofVoid(I31, I31, F31))
  override def glProgramUniform1f(program: Int, location: Int, v0: Float): Unit =
    _glProgramUniform1f.invoke(program, location, v0)

  private lazy val _glProgramUniform2f =
    h31("glProgramUniform2f", FunctionDescriptor.ofVoid(I31, I31, F31, F31))
  override def glProgramUniform2f(program: Int, location: Int, v0: Float, v1: Float): Unit =
    _glProgramUniform2f.invoke(program, location, v0, v1)

  private lazy val _glProgramUniform3f =
    h31("glProgramUniform3f", FunctionDescriptor.ofVoid(I31, I31, F31, F31, F31))
  override def glProgramUniform3f(program: Int, location: Int, v0: Float, v1: Float, v2: Float): Unit =
    _glProgramUniform3f.invoke(program, location, v0, v1, v2)

  private lazy val _glProgramUniform4f =
    h31("glProgramUniform4f", FunctionDescriptor.ofVoid(I31, I31, F31, F31, F31, F31))
  override def glProgramUniform4f(program: Int, location: Int, v0: Float, v1: Float, v2: Float, v3: Float): Unit =
    _glProgramUniform4f.invoke(program, location, v0, v1, v2, v3)

  // ─── Program uniforms (buffer/vector) ─────────────────────────────────────

  // glProgramUniform*iv: void (GLuint program, GLint location, GLsizei count, const GLint *value)
  // The buffer's remaining() gives count (adjusted by component count for 2iv/3iv/4iv)

  private lazy val _glProgramUniform1iv =
    h31("glProgramUniform1iv", FunctionDescriptor.ofVoid(I31, I31, I31, P31))
  override def glProgramUniform1iv(program: Int, location: Int, value: IntBuffer): Unit =
    _glProgramUniform1iv.invoke(program, location, value.remaining(), bufAddr31(value))

  private lazy val _glProgramUniform2iv =
    h31("glProgramUniform2iv", FunctionDescriptor.ofVoid(I31, I31, I31, P31))
  override def glProgramUniform2iv(program: Int, location: Int, value: IntBuffer): Unit =
    _glProgramUniform2iv.invoke(program, location, value.remaining() / 2, bufAddr31(value))

  private lazy val _glProgramUniform3iv =
    h31("glProgramUniform3iv", FunctionDescriptor.ofVoid(I31, I31, I31, P31))
  override def glProgramUniform3iv(program: Int, location: Int, value: IntBuffer): Unit =
    _glProgramUniform3iv.invoke(program, location, value.remaining() / 3, bufAddr31(value))

  private lazy val _glProgramUniform4iv =
    h31("glProgramUniform4iv", FunctionDescriptor.ofVoid(I31, I31, I31, P31))
  override def glProgramUniform4iv(program: Int, location: Int, value: IntBuffer): Unit =
    _glProgramUniform4iv.invoke(program, location, value.remaining() / 4, bufAddr31(value))

  private lazy val _glProgramUniform1uiv =
    h31("glProgramUniform1uiv", FunctionDescriptor.ofVoid(I31, I31, I31, P31))
  override def glProgramUniform1uiv(program: Int, location: Int, value: IntBuffer): Unit =
    _glProgramUniform1uiv.invoke(program, location, value.remaining(), bufAddr31(value))

  private lazy val _glProgramUniform2uiv =
    h31("glProgramUniform2uiv", FunctionDescriptor.ofVoid(I31, I31, I31, P31))
  override def glProgramUniform2uiv(program: Int, location: Int, value: IntBuffer): Unit =
    _glProgramUniform2uiv.invoke(program, location, value.remaining() / 2, bufAddr31(value))

  private lazy val _glProgramUniform3uiv =
    h31("glProgramUniform3uiv", FunctionDescriptor.ofVoid(I31, I31, I31, P31))
  override def glProgramUniform3uiv(program: Int, location: Int, value: IntBuffer): Unit =
    _glProgramUniform3uiv.invoke(program, location, value.remaining() / 3, bufAddr31(value))

  private lazy val _glProgramUniform4uiv =
    h31("glProgramUniform4uiv", FunctionDescriptor.ofVoid(I31, I31, I31, P31))
  override def glProgramUniform4uiv(program: Int, location: Int, value: IntBuffer): Unit =
    _glProgramUniform4uiv.invoke(program, location, value.remaining() / 4, bufAddr31(value))

  private lazy val _glProgramUniform1fv =
    h31("glProgramUniform1fv", FunctionDescriptor.ofVoid(I31, I31, I31, P31))
  override def glProgramUniform1fv(program: Int, location: Int, value: FloatBuffer): Unit =
    _glProgramUniform1fv.invoke(program, location, value.remaining(), bufAddr31(value))

  private lazy val _glProgramUniform2fv =
    h31("glProgramUniform2fv", FunctionDescriptor.ofVoid(I31, I31, I31, P31))
  override def glProgramUniform2fv(program: Int, location: Int, value: FloatBuffer): Unit =
    _glProgramUniform2fv.invoke(program, location, value.remaining() / 2, bufAddr31(value))

  private lazy val _glProgramUniform3fv =
    h31("glProgramUniform3fv", FunctionDescriptor.ofVoid(I31, I31, I31, P31))
  override def glProgramUniform3fv(program: Int, location: Int, value: FloatBuffer): Unit =
    _glProgramUniform3fv.invoke(program, location, value.remaining() / 3, bufAddr31(value))

  private lazy val _glProgramUniform4fv =
    h31("glProgramUniform4fv", FunctionDescriptor.ofVoid(I31, I31, I31, P31))
  override def glProgramUniform4fv(program: Int, location: Int, value: FloatBuffer): Unit =
    _glProgramUniform4fv.invoke(program, location, value.remaining() / 4, bufAddr31(value))

  // ─── Program uniform matrices ─────────────────────────────────────────────

  // glProgramUniformMatrix*fv: void (GLuint program, GLint location, GLsizei count, GLboolean transpose, const GLfloat *value)

  private lazy val _glProgramUniformMatrix2fv =
    h31("glProgramUniformMatrix2fv", FunctionDescriptor.ofVoid(I31, I31, I31, B31, P31))
  override def glProgramUniformMatrix2fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit =
    _glProgramUniformMatrix2fv.invoke(program, location, value.remaining() / 4, glBool31(transpose), bufAddr31(value))

  private lazy val _glProgramUniformMatrix3fv =
    h31("glProgramUniformMatrix3fv", FunctionDescriptor.ofVoid(I31, I31, I31, B31, P31))
  override def glProgramUniformMatrix3fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit =
    _glProgramUniformMatrix3fv.invoke(program, location, value.remaining() / 9, glBool31(transpose), bufAddr31(value))

  private lazy val _glProgramUniformMatrix4fv =
    h31("glProgramUniformMatrix4fv", FunctionDescriptor.ofVoid(I31, I31, I31, B31, P31))
  override def glProgramUniformMatrix4fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit =
    _glProgramUniformMatrix4fv.invoke(program, location, value.remaining() / 16, glBool31(transpose), bufAddr31(value))

  private lazy val _glProgramUniformMatrix2x3fv =
    h31("glProgramUniformMatrix2x3fv", FunctionDescriptor.ofVoid(I31, I31, I31, B31, P31))
  override def glProgramUniformMatrix2x3fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit =
    _glProgramUniformMatrix2x3fv.invoke(program, location, value.remaining() / 6, glBool31(transpose), bufAddr31(value))

  private lazy val _glProgramUniformMatrix3x2fv =
    h31("glProgramUniformMatrix3x2fv", FunctionDescriptor.ofVoid(I31, I31, I31, B31, P31))
  override def glProgramUniformMatrix3x2fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit =
    _glProgramUniformMatrix3x2fv.invoke(program, location, value.remaining() / 6, glBool31(transpose), bufAddr31(value))

  private lazy val _glProgramUniformMatrix2x4fv =
    h31("glProgramUniformMatrix2x4fv", FunctionDescriptor.ofVoid(I31, I31, I31, B31, P31))
  override def glProgramUniformMatrix2x4fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit =
    _glProgramUniformMatrix2x4fv.invoke(program, location, value.remaining() / 8, glBool31(transpose), bufAddr31(value))

  private lazy val _glProgramUniformMatrix4x2fv =
    h31("glProgramUniformMatrix4x2fv", FunctionDescriptor.ofVoid(I31, I31, I31, B31, P31))
  override def glProgramUniformMatrix4x2fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit =
    _glProgramUniformMatrix4x2fv.invoke(program, location, value.remaining() / 8, glBool31(transpose), bufAddr31(value))

  private lazy val _glProgramUniformMatrix3x4fv =
    h31("glProgramUniformMatrix3x4fv", FunctionDescriptor.ofVoid(I31, I31, I31, B31, P31))
  override def glProgramUniformMatrix3x4fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit =
    _glProgramUniformMatrix3x4fv.invoke(program, location, value.remaining() / 12, glBool31(transpose), bufAddr31(value))

  private lazy val _glProgramUniformMatrix4x3fv =
    h31("glProgramUniformMatrix4x3fv", FunctionDescriptor.ofVoid(I31, I31, I31, B31, P31))
  override def glProgramUniformMatrix4x3fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit =
    _glProgramUniformMatrix4x3fv.invoke(program, location, value.remaining() / 12, glBool31(transpose), bufAddr31(value))

  // ─── Pipeline validation ──────────────────────────────────────────────────

  private lazy val _glValidateProgramPipeline =
    h31("glValidateProgramPipeline", FunctionDescriptor.ofVoid(I31))
  override def glValidateProgramPipeline(pipeline: Int): Unit =
    _glValidateProgramPipeline.invoke(pipeline)

  private lazy val _glGetProgramPipelineInfoLog =
    h31("glGetProgramPipelineInfoLog", FunctionDescriptor.ofVoid(I31, I31, P31, P31))
  override def glGetProgramPipelineInfoLog(program: Int): String = {
    val arena = Arena.ofConfined()
    try {
      val maxLen = 10240
      val logBuf = arena.allocate(maxLen.toLong)
      val lenBuf = arena.allocate(I31)
      _glGetProgramPipelineInfoLog.invoke(program, maxLen, lenBuf, logBuf)
      val len = lenBuf.get(I31, 0L)
      if (len <= 0) ""
      else logBuf.reinterpret(len.toLong + 1).getString(0)
    } finally arena.close()
  }

  // ─── Image textures ───────────────────────────────────────────────────────

  private lazy val _glBindImageTexture =
    h31("glBindImageTexture", FunctionDescriptor.ofVoid(I31, I31, I31, B31, I31, I31, I31))
  override def glBindImageTexture(
    unit:    Int,
    texture: Int,
    level:   Int,
    layered: Boolean,
    layer:   Int,
    access:  Int,
    format:  Int
  ): Unit =
    _glBindImageTexture.invoke(unit, texture, level, glBool31(layered), layer, access, format)

  // ─── Boolean indexed queries ──────────────────────────────────────────────

  // GLES glGetBooleani_v writes GLboolean* (bytes). We read into a temp byte buffer and convert to IntBuffer.
  private lazy val _glGetBooleani_v =
    h31("glGetBooleani_v", FunctionDescriptor.ofVoid(I31, I31, P31))
  override def glGetBooleani_v(target: Int, index: Int, data: IntBuffer): Unit = {
    val arena = Arena.ofConfined()
    try {
      val tmpBuf = arena.allocate(16L)
      _glGetBooleani_v.invoke(target, index, tmpBuf)
      val remaining = scala.math.min(data.remaining(), 16)
      var i         = 0
      while (i < remaining) {
        data.put(if (tmpBuf.get(B31, i.toLong) != 0) 1 else 0)
        i += 1
      }
    } finally arena.close()
  }

  // ─── Memory barriers ─────────────────────────────────────────────────────

  private lazy val _glMemoryBarrier =
    h31("glMemoryBarrier", FunctionDescriptor.ofVoid(I31))
  override def glMemoryBarrier(barriers: Int): Unit =
    _glMemoryBarrier.invoke(barriers)

  private lazy val _glMemoryBarrierByRegion =
    h31("glMemoryBarrierByRegion", FunctionDescriptor.ofVoid(I31))
  override def glMemoryBarrierByRegion(barriers: Int): Unit =
    _glMemoryBarrierByRegion.invoke(barriers)

  // ─── Multisample textures ─────────────────────────────────────────────────

  private lazy val _glTexStorage2DMultisample =
    h31("glTexStorage2DMultisample", FunctionDescriptor.ofVoid(I31, I31, I31, I31, I31, B31))
  override def glTexStorage2DMultisample(
    target:               TextureTarget,
    samples:              Int,
    internalformat:       Int,
    width:                Int,
    height:               Int,
    fixedsamplelocations: Boolean
  ): Unit =
    _glTexStorage2DMultisample.invoke(target, samples, internalformat, width, height, glBool31(fixedsamplelocations))

  private lazy val _glGetMultisamplefv =
    h31("glGetMultisamplefv", FunctionDescriptor.ofVoid(I31, I31, P31))
  override def glGetMultisamplefv(pname: Int, index: Int, value: FloatBuffer): Unit =
    _glGetMultisamplefv.invoke(pname, index, bufAddr31(value))

  private lazy val _glSampleMaski =
    h31("glSampleMaski", FunctionDescriptor.ofVoid(I31, I31))
  override def glSampleMaski(maskNumber: Int, mask: Int): Unit =
    _glSampleMaski.invoke(maskNumber, mask)

  // ─── Texture level parameters ─────────────────────────────────────────────

  private lazy val _glGetTexLevelParameteriv =
    h31("glGetTexLevelParameteriv", FunctionDescriptor.ofVoid(I31, I31, I31, P31))
  override def glGetTexLevelParameteriv(target: TextureTarget, level: Int, pname: Int, params: IntBuffer): Unit =
    _glGetTexLevelParameteriv.invoke(target, level, pname, bufAddr31(params))

  private lazy val _glGetTexLevelParameterfv =
    h31("glGetTexLevelParameterfv", FunctionDescriptor.ofVoid(I31, I31, I31, P31))
  override def glGetTexLevelParameterfv(target: TextureTarget, level: Int, pname: Int, params: FloatBuffer): Unit =
    _glGetTexLevelParameterfv.invoke(target, level, pname, bufAddr31(params))

  // ─── Vertex attribute binding ─────────────────────────────────────────────

  private lazy val _glBindVertexBuffer =
    h31("glBindVertexBuffer", FunctionDescriptor.ofVoid(I31, I31, L31, I31))
  override def glBindVertexBuffer(bindingindex: Int, buffer: Int, offset: Long, stride: Int): Unit =
    _glBindVertexBuffer.invoke(bindingindex, buffer, offset, stride)

  private lazy val _glVertexAttribFormat =
    h31("glVertexAttribFormat", FunctionDescriptor.ofVoid(I31, I31, I31, B31, I31))
  override def glVertexAttribFormat(
    attribindex:    Int,
    size:           Int,
    `type`:         DataType,
    normalized:     Boolean,
    relativeoffset: Int
  ): Unit =
    _glVertexAttribFormat.invoke(attribindex, size, `type`, glBool31(normalized), relativeoffset)

  private lazy val _glVertexAttribIFormat =
    h31("glVertexAttribIFormat", FunctionDescriptor.ofVoid(I31, I31, I31, I31))
  override def glVertexAttribIFormat(attribindex: Int, size: Int, `type`: DataType, relativeoffset: Int): Unit =
    _glVertexAttribIFormat.invoke(attribindex, size, `type`, relativeoffset)

  private lazy val _glVertexAttribBinding =
    h31("glVertexAttribBinding", FunctionDescriptor.ofVoid(I31, I31))
  override def glVertexAttribBinding(attribindex: Int, bindingindex: Int): Unit =
    _glVertexAttribBinding.invoke(attribindex, bindingindex)

  private lazy val _glVertexBindingDivisor =
    h31("glVertexBindingDivisor", FunctionDescriptor.ofVoid(I31, I31))
  override def glVertexBindingDivisor(bindingindex: Int, divisor: Int): Unit =
    _glVertexBindingDivisor.invoke(bindingindex, divisor)
}
