/*
 * SGE Demos — application wrapper that runs a single DemoScene.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demos
package shared

import _root_.sge.{ApplicationListener, Pixels, Sge}

/** Wraps a [[DemoScene]] as an [[_root_.sge.ApplicationListener]] for platform launchers.
  */
class SingleSceneApp(scene: DemoScene) extends ApplicationListener {

  override def create()(using Sge): Unit =
    scene.init()

  override def resize(width: Pixels, height: Pixels)(using Sge): Unit =
    scene.resize(width, height)

  override def render()(using Sge): Unit =
    scene.render(Sge().graphics.getDeltaTime())

  override def pause()(using Sge): Unit = ()

  override def resume()(using Sge): Unit = ()

  override def dispose()(using Sge): Unit =
    scene.dispose()
}
