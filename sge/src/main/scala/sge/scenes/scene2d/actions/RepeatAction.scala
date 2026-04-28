/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/RepeatAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: no return; split packages; braces on class
 *   Renames: static FOREVER -> companion object val; implements FinishableAction -> with
 *   Idiom: action.act(delta) with multiple returns -> action.fold(true) with nested if/else
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 67
 * Covenant-baseline-methods: FOREVER,RepeatAction,_isFinished,count,delegate,executedCount,finish,restart
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/RepeatAction.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 59d660057ddc8f835b38c4dd66ed8954259edac7
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.Seconds

/** Repeats an action a number of times or forever.
  * @author
  *   Nathan Sweet
  */
class RepeatAction extends DelegateAction with FinishableAction {
  var count:                 Int     = 0
  private var executedCount: Int     = 0
  private var _isFinished:   Boolean = false

  override protected def delegate(delta: Seconds): Boolean =
    if (executedCount == count) true
    else {
      action.forall { a =>
        if (a.act(delta)) {
          if (_isFinished) true
          else {
            if (count > 0) executedCount += 1
            if (executedCount == count) true
            else {
              a.restart()
              false
            }
          }
        } else false
      }
    }

  def finish(): Unit = _isFinished = true

  override def restart(): Unit = {
    super.restart()
    executedCount = 0
    _isFinished = false
  }
}

object RepeatAction {
  val FOREVER: Int = -1
}
