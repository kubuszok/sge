/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package vfx
package effects

import sge.vfx.VfxRenderContext
import sge.vfx.framebuffer.{ VfxFrameBuffer, VfxPingPongWrapper }
import sge.vfx.gl.VfxGLUtils

class FilmGrainEffect(using Sge)
    extends ShaderVfxEffect(
      VfxGLUtils.compileShader(
        Sge().files.classpath("sge/vfx/shaders/screenspace.vert"),
        Sge().files.classpath("sge/vfx/shaders/film-grain.frag")
      )
    )
    with ChainVfxEffect {

  import ShaderVfxEffect.*

  private var _seed:        Float = 0f
  private var _noiseAmount: Float = 0.18f

  rebind()

  override def update(delta: Float): Unit = {
    super.update(delta)
    seed = (this._seed + delta) % 1f
  }

  override def rebind(): Unit = {
    super.rebind()
    program.bind()
    program.setUniformi("u_texture0", TEXTURE_HANDLE0)
    program.setUniformf("u_seed", _seed)
    program.setUniformf("u_noiseAmount", _noiseAmount)
    Sge().graphics.gl20.glUseProgram(0)
  }

  override def render(context: VfxRenderContext, buffers: VfxPingPongWrapper): Unit =
    render(context, buffers.srcBuffer, buffers.dstBuffer)

  def render(context: VfxRenderContext, src: VfxFrameBuffer, dst: VfxFrameBuffer): Unit = {
    src.texture.get.bind(TEXTURE_HANDLE0)
    renderShader(context, dst)
  }

  def seed:                 Float = _seed
  def seed_=(value: Float): Unit  = {
    _seed = value
    setUniform("u_seed", value)
  }

  def noiseAmount:                 Float = _noiseAmount
  def noiseAmount_=(value: Float): Unit  = {
    _noiseAmount = value
    setUniform("u_noiseAmount", value)
  }
}
