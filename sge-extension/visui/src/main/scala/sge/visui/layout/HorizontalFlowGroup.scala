/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 131
 * Covenant-baseline-methods: HorizontalFlowGroup,_prefHeight,_prefWidth,childArr,computeSize,i,invalidate,lastPrefHeight,layout,prefHeight,prefWidth,rowHeight,sizeInvalid,spacing,spacing_,x,y
 * Covenant-source-reference: com/kotcrab/vis/ui/layout/HorizontalFlowGroup.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package layout

import sge.scenes.scene2d.Touchable
import sge.scenes.scene2d.ui.WidgetGroup
import sge.scenes.scene2d.utils.Layout

/** Arranges actors in rows filling available horizontal space. Creates new rows and expands vertically as necessary. Children automatically overflow to next row when necessary.
  *
  * Can be embedded in scroll pane however in that case scrolling in X direction must be disabled.
  * @author
  *   Kotcrab
  * @since 1.0.0
  * @deprecated
  *   Deprecated since 1.4.7. Use [[FlowGroup]] instead.
  */
@deprecated("Use FlowGroup instead", "1.4.7")
class HorizontalFlowGroup(private var _spacing: Float = 0f)(using Sge) extends WidgetGroup() {
  private var _prefWidth:     Float   = 0f
  private var _prefHeight:    Float   = 0f
  private var lastPrefHeight: Float   = 0f
  private var sizeInvalid:    Boolean = true

  touchable = Touchable.childrenOnly

  private def computeSize(): Unit = {
    _prefWidth = width
    _prefHeight = 0
    sizeInvalid = false

    val childArr  = children
    var x         = 0f
    var rowHeight = 0f

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

      if (x + w > width) {
        x = 0
        _prefHeight += rowHeight + _spacing
        rowHeight = h
      } else {
        rowHeight = scala.math.max(h, rowHeight)
      }
      x += w + _spacing
      i += 1
    }

    // handle last row height
    _prefHeight += rowHeight + _spacing
  }

  override def layout(): Unit = {
    if (sizeInvalid) {
      computeSize()
      if (lastPrefHeight != _prefHeight) {
        lastPrefHeight = _prefHeight
        invalidateHierarchy()
      }
    }

    val childArr  = children
    var x         = 0f
    var y         = height
    var rowHeight = 0f

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

      if (x + w > width) {
        x = 0
        y -= rowHeight + _spacing
        rowHeight = h
      } else {
        rowHeight = scala.math.max(h, rowHeight)
      }

      child.setBounds(x, y - h, w, h)
      x += w + _spacing
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
