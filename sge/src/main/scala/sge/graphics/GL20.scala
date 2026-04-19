/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/GL20.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: constants moved to companion object
 *   Idiom: split packages
 *   Idiom: opaque Pixels for width/height/coordinate params in glViewport, glScissor, glReadPixels, glTexImage2D, glRenderbufferStorage etc.
 *   Idiom: opaque GL enum types (TextureTarget, BlendFactor, BlendEquation, CompareFunc, StencilOp, PrimitiveMode, BufferTarget, BufferUsage, ShaderType, PixelFormat, DataType, ClearMask, CullFace, EnableCap) for method params -- see GLEnum.scala
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 673
 * Covenant-baseline-methods: GL20,GL_ACTIVE_ATTRIBUTES,GL_ACTIVE_ATTRIBUTE_MAX_LENGTH,GL_ACTIVE_TEXTURE,GL_ACTIVE_UNIFORMS,GL_ACTIVE_UNIFORM_MAX_LENGTH,GL_ALIASED_LINE_WIDTH_RANGE,GL_ALIASED_POINT_SIZE_RANGE,GL_ALPHA,GL_ALPHA_BITS,GL_ALWAYS,GL_ARRAY_BUFFER,GL_ARRAY_BUFFER_BINDING,GL_ATTACHED_SHADERS,GL_BACK,GL_BLEND,GL_BLEND_COLOR,GL_BLEND_DST_ALPHA,GL_BLEND_DST_RGB,GL_BLEND_EQUATION,GL_BLEND_EQUATION_ALPHA,GL_BLEND_EQUATION_RGB,GL_BLEND_SRC_ALPHA,GL_BLEND_SRC_RGB,GL_BLUE_BITS,GL_BOOL,GL_BOOL_VEC2,GL_BOOL_VEC3,GL_BOOL_VEC4,GL_BUFFER_SIZE,GL_BUFFER_USAGE,GL_BYTE,GL_CCW,GL_CLAMP_TO_EDGE,GL_COLOR_ATTACHMENT0,GL_COLOR_BUFFER_BIT,GL_COLOR_CLEAR_VALUE,GL_COLOR_WRITEMASK,GL_COMPILE_STATUS,GL_COMPRESSED_TEXTURE_FORMATS,GL_CONSTANT_ALPHA,GL_CONSTANT_COLOR,GL_COVERAGE_BUFFER_BIT_NV,GL_CULL_FACE,GL_CULL_FACE_MODE,GL_CURRENT_PROGRAM,GL_CURRENT_VERTEX_ATTRIB,GL_CW,GL_DECR,GL_DECR_WRAP,GL_DELETE_STATUS,GL_DEPTH_ATTACHMENT,GL_DEPTH_BITS,GL_DEPTH_BUFFER_BIT,GL_DEPTH_CLEAR_VALUE,GL_DEPTH_COMPONENT,GL_DEPTH_COMPONENT16,GL_DEPTH_FUNC,GL_DEPTH_RANGE,GL_DEPTH_TEST,GL_DEPTH_WRITEMASK,GL_DITHER,GL_DONT_CARE,GL_DST_ALPHA,GL_DST_COLOR,GL_DYNAMIC_DRAW,GL_ELEMENT_ARRAY_BUFFER,GL_ELEMENT_ARRAY_BUFFER_BINDING,GL_EQUAL,GL_ES_VERSION_2_0,GL_EXTENSIONS,GL_FALSE,GL_FASTEST,GL_FIXED,GL_FLOAT,GL_FLOAT_MAT2,GL_FLOAT_MAT3,GL_FLOAT_MAT4,GL_FLOAT_VEC2,GL_FLOAT_VEC3,GL_FLOAT_VEC4,GL_FRAGMENT_SHADER,GL_FRAMEBUFFER,GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME,GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE,GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE,GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL,GL_FRAMEBUFFER_BINDING,GL_FRAMEBUFFER_COMPLETE,GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT,GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS,GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT,GL_FRAMEBUFFER_UNSUPPORTED,GL_FRONT,GL_FRONT_AND_BACK,GL_FRONT_FACE,GL_FUNC_ADD,GL_FUNC_REVERSE_SUBTRACT,GL_FUNC_SUBTRACT,GL_GENERATE_MIPMAP,GL_GENERATE_MIPMAP_HINT,GL_GEQUAL,GL_GREATER,GL_GREEN_BITS,GL_HIGH_FLOAT,GL_HIGH_INT,GL_IMPLEMENTATION_COLOR_READ_FORMAT,GL_IMPLEMENTATION_COLOR_READ_TYPE,GL_INCR,GL_INCR_WRAP,GL_INFO_LOG_LENGTH,GL_INT,GL_INT_VEC2,GL_INT_VEC3,GL_INT_VEC4,GL_INVALID_ENUM,GL_INVALID_FRAMEBUFFER_OPERATION,GL_INVALID_OPERATION,GL_INVALID_VALUE,GL_INVERT,GL_KEEP,GL_LEQUAL,GL_LESS,GL_LINEAR,GL_LINEAR_MIPMAP_LINEAR,GL_LINEAR_MIPMAP_NEAREST,GL_LINES,GL_LINE_LOOP,GL_LINE_STRIP,GL_LINE_WIDTH,GL_LINK_STATUS,GL_LOW_FLOAT,GL_LOW_INT,GL_LUMINANCE,GL_LUMINANCE_ALPHA,GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS,GL_MAX_CUBE_MAP_TEXTURE_SIZE,GL_MAX_FRAGMENT_UNIFORM_VECTORS,GL_MAX_RENDERBUFFER_SIZE,GL_MAX_TEXTURE_IMAGE_UNITS,GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT,GL_MAX_TEXTURE_SIZE,GL_MAX_TEXTURE_UNITS,GL_MAX_VARYING_VECTORS,GL_MAX_VERTEX_ATTRIBS,GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS,GL_MAX_VERTEX_UNIFORM_VECTORS,GL_MAX_VIEWPORT_DIMS,GL_MEDIUM_FLOAT,GL_MEDIUM_INT,GL_MIRRORED_REPEAT,GL_NEAREST,GL_NEAREST_MIPMAP_LINEAR,GL_NEAREST_MIPMAP_NEAREST,GL_NEVER,GL_NICEST,GL_NONE,GL_NOTEQUAL,GL_NO_ERROR,GL_NUM_COMPRESSED_TEXTURE_FORMATS,GL_NUM_SHADER_BINARY_FORMATS,GL_ONE,GL_ONE_MINUS_CONSTANT_ALPHA,GL_ONE_MINUS_CONSTANT_COLOR,GL_ONE_MINUS_DST_ALPHA,GL_ONE_MINUS_DST_COLOR,GL_ONE_MINUS_SRC_ALPHA,GL_ONE_MINUS_SRC_COLOR,GL_OUT_OF_MEMORY,GL_PACK_ALIGNMENT,GL_POINTS,GL_POLYGON_OFFSET_FACTOR,GL_POLYGON_OFFSET_FILL,GL_POLYGON_OFFSET_UNITS,GL_RED_BITS,GL_RENDERBUFFER,GL_RENDERBUFFER_ALPHA_SIZE,GL_RENDERBUFFER_BINDING,GL_RENDERBUFFER_BLUE_SIZE,GL_RENDERBUFFER_DEPTH_SIZE,GL_RENDERBUFFER_GREEN_SIZE,GL_RENDERBUFFER_HEIGHT,GL_RENDERBUFFER_INTERNAL_FORMAT,GL_RENDERBUFFER_RED_SIZE,GL_RENDERBUFFER_STENCIL_SIZE,GL_RENDERBUFFER_WIDTH,GL_RENDERER,GL_REPEAT,GL_REPLACE,GL_RGB,GL_RGB565,GL_RGB5_A1,GL_RGBA,GL_RGBA4,GL_SAMPLER_2D,GL_SAMPLER_CUBE,GL_SAMPLES,GL_SAMPLE_ALPHA_TO_COVERAGE,GL_SAMPLE_BUFFERS,GL_SAMPLE_COVERAGE,GL_SAMPLE_COVERAGE_INVERT,GL_SAMPLE_COVERAGE_VALUE,GL_SCISSOR_BOX,GL_SCISSOR_TEST,GL_SHADER_BINARY_FORMATS,GL_SHADER_COMPILER,GL_SHADER_SOURCE_LENGTH,GL_SHADER_TYPE,GL_SHADING_LANGUAGE_VERSION,GL_SHORT,GL_SRC_ALPHA,GL_SRC_ALPHA_SATURATE,GL_SRC_COLOR,GL_STATIC_DRAW,GL_STENCIL_ATTACHMENT,GL_STENCIL_BACK_FAIL,GL_STENCIL_BACK_FUNC,GL_STENCIL_BACK_PASS_DEPTH_FAIL,GL_STENCIL_BACK_PASS_DEPTH_PASS,GL_STENCIL_BACK_REF,GL_STENCIL_BACK_VALUE_MASK,GL_STENCIL_BACK_WRITEMASK,GL_STENCIL_BITS,GL_STENCIL_BUFFER_BIT,GL_STENCIL_CLEAR_VALUE,GL_STENCIL_FAIL,GL_STENCIL_FUNC,GL_STENCIL_INDEX,GL_STENCIL_INDEX8,GL_STENCIL_PASS_DEPTH_FAIL,GL_STENCIL_PASS_DEPTH_PASS,GL_STENCIL_REF,GL_STENCIL_TEST,GL_STENCIL_VALUE_MASK,GL_STENCIL_WRITEMASK,GL_STREAM_DRAW,GL_SUBPIXEL_BITS,GL_TEXTURE,GL_TEXTURE0,GL_TEXTURE1,GL_TEXTURE10,GL_TEXTURE11,GL_TEXTURE12,GL_TEXTURE13,GL_TEXTURE14,GL_TEXTURE15,GL_TEXTURE16,GL_TEXTURE17,GL_TEXTURE18,GL_TEXTURE19,GL_TEXTURE2,GL_TEXTURE20,GL_TEXTURE21,GL_TEXTURE22,GL_TEXTURE23,GL_TEXTURE24,GL_TEXTURE25,GL_TEXTURE26,GL_TEXTURE27,GL_TEXTURE28,GL_TEXTURE29,GL_TEXTURE3,GL_TEXTURE30,GL_TEXTURE31,GL_TEXTURE4,GL_TEXTURE5,GL_TEXTURE6,GL_TEXTURE7,GL_TEXTURE8,GL_TEXTURE9,GL_TEXTURE_2D,GL_TEXTURE_BINDING_2D,GL_TEXTURE_BINDING_CUBE_MAP,GL_TEXTURE_CUBE_MAP,GL_TEXTURE_CUBE_MAP_NEGATIVE_X,GL_TEXTURE_CUBE_MAP_NEGATIVE_Y,GL_TEXTURE_CUBE_MAP_NEGATIVE_Z,GL_TEXTURE_CUBE_MAP_POSITIVE_X,GL_TEXTURE_CUBE_MAP_POSITIVE_Y,GL_TEXTURE_CUBE_MAP_POSITIVE_Z,GL_TEXTURE_MAG_FILTER,GL_TEXTURE_MAX_ANISOTROPY_EXT,GL_TEXTURE_MIN_FILTER,GL_TEXTURE_WRAP_S,GL_TEXTURE_WRAP_T,GL_TRIANGLES,GL_TRIANGLE_FAN,GL_TRIANGLE_STRIP,GL_TRUE,GL_UNPACK_ALIGNMENT,GL_UNSIGNED_BYTE,GL_UNSIGNED_INT,GL_UNSIGNED_SHORT,GL_UNSIGNED_SHORT_4_4_4_4,GL_UNSIGNED_SHORT_5_5_5_1,GL_UNSIGNED_SHORT_5_6_5,GL_VALIDATE_STATUS,GL_VENDOR,GL_VERSION,GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING,GL_VERTEX_ATTRIB_ARRAY_ENABLED,GL_VERTEX_ATTRIB_ARRAY_NORMALIZED,GL_VERTEX_ATTRIB_ARRAY_POINTER,GL_VERTEX_ATTRIB_ARRAY_SIZE,GL_VERTEX_ATTRIB_ARRAY_STRIDE,GL_VERTEX_ATTRIB_ARRAY_TYPE,GL_VERTEX_PROGRAM_POINT_SIZE,GL_VERTEX_SHADER,GL_VIEWPORT,GL_ZERO,glActiveTexture,glAttachShader,glBindAttribLocation,glBindBuffer,glBindFramebuffer,glBindRenderbuffer,glBindTexture,glBlendColor,glBlendEquation,glBlendEquationSeparate,glBlendFunc,glBlendFuncSeparate,glBufferData,glBufferSubData,glCheckFramebufferStatus,glClear,glClearColor,glClearDepthf,glClearStencil,glColorMask,glCompileShader,glCompressedTexImage2D,glCompressedTexSubImage2D,glCopyTexImage2D,glCopyTexSubImage2D,glCreateProgram,glCreateShader,glCullFace,glDeleteBuffer,glDeleteBuffers,glDeleteFramebuffer,glDeleteFramebuffers,glDeleteProgram,glDeleteRenderbuffer,glDeleteRenderbuffers,glDeleteShader,glDeleteTexture,glDeleteTextures,glDepthFunc,glDepthMask,glDepthRangef,glDetachShader,glDisable,glDisableVertexAttribArray,glDrawArrays,glDrawElements,glEnable,glEnableVertexAttribArray,glFinish,glFlush,glFramebufferRenderbuffer,glFramebufferTexture2D,glFrontFace,glGenBuffer,glGenBuffers,glGenFramebuffer,glGenFramebuffers,glGenRenderbuffer,glGenRenderbuffers,glGenTexture,glGenTextures,glGenerateMipmap,glGetActiveAttrib,glGetActiveUniform,glGetAttachedShaders,glGetAttribLocation,glGetBooleanv,glGetBufferParameteriv,glGetError,glGetFloatv,glGetFramebufferAttachmentParameteriv,glGetIntegerv,glGetProgramInfoLog,glGetProgramiv,glGetRenderbufferParameteriv,glGetShaderInfoLog,glGetShaderPrecisionFormat,glGetShaderiv,glGetString,glGetTexParameterfv,glGetTexParameteriv,glGetUniformLocation,glGetUniformfv,glGetUniformiv,glGetVertexAttribPointerv,glGetVertexAttribfv,glGetVertexAttribiv,glHint,glIsBuffer,glIsEnabled,glIsFramebuffer,glIsProgram,glIsRenderbuffer,glIsShader,glIsTexture,glLineWidth,glLinkProgram,glPixelStorei,glPolygonOffset,glReadPixels,glReleaseShaderCompiler,glRenderbufferStorage,glSampleCoverage,glScissor,glShaderBinary,glShaderSource,glStencilFunc,glStencilFuncSeparate,glStencilMask,glStencilMaskSeparate,glStencilOp,glStencilOpSeparate,glTexImage2D,glTexParameterf,glTexParameterfv,glTexParameteri,glTexParameteriv,glTexSubImage2D,glUniform1f,glUniform1fv,glUniform1i,glUniform1iv,glUniform2f,glUniform2fv,glUniform2i,glUniform2iv,glUniform3f,glUniform3fv,glUniform3i,glUniform3iv,glUniform4f,glUniform4fv,glUniform4i,glUniform4iv,glUniformMatrix2fv,glUniformMatrix3fv,glUniformMatrix4fv,glUseProgram,glValidateProgram,glVertexAttrib1f,glVertexAttrib1fv,glVertexAttrib2f,glVertexAttrib2fv,glVertexAttrib3f,glVertexAttrib3fv,glVertexAttrib4f,glVertexAttrib4fv,glVertexAttribPointer,glViewport
 * Covenant-source-reference: com/badlogic/gdx/graphics/GL20.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics

import java.nio.{ Buffer, FloatBuffer, IntBuffer }

/** Interface wrapping all the methods of OpenGL ES 2.0
  * @author
  *   mzechner (original implementation)
  */
trait GL20 {

  def glActiveTexture(texture: Int): Unit

  def glBindTexture(target: TextureTarget, texture: Int): Unit

  def glBlendFunc(sfactor: BlendFactor, dfactor: BlendFactor): Unit

  def glClear(mask: ClearMask): Unit

  def glClearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit

  def glClearDepthf(depth: Float): Unit

  def glClearStencil(s: Int): Unit

  def glColorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit

  def glCompressedTexImage2D(target: TextureTarget, level: Int, internalformat: Int, width: Pixels, height: Pixels, border: Int, imageSize: Int, data: Buffer): Unit

  def glCompressedTexSubImage2D(target: TextureTarget, level: Int, xoffset: Pixels, yoffset: Pixels, width: Pixels, height: Pixels, format: PixelFormat, imageSize: Int, data: Buffer): Unit

  def glCopyTexImage2D(target: TextureTarget, level: Int, internalformat: Int, x: Pixels, y: Pixels, width: Pixels, height: Pixels, border: Int): Unit

  def glCopyTexSubImage2D(target: TextureTarget, level: Int, xoffset: Pixels, yoffset: Pixels, x: Pixels, y: Pixels, width: Pixels, height: Pixels): Unit

  def glCullFace(mode: CullFace): Unit

  def glDeleteTextures(n: Int, textures: IntBuffer): Unit

  def glDeleteTexture(texture: Int): Unit

  def glDepthFunc(func: CompareFunc): Unit

  def glDepthMask(flag: Boolean): Unit

  def glDepthRangef(zNear: Float, zFar: Float): Unit

  def glDisable(cap: EnableCap): Unit

  def glDrawArrays(mode: PrimitiveMode, first: Int, count: Int): Unit

  /** Not fully supported with GWT backend: indices content is ignored, only buffer position is used. */
  def glDrawElements(mode: PrimitiveMode, count: Int, `type`: DataType, indices: Buffer): Unit

  def glEnable(cap: EnableCap): Unit

  def glFinish(): Unit

  def glFlush(): Unit

  def glFrontFace(mode: Int): Unit

  def glGenTextures(n: Int, textures: IntBuffer): Unit

  def glGenTexture(): Int

  def glGetError(): Int

  def glGetIntegerv(pname: Int, params: IntBuffer): Unit

  def glGetString(name: Int): String

  def glHint(target: Int, mode: Int): Unit

  def glLineWidth(width: Float): Unit

  def glPixelStorei(pname: Int, param: Int): Unit

  def glPolygonOffset(factor: Float, units: Float): Unit

  def glReadPixels(x: Pixels, y: Pixels, width: Pixels, height: Pixels, format: PixelFormat, `type`: DataType, pixels: Buffer): Unit

  def glScissor(x: Pixels, y: Pixels, width: Pixels, height: Pixels): Unit

  def glStencilFunc(func: CompareFunc, ref: Int, mask: Int): Unit

  def glStencilMask(mask: Int): Unit

  def glStencilOp(fail: StencilOp, zfail: StencilOp, zpass: StencilOp): Unit

  def glTexImage2D(target: TextureTarget, level: Int, internalformat: Int, width: Pixels, height: Pixels, border: Int, format: PixelFormat, `type`: DataType, pixels: Buffer): Unit

  def glTexParameterf(target: TextureTarget, pname: Int, param: Float): Unit

  def glTexSubImage2D(target: TextureTarget, level: Int, xoffset: Pixels, yoffset: Pixels, width: Pixels, height: Pixels, format: PixelFormat, `type`: DataType, pixels: Buffer): Unit

  def glViewport(x: Pixels, y: Pixels, width: Pixels, height: Pixels): Unit

  def glAttachShader(program: Int, shader: Int): Unit

  def glBindAttribLocation(program: Int, index: Int, name: String): Unit

  def glBindBuffer(target: BufferTarget, buffer: Int): Unit

  def glBindFramebuffer(target: Int, framebuffer: Int): Unit

  def glBindRenderbuffer(target: Int, renderbuffer: Int): Unit

  def glBlendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit

  def glBlendEquation(mode: BlendEquation): Unit

  def glBlendEquationSeparate(modeRGB: BlendEquation, modeAlpha: BlendEquation): Unit

  def glBlendFuncSeparate(srcRGB: BlendFactor, dstRGB: BlendFactor, srcAlpha: BlendFactor, dstAlpha: BlendFactor): Unit

  def glBufferData(target: BufferTarget, size: Int, data: Buffer, usage: BufferUsage): Unit

  def glBufferSubData(target: BufferTarget, offset: Int, size: Int, data: Buffer): Unit

  def glCheckFramebufferStatus(target: Int): Int

  def glCompileShader(shader: Int): Unit

  def glCreateProgram(): Int

  def glCreateShader(`type`: ShaderType): Int

  def glDeleteBuffer(buffer: Int): Unit

  def glDeleteBuffers(n: Int, buffers: IntBuffer): Unit

  def glDeleteFramebuffer(framebuffer: Int): Unit

  def glDeleteFramebuffers(n: Int, framebuffers: IntBuffer): Unit

  def glDeleteProgram(program: Int): Unit

  def glDeleteRenderbuffer(renderbuffer: Int): Unit

  def glDeleteRenderbuffers(n: Int, renderbuffers: IntBuffer): Unit

  def glDeleteShader(shader: Int): Unit

  def glDetachShader(program: Int, shader: Int): Unit

  def glDisableVertexAttribArray(index: Int): Unit

  def glDrawElements(mode: PrimitiveMode, count: Int, `type`: DataType, indices: Int): Unit

  def glEnableVertexAttribArray(index: Int): Unit

  def glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int): Unit

  def glFramebufferTexture2D(target: Int, attachment: Int, textarget: TextureTarget, texture: Int, level: Int): Unit

  def glGenBuffer(): Int

  def glGenBuffers(n: Int, buffers: IntBuffer): Unit

  def glGenerateMipmap(target: TextureTarget): Unit

  def glGenFramebuffer(): Int

  def glGenFramebuffers(n: Int, framebuffers: IntBuffer): Unit

  def glGenRenderbuffer(): Int

  def glGenRenderbuffers(n: Int, renderbuffers: IntBuffer): Unit

  // deviates
  def glGetActiveAttrib(program: Int, index: Int, size: IntBuffer, `type`: IntBuffer): String

  // deviates
  def glGetActiveUniform(program: Int, index: Int, size: IntBuffer, `type`: IntBuffer): String

  def glGetAttachedShaders(program: Int, maxcount: Int, count: Buffer, shaders: IntBuffer): Unit

  def glGetAttribLocation(program: Int, name: String): Int

  def glGetBooleanv(pname: Int, params: Buffer): Unit

  def glGetBufferParameteriv(target: BufferTarget, pname: Int, params: IntBuffer): Unit

  def glGetFloatv(pname: Int, params: FloatBuffer): Unit

  def glGetFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: IntBuffer): Unit

  def glGetProgramiv(program: Int, pname: Int, params: IntBuffer): Unit

  // deviates
  def glGetProgramInfoLog(program: Int): String

  def glGetRenderbufferParameteriv(target: Int, pname: Int, params: IntBuffer): Unit

  def glGetShaderiv(shader: Int, pname: Int, params: IntBuffer): Unit

  // deviates
  def glGetShaderInfoLog(shader: Int): String

  def glGetShaderPrecisionFormat(shadertype: ShaderType, precisiontype: Int, range: IntBuffer, precision: IntBuffer): Unit

  def glGetTexParameterfv(target: TextureTarget, pname: Int, params: FloatBuffer): Unit

  def glGetTexParameteriv(target: TextureTarget, pname: Int, params: IntBuffer): Unit

  def glGetUniformfv(program: Int, location: Int, params: FloatBuffer): Unit

  def glGetUniformiv(program: Int, location: Int, params: IntBuffer): Unit

  def glGetUniformLocation(program: Int, name: String): Int

  def glGetVertexAttribfv(index: Int, pname: Int, params: FloatBuffer): Unit

  def glGetVertexAttribiv(index: Int, pname: Int, params: IntBuffer): Unit

  def glGetVertexAttribPointerv(index: Int, pname: Int, pointer: Buffer): Unit

  def glIsBuffer(buffer: Int): Boolean

  def glIsEnabled(cap: EnableCap): Boolean

  def glIsFramebuffer(framebuffer: Int): Boolean

  def glIsProgram(program: Int): Boolean

  def glIsRenderbuffer(renderbuffer: Int): Boolean

  def glIsShader(shader: Int): Boolean

  def glIsTexture(texture: Int): Boolean

  def glLinkProgram(program: Int): Unit

  def glReleaseShaderCompiler(): Unit

  def glRenderbufferStorage(target: Int, internalformat: Int, width: Pixels, height: Pixels): Unit

  def glSampleCoverage(value: Float, invert: Boolean): Unit

  def glShaderBinary(n: Int, shaders: IntBuffer, binaryformat: Int, binary: Buffer, length: Int): Unit

  // Deviates
  def glShaderSource(shader: Int, string: String): Unit

  def glStencilFuncSeparate(face: CullFace, func: CompareFunc, ref: Int, mask: Int): Unit

  def glStencilMaskSeparate(face: CullFace, mask: Int): Unit

  def glStencilOpSeparate(face: CullFace, fail: StencilOp, zfail: StencilOp, zpass: StencilOp): Unit

  def glTexParameterfv(target: TextureTarget, pname: Int, params: FloatBuffer): Unit

  def glTexParameteri(target: TextureTarget, pname: Int, param: Int): Unit

  def glTexParameteriv(target: TextureTarget, pname: Int, params: IntBuffer): Unit

  def glUniform1f(location: Int, x: Float): Unit

  def glUniform1fv(location: Int, count: Int, v: FloatBuffer): Unit

  def glUniform1fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit

  def glUniform1i(location: Int, x: Int): Unit

  def glUniform1iv(location: Int, count: Int, v: IntBuffer): Unit

  def glUniform1iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit

  def glUniform2f(location: Int, x: Float, y: Float): Unit

  def glUniform2fv(location: Int, count: Int, v: FloatBuffer): Unit

  def glUniform2fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit

  def glUniform2i(location: Int, x: Int, y: Int): Unit

  def glUniform2iv(location: Int, count: Int, v: IntBuffer): Unit

  def glUniform2iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit

  def glUniform3f(location: Int, x: Float, y: Float, z: Float): Unit

  def glUniform3fv(location: Int, count: Int, v: FloatBuffer): Unit

  def glUniform3fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit

  def glUniform3i(location: Int, x: Int, y: Int, z: Int): Unit

  def glUniform3iv(location: Int, count: Int, v: IntBuffer): Unit

  def glUniform3iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit

  def glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float): Unit

  def glUniform4fv(location: Int, count: Int, v: FloatBuffer): Unit

  def glUniform4fv(location: Int, count: Int, v: Array[Float], offset: Int): Unit

  def glUniform4i(location: Int, x: Int, y: Int, z: Int, w: Int): Unit

  def glUniform4iv(location: Int, count: Int, v: IntBuffer): Unit

  def glUniform4iv(location: Int, count: Int, v: Array[Int], offset: Int): Unit

  def glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit

  def glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit

  def glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit

  def glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, value: Array[Float], offset: Int): Unit

  def glUseProgram(program: Int): Unit

  def glValidateProgram(program: Int): Unit

  def glVertexAttrib1f(indx: Int, x: Float): Unit

  def glVertexAttrib1fv(indx: Int, values: FloatBuffer): Unit

  def glVertexAttrib2f(indx: Int, x: Float, y: Float): Unit

  def glVertexAttrib2fv(indx: Int, values: FloatBuffer): Unit

  def glVertexAttrib3f(indx: Int, x: Float, y: Float, z: Float): Unit

  def glVertexAttrib3fv(indx: Int, values: FloatBuffer): Unit

  def glVertexAttrib4f(indx: Int, x: Float, y: Float, z: Float, w: Float): Unit

  def glVertexAttrib4fv(indx: Int, values: FloatBuffer): Unit

  /** In OpenGl core profiles (3.1+), passing a pointer to client memory is not valid. In 3.0 and later, use the other version of this function instead, pass a zero-based offset which references the
    * buffer currently bound to GL_ARRAY_BUFFER.
    */
  def glVertexAttribPointer(indx: Int, size: Int, `type`: DataType, normalized: Boolean, stride: Int, ptr: Buffer): Unit

  def glVertexAttribPointer(indx: Int, size: Int, `type`: DataType, normalized: Boolean, stride: Int, ptr: Int): Unit
}

object GL20 {
  val GL_ES_VERSION_2_0                               = 1
  val GL_DEPTH_BUFFER_BIT                             = 0x00000100
  val GL_STENCIL_BUFFER_BIT                           = 0x00000400
  val GL_COLOR_BUFFER_BIT                             = 0x00004000
  val GL_FALSE                                        = 0
  val GL_TRUE                                         = 1
  val GL_POINTS                                       = 0x0000
  val GL_LINES                                        = 0x0001
  val GL_LINE_LOOP                                    = 0x0002
  val GL_LINE_STRIP                                   = 0x0003
  val GL_TRIANGLES                                    = 0x0004
  val GL_TRIANGLE_STRIP                               = 0x0005
  val GL_TRIANGLE_FAN                                 = 0x0006
  val GL_ZERO                                         = 0
  val GL_ONE                                          = 1
  val GL_SRC_COLOR                                    = 0x0300
  val GL_ONE_MINUS_SRC_COLOR                          = 0x0301
  val GL_SRC_ALPHA                                    = 0x0302
  val GL_ONE_MINUS_SRC_ALPHA                          = 0x0303
  val GL_DST_ALPHA                                    = 0x0304
  val GL_ONE_MINUS_DST_ALPHA                          = 0x0305
  val GL_DST_COLOR                                    = 0x0306
  val GL_ONE_MINUS_DST_COLOR                          = 0x0307
  val GL_SRC_ALPHA_SATURATE                           = 0x0308
  val GL_FUNC_ADD                                     = 0x8006
  val GL_BLEND_EQUATION                               = 0x8009
  val GL_BLEND_EQUATION_RGB                           = 0x8009
  val GL_BLEND_EQUATION_ALPHA                         = 0x883d
  val GL_FUNC_SUBTRACT                                = 0x800a
  val GL_FUNC_REVERSE_SUBTRACT                        = 0x800b
  val GL_BLEND_DST_RGB                                = 0x80c8
  val GL_BLEND_SRC_RGB                                = 0x80c9
  val GL_BLEND_DST_ALPHA                              = 0x80ca
  val GL_BLEND_SRC_ALPHA                              = 0x80cb
  val GL_CONSTANT_COLOR                               = 0x8001
  val GL_ONE_MINUS_CONSTANT_COLOR                     = 0x8002
  val GL_CONSTANT_ALPHA                               = 0x8003
  val GL_ONE_MINUS_CONSTANT_ALPHA                     = 0x8004
  val GL_BLEND_COLOR                                  = 0x8005
  val GL_ARRAY_BUFFER                                 = 0x8892
  val GL_ELEMENT_ARRAY_BUFFER                         = 0x8893
  val GL_ARRAY_BUFFER_BINDING                         = 0x8894
  val GL_ELEMENT_ARRAY_BUFFER_BINDING                 = 0x8895
  val GL_STREAM_DRAW                                  = 0x88e0
  val GL_STATIC_DRAW                                  = 0x88e4
  val GL_DYNAMIC_DRAW                                 = 0x88e8
  val GL_BUFFER_SIZE                                  = 0x8764
  val GL_BUFFER_USAGE                                 = 0x8765
  val GL_CURRENT_VERTEX_ATTRIB                        = 0x8626
  val GL_FRONT                                        = 0x0404
  val GL_BACK                                         = 0x0405
  val GL_FRONT_AND_BACK                               = 0x0408
  val GL_TEXTURE_2D                                   = 0x0de1
  val GL_CULL_FACE                                    = 0x0b44
  val GL_BLEND                                        = 0x0be2
  val GL_DITHER                                       = 0x0bd0
  val GL_STENCIL_TEST                                 = 0x0b90
  val GL_DEPTH_TEST                                   = 0x0b71
  val GL_SCISSOR_TEST                                 = 0x0c11
  val GL_POLYGON_OFFSET_FILL                          = 0x8037
  val GL_SAMPLE_ALPHA_TO_COVERAGE                     = 0x809e
  val GL_SAMPLE_COVERAGE                              = 0x80a0
  val GL_NO_ERROR                                     = 0
  val GL_INVALID_ENUM                                 = 0x0500
  val GL_INVALID_VALUE                                = 0x0501
  val GL_INVALID_OPERATION                            = 0x0502
  val GL_OUT_OF_MEMORY                                = 0x0505
  val GL_CW                                           = 0x0900
  val GL_CCW                                          = 0x0901
  val GL_LINE_WIDTH                                   = 0x0b21
  val GL_ALIASED_POINT_SIZE_RANGE                     = 0x846d
  val GL_ALIASED_LINE_WIDTH_RANGE                     = 0x846e
  val GL_CULL_FACE_MODE                               = 0x0b45
  val GL_FRONT_FACE                                   = 0x0b46
  val GL_DEPTH_RANGE                                  = 0x0b70
  val GL_DEPTH_WRITEMASK                              = 0x0b72
  val GL_DEPTH_CLEAR_VALUE                            = 0x0b73
  val GL_DEPTH_FUNC                                   = 0x0b74
  val GL_STENCIL_CLEAR_VALUE                          = 0x0b91
  val GL_STENCIL_FUNC                                 = 0x0b92
  val GL_STENCIL_FAIL                                 = 0x0b94
  val GL_STENCIL_PASS_DEPTH_FAIL                      = 0x0b95
  val GL_STENCIL_PASS_DEPTH_PASS                      = 0x0b96
  val GL_STENCIL_REF                                  = 0x0b97
  val GL_STENCIL_VALUE_MASK                           = 0x0b93
  val GL_STENCIL_WRITEMASK                            = 0x0b98
  val GL_STENCIL_BACK_FUNC                            = 0x8800
  val GL_STENCIL_BACK_FAIL                            = 0x8801
  val GL_STENCIL_BACK_PASS_DEPTH_FAIL                 = 0x8802
  val GL_STENCIL_BACK_PASS_DEPTH_PASS                 = 0x8803
  val GL_STENCIL_BACK_REF                             = 0x8ca3
  val GL_STENCIL_BACK_VALUE_MASK                      = 0x8ca4
  val GL_STENCIL_BACK_WRITEMASK                       = 0x8ca5
  val GL_VIEWPORT                                     = 0x0ba2
  val GL_SCISSOR_BOX                                  = 0x0c10
  val GL_COLOR_CLEAR_VALUE                            = 0x0c22
  val GL_COLOR_WRITEMASK                              = 0x0c23
  val GL_UNPACK_ALIGNMENT                             = 0x0cf5
  val GL_PACK_ALIGNMENT                               = 0x0d05
  val GL_MAX_TEXTURE_SIZE                             = 0x0d33
  val GL_MAX_TEXTURE_UNITS                            = 0x84e2
  val GL_MAX_VIEWPORT_DIMS                            = 0x0d3a
  val GL_SUBPIXEL_BITS                                = 0x0d50
  val GL_RED_BITS                                     = 0x0d52
  val GL_GREEN_BITS                                   = 0x0d53
  val GL_BLUE_BITS                                    = 0x0d54
  val GL_ALPHA_BITS                                   = 0x0d55
  val GL_DEPTH_BITS                                   = 0x0d56
  val GL_STENCIL_BITS                                 = 0x0d57
  val GL_POLYGON_OFFSET_UNITS                         = 0x2a00
  val GL_POLYGON_OFFSET_FACTOR                        = 0x8038
  val GL_TEXTURE_BINDING_2D                           = 0x8069
  val GL_SAMPLE_BUFFERS                               = 0x80a8
  val GL_SAMPLES                                      = 0x80a9
  val GL_SAMPLE_COVERAGE_VALUE                        = 0x80aa
  val GL_SAMPLE_COVERAGE_INVERT                       = 0x80ab
  val GL_NUM_COMPRESSED_TEXTURE_FORMATS               = 0x86a2
  val GL_COMPRESSED_TEXTURE_FORMATS                   = 0x86a3
  val GL_DONT_CARE                                    = 0x1100
  val GL_FASTEST                                      = 0x1101
  val GL_NICEST                                       = 0x1102
  val GL_GENERATE_MIPMAP                              = 0x8191
  val GL_GENERATE_MIPMAP_HINT                         = 0x8192
  val GL_BYTE                                         = 0x1400
  val GL_UNSIGNED_BYTE                                = 0x1401
  val GL_SHORT                                        = 0x1402
  val GL_UNSIGNED_SHORT                               = 0x1403
  val GL_INT                                          = 0x1404
  val GL_UNSIGNED_INT                                 = 0x1405
  val GL_FLOAT                                        = 0x1406
  val GL_FIXED                                        = 0x140c
  val GL_DEPTH_COMPONENT                              = 0x1902
  val GL_ALPHA                                        = 0x1906
  val GL_RGB                                          = 0x1907
  val GL_RGBA                                         = 0x1908
  val GL_LUMINANCE                                    = 0x1909
  val GL_LUMINANCE_ALPHA                              = 0x190a
  val GL_UNSIGNED_SHORT_4_4_4_4                       = 0x8033
  val GL_UNSIGNED_SHORT_5_5_5_1                       = 0x8034
  val GL_UNSIGNED_SHORT_5_6_5                         = 0x8363
  val GL_FRAGMENT_SHADER                              = 0x8b30
  val GL_VERTEX_SHADER                                = 0x8b31
  val GL_MAX_VERTEX_ATTRIBS                           = 0x8869
  val GL_MAX_VERTEX_UNIFORM_VECTORS                   = 0x8dfb
  val GL_MAX_VARYING_VECTORS                          = 0x8dfc
  val GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS             = 0x8b4d
  val GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS               = 0x8b4c
  val GL_MAX_TEXTURE_IMAGE_UNITS                      = 0x8872
  val GL_MAX_FRAGMENT_UNIFORM_VECTORS                 = 0x8dfd
  val GL_SHADER_TYPE                                  = 0x8b4f
  val GL_DELETE_STATUS                                = 0x8b80
  val GL_LINK_STATUS                                  = 0x8b82
  val GL_VALIDATE_STATUS                              = 0x8b83
  val GL_ATTACHED_SHADERS                             = 0x8b85
  val GL_ACTIVE_UNIFORMS                              = 0x8b86
  val GL_ACTIVE_UNIFORM_MAX_LENGTH                    = 0x8b87
  val GL_ACTIVE_ATTRIBUTES                            = 0x8b89
  val GL_ACTIVE_ATTRIBUTE_MAX_LENGTH                  = 0x8b8a
  val GL_SHADING_LANGUAGE_VERSION                     = 0x8b8c
  val GL_CURRENT_PROGRAM                              = 0x8b8d
  val GL_NEVER                                        = 0x0200
  val GL_LESS                                         = 0x0201
  val GL_EQUAL                                        = 0x0202
  val GL_LEQUAL                                       = 0x0203
  val GL_GREATER                                      = 0x0204
  val GL_NOTEQUAL                                     = 0x0205
  val GL_GEQUAL                                       = 0x0206
  val GL_ALWAYS                                       = 0x0207
  val GL_KEEP                                         = 0x1e00
  val GL_REPLACE                                      = 0x1e01
  val GL_INCR                                         = 0x1e02
  val GL_DECR                                         = 0x1e03
  val GL_INVERT                                       = 0x150a
  val GL_INCR_WRAP                                    = 0x8507
  val GL_DECR_WRAP                                    = 0x8508
  val GL_VENDOR                                       = 0x1f00
  val GL_RENDERER                                     = 0x1f01
  val GL_VERSION                                      = 0x1f02
  val GL_EXTENSIONS                                   = 0x1f03
  val GL_NEAREST                                      = 0x2600
  val GL_LINEAR                                       = 0x2601
  val GL_NEAREST_MIPMAP_NEAREST                       = 0x2700
  val GL_LINEAR_MIPMAP_NEAREST                        = 0x2701
  val GL_NEAREST_MIPMAP_LINEAR                        = 0x2702
  val GL_LINEAR_MIPMAP_LINEAR                         = 0x2703
  val GL_TEXTURE_MAG_FILTER                           = 0x2800
  val GL_TEXTURE_MIN_FILTER                           = 0x2801
  val GL_TEXTURE_WRAP_S                               = 0x2802
  val GL_TEXTURE_WRAP_T                               = 0x2803
  val GL_TEXTURE                                      = 0x1702
  val GL_TEXTURE_CUBE_MAP                             = 0x8513
  val GL_TEXTURE_BINDING_CUBE_MAP                     = 0x8514
  val GL_TEXTURE_CUBE_MAP_POSITIVE_X                  = 0x8515
  val GL_TEXTURE_CUBE_MAP_NEGATIVE_X                  = 0x8516
  val GL_TEXTURE_CUBE_MAP_POSITIVE_Y                  = 0x8517
  val GL_TEXTURE_CUBE_MAP_NEGATIVE_Y                  = 0x8518
  val GL_TEXTURE_CUBE_MAP_POSITIVE_Z                  = 0x8519
  val GL_TEXTURE_CUBE_MAP_NEGATIVE_Z                  = 0x851a
  val GL_MAX_CUBE_MAP_TEXTURE_SIZE                    = 0x851c
  val GL_TEXTURE0                                     = 0x84c0
  val GL_TEXTURE1                                     = 0x84c1
  val GL_TEXTURE2                                     = 0x84c2
  val GL_TEXTURE3                                     = 0x84c3
  val GL_TEXTURE4                                     = 0x84c4
  val GL_TEXTURE5                                     = 0x84c5
  val GL_TEXTURE6                                     = 0x84c6
  val GL_TEXTURE7                                     = 0x84c7
  val GL_TEXTURE8                                     = 0x84c8
  val GL_TEXTURE9                                     = 0x84c9
  val GL_TEXTURE10                                    = 0x84ca
  val GL_TEXTURE11                                    = 0x84cb
  val GL_TEXTURE12                                    = 0x84cc
  val GL_TEXTURE13                                    = 0x84cd
  val GL_TEXTURE14                                    = 0x84ce
  val GL_TEXTURE15                                    = 0x84cf
  val GL_TEXTURE16                                    = 0x84d0
  val GL_TEXTURE17                                    = 0x84d1
  val GL_TEXTURE18                                    = 0x84d2
  val GL_TEXTURE19                                    = 0x84d3
  val GL_TEXTURE20                                    = 0x84d4
  val GL_TEXTURE21                                    = 0x84d5
  val GL_TEXTURE22                                    = 0x84d6
  val GL_TEXTURE23                                    = 0x84d7
  val GL_TEXTURE24                                    = 0x84d8
  val GL_TEXTURE25                                    = 0x84d9
  val GL_TEXTURE26                                    = 0x84da
  val GL_TEXTURE27                                    = 0x84db
  val GL_TEXTURE28                                    = 0x84dc
  val GL_TEXTURE29                                    = 0x84dd
  val GL_TEXTURE30                                    = 0x84de
  val GL_TEXTURE31                                    = 0x84df
  val GL_ACTIVE_TEXTURE                               = 0x84e0
  val GL_REPEAT                                       = 0x2901
  val GL_CLAMP_TO_EDGE                                = 0x812f
  val GL_MIRRORED_REPEAT                              = 0x8370
  val GL_FLOAT_VEC2                                   = 0x8b50
  val GL_FLOAT_VEC3                                   = 0x8b51
  val GL_FLOAT_VEC4                                   = 0x8b52
  val GL_INT_VEC2                                     = 0x8b53
  val GL_INT_VEC3                                     = 0x8b54
  val GL_INT_VEC4                                     = 0x8b55
  val GL_BOOL                                         = 0x8b56
  val GL_BOOL_VEC2                                    = 0x8b57
  val GL_BOOL_VEC3                                    = 0x8b58
  val GL_BOOL_VEC4                                    = 0x8b59
  val GL_FLOAT_MAT2                                   = 0x8b5a
  val GL_FLOAT_MAT3                                   = 0x8b5b
  val GL_FLOAT_MAT4                                   = 0x8b5c
  val GL_SAMPLER_2D                                   = 0x8b5e
  val GL_SAMPLER_CUBE                                 = 0x8b60
  val GL_VERTEX_ATTRIB_ARRAY_ENABLED                  = 0x8622
  val GL_VERTEX_ATTRIB_ARRAY_SIZE                     = 0x8623
  val GL_VERTEX_ATTRIB_ARRAY_STRIDE                   = 0x8624
  val GL_VERTEX_ATTRIB_ARRAY_TYPE                     = 0x8625
  val GL_VERTEX_ATTRIB_ARRAY_NORMALIZED               = 0x886a
  val GL_VERTEX_ATTRIB_ARRAY_POINTER                  = 0x8645
  val GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING           = 0x889f
  val GL_IMPLEMENTATION_COLOR_READ_TYPE               = 0x8b9a
  val GL_IMPLEMENTATION_COLOR_READ_FORMAT             = 0x8b9b
  val GL_COMPILE_STATUS                               = 0x8b81
  val GL_INFO_LOG_LENGTH                              = 0x8b84
  val GL_SHADER_SOURCE_LENGTH                         = 0x8b88
  val GL_SHADER_COMPILER                              = 0x8dfa
  val GL_SHADER_BINARY_FORMATS                        = 0x8df8
  val GL_NUM_SHADER_BINARY_FORMATS                    = 0x8df9
  val GL_LOW_FLOAT                                    = 0x8df0
  val GL_MEDIUM_FLOAT                                 = 0x8df1
  val GL_HIGH_FLOAT                                   = 0x8df2
  val GL_LOW_INT                                      = 0x8df3
  val GL_MEDIUM_INT                                   = 0x8df4
  val GL_HIGH_INT                                     = 0x8df5
  val GL_FRAMEBUFFER                                  = 0x8d40
  val GL_RENDERBUFFER                                 = 0x8d41
  val GL_RGBA4                                        = 0x8056
  val GL_RGB5_A1                                      = 0x8057
  val GL_RGB565                                       = 0x8d62
  val GL_DEPTH_COMPONENT16                            = 0x81a5
  val GL_STENCIL_INDEX                                = 0x1901
  val GL_STENCIL_INDEX8                               = 0x8d48
  val GL_RENDERBUFFER_WIDTH                           = 0x8d42
  val GL_RENDERBUFFER_HEIGHT                          = 0x8d43
  val GL_RENDERBUFFER_INTERNAL_FORMAT                 = 0x8d44
  val GL_RENDERBUFFER_RED_SIZE                        = 0x8d50
  val GL_RENDERBUFFER_GREEN_SIZE                      = 0x8d51
  val GL_RENDERBUFFER_BLUE_SIZE                       = 0x8d52
  val GL_RENDERBUFFER_ALPHA_SIZE                      = 0x8d53
  val GL_RENDERBUFFER_DEPTH_SIZE                      = 0x8d54
  val GL_RENDERBUFFER_STENCIL_SIZE                    = 0x8d55
  val GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE           = 0x8cd0
  val GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME           = 0x8cd1
  val GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL         = 0x8cd2
  val GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE = 0x8cd3
  val GL_COLOR_ATTACHMENT0                            = 0x8ce0
  val GL_DEPTH_ATTACHMENT                             = 0x8d00
  val GL_STENCIL_ATTACHMENT                           = 0x8d20
  val GL_NONE                                         = 0
  val GL_FRAMEBUFFER_COMPLETE                         = 0x8cd5
  val GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT            = 0x8cd6
  val GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT    = 0x8cd7
  val GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS            = 0x8cd9
  val GL_FRAMEBUFFER_UNSUPPORTED                      = 0x8cdd
  val GL_FRAMEBUFFER_BINDING                          = 0x8ca6
  val GL_RENDERBUFFER_BINDING                         = 0x8ca7
  val GL_MAX_RENDERBUFFER_SIZE                        = 0x84e8
  val GL_INVALID_FRAMEBUFFER_OPERATION                = 0x0506
  val GL_VERTEX_PROGRAM_POINT_SIZE                    = 0x8642

  // Extensions
  val GL_COVERAGE_BUFFER_BIT_NV         = 0x8000
  val GL_TEXTURE_MAX_ANISOTROPY_EXT     = 0x84fe
  val GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT = 0x84ff
}
