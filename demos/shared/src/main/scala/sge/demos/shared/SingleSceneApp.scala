/*
 * SGE Demos — application wrapper that runs a single DemoScene.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demos
package shared

import _root_.sge.{ApplicationListener, Pixels, Sge, SgeAware}

/** Wraps a [[DemoScene]] as an [[_root_.sge.ApplicationListener]] for platform launchers.
  *
  * Implements [[_root_.sge.SgeAware]] to capture the `Sge` context before `create()` is called.
  */
class SingleSceneApp(scene: DemoScene) extends ApplicationListener with SgeAware {

  private var _sge: Sge = scala.compiletime.uninitialized

  override def sgeAvailable(sge: Sge): Unit =
    this._sge = sge

  override def create(): Unit = {
    given Sge = _sge
    scene.init()
  }

  override def resize(width: Pixels, height: Pixels): Unit = {
    given Sge = _sge
    scene.resize(width, height)
  }

  override def render(): Unit = {
    given Sge = _sge
    scene.render(_sge.graphics.getDeltaTime())
  }

  override def pause(): Unit = ()

  override def resume(): Unit = ()

  override def dispose(): Unit = {
    given Sge = _sge
    scene.dispose()
  }
}
