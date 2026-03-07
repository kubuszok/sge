/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/MoveToAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: no return; split packages; braces on class
 *   Renames: int alignment -> Align (opaque type); Align.bottomLeft constant
 *   Idiom: target.getX -> target.foreach(t => startX = t.getX(alignment))
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.Align

/** Moves an actor from its current position to a specific position.
  * @author
  *   Nathan Sweet
  */
class MoveToAction extends TemporalAction {

  /** Gets the starting X value, set in {@link #begin()}. */
  var startX: Float = 0

  /** Gets the starting Y value, set in {@link #begin()}. */
  var startY:    Float = 0
  var endX:      Float = 0
  var endY:      Float = 0
  var alignment: Align = Align.bottomLeft

  override protected def begin(): Unit =
    target.foreach { t =>
      startX = t.getX(alignment)
      startY = t.getY(alignment)
    }

  override protected def update(percent: Float): Unit =
    target.foreach { t =>
      val x = if (percent == 0) startX else if (percent == 1) endX else startX + (endX - startX) * percent
      val y = if (percent == 0) startY else if (percent == 1) endY else startY + (endY - startY) * percent
      t.setPosition(x, y, alignment)
    }

  def setStartPosition(x: Float, y: Float): Unit = { startX = x; startY = y }

  def setPosition(x: Float, y: Float): Unit = { endX = x; endY = y }

  def setPosition(x: Float, y: Float, alignment: Align): Unit = { endX = x; endY = y; this.alignment = alignment }

  override def reset(): Unit = {
    super.reset()
    alignment = Align.bottomLeft
  }
}
