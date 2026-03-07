/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/influencers/ColorInfluencer.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audit 2026-03-03):
 * - Inner classes Random, Single moved to companion object (Java static classes).
 * - colorChannel in abstract class promoted to public var (was package-private in Java).
 * - Random.colorChannel removed (inherits from parent); Java had a separate field.
 * - write/read(Json) in Single omitted (Json serialization not ported).
 * - All public methods faithfully ported.
 * - TODO: direct Color.r/g/b field mutation — update when Color becomes immutable
 * - Status: pass
 */
package sge
package graphics
package g3d
package particles
package influencers

import sge.graphics.g3d.particles.ParallelArray.FloatChannel
import sge.graphics.g3d.particles.ParticleChannels
import sge.graphics.g3d.particles.values.{ GradientColorValue, ScaledNumericValue }
import sge.math.MathUtils

/** It's an {@link Influencer} which controls particles color and transparency.
  * @author
  *   Inferno
  */
abstract class ColorInfluencer extends Influencer {
  var colorChannel: FloatChannel = scala.compiletime.uninitialized

  override def allocateChannels(): Unit =
    colorChannel = controller.particles.addChannel(ParticleChannels.Color)
}

object ColorInfluencer {

  /** It's an {@link Influencer} which assigns a random color when a particle is activated. */
  class Random extends ColorInfluencer {

    override def allocateChannels(): Unit =
      colorChannel = controller.particles.addChannel(ParticleChannels.Color)

    override def activateParticles(startIndex: Int, count: Int): Unit = {
      var i = startIndex * colorChannel.strideSize
      val c = i + count * colorChannel.strideSize
      while (i < c) {
        colorChannel.floatData(i + ParticleChannels.RedOffset) = MathUtils.random()
        colorChannel.floatData(i + ParticleChannels.GreenOffset) = MathUtils.random()
        colorChannel.floatData(i + ParticleChannels.BlueOffset) = MathUtils.random()
        colorChannel.floatData(i + ParticleChannels.AlphaOffset) = MathUtils.random()
        i += colorChannel.strideSize
      }
    }

    override def copy(): Random =
      Random()
  }

  /** It's an {@link Influencer} which manages the particle color during its life time. */
  class Single extends ColorInfluencer {
    var alphaInterpolationChannel: FloatChannel       = scala.compiletime.uninitialized
    var lifeChannel:               FloatChannel       = scala.compiletime.uninitialized
    var alphaValue:                ScaledNumericValue = ScaledNumericValue()
    var colorValue:                GradientColorValue = GradientColorValue()

    alphaValue.setHigh(1)

    def this(billboardColorInfluencer: Single) = {
      this()
      set(billboardColorInfluencer)
    }

    def set(colorInfluencer: Single): Unit = {
      this.colorValue.load(colorInfluencer.colorValue)
      this.alphaValue.load(colorInfluencer.alphaValue)
    }

    override def allocateChannels(): Unit = {
      super.allocateChannels()
      // Hack this allows to share the channel descriptor structure but using a different id temporarily
      ParticleChannels.Interpolation.id = controller.particleChannels.newId()
      alphaInterpolationChannel = controller.particles.addChannel(ParticleChannels.Interpolation)
      lifeChannel = controller.particles.addChannel(ParticleChannels.Life)
    }

    override def activateParticles(startIndex: Int, count: Int): Unit = {
      var i = startIndex * colorChannel.strideSize
      var a = startIndex * alphaInterpolationChannel.strideSize
      var l = startIndex * lifeChannel.strideSize + ParticleChannels.LifePercentOffset
      val c = i + count * colorChannel.strideSize
      while (i < c) {
        val alphaStart = alphaValue.newLowValue()
        val alphaDiff  = alphaValue.newHighValue() - alphaStart
        colorValue.getColor(0, colorChannel.floatData, i)
        colorChannel.floatData(i + ParticleChannels.AlphaOffset) = alphaStart +
          alphaDiff * alphaValue.getScale(lifeChannel.floatData(l))
        alphaInterpolationChannel.floatData(a + ParticleChannels.InterpolationStartOffset) = alphaStart
        alphaInterpolationChannel.floatData(a + ParticleChannels.InterpolationDiffOffset) = alphaDiff
        i += colorChannel.strideSize
        a += alphaInterpolationChannel.strideSize
        l += lifeChannel.strideSize
      }
    }

    override def update(): Unit = {
      var i = 0
      var a = 0
      var l = ParticleChannels.LifePercentOffset
      val c = controller.particles.size * colorChannel.strideSize
      while (i < c) {
        val lifePercent = lifeChannel.floatData(l)
        colorValue.getColor(lifePercent, colorChannel.floatData, i)
        colorChannel.floatData(i + ParticleChannels.AlphaOffset) = alphaInterpolationChannel.floatData(a + ParticleChannels.InterpolationStartOffset) +
          alphaInterpolationChannel.floatData(a + ParticleChannels.InterpolationDiffOffset) *
          alphaValue.getScale(lifePercent)
        i += colorChannel.strideSize
        a += alphaInterpolationChannel.strideSize
        l += lifeChannel.strideSize
      }
    }

    override def copy(): Single =
      Single(this)
  }
}
