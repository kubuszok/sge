/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/TouchableAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package actions

/** Sets the actor's {@link Actor#setTouchable(Touchable) touchability}.
  * @author
  *   Nathan Sweet
  */
class TouchableAction extends Action {
  private var touchable: Touchable = scala.compiletime.uninitialized

  def act(delta: Float): Boolean = {
    target.foreach(_.setTouchable(touchable))
    true
  }

  def getTouchable: Touchable = touchable

  def setTouchable(touchable: Touchable): Unit = this.touchable = touchable
}
