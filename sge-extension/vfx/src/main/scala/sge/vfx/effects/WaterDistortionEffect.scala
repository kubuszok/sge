/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 74
 * Covenant-baseline-methods: WaterDistortionEffect,_time,amount,amount_,rebind,render,resize,speed,speed_,time,time_,update
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package vfx
package effects

import sge.vfx.VfxRenderContext
import sge.vfx.framebuffer.{ VfxFrameBuffer, VfxPingPongWrapper }
import sge.vfx.gl.VfxGLUtils

class WaterDistortionEffect(private var _amount: Float, private var _speed: Float)(using Sge)
    extends ShaderVfxEffect(
      VfxGLUtils.compileShader(
        Sge().files.classpath("sge/vfx/shaders/screenspace.vert"),
        Sge().files.classpath("sge/vfx/shaders/water-distortion.frag")
      )
    )
    with ChainVfxEffect {

  import ShaderVfxEffect.*

  private var _time: Float = 0f

  rebind()

  override def update(delta: Float): Unit = {
    super.update(delta)
    time = _time + delta
  }

  def time:                 Float = _time
  def time_=(value: Float): Unit  = {
    _time = value
    setUniform("u_time", value)
  }

  def amount:                 Float = _amount
  def amount_=(value: Float): Unit  = {
    _amount = value
    setUniform("u_amount", value)
  }

  def speed:                 Float = _speed
  def speed_=(value: Float): Unit  = {
    _speed = value
    setUniform("u_speed", value)
  }

  override def resize(width: Int, height: Int): Unit = {
    // Do nothing.
  }

  override def rebind(): Unit = {
    super.rebind()
    program.bind()
    program.setUniformi("u_texture0", TEXTURE_HANDLE0)
    program.setUniformf("u_time", _time)
    program.setUniformf("u_amount", _amount)
    program.setUniformf("u_speed", _speed)
    Sge().graphics.gl20.glUseProgram(0)
  }

  override def render(context: VfxRenderContext, buffers: VfxPingPongWrapper): Unit =
    render(context, buffers.srcBuffer, buffers.dstBuffer)

  def render(context: VfxRenderContext, src: VfxFrameBuffer, dst: VfxFrameBuffer): Unit = {
    src.texture.get.bind(TEXTURE_HANDLE0)
    renderShader(context, dst)
  }
}
