/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 151
 * Covenant-baseline-methods: GridGroup,_itemHeight,_itemWidth,_prefHeight,_prefWidth,_spacing,childArr,computeSize,i,invalidate,itemHeight,itemHeight_,itemWidth,itemWidth_,lastPrefHeight,layout,maxHeight,notEnoughSpace,prefHeight,prefWidth,setItemSize,sizeInvalid,spacing,spacing_,tempX,this,w,x,y
 * Covenant-source-reference: com/kotcrab/vis/ui/layout/GridGroup.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package layout

import sge.scenes.scene2d.Touchable
import sge.scenes.scene2d.ui.WidgetGroup

/** Arrange actors in grid layout. You can set item width, height and spacing between items.
  *
  * Grid group can be embedded in scroll pane. However in such case scrolling in X direction must be disabled.
  * @author
  *   Kotcrab
  * @since 0.7.2
  */
class GridGroup()(using Sge) extends WidgetGroup() {
  private var _prefWidth:     Float   = 0f
  private var _prefHeight:    Float   = 0f
  private var lastPrefHeight: Float   = 0f
  private var sizeInvalid:    Boolean = true

  private var _itemWidth:  Float = 256f
  private var _itemHeight: Float = 256f
  private var _spacing:    Float = 8f

  touchable = Touchable.childrenOnly

  def this(itemSize: Float)(using Sge) = {
    this()
    _itemWidth = itemSize
    _itemHeight = itemSize
  }

  def this(itemSize: Float, spacing: Float)(using Sge) = {
    this()
    _spacing = spacing
    _itemWidth = itemSize
    _itemHeight = itemSize
  }

  private def computeSize(): Unit = {
    _prefWidth = width
    _prefHeight = 0
    sizeInvalid = false

    val childArr = children

    if (childArr.size == 0) {
      _prefWidth = 0
      _prefHeight = 0
      return
    }

    val w         = width
    var maxHeight = 0f
    var tempX     = _spacing

    var i = 0
    while (i < childArr.size) {
      if (tempX + _itemWidth + _spacing > w) {
        tempX = _spacing
        maxHeight += _itemHeight + _spacing
      }
      tempX += _itemWidth + _spacing
      i += 1
    }

    if (_itemWidth + _spacing * 2 > _prefWidth) {
      maxHeight += _spacing
    } else {
      maxHeight += _itemHeight + _spacing * 2
    }

    _prefHeight = maxHeight
  }

  override def layout(): Unit = {
    if (sizeInvalid) {
      computeSize()
      if (lastPrefHeight != _prefHeight) {
        lastPrefHeight = _prefHeight
        invalidateHierarchy()
      }
    }

    val childArr       = children
    val w              = width
    val notEnoughSpace = _itemWidth + _spacing * 2 > w

    var x = _spacing
    var y = if (notEnoughSpace) height else height - _itemHeight - _spacing

    var i = 0
    while (i < childArr.size) {
      val child = childArr(i)
      if (x + _itemWidth + _spacing > w) {
        x = _spacing
        y -= _itemHeight + _spacing
      }
      child.setBounds(x, y, _itemWidth, _itemHeight)
      x += _itemWidth + _spacing
      i += 1
    }
  }

  def spacing: Float = _spacing

  def spacing_=(spacing: Float): Unit = {
    _spacing = spacing
    invalidateHierarchy()
  }

  def setItemSize(itemSize: Float): Unit = {
    _itemWidth = itemSize
    _itemHeight = itemSize
    invalidateHierarchy()
  }

  def setItemSize(itemWidth: Float, itemHeight: Float): Unit = {
    _itemWidth = itemWidth
    _itemHeight = itemHeight
    invalidateHierarchy()
  }

  def itemWidth:                     Float = _itemWidth
  def itemWidth_=(itemWidth: Float): Unit  = _itemWidth = itemWidth

  def itemHeight:                      Float = _itemHeight
  def itemHeight_=(itemHeight: Float): Unit  = _itemHeight = itemHeight

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
