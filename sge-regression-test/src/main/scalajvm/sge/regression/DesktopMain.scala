/*
 * SGE Regression Test — JVM desktop entry point.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package regression

/** JVM desktop entry point for the SGE regression test.
  *
  * Uses Panama FFM to load GLFW, ANGLE, and miniaudio shared libraries, then launches the regression test application. The Sge context is passed to `create()` via `(using Sge)`.
  */
object DesktopMain {

  def main(args: Array[String]): Unit = {
    val app    = DesktopLauncher.createApp()
    val config = DesktopLauncher.createConfig()

    // DesktopApplicationFactory blocks until the application exits.
    DesktopApplicationFactory(app, config)
  }
}
