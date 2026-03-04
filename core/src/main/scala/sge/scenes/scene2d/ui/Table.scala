/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Table.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: null -> Nullable; Align opaque type; DynamicArray for cells; boundary/break; Debug enum in companion object
 *   Idiom: split packages
 *   TODO: Java-style getters/setters — getBackground/setBackground, getClip/setClip, getCells, getPadTopValue, getAlign, getRows, getColumns, getSkin, etc.
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.graphics.Color
import sge.graphics.g2d.Batch
import sge.graphics.glutils.ShapeRenderer
import sge.math.Rectangle
import sge.scenes.scene2d.utils.{ Drawable, Layout }
import sge.utils.{ Align, DynamicArray, Nullable, Pool }

/** A group that sizes and positions children using table constraints.
  *
  * Children added with {@link #add(Actor...)} (and similar methods returning a {@link Cell}) are laid out in rows and columns. Other children may be added with {@link #addActor(Actor)} (and similar
  * methods) but are not laid out automatically and don't affect the preferred or minimum sizes.
  *
  * By default, {@link #getTouchable()} is {@link Touchable#childrenOnly}.
  *
  * The preferred and minimum sizes are that of the children laid out in columns and rows.
  * @author
  *   Nathan Sweet
  */
class Table(private var skin: Nullable[Any] = Nullable.empty)(using Sge) extends WidgetGroup() {
  import Table._

  private var columns:        Int     = 0
  private var rows:           Int     = 0
  private var implicitEndRow: Boolean = false

  private val cells:           DynamicArray[Cell[?]]           = DynamicArray[Cell[?]]()
  private val cellDefaults:    Cell[?]                         = obtainCell()
  private val _columnDefaults: DynamicArray[Nullable[Cell[?]]] = DynamicArray[Nullable[Cell[?]]]()
  private var rowDefaults:     Nullable[Cell[?]]               = Nullable.empty

  private var sizeInvalid:     Boolean      = true
  private var columnMinWidth:  Array[Float] = Array.empty
  private var rowMinHeight:    Array[Float] = Array.empty
  private var columnPrefWidth: Array[Float] = Array.empty
  private var rowPrefHeight:   Array[Float] = Array.empty
  private var tableMinWidth:   Float        = 0f
  private var tableMinHeight:  Float        = 0f
  private var tablePrefWidth:  Float        = 0f
  private var tablePrefHeight: Float        = 0f
  private var _columnWidth:    Array[Float] = Array.empty
  private var _rowHeight:      Array[Float] = Array.empty
  private var expandWidth:     Array[Float] = Array.empty
  private var expandHeight:    Array[Float] = Array.empty

  var padTop:     Value = backgroundTop
  var padLeft:    Value = backgroundLeft
  var padBottom:  Value = backgroundBottom
  var padRight:   Value = backgroundRight
  var tableAlign: Align = Align.center

  var tableDebug: Table.Debug                       = Table.Debug.none
  var debugRects: Nullable[DynamicArray[DebugRect]] = Nullable.empty

  var background:    Nullable[Drawable] = Nullable.empty
  private var _clip: Boolean            = false
  var round:         Boolean            = true

  setTransform(false)
  setTouchable(Touchable.childrenOnly)

  private def obtainCell(): Cell[?] = {
    val cell = cellPool.obtain()
    cell.setTable(this)
    cell
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    validate()
    if (isTransform) {
      applyTransform(batch, computeTransform())
      drawBackground(batch, parentAlpha, 0, 0)
      if (_clip) {
        batch.flush()
        val pl = this.padLeft.get(Nullable(this))
        val pb = this.padBottom.get(Nullable(this))
        if (clipBegin(pl, pb, getWidth - pl - padRight.get(Nullable(this)), getHeight - pb - padTop.get(Nullable(this)))) {
          drawChildren(batch, parentAlpha)
          batch.flush()
          clipEnd()
        }
      } else {
        drawChildren(batch, parentAlpha)
      }
      resetTransform(batch)
    } else {
      drawBackground(batch, parentAlpha, getX, getY)
      super.draw(batch, parentAlpha)
    }
  }

  /** Called to draw the background, before clipping is applied (if enabled). Default implementation draws the background drawable.
    */
  protected def drawBackground(batch: Batch, parentAlpha: Float, x: Float, y: Float): Unit =
    background.foreach { bg =>
      val color = getColor
      batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
      bg.draw(batch, x, y, getWidth, getHeight)
    }

  /** Sets the background drawable from the skin and adjusts the table's padding to match the background. This may only be called if a skin has been set with {@link Table#Table(Skin)} or
    * {@link #setSkin(Skin)}.
    * @see
    *   #setBackground(Drawable)
    */
  def setBackground(drawableName: String): Unit =
    skin.fold(throw new IllegalStateException("Table must have a skin set to use this method.")) { s =>
      setBackground(Nullable(s.asInstanceOf[Skin].getDrawable(drawableName)))
    }

  /** @param background May be null to clear the background. */
  def setBackground(background: Nullable[Drawable]): Unit =
    if (this.background != background) {
      val padTopOld    = getPadTop
      val padLeftOld   = getPadLeft
      val padBottomOld = getPadBottom
      val padRightOld  = getPadRight
      this.background = background // The default pad values use the background's padding.
      val padTopNew    = getPadTop
      val padLeftNew   = getPadLeft
      val padBottomNew = getPadBottom
      val padRightNew  = getPadRight
      if (padTopOld + padBottomOld != padTopNew + padBottomNew || padLeftOld + padRightOld != padLeftNew + padRightNew)
        invalidateHierarchy()
      else if (padTopOld != padTopNew || padLeftOld != padLeftNew || padBottomOld != padBottomNew || padRightOld != padRightNew)
        invalidate()
    }

  /** @see #setBackground(Drawable) */
  def background(background: Nullable[Drawable]): Table = {
    setBackground(background)
    this
  }

  /** @see #setBackground(String) */
  // TODO: uncomment when Skin is ported
  // def background(drawableName: String): Table = {
  //   setBackground(drawableName)
  //   this
  // }

  def getBackground: Nullable[Drawable] = background

  override def hit(x: Float, y: Float, touchable: Boolean): Nullable[Actor] = scala.util.boundary {
    if (_clip) {
      if (touchable && getTouchable == Touchable.disabled) scala.util.boundary.break(Nullable.empty)
      if (x < 0 || x >= getWidth || y < 0 || y >= getHeight) scala.util.boundary.break(Nullable.empty)
    }
    super.hit(x, y, touchable)
  }

  /** Sets {@link #setClip(boolean)} to true. */
  def clip(): Table = {
    setClip(true)
    this
  }

  def clip(enabled: Boolean): Table = {
    setClip(enabled)
    this
  }

  /** Causes the contents to be clipped if they exceed the table's bounds. Enabling clipping sets {@link #setTransform(boolean)} to true.
    */
  def setClip(enabled: Boolean): Unit = {
    _clip = enabled
    setTransform(enabled)
    invalidate()
  }

  def getClip: Boolean = _clip

  override def invalidate(): Unit = {
    sizeInvalid = true
    super.invalidate()
  }

  /** Adds a new cell to the table with the specified actor. */
  def add[T <: Actor](actor: Nullable[T]): Cell[T] = {
    val cell = obtainCell().asInstanceOf[Cell[T]]
    cell.actor = actor.asInstanceOf[Nullable[Actor]]

    // The row was ended for layout, not by the user, so revert it.
    if (implicitEndRow) {
      implicitEndRow = false
      rows -= 1
      cells.last.endRow = false
    }

    val cellCount = cells.size
    if (cellCount > 0) {
      // Set cell column and row.
      val lastCell = cells.last
      if (!lastCell.endRow) {
        cell.column = lastCell.column + lastCell.colspan.getOrElse(1)
        cell._row = lastCell._row
      } else {
        cell.column = 0
        cell._row = lastCell._row + 1
      }
      // Set the index of the cell above.
      if (cell._row > 0) {
        scala.util.boundary {
          var i = cellCount - 1
          while (i >= 0) {
            val other = cells(i)
            var col   = other.column
            val nn    = col + other.colspan.getOrElse(1)
            while (col < nn) {
              if (col == cell.column) {
                cell.cellAboveIndex = i
                scala.util.boundary.break()
              }
              col += 1
            }
            i -= 1
          }
        }
      }
    } else {
      cell.column = 0
      cell._row = 0
    }
    cells.add(cell)

    cell.set(cellDefaults)
    if (cell.column < _columnDefaults.size) cell.merge(_columnDefaults(cell.column))
    cell.merge(rowDefaults)

    actor.foreach(addActor)

    cell
  }

  def add(actors: Actor*): Table = {
    actors.foreach(a => add(Nullable(a)))
    this
  }

  /** Adds a new cell with a label. This may only be called if a skin has been set with {@link Table#Table(Skin)} or {@link #setSkin(Skin)}.
    */
  @scala.annotation.targetName("addLabel")
  def add(text: Nullable[CharSequence]): Cell[Label] =
    skin.fold(throw new IllegalStateException("Table must have a skin set to use this method.")) { s =>
      add(Nullable(Label(text, s.asInstanceOf[Skin]))).asInstanceOf[Cell[Label]]
    }

  /** Adds a new cell with a label. This may only be called if a skin has been set with {@link Table#Table(Skin)} or {@link #setSkin(Skin)}.
    */
  def add(text: Nullable[CharSequence], labelStyleName: String): Cell[Label] =
    skin.fold(throw new IllegalStateException("Table must have a skin set to use this method.")) { s =>
      add(Nullable(Label(text, s.asInstanceOf[Skin].get(labelStyleName, classOf[Label.LabelStyle])))).asInstanceOf[Cell[Label]]
    }

  /** Adds a new cell with a label. This may only be called if a skin has been set with {@link Table#Table(Skin)} or {@link #setSkin(Skin)}.
    */
  def add(text: Nullable[CharSequence], fontName: String, color: Nullable[Color]): Cell[Label] =
    skin.fold(throw new IllegalStateException("Table must have a skin set to use this method.")) { s =>
      val sk = s.asInstanceOf[Skin]
      add(Nullable(Label(text, Label.LabelStyle(sk.getFont(fontName), color)))).asInstanceOf[Cell[Label]]
    }

  /** Adds a new cell with a label. This may only be called if a skin has been set with {@link Table#Table(Skin)} or {@link #setSkin(Skin)}.
    */
  def add(text: Nullable[CharSequence], fontName: String, colorName: String): Cell[Label] =
    skin.fold(throw new IllegalStateException("Table must have a skin set to use this method.")) { s =>
      val sk = s.asInstanceOf[Skin]
      add(Nullable(Label(text, Label.LabelStyle(sk.getFont(fontName), Nullable(sk.getColor(colorName)))))).asInstanceOf[Cell[Label]]
    }

  /** Adds a cell without an actor. */
  def add(): Cell[?] = add(Nullable.empty[Actor])

  /** Adds a new cell to the table with the specified actors in a {@link Stack}.
    * @param actors
    *   May be null or empty to add a stack without any actors.
    */
  def stack(actors: Actor*): Cell[Stack] = {
    val s = Stack()
    actors.foreach(a => s.addActor(a))
    add(Nullable(s)).asInstanceOf[Cell[Stack]]
  }

  override def removeActor(actor: Actor): Boolean =
    removeActor(actor, true)

  override def removeActor(actor: Actor, unfocus: Boolean): Boolean =
    if (!super.removeActor(actor, unfocus)) false
    else {
      val cell = getCell(actor)
      cell.foreach(_.actor = Nullable.empty)
      true
    }

  override def removeActorAt(index: Int, unfocus: Boolean): Actor = {
    val actor = super.removeActorAt(index, unfocus)
    val cell  = getCell(actor)
    cell.foreach(_.actor = Nullable.empty)
    actor
  }

  /** Removes all actors and cells from the table. */
  override def clearChildren(unfocus: Boolean): Unit = {
    var i = cells.size - 1
    while (i >= 0) {
      val cell = cells(i)
      cell.actor.foreach(_.remove())
      i -= 1
    }
    cellPool.freeAll(cells)
    cells.clear()
    rows = 0
    columns = 0
    rowDefaults.foreach(cellPool.free)
    rowDefaults = Nullable.empty
    implicitEndRow = false

    super.clearChildren(unfocus)
  }

  /** Removes all actors and cells from the table (same as {@link #clearChildren()}) and additionally resets all table properties and cell, column, and row defaults.
    */
  def reset(): Unit = {
    clearChildren()
    padTop = backgroundTop
    padLeft = backgroundLeft
    padBottom = backgroundBottom
    padRight = backgroundRight
    tableAlign = Align.center
    debug(Table.Debug.none)
    cellDefaults.reset()
    var i = 0
    while (i < _columnDefaults.size) {
      _columnDefaults(i).foreach(cellPool.free)
      i += 1
    }
    _columnDefaults.clear()
  }

  /** Indicates that subsequent cells should be added to a new row and returns the cell values that will be used as the defaults for all cells in the new row.
    */
  def row(): Cell[?] = scala.util.boundary {
    if (cells.nonEmpty) {
      if (!implicitEndRow) {
        if (cells.last.endRow) rowDefaults.foreach(rd => scala.util.boundary.break(rd)) // Row was already ended.
        endRow()
      }
      invalidate()
    }
    implicitEndRow = false
    rowDefaults.foreach(cellPool.free)
    val rd = obtainCell()
    rd.clear()
    rowDefaults = Nullable(rd)
    rd
  }

  private def endRow(): Unit = {
    var rowColumns = 0
    var i          = cells.size - 1
    while (i >= 0) {
      val cell = cells(i)
      if (cell.endRow) {
        i = -1 // break
      } else {
        rowColumns += cell.colspan.getOrElse(1)
        i -= 1
      }
    }
    columns = Math.max(columns, rowColumns)
    rows += 1
    cells.last.endRow = true
  }

  /** Gets the cell values that will be used as the defaults for all cells in the specified column. Columns are indexed starting at 0.
    */
  def columnDefaults(column: Int): Cell[?] = {
    val cell = if (this._columnDefaults.size > column) this._columnDefaults(column) else Nullable.empty[Cell[?]]
    cell.getOrElse {
      val c = obtainCell()
      c.clear()
      if (column >= this._columnDefaults.size) {
        var i = this._columnDefaults.size
        while (i < column) {
          this._columnDefaults.add(Nullable.empty)
          i += 1
        }
        this._columnDefaults.add(Nullable(c))
      } else {
        this._columnDefaults(column) = Nullable(c)
      }
      c
    }
  }

  /** Returns the cell for the specified actor in this table, or null. */
  def getCell[T <: Actor](actor: T): Nullable[Cell[T]] =
    scala.util.boundary {
      var i = 0
      while (i < cells.size) {
        val c = cells(i)
        if (c.actor.exists(_ eq actor)) scala.util.boundary.break(Nullable(c.asInstanceOf[Cell[T]]))
        i += 1
      }
      Nullable.empty
    }

  /** Returns the cells for this table. */
  def getCells: DynamicArray[Cell[?]] = cells

  override def getPrefWidth: Float = {
    if (sizeInvalid) computeSize()
    val width = tablePrefWidth
    background.fold(width)(bg => Math.max(width, bg.getMinWidth))
  }

  override def getPrefHeight: Float = {
    if (sizeInvalid) computeSize()
    val height = tablePrefHeight
    background.fold(height)(bg => Math.max(height, bg.getMinHeight))
  }

  override def getMinWidth: Float = {
    if (sizeInvalid) computeSize()
    tableMinWidth
  }

  override def getMinHeight: Float = {
    if (sizeInvalid) computeSize()
    tableMinHeight
  }

  /** The cell values that will be used as the defaults for all cells. */
  def defaults(): Cell[?] = cellDefaults

  /** Sets the padTop, padLeft, padBottom, and padRight around the table to the specified value. */
  def pad(pad: Value): Table = {
    padTop = pad
    padLeft = pad
    padBottom = pad
    padRight = pad
    sizeInvalid = true
    this
  }

  def pad(top: Value, left: Value, bottom: Value, right: Value): Table = {
    padTop = top
    padLeft = left
    padBottom = bottom
    padRight = right
    sizeInvalid = true
    this
  }

  /** Padding at the top edge of the table. */
  def padTop(padTop: Value): Table = {
    this.padTop = padTop
    sizeInvalid = true
    this
  }

  /** Padding at the left edge of the table. */
  def padLeft(padLeft: Value): Table = {
    this.padLeft = padLeft
    sizeInvalid = true
    this
  }

  /** Padding at the bottom edge of the table. */
  def padBottom(padBottom: Value): Table = {
    this.padBottom = padBottom
    sizeInvalid = true
    this
  }

  /** Padding at the right edge of the table. */
  def padRight(padRight: Value): Table = {
    this.padRight = padRight
    sizeInvalid = true
    this
  }

  /** Sets the padTop, padLeft, padBottom, and padRight around the table to the specified value. */
  def pad(pad: Float): Table = {
    this.pad(Value.Fixed.valueOf(pad))
    this
  }

  def pad(top: Float, left: Float, bottom: Float, right: Float): Table = {
    padTop = Value.Fixed.valueOf(top)
    padLeft = Value.Fixed.valueOf(left)
    padBottom = Value.Fixed.valueOf(bottom)
    padRight = Value.Fixed.valueOf(right)
    sizeInvalid = true
    this
  }

  /** Padding at the top edge of the table. */
  def padTop(padTop: Float): Table = {
    this.padTop = Value.Fixed.valueOf(padTop)
    sizeInvalid = true
    this
  }

  /** Padding at the left edge of the table. */
  def padLeft(padLeft: Float): Table = {
    this.padLeft = Value.Fixed.valueOf(padLeft)
    sizeInvalid = true
    this
  }

  /** Padding at the bottom edge of the table. */
  def padBottom(padBottom: Float): Table = {
    this.padBottom = Value.Fixed.valueOf(padBottom)
    sizeInvalid = true
    this
  }

  /** Padding at the right edge of the table. */
  def padRight(padRight: Float): Table = {
    this.padRight = Value.Fixed.valueOf(padRight)
    sizeInvalid = true
    this
  }

  /** Alignment of the logical table within the table actor. Set to {@link Align#center}, {@link Align#top}, {@link Align#bottom} , {@link Align#left}, {@link Align#right}, or any combination of
    * those.
    */
  def align(align: Align): Table = {
    this.tableAlign = align
    this
  }

  /** Sets the alignment of the logical table within the table actor to {@link Align#center}. This clears any other alignment. */
  def center(): Table = {
    tableAlign = Align.center
    this
  }

  /** Adds {@link Align#top} and clears {@link Align#bottom} for the alignment of the logical table within the table actor. */
  def top(): Table = {
    tableAlign = (tableAlign | Align.top) & ~Align.bottom
    this
  }

  /** Adds {@link Align#left} and clears {@link Align#right} for the alignment of the logical table within the table actor. */
  def left(): Table = {
    tableAlign = (tableAlign | Align.left) & ~Align.right
    this
  }

  /** Adds {@link Align#bottom} and clears {@link Align#top} for the alignment of the logical table within the table actor. */
  def bottom(): Table = {
    tableAlign = (tableAlign | Align.bottom) & ~Align.top
    this
  }

  /** Adds {@link Align#right} and clears {@link Align#left} for the alignment of the logical table within the table actor. */
  def right(): Table = {
    tableAlign = (tableAlign | Align.right) & ~Align.left
    this
  }

  override def setDebug(enabled: Boolean): Unit =
    debug(if (enabled) Table.Debug.all else Table.Debug.none)

  override def debug(): Actor = {
    super.debug()
    this
  }

  override def debugAll(): Group = {
    super.debugAll()
    this
  }

  /** Turns on table debug lines. */
  def debugTable(): Table = {
    super.setDebug(true)
    if (tableDebug != Table.Debug.table) {
      this.tableDebug = Table.Debug.table
      invalidate()
    }
    this
  }

  /** Turns on cell debug lines. */
  def debugCell(): Table = {
    super.setDebug(true)
    if (tableDebug != Table.Debug.cell) {
      this.tableDebug = Table.Debug.cell
      invalidate()
    }
    this
  }

  /** Turns on actor debug lines. */
  def debugActor(): Table = {
    super.setDebug(true)
    if (tableDebug != Table.Debug.actor) {
      this.tableDebug = Table.Debug.actor
      invalidate()
    }
    this
  }

  /** Turns debug lines on or off. */
  def debug(debug: Table.Debug): Table = {
    super.setDebug(debug != Table.Debug.none)
    if (this.tableDebug != debug) {
      this.tableDebug = debug
      if (debug == Table.Debug.none)
        clearDebugRects()
      else
        invalidate()
    }
    this
  }

  def getTableDebug: Table.Debug = tableDebug

  def getPadTopValue: Value = padTop

  def getPadTop: Float = padTop.get(Nullable(this))

  def getPadLeftValue: Value = padLeft

  def getPadLeft: Float = padLeft.get(Nullable(this))

  def getPadBottomValue: Value = padBottom

  def getPadBottom: Float = padBottom.get(Nullable(this))

  def getPadRightValue: Value = padRight

  def getPadRight: Float = padRight.get(Nullable(this))

  /** Returns {@link #getPadLeft()} plus {@link #getPadRight()}. */
  def getPadX: Float = padLeft.get(Nullable(this)) + padRight.get(Nullable(this))

  /** Returns {@link #getPadTop()} plus {@link #getPadBottom()}. */
  def getPadY: Float = padTop.get(Nullable(this)) + padBottom.get(Nullable(this))

  def getAlign: Align = tableAlign

  /** Returns the row index for the y coordinate, or -1 if not over a row.
    * @param y
    *   The y coordinate, where 0 is the top of the table.
    */
  def getRow(y: Float): Int = scala.util.boundary {
    val n = cells.size
    if (n == 0) scala.util.boundary.break(-1)
    val yy  = y + getPadTop
    var i   = 0
    var row = 0
    while (i < n) {
      val c = cells(i)
      if (c.actorY + c.computedPadTop < yy) scala.util.boundary.break(row)
      if (c.endRow) row += 1
      i += 1
    }
    -1
  }

  def setSkin(skin: Nullable[Skin]): Unit = this.skin = skin.map(s => s: Any)

  /** If true (the default), positions and sizes of child actors are rounded and ceiled to the nearest integer value. */
  def setRound(round: Boolean): Unit =
    this.round = round

  def getRows: Int = rows

  def getColumns: Int = columns

  /** Returns the height of the specified row, or 0 if the table layout has not been validated. */
  def getRowHeight(rowIndex: Int): Float =
    if (_rowHeight.isEmpty) 0f
    else _rowHeight(rowIndex)

  /** Returns the min height of the specified row. */
  def getRowMinHeight(rowIndex: Int): Float = {
    if (sizeInvalid) computeSize()
    rowMinHeight(rowIndex)
  }

  /** Returns the pref height of the specified row. */
  def getRowPrefHeight(rowIndex: Int): Float = {
    if (sizeInvalid) computeSize()
    rowPrefHeight(rowIndex)
  }

  /** Returns the width of the specified column, or 0 if the table layout has not been validated. */
  def getColumnWidth(columnIndex: Int): Float =
    if (_columnWidth.isEmpty) 0f
    else _columnWidth(columnIndex)

  /** Returns the min width of the specified column. */
  def getColumnMinWidth(columnIndex: Int): Float = {
    if (sizeInvalid) computeSize()
    columnMinWidth(columnIndex)
  }

  /** Returns the pref width of the specified column. */
  def getColumnPrefWidth(columnIndex: Int): Float = {
    if (sizeInvalid) computeSize()
    columnPrefWidth(columnIndex)
  }

  private def ensureSize(array: Array[Float], size: Int): Array[Float] =
    if (array.length < size) new Array[Float](size)
    else {
      java.util.Arrays.fill(array, 0, size, 0f)
      array
    }

  private def computeSize(): Unit = {
    sizeInvalid = false

    val cellCount = cells.size

    // Implicitly end the row for layout purposes.
    if (cellCount > 0 && !cells(cellCount - 1).endRow) {
      endRow()
      implicitEndRow = true
    }

    val columns = this.columns
    val rows    = this.rows
    this.columnMinWidth = ensureSize(this.columnMinWidth, columns)
    this.rowMinHeight = ensureSize(this.rowMinHeight, rows)
    this.columnPrefWidth = ensureSize(this.columnPrefWidth, columns)
    this.rowPrefHeight = ensureSize(this.rowPrefHeight, rows)
    this._columnWidth = ensureSize(this._columnWidth, columns)
    this._rowHeight = ensureSize(this._rowHeight, rows)
    this.expandWidth = ensureSize(this.expandWidth, columns)
    this.expandHeight = ensureSize(this.expandHeight, rows)
    val columnMinWidth  = this.columnMinWidth
    val rowMinHeight    = this.rowMinHeight
    val columnPrefWidth = this.columnPrefWidth
    val rowPrefHeight   = this.rowPrefHeight
    val expandWidth     = this.expandWidth
    val expandHeight    = this.expandHeight

    var spaceRightLast = 0f
    var i              = 0
    while (i < cellCount) {
      val c       = cells(i)
      val column  = c.column
      val row     = c._row
      val colspan = c.colspan.getOrElse(1)
      val a       = c.actor

      // Collect rows that expand and colspan=1 columns that expand.
      val cExpandY = c._expandY.getOrElse(0)
      if (cExpandY != 0 && expandHeight(row) == 0) expandHeight(row) = cExpandY.toFloat
      val cExpandX = c._expandX.getOrElse(0)
      if (colspan == 1 && cExpandX != 0 && expandWidth(column) == 0) expandWidth(column) = cExpandX.toFloat

      // Compute combined padding/spacing for cells.
      // Spacing between actors isn't additive, the larger is used. Also, no spacing around edges.
      c.computedPadLeft = c.padLeft.map(_.get(a)).getOrElse(0f) + (if (column == 0) 0f
                                                                   else Math.max(0f, c.spaceLeft.map(_.get(a)).getOrElse(0f) - spaceRightLast))
      c.computedPadTop = c.padTop.map(_.get(a)).getOrElse(0f)
      if (c.cellAboveIndex != -1) {
        val above = cells(c.cellAboveIndex)
        c.computedPadTop += Math.max(0f, c.spaceTop.map(_.get(a)).getOrElse(0f) - above.spaceBottom.map(_.get(a)).getOrElse(0f))
      }
      val spaceRight = c.spaceRight.map(_.get(a)).getOrElse(0f)
      c.computedPadRight = c.padRight.map(_.get(a)).getOrElse(0f) + (if ((column + colspan) == columns) 0f else spaceRight)
      c.computedPadBottom = c.padBottom.map(_.get(a)).getOrElse(0f) + (if (row == rows - 1) 0f else c.spaceBottom.map(_.get(a)).getOrElse(0f))
      spaceRightLast = spaceRight

      // Determine minimum and preferred cell sizes.
      var prefWidth  = c.prefWidth.map(_.get(a)).getOrElse(0f)
      var prefHeight = c.prefHeight.map(_.get(a)).getOrElse(0f)
      var minWidth   = c.minWidth.map(_.get(a)).getOrElse(0f)
      var minHeight  = c.minHeight.map(_.get(a)).getOrElse(0f)
      val maxWidth   = c.maxWidth.map(_.get(a)).getOrElse(0f)
      val maxHeight  = c.maxHeight.map(_.get(a)).getOrElse(0f)
      if (prefWidth < minWidth) prefWidth = minWidth
      if (prefHeight < minHeight) prefHeight = minHeight
      if (maxWidth > 0 && prefWidth > maxWidth) prefWidth = maxWidth
      if (maxHeight > 0 && prefHeight > maxHeight) prefHeight = maxHeight
      if (round) {
        minWidth = Math.ceil(minWidth.toDouble).toFloat
        minHeight = Math.ceil(minHeight.toDouble).toFloat
        prefWidth = Math.ceil(prefWidth.toDouble).toFloat
        prefHeight = Math.ceil(prefHeight.toDouble).toFloat
      }

      if (colspan == 1) { // Spanned column min and pref width is added later.
        val hpadding = c.computedPadLeft + c.computedPadRight
        columnPrefWidth(column) = Math.max(columnPrefWidth(column), prefWidth + hpadding)
        columnMinWidth(column) = Math.max(columnMinWidth(column), minWidth + hpadding)
      }
      val vpadding = c.computedPadTop + c.computedPadBottom
      rowPrefHeight(row) = Math.max(rowPrefHeight(row), prefHeight + vpadding)
      rowMinHeight(row) = Math.max(rowMinHeight(row), minHeight + vpadding)
      i += 1
    }

    var uniformMinWidth   = 0f
    var uniformMinHeight  = 0f
    var uniformPrefWidth  = 0f
    var uniformPrefHeight = 0f
    i = 0
    while (i < cellCount) {
      val c      = cells(i)
      val column = c.column

      // Colspan with expand will expand all spanned columns if none of the spanned columns have expand.
      val cExpandX = c._expandX.getOrElse(0)
      if (cExpandX != 0) {
        val nn        = column + c.colspan.getOrElse(1)
        var hasExpand = false
        var ii        = column
        while (ii < nn) {
          if (expandWidth(ii) != 0) hasExpand = true
          ii += 1
        }
        if (!hasExpand) {
          ii = column
          while (ii < nn) {
            expandWidth(ii) = cExpandX.toFloat
            ii += 1
          }
        }
      }

      // Collect uniform sizes.
      if (c._uniformX.getOrElse(false) && c.colspan.getOrElse(1) == 1) {
        val hpadding = c.computedPadLeft + c.computedPadRight
        uniformMinWidth = Math.max(uniformMinWidth, columnMinWidth(column) - hpadding)
        uniformPrefWidth = Math.max(uniformPrefWidth, columnPrefWidth(column) - hpadding)
      }
      if (c._uniformY.getOrElse(false)) {
        val vpadding = c.computedPadTop + c.computedPadBottom
        uniformMinHeight = Math.max(uniformMinHeight, rowMinHeight(c._row) - vpadding)
        uniformPrefHeight = Math.max(uniformPrefHeight, rowPrefHeight(c._row) - vpadding)
      }
      i += 1
    }

    // Size uniform cells to the same width/height.
    if (uniformPrefWidth > 0 || uniformPrefHeight > 0) {
      i = 0
      while (i < cellCount) {
        val c = cells(i)
        if (uniformPrefWidth > 0 && c._uniformX.getOrElse(false) && c.colspan.getOrElse(1) == 1) {
          val hpadding = c.computedPadLeft + c.computedPadRight
          columnMinWidth(c.column) = uniformMinWidth + hpadding
          columnPrefWidth(c.column) = uniformPrefWidth + hpadding
        }
        if (uniformPrefHeight > 0 && c._uniformY.getOrElse(false)) {
          val vpadding = c.computedPadTop + c.computedPadBottom
          rowMinHeight(c._row) = uniformMinHeight + vpadding
          rowPrefHeight(c._row) = uniformPrefHeight + vpadding
        }
        i += 1
      }
    }

    // Distribute any additional min and pref width added by colspanned cells to the columns spanned.
    i = 0
    while (i < cellCount) {
      val c       = cells(i)
      val colspan = c.colspan.getOrElse(1)
      if (colspan != 1) {
        val column    = c.column
        val a         = c.actor
        var minWidth  = c.minWidth.map(_.get(a)).getOrElse(0f)
        var prefWidth = c.prefWidth.map(_.get(a)).getOrElse(0f)
        val maxWidth  = c.maxWidth.map(_.get(a)).getOrElse(0f)
        if (prefWidth < minWidth) prefWidth = minWidth
        if (maxWidth > 0 && prefWidth > maxWidth) prefWidth = maxWidth
        if (round) {
          minWidth = Math.ceil(minWidth.toDouble).toFloat
          prefWidth = Math.ceil(prefWidth.toDouble).toFloat
        }

        var spannedMinWidth  = -(c.computedPadLeft + c.computedPadRight)
        var spannedPrefWidth = spannedMinWidth
        var totalExpandWidth = 0f
        var ii               = column
        val nn               = ii + colspan
        while (ii < nn) {
          spannedMinWidth += columnMinWidth(ii)
          spannedPrefWidth += columnPrefWidth(ii)
          totalExpandWidth += expandWidth(ii) // Distribute extra space using expand, if any columns have expand.
          ii += 1
        }

        val extraMinWidth  = Math.max(0f, minWidth - spannedMinWidth)
        val extraPrefWidth = Math.max(0f, prefWidth - spannedPrefWidth)
        ii = column
        while (ii < nn) {
          val ratio = if (totalExpandWidth == 0) 1f / colspan else expandWidth(ii) / totalExpandWidth
          columnMinWidth(ii) += extraMinWidth * ratio
          columnPrefWidth(ii) += extraPrefWidth * ratio
          ii += 1
        }
      }
      i += 1
    }

    // Determine table min and pref size.
    val hpadding = padLeft.get(Nullable(this)) + padRight.get(Nullable(this))
    val vpadding = padTop.get(Nullable(this)) + padBottom.get(Nullable(this))
    tableMinWidth = hpadding
    tablePrefWidth = hpadding
    i = 0
    while (i < columns) {
      tableMinWidth += columnMinWidth(i)
      tablePrefWidth += columnPrefWidth(i)
      i += 1
    }
    tableMinHeight = vpadding
    tablePrefHeight = vpadding
    i = 0
    while (i < rows) {
      tableMinHeight += rowMinHeight(i)
      tablePrefHeight += Math.max(rowMinHeight(i), rowPrefHeight(i))
      i += 1
    }
    tablePrefWidth = Math.max(tableMinWidth, tablePrefWidth)
    tablePrefHeight = Math.max(tableMinHeight, tablePrefHeight)
  }

  /** Positions and sizes children of the table using the cell associated with each child. The values given are the position within the parent and size of the table.
    */
  override def layout(): Unit = {
    if (sizeInvalid) computeSize()

    val layoutWidth  = getWidth
    val layoutHeight = getHeight
    val columns      = this.columns
    val rows         = this.rows
    val columnWidth  = this._columnWidth
    val rowHeight    = this._rowHeight
    val padLeft      = this.padLeft.get(Nullable(this))
    val hpadding     = padLeft + this.padRight.get(Nullable(this))
    val padTop       = this.padTop.get(Nullable(this))
    val vpadding     = padTop + this.padBottom.get(Nullable(this))

    // Size columns and rows between min and pref size using (preferred - min) size to weight distribution of extra space.
    val totalGrowWidth = tablePrefWidth - tableMinWidth
    val columnWeightedWidth: Array[Float] =
      if (totalGrowWidth == 0) columnMinWidth
      else {
        val extraWidth = Math.min(totalGrowWidth, Math.max(0f, layoutWidth - tableMinWidth))
        val cww        = ensureSize(Table._columnWeightedWidth, columns)
        Table._columnWeightedWidth = cww
        val columnMinWidth  = this.columnMinWidth
        val columnPrefWidth = this.columnPrefWidth
        var i               = 0
        while (i < columns) {
          val growWidth = columnPrefWidth(i) - columnMinWidth(i)
          val growRatio = growWidth / totalGrowWidth
          cww(i) = columnMinWidth(i) + extraWidth * growRatio
          i += 1
        }
        cww
      }

    val totalGrowHeight = tablePrefHeight - tableMinHeight
    val rowWeightedHeight: Array[Float] =
      if (totalGrowHeight == 0) rowMinHeight
      else {
        val rwh = ensureSize(Table._rowWeightedHeight, rows)
        Table._rowWeightedHeight = rwh
        val extraHeight   = Math.min(totalGrowHeight, Math.max(0f, layoutHeight - tableMinHeight))
        val rowMinHeight  = this.rowMinHeight
        val rowPrefHeight = this.rowPrefHeight
        var i             = 0
        while (i < rows) {
          val growHeight = rowPrefHeight(i) - rowMinHeight(i)
          val growRatio  = growHeight / totalGrowHeight
          rwh(i) = rowMinHeight(i) + extraHeight * growRatio
          i += 1
        }
        rwh
      }

    // Determine actor and cell sizes (before expand or fill).
    val cellCount = cells.size
    var i         = 0
    while (i < cellCount) {
      val c      = cells(i)
      val column = c.column
      val row    = c._row
      val a      = c.actor

      var spannedWeightedWidth = 0f
      val colspan              = c.colspan.getOrElse(1)
      var ii                   = column
      val nn                   = ii + colspan
      while (ii < nn) {
        spannedWeightedWidth += columnWeightedWidth(ii)
        ii += 1
      }
      val weightedHeight = rowWeightedHeight(row)

      var prefWidth  = c.prefWidth.map(_.get(a)).getOrElse(0f)
      var prefHeight = c.prefHeight.map(_.get(a)).getOrElse(0f)
      val minWidth   = c.minWidth.map(_.get(a)).getOrElse(0f)
      val minHeight  = c.minHeight.map(_.get(a)).getOrElse(0f)
      val maxWidth   = c.maxWidth.map(_.get(a)).getOrElse(0f)
      val maxHeight  = c.maxHeight.map(_.get(a)).getOrElse(0f)
      if (prefWidth < minWidth) prefWidth = minWidth
      if (prefHeight < minHeight) prefHeight = minHeight
      if (maxWidth > 0 && prefWidth > maxWidth) prefWidth = maxWidth
      if (maxHeight > 0 && prefHeight > maxHeight) prefHeight = maxHeight

      c.actorWidth = Math.min(spannedWeightedWidth - c.computedPadLeft - c.computedPadRight, prefWidth)
      c.actorHeight = Math.min(weightedHeight - c.computedPadTop - c.computedPadBottom, prefHeight)

      if (colspan == 1) columnWidth(column) = Math.max(columnWidth(column), spannedWeightedWidth)
      rowHeight(row) = Math.max(rowHeight(row), weightedHeight)
      i += 1
    }

    // Distribute remaining space to any expanding columns/rows.
    val expandWidth  = this.expandWidth
    val expandHeight = this.expandHeight
    var totalExpand  = 0f
    i = 0
    while (i < columns) {
      totalExpand += expandWidth(i)
      i += 1
    }
    if (totalExpand > 0) {
      var extra = layoutWidth - hpadding
      i = 0
      while (i < columns) {
        extra -= columnWidth(i)
        i += 1
      }
      if (extra > 0) { // layoutWidth < tableMinWidth.
        var used      = 0f
        var lastIndex = 0
        i = 0
        while (i < columns) {
          if (expandWidth(i) != 0) {
            val amount = extra * expandWidth(i) / totalExpand
            columnWidth(i) += amount
            used += amount
            lastIndex = i
          }
          i += 1
        }
        columnWidth(lastIndex) += extra - used
      }
    }

    totalExpand = 0f
    i = 0
    while (i < rows) {
      totalExpand += expandHeight(i)
      i += 1
    }
    if (totalExpand > 0) {
      var extra = layoutHeight - vpadding
      i = 0
      while (i < rows) {
        extra -= rowHeight(i)
        i += 1
      }
      if (extra > 0) { // layoutHeight < tableMinHeight.
        var used      = 0f
        var lastIndex = 0
        i = 0
        while (i < rows) {
          if (expandHeight(i) != 0) {
            val amount = extra * expandHeight(i) / totalExpand
            rowHeight(i) += amount
            used += amount
            lastIndex = i
          }
          i += 1
        }
        rowHeight(lastIndex) += extra - used
      }
    }

    // Distribute any additional width added by colspanned cells to the columns spanned.
    i = 0
    while (i < cellCount) {
      val c       = cells(i)
      val colspan = c.colspan.getOrElse(1)
      if (colspan != 1) {
        var extraWidth = 0f
        var column     = c.column
        val nn         = column + colspan
        while (column < nn) {
          extraWidth += columnWeightedWidth(column) - columnWidth(column)
          column += 1
        }
        extraWidth -= Math.max(0f, c.computedPadLeft + c.computedPadRight)

        extraWidth /= colspan
        if (extraWidth > 0) {
          column = c.column
          while (column < nn) {
            columnWidth(column) += extraWidth
            column += 1
          }
        }
      }
      i += 1
    }

    // Determine table size.
    var tableWidth  = hpadding
    var tableHeight = vpadding
    i = 0
    while (i < columns) {
      tableWidth += columnWidth(i)
      i += 1
    }
    i = 0
    while (i < rows) {
      tableHeight += rowHeight(i)
      i += 1
    }

    // Position table within the container.
    val align = this.tableAlign
    var x     = padLeft
    if (align.isRight)
      x += layoutWidth - tableWidth
    else if (!align.isLeft) // Center
      x += (layoutWidth - tableWidth) / 2

    var y = padTop
    if (align.isBottom)
      y += layoutHeight - tableHeight
    else if (!align.isTop) // Center
      y += (layoutHeight - tableHeight) / 2

    // Size and position actors within cells.
    var currentX = x
    var currentY = y
    i = 0
    while (i < cellCount) {
      val c = cells(i)

      var spannedCellWidth = 0f
      var column           = c.column
      val nn               = column + c.colspan.getOrElse(1)
      while (column < nn) {
        spannedCellWidth += columnWidth(column)
        column += 1
      }
      spannedCellWidth -= c.computedPadLeft + c.computedPadRight

      currentX += c.computedPadLeft

      val fillX = c._fillX.getOrElse(0f)
      val fillY = c._fillY.getOrElse(0f)
      if (fillX > 0) {
        c.actorWidth = Math.max(spannedCellWidth * fillX, c.minWidth.map(_.get(c.actor)).getOrElse(0f))
        val maxWidth = c.maxWidth.map(_.get(c.actor)).getOrElse(0f)
        if (maxWidth > 0) c.actorWidth = Math.min(c.actorWidth, maxWidth)
      }
      if (fillY > 0) {
        c.actorHeight = Math.max(rowHeight(c._row) * fillY - c.computedPadTop - c.computedPadBottom, c.minHeight.map(_.get(c.actor)).getOrElse(0f))
        val maxHeight = c.maxHeight.map(_.get(c.actor)).getOrElse(0f)
        if (maxHeight > 0) c.actorHeight = Math.min(c.actorHeight, maxHeight)
      }

      val cellAlign = c.align.getOrElse(Align.center)
      if (cellAlign.isLeft)
        c.actorX = currentX
      else if (cellAlign.isRight)
        c.actorX = currentX + spannedCellWidth - c.actorWidth
      else
        c.actorX = currentX + (spannedCellWidth - c.actorWidth) / 2

      if (cellAlign.isTop)
        c.actorY = c.computedPadTop
      else if (cellAlign.isBottom)
        c.actorY = rowHeight(c._row) - c.actorHeight - c.computedPadBottom
      else
        c.actorY = (rowHeight(c._row) - c.actorHeight + c.computedPadTop - c.computedPadBottom) / 2
      c.actorY = layoutHeight - currentY - c.actorY - c.actorHeight

      if (round) {
        c.actorWidth = Math.ceil(c.actorWidth.toDouble).toFloat
        c.actorHeight = Math.ceil(c.actorHeight.toDouble).toFloat
        c.actorX = Math.floor(c.actorX.toDouble).toFloat
        c.actorY = Math.floor(c.actorY.toDouble).toFloat
      }

      c.actor.foreach(_.setBounds(c.actorX, c.actorY, c.actorWidth, c.actorHeight))

      if (c.endRow) {
        currentX = x
        currentY += rowHeight(c._row)
      } else {
        currentX += spannedCellWidth + c.computedPadRight
      }
      i += 1
    }

    // Validate all children (some may not be in cells).
    val childrenArray = getChildren
    i = 0
    while (i < childrenArray.size) {
      childrenArray(i) match {
        case l: Layout => l.validate()
        case _ =>
      }
      i += 1
    }

    // Store debug rectangles.
    if (tableDebug != Table.Debug.none) addDebugRects(x, y, tableWidth - hpadding, tableHeight - vpadding)
  }

  private def addDebugRects(currentX: Float, currentY: Float, width: Float, height: Float): Unit = {
    clearDebugRects()
    if (tableDebug == Table.Debug.table || tableDebug == Table.Debug.all) {
      // Table actor bounds.
      addDebugRect(0, 0, getWidth, getHeight, debugTableColor)
      // Table bounds.
      addDebugRect(currentX, getHeight - currentY, width, -height, debugTableColor)
    }
    val x  = currentX
    var cx = currentX
    var cy = currentY
    var i  = 0
    while (i < cells.size) {
      val c = cells(i)

      // Cell actor bounds.
      if (tableDebug == Table.Debug.actor || tableDebug == Table.Debug.all)
        addDebugRect(c.actorX, c.actorY, c.actorWidth, c.actorHeight, debugActorColor)

      // Cell bounds.
      var spannedCellWidth = 0f
      var column           = c.column
      val nn               = column + c.colspan.getOrElse(1)
      while (column < nn) {
        spannedCellWidth += _columnWidth(column)
        column += 1
      }
      spannedCellWidth -= c.computedPadLeft + c.computedPadRight
      cx += c.computedPadLeft
      if (tableDebug == Table.Debug.cell || tableDebug == Table.Debug.all) {
        val h = _rowHeight(c._row) - c.computedPadTop - c.computedPadBottom
        val y = cy + c.computedPadTop
        addDebugRect(cx, getHeight - y, spannedCellWidth, -h, debugCellColor)
      }

      if (c.endRow) {
        cx = x
        cy += _rowHeight(c._row)
      } else {
        cx += spannedCellWidth + c.computedPadRight
      }
      i += 1
    }
  }

  private def clearDebugRects(): Unit = {
    if (debugRects.isEmpty) debugRects = Nullable(DynamicArray[DebugRect]())
    debugRects.foreach { rects =>
      DebugRect.pool.freeAll(rects)
      rects.clear()
    }
  }

  private def addDebugRect(x: Float, y: Float, w: Float, h: Float, color: Color): Unit = {
    val rect = DebugRect.pool.obtain()
    rect.color = color
    rect.set(x, y, w, h)
    debugRects.foreach(_ += rect)
  }

  override def drawDebug(shapes: ShapeRenderer): Unit =
    if (isTransform) {
      applyTransform(shapes, computeTransform())
      drawDebugRects(shapes)
      if (_clip) {
        // TODO: add shapes.flush() when ShapeRenderer supports it
        var x      = 0f
        var y      = 0f
        var width  = getWidth
        var height = getHeight
        background.foreach { bg =>
          x = padLeft.get(Nullable(this))
          y = padBottom.get(Nullable(this))
          width -= x + padRight.get(Nullable(this))
          height -= y + padTop.get(Nullable(this))
        }
        if (clipBegin(x, y, width, height)) {
          drawDebugChildren(shapes)
          clipEnd()
        }
      } else {
        drawDebugChildren(shapes)
      }
      resetTransform(shapes)
    } else {
      drawDebugRects(shapes)
      super.drawDebug(shapes)
    }

  override protected def drawDebugBounds(shapes: ShapeRenderer): Unit = {}

  private def drawDebugRects(shapes: ShapeRenderer): Unit =
    debugRects.foreach { rects =>
      if (getDebug) {
        shapes.set(shapes.ShapeType.Line)
        getStage.foreach(s => shapes.setColor(s.getDebugColor))
        var x = 0f
        var y = 0f
        if (!isTransform) {
          x = getX
          y = getY
        }
        var i = 0
        while (i < rects.size) {
          val debugRect = rects(i)
          shapes.setColor(debugRect.color)
          shapes.rectangle(x + debugRect.x, y + debugRect.y, debugRect.width, debugRect.height)
          i += 1
        }
      }
    }

  /** @return The skin that was passed to this table in its constructor, or null if none was given. */
  // TODO: change to Nullable[Skin] when Skin is ported
  def getSkin: Nullable[Any] = skin
}

object Table {
  var debugTableColor: Color = Color(0, 0, 1, 1)
  var debugCellColor:  Color = Color(1, 0, 0, 1)
  var debugActorColor: Color = Color(0, 1, 0, 1)

  val cellPool: Pool[Cell[?]] = Pool.Default[Cell[?]](() => Cell[Actor]())

  private var _columnWeightedWidth: Array[Float] = Array.empty
  private var _rowWeightedHeight:   Array[Float] = Array.empty

  /** @author Nathan Sweet */
  class DebugRect extends Rectangle {
    var color: Color = scala.compiletime.uninitialized
  }

  object DebugRect {
    val pool: Pool[DebugRect] = Pool.Default[DebugRect](() => DebugRect())
  }

  /** @author Nathan Sweet */
  enum Debug {
    case none, all, table, cell, actor
  }

  /** Value that is the top padding of the table's background.
    * @author
    *   Nathan Sweet
    */
  val backgroundTop: Value = new Value {
    def get(context: Nullable[Actor]): Float =
      context.fold(0f) {
        case t: Table => t.background.map(_.getTopHeight).getOrElse(0f)
        case _ => 0f
      }
  }

  /** Value that is the left padding of the table's background.
    * @author
    *   Nathan Sweet
    */
  val backgroundLeft: Value = new Value {
    def get(context: Nullable[Actor]): Float =
      context.fold(0f) {
        case t: Table => t.background.map(_.getLeftWidth).getOrElse(0f)
        case _ => 0f
      }
  }

  /** Value that is the bottom padding of the table's background.
    * @author
    *   Nathan Sweet
    */
  val backgroundBottom: Value = new Value {
    def get(context: Nullable[Actor]): Float =
      context.fold(0f) {
        case t: Table => t.background.map(_.getBottomHeight).getOrElse(0f)
        case _ => 0f
      }
  }

  /** Value that is the right padding of the table's background.
    * @author
    *   Nathan Sweet
    */
  val backgroundRight: Value = new Value {
    def get(context: Nullable[Actor]): Float =
      context.fold(0f) {
        case t: Table => t.background.map(_.getRightWidth).getOrElse(0f)
        case _ => 0f
      }
  }
}
