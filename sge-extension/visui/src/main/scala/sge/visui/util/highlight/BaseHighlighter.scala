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

/** Highlighter aggregates multiple [[HighlightRule]] into single collection. Highlighter is used by [[HighlightTextArea]] to get information about which parts of text should be highlighted. Compared
  * to [[Highlighter]] this class is GWT compatible.
  * @author
  *   Kotcrab
  * @see
  *   [[Highlighter]]
  * @since 1.1.2
  */
class BaseHighlighter {
  private val rules: DynamicArray[HighlightRule] = DynamicArray[HighlightRule]()

  /** Adds highlighter rule. What is highlighted depends on rule implementation. */
  def addRule(rule: HighlightRule): Unit = rules.add(rule)

  /** Adds word based highlighter rule. Note that for most uses, word based rules are not sophisticated enough - for example using regex rule for programming language keywords detection is far more
    * robust.
    * @see
    *   [[WordHighlightRule]]
    */
  def word(color: Color, word: String): Unit = addRule(new WordHighlightRule(color, word))

  /** Adds word based highlighter rule. Utility method allowing to add many words at once.
    * @see
    *   [[word(Color,String)*]]
    * @see
    *   [[WordHighlightRule]]
    */
  def word(color: Color, words: String*): Unit =
    words.foreach(w => addRule(new WordHighlightRule(color, w)))

  /** Process all rules in this highlighter.
    * @param highlights
    *   current highlights, new highlights can be added to this list however it should not be modified in any other ways
    */
  def process(textArea: HighlightTextArea, highlights: DynamicArray[Highlight]): Unit = {
    var i = 0
    while (i < rules.size) {
      rules(i).process(textArea, highlights)
      i += 1
    }
  }
}
