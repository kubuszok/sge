/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package layout

import sge.scenes.scene2d.Touchable
import sge.scenes.scene2d.ui.WidgetGroup
import sge.scenes.scene2d.utils.Layout

/** Arranges actors in single column filling available vertical space. Creates new columns and expands horizontally as necessary. Children automatically overflow to next column when necessary.
  *
  * Can be embedded in scroll pane however in that case scrolling in Y direction must be disabled.
  * @author
  *   Kotcrab
  * @since 1.0.0
  * @deprecated
  *   Deprecated since 1.4.7. Use [[FlowGroup]] instead.
  */
@deprecated("Use FlowGroup instead", "1.4.7")
class VerticalFlowGroup(private var _spacing: Float = 0f)(using Sge) extends WidgetGroup() {
  private var _prefWidth:     Float   = 0f
  private var _prefHeight:    Float   = 0f
  private var lastPrefHeight: Float   = 0f
  private var sizeInvalid:    Boolean = true

  touchable = Touchable.childrenOnly

  private def computeSize(): Unit = {
    _prefWidth = 0
    _prefHeight = height
    sizeInvalid = false

    val childArr    = children
    var y           = 0f
    var columnWidth = 0f

    var i = 0
    while (i < childArr.size) {
      val child = childArr(i)
      var w     = child.width
      var h     = child.height
      child match {
        case layout: Layout =>
          w = layout.prefWidth
          h = layout.prefHeight
        case _ =>
      }

      if (y + h > height) {
        y = 0
        _prefWidth += columnWidth + _spacing
        columnWidth = w
      } else {
        columnWidth = scala.math.max(w, columnWidth)
      }
      y += h + _spacing
      i += 1
    }

    // handle last column width
    _prefWidth += columnWidth + _spacing
  }

  override def layout(): Unit = {
    if (sizeInvalid) {
      computeSize()
      if (lastPrefHeight != _prefHeight) {
        lastPrefHeight = _prefHeight
        invalidateHierarchy()
      }
    }

    val childArr    = children
    var x           = 0f
    var y           = height
    var columnWidth = 0f

    var i = 0
    while (i < childArr.size) {
      val child = childArr(i)
      var w     = child.width
      var h     = child.height
      child match {
        case layout: Layout =>
          w = layout.prefWidth
          h = layout.prefHeight
        case _ =>
      }

      if (y - h < 0) {
        y = height
        x += columnWidth + _spacing
        columnWidth = w
      } else {
        columnWidth = scala.math.max(w, columnWidth)
      }

      child.setBounds(x, y - h, w, h)
      y -= h + _spacing
      i += 1
    }
  }

  def spacing: Float = _spacing

  def spacing_=(spacing: Float): Unit = {
    _spacing = spacing
    invalidateHierarchy()
  }

  override def invalidate(): Unit = {
    super.invalidate()
    sizeInvalid = true
  }

  override def prefWidth: Float = {
    if (sizeInvalid) computeSize()
    _prefWidth
  }

  override def prefHeight: Float = {
    if (sizeInvalid) computeSize()
    _prefHeight
  }
}
