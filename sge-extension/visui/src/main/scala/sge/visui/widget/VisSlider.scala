/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 27
 * Covenant-baseline-methods: VisSlider,this
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/VisSlider.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget

import sge.scenes.scene2d.ui.Slider

/** Compatible with [[Slider]]. Does not provide additional features.
  * @author
  *   Kotcrab
  * @see
  *   [[Slider]]
  */
class VisSlider(min: Float, max: Float, stepSize: Float, vertical: Boolean, sliderStyle: Slider.SliderStyle)(using Sge) extends Slider(min, max, stepSize, vertical, sliderStyle) {

  def this(min: Float, max: Float, stepSize: Float, vertical: Boolean)(using Sge) =
    this(min, max, stepSize, vertical, VisUI.getSkin.get[Slider.SliderStyle]("default-" + (if (vertical) "vertical" else "horizontal")))

  def this(min: Float, max: Float, stepSize: Float, vertical: Boolean, styleName: String)(using Sge) =
    this(min, max, stepSize, vertical, VisUI.getSkin.get[Slider.SliderStyle](styleName))
}
