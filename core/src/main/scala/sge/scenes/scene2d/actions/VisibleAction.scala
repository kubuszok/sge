/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/VisibleAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; no return; split packages; braces on class
 *   Idiom: target.setVisible(visible) -> target.foreach(_.setVisible(visible))
 *   TODO: Java-style getters/setters -- isVisible/setVisible
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

/** Sets the actor's {@link Actor#setVisible(boolean) visibility}.
  * @author
  *   Nathan Sweet
  */
class VisibleAction extends Action {
  private var visible: Boolean = false

  def act(delta: Float): Boolean = {
    target.foreach(_.setVisible(visible))
    true
  }

  def isVisible: Boolean = visible

  def setVisible(visible: Boolean): Unit = this.visible = visible
}
