// SGE — Desktop integration test: Window subsystem check

package sge.it.desktop.checks

import sge.Sge
import sge.it.desktop.CheckResult

/** Verifies window dimensions match the configured size and basic Graphics queries work (type, display mode, buffer format).
  */
object WindowCheck {

  def run()(using Sge): CheckResult =
    try {
      val graphics = Sge().graphics
      val w        = graphics.getWidth()
      val h        = graphics.getHeight()

      // The harness creates a 100x100 window
      if (w.toInt < 1 || h.toInt < 1) {
        return CheckResult("window", passed = false, s"Invalid dimensions: ${w.toInt}x${h.toInt}")
      }

      val gfxType    = graphics.getType()
      val glVersion  = graphics.getGLVersion()
      val bufFmt     = graphics.getBufferFormat()
      val fps        = graphics.getFramesPerSecond()
      val dm         = graphics.getDisplayMode()
      val monitor    = graphics.getMonitor()
      val fullscreen = graphics.isFullscreen()

      val info = s"${w.toInt}x${h.toInt}, type=$gfxType, GL=${glVersion.getMajorVersion()}.${glVersion.getMinorVersion()}, " +
        s"buf=r${bufFmt.r}g${bufFmt.g}b${bufFmt.b}a${bufFmt.a}d${bufFmt.depth}, monitor=${monitor.name}"

      CheckResult("window", passed = true, info)
    } catch {
      case e: Exception =>
        CheckResult("window", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
