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

import sge.graphics.g2d.Batch
import sge.scenes.scene2d.ui.Widget
import sge.scenes.scene2d.utils.Drawable
import sge.visui.VisUI

/** BusyBar is a type of indeterminate progress bar. This widget is usually added at the top of table and is shown to indicate that some background work is going on. This widget should span across
  * full width of table that is added to. Default style of widget is blue rectangle that moves from left to right edge of screen in loop.
  * @author
  *   Kotcrab
  * @since 1.1.4
  */
class BusyBar(val style: BusyBar.BusyBarStyle)(using Sge) extends Widget() {
  private var segmentX: Float = 0f

  def this()(using Sge) = this(VisUI.getSkin.get[BusyBar.BusyBarStyle])

  def this(styleName: String)(using Sge) = this(VisUI.getSkin.get[BusyBar.BusyBarStyle](styleName))

  override def prefHeight: Float = style.height.toFloat

  override def prefWidth: Float = style.segmentWidth.toFloat

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    batch.flush()
    if (clipBegin()) {
      val c = color
      batch.setColor(c.r, c.g, c.b, c.a * parentAlpha)
      segmentX += segmentDeltaX
      style.segment.draw(batch, x + segmentX, y, style.segmentWidth.toFloat, style.height.toFloat)
      if (segmentX > width + style.segmentOverflow) {
        resetSegment()
      }
      if (visible) Sge().graphics.requestRendering()
      batch.flush()
      clipEnd()
    }
  }

  def resetSegment(): Unit =
    segmentX = -style.segmentWidth.toFloat - style.segmentOverflow

  protected def segmentDeltaX: Float =
    Sge().graphics.deltaTime.toFloat * width
}

object BusyBar {

  class BusyBarStyle {
    var segment:         Drawable = scala.compiletime.uninitialized
    var segmentOverflow: Int      = 0
    var segmentWidth:    Int      = 0
    var height:          Int      = 0

    def this(style: BusyBarStyle) = {
      this()
      this.segment = style.segment
      this.segmentOverflow = style.segmentOverflow
      this.segmentWidth = style.segmentWidth
      this.height = style.height
    }

    def this(segment: Drawable, segmentOverflow: Int, segmentWidth: Int, height: Int) = {
      this()
      this.segment = segment
      this.segmentOverflow = segmentOverflow
      this.segmentWidth = segmentWidth
      this.height = height
    }
  }
}
