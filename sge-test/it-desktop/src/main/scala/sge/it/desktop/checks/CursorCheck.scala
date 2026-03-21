// SGE — Desktop integration test: Cursor management
//
// Sets system cursors and creates a custom cursor from a Pixmap.
// No crash = pass.

package sge.it.desktop.checks

import sge.{ Pixels, Sge }
import sge.graphics.Cursor.SystemCursor
import sge.graphics.Pixmap
import sge.it.desktop.CheckResult

/** Verifies system cursor switching and custom cursor creation from Pixmap. */
object CursorCheck {

  def run()(using Sge): CheckResult =
    try {
      val gfx = Sge().graphics

      // Switch between system cursors
      gfx.setSystemCursor(SystemCursor.Hand)
      gfx.setSystemCursor(SystemCursor.Crosshair)
      gfx.setSystemCursor(SystemCursor.Arrow)

      // Create a custom cursor from a small pixmap
      val pm = new Pixmap(16, 16, Pixmap.Format.RGBA8888)
      pm.setColor(1f, 1f, 1f, 1f)
      pm.fill()
      val cursor = gfx.newCursor(pm, Pixels(0), Pixels(0))
      pm.close()

      cursor.foreach { c =>
        gfx.setCursor(c)
        gfx.setSystemCursor(SystemCursor.Arrow) // restore default
      }

      CheckResult("cursor", passed = true, s"System + custom cursor OK, custom=${cursor.isDefined}")
    } catch {
      case e: Exception =>
        CheckResult("cursor", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
