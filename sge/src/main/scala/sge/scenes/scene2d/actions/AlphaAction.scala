/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/AlphaAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; no return; split packages; braces on class
 *   Renames: end -> _end (reserved-ish name avoidance)
 *   Idiom: null color fallback -> Nullable.getOrElse(target.fold(...)(_.color))
 *   TODO: direct Color.a mutation -- update when Color becomes immutable
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 53
 * Covenant-baseline-methods: AlphaAction,alpha,begin,c,color,reset,start,update
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/AlphaAction.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.Nullable
import sge.graphics.Color

/** Sets the alpha for an actor's color (or a specified color), from the current alpha to the new alpha. Note this action transitions from the alpha at the time the action starts to the specified
  * alpha.
  * @author
  *   Nathan Sweet
  */
class AlphaAction extends TemporalAction {
  private var start: Float           = 0
  var alpha:         Float           = 0
  var color:         Nullable[Color] = Nullable.empty

  override protected def begin(): Unit = {
    val c = this.color.getOrElse(target.map(_.color).getOrElse(Color()))
    start = c.a
  }

  override protected def update(percent: Float): Unit = {
    val c = this.color.getOrElse(target.map(_.color).getOrElse(Color()))
    if (percent == 0)
      c.a = start
    else if (percent == 1)
      c.a = alpha
    else
      c.a = start + (alpha - start) * percent
  }

  override def reset(): Unit = {
    super.reset()
    color = Nullable.empty
  }
}
