// SGE — Desktop integration test: Bootstrap subsystem check

package sge.it.desktop.checks

import sge.Sge
import sge.it.desktop.CheckResult

/** Verifies that Sge context was created and GL is available. */
object BootstrapCheck {

  def run()(using Sge): CheckResult =
    try {
      val sge = Sge()
      val gl  = sge.graphics.gl20
      if (gl == null) {
        CheckResult("bootstrap", passed = false, "GL20 is null")
      } else {
        CheckResult("bootstrap", passed = true, "Sge context created, GL20 available")
      }
    } catch {
      case e: Exception =>
        CheckResult("bootstrap", passed = false, s"Exception: ${e.getMessage}")
    }
}
