/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 28
 * Covenant-baseline-methods: TableUtils,setSpacingDefaults,sizes
 * Covenant-source-reference: com/kotcrab/vis/ui/util/TableUtils.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package util

import sge.scenes.scene2d.ui.Table

/** Utilities for VisTable/Table.
  * @author
  *   Kotcrab
  */
object TableUtils {

  /** Sets default table spacing for VisUI skin. Uses values from current skin [[Sizes]] class obtained from [[VisUI.getSizes]]. */
  def setSpacingDefaults(table: Table): Unit = {
    val sizes = VisUI.getSizes
    if (sizes.spacingTop != 0) table.defaults().spaceTop(sizes.spacingTop)
    if (sizes.spacingBottom != 0) table.defaults().spaceBottom(sizes.spacingBottom)
    if (sizes.spacingRight != 0) table.defaults().spaceRight(sizes.spacingRight)
    if (sizes.spacingLeft != 0) table.defaults().spaceLeft(sizes.spacingLeft)
  }
}
