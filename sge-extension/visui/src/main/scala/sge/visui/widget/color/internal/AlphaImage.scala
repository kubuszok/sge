/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 32
 * Covenant-baseline-methods: AlphaImage,draw,gridImage
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/color/internal/AlphaImage.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget
package color
package internal

import sge.graphics.g2d.Batch
import sge.visui.widget.VisImage

/** Image that displays checkerboard as background, used by ColorPicker to display selected colors with alphas. Note that for perfect grid this image should have size which is multiplication of
  * gridSize. Eg. if gridSize is equal to 5, this image can have size 65x100. (because both 65 and 100 are divisible by 5)
  * @author
  *   Kotcrab
  */
class AlphaImage(commons: PickerCommons, gridSize: Float)(using Sge) extends VisImage(commons.whiteTexture) {
  private val gridImage: GridSubImage = new GridSubImage(commons.gridShader, commons.whiteTexture, gridSize)

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    // don't draw grid if widget alpha is different than 1 because
    // this creates weird affect when window is fading in/out,
    // both parent image and grid is visible
    if (color.a != 1) gridImage.draw(batch, this)
    super.draw(batch, parentAlpha)
  }
}
