/*
 * SGE Demos — application wrapper that runs a single DemoScene.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package demos.shared

import sge.{ApplicationListener, Pixels, Sge}

/** Wraps a [[DemoScene]] as an [[sge.ApplicationListener]] for platform launchers.
  */
class SingleSceneApp(scene: DemoScene)(using sge: Sge) extends ApplicationListener {

  override def create(): Unit = {
    scene.init()
  }

  override def resize(width: Pixels, height: Pixels): Unit = {
    scene.resize(width, height)
  }

  override def render(): Unit = {
    scene.render(sge.graphics.deltaTime)
  }

  override def pause(): Unit = ()

  override def resume(): Unit = ()

  override def dispose(): Unit = {
    scene.dispose()
  }
}
