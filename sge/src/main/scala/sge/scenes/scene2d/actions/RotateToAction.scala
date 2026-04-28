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
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 46
 * Covenant-baseline-methods: RotateToAction,begin,rotation,start,update
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/RotateToAction.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
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
