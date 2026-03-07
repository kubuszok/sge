/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/ScaleToAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: no return; split packages; braces on class
 *   Idiom: target.scaleX -> target.foreach; tuple destructure for (x,y)
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

/** Sets the actor's scale from its current value to a specific value.
  * @author
  *   Nathan Sweet
  */
class ScaleToAction extends TemporalAction {
  private var startX: Float = 0
  private var startY: Float = 0
  var endX:           Float = 0
  var endY:           Float = 0

  override protected def begin(): Unit =
    target.foreach { t =>
      startX = t.scaleX
      startY = t.scaleY
    }

  override protected def update(percent: Float): Unit =
    target.foreach { t =>
      val (x, y) =
        if (percent == 0) (startX, startY)
        else if (percent == 1) (endX, endY)
        else (startX + (endX - startX) * percent, startY + (endY - startY) * percent)
      t.setScale(x, y)
    }

  def setScale(x: Float, y: Float): Unit = { endX = x; endY = y }

  def setScale(scale: Float): Unit = { endX = scale; endY = scale }
}
