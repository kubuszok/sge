/*
 * SGE Demo — JVM desktop entry point.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demo

/** JVM desktop entry point for the SGE demo.
  *
  * Uses Panama FFM to load GLFW, ANGLE, and miniaudio shared libraries, then launches the demo application. The DemoApp receives the Sge context via [[SgeAware.sgeAvailable]] before `create()` is
  * called.
  */
object DesktopDemoMain {

  def main(args: Array[String]): Unit = {
    val demoApp = DesktopDemoLauncher.createDemoApp()
    val config  = DesktopDemoLauncher.createConfig()

    // DesktopApplicationFactory blocks until the application exits.
    // The DemoApp implements SgeAware, so DesktopApplication will call
    // sgeAvailable() with the Sge context before calling create().
    DesktopApplicationFactory(demoApp, config)
  }
}
