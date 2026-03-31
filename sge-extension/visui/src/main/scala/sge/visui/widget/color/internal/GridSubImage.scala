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

import sge.graphics.Texture
import sge.graphics.g2d.Batch
import sge.graphics.glutils.ShaderProgram
import sge.scenes.scene2d.ui.Image
import sge.utils.Nullable

/** @author Kotcrab */
class GridSubImage(gridShader: ShaderProgram, whiteTexture: Texture, gridSize: Float) {

  def draw(batch: Batch, parent: Image): Unit = {
    val originalShader = batch.shader
    batch.shader = Nullable(gridShader)
    gridShader.setUniformf("u_width", parent.width)
    gridShader.setUniformf("u_height", parent.height)
    gridShader.setUniformf("u_gridSize", gridSize)
    batch.draw(whiteTexture, parent.x + parent.imageX, parent.y + parent.imageY,
      parent.imageWidth * parent.scaleX, parent.imageHeight * parent.scaleY)
    batch.shader = Nullable(originalShader)
  }
}
