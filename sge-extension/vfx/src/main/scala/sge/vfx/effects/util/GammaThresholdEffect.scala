/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package vfx
package effects
package util

import sge.vfx.VfxRenderContext
import sge.vfx.framebuffer.{ VfxFrameBuffer, VfxPingPongWrapper }
import sge.vfx.gl.VfxGLUtils

/** Keeps only values brighter than the specified gamma. */
class GammaThresholdEffect(thresholdType: GammaThresholdEffect.Type)(using Sge)
    extends ShaderVfxEffect(
      VfxGLUtils.compileShader(
        Sge().files.classpath("sge/vfx/shaders/screenspace.vert"),
        Sge().files.classpath("sge/vfx/shaders/gamma-threshold.frag"),
        "#define THRESHOLD_TYPE " + thresholdType.toString
      )
    )
    with ChainVfxEffect {

  import ShaderVfxEffect.*

  private var _gamma: Float = 0f

  rebind()

  override def rebind(): Unit = {
    super.rebind()
    program.bind()
    program.setUniformi("u_texture0", TEXTURE_HANDLE0)
    program.setUniformf("u_threshold", _gamma)
    program.setUniformf("u_thresholdInv", 1f / (1f - _gamma))
    Sge().graphics.gl20.glUseProgram(0)
  }

  override def render(context: VfxRenderContext, buffers: VfxPingPongWrapper): Unit =
    render(context, buffers.srcBuffer, buffers.dstBuffer)

  def render(context: VfxRenderContext, src: VfxFrameBuffer, dst: VfxFrameBuffer): Unit = {
    // Bind src buffer's texture as a primary one.
    src.texture.get.bind(TEXTURE_HANDLE0)
    // Apply shader effect and render result to dst buffer.
    renderShader(context, dst)
  }

  def gamma:                 Float = _gamma
  def gamma_=(value: Float): Unit  = {
    _gamma = value
    setUniform("u_threshold", value)
    setUniform("u_thresholdInv", 1f / (1f - value))
  }
}

object GammaThresholdEffect {
  enum Type extends java.lang.Enum[Type] {
    case RGBA, RGB, ALPHA_PREMULTIPLIED
  }
}
