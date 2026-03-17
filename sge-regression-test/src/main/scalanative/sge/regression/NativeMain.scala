/*
 * SGE Regression Test — Scala Native desktop entry point.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package regression

/** Scala Native desktop entry point for the SGE regression test.
  *
  * Uses @extern C FFI for GLFW, ANGLE, and miniaudio shared libraries.
  */
object NativeMain {

  def main(args: Array[String]): Unit = {
    val app    = DesktopLauncher.createApp()
    val config = DesktopLauncher.createConfig()
    DesktopApplicationFactory(app, config)
  }
}
