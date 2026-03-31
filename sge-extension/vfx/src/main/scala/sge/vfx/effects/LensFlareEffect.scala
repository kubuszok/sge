/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package vfx
package effects

import sge.graphics.Color
import sge.math.{ Vector2, Vector3 }
import sge.vfx.VfxRenderContext
import sge.vfx.framebuffer.{ VfxFrameBuffer, VfxPingPongWrapper }
import sge.vfx.gl.VfxGLUtils

/** Lens flare effect.
  * @author
  *   Toni Sagrista
  */
class LensFlareEffect(using Sge)
    extends ShaderVfxEffect(
      VfxGLUtils.compileShader(
        Sge().files.classpath("sge/vfx/shaders/screenspace.vert"),
        Sge().files.classpath("sge/vfx/shaders/lens-flare.frag")
      )
    )
    with ChainVfxEffect {

  import ShaderVfxEffect.*

  private val _lightPosition: Vector2 = Vector2(0.5f, 0.5f)
  private val _viewport:      Vector2 = Vector2()
  private val _color:         Vector3 = Vector3(1f, 0.8f, 0.2f)
  private var _intensity:     Float   = 5.0f

  rebind()

  override def resize(width: Int, height: Int): Unit = {
    super.resize(width, height)
    _viewport.set(width.toFloat, height.toFloat)
    setUniform("u_viewport", _viewport)
  }

  override def rebind(): Unit = {
    super.rebind()
    program.bind()
    program.setUniformi("u_texture0", TEXTURE_HANDLE0)
    program.setUniformf("u_lightPosition", _lightPosition)
    program.setUniformf("u_intensity", _intensity)
    program.setUniformf("u_color", _color)
    program.setUniformf("u_viewport", _viewport)
    Sge().graphics.gl20.glUseProgram(0)
  }

  override def render(context: VfxRenderContext, buffers: VfxPingPongWrapper): Unit =
    render(context, buffers.srcBuffer, buffers.dstBuffer)

  def render(context: VfxRenderContext, src: VfxFrameBuffer, dst: VfxFrameBuffer): Unit = {
    src.texture.get.bind(TEXTURE_HANDLE0)
    renderShader(context, dst)
  }

  def lightPosition: Vector2 = _lightPosition

  /** Sets the light position in screen normalized coordinates [0..1]. */
  def setLightPosition(x: Float, y: Float): Unit = {
    _lightPosition.set(x, y)
    setUniform("u_lightPosition", _lightPosition)
  }

  def intensity:                 Float = _intensity
  def intensity_=(value: Float): Unit  = {
    _intensity = value
    setUniform("u_intensity", value)
  }

  def color: Vector3 = _color

  def setColor(r: Float, g: Float, b: Float): Unit = {
    _color.set(r, g, b)
    setUniform("u_color", _color)
  }

  def setColor(c: Color): Unit =
    setColor(c.r, c.g, c.b)
}
