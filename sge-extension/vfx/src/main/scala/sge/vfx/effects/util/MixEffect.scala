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

import sge.math.MathUtils
import sge.vfx.VfxRenderContext
import sge.vfx.framebuffer.VfxFrameBuffer
import sge.vfx.gl.VfxGLUtils

/** Simply mixes two frames with a factor of [[mixFactor]].
  *
  * Depends on [[MixEffect.Method]] the result will be combined with either: `max(src0, src1 * mixFactor)` or `mix(src0, src1,
  * mixFactor)`
  */
class MixEffect(method: MixEffect.Method)(using Sge)
    extends ShaderVfxEffect(
      VfxGLUtils.compileShader(
        Sge().files.classpath("sge/vfx/shaders/screenspace.vert"),
        Sge().files.classpath("sge/vfx/shaders/mix.frag"),
        "#define METHOD " + method.toString
      )
    ) {

  import ShaderVfxEffect.*

  private var _mixFactor: Float = 0.5f

  rebind()

  override def rebind(): Unit = {
    super.rebind()
    program.bind()
    program.setUniformi("u_texture0", TEXTURE_HANDLE0)
    program.setUniformi("u_texture1", TEXTURE_HANDLE1)
    program.setUniformf("u_mix", _mixFactor)
    Sge().graphics.gl20.glUseProgram(0)
  }

  def render(context: VfxRenderContext, src0: VfxFrameBuffer, src1: VfxFrameBuffer, dst: VfxFrameBuffer): Unit = {
    src0.texture.get.bind(TEXTURE_HANDLE0)
    src1.texture.get.bind(TEXTURE_HANDLE1)
    renderShader(context, dst)
  }

  def mixFactor: Float = _mixFactor
  def mixFactor_=(value: Float): Unit = {
    _mixFactor = MathUtils.clamp(value, 0f, 1f)
    setUniform("u_mix", _mixFactor)
  }
}

object MixEffect {
  /** Defines which function will be used to combine mix the two frames. */
  enum Method extends java.lang.Enum[Method] {
    case MAX, MIX
  }
}
