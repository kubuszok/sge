/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: MJ
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 50
 * Covenant-baseline-methods: TableLayout,convertToActor,convertToActorFromCells,convertToTable,grid
 * Covenant-source-reference: com/kotcrab/vis/ui/building/utilities/layouts/TableLayout.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package building
package utilities
package layouts

import sge.Sge
import sge.scenes.scene2d.Actor
import sge.visui.building.{ OneColumnTableBuilder, OneRowTableBuilder, TableBuilder }

/** Default ActorLayout implementations, using table builders that don't require row() calls to convert multiple actors into one cell.
  * @author
  *   MJ
  */
enum TableLayout extends ActorLayout {

  /** Converts passed widgets into a single column. */
  case VERTICAL

  /** Converts passed widgets into a single row. */
  case HORIZONTAL

  override def convertToActor(widgets: Actor*)(using Sge): Actor =
    convertToActorFromCells(CellWidget.wrap(widgets*)*)

  override def convertToActorFromCells(widgets: CellWidget[?]*)(using Sge): Actor =
    this match {
      case VERTICAL   => TableLayout.convertToTable(new OneColumnTableBuilder(), widgets*)
      case HORIZONTAL => TableLayout.convertToTable(new OneRowTableBuilder(), widgets*)
    }
}

object TableLayout {

  /** Utility method. Appends all widgets into the passed builder and creates a table with no additional settings. */
  def convertToTable(usingBuilder: TableBuilder, widgets: CellWidget[?]*)(using Sge): Actor = {
    for (widget <- widgets) usingBuilder.append(widget)
    usingBuilder.build()
  }

  /** @return a new instance of GridTableLayout that creates tables as grids with the specified row size. */
  def grid(rowSize: Int): GridTableLayout = GridTableLayout.withRowSize(rowSize)
}
