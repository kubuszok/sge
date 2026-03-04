/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Cell.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   TODO: extends Pool.Poolable → define given Poolable[Cell[?]] in companion
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.utils.{ Align, Nullable, Pool }

/** A cell for a {@link Table}.
  * @author
  *   Nathan Sweet
  */
class Cell[T <: Actor] extends Pool.Poolable {
  import Cell._

  var minWidth:    Nullable[Value]   = Nullable.empty
  var minHeight:   Nullable[Value]   = Nullable.empty
  var prefWidth:   Nullable[Value]   = Nullable.empty
  var prefHeight:  Nullable[Value]   = Nullable.empty
  var maxWidth:    Nullable[Value]   = Nullable.empty
  var maxHeight:   Nullable[Value]   = Nullable.empty
  var spaceTop:    Nullable[Value]   = Nullable.empty
  var spaceLeft:   Nullable[Value]   = Nullable.empty
  var spaceBottom: Nullable[Value]   = Nullable.empty
  var spaceRight:  Nullable[Value]   = Nullable.empty
  var padTop:      Nullable[Value]   = Nullable.empty
  var padLeft:     Nullable[Value]   = Nullable.empty
  var padBottom:   Nullable[Value]   = Nullable.empty
  var padRight:    Nullable[Value]   = Nullable.empty
  var _fillX:      Nullable[Float]   = Nullable.empty
  var _fillY:      Nullable[Float]   = Nullable.empty
  var align:       Nullable[Align]   = Nullable.empty
  var _expandX:    Nullable[Int]     = Nullable.empty
  var _expandY:    Nullable[Int]     = Nullable.empty
  var colspan:     Nullable[Int]     = Nullable.empty
  var _uniformX:   Nullable[Boolean] = Nullable.empty
  var _uniformY:   Nullable[Boolean] = Nullable.empty

  var actor:       Nullable[Actor] = Nullable.empty
  var actorX:      Float           = 0f
  var actorY:      Float           = 0f
  var actorWidth:  Float           = 0f
  var actorHeight: Float           = 0f

  private var table:     Nullable[Table] = Nullable.empty
  var endRow:            Boolean         = false
  var column:            Int             = 0
  var _row:              Int             = 0
  var cellAboveIndex:    Int             = -1
  var computedPadTop:    Float           = 0f
  var computedPadLeft:   Float           = 0f
  var computedPadBottom: Float           = 0f
  var computedPadRight:  Float           = 0f

  locally {
    val defs = Cell.defaults()
    defs.foreach(set)
  }

  def setTable(table: Table): Unit =
    this.table = Nullable(table)

  /** Sets the actor in this cell and adds the actor to the cell's table. If null, removes any current actor. */
  def setActor[A <: Actor](newActor: Nullable[A]): Cell[A] = {
    if (actor != newActor) {
      actor.foreach { a =>
        table.foreach { t =>
          if (a.getParent.exists(_ eq t)) a.remove()
        }
      }
      actor = newActor.asInstanceOf[Nullable[Actor]]
      newActor.foreach { na =>
        table.foreach(_.addActor(na))
      }
    }
    this.asInstanceOf[Cell[A]]
  }

  /** Removes the current actor for the cell, if any. */
  def clearActor(): Cell[T] = {
    setActor(Nullable.empty[Actor])
    this
  }

  /** Returns the actor for this cell, or null. */
  def getActor: Nullable[T] = actor.asInstanceOf[Nullable[T]]

  /** Returns true if the cell's actor is not null. */
  def hasActor: Boolean = actor.isDefined

  /** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified value. */
  def size(size: Value): Cell[T] = {

    minWidth = Nullable(size)
    minHeight = Nullable(size)
    prefWidth = Nullable(size)
    prefHeight = Nullable(size)
    maxWidth = Nullable(size)
    maxHeight = Nullable(size)
    this
  }

  /** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified values. */
  def size(width: Value, height: Value): Cell[T] = {

    minWidth = Nullable(width)
    minHeight = Nullable(height)
    prefWidth = Nullable(width)
    prefHeight = Nullable(height)
    maxWidth = Nullable(width)
    maxHeight = Nullable(height)
    this
  }

  /** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified value. */
  def size(size: Float): Cell[T] = {
    this.size(Value.Fixed.valueOf(size))
    this
  }

  /** Sets the minWidth, prefWidth, maxWidth, minHeight, prefHeight, and maxHeight to the specified values. */
  def size(width: Float, height: Float): Cell[T] = {
    this.size(Value.Fixed.valueOf(width), Value.Fixed.valueOf(height))
    this
  }

  /** Sets the minWidth, prefWidth, and maxWidth to the specified value. */
  def width(width: Value): Cell[T] = {

    minWidth = Nullable(width)
    prefWidth = Nullable(width)
    maxWidth = Nullable(width)
    this
  }

  /** Sets the minWidth, prefWidth, and maxWidth to the specified value. */
  def width(width: Float): Cell[T] = {
    this.width(Value.Fixed.valueOf(width))
    this
  }

  /** Sets the minHeight, prefHeight, and maxHeight to the specified value. */
  def height(height: Value): Cell[T] = {

    minHeight = Nullable(height)
    prefHeight = Nullable(height)
    maxHeight = Nullable(height)
    this
  }

  /** Sets the minHeight, prefHeight, and maxHeight to the specified value. */
  def height(height: Float): Cell[T] = {
    this.height(Value.Fixed.valueOf(height))
    this
  }

  /** Sets the minWidth and minHeight to the specified value. */
  def minSize(size: Value): Cell[T] = {

    minWidth = Nullable(size)
    minHeight = Nullable(size)
    this
  }

  /** Sets the minWidth and minHeight to the specified values. */
  def minSize(width: Value, height: Value): Cell[T] = {

    minWidth = Nullable(width)
    minHeight = Nullable(height)
    this
  }

  def minWidth(minWidth: Value): Cell[T] = {

    this.minWidth = Nullable(minWidth)
    this
  }

  def minHeight(minHeight: Value): Cell[T] = {

    this.minHeight = Nullable(minHeight)
    this
  }

  /** Sets the minWidth and minHeight to the specified value. */
  def minSize(size: Float): Cell[T] = {
    minSize(Value.Fixed.valueOf(size))
    this
  }

  /** Sets the minWidth and minHeight to the specified values. */
  def minSize(width: Float, height: Float): Cell[T] = {
    minSize(Value.Fixed.valueOf(width), Value.Fixed.valueOf(height))
    this
  }

  def minWidth(minWidth: Float): Cell[T] = {
    this.minWidth = Nullable(Value.Fixed.valueOf(minWidth))
    this
  }

  def minHeight(minHeight: Float): Cell[T] = {
    this.minHeight = Nullable(Value.Fixed.valueOf(minHeight))
    this
  }

  /** Sets the prefWidth and prefHeight to the specified value. */
  def prefSize(size: Value): Cell[T] = {

    prefWidth = Nullable(size)
    prefHeight = Nullable(size)
    this
  }

  /** Sets the prefWidth and prefHeight to the specified values. */
  def prefSize(width: Value, height: Value): Cell[T] = {

    prefWidth = Nullable(width)
    prefHeight = Nullable(height)
    this
  }

  def prefWidth(prefWidth: Value): Cell[T] = {

    this.prefWidth = Nullable(prefWidth)
    this
  }

  def prefHeight(prefHeight: Value): Cell[T] = {

    this.prefHeight = Nullable(prefHeight)
    this
  }

  /** Sets the prefWidth and prefHeight to the specified value. */
  def prefSize(width: Float, height: Float): Cell[T] = {
    prefSize(Value.Fixed.valueOf(width), Value.Fixed.valueOf(height))
    this
  }

  /** Sets the prefWidth and prefHeight to the specified values. */
  def prefSize(size: Float): Cell[T] = {
    prefSize(Value.Fixed.valueOf(size))
    this
  }

  def prefWidth(prefWidth: Float): Cell[T] = {
    this.prefWidth = Nullable(Value.Fixed.valueOf(prefWidth))
    this
  }

  def prefHeight(prefHeight: Float): Cell[T] = {
    this.prefHeight = Nullable(Value.Fixed.valueOf(prefHeight))
    this
  }

  /** Sets the maxWidth and maxHeight to the specified value. If the max size is 0, no maximum size is used. */
  def maxSize(size: Value): Cell[T] = {

    maxWidth = Nullable(size)
    maxHeight = Nullable(size)
    this
  }

  /** Sets the maxWidth and maxHeight to the specified values. If the max size is 0, no maximum size is used. */
  def maxSize(width: Value, height: Value): Cell[T] = {

    maxWidth = Nullable(width)
    maxHeight = Nullable(height)
    this
  }

  /** If the maxWidth is 0, no maximum width is used. */
  def maxWidth(maxWidth: Value): Cell[T] = {

    this.maxWidth = Nullable(maxWidth)
    this
  }

  /** If the maxHeight is 0, no maximum height is used. */
  def maxHeight(maxHeight: Value): Cell[T] = {

    this.maxHeight = Nullable(maxHeight)
    this
  }

  /** Sets the maxWidth and maxHeight to the specified value. If the max size is 0, no maximum size is used. */
  def maxSize(size: Float): Cell[T] = {
    maxSize(Value.Fixed.valueOf(size))
    this
  }

  /** Sets the maxWidth and maxHeight to the specified values. If the max size is 0, no maximum size is used. */
  def maxSize(width: Float, height: Float): Cell[T] = {
    maxSize(Value.Fixed.valueOf(width), Value.Fixed.valueOf(height))
    this
  }

  /** If the maxWidth is 0, no maximum width is used. */
  def maxWidth(maxWidth: Float): Cell[T] = {
    this.maxWidth = Nullable(Value.Fixed.valueOf(maxWidth))
    this
  }

  /** If the maxHeight is 0, no maximum height is used. */
  def maxHeight(maxHeight: Float): Cell[T] = {
    this.maxHeight = Nullable(Value.Fixed.valueOf(maxHeight))
    this
  }

  /** Sets the spaceTop, spaceLeft, spaceBottom, and spaceRight to the specified value. */
  def space(space: Value): Cell[T] = {

    spaceTop = Nullable(space)
    spaceLeft = Nullable(space)
    spaceBottom = Nullable(space)
    spaceRight = Nullable(space)
    this
  }

  def space(top: Value, left: Value, bottom: Value, right: Value): Cell[T] = {

    spaceTop = Nullable(top)
    spaceLeft = Nullable(left)
    spaceBottom = Nullable(bottom)
    spaceRight = Nullable(right)
    this
  }

  def spaceTop(spaceTop: Value): Cell[T] = {

    this.spaceTop = Nullable(spaceTop)
    this
  }

  def spaceLeft(spaceLeft: Value): Cell[T] = {

    this.spaceLeft = Nullable(spaceLeft)
    this
  }

  def spaceBottom(spaceBottom: Value): Cell[T] = {

    this.spaceBottom = Nullable(spaceBottom)
    this
  }

  def spaceRight(spaceRight: Value): Cell[T] = {

    this.spaceRight = Nullable(spaceRight)
    this
  }

  /** Sets the spaceTop, spaceLeft, spaceBottom, and spaceRight to the specified value. The space cannot be < 0. */
  def space(space: Float): Cell[T] = {
    require(space >= 0, s"space cannot be < 0: $space")
    this.space(Value.Fixed.valueOf(space))
    this
  }

  /** The space cannot be < 0. */
  def space(top: Float, left: Float, bottom: Float, right: Float): Cell[T] = {
    require(top >= 0, s"top cannot be < 0: $top")
    require(left >= 0, s"left cannot be < 0: $left")
    require(bottom >= 0, s"bottom cannot be < 0: $bottom")
    require(right >= 0, s"right cannot be < 0: $right")
    space(Value.Fixed.valueOf(top), Value.Fixed.valueOf(left), Value.Fixed.valueOf(bottom), Value.Fixed.valueOf(right))
    this
  }

  /** The space cannot be < 0. */
  def spaceTop(spaceTop: Float): Cell[T] = {
    require(spaceTop >= 0, s"spaceTop cannot be < 0: $spaceTop")
    this.spaceTop = Nullable(Value.Fixed.valueOf(spaceTop))
    this
  }

  /** The space cannot be < 0. */
  def spaceLeft(spaceLeft: Float): Cell[T] = {
    require(spaceLeft >= 0, s"spaceLeft cannot be < 0: $spaceLeft")
    this.spaceLeft = Nullable(Value.Fixed.valueOf(spaceLeft))
    this
  }

  /** The space cannot be < 0. */
  def spaceBottom(spaceBottom: Float): Cell[T] = {
    require(spaceBottom >= 0, s"spaceBottom cannot be < 0: $spaceBottom")
    this.spaceBottom = Nullable(Value.Fixed.valueOf(spaceBottom))
    this
  }

  /** The space cannot be < 0. */
  def spaceRight(spaceRight: Float): Cell[T] = {
    require(spaceRight >= 0, s"spaceRight cannot be < 0: $spaceRight")
    this.spaceRight = Nullable(Value.Fixed.valueOf(spaceRight))
    this
  }

  /** Sets the padTop, padLeft, padBottom, and padRight to the specified value. */
  def pad(pad: Value): Cell[T] = {

    padTop = Nullable(pad)
    padLeft = Nullable(pad)
    padBottom = Nullable(pad)
    padRight = Nullable(pad)
    this
  }

  def pad(top: Value, left: Value, bottom: Value, right: Value): Cell[T] = {

    padTop = Nullable(top)
    padLeft = Nullable(left)
    padBottom = Nullable(bottom)
    padRight = Nullable(right)
    this
  }

  def padTop(padTop: Value): Cell[T] = {

    this.padTop = Nullable(padTop)
    this
  }

  def padLeft(padLeft: Value): Cell[T] = {

    this.padLeft = Nullable(padLeft)
    this
  }

  def padBottom(padBottom: Value): Cell[T] = {

    this.padBottom = Nullable(padBottom)
    this
  }

  def padRight(padRight: Value): Cell[T] = {

    this.padRight = Nullable(padRight)
    this
  }

  /** Sets the padTop, padLeft, padBottom, and padRight to the specified value. */
  def pad(pad: Float): Cell[T] = {
    this.pad(Value.Fixed.valueOf(pad))
    this
  }

  def pad(top: Float, left: Float, bottom: Float, right: Float): Cell[T] = {
    pad(Value.Fixed.valueOf(top), Value.Fixed.valueOf(left), Value.Fixed.valueOf(bottom), Value.Fixed.valueOf(right))
    this
  }

  def padTop(padTop: Float): Cell[T] = {
    this.padTop = Nullable(Value.Fixed.valueOf(padTop))
    this
  }

  def padLeft(padLeft: Float): Cell[T] = {
    this.padLeft = Nullable(Value.Fixed.valueOf(padLeft))
    this
  }

  def padBottom(padBottom: Float): Cell[T] = {
    this.padBottom = Nullable(Value.Fixed.valueOf(padBottom))
    this
  }

  def padRight(padRight: Float): Cell[T] = {
    this.padRight = Nullable(Value.Fixed.valueOf(padRight))
    this
  }

  /** Sets fillX and fillY to 1. */
  def fill(): Cell[T] = {
    _fillX = Nullable(onef)
    _fillY = Nullable(onef)
    this
  }

  /** Sets fillX to 1. */
  def fillX(): Cell[T] = {
    _fillX = Nullable(onef)
    this
  }

  /** Sets fillY to 1. */
  def fillY(): Cell[T] = {
    _fillY = Nullable(onef)
    this
  }

  def fill(x: Float, y: Float): Cell[T] = {
    _fillX = Nullable(x)
    _fillY = Nullable(y)
    this
  }

  /** Sets fillX and fillY to 1 if true, 0 if false. */
  def fill(x: Boolean, y: Boolean): Cell[T] = {
    _fillX = Nullable(if (x) onef else zerof)
    _fillY = Nullable(if (y) onef else zerof)
    this
  }

  /** Sets fillX and fillY to 1 if true, 0 if false. */
  def fill(fill: Boolean): Cell[T] = {
    _fillX = Nullable(if (fill) onef else zerof)
    _fillY = Nullable(if (fill) onef else zerof)
    this
  }

  /** Sets the alignment of the actor within the cell. Set to {@link Align#center}, {@link Align#top}, {@link Align#bottom}, {@link Align#left}, {@link Align#right}, or any combination of those.
    */
  def align(align: Align): Cell[T] = {
    this.align = Nullable(align)
    this
  }

  /** Sets the alignment of the actor within the cell to {@link Align#center}. This clears any other alignment. */
  def center(): Cell[T] = {
    align = Nullable(centeri)
    this
  }

  /** Adds {@link Align#top} and clears {@link Align#bottom} for the alignment of the actor within the cell. */
  def top(): Cell[T] = {
    align = align.fold(Nullable(topi))(a => Nullable((a | Align.top) & ~Align.bottom))
    this
  }

  /** Adds {@link Align#left} and clears {@link Align#right} for the alignment of the actor within the cell. */
  def left(): Cell[T] = {
    align = align.fold(Nullable(lefti))(a => Nullable((a | Align.left) & ~Align.right))
    this
  }

  /** Adds {@link Align#bottom} and clears {@link Align#top} for the alignment of the actor within the cell. */
  def bottom(): Cell[T] = {
    align = align.fold(Nullable(bottomi))(a => Nullable((a | Align.bottom) & ~Align.top))
    this
  }

  /** Adds {@link Align#right} and clears {@link Align#left} for the alignment of the actor within the cell. */
  def right(): Cell[T] = {
    align = align.fold(Nullable(righti))(a => Nullable((a | Align.right) & ~Align.left))
    this
  }

  /** Sets expandX, expandY, fillX, and fillY to 1. */
  def grow(): Cell[T] = {
    _expandX = Nullable(onei)
    _expandY = Nullable(onei)
    _fillX = Nullable(onef)
    _fillY = Nullable(onef)
    this
  }

  /** Sets expandX and fillX to 1. */
  def growX(): Cell[T] = {
    _expandX = Nullable(onei)
    _fillX = Nullable(onef)
    this
  }

  /** Sets expandY and fillY to 1. */
  def growY(): Cell[T] = {
    _expandY = Nullable(onei)
    _fillY = Nullable(onef)
    this
  }

  /** Sets expandX and expandY to 1. */
  def expand(): Cell[T] = {
    _expandX = Nullable(onei)
    _expandY = Nullable(onei)
    this
  }

  /** Sets expandX to 1. */
  def expandX(): Cell[T] = {
    _expandX = Nullable(onei)
    this
  }

  /** Sets expandY to 1. */
  def expandY(): Cell[T] = {
    _expandY = Nullable(onei)
    this
  }

  def expand(x: Int, y: Int): Cell[T] = {
    _expandX = Nullable(x)
    _expandY = Nullable(y)
    this
  }

  /** Sets expandX and expandY to 1 if true, 0 if false. */
  def expand(x: Boolean, y: Boolean): Cell[T] = {
    _expandX = Nullable(if (x) onei else zeroi)
    _expandY = Nullable(if (y) onei else zeroi)
    this
  }

  def colspan(colspan: Int): Cell[T] = {
    this.colspan = Nullable(colspan)
    this
  }

  /** Sets uniformX and uniformY to true. */
  def uniform(): Cell[T] = {
    _uniformX = Nullable(true)
    _uniformY = Nullable(true)
    this
  }

  /** Sets uniformX to true. */
  def uniformX(): Cell[T] = {
    _uniformX = Nullable(true)
    this
  }

  /** Sets uniformY to true. */
  def uniformY(): Cell[T] = {
    _uniformY = Nullable(true)
    this
  }

  def uniform(uniform: Boolean): Cell[T] = {
    _uniformX = Nullable(uniform)
    _uniformY = Nullable(uniform)
    this
  }

  def uniform(x: Boolean, y: Boolean): Cell[T] = {
    _uniformX = Nullable(x)
    _uniformY = Nullable(y)
    this
  }

  def setActorBounds(x: Float, y: Float, width: Float, height: Float): Unit = {
    actorX = x
    actorY = y
    actorWidth = width
    actorHeight = height
  }

  def getActorX: Float = actorX

  def setActorX(actorX: Float): Unit =
    this.actorX = actorX

  def getActorY: Float = actorY

  def setActorY(actorY: Float): Unit =
    this.actorY = actorY

  def getActorWidth: Float = actorWidth

  def setActorWidth(actorWidth: Float): Unit =
    this.actorWidth = actorWidth

  def getActorHeight: Float = actorHeight

  def setActorHeight(actorHeight: Float): Unit =
    this.actorHeight = actorHeight

  def getColumn: Int = column

  def getRow: Int = _row

  /** @return May be null if this cell is row defaults. */
  def getMinWidthValue: Nullable[Value] = minWidth

  def getMinWidth: Float = minWidth.map(_.get(actor)).getOrElse(0f)

  /** @return May be null if this cell is row defaults. */
  def getMinHeightValue: Nullable[Value] = minHeight

  def getMinHeight: Float = minHeight.map(_.get(actor)).getOrElse(0f)

  /** @return May be null if this cell is row defaults. */
  def getPrefWidthValue: Nullable[Value] = prefWidth

  def getPrefWidth: Float = prefWidth.map(_.get(actor)).getOrElse(0f)

  /** @return May be null if this cell is row defaults. */
  def getPrefHeightValue: Nullable[Value] = prefHeight

  def getPrefHeight: Float = prefHeight.map(_.get(actor)).getOrElse(0f)

  /** @return May be null if this cell is row defaults. */
  def getMaxWidthValue: Nullable[Value] = maxWidth

  def getMaxWidth: Float = maxWidth.map(_.get(actor)).getOrElse(0f)

  /** @return May be null if this cell is row defaults. */
  def getMaxHeightValue: Nullable[Value] = maxHeight

  def getMaxHeight: Float = maxHeight.map(_.get(actor)).getOrElse(0f)

  /** @return May be null if this value is not set. */
  def getSpaceTopValue: Nullable[Value] = spaceTop

  def getSpaceTop: Float = spaceTop.map(_.get(actor)).getOrElse(0f)

  /** @return May be null if this value is not set. */
  def getSpaceLeftValue: Nullable[Value] = spaceLeft

  def getSpaceLeft: Float = spaceLeft.map(_.get(actor)).getOrElse(0f)

  /** @return May be null if this value is not set. */
  def getSpaceBottomValue: Nullable[Value] = spaceBottom

  def getSpaceBottom: Float = spaceBottom.map(_.get(actor)).getOrElse(0f)

  /** @return May be null if this value is not set. */
  def getSpaceRightValue: Nullable[Value] = spaceRight

  def getSpaceRight: Float = spaceRight.map(_.get(actor)).getOrElse(0f)

  /** @return May be null if this value is not set. */
  def getPadTopValue: Nullable[Value] = padTop

  def getPadTop: Float = padTop.map(_.get(actor)).getOrElse(0f)

  /** @return May be null if this value is not set. */
  def getPadLeftValue: Nullable[Value] = padLeft

  def getPadLeft: Float = padLeft.map(_.get(actor)).getOrElse(0f)

  /** @return May be null if this value is not set. */
  def getPadBottomValue: Nullable[Value] = padBottom

  def getPadBottom: Float = padBottom.map(_.get(actor)).getOrElse(0f)

  /** @return May be null if this value is not set. */
  def getPadRightValue: Nullable[Value] = padRight

  def getPadRight: Float = padRight.map(_.get(actor)).getOrElse(0f)

  /** Returns {@link #getPadLeft()} plus {@link #getPadRight()}. */
  def getPadX: Float = padLeft.map(_.get(actor)).getOrElse(0f) + padRight.map(_.get(actor)).getOrElse(0f)

  /** Returns {@link #getPadTop()} plus {@link #getPadBottom()}. */
  def getPadY: Float = padTop.map(_.get(actor)).getOrElse(0f) + padBottom.map(_.get(actor)).getOrElse(0f)

  def getFillX: Nullable[Float] = _fillX

  def getFillY: Nullable[Float] = _fillY

  def getAlign: Nullable[Align] = align

  def getExpandX: Nullable[Int] = _expandX

  def getExpandY: Nullable[Int] = _expandY

  def getColspan: Nullable[Int] = colspan

  def getUniformX: Nullable[Boolean] = _uniformX

  def getUniformY: Nullable[Boolean] = _uniformY

  /** Returns true if this cell is the last cell in the row. */
  def isEndRow: Boolean = endRow

  /** The actual amount of combined padding and spacing from the last layout. */
  def getComputedPadTop: Float = computedPadTop

  /** The actual amount of combined padding and spacing from the last layout. */
  def getComputedPadLeft: Float = computedPadLeft

  /** The actual amount of combined padding and spacing from the last layout. */
  def getComputedPadBottom: Float = computedPadBottom

  /** The actual amount of combined padding and spacing from the last layout. */
  def getComputedPadRight: Float = computedPadRight

  def row(): Unit =
    table.foreach(_.row())

  def getTable: Nullable[Table] = table

  /** Sets all constraint fields to null. */
  private[ui] def clear(): Unit = {
    minWidth = Nullable.empty
    minHeight = Nullable.empty
    prefWidth = Nullable.empty
    prefHeight = Nullable.empty
    maxWidth = Nullable.empty
    maxHeight = Nullable.empty
    spaceTop = Nullable.empty
    spaceLeft = Nullable.empty
    spaceBottom = Nullable.empty
    spaceRight = Nullable.empty
    padTop = Nullable.empty
    padLeft = Nullable.empty
    padBottom = Nullable.empty
    padRight = Nullable.empty
    _fillX = Nullable.empty
    _fillY = Nullable.empty
    align = Nullable.empty
    _expandX = Nullable.empty
    _expandY = Nullable.empty
    colspan = Nullable.empty
    _uniformX = Nullable.empty
    _uniformY = Nullable.empty
  }

  /** Reset state so the cell can be reused, setting all constraints to their {@link #defaults() default} values. */
  def reset(): Unit = {
    actor = Nullable.empty
    table = Nullable.empty
    endRow = false
    cellAboveIndex = -1
    Cell.defaults().foreach(set)
  }

  private[ui] def set(cell: Cell[?]): Unit = {
    minWidth = cell.minWidth
    minHeight = cell.minHeight
    prefWidth = cell.prefWidth
    prefHeight = cell.prefHeight
    maxWidth = cell.maxWidth
    maxHeight = cell.maxHeight
    spaceTop = cell.spaceTop
    spaceLeft = cell.spaceLeft
    spaceBottom = cell.spaceBottom
    spaceRight = cell.spaceRight
    padTop = cell.padTop
    padLeft = cell.padLeft
    padBottom = cell.padBottom
    padRight = cell.padRight
    _fillX = cell._fillX
    _fillY = cell._fillY
    align = cell.align
    _expandX = cell._expandX
    _expandY = cell._expandY
    colspan = cell.colspan
    _uniformX = cell._uniformX
    _uniformY = cell._uniformY
  }

  private[ui] def merge(cell: Nullable[Cell[?]]): Unit =
    cell.foreach { c =>
      c.minWidth.foreach(v => minWidth = Nullable(v))
      c.minHeight.foreach(v => minHeight = Nullable(v))
      c.prefWidth.foreach(v => prefWidth = Nullable(v))
      c.prefHeight.foreach(v => prefHeight = Nullable(v))
      c.maxWidth.foreach(v => maxWidth = Nullable(v))
      c.maxHeight.foreach(v => maxHeight = Nullable(v))
      c.spaceTop.foreach(v => spaceTop = Nullable(v))
      c.spaceLeft.foreach(v => spaceLeft = Nullable(v))
      c.spaceBottom.foreach(v => spaceBottom = Nullable(v))
      c.spaceRight.foreach(v => spaceRight = Nullable(v))
      c.padTop.foreach(v => padTop = Nullable(v))
      c.padLeft.foreach(v => padLeft = Nullable(v))
      c.padBottom.foreach(v => padBottom = Nullable(v))
      c.padRight.foreach(v => padRight = Nullable(v))
      c._fillX.foreach(v => _fillX = Nullable(v))
      c._fillY.foreach(v => _fillY = Nullable(v))
      c.align.foreach(v => align = Nullable(v))
      c._expandX.foreach(v => _expandX = Nullable(v))
      c._expandY.foreach(v => _expandY = Nullable(v))
      c.colspan.foreach(v => colspan = Nullable(v))
      c._uniformX.foreach(v => _uniformX = Nullable(v))
      c._uniformY.foreach(v => _uniformY = Nullable(v))
    }

  override def toString: String = actor.map(_.toString).getOrElse(super.toString)
}

object Cell {
  private val zerof:   Float = 0f
  private val onef:    Float = 1f
  private val zeroi:   Int   = 0
  private val onei:    Int   = 1
  private val centeri: Align = Align.center
  private val topi:    Align = Align.top
  private val bottomi: Align = Align.bottom
  private val lefti:   Align = Align.left
  private val righti:  Align = Align.right

  @volatile private var _defaults:         Nullable[Cell[?]] = Nullable.empty
  @volatile private var _creatingDefaults: Boolean           = false

  /** Returns the defaults to use for all cells. This can be used to avoid needing to set the same defaults for every table (eg, for spacing).
    */
  def defaults(): Nullable[Cell[?]] = {
    if (_defaults.isEmpty && !_creatingDefaults) {
      _creatingDefaults = true
      val d = Cell[Actor]()
      d.minWidth = Nullable(Value.minWidth)
      d.minHeight = Nullable(Value.minHeight)
      d.prefWidth = Nullable(Value.prefWidth)
      d.prefHeight = Nullable(Value.prefHeight)
      d.maxWidth = Nullable(Value.maxWidth)
      d.maxHeight = Nullable(Value.maxHeight)
      d.spaceTop = Nullable(Value.zero)
      d.spaceLeft = Nullable(Value.zero)
      d.spaceBottom = Nullable(Value.zero)
      d.spaceRight = Nullable(Value.zero)
      d.padTop = Nullable(Value.zero)
      d.padLeft = Nullable(Value.zero)
      d.padBottom = Nullable(Value.zero)
      d.padRight = Nullable(Value.zero)
      d._fillX = Nullable(zerof)
      d._fillY = Nullable(zerof)
      d.align = Nullable(centeri)
      d._expandX = Nullable(zeroi)
      d._expandY = Nullable(zeroi)
      d.colspan = Nullable(onei)
      d._uniformX = Nullable.empty
      d._uniformY = Nullable.empty
      _defaults = Nullable(d)
      _creatingDefaults = false
    }
    _defaults
  }
}
