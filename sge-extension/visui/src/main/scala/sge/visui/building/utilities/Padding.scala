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

import sge.scenes.scene2d.ui.{ Cell, Table }

/** A simple helper class that holds informations about padding on each side of an object.
  * @author
  *   MJ
  */
class Padding(val top: Float, val left: Float, val bottom: Float, val right: Float) {

  /** @param padding will be set as padding for all directions. */
  def this(padding: Float) = this(padding, padding, padding, padding)

  /** @param horizontal will be set as left and right padding.
    * @param vertical will be set as top and bottom padding.
    */
  def this(horizontal: Float, vertical: Float) = this(vertical, horizontal, vertical, horizontal)

  /** @return new Padding object with summed pad values. */
  def add(padding: Padding): Padding = new Padding(top + padding.top, left + padding.left, bottom + padding.bottom, right + padding.right)

  /** @return new Padding object with subtracted pad values. */
  def subtract(padding: Padding): Padding = new Padding(top - padding.top, left - padding.left, bottom - padding.bottom, right - padding.right)

  /** @return new Padding object with reversed pad values. */
  def reverse(): Padding = new Padding(-top, -left, -bottom, -right)

  def applyPadding(table: Table): Table = { table.pad(top, left, bottom, right); table }
  def applyPadding(cell: Cell[?]): Cell[?] = { cell.pad(top, left, bottom, right); cell }
  def applySpacing(cell: Cell[?]): Cell[?] = { cell.space(top, left, bottom, right); cell }
}

object Padding {
  val PAD_0: Padding = of(0f)
  val PAD_2: Padding = of(2f)
  val PAD_4: Padding = of(4f)
  val PAD_8: Padding = of(8f)

  def of(padding: Float): Padding = new Padding(padding, padding, padding, padding)
  def of(horizontal: Float, vertical: Float): Padding = new Padding(vertical, horizontal, vertical, horizontal)
  def of(top: Float, left: Float, bottom: Float, right: Float): Padding = new Padding(top, left, bottom, right)

  def setPadding(padding: Padding, table: Table): Table = { table.pad(padding.top, padding.left, padding.bottom, padding.right); table }
  def setPadding(padding: Padding, cell: Cell[?]): Cell[?] = cell.pad(padding.top, padding.left, padding.bottom, padding.right)
  def setSpacing(spacing: Padding, cell: Cell[?]): Cell[?] = cell.space(spacing.top, spacing.left, spacing.bottom, spacing.right)
}
