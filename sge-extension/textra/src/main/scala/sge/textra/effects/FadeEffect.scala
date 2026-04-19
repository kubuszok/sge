/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Licensed under the Apache License, Version 2.0
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 47
 * Covenant-baseline-methods: FadeEffect,alpha1,alpha2,color1,color2,fadeDuration,g,onApply,progress,timePassed,timePassedByGlyphIndex
 * Covenant-source-reference: com/github/tommyettinger/textra/effects/FadeEffect.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra
package effects

import sge.math.MathUtils
import sge.textra.utils.ColorUtils
import scala.collection.mutable.HashMap

/** Fades the text's color from between colors or alphas. Doesn't repeat itself. */
class FadeEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private var color1                 = 256
  private var color2                 = 256
  private var alpha1                 = 0f
  private var alpha2                 = 1f
  private var fadeDuration           = 1f
  private val timePassedByGlyphIndex = new HashMap[Int, Float]()

  if (params.length > 0) { color1 = paramAsColor(params(0)); if (color1 == 256) alpha1 = paramAsFloat(params(0), 0f) }
  if (params.length > 1) { color2 = paramAsColor(params(1)); if (color2 == 256) alpha2 = paramAsFloat(params(1), 1f) }
  if (params.length > 2) fadeDuration = paramAsFloat(params(2), 1f)

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val timePassed = timePassedByGlyphIndex.getOrElse(localIndex, 0f)
    timePassedByGlyphIndex(localIndex) = timePassed + delta
    val progress = MathUtils.clamp(timePassed / fadeDuration, 0f, 1f)
    var g        = glyph
    if (color1 == 256) {
      g = (g & 0xffffff00ffffffffL) | (MathUtils.lerp((g >>> 32 & 255).toFloat, alpha1 * (label.getInLayout(label.layout, globalIndex) >>> 32 & 255).toFloat, 1f - progress).toLong << 32)
      label.setInWorkingLayout(globalIndex, g)
    } else {
      g = (g & 0xffffffffL) | (ColorUtils.lerpColors((g >>> 32).toInt, color1, 1f - progress).toLong << 32)
      label.setInWorkingLayout(globalIndex, g)
    }
    if (color2 == 256) {
      label.setInWorkingLayout(
        globalIndex,
        (g & 0xffffff00ffffffffL) | (MathUtils.lerp((g >>> 32 & 255).toFloat, alpha2 * (label.getInLayout(label.layout, globalIndex) >>> 32 & 255).toFloat, progress).toLong << 32)
      )
    } else {
      label.setInWorkingLayout(globalIndex, (g & 0xffffffffL) | (ColorUtils.lerpColors((g >>> 32).toInt, color2, progress).toLong << 32))
    }
  }
}
