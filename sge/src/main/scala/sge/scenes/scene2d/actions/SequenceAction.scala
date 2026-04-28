/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/SequenceAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; no return; split packages; braces on class
 *   Idiom: setPool(null) -> setPool(Nullable.empty); actor == null -> actor.isEmpty;
 *          early returns -> if/else chain
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 85
 * Covenant-baseline-methods: SequenceAction,act,index,restart,this
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/SequenceAction.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.{ Nullable, Seconds }

/** Executes a number of actions one at a time.
  * @author
  *   Nathan Sweet
  */
class SequenceAction extends ParallelAction {
  private var index: Int = 0

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

  override def act(delta: Seconds): Boolean =
    if (index >= actions.size) true
    else {
      val savedPool = pool
      pool = Nullable.empty // Ensure this action can't be returned to the pool while executing.
      try
        if (actions(index).act(delta)) {
          if (actor.isEmpty) true // This action was removed.
          else {
            index += 1
            index >= actions.size
          }
        } else false
      finally
        pool = savedPool
    }

  override def restart(): Unit = {
    super.restart()
    index = 0
  }
}
