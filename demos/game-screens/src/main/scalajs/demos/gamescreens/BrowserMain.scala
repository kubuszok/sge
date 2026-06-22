/*
 * SGE Demos — browser entry point for the Game/Screen demo.
 *
 * Uses SGE's `BrowserApplication` directly (not the demos' shared
 * `BrowserLauncher`, which is `DemoScene`-based).
 *
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package demos.gamescreens

import sge.{ ApplicationListener, BrowserApplication, BrowserApplicationConfig, Sge }

object BrowserMain {
  def main(args: Array[String]): Unit = {
    // Keep the build-time-generated, self-registering embedded-resources object
    // (Scala.js DCE drops it otherwise — it has no @JSExportTopLevel).
    val _ : AnyRef = _root_.demos.gamescreens.GeneratedEmbeddedResources
    // width/height in CSS pixels.
    val config = new BrowserApplicationConfig(800, 600)
    val app: Sge ?=> ApplicationListener = new GameScreensDemo()
    new BrowserApplication(app, config)
    ()
  }
}
