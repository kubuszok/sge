/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/ParallelAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; no return (boundary/break); split packages; braces on class
 *   Renames: Array<Action> -> DynamicArray[Action]
 *   Idiom: setPool(null) -> setPool(Nullable.empty); actor != null -> actor.isDefined;
 *          getActor() != null -> getActor.isDefined; for loop -> while;
 *          early return in loop -> boundary/break; if (actor != null) -> actor.foreach
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.{ DynamicArray, Nullable, Seconds }

/** Executes a number of actions at the same time.
  * @author
  *   Nathan Sweet
  */
class ParallelAction extends Action {
  val actions:          DynamicArray[Action] = DynamicArray[Action]()
  private var complete: Boolean              = false

  def this(action1: Action) = {
    this()
    addAction(action1)
  }

  def this(action1: Action, action2: Action) = {
    this()
    addAction(action1)
    addAction(action2)
  }

  def this(action1: Action, action2: Action, action3: Action) = {
    this()
    addAction(action1)
    addAction(action2)
    addAction(action3)
  }

  def this(action1: Action, action2: Action, action3: Action, action4: Action) = {
    this()
    addAction(action1)
    addAction(action2)
    addAction(action3)
    addAction(action4)
  }

  def this(action1: Action, action2: Action, action3: Action, action4: Action, action5: Action) = {
    this()
    addAction(action1)
    addAction(action2)
    addAction(action3)
    addAction(action4)
    addAction(action5)
  }

  def act(delta: Seconds): Boolean = scala.util.boundary {
    if (complete) scala.util.boundary.break(true)
    complete = true
    val savedPool = pool
    pool = Nullable.empty // Ensure this action can't be returned to the pool while executing.
    try {
      var i = 0
      val n = actions.size
      while (i < n && actor.isDefined) {
        val currentAction = actions(i)
        if (currentAction.actor.isDefined && !currentAction.act(delta)) complete = false
        if (actor.isEmpty) scala.util.boundary.break(true) // This action was removed.
        i += 1
      }
      complete
    } finally
      pool = savedPool
  }

  override def restart(): Unit = {
    complete = false
    var i = 0
    while (i < actions.size) {
      actions(i).restart()
      i += 1
    }
  }

  override def reset(): Unit = {
    super.reset()
    actions.clear()
  }

  def addAction(action: Action): Unit = {
    actions.add(action)
    actor.foreach(a => action.setActor(Nullable(a)))
  }

  override def setActor(actor: Nullable[Actor]): Unit = {
    var i = 0
    while (i < actions.size) {
      actions(i).setActor(actor)
      i += 1
    }
    super.setActor(actor)
  }

  override def toString: String = {
    val buffer = new StringBuilder(64)
    buffer.append(super.toString)
    buffer.append('(')
    var i = 0
    while (i < actions.size) {
      if (i > 0) buffer.append(", ")
      buffer.append(actions(i))
      i += 1
    }
    buffer.append(')')
    buffer.toString
  }
}
