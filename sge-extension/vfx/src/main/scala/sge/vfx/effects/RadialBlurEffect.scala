/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package vfx
package effects

import sge.utils.Align
import sge.vfx.VfxRenderContext
import sge.vfx.framebuffer.{ VfxFrameBuffer, VfxPingPongWrapper }
import sge.vfx.gl.VfxGLUtils

class RadialBlurEffect(passes: Int)(using Sge)
    extends ShaderVfxEffect(
      VfxGLUtils.compileShader(
        Sge().files.classpath("sge/vfx/shaders/radial-blur.vert"),
        Sge().files.classpath("sge/vfx/shaders/radial-blur.frag"),
        "#define PASSES " + passes
      )
    )
    with ChainVfxEffect {

  import ShaderVfxEffect.*

  private var _strength: Float = 0.2f
  private var _originX:  Float = 0.5f
  private var _originY:  Float = 0.5f
  private var _zoom:     Float = 1f

  rebind()

  override def rebind(): Unit = {
    super.rebind()
    program.bind()
    program.setUniformi("u_texture0", TEXTURE_HANDLE0)
    program.setUniformf("u_blurDiv", _strength / passes.toFloat)
    program.setUniformf("u_offsetX", _originX)
    program.setUniformf("u_offsetY", _originY)
    program.setUniformf("u_zoom", _zoom)
    Sge().graphics.gl20.glUseProgram(0)
  }

  override def render(context: VfxRenderContext, buffers: VfxPingPongWrapper): Unit =
    render(context, buffers.srcBuffer, buffers.dstBuffer)

  def render(context: VfxRenderContext, src: VfxFrameBuffer, dst: VfxFrameBuffer): Unit = {
    src.texture.get.bind(TEXTURE_HANDLE0)
    renderShader(context, dst)
  }

  def originX: Float = _originX
  def originY: Float = _originY

  /** Specify the zoom origin in [[Align]] bits. */
  def setOrigin(align: Align): Unit = {
    val ox = if (align.isLeft) 0f else if (align.isRight) 1f else 0.5f
    val oy = if (align.isBottom) 0f else if (align.isTop) 1f else 0.5f
    setOrigin(ox, oy)
  }

  /** Specify the zoom origin in normalized screen coordinates.
    * @param ox
    *   horizontal origin [0..1].
    * @param oy
    *   vertical origin [0..1].
    */
  def setOrigin(ox: Float, oy: Float): Unit = {
    _originX = ox
    _originY = oy
    program.bind()
    program.setUniformf("u_offsetX", _originX)
    program.setUniformf("u_offsetY", _originY)
    Sge().graphics.gl20.glUseProgram(0)
  }

  def strength:                 Float = _strength
  def strength_=(value: Float): Unit  = {
    _strength = value
    setUniform("u_blurDiv", value / passes.toFloat)
  }

  def zoom:                 Float = _zoom
  def zoom_=(value: Float): Unit  = {
    _zoom = value
    setUniform("u_zoom", value)
  }
}
