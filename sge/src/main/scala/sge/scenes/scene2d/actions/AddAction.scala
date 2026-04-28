/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/AddAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; no return; split packages; braces on class
 *   Idiom: target.addAction(action) -> target.foreach + actionToAdd.foreach with DynamicArray +=
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 44
 * Covenant-baseline-methods: AddAction,act,actionToAdd,reset,restart
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/AddAction.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.{ Nullable, Seconds }

/** Adds an action to an actor.
  * @author
  *   Nathan Sweet
  */
class AddAction extends Action {
  var actionToAdd: Nullable[Action] = Nullable.empty

  def act(delta: Seconds): Boolean = {
    target.foreach { t =>
      actionToAdd.foreach { a =>
        a.setActor(Nullable(t))
        t.actions += a
      }
    }
    true
  }

  override def restart(): Unit = actionToAdd.foreach(_.restart())

  override def reset(): Unit = {
    super.reset()
    actionToAdd = Nullable.empty
  }
}
