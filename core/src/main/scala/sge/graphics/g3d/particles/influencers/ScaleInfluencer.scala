/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/influencers/ScaleInfluencer.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audit 2026-03-03):
 * - Copy constructor manually copies value and valueChannelDescriptor instead of
 *   delegating to super(scaleInfluencer). Both approaches are functionally equivalent.
 * - All public methods faithfully ported (activateParticles, copy).
 * - Status: pass
 */
package sge
package graphics
package g3d
package particles
package influencers

import sge.graphics.g3d.particles.ParticleChannels
import sge.graphics.g3d.particles.ParticleControllerComponent

/** It's an {@link Influencer} which controls the scale of the particles.
  * @author
  *   Inferno
  */
class ScaleInfluencer extends SimpleInfluencer {

  valueChannelDescriptor = ParticleChannels.Scale

  def this(scaleInfluencer: ScaleInfluencer) = {
    this()
    value.load(scaleInfluencer.value)
    valueChannelDescriptor = scaleInfluencer.valueChannelDescriptor
  }

  override def activateParticles(startIndex: Int, count: Int): Unit =
    if (value.relative) {
      var i = startIndex * valueChannel.strideSize
      var a = startIndex * interpolationChannel.strideSize
      val c = i + count * valueChannel.strideSize
      while (i < c) {
        val start = value.newLowValue() * controller.scale.x
        val diff  = value.newHighValue() * controller.scale.x
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
        val start = value.newLowValue() * controller.scale.x
        val diff  = value.newHighValue() * controller.scale.x - start
        interpolationChannel.floatData(a + ParticleChannels.InterpolationStartOffset) = start
        interpolationChannel.floatData(a + ParticleChannels.InterpolationDiffOffset) = diff
        valueChannel.floatData(i) = start + diff * value.getScale(0)
        i += valueChannel.strideSize
        a += interpolationChannel.strideSize
      }
    }

  override def copy(): ParticleControllerComponent =
    ScaleInfluencer(this)
}
