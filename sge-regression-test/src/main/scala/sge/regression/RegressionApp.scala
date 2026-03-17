/*
 * SGE Regression Test — main application that runs through check scenes.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package regression

/** Regression test application that runs through [[RegressionScene]]s sequentially.
  *
  * Each scene runs for `sceneDuration` seconds, then the app advances to the next scene. After the last scene completes, prints a structured summary and exits.
  *
  * Not a visual demo — purely functional regression checks. Uses the `SGE-IT:...:PASS/FAIL:...` structured logging protocol.
  *
  * @param scenes
  *   the check scenes to run through, in order
  * @param sceneDuration
  *   how long each scene runs (seconds) before auto-advancing
  */
class RegressionApp(scenes: Array[RegressionScene], sceneDuration: Float = 3f) extends ApplicationListener {

  private var initialized: Boolean = false

  private var currentIndex: Int     = 0
  private var sceneElapsed: Float   = 0f
  private var sceneInited:  Boolean = false

  // --- ApplicationListener ---

  override def create()(using Sge): Unit = {
    initCurrentScene()
    initialized = true
  }

  override def resize(width: Pixels, height: Pixels)(using Sge): Unit = ()

  override def render()(using Sge): Unit = {
    if (!initialized) return

    val dt = Sge().graphics.getDeltaTime()
    sceneElapsed += dt

    // Auto-advance after duration
    if (sceneElapsed >= sceneDuration) advanceScene()

    // Render current scene
    if (currentIndex < scenes.length && sceneInited) {
      scenes(currentIndex).render(sceneElapsed)
    }
  }

  override def pause()(using Sge): Unit = ()

  override def resume()(using Sge): Unit = ()

  override def dispose()(using Sge): Unit = {
    if (!initialized) return
    disposeCurrentScene()
  }

  // --- Scene management ---

  private def advanceScene()(using Sge): Unit = {
    disposeCurrentScene()
    currentIndex += 1
    sceneElapsed = 0f
    if (currentIndex >= scenes.length) {
      // All scenes complete — print summary and exit
      SmokeResult.summary()
      Sge().application.exit()
    } else {
      initCurrentScene()
    }
  }

  private def initCurrentScene()(using Sge): Unit =
    if (currentIndex < scenes.length) {
      scenes(currentIndex).init()
      sceneInited = true
    }

  private def disposeCurrentScene()(using Sge): Unit =
    if (sceneInited && currentIndex < scenes.length) {
      scenes(currentIndex).dispose()
      sceneInited = false
    }
}
