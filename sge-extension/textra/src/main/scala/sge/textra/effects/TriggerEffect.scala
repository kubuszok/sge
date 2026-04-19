/* Ported from TextraTypist. Licensed under Apache 2.0.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 18
 * Covenant-baseline-methods: TriggerEffect,event,onApply
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/TriggerEffect.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra
package effects

import sge.textra.utils.StringUtils

class TriggerEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private var event = "start"
  label.trackingInput = true
  if (params.length > 0) event = StringUtils.join(";", params*)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit =
    if (label.lastTouchedIndex == globalIndex) {
      label.lastTouchedIndex = -1
      label.triggerEvent(event, true)
    }
}
