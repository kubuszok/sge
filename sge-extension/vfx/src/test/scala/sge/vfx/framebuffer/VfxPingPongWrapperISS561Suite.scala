/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Test coverage for ISS-561 (batch F): vfx VfxPingPongWrapper — the src/dst
 * ping-pong buffer pair that drives chained multi-pass rendering. Had ZERO
 * tests.
 *
 * Every expected value is traced from the ORIGINAL Java
 * com/crashinvaders/vfx/framebuffer/VfxPingPongWrapper.java. Cited "java" lines
 * refer to that original; cited "port" lines refer to VfxPingPongWrapper.scala
 * under test:
 *   - the two-arg constructor (java:68-70): VfxPingPongWrapper(bufDst, bufSrc)
 *     calls initialize(bufSrc, bufDst), so arg1 becomes the destination buffer
 *     and arg2 the source buffer. (ISS-692: the original port inverted this; the
 *     assertions below are the red->green proof and now match the Java.)
 *   - initialize(bufSrc, bufDst) setter (java:79-89 / port lines 55-67):
 *     bufSrc = first param, bufDst = second param.
 *   - srcBuffer/dstBuffer (port lines 129/135) return bufSrc/bufDst; srcTexture/
 *     dstTexture (port lines 126/132) return their color textures.
 *   - swap (port lines 108-121): swaps bufDst and bufSrc; the previous dst
 *     becomes the new src and vice versa. (Outside capturing it does no GL.)
 *   - capturing (port line 123): false initially, true between begin()
 *     (port lines 89-96) and end() (port lines 99-105).
 *   - begin twice / end without begin throw IllegalStateException
 *     (port lines 90-92 / 100-101).
 *   - reset (port lines 69-83): when the buffers came from a pool, both are
 *     freed back to it and the wrapper is de-initialized.
 *
 * Headless strategy: a VfxFrameBuffer constructs without GL; only initialize()
 * builds a real FrameBuffer (needs glCheckFramebufferStatus == COMPLETE, from
 * VfxHeadless.CompleteGL20). begin()/end() bind/unbind FBO 0 headlessly (the
 * no-op glGetIntegerv leaves the bound handle at 0, matching the FBO's own
 * handle so end()'s consistency check passes).
 *
 * Mutations these tests catch (canary DoD):
 *   - swap that does not exchange bufSrc/bufDst: the swap test fails because
 *     src/dst (and their textures) stay put.
 *   - begin/end that does not toggle _capturing: the capturing-flag test fails.
 *   - drop the begin-already-capturing / end-not-capturing guard: the throwing
 *     tests stop throwing.
 *   - reset that does not free to the pool: the pool's freeCount stays 0.
 */
package sge
package vfx
package framebuffer

import sge.graphics.Pixmap

class VfxPingPongWrapperISS561Suite extends munit.FunSuite {

  private val Format = Pixmap.Format.RGBA8888

  private def initBuffer()(using Sge): VfxFrameBuffer = {
    val b = new VfxFrameBuffer(Format)
    b.initialize(4, 4)
    b
  }

  test("ISS561/ISS-692: the two-arg constructor maps arg1 -> dstBuffer and arg2 -> srcBuffer (java:68-70)") {
    given Sge  = VfxHeadless.headlessSge()
    val bufDst = new VfxFrameBuffer(Format)
    val bufSrc = new VfxFrameBuffer(Format)
    // Java: VfxPingPongWrapper(bufDst, bufSrc) { initialize(bufSrc, bufDst); }
    // so the first arg is the destination and the second is the source buffer.
    val pp = new VfxPingPongWrapper(bufDst, bufSrc)

    assert(pp.isInitialized, "the wrapper is initialized once both buffers are set (port line 85)")
    assert(!pp.capturing, "capturing is false before begin() (port line 123)")
    assert(pp.dstBuffer eq bufDst, "the first constructor arg is the destination buffer (java:68-70)")
    assert(pp.srcBuffer eq bufSrc, "the second constructor arg is the source buffer (java:68-70)")
  }

  test("ISS561: swap exchanges src and dst (buffers and their textures), and swapping twice restores the original") {
    given Sge  = VfxHeadless.headlessSge(new VfxHeadless.CompleteGL20)
    val bufDst = initBuffer()
    val bufSrc = initBuffer()
    // ctor(bufDst, bufSrc) -> dstBuffer == bufDst, srcBuffer == bufSrc (java:68-70).
    val pp = new VfxPingPongWrapper(bufDst, bufSrc)

    assert(pp.srcTexture eq bufSrc.getFbo.get.colorBufferTexture, "srcTexture follows bufSrc (port line 126)")
    assert(pp.dstTexture eq bufDst.getFbo.get.colorBufferTexture, "dstTexture follows bufDst (port line 132)")

    pp.swap()
    assert(pp.srcBuffer eq bufDst, "after swap the old dst is the new src (port lines 114-116)")
    assert(pp.dstBuffer eq bufSrc, "after swap the old src is the new dst (port lines 114-116)")
    assert(pp.srcTexture eq bufDst.getFbo.get.colorBufferTexture, "srcTexture tracks the swapped src")
    assert(pp.dstTexture eq bufSrc.getFbo.get.colorBufferTexture, "dstTexture tracks the swapped dst")

    pp.swap()
    assert(pp.srcBuffer eq bufSrc, "a second swap restores the original src")
    assert(pp.dstBuffer eq bufDst, "a second swap restores the original dst")
  }

  test("ISS561: begin() enters capturing state and end() leaves it") {
    given Sge = VfxHeadless.headlessSge(new VfxHeadless.CompleteGL20)
    val pp    = new VfxPingPongWrapper(initBuffer(), initBuffer())

    assert(!pp.capturing, "not capturing before begin()")
    pp.begin()
    assert(pp.capturing, "begin() sets capturing true (port line 94)")
    pp.end()
    assert(!pp.capturing, "end() sets capturing false (port line 104)")
  }

  test("ISS561: begin() twice and end() without begin both throw IllegalStateException") {
    given Sge = VfxHeadless.headlessSge(new VfxHeadless.CompleteGL20)

    val notCapturing = new VfxPingPongWrapper(initBuffer(), initBuffer())
    intercept[IllegalStateException](notCapturing.end())

    val capturing = new VfxPingPongWrapper(initBuffer(), initBuffer())
    capturing.begin()
    intercept[IllegalStateException](capturing.begin())
    capturing.end() // restore balanced begin/end
  }

  test("ISS561: reset frees pool-obtained buffers back to the pool and de-initializes the wrapper") {
    given Sge = VfxHeadless.headlessSge(new VfxHeadless.CompleteGL20)
    val pool  = new VfxFrameBufferPool(Format, 4, 4, 0)
    try {
      val pp = new VfxPingPongWrapper(pool)
      assertEquals(pool.freeCount, 0, "both buffers were obtained from the pool (port lines 48-52)")
      assert(pp.isInitialized, "the pool-backed wrapper is initialized")

      pp.reset()
      assertEquals(pool.freeCount, 2, "reset frees both buffers back to the pool (port lines 75-78)")
      assert(!pp.isInitialized, "reset de-initializes the wrapper (port lines 81-82)")
    } finally pool.close()
  }
}
