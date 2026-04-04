// SGE — Desktop integration test: FreeType extension check
//
// Initializes the FreeType library via the public API, which triggers
// native lib loading (sge_freetype). A successful init + dispose = pass.

package sge.it.desktop.checks

import sge.Sge
import sge.graphics.g2d.freetype.FreeType
import sge.it.desktop.CheckResult

/** Verifies FreeType native library loading and library initialization. */
object FreetypeCheck {

  def run()(using Sge): CheckResult =
    try {
      val lib = FreeType.initFreeType()
      lib.close()
      CheckResult("freetype_load", passed = true, "FreeType library init + dispose OK")
    } catch {
      case e: UnsatisfiedLinkError =>
        CheckResult("freetype_load", passed = false, s"Native lib missing: ${e.getMessage}")
      case e: Exception =>
        CheckResult("freetype_load", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
