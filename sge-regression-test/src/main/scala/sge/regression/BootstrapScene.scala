/*
 * SGE Regression Test — bootstrap subsystem checks.
 *
 * Tests: Sge context, GL20, graphics dimensions, audio/files/input subsystems.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package regression

import sge.utils.ScreenUtils

/** Verifies core subsystems are accessible immediately after GL context creation.
  *
  * All checks run in `init()` — no visual rendering needed, just clear to green on pass / red on fail.
  */
object BootstrapScene extends RegressionScene {

  override val name: String = "Bootstrap"

  private var ok: Boolean = false

  override def init()(using Sge): Unit = {
    try {
      val gl = Sge().graphics.getGL20()
      SmokeResult.logCheck("GL20", gl != null, if (gl != null) "GL20 available" else "GL20 is null") // scalafix:ok

      val w = Sge().graphics.getWidth().toInt
      val h = Sge().graphics.getHeight().toInt
      SmokeResult.logCheck("VIEWPORT", w > 0 && h > 0, s"${w}x${h}")

      val audio = Sge().audio
      SmokeResult.logCheck("AUDIO_ACCESS", audio != null, audio.getClass.getSimpleName) // scalafix:ok

      val files = Sge().files
      SmokeResult.logCheck("FILES_ACCESS", files != null, files.getClass.getSimpleName) // scalafix:ok

      val maxP = Sge().input.getMaxPointers()
      SmokeResult.logCheck("INPUT_ACCESS", maxP > 0, s"maxPointers=$maxP")

      ok = true
    } catch {
      case e: Exception =>
        SmokeResult.logCheck("BOOTSTRAP", false, s"Exception: ${e.getMessage}")
    }
  }

  override def render(elapsed: Float)(using Sge): Unit =
    if (ok) ScreenUtils.clear(0f, 0.5f, 0f, 1f)
    else ScreenUtils.clear(0.5f, 0f, 0f, 1f)

  override def dispose()(using Sge): Unit = ()
}
