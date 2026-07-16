/*
 * SGE Gauntlet — g2d: FBO render-to-texture round-trip (ISS-572 re-verification).
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.graphics.{ ClearMask, DataType, PixelFormat, Pixmap }
import sge.graphics.glutils.FrameBuffer

import scala.collection.mutable.ListBuffer

/** Renders solid red into a private 64x64 FBO, reads it back, then samples the FBO's color texture through a SpriteBatch draw and asserts the pixels — the full render-to-texture round-trip.
  *
  * This is also the ISS-572 re-verification: the historical ClassCastException fired on the first FrameBuffer use (`textureAttachments.foreach`, a createRef-backed DynamicArray issue fixed in lls
  * 0.3.0). If this probe passes, ISS-572 no longer reproduces.
  */
object FboRoundtripProbe extends FeatureProbe {

  override def id: String = "g2d/fbo-roundtrip"

  override def area: String = "g2d"

  override def requiresGpu: Boolean = true

  override def frames: Int = 2

  private val checks = ListBuffer.empty[Check]

  private var inner:    Option[FrameBuffer]          = None
  private var readback: Option[(Int, Int, Int, Int)] = None

  override def init(ctx: ProbeContext): Unit = {
    checks.clear()
    readback = None
    ctx.clear(0f, 0f, 0f, 1f)
  }

  override def render(ctx: ProbeContext, frame: Int): Unit =
    if (frame == 1) {
      given Sge = ctx.sgeCtx
      // First FBO use — the ISS-572 trigger point.
      val fbo = new FrameBuffer(Pixmap.Format.RGBA8888, Pixels(64), Pixels(64), false)
      inner = Some(fbo)
      val gl = ctx.sgeCtx.graphics.gl20
      val rb = fbo.use {
        while (gl.glGetError() != 0) {}
        gl.glClearColor(1f, 0f, 0f, 1f)
        gl.glClear(ClearMask.ColorBufferBit)
        val buf = java.nio.ByteBuffer.allocateDirect(4)
        buf.order(java.nio.ByteOrder.nativeOrder())
        gl.glReadPixels(Pixels(32), Pixels(32), Pixels(1), Pixels(1), PixelFormat.RGBA, DataType.UnsignedByte, buf)
        (buf.get(0) & 0xff, buf.get(1) & 0xff, buf.get(2) & 0xff, buf.get(3) & 0xff)
      }
      readback = Some(rb)
      // fbo.use() rebound the DEFAULT framebuffer; the runner re-binds the probe FBO next frame.
      ()
    } else {
      // Draw the inner FBO's color texture into the probe target — texture side of the round-trip.
      inner.foreach { fbo =>
        val batch = ctx.batch
        batch.rendering {
          batch.setColor(1f, 1f, 1f, 1f)
          batch.draw(fbo.colorBufferTexture, 100f, 100f, 200f, 200f)
        }
      }
    }

  private def near(actual: Int, expected: Int, tolerance: Int = 12): Boolean =
    Math.abs(actual - expected) <= tolerance

  override def verify(ctx: ProbeContext): List[Check] = {
    readback match {
      case Some((r, g, b, a)) =>
        checks += Check.cond("fbo-clear-readback", near(r, 255) && near(g, 0) && near(b, 0) && near(a, 255), "RGBA(255,0,0,255) +-12", s"RGBA($r,$g,$b,$a)")
      case None =>
        checks += Check.cond("fbo-clear-readback", passed = false, "readback captured", "no readback (FBO creation failed?)")
    }
    val (tr, tg, tb, _) = ctx.pixelRgba(200, 200) // inside the drawn texture quad
    checks += Check.cond("fbo-texture-sampled", near(tr, 255) && near(tg, 0) && near(tb, 0), "RGB(255,0,0) +-12 at (200,200)", s"RGB($tr,$tg,$tb)")
    val (br, bg, bb, _) = ctx.pixelRgba(500, 500) // outside the quad
    checks += Check.cond("background-untouched", near(br, 0) && near(bg, 0) && near(bb, 0), "RGB(0,0,0) +-12 at (500,500)", s"RGB($br,$bg,$bb)")
    inner.foreach(_.close())
    inner = None
    checks.toList
  }
}
