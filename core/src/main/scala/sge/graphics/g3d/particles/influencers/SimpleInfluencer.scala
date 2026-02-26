/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/influencers/SimpleInfluencer.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package particles
package influencers

import sge.graphics.g3d.particles.ParallelArray.{ ChannelDescriptor, FloatChannel }
import sge.graphics.g3d.particles.ParticleChannels
import sge.graphics.g3d.particles.values.ScaledNumericValue

/** It's an {@link Influencer} which controls a generic channel of the particles. It handles the interpolation through time using {@link ScaledNumericValue}.
  * @author
  *   Inferno
  */
abstract class SimpleInfluencer extends Influencer {

  var value: ScaledNumericValue = new ScaledNumericValue()
  value.setHigh(1)

  var valueChannel:           FloatChannel      = scala.compiletime.uninitialized
  var interpolationChannel:   FloatChannel      = scala.compiletime.uninitialized
  var lifeChannel:            FloatChannel      = scala.compiletime.uninitialized
  var valueChannelDescriptor: ChannelDescriptor = scala.compiletime.uninitialized

  def this(billboardScaleinfluencer: SimpleInfluencer) = {
    this()
    set(billboardScaleinfluencer)
  }

  private def set(scaleInfluencer: SimpleInfluencer): Unit = {
    value.load(scaleInfluencer.value)
    valueChannelDescriptor = scaleInfluencer.valueChannelDescriptor
  }

  override def allocateChannels(): Unit = {
    valueChannel = controller.particles.addChannel(valueChannelDescriptor)
    ParticleChannels.Interpolation.id = controller.particleChannels.newId()
    interpolationChannel = controller.particles.addChannel(ParticleChannels.Interpolation)
    lifeChannel = controller.particles.addChannel(ParticleChannels.Life)
  }

  override def activateParticles(startIndex: Int, count: Int): Unit =
    if (!value.isRelative()) {
      var i = startIndex * valueChannel.strideSize
      var a = startIndex * interpolationChannel.strideSize
      val c = i + count * valueChannel.strideSize
      while (i < c) {
        val start = value.newLowValue()
        val diff  = value.newHighValue() - start
        interpolationChannel.floatData(a + ParticleChannels.InterpolationStartOffset) = start
        interpolationChannel.floatData(a + ParticleChannels.InterpolationDiffOffset) = diff
        valueChannel.floatData(i) = start + diff * value.getScale(0)
        i += valueChannel.strideSize
        a += interpolationChannel.strideSize
      }
    } else {
      var i = startIndex * valueChannel.strideSize
      var a = startIndex * interpolationChannel.strideSize
      val c = i + count * valueChannel.strideSize
      while (i < c) {
        val start = value.newLowValue()
        val diff  = value.newHighValue()
        interpolationChannel.floatData(a + ParticleChannels.InterpolationStartOffset) = start
        interpolationChannel.floatData(a + ParticleChannels.InterpolationDiffOffset) = diff
        valueChannel.floatData(i) = start + diff * value.getScale(0)
        i += valueChannel.strideSize
        a += interpolationChannel.strideSize
      }
    }

  override def update(): Unit = {
    var i = 0
    var a = 0
    var l = ParticleChannels.LifePercentOffset
    val c = controller.particles.size * valueChannel.strideSize
    while (i < c) {
      valueChannel.floatData(i) = interpolationChannel.floatData(a + ParticleChannels.InterpolationStartOffset) +
        interpolationChannel.floatData(a + ParticleChannels.InterpolationDiffOffset) * value.getScale(lifeChannel.floatData(l))
      i += valueChannel.strideSize
      a += interpolationChannel.strideSize
      l += lifeChannel.strideSize
    }
  }
}
