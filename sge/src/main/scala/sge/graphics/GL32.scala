/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/GL32.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: DebugProc Java interface → inner trait
 *   Idiom: split packages
 *   Convention: typed GL enums
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 650
 * Covenant-baseline-methods: DebugProc,GL32,GL_BUFFER,GL_CLAMP_TO_BORDER,GL_COLORBURN,GL_COLORDODGE,GL_COMPRESSED_RGBA_ASTC_10x10,GL_COMPRESSED_RGBA_ASTC_10x5,GL_COMPRESSED_RGBA_ASTC_10x6,GL_COMPRESSED_RGBA_ASTC_10x8,GL_COMPRESSED_RGBA_ASTC_12x10,GL_COMPRESSED_RGBA_ASTC_12x12,GL_COMPRESSED_RGBA_ASTC_4x4,GL_COMPRESSED_RGBA_ASTC_5x4,GL_COMPRESSED_RGBA_ASTC_5x5,GL_COMPRESSED_RGBA_ASTC_6x5,GL_COMPRESSED_RGBA_ASTC_6x6,GL_COMPRESSED_RGBA_ASTC_8x5,GL_COMPRESSED_RGBA_ASTC_8x6,GL_COMPRESSED_RGBA_ASTC_8x8,GL_COMPRESSED_SRGB8_ALPHA8_ASTC_10x10,GL_COMPRESSED_SRGB8_ALPHA8_ASTC_10x5,GL_COMPRESSED_SRGB8_ALPHA8_ASTC_10x6,GL_COMPRESSED_SRGB8_ALPHA8_ASTC_10x8,GL_COMPRESSED_SRGB8_ALPHA8_ASTC_12x10,GL_COMPRESSED_SRGB8_ALPHA8_ASTC_12x12,GL_COMPRESSED_SRGB8_ALPHA8_ASTC_4x4,GL_COMPRESSED_SRGB8_ALPHA8_ASTC_5x4,GL_COMPRESSED_SRGB8_ALPHA8_ASTC_5x5,GL_COMPRESSED_SRGB8_ALPHA8_ASTC_6x5,GL_COMPRESSED_SRGB8_ALPHA8_ASTC_6x6,GL_COMPRESSED_SRGB8_ALPHA8_ASTC_8x5,GL_COMPRESSED_SRGB8_ALPHA8_ASTC_8x6,GL_COMPRESSED_SRGB8_ALPHA8_ASTC_8x8,GL_CONTEXT_FLAGS,GL_CONTEXT_FLAG_DEBUG_BIT,GL_CONTEXT_FLAG_ROBUST_ACCESS_BIT,GL_CONTEXT_LOST,GL_DARKEN,GL_DEBUG_CALLBACK_FUNCTION,GL_DEBUG_CALLBACK_USER_PARAM,GL_DEBUG_GROUP_STACK_DEPTH,GL_DEBUG_LOGGED_MESSAGES,GL_DEBUG_NEXT_LOGGED_MESSAGE_LENGTH,GL_DEBUG_OUTPUT,GL_DEBUG_OUTPUT_SYNCHRONOUS,GL_DEBUG_SEVERITY_HIGH,GL_DEBUG_SEVERITY_LOW,GL_DEBUG_SEVERITY_MEDIUM,GL_DEBUG_SEVERITY_NOTIFICATION,GL_DEBUG_SOURCE_API,GL_DEBUG_SOURCE_APPLICATION,GL_DEBUG_SOURCE_OTHER,GL_DEBUG_SOURCE_SHADER_COMPILER,GL_DEBUG_SOURCE_THIRD_PARTY,GL_DEBUG_SOURCE_WINDOW_SYSTEM,GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR,GL_DEBUG_TYPE_ERROR,GL_DEBUG_TYPE_MARKER,GL_DEBUG_TYPE_OTHER,GL_DEBUG_TYPE_PERFORMANCE,GL_DEBUG_TYPE_POP_GROUP,GL_DEBUG_TYPE_PORTABILITY,GL_DEBUG_TYPE_PUSH_GROUP,GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR,GL_DIFFERENCE,GL_EXCLUSION,GL_FIRST_VERTEX_CONVENTION,GL_FRACTIONAL_EVEN,GL_FRACTIONAL_ODD,GL_FRAGMENT_INTERPOLATION_OFFSET_BITS,GL_FRAMEBUFFER_ATTACHMENT_LAYERED,GL_FRAMEBUFFER_DEFAULT_LAYERS,GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS,GL_GEOMETRY_INPUT_TYPE,GL_GEOMETRY_OUTPUT_TYPE,GL_GEOMETRY_SHADER,GL_GEOMETRY_SHADER_BIT,GL_GEOMETRY_SHADER_INVOCATIONS,GL_GEOMETRY_VERTICES_OUT,GL_GUILTY_CONTEXT_RESET,GL_HARDLIGHT,GL_HSL_COLOR,GL_HSL_HUE,GL_HSL_LUMINOSITY,GL_HSL_SATURATION,GL_IMAGE_BUFFER,GL_IMAGE_CUBE_MAP_ARRAY,GL_INNOCENT_CONTEXT_RESET,GL_INT_IMAGE_BUFFER,GL_INT_IMAGE_CUBE_MAP_ARRAY,GL_INT_SAMPLER_2D_MULTISAMPLE_ARRAY,GL_INT_SAMPLER_BUFFER,GL_INT_SAMPLER_CUBE_MAP_ARRAY,GL_ISOLINES,GL_IS_PER_PATCH,GL_LAST_VERTEX_CONVENTION,GL_LAYER_PROVOKING_VERTEX,GL_LIGHTEN,GL_LINES_ADJACENCY,GL_LINE_STRIP_ADJACENCY,GL_LOSE_CONTEXT_ON_RESET,GL_MAX_COMBINED_GEOMETRY_UNIFORM_COMPONENTS,GL_MAX_COMBINED_TESS_CONTROL_UNIFORM_COMPONENTS,GL_MAX_COMBINED_TESS_EVALUATION_UNIFORM_COMPONENTS,GL_MAX_DEBUG_GROUP_STACK_DEPTH,GL_MAX_DEBUG_LOGGED_MESSAGES,GL_MAX_DEBUG_MESSAGE_LENGTH,GL_MAX_FRAGMENT_INTERPOLATION_OFFSET,GL_MAX_FRAMEBUFFER_LAYERS,GL_MAX_GEOMETRY_ATOMIC_COUNTERS,GL_MAX_GEOMETRY_ATOMIC_COUNTER_BUFFERS,GL_MAX_GEOMETRY_IMAGE_UNIFORMS,GL_MAX_GEOMETRY_INPUT_COMPONENTS,GL_MAX_GEOMETRY_OUTPUT_COMPONENTS,GL_MAX_GEOMETRY_OUTPUT_VERTICES,GL_MAX_GEOMETRY_SHADER_INVOCATIONS,GL_MAX_GEOMETRY_SHADER_STORAGE_BLOCKS,GL_MAX_GEOMETRY_TEXTURE_IMAGE_UNITS,GL_MAX_GEOMETRY_TOTAL_OUTPUT_COMPONENTS,GL_MAX_GEOMETRY_UNIFORM_BLOCKS,GL_MAX_GEOMETRY_UNIFORM_COMPONENTS,GL_MAX_LABEL_LENGTH,GL_MAX_PATCH_VERTICES,GL_MAX_TESS_CONTROL_ATOMIC_COUNTERS,GL_MAX_TESS_CONTROL_ATOMIC_COUNTER_BUFFERS,GL_MAX_TESS_CONTROL_IMAGE_UNIFORMS,GL_MAX_TESS_CONTROL_INPUT_COMPONENTS,GL_MAX_TESS_CONTROL_OUTPUT_COMPONENTS,GL_MAX_TESS_CONTROL_SHADER_STORAGE_BLOCKS,GL_MAX_TESS_CONTROL_TEXTURE_IMAGE_UNITS,GL_MAX_TESS_CONTROL_TOTAL_OUTPUT_COMPONENTS,GL_MAX_TESS_CONTROL_UNIFORM_BLOCKS,GL_MAX_TESS_CONTROL_UNIFORM_COMPONENTS,GL_MAX_TESS_EVALUATION_ATOMIC_COUNTERS,GL_MAX_TESS_EVALUATION_ATOMIC_COUNTER_BUFFERS,GL_MAX_TESS_EVALUATION_IMAGE_UNIFORMS,GL_MAX_TESS_EVALUATION_INPUT_COMPONENTS,GL_MAX_TESS_EVALUATION_OUTPUT_COMPONENTS,GL_MAX_TESS_EVALUATION_SHADER_STORAGE_BLOCKS,GL_MAX_TESS_EVALUATION_TEXTURE_IMAGE_UNITS,GL_MAX_TESS_EVALUATION_UNIFORM_BLOCKS,GL_MAX_TESS_EVALUATION_UNIFORM_COMPONENTS,GL_MAX_TESS_GEN_LEVEL,GL_MAX_TESS_PATCH_COMPONENTS,GL_MAX_TEXTURE_BUFFER_SIZE,GL_MIN_FRAGMENT_INTERPOLATION_OFFSET,GL_MIN_SAMPLE_SHADING_VALUE,GL_MULTIPLY,GL_MULTISAMPLE_LINE_WIDTH_GRANULARITY,GL_MULTISAMPLE_LINE_WIDTH_RANGE,GL_NO_RESET_NOTIFICATION,GL_OVERLAY,GL_PATCHES,GL_PATCH_VERTICES,GL_PRIMITIVES_GENERATED,GL_PRIMITIVE_BOUNDING_BOX,GL_PRIMITIVE_RESTART_FOR_PATCHES_SUPPORTED,GL_PROGRAM,GL_PROGRAM_PIPELINE,GL_QUADS,GL_QUERY,GL_REFERENCED_BY_GEOMETRY_SHADER,GL_REFERENCED_BY_TESS_CONTROL_SHADER,GL_REFERENCED_BY_TESS_EVALUATION_SHADER,GL_RESET_NOTIFICATION_STRATEGY,GL_SAMPLER,GL_SAMPLER_2D_MULTISAMPLE_ARRAY,GL_SAMPLER_BUFFER,GL_SAMPLER_CUBE_MAP_ARRAY,GL_SAMPLER_CUBE_MAP_ARRAY_SHADOW,GL_SAMPLE_SHADING,GL_SCREEN,GL_SHADER,GL_SOFTLIGHT,GL_STACK_OVERFLOW,GL_STACK_UNDERFLOW,GL_TESS_CONTROL_OUTPUT_VERTICES,GL_TESS_CONTROL_SHADER,GL_TESS_CONTROL_SHADER_BIT,GL_TESS_EVALUATION_SHADER,GL_TESS_EVALUATION_SHADER_BIT,GL_TESS_GEN_MODE,GL_TESS_GEN_POINT_MODE,GL_TESS_GEN_SPACING,GL_TESS_GEN_VERTEX_ORDER,GL_TEXTURE_2D_MULTISAMPLE_ARRAY,GL_TEXTURE_BINDING_2D_MULTISAMPLE_ARRAY,GL_TEXTURE_BINDING_BUFFER,GL_TEXTURE_BINDING_CUBE_MAP_ARRAY,GL_TEXTURE_BORDER_COLOR,GL_TEXTURE_BUFFER,GL_TEXTURE_BUFFER_BINDING,GL_TEXTURE_BUFFER_DATA_STORE_BINDING,GL_TEXTURE_BUFFER_OFFSET,GL_TEXTURE_BUFFER_OFFSET_ALIGNMENT,GL_TEXTURE_BUFFER_SIZE,GL_TEXTURE_CUBE_MAP_ARRAY,GL_TRIANGLES_ADJACENCY,GL_TRIANGLE_STRIP_ADJACENCY,GL_UNDEFINED_VERTEX,GL_UNKNOWN_CONTEXT_RESET,GL_UNSIGNED_INT_IMAGE_BUFFER,GL_UNSIGNED_INT_IMAGE_CUBE_MAP_ARRAY,GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE_ARRAY,GL_UNSIGNED_INT_SAMPLER_BUFFER,GL_UNSIGNED_INT_SAMPLER_CUBE_MAP_ARRAY,GL_VERTEX_ARRAY,glBlendBarrier,glBlendEquationSeparatei,glBlendEquationi,glBlendFuncSeparatei,glBlendFunci,glColorMaski,glCopyImageSubData,glDebugMessageCallback,glDebugMessageControl,glDebugMessageInsert,glDisablei,glDrawElementsBaseVertex,glDrawElementsInstancedBaseVertex,glDrawRangeElementsBaseVertex,glEnablei,glFramebufferTexture,glGetDebugMessageLog,glGetGraphicsResetStatus,glGetObjectLabel,glGetPointerv,glGetSamplerParameterIiv,glGetSamplerParameterIuiv,glGetTexParameterIiv,glGetTexParameterIuiv,glGetnUniformfv,glGetnUniformiv,glGetnUniformuiv,glIsEnabledi,glMinSampleShading,glObjectLabel,glPatchParameteri,glPopDebugGroup,glPushDebugGroup,glReadnPixels,glSamplerParameterIiv,glSamplerParameterIuiv,glTexBuffer,glTexBufferRange,glTexParameterIiv,glTexParameterIuiv,glTexStorage3DMultisample,onMessage
 * Covenant-source-reference: com/badlogic/gdx/graphics/GL32.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 5f2dae5f6879556f663774ba9d9ff439b5bae822
 */
package sge
package graphics

import java.nio.{ Buffer, FloatBuffer, IntBuffer }

trait GL32 extends GL31 {

  // C function void glBlendBarrier ( void )

  def glBlendBarrier(): Unit

  // C function void glCopyImageSubData ( GLuint srcName, GLenum srcTarget, GLint srcLevel, GLint srcX, GLint srcY, GLint srcZ,
  // GLuint dstName, GLenum dstTarget, GLint dstLevel, GLint dstX, GLint dstY, GLint dstZ, GLsizei srcWidth, GLsizei srcHeight,
  // GLsizei srcDepth )

  def glCopyImageSubData(
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
  ): Unit

  // C function void glDebugMessageControl ( GLenum source, GLenum type, GLenum severity, GLsizei count, const GLuint *ids,
  // GLboolean enabled )

// void glDebugMessageControl(
// int source,
// int type,
// int severity,
// int count,
// int[] ids,
// int offset,
// boolean enabled
// );

  // C function void glDebugMessageControl ( GLenum source, GLenum type, GLenum severity, GLsizei count, const GLuint *ids,
  // GLboolean enabled )

  def glDebugMessageControl(
    source:   Int,
    `type`:   Int,
    severity: Int,
// int count,
    ids:     IntBuffer,
    enabled: Boolean
  ): Unit

  // C function void glDebugMessageInsert ( GLenum source, GLenum type, GLuint id, GLenum severity, GLsizei length, const GLchar
  // *buf )

  def glDebugMessageInsert(
    source:   Int,
    `type`:   Int,
    id:       Int,
    severity: Int,
// int length
    buf: String
  ): Unit

  // C function void glDebugMessageCallback ( GLDEBUGPROC callback, const void *userParam )

  trait DebugProc {
    def onMessage(source: Int, `type`: Int, id: Int, severity: Int, message: String): Unit
  }

  def glDebugMessageCallback(callback: DebugProc): Unit

  // C function GLuint glGetDebugMessageLog ( GLuint count, GLsizei bufSize, GLenum *sources, GLenum *types, GLuint *ids, GLenum
  // *severities, GLsizei *lengths, GLchar *messageLog )

// int glGetDebugMessageLog(
// int count,
// int bufSize,
// int[] sources,
// int sourcesOffset,
// int[] types,
// int typesOffset,
// int[] ids,
// int idsOffset,
// int[] severities,
// int severitiesOffset,
// int[] lengths,
// int lengthsOffset,
// byte[] messageLog,
// int messageLogOffset);

  // C function GLuint glGetDebugMessageLog ( GLuint count, GLsizei bufSize, GLenum *sources, GLenum *types, GLuint *ids, GLenum
  // *severities, GLsizei *lengths, GLchar *messageLog )

  def glGetDebugMessageLog(count: Int, sources: IntBuffer, types: IntBuffer, ids: IntBuffer, severities: IntBuffer, lengths: IntBuffer, messageLog: java.nio.ByteBuffer): Int

  // C function GLuint glGetDebugMessageLog ( GLuint count, GLsizei bufSize, GLenum *sources, GLenum *types, GLuint *ids, GLenum
  // *severities, GLsizei *lengths, GLchar *messageLog )

// String[] glGetDebugMessageLog(
// int count,
// int[] sources,
// int sourcesOffset,
// int[] types,
// int typesOffset,
// int[] ids,
// int idsOffset,
// int[] severities,
// int severitiesOffset);

  // C function GLuint glGetDebugMessageLog ( GLuint count, GLsizei bufSize, GLenum *sources, GLenum *types, GLuint *ids, GLenum
  // *severities, GLsizei *lengths, GLchar *messageLog )

// String[] glGetDebugMessageLog(
// int count,
// java.nio.IntBuffer sources,
// java.nio.IntBuffer types,
// java.nio.IntBuffer ids,
// java.nio.IntBuffer severities);

  // C function void glPushDebugGroup ( GLenum source, GLuint id, GLsizei length, const GLchar *message )

  def glPushDebugGroup(
    source: Int,
    id:     Int,
// int length,
    message: String
  ): Unit

  // C function void glPopDebugGroup ( void )

  def glPopDebugGroup(): Unit

  // C function void glObjectLabel ( GLenum identifier, GLuint name, GLsizei length, const GLchar *label )

  def glObjectLabel(
    identifier: Int,
    name:       Int,
// int length,
    label: String
  ): Unit

  // C function void glGetObjectLabel ( GLenum identifier, GLuint name, GLsizei bufSize, GLsizei *length, GLchar *label )

  def glGetObjectLabel(identifier: Int, name: Int): String

  // C function void glObjectPtrLabel ( const void *ptr, GLsizei length, const GLchar *label )

// void glObjectPtrLabel (long ptr, String label);

  // C function void glGetObjectPtrLabel ( const void *ptr, GLsizei bufSize, GLsizei *length, GLchar *label )

// String glGetObjectPtrLabel (long ptr);

  // C function void glGetPointerv ( GLenum pname, void **params )

  def glGetPointerv(pname: Int): Long

  // C function void glEnablei ( GLenum target, GLuint index )

  def glEnablei(target: Int, index: Int): Unit

  // C function void glDisablei ( GLenum target, GLuint index )

  def glDisablei(target: Int, index: Int): Unit

  // C function void glBlendEquationi ( GLuint buf, GLenum mode )

  def glBlendEquationi(buf: Int, mode: BlendEquation): Unit

  // C function void glBlendEquationSeparatei ( GLuint buf, GLenum modeRGB, GLenum modeAlpha )

  def glBlendEquationSeparatei(buf: Int, modeRGB: BlendEquation, modeAlpha: BlendEquation): Unit

  // C function void glBlendFunci ( GLuint buf, GLenum src, GLenum dst )

  def glBlendFunci(buf: Int, src: BlendFactor, dst: BlendFactor): Unit

  // C function void glBlendFuncSeparatei ( GLuint buf, GLenum srcRGB, GLenum dstRGB, GLenum srcAlpha, GLenum dstAlpha )

  def glBlendFuncSeparatei(buf: Int, srcRGB: BlendFactor, dstRGB: BlendFactor, srcAlpha: BlendFactor, dstAlpha: BlendFactor): Unit

  // C function void glColorMaski ( GLuint index, GLboolean r, GLboolean g, GLboolean b, GLboolean a )

  def glColorMaski(index: Int, r: Boolean, g: Boolean, b: Boolean, a: Boolean): Unit

  // C function GLboolean glIsEnabledi ( GLenum target, GLuint index )

  def glIsEnabledi(target: Int, index: Int): Boolean

  // C function void glDrawElementsBaseVertex ( GLenum mode, GLsizei count, GLenum type, const void *indices, GLint basevertex )

  def glDrawElementsBaseVertex(mode: PrimitiveMode, count: Int, `type`: DataType, indices: Buffer, basevertex: Int): Unit

  // C function void glDrawRangeElementsBaseVertex ( GLenum mode, GLuint start, GLuint end, GLsizei count, GLenum type, const
  // void *indices, GLint basevertex )

  def glDrawRangeElementsBaseVertex(mode: PrimitiveMode, start: Int, end: Int, count: Int, `type`: DataType, indices: Buffer, basevertex: Int): Unit

  // C function void glDrawElementsInstancedBaseVertex ( GLenum mode, GLsizei count, GLenum type, const void *indices, GLsizei
  // instanceCount, GLint basevertex )

  def glDrawElementsInstancedBaseVertex(mode: PrimitiveMode, count: Int, `type`: DataType, indices: Buffer, instanceCount: Int, basevertex: Int): Unit

  // C function void glDrawElementsInstancedBaseVertex ( GLenum mode, GLsizei count, GLenum type, const void *indices, GLsizei
  // instanceCount, GLint basevertex )

  def glDrawElementsInstancedBaseVertex(mode: PrimitiveMode, count: Int, `type`: DataType, indicesOffset: Int, instanceCount: Int, basevertex: Int): Unit

  // C function void glFramebufferTexture ( GLenum target, GLenum attachment, GLuint texture, GLint level )

  def glFramebufferTexture(target: Int, attachment: Int, texture: Int, level: Int): Unit

  // C function void glPrimitiveBoundingBox ( GLfloat minX, GLfloat minY, GLfloat minZ, GLfloat minW, GLfloat maxX, GLfloat maxY,
  // GLfloat maxZ, GLfloat maxW )

// void glPrimitiveBoundingBox(
// float minX,
// float minY,
// float minZ,
// float minW,
// float maxX,
// float maxY,
// float maxZ,
// float maxW
// );

  // C function GLenum glGetGraphicsResetStatus ( void )

  def glGetGraphicsResetStatus(): Int

  // C function void glReadnPixels ( GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLsizei
  // bufSize, void *data )

  def glReadnPixels(x: Int, y: Int, width: Int, height: Int, format: PixelFormat, `type`: DataType, bufSize: Int, data: Buffer): Unit

  // C function void glGetnUniformfv ( GLuint program, GLint location, GLsizei bufSize, GLfloat *params )

// void glGetnUniformfv(
// int program,
// int location,
// int bufSize,
// float[] params,
// int offset
// );

  // C function void glGetnUniformfv ( GLuint program, GLint location, GLsizei bufSize, GLfloat *params )

  def glGetnUniformfv(
    program:  Int,
    location: Int,
// int bufSize,
    params: FloatBuffer
  ): Unit

  // C function void glGetnUniformiv ( GLuint program, GLint location, GLsizei bufSize, GLint *params )

// void glGetnUniformiv(
// int program,
// int location,
// int bufSize,
// int[] params,
// int offset
// );

  // C function void glGetnUniformiv ( GLuint program, GLint location, GLsizei bufSize, GLint *params )

  def glGetnUniformiv(
    program:  Int,
    location: Int,
// int bufSize,
    params: IntBuffer
  ): Unit

  // C function void glGetnUniformuiv ( GLuint program, GLint location, GLsizei bufSize, GLuint *params )

// void glGetnUniformuiv(
// int program,
// int location,
// int bufSize,
// int[] params,
// int offset
// );

  // C function void glGetnUniformuiv ( GLuint program, GLint location, GLsizei bufSize, GLuint *params )

  def glGetnUniformuiv(
    program:  Int,
    location: Int,
// int bufSize,
    params: IntBuffer
  ): Unit

  // C function void glMinSampleShading ( GLfloat value )

  def glMinSampleShading(value: Float): Unit

  // C function void glPatchParameteri ( GLenum pname, GLint value )

  def glPatchParameteri(pname: Int, value: Int): Unit

  // C function void glTexParameterIiv ( GLenum target, GLenum pname, const GLint *params )

// void glTexParameterIiv(
// int target,
// int pname,
// int[] params,
// int offset
// );

  // C function void glTexParameterIiv ( GLenum target, GLenum pname, const GLint *params )

  def glTexParameterIiv(target: TextureTarget, pname: Int, params: IntBuffer): Unit

  // C function void glTexParameterIuiv ( GLenum target, GLenum pname, const GLuint *params )

// void glTexParameterIuiv(
// int target,
// int pname,
// int[] params,
// int offset
// );

  // C function void glTexParameterIuiv ( GLenum target, GLenum pname, const GLuint *params )

  def glTexParameterIuiv(target: TextureTarget, pname: Int, params: IntBuffer): Unit

  // C function void glGetTexParameterIiv ( GLenum target, GLenum pname, GLint *params )

// void glGetTexParameterIiv(
// int target,
// int pname,
// int[] params,
// int offset
// );

  // C function void glGetTexParameterIiv ( GLenum target, GLenum pname, GLint *params )

  def glGetTexParameterIiv(target: TextureTarget, pname: Int, params: IntBuffer): Unit

  // C function void glGetTexParameterIuiv ( GLenum target, GLenum pname, GLuint *params )

// void glGetTexParameterIuiv(
// int target,
// int pname,
// int[] params,
// int offset
// );

  // C function void glGetTexParameterIuiv ( GLenum target, GLenum pname, GLuint *params )

  def glGetTexParameterIuiv(target: TextureTarget, pname: Int, params: IntBuffer): Unit

  // C function void glSamplerParameterIiv ( GLuint sampler, GLenum pname, const GLint *param )

// void glSamplerParameterIiv(
// int sampler,
// int pname,
// int[] param,
// int offset
// );

  // C function void glSamplerParameterIiv ( GLuint sampler, GLenum pname, const GLint *param )

  def glSamplerParameterIiv(sampler: Int, pname: Int, param: IntBuffer): Unit

  // C function void glSamplerParameterIuiv ( GLuint sampler, GLenum pname, const GLuint *param )

// void glSamplerParameterIuiv(
// int sampler,
// int pname,
// int[] param,
// int offset
// );

  // C function void glSamplerParameterIuiv ( GLuint sampler, GLenum pname, const GLuint *param )

  def glSamplerParameterIuiv(sampler: Int, pname: Int, param: IntBuffer): Unit

  // C function void glGetSamplerParameterIiv ( GLuint sampler, GLenum pname, GLint *params )

// void glGetSamplerParameterIiv(
// int sampler,
// int pname,
// int[] params,
// int offset
// );

  // C function void glGetSamplerParameterIiv ( GLuint sampler, GLenum pname, GLint *params )

  def glGetSamplerParameterIiv(sampler: Int, pname: Int, params: IntBuffer): Unit

  // C function void glGetSamplerParameterIuiv ( GLuint sampler, GLenum pname, GLuint *params )

// void glGetSamplerParameterIuiv(
// int sampler,
// int pname,
// int[] params,
// int offset
// );

  // C function void glGetSamplerParameterIuiv ( GLuint sampler, GLenum pname, GLuint *params )

  def glGetSamplerParameterIuiv(sampler: Int, pname: Int, params: IntBuffer): Unit

  // C function void glTexBuffer ( GLenum target, GLenum internalformat, GLuint buffer )

  def glTexBuffer(target: TextureTarget, internalformat: Int, buffer: Int): Unit

  // C function void glTexBufferRange ( GLenum target, GLenum internalformat, GLuint buffer, GLintptr offset, GLsizeiptr size )

  def glTexBufferRange(target: TextureTarget, internalformat: Int, buffer: Int, offset: Int, size: Int): Unit

  // C function void glTexStorage3DMultisample ( GLenum target, GLsizei samples, GLenum internalformat, GLsizei width, GLsizei
  // height, GLsizei depth, GLboolean fixedsamplelocations )

  def glTexStorage3DMultisample(target: TextureTarget, samples: Int, internalformat: Int, width: Int, height: Int, depth: Int, fixedsamplelocations: Boolean): Unit
}

object GL32 {
  val GL_CONTEXT_FLAG_DEBUG_BIT = 0x00000002

  val GL_CONTEXT_FLAG_ROBUST_ACCESS_BIT = 0x00000004

  val GL_GEOMETRY_SHADER_BIT        = 0x00000004
  val GL_TESS_CONTROL_SHADER_BIT    = 0x00000008
  val GL_TESS_EVALUATION_SHADER_BIT = 0x00000010

  val GL_QUADS                                           = 0x0007
  val GL_LINES_ADJACENCY                                 = 0x000a
  val GL_LINE_STRIP_ADJACENCY                            = 0x000b
  val GL_TRIANGLES_ADJACENCY                             = 0x000c
  val GL_TRIANGLE_STRIP_ADJACENCY                        = 0x000d
  val GL_PATCHES                                         = 0x000e
  val GL_STACK_OVERFLOW                                  = 0x0503
  val GL_STACK_UNDERFLOW                                 = 0x0504
  val GL_CONTEXT_LOST                                    = 0x0507
  val GL_TEXTURE_BORDER_COLOR                            = 0x1004
  val GL_VERTEX_ARRAY                                    = 0x8074
  val GL_CLAMP_TO_BORDER                                 = 0x812d
  val GL_CONTEXT_FLAGS                                   = 0x821e
  val GL_PRIMITIVE_RESTART_FOR_PATCHES_SUPPORTED         = 0x8221
  val GL_DEBUG_OUTPUT_SYNCHRONOUS                        = 0x8242
  val GL_DEBUG_NEXT_LOGGED_MESSAGE_LENGTH                = 0x8243
  val GL_DEBUG_CALLBACK_FUNCTION                         = 0x8244
  val GL_DEBUG_CALLBACK_USER_PARAM                       = 0x8245
  val GL_DEBUG_SOURCE_API                                = 0x8246
  val GL_DEBUG_SOURCE_WINDOW_SYSTEM                      = 0x8247
  val GL_DEBUG_SOURCE_SHADER_COMPILER                    = 0x8248
  val GL_DEBUG_SOURCE_THIRD_PARTY                        = 0x8249
  val GL_DEBUG_SOURCE_APPLICATION                        = 0x824a
  val GL_DEBUG_SOURCE_OTHER                              = 0x824b
  val GL_DEBUG_TYPE_ERROR                                = 0x824c
  val GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR                  = 0x824d
  val GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR                   = 0x824e
  val GL_DEBUG_TYPE_PORTABILITY                          = 0x824f
  val GL_DEBUG_TYPE_PERFORMANCE                          = 0x8250
  val GL_DEBUG_TYPE_OTHER                                = 0x8251
  val GL_LOSE_CONTEXT_ON_RESET                           = 0x8252
  val GL_GUILTY_CONTEXT_RESET                            = 0x8253
  val GL_INNOCENT_CONTEXT_RESET                          = 0x8254
  val GL_UNKNOWN_CONTEXT_RESET                           = 0x8255
  val GL_RESET_NOTIFICATION_STRATEGY                     = 0x8256
  val GL_LAYER_PROVOKING_VERTEX                          = 0x825e
  val GL_UNDEFINED_VERTEX                                = 0x8260
  val GL_NO_RESET_NOTIFICATION                           = 0x8261
  val GL_DEBUG_TYPE_MARKER                               = 0x8268
  val GL_DEBUG_TYPE_PUSH_GROUP                           = 0x8269
  val GL_DEBUG_TYPE_POP_GROUP                            = 0x826a
  val GL_DEBUG_SEVERITY_NOTIFICATION                     = 0x826b
  val GL_MAX_DEBUG_GROUP_STACK_DEPTH                     = 0x826c
  val GL_DEBUG_GROUP_STACK_DEPTH                         = 0x826d
  val GL_BUFFER                                          = 0x82e0
  val GL_SHADER                                          = 0x82e1
  val GL_PROGRAM                                         = 0x82e2
  val GL_QUERY                                           = 0x82e3
  val GL_PROGRAM_PIPELINE                                = 0x82e4
  val GL_SAMPLER                                         = 0x82e6
  val GL_MAX_LABEL_LENGTH                                = 0x82e8
  val GL_MAX_TESS_CONTROL_INPUT_COMPONENTS               = 0x886c
  val GL_MAX_TESS_EVALUATION_INPUT_COMPONENTS            = 0x886d
  val GL_GEOMETRY_SHADER_INVOCATIONS                     = 0x887f
  val GL_GEOMETRY_VERTICES_OUT                           = 0x8916
  val GL_GEOMETRY_INPUT_TYPE                             = 0x8917
  val GL_GEOMETRY_OUTPUT_TYPE                            = 0x8918
  val GL_MAX_GEOMETRY_UNIFORM_BLOCKS                     = 0x8a2c
  val GL_MAX_COMBINED_GEOMETRY_UNIFORM_COMPONENTS        = 0x8a32
  val GL_MAX_GEOMETRY_TEXTURE_IMAGE_UNITS                = 0x8c29
  val GL_TEXTURE_BUFFER                                  = 0x8c2a
  val GL_TEXTURE_BUFFER_BINDING                          = 0x8c2a
  val GL_MAX_TEXTURE_BUFFER_SIZE                         = 0x8c2b
  val GL_TEXTURE_BINDING_BUFFER                          = 0x8c2c
  val GL_TEXTURE_BUFFER_DATA_STORE_BINDING               = 0x8c2d
  val GL_SAMPLE_SHADING                                  = 0x8c36
  val GL_MIN_SAMPLE_SHADING_VALUE                        = 0x8c37
  val GL_PRIMITIVES_GENERATED                            = 0x8c87
  val GL_FRAMEBUFFER_ATTACHMENT_LAYERED                  = 0x8da7
  val GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS            = 0x8da8
  val GL_SAMPLER_BUFFER                                  = 0x8dc2
  val GL_INT_SAMPLER_BUFFER                              = 0x8dd0
  val GL_UNSIGNED_INT_SAMPLER_BUFFER                     = 0x8dd8
  val GL_GEOMETRY_SHADER                                 = 0x8dd9
  val GL_MAX_GEOMETRY_UNIFORM_COMPONENTS                 = 0x8ddf
  val GL_MAX_GEOMETRY_OUTPUT_VERTICES                    = 0x8de0
  val GL_MAX_GEOMETRY_TOTAL_OUTPUT_COMPONENTS            = 0x8de1
  val GL_MAX_COMBINED_TESS_CONTROL_UNIFORM_COMPONENTS    = 0x8e1e
  val GL_MAX_COMBINED_TESS_EVALUATION_UNIFORM_COMPONENTS = 0x8e1f
  val GL_FIRST_VERTEX_CONVENTION                         = 0x8e4d
  val GL_LAST_VERTEX_CONVENTION                          = 0x8e4e
  val GL_MAX_GEOMETRY_SHADER_INVOCATIONS                 = 0x8e5a
  val GL_MIN_FRAGMENT_INTERPOLATION_OFFSET               = 0x8e5b
  val GL_MAX_FRAGMENT_INTERPOLATION_OFFSET               = 0x8e5c
  val GL_FRAGMENT_INTERPOLATION_OFFSET_BITS              = 0x8e5d
  val GL_PATCH_VERTICES                                  = 0x8e72
  val GL_TESS_CONTROL_OUTPUT_VERTICES                    = 0x8e75
  val GL_TESS_GEN_MODE                                   = 0x8e76
  val GL_TESS_GEN_SPACING                                = 0x8e77
  val GL_TESS_GEN_VERTEX_ORDER                           = 0x8e78
  val GL_TESS_GEN_POINT_MODE                             = 0x8e79
  val GL_ISOLINES                                        = 0x8e7a
  val GL_FRACTIONAL_ODD                                  = 0x8e7b
  val GL_FRACTIONAL_EVEN                                 = 0x8e7c
  val GL_MAX_PATCH_VERTICES                              = 0x8e7d
  val GL_MAX_TESS_GEN_LEVEL                              = 0x8e7e
  val GL_MAX_TESS_CONTROL_UNIFORM_COMPONENTS             = 0x8e7f
  val GL_MAX_TESS_EVALUATION_UNIFORM_COMPONENTS          = 0x8e80
  val GL_MAX_TESS_CONTROL_TEXTURE_IMAGE_UNITS            = 0x8e81
  val GL_MAX_TESS_EVALUATION_TEXTURE_IMAGE_UNITS         = 0x8e82
  val GL_MAX_TESS_CONTROL_OUTPUT_COMPONENTS              = 0x8e83
  val GL_MAX_TESS_PATCH_COMPONENTS                       = 0x8e84
  val GL_MAX_TESS_CONTROL_TOTAL_OUTPUT_COMPONENTS        = 0x8e85
  val GL_MAX_TESS_EVALUATION_OUTPUT_COMPONENTS           = 0x8e86
  val GL_TESS_EVALUATION_SHADER                          = 0x8e87
  val GL_TESS_CONTROL_SHADER                             = 0x8e88
  val GL_MAX_TESS_CONTROL_UNIFORM_BLOCKS                 = 0x8e89
  val GL_MAX_TESS_EVALUATION_UNIFORM_BLOCKS              = 0x8e8a
  val GL_TEXTURE_CUBE_MAP_ARRAY                          = 0x9009
  val GL_TEXTURE_BINDING_CUBE_MAP_ARRAY                  = 0x900a
  val GL_SAMPLER_CUBE_MAP_ARRAY                          = 0x900c
  val GL_SAMPLER_CUBE_MAP_ARRAY_SHADOW                   = 0x900d
  val GL_INT_SAMPLER_CUBE_MAP_ARRAY                      = 0x900e
  val GL_UNSIGNED_INT_SAMPLER_CUBE_MAP_ARRAY             = 0x900f
  val GL_IMAGE_BUFFER                                    = 0x9051
  val GL_IMAGE_CUBE_MAP_ARRAY                            = 0x9054
  val GL_INT_IMAGE_BUFFER                                = 0x905c
  val GL_INT_IMAGE_CUBE_MAP_ARRAY                        = 0x905f
  val GL_UNSIGNED_INT_IMAGE_BUFFER                       = 0x9067
  val GL_UNSIGNED_INT_IMAGE_CUBE_MAP_ARRAY               = 0x906a
  val GL_MAX_TESS_CONTROL_IMAGE_UNIFORMS                 = 0x90cb
  val GL_MAX_TESS_EVALUATION_IMAGE_UNIFORMS              = 0x90cc
  val GL_MAX_GEOMETRY_IMAGE_UNIFORMS                     = 0x90cd
  val GL_MAX_GEOMETRY_SHADER_STORAGE_BLOCKS              = 0x90d7
  val GL_MAX_TESS_CONTROL_SHADER_STORAGE_BLOCKS          = 0x90d8
  val GL_MAX_TESS_EVALUATION_SHADER_STORAGE_BLOCKS       = 0x90d9
  val GL_TEXTURE_2D_MULTISAMPLE_ARRAY                    = 0x9102
  val GL_TEXTURE_BINDING_2D_MULTISAMPLE_ARRAY            = 0x9105
  val GL_SAMPLER_2D_MULTISAMPLE_ARRAY                    = 0x910b
  val GL_INT_SAMPLER_2D_MULTISAMPLE_ARRAY                = 0x910c
  val GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE_ARRAY       = 0x910d
  val GL_MAX_GEOMETRY_INPUT_COMPONENTS                   = 0x9123
  val GL_MAX_GEOMETRY_OUTPUT_COMPONENTS                  = 0x9124
  val GL_MAX_DEBUG_MESSAGE_LENGTH                        = 0x9143
  val GL_MAX_DEBUG_LOGGED_MESSAGES                       = 0x9144
  val GL_DEBUG_LOGGED_MESSAGES                           = 0x9145
  val GL_DEBUG_SEVERITY_HIGH                             = 0x9146
  val GL_DEBUG_SEVERITY_MEDIUM                           = 0x9147
  val GL_DEBUG_SEVERITY_LOW                              = 0x9148
  val GL_TEXTURE_BUFFER_OFFSET                           = 0x919d
  val GL_TEXTURE_BUFFER_SIZE                             = 0x919e
  val GL_TEXTURE_BUFFER_OFFSET_ALIGNMENT                 = 0x919f
  val GL_MULTIPLY                                        = 0x9294
  val GL_SCREEN                                          = 0x9295
  val GL_OVERLAY                                         = 0x9296
  val GL_DARKEN                                          = 0x9297
  val GL_LIGHTEN                                         = 0x9298
  val GL_COLORDODGE                                      = 0x9299
  val GL_COLORBURN                                       = 0x929a
  val GL_HARDLIGHT                                       = 0x929b
  val GL_SOFTLIGHT                                       = 0x929c
  val GL_DIFFERENCE                                      = 0x929e
  val GL_EXCLUSION                                       = 0x92a0
  val GL_HSL_HUE                                         = 0x92ad
  val GL_HSL_SATURATION                                  = 0x92ae
  val GL_HSL_COLOR                                       = 0x92af
  val GL_HSL_LUMINOSITY                                  = 0x92b0
  val GL_PRIMITIVE_BOUNDING_BOX                          = 0x92be
  val GL_MAX_TESS_CONTROL_ATOMIC_COUNTER_BUFFERS         = 0x92cd
  val GL_MAX_TESS_EVALUATION_ATOMIC_COUNTER_BUFFERS      = 0x92ce
  val GL_MAX_GEOMETRY_ATOMIC_COUNTER_BUFFERS             = 0x92cf
  val GL_MAX_TESS_CONTROL_ATOMIC_COUNTERS                = 0x92d3
  val GL_MAX_TESS_EVALUATION_ATOMIC_COUNTERS             = 0x92d4
  val GL_MAX_GEOMETRY_ATOMIC_COUNTERS                    = 0x92d5
  val GL_DEBUG_OUTPUT                                    = 0x92e0
  val GL_IS_PER_PATCH                                    = 0x92e7
  val GL_REFERENCED_BY_TESS_CONTROL_SHADER               = 0x9307
  val GL_REFERENCED_BY_TESS_EVALUATION_SHADER            = 0x9308
  val GL_REFERENCED_BY_GEOMETRY_SHADER                   = 0x9309
  val GL_FRAMEBUFFER_DEFAULT_LAYERS                      = 0x9312
  val GL_MAX_FRAMEBUFFER_LAYERS                          = 0x9317
  val GL_MULTISAMPLE_LINE_WIDTH_RANGE                    = 0x9381
  val GL_MULTISAMPLE_LINE_WIDTH_GRANULARITY              = 0x9382
  val GL_COMPRESSED_RGBA_ASTC_4x4                        = 0x93b0
  val GL_COMPRESSED_RGBA_ASTC_5x4                        = 0x93b1
  val GL_COMPRESSED_RGBA_ASTC_5x5                        = 0x93b2
  val GL_COMPRESSED_RGBA_ASTC_6x5                        = 0x93b3
  val GL_COMPRESSED_RGBA_ASTC_6x6                        = 0x93b4
  val GL_COMPRESSED_RGBA_ASTC_8x5                        = 0x93b5
  val GL_COMPRESSED_RGBA_ASTC_8x6                        = 0x93b6
  val GL_COMPRESSED_RGBA_ASTC_8x8                        = 0x93b7
  val GL_COMPRESSED_RGBA_ASTC_10x5                       = 0x93b8
  val GL_COMPRESSED_RGBA_ASTC_10x6                       = 0x93b9
  val GL_COMPRESSED_RGBA_ASTC_10x8                       = 0x93ba
  val GL_COMPRESSED_RGBA_ASTC_10x10                      = 0x93bb
  val GL_COMPRESSED_RGBA_ASTC_12x10                      = 0x93bc
  val GL_COMPRESSED_RGBA_ASTC_12x12                      = 0x93bd
  val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_4x4                = 0x93d0
  val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_5x4                = 0x93d1
  val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_5x5                = 0x93d2
  val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_6x5                = 0x93d3
  val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_6x6                = 0x93d4
  val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_8x5                = 0x93d5
  val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_8x6                = 0x93d6
  val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_8x8                = 0x93d7
  val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_10x5               = 0x93d8
  val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_10x6               = 0x93d9
  val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_10x8               = 0x93da
  val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_10x10              = 0x93db
  val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_12x10              = 0x93dc
  val GL_COMPRESSED_SRGB8_ALPHA8_ASTC_12x12              = 0x93dd
}
