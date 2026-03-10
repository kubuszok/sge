// SGE — Desktop integration test: SpriteBatch subsystem check

package sge.it.desktop.checks

import sge.Sge
import sge.graphics.g2d.SpriteBatch
import sge.it.desktop.CheckResult

/** Creates a SpriteBatch and runs a begin/end cycle to verify it works. */
object SpriteBatchCheck {

  def run()(using Sge): CheckResult =
    try {
      val batch = SpriteBatch()
      try {
        batch.begin()
        batch.end()
        CheckResult("spritebatch", passed = true, "SpriteBatch begin/end cycle OK")
      } finally
        batch.close()
    } catch {
      case e: Exception =>
        CheckResult("spritebatch", passed = false, s"Exception: ${e.getMessage}")
    }
}
