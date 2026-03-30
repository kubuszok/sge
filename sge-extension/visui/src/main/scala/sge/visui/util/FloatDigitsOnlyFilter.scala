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
