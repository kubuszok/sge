/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package graphics
package profiling

import sge.noop.{ NoopGL20, NoopGraphics }

/** Additional exact-value behavioral coverage of the GL profiling stack (ISS-725, territory K2), complementing [[GLProfilerCharacterizationSuite]]. Verified against the LibGDX reference semantics of
  * `GLProfiler`, `GL20Interceptor`, `GLInterceptor` and `GLErrorListener`:
  *
  *   - `GLErrorListener.THROWING_LISTENER` raises with the resolved error name; `LOGGING_LISTENER` does not raise
  *   - a swapped custom listener receives every drained error, in order, exactly once per `glGetError` yield
  *   - the `glDrawElements(Buffer)` overload feeds `drawCalls`/`vertexCount` just like the offset overload
  *   - the interceptor's `FloatCounter(0)` (no window) sets `min`/`max`/`latest`/`value` to the count on the first draw and recomputes `average` as `total / count` across draws
  *   - `disable()` freezes the counters at their accumulated values (it does not reset) and stops routing calls
  *   - `GLInterceptor.resolveErrorNumber` falls back to `"number N"` for `GL_NO_ERROR` and unmapped codes
  *
  * As in the sibling suite, calls are driven through the interceptor installed by `enable()` (the production path), so every increment is observed through real code, not by poking private state.
  */
class GLProfilerBehaviorIss725Suite extends munit.FunSuite {

  test("THROWING_LISTENER raises RuntimeException carrying the resolved error name") {
    val ex = intercept[RuntimeException] {
      GLErrorListener.THROWING_LISTENER.onError(GL20.GL_INVALID_ENUM)
    }
    assertEquals(ex.getMessage, "GLProfiler: Got GL error GL_INVALID_ENUM")
  }

  test("LOGGING_LISTENER.onError does not raise") {
    // The default listener logs rather than throwing; exercising it must complete normally.
    GLErrorListener.LOGGING_LISTENER.onError(GL20.GL_OUT_OF_MEMORY)
  }

  test("a swapped custom listener drains every glGetError in order, exactly once each") {
    // A GL20 delegate whose glGetError yields a fixed error run then GL_NO_ERROR to drive check().
    val errorSeq = scala.collection.mutable.Queue[Int](
      GL20.GL_INVALID_ENUM,
      GL20.GL_INVALID_VALUE,
      GL20.GL_OUT_OF_MEMORY
    )
    val profiler = GLProfiler(new NoopGraphics())
    val delegate: GL20 = new GL20Interceptor(profiler, NoopGL20) {
      override def glGetError(): Int = if (errorSeq.nonEmpty) errorSeq.dequeue() else GL20.GL_NO_ERROR
    }

    val recorded         = scala.collection.mutable.ListBuffer[Int]()
    val listenerProfiler = GLProfiler(new NoopGraphics())
    listenerProfiler.listener = new GLErrorListener {
      override def onError(error: Int): Unit = recorded += error
    }
    val sut = new GL20Interceptor(listenerProfiler, delegate)

    // A single intercepted call must drain the full error run through check().
    sut.glFinish()
    assertEquals(
      recorded.toList,
      List(GL20.GL_INVALID_ENUM, GL20.GL_INVALID_VALUE, GL20.GL_OUT_OF_MEMORY)
    )
    // The intercepted glFinish itself counts exactly once.
    assertEquals(sut.calls, 1)
  }

  test("glDrawElements(Buffer overload) increments drawCalls and accumulates the vertex count") {
    val graphics = new NoopGraphics()
    val profiler = GLProfiler(graphics)
    profiler.enable()
    val gl      = graphics.gl20
    val indices = java.nio.ByteBuffer.allocate(16)
    gl.glDrawElements(PrimitiveMode.Triangles, 9, DataType.UnsignedShort, indices)
    assertEquals(profiler.drawCalls, 1)
    assertEquals(profiler.calls, 1)
    assertEquals(profiler.vertexCount.count, 1)
    assertEquals(profiler.vertexCount.total, 9f)
    assertEquals(profiler.vertexCount.latest, 9f)
  }

  test("first draw sets vertex min/max/latest/value to the count and average to the same value") {
    val graphics = new NoopGraphics()
    val profiler = GLProfiler(graphics)
    profiler.enable()
    val gl = graphics.gl20
    gl.glDrawArrays(PrimitiveMode.Triangles, 0, 15)
    val vc = profiler.vertexCount
    // FloatCounter(0) has no window, so value follows latest and min/max update on the first put.
    assertEquals(vc.latest, 15f)
    assertEquals(vc.value, 15f)
    assertEquals(vc.min, 15f)
    assertEquals(vc.max, 15f)
    assertEquals(vc.average, 15f)
    assertEquals(vc.total, 15f)
  }

  test("average is recomputed as total/count across a sequence of draws") {
    val graphics = new NoopGraphics()
    val profiler = GLProfiler(graphics)
    profiler.enable()
    val gl = graphics.gl20
    gl.glDrawArrays(PrimitiveMode.Triangles, 0, 10)
    gl.glDrawArrays(PrimitiveMode.Triangles, 0, 20)
    gl.glDrawArrays(PrimitiveMode.Triangles, 0, 30)
    val vc = profiler.vertexCount
    assertEquals(vc.count, 3)
    assertEquals(vc.total, 60f)
    assertEquals(vc.average, 20f)
    assertEquals(vc.min, 10f)
    assertEquals(vc.max, 30f)
    assertEquals(vc.latest, 30f)
  }

  test("a mixed frame produces exact totals for every counter simultaneously") {
    val graphics = new NoopGraphics()
    val profiler = GLProfiler(graphics)
    profiler.enable()
    val gl = graphics.gl20
    // 2 texture bindings, 1 shader switch, 2 draws, plus 3 plain calls.
    gl.glBindTexture(TextureTarget.Texture2D, 1) // call, textureBinding
    gl.glBindTexture(TextureTarget.Texture2D, 2) // call, textureBinding
    gl.glUseProgram(5) // call, shaderSwitch
    gl.glActiveTexture(0) // call
    gl.glEnable(EnableCap.Blend) // call
    gl.glDrawArrays(PrimitiveMode.Triangles, 0, 6) // call, drawCall, 6 verts
    gl.glDrawElements(PrimitiveMode.Triangles, 12, DataType.UnsignedShort, 0) // call, drawCall, 12 verts
    gl.glFinish() // call

    assertEquals(profiler.calls, 8)
    assertEquals(profiler.textureBindings, 2)
    assertEquals(profiler.shaderSwitches, 1)
    assertEquals(profiler.drawCalls, 2)
    assertEquals(profiler.vertexCount.count, 2)
    assertEquals(profiler.vertexCount.total, 18f)
    assertEquals(profiler.vertexCount.min, 6f)
    assertEquals(profiler.vertexCount.max, 12f)
  }

  test("disable freezes accumulated counters and stops routing further calls") {
    val graphics = new NoopGraphics()
    val original = graphics.gl20
    val profiler = GLProfiler(graphics)
    profiler.enable()
    val gl = graphics.gl20
    gl.glActiveTexture(0)
    gl.glBindTexture(TextureTarget.Texture2D, 1)
    gl.glDrawArrays(PrimitiveMode.Triangles, 0, 6)
    assertEquals(profiler.calls, 3)

    profiler.disable()
    // disable() restores the raw GL20 and must NOT reset the accumulated statistics.
    assert(graphics.gl20 eq original)
    assertEquals(profiler.calls, 3)
    assertEquals(profiler.textureBindings, 1)
    assertEquals(profiler.drawCalls, 1)
    assertEquals(profiler.vertexCount.total, 6f)

    // Calls issued against the restored raw GL20 bypass the interceptor entirely.
    graphics.gl20.glActiveTexture(0)
    assertEquals(profiler.calls, 3)
  }

  test("resolveErrorNumber falls back to \"number N\" for GL_NO_ERROR and unmapped codes") {
    assertEquals(GLInterceptor.resolveErrorNumber(GL20.GL_NO_ERROR), "number 0")
    assertEquals(GLInterceptor.resolveErrorNumber(GL20.GL_INVALID_FRAMEBUFFER_OPERATION), "GL_INVALID_FRAMEBUFFER_OPERATION")
    assertEquals(GLInterceptor.resolveErrorNumber(-7), "number -7")
  }
}
