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

class OldTvEffect(using Sge)
    extends ShaderVfxEffect(
      VfxGLUtils.compileShader(
        Sge().files.classpath("sge/vfx/shaders/screenspace.vert"),
        Sge().files.classpath("sge/vfx/shaders/old-tv.frag")
      )
    )
    with ChainVfxEffect {

  import ShaderVfxEffect.*

  private val resolution: Vector2 = Vector2()
  private var _time:      Float   = 0f

  rebind()

  override def resize(width: Int, height: Int): Unit = {
    super.resize(width, height)
    resolution.set(width.toFloat, height.toFloat)
    rebind()
  }

  override def rebind(): Unit = {
    super.rebind()
    program.bind()
    program.setUniformi("u_texture0", TEXTURE_HANDLE0)
    program.setUniformf("u_resolution", resolution)
    program.setUniformf("u_time", _time)
    Sge().graphics.gl20.glUseProgram(0)
  }

  override def update(delta: Float): Unit = {
    super.update(delta)
    time = _time + delta
  }

  def time:                 Float = _time
  def time_=(value: Float): Unit  = {
    _time = value
    setUniform("u_time", value)
  }

  override def render(context: VfxRenderContext, buffers: VfxPingPongWrapper): Unit =
    render(context, buffers.srcBuffer, buffers.dstBuffer)

  def render(context: VfxRenderContext, src: VfxFrameBuffer, dst: VfxFrameBuffer): Unit = {
    src.texture.get.bind(TEXTURE_HANDLE0)
    renderShader(context, dst)
  }
}
