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
 *   Idiom: Java (int) cast -> .toInt; constructor chaining like FloatAction
 *   Convention: opaque Seconds for duration constructor param
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.{ Nullable, Seconds }
import sge.math.Interpolation

/** An action that has an int, whose value is transitioned over time.
  * @author
  *   Nathan Sweet
  */
class IntAction(var start: Int = 0, private var _end: Int = 1) extends TemporalAction {
  var value: Int = 0

  def getEnd:           Int  = _end
  def setEnd(end: Int): Unit = _end = end

  def this(start: Int, _end: Int, duration: Seconds) = {
    this(start, _end)
    this.duration = duration
  }

  def this(start: Int, _end: Int, duration: Seconds, interpolation: Nullable[Interpolation]) = {
    this(start, _end, duration)
    this.interpolation = interpolation
  }

  override protected def begin(): Unit = value = start

  override protected def update(percent: Float): Unit =
    if (percent == 0) value = start
    else if (percent == 1) value = _end
    else value = (start + (_end - start) * percent).toInt
}
