/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/RotateToAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: no return; split packages; braces on class
 *   Renames: end -> rotation (public var)
 *   Idiom: target.getRotation -> target.foreach; target.setRotation -> target.foreach
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
class RotateToAction(var useShortestDirection: Boolean = false) extends TemporalAction {
  private var start: Float = 0
  var rotation:      Float = 0

  override protected def begin(): Unit =
    target.foreach(t => start = t.rotation)

  override protected def update(percent: Float): Unit =
    target.foreach { t =>
      val r =
        if (percent == 0) start
        else if (percent == 1) rotation
        else if (useShortestDirection) MathUtils.lerpAngleDeg(this.start, this.rotation, percent)
        else start + (rotation - start) * percent
      t.setRotation(r)
    }
}
