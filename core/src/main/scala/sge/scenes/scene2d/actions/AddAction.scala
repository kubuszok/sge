/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/AddAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; no return; split packages; braces on class
 *   Idiom: target.addAction(action) -> target.foreach + actionToAdd.foreach with DynamicArray +=
 *   TODO: Java-style getters/setters -- getAction/setAction
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.Nullable

/** Adds an action to an actor.
  * @author
  *   Nathan Sweet
  */
class AddAction extends Action {
  private var actionToAdd: Nullable[Action] = Nullable.empty

  def act(delta: Float): Boolean = {
    target.foreach { t =>
      actionToAdd.foreach { a =>
        a.setActor(Nullable(t))
        t.getActions += a
      }
    }
    true
  }

  def getAction: Nullable[Action] = actionToAdd

  def setAction(action: Action): Unit = this.actionToAdd = Nullable(action)

  override def restart(): Unit = actionToAdd.foreach(_.restart())

  override def reset(): Unit = {
    super.reset()
    actionToAdd = Nullable.empty
  }
}
