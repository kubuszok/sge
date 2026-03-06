/*
 * SGE Demo — shared desktop launcher logic for JVM and Native.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demo

/** Shared desktop demo launcher. Creates the DemoApp with all scenes.
  *
  * The actual `main` method is in the platform-specific DesktopDemoMain (JVM or Native), which provides the appropriate DesktopApplicationFactory wiring.
  */
object DesktopDemoLauncher {

  /** Creates the demo application listener with all scenes registered. */
  def createDemoApp(): DemoApp = {
    val scenes: Array[DemoScene] = Array(
      ClearColorScene,
      new ShapeScene(),
      new InputScene()
    )
    new DemoApp(scenes, sceneDuration = 10f)
  }

  /** Creates the desktop application config for the demo. */
  def createConfig(): DesktopApplicationConfig = {
    val config = DesktopApplicationConfig()
    config.title = "SGE Demo"
    config.windowWidth = 800
    config.windowHeight = 600
    config
  }
}
