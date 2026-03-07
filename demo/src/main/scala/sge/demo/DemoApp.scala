/*
 * SGE Demo — main application that cycles through demo scenes.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demo

/** Main demo application that cycles through [[DemoScene]]s.
  *
  * Each scene runs for `sceneDuration` seconds, then the app advances to the next scene. Press SPACE to skip to the next scene, ESCAPE to exit.
  *
  * Implements [[SgeAware]] to receive the Sge context before `create()` is called by the platform application.
  *
  * @param scenes
  *   the demo scenes to cycle through, in order
  * @param sceneDuration
  *   how long each scene runs (seconds) before auto-advancing
  */
class DemoApp(scenes: Array[DemoScene], sceneDuration: Float = 10f) extends ApplicationListener with SgeAware {

  private var sge:         Sge     = scala.compiletime.uninitialized
  private var initialized: Boolean = false

  private var currentIndex: Int     = 0
  private var sceneElapsed: Float   = 0f
  private var sceneInited:  Boolean = false

  // --- SgeAware ---

  override def sgeAvailable(sge: Sge): Unit =
    this.sge = sge

  // --- ApplicationListener ---

  override def create(): Unit = {
    if (sge == null) return // safety — shouldn't happen with SgeAware
    given Sge = sge
    initCurrentScene()
    initialized = true
  }

  override def resize(width: Int, height: Int): Unit = ()

  override def render(): Unit = {
    if (!initialized) return
    given Sge = sge

    val dt = Sge().graphics.getDeltaTime()
    sceneElapsed += dt

    // Check for scene skip (SPACE) or exit (ESCAPE)
    if (Sge().input.isKeyJustPressed(Input.Keys.SPACE)) advanceScene()
    if (Sge().input.isKeyJustPressed(Input.Keys.ESCAPE)) Sge().application.exit()

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
    given Sge = sge
    disposeCurrentScene()
  }

  // --- Scene management ---

  private def advanceScene()(using Sge): Unit = {
    disposeCurrentScene()
    currentIndex = (currentIndex + 1) % scenes.length
    sceneElapsed = 0f
    initCurrentScene()
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
