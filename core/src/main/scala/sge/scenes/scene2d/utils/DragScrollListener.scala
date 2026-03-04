/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/DragScrollListener.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Static tmpCoords -> companion object val
 * - Timer.Task and Timer.schedule require (using Sge)
 * - TODO: Java-style getters/setters — setPadding
 * - Audited: 2026-03-04
 */
package sge
package scenes
package scene2d
package utils

import sge.math.{ Interpolation, Vector2 }
import sge.scenes.scene2d.InputEvent
import sge.scenes.scene2d.ui.ScrollPane
import sge.utils.Timer

/** Causes a scroll pane to scroll when a drag goes outside the bounds of the scroll pane. Attach the listener to the actor which will cause scrolling when dragged, usually the scroll pane or the
  * scroll pane's actor. <p> If {@link ScrollPane#setFlickScroll(boolean)} is true, the scroll pane must have {@link ScrollPane#setCancelTouchFocus(boolean)} false. When a drag starts that should drag
  * rather than flick scroll, cancel the scroll pane's touch focus using <code>stage.cancelTouchFocus(scrollPane);</code>. In this case the drag scroll listener must not be attached to the scroll
  * pane, else it would also lose touch focus. Instead it can be attached to the scroll pane's actor. <p> If using drag and drop, {@link DragAndDrop#setCancelTouchFocus(boolean)} must be false.
  * @author
  *   Nathan Sweet
  */
class DragScrollListener(scroll: ScrollPane)(using Sge) extends DragListener {
  import DragScrollListener._

  var interpolation: Interpolation = Interpolation.exp5In
  var minSpeed:      Float         = 15
  var maxSpeed:      Float         = 75
  var tickSecs:      Float         = 0.05f
  var startTime:     Long          = 0
  var rampTime:      Long          = 1750
  var padTop:        Float         = 0
  var padBottom:     Float         = 0

  private val scrollUp: Timer.Task = new Timer.Task {
    override def run(): Unit =
      scroll(scroll.getScrollY - getScrollPixels)
  }

  private val scrollDown: Timer.Task = new Timer.Task {
    override def run(): Unit =
      scroll(scroll.getScrollY + getScrollPixels)
  }

  def setup(minSpeedPixels: Float, maxSpeedPixels: Float, tickSecs: Float, rampSecs: Float): Unit = {
    this.minSpeed = minSpeedPixels
    this.maxSpeed = maxSpeedPixels
    this.tickSecs = tickSecs
    rampTime = (rampSecs * 1000).toLong
  }

  def getScrollPixels: Float =
    interpolation.apply(minSpeed, maxSpeed, Math.min(1f, (System.currentTimeMillis() - startTime) / rampTime.toFloat))

  override def drag(event: InputEvent, x: Float, y: Float, pointer: Int): Unit = {
    event.getListenerActor.localToActorCoordinates(scroll, tmpCoords.set(x, y))
    if (isAbove(tmpCoords.y)) {
      scrollDown.cancel()
      if (!scrollUp.isScheduled) {
        startTime = System.currentTimeMillis()
        Timer.schedule(scrollUp, tickSecs, tickSecs)
      }
    } else if (isBelow(tmpCoords.y)) {
      scrollUp.cancel()
      if (!scrollDown.isScheduled) {
        startTime = System.currentTimeMillis()
        Timer.schedule(scrollDown, tickSecs, tickSecs)
      }
    } else {
      scrollUp.cancel()
      scrollDown.cancel()
    }
  }

  override def dragStop(event: InputEvent, x: Float, y: Float, pointer: Int): Unit = {
    scrollUp.cancel()
    scrollDown.cancel()
  }

  protected def isAbove(y: Float): Boolean =
    y >= scroll.getHeight - padTop

  protected def isBelow(y: Float): Boolean =
    y < padBottom

  protected def scroll(y: Float): Unit =
    scroll.setScrollY(y)

  def setPadding(padTop: Float, padBottom: Float): Unit = {
    this.padTop = padTop
    this.padBottom = padBottom
  }
}

object DragScrollListener {
  private[utils] val tmpCoords: Vector2 = Vector2()
}
