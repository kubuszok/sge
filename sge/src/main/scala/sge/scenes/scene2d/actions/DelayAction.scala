/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/DelayAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: no return; split packages; braces on class
 *   Renames: implements FinishableAction -> with FinishableAction
 *   Idiom: action null-check + early return -> action.fold(true)(_.act(delta))
 *   Convention: opaque Seconds for duration/time params
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 49
 * Covenant-baseline-methods: DelayAction,delegate,finish,restart,time
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/DelayAction.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 59d660057ddc8f835b38c4dd66ed8954259edac7
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.Seconds

/** Delays execution of an action or inserts a pause in a {@link SequenceAction}.
  * @author
  *   Nathan Sweet
  */
class DelayAction(var duration: Seconds = Seconds.zero) extends DelegateAction with FinishableAction {
  var time: Seconds = Seconds.zero

  override protected def delegate(delta: Seconds): Boolean =
    if (time < duration) {
      time += delta
      if (time < duration) false
      else {
        val actionDelta = time - duration
        time = duration
        action.forall(_.act(actionDelta))
      }
    } else {
      action.forall(_.act(delta))
    }

  def finish(): Unit = time = duration

  override def restart(): Unit = {
    super.restart()
    time = Seconds.zero
  }
}
