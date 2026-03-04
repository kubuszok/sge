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
 *   Missing: setStartPosition(x,y), getStartX, getStartY from Java source
 *   TODO: Java-style getters/setters -- getX/setX, getY/setY, getAlignment/setAlignment
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
  private var startX:    Float = 0
  private var startY:    Float = 0
  private var endX:      Float = 0
  private var endY:      Float = 0
  private var alignment: Align = Align.bottomLeft

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

  def setPosition(x: Float, y: Float): Unit = { endX = x; endY = y }

  def setPosition(x: Float, y: Float, alignment: Align): Unit = { endX = x; endY = y; this.alignment = alignment }

  def getX: Float = endX

  def setX(x: Float): Unit = this.endX = x

  def getY: Float = endY

  def setY(y: Float): Unit = this.endY = y

  def getAlignment: Align = alignment

  def setAlignment(alignment: Align): Unit = this.alignment = alignment

  override def reset(): Unit = {
    super.reset()
    alignment = Align.bottomLeft
  }
}
