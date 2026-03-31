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

/** Ignores row() calls and builds table with all widgets put into one row.
  * @author
  *   MJ
  */
class OneRowTableBuilder(estimatedWidgetsAmount: Int, estimatedRowsAmount: Int, defaultWidgetPadding: Padding) extends TableBuilder(estimatedWidgetsAmount, estimatedRowsAmount, defaultWidgetPadding) {

  def this() = this(10, 3, Padding.PAD_0)
  def this(defaultWidgetPadding:   Padding) = this(10, 3, defaultWidgetPadding)
  def this(estimatedWidgetsAmount: Int, estimatedRowsAmount: Int) = this(estimatedWidgetsAmount, estimatedRowsAmount, Padding.PAD_0)

  override protected def fillTable(table: Table): Unit = {
    val iter = getWidgets.iterator
    while (iter.hasNext) iter.next().buildCell(table, Nullable(getDefaultWidgetPadding))
  }
}
