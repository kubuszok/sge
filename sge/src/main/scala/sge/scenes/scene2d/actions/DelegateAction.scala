/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/DelegateAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; no return; split packages; braces on class
 *   Idiom: action null-check -> action.foreach; setPool(null) -> setPool(Nullable.empty);
 *          action == null ? "" : "(" + action + ")" -> action.fold("")(a => ...)
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 60
 * Covenant-baseline-methods: DelegateAction,act,action,delegate,reset,restart,savedPool,setActor,setTarget,toString
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/DelegateAction.java
 * Covenant-verified: 2026-04-19
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.{ Nullable, Seconds }

/** Base class for an action that wraps another action.
  * @author
  *   Nathan Sweet
  */
abstract class DelegateAction extends Action {
  var action: Nullable[Action] = Nullable.empty

  protected def delegate(delta: Seconds): Boolean

  final def act(delta: Seconds): Boolean = {
    val savedPool = pool
    pool = Nullable.empty // Ensure this action can't be returned to the pool inside the delegate action.
    try
      delegate(delta)
    finally
      pool = savedPool
  }

  override def restart(): Unit =
    action.foreach(_.restart())

  override def reset(): Unit = {
    super.reset()
    action = Nullable.empty
  }

  override def setActor(actor: Nullable[Actor]): Unit = {
    action.foreach(_.setActor(actor))
    super.setActor(actor)
  }

  override def setTarget(target: Nullable[Actor]): Unit = {
    action.foreach(_.setTarget(target))
    super.setTarget(target)
  }

  override def toString: String =
    super.toString + action.map(a => "(" + a + ")").getOrElse("")
}
