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

/** Builds a table with the appended widgets, trying to keep them centered.
  * @author
  *   MJ
  */
class CenteredTableBuilder(estimatedWidgetsAmount: Int, estimatedRowsAmount: Int, defaultWidgetPadding: Padding)
    extends TableBuilder(estimatedWidgetsAmount, estimatedRowsAmount, defaultWidgetPadding) {

  def this() = this(10, 3, Padding.PAD_0)
  def this(defaultWidgetPadding: Padding) = this(10, 3, defaultWidgetPadding)
  def this(estimatedWidgetsAmount: Int, estimatedRowsAmount: Int) = this(estimatedWidgetsAmount, estimatedRowsAmount, Padding.PAD_0)

  override protected def fillTable(table: Table): Unit = {
    val rowSizes     = getRowSizes
    val widgetsInRow = TableBuilder.getLowestCommonMultiple(rowSizes)
    var rowIndex     = 0
    var widgetIndex  = 0
    while (rowIndex < rowSizes.size) {
      val rowSize              = rowSizes(rowIndex)
      val currentWidgetColspan = widgetsInRow / rowSize
      var isFirst              = shouldExpand(rowSize)
      val totalWidgets         = widgetIndex + rowSize

      while (widgetIndex < totalWidgets) {
        val cell = getWidget(widgetIndex).buildCell(table, Nullable(getDefaultWidgetPadding)).colspan(currentWidgetColspan)
        if (isFirst) { isFirst = false; cell.expandX().right() }
        else if (isLast(widgetIndex, rowSize, totalWidgets)) cell.expandX().left()
        widgetIndex += 1
      }
      table.row()
      rowIndex += 1
    }
  }

  private def shouldExpand(rowSize: Int): Boolean = rowSize != 1
  private def isLast(widgetIndex: Int, rowSize: Int, totalWidgetsInRow: Int): Boolean = shouldExpand(rowSize) && widgetIndex == totalWidgetsInRow - 1
}
