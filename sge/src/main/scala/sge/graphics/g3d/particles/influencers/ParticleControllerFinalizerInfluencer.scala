/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/influencers/ParticleControllerFinalizerInfluencer.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audit 2026-03-03):
 * - init(): null checks replaced with Nullable getChannel + getOrElse/isDefined/foreach.
 * - update(): particleController.update(controller.deltaTime) passes deltaTime explicitly;
 *   Java calls parameterless update() which reads Gdx.graphics.getDeltaTime() internally.
 *   The SGE ParticleController.update() requires (using Sge) context, so the deltaTime
 *   overload is used here instead. Functionally equivalent.
 * - channel.data[] renamed to channel.floatData()/objectData() per SGE ParallelArray API.
 * - Fields promoted from package-private to public vars.
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 91
 * Covenant-baseline-methods: ParticleControllerFinalizerInfluencer,allocateChannels,c,ch,controllerChannel,copy,hasRotation,hasScale,i,init,positionChannel,positionOffset,rc,rotationChannel,sc,scaleChannel,update
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/particles/influencers/ParticleControllerFinalizerInfluencer.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package particles
package influencers

import sge.graphics.g3d.particles.ParallelArray.{ FloatChannel, ObjectChannel }
import sge.graphics.g3d.particles.{ ParticleChannels, ParticleController }
import sge.utils.SgeError

/** It's an {@link Influencer} which updates the simulation of particles containing a {@link ParticleController}. Must be the last influencer to be updated, so it has to be placed at the end of the
  * influencers list when creating a {@link ParticleController}.
  * @author
  *   Inferno
  */
class ParticleControllerFinalizerInfluencer extends Influencer {
  var positionChannel:   FloatChannel                      = scala.compiletime.uninitialized
  var scaleChannel:      FloatChannel                      = scala.compiletime.uninitialized
  var rotationChannel:   FloatChannel                      = scala.compiletime.uninitialized
  var controllerChannel: ObjectChannel[ParticleController] = scala.compiletime.uninitialized
  var hasScale:          Boolean                           = false
  var hasRotation:       Boolean                           = false

  override def init(): Unit = {
    val ch = controller.particles.getChannel[ObjectChannel[ParticleController]](ParticleChannels.ParticleController)
    controllerChannel = ch.getOrElse(
      throw SgeError.InvalidInput("ParticleController channel not found, specify an influencer which will allocate it please.")
    )
    val sc = controller.particles.getChannel[FloatChannel](ParticleChannels.Scale)
    hasScale = sc.isDefined
    sc.foreach { s => scaleChannel = s }
    val rc = controller.particles.getChannel[FloatChannel](ParticleChannels.Rotation3D)
    hasRotation = rc.isDefined
    rc.foreach { r => rotationChannel = r }
  }

  override def allocateChannels(): Unit =
    positionChannel = controller.particles.addChannel(ParticleChannels.Position)

  override def update(): Unit = {
    var i              = 0
    var positionOffset = 0
    val c              = controller.particles.size
    while (i < c) {
      val particleController = controllerChannel.objectData(i)
      val scale              = if (hasScale) scaleChannel.floatData(i) else 1f
      var qx                 = 0f; var qy = 0f; var qz = 0f; var qw = 1f
      if (hasRotation) {
        val rotationOffset = i * rotationChannel.strideSize
        qx = rotationChannel.floatData(rotationOffset + ParticleChannels.XOffset)
        qy = rotationChannel.floatData(rotationOffset + ParticleChannels.YOffset)
        qz = rotationChannel.floatData(rotationOffset + ParticleChannels.ZOffset)
        qw = rotationChannel.floatData(rotationOffset + ParticleChannels.WOffset)
      }
      particleController.setTransform(
        positionChannel.floatData(positionOffset + ParticleChannels.XOffset),
        positionChannel.floatData(positionOffset + ParticleChannels.YOffset),
        positionChannel.floatData(positionOffset + ParticleChannels.ZOffset),
        qx,
        qy,
        qz,
        qw,
        scale
      )
      particleController.update(controller.deltaTime)
      i += 1
      positionOffset += positionChannel.strideSize
    }
  }

  override def copy(): ParticleControllerFinalizerInfluencer =
    ParticleControllerFinalizerInfluencer()
}
