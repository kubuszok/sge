/* Ported from TextraTypist. Licensed under Apache 2.0. */
package sge
package textra
package effects

import sge.textra.utils.ColorUtils

class HighlightEffect(label: TypingLabel, params: Array[String]) extends Effect(label) {
  private val DEFAULT_DISTANCE = 0.975f; private val DEFAULT_FREQUENCY = 2f; private val DEFAULT_COLOR = -2
  private var baseColor        = DEFAULT_COLOR; private var distance   = 1f; private var frequency     = 1f
  private var saturation       = 1f; private var lightness             = 0.5f; private var all         = false
  label.trackingInput = true

  if (params.length > 0) { baseColor = paramAsColor(params(0)); if (baseColor == 256) baseColor = DEFAULT_COLOR }
  if (params.length > 1) distance = paramAsFloat(params(1), 1f)
  if (params.length > 2) frequency = paramAsFloat(params(2), 1f)
  if (params.length > 3) saturation = paramAsFloat(params(3), 1f)
  if (params.length > 4) lightness = paramAsFloat(params(4), 0.5f)
  if (params.length > 5) all = paramAsBoolean(params(5))

  override protected def onApply(glyph: Long, localIndex: Int, globalIndex: Int, delta: Float): Unit = {
    val shouldApply =
      if (all) label.overIndex >= indexStart && label.overIndex <= indexEnd
      else label.overIndex == globalIndex
    if (!shouldApply) {
      label.setInWorkingLayout(globalIndex, (glyph & 0xffffffffL) | (baseColor.toLong << 32))
    } else {
      val distanceMod  = (1f / distance) * (1f - DEFAULT_DISTANCE)
      val frequencyMod = (1f / frequency) * DEFAULT_FREQUENCY
      val progress     = calculateProgress(frequencyMod, distanceMod * localIndex, pingpong = false)
      label.setInWorkingLayout(globalIndex, (glyph & 0xffffffffL) | (ColorUtils.hsl2rgb(progress, saturation, lightness, 1f).toLong << 32))
    }
  }
}
