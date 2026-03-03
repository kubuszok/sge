/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/influencers/DynamicsInfluencer.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes (audit 2026-03-03):
 * - Array<DynamicsModifier> replaced with DynamicArray[DynamicsModifier].
 * - velocities.items[k] replaced with velocities(k) (DynamicArray indexed access).
 * - Null checks replaced with Nullable getChannel + isEmpty/foreach pattern.
 * - TMP_Q moved to companion object (was instance-level in Java via superclass static).
 * - write/read(Json) omitted (Json serialization not ported).
 * - All public methods faithfully ported.
 * - Status: pass
 */
package sge
package graphics
package g3d
package particles
package influencers

import java.util.Arrays

import sge.graphics.g3d.particles.ParallelArray.FloatChannel
import sge.graphics.g3d.particles.ParticleChannels
import sge.graphics.g3d.particles.ParticleController
import sge.math.{ MathUtils, Quaternion }
import sge.utils.DynamicArray
import sge.utils.Nullable

/** It's an {@link Influencer} which controls the particles dynamics (movement, rotations).
  * @author
  *   Inferno
  */
class DynamicsInfluencer extends Influencer {

  import DynamicsInfluencer.*

  var velocities:                      DynamicArray[DynamicsModifier] = scala.compiletime.uninitialized
  private var accellerationChannel:    FloatChannel                   = scala.compiletime.uninitialized
  private var positionChannel:         FloatChannel                   = scala.compiletime.uninitialized
  private var previousPositionChannel: FloatChannel                   = scala.compiletime.uninitialized
  private var rotationChannel:         FloatChannel                   = scala.compiletime.uninitialized
  private var angularVelocityChannel:  FloatChannel                   = scala.compiletime.uninitialized
  private var hasAcceleration:         Boolean                        = false
  private var has2dAngularVelocity:    Boolean                        = false
  private var has3dAngularVelocity:    Boolean                        = false

  this.velocities = DynamicArray[DynamicsModifier](3)

  def this(velocityModifiers: DynamicsModifier*) = {
    this()
    this.velocities = DynamicArray[DynamicsModifier](velocityModifiers.length)
    for (value <- velocityModifiers)
      this.velocities.add(value.copy().asInstanceOf[DynamicsModifier])
  }

  def this(velocityInfluencer: DynamicsInfluencer) = {
    this()
    this.velocities = DynamicArray[DynamicsModifier](velocityInfluencer.velocities.size)
    for (value <- velocityInfluencer.velocities)
      this.velocities.add(value.copy().asInstanceOf[DynamicsModifier])
  }

  override def allocateChannels(): Unit = {
    var k = 0
    while (k < velocities.size) {
      velocities(k).allocateChannels()
      k += 1
    }

    // Hack, shouldn't be done but after all the modifiers allocated their channels
    // it's possible to check if we need to allocate previous position channel
    val accelChannel: Nullable[FloatChannel] = controller.particles.getChannel(ParticleChannels.Acceleration)
    hasAcceleration = !accelChannel.isEmpty
    accelChannel.foreach { ch =>
      accellerationChannel = ch
    }
    if (hasAcceleration) {
      positionChannel = controller.particles.addChannel(ParticleChannels.Position)
      previousPositionChannel = controller.particles.addChannel(ParticleChannels.PreviousPosition)
    }

    // Angular velocity check
    val angVel2d: Nullable[FloatChannel] = controller.particles.getChannel(ParticleChannels.AngularVelocity2D)
    has2dAngularVelocity = !angVel2d.isEmpty
    if (has2dAngularVelocity) {
      angVel2d.foreach { ch =>
        angularVelocityChannel = ch
      }
      rotationChannel = controller.particles.addChannel(ParticleChannels.Rotation2D)
      has3dAngularVelocity = false
    } else {
      val angVel3d: Nullable[FloatChannel] = controller.particles.getChannel(ParticleChannels.AngularVelocity3D)
      has3dAngularVelocity = !angVel3d.isEmpty
      angVel3d.foreach { ch =>
        angularVelocityChannel = ch
      }
      if (has3dAngularVelocity) rotationChannel = controller.particles.addChannel(ParticleChannels.Rotation3D)
    }
  }

  override def set(particleController: ParticleController): Unit = {
    super.set(particleController)
    var k = 0
    while (k < velocities.size) {
      velocities(k).set(particleController)
      k += 1
    }
  }

  override def init(): Unit = {
    var k = 0
    while (k < velocities.size) {
      velocities(k).init()
      k += 1
    }
  }

  override def activateParticles(startIndex: Int, count: Int): Unit = {
    if (hasAcceleration) {
      // Previous position is the current position
      // Attention, this requires that some other influencer setting the position channel must execute before this influencer.
      var i = startIndex * positionChannel.strideSize
      val c = i + count * positionChannel.strideSize
      while (i < c) {
        previousPositionChannel.floatData(i + ParticleChannels.XOffset) = positionChannel.floatData(i + ParticleChannels.XOffset)
        previousPositionChannel.floatData(i + ParticleChannels.YOffset) = positionChannel.floatData(i + ParticleChannels.YOffset)
        previousPositionChannel.floatData(i + ParticleChannels.ZOffset) = positionChannel.floatData(i + ParticleChannels.ZOffset)
        /*
         * //Euler intialization previousPositionChannel.floatData(i+ParticleChannels.XOffset) =
         * previousPositionChannel.floatData(i+ParticleChannels.YOffset) = previousPositionChannel.floatData(i+ParticleChannels.ZOffset)
         * = 0;
         */
        i += positionChannel.strideSize
      }
    }

    if (has2dAngularVelocity) {
      // Rotation back to 0
      var i = startIndex * rotationChannel.strideSize
      val c = i + count * rotationChannel.strideSize
      while (i < c) {
        rotationChannel.floatData(i + ParticleChannels.CosineOffset) = 1
        rotationChannel.floatData(i + ParticleChannels.SineOffset) = 0
        i += rotationChannel.strideSize
      }
    } else if (has3dAngularVelocity) {
      // Rotation back to 0
      var i = startIndex * rotationChannel.strideSize
      val c = i + count * rotationChannel.strideSize
      while (i < c) {
        rotationChannel.floatData(i + ParticleChannels.XOffset) = 0
        rotationChannel.floatData(i + ParticleChannels.YOffset) = 0
        rotationChannel.floatData(i + ParticleChannels.ZOffset) = 0
        rotationChannel.floatData(i + ParticleChannels.WOffset) = 1
        i += rotationChannel.strideSize
      }
    }

    var k = 0
    while (k < velocities.size) {
      velocities(k).activateParticles(startIndex, count)
      k += 1
    }
  }

  override def update(): Unit = {
    // Clean previous frame velocities
    if (hasAcceleration)
      Arrays.fill(accellerationChannel.floatData, 0, controller.particles.size * accellerationChannel.strideSize, 0f)
    if (has2dAngularVelocity || has3dAngularVelocity)
      Arrays.fill(angularVelocityChannel.floatData, 0, controller.particles.size * angularVelocityChannel.strideSize, 0f)

    // Sum all the forces/accelerations
    var k = 0
    while (k < velocities.size) {
      velocities(k).update()
      k += 1
    }

    // Apply the forces
    if (hasAcceleration) {
      /*
       * //Euler Integration for(int i=0, offset = 0; i < controller.particles.size; ++i, offset +=positionChannel.strideSize){
       * previousPositionChannel.floatData(offset + ParticleChannels.XOffset) += accellerationChannel.floatData(offset +
       * ParticleChannels.XOffset)*controller.deltaTime; previousPositionChannel.floatData(offset + ParticleChannels.YOffset) +=
       * accellerationChannel.floatData(offset + ParticleChannels.YOffset)*controller.deltaTime; previousPositionChannel.floatData(offset
       * + ParticleChannels.ZOffset) += accellerationChannel.floatData(offset + ParticleChannels.ZOffset)*controller.deltaTime;
       *
       * positionChannel.floatData(offset + ParticleChannels.XOffset) += previousPositionChannel.floatData(offset +
       * ParticleChannels.XOffset)*controller.deltaTime; positionChannel.floatData(offset + ParticleChannels.YOffset) +=
       * previousPositionChannel.floatData(offset + ParticleChannels.YOffset)*controller.deltaTime; positionChannel.floatData(offset +
       * ParticleChannels.ZOffset) += previousPositionChannel.floatData(offset + ParticleChannels.ZOffset)*controller.deltaTime; }
       */
      // Verlet integration
      var i      = 0
      var offset = 0
      while (i < controller.particles.size) {
        val x = positionChannel.floatData(offset + ParticleChannels.XOffset)
        val y = positionChannel.floatData(offset + ParticleChannels.YOffset)
        val z = positionChannel.floatData(offset + ParticleChannels.ZOffset)
        positionChannel.floatData(offset + ParticleChannels.XOffset) = 2 * x -
          previousPositionChannel.floatData(offset + ParticleChannels.XOffset) +
          accellerationChannel.floatData(offset + ParticleChannels.XOffset) * controller.deltaTimeSqr
        positionChannel.floatData(offset + ParticleChannels.YOffset) = 2 * y -
          previousPositionChannel.floatData(offset + ParticleChannels.YOffset) +
          accellerationChannel.floatData(offset + ParticleChannels.YOffset) * controller.deltaTimeSqr
        positionChannel.floatData(offset + ParticleChannels.ZOffset) = 2 * z -
          previousPositionChannel.floatData(offset + ParticleChannels.ZOffset) +
          accellerationChannel.floatData(offset + ParticleChannels.ZOffset) * controller.deltaTimeSqr
        previousPositionChannel.floatData(offset + ParticleChannels.XOffset) = x
        previousPositionChannel.floatData(offset + ParticleChannels.YOffset) = y
        previousPositionChannel.floatData(offset + ParticleChannels.ZOffset) = z
        i += 1
        offset += positionChannel.strideSize
      }
    }

    if (has2dAngularVelocity) {
      var i      = 0
      var offset = 0
      while (i < controller.particles.size) {
        val rotation = angularVelocityChannel.floatData(i) * controller.deltaTime
        if (rotation != 0) {
          val cosBeta       = MathUtils.cosDeg(rotation)
          val sinBeta       = MathUtils.sinDeg(rotation)
          val currentCosine = rotationChannel.floatData(offset + ParticleChannels.CosineOffset)
          val currentSine   = rotationChannel.floatData(offset + ParticleChannels.SineOffset)
          val newCosine     = currentCosine * cosBeta - currentSine * sinBeta
          val newSine       = currentSine * cosBeta + currentCosine * sinBeta
          rotationChannel.floatData(offset + ParticleChannels.CosineOffset) = newCosine
          rotationChannel.floatData(offset + ParticleChannels.SineOffset) = newSine
        }
        i += 1
        offset += rotationChannel.strideSize
      }
    } else if (has3dAngularVelocity) {
      var i             = 0
      var offset        = 0
      var angularOffset = 0
      while (i < controller.particles.size) {
        val wx = angularVelocityChannel.floatData(angularOffset + ParticleChannels.XOffset)
        val wy = angularVelocityChannel.floatData(angularOffset + ParticleChannels.YOffset)
        val wz = angularVelocityChannel.floatData(angularOffset + ParticleChannels.ZOffset)
        val qx = rotationChannel.floatData(offset + ParticleChannels.XOffset)
        val qy = rotationChannel.floatData(offset + ParticleChannels.YOffset)
        val qz = rotationChannel.floatData(offset + ParticleChannels.ZOffset)
        val qw = rotationChannel.floatData(offset + ParticleChannels.WOffset)
        TMP_Q.set(wx, wy, wz, 0).mul(qx, qy, qz, qw).mul(0.5f * controller.deltaTime).add(qx, qy, qz, qw).nor()
        rotationChannel.floatData(offset + ParticleChannels.XOffset) = TMP_Q.x
        rotationChannel.floatData(offset + ParticleChannels.YOffset) = TMP_Q.y
        rotationChannel.floatData(offset + ParticleChannels.ZOffset) = TMP_Q.z
        rotationChannel.floatData(offset + ParticleChannels.WOffset) = TMP_Q.w
        i += 1
        offset += rotationChannel.strideSize
        angularOffset += angularVelocityChannel.strideSize
      }
    }
  }

  override def copy(): DynamicsInfluencer =
    new DynamicsInfluencer(this)
}

object DynamicsInfluencer {
  private val TMP_Q: Quaternion = new Quaternion()
}
