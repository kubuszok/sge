/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/GL30.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Convention: typed GL enums
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 527
 * Covenant-baseline-methods: GL30,GL_ACTIVE_UNIFORM_BLOCKS,GL_ACTIVE_UNIFORM_BLOCK_MAX_NAME_LENGTH,GL_ALREADY_SIGNALED,GL_ANY_SAMPLES_PASSED,GL_ANY_SAMPLES_PASSED_CONSERVATIVE,GL_BLUE,GL_BUFFER_ACCESS_FLAGS,GL_BUFFER_MAPPED,GL_BUFFER_MAP_LENGTH,GL_BUFFER_MAP_OFFSET,GL_BUFFER_MAP_POINTER,GL_COLOR,GL_COLOR_ATTACHMENT1,GL_COLOR_ATTACHMENT10,GL_COLOR_ATTACHMENT11,GL_COLOR_ATTACHMENT12,GL_COLOR_ATTACHMENT13,GL_COLOR_ATTACHMENT14,GL_COLOR_ATTACHMENT15,GL_COLOR_ATTACHMENT2,GL_COLOR_ATTACHMENT3,GL_COLOR_ATTACHMENT4,GL_COLOR_ATTACHMENT5,GL_COLOR_ATTACHMENT6,GL_COLOR_ATTACHMENT7,GL_COLOR_ATTACHMENT8,GL_COLOR_ATTACHMENT9,GL_COMPARE_REF_TO_TEXTURE,GL_COMPRESSED_R11_EAC,GL_COMPRESSED_RG11_EAC,GL_COMPRESSED_RGB8_ETC2,GL_COMPRESSED_RGB8_PUNCHTHROUGH_ALPHA1_ETC2,GL_COMPRESSED_RGBA8_ETC2_EAC,GL_COMPRESSED_SIGNED_R11_EAC,GL_COMPRESSED_SIGNED_RG11_EAC,GL_COMPRESSED_SRGB8_ALPHA8_ETC2_EAC,GL_COMPRESSED_SRGB8_ETC2,GL_COMPRESSED_SRGB8_PUNCHTHROUGH_ALPHA1_ETC2,GL_CONDITION_SATISFIED,GL_COPY_READ_BUFFER,GL_COPY_READ_BUFFER_BINDING,GL_COPY_WRITE_BUFFER,GL_COPY_WRITE_BUFFER_BINDING,GL_CURRENT_QUERY,GL_DEPTH,GL_DEPTH24_STENCIL8,GL_DEPTH32F_STENCIL8,GL_DEPTH_COMPONENT24,GL_DEPTH_COMPONENT32F,GL_DEPTH_STENCIL,GL_DEPTH_STENCIL_ATTACHMENT,GL_DRAW_BUFFER0,GL_DRAW_BUFFER1,GL_DRAW_BUFFER10,GL_DRAW_BUFFER11,GL_DRAW_BUFFER12,GL_DRAW_BUFFER13,GL_DRAW_BUFFER14,GL_DRAW_BUFFER15,GL_DRAW_BUFFER2,GL_DRAW_BUFFER3,GL_DRAW_BUFFER4,GL_DRAW_BUFFER5,GL_DRAW_BUFFER6,GL_DRAW_BUFFER7,GL_DRAW_BUFFER8,GL_DRAW_BUFFER9,GL_DRAW_FRAMEBUFFER,GL_DRAW_FRAMEBUFFER_BINDING,GL_DYNAMIC_COPY,GL_DYNAMIC_READ,GL_FLOAT_32_UNSIGNED_INT_24_8_REV,GL_FLOAT_MAT2x3,GL_FLOAT_MAT2x4,GL_FLOAT_MAT3x2,GL_FLOAT_MAT3x4,GL_FLOAT_MAT4x2,GL_FLOAT_MAT4x3,GL_FRAGMENT_SHADER_DERIVATIVE_HINT,GL_FRAMEBUFFER_ATTACHMENT_ALPHA_SIZE,GL_FRAMEBUFFER_ATTACHMENT_BLUE_SIZE,GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING,GL_FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE,GL_FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE,GL_FRAMEBUFFER_ATTACHMENT_GREEN_SIZE,GL_FRAMEBUFFER_ATTACHMENT_RED_SIZE,GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE,GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LAYER,GL_FRAMEBUFFER_DEFAULT,GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE,GL_FRAMEBUFFER_UNDEFINED,GL_GREEN,GL_HALF_FLOAT,GL_INTERLEAVED_ATTRIBS,GL_INT_2_10_10_10_REV,GL_INT_SAMPLER_2D,GL_INT_SAMPLER_2D_ARRAY,GL_INT_SAMPLER_3D,GL_INT_SAMPLER_CUBE,GL_INVALID_INDEX,GL_MAJOR_VERSION,GL_MAP_FLUSH_EXPLICIT_BIT,GL_MAP_INVALIDATE_BUFFER_BIT,GL_MAP_INVALIDATE_RANGE_BIT,GL_MAP_READ_BIT,GL_MAP_UNSYNCHRONIZED_BIT,GL_MAP_WRITE_BIT,GL_MAX,GL_MAX_3D_TEXTURE_SIZE,GL_MAX_ARRAY_TEXTURE_LAYERS,GL_MAX_COLOR_ATTACHMENTS,GL_MAX_COMBINED_FRAGMENT_UNIFORM_COMPONENTS,GL_MAX_COMBINED_UNIFORM_BLOCKS,GL_MAX_COMBINED_VERTEX_UNIFORM_COMPONENTS,GL_MAX_DRAW_BUFFERS,GL_MAX_ELEMENTS_INDICES,GL_MAX_ELEMENTS_VERTICES,GL_MAX_ELEMENT_INDEX,GL_MAX_FRAGMENT_INPUT_COMPONENTS,GL_MAX_FRAGMENT_UNIFORM_BLOCKS,GL_MAX_FRAGMENT_UNIFORM_COMPONENTS,GL_MAX_PROGRAM_TEXEL_OFFSET,GL_MAX_SAMPLES,GL_MAX_SERVER_WAIT_TIMEOUT,GL_MAX_TEXTURE_LOD_BIAS,GL_MAX_TRANSFORM_FEEDBACK_INTERLEAVED_COMPONENTS,GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_ATTRIBS,GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_COMPONENTS,GL_MAX_UNIFORM_BLOCK_SIZE,GL_MAX_UNIFORM_BUFFER_BINDINGS,GL_MAX_VARYING_COMPONENTS,GL_MAX_VERTEX_OUTPUT_COMPONENTS,GL_MAX_VERTEX_UNIFORM_BLOCKS,GL_MAX_VERTEX_UNIFORM_COMPONENTS,GL_MIN,GL_MINOR_VERSION,GL_MIN_PROGRAM_TEXEL_OFFSET,GL_NUM_EXTENSIONS,GL_NUM_PROGRAM_BINARY_FORMATS,GL_NUM_SAMPLE_COUNTS,GL_OBJECT_TYPE,GL_PACK_ROW_LENGTH,GL_PACK_SKIP_PIXELS,GL_PACK_SKIP_ROWS,GL_PIXEL_PACK_BUFFER,GL_PIXEL_PACK_BUFFER_BINDING,GL_PIXEL_UNPACK_BUFFER,GL_PIXEL_UNPACK_BUFFER_BINDING,GL_PRIMITIVE_RESTART_FIXED_INDEX,GL_PROGRAM_BINARY_FORMATS,GL_PROGRAM_BINARY_LENGTH,GL_PROGRAM_BINARY_RETRIEVABLE_HINT,GL_QUERY_RESULT,GL_QUERY_RESULT_AVAILABLE,GL_R11F_G11F_B10F,GL_R16F,GL_R16I,GL_R16UI,GL_R32F,GL_R32I,GL_R32UI,GL_R8,GL_R8I,GL_R8UI,GL_R8_SNORM,GL_RASTERIZER_DISCARD,GL_READ_BUFFER,GL_READ_FRAMEBUFFER,GL_READ_FRAMEBUFFER_BINDING,GL_RED,GL_RED_INTEGER,GL_RENDERBUFFER_SAMPLES,GL_RG,GL_RG16F,GL_RG16I,GL_RG16UI,GL_RG32F,GL_RG32I,GL_RG32UI,GL_RG8,GL_RG8I,GL_RG8UI,GL_RG8_SNORM,GL_RGB10_A2,GL_RGB10_A2UI,GL_RGB16F,GL_RGB16I,GL_RGB16UI,GL_RGB32F,GL_RGB32I,GL_RGB32UI,GL_RGB8,GL_RGB8I,GL_RGB8UI,GL_RGB8_SNORM,GL_RGB9_E5,GL_RGBA16F,GL_RGBA16I,GL_RGBA16UI,GL_RGBA32F,GL_RGBA32I,GL_RGBA32UI,GL_RGBA8,GL_RGBA8I,GL_RGBA8UI,GL_RGBA8_SNORM,GL_RGBA_INTEGER,GL_RGB_INTEGER,GL_RG_INTEGER,GL_SAMPLER_2D_ARRAY,GL_SAMPLER_2D_ARRAY_SHADOW,GL_SAMPLER_2D_SHADOW,GL_SAMPLER_3D,GL_SAMPLER_BINDING,GL_SAMPLER_CUBE_SHADOW,GL_SEPARATE_ATTRIBS,GL_SIGNALED,GL_SIGNED_NORMALIZED,GL_SRGB,GL_SRGB8,GL_SRGB8_ALPHA8,GL_STATIC_COPY,GL_STATIC_READ,GL_STENCIL,GL_STREAM_COPY,GL_STREAM_READ,GL_SYNC_CONDITION,GL_SYNC_FENCE,GL_SYNC_FLAGS,GL_SYNC_FLUSH_COMMANDS_BIT,GL_SYNC_GPU_COMMANDS_COMPLETE,GL_SYNC_STATUS,GL_TEXTURE_2D_ARRAY,GL_TEXTURE_3D,GL_TEXTURE_BASE_LEVEL,GL_TEXTURE_BINDING_2D_ARRAY,GL_TEXTURE_BINDING_3D,GL_TEXTURE_COMPARE_FUNC,GL_TEXTURE_COMPARE_MODE,GL_TEXTURE_IMMUTABLE_FORMAT,GL_TEXTURE_IMMUTABLE_LEVELS,GL_TEXTURE_MAX_LEVEL,GL_TEXTURE_MAX_LOD,GL_TEXTURE_MIN_LOD,GL_TEXTURE_SWIZZLE_A,GL_TEXTURE_SWIZZLE_B,GL_TEXTURE_SWIZZLE_G,GL_TEXTURE_SWIZZLE_R,GL_TEXTURE_WRAP_R,GL_TIMEOUT_EXPIRED,GL_TIMEOUT_IGNORED,GL_TRANSFORM_FEEDBACK,GL_TRANSFORM_FEEDBACK_ACTIVE,GL_TRANSFORM_FEEDBACK_BINDING,GL_TRANSFORM_FEEDBACK_BUFFER,GL_TRANSFORM_FEEDBACK_BUFFER_BINDING,GL_TRANSFORM_FEEDBACK_BUFFER_MODE,GL_TRANSFORM_FEEDBACK_BUFFER_SIZE,GL_TRANSFORM_FEEDBACK_BUFFER_START,GL_TRANSFORM_FEEDBACK_PAUSED,GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN,GL_TRANSFORM_FEEDBACK_VARYINGS,GL_TRANSFORM_FEEDBACK_VARYING_MAX_LENGTH,GL_UNIFORM_ARRAY_STRIDE,GL_UNIFORM_BLOCK_ACTIVE_UNIFORMS,GL_UNIFORM_BLOCK_ACTIVE_UNIFORM_INDICES,GL_UNIFORM_BLOCK_BINDING,GL_UNIFORM_BLOCK_DATA_SIZE,GL_UNIFORM_BLOCK_INDEX,GL_UNIFORM_BLOCK_NAME_LENGTH,GL_UNIFORM_BLOCK_REFERENCED_BY_FRAGMENT_SHADER,GL_UNIFORM_BLOCK_REFERENCED_BY_VERTEX_SHADER,GL_UNIFORM_BUFFER,GL_UNIFORM_BUFFER_BINDING,GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT,GL_UNIFORM_BUFFER_SIZE,GL_UNIFORM_BUFFER_START,GL_UNIFORM_IS_ROW_MAJOR,GL_UNIFORM_MATRIX_STRIDE,GL_UNIFORM_NAME_LENGTH,GL_UNIFORM_OFFSET,GL_UNIFORM_SIZE,GL_UNIFORM_TYPE,GL_UNPACK_IMAGE_HEIGHT,GL_UNPACK_ROW_LENGTH,GL_UNPACK_SKIP_IMAGES,GL_UNPACK_SKIP_PIXELS,GL_UNPACK_SKIP_ROWS,GL_UNSIGNALED,GL_UNSIGNED_INT_10F_11F_11F_REV,GL_UNSIGNED_INT_24_8,GL_UNSIGNED_INT_2_10_10_10_REV,GL_UNSIGNED_INT_5_9_9_9_REV,GL_UNSIGNED_INT_SAMPLER_2D,GL_UNSIGNED_INT_SAMPLER_2D_ARRAY,GL_UNSIGNED_INT_SAMPLER_3D,GL_UNSIGNED_INT_SAMPLER_CUBE,GL_UNSIGNED_INT_VEC2,GL_UNSIGNED_INT_VEC3,GL_UNSIGNED_INT_VEC4,GL_UNSIGNED_NORMALIZED,GL_VERTEX_ARRAY_BINDING,GL_VERTEX_ATTRIB_ARRAY_DIVISOR,GL_VERTEX_ATTRIB_ARRAY_INTEGER,GL_WAIT_FAILED,glBeginQuery,glBeginTransformFeedback,glBindBufferBase,glBindBufferRange,glBindSampler,glBindTransformFeedback,glBindVertexArray,glBlitFramebuffer,glClearBufferfi,glClearBufferfv,glClearBufferiv,glClearBufferuiv,glCopyBufferSubData,glCopyTexSubImage3D,glDeleteQueries,glDeleteSamplers,glDeleteTransformFeedbacks,glDeleteVertexArrays,glDrawArraysInstanced,glDrawBuffers,glDrawElementsInstanced,glDrawRangeElements,glEndQuery,glEndTransformFeedback,glFlushMappedBufferRange,glFramebufferTextureLayer,glGenQueries,glGenSamplers,glGenTransformFeedbacks,glGenVertexArrays,glGetActiveUniformBlockName,glGetActiveUniformBlockiv,glGetActiveUniformsiv,glGetBufferParameteri64v,glGetBufferPointerv,glGetFragDataLocation,glGetInteger64v,glGetQueryObjectuiv,glGetQueryiv,glGetSamplerParameterfv,glGetSamplerParameteriv,glGetStringi,glGetUniformBlockIndex,glGetUniformIndices,glGetUniformuiv,glGetVertexAttribIiv,glGetVertexAttribIuiv,glInvalidateFramebuffer,glInvalidateSubFramebuffer,glIsQuery,glIsSampler,glIsTransformFeedback,glIsVertexArray,glMapBufferRange,glPauseTransformFeedback,glProgramParameteri,glReadBuffer,glRenderbufferStorageMultisample,glResumeTransformFeedback,glSamplerParameterf,glSamplerParameterfv,glSamplerParameteri,glSamplerParameteriv,glTexImage2D,glTexImage3D,glTexSubImage2D,glTexSubImage3D,glTransformFeedbackVaryings,glUniform1uiv,glUniform3uiv,glUniform4uiv,glUniformBlockBinding,glUniformMatrix2x3fv,glUniformMatrix2x4fv,glUniformMatrix3x2fv,glUniformMatrix3x4fv,glUniformMatrix4x2fv,glUniformMatrix4x3fv,glUnmapBuffer,glVertexAttribDivisor,glVertexAttribI4i,glVertexAttribI4ui,glVertexAttribIPointer,glVertexAttribPointer
 * Covenant-source-reference: com/badlogic/gdx/graphics/GL30.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 605e63608e093070c0005eec92bfb73d7c20706d
 */
package sge
package graphics

import java.nio.{ Buffer, FloatBuffer, IntBuffer, LongBuffer }

/** OpenGL ES 3.0 */
trait GL30 extends GL20 {

  def glReadBuffer(mode: Int): Unit

  def glDrawRangeElements(mode: PrimitiveMode, start: Int, end: Int, count: Int, `type`: DataType, indices: Buffer): Unit

  def glDrawRangeElements(mode: PrimitiveMode, start: Int, end: Int, count: Int, `type`: DataType, offset: Int): Unit

  def glTexImage2D(target: TextureTarget, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: PixelFormat, `type`: DataType, offset: Int): Unit

  def glTexImage3D(target: TextureTarget, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: PixelFormat, `type`: DataType, pixels: Buffer): Unit

  def glTexImage3D(target: TextureTarget, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: PixelFormat, `type`: DataType, offset: Int): Unit

  def glTexSubImage2D(target: TextureTarget, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: PixelFormat, `type`: DataType, offset: Int): Unit

  def glTexSubImage3D(target: TextureTarget, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int, format: PixelFormat, `type`: DataType, pixels: Buffer): Unit

  def glTexSubImage3D(target: TextureTarget, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int, format: PixelFormat, `type`: DataType, offset: Int): Unit

  def glCopyTexSubImage3D(target: TextureTarget, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit

  def glGenQueries(n: Int, ids: Array[Int], offset: Int): Unit

  def glGenQueries(n: Int, ids: IntBuffer): Unit

  def glDeleteQueries(n: Int, ids: Array[Int], offset: Int): Unit

  def glDeleteQueries(n: Int, ids: IntBuffer): Unit

  def glIsQuery(id: Int): Boolean

  def glBeginQuery(target: Int, id: Int): Unit

  def glEndQuery(target: Int): Unit

  def glGetQueryiv(target: Int, pname: Int, params: IntBuffer): Unit

  def glGetQueryObjectuiv(id: Int, pname: Int, params: IntBuffer): Unit

  def glUnmapBuffer(target: BufferTarget): Boolean

  def glGetBufferPointerv(target: BufferTarget, pname: Int): Buffer

  def glDrawBuffers(n: Int, bufs: IntBuffer): Unit

  def glUniformMatrix2x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glUniformMatrix3x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glUniformMatrix2x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glUniformMatrix4x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glUniformMatrix3x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glUniformMatrix4x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glBlitFramebuffer(srcX0: Int, srcY0: Int, srcX1: Int, srcY1: Int, dstX0: Int, dstY0: Int, dstX1: Int, dstY1: Int, mask: ClearMask, filter: Int): Unit

  def glRenderbufferStorageMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int): Unit

  def glFramebufferTextureLayer(target: Int, attachment: Int, texture: Int, level: Int, layer: Int): Unit

  def glMapBufferRange(target: BufferTarget, offset: Int, length: Int, access: Int): Buffer

  def glFlushMappedBufferRange(target: BufferTarget, offset: Int, length: Int): Unit

  def glBindVertexArray(array: Int): Unit

  def glDeleteVertexArrays(n: Int, arrays: Array[Int], offset: Int): Unit

  def glDeleteVertexArrays(n: Int, arrays: IntBuffer): Unit

  def glGenVertexArrays(n: Int, arrays: Array[Int], offset: Int): Unit

  def glGenVertexArrays(n: Int, arrays: IntBuffer): Unit

  def glIsVertexArray(array: Int): Boolean

  def glBeginTransformFeedback(primitiveMode: PrimitiveMode): Unit

  def glEndTransformFeedback(): Unit

  def glBindBufferRange(target: BufferTarget, index: Int, buffer: Int, offset: Int, size: Int): Unit

  def glBindBufferBase(target: BufferTarget, index: Int, buffer: Int): Unit

  def glTransformFeedbackVaryings(program: Int, varyings: Array[String], bufferMode: Int): Unit

  def glVertexAttribIPointer(index: Int, size: Int, `type`: DataType, stride: Int, offset: Int): Unit

  def glGetVertexAttribIiv(index: Int, pname: Int, params: IntBuffer): Unit

  def glGetVertexAttribIuiv(index: Int, pname: Int, params: IntBuffer): Unit

  def glVertexAttribI4i(index: Int, x: Int, y: Int, z: Int, w: Int): Unit

  def glVertexAttribI4ui(index: Int, x: Int, y: Int, z: Int, w: Int): Unit

  def glGetUniformuiv(program: Int, location: Int, params: IntBuffer): Unit

  def glGetFragDataLocation(program: Int, name: String): Int

  def glUniform1uiv(location: Int, count: Int, value: IntBuffer): Unit

  def glUniform3uiv(location: Int, count: Int, value: IntBuffer): Unit

  def glUniform4uiv(location: Int, count: Int, value: IntBuffer): Unit

  def glClearBufferiv(buffer: Int, drawbuffer: Int, value: IntBuffer): Unit

  def glClearBufferuiv(buffer: Int, drawbuffer: Int, value: IntBuffer): Unit

  def glClearBufferfv(buffer: Int, drawbuffer: Int, value: FloatBuffer): Unit

  def glClearBufferfi(buffer: Int, drawbuffer: Int, depth: Float, stencil: Int): Unit

  def glGetStringi(name: Int, index: Int): String

  def glCopyBufferSubData(readTarget: BufferTarget, writeTarget: BufferTarget, readOffset: Int, writeOffset: Int, size: Int): Unit

  def glGetUniformIndices(program: Int, uniformNames: Array[String], uniformIndices: IntBuffer): Unit

  def glGetActiveUniformsiv(program: Int, uniformCount: Int, uniformIndices: IntBuffer, pname: Int, params: IntBuffer): Unit

  def glGetUniformBlockIndex(program: Int, uniformBlockName: String): Int

  def glGetActiveUniformBlockiv(program: Int, uniformBlockIndex: Int, pname: Int, params: IntBuffer): Unit

  def glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int, length: Buffer, uniformBlockName: Buffer): Unit

  def glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int): String

  def glUniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int): Unit

  def glDrawArraysInstanced(mode: PrimitiveMode, first: Int, count: Int, instanceCount: Int): Unit

  def glDrawElementsInstanced(mode: PrimitiveMode, count: Int, `type`: DataType, indicesOffset: Int, instanceCount: Int): Unit

  def glGetInteger64v(pname: Int, params: LongBuffer): Unit

  def glGetBufferParameteri64v(target: BufferTarget, pname: Int, params: LongBuffer): Unit

  def glGenSamplers(count: Int, samplers: Array[Int], offset: Int): Unit

  def glGenSamplers(count: Int, samplers: IntBuffer): Unit

  def glDeleteSamplers(count: Int, samplers: Array[Int], offset: Int): Unit

  def glDeleteSamplers(count: Int, samplers: IntBuffer): Unit

  def glIsSampler(sampler: Int): Boolean

  def glBindSampler(unit: Int, sampler: Int): Unit

  def glSamplerParameteri(sampler: Int, pname: Int, param: Int): Unit

  def glSamplerParameteriv(sampler: Int, pname: Int, param: IntBuffer): Unit

  def glSamplerParameterf(sampler: Int, pname: Int, param: Float): Unit

  def glSamplerParameterfv(sampler: Int, pname: Int, param: FloatBuffer): Unit

  def glGetSamplerParameteriv(sampler: Int, pname: Int, params: IntBuffer): Unit

  def glGetSamplerParameterfv(sampler: Int, pname: Int, params: FloatBuffer): Unit

  def glVertexAttribDivisor(index: Int, divisor: Int): Unit

  def glBindTransformFeedback(target: Int, id: Int): Unit

  def glDeleteTransformFeedbacks(n: Int, ids: Array[Int], offset: Int): Unit

  def glDeleteTransformFeedbacks(n: Int, ids: IntBuffer): Unit

  def glGenTransformFeedbacks(n: Int, ids: Array[Int], offset: Int): Unit

  def glGenTransformFeedbacks(n: Int, ids: IntBuffer): Unit

  def glIsTransformFeedback(id: Int): Boolean

  def glPauseTransformFeedback(): Unit

  def glResumeTransformFeedback(): Unit

  def glProgramParameteri(program: Int, pname: Int, value: Int): Unit

  def glInvalidateFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer): Unit

  def glInvalidateSubFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer, x: Int, y: Int, width: Int, height: Int): Unit

  @Deprecated
  /** In OpenGl core profiles (3.1+), passing a pointer to client memory is not valid. Use the other version of this function instead, pass a zero-based offset which references the buffer currently
    * bound to GL_ARRAY_BUFFER.
    */
  override def glVertexAttribPointer(indx: Int, size: Int, `type`: DataType, normalized: Boolean, stride: Int, ptr: Buffer): Unit
}

object GL30 {
  val GL_READ_BUFFER                                   = 0x0c02
  val GL_UNPACK_ROW_LENGTH                             = 0x0cf2
  val GL_UNPACK_SKIP_ROWS                              = 0x0cf3
  val GL_UNPACK_SKIP_PIXELS                            = 0x0cf4
  val GL_PACK_ROW_LENGTH                               = 0x0d02
  val GL_PACK_SKIP_ROWS                                = 0x0d03
  val GL_PACK_SKIP_PIXELS                              = 0x0d04
  val GL_COLOR                                         = 0x1800
  val GL_DEPTH                                         = 0x1801
  val GL_STENCIL                                       = 0x1802
  val GL_RED                                           = 0x1903
  val GL_RGB8                                          = 0x8051
  val GL_RGBA8                                         = 0x8058
  val GL_RGB10_A2                                      = 0x8059
  val GL_TEXTURE_BINDING_3D                            = 0x806a
  val GL_UNPACK_SKIP_IMAGES                            = 0x806d
  val GL_UNPACK_IMAGE_HEIGHT                           = 0x806e
  val GL_TEXTURE_3D                                    = 0x806f
  val GL_TEXTURE_WRAP_R                                = 0x8072
  val GL_MAX_3D_TEXTURE_SIZE                           = 0x8073
  val GL_UNSIGNED_INT_2_10_10_10_REV                   = 0x8368
  val GL_MAX_ELEMENTS_VERTICES                         = 0x80e8
  val GL_MAX_ELEMENTS_INDICES                          = 0x80e9
  val GL_TEXTURE_MIN_LOD                               = 0x813a
  val GL_TEXTURE_MAX_LOD                               = 0x813b
  val GL_TEXTURE_BASE_LEVEL                            = 0x813c
  val GL_TEXTURE_MAX_LEVEL                             = 0x813d
  val GL_MIN                                           = 0x8007
  val GL_MAX                                           = 0x8008
  val GL_DEPTH_COMPONENT24                             = 0x81a6
  val GL_MAX_TEXTURE_LOD_BIAS                          = 0x84fd
  val GL_TEXTURE_COMPARE_MODE                          = 0x884c
  val GL_TEXTURE_COMPARE_FUNC                          = 0x884d
  val GL_CURRENT_QUERY                                 = 0x8865
  val GL_QUERY_RESULT                                  = 0x8866
  val GL_QUERY_RESULT_AVAILABLE                        = 0x8867
  val GL_BUFFER_MAPPED                                 = 0x88bc
  val GL_BUFFER_MAP_POINTER                            = 0x88bd
  val GL_STREAM_READ                                   = 0x88e1
  val GL_STREAM_COPY                                   = 0x88e2
  val GL_STATIC_READ                                   = 0x88e5
  val GL_STATIC_COPY                                   = 0x88e6
  val GL_DYNAMIC_READ                                  = 0x88e9
  val GL_DYNAMIC_COPY                                  = 0x88ea
  val GL_MAX_DRAW_BUFFERS                              = 0x8824
  val GL_DRAW_BUFFER0                                  = 0x8825
  val GL_DRAW_BUFFER1                                  = 0x8826
  val GL_DRAW_BUFFER2                                  = 0x8827
  val GL_DRAW_BUFFER3                                  = 0x8828
  val GL_DRAW_BUFFER4                                  = 0x8829
  val GL_DRAW_BUFFER5                                  = 0x882a
  val GL_DRAW_BUFFER6                                  = 0x882b
  val GL_DRAW_BUFFER7                                  = 0x882c
  val GL_DRAW_BUFFER8                                  = 0x882d
  val GL_DRAW_BUFFER9                                  = 0x882e
  val GL_DRAW_BUFFER10                                 = 0x882f
  val GL_DRAW_BUFFER11                                 = 0x8830
  val GL_DRAW_BUFFER12                                 = 0x8831
  val GL_DRAW_BUFFER13                                 = 0x8832
  val GL_DRAW_BUFFER14                                 = 0x8833
  val GL_DRAW_BUFFER15                                 = 0x8834
  val GL_MAX_FRAGMENT_UNIFORM_COMPONENTS               = 0x8b49
  val GL_MAX_VERTEX_UNIFORM_COMPONENTS                 = 0x8b4a
  val GL_SAMPLER_3D                                    = 0x8b5f
  val GL_SAMPLER_2D_SHADOW                             = 0x8b62
  val GL_FRAGMENT_SHADER_DERIVATIVE_HINT               = 0x8b8b
  val GL_PIXEL_PACK_BUFFER                             = 0x88eb
  val GL_PIXEL_UNPACK_BUFFER                           = 0x88ec
  val GL_PIXEL_PACK_BUFFER_BINDING                     = 0x88ed
  val GL_PIXEL_UNPACK_BUFFER_BINDING                   = 0x88ef
  val GL_FLOAT_MAT2x3                                  = 0x8b65
  val GL_FLOAT_MAT2x4                                  = 0x8b66
  val GL_FLOAT_MAT3x2                                  = 0x8b67
  val GL_FLOAT_MAT3x4                                  = 0x8b68
  val GL_FLOAT_MAT4x2                                  = 0x8b69
  val GL_FLOAT_MAT4x3                                  = 0x8b6a
  val GL_SRGB                                          = 0x8c40
  val GL_SRGB8                                         = 0x8c41
  val GL_SRGB8_ALPHA8                                  = 0x8c43
  val GL_COMPARE_REF_TO_TEXTURE                        = 0x884e
  val GL_MAJOR_VERSION                                 = 0x821b
  val GL_MINOR_VERSION                                 = 0x821c
  val GL_NUM_EXTENSIONS                                = 0x821d
  val GL_RGBA32F                                       = 0x8814
  val GL_RGB32F                                        = 0x8815
  val GL_RGBA16F                                       = 0x881a
  val GL_RGB16F                                        = 0x881b
  val GL_VERTEX_ATTRIB_ARRAY_INTEGER                   = 0x88fd
  val GL_MAX_ARRAY_TEXTURE_LAYERS                      = 0x88ff
  val GL_MIN_PROGRAM_TEXEL_OFFSET                      = 0x8904
  val GL_MAX_PROGRAM_TEXEL_OFFSET                      = 0x8905
  val GL_MAX_VARYING_COMPONENTS                        = 0x8b4b
  val GL_TEXTURE_2D_ARRAY                              = 0x8c1a
  val GL_TEXTURE_BINDING_2D_ARRAY                      = 0x8c1d
  val GL_R11F_G11F_B10F                                = 0x8c3a
  val GL_UNSIGNED_INT_10F_11F_11F_REV                  = 0x8c3b
  val GL_RGB9_E5                                       = 0x8c3d
  val GL_UNSIGNED_INT_5_9_9_9_REV                      = 0x8c3e
  val GL_TRANSFORM_FEEDBACK_VARYING_MAX_LENGTH         = 0x8c76
  val GL_TRANSFORM_FEEDBACK_BUFFER_MODE                = 0x8c7f
  val GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_COMPONENTS    = 0x8c80
  val GL_TRANSFORM_FEEDBACK_VARYINGS                   = 0x8c83
  val GL_TRANSFORM_FEEDBACK_BUFFER_START               = 0x8c84
  val GL_TRANSFORM_FEEDBACK_BUFFER_SIZE                = 0x8c85
  val GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN         = 0x8c88
  val GL_RASTERIZER_DISCARD                            = 0x8c89
  val GL_MAX_TRANSFORM_FEEDBACK_INTERLEAVED_COMPONENTS = 0x8c8a
  val GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_ATTRIBS       = 0x8c8b
  val GL_INTERLEAVED_ATTRIBS                           = 0x8c8c
  val GL_SEPARATE_ATTRIBS                              = 0x8c8d
  val GL_TRANSFORM_FEEDBACK_BUFFER                     = 0x8c8e
  val GL_TRANSFORM_FEEDBACK_BUFFER_BINDING             = 0x8c8f
  val GL_RGBA32UI                                      = 0x8d70
  val GL_RGB32UI                                       = 0x8d71
  val GL_RGBA16UI                                      = 0x8d76
  val GL_RGB16UI                                       = 0x8d77
  val GL_RGBA8UI                                       = 0x8d7c
  val GL_RGB8UI                                        = 0x8d7d
  val GL_RGBA32I                                       = 0x8d82
  val GL_RGB32I                                        = 0x8d83
  val GL_RGBA16I                                       = 0x8d88
  val GL_RGB16I                                        = 0x8d89
  val GL_RGBA8I                                        = 0x8d8e
  val GL_RGB8I                                         = 0x8d8f
  val GL_RED_INTEGER                                   = 0x8d94
  val GL_RGB_INTEGER                                   = 0x8d98
  val GL_RGBA_INTEGER                                  = 0x8d99
  val GL_SAMPLER_2D_ARRAY                              = 0x8dc1
  val GL_SAMPLER_2D_ARRAY_SHADOW                       = 0x8dc4
  val GL_SAMPLER_CUBE_SHADOW                           = 0x8dc5
  val GL_UNSIGNED_INT_VEC2                             = 0x8dc6
  val GL_UNSIGNED_INT_VEC3                             = 0x8dc7
  val GL_UNSIGNED_INT_VEC4                             = 0x8dc8
  val GL_INT_SAMPLER_2D                                = 0x8dca
  val GL_INT_SAMPLER_3D                                = 0x8dcb
  val GL_INT_SAMPLER_CUBE                              = 0x8dcc
  val GL_INT_SAMPLER_2D_ARRAY                          = 0x8dcf
  val GL_UNSIGNED_INT_SAMPLER_2D                       = 0x8dd2
  val GL_UNSIGNED_INT_SAMPLER_3D                       = 0x8dd3
  val GL_UNSIGNED_INT_SAMPLER_CUBE                     = 0x8dd4
  val GL_UNSIGNED_INT_SAMPLER_2D_ARRAY                 = 0x8dd7
  val GL_BUFFER_ACCESS_FLAGS                           = 0x911f
  val GL_BUFFER_MAP_LENGTH                             = 0x9120
  val GL_BUFFER_MAP_OFFSET                             = 0x9121
  val GL_DEPTH_COMPONENT32F                            = 0x8cac
  val GL_DEPTH32F_STENCIL8                             = 0x8cad
  val GL_FLOAT_32_UNSIGNED_INT_24_8_REV                = 0x8dad
  val GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING         = 0x8210
  val GL_FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE         = 0x8211
  val GL_FRAMEBUFFER_ATTACHMENT_RED_SIZE               = 0x8212
  val GL_FRAMEBUFFER_ATTACHMENT_GREEN_SIZE             = 0x8213
  val GL_FRAMEBUFFER_ATTACHMENT_BLUE_SIZE              = 0x8214
  val GL_FRAMEBUFFER_ATTACHMENT_ALPHA_SIZE             = 0x8215
  val GL_FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE             = 0x8216
  val GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE           = 0x8217
  val GL_FRAMEBUFFER_DEFAULT                           = 0x8218
  val GL_FRAMEBUFFER_UNDEFINED                         = 0x8219
  val GL_DEPTH_STENCIL_ATTACHMENT                      = 0x821a
  val GL_DEPTH_STENCIL                                 = 0x84f9
  val GL_UNSIGNED_INT_24_8                             = 0x84fa
  val GL_DEPTH24_STENCIL8                              = 0x88f0
  val GL_UNSIGNED_NORMALIZED                           = 0x8c17
  val GL_DRAW_FRAMEBUFFER_BINDING                      = GL20.GL_FRAMEBUFFER_BINDING
  val GL_READ_FRAMEBUFFER                              = 0x8ca8
  val GL_DRAW_FRAMEBUFFER                              = 0x8ca9
  val GL_READ_FRAMEBUFFER_BINDING                      = 0x8caa
  val GL_RENDERBUFFER_SAMPLES                          = 0x8cab
  val GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LAYER          = 0x8cd4
  val GL_MAX_COLOR_ATTACHMENTS                         = 0x8cdf
  val GL_COLOR_ATTACHMENT1                             = 0x8ce1
  val GL_COLOR_ATTACHMENT2                             = 0x8ce2
  val GL_COLOR_ATTACHMENT3                             = 0x8ce3
  val GL_COLOR_ATTACHMENT4                             = 0x8ce4
  val GL_COLOR_ATTACHMENT5                             = 0x8ce5
  val GL_COLOR_ATTACHMENT6                             = 0x8ce6
  val GL_COLOR_ATTACHMENT7                             = 0x8ce7
  val GL_COLOR_ATTACHMENT8                             = 0x8ce8
  val GL_COLOR_ATTACHMENT9                             = 0x8ce9
  val GL_COLOR_ATTACHMENT10                            = 0x8cea
  val GL_COLOR_ATTACHMENT11                            = 0x8ceb
  val GL_COLOR_ATTACHMENT12                            = 0x8cec
  val GL_COLOR_ATTACHMENT13                            = 0x8ced
  val GL_COLOR_ATTACHMENT14                            = 0x8cee
  val GL_COLOR_ATTACHMENT15                            = 0x8cef
  val GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE            = 0x8d56
  val GL_MAX_SAMPLES                                   = 0x8d57
  val GL_HALF_FLOAT                                    = 0x140b
  val GL_MAP_READ_BIT                                  = 0x0001
  val GL_MAP_WRITE_BIT                                 = 0x0002
  val GL_MAP_INVALIDATE_RANGE_BIT                      = 0x0004
  val GL_MAP_INVALIDATE_BUFFER_BIT                     = 0x0008
  val GL_MAP_FLUSH_EXPLICIT_BIT                        = 0x0010
  val GL_MAP_UNSYNCHRONIZED_BIT                        = 0x0020
  val GL_RG                                            = 0x8227
  val GL_RG_INTEGER                                    = 0x8228
  val GL_R8                                            = 0x8229
  val GL_RG8                                           = 0x822b
  val GL_R16F                                          = 0x822d
  val GL_R32F                                          = 0x822e
  val GL_RG16F                                         = 0x822f
  val GL_RG32F                                         = 0x8230
  val GL_R8I                                           = 0x8231
  val GL_R8UI                                          = 0x8232
  val GL_R16I                                          = 0x8233
  val GL_R16UI                                         = 0x8234
  val GL_R32I                                          = 0x8235
  val GL_R32UI                                         = 0x8236
  val GL_RG8I                                          = 0x8237
  val GL_RG8UI                                         = 0x8238
  val GL_RG16I                                         = 0x8239
  val GL_RG16UI                                        = 0x823a
  val GL_RG32I                                         = 0x823b
  val GL_RG32UI                                        = 0x823c
  val GL_VERTEX_ARRAY_BINDING                          = 0x85b5
  val GL_R8_SNORM                                      = 0x8f94
  val GL_RG8_SNORM                                     = 0x8f95
  val GL_RGB8_SNORM                                    = 0x8f96
  val GL_RGBA8_SNORM                                   = 0x8f97
  val GL_SIGNED_NORMALIZED                             = 0x8f9c
  val GL_PRIMITIVE_RESTART_FIXED_INDEX                 = 0x8d69
  val GL_COPY_READ_BUFFER                              = 0x8f36
  val GL_COPY_WRITE_BUFFER                             = 0x8f37
  val GL_COPY_READ_BUFFER_BINDING                      = GL_COPY_READ_BUFFER
  val GL_COPY_WRITE_BUFFER_BINDING                     = GL_COPY_WRITE_BUFFER
  val GL_UNIFORM_BUFFER                                = 0x8a11
  val GL_UNIFORM_BUFFER_BINDING                        = 0x8a28
  val GL_UNIFORM_BUFFER_START                          = 0x8a29
  val GL_UNIFORM_BUFFER_SIZE                           = 0x8a2a
  val GL_MAX_VERTEX_UNIFORM_BLOCKS                     = 0x8a2b
  val GL_MAX_FRAGMENT_UNIFORM_BLOCKS                   = 0x8a2d
  val GL_MAX_COMBINED_UNIFORM_BLOCKS                   = 0x8a2e
  val GL_MAX_UNIFORM_BUFFER_BINDINGS                   = 0x8a2f
  val GL_MAX_UNIFORM_BLOCK_SIZE                        = 0x8a30
  val GL_MAX_COMBINED_VERTEX_UNIFORM_COMPONENTS        = 0x8a31
  val GL_MAX_COMBINED_FRAGMENT_UNIFORM_COMPONENTS      = 0x8a33
  val GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT               = 0x8a34
  val GL_ACTIVE_UNIFORM_BLOCK_MAX_NAME_LENGTH          = 0x8a35
  val GL_ACTIVE_UNIFORM_BLOCKS                         = 0x8a36
  val GL_UNIFORM_TYPE                                  = 0x8a37
  val GL_UNIFORM_SIZE                                  = 0x8a38
  val GL_UNIFORM_NAME_LENGTH                           = 0x8a39
  val GL_UNIFORM_BLOCK_INDEX                           = 0x8a3a
  val GL_UNIFORM_OFFSET                                = 0x8a3b
  val GL_UNIFORM_ARRAY_STRIDE                          = 0x8a3c
  val GL_UNIFORM_MATRIX_STRIDE                         = 0x8a3d
  val GL_UNIFORM_IS_ROW_MAJOR                          = 0x8a3e
  val GL_UNIFORM_BLOCK_BINDING                         = 0x8a3f
  val GL_UNIFORM_BLOCK_DATA_SIZE                       = 0x8a40
  val GL_UNIFORM_BLOCK_NAME_LENGTH                     = 0x8a41
  val GL_UNIFORM_BLOCK_ACTIVE_UNIFORMS                 = 0x8a42
  val GL_UNIFORM_BLOCK_ACTIVE_UNIFORM_INDICES          = 0x8a43
  val GL_UNIFORM_BLOCK_REFERENCED_BY_VERTEX_SHADER     = 0x8a44
  val GL_UNIFORM_BLOCK_REFERENCED_BY_FRAGMENT_SHADER   = 0x8a46
  // GL_INVALID_INDEX is defined as 0xFFFFFFFFu in C.
  val GL_INVALID_INDEX                 = -1
  val GL_MAX_VERTEX_OUTPUT_COMPONENTS  = 0x9122
  val GL_MAX_FRAGMENT_INPUT_COMPONENTS = 0x9125
  val GL_MAX_SERVER_WAIT_TIMEOUT       = 0x9111
  val GL_OBJECT_TYPE                   = 0x9112
  val GL_SYNC_CONDITION                = 0x9113
  val GL_SYNC_STATUS                   = 0x9114
  val GL_SYNC_FLAGS                    = 0x9115
  val GL_SYNC_FENCE                    = 0x9116
  val GL_SYNC_GPU_COMMANDS_COMPLETE    = 0x9117
  val GL_UNSIGNALED                    = 0x9118
  val GL_SIGNALED                      = 0x9119
  val GL_ALREADY_SIGNALED              = 0x911a
  val GL_TIMEOUT_EXPIRED               = 0x911b
  val GL_CONDITION_SATISFIED           = 0x911c
  val GL_WAIT_FAILED                   = 0x911d
  val GL_SYNC_FLUSH_COMMANDS_BIT       = 0x00000001
  // GL_TIMEOUT_IGNORED is defined as 0xFFFFFFFFFFFFFFFFull in C.
  val GL_TIMEOUT_IGNORED                           = -1L
  val GL_VERTEX_ATTRIB_ARRAY_DIVISOR               = 0x88fe
  val GL_ANY_SAMPLES_PASSED                        = 0x8c2f
  val GL_ANY_SAMPLES_PASSED_CONSERVATIVE           = 0x8d6a
  val GL_SAMPLER_BINDING                           = 0x8919
  val GL_RGB10_A2UI                                = 0x906f
  val GL_TEXTURE_SWIZZLE_R                         = 0x8e42
  val GL_TEXTURE_SWIZZLE_G                         = 0x8e43
  val GL_TEXTURE_SWIZZLE_B                         = 0x8e44
  val GL_TEXTURE_SWIZZLE_A                         = 0x8e45
  val GL_GREEN                                     = 0x1904
  val GL_BLUE                                      = 0x1905
  val GL_INT_2_10_10_10_REV                        = 0x8d9f
  val GL_TRANSFORM_FEEDBACK                        = 0x8e22
  val GL_TRANSFORM_FEEDBACK_PAUSED                 = 0x8e23
  val GL_TRANSFORM_FEEDBACK_ACTIVE                 = 0x8e24
  val GL_TRANSFORM_FEEDBACK_BINDING                = 0x8e25
  val GL_PROGRAM_BINARY_RETRIEVABLE_HINT           = 0x8257
  val GL_PROGRAM_BINARY_LENGTH                     = 0x8741
  val GL_NUM_PROGRAM_BINARY_FORMATS                = 0x87fe
  val GL_PROGRAM_BINARY_FORMATS                    = 0x87ff
  val GL_COMPRESSED_R11_EAC                        = 0x9270
  val GL_COMPRESSED_SIGNED_R11_EAC                 = 0x9271
  val GL_COMPRESSED_RG11_EAC                       = 0x9272
  val GL_COMPRESSED_SIGNED_RG11_EAC                = 0x9273
  val GL_COMPRESSED_RGB8_ETC2                      = 0x9274
  val GL_COMPRESSED_SRGB8_ETC2                     = 0x9275
  val GL_COMPRESSED_RGB8_PUNCHTHROUGH_ALPHA1_ETC2  = 0x9276
  val GL_COMPRESSED_SRGB8_PUNCHTHROUGH_ALPHA1_ETC2 = 0x9277
  val GL_COMPRESSED_RGBA8_ETC2_EAC                 = 0x9278
  val GL_COMPRESSED_SRGB8_ALPHA8_ETC2_EAC          = 0x9279
  val GL_TEXTURE_IMMUTABLE_FORMAT                  = 0x912f
  val GL_MAX_ELEMENT_INDEX                         = 0x8d6b
  val GL_NUM_SAMPLE_COUNTS                         = 0x9380
  val GL_TEXTURE_IMMUTABLE_LEVELS                  = 0x82df
}
