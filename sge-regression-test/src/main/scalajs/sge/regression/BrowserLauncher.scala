/*
 * SGE Regression Test — Browser launcher logic.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package regression

/** Browser-specific regression test launcher. Creates the RegressionApp and BrowserApplicationConfig. */
object BrowserLauncher {

  /** Creates the regression test application with all check scenes. */
  def createApp(): Sge ?=> RegressionApp = {
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

  /** Creates the browser application config for the smoke test. */
  def createConfig(): BrowserApplicationConfig =
    new BrowserApplicationConfig(width = 800, height = 600)
}
