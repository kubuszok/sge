/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 130
 * Covenant-baseline-methods: BloomEffect,Settings,_blending,apply,applySettings,baseIntensity,baseIntensity_,baseSaturation,baseSaturation_,blendingDestFactor,blendingEnabled,blendingSourceFactor,blendingWasEnabled,bloomIntensity,bloomIntensity_,bloomSaturation,bloomSaturation_,bloomThreshold,bloomThreshold_,blur,blurAmount,blurAmount_,blurPasses,blurPasses_,blurType,blurType_,combine,copy,dfactor,disableBlending,enableBlending,origSrc,render,sfactor,this,threshold
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package vfx
package effects

import sge.graphics.{ BlendFactor, EnableCap, GL20 }
import sge.vfx.VfxRenderContext
import sge.vfx.effects.GaussianBlurEffect.BlurType
import sge.vfx.effects.util.{ CombineEffect, CopyEffect, GammaThresholdEffect }
import sge.vfx.framebuffer.VfxPingPongWrapper
import sge.vfx.gl.VfxGLUtils

class BloomEffect(settings: BloomEffect.Settings)(using Sge) extends CompositeVfxEffect with ChainVfxEffect {

  def this()(using Sge) = this(BloomEffect.Settings(10, 0.85f, 1f, .85f, 1.1f, .85f))

  private val copy:      CopyEffect           = register(CopyEffect())
  private val blur:      GaussianBlurEffect   = register(GaussianBlurEffect())
  private val threshold: GammaThresholdEffect = register(GammaThresholdEffect(GammaThresholdEffect.Type.RGBA))
  private val combine:   CombineEffect        = register(CombineEffect())

  private var _blending: Boolean     = false
  private var sfactor:   BlendFactor = BlendFactor(0)
  private var dfactor:   BlendFactor = BlendFactor(0)

  applySettings(settings)

  override def render(context: VfxRenderContext, buffers: VfxPingPongWrapper): Unit = {
    // Preserve the input buffer data.
    val origSrc = context.bufferPool.obtain()
    copy.render(context, buffers.srcBuffer, origSrc)

    val blendingWasEnabled = VfxGLUtils.isGLEnabled(GL20.GL_BLEND)
    Sge().graphics.gl20.glDisable(EnableCap.Blend)

    // High-pass filter
    // Only areas with pixels >= threshold are blit.
    threshold.render(context, buffers)
    buffers.swap()

    // Blur pass
    blur.render(context, buffers)
    buffers.swap()

    if (_blending || blendingWasEnabled) {
      Sge().graphics.gl20.glEnable(EnableCap.Blend)
    }

    if (_blending) {
      Sge().graphics.gl20.glBlendFunc(sfactor, dfactor)
    }

    // Mix original scene and blurred result.
    combine.render(context, origSrc, buffers.srcBuffer, buffers.dstBuffer)

    context.bufferPool.free(origSrc)
  }

  def baseIntensity:                     Float = combine.source1Intensity
  def baseIntensity_=(intensity: Float): Unit  = combine.source1Intensity = intensity

  def baseSaturation:                      Float = combine.source1Saturation
  def baseSaturation_=(saturation: Float): Unit  = combine.source1Saturation = saturation

  def bloomIntensity:                     Float = combine.source2Intensity
  def bloomIntensity_=(intensity: Float): Unit  = combine.source2Intensity = intensity

  def bloomSaturation:                      Float = combine.source2Saturation
  def bloomSaturation_=(saturation: Float): Unit  = combine.source2Saturation = saturation

  def blurPasses:                Int  = blur.passes
  def blurPasses_=(passes: Int): Unit = blur.passes = passes

  def blurAmount:                  Float = blur.amount
  def blurAmount_=(amount: Float): Unit  = blur.amount = amount

  def blendingEnabled: Boolean = _blending

  def bloomThreshold:                 Float = threshold.gamma
  def bloomThreshold_=(gamma: Float): Unit  = threshold.gamma = gamma

  def blendingSourceFactor: BlendFactor = sfactor
  def blendingDestFactor:   BlendFactor = dfactor

  def enableBlending(srcFactor: BlendFactor, dstFactor: BlendFactor): Unit = {
    _blending = true
    sfactor = srcFactor
    dfactor = dstFactor
  }

  def disableBlending(): Unit =
    _blending = false

  def blurType:                       BlurType = blur.blurType
  def blurType_=(blurType: BlurType): Unit     = blur.setType(blurType)

  def applySettings(s: BloomEffect.Settings): Unit = {
    bloomThreshold = s.bloomThreshold
    baseIntensity = s.baseIntensity
    baseSaturation = s.baseSaturation
    bloomIntensity = s.bloomIntensity
    bloomSaturation = s.bloomSaturation
    blurPasses = s.blurPasses
    blurAmount = s.blurAmount
    blurType = s.blurType
  }
}

object BloomEffect {
  final case class Settings(
    blurType:        BlurType = BlurType.Gaussian5x5b,
    blurPasses:      Int = 10,
    blurAmount:      Float = 0f,
    bloomThreshold:  Float = 0.85f,
    baseIntensity:   Float = 1f,
    baseSaturation:  Float = 0.85f,
    bloomIntensity:  Float = 1.1f,
    bloomSaturation: Float = 0.85f
  )

  object Settings {
    def apply(blurPasses: Int, bloomThreshold: Float, baseIntensity: Float, baseSaturation: Float, bloomIntensity: Float, bloomSaturation: Float): Settings =
      Settings(BlurType.Gaussian5x5b, blurPasses, 0f, bloomThreshold, baseIntensity, baseSaturation, bloomIntensity, bloomSaturation)
  }
}
