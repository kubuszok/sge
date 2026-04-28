/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 29
 * Covenant-baseline-methods: HighlightRule,process
 * Covenant-source-reference: com/kotcrab/vis/ui/util/highlight/HighlightRule.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package util
package highlight

import sge.utils.DynamicArray
import sge.visui.widget.HighlightTextArea

/** @author
  *   Kotcrab
  * @since 1.1.2
  */
trait HighlightRule {

  /** Process this rule. This method should detect matches in text area text, create [[Highlight]] instances and add them to provided highlights array.
    * @param textArea
    *   text area
    * @param highlights
    *   current highlights, new highlights can be added to this list however it should not be modified in any other ways
    */
  def process(textArea: HighlightTextArea, highlights: DynamicArray[Highlight]): Unit
}
