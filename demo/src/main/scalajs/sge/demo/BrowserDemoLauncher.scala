/*
 * SGE Demo — Browser launcher logic.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demo

/** Browser-specific demo launcher. Creates the DemoApp and BrowserApplicationConfig. */
object BrowserDemoLauncher {

  /** Creates the demo application listener with all scenes registered. */
  def createDemoApp(): DemoApp = {
    val scenes: Array[DemoScene] = Array(
      ClearColorScene,
      new ShapeScene(),
      new InputScene()
    )
    new DemoApp(scenes, sceneDuration = 10f)
  }

  /** Creates the browser application config for the demo. */
  def createConfig(): BrowserApplicationConfig =
    new BrowserApplicationConfig(width = 800, height = 600)
}
