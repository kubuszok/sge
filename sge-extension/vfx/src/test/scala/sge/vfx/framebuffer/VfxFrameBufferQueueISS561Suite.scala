/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Test coverage for ISS-561 (batch F): vfx VfxFrameBufferQueue — the
 * round-robin buffer rotation used by multi-pass effects. Had ZERO tests.
 *
 * Every expected value is hand-traced from the faithful full-port
 * VfxFrameBufferQueue.scala (covenant: full-port, source reference
 * com/crashinvaders/vfx/framebuffer/VfxFrameBufferQueue.java). Cited "port"
 * line numbers refer to VfxFrameBufferQueue.scala under test:
 *   - require(fboAmount >= 1) (port line 25): a non-positive amount is rejected.
 *   - the constructor (port lines 27-35) builds exactly fboAmount buffers.
 *   - current (port line 76) is buffers(currentIdx); currentIdx starts at 0
 *     (port line 37) so the first current is buffer 0.
 *   - changeToNext (port lines 78-81): currentIdx = (currentIdx + 1) % size,
 *     then returns current — i.e. cyclic with modulo wrap.
 *   - close (port lines 44-51): closes every buffer then clears the list.
 *   - setTextureParams (port lines 83-89): stores wrap/filter then rebind()
 *     (port lines 62-74), which pushes those params onto each initialized
 *     buffer's color texture.
 *
 * Headless strategy: a VfxFrameBuffer is created without touching GL (its
 * constructor only allocates matrices); only resize()/initialize() build a real
 * FrameBuffer, which needs glCheckFramebufferStatus == COMPLETE — supplied by
 * VfxHeadless.CompleteGL20. Tests that only exercise the rotation arithmetic
 * use the pure no-op GL.
 *
 * Mutations these tests catch (canary DoD):
 *   - drop the require (port line 25): the zero-amount test stops throwing.
 *   - changeToNext without the modulo (currentIdx += 1): the wrap test throws
 *     IndexOutOfBounds instead of cycling back to buffer 0.
 *   - close without buffers.clear() (port line 50): post-close changeToNext no
 *     longer throws (no divide-by-zero).
 *   - close without closing each buffer (port lines 46-49): the collected
 *     buffers remain initialized.
 *   - setTextureParams without rebind() (port line 88): the texture params stay
 *     at the post-resize defaults instead of the new ones.
 */
package sge
package vfx
package framebuffer

import sge.graphics.{ Pixmap, Texture }

class VfxFrameBufferQueueISS561Suite extends munit.FunSuite {

  private val Format = Pixmap.Format.RGBA8888

  /** Collects all distinct buffers a queue exposes by walking current + changeToNext, which also pins that the queue holds exactly `expected` buffers before wrapping. */
  private def collect(queue: VfxFrameBufferQueue, expected: Int): Vector[VfxFrameBuffer] = {
    val first   = queue.current
    val builder = Vector.newBuilder[VfxFrameBuffer]
    builder += first
    var i = 1
    while (i < expected) {
      builder += queue.changeToNext()
      i += 1
    }
    builder.result()
  }

  test("ISS561: constructing with fboAmount < 1 is rejected") {
    given Sge = VfxHeadless.headlessSge()
    intercept[IllegalArgumentException](new VfxFrameBufferQueue(Format, 0))
    intercept[IllegalArgumentException](new VfxFrameBufferQueue(Format, -1))
  }

  test(
    "ISS561: the queue holds exactly fboAmount buffers, current starts at buffer 0, and changeToNext cycles with modulo wrap"
  ) {
    given Sge = VfxHeadless.headlessSge()
    val queue = new VfxFrameBufferQueue(Format, 3)
    val b0    = queue.current
    assert(queue.current eq b0, "current is stable until changeToNext is called (port line 76)")

    val b1 = queue.changeToNext()
    val b2 = queue.changeToNext()
    val b3 = queue.changeToNext()

    assert(b0 ne b1, "buffer 0 and buffer 1 are distinct instances")
    assert(b1 ne b2, "buffer 1 and buffer 2 are distinct instances")
    assert(
      b3 eq b0,
      "the third changeToNext on a 3-buffer queue wraps back to buffer 0 via the modulo (port line 79); without the modulo this would be a fourth, out-of-bounds buffer"
    )
  }

  test("ISS561: a single-buffer queue always returns the same buffer") {
    given Sge = VfxHeadless.headlessSge()
    val queue = new VfxFrameBufferQueue(Format, 1)
    val b0    = queue.current
    assert(queue.changeToNext() eq b0, "(0 + 1) % 1 == 0, so changeToNext stays on buffer 0")
    assert(queue.changeToNext() eq b0, "and remains there on every subsequent call")
  }

  test("ISS561: close() closes every buffer and clears the queue") {
    given Sge = VfxHeadless.headlessSge(new VfxHeadless.CompleteGL20)
    val queue = new VfxFrameBufferQueue(Format, 3)
    queue.resize(4, 4)
    val buffers = collect(queue, 3)
    assert(buffers.forall(_.initialized), "after resize every buffer is initialized (precondition)")

    queue.close()

    assert(buffers.forall(!_.initialized), "close() must close (reset) every buffer (port lines 46-49)")
    intercept[ArithmeticException](queue.changeToNext())
    // After buffers.clear() (port line 50) the queue is empty, so
    // changeToNext's `% buffers.size` divides by zero. A close() that skipped
    // the clear would NOT throw here.
  }

  test("ISS561: setTextureParams stores the params and rebinds them onto every initialized buffer's texture") {
    given Sge = VfxHeadless.headlessSge(new VfxHeadless.CompleteGL20)
    val queue = new VfxFrameBufferQueue(Format, 2)
    queue.resize(4, 4)

    // Defaults after resize are ClampToEdge wrap + Nearest filter; pick values
    // that differ on every axis so a missing rebind() is observable.
    queue.setTextureParams(
      Texture.TextureWrap.Repeat,
      Texture.TextureWrap.MirroredRepeat,
      Texture.TextureFilter.Linear,
      Texture.TextureFilter.Linear
    )

    val buffers = collect(queue, 2)
    buffers.foreach { buffer =>
      val tex = buffer.getFbo.get.colorBufferTexture
      assertEquals(tex.uWrap, Texture.TextureWrap.Repeat, "rebind must push wrapU onto the texture (port lines 69, 88)")
      assertEquals(tex.vWrap, Texture.TextureWrap.MirroredRepeat, "rebind must push wrapV onto the texture (port line 69)")
      assertEquals(tex.minFilter, Texture.TextureFilter.Linear, "rebind must push filterMin onto the texture (port line 70)")
      assertEquals(tex.magFilter, Texture.TextureFilter.Linear, "rebind must push filterMag onto the texture (port line 70)")
    }

    queue.close()
  }
}
