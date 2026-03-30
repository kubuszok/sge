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

class RadialDistortionEffect(using Sge)
    extends ShaderVfxEffect(
      VfxGLUtils.compileShader(
        Sge().files.classpath("sge/vfx/shaders/screenspace.vert"),
        Sge().files.classpath("sge/vfx/shaders/radial-distortion.frag")
      )
    )
    with ChainVfxEffect {

  import ShaderVfxEffect.*

  private var _zoom: Float = 1f
  private var _distortion: Float = 0.3f

  rebind()

  override def rebind(): Unit = {
    super.rebind()
    program.bind()
    program.setUniformi("u_texture0", TEXTURE_HANDLE0)
    program.setUniformf("distortion", _distortion)
    program.setUniformf("zoom", _zoom)
    Sge().graphics.gl20.glUseProgram(0)
  }

  override def render(context: VfxRenderContext, buffers: VfxPingPongWrapper): Unit =
    render(context, buffers.srcBuffer, buffers.dstBuffer)

  def render(context: VfxRenderContext, src: VfxFrameBuffer, dst: VfxFrameBuffer): Unit = {
    src.texture.get.bind(TEXTURE_HANDLE0)
    renderShader(context, dst)
  }

  def zoom: Float = _zoom
  def zoom_=(value: Float): Unit = {
    _zoom = value
    setUniform("zoom", value)
  }

  def distortion: Float = _distortion
  def distortion_=(value: Float): Unit = {
    _distortion = value
    setUniform("distortion", value)
  }
}
