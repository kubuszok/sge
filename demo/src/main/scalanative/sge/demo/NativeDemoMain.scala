/*
 * SGE Demo — Scala Native desktop entry point.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demo

/** Scala Native desktop entry point for the SGE demo.
  *
  * Uses @extern C FFI for GLFW, ANGLE, and miniaudio shared libraries.
  */
object NativeDemoMain {

  def main(args: Array[String]): Unit = {
    val demoApp = DesktopDemoLauncher.createDemoApp()
    val config  = DesktopDemoLauncher.createConfig()
    DesktopApplicationFactory(demoApp, config)
  }
}
