/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/RemoveAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; no return; split packages; braces on class
 *   Idiom: target.removeAction(action) -> target.foreach(_.removeAction(actionToRemove))
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 37
 * Covenant-baseline-methods: RemoveAction,act,actionToRemove,reset
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/RemoveAction.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 72917675ed4718b7997044e26dc6f94544150391
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.{ Nullable, Seconds }

/** Removes an action from an actor.
  * @author
  *   Nathan Sweet
  */
class RemoveAction extends Action {
  var actionToRemove: Nullable[Action] = Nullable.empty

  def act(delta: Seconds): Boolean = {
    target.foreach(_.removeAction(actionToRemove))
    true
  }

  override def reset(): Unit = {
    super.reset()
    actionToRemove = Nullable.empty
  }
}
