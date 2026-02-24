/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/SizeToAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package actions

/** Moves an actor from its current size to a specific size.
  * @author
  *   Nathan Sweet
  */
class SizeToAction extends TemporalAction {
  private var startWidth:  Float = 0
  private var startHeight: Float = 0
  private var endWidth:    Float = 0
  private var endHeight:   Float = 0

  override protected def begin(): Unit =
    target.foreach { t =>
      startWidth = t.getWidth
      startHeight = t.getHeight
    }

  override protected def update(percent: Float): Unit =
    target.foreach { t =>
      val (w, h) =
        if (percent == 0) (startWidth, startHeight)
        else if (percent == 1) (endWidth, endHeight)
        else (startWidth + (endWidth - startWidth) * percent, startHeight + (endHeight - startHeight) * percent)
      t.setSize(w, h)
    }

  def setSize(width: Float, height: Float): Unit = { endWidth = width; endHeight = height }

  def getWidth: Float = endWidth

  def setWidth(width: Float): Unit = endWidth = width

  def getHeight: Float = endHeight

  def setHeight(height: Float): Unit = endHeight = height
}
