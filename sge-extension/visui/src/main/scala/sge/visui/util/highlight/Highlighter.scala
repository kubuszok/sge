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
 * Covenant-baseline-methods: Highlighter,regex
 * Covenant-source-reference: com/kotcrab/vis/ui/util/highlight/Highlighter.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package util
package highlight

import sge.graphics.Color

/** Highlighter aggregates multiple [[HighlightRule]] into single collection. Highlighter is used by [[sge.visui.widget.HighlightTextArea]] to get information about which parts of text should be
  * highlighted. If you need GWT compatibility, you need to use [[BaseHighlighter]].
  * @author
  *   Kotcrab
  * @see
  *   [[BaseHighlighter]]
  * @since 1.1.2
  */
class Highlighter extends BaseHighlighter {

  /** Adds regex based highlighter rule. */
  def regex(color: Color, regex: String): Unit =
    addRule(new RegexHighlightRule(color, regex))
}
