/*
 * SGE Regression Test — shared desktop launcher logic for JVM and Native.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package regression

/** Shared desktop regression test launcher. Creates the RegressionApp with all check scenes.
  *
  * The actual `main` method is in the platform-specific DesktopMain (JVM or Native), which provides the appropriate DesktopApplicationFactory wiring.
  */
object DesktopLauncher {

  /** Creates the regression test application with all check scenes. */
  def createApp(): RegressionApp = {
    val scenes: Array[RegressionScene] = Array(
      BootstrapScene,
      AssetLoadingScene,
      ShaderScene,
      Model3DScene,
      ClearColorScene,
      InputScene
    )
    new RegressionApp(scenes, sceneDuration = 3f)
  }

  /** Creates the desktop application config for the smoke test. */
  def createConfig(): DesktopApplicationConfig = {
    val config = DesktopApplicationConfig()
    config.title = "SGE Smoke Test"
    config.windowWidth = 800
    config.windowHeight = 600
    config
  }
}
