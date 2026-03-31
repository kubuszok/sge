/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package util

import sge.scenes.scene2d.ui.TextField

/** [[TextField.TextFieldFilter]] that only allows digits for integer values.
  * @author
  *   Kotcrab
  */
class IntDigitsOnlyFilter(acceptNegativeValues: Boolean) extends NumberDigitsTextFieldFilter(acceptNegativeValues) {

  override def acceptChar(field: TextField, c: Char): Boolean =
    if (this.acceptNegativeValues && c == '-') {
      if (useFieldCursorPosition) {
        !(field.cursorPosition > 0 || field.text.startsWith("-"))
      } else {
        !field.text.startsWith("-")
      }
    } else {
      Character.isDigit(c)
    }
}
