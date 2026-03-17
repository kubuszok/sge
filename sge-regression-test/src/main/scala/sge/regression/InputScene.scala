/*
 * SGE Regression Test — input subsystem polling checks.
 *
 * Tests: Input polling (getX, getY, isTouched, isKeyPressed, getMaxPointers).
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package regression

import sge.utils.ScreenUtils

/** Verifies that input subsystem polling methods work without throwing.
  *
  * Checks run in `init()` — render just clears screen.
  */
object InputScene extends RegressionScene {

  override val name: String = "Input"

  override def init()(using Sge): Unit =
    try {
      val input = Sge().input
      // All of these should work without throwing
      val x    = input.getX()
      val y    = input.getY()
      val maxP = input.getMaxPointers()
      input.isTouched()
      input.isKeyPressed(Input.Keys.A)

      SmokeResult.logCheck("INPUT_POLL", maxP > 0, s"pos=($x,$y), maxPointers=$maxP")
    } catch {
      case e: Exception =>
        SmokeResult.logCheck("INPUT_POLL", false, s"Exception: ${e.getMessage}")
    }

  override def render(elapsed: Float)(using Sge): Unit =
    ScreenUtils.clear(0.05f, 0.1f, 0.15f, 1f)

  override def dispose()(using Sge): Unit = ()
}
