/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package graphics
package profiling

import sge.noop.{ NoopGL20, NoopGraphics }

/** Behavioral characterization of the GL profiling stack, verified against the LibGDX reference semantics of `GLProfiler` (com/badlogic/gdx/graphics/profiling/GLProfiler.java) and `GL20Interceptor` /
  * `GLInterceptor`:
  *
  *   - every intercepted GL call increments `calls`
  *   - `glBindTexture` also increments `textureBindings`
  *   - `glUseProgram` also increments `shaderSwitches`
  *   - `glDrawArrays`/`glDrawElements` increment `drawCalls` and feed the vertex count
  *   - `reset()` zeroes all counters and the vertex `FloatCounter`
  *   - `enable()`/`disable()` swap and restore the wrapped GL instance and toggle `enabled`
  *   - the constructor selects `GL20Interceptor` when only GL20 is available
  *   - `check()` drains `glGetError()` and reports every error to the listener
  *   - `GLInterceptor.resolveErrorNumber` maps GL error codes to their names
  *
  * The tests drive the real interceptor (installed by `enable()` as the graphics' GL20) so counter increments are observed through the production code path, not by inspecting private state.
  */
class GLProfilerCharacterizationSuite extends munit.FunSuite {

  test("fresh profiler: counters zeroed, disabled, logging listener installed") {
    val profiler = GLProfiler(new NoopGraphics())
    assertEquals(profiler.enabled, false)
    assertEquals(profiler.calls, 0)
    assertEquals(profiler.textureBindings, 0)
    assertEquals(profiler.drawCalls, 0)
    assertEquals(profiler.shaderSwitches, 0)
    assertEquals(profiler.vertexCount.count, 0)
    assertEquals(profiler.vertexCount.total, 0f)
    assert(profiler.listener eq GLErrorListener.LOGGING_LISTENER)
  }

  test("constructor selects GL20Interceptor when only GL20 is available") {
    // NoopGraphics exposes no GL30/31/32 (all Nullable.empty), so the highest available level is GL20.
    val graphics = new NoopGraphics()
    val profiler = GLProfiler(graphics)
    profiler.enable()
    assert(graphics.gl20.isInstanceOf[GL20Interceptor], s"expected GL20Interceptor, got ${graphics.gl20.getClass.getName}")
    assert(!graphics.gl20.isInstanceOf[GL30], "GL20Interceptor must not masquerade as a GL30 interceptor")
  }

  test("enable installs the interceptor as graphics.gl20 and is idempotent") {
    val graphics = new NoopGraphics()
    val profiler = GLProfiler(graphics)
    assert(graphics.gl20 eq NoopGL20)
    profiler.enable()
    assertEquals(profiler.enabled, true)
    val installed = graphics.gl20
    assert(installed.isInstanceOf[GL20Interceptor])
    // A second enable() must not re-wrap or throw.
    profiler.enable()
    assertEquals(profiler.enabled, true)
    assert(graphics.gl20 eq installed)
  }

  test("disable restores the original GL20 and clears enabled; idempotent when already disabled") {
    val graphics = new NoopGraphics()
    val original = graphics.gl20
    val profiler = GLProfiler(graphics)
    profiler.enable()
    assert(!(graphics.gl20 eq original))
    profiler.disable()
    assertEquals(profiler.enabled, false)
    assert(graphics.gl20 eq original, "disable() must restore the wrapped GL20 instance")
    // Disabling again is a no-op.
    profiler.disable()
    assertEquals(profiler.enabled, false)
    assert(graphics.gl20 eq original)
  }

  test("every intercepted call increments the total call counter") {
    val graphics = new NoopGraphics()
    val profiler = GLProfiler(graphics)
    profiler.enable()
    val gl = graphics.gl20
    gl.glActiveTexture(0)
    assertEquals(profiler.calls, 1)
    gl.glFinish()
    assertEquals(profiler.calls, 2)
    gl.glFlush()
    assertEquals(profiler.calls, 3)
  }

  test("glBindTexture increments textureBindings and calls") {
    val graphics = new NoopGraphics()
    val profiler = GLProfiler(graphics)
    profiler.enable()
    val gl = graphics.gl20
    gl.glBindTexture(TextureTarget.Texture2D, 7)
    assertEquals(profiler.textureBindings, 1)
    assertEquals(profiler.calls, 1)
    gl.glBindTexture(TextureTarget.Texture2D, 8)
    assertEquals(profiler.textureBindings, 2)
    assertEquals(profiler.calls, 2)
  }

  test("glUseProgram increments shaderSwitches and calls") {
    val graphics = new NoopGraphics()
    val profiler = GLProfiler(graphics)
    profiler.enable()
    val gl = graphics.gl20
    gl.glUseProgram(3)
    assertEquals(profiler.shaderSwitches, 1)
    assertEquals(profiler.calls, 1)
  }

  test("glDrawArrays increments drawCalls and accumulates the vertex count") {
    val graphics = new NoopGraphics()
    val profiler = GLProfiler(graphics)
    profiler.enable()
    val gl = graphics.gl20
    gl.glDrawArrays(PrimitiveMode.Triangles, 0, 6)
    assertEquals(profiler.drawCalls, 1)
    assertEquals(profiler.vertexCount.count, 1)
    assertEquals(profiler.vertexCount.total, 6f)
    assertEquals(profiler.vertexCount.latest, 6f)
    gl.glDrawArrays(PrimitiveMode.Triangles, 0, 3)
    assertEquals(profiler.drawCalls, 2)
    assertEquals(profiler.vertexCount.count, 2)
    assertEquals(profiler.vertexCount.total, 9f)
    assertEquals(profiler.vertexCount.min, 3f)
    assertEquals(profiler.vertexCount.max, 6f)
  }

  test("glDrawElements increments drawCalls and vertex count") {
    val graphics = new NoopGraphics()
    val profiler = GLProfiler(graphics)
    profiler.enable()
    val gl = graphics.gl20
    gl.glDrawElements(PrimitiveMode.Triangles, 12, DataType.UnsignedShort, 0)
    assertEquals(profiler.drawCalls, 1)
    assertEquals(profiler.vertexCount.count, 1)
    assertEquals(profiler.vertexCount.total, 12f)
  }

  test("reset zeroes counters and the vertex FloatCounter") {
    val graphics = new NoopGraphics()
    val profiler = GLProfiler(graphics)
    profiler.enable()
    val gl = graphics.gl20
    gl.glActiveTexture(0)
    gl.glBindTexture(TextureTarget.Texture2D, 1)
    gl.glUseProgram(1)
    gl.glDrawArrays(PrimitiveMode.Triangles, 0, 6)
    assert(profiler.calls > 0)
    profiler.reset()
    assertEquals(profiler.calls, 0)
    assertEquals(profiler.textureBindings, 0)
    assertEquals(profiler.drawCalls, 0)
    assertEquals(profiler.shaderSwitches, 0)
    assertEquals(profiler.vertexCount.count, 0)
    assertEquals(profiler.vertexCount.total, 0f)
    // Counting resumes after reset.
    gl.glActiveTexture(0)
    assertEquals(profiler.calls, 1)
  }

  test("check() drains glGetError and reports every error to the listener") {
    // A GL20 whose glGetError yields a fixed error run, then GL_NO_ERROR, to drive check().
    val errorProfiler = GLProfiler(new NoopGraphics())
    val errorSeq      = scala.collection.mutable.Queue[Int](GL20.GL_INVALID_VALUE, GL20.GL_INVALID_OPERATION)
    val errorDelegate: GL20 = new GL20Interceptor(errorProfiler, NoopGL20) {
      override def glGetError(): Int = if (errorSeq.nonEmpty) errorSeq.dequeue() else GL20.GL_NO_ERROR
    }

    val recorded          = scala.collection.mutable.ListBuffer[Int]()
    val recordingProfiler = GLProfiler(new NoopGraphics())
    recordingProfiler.listener = new GLErrorListener {
      override def onError(error: Int): Unit = recorded += error
    }
    val sut = new GL20Interceptor(recordingProfiler, errorDelegate)

    // glActiveTexture triggers check(): loop glGetError() until GL_NO_ERROR, reporting each.
    sut.glActiveTexture(0)
    assertEquals(recorded.toList, List(GL20.GL_INVALID_VALUE, GL20.GL_INVALID_OPERATION))
  }

  test("check() with no GL error does not invoke the listener") {
    val recorded = scala.collection.mutable.ListBuffer[Int]()
    val profiler = GLProfiler(new NoopGraphics())
    profiler.listener = new GLErrorListener {
      override def onError(error: Int): Unit = recorded += error
    }
    // NoopGL20.glGetError() always returns GL_NO_ERROR.
    val sut = new GL20Interceptor(profiler, NoopGL20)
    sut.glActiveTexture(0)
    assertEquals(recorded.toList, Nil)
  }

  test("GLInterceptor.resolveErrorNumber maps known codes and falls back for unknown ones") {
    assertEquals(GLInterceptor.resolveErrorNumber(GL20.GL_INVALID_VALUE), "GL_INVALID_VALUE")
    assertEquals(GLInterceptor.resolveErrorNumber(GL20.GL_INVALID_OPERATION), "GL_INVALID_OPERATION")
    assertEquals(GLInterceptor.resolveErrorNumber(GL20.GL_INVALID_FRAMEBUFFER_OPERATION), "GL_INVALID_FRAMEBUFFER_OPERATION")
    assertEquals(GLInterceptor.resolveErrorNumber(GL20.GL_INVALID_ENUM), "GL_INVALID_ENUM")
    assertEquals(GLInterceptor.resolveErrorNumber(GL20.GL_OUT_OF_MEMORY), "GL_OUT_OF_MEMORY")
    assertEquals(GLInterceptor.resolveErrorNumber(0x1234), "number 4660")
  }
}
