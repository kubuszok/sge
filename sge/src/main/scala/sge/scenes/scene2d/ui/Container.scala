/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Container.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: null -> Nullable; Align opaque type
 *   Idiom: split packages
 *   Note: Java-style getters/setters retained — fluent builder pattern (returns Container[T] for chaining) conflicts with Scala property naming
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.graphics.g2d.Batch
import sge.graphics.glutils.ShapeRenderer
import sge.math.Rectangle
import sge.scenes.scene2d.{ Actor, Touchable }
import sge.scenes.scene2d.ui.Value.Fixed
import sge.scenes.scene2d.utils.{ Cullable, Drawable, Layout }
import sge.utils.{ Align, Nullable }

import scala.language.implicitConversions

/** A group with a single child that sizes and positions the child using constraints. This provides layout similar to a {@link Table} with a single cell but is more lightweight.
  * @author
  *   Nathan Sweet
  */
class Container[T <: Actor]()(using Sge) extends WidgetGroup() {

  private var actor:        Nullable[T]         = Nullable.empty
  private var _minWidth:    Value               = Value.minWidth
  private var _minHeight:   Value               = Value.minHeight
  private var _prefWidth:   Value               = Value.prefWidth
  private var _prefHeight:  Value               = Value.prefHeight
  private var _maxWidth:    Value               = Value.zero
  private var _maxHeight:   Value               = Value.zero
  private var _padTop:      Value               = Value.zero
  private var _padLeft:     Value               = Value.zero
  private var _padBottom:   Value               = Value.zero
  private var _padRight:    Value               = Value.zero
  private var _fillX:       Float               = 0f
  private var _fillY:       Float               = 0f
  private var _align:       Align               = Align.center
  private var _background:  Nullable[Drawable]  = Nullable.empty
  private var _clip:        Boolean             = false
  private var _round:       Boolean             = true
  private var actorCulling: Nullable[Rectangle] = Nullable.empty

  touchable = Touchable.childrenOnly
  setTransform(false)

  def this(actor: Nullable[T])(using Sge) = {
    this()
    setActor(actor)
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    validate()
    if (transform) {
      applyTransform(batch, computeTransform())
      drawBackground(batch, parentAlpha, 0, 0)
      if (_clip) {
        batch.flush()
        val padLeft   = this._padLeft.get(this)
        val padBottom = this._padBottom.get(this)
        if (clipBegin(padLeft, padBottom, width - padLeft - _padRight.get(this), height - padBottom - _padTop.get(this))) {
          drawChildren(batch, parentAlpha)
          batch.flush()
          clipEnd()
        }
      } else {
        drawChildren(batch, parentAlpha)
      }
      resetTransform(batch)
    } else {
      drawBackground(batch, parentAlpha, x, y)
      super.draw(batch, parentAlpha)
    }
  }

  /** Called to draw the background, before clipping is applied (if enabled). Default implementation draws the background drawable.
    */
  protected def drawBackground(batch: Batch, parentAlpha: Float, x: Float, y: Float): Unit =
    _background.foreach { bg =>
      val color = this.color
      batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
      bg.draw(batch, x, y, width, height)
    }

  /** Sets the background drawable and adjusts the container's padding to match the background.
    * @see
    *   #setBackground(Drawable, boolean)
    */
  def setBackground(background: Nullable[Drawable]): Unit =
    setBackground(background, true)

  /** Sets the background drawable and, if adjustPadding is true, sets the container's padding to {@link Drawable#getBottomHeight()} , {@link Drawable#getTopHeight()}, {@link Drawable#getLeftWidth()},
    * and {@link Drawable#getRightWidth()}.
    * @param background
    *   If null, the background will be cleared and padding removed.
    */
  def setBackground(background: Nullable[Drawable], adjustPadding: Boolean): Unit =
    if (this._background != background) {
      this._background = background
      if (adjustPadding) {
        background.fold {
          pad(Value.zero)
        } { bg =>
          pad(bg.topHeight, bg.leftWidth, bg.bottomHeight, bg.rightWidth)
        }
        invalidate()
      }
    }

  /** @see #setBackground(Drawable) */
  def background(background: Nullable[Drawable]): Container[T] = {
    setBackground(background)
    this
  }

  def background: Nullable[Drawable] = _background

  override def layout(): Unit =
    actor.foreach { a =>
      val padLeft         = this._padLeft.get(this)
      val padBottom       = this._padBottom.get(this)
      val containerWidth  = this.width - padLeft - _padRight.get(this)
      val containerHeight = this.height - padBottom - _padTop.get(this)
      val minW            = this._minWidth.get(a)
      val minH            = this._minHeight.get(a)
      val prefW           = this._prefWidth.get(a)
      val prefH           = this._prefHeight.get(a)
      val maxW            = this._maxWidth.get(a)
      val maxH            = this._maxHeight.get(a)

      var cw =
        if (_fillX > 0) containerWidth * _fillX
        else Math.min(prefW, containerWidth)
      if (cw < minW) cw = minW
      if (maxW > 0 && cw > maxW) cw = maxW

      var ch =
        if (_fillY > 0) containerHeight * _fillY
        else Math.min(prefH, containerHeight)
      if (ch < minH) ch = minH
      if (maxH > 0 && ch > maxH) ch = maxH

      var cx = padLeft
      if (_align.isRight)
        cx += containerWidth - cw
      else if (_align.isCenterHorizontal) // center
        cx += (containerWidth - cw) / 2

      var cy = padBottom
      if (_align.isTop)
        cy += containerHeight - ch
      else if (_align.isCenterVertical) // center
        cy += (containerHeight - ch) / 2

      if (_round) {
        cx = Math.floor(cx.toDouble).toFloat
        cy = Math.floor(cy.toDouble).toFloat
        cw = Math.ceil(cw.toDouble).toFloat
        ch = Math.ceil(ch.toDouble).toFloat
      }

      a.setBounds(cx, cy, cw, ch)
      a match {
        case l: Layout => l.validate()
        case _ =>
      }
    }

  override def setCullingArea(cullingArea: Nullable[Rectangle]): Unit = {
    super.setCullingArea(cullingArea)
    actor.foreach { a =>
      a match {
        case cullable: Cullable =>
          cullingArea.fold {
            cullable.setCullingArea(Nullable.empty)
          } { ca =>
            val ac = actorCulling.getOrElse {
              val r = Rectangle()
              actorCulling = Nullable(r)
              r
            }
            ac.x = ca.x - a.x
            ac.y = ca.y - a.y
            ac.width = ca.width
            ac.height = ca.height
            cullable.setCullingArea(Nullable(ac))
          }
        case _ =>
      }
    }
  }

  /** @param actor May be null. */
  def setActor(actor: Nullable[T]): Unit = {
    actor.foreach { a =>
      if (a.asInstanceOf[Actor] eq this) throw new IllegalArgumentException("actor cannot be the Container.")
    }
    this.actor.foreach { a =>
      super.removeActor(a)
    }
    this.actor = actor
    actor.foreach { a =>
      super.addActor(a)
    }
  }

  /** @return May be null. */
  def getActor: Nullable[T] = actor

  /** @deprecated
    *   Container may have only a single child.
    * @see
    *   #setActor(Actor)
    */
  // @deprecated("Use Container#setActor.", "")
  override def addActor(actor: Actor): Unit =
    throw new UnsupportedOperationException("Use Container#setActor.")

  /** @deprecated
    *   Container may have only a single child.
    * @see
    *   #setActor(Actor)
    */
  // @deprecated("Use Container#setActor.", "")
  override def addActorAt(index: Int, actor: Actor): Unit =
    throw new UnsupportedOperationException("Use Container#setActor.")

  /** @deprecated
    *   Container may have only a single child.
    * @see
    *   #setActor(Actor)
    */
  // @deprecated("Use Container#setActor.", "")
  override def addActorBefore(actorBefore: Actor, actor: Actor): Unit =
    throw new UnsupportedOperationException("Use Container#setActor.")

  /** @deprecated
    *   Container may have only a single child.
    * @see
    *   #setActor(Actor)
    */
  // @deprecated("Use Container#setActor.", "")
  override def addActorAfter(actorAfter: Actor, actor: Actor): Unit =
    throw new UnsupportedOperationException("Use Container#setActor.")

  override def removeActor(actor: Actor): Boolean =
    if (!this.actor.exists(_ eq actor)) false
    else {
      setActor(Nullable.empty)
      true
    }

  override def removeActor(actor: Actor, unfocus: Boolean): Boolean =
    if (!this.actor.exists(_ eq actor)) false
    else {
      this.actor = Nullable.empty
      super.removeActor(actor, unfocus)
    }

  override def removeActorAt(index: Int, unfocus: Boolean): Actor = {
    val removed = super.removeActorAt(index, unfocus)
    if (this.actor.exists(_ eq removed)) this.actor = Nullable.empty
    removed
  }

  // --- Fluent builder methods ---

  /** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified value. */
  def size(size: Value): Container[T] = {
    _minWidth = size; _minHeight = size
    _prefWidth = size; _prefHeight = size
    _maxWidth = size; _maxHeight = size
    this
  }

  /** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified values. */
  def size(width: Value, height: Value): Container[T] = {
    _minWidth = width; _minHeight = height
    _prefWidth = width; _prefHeight = height
    _maxWidth = width; _maxHeight = height
    this
  }

  /** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified value. */
  def size(size: Float): Container[T] = { this.size(Fixed.valueOf(size)); this }

  /** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified values. */
  def size(width: Float, height: Float): Container[T] = { this.size(Fixed.valueOf(width), Fixed.valueOf(height)); this }

  /** Sets the minWidth, prefWidth, and maxWidth to the specified value. */
  def width(width: Value): Container[T] = {
    _minWidth = width; _prefWidth = width; _maxWidth = width
    this
  }

  /** Sets the minWidth, prefWidth, and maxWidth to the specified value. */
  def width(width: Float): Container[T] = { this.width(Fixed.valueOf(width)); this }

  /** Sets the minHeight, prefHeight, and maxHeight to the specified value. */
  def height(height: Value): Container[T] = {
    _minHeight = height; _prefHeight = height; _maxHeight = height
    this
  }

  /** Sets the minHeight, prefHeight, and maxHeight to the specified value. */
  def height(height: Float): Container[T] = { this.height(Fixed.valueOf(height)); this }

  /** Sets the minWidth and minHeight to the specified value. */
  def minSize(size: Value): Container[T] = {
    _minWidth = size; _minHeight = size; this
  }

  /** Sets the minWidth and minHeight to the specified values. */
  def minSize(width: Value, height: Value): Container[T] = {
    _minWidth = width; _minHeight = height; this
  }

  def minWidth(minWidth: Value): Container[T] = {
    this._minWidth = minWidth; this
  }

  def minHeight(minHeight: Value): Container[T] = {
    this._minHeight = minHeight; this
  }

  /** Sets the minWidth and minHeight to the specified value. */
  def minSize(size: Float): Container[T] = { minSize(Fixed.valueOf(size)); this }

  /** Sets the minWidth and minHeight to the specified values. */
  def minSize(width: Float, height: Float): Container[T] = { minSize(Fixed.valueOf(width), Fixed.valueOf(height)); this }

  def minWidth(minWidth: Float): Container[T] = { this._minWidth = Fixed.valueOf(minWidth); this }

  def minHeight(minHeight: Float): Container[T] = { this._minHeight = Fixed.valueOf(minHeight); this }

  /** Sets the prefWidth and prefHeight to the specified value. */
  def prefSize(size: Value): Container[T] = {
    _prefWidth = size; _prefHeight = size; this
  }

  /** Sets the prefWidth and prefHeight to the specified values. */
  def prefSize(width: Value, height: Value): Container[T] = {
    _prefWidth = width; _prefHeight = height; this
  }

  def prefWidth(prefWidth: Value): Container[T] = {
    this._prefWidth = prefWidth; this
  }

  def prefHeight(prefHeight: Value): Container[T] = {
    this._prefHeight = prefHeight; this
  }

  /** Sets the prefWidth and prefHeight to the specified values. */
  def prefSize(width: Float, height: Float): Container[T] = { prefSize(Fixed.valueOf(width), Fixed.valueOf(height)); this }

  /** Sets the prefWidth and prefHeight to the specified value. */
  def prefSize(size: Float): Container[T] = { prefSize(Fixed.valueOf(size)); this }

  def prefWidth(prefWidth: Float): Container[T] = { this._prefWidth = Fixed.valueOf(prefWidth); this }

  def prefHeight(prefHeight: Float): Container[T] = { this._prefHeight = Fixed.valueOf(prefHeight); this }

  /** Sets the maxWidth and maxHeight to the specified value. */
  def maxSize(size: Value): Container[T] = {
    _maxWidth = size; _maxHeight = size; this
  }

  /** Sets the maxWidth and maxHeight to the specified values. */
  def maxSize(width: Value, height: Value): Container[T] = {
    _maxWidth = width; _maxHeight = height; this
  }

  def maxWidth(maxWidth: Value): Container[T] = {
    this._maxWidth = maxWidth; this
  }

  def maxHeight(maxHeight: Value): Container[T] = {
    this._maxHeight = maxHeight; this
  }

  /** Sets the maxWidth and maxHeight to the specified value. */
  def maxSize(size: Float): Container[T] = { maxSize(Fixed.valueOf(size)); this }

  /** Sets the maxWidth and maxHeight to the specified values. */
  def maxSize(width: Float, height: Float): Container[T] = { maxSize(Fixed.valueOf(width), Fixed.valueOf(height)); this }

  def maxWidth(maxWidth: Float): Container[T] = { this._maxWidth = Fixed.valueOf(maxWidth); this }

  def maxHeight(maxHeight: Float): Container[T] = { this._maxHeight = Fixed.valueOf(maxHeight); this }

  /** Sets the padTop, padLeft, padBottom, and padRight to the specified value. */
  def pad(pad: Value): Container[T] = {
    _padTop = pad; _padLeft = pad; _padBottom = pad; _padRight = pad; this
  }

  def pad(top: Value, left: Value, bottom: Value, right: Value): Container[T] = {
    _padTop = top; _padLeft = left; _padBottom = bottom; _padRight = right; this
  }

  def padTop(padTop: Value): Container[T] = {
    this._padTop = padTop; this
  }

  def padLeft(padLeft: Value): Container[T] = {
    this._padLeft = padLeft; this
  }

  def padBottom(padBottom: Value): Container[T] = {
    this._padBottom = padBottom; this
  }

  def padRight(padRight: Value): Container[T] = {
    this._padRight = padRight; this
  }

  /** Sets the padTop, padLeft, padBottom, and padRight to the specified value. */
  def pad(pad: Float): Container[T] = {
    val value = Fixed.valueOf(pad)
    _padTop = value; _padLeft = value; _padBottom = value; _padRight = value; this
  }

  def pad(top: Float, left: Float, bottom: Float, right: Float): Container[T] = {
    _padTop = Fixed.valueOf(top); _padLeft = Fixed.valueOf(left)
    _padBottom = Fixed.valueOf(bottom); _padRight = Fixed.valueOf(right); this
  }

  def padTop(padTop: Float): Container[T] = { this._padTop = Fixed.valueOf(padTop); this }

  def padLeft(padLeft: Float): Container[T] = { this._padLeft = Fixed.valueOf(padLeft); this }

  def padBottom(padBottom: Float): Container[T] = { this._padBottom = Fixed.valueOf(padBottom); this }

  def padRight(padRight: Float): Container[T] = { this._padRight = Fixed.valueOf(padRight); this }

  /** Sets fillX and fillY to 1. */
  def fill(): Container[T] = { _fillX = 1f; _fillY = 1f; this }

  /** Sets fillX to 1. */
  def fillX(): Container[T] = { _fillX = 1f; this }

  /** Sets fillY to 1. */
  def fillY(): Container[T] = { _fillY = 1f; this }

  def fill(x: Float, y: Float): Container[T] = { _fillX = x; _fillY = y; this }

  /** Sets fillX and fillY to 1 if true, 0 if false. */
  def fill(x: Boolean, y: Boolean): Container[T] = {
    _fillX = if (x) 1f else 0f; _fillY = if (y) 1f else 0f; this
  }

  /** Sets fillX and fillY to 1 if true, 0 if false. */
  def fill(fill: Boolean): Container[T] = {
    _fillX = if (fill) 1f else 0f; _fillY = if (fill) 1f else 0f; this
  }

  /** Sets the alignment of the actor within the container. Set to {@link Align#center}, {@link Align#top}, {@link Align#bottom}, {@link Align#left}, {@link Align#right}, or any combination of those.
    */
  def align(align: Align): Container[T] = { this._align = align; this }

  /** Sets the alignment of the actor within the container to {@link Align#center}. This clears any other alignment. */
  def center(): Container[T] = { _align = Align.center; this }

  /** Sets {@link Align#top} and clears {@link Align#bottom} for the alignment of the actor within the container. */
  def alignTop(): Container[T] = { _align = (_align | Align.top) & ~Align.bottom; this }

  /** Sets {@link Align#left} and clears {@link Align#right} for the alignment of the actor within the container. */
  def left(): Container[T] = { _align = (_align | Align.left) & ~Align.right; this }

  /** Sets {@link Align#bottom} and clears {@link Align#top} for the alignment of the actor within the container. */
  def bottom(): Container[T] = { _align = (_align | Align.bottom) & ~Align.top; this }

  /** Sets {@link Align#right} and clears {@link Align#left} for the alignment of the actor within the container. */
  def alignRight(): Container[T] = { _align = (_align | Align.right) & ~Align.left; this }

  override def minWidth: Float =
    actor.map(a => _minWidth.get(a)).getOrElse(0f) + _padLeft.get(this) + _padRight.get(this)

  def minHeightValue: Value = _minHeight

  override def minHeight: Float =
    actor.map(a => _minHeight.get(a)).getOrElse(0f) + _padTop.get(this) + _padBottom.get(this)

  def prefWidthValue: Value = _prefWidth

  override def prefWidth: Float = {
    var v = actor.map(a => _prefWidth.get(a)).getOrElse(0f)
    _background.foreach { bg => v = Math.max(v, bg.minWidth) }
    Math.max(minWidth, v + _padLeft.get(this) + _padRight.get(this))
  }

  def prefHeightValue: Value = _prefHeight

  override def prefHeight: Float = {
    var v = actor.map(a => _prefHeight.get(a)).getOrElse(0f)
    _background.foreach { bg => v = Math.max(v, bg.minHeight) }
    Math.max(minHeight, v + _padTop.get(this) + _padBottom.get(this))
  }

  def maxWidthValue: Value = _maxWidth

  override def maxWidth: Float = {
    var v = actor.map(a => _maxWidth.get(a)).getOrElse(0f)
    if (v > 0) v += _padLeft.get(this) + _padRight.get(this)
    v
  }

  def maxHeightValue: Value = _maxHeight

  override def maxHeight: Float = {
    var v = actor.map(a => _maxHeight.get(a)).getOrElse(0f)
    if (v > 0) v += _padTop.get(this) + _padBottom.get(this)
    v
  }

  def padTopValue: Value = _padTop
  def getPadTop:   Float = _padTop.get(this)

  def padLeftValue: Value = _padLeft
  def getPadLeft:   Float = _padLeft.get(this)

  def padBottomValue: Value = _padBottom
  def getPadBottom:   Float = _padBottom.get(this)

  def padRightValue: Value = _padRight
  def getPadRight:   Float = _padRight.get(this)

  /** Returns {@link #getPadLeft()} plus {@link #getPadRight()}. */
  def getPadX: Float = _padLeft.get(this) + _padRight.get(this)

  /** Returns {@link #getPadTop()} plus {@link #getPadBottom()}. */
  def getPadY: Float = _padTop.get(this) + _padBottom.get(this)

  def getFillX: Float = _fillX
  def getFillY: Float = _fillY
  def align:    Align = _align

  /** If true (the default), positions and sizes are rounded to integers. */
  def setRound(round: Boolean): Unit =
    this._round = round

  /** Sets clip to true. */
  def clip(): Container[T] = { setClip(true); this }

  def clip(enabled: Boolean): Container[T] = { setClip(enabled); this }

  /** Causes the contents to be clipped if they exceed the container bounds. Enabling clipping will set {@link #setTransform(boolean)} to true.
    */
  def setClip(enabled: Boolean): Unit = {
    _clip = enabled
    setTransform(enabled)
    invalidate()
  }

  def isClip: Boolean = _clip

  override def hit(x: Float, y: Float, touchable: Boolean): Nullable[Actor] = scala.util.boundary {
    if (_clip) {
      if (touchable && this.touchable == Touchable.disabled) scala.util.boundary.break(Nullable.empty)
      if (x < 0 || x >= width || y < 0 || y >= height) scala.util.boundary.break(Nullable.empty)
    }
    super.hit(x, y, touchable)
  }

  override def drawDebug(shapes: ShapeRenderer): Unit = {
    validate()
    if (transform) {
      applyTransform(shapes, computeTransform())
      if (_clip) {
        shapes.flush()
        val padLeft   = this._padLeft.get(this)
        val padBottom = this._padBottom.get(this)
        val draw      =
          if (_background.isEmpty) clipBegin(0, 0, width, height)
          else clipBegin(padLeft, padBottom, width - padLeft - _padRight.get(this), height - padBottom - _padTop.get(this))
        if (draw) {
          drawDebugChildren(shapes)
          clipEnd()
        }
      } else {
        drawDebugChildren(shapes)
      }
      resetTransform(shapes)
    } else {
      super.drawDebug(shapes)
    }
  }
}
