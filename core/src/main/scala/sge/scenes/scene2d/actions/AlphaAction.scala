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
 *   Idiom: null color fallback -> Nullable.getOrElse(target.fold(...)(_.getColor))
 *   TODO: direct Color.a mutation -- update when Color becomes immutable
 *   TODO: Java-style getters/setters -- getColor/setColor, setAlpha
 *   Audited: 2026-03-03
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
  private var _end:  Float           = 0
  private var color: Nullable[Color] = Nullable.empty

  override protected def begin(): Unit = {
    val c = this.color.getOrElse(target.map(_.getColor).getOrElse(Color()))
    start = c.a
  }

  override protected def update(percent: Float): Unit = {
    val c = this.color.getOrElse(target.map(_.getColor).getOrElse(Color()))
    if (percent == 0)
      c.a = start
    else if (percent == 1)
      c.a = _end
    else
      c.a = start + (_end - start) * percent
  }

  override def reset(): Unit = {
    super.reset()
    color = Nullable.empty
  }

  def getColor: Nullable[Color] = color

  def setColor(color: Nullable[Color]): Unit = this.color = color

  def getAlpha: Float = _end

  def setAlpha(alpha: Float): Unit = this._end = alpha
}
