/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/RemoveAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.Nullable

/** Removes an action from an actor.
  * @author
  *   Nathan Sweet
  */
class RemoveAction extends Action {
  private var actionToRemove: Nullable[Action] = Nullable.empty

  def act(delta: Float): Boolean = {
    target.foreach(_.removeAction(actionToRemove))
    true
  }

  def getAction: Nullable[Action] = actionToRemove

  def setAction(action: Action): Unit = this.actionToRemove = Nullable(action)

  override def reset(): Unit = {
    super.reset()
    actionToRemove = Nullable.empty
  }
}
