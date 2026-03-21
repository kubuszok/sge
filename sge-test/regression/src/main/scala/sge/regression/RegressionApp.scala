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
class RegressionApp(scenes: Array[RegressionScene], sceneDuration: Float = 3f)(using sge: Sge) extends ApplicationListener {

  private var initialized: Boolean = false

  private var currentIndex: Int     = 0
  private var sceneElapsed: Float   = 0f
  private var sceneInited:  Boolean = false

  // --- ApplicationListener ---

  override def create(): Unit = {
    initCurrentScene()
    initialized = true
  }

  override def resize(width: Pixels, height: Pixels): Unit = ()

  override def render(): Unit = {
    if (!initialized) return

    sceneElapsed += sge.graphics.deltaTime.toFloat

    // Auto-advance after duration
    if (sceneElapsed >= sceneDuration) advanceScene()

    // Render current scene
    if (currentIndex < scenes.length && sceneInited) {
      scenes(currentIndex).render(sceneElapsed)
    }
  }

  override def pause(): Unit = ()

  override def resume(): Unit = ()

  override def dispose(): Unit = {
    if (!initialized) return
    disposeCurrentScene()
  }

  // --- Scene management ---

  private def advanceScene(): Unit = {
    disposeCurrentScene()
    currentIndex += 1
    sceneElapsed = 0f
    if (currentIndex >= scenes.length) {
      // All scenes complete — print summary and exit
      SmokeResult.summary()
      sge.application.exit()
    } else {
      initCurrentScene()
    }
  }

  private def initCurrentScene(): Unit =
    if (currentIndex < scenes.length) {
      scenes(currentIndex).init()
      sceneInited = true
    }

  private def disposeCurrentScene(): Unit =
    if (sceneInited && currentIndex < scenes.length) {
      scenes(currentIndex).dispose()
      sceneInited = false
    }
}
