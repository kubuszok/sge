/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: MJ
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 36
 * Covenant-baseline-methods: GridTableBuilder,fillTable,iter,this,widgetsCounter
 * Covenant-source-reference: com/kotcrab/vis/ui/building/GridTableBuilder.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package building

import sge.scenes.scene2d.ui.Table
import sge.utils.Nullable
import sge.visui.building.utilities.Padding

/** Ignores row() calls and builds table with all widgets put into rows of given size.
  * @author
  *   MJ
  */
class GridTableBuilder(rowSize: Int, estimatedWidgetsAmount: Int, estimatedRowsAmount: Int, defaultWidgetPadding: Padding)
    extends TableBuilder(estimatedWidgetsAmount, estimatedRowsAmount, defaultWidgetPadding) {

  def this(rowSize:              Int) = this(rowSize, 10, 3, Padding.PAD_0)
  def this(defaultWidgetPadding: Padding, rowSize:            Int) = this(rowSize, 10, 3, defaultWidgetPadding)
  def this(rowSize:              Int, estimatedWidgetsAmount: Int, estimatedRowsAmount: Int) = this(rowSize, estimatedWidgetsAmount, estimatedRowsAmount, Padding.PAD_0)

  override protected def fillTable(table: Table): Unit = {
    var widgetsCounter = 0
    val iter           = getWidgets.iterator
    while (iter.hasNext) {
      iter.next().buildCell(table, Nullable(getDefaultWidgetPadding))
      widgetsCounter += 1
      if (widgetsCounter == rowSize) { widgetsCounter -= rowSize; table.row() }
    }
  }
}
