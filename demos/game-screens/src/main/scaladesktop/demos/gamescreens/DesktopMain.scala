/*
 * SGE Demos — desktop entry point for the Game/Screen demo.
 *
 * Uses SGE's `DesktopApplicationFactory` directly (not the demos' shared
 * `DesktopLauncher`, which is `DemoScene`-based). Shared between JVM (Panama
 * FFM) and Scala Native (@extern C FFI); the platform-specific
 * `DesktopApplicationFactory` is resolved at link time.
 *
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package demos.gamescreens

import sge.{ ApplicationListener, DesktopApplicationConfig, DesktopApplicationFactory, Sge }

object DesktopMain {
  def main(args: Array[String]): Unit = {
    val config = DesktopApplicationConfig()
    config.title = "SGE Game/Screen"
    config.windowWidth = 800
    config.windowHeight = 600
    config.foregroundFPS = 60

    // `Sge ?=> ApplicationListener`: GameScreensDemo's (using Sge) is filled in by the launcher.
    val app: Sge ?=> ApplicationListener = new GameScreensDemo()
    DesktopApplicationFactory(app, config)
  }
}
