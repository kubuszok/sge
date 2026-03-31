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
package highlight

import sge.graphics.Color
import sge.utils.DynamicArray
import sge.visui.widget.HighlightTextArea

/** Highlighter rule using [[String#indexOf]] to detect text matches.
  * @author
  *   Kotcrab
  * @since 1.1.2
  */
class WordHighlightRule(color: Color, word: String) extends HighlightRule {

  override def process(textArea: HighlightTextArea, highlights: DynamicArray[Highlight]): Unit = {
    val text  = textArea.text
    var index = text.indexOf(word)
    while (index >= 0) {
      val end = index + word.length
      highlights.add(Highlight(color, index, end))
      index = text.indexOf(word, end)
    }
  }
}
