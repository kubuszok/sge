/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: ccmb2r
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 293
 * Covenant-baseline-methods: FlowGroup,_minHeight,_minWidth,childArr,columnWidth,computeSize,computeSizeHorizontal,computeSizeIfNeeded,computeSizeVertical,currentColumnWidth,currentRowHeight,i,invalidate,layout,layoutHorizontal,layoutVertical,layoutedHeight,layoutedWidth,maxChildHeight,maxChildWidth,minHeight,minWidth,prefHeight,prefWidth,relaxedHeight,relaxedWidth,rowHeight,sizeInvalid,spacing,spacing_,targetHeight,targetWidth,totalHeight,totalWidth,vertical,vertical_,wasChildProcessed,x,y
 * Covenant-source-reference: com/kotcrab/vis/ui/layout/FlowGroup.java
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

/** Arranges actors to flow in a specified layout direction using up available space and, if sensible, expanding in that direction. This is a more versatile implementation of a flow group subsuming
  * [[HorizontalFlowGroup]] and [[VerticalFlowGroup]].
  * @author
  *   ccmb2r
  * @since 1.4.7
  */
class FlowGroup(private var _vertical: Boolean, private var _spacing: Float = 0f)(using Sge) extends WidgetGroup() {
  private var sizeInvalid: Boolean = true

  private var _minWidth:  Float = 0f
  private var _minHeight: Float = 0f

  private var layoutedWidth:  Float = 0f
  private var layoutedHeight: Float = 0f

  private var relaxedWidth:  Float = 0f
  private var relaxedHeight: Float = 0f

  touchable = Touchable.childrenOnly

  def vertical: Boolean = _vertical

  def vertical_=(vertical: Boolean): Unit = {
    if (_vertical == vertical) return
    _vertical = vertical
    invalidate()
  }

  protected def computeSize(): Unit =
    if (_vertical) computeSizeVertical()
    else computeSizeHorizontal()

  protected def computeSizeHorizontal(): Unit = {
    val targetWidth = width

    var maxChildWidth    = 0f
    var maxChildHeight   = 0f
    var x                = 0f
    var currentRowHeight = 0f
    var totalWidth       = 0f
    var totalHeight      = 0f

    val childArr          = children
    var wasChildProcessed = false

    var i = 0
    while (i < childArr.size) {
      val child       = childArr(i)
      var childWidth  = 0f
      var childHeight = 0f

      child match {
        case layout: Layout =>
          childWidth = layout.prefWidth
          childHeight = layout.prefHeight
        case _ =>
          childWidth = child.width
          childHeight = child.height
      }

      if (x + childWidth <= targetWidth || !wasChildProcessed) {
        currentRowHeight = scala.math.max(childHeight, currentRowHeight)
      } else {
        totalHeight += currentRowHeight + _spacing
        x = 0
        currentRowHeight = childHeight
      }

      val widthIncrement = childWidth + _spacing
      x += widthIncrement
      totalWidth += widthIncrement

      maxChildWidth = scala.math.max(maxChildWidth, childWidth)
      maxChildHeight = scala.math.max(maxChildHeight, childHeight)
      wasChildProcessed = true
      i += 1
    }

    if (wasChildProcessed) totalWidth -= _spacing
    totalHeight += currentRowHeight

    _minWidth = maxChildWidth
    _minHeight = maxChildHeight
    layoutedWidth = targetWidth
    layoutedHeight = totalHeight
    relaxedWidth = totalWidth

    sizeInvalid = false
  }

  protected def computeSizeVertical(): Unit = {
    val targetHeight = height

    var maxChildWidth      = 0f
    var maxChildHeight     = 0f
    var y                  = targetHeight
    var currentColumnWidth = 0f
    var totalWidth         = 0f
    var totalHeight        = 0f

    val childArr          = children
    var wasChildProcessed = false

    var i = 0
    while (i < childArr.size) {
      val child       = childArr(i)
      var childWidth  = 0f
      var childHeight = 0f

      child match {
        case layout: Layout =>
          childWidth = layout.prefWidth
          childHeight = layout.prefHeight
        case _ =>
          childWidth = child.width
          childHeight = child.height
      }

      if (y - childHeight >= 0 || !wasChildProcessed) {
        currentColumnWidth = scala.math.max(childWidth, currentColumnWidth)
      } else {
        totalWidth += currentColumnWidth + _spacing
        y = targetHeight
        currentColumnWidth = childWidth
      }

      val heightIncrement = childHeight + _spacing
      y -= heightIncrement
      totalHeight += heightIncrement

      maxChildWidth = scala.math.max(maxChildWidth, childWidth)
      maxChildHeight = scala.math.max(maxChildHeight, childHeight)
      wasChildProcessed = true
      i += 1
    }

    if (wasChildProcessed) totalHeight -= _spacing
    totalWidth += currentColumnWidth

    _minWidth = maxChildWidth
    _minHeight = maxChildHeight
    layoutedWidth = totalWidth
    layoutedHeight = targetHeight
    relaxedHeight = totalHeight

    sizeInvalid = false
  }

  override def layout(): Unit =
    if (_vertical) layoutVertical()
    else layoutHorizontal()

  protected def layoutHorizontal(): Unit = {
    computeSizeIfNeeded()

    val targetWidth  = width
    val targetHeight = height

    var x         = 0f
    var y         = targetHeight
    var rowHeight = 0f

    val childArr          = children
    var wasChildProcessed = false

    var i = 0
    while (i < childArr.size) {
      val child       = childArr(i)
      var childWidth  = 0f
      var childHeight = 0f

      child match {
        case layout: Layout =>
          childWidth = layout.prefWidth
          childHeight = layout.prefHeight
          child.setSize(childWidth, childHeight)
        case _ =>
          childWidth = child.width
          childHeight = child.height
      }

      if (x + childWidth <= targetWidth || !wasChildProcessed) {
        rowHeight = scala.math.max(childHeight, rowHeight)
      } else {
        x = 0
        y -= rowHeight + _spacing
        rowHeight = childHeight
      }

      child.setPosition(x, y - childHeight)
      x += childWidth + _spacing
      wasChildProcessed = true
      i += 1
    }

    if (height != layoutedHeight) invalidateHierarchy()
  }

  protected def layoutVertical(): Unit = {
    computeSizeIfNeeded()

    val targetHeight = height

    var x           = 0f
    var y           = targetHeight
    var columnWidth = 0f

    val childArr          = children
    var wasChildProcessed = false

    var i = 0
    while (i < childArr.size) {
      val child       = childArr(i)
      var childWidth  = 0f
      var childHeight = 0f

      child match {
        case layout: Layout =>
          childWidth = layout.prefWidth
          childHeight = layout.prefHeight
          child.setSize(childWidth, childHeight)
        case _ =>
          childWidth = child.width
          childHeight = child.height
      }

      if (y - childHeight >= 0 || !wasChildProcessed) {
        columnWidth = scala.math.max(childWidth, columnWidth)
      } else {
        x += columnWidth + _spacing
        y = targetHeight
        columnWidth = childWidth
      }

      child.setPosition(x, y - childHeight)
      y -= childHeight + _spacing
      wasChildProcessed = true
      i += 1
    }

    if (width != layoutedWidth) invalidateHierarchy()
  }

  def spacing: Float = _spacing

  def spacing_=(spacing: Float): Unit = {
    _spacing = spacing
    invalidateHierarchy()
  }

  override def invalidate(): Unit = {
    sizeInvalid = true
    super.invalidate()
  }

  override def minWidth: Float = {
    computeSizeIfNeeded()
    _minWidth
  }

  override def minHeight: Float = {
    computeSizeIfNeeded()
    _minHeight
  }

  override def prefWidth: Float = {
    computeSizeIfNeeded()
    if (_vertical) layoutedWidth else relaxedWidth
  }

  override def prefHeight: Float = {
    computeSizeIfNeeded()
    if (_vertical) relaxedHeight else layoutedHeight
  }

  protected def computeSizeIfNeeded(): Unit =
    if (sizeInvalid) computeSize()
}
