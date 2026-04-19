/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: MJ
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 60
 * Covenant-baseline-methods: Alignment,apply,getByIndex,getByValidIndex,isAlignedWithBottom,isAlignedWithLeft,isAlignedWithRight,isAlignedWithTop,isCentered,isIndexLast,isIndexValid,vals
 * Covenant-source-reference: com/kotcrab/vis/ui/building/utilities/Alignment.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package building
package utilities

import sge.scenes.scene2d.ui.Cell
import sge.utils.Align

/** Wraps all default alignments, allowing to validate if the alignment value is actually correct.
  * @author
  *   MJ
  */
enum Alignment(val alignment: Align) {
  case CENTER extends Alignment(Align.center)
  case TOP extends Alignment(Align.top)
  case BOTTOM extends Alignment(Align.bottom)
  case LEFT extends Alignment(Align.left)
  case RIGHT extends Alignment(Align.right)
  case TOP_LEFT extends Alignment(Align.topLeft)
  case TOP_RIGHT extends Alignment(Align.topRight)
  case BOTTOM_LEFT extends Alignment(Align.bottomLeft)
  case BOTTOM_RIGHT extends Alignment(Align.bottomRight)

  def apply(cell: Cell[?]): Unit = cell.align(alignment)

  /** @return true for TOP, TOP_LEFT and TOP_RIGHT. */
  def isAlignedWithTop: Boolean = alignment.isTop

  /** @return true for BOTTOM, BOTTOM_LEFT and BOTTOM_RIGHT. */
  def isAlignedWithBottom: Boolean = alignment.isBottom

  /** @return true for LEFT, BOTTOM_LEFT and TOP_LEFT. */
  def isAlignedWithLeft: Boolean = alignment.isLeft

  /** @return true for RIGHT, BOTTOM_RIGHT and TOP_RIGHT. */
  def isAlignedWithRight: Boolean = alignment.isRight

  /** @return true for CENTER. */
  def isCentered: Boolean = alignment.isCenter
}

object Alignment {
  def getByIndex(index: Int): Alignment = {
    val vals = Alignment.values
    if (index >= 0 && index < vals.length) vals(index)
    else null // @nowarn -- Java interop boundary: returns null for invalid index
  }

  def getByValidIndex(index: Int): Alignment = Alignment.values(index)

  def isIndexValid(index: Int): Boolean = index >= 0 && index < Alignment.values.length
  def isIndexLast(index:  Int): Boolean = index == Alignment.values.length - 1
}
