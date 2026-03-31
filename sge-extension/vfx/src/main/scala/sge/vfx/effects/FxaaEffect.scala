/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package vfx
package effects

import sge.math.Vector2
import sge.vfx.VfxRenderContext
import sge.vfx.framebuffer.{ VfxFrameBuffer, VfxPingPongWrapper }
import sge.vfx.gl.VfxGLUtils

/** Implements the fast approximate anti-aliasing. Very fast and useful for combining with other post-processing effects.
  * @author
  *   Toni Sagrista
  * @author
  *   metaphore
  */
class FxaaEffect(
  private var fxaaReduceMin: Float,
  private var fxaaReduceMul: Float,
  private var fxaaSpanMax:   Float,
  supportAlpha:              Boolean
)(using Sge)
    extends ShaderVfxEffect(
      VfxGLUtils.compileShader(
        Sge().files.classpath("sge/vfx/shaders/screenspace.vert"),
        Sge().files.classpath("sge/vfx/shaders/fxaa.frag"),
        if (supportAlpha) "#define SUPPORT_ALPHA" else ""
      )
    )
    with ChainVfxEffect {

  import ShaderVfxEffect.*

  private val viewportInverse: Vector2 = Vector2()

  def this()(using Sge) = this(1f / 128f, 1f / 8f, 8f, true)

  rebind()

  override def resize(width: Int, height: Int): Unit = {
    super.resize(width, height)
    viewportInverse.set(1f / width, 1f / height)
    setUniform("u_viewportInverse", viewportInverse)
  }

  override def rebind(): Unit = {
    super.rebind()
    program.bind()
    program.setUniformi("u_texture0", TEXTURE_HANDLE0)
    program.setUniformf("u_viewportInverse", viewportInverse)
    program.setUniformf("u_fxaaReduceMin", fxaaReduceMin)
    program.setUniformf("u_fxaaReduceMul", fxaaReduceMul)
    program.setUniformf("u_fxaaSpanMax", fxaaSpanMax)
    Sge().graphics.gl20.glUseProgram(0)
  }

  override def render(context: VfxRenderContext, buffers: VfxPingPongWrapper): Unit =
    render(context, buffers.srcBuffer, buffers.dstBuffer)

  def render(context: VfxRenderContext, src: VfxFrameBuffer, dst: VfxFrameBuffer): Unit = {
    src.texture.get.bind(TEXTURE_HANDLE0)
    renderShader(context, dst)
  }

  /** Sets the parameter. The default value is 1/128. */
  def reduceMin_=(value: Float): Unit = {
    fxaaReduceMin = value
    setUniform("u_fxaaReduceMin", fxaaReduceMin)
  }

  /** Sets the parameter. The default value is 1/8. */
  def reduceMul_=(value: Float): Unit = {
    fxaaReduceMul = value
    setUniform("u_fxaaReduceMul", fxaaReduceMul)
  }

  /** Sets the parameter. The default value is 8. */
  def spanMax_=(value: Float): Unit = {
    fxaaSpanMax = value
    setUniform("u_fxaaSpanMax", fxaaSpanMax)
  }
}
