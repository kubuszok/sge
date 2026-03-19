/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/VerticalGroup.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Align opaque type; Nullable[DynamicArray[Float]] for columnSizes (lazy init)
 *   Idiom: split packages
 *   Note: getXyz getters retained — fluent builder pattern (space(v), pad(v), fill(v), wrap(v)) conflicts with Scala var naming
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.graphics.glutils.ShapeRenderer
import sge.scenes.scene2d.utils.Layout
import sge.utils.{ Align, DynamicArray, Nullable }

/** A group that lays out its children top to bottom vertically, with optional wrapping. {@link #getChildren()} can be sorted to change the order of the actors (eg {@link Actor#setZIndex(int)}). This
  * can be easier than using {@link Table} when actors need to be inserted into or removed from the middle of the group. {@link #invalidate()} must be called after changing the children order.
  *
  * The preferred width is the largest preferred width of any child. The preferred height is the sum of the children's preferred heights plus spacing. The preferred size is slightly different when
  * {@link #wrap() wrap} is enabled. The min size is the preferred size and the max size is 0.
  *
  * Widgets are sized using their {@link Layout#prefWidth preferred height}, so widgets which return 0 as their preferred height will be given a height of 0.
  * @author
  *   Nathan Sweet
  */
class VerticalGroup()(using Sge) extends WidgetGroup() {

  private var _prefWidth:    Float                         = 0
  private var _prefHeight:   Float                         = 0
  private var lastPrefWidth: Float                         = 0
  private var sizeInvalid:   Boolean                       = true
  private var columnSizes:   Nullable[DynamicArray[Float]] = Nullable.empty // column height, column width, ...

  private var _align:       Align   = Align.top
  private var _columnAlign: Align   = Align.center
  private var _reverse:     Boolean = false
  private var _round:       Boolean = true
  private var _wrap:        Boolean = false
  private var _expand:      Boolean = false
  private var _space:       Float   = 0
  private var _wrapSpace:   Float   = 0
  private var _fill:        Float   = 0
  private var _padTop:      Float   = 0
  private var _padLeft:     Float   = 0
  private var _padBottom:   Float   = 0
  private var _padRight:    Float   = 0

  transform = false
  touchable = Touchable.childrenOnly

  override def invalidate(): Unit = {
    super.invalidate()
    sizeInvalid = true
  }

  private def computeSize(): Unit = {
    sizeInvalid = false
    val children = this.children
    val n        = children.size
    _prefWidth = 0
    if (_wrap) {
      _prefHeight = 0
      val colSizes = columnSizes.getOrElse {
        val buf = DynamicArray[Float]()
        columnSizes = Nullable(buf)
        buf
      }
      colSizes.clear()
      val space       = this._space
      val wrapSpace   = this._wrapSpace
      val pad         = _padTop + _padBottom
      val groupHeight = height - pad
      var x           = 0f
      var y           = 0f
      var columnWidth = 0f
      var i           = if (_reverse) n - 1 else 0
      val end         = if (_reverse) -1 else n
      val incr        = if (_reverse) -1 else 1
      while (i != end) {
        val child = children(i)

        var width:  Float = 0
        var height: Float = 0
        child match {
          case layout: Layout =>
            width = layout.prefWidth
            height = layout.prefHeight
            if (height > groupHeight) height = Math.max(groupHeight, layout.minHeight)
          case _ =>
            width = child.width
            height = child.height
        }

        val incrY0 = height + (if (y > 0) space else 0)
        var incrY  = incrY0
        if (y + incrY > groupHeight && y > 0) {
          colSizes.add(y)
          colSizes.add(columnWidth)
          _prefHeight = Math.max(_prefHeight, y + pad)
          if (x > 0) x += wrapSpace
          x += columnWidth
          columnWidth = 0
          y = 0
          incrY = height
        }
        y += incrY
        columnWidth = Math.max(columnWidth, width)
        i += incr
      }
      colSizes.add(y)
      colSizes.add(columnWidth)
      _prefHeight = Math.max(_prefHeight, y + pad)
      if (x > 0) x += wrapSpace
      _prefWidth = Math.max(_prefWidth, x + columnWidth)
    } else {
      _prefHeight = _padTop + _padBottom + _space * (n - 1)
      var i = 0
      while (i < n) {
        val child = children(i)
        child match {
          case layout: Layout =>
            _prefWidth = Math.max(_prefWidth, layout.prefWidth)
            _prefHeight += layout.prefHeight
          case _ =>
            _prefWidth = Math.max(_prefWidth, child.width)
            _prefHeight += child.height
        }
        i += 1
      }
    }
    _prefWidth += _padLeft + _padRight
    if (_round) {
      _prefWidth = Math.ceil(_prefWidth).toFloat
      _prefHeight = Math.ceil(_prefHeight).toFloat
    }
  }

  override def layout(): Unit = {
    if (sizeInvalid) computeSize()

    if (_wrap) {
      layoutWrapped()
    } else {
      val round       = this._round
      var align       = this._align
      val space       = this._space
      val padLeft     = this._padLeft
      val fill        = this._fill
      val columnWidth = (if (_expand) width else _prefWidth) - _padLeft - _padRight
      var y           = _prefHeight - _padTop + space

      if (align.isTop)
        y += height - _prefHeight
      else if (!align.isBottom) // center
        y += (height - _prefHeight) / 2

      var startX: Float = 0
      if (align.isLeft)
        startX = padLeft
      else if (align.isRight)
        startX = width - _padRight - columnWidth
      else
        startX = padLeft + (width - _padLeft - _padRight - columnWidth) / 2

      align = _columnAlign

      val children = this.children
      val n        = children.size
      var i        = if (_reverse) n - 1 else 0
      val end      = if (_reverse) -1 else n
      val incr     = if (_reverse) -1 else 1
      while (i != end) {
        val child = children(i)

        var width:  Float            = 0
        var height: Float            = 0
        var layout: Nullable[Layout] = Nullable.empty
        child match {
          case l: Layout =>
            layout = Nullable(l)
            width = l.prefWidth
            height = l.prefHeight
          case _ =>
            width = child.width
            height = child.height
        }

        if (fill > 0) width = columnWidth * fill

        layout.foreach { l =>
          width = Math.max(width, l.minWidth)
          val maxWidth = l.maxWidth
          if (maxWidth > 0 && width > maxWidth) width = maxWidth
        }

        var x = startX
        if (align.isRight)
          x += columnWidth - width
        else if (!align.isLeft) // center
          x += (columnWidth - width) / 2

        y -= height + space
        if (round)
          child.setBounds(Math.floor(x).toFloat, Math.floor(y).toFloat, Math.ceil(width).toFloat, Math.ceil(height).toFloat)
        else
          child.setBounds(x, y, width, height)

        layout.foreach(_.validate())
        i += incr
      }
    }
  }

  private def layoutWrapped(): Unit = {
    val prefWidth = this.prefWidth
    if (prefWidth != lastPrefWidth) {
      lastPrefWidth = prefWidth
      invalidateHierarchy()
    }

    var align       = this._align
    val round       = this._round
    val space       = this._space
    val padLeft     = this._padLeft
    val fill        = this._fill
    val wrapSpace   = this._wrapSpace
    val maxHeight   = _prefHeight - _padTop - _padBottom
    var columnX     = padLeft
    var groupHeight = height
    var yStart      = _prefHeight - _padTop + space
    var y           = 0f
    var columnWidth = 0f

    if (align.isRight)
      columnX += width - prefWidth
    else if (!align.isLeft) // center
      columnX += (width - prefWidth) / 2

    if (align.isTop)
      yStart += groupHeight - _prefHeight
    else if (!align.isBottom) // center
      yStart += (groupHeight - _prefHeight) / 2

    groupHeight -= _padTop
    align = _columnAlign

    val colSizes = columnSizes.getOrElse(DynamicArray[Float]())
    val children = this.children
    val n        = children.size
    var i        = if (_reverse) n - 1 else 0
    val end      = if (_reverse) -1 else n
    val incr     = if (_reverse) -1 else 1
    var r        = 0
    while (i != end) {
      val child = children(i)

      var width:  Float            = 0
      var height: Float            = 0
      var layout: Nullable[Layout] = Nullable.empty
      child match {
        case l: Layout =>
          layout = Nullable(l)
          width = l.prefWidth
          height = l.prefHeight
          if (height > groupHeight) height = Math.max(groupHeight, l.minHeight)
        case _ =>
          width = child.width
          height = child.height
      }

      if (y - height - space < _padBottom || r == 0) {
        r = Math.min(r, colSizes.size - 2) // In case an actor changed size without invalidating this layout.
        y = yStart
        if (align.isBottom)
          y -= maxHeight - colSizes(r)
        else if (!align.isTop) // center
          y -= (maxHeight - colSizes(r)) / 2
        if (r > 0) {
          columnX += wrapSpace
          columnX += columnWidth
        }
        columnWidth = colSizes(r + 1)
        r += 2
      }

      if (fill > 0) width = columnWidth * fill

      layout.foreach { l =>
        width = Math.max(width, l.minWidth)
        val maxWidth = l.maxWidth
        if (maxWidth > 0 && width > maxWidth) width = maxWidth
      }

      var x = columnX
      if (align.isRight)
        x += columnWidth - width
      else if (!align.isLeft) // center
        x += (columnWidth - width) / 2

      y -= height + space
      if (round)
        child.setBounds(Math.floor(x).toFloat, Math.floor(y).toFloat, Math.ceil(width).toFloat, Math.ceil(height).toFloat)
      else
        child.setBounds(x, y, width, height)

      layout.foreach(_.validate())
      i += incr
    }
  }

  override def prefWidth: Float = {
    if (sizeInvalid) computeSize()
    _prefWidth
  }

  override def prefHeight: Float =
    if (_wrap) 0
    else {
      if (sizeInvalid) computeSize()
      _prefHeight
    }

  /** When wrapping is enabled, the number of columns may be > 1. */
  def columns: Int =
    if (_wrap) columnSizes.map(_.size >> 1).getOrElse(0) else 1

  /** If true (the default), positions and sizes are rounded to integers. */
  def setRound(round: Boolean): Unit =
    this._round = round

  /** The children will be displayed last to first. */
  def reverse(): VerticalGroup = {
    this._reverse = true
    this
  }

  /** If true, the children will be displayed last to first. */
  def reverse(reverse: Boolean): VerticalGroup = {
    this._reverse = reverse
    this
  }

  def isReverse: Boolean = _reverse

  /** Sets the vertical space between children. */
  def space(space: Float): VerticalGroup = {
    this._space = space
    this
  }

  def space: Float = _space

  /** Sets the horizontal space between columns when wrap is enabled. */
  def wrapSpace(wrapSpace: Float): VerticalGroup = {
    this._wrapSpace = wrapSpace
    this
  }

  def wrapSpace: Float = _wrapSpace

  /** Sets the padTop, padLeft, padBottom, and padRight to the specified value. */
  def pad(pad: Float): VerticalGroup = {
    _padTop = pad
    _padLeft = pad
    _padBottom = pad
    _padRight = pad
    this
  }

  def pad(top: Float, left: Float, bottom: Float, right: Float): VerticalGroup = {
    _padTop = top
    _padLeft = left
    _padBottom = bottom
    _padRight = right
    this
  }

  def padTop(padTop: Float): VerticalGroup = {
    this._padTop = padTop
    this
  }

  def padLeft(padLeft: Float): VerticalGroup = {
    this._padLeft = padLeft
    this
  }

  def padBottom(padBottom: Float): VerticalGroup = {
    this._padBottom = padBottom
    this
  }

  def padRight(padRight: Float): VerticalGroup = {
    this._padRight = padRight
    this
  }

  def getPadTop: Float = _padTop

  def getPadLeft: Float = _padLeft

  def getPadBottom: Float = _padBottom

  def getPadRight: Float = _padRight

  /** Sets the alignment of all widgets within the vertical group. Set to {@link Align#center}, {@link Align#top}, {@link Align#bottom}, {@link Align#left}, {@link Align#right}, or any combination of
    * those.
    */
  def align(align: Align): VerticalGroup = {
    this._align = align
    this
  }

  /** Sets the alignment of all widgets within the vertical group to {@link Align#center}. This clears any other alignment. */
  def center(): VerticalGroup = {
    _align = Align.center
    this
  }

  /** Sets {@link Align#top} and clears {@link Align#bottom} for the alignment of all widgets within the vertical group. */
  def alignTop(): VerticalGroup = {
    _align = _align | Align.top
    _align = _align & ~Align.bottom
    this
  }

  /** Adds {@link Align#left} and clears {@link Align#right} for the alignment of all widgets within the vertical group. */
  def left(): VerticalGroup = {
    _align = _align | Align.left
    _align = _align & ~Align.right
    this
  }

  /** Sets {@link Align#bottom} and clears {@link Align#top} for the alignment of all widgets within the vertical group. */
  def bottom(): VerticalGroup = {
    _align = _align | Align.bottom
    _align = _align & ~Align.top
    this
  }

  /** Adds {@link Align#right} and clears {@link Align#left} for the alignment of all widgets within the vertical group. */
  def alignRight(): VerticalGroup = {
    _align = _align | Align.right
    _align = _align & ~Align.left
    this
  }

  def align: Align = _align

  def fill(): VerticalGroup = {
    _fill = 1f
    this
  }

  /** @param fill 0 will use preferred height. */
  def fill(fill: Float): VerticalGroup = {
    this._fill = fill
    this
  }

  def getFill: Float = _fill

  def expand(): VerticalGroup = {
    _expand = true
    this
  }

  /** When true and wrap is false, the columns will take up the entire vertical group width. */
  def expand(expand: Boolean): VerticalGroup = {
    this._expand = expand
    this
  }

  def isExpand: Boolean = _expand

  /** Sets fill to 1 and expand to true. */
  def grow(): VerticalGroup = {
    _expand = true
    _fill = 1
    this
  }

  /** If false, the widgets are arranged in a single column and the preferred height is the widget heights plus spacing.
    *
    * If true, the widgets will wrap using the height of the vertical group. The preferred height of the group will be 0 as it is expected that something external will set the height of the group.
    * Widgets are sized to their preferred height unless it is larger than the group's height, in which case they are sized to the group's height but not less than their minimum height. Default is
    * false.
    *
    * When wrap is enabled, the group's preferred width depends on the height of the group. In some cases the parent of the group will need to layout twice: once to set the height of the group and a
    * second time to adjust to the group's new preferred width.
    */
  def wrap(): VerticalGroup = {
    _wrap = true
    this
  }

  def wrap(wrap: Boolean): VerticalGroup = {
    this._wrap = wrap
    this
  }

  def isWrap: Boolean = _wrap

  /** Sets the vertical alignment of each column of widgets when {@link #wrap() wrapping} is enabled and sets the horizontal alignment of widgets within each column. Set to {@link Align#center},
    * {@link Align#top}, {@link Align#bottom}, {@link Align#left}, {@link Align#right}, or any combination of those.
    */
  def columnAlign(columnAlign: Align): VerticalGroup = {
    this._columnAlign = columnAlign
    this
  }

  /** Sets the alignment of widgets within each column to {@link Align#center}. This clears any other alignment. */
  def columnCenter(): VerticalGroup = {
    _columnAlign = Align.center
    this
  }

  /** Adds {@link Align#top} and clears {@link Align#bottom} for the alignment of each column of widgets when {@link #wrap() wrapping} is enabled.
    */
  def columnTop(): VerticalGroup = {
    _columnAlign = _columnAlign | Align.top
    _columnAlign = _columnAlign & ~Align.bottom
    this
  }

  /** Adds {@link Align#left} and clears {@link Align#right} for the alignment of widgets within each column. */
  def columnLeft(): VerticalGroup = {
    _columnAlign = _columnAlign | Align.left
    _columnAlign = _columnAlign & ~Align.right
    this
  }

  /** Adds {@link Align#bottom} and clears {@link Align#top} for the alignment of each column of widgets when {@link #wrap() wrapping} is enabled.
    */
  def columnBottom(): VerticalGroup = {
    _columnAlign = _columnAlign | Align.bottom
    _columnAlign = _columnAlign & ~Align.top
    this
  }

  /** Adds {@link Align#right} and clears {@link Align#left} for the alignment of widgets within each column. */
  def columnRight(): VerticalGroup = {
    _columnAlign = _columnAlign | Align.right
    _columnAlign = _columnAlign & ~Align.left
    this
  }

  override protected def drawDebugBounds(shapes: ShapeRenderer): Unit = {
    super.drawDebugBounds(shapes)
    if (isDebug) {
      shapes.set(ShapeRenderer.ShapeType.Line)
      stage.foreach(s => shapes.color.set(s.debugColor))
      shapes.rectangle(
        x + _padLeft,
        y + _padBottom,
        originX,
        originY,
        width - _padLeft - _padRight,
        height - _padBottom - _padTop,
        scaleX,
        scaleY,
        rotation
      )
    }
  }
}
