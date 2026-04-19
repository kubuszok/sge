/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.kotcrab.vis.ui.util` -> `sge.visui.util`
 *   Idiom: `return` -> direct expression
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 39
 * Covenant-baseline-methods: FloatDigitsOnlyFilter,acceptChar,cursorPos,selectionStart,text
 * Covenant-source-reference: com/kotcrab/vis/ui/util/FloatDigitsOnlyFilter.java
 * Covenant-verified: 2026-04-19
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
    val selectionStart = field.getSelectionStart
    val cursorPos      = field.cursorPosition
    val text: String =
      if (field.selection.nonEmpty) { // issue #131
        val beforeSelection = field.text.substring(0, Math.min(selectionStart, cursorPos))
        val afterSelection  = field.text.substring(Math.max(selectionStart, cursorPos))
        beforeSelection + afterSelection
      } else {
        field.text
      }

    if (c == '.' && !text.contains(".")) true
    else super.acceptChar(field, c)
  }
}
