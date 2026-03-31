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
import sge.vfx.framebuffer.VfxFrameBuffer
import sge.vfx.gl.VfxGLUtils

/** Merges two frames with an option to change intensity and saturation for each. After applying saturation and intensity factors, the result frame is `src0 * (1.0 - src1) + src1`.
  *
  * If you're looking for rather straightforward way to mix two frames into one, have a look at [[MixEffect]].
  */
class CombineEffect(using Sge)
    extends ShaderVfxEffect(
      VfxGLUtils.compileShader(
        Sge().files.classpath("sge/vfx/shaders/screenspace.vert"),
        Sge().files.classpath("sge/vfx/shaders/combine.frag")
      )
    ) {

  import ShaderVfxEffect.*

  private var s1i: Float = 1f
  private var s1s: Float = 1f
  private var s2i: Float = 1f
  private var s2s: Float = 1f

  rebind()

  override def rebind(): Unit = {
    super.rebind()
    program.bind()
    program.setUniformi("u_texture0", TEXTURE_HANDLE0)
    program.setUniformi("u_texture1", TEXTURE_HANDLE1)
    program.setUniformf("u_src0Intensity", s1i)
    program.setUniformf("u_src1Intensity", s2i)
    program.setUniformf("u_src0Saturation", s1s)
    program.setUniformf("u_src1Saturation", s2s)
    Sge().graphics.gl20.glUseProgram(0)
  }

  def render(context: VfxRenderContext, src0: VfxFrameBuffer, src1: VfxFrameBuffer, dst: VfxFrameBuffer): Unit = {
    src0.texture.get.bind(TEXTURE_HANDLE0)
    src1.texture.get.bind(TEXTURE_HANDLE1)
    renderShader(context, dst)
  }

  def source1Intensity:                     Float = s1i
  def source1Intensity_=(intensity: Float): Unit  = {
    s1i = intensity
    setUniform("u_src0Intensity", intensity)
  }

  def source2Intensity:                     Float = s2i
  def source2Intensity_=(intensity: Float): Unit  = {
    s2i = intensity
    setUniform("u_src1Intensity", intensity)
  }

  def source1Saturation:                      Float = s1s
  def source1Saturation_=(saturation: Float): Unit  = {
    s1s = saturation
    setUniform("u_src0Saturation", saturation)
  }

  def source2Saturation:                      Float = s2s
  def source2Saturation_=(saturation: Float): Unit  = {
    s2s = saturation
    setUniform("u_src1Saturation", saturation)
  }
}
