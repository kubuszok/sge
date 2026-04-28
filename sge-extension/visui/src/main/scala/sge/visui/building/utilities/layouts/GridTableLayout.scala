/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: MJ
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 33
 * Covenant-baseline-methods: GridTableLayout,convertToActor,convertToActorFromCells,withRowSize
 * Covenant-source-reference: com/kotcrab/vis/ui/building/utilities/layouts/GridTableLayout.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
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
