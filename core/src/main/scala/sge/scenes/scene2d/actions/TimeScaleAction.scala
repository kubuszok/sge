/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/TimeScaleAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package actions

/** Scales the delta time of an action.
  * @author
  *   Nathan Sweet
  */
class TimeScaleAction extends DelegateAction {
  private var scale: Float = 0

  override protected def delegate(delta: Float): Boolean =
    action.fold(true)(_.act(delta * scale))

  def getScale: Float = scale

  def setScale(scale: Float): Unit = this.scale = scale
}
