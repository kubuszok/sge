/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/TouchableAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: split packages; braces on class
 *   Idiom: target.setTouchable -> target.foreach(_.touchable = ...); uninitialized Touchable field
 *   Audited: 2026-03-03
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
  var touchable: Touchable = scala.compiletime.uninitialized

  def act(delta: Float): Boolean = {
    target.foreach(_.touchable = touchable)
    true
  }
}
