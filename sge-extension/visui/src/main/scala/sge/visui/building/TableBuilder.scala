/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: MJ
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 96
 * Covenant-baseline-methods: TableBuilder,_rowSizes,append,build,currentRowSize,fillTable,getDefaultWidgetPadding,getGreatestCommonDenominator,getLowestCommonMultiple,getRowSizes,getWidget,getWidgets,i,lcm,prepareBuiltTable,prepareNewTable,row,setTablePadding,tablePadding,this,validateRowSize,widgets
 * Covenant-source-reference: com/kotcrab/vis/ui/building/TableBuilder.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package building

import scala.language.implicitConversions

import sge.Sge
import sge.scenes.scene2d.Actor
import sge.scenes.scene2d.ui.Table
import sge.utils.DynamicArray
import sge.visui.building.utilities.{ CellWidget, Padding }
import sge.visui.building.utilities.layouts.{ ActorLayout, TableLayout }

/** Allows to easily build Scene2D tables, without having to worry about different colspans of table's rows.
  * @author
  *   MJ
  */
abstract class TableBuilder(estimatedWidgetsAmount: Int, estimatedRowsAmount: Int, private val widgetPadding: Padding) {

  private val widgets:        DynamicArray[CellWidget[? <: Actor]] = DynamicArray[CellWidget[? <: Actor]](estimatedWidgetsAmount)
  private val _rowSizes:      DynamicArray[Int]                    = DynamicArray[Int](estimatedRowsAmount)
  private var currentRowSize: Int                                  = 0
  private var tablePadding:   Padding                              = scala.compiletime.uninitialized

  def this() = this(10, 3, Padding.PAD_0)
  def this(defaultWidgetPadding:   Padding) = this(10, 3, defaultWidgetPadding)
  def this(estimatedWidgetsAmount: Int, estimatedRowsAmount: Int) = this(estimatedWidgetsAmount, estimatedRowsAmount, Padding.PAD_0)

  def setTablePadding(tablePadding: Padding): TableBuilder = { this.tablePadding = tablePadding; this }

  protected def getDefaultWidgetPadding: Padding = widgetPadding

  def append(widget: Actor): TableBuilder = append(CellWidget.of(widget).padding(widgetPadding).wrap())

  def append(widget: CellWidget[? <: Actor]): TableBuilder = { widgets.add(widget); currentRowSize += 1; this }

  def append(widgets: Actor*)(using Sge): TableBuilder = append(TableLayout.HORIZONTAL, widgets*)

  def append(layout: ActorLayout, widgets: Actor*)(using Sge): TableBuilder = append(layout.convertToActor(widgets*))

  /** Appends an empty cell to the table. */
  def append(): TableBuilder = append(CellWidget.EMPTY.asInstanceOf[CellWidget[Actor]])

  def row(): TableBuilder = {
    if (currentRowSize != 0) { _rowSizes.add(currentRowSize); currentRowSize = 0 }
    this
  }

  def build()(using Sge): Table = build(new Table())

  def build[T <: Table](table: T): T = {
    prepareNewTable(table)
    if (widgets.size == 0) table
    else { fillTable(table); prepareBuiltTable(table) }
  }

  private def prepareNewTable(table: Table): Table = {
    validateRowSize()
    if (tablePadding != null) tablePadding.applyPadding(table) // @nowarn -- Java interop boundary
    table
  }

  protected def fillTable(table: Table): Unit

  private def prepareBuiltTable[T <: Table](table: T): T = { table.pack(); table }

  private def validateRowSize(): Unit = if (currentRowSize != 0) row()

  protected def getRowSizes: DynamicArray[Int] = _rowSizes

  protected def getWidget(index: Int): CellWidget[? <: Actor] = widgets(index)

  protected def getWidgets: DynamicArray[CellWidget[? <: Actor]] = widgets
}

object TableBuilder {
  def getGreatestCommonDenominator(valueA: Int, valueB: Int): Int =
    if (valueB == 0) valueA else getGreatestCommonDenominator(valueB, valueA % valueB)

  def getLowestCommonMultiple(valueA: Int, valueB: Int): Int =
    valueA * (valueB / getGreatestCommonDenominator(valueA, valueB))

  def getLowestCommonMultiple(values: DynamicArray[Int]): Int = {
    var lcm = values.first
    var i   = 1
    while (i < values.size) { lcm = getLowestCommonMultiple(lcm, values(i)); i += 1 }
    lcm
  }
}
