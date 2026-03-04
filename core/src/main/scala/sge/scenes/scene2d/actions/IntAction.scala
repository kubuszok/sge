/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/IntAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; no return; split packages; braces on class
 *   Renames: end -> _end (reserved-ish name avoidance)
 *   Idiom: Java (int) cast -> .toInt; constructor chaining like FloatAction
 *   TODO: Java-style getters/setters -- getValue/setValue, getStart/setStart, getEnd/setEnd
 *   TODO: opaque Seconds for duration constructor param -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.Nullable
import sge.math.Interpolation

/** An action that has an int, whose value is transitioned over time.
  * @author
  *   Nathan Sweet
  */
class IntAction(private var start: Int = 0, private var _end: Int = 1) extends TemporalAction {
  private var value: Int = 0

  def this(start: Int, _end: Int, duration: Float) = {
    this(start, _end)
    setDuration(duration)
  }

  def this(start: Int, _end: Int, duration: Float, interpolation: Nullable[Interpolation]) = {
    this(start, _end)
    setDuration(duration)
    setInterpolation(interpolation)
  }

  override protected def begin(): Unit = value = start

  override protected def update(percent: Float): Unit =
    if (percent == 0) value = start
    else if (percent == 1) value = _end
    else value = (start + (_end - start) * percent).toInt

  def getValue: Int = value

  def setValue(value: Int): Unit = this.value = value

  def getStart: Int = start

  def setStart(start: Int): Unit = this.start = start

  def getEnd: Int = _end

  def setEnd(end: Int): Unit = this._end = end
}
