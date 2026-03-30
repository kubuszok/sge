/* Ported from TextraTypist. Licensed under Apache 2.0. */
package sge
package textra
package effects

class InstantEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit =
    if (indexEnd >= 0) label.setTextSpeed(TypingConfig.DEFAULT_SPEED_PER_CHAR) else label.setTextSpeed(0f)
}
