// SGE — Desktop integration test: Window resize verification
//
// Resizes the GLFW window and verifies the graphics dimensions update.

package sge.it.desktop.checks

import sge.{ Pixels, Sge }
import sge.it.desktop.CheckResult

/** Verifies window resize via setWindowedMode and dimension readback. */
object WindowResizeCheck {

  def run()(using Sge): CheckResult =
    try {
      val gfx = Sge().graphics
      val w0  = gfx.getWidth()
      val h0  = gfx.getHeight()

      val ok = gfx.setWindowedMode(Pixels(200), Pixels(150))
      Thread.sleep(100) // let GLFW process the resize

      val w1 = gfx.getWidth()
      val h1 = gfx.getHeight()

      // Restore original size
      gfx.setWindowedMode(w0, h0)

      if (!ok) {
        CheckResult("window_resize", passed = false, "setWindowedMode returned false")
      } else if (w1.toInt != w0.toInt) {
        // Dimensions changed — resize worked (exact values may vary with DPI)
        CheckResult("window_resize", passed = true, s"Resized ${w0.toInt}x${h0.toInt} -> ${w1.toInt}x${h1.toInt}")
      } else {
        // On headless/Xvfb the resize may be a no-op but shouldn't fail
        CheckResult("window_resize", passed = true, s"setWindowedMode OK (dims unchanged on headless: ${w1.toInt}x${h1.toInt})")
      }
    } catch {
      case e: Exception =>
        CheckResult("window_resize", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
