/*
 * SGE Regression Test — shared desktop launcher logic for JVM and Native.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package regression

/** Shared desktop regression test launcher. Creates the RegressionApp with all check scenes.
  *
  * The actual `main` method is in the platform-specific DesktopMain (JVM or Native), which provides the appropriate DesktopApplicationFactory (windowed) or, when `--headless` is passed, delegates to
  * [[runHeadless]] which runs the app under [[HeadlessApplication]] with no GL context.
  */
object DesktopLauncher {

  /** All regression scenes, in run order. */
  private def allScenes: Array[RegressionScene] = Array(
    BootstrapScene,
    AssetLoadingScene,
    ShaderScene,
    Model3DScene,
    ClearColorScene,
    InputScene
  )

  /** Scenes excluded from a headless run because they cannot be honestly exercised under [[sge.noop.NoopGraphics]]/[[sge.noop.NoopGL20]] (no real GL context):
    *
    *   - [[ShaderScene]]: its entire assertion is the shader COMPILE_STATUS, which NoopGL20 never sets (`glGetShaderiv`/`glGetProgramiv` leave the status buffer at 0). The check could only pass by
    *     faking a compile — so the scene is skipped rather than weakened.
    *   - [[Model3DScene]]: the 3D pipeline needs a real default shader; under NoopGL20 the ModelBatch shader reports not-compiled and rendering the instance would throw. Skipped rather than crash.
    */
  private def headlessExcluded: Array[RegressionScene] = Array(ShaderScene, Model3DScene)

  /** Creates the regression test application with the check scenes.
    *
    * @param headless
    *   when true, GPU-only scenes (see [[headlessExcluded]]) are dropped so the app runs cleanly under [[HeadlessApplication]]; scene duration is shortened since there is no display to observe.
    */
  def createApp(headless: Boolean = false): Sge ?=> RegressionApp = {
    val excluded = if (headless) headlessExcluded.toSet else Set.empty[RegressionScene]
    val scenes   = allScenes.filterNot(excluded.contains)
    val duration = if (headless) 0.5f else 3f
    new RegressionApp(scenes, sceneDuration = duration)
  }

  /** Emits an SGE-IT `SKIP` line for every scene excluded from the headless run, with the reason. */
  def logHeadlessExclusions(): Unit =
    headlessExcluded.foreach { scene =>
      System.out.println(
        s"SGE-IT:${scene.name}:SKIP:excluded from headless run — no GL context under NoopGraphics/NoopGL20"
      )
    }

  /** Runs the regression app under a [[HeadlessApplication]] (no GL context, NoopGraphics/NoopGL20) and terminates the process with exit code 0 on `SMOKE_TEST_PASSED`, non-zero otherwise.
    *
    * Shared by both the JVM (`DesktopMain`) and Native (`NativeMain`) `--headless` entry points. The HeadlessApplication runs its render loop on a background (non-daemon) thread; this call blocks on
    * a latch released by [[SmokeResult]]'s completion hook until the app has printed its summary, then exits with the derived code. The latch lives here (desktop-only, JVM/Native) rather than in the
    * shared SmokeResult so that source still links on Scala.js.
    */
  def runHeadless(): Unit = {
    logHeadlessExclusions()
    val completion = new java.util.concurrent.CountDownLatch(1)
    SmokeResult.setOnComplete(() => completion.countDown())
    val config = HeadlessApplicationConfig()
    val _      = new HeadlessApplication(createApp(headless = true), config)
    completion.await()
    System.exit(if (SmokeResult.allPassed) 0 else 1)
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
