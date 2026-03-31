/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: MJ
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package building

import sge.scenes.scene2d.ui.Table
import sge.utils.Nullable
import sge.visui.building.utilities.Padding

/** Builds a standard table with the appended widgets. Each table's row will have the same colspan.
  * @author
  *   MJ
  */
class StandardTableBuilder(estimatedWidgetsAmount: Int, estimatedRowsAmount: Int, defaultWidgetPadding: Padding)
    extends TableBuilder(estimatedWidgetsAmount, estimatedRowsAmount, defaultWidgetPadding) {

  def this() = this(10, 3, Padding.PAD_0)
  def this(defaultWidgetPadding: Padding) = this(10, 3, defaultWidgetPadding)
  def this(estimatedWidgetsAmount: Int, estimatedRowsAmount: Int) = this(estimatedWidgetsAmount, estimatedRowsAmount, Padding.PAD_0)

  override protected def fillTable(table: Table): Unit = {
    val rowSizes       = getRowSizes
    val widgetsInRow   = TableBuilder.getLowestCommonMultiple(rowSizes)
    var rowIndex       = 0
    var widgetIndex    = 0
    while (rowIndex < rowSizes.size) {
      val rowSize              = rowSizes(rowIndex)
      val currentWidgetColspan = widgetsInRow / rowSize
      val totalWidgets         = widgetIndex + rowSize
      while (widgetIndex < totalWidgets) {
        getWidget(widgetIndex).buildCell(table, Nullable(getDefaultWidgetPadding)).colspan(currentWidgetColspan)
        widgetIndex += 1
      }
      table.row()
      rowIndex += 1
    }
  }
}
