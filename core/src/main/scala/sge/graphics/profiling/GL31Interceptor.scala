/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/profiling/GL31Interceptor.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package profiling

import java.nio.{ FloatBuffer, IntBuffer }

class GL31Interceptor(glProfiler: GLProfiler, val gl31: GL31) extends GL30Interceptor(glProfiler, gl31) with GL31 {

  override protected def check(): Unit = {
    var error = gl30.glGetError()
    while (error != GL20.GL_NO_ERROR) {
      glProfiler.getListener().onError(error)
      error = gl30.glGetError()
    }
  }

  override def glDispatchCompute(num_groups_x: Int, num_groups_y: Int, num_groups_z: Int): Unit = {
    calls += 1
    gl31.glDispatchCompute(num_groups_x, num_groups_y, num_groups_z)
    check()
  }

  override def glDispatchComputeIndirect(indirect: Long): Unit = {
    calls += 1
    gl31.glDispatchComputeIndirect(indirect)
    check()
  }

  override def glDrawArraysIndirect(mode: Int, indirect: Long): Unit = {
    drawCalls += 1
    calls += 1
    gl31.glDrawArraysIndirect(mode, indirect)
    check()
  }

  override def glDrawElementsIndirect(mode: Int, `type`: Int, indirect: Long): Unit = {
    drawCalls += 1
    calls += 1
    gl31.glDrawElementsIndirect(mode, `type`, indirect)
    check()
  }

  override def glFramebufferParameteri(target: Int, pname: Int, param: Int): Unit = {
    calls += 1
    gl31.glFramebufferParameteri(target, pname, param)
    check()
  }

  override def glGetFramebufferParameteriv(target: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl31.glGetFramebufferParameteriv(target, pname, params)
    check()
  }

  override def glGetProgramInterfaceiv(program: Int, programInterface: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl31.glGetProgramInterfaceiv(program, programInterface, pname, params)
    check()
  }

  override def glGetProgramResourceIndex(program: Int, programInterface: Int, name: String): Int = {
    calls += 1
    val v = gl31.glGetProgramResourceIndex(program, programInterface, name)
    check()
    v
  }

  override def glGetProgramResourceName(program: Int, programInterface: Int, index: Int): String = {
    calls += 1
    val s = gl31.glGetProgramResourceName(program, programInterface, index)
    check()
    s
  }

  override def glGetProgramResourceiv(program: Int, programInterface: Int, index: Int, props: IntBuffer, length: IntBuffer, params: IntBuffer): Unit = {
    calls += 1
    gl31.glGetProgramResourceiv(program, programInterface, index, props, length, params)
    check()
  }

  override def glGetProgramResourceLocation(program: Int, programInterface: Int, name: String): Int = {
    calls += 1
    val v = gl31.glGetProgramResourceLocation(program, programInterface, name)
    check()
    v
  }

  override def glUseProgramStages(pipeline: Int, stages: Int, program: Int): Unit = {
    calls += 1
    gl31.glUseProgramStages(pipeline, stages, program)
    check()
  }

  override def glActiveShaderProgram(pipeline: Int, program: Int): Unit = {
    calls += 1
    gl31.glActiveShaderProgram(pipeline, program)
    check()
  }

  override def glCreateShaderProgramv(`type`: Int, strings: Array[String]): Int = {
    calls += 1
    val v = gl31.glCreateShaderProgramv(`type`, strings)
    check()
    v
  }

  override def glBindProgramPipeline(pipeline: Int): Unit = {
    calls += 1
    gl31.glBindProgramPipeline(pipeline)
    check()
  }

  override def glDeleteProgramPipelines(count: Int, pipelines: IntBuffer): Unit = {
    calls += 1
    gl31.glDeleteProgramPipelines(count, pipelines)
    check()
  }

  override def glGenProgramPipelines(count: Int, pipelines: IntBuffer): Unit = {
    calls += 1
    gl31.glGenProgramPipelines(count, pipelines)
    check()
  }

  override def glIsProgramPipeline(pipeline: Int): Boolean = {
    calls += 1
    val v = gl31.glIsProgramPipeline(pipeline)
    check()
    v
  }

  override def glGetProgramPipelineiv(pipeline: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl31.glGetProgramPipelineiv(pipeline, pname, params)
    check()
  }

  override def glProgramUniform1i(program: Int, location: Int, v0: Int): Unit = {
    calls += 1
    gl31.glProgramUniform1i(program, location, v0)
    check()
  }

  override def glProgramUniform2i(program: Int, location: Int, v0: Int, v1: Int): Unit = {
    calls += 1
    gl31.glProgramUniform2i(program, location, v0, v1)
    check()
  }

  override def glProgramUniform3i(program: Int, location: Int, v0: Int, v1: Int, v2: Int): Unit = {
    calls += 1
    gl31.glProgramUniform3i(program, location, v0, v1, v2)
    check()
  }

  override def glProgramUniform4i(program: Int, location: Int, v0: Int, v1: Int, v2: Int, v3: Int): Unit = {
    calls += 1
    gl31.glProgramUniform4i(program, location, v0, v1, v2, v3)
    check()
  }

  override def glProgramUniform1ui(program: Int, location: Int, v0: Int): Unit = {
    calls += 1
    gl31.glProgramUniform1ui(program, location, v0)
    check()
  }

  override def glProgramUniform2ui(program: Int, location: Int, v0: Int, v1: Int): Unit = {
    calls += 1
    gl31.glProgramUniform2ui(program, location, v0, v1)
    check()
  }

  override def glProgramUniform3ui(program: Int, location: Int, v0: Int, v1: Int, v2: Int): Unit = {
    calls += 1
    gl31.glProgramUniform3ui(program, location, v0, v1, v2)
    check()
  }

  override def glProgramUniform4ui(program: Int, location: Int, v0: Int, v1: Int, v2: Int, v3: Int): Unit = {
    calls += 1
    gl31.glProgramUniform4ui(program, location, v0, v1, v2, v3)
    check()
  }

  override def glProgramUniform1f(program: Int, location: Int, v0: Float): Unit = {
    calls += 1
    gl31.glProgramUniform1f(program, location, v0)
    check()
  }

  override def glProgramUniform2f(program: Int, location: Int, v0: Float, v1: Float): Unit = {
    calls += 1
    gl31.glProgramUniform2f(program, location, v0, v1)
    check()
  }

  override def glProgramUniform3f(program: Int, location: Int, v0: Float, v1: Float, v2: Float): Unit = {
    calls += 1
    gl31.glProgramUniform3f(program, location, v0, v1, v2)
    check()
  }

  override def glProgramUniform4f(program: Int, location: Int, v0: Float, v1: Float, v2: Float, v3: Float): Unit = {
    calls += 1
    gl31.glProgramUniform4f(program, location, v0, v1, v2, v3)
    check()
  }

  override def glProgramUniform1iv(program: Int, location: Int, value: IntBuffer): Unit = {
    calls += 1
    gl31.glProgramUniform1iv(program, location, value)
    check()
  }

  override def glProgramUniform2iv(program: Int, location: Int, value: IntBuffer): Unit = {
    calls += 1
    gl31.glProgramUniform2iv(program, location, value)
    check()
  }

  override def glProgramUniform3iv(program: Int, location: Int, value: IntBuffer): Unit = {
    calls += 1
    gl31.glProgramUniform3iv(program, location, value)
    check()
  }

  override def glProgramUniform4iv(program: Int, location: Int, value: IntBuffer): Unit = {
    calls += 1
    gl31.glProgramUniform4iv(program, location, value)
    check()
  }

  override def glProgramUniform1uiv(program: Int, location: Int, value: IntBuffer): Unit = {
    calls += 1
    gl31.glProgramUniform1uiv(program, location, value)
    check()
  }

  override def glProgramUniform2uiv(program: Int, location: Int, value: IntBuffer): Unit = {
    calls += 1
    gl31.glProgramUniform2uiv(program, location, value)
    check()
  }

  override def glProgramUniform3uiv(program: Int, location: Int, value: IntBuffer): Unit = {
    calls += 1
    gl31.glProgramUniform3uiv(program, location, value)
    check()
  }

  override def glProgramUniform4uiv(program: Int, location: Int, value: IntBuffer): Unit = {
    calls += 1
    gl31.glProgramUniform4uiv(program, location, value)
    check()
  }

  override def glProgramUniform1fv(program: Int, location: Int, value: FloatBuffer): Unit = {
    calls += 1
    gl31.glProgramUniform1fv(program, location, value)
    check()
  }

  override def glProgramUniform2fv(program: Int, location: Int, value: FloatBuffer): Unit = {
    calls += 1
    gl31.glProgramUniform2fv(program, location, value)
    check()
  }

  override def glProgramUniform3fv(program: Int, location: Int, value: FloatBuffer): Unit = {
    calls += 1
    gl31.glProgramUniform3fv(program, location, value)
    check()
  }

  override def glProgramUniform4fv(program: Int, location: Int, value: FloatBuffer): Unit = {
    calls += 1
    gl31.glProgramUniform4fv(program, location, value)
    check()
  }

  override def glProgramUniformMatrix2fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit = {
    calls += 1
    gl31.glProgramUniformMatrix2fv(program, location, transpose, value)
    check()
  }

  override def glProgramUniformMatrix3fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit = {
    calls += 1
    gl31.glProgramUniformMatrix3fv(program, location, transpose, value)
    check()
  }

  override def glProgramUniformMatrix4fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit = {
    calls += 1
    gl31.glProgramUniformMatrix4fv(program, location, transpose, value)
    check()
  }

  override def glProgramUniformMatrix2x3fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit = {
    calls += 1
    gl31.glProgramUniformMatrix2x3fv(program, location, transpose, value)
    check()
  }

  override def glProgramUniformMatrix3x2fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit = {
    calls += 1
    gl31.glProgramUniformMatrix3x2fv(program, location, transpose, value)
    check()
  }

  override def glProgramUniformMatrix2x4fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit = {
    calls += 1
    gl31.glProgramUniformMatrix2x4fv(program, location, transpose, value)
    check()
  }

  override def glProgramUniformMatrix4x2fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit = {
    calls += 1
    gl31.glProgramUniformMatrix4x2fv(program, location, transpose, value)
    check()
  }

  override def glProgramUniformMatrix3x4fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit = {
    calls += 1
    gl31.glProgramUniformMatrix3x4fv(program, location, transpose, value)
    check()
  }

  override def glProgramUniformMatrix4x3fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit = {
    calls += 1
    gl31.glProgramUniformMatrix4x3fv(program, location, transpose, value)
    check()
  }

  override def glValidateProgramPipeline(pipeline: Int): Unit = {
    calls += 1
    gl31.glValidateProgramPipeline(pipeline)
    check()
  }

  override def glGetProgramPipelineInfoLog(program: Int): String = {
    calls += 1
    val s = gl31.glGetProgramPipelineInfoLog(program)
    check()
    s
  }

  override def glBindImageTexture(unit: Int, texture: Int, level: Int, layered: Boolean, layer: Int, access: Int, format: Int): Unit = {
    calls += 1
    gl31.glBindImageTexture(unit, texture, level, layered, layer, access, format)
    check()
  }

  override def glGetBooleani_v(target: Int, index: Int, data: IntBuffer): Unit = {
    calls += 1
    gl31.glGetBooleani_v(target, index, data)
    check()
  }

  override def glMemoryBarrier(barriers: Int): Unit = {
    calls += 1
    gl31.glMemoryBarrier(barriers)
    check()
  }

  override def glMemoryBarrierByRegion(barriers: Int): Unit = {
    calls += 1
    gl31.glMemoryBarrierByRegion(barriers)
    check()
  }

  override def glTexStorage2DMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int, fixedsamplelocations: Boolean): Unit = {
    calls += 1
    gl31.glTexStorage2DMultisample(target, samples, internalformat, width, height, fixedsamplelocations)
    check()
  }

  override def glGetMultisamplefv(pname: Int, index: Int, `val`: FloatBuffer): Unit = {
    calls += 1
    gl31.glGetMultisamplefv(pname, index, `val`)
    check()
  }

  override def glSampleMaski(maskNumber: Int, mask: Int): Unit = {
    calls += 1
    gl31.glSampleMaski(maskNumber, mask)
    check()
  }

  override def glGetTexLevelParameteriv(target: Int, level: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl31.glGetTexLevelParameteriv(target, level, pname, params)
    check()
  }

  override def glGetTexLevelParameterfv(target: Int, level: Int, pname: Int, params: FloatBuffer): Unit = {
    calls += 1
    gl31.glGetTexLevelParameterfv(target, level, pname, params)
    check()
  }

  override def glBindVertexBuffer(bindingindex: Int, buffer: Int, offset: Long, stride: Int): Unit = {
    calls += 1
    gl31.glBindVertexBuffer(bindingindex, buffer, offset, stride)
    check()
  }

  override def glVertexAttribFormat(attribindex: Int, size: Int, `type`: Int, normalized: Boolean, relativeoffset: Int): Unit = {
    calls += 1
    gl31.glVertexAttribFormat(attribindex, size, `type`, normalized, relativeoffset)
    check()
  }

  override def glVertexAttribIFormat(attribindex: Int, size: Int, `type`: Int, relativeoffset: Int): Unit = {
    calls += 1
    gl31.glVertexAttribIFormat(attribindex, size, `type`, relativeoffset)
    check()
  }

  override def glVertexAttribBinding(attribindex: Int, bindingindex: Int): Unit = {
    calls += 1
    gl31.glVertexAttribBinding(attribindex, bindingindex)
    check()
  }

  override def glVertexBindingDivisor(bindingindex: Int, divisor: Int): Unit = {
    calls += 1
    gl31.glVertexBindingDivisor(bindingindex, divisor)
    check()
  }
}
