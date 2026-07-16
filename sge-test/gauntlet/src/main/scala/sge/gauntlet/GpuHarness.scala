/*
 * SGE Gauntlet — shared GPU resources for GPU probes.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet

import sge.graphics.{ ClearMask, DataType, PixelFormat, Pixmap }
import sge.graphics.g2d.SpriteBatch
import sge.graphics.glutils.{ FrameBuffer, ShapeRenderer }
import sge.graphics.OrthographicCamera

/** GPU resources shared by all GPU probes: the fixed 1280x720 offscreen FBO every GPU probe renders into, a SpriteBatch, a ShapeRenderer and an orthographic pixel-perfect camera.
  *
  * Created lazily by the runner on the first GPU probe (never in headless mode) and disposed after the last probe.
  */
final class GpuHarness(using sge: Sge) {

  val width:  Int = GpuHarness.Width
  val height: Int = GpuHarness.Height

  /** Offscreen render target for all GPU probes; depth-backed so 3D probes work. */
  val fbo: FrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, Pixels(width), Pixels(height), true)

  val camera: OrthographicCamera = {
    val cam = new OrthographicCamera()
    cam.setToOrtho(false, WorldUnits(width.toFloat), WorldUnits(height.toFloat))
    cam.update()
    cam
  }

  val batch: SpriteBatch = {
    val b = SpriteBatch()
    b.projectionMatrix = camera.combined
    b
  }

  val shapes: ShapeRenderer = {
    val s = ShapeRenderer()
    s.setProjectionMatrix(camera.combined)
    s
  }

  /** Clears color (to the given RGBA) and depth of the currently bound framebuffer. */
  def clear(r: Float, g: Float, b: Float, a: Float): Unit = {
    val gl = sge.graphics.gl20
    gl.glClearColor(r, g, b, a)
    gl.glClear(ClearMask.ColorBufferBit | ClearMask.DepthBufferBit)
  }

  /** Reads one RGBA pixel (0..255 per channel) from the currently bound framebuffer. GL coordinates: y grows upwards, matching the probe camera's world coordinates. */
  def readPixel(x: Int, y: Int): (Int, Int, Int, Int) = {
    val gl  = sge.graphics.gl20
    val buf = java.nio.ByteBuffer.allocateDirect(4)
    buf.order(java.nio.ByteOrder.nativeOrder())
    gl.glReadPixels(Pixels(x), Pixels(y), Pixels(1), Pixels(1), PixelFormat.RGBA, DataType.UnsignedByte, buf)
    (buf.get(0) & 0xff, buf.get(1) & 0xff, buf.get(2) & 0xff, buf.get(3) & 0xff)
  }

  /** Captures the currently bound framebuffer into a Pixmap (caller closes it). */
  def capture(): Pixmap =
    Pixmap.createFromFrameBuffer(Pixels(0), Pixels(0), Pixels(width), Pixels(height))

  def dispose(): Unit = {
    batch.close()
    shapes.close()
    fbo.close()
  }
}

object GpuHarness {
  val Width:  Int = 1280
  val Height: Int = 720
}
