package sge
package graphics
package profiling

import java.nio.{ Buffer, ByteBuffer, FloatBuffer, IntBuffer }

class GL32Interceptor(glProfiler: GLProfiler, val gl32: GL32) extends GL31Interceptor(glProfiler, gl32) with GL32 {

  override def glBlendBarrier(): Unit = {
    calls += 1
    gl32.glBlendBarrier()
    check()
  }

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
  ): Unit = {
    calls += 1
    gl32.glCopyImageSubData(srcName, srcTarget, srcLevel, srcX, srcY, srcZ, dstName, dstTarget, dstLevel, dstX, dstY, dstZ, srcWidth, srcHeight, srcDepth)
    check()
  }

  override def glDebugMessageControl(source: Int, `type`: Int, severity: Int, ids: IntBuffer, enabled: Boolean): Unit = {
    calls += 1
    gl32.glDebugMessageControl(source, `type`, severity, ids, enabled)
    check()
  }

  override def glDebugMessageInsert(source: Int, `type`: Int, id: Int, severity: Int, buf: String): Unit = {
    calls += 1
    gl32.glDebugMessageInsert(source, `type`, id, severity, buf)
    check()
  }

  override def glDebugMessageCallback(callsback: DebugProc): Unit = {
    calls += 1
    gl32.glDebugMessageCallback(callsback.asInstanceOf[gl32.DebugProc])
    check()
  }

  override def glGetDebugMessageLog(count: Int, sources: IntBuffer, types: IntBuffer, ids: IntBuffer, severities: IntBuffer, lengths: IntBuffer, messageLog: ByteBuffer): Int = {
    calls += 1
    val v = gl32.glGetDebugMessageLog(count, sources, types, ids, severities, lengths, messageLog)
    check()
    v
  }

  override def glPushDebugGroup(source: Int, id: Int, message: String): Unit = {
    calls += 1
    gl32.glPushDebugGroup(source, id, message)
    check()
  }

  override def glPopDebugGroup(): Unit = {
    calls += 1
    gl32.glPopDebugGroup()
    check()
  }

  override def glObjectLabel(identifier: Int, name: Int, label: String): Unit = {
    calls += 1
    gl32.glObjectLabel(identifier, name, label)
    check()
  }

  override def glGetObjectLabel(identifier: Int, name: Int): String = {
    calls += 1
    val v = gl32.glGetObjectLabel(identifier, name)
    check()
    v
  }

  override def glGetPointerv(pname: Int): Long = {
    calls += 1
    val v = gl32.glGetPointerv(pname)
    check()
    v
  }

  override def glEnablei(target: Int, index: Int): Unit = {
    calls += 1
    gl32.glEnablei(target, index)
    check()
  }

  override def glDisablei(target: Int, index: Int): Unit = {
    calls += 1
    gl32.glDisablei(target, index)
    check()
  }

  override def glBlendEquationi(buf: Int, mode: Int): Unit = {
    calls += 1
    gl32.glBlendEquationi(buf, mode)
    check()
  }

  override def glBlendEquationSeparatei(buf: Int, modeRGB: Int, modeAlpha: Int): Unit = {
    calls += 1
    gl32.glBlendEquationSeparatei(buf, modeRGB, modeAlpha)
    check()
  }

  override def glBlendFunci(buf: Int, src: Int, dst: Int): Unit = {
    calls += 1
    gl32.glBlendFunci(buf, src, dst)
    check()
  }

  override def glBlendFuncSeparatei(buf: Int, srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int): Unit = {
    calls += 1
    gl32.glBlendFuncSeparatei(buf, srcRGB, dstRGB, srcAlpha, dstAlpha)
    check()
  }

  override def glColorMaski(index: Int, r: Boolean, g: Boolean, b: Boolean, a: Boolean): Unit = {
    calls += 1
    gl32.glColorMaski(index, r, g, b, a)
    check()
  }

  override def glIsEnabledi(target: Int, index: Int): Boolean = {
    calls += 1
    val v = gl32.glIsEnabledi(target, index)
    check()
    v
  }

  override def glDrawElementsBaseVertex(mode: Int, count: Int, `type`: Int, indices: Buffer, basevertex: Int): Unit = {
    vertexCount.put(count.toFloat)
    drawCalls += 1
    calls += 1
    gl32.glDrawElementsBaseVertex(mode, count, `type`, indices, basevertex)
    check()
  }

  override def glDrawRangeElementsBaseVertex(mode: Int, start: Int, end: Int, count: Int, `type`: Int, indices: Buffer, basevertex: Int): Unit = {
    vertexCount.put(count.toFloat)
    drawCalls += 1
    calls += 1
    gl32.glDrawRangeElementsBaseVertex(mode, start, end, count, `type`, indices, basevertex)
    check()
  }

  override def glDrawElementsInstancedBaseVertex(mode: Int, count: Int, `type`: Int, indices: Buffer, instanceCount: Int, basevertex: Int): Unit = {
    vertexCount.put(count.toFloat)
    drawCalls += 1
    calls += 1
    gl32.glDrawElementsInstancedBaseVertex(mode, count, `type`, indices, instanceCount, basevertex)
    check()
  }

  override def glDrawElementsInstancedBaseVertex(mode: Int, count: Int, `type`: Int, indicesOffset: Int, instanceCount: Int, basevertex: Int): Unit = {
    vertexCount.put(count.toFloat)
    drawCalls += 1
    calls += 1
    gl32.glDrawElementsInstancedBaseVertex(mode, count, `type`, indicesOffset, instanceCount, basevertex)
    check()
  }

  override def glFramebufferTexture(target: Int, attachment: Int, texture: Int, level: Int): Unit = {
    calls += 1
    gl32.glFramebufferTexture(target, attachment, texture, level)
    check()
  }

  override def glGetGraphicsResetStatus(): Int = {
    calls += 1
    val v = gl32.glGetGraphicsResetStatus()
    check()
    v
  }

  override def glReadnPixels(x: Int, y: Int, width: Int, height: Int, format: Int, `type`: Int, bufSize: Int, data: Buffer): Unit = {
    calls += 1
    gl32.glReadnPixels(x, y, width, height, format, `type`, bufSize, data)
    check()
  }

  override def glGetnUniformfv(program: Int, location: Int, params: FloatBuffer): Unit = {
    calls += 1
    gl32.glGetnUniformfv(program, location, params)
    check()
  }

  override def glGetnUniformiv(program: Int, location: Int, params: IntBuffer): Unit = {
    calls += 1
    gl32.glGetnUniformiv(program, location, params)
    check()
  }

  override def glGetnUniformuiv(program: Int, location: Int, params: IntBuffer): Unit = {
    calls += 1
    gl32.glGetnUniformuiv(program, location, params)
    check()
  }

  override def glMinSampleShading(value: Float): Unit = {
    calls += 1
    gl32.glMinSampleShading(value)
    check()
  }

  override def glPatchParameteri(pname: Int, value: Int): Unit = {
    calls += 1
    gl32.glPatchParameteri(pname, value)
    check()
  }

  override def glTexParameterIiv(target: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl32.glTexParameterIiv(target, pname, params)
    check()
  }

  override def glTexParameterIuiv(target: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl32.glTexParameterIuiv(target, pname, params)
    check()
  }

  override def glGetTexParameterIiv(target: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl32.glGetTexParameterIiv(target, pname, params)
    check()
  }

  override def glGetTexParameterIuiv(target: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl32.glGetTexParameterIuiv(target, pname, params)
    check()
  }

  override def glSamplerParameterIiv(sampler: Int, pname: Int, param: IntBuffer): Unit = {
    calls += 1
    gl32.glSamplerParameterIiv(sampler, pname, param)
    check()
  }

  override def glSamplerParameterIuiv(sampler: Int, pname: Int, param: IntBuffer): Unit = {
    calls += 1
    gl32.glSamplerParameterIuiv(sampler, pname, param)
    check()
  }

  override def glGetSamplerParameterIiv(sampler: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl32.glGetSamplerParameterIiv(sampler, pname, params)
    check()
  }

  override def glGetSamplerParameterIuiv(sampler: Int, pname: Int, params: IntBuffer): Unit = {
    calls += 1
    gl32.glGetSamplerParameterIuiv(sampler, pname, params)
    check()
  }

  override def glTexBuffer(target: Int, internalformat: Int, buffer: Int): Unit = {
    calls += 1
    gl32.glTexBuffer(target, internalformat, buffer)
    check()
  }

  override def glTexBufferRange(target: Int, internalformat: Int, buffer: Int, offset: Int, size: Int): Unit = {
    calls += 1
    gl32.glTexBufferRange(target, internalformat, buffer, offset, size)
    check()
  }

  override def glTexStorage3DMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int, depth: Int, fixedsamplelocations: Boolean): Unit = {
    calls += 1
    gl32.glTexStorage3DMultisample(target, samples, internalformat, width, height, depth, fixedsamplelocations)
    check()
  }
}
