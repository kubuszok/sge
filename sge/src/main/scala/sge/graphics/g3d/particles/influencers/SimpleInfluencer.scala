/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/influencers/SimpleInfluencer.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audit 2026-03-03):
 * - All public methods faithfully ported (allocateChannels, activateParticles, update).
 * - write/read(Json) omitted (Json serialization not ported).
 * - channel.data[] renamed to channel.floatData() per SGE ParallelArray API.
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 98
 * Covenant-baseline-methods: SimpleInfluencer,a,activateParticles,allocateChannels,c,i,interpolationChannel,l,lifeChannel,set,this,update,value,valueChannel,valueChannelDescriptor
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/particles/influencers/SimpleInfluencer.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
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

  var value: ScaledNumericValue = ScaledNumericValue()
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
    if (!value.relative) {
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
