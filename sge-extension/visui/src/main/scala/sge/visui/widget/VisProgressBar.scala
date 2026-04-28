/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 34
 * Covenant-baseline-methods: VisProgressBar,this
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/VisProgressBar.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget

import sge.scenes.scene2d.ui.ProgressBar

/** Compatible with [[ProgressBar]]. Does not provide additional features.
  * @author
  *   Kotcrab
  * @see
  *   [[ProgressBar]]
  */
class VisProgressBar(min: Float, max: Float, stepSize: Float, vertical: Boolean, progressBarStyle: ProgressBar.ProgressBarStyle)(using Sge)
    extends ProgressBar(min, max, stepSize, vertical, progressBarStyle) {

  def this(min: Float, max: Float, stepSize: Float, vertical: Boolean)(using Sge) =
    this(
      min,
      max,
      stepSize,
      vertical,
      VisUI.getSkin.get[ProgressBar.ProgressBarStyle]("default-" + (if (vertical) "vertical" else "horizontal"))
    )

  def this(min: Float, max: Float, stepSize: Float, vertical: Boolean, styleName: String)(using Sge) =
    this(min, max, stepSize, vertical, VisUI.getSkin.get[ProgressBar.ProgressBarStyle](styleName))
}
