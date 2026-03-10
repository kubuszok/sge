// SGE — Desktop integration test: Fullscreen toggle check
//
// Verifies that fullscreen-related API calls work without crashing.
// On headless/CI the actual mode switch may be a no-op, but the API
// should not throw.

package sge.it.desktop.checks

import sge.Sge
import sge.it.desktop.CheckResult

/** Verifies fullscreen API: isFullscreen, getDisplayMode, setFullscreenMode. */
object FullscreenCheck {

  def run()(using Sge): CheckResult =
    try {
      val gfx = Sge().graphics

      val wasFullscreen = gfx.isFullscreen()
      val displayMode   = gfx.getDisplayMode()
      val monitors      = gfx.getMonitors()

      // Attempt fullscreen toggle — may be no-op on headless but should not throw
      val setResult = gfx.setFullscreenMode(displayMode)

      // Restore windowed mode regardless of setResult
      gfx.setWindowedMode(gfx.getWidth(), gfx.getHeight())

      CheckResult(
        "fullscreen",
        passed = true,
        s"Fullscreen API OK: was=$wasFullscreen, setResult=$setResult, monitors=${monitors.length}, mode=${displayMode.width}x${displayMode.height}"
      )
    } catch {
      case e: Exception =>
        CheckResult("fullscreen", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
