/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package graphics
package profiling

import lowlevel.Nullable
import sge.noop.{ NoopGL32, NoopGraphics }

/** Exact-value behavioral coverage of the GL30/GL31/GL32 profiling interceptors (ISS-863, from the ISS-725 K2 audit), complementing [[GLProfilerCharacterizationSuite]] and
  * [[GLProfilerBehaviorIss725Suite]] which only reach the `GL20Interceptor` (`NoopGraphics` exposes no GL30+).
  *
  * `GLProfiler` selects the interceptor for the highest GL level the `Graphics` advertises, so these tests feed a [[NoopGL32]] fake — either as a directly constructed interceptor's delegate, or as
  * the `gl30`/`gl31`/`gl32` of a fake `Graphics` — to route calls through the GL30+ interceptors. Verified against the LibGDX reference semantics of
  * `GL30Interceptor`/`GL31Interceptor`/`GL32Interceptor`:
  *
  *   - GL30 adds `glDrawArraysInstanced`, `glDrawElementsInstanced` and both `glDrawRangeElements` overloads: each bumps `drawCalls` and feeds `count` into the vertex `FloatCounter`
  *   - GL30 `glDrawBuffers` bumps `drawCalls` but does NOT touch the vertex count (it selects render targets, it does not draw geometry)
  *   - the shared GL20 draw paths (`glDrawArrays`/`glDrawElements`) still count when routed through a GL30+ interceptor
  *   - GL31 adds `glDrawArraysIndirect`/`glDrawElementsIndirect`: `drawCalls` only, no vertex count (the vertex count lives in a GPU-side indirect buffer)
  *   - GL32 adds the base-vertex family (`glDrawElementsBaseVertex`, `glDrawRangeElementsBaseVertex`, both `glDrawElementsInstancedBaseVertex` overloads): each bumps `drawCalls` and feeds `count`
  */
class GLProfilerGL30BehaviorIss863Suite extends munit.FunSuite {

  /** A headless `Graphics` that advertises a GL30 (so `GLProfiler` selects the `GL30Interceptor`) while remaining GL31/GL32-free. */
  final private class GL30Graphics(initial: GL30) extends NoopGraphics {
    private var _gl30v:               GL30           = initial
    override def gl30Available:       Boolean        = true
    override def gl30:                Nullable[GL30] = Nullable(_gl30v)
    override def gl30_=(value: GL30): Unit           = _gl30v = value
  }

  /** A headless `Graphics` that advertises a GL31 (so `GLProfiler` selects the `GL31Interceptor`). */
  final private class GL31Graphics(initial: GL31) extends NoopGraphics {
    private var _gl31v:               GL31           = initial
    override def gl30Available:       Boolean        = true
    override def gl31Available:       Boolean        = true
    override def gl30:                Nullable[GL30] = Nullable(_gl31v)
    override def gl31:                Nullable[GL31] = Nullable(_gl31v)
    override def gl30_=(value: GL30): Unit           = ()
    override def gl31_=(value: GL31): Unit           = _gl31v = value
  }

  /** A headless `Graphics` that advertises a GL32 (so `GLProfiler` selects the `GL32Interceptor`). */
  final private class GL32Graphics(initial: GL32) extends NoopGraphics {
    private var _gl32v:               GL32           = initial
    override def gl30Available:       Boolean        = true
    override def gl31Available:       Boolean        = true
    override def gl32Available:       Boolean        = true
    override def gl30:                Nullable[GL30] = Nullable(_gl32v)
    override def gl31:                Nullable[GL31] = Nullable(_gl32v)
    override def gl32:                Nullable[GL32] = Nullable(_gl32v)
    override def gl30_=(value: GL30): Unit           = ()
    override def gl31_=(value: GL31): Unit           = ()
    override def gl32_=(value: GL32): Unit           = _gl32v = value
  }

  private def indices16 = java.nio.ByteBuffer.allocate(16)
  private def bufs      = java.nio.IntBuffer.allocate(4)

  // ---- interceptor selection ----

  test("GLProfiler selects GL30Interceptor when the Graphics advertises gl30") {
    val graphics = new GL30Graphics(new NoopGL32)
    val profiler = GLProfiler(graphics)
    profiler.enable()
    val installed = graphics.gl30.get
    assert(installed.isInstanceOf[GL30Interceptor], s"expected GL30Interceptor, got ${installed.getClass.getName}")
    assert(!installed.isInstanceOf[GL31Interceptor], "GL30Interceptor must not be a GL31Interceptor")
    // The interceptor is installed as gl20 too, so the plain GL20 path routes through it.
    assert(graphics.gl20.isInstanceOf[GL30Interceptor])
  }

  test("GLProfiler selects GL31Interceptor when the Graphics advertises gl31") {
    val graphics = new GL31Graphics(new NoopGL32)
    val profiler = GLProfiler(graphics)
    profiler.enable()
    val installed = graphics.gl31.get
    assert(installed.isInstanceOf[GL31Interceptor], s"expected GL31Interceptor, got ${installed.getClass.getName}")
    assert(!installed.isInstanceOf[GL32Interceptor], "GL31Interceptor must not be a GL32Interceptor")
  }

  test("GLProfiler selects GL32Interceptor when the Graphics advertises gl32") {
    val graphics = new GL32Graphics(new NoopGL32)
    val profiler = GLProfiler(graphics)
    profiler.enable()
    val installed = graphics.gl32.get
    assert(installed.isInstanceOf[GL32Interceptor], s"expected GL32Interceptor, got ${installed.getClass.getName}")
  }

  // ---- GL30 unique draw paths ----

  private def gl30Sut: GL30Interceptor = new GL30Interceptor(GLProfiler(new NoopGraphics()), new NoopGL32)

  test("glDrawArraysInstanced bumps drawCalls and feeds the vertex count") {
    val sut = gl30Sut
    sut.glDrawArraysInstanced(PrimitiveMode.Triangles, 0, 12, 4)
    assertEquals(sut.drawCalls, 1)
    assertEquals(sut.calls, 1)
    assertEquals(sut.vertexCount.count, 1)
    assertEquals(sut.vertexCount.total, 12f)
    assertEquals(sut.vertexCount.latest, 12f)
  }

  test("glDrawElementsInstanced bumps drawCalls and feeds the vertex count") {
    val sut = gl30Sut
    sut.glDrawElementsInstanced(PrimitiveMode.Triangles, 9, DataType.UnsignedShort, 0, 3)
    assertEquals(sut.drawCalls, 1)
    assertEquals(sut.vertexCount.count, 1)
    assertEquals(sut.vertexCount.total, 9f)
  }

  test("both glDrawRangeElements overloads bump drawCalls and feed the vertex count") {
    val sut = gl30Sut
    sut.glDrawRangeElements(PrimitiveMode.Triangles, 0, 100, 15, DataType.UnsignedShort, indices16)
    sut.glDrawRangeElements(PrimitiveMode.Triangles, 0, 100, 21, DataType.UnsignedShort, 0)
    assertEquals(sut.drawCalls, 2)
    assertEquals(sut.vertexCount.count, 2)
    assertEquals(sut.vertexCount.total, 36f)
    assertEquals(sut.vertexCount.min, 15f)
    assertEquals(sut.vertexCount.max, 21f)
  }

  test("glDrawBuffers bumps drawCalls but NOT the vertex count") {
    val sut = gl30Sut
    sut.glDrawBuffers(2, bufs)
    assertEquals(sut.drawCalls, 1)
    assertEquals(sut.calls, 1)
    assertEquals(sut.vertexCount.count, 0)
    assertEquals(sut.vertexCount.total, 0f)
  }

  test("a mixed GL30 frame produces exact totals for every counter simultaneously") {
    val sut = gl30Sut
    // Shared GL20 paths: 1 bind, 1 shader switch, 1 draw-arrays (6 verts), 1 draw-elements (12 verts).
    sut.glBindTexture(TextureTarget.Texture2D, 1) // call, textureBinding
    sut.glUseProgram(3) // call, shaderSwitch
    sut.glDrawArrays(PrimitiveMode.Triangles, 0, 6) // call, drawCall, 6 verts
    sut.glDrawElements(PrimitiveMode.Triangles, 12, DataType.UnsignedShort, 0) // call, drawCall, 12 verts
    // GL30 paths: instanced (10 verts), range (8 verts), and a draw-buffers (no verts).
    sut.glDrawArraysInstanced(PrimitiveMode.Triangles, 0, 10, 2) // call, drawCall, 10 verts
    sut.glDrawRangeElements(PrimitiveMode.Triangles, 0, 50, 8, DataType.UnsignedShort, 0) // call, drawCall, 8 verts
    sut.glDrawBuffers(1, bufs) // call, drawCall, 0 verts

    assertEquals(sut.calls, 7)
    assertEquals(sut.textureBindings, 1)
    assertEquals(sut.shaderSwitches, 1)
    assertEquals(sut.drawCalls, 5)
    // Only the 4 geometry draws feed the vertex counter; glDrawBuffers does not.
    assertEquals(sut.vertexCount.count, 4)
    assertEquals(sut.vertexCount.total, 36f)
    assertEquals(sut.vertexCount.min, 6f)
    assertEquals(sut.vertexCount.max, 12f)
    assertEquals(sut.vertexCount.average, 9f)
  }

  test("the GL30 mixed frame counts identically through the enable() production path") {
    val graphics = new GL30Graphics(new NoopGL32)
    val profiler = GLProfiler(graphics)
    profiler.enable()
    val gl = graphics.gl30.get
    gl.glDrawArraysInstanced(PrimitiveMode.Triangles, 0, 10, 2)
    gl.glDrawBuffers(1, bufs)
    assertEquals(profiler.drawCalls, 2)
    assertEquals(profiler.vertexCount.count, 1)
    assertEquals(profiler.vertexCount.total, 10f)
  }

  // ---- GL31 unique draw paths ----

  private def gl31Sut: GL31Interceptor = new GL31Interceptor(GLProfiler(new NoopGraphics()), new NoopGL32)

  test("GL31 indirect draws bump drawCalls only, never the vertex count") {
    val sut = gl31Sut
    sut.glDrawArraysIndirect(PrimitiveMode.Triangles, 0L)
    sut.glDrawElementsIndirect(PrimitiveMode.Triangles, DataType.UnsignedShort, 0L)
    assertEquals(sut.drawCalls, 2)
    assertEquals(sut.calls, 2)
    assertEquals(sut.vertexCount.count, 0)
    assertEquals(sut.vertexCount.total, 0f)
  }

  test("GL31Interceptor still counts the inherited GL30 instanced draw path") {
    val sut = gl31Sut
    sut.glDrawArraysInstanced(PrimitiveMode.Triangles, 0, 14, 2)
    sut.glDrawElementsIndirect(PrimitiveMode.Triangles, DataType.UnsignedShort, 0L)
    assertEquals(sut.drawCalls, 2)
    // Only the instanced draw feeds vertices; the indirect draw does not.
    assertEquals(sut.vertexCount.count, 1)
    assertEquals(sut.vertexCount.total, 14f)
  }

  // ---- GL32 unique draw paths ----

  private def gl32Sut: GL32Interceptor = new GL32Interceptor(GLProfiler(new NoopGraphics()), new NoopGL32)

  test("GL32 base-vertex draws each bump drawCalls and feed the vertex count") {
    val sut = gl32Sut
    sut.glDrawElementsBaseVertex(PrimitiveMode.Triangles, 6, DataType.UnsignedShort, indices16, 0) // 6 verts
    sut.glDrawRangeElementsBaseVertex(PrimitiveMode.Triangles, 0, 50, 9, DataType.UnsignedShort, indices16, 0) // 9 verts
    sut.glDrawElementsInstancedBaseVertex(PrimitiveMode.Triangles, 12, DataType.UnsignedShort, indices16, 3, 0) // 12 verts
    sut.glDrawElementsInstancedBaseVertex(PrimitiveMode.Triangles, 15, DataType.UnsignedShort, 0, 3, 0) // 15 verts
    assertEquals(sut.drawCalls, 4)
    assertEquals(sut.calls, 4)
    assertEquals(sut.vertexCount.count, 4)
    assertEquals(sut.vertexCount.total, 42f)
    assertEquals(sut.vertexCount.min, 6f)
    assertEquals(sut.vertexCount.max, 15f)
  }

  test("GL32Interceptor accumulates GL20/GL30/GL31/GL32 draw paths together") {
    val sut = gl32Sut
    sut.glDrawArrays(PrimitiveMode.Triangles, 0, 3) // GL20: 3 verts
    sut.glDrawArraysInstanced(PrimitiveMode.Triangles, 0, 6, 2) // GL30: 6 verts
    sut.glDrawArraysIndirect(PrimitiveMode.Triangles, 0L) // GL31: 0 verts
    sut.glDrawElementsBaseVertex(PrimitiveMode.Triangles, 9, DataType.UnsignedShort, indices16, 0) // GL32: 9 verts
    assertEquals(sut.drawCalls, 4)
    assertEquals(sut.vertexCount.count, 3)
    assertEquals(sut.vertexCount.total, 18f)
  }
}
