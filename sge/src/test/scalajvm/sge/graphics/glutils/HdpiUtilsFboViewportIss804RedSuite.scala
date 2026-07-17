/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Assertion-first pin — ISS-804 (adjudicated FAITHFUL, wave 2026-07-17-F,
 * territory X; behavioral follow-up split to ISS-840). Reproducer-authored;
 * converted from design-pin to assertion-first at train assembly (orchestrator,
 * ISS-828 precedent): the pin asserts the CURRENT faithful values and
 * hard-fails when the ISS-840 FBO-aware API lands — then flip per the comment.
 *
 * MECHANISM. HdpiUtils.glViewport (HdpiUtils.scala:68-76) is a faithful,
 * verbatim port of com/badlogic/gdx/graphics/glutils/HdpiUtils.java:62-69: in
 * Logical mode, whenever the logical width/height differ from the backbuffer
 * width/height (a Retina/HiDPI backbuffer), it scales logical coordinates UP to
 * the screen backbuffer via toBackBufferX/Y (ratio backBufferWidth/width).
 *
 * The problem (surfaced by the gauntlet ViewportLetterboxProbe, which worked
 * around it with a raw glViewport) is that HdpiUtils has NO knowledge of the
 * currently-bound render target. LibGDX's own FrameBuffer never routes through
 * HdpiUtils: GLFrameBuffer.begin() -> setFrameBufferViewport() calls
 *   Gdx.gl20.glViewport(0, 0, bufferBuilder.width, bufferBuilder.height)
 * (GLFrameBuffer.java:379-387) with the FBO's PIXEL dimensions directly. When
 * application code instead calls HdpiUtils.glViewport(...) while an FBO is
 * bound, HdpiUtils still scales by the SCREEN backbuffer ratio, double-scaling
 * the viewport past the FBO's bounds. LibGDX's only documented remedy is
 * HdpiUtils.setMode(HdpiMode.Pixels) around FBO rendering (setMode docstring).
 *
 * ADJUDICATION (wave-F): the double-scale IS faithful LibGDX behavior — gdx has
 * the identical limitation and the identical remedy (setMode(HdpiMode.Pixels)
 * or raw glViewport). The port stays 1:1; the desired FBO-correct behavior
 * below requires NEW API surface and is tracked as ISS-840 (improvement).
 *
 * SPEC for ISS-840 (fix requires new FBO-aware API surface — beyond a faithful 1:1 port):
 * HdpiUtils.glViewport/glScissor must convert relative to the ACTIVE render
 * target's pixel size, not unconditionally the screen backbuffer. Concretely,
 * either (a) HdpiUtils tracks the currently-bound FBO's pixel dims (set by
 * FrameBuffer.begin(), cleared by end()) and uses those as the "backbuffer"
 * for conversion, or (b) Graphics exposes the active target size and HdpiUtils
 * reads it. When no FBO is bound, behavior is unchanged. This test pins the
 * CURRENT (faithful) values until that API lands (close ISS-840, flip the pin).
 */
package sge
package graphics
package glutils

import sge.noop.{ NoopGL20, NoopGraphics }

class HdpiUtilsFboViewportIss804RedSuite extends munit.FunSuite {

  /** Records the last glViewport(x, y, w, h) call as raw ints. */
  final private class RecordingGL20 extends GL20 {
    var last:               Option[(Int, Int, Int, Int)] = None
    private val underlying: GL20                         = NoopGL20
    export underlying.{ glViewport as _, * }
    override def glViewport(x: Pixels, y: Pixels, width: Pixels, height: Pixels): Unit =
      last = Some((x.toInt, y.toInt, width.toInt, height.toInt))
  }

  /** Screen graphics: LOGICAL 400x300, Retina backbuffer 800x600 (scale 2). */
  private def hdpiSge(recorder: RecordingGL20): Sge =
    SgeTestFixture.testSge(
      graphics = new NoopGraphics(noopWidth = 400, noopHeight = 300) {
        override def gl20:             GL20   = recorder
        override def backBufferWidth:  Pixels = Pixels(800)
        override def backBufferHeight: Pixels = Pixels(600)
      }
    )

  test(
    "ISS-804 (assertion-first pin): HdpiUtils.glViewport is NOT FBO-aware — faithful window-backbuffer conversion until ISS-840 lands"
  ) {
    val recorder = new RecordingGL20
    given Sge    = hdpiSge(recorder)

    // Simulated FBO-bound state: a letterbox FBO whose pixel size equals the
    // LOGICAL screen size (400x300). Rendering into it, the app sets a full-FBO
    // viewport in logical coordinates.
    val fboWidth  = 400
    val fboHeight = 300

    HdpiUtils.setMode(HdpiMode.Logical)
    try {
      HdpiUtils.glViewport(Pixels(0), Pixels(0), Pixels(fboWidth), Pixels(fboHeight))

      val recorded = recorder.last.getOrElse(fail("HdpiUtils.glViewport did not issue a glViewport call"))

      // FAITHFUL current behavior (ISS-804 adjudicated wave-F): HdpiUtils converts
      // logical -> WINDOW backbuffer unconditionally, exactly like LibGDX
      // (HdpiUtils.java; gdx's own GLFrameBuffer.begin bypasses HdpiUtils with a
      // raw pixel glViewport, GLFrameBuffer.java:379-387). So an FBO-bound caller
      // gets toBackBufferX(400) = 800, toBackBufferY(300) = 600 -> (0,0,800,600),
      // overflowing the 400x300 FBO — the documented remedy is
      // setMode(HdpiMode.Pixels) or a raw glViewport. This pin asserts the
      // faithful value; when the FBO-aware API lands, close ISS-840 and flip
      // this assertion to (0, 0, fboWidth, fboHeight) = (0,0,400,300).
      assertEquals(
        recorded,
        (0, 0, 800, 600),
        "HdpiUtils.glViewport unexpectedly stopped double-scaling to the window backbuffer: has FBO-awareness " +
          "shipped? Close ISS-840 and flip this assertion to (0,0,400,300) (see SPEC in the file header)."
      )
    } finally
      HdpiUtils.setMode(HdpiMode.Logical) // restore default; avoid cross-test pollution
  }

  test("ISS-804 (control): with no HiDPI scaling (backbuffer == logical) glViewport passes through unscaled") {
    // Positive control: when width == backBufferWidth, HdpiUtils takes the
    // else-branch (HdpiUtils.scala:74-75) and emits logical coords verbatim.
    val recorder = new RecordingGL20
    given Sge    = SgeTestFixture.testSge(graphics = new NoopGraphics(noopWidth = 400, noopHeight = 300) {
      override def gl20: GL20 = recorder
    })
    HdpiUtils.setMode(HdpiMode.Logical)
    try {
      HdpiUtils.glViewport(Pixels(0), Pixels(0), Pixels(400), Pixels(300))
      assertEquals(recorder.last, Some((0, 0, 400, 300)), "no HiDPI scaling -> pass-through viewport")
    } finally
      HdpiUtils.setMode(HdpiMode.Logical)
  }
}
