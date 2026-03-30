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

class VignettingEffect(val saturationControlEnabled: Boolean)(using Sge)
    extends ShaderVfxEffect(
      VfxGLUtils.compileShader(
        Sge().files.classpath("sge/vfx/shaders/screenspace.vert"),
        Sge().files.classpath("sge/vfx/shaders/vignetting.frag"),
        if (saturationControlEnabled) "#define CONTROL_SATURATION" else ""
      )
    )
    with ChainVfxEffect {

  import ShaderVfxEffect.*

  private var _vignetteX: Float = 0.8f
  private var _vignetteY: Float = 0.25f
  private var _centerX: Float = 0.5f
  private var _centerY: Float = 0.5f
  private var _intensity: Float = 1f
  private var _saturation: Float = 0f
  private var _saturationMul: Float = 0f

  rebind()

  override def rebind(): Unit = {
    program.bind()
    program.setUniformi("u_texture0", TEXTURE_HANDLE0)
    if (saturationControlEnabled) {
      program.setUniformf("u_saturation", _saturation)
      program.setUniformf("u_saturationMul", _saturationMul)
    }
    program.setUniformf("u_vignetteIntensity", _intensity)
    program.setUniformf("u_vignetteX", _vignetteX)
    program.setUniformf("u_vignetteY", _vignetteY)
    program.setUniformf("u_centerX", _centerX)
    program.setUniformf("u_centerY", _centerY)
    Sge().graphics.gl20.glUseProgram(0)
  }

  override def render(context: VfxRenderContext, buffers: VfxPingPongWrapper): Unit =
    render(context, buffers.srcBuffer, buffers.dstBuffer)

  def render(context: VfxRenderContext, src: VfxFrameBuffer, dst: VfxFrameBuffer): Unit = {
    src.texture.get.bind(TEXTURE_HANDLE0)
    renderShader(context, dst)
  }

  def intensity: Float = _intensity
  def intensity_=(value: Float): Unit = {
    _intensity = value
    setUniform("u_vignetteIntensity", value)
  }

  def saturation: Float = _saturation
  def saturation_=(value: Float): Unit = {
    _saturation = value
    if (saturationControlEnabled) { setUniform("u_saturation", value) }
  }

  def saturationMul: Float = _saturationMul
  def saturationMul_=(value: Float): Unit = {
    _saturationMul = value
    if (saturationControlEnabled) { setUniform("u_saturationMul", value) }
  }

  def setCoords(x: Float, y: Float): Unit = {
    _vignetteX = x
    _vignetteY = y
    program.bind()
    program.setUniformf("u_vignetteX", x)
    program.setUniformf("u_vignetteY", y)
    Sge().graphics.gl20.glUseProgram(0)
  }

  def vignetteX: Float = _vignetteX
  def vignetteX_=(x: Float): Unit = { _vignetteX = x; setUniform("u_vignetteX", x) }

  def vignetteY: Float = _vignetteY
  def vignetteY_=(y: Float): Unit = { _vignetteY = y; setUniform("u_vignetteY", y) }

  /** Specify the center, in normalized screen coordinates. */
  def setCenter(x: Float, y: Float): Unit = {
    _centerX = x
    _centerY = y
    program.bind()
    program.setUniformf("u_centerX", _centerX)
    program.setUniformf("u_centerY", _centerY)
    Sge().graphics.gl20.glUseProgram(0)
  }

  def centerX: Float = _centerX
  def centerY: Float = _centerY
}
