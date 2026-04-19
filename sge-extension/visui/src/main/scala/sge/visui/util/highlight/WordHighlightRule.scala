/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 33
 * Covenant-baseline-methods: WordHighlightRule,index,process,text
 * Covenant-source-reference: com/kotcrab/vis/ui/util/highlight/WordHighlightRule.java
 * Covenant-verified: 2026-04-19
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
