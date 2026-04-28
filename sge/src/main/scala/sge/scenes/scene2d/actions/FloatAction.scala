/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/FloatAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; no return; split packages; braces on class
 *   Idiom: Java constructor chaining super(duration) -> this() + setDuration();
 *          super(duration, interpolation) -> this() + setDuration() + setInterpolation()
 *   Convention: opaque Seconds for duration constructor param
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 50
 * Covenant-baseline-methods: FloatAction,begin,getEnd,setEnd,this,update,value
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/FloatAction.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.{ Nullable, Seconds }
import sge.math.Interpolation

/** An action that has a float, whose value is transitioned over time.
  * @author
  *   Nathan Sweet
  */
class FloatAction(var start: Float = 0, private var _end: Float = 1) extends TemporalAction {
  var value: Float = 0

  def getEnd:             Float = _end
  def setEnd(end: Float): Unit  = _end = end

  def this(start: Float, _end: Float, duration: Seconds) = {
    this(start, _end)
    this.duration = duration
  }

  def this(start: Float, _end: Float, duration: Seconds, interpolation: Nullable[Interpolation]) = {
    this(start, _end, duration)
    this.interpolation = interpolation
  }

  override protected def begin(): Unit = value = start

  override protected def update(percent: Float): Unit =
    if (percent == 0) value = start
    else if (percent == 1) value = _end
    else value = start + (_end - start) * percent
}
