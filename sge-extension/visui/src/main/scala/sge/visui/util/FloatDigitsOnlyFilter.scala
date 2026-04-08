/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: partial-port
 * Covenant-source-reference: vis-ui/ui/src/main/java/com/kotcrab/vis/ui/util/FloatDigitsOnlyFilter.java
 * Covenant-verified: 2026-04-08
 *
 * Partial-port debt:
 *   - Simplified TextField API: validation always uses full text rather than the
 *     original incremental insertion check (SGE TextField API differs from VisUI).
 */
package sge
package visui
package util

import sge.scenes.scene2d.ui.TextField

/** [[TextField.TextFieldFilter]] that only allows digits for float values.
  * @author
  *   Kotcrab
  */
class FloatDigitsOnlyFilter(acceptNegativeValues: Boolean) extends IntDigitsOnlyFilter(acceptNegativeValues) {

  override def acceptChar(field: TextField, c: Char): Boolean = {
    // Simplified: always use full text since SGE TextField API differs from VisUI
    val text = field.text

    if (c == '.' && !text.contains(".")) return true
    super.acceptChar(field, c)
  }
}
