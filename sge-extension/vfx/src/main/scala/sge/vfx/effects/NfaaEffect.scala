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

/** Normal filtered anti-aliasing filter.
  * @author
  *   Toni Sagrista
  * @author
  *   metaphore
  */
class NfaaEffect(supportAlpha: Boolean)(using Sge)
    extends ShaderVfxEffect(
      VfxGLUtils.compileShader(
        Sge().files.classpath("sge/vfx/shaders/screenspace.vert"),
        Sge().files.classpath("sge/vfx/shaders/nfaa.frag"),
        if (supportAlpha) "#define SUPPORT_ALPHA" else ""
      )
    )
    with ChainVfxEffect {

  import ShaderVfxEffect.*

  private val viewportInverse: Vector2 = Vector2()

  override def rebind(): Unit = {
    super.rebind()
    program.bind()
    program.setUniformi("u_texture0", TEXTURE_HANDLE0)
    program.setUniformf("u_viewportInverse", viewportInverse)
    Sge().graphics.gl20.glUseProgram(0)
  }

  override def resize(width: Int, height: Int): Unit = {
    super.resize(width, height)
    viewportInverse.set(1f / width, 1f / height)
    setUniform("u_viewportInverse", viewportInverse)
  }

  override def render(context: VfxRenderContext, buffers: VfxPingPongWrapper): Unit =
    render(context, buffers.srcBuffer, buffers.dstBuffer)

  def render(context: VfxRenderContext, src: VfxFrameBuffer, dst: VfxFrameBuffer): Unit = {
    src.texture.get.bind(TEXTURE_HANDLE0)
    renderShader(context, dst)
  }
}
