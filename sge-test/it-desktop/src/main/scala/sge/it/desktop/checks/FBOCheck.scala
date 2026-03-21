// SGE — Desktop integration test: FrameBuffer readback check
//
// Creates an FBO, renders red to it, reads back pixels via glReadPixels,
// and verifies the color matches.

package sge.it.desktop.checks

import sge.{ Pixels, Sge }
import sge.graphics.{ ClearMask, DataType, PixelFormat, Pixmap }
import sge.graphics.glutils.FrameBuffer
import sge.it.desktop.CheckResult

/** Verifies FBO rendering and pixel readback via glReadPixels. */
object FBOCheck {

  def run()(using Sge): CheckResult =
    try {
      val gl = Sge().graphics.gl20

      // Create a small FBO
      val fbo = new FrameBuffer(Pixmap.Format.RGBA8888, Pixels(4), Pixels(4), false)

      val (r, g, b, a) = fbo.use {
        // Clear to red
        gl.glClearColor(1f, 0f, 0f, 1f)
        gl.glClear(ClearMask.ColorBufferBit)

        // Read back a pixel
        val buf = java.nio.ByteBuffer.allocateDirect(4)
        buf.order(java.nio.ByteOrder.nativeOrder())
        gl.glReadPixels(Pixels(0), Pixels(0), Pixels(1), Pixels(1), PixelFormat.RGBA, DataType.UnsignedByte, buf)

        (buf.get(0) & 0xff, buf.get(1) & 0xff, buf.get(2) & 0xff, buf.get(3) & 0xff)
      }
      fbo.close()

      val err = gl.glGetError()
      if (err != 0) {
        CheckResult("fbo", passed = false, s"GL error: 0x${err.toHexString}")
      } else if (r < 200 || g > 50 || b > 50 || a < 200) {
        CheckResult("fbo", passed = false, s"Expected red pixel, got RGBA($r,$g,$b,$a)")
      } else {
        CheckResult("fbo", passed = true, s"FBO render + readback OK: RGBA($r,$g,$b,$a)")
      }
    } catch {
      case e: Exception =>
        CheckResult("fbo", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
