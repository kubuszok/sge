/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package widget
package internal

import sge.graphics.Cursor
import sge.scenes.scene2d.{ Actor, InputEvent }
import sge.scenes.scene2d.utils.ClickListener
import sge.utils.Nullable
import sge.visui.util.CursorManager

/** Manages setting custom cursor for split panes. This is VisUI internal class
  * @author
  *   Kotcrab
  * @since 1.4.0
  */
abstract class SplitPaneCursorManager(owner: Actor, vertical: Boolean)(using Sge) extends ClickListener() {

  private var currentCursor: Nullable[Cursor.SystemCursor] = Nullable.empty

  override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean =
    handleBoundsContains(x, y)

  override def touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int): Unit = {
    super.touchDragged(event, x, y, pointer) // handles setting cursor when mouse returned to widget after exiting it while dragged
    if (contains(x, y)) {
      setCustomCursor()
    } else {
      clearCustomCursor()
    }
  }

  override def mouseMoved(event: InputEvent, x: Float, y: Float): Boolean = {
    super.mouseMoved(event, x, y)
    if (handleBoundsContains(x, y)) {
      setCustomCursor()
    } else {
      clearCustomCursor()
    }
    false
  }

  override def exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Nullable[Actor]): Unit = {
    super.exit(event, x, y, pointer, toActor)
    if (pointer == -1 && (toActor.isEmpty || !toActor.get.isDescendantOf(owner))) {
      clearCustomCursor()
    }
  }

  private def setCustomCursor(): Unit = {
    val targetCursor =
      if (vertical) Cursor.SystemCursor.VerticalResize
      else Cursor.SystemCursor.HorizontalResize

    if (currentCursor.isEmpty || currentCursor.get != targetCursor) {
      Sge().graphics.setSystemCursor(targetCursor)
      currentCursor = Nullable(targetCursor)
    }
  }

  private def clearCustomCursor(): Unit =
    if (currentCursor.isDefined) {
      CursorManager.restoreDefaultCursor()
      currentCursor = Nullable.empty
    }

  protected def handleBoundsContains(x: Float, y: Float): Boolean

  protected def contains(x: Float, y: Float): Boolean
}
