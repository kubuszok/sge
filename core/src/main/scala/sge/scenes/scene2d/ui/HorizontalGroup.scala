/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/HorizontalGroup.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.graphics.glutils.ShapeRenderer
import sge.scenes.scene2d.utils.Layout
import sge.utils.{ Align, Nullable }

import scala.collection.mutable.ArrayBuffer

/** A group that lays out its children side by side horizontally, with optional wrapping. This can be easier than using {@link Table} when actors need to be inserted into or removed from the middle of
  * the group. {@link #getChildren()} can be sorted to change the order of the actors (eg {@link Actor#setZIndex(int)}). {@link #invalidate()} must be called after changing the children order. <p> The
  * preferred width is the sum of the children's preferred widths plus spacing. The preferred height is the largest preferred height of any child. The preferred size is slightly different when
  * {@link #wrap() wrap} is enabled. The min size is the preferred size and the max size is 0. <p> Widgets are sized using their {@link Layout#getPrefWidth() preferred width}, so widgets which return
  * 0 as their preferred width will be given a width of 0 (eg, a label with {@link Label#setWrap(boolean) word wrap} enabled).
  * @author
  *   Nathan Sweet
  */
class HorizontalGroup extends WidgetGroup {
  private var _prefWidth:     Float              = scala.compiletime.uninitialized
  private var _prefHeight:    Float              = scala.compiletime.uninitialized
  private var lastPrefHeight: Float              = scala.compiletime.uninitialized
  private var sizeInvalid:    Boolean            = true
  private var rowSizes:       ArrayBuffer[Float] = ArrayBuffer.empty // row width, row height, ...

  private var _align:       Align   = Align.left
  private var _rowAlign:    Align   = Align.center
  private var _reverse:     Boolean = false
  private var _round:       Boolean = true
  private var _wrap:        Boolean = false
  private var _wrapReverse: Boolean = false
  private var _expand:      Boolean = false
  private var _space:       Float   = 0
  private var _wrapSpace:   Float   = 0
  private var _fill:        Float   = 0
  private var _padTop:      Float   = 0
  private var _padLeft:     Float   = 0
  private var _padBottom:   Float   = 0
  private var _padRight:    Float   = 0

  setTransform(false)
  setTouchable(Touchable.childrenOnly)

  override def invalidate(): Unit = {
    super.invalidate()
    sizeInvalid = true
  }

  private def computeSize(): Unit = {
    sizeInvalid = false
    val children = getChildren
    val n        = children.size
    _prefHeight = 0
    if (_wrap) {
      _prefWidth = 0
      rowSizes.clear()
      val space      = this._space
      val wrapSpace  = this._wrapSpace
      val pad        = _padLeft + _padRight
      val groupWidth = getWidth - pad
      var x          = 0f
      var y          = 0f
      var rowHeight  = 0f
      var i          = if (_reverse) n - 1 else 0
      val end        = if (_reverse) -1 else n
      val incr       = if (_reverse) -1 else 1
      while (i != end) {
        val child = children(i)

        var width  = 0f
        var height = 0f
        child match {
          case layout: Layout =>
            width = layout.getPrefWidth
            if (width > groupWidth) width = Math.max(groupWidth, layout.getMinWidth)
            height = layout.getPrefHeight
          case _ =>
            width = child.getWidth
            height = child.getHeight
        }

        var incrX = width + (if (x > 0) space else 0)
        if (x + incrX > groupWidth && x > 0) {
          rowSizes += x
          rowSizes += rowHeight
          _prefWidth = Math.max(_prefWidth, x + pad)
          if (y > 0) y += wrapSpace
          y += rowHeight
          rowHeight = 0
          x = 0
          incrX = width
        }
        x += incrX
        rowHeight = Math.max(rowHeight, height)
        i += incr
      }
      rowSizes += x
      rowSizes += rowHeight
      _prefWidth = Math.max(_prefWidth, x + pad)
      if (y > 0) y += wrapSpace
      _prefHeight = Math.max(_prefHeight, y + rowHeight)
    } else {
      _prefWidth = _padLeft + _padRight + _space * (n - 1)
      var i = 0
      while (i < n) {
        val child = children(i)
        child match {
          case layout: Layout =>
            _prefWidth += layout.getPrefWidth
            _prefHeight = Math.max(_prefHeight, layout.getPrefHeight)
          case _ =>
            _prefWidth += child.getWidth
            _prefHeight = Math.max(_prefHeight, child.getHeight)
        }
        i += 1
      }
    }
    _prefHeight += _padTop + _padBottom
    if (_round) {
      _prefWidth = Math.ceil(_prefWidth.toDouble).toFloat
      _prefHeight = Math.ceil(_prefHeight.toDouble).toFloat
    }
  }

  override def layout(): Unit = {
    if (sizeInvalid) computeSize()

    if (_wrap) {
      layoutWrapped()
    } else {
      val round     = this._round
      val space     = this._space
      val padBottom = this._padBottom
      val fill      = this._fill
      val rowHeight = (if (_expand) getHeight else _prefHeight) - _padTop - padBottom
      var x         = _padLeft

      if (_align.isRight)
        x += getWidth - _prefWidth
      else if (!_align.isLeft) // center
        x += (getWidth - _prefWidth) / 2

      var startY = 0f
      if (_align.isBottom)
        startY = padBottom
      else if (_align.isTop)
        startY = getHeight - _padTop - rowHeight
      else
        startY = padBottom + (getHeight - padBottom - _padTop - rowHeight) / 2

      val align = _rowAlign

      val children = getChildren
      var i        = if (_reverse) children.size - 1 else 0
      val n        = if (_reverse) -1 else children.size
      val incr     = if (_reverse) -1 else 1
      while (i != n) {
        val child = children(i)

        var width  = 0f
        var height = 0f
        var layout: Nullable[Layout] = Nullable.empty
        child match {
          case l: Layout =>
            layout = Nullable(l)
            width = l.getPrefWidth
            height = l.getPrefHeight
          case _ =>
            width = child.getWidth
            height = child.getHeight
        }

        if (fill > 0) height = rowHeight * fill

        layout.foreach { l =>
          height = Math.max(height, l.getMinHeight)
          val maxHeight = l.getMaxHeight
          if (maxHeight > 0 && height > maxHeight) height = maxHeight
        }

        var y = startY
        if (align.isTop)
          y += rowHeight - height
        else if (!align.isBottom) // center
          y += (rowHeight - height) / 2

        if (round)
          child.setBounds(
            Math.floor(x.toDouble).toFloat,
            Math.floor(y.toDouble).toFloat,
            Math.ceil(width.toDouble).toFloat,
            Math.ceil(height.toDouble).toFloat
          )
        else
          child.setBounds(x, y, width, height)
        x += width + space

        layout.foreach(_.validate())
        i += incr
      }
    }
  }

  private def layoutWrapped(): Unit = {
    val prefHeight = getPrefHeight
    if (prefHeight != lastPrefHeight) {
      lastPrefHeight = prefHeight
      invalidateHierarchy()
    }

    val round      = this._round
    val space      = this._space
    val fill       = this._fill
    val wrapSpace  = this._wrapSpace
    val maxWidth   = _prefWidth - _padLeft - _padRight
    var rowY       = prefHeight - _padTop
    var groupWidth = getWidth
    val xStart     = _padLeft
    var x          = 0f
    var rowHeight  = 0f
    var rowDir     = -1f

    if (_align.isTop)
      rowY += getHeight - prefHeight
    else if (!_align.isBottom) // center
      rowY += (getHeight - prefHeight) / 2
    if (_wrapReverse) {
      rowY -= prefHeight + rowSizes(1)
      rowDir = 1
    }

    var xStartAdjusted = xStart
    if (_align.isRight)
      xStartAdjusted += groupWidth - _prefWidth
    else if (!_align.isLeft) // center
      xStartAdjusted += (groupWidth - _prefWidth) / 2

    groupWidth -= _padRight
    val align = this._rowAlign

    val children = getChildren
    var i        = if (_reverse) children.size - 1 else 0
    val n        = if (_reverse) -1 else children.size
    val incr     = if (_reverse) -1 else 1
    var r        = 0
    while (i != n) {
      val child = children(i)

      var width  = 0f
      var height = 0f
      var layout: Nullable[Layout] = Nullable.empty
      child match {
        case l: Layout =>
          layout = Nullable(l)
          width = l.getPrefWidth
          if (width > groupWidth) width = Math.max(groupWidth, l.getMinWidth)
          height = l.getPrefHeight
        case _ =>
          width = child.getWidth
          height = child.getHeight
      }

      if (x + width > groupWidth || r == 0) {
        r = Math.min(r, rowSizes.size - 2) // In case an actor changed size without invalidating this layout.
        x = xStartAdjusted
        if (align.isRight)
          x += maxWidth - rowSizes(r)
        else if (!align.isLeft) // center
          x += (maxWidth - rowSizes(r)) / 2
        rowHeight = rowSizes(r + 1)
        if (r > 0) rowY += wrapSpace * rowDir
        rowY += rowHeight * rowDir
        r += 2
      }

      if (fill > 0) height = rowHeight * fill

      layout.foreach { l =>
        height = Math.max(height, l.getMinHeight)
        val maxHeight = l.getMaxHeight
        if (maxHeight > 0 && height > maxHeight) height = maxHeight
      }

      var y = rowY
      if (align.isTop)
        y += rowHeight - height
      else if (!align.isBottom) // center
        y += (rowHeight - height) / 2

      if (round)
        child.setBounds(
          Math.floor(x.toDouble).toFloat,
          Math.floor(y.toDouble).toFloat,
          Math.ceil(width.toDouble).toFloat,
          Math.ceil(height.toDouble).toFloat
        )
      else
        child.setBounds(x, y, width, height)
      x += width + space

      layout.foreach(_.validate())
      i += incr
    }
  }

  override def getPrefWidth: Float =
    if (_wrap) 0
    else {
      if (sizeInvalid) computeSize()
      _prefWidth
    }

  override def getPrefHeight: Float = {
    if (sizeInvalid) computeSize()
    _prefHeight
  }

  /** When wrapping is enabled, the number of rows may be > 1. */
  def getRows: Int =
    if (_wrap) rowSizes.size >> 1 else 1

  /** If true (the default), positions and sizes are rounded to integers. */
  def setRound(round: Boolean): Unit =
    this._round = round

  /** The children will be displayed last to first. */
  def reverse(): HorizontalGroup = {
    _reverse = true
    this
  }

  /** If true, the children will be displayed last to first. */
  def reverse(reverse: Boolean): HorizontalGroup = {
    this._reverse = reverse
    this
  }

  def getReverse: Boolean = _reverse

  /** Rows will wrap above the previous rows. */
  def wrapReverse(): HorizontalGroup = {
    _wrapReverse = true
    this
  }

  /** If true, rows will wrap above the previous rows. */
  def wrapReverse(wrapReverse: Boolean): HorizontalGroup = {
    this._wrapReverse = wrapReverse
    this
  }

  def getWrapReverse: Boolean = _wrapReverse

  /** Sets the horizontal space between children. */
  def space(space: Float): HorizontalGroup = {
    this._space = space
    this
  }

  def getSpace: Float = _space

  /** Sets the vertical space between rows when wrap is enabled. */
  def wrapSpace(wrapSpace: Float): HorizontalGroup = {
    this._wrapSpace = wrapSpace
    this
  }

  def getWrapSpace: Float = _wrapSpace

  /** Sets the padTop, padLeft, padBottom, and padRight to the specified value. */
  def pad(pad: Float): HorizontalGroup = {
    _padTop = pad
    _padLeft = pad
    _padBottom = pad
    _padRight = pad
    this
  }

  def pad(top: Float, left: Float, bottom: Float, right: Float): HorizontalGroup = {
    _padTop = top
    _padLeft = left
    _padBottom = bottom
    _padRight = right
    this
  }

  def padTop(padTop: Float): HorizontalGroup = {
    this._padTop = padTop
    this
  }

  def padLeft(padLeft: Float): HorizontalGroup = {
    this._padLeft = padLeft
    this
  }

  def padBottom(padBottom: Float): HorizontalGroup = {
    this._padBottom = padBottom
    this
  }

  def padRight(padRight: Float): HorizontalGroup = {
    this._padRight = padRight
    this
  }

  def getPadTop: Float = _padTop

  def getPadLeft: Float = _padLeft

  def getPadBottom: Float = _padBottom

  def getPadRight: Float = _padRight

  /** Sets the alignment of all widgets within the horizontal group. Set to {@link Align#center}, {@link Align#top}, {@link Align#bottom}, {@link Align#left}, {@link Align#right}, or any combination
    * of those.
    */
  def align(align: Align): HorizontalGroup = {
    this._align = align
    this
  }

  /** Sets the alignment of all widgets within the horizontal group to {@link Align#center}. This clears any other alignment. */
  def center(): HorizontalGroup = {
    _align = Align.center
    this
  }

  /** Sets {@link Align#top} and clears {@link Align#bottom} for the alignment of all widgets within the horizontal group. */
  def top(): HorizontalGroup = {
    _align = (_align | Align.top) & (~Align.bottom)
    this
  }

  /** Adds {@link Align#left} and clears {@link Align#right} for the alignment of all widgets within the horizontal group. */
  def left(): HorizontalGroup = {
    _align = (_align | Align.left) & (~Align.right)
    this
  }

  /** Sets {@link Align#bottom} and clears {@link Align#top} for the alignment of all widgets within the horizontal group. */
  def bottom(): HorizontalGroup = {
    _align = (_align | Align.bottom) & (~Align.top)
    this
  }

  /** Adds {@link Align#right} and clears {@link Align#left} for the alignment of all widgets within the horizontal group. */
  def right(): HorizontalGroup = {
    _align = (_align | Align.right) & (~Align.left)
    this
  }

  def getAlign: Align = _align

  def fill(): HorizontalGroup = {
    _fill = 1f
    this
  }

  /** @param fill 0 will use preferred width. */
  def fill(fill: Float): HorizontalGroup = {
    this._fill = fill
    this
  }

  def getFill: Float = _fill

  def expand(): HorizontalGroup = {
    _expand = true
    this
  }

  /** When true and wrap is false, the rows will take up the entire horizontal group height. */
  def expand(expand: Boolean): HorizontalGroup = {
    this._expand = expand
    this
  }

  def getExpand: Boolean = _expand

  /** Sets fill to 1 and expand to true. */
  def grow(): HorizontalGroup = {
    _expand = true
    _fill = 1
    this
  }

  /** If false, the widgets are arranged in a single row and the preferred width is the widget widths plus spacing. <p> If true, the widgets will wrap using the width of the horizontal group. The
    * preferred width of the group will be 0 as it is expected that something external will set the width of the group. Widgets are sized to their preferred width unless it is larger than the group's
    * width, in which case they are sized to the group's width but not less than their minimum width. Default is false. <p> When wrap is enabled, the group's preferred height depends on the width of
    * the group. In some cases the parent of the group will need to layout twice: once to set the width of the group and a second time to adjust to the group's new preferred height.
    */
  def wrap(): HorizontalGroup = {
    _wrap = true
    this
  }

  def wrap(wrap: Boolean): HorizontalGroup = {
    this._wrap = wrap
    this
  }

  def getWrap: Boolean = _wrap

  /** Sets the horizontal alignment of each row of widgets when {@link #wrap() wrapping} is enabled and sets the vertical alignment of widgets within each row. Set to {@link Align#center},
    * {@link Align#top}, {@link Align#bottom}, {@link Align#left}, {@link Align#right}, or any combination of those.
    */
  def rowAlign(rowAlign: Align): HorizontalGroup = {
    this._rowAlign = rowAlign
    this
  }

  /** Sets the alignment of widgets within each row to {@link Align#center}. This clears any other alignment. */
  def rowCenter(): HorizontalGroup = {
    _rowAlign = Align.center
    this
  }

  /** Sets {@link Align#top} and clears {@link Align#bottom} for the alignment of widgets within each row. */
  def rowTop(): HorizontalGroup = {
    _rowAlign = (_rowAlign | Align.top) & (~Align.bottom)
    this
  }

  /** Adds {@link Align#left} and clears {@link Align#right} for the alignment of each row of widgets when {@link #wrap() wrapping} is enabled.
    */
  def rowLeft(): HorizontalGroup = {
    _rowAlign = (_rowAlign | Align.left) & (~Align.right)
    this
  }

  /** Sets {@link Align#bottom} and clears {@link Align#top} for the alignment of widgets within each row. */
  def rowBottom(): HorizontalGroup = {
    _rowAlign = (_rowAlign | Align.bottom) & (~Align.top)
    this
  }

  /** Adds {@link Align#right} and clears {@link Align#left} for the alignment of each row of widgets when {@link #wrap() wrapping} is enabled.
    */
  def rowRight(): HorizontalGroup = {
    _rowAlign = (_rowAlign | Align.right) & (~Align.left)
    this
  }

  override protected def drawDebugBounds(shapes: ShapeRenderer): Unit = {
    super.drawDebugBounds(shapes)
    if (getDebug) {
      shapes.set(shapes.ShapeType.Line)
      getStage.foreach(s => shapes.setColor(s.getDebugColor))
      // TODO: uncomment when ShapeRenderer.rectangle with transform params is ported
      // shapes.rectangle(getX + _padLeft, getY + _padBottom, getOriginX, getOriginY, getWidth - _padLeft - _padRight,
      //   getHeight - _padBottom - _padTop, getScaleX, getScaleY, getRotation)
      shapes.rectangle(getX + _padLeft, getY + _padBottom, getWidth - _padLeft - _padRight, getHeight - _padBottom - _padTop)
    }
  }
}
