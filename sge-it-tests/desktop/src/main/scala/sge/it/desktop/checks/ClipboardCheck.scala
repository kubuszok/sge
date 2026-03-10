// SGE — Desktop integration test: Clipboard subsystem check

package sge.it.desktop.checks

import sge.Sge
import sge.utils.Nullable
import sge.it.desktop.CheckResult

/** Writes text to the clipboard, reads it back, and verifies the roundtrip. */
object ClipboardCheck {

  def run()(using Sge): CheckResult =
    try {
      val clipboard = Sge().application.getClipboard()
      val testText  = s"sge-it-${System.nanoTime()}"

      // Write to clipboard
      clipboard.contents = Nullable(testText)

      // Read back
      val readBack = clipboard.contents
      if (readBack.isEmpty) {
        CheckResult("clipboard", passed = false, "Clipboard contents empty after write")
      } else if (readBack.get != testText) {
        CheckResult("clipboard", passed = false, s"Mismatch: wrote '$testText', read '${readBack.get}'")
      } else {
        // Verify hasContents
        if (!clipboard.hasContents) {
          CheckResult("clipboard", passed = false, "hasContents false after write")
        } else {
          CheckResult("clipboard", passed = true, "Clipboard write/read roundtrip OK")
        }
      }
    } catch {
      case e: Exception =>
        CheckResult("clipboard", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
