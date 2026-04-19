/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 79
 * Covenant-baseline-methods: LevelsEffect,_brightness,_contrast,_gamma,_hue,_saturation,brightness,brightness_,contrast,contrast_,gamma,gamma_,hue,hue_,rebind,render,saturation,saturation_
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package vfx
package effects

import sge.vfx.VfxRenderContext
import sge.vfx.framebuffer.{ VfxFrameBuffer, VfxPingPongWrapper }
import sge.vfx.gl.VfxGLUtils

/** Controls levels of brightness and contrast. */
class LevelsEffect(using Sge)
    extends ShaderVfxEffect(
      VfxGLUtils.compileShader(
        Sge().files.classpath("sge/vfx/shaders/screenspace.vert"),
        Sge().files.classpath("sge/vfx/shaders/levels.frag")
      )
    )
    with ChainVfxEffect {

  import ShaderVfxEffect.*

  private var _brightness: Float = 0.0f
  private var _contrast:   Float = 1.0f
  private var _saturation: Float = 1.0f
  private var _hue:        Float = 1.0f
  private var _gamma:      Float = 1.0f

  rebind()

  override def rebind(): Unit = {
    super.rebind()
    program.bind()
    program.setUniformi("u_texture0", TEXTURE_HANDLE0)
    program.setUniformf("u_brightness", _brightness)
    program.setUniformf("u_contrast", _contrast)
    program.setUniformf("u_saturation", _saturation)
    program.setUniformf("u_hue", _hue)
    program.setUniformf("u_gamma", _gamma)
    Sge().graphics.gl20.glUseProgram(0)
  }

  override def render(context: VfxRenderContext, buffers: VfxPingPongWrapper): Unit =
    render(context, buffers.srcBuffer, buffers.dstBuffer)

  def render(context: VfxRenderContext, src: VfxFrameBuffer, dst: VfxFrameBuffer): Unit = {
    src.texture.get.bind(TEXTURE_HANDLE0)
    renderShader(context, dst)
  }

  def contrast: Float = _contrast

  /** Sets the contrast level @param value in [0..2] */
  def contrast_=(value: Float): Unit = { _contrast = value; setUniform("u_contrast", value) }

  def brightness: Float = _brightness

  /** Sets the brightness level @param value in [-1..1] */
  def brightness_=(value: Float): Unit = { _brightness = value; setUniform("u_brightness", value) }

  def saturation: Float = _saturation

  /** Sets the saturation @param value in [0..2] */
  def saturation_=(value: Float): Unit = { _saturation = value; setUniform("u_saturation", value) }

  def hue: Float = _hue

  /** Sets the hue @param value in [0..2] */
  def hue_=(value: Float): Unit = { _hue = value; setUniform("u_hue", value) }

  def gamma: Float = _gamma

  /** Sets the gamma correction value @param value in [0..3] */
  def gamma_=(value: Float): Unit = { _gamma = value; setUniform("u_gamma", value) }
}
