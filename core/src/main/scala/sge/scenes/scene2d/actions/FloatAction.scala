/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/FloatAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.Nullable
import sge.math.Interpolation

/** An action that has a float, whose value is transitioned over time.
  * @author
  *   Nathan Sweet
  */
class FloatAction(private var start: Float, private var _end: Float) extends TemporalAction {
  private var value: Float = 0

  def this() = this(0, 1)

  def this(start: Float, _end: Float, duration: Float) = {
    this(start, _end)
    setDuration(duration)
  }

  def this(start: Float, _end: Float, duration: Float, interpolation: Nullable[Interpolation]) = {
    this(start, _end)
    setDuration(duration)
    setInterpolation(interpolation)
  }

  override protected def begin(): Unit = value = start

  override protected def update(percent: Float): Unit =
    if (percent == 0) value = start
    else if (percent == 1) value = _end
    else value = start + (_end - start) * percent

  def getValue: Float = value

  def setValue(value: Float): Unit = this.value = value

  def getStart: Float = start

  def setStart(start: Float): Unit = this.start = start

  def getEnd: Float = _end

  def setEnd(end: Float): Unit = this._end = end
}
