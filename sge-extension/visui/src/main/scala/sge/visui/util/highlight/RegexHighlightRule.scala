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

import java.util.regex.Pattern

import sge.graphics.Color
import sge.utils.DynamicArray
import sge.visui.widget.HighlightTextArea

/** Highlighter rule using regex to detect text matches. Regexes and thus this rule can't be used on GWT.
  * @author
  *   Kotcrab
  * @since 1.1.2
  */
class RegexHighlightRule(color: Color, regex: String) extends HighlightRule {
  private val pattern: Pattern = Pattern.compile(regex)

  override def process(textArea: HighlightTextArea, highlights: DynamicArray[Highlight]): Unit = {
    val matcher = pattern.matcher(textArea.text)
    while (matcher.find()) {
      highlights.add(Highlight(color, matcher.start(), matcher.end()))
    }
  }
}
