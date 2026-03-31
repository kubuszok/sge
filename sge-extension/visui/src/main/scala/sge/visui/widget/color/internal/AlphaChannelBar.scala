/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package widget
package color
package internal

import sge.graphics.g2d.Batch
import sge.scenes.scene2d.utils.ChangeListener

/** Channel bar intended for alpha channel, renders alpha grid below channel texture.
  * @author
  *   Kotcrab
  */
class AlphaChannelBar(commons: PickerCommons, mode: Int, maxValue: Int, changeListener: ChangeListener)(using Sge)
    extends ChannelBar(commons, mode, maxValue, changeListener) {

  private val gridImage: GridSubImage = new GridSubImage(commons.gridShader, commons.whiteTexture, 6 * commons.sizes.scaleFactor)

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    gridImage.draw(batch, this)
    super.draw(batch, parentAlpha)
  }
}
