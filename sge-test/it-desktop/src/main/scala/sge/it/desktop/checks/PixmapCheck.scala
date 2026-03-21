// SGE — Desktop integration test: Pixmap subsystem check

package sge.it.desktop.checks

import sge.{ Pixels, Sge }
import sge.graphics.{ Color, Pixmap }
import sge.it.desktop.CheckResult

/** Creates a Pixmap, sets and gets a pixel, verifies color roundtrip. */
object PixmapCheck {

  def run()(using Sge): CheckResult =
    try {
      val pixmap = Pixmap(16, 16, Pixmap.Format.RGBA8888)
      try {
        // Draw a red pixel
        pixmap.setColor(Color.RED)
        pixmap.drawPixel(Pixels(5), Pixels(5))

        // Read it back
        val pixel = pixmap.getPixel(Pixels(5), Pixels(5))
        // RED in RGBA8888 is 0xFF0000FF
        val expected = Color.rgba8888(Color.RED)
        if (pixel == expected) {
          CheckResult("pixmap", passed = true, s"Pixmap set/get pixel roundtrip OK (0x${pixel.toHexString})")
        } else {
          CheckResult("pixmap", passed = false, s"Pixel mismatch: got 0x${pixel.toHexString}, expected 0x${expected.toHexString}")
        }
      } finally
        pixmap.close()
    } catch {
      case e: Exception =>
        CheckResult("pixmap", passed = false, s"Exception: ${e.getMessage}")
    }
}
