// SGE — Desktop integration test: Input subsystem check

package sge.it.desktop.checks

import sge.Sge
import sge.it.desktop.CheckResult

/** Verifies that Sge().input state queries work without exceptions. */
object InputCheck {

  def run()(using Sge): CheckResult =
    try {
      val input = Sge().input
      // Basic state queries should not throw
      val x     = input.getX()
      val y     = input.getY()
      val dx    = input.getDeltaX()
      val dy    = input.getDeltaY()
      val touch = input.isTouched()
      val jt    = input.justTouched()
      val kp    = input.isKeyPressed(sge.Input.Keys.A)
      val maxP  = input.getMaxPointers()

      if (maxP <= 0) {
        CheckResult("input", passed = false, s"maxPointers=$maxP, expected > 0")
      } else {
        CheckResult("input", passed = true, s"Input queries OK: maxPointers=$maxP, pos=($x,$y)")
      }
    } catch {
      case e: Exception =>
        CheckResult("input", passed = false, s"Exception: ${e.getMessage}")
    }
}
