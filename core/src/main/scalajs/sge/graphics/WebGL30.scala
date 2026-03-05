/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../GwtGL30.java
 * Original authors: Simon Gerst, JamesTKhan
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: GwtGL30 -> WebGL30
 *   Convention: Scala.js only; extends WebGL20; WebGL2RenderingContext via js.Dynamic
 *   Convention: GWT IntMap<JSO> -> js.Array[js.Dynamic] for GL30 handle maps
 *   Idiom: GdxRuntimeException -> SgeError.GraphicsError
 *   TODO: Pixmap-based texImage3D/texSubImage3D path (requires browser Pixmap implementation)
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import java.nio.{ Buffer, ByteBuffer, FloatBuffer, IntBuffer, LongBuffer }
import scala.scalajs.js
import scala.scalajs.js.typedarray._

/** WebGL 2.0 implementation of [[GL30]] for the browser backend. Extends [[WebGL20]] with WebGL2-specific methods.
  *
  * @param gl2
  *   the WebGL2RenderingContext obtained from a canvas element
  */
class WebGL30(gl2: js.Dynamic) extends WebGL20(gl2) with GL30 {

  // ------ GL30-specific handle maps ------

  private val queries:           js.Array[js.Dynamic] = js.Array(js.undefined.asInstanceOf[js.Dynamic])
  private var nextQueryId:       Int                  = 1
  private val samplers:          js.Array[js.Dynamic] = js.Array(js.undefined.asInstanceOf[js.Dynamic])
  private var nextSamplerId:     Int                  = 1
  private val feedbacks:         js.Array[js.Dynamic] = js.Array(js.undefined.asInstanceOf[js.Dynamic])
  private var nextFeedbackId:    Int                  = 1
  private val vertexArrays:      js.Array[js.Dynamic] = js.Array(js.undefined.asInstanceOf[js.Dynamic])
  private var nextVertexArrayId: Int                  = 1

  private val uIntBuffer: Uint32Array = new Uint32Array(2000 * 6)

  // ------ GL30-specific helpers ------

  private def copyUnsigned(buffer: IntBuffer): Uint32Array = {
    val remaining = buffer.remaining()
    var i         = buffer.position()
    var j         = 0
    while (i < buffer.limit()) {
      uIntBuffer(j) = (buffer.get(i).toLong & 0xffffffffL).toDouble
      i += 1
      j += 1
    }
    uIntBuffer.subarray(0, remaining)
  }

  private def allocateQueryId(query: js.Dynamic): Int = {
    val id = nextQueryId
    nextQueryId += 1
    queries(id) = query
    id
  }

  private def deallocateQueryId(id: Int): Unit =
    queries(id) = js.undefined.asInstanceOf[js.Dynamic]

  private def allocateSamplerId(sampler: js.Dynamic): Int = {
    val id = nextSamplerId
    nextSamplerId += 1
    samplers(id) = sampler
    id
  }

  private def deallocateSamplerId(id: Int): Unit =
    samplers(id) = js.undefined.asInstanceOf[js.Dynamic]

  private def allocateFeedbackId(feedback: js.Dynamic): Int = {
    val id = nextFeedbackId
    nextFeedbackId += 1
    feedbacks(id) = feedback
    id
  }

  private def deallocateFeedbackId(id: Int): Unit =
    feedbacks(id) = js.undefined.asInstanceOf[js.Dynamic]

  private def allocateVertexArrayId(vArray: js.Dynamic): Int = {
    val id = nextVertexArrayId
    nextVertexArrayId += 1
    vertexArrays(id) = vArray
    id
  }

  private def deallocateVertexArrayId(id: Int): Unit =
    vertexArrays(id) = js.undefined.asInstanceOf[js.Dynamic]

  // ======================================================================
  // GL30 method implementations
  // ======================================================================

  override def glReadBuffer(mode: Int): Unit =
    gl.readBuffer(mode)

  override def glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, `type`: Int, indices: Buffer): Unit =
    gl.drawRangeElements(mode, start, end, count, `type`, indices.position())

  override def glDrawRangeElements(mode: Int, start: Int, end: Int, count: Int, `type`: Int, offset: Int): Unit =
    gl.drawRangeElements(mode, start, end, count, `type`, offset)

  override def glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, `type`: Int, offset: Int): Unit =
    gl.texImage2D(target, level, internalformat, width, height, border, format, `type`, offset)

  override def glTexImage3D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: Int, `type`: Int, pixels: Buffer): Unit =
    if (pixels == null) {
      gl.texImage3D(target, level, internalformat, width, height, depth, border, format, `type`, null)
    } else {
      pixels match {
        case fb: FloatBuffer =>
          gl.texImage3D(target, level, internalformat, width, height, depth, border, format, `type`, copy(fb))
        case ib: IntBuffer =>
          val int32 = copy(ib)
          val uint8 = new Uint8Array(int32.buffer, int32.byteOffset, int32.byteLength)
          gl.texImage3D(target, level, internalformat, width, height, depth, border, format, `type`, uint8)
        case bb: ByteBuffer =>
          val int8  = copy(bb)
          val uint8 = new Uint8Array(int8.buffer, int8.byteOffset, int8.byteLength)
          gl.texImage3D(target, level, internalformat, width, height, depth, border, format, `type`, uint8)
        case _ =>
          throw utils.SgeError.GraphicsError("Unsupported Buffer type for glTexImage3D")
      }
    }

  override def glTexImage3D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int, format: Int, `type`: Int, offset: Int): Unit =
    gl.texImage3D(target, level, internalformat, width, height, depth, border, format, `type`, offset)

  override def glTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, `type`: Int, offset: Int): Unit =
    gl.texSubImage2D(target, level, xoffset, yoffset, width, height, format, `type`, offset)

  override def glTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int, format: Int, `type`: Int, pixels: Buffer): Unit =
    pixels match {
      case fb: FloatBuffer =>
        gl.texSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, `type`, copy(fb))
      case ib: IntBuffer =>
        val int32 = copy(ib)
        val uint8 = new Uint8Array(int32.buffer, int32.byteOffset, int32.byteLength)
        gl.texSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, `type`, uint8)
      case bb: ByteBuffer =>
        val int8  = copy(bb)
        val uint8 = new Uint8Array(int8.buffer, int8.byteOffset, int8.byteLength)
        gl.texSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, `type`, uint8)
      case _ =>
        throw utils.SgeError.GraphicsError("Unsupported Buffer type for glTexSubImage3D")
    }

  override def glTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, width: Int, height: Int, depth: Int, format: Int, `type`: Int, offset: Int): Unit =
    gl.texSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, `type`, offset)

  override def glCopyTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit =
    gl.copyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height)

  // ------ Queries ------

  override def glGenQueries(n: Int, ids: Array[Int], offset: Int): Unit = {
    var i = offset
    while (i < offset + n) {
      val query = gl.createQuery()
      ids(i) = allocateQueryId(query)
      i += 1
    }
  }

  override def glGenQueries(n: Int, ids: IntBuffer): Unit = {
    val startPosition = ids.position()
    var i             = 0
    while (i < n) {
      val query = gl.createQuery()
      ids.put(allocateQueryId(query))
      i += 1
    }
    ids.position(startPosition)
  }

  override def glDeleteQueries(n: Int, ids: Array[Int], offset: Int): Unit = {
    var i = offset
    while (i < offset + n) {
      val id    = ids(i)
      val query = queries(id)
      deallocateQueryId(id)
      gl.deleteQuery(query)
      i += 1
    }
  }

  override def glDeleteQueries(n: Int, ids: IntBuffer): Unit = {
    val startPosition = ids.position()
    var i             = 0
    while (i < n) {
      val id    = ids.get()
      val query = queries(id)
      deallocateQueryId(id)
      gl.deleteQuery(query)
      i += 1
    }
    ids.position(startPosition)
  }

  override def glIsQuery(id: Int): Boolean =
    gl.isQuery(queries(id)).asInstanceOf[Boolean]

  override def glBeginQuery(target: Int, id: Int): Unit =
    gl.beginQuery(target, queries(id))

  override def glEndQuery(target: Int): Unit =
    gl.endQuery(target)

  override def glGetQueryiv(target: Int, pname: Int, params: IntBuffer): Unit = {
    val query = gl.getQuery(target, pname)
    if (query == null) params.put(0)
    else params.put(findKey(queries, query))
    params.flip()
  }

  override def glGetQueryObjectuiv(id: Int, pname: Int, params: IntBuffer): Unit = {
    if (pname == GL30.GL_QUERY_RESULT) {
      params.put(gl.getQueryParameter(queries(id), pname).asInstanceOf[Int])
    } else if (pname == GL30.GL_QUERY_RESULT_AVAILABLE) {
      val result = gl.getQueryParameter(queries(id), pname).asInstanceOf[Boolean]
      params.put(if (result) GL20.GL_TRUE else GL20.GL_FALSE)
    } else {
      throw utils.SgeError.GraphicsError("Unsupported pname passed to glGetQueryObjectuiv")
    }
    params.flip()
  }

  // ------ Buffer mapping ------

  override def glUnmapBuffer(target: Int): Boolean =
    throw utils.SgeError.GraphicsError("glUnmapBuffer not supported on WebGL2")

  override def glGetBufferPointerv(target: Int, pname: Int): Buffer =
    throw utils.SgeError.GraphicsError("glGetBufferPointerv not supported on WebGL2")

  // ------ Draw buffers ------

  override def glDrawBuffers(n: Int, bufs: IntBuffer): Unit = {
    val startPosition = bufs.position()
    gl.drawBuffers(copy(bufs).subarray(0, n))
    bufs.position(startPosition)
  }

  // ------ Non-square uniform matrices ------

  override def glUniformMatrix2x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    gl.uniformMatrix2x3fv(getUniformLocation(location), transpose, copy(value))

  override def glUniformMatrix3x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    gl.uniformMatrix3x2fv(getUniformLocation(location), transpose, copy(value))

  override def glUniformMatrix2x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    gl.uniformMatrix2x4fv(getUniformLocation(location), transpose, copy(value))

  override def glUniformMatrix4x2fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    gl.uniformMatrix4x2fv(getUniformLocation(location), transpose, copy(value))

  override def glUniformMatrix3x4fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    gl.uniformMatrix3x4fv(getUniformLocation(location), transpose, copy(value))

  override def glUniformMatrix4x3fv(location: Int, count: Int, transpose: Boolean, value: FloatBuffer): Unit =
    gl.uniformMatrix4x3fv(getUniformLocation(location), transpose, copy(value))

  // ------ Framebuffer ------

  override def glBlitFramebuffer(srcX0: Int, srcY0: Int, srcX1: Int, srcY1: Int, dstX0: Int, dstY0: Int, dstX1: Int, dstY1: Int, mask: Int, filter: Int): Unit =
    gl.blitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter)

  override def glRenderbufferStorageMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int): Unit =
    gl.renderbufferStorageMultisample(target, samples, internalformat, width, height)

  override def glFramebufferTextureLayer(target: Int, attachment: Int, texture: Int, level: Int, layer: Int): Unit =
    gl.framebufferTextureLayer(target, attachment, textures(texture), level, layer)

  // ------ Buffer mapping (continued) ------

  override def glMapBufferRange(target: Int, offset: Int, length: Int, access: Int): Buffer =
    throw utils.SgeError.GraphicsError("glMapBufferRange not supported on WebGL2")

  override def glFlushMappedBufferRange(target: Int, offset: Int, length: Int): Unit =
    throw utils.SgeError.GraphicsError("glFlushMappedBufferRange not supported on WebGL2")

  // ------ Vertex arrays ------

  override def glBindVertexArray(array: Int): Unit =
    gl.bindVertexArray(vertexArrays(array))

  override def glDeleteVertexArrays(n: Int, arrays: Array[Int], offset: Int): Unit = {
    var i = offset
    while (i < offset + n) {
      val id     = arrays(i)
      val vArray = vertexArrays(id)
      deallocateVertexArrayId(id)
      gl.deleteVertexArray(vArray)
      i += 1
    }
  }

  override def glDeleteVertexArrays(n: Int, arrays: IntBuffer): Unit = {
    val startPosition = arrays.position()
    var i             = 0
    while (i < n) {
      val id     = arrays.get()
      val vArray = vertexArrays(id)
      deallocateVertexArrayId(id)
      gl.deleteVertexArray(vArray)
      i += 1
    }
    arrays.position(startPosition)
  }

  override def glGenVertexArrays(n: Int, arrays: Array[Int], offset: Int): Unit = {
    var i = offset
    while (i < offset + n) {
      val vArray = gl.createVertexArray()
      arrays(i) = allocateVertexArrayId(vArray)
      i += 1
    }
  }

  override def glGenVertexArrays(n: Int, arrays: IntBuffer): Unit = {
    val startPosition = arrays.position()
    var i             = 0
    while (i < n) {
      val vArray = gl.createVertexArray()
      arrays.put(allocateVertexArrayId(vArray))
      i += 1
    }
    arrays.position(startPosition)
  }

  override def glIsVertexArray(array: Int): Boolean =
    gl.isVertexArray(vertexArrays(array)).asInstanceOf[Boolean]

  // ------ Transform feedback ------

  override def glBeginTransformFeedback(primitiveMode: Int): Unit =
    gl.beginTransformFeedback(primitiveMode)

  override def glEndTransformFeedback(): Unit =
    gl.endTransformFeedback()

  override def glBindBufferRange(target: Int, index: Int, buffer: Int, offset: Int, size: Int): Unit =
    gl.bindBufferRange(target, index, buffers(buffer), offset, size)

  override def glBindBufferBase(target: Int, index: Int, buffer: Int): Unit =
    gl.bindBufferBase(target, index, buffers(buffer))

  override def glTransformFeedbackVaryings(program: Int, varyings: Array[String], bufferMode: Int): Unit =
    gl.transformFeedbackVaryings(programs(program), varyings.asInstanceOf[js.Any], bufferMode)

  override def glBindTransformFeedback(target: Int, id: Int): Unit =
    gl.bindTransformFeedback(target, feedbacks(id))

  override def glDeleteTransformFeedbacks(n: Int, ids: Array[Int], offset: Int): Unit = {
    var i = offset
    while (i < offset + n) {
      val id       = ids(i)
      val feedback = feedbacks(id)
      deallocateFeedbackId(id)
      gl.deleteTransformFeedback(feedback)
      i += 1
    }
  }

  override def glDeleteTransformFeedbacks(n: Int, ids: IntBuffer): Unit = {
    val startPosition = ids.position()
    var i             = 0
    while (i < n) {
      val id       = ids.get()
      val feedback = feedbacks(id)
      deallocateFeedbackId(id)
      gl.deleteTransformFeedback(feedback)
      i += 1
    }
    ids.position(startPosition)
  }

  override def glGenTransformFeedbacks(n: Int, ids: Array[Int], offset: Int): Unit = {
    var i = offset
    while (i < offset + n) {
      val feedback = gl.createTransformFeedback()
      ids(i) = allocateFeedbackId(feedback)
      i += 1
    }
  }

  override def glGenTransformFeedbacks(n: Int, ids: IntBuffer): Unit = {
    val startPosition = ids.position()
    var i             = 0
    while (i < n) {
      val feedback = gl.createTransformFeedback()
      ids.put(allocateFeedbackId(feedback))
      i += 1
    }
    ids.position(startPosition)
  }

  override def glIsTransformFeedback(id: Int): Boolean =
    gl.isTransformFeedback(feedbacks(id)).asInstanceOf[Boolean]

  override def glPauseTransformFeedback(): Unit =
    gl.pauseTransformFeedback()

  override def glResumeTransformFeedback(): Unit =
    gl.resumeTransformFeedback()

  // ------ Vertex attrib integer ------

  override def glVertexAttribIPointer(index: Int, size: Int, `type`: Int, stride: Int, offset: Int): Unit =
    gl.vertexAttribIPointer(index, size, `type`, stride, offset)

  override def glGetVertexAttribIiv(index: Int, pname: Int, params: IntBuffer): Unit =
    throw utils.SgeError.GraphicsError("glGetVertexAttribIiv not implemented on WebGL2")

  override def glGetVertexAttribIuiv(index: Int, pname: Int, params: IntBuffer): Unit =
    throw utils.SgeError.GraphicsError("glGetVertexAttribIuiv not implemented on WebGL2")

  override def glVertexAttribI4i(index: Int, x: Int, y: Int, z: Int, w: Int): Unit =
    gl.vertexAttribI4i(index, x, y, z, w)

  override def glVertexAttribI4ui(index: Int, x: Int, y: Int, z: Int, w: Int): Unit =
    gl.vertexAttribI4ui(index, x, y, z, w)

  // ------ Unsigned integer uniforms ------

  override def glGetUniformuiv(program: Int, location: Int, params: IntBuffer): Unit =
    throw utils.SgeError.GraphicsError("glGetUniformuiv not implemented on WebGL2")

  override def glGetFragDataLocation(program: Int, name: String): Int =
    gl.getFragDataLocation(programs(program), name).asInstanceOf[Int]

  override def glUniform1uiv(location: Int, count: Int, value: IntBuffer): Unit =
    gl.uniform1uiv(getUniformLocation(location), copyUnsigned(value), 0, count)

  override def glUniform3uiv(location: Int, count: Int, value: IntBuffer): Unit =
    gl.uniform3uiv(getUniformLocation(location), copyUnsigned(value), 0, count)

  override def glUniform4uiv(location: Int, count: Int, value: IntBuffer): Unit =
    gl.uniform4uiv(getUniformLocation(location), copyUnsigned(value), 0, count)

  // ------ Clear buffers ------

  override def glClearBufferiv(buffer: Int, drawbuffer: Int, value: IntBuffer): Unit =
    gl.clearBufferiv(buffer, drawbuffer, copy(value))

  override def glClearBufferuiv(buffer: Int, drawbuffer: Int, value: IntBuffer): Unit =
    gl.clearBufferuiv(buffer, drawbuffer, copy(value))

  override def glClearBufferfv(buffer: Int, drawbuffer: Int, value: FloatBuffer): Unit =
    gl.clearBufferfv(buffer, drawbuffer, copy(value))

  override def glClearBufferfi(buffer: Int, drawbuffer: Int, depth: Float, stencil: Int): Unit =
    gl.clearBufferfi(buffer, drawbuffer, depth, stencil)

  // ------ String queries ------

  override def glGetStringi(name: Int, index: Int): String =
    throw utils.SgeError.GraphicsError("glGetStringi not supported on WebGL2")

  // ------ Buffer copy ------

  override def glCopyBufferSubData(readTarget: Int, writeTarget: Int, readOffset: Int, writeOffset: Int, size: Int): Unit =
    gl.copyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size)

  // ------ Uniform blocks ------

  override def glGetUniformIndices(program: Int, uniformNames: Array[String], uniformIndices: IntBuffer): Unit = {
    val array = gl.getUniformIndices(programs(program), uniformNames.asInstanceOf[js.Any])
    val len   = array.length.asInstanceOf[Int]
    var i     = 0
    while (i < len) {
      uniformIndices.put(i, array(i).asInstanceOf[Int])
      i += 1
    }
    uniformIndices.flip()
  }

  override def glGetActiveUniformsiv(program: Int, uniformCount: Int, uniformIndices: IntBuffer, pname: Int, params: IntBuffer): Unit = {
    if (pname == GL30.GL_UNIFORM_IS_ROW_MAJOR) {
      val arr = gl.getActiveUniforms(programs(program), copy(uniformIndices).subarray(0, uniformCount), pname)
      var i   = 0
      while (i < uniformCount) {
        val v = arr(i).asInstanceOf[Boolean]
        params.put(i, if (v) GL20.GL_TRUE else GL20.GL_FALSE)
        i += 1
      }
    } else {
      val arr = gl.getActiveUniforms(programs(program), copy(uniformIndices).subarray(0, uniformCount), pname)
      var i   = 0
      while (i < uniformCount) {
        params.put(i, arr(i).asInstanceOf[Int])
        i += 1
      }
    }
    params.flip()
  }

  override def glGetUniformBlockIndex(program: Int, uniformBlockName: String): Int =
    gl.getUniformBlockIndex(programs(program), uniformBlockName).asInstanceOf[Int]

  override def glGetActiveUniformBlockiv(program: Int, uniformBlockIndex: Int, pname: Int, params: IntBuffer): Unit = {
    if (
      pname == GL30.GL_UNIFORM_BLOCK_BINDING || pname == GL30.GL_UNIFORM_BLOCK_DATA_SIZE
      || pname == GL30.GL_UNIFORM_BLOCK_ACTIVE_UNIFORMS
    ) {
      params.put(gl.getActiveUniformBlockParameter(programs(program), uniformBlockIndex, pname).asInstanceOf[Int])
    } else if (pname == GL30.GL_UNIFORM_BLOCK_ACTIVE_UNIFORM_INDICES) {
      val array = gl.getActiveUniformBlockParameter(programs(program), uniformBlockIndex, pname).asInstanceOf[Uint32Array]
      var i     = 0
      while (i < array.length) {
        params.put(i, array(i).toInt)
        i += 1
      }
    } else if (
      pname == GL30.GL_UNIFORM_BLOCK_REFERENCED_BY_VERTEX_SHADER
      || pname == GL30.GL_UNIFORM_BLOCK_REFERENCED_BY_FRAGMENT_SHADER
    ) {
      val result = gl.getActiveUniformBlockParameter(programs(program), uniformBlockIndex, pname).asInstanceOf[Boolean]
      params.put(if (result) GL20.GL_TRUE else GL20.GL_FALSE)
    } else {
      throw utils.SgeError.GraphicsError("Unsupported pname passed to glGetActiveUniformBlockiv")
    }
    params.flip()
  }

  override def glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int, length: Buffer, uniformBlockName: Buffer): Unit =
    throw utils.SgeError.GraphicsError("glGetActiveUniformBlockName with Buffer parameters not supported on WebGL2")

  override def glGetActiveUniformBlockName(program: Int, uniformBlockIndex: Int): String =
    gl.getActiveUniformBlockName(programs(program), uniformBlockIndex).asInstanceOf[String]

  override def glUniformBlockBinding(program: Int, uniformBlockIndex: Int, uniformBlockBinding: Int): Unit =
    gl.uniformBlockBinding(programs(program), uniformBlockIndex, uniformBlockBinding)

  // ------ Instancing ------

  override def glDrawArraysInstanced(mode: Int, first: Int, count: Int, instanceCount: Int): Unit =
    gl.drawArraysInstanced(mode, first, count, instanceCount)

  override def glDrawElementsInstanced(mode: Int, count: Int, `type`: Int, indicesOffset: Int, instanceCount: Int): Unit =
    gl.drawElementsInstanced(mode, count, `type`, indicesOffset, instanceCount)

  // ------ 64-bit queries ------

  override def glGetInteger64v(pname: Int, params: LongBuffer): Unit =
    pname match {
      case GL30.GL_MAX_COMBINED_FRAGMENT_UNIFORM_COMPONENTS | GL30.GL_MAX_COMBINED_VERTEX_UNIFORM_COMPONENTS | GL30.GL_MAX_ELEMENT_INDEX | GL30.GL_MAX_SERVER_WAIT_TIMEOUT |
          GL30.GL_MAX_UNIFORM_BLOCK_SIZE =>
        params.put(gl.getParameter(pname).asInstanceOf[Double].toLong)
        params.flip()
      case _ =>
        throw utils.SgeError.GraphicsError("Given glGetInteger64v enum not supported on WebGL2")
    }

  override def glGetBufferParameteri64v(target: Int, pname: Int, params: LongBuffer): Unit =
    throw utils.SgeError.GraphicsError("glGetBufferParameteri64v not supported on WebGL2")

  // ------ Samplers ------

  override def glGenSamplers(count: Int, samplers: Array[Int], offset: Int): Unit = {
    var i = offset
    while (i < offset + count) {
      val sampler = gl.createSampler()
      samplers(i) = allocateSamplerId(sampler)
      i += 1
    }
  }

  override def glGenSamplers(count: Int, samplers: IntBuffer): Unit = {
    val startPosition = samplers.position()
    var i             = 0
    while (i < count) {
      val sampler = gl.createSampler()
      samplers.put(allocateSamplerId(sampler))
      i += 1
    }
    samplers.position(startPosition)
  }

  override def glDeleteSamplers(count: Int, samplers: Array[Int], offset: Int): Unit = {
    var i = offset
    while (i < offset + count) {
      val id      = samplers(i)
      val sampler = this.samplers(id)
      deallocateSamplerId(id)
      gl.deleteSampler(sampler)
      i += 1
    }
  }

  override def glDeleteSamplers(count: Int, samplers: IntBuffer): Unit = {
    val startPosition = samplers.position()
    var i             = 0
    while (i < count) {
      val id      = samplers.get()
      val sampler = this.samplers(id)
      deallocateSamplerId(id)
      gl.deleteSampler(sampler)
      i += 1
    }
    samplers.position(startPosition)
  }

  override def glIsSampler(sampler: Int): Boolean =
    gl.isSampler(samplers(sampler)).asInstanceOf[Boolean]

  override def glBindSampler(unit: Int, sampler: Int): Unit =
    gl.bindSampler(unit, samplers(sampler))

  override def glSamplerParameteri(sampler: Int, pname: Int, param: Int): Unit =
    gl.samplerParameteri(samplers(sampler), pname, param)

  override def glSamplerParameteriv(sampler: Int, pname: Int, param: IntBuffer): Unit =
    gl.samplerParameterf(samplers(sampler), pname, param.get())

  override def glSamplerParameterf(sampler: Int, pname: Int, param: Float): Unit =
    gl.samplerParameterf(samplers(sampler), pname, param)

  override def glSamplerParameterfv(sampler: Int, pname: Int, param: FloatBuffer): Unit =
    gl.samplerParameterf(samplers(sampler), pname, param.get())

  override def glGetSamplerParameteriv(sampler: Int, pname: Int, params: IntBuffer): Unit = {
    params.put(gl.getSamplerParameter(samplers(sampler), pname).asInstanceOf[Int])
    params.flip()
  }

  override def glGetSamplerParameterfv(sampler: Int, pname: Int, params: FloatBuffer): Unit = {
    params.put(gl.getSamplerParameter(samplers(sampler), pname).asInstanceOf[Double].toFloat)
    params.flip()
  }

  // ------ Vertex attrib divisor ------

  override def glVertexAttribDivisor(index: Int, divisor: Int): Unit =
    gl.vertexAttribDivisor(index, divisor)

  // ------ Program parameters ------

  override def glProgramParameteri(program: Int, pname: Int, value: Int): Unit =
    throw utils.SgeError.GraphicsError("glProgramParameteri not supported on WebGL2")

  // ------ Framebuffer invalidation ------

  override def glInvalidateFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer): Unit = {
    val startPosition = attachments.position()
    gl.invalidateFramebuffer(target, copy(attachments).subarray(0, numAttachments))
    attachments.position(startPosition)
  }

  override def glInvalidateSubFramebuffer(target: Int, numAttachments: Int, attachments: IntBuffer, x: Int, y: Int, width: Int, height: Int): Unit = {
    val startPosition = attachments.position()
    gl.invalidateSubFramebuffer(target, copy(attachments).subarray(0, numAttachments), x, y, width, height)
    attachments.position(startPosition)
  }

  // ------ Override GL20 methods for GL30-specific pnames ------

  override def glGetFloatv(pname: Int, params: FloatBuffer): Unit =
    if (pname == GL30.GL_MAX_TEXTURE_LOD_BIAS) {
      params.put(0, gl.getParameter(pname).asInstanceOf[Double].toFloat)
      params.flip()
    } else {
      super.glGetFloatv(pname, params)
    }

  override def glGetIntegerv(pname: Int, params: IntBuffer): Unit =
    pname match {
      case GL30.GL_DRAW_BUFFER0 | GL30.GL_DRAW_BUFFER1 | GL30.GL_DRAW_BUFFER2 | GL30.GL_DRAW_BUFFER3 | GL30.GL_DRAW_BUFFER4 | GL30.GL_DRAW_BUFFER5 | GL30.GL_DRAW_BUFFER6 | GL30.GL_DRAW_BUFFER7 |
          GL30.GL_DRAW_BUFFER8 | GL30.GL_DRAW_BUFFER9 | GL30.GL_DRAW_BUFFER10 | GL30.GL_FRAGMENT_SHADER_DERIVATIVE_HINT | GL30.GL_MAX_3D_TEXTURE_SIZE | GL30.GL_MAX_ARRAY_TEXTURE_LAYERS |
          GL30.GL_MAX_COLOR_ATTACHMENTS | GL30.GL_MAX_DRAW_BUFFERS | GL30.GL_MAX_ELEMENTS_INDICES | GL30.GL_MAX_ELEMENTS_VERTICES | GL30.GL_MAX_FRAGMENT_INPUT_COMPONENTS |
          GL30.GL_MAX_FRAGMENT_UNIFORM_BLOCKS | GL30.GL_MAX_FRAGMENT_UNIFORM_COMPONENTS | GL30.GL_MAX_PROGRAM_TEXEL_OFFSET | GL30.GL_MAX_SAMPLES |
          GL30.GL_MAX_TRANSFORM_FEEDBACK_INTERLEAVED_COMPONENTS | GL30.GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_ATTRIBS | GL30.GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_COMPONENTS |
          GL30.GL_MAX_UNIFORM_BUFFER_BINDINGS | GL30.GL_MAX_VARYING_COMPONENTS | GL30.GL_MAX_VERTEX_OUTPUT_COMPONENTS | GL30.GL_MAX_VERTEX_UNIFORM_BLOCKS | GL30.GL_MAX_VERTEX_UNIFORM_COMPONENTS |
          GL30.GL_MIN_PROGRAM_TEXEL_OFFSET | GL30.GL_PACK_ROW_LENGTH | GL30.GL_PACK_SKIP_PIXELS | GL30.GL_PACK_SKIP_ROWS | GL30.GL_READ_BUFFER | GL30.GL_UNPACK_IMAGE_HEIGHT |
          GL30.GL_UNPACK_ROW_LENGTH | GL30.GL_UNPACK_SKIP_IMAGES | GL30.GL_UNPACK_SKIP_PIXELS | GL30.GL_UNPACK_SKIP_ROWS =>
        params.put(0, gl.getParameter(pname).asInstanceOf[Int])
        params.flip()
      case GL30.GL_DRAW_FRAMEBUFFER_BINDING | GL30.GL_READ_FRAMEBUFFER_BINDING =>
        val fbo = gl.getParameter(pname)
        if (fbo == null) params.put(0)
        else params.put(findKey(frameBuffers, fbo))
        params.flip()
      case GL30.GL_TEXTURE_BINDING_2D_ARRAY | GL30.GL_TEXTURE_BINDING_3D =>
        val tex = gl.getParameter(pname)
        if (tex == null) params.put(0)
        else params.put(findKey(textures, tex))
        params.flip()
      case GL30.GL_VERTEX_ARRAY_BINDING =>
        val obj = gl.getParameter(pname)
        if (obj == null) params.put(0)
        else params.put(findKey(vertexArrays, obj))
        params.flip()
      case _ =>
        super.glGetIntegerv(pname, params)
    }

  override def glGetFramebufferAttachmentParameteriv(target: Int, attachment: Int, pname: Int, params: IntBuffer): Unit =
    pname match {
      case GL30.GL_FRAMEBUFFER_ATTACHMENT_ALPHA_SIZE | GL30.GL_FRAMEBUFFER_ATTACHMENT_BLUE_SIZE | GL30.GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING | GL30.GL_FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE |
          GL30.GL_FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE | GL30.GL_FRAMEBUFFER_ATTACHMENT_GREEN_SIZE | GL30.GL_FRAMEBUFFER_ATTACHMENT_RED_SIZE | GL30.GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE |
          GL30.GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LAYER =>
        params.put(0, gl.getFramebufferAttachmentParameter(target, attachment, pname).asInstanceOf[Int])
        params.flip()
      case _ =>
        super.glGetFramebufferAttachmentParameteriv(target, attachment, pname, params)
    }

  // Deprecated in GL30: vertex arrays from client memory
  override def glVertexAttribPointer(indx: Int, size: Int, `type`: Int, normalized: Boolean, stride: Int, ptr: Buffer): Unit =
    throw utils.SgeError.GraphicsError("Vertex arrays from client memory not supported in WebGL")
}
