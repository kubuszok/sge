/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 52
 * Covenant-baseline-methods: ChromaticAberrationEffect,_maxDistortion,maxDistortion,maxDistortion_,rebind,render
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package vfx
package effects

import sge.vfx.VfxRenderContext
import sge.vfx.framebuffer.{ VfxFrameBuffer, VfxPingPongWrapper }
import sge.vfx.gl.VfxGLUtils

class ChromaticAberrationEffect(passes: Int)(using Sge)
    extends ShaderVfxEffect(
      VfxGLUtils.compileShader(
        Sge().files.classpath("sge/vfx/shaders/screenspace.vert"),
        Sge().files.classpath("sge/vfx/shaders/chromatic-aberration.frag"),
        "#define PASSES " + passes
      )
    )
    with ChainVfxEffect {

  import ShaderVfxEffect.*

  private var _maxDistortion: Float = 1.2f

  rebind()

  override def rebind(): Unit = {
    super.rebind()
    program.bind()
    program.setUniformi("u_texture0", TEXTURE_HANDLE0)
    program.setUniformf("u_maxDistortion", _maxDistortion)
    Sge().graphics.gl20.glUseProgram(0)
  }

  def maxDistortion:                 Float = _maxDistortion
  def maxDistortion_=(value: Float): Unit  = {
    _maxDistortion = value
    setUniform("u_maxDistortion", value)
  }

  override def render(context: VfxRenderContext, buffers: VfxPingPongWrapper): Unit =
    render(context, buffers.srcBuffer, buffers.dstBuffer)

  def render(context: VfxRenderContext, src: VfxFrameBuffer, dst: VfxFrameBuffer): Unit = {
    src.texture.get.bind(TEXTURE_HANDLE0)
    renderShader(context, dst)
  }
}
