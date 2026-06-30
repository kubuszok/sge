/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Test coverage for ISS-561 (batch F): vfx VfxFrameBufferPool — the obtain/free
 * buffer pool that effect chains lease scratch buffers from. Had ZERO tests.
 *
 * Every expected value is hand-traced from the port VfxFrameBufferPool.scala
 * (source reference com/crashinvaders/vfx/framebuffer/VfxFrameBufferPool.java).
 * Cited "port" line numbers refer to VfxFrameBufferPool.scala under test:
 *   - obtain (port lines 76-80): if disposed -> throw; if the free list is empty
 *     createBuffer() (port lines 115-120: new buffer, initialize to pool size,
 *     register in managedBuffers), else pop the last free buffer (LIFO reuse).
 *   - free (port lines 86-98): if disposed -> throw; if the buffer is still
 *     valid push it onto the free list, update freePeak, resetBuffer it
 *     (port lines 123-130: reset its texture wrap/filter to the pool defaults).
 *   - freeCount (port line 113) = freeBuffers.size; freePeak (port line 34) =
 *     the running maximum free-list size.
 *
 * Headless strategy: createBuffer() builds a real FrameBuffer, which needs
 * glCheckFramebufferStatus == COMPLETE — supplied by VfxHeadless.CompleteGL20.
 *
 * Mutations these tests catch (canary DoD):
 *   - obtain ignores the free list (always createBuffer): the reuse test fails
 *     because obtain returns a fresh instance, not the freed one.
 *   - free does not add to freeBuffers (or omits the freePeak update): the
 *     freeCount/freePeak assertions fail.
 *   - free omits resetBuffer (port line 96): the freed buffer keeps the mutated
 *     texture params instead of the pool defaults.
 *   - drop the disposed guard (port lines 77/87): obtain/free on a closed pool
 *     no longer throw.
 */
package sge
package vfx
package framebuffer

import sge.graphics.{ Pixmap, Texture }

class VfxFrameBufferPoolISS561Suite extends munit.FunSuite {

  private val Format = Pixmap.Format.RGBA8888
  private val Size   = 4

  private def newPool()(using Sge): VfxFrameBufferPool =
    new VfxFrameBufferPool(Format, Size, Size, 0)

  test("ISS561: obtain on an empty pool creates a fresh, initialized buffer sized to the pool; obtains are distinct") {
    given Sge = VfxHeadless.headlessSge(new VfxHeadless.CompleteGL20)
    val pool  = newPool()
    try {
      assertEquals(pool.freeCount, 0, "a new pool has no free buffers")

      val a = pool.obtain()
      assert(a.initialized, "obtain createBuffer initializes the buffer (port line 117)")
      assertEquals(a.getFbo.get.width.toInt, Size, "the buffer is sized to the pool width (port line 117)")
      assertEquals(pool.freeCount, 0, "obtain does not touch the free list when it is empty")

      val b = pool.obtain()
      assert(a ne b, "two obtains with no intervening free yield distinct buffers (each createBuffer)")
    } finally pool.close()
  }

  test("ISS561: free returns a buffer to the pool and the next obtain reuses it") {
    given Sge = VfxHeadless.headlessSge(new VfxHeadless.CompleteGL20)
    val pool  = newPool()
    try {
      val a = pool.obtain()
      pool.free(a)
      assertEquals(pool.freeCount, 1, "free pushes the buffer onto the free list (port line 94)")
      assertEquals(pool.freePeak, 1, "freePeak records the new free-list maximum (port line 95)")

      val reused = pool.obtain()
      assert(reused eq a, "obtain must reuse the freed buffer, not create a new one (port line 79)")
      assertEquals(pool.freeCount, 0, "the reused buffer leaves the free list")
    } finally pool.close()
  }

  test("ISS561: freeing resets the buffer's texture params back to the pool defaults") {
    given Sge = VfxHeadless.headlessSge(new VfxHeadless.CompleteGL20)
    val pool  = newPool()
    try {
      val a   = pool.obtain()
      val tex = a.getFbo.get.colorBufferTexture
      // Drive the texture away from the pool defaults (ClampToEdge / Nearest).
      tex.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
      tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

      pool.free(a)
      val reused    = pool.obtain()
      val reusedTex = reused.getFbo.get.colorBufferTexture
      assert(reused eq a, "precondition: the same buffer is reused")
      assertEquals(reusedTex.uWrap, Texture.TextureWrap.ClampToEdge, "resetBuffer restores the default wrapU (port line 128)")
      assertEquals(reusedTex.vWrap, Texture.TextureWrap.ClampToEdge, "resetBuffer restores the default wrapV (port line 128)")
      assertEquals(reusedTex.minFilter, Texture.TextureFilter.Nearest, "resetBuffer restores the default min filter (port line 129)")
      assertEquals(reusedTex.magFilter, Texture.TextureFilter.Nearest, "resetBuffer restores the default mag filter (port line 129)")
    } finally pool.close()
  }

  test("ISS561: freePeak tracks the high-water mark and does not decrease when buffers are obtained again") {
    given Sge = VfxHeadless.headlessSge(new VfxHeadless.CompleteGL20)
    val pool  = newPool()
    try {
      val a = pool.obtain()
      val b = pool.obtain()
      val c = pool.obtain()
      pool.free(a)
      pool.free(b)
      pool.free(c)
      assertEquals(pool.freeCount, 3, "all three buffers are free")
      assertEquals(pool.freePeak, 3, "freePeak reached 3 (port line 95)")

      pool.obtain()
      assertEquals(pool.freeCount, 2, "obtaining one drops the free count")
      assertEquals(pool.freePeak, 3, "freePeak is a maximum and must not shrink (port line 95)")
    } finally pool.close()
  }

  test("ISS561: obtain and free on a disposed pool throw IllegalStateException") {
    given Sge = VfxHeadless.headlessSge(new VfxHeadless.CompleteGL20)
    val pool  = newPool()
    val a     = pool.obtain()
    pool.close()
    intercept[IllegalStateException](pool.obtain())
    intercept[IllegalStateException](pool.free(a))
  }
}
