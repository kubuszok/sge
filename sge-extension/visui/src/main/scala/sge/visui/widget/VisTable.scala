/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 45
 * Covenant-baseline-methods: VisTable,addSeparator,cell,sep
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/VisTable.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget

import sge.scenes.scene2d.ui.{ Cell, Table }
import sge.utils.Nullable
import sge.visui.util.TableUtils

/** Extends functionality of standard [[Table]], supports setting default VisUI spacing and has utility methods for adding separators. Compatible with [[Table]].
  * @author
  *   Kotcrab
  * @see
  *   [[Table]]
  */
class VisTable(setVisDefaults: Boolean = false)(using Sge) extends Table(Nullable(VisUI.getSkin)) {

  if (setVisDefaults) TableUtils.setSpacingDefaults(this)

  /** Adds vertical or horizontal [[Separator]] widget to table with padding top, bottom 2px with fill and expand properties. If vertical == false then inserts new row after separator (not before!)
    */
  def addSeparator(vertical: Boolean): Cell[Separator] = {
    val sep  = new Separator(if (vertical) "vertical" else "default")
    val cell = add(Nullable[sge.scenes.scene2d.Actor](sep)).padTop(2).padBottom(2).asInstanceOf[Cell[Separator]]

    if (vertical) {
      cell.fillY().expandY()
    } else {
      cell.fillX().expandX()
      row()
    }

    cell
  }

  /** Adds horizontal [[Separator]] widget to table with padding top, bottom 2px with fillX and expandX properties and inserts new row after separator (not before!)
    */
  def addSeparator(): Cell[Separator] = addSeparator(false)
}
