/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 43
 * Covenant-baseline-methods: CopyEffect,rebind,render
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package vfx
package effects
package util

import sge.vfx.VfxRenderContext
import sge.vfx.framebuffer.{ VfxFrameBuffer, VfxPingPongWrapper }
import sge.vfx.gl.VfxGLUtils

class CopyEffect(using Sge)
    extends ShaderVfxEffect(
      VfxGLUtils.compileShader(
        Sge().files.classpath("sge/vfx/shaders/screenspace.vert"),
        Sge().files.classpath("sge/vfx/shaders/copy.frag")
      )
    )
    with ChainVfxEffect {

  import ShaderVfxEffect.*

  rebind()

  override def rebind(): Unit = {
    super.rebind()
    setUniform("u_texture0", TEXTURE_HANDLE0)
  }

  override def render(context: VfxRenderContext, buffers: VfxPingPongWrapper): Unit =
    render(context, buffers.srcBuffer, buffers.dstBuffer)

  def render(context: VfxRenderContext, src: VfxFrameBuffer, dst: VfxFrameBuffer): Unit = {
    // Bind src buffer's texture as a primary one.
    src.texture.get.bind(TEXTURE_HANDLE0)
    // Apply shader effect.
    renderShader(context, dst)
  }
}
