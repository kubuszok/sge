/* Ported from TextraTypist. Licensed under Apache 2.0. */
package sge
package textra
package effects

class RotateEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private var rotation = 90f
  if (params.length > 0) rotation = paramAsFloat(params(0), 90.0f)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit =
    label.getRotations.incr(globalIndex, rotation)
}
