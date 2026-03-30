/* Ported from TextraTypist. Licensed under Apache 2.0. */
package sge
package textra
package effects

import sge.textra.utils.StringUtils

class LinkEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val link: String = if (params.length > 0) StringUtils.join(";", params*) else "https://libgdx.com"
  label.trackingInput = true

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit =
    if (label.lastTouchedIndex == globalIndex) {
      label.lastTouchedIndex = -1
      // Open URI would use Sge.net.openURI(link) in the full implementation
      val _ = link // suppress unused warning until URI opening is implemented
    }
}
