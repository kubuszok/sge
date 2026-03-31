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
package utilities
package layouts

import sge.Sge
import sge.scenes.scene2d.Actor
import sge.visui.building.GridTableBuilder

/** Additional TableLayout with customizable variables. Converts passed widgets into a table using GridTableBuilder.
  * @author
  *   MJ
  */
class GridTableLayout(rowSize: Int) extends ActorLayout {

  override def convertToActor(widgets: Actor*)(using Sge): Actor =
    convertToActorFromCells(CellWidget.wrap(widgets*)*)

  override def convertToActorFromCells(widgets: CellWidget[?]*)(using Sge): Actor =
    TableLayout.convertToTable(new GridTableBuilder(rowSize), widgets*)
}

object GridTableLayout {
  def withRowSize(rowSize: Int): GridTableLayout = new GridTableLayout(rowSize)
}
