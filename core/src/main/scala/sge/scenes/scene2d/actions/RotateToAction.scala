/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/RotateToAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: no return; split packages; braces on class
 *   Renames: end -> _end
 *   Idiom: target.getRotation -> target.foreach; target.setRotation -> target.foreach
 *   TODO: Java-style getters/setters -- isUseShortestDirection/setUseShortestDirection
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

import sge.math.MathUtils

/** Sets the actor's rotation from its current value to a specific value.
  * @author
  *   Nathan Sweet
  * @author
  *   Cole Green
  * @author
  *   Tom Gall
  */
class RotateToAction(private var useShortestDirection: Boolean) extends TemporalAction {
  private var start: Float = 0
  private var _end:  Float = 0

  def this() = this(false)

  override protected def begin(): Unit =
    target.foreach(t => start = t.getRotation)

  override protected def update(percent: Float): Unit =
    target.foreach { t =>
      val rotation =
        if (percent == 0) start
        else if (percent == 1) _end
        else if (useShortestDirection) MathUtils.lerpAngleDeg(this.start, this._end, percent)
        else start + (_end - start) * percent
      t.setRotation(rotation)
    }

  def getRotation: Float = _end

  def setRotation(rotation: Float): Unit = this._end = rotation

  def isUseShortestDirection: Boolean = useShortestDirection

  def setUseShortestDirection(useShortestDirection: Boolean): Unit = this.useShortestDirection = useShortestDirection
}
