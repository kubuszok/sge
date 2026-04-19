/* Ported from TextraTypist. Licensed under Apache 2.0.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 45
 * Covenant-baseline-methods: StylistEffect,all,effects,onApply,shouldApply
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/StylistEffect.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra
package effects

import sge.textra.utils.CaseInsensitiveIntMap

class StylistEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private var effects = 0L
  private var all     = false
  label.trackingInput = true

  if (params.length > 0) {
    if (paramAsBoolean(params(0))) effects |= Font.BOLD
    else if (params.length == 1) {
      val split    = params(0).split("[\\s\t,]+")
      val matching = new Array[Int](split.length)
      val set      = new CaseInsensitiveIntMap(split, matching)
      if (set.containsKey("bold") || set.containsKey("b") || set.containsKey("*")) effects |= Font.BOLD
      if (set.containsKey("italic") || set.containsKey("i") || set.containsKey("/") || set.containsKey("oblique") || set.containsKey("o")) effects |= Font.OBLIQUE
      if (set.containsKey("underline") || set.containsKey("u") || set.containsKey("_")) effects |= Font.UNDERLINE
      if (set.containsKey("strikethrough") || set.containsKey("s") || set.containsKey("~") || set.containsKey("strike")) effects |= Font.STRIKETHROUGH
      if (set.containsKey("superscript") || set.containsKey("^") || set.containsKey("super")) effects |= Font.SUPERSCRIPT
      else if (set.containsKey("midscript") || set.containsKey("=") || set.containsKey("mid")) effects |= Font.MIDSCRIPT
      else if (set.containsKey("subscript") || set.containsKey(".") || set.containsKey("sub")) effects |= Font.SUBSCRIPT
      if (set.containsKey("all") || set.containsKey("a")) all = true
    }
  } else {
    effects |= Font.BOLD | Font.UNDERLINE; all = true
  }

  if (params.length > 1 && paramAsBoolean(params(1))) effects |= Font.OBLIQUE
  if (params.length > 2 && paramAsBoolean(params(2))) effects |= Font.UNDERLINE
  if (params.length > 3 && paramAsBoolean(params(3))) effects |= Font.STRIKETHROUGH
  if (params.length > 4) effects |= (paramAsFloat(params(4), 0f).toLong & 3L) << 25
  if (params.length > 5) all = paramAsBoolean(params(5))

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val shouldApply =
      if (all) label.overIndex >= indexStart && label.overIndex <= indexEnd
      else label.overIndex == globalIndex
    if (shouldApply) label.setInWorkingLayout(globalIndex, (glyph & ~effects) | effects)
    else label.setInWorkingLayout(globalIndex, glyph & ~effects)
  }
}
