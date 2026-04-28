/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 35
 * Covenant-baseline-methods: ShaderImage,draw,originalShader,setShaderUniforms
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/color/internal/ShaderImage.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget
package color
package internal

import sge.graphics.Texture
import sge.graphics.g2d.Batch
import sge.graphics.glutils.ShaderProgram
import sge.utils.Nullable
import sge.visui.widget.VisImage

/** Allow to render standard [[VisImage]] with shader. Shader uniforms can be set in [[setShaderUniforms]].
  * @author
  *   Kotcrab
  */
class ShaderImage(private val shader: ShaderProgram, texture: Texture)(using Sge) extends VisImage(texture) {

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    val originalShader = batch.shader
    batch.shader = Nullable(shader)
    setShaderUniforms(shader)
    super.draw(batch, parentAlpha)
    batch.shader = Nullable(originalShader)
  }

  protected def setShaderUniforms(shader: ShaderProgram): Unit = ()
}
