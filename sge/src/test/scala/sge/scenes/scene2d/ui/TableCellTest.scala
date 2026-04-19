/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package scenes
package scene2d
package ui

import sge.utils.{ Align, Nullable }

/** Tests for Table cell management and Cell configuration. No GL context required. */
class TableCellTest extends munit.FunSuite {

  private def ctx(): Sge = SgeTestFixture.testSge()

  // ---------------------------------------------------------------------------
  // Table: add cells
  // ---------------------------------------------------------------------------

  test("Table starts with no cells") {
    given Sge = ctx()
    val table = Table()
    assertEquals(table.getCells.size, 0)
  }

  test("add(Nullable[Actor]) adds a cell") {
    given Sge = ctx()
    val table = Table()
    val actor = Actor()
    val cell  = table.add(Nullable(actor))
    assertEquals(table.getCells.size, 1)
    assert(cell.hasActor)
    assert(cell.getActor.exists(_ eq actor))
  }

  test("add with null actor creates empty cell") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable.empty[Actor])
    assertEquals(table.getCells.size, 1)
    assert(!cell.hasActor)
  }

  test("add() creates empty cell") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add()
    assertEquals(table.getCells.size, 1)
    assert(!cell.hasActor)
  }

  test("add multiple actors") {
    given Sge = ctx()
    val table = Table()
    val a1    = Actor()
    val a2    = Actor()
    val a3    = Actor()
    table.add(a1, a2, a3)
    assertEquals(table.getCells.size, 3)
  }

  // ---------------------------------------------------------------------------
  // Table: row management
  // ---------------------------------------------------------------------------

  test("row increments row count") {
    given Sge = ctx()
    val table = Table()
    table.add(Nullable(Actor()))
    table.row()
    table.add(Nullable(Actor()))
    table.row()

    // Rows are counted after endRow is called for each row
    assertEquals(table.getRows, 2)
  }

  test("cells are assigned correct row and column") {
    given Sge = ctx()
    val table = Table()
    val c1    = table.add(Nullable(Actor()))
    val c2    = table.add(Nullable(Actor()))
    table.row()
    val c3 = table.add(Nullable(Actor()))

    assertEquals(c1.column, 0)
    assertEquals(c1._row, 0)
    assertEquals(c2.column, 1)
    assertEquals(c2._row, 0)
    assertEquals(c3.column, 0)
    assertEquals(c3._row, 1)
  }

  // ---------------------------------------------------------------------------
  // Table: getCell
  // ---------------------------------------------------------------------------

  test("getCell finds cell by actor") {
    given Sge = ctx()
    val table = Table()
    val actor = Actor()
    table.add(Nullable(actor))
    val found = table.getCell(actor)
    assert(found.isDefined)
    assert(found.exists(_.getActor.exists(_ eq actor)))
  }

  test("getCell returns empty for unknown actor") {
    given Sge = ctx()
    val table = Table()
    table.add(Nullable(Actor()))
    val found = table.getCell(Actor())
    assert(found.isEmpty)
  }

  // ---------------------------------------------------------------------------
  // Table: clearChildren
  // ---------------------------------------------------------------------------

  test("clearChildren removes all cells and actors") {
    given Sge = ctx()
    val table = Table()
    val actor = Actor()
    table.add(Nullable(actor))
    table.add(Nullable(Actor()))

    table.clearChildren()
    assertEquals(table.getCells.size, 0)
    assertEquals(table.children.size, 0)
  }

  // ---------------------------------------------------------------------------
  // Table: stack
  // ---------------------------------------------------------------------------

  test("stack creates a Stack cell with actors") {
    given Sge = ctx()
    val table = Table()
    val a1    = Actor()
    val a2    = Actor()
    val cell  = table.stack(a1, a2)
    assertEquals(table.getCells.size, 1)
    assert(cell.hasActor)
  }

  // ---------------------------------------------------------------------------
  // Table: clip
  // ---------------------------------------------------------------------------

  test("clip sets clip and transform") {
    given Sge = ctx()
    val table = Table()
    assert(!table.isClip)
    table.setClip(true)
    assert(table.isClip)
    assert(table.transform)
  }

  // ---------------------------------------------------------------------------
  // Table: padding and alignment
  // ---------------------------------------------------------------------------

  test("Table default alignment is center") {
    given Sge = ctx()
    val table = Table()
    assertEquals(table.tableAlign, Align.center)
  }

  test("Table pad methods set padding values") {
    given Sge = ctx()
    val table = Table()
    table.pad(10f)
    assertEquals(table.getPadTop, 10f)
    assertEquals(table.getPadLeft, 10f)
    assertEquals(table.getPadBottom, 10f)
    assertEquals(table.getPadRight, 10f)
  }

  test("Table individual pad methods") {
    given Sge = ctx()
    val table = Table()
    table.padTop = Value.Fixed.valueOf(5f)
    table.padLeft = Value.Fixed.valueOf(10f)
    table.padBottom = Value.Fixed.valueOf(15f)
    table.padRight = Value.Fixed.valueOf(20f)
    assertEquals(table.getPadTop, 5f)
    assertEquals(table.getPadLeft, 10f)
    assertEquals(table.getPadBottom, 15f)
    assertEquals(table.getPadRight, 20f)
  }

  // ---------------------------------------------------------------------------
  // Table: debug
  // ---------------------------------------------------------------------------

  test("Table debug default is none") {
    given Sge = ctx()
    val table = Table()
    assertEquals(table.tableDebug, Table.Debug.none)
  }

  // ---------------------------------------------------------------------------
  // Table: defaults
  // ---------------------------------------------------------------------------

  test("Table touchable default is childrenOnly") {
    given Sge = ctx()
    val table = Table()
    assertEquals(table.touchable, Touchable.childrenOnly)
  }

  test("Table transform default is false") {
    given Sge = ctx()
    val table = Table()
    assert(!table.transform)
  }

  // ---------------------------------------------------------------------------
  // Cell: size configuration
  // ---------------------------------------------------------------------------

  test("Cell size sets all dimension constraints") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.size(100f)
    assertEquals(cell.getMinWidth, 100f)
    assertEquals(cell.getMinHeight, 100f)
    assertEquals(cell.getPrefWidth, 100f)
    assertEquals(cell.getPrefHeight, 100f)
    assertEquals(cell.getMaxWidth, 100f)
    assertEquals(cell.getMaxHeight, 100f)
  }

  test("Cell size with separate width and height") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.size(80f, 60f)
    assertEquals(cell.getMinWidth, 80f)
    assertEquals(cell.getMinHeight, 60f)
  }

  test("Cell width sets only width constraints") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.width(50f)
    assertEquals(cell.getMinWidth, 50f)
    assertEquals(cell.getPrefWidth, 50f)
    assertEquals(cell.getMaxWidth, 50f)
  }

  test("Cell height sets only height constraints") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.height(70f)
    assertEquals(cell.getMinHeight, 70f)
    assertEquals(cell.getPrefHeight, 70f)
    assertEquals(cell.getMaxHeight, 70f)
  }

  // ---------------------------------------------------------------------------
  // Cell: min/pref/max individually
  // ---------------------------------------------------------------------------

  test("Cell minSize sets min constraints") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.minSize(30f, 40f)
    assertEquals(cell.getMinWidth, 30f)
    assertEquals(cell.getMinHeight, 40f)
  }

  test("Cell prefSize sets pref constraints") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.prefSize(50f, 60f)
    assertEquals(cell.getPrefWidth, 50f)
    assertEquals(cell.getPrefHeight, 60f)
  }

  test("Cell maxSize sets max constraints") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.maxSize(200f, 300f)
    assertEquals(cell.getMaxWidth, 200f)
    assertEquals(cell.getMaxHeight, 300f)
  }

  // ---------------------------------------------------------------------------
  // Cell: spacing
  // ---------------------------------------------------------------------------

  test("Cell space sets all spacing") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.space(5f)
    assertEquals(cell.getSpaceTop, 5f)
    assertEquals(cell.getSpaceLeft, 5f)
    assertEquals(cell.getSpaceBottom, 5f)
    assertEquals(cell.getSpaceRight, 5f)
  }

  test("Cell space with individual values") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.space(1f, 2f, 3f, 4f)
    assertEquals(cell.getSpaceTop, 1f)
    assertEquals(cell.getSpaceLeft, 2f)
    assertEquals(cell.getSpaceBottom, 3f)
    assertEquals(cell.getSpaceRight, 4f)
  }

  test("Cell space rejects negative values") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    intercept[IllegalArgumentException] {
      cell.space(-1f)
    }
  }

  // ---------------------------------------------------------------------------
  // Cell: padding
  // ---------------------------------------------------------------------------

  test("Cell pad sets all padding") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.pad(10f)
    assertEquals(cell.getPadTop, 10f)
    assertEquals(cell.getPadLeft, 10f)
    assertEquals(cell.getPadBottom, 10f)
    assertEquals(cell.getPadRight, 10f)
  }

  test("Cell getPadX and getPadY") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.pad(5f, 10f, 15f, 20f)
    assertEquals(cell.getPadX, 30f) // left(10) + right(20)
    assertEquals(cell.getPadY, 20f) // top(5) + bottom(15)
  }

  // ---------------------------------------------------------------------------
  // Cell: fill
  // ---------------------------------------------------------------------------

  test("Cell fill sets fillX and fillY to 1") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.fill()
    assert(cell._fillX.exists(_ == 1f))
    assert(cell._fillY.exists(_ == 1f))
  }

  test("Cell fillX sets only fillX") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.fillX()
    assert(cell._fillX.exists(_ == 1f))
    assertEquals(cell._fillY.getOrElse(-1f), 0f) // default is 0
  }

  // ---------------------------------------------------------------------------
  // Cell: expand
  // ---------------------------------------------------------------------------

  test("Cell expand sets expandX and expandY to 1") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.expand()
    assert(cell._expandX.exists(_ == 1))
    assert(cell._expandY.exists(_ == 1))
  }

  // ---------------------------------------------------------------------------
  // Cell: grow (expand + fill)
  // ---------------------------------------------------------------------------

  test("Cell grow sets expand and fill") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.grow()
    assert(cell._expandX.exists(_ == 1))
    assert(cell._expandY.exists(_ == 1))
    assert(cell._fillX.exists(_ == 1f))
    assert(cell._fillY.exists(_ == 1f))
  }

  // ---------------------------------------------------------------------------
  // Cell: colspan
  // ---------------------------------------------------------------------------

  test("Cell colspan") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.colspan(3)
    assert(cell.colspan.exists(_ == 3))
  }

  // ---------------------------------------------------------------------------
  // Cell: alignment
  // ---------------------------------------------------------------------------

  test("Cell align sets alignment") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.align(Align.topLeft)
    assert(cell.align.exists(_ == Align.topLeft))
  }

  test("Cell center sets center alignment") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.center()
    assert(cell.align.exists(_ == Align.center))
  }

  test("Cell top adds top alignment") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.top()
    assert(cell.align.isDefined)
  }

  test("Cell left adds left alignment") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.left()
    assert(cell.align.isDefined)
  }

  // ---------------------------------------------------------------------------
  // Cell: uniform
  // ---------------------------------------------------------------------------

  test("Cell uniform sets both uniformX and uniformY") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.uniform()
    assert(cell._uniformX.exists(_ == true))
    assert(cell._uniformY.exists(_ == true))
  }

  // ---------------------------------------------------------------------------
  // Cell: setActor
  // ---------------------------------------------------------------------------

  test("Cell setActor replaces actor") {
    given Sge = ctx()
    val table = Table()
    val a1    = Actor()
    val a2    = Actor()
    val cell  = table.add(Nullable(a1))
    cell.setActor(Nullable(a2))
    assert(cell.getActor.exists(_ eq a2))
  }

  test("Cell clearActor removes actor") {
    given Sge = ctx()
    val table = Table()
    val actor = Actor()
    val cell  = table.add(Nullable(actor))
    cell.clearActor()
    assert(!cell.hasActor)
  }

  // ---------------------------------------------------------------------------
  // Cell: setActorBounds
  // ---------------------------------------------------------------------------

  test("Cell setActorBounds") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    cell.setActorBounds(10f, 20f, 100f, 200f)
    assertEquals(cell.actorX, 10f)
    assertEquals(cell.actorY, 20f)
    assertEquals(cell.actorWidth, 100f)
    assertEquals(cell.actorHeight, 200f)
  }

  // ---------------------------------------------------------------------------
  // Cell: fluent API returns this
  // ---------------------------------------------------------------------------

  test("Cell methods return this for fluent API") {
    given Sge = ctx()
    val table = Table()
    val cell  = table.add(Nullable(Actor()))
    val same  = cell.size(50f).pad(5f).fill().expand().colspan(2)
    assert(same eq cell)
  }
}
