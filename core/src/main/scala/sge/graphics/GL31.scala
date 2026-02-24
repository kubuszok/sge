/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/GL31.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics

import java.nio.{ Buffer, FloatBuffer, IntBuffer, LongBuffer }

trait GL31 extends GL30 {

  def glDispatchCompute(num_groups_x: Int, num_groups_y: Int, num_groups_z: Int): Unit

  def glDispatchComputeIndirect(indirect: Long): Unit

  def glDrawArraysIndirect(mode: Int, indirect: Long): Unit

  def glDrawElementsIndirect(mode: Int, `type`: Int, indirect: Long): Unit

  def glFramebufferParameteri(target: Int, pname: Int, param: Int): Unit

  def glGetFramebufferParameteriv(target: Int, pname: Int, params: IntBuffer): Unit

  def glGetProgramInterfaceiv(program: Int, programInterface: Int, pname: Int, params: IntBuffer): Unit

  def glGetProgramResourceIndex(program: Int, programInterface: Int, name: String): Int

  def glGetProgramResourceName(program: Int, programInterface: Int, index: Int): String

  def glGetProgramResourceiv(program: Int, programInterface: Int, index: Int, props: IntBuffer, length: IntBuffer, params: IntBuffer): Unit

  def glGetProgramResourceLocation(program: Int, programInterface: Int, name: String): Int

  def glUseProgramStages(pipeline: Int, stages: Int, program: Int): Unit

  def glActiveShaderProgram(pipeline: Int, program: Int): Unit

  def glCreateShaderProgramv(`type`: Int, strings: Array[String]): Int

  def glBindProgramPipeline(pipeline: Int): Unit

  def glDeleteProgramPipelines(n: Int, pipelines: IntBuffer): Unit

  def glGenProgramPipelines(n: Int, pipelines: IntBuffer): Unit

  def glIsProgramPipeline(pipeline: Int): Boolean

  def glGetProgramPipelineiv(pipeline: Int, pname: Int, params: IntBuffer): Unit

  def glProgramUniform1i(program: Int, location: Int, v0: Int): Unit

  def glProgramUniform2i(program: Int, location: Int, v0: Int, v1: Int): Unit

  def glProgramUniform3i(program: Int, location: Int, v0: Int, v1: Int, v2: Int): Unit

  def glProgramUniform4i(program: Int, location: Int, v0: Int, v1: Int, v2: Int, v3: Int): Unit

  def glProgramUniform1ui(program: Int, location: Int, v0: Int): Unit

  def glProgramUniform2ui(program: Int, location: Int, v0: Int, v1: Int): Unit

  def glProgramUniform3ui(program: Int, location: Int, v0: Int, v1: Int, v2: Int): Unit

  def glProgramUniform4ui(program: Int, location: Int, v0: Int, v1: Int, v2: Int, v3: Int): Unit

  def glProgramUniform1f(program: Int, location: Int, v0: Float): Unit

  def glProgramUniform2f(program: Int, location: Int, v0: Float, v1: Float): Unit

  def glProgramUniform3f(program: Int, location: Int, v0: Float, v1: Float, v2: Float): Unit

  def glProgramUniform4f(program: Int, location: Int, v0: Float, v1: Float, v2: Float, v3: Float): Unit

  def glProgramUniform1iv(program: Int, location: Int, value: IntBuffer): Unit

  def glProgramUniform2iv(program: Int, location: Int, value: IntBuffer): Unit

  def glProgramUniform3iv(program: Int, location: Int, value: IntBuffer): Unit

  def glProgramUniform4iv(program: Int, location: Int, value: IntBuffer): Unit

  def glProgramUniform1uiv(program: Int, location: Int, value: IntBuffer): Unit

  def glProgramUniform2uiv(program: Int, location: Int, value: IntBuffer): Unit

  def glProgramUniform3uiv(program: Int, location: Int, value: IntBuffer): Unit

  def glProgramUniform4uiv(program: Int, location: Int, value: IntBuffer): Unit

  def glProgramUniform1fv(program: Int, location: Int, value: FloatBuffer): Unit

  def glProgramUniform2fv(program: Int, location: Int, value: FloatBuffer): Unit

  def glProgramUniform3fv(program: Int, location: Int, value: FloatBuffer): Unit

  def glProgramUniform4fv(program: Int, location: Int, value: FloatBuffer): Unit

  def glProgramUniformMatrix2fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glProgramUniformMatrix3fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glProgramUniformMatrix4fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glProgramUniformMatrix2x3fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glProgramUniformMatrix3x2fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glProgramUniformMatrix2x4fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glProgramUniformMatrix4x2fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glProgramUniformMatrix3x4fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glProgramUniformMatrix4x3fv(program: Int, location: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glValidateProgramPipeline(pipeline: Int): Unit

  def glGetProgramPipelineInfoLog(program: Int): String

  def glBindImageTexture(unit: Int, texture: Int, level: Int, layered: Boolean, layer: Int, access: Int, format: Int): Unit

  def glGetBooleani_v(target: Int, index: Int, data: IntBuffer): Unit

  def glMemoryBarrier(barriers: Int): Unit

  def glMemoryBarrierByRegion(barriers: Int): Unit

  def glTexStorage2DMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int, fixedsamplelocations: Boolean): Unit

  def glGetMultisamplefv(pname: Int, index: Int, value: FloatBuffer): Unit

  def glSampleMaski(maskNumber: Int, mask: Int): Unit

  def glGetTexLevelParameteriv(target: Int, level: Int, pname: Int, params: IntBuffer): Unit

  def glGetTexLevelParameterfv(target: Int, level: Int, pname: Int, params: FloatBuffer): Unit

  def glBindVertexBuffer(bindingindex: Int, buffer: Int, offset: Long, stride: Int): Unit

  def glVertexAttribFormat(attribindex: Int, size: Int, `type`: Int, normalized: Boolean, relativeoffset: Int): Unit

  def glVertexAttribIFormat(attribindex: Int, size: Int, `type`: Int, relativeoffset: Int): Unit

  def glVertexAttribBinding(attribindex: Int, bindingindex: Int): Unit

  def glVertexBindingDivisor(bindingindex: Int, divisor: Int): Unit
}

object GL31 {
  val GL_VERTEX_SHADER_BIT   = 0x00000001
  val GL_FRAGMENT_SHADER_BIT = 0x00000002
  val GL_COMPUTE_SHADER_BIT  = 0x00000020
  val GL_ALL_SHADER_BITS     = -1 // 0xFFFFFFFF

  val GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT = 0x00000001
  val GL_ELEMENT_ARRAY_BARRIER_BIT       = 0x00000002
  val GL_UNIFORM_BARRIER_BIT             = 0x00000004
  val GL_TEXTURE_FETCH_BARRIER_BIT       = 0x00000008
  val GL_SHADER_IMAGE_ACCESS_BARRIER_BIT = 0x00000020
  val GL_COMMAND_BARRIER_BIT             = 0x00000040
  val GL_PIXEL_BUFFER_BARRIER_BIT        = 0x00000080
  val GL_TEXTURE_UPDATE_BARRIER_BIT      = 0x00000100
  val GL_BUFFER_UPDATE_BARRIER_BIT       = 0x00000200
  val GL_FRAMEBUFFER_BARRIER_BIT         = 0x00000400
  val GL_TRANSFORM_FEEDBACK_BARRIER_BIT  = 0x00000800
  val GL_ATOMIC_COUNTER_BARRIER_BIT      = 0x00001000
  val GL_SHADER_STORAGE_BARRIER_BIT      = 0x00002000
  val GL_ALL_BARRIER_BITS                = -1 // 0xFFFFFFFF

  val GL_TEXTURE_WIDTH                              = 0x1000
  val GL_TEXTURE_HEIGHT                             = 0x1001
  val GL_TEXTURE_INTERNAL_FORMAT                    = 0x1003
  val GL_STENCIL_INDEX                              = 0x1901
  val GL_TEXTURE_RED_SIZE                           = 0x805c
  val GL_TEXTURE_GREEN_SIZE                         = 0x805d
  val GL_TEXTURE_BLUE_SIZE                          = 0x805e
  val GL_TEXTURE_ALPHA_SIZE                         = 0x805f
  val GL_TEXTURE_DEPTH                              = 0x8071
  val GL_PROGRAM_SEPARABLE                          = 0x8258
  val GL_ACTIVE_PROGRAM                             = 0x8259
  val GL_PROGRAM_PIPELINE_BINDING                   = 0x825a
  val GL_MAX_COMPUTE_SHARED_MEMORY_SIZE             = 0x8262
  val GL_MAX_COMPUTE_UNIFORM_COMPONENTS             = 0x8263
  val GL_MAX_COMPUTE_ATOMIC_COUNTER_BUFFERS         = 0x8264
  val GL_MAX_COMPUTE_ATOMIC_COUNTERS                = 0x8265
  val GL_MAX_COMBINED_COMPUTE_UNIFORM_COMPONENTS    = 0x8266
  val GL_COMPUTE_WORK_GROUP_SIZE                    = 0x8267
  val GL_MAX_UNIFORM_LOCATIONS                      = 0x826e
  val GL_VERTEX_ATTRIB_BINDING                      = 0x82d4
  val GL_VERTEX_ATTRIB_RELATIVE_OFFSET              = 0x82d5
  val GL_VERTEX_BINDING_DIVISOR                     = 0x82d6
  val GL_VERTEX_BINDING_OFFSET                      = 0x82d7
  val GL_VERTEX_BINDING_STRIDE                      = 0x82d8
  val GL_MAX_VERTEX_ATTRIB_RELATIVE_OFFSET          = 0x82d9
  val GL_MAX_VERTEX_ATTRIB_BINDINGS                 = 0x82da
  val GL_MAX_VERTEX_ATTRIB_STRIDE                   = 0x82e5
  val GL_TEXTURE_COMPRESSED                         = 0x86a1
  val GL_TEXTURE_DEPTH_SIZE                         = 0x884a
  val GL_READ_ONLY                                  = 0x88b8
  val GL_WRITE_ONLY                                 = 0x88b9
  val GL_READ_WRITE                                 = 0x88ba
  val GL_TEXTURE_STENCIL_SIZE                       = 0x88f1
  val GL_TEXTURE_RED_TYPE                           = 0x8c10
  val GL_TEXTURE_GREEN_TYPE                         = 0x8c11
  val GL_TEXTURE_BLUE_TYPE                          = 0x8c12
  val GL_TEXTURE_ALPHA_TYPE                         = 0x8c13
  val GL_TEXTURE_DEPTH_TYPE                         = 0x8c16
  val GL_TEXTURE_SHARED_SIZE                        = 0x8c3f
  val GL_SAMPLE_POSITION                            = 0x8e50
  val GL_SAMPLE_MASK                                = 0x8e51
  val GL_SAMPLE_MASK_VALUE                          = 0x8e52
  val GL_MAX_SAMPLE_MASK_WORDS                      = 0x8e59
  val GL_MIN_PROGRAM_TEXTURE_GATHER_OFFSET          = 0x8e5e
  val GL_MAX_PROGRAM_TEXTURE_GATHER_OFFSET          = 0x8e5f
  val GL_MAX_IMAGE_UNITS                            = 0x8f38
  val GL_MAX_COMBINED_SHADER_OUTPUT_RESOURCES       = 0x8f39
  val GL_IMAGE_BINDING_NAME                         = 0x8f3a
  val GL_IMAGE_BINDING_LEVEL                        = 0x8f3b
  val GL_IMAGE_BINDING_LAYERED                      = 0x8f3c
  val GL_IMAGE_BINDING_LAYER                        = 0x8f3d
  val GL_IMAGE_BINDING_ACCESS                       = 0x8f3e
  val GL_DRAW_INDIRECT_BUFFER                       = 0x8f3f
  val GL_DRAW_INDIRECT_BUFFER_BINDING               = 0x8f43
  val GL_VERTEX_BINDING_BUFFER                      = 0x8f4f
  val GL_IMAGE_2D                                   = 0x904d
  val GL_IMAGE_3D                                   = 0x904e
  val GL_IMAGE_CUBE                                 = 0x9050
  val GL_IMAGE_2D_ARRAY                             = 0x9053
  val GL_INT_IMAGE_2D                               = 0x9058
  val GL_INT_IMAGE_3D                               = 0x9059
  val GL_INT_IMAGE_CUBE                             = 0x905b
  val GL_INT_IMAGE_2D_ARRAY                         = 0x905e
  val GL_UNSIGNED_INT_IMAGE_2D                      = 0x9063
  val GL_UNSIGNED_INT_IMAGE_3D                      = 0x9064
  val GL_UNSIGNED_INT_IMAGE_CUBE                    = 0x9066
  val GL_UNSIGNED_INT_IMAGE_2D_ARRAY                = 0x9069
  val GL_IMAGE_BINDING_FORMAT                       = 0x906e
  val GL_IMAGE_FORMAT_COMPATIBILITY_TYPE            = 0x90c7
  val GL_IMAGE_FORMAT_COMPATIBILITY_BY_SIZE         = 0x90c8
  val GL_IMAGE_FORMAT_COMPATIBILITY_BY_CLASS        = 0x90c9
  val GL_MAX_VERTEX_IMAGE_UNIFORMS                  = 0x90ca
  val GL_MAX_FRAGMENT_IMAGE_UNIFORMS                = 0x90ce
  val GL_MAX_COMBINED_IMAGE_UNIFORMS                = 0x90cf
  val GL_SHADER_STORAGE_BUFFER                      = 0x90d2
  val GL_SHADER_STORAGE_BUFFER_BINDING              = 0x90d3
  val GL_SHADER_STORAGE_BUFFER_START                = 0x90d4
  val GL_SHADER_STORAGE_BUFFER_SIZE                 = 0x90d5
  val GL_MAX_VERTEX_SHADER_STORAGE_BLOCKS           = 0x90d6
  val GL_MAX_FRAGMENT_SHADER_STORAGE_BLOCKS         = 0x90da
  val GL_MAX_COMPUTE_SHADER_STORAGE_BLOCKS          = 0x90db
  val GL_MAX_COMBINED_SHADER_STORAGE_BLOCKS         = 0x90dc
  val GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS         = 0x90dd
  val GL_MAX_SHADER_STORAGE_BLOCK_SIZE              = 0x90de
  val GL_SHADER_STORAGE_BUFFER_OFFSET_ALIGNMENT     = 0x90df
  val GL_DEPTH_STENCIL_TEXTURE_MODE                 = 0x90ea
  val GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS         = 0x90eb
  val GL_DISPATCH_INDIRECT_BUFFER                   = 0x90ee
  val GL_DISPATCH_INDIRECT_BUFFER_BINDING           = 0x90ef
  val GL_TEXTURE_2D_MULTISAMPLE                     = 0x9100
  val GL_TEXTURE_BINDING_2D_MULTISAMPLE             = 0x9104
  val GL_TEXTURE_SAMPLES                            = 0x9106
  val GL_TEXTURE_FIXED_SAMPLE_LOCATIONS             = 0x9107
  val GL_SAMPLER_2D_MULTISAMPLE                     = 0x9108
  val GL_INT_SAMPLER_2D_MULTISAMPLE                 = 0x9109
  val GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE        = 0x910a
  val GL_MAX_COLOR_TEXTURE_SAMPLES                  = 0x910e
  val GL_MAX_DEPTH_TEXTURE_SAMPLES                  = 0x910f
  val GL_MAX_INTEGER_SAMPLES                        = 0x9110
  val GL_COMPUTE_SHADER                             = 0x91b9
  val GL_MAX_COMPUTE_UNIFORM_BLOCKS                 = 0x91bb
  val GL_MAX_COMPUTE_TEXTURE_IMAGE_UNITS            = 0x91bc
  val GL_MAX_COMPUTE_IMAGE_UNIFORMS                 = 0x91bd
  val GL_MAX_COMPUTE_WORK_GROUP_COUNT               = 0x91be
  val GL_MAX_COMPUTE_WORK_GROUP_SIZE                = 0x91bf
  val GL_ATOMIC_COUNTER_BUFFER                      = 0x92c0
  val GL_ATOMIC_COUNTER_BUFFER_BINDING              = 0x92c1
  val GL_ATOMIC_COUNTER_BUFFER_START                = 0x92c2
  val GL_ATOMIC_COUNTER_BUFFER_SIZE                 = 0x92c3
  val GL_MAX_VERTEX_ATOMIC_COUNTER_BUFFERS          = 0x92cc
  val GL_MAX_FRAGMENT_ATOMIC_COUNTER_BUFFERS        = 0x92d0
  val GL_MAX_COMBINED_ATOMIC_COUNTER_BUFFERS        = 0x92d1
  val GL_MAX_VERTEX_ATOMIC_COUNTERS                 = 0x92d2
  val GL_MAX_FRAGMENT_ATOMIC_COUNTERS               = 0x92d6
  val GL_MAX_COMBINED_ATOMIC_COUNTERS               = 0x92d7
  val GL_MAX_ATOMIC_COUNTER_BUFFER_SIZE             = 0x92d8
  val GL_ACTIVE_ATOMIC_COUNTER_BUFFERS              = 0x92d9
  val GL_UNSIGNED_INT_ATOMIC_COUNTER                = 0x92db
  val GL_MAX_ATOMIC_COUNTER_BUFFER_BINDINGS         = 0x92dc
  val GL_UNIFORM                                    = 0x92e1
  val GL_UNIFORM_BLOCK                              = 0x92e2
  val GL_PROGRAM_INPUT                              = 0x92e3
  val GL_PROGRAM_OUTPUT                             = 0x92e4
  val GL_BUFFER_VARIABLE                            = 0x92e5
  val GL_SHADER_STORAGE_BLOCK                       = 0x92e6
  val GL_TRANSFORM_FEEDBACK_VARYING                 = 0x92f4
  val GL_ACTIVE_RESOURCES                           = 0x92f5
  val GL_MAX_NAME_LENGTH                            = 0x92f6
  val GL_MAX_NUM_ACTIVE_VARIABLES                   = 0x92f7
  val GL_NAME_LENGTH                                = 0x92f9
  val GL_TYPE                                       = 0x92fa
  val GL_ARRAY_SIZE                                 = 0x92fb
  val GL_OFFSET                                     = 0x92fc
  val GL_BLOCK_INDEX                                = 0x92fd
  val GL_ARRAY_STRIDE                               = 0x92fe
  val GL_MATRIX_STRIDE                              = 0x92ff
  val GL_IS_ROW_MAJOR                               = 0x9300
  val GL_ATOMIC_COUNTER_BUFFER_INDEX                = 0x9301
  val GL_BUFFER_BINDING                             = 0x9302
  val GL_BUFFER_DATA_SIZE                           = 0x9303
  val GL_NUM_ACTIVE_VARIABLES                       = 0x9304
  val GL_ACTIVE_VARIABLES                           = 0x9305
  val GL_REFERENCED_BY_VERTEX_SHADER                = 0x9306
  val GL_REFERENCED_BY_FRAGMENT_SHADER              = 0x930a
  val GL_REFERENCED_BY_COMPUTE_SHADER               = 0x930b
  val GL_TOP_LEVEL_ARRAY_SIZE                       = 0x930c
  val GL_TOP_LEVEL_ARRAY_STRIDE                     = 0x930d
  val GL_LOCATION                                   = 0x930e
  val GL_FRAMEBUFFER_DEFAULT_WIDTH                  = 0x9310
  val GL_FRAMEBUFFER_DEFAULT_HEIGHT                 = 0x9311
  val GL_FRAMEBUFFER_DEFAULT_SAMPLES                = 0x9313
  val GL_FRAMEBUFFER_DEFAULT_FIXED_SAMPLE_LOCATIONS = 0x9314
  val GL_MAX_FRAMEBUFFER_WIDTH                      = 0x9315
  val GL_MAX_FRAMEBUFFER_HEIGHT                     = 0x9316
  val GL_MAX_FRAMEBUFFER_SAMPLES                    = 0x9318
}
