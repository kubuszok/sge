/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/ParticleChannels.java
 * Original authors: inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All channel descriptors, offsets, and initializers ported faithfully
 * - Static members → companion object vals
 * - Initializer singletons use lazy val + get() (Java used null-check lazy init)
 * - resetIds visibility: protected → protected[particles] (Java protected accessible to subpackage)
 * - Initializer channel.data → channel.floatData (Scala FloatChannel rename)
 * - Constructor calls resetIds() in Java; Scala initializes currentId = currentGlobalId directly
 */
package sge
package graphics
package g3d
package particles

import java.util.Arrays

import sge.graphics.g3d.particles.ParallelArray.{ ChannelDescriptor, ChannelInitializer, FloatChannel }

/** This contains all the definitions of particle related channels and channel initializers. It is also used by the {@link ParticleController} to handle temporary channels allocated by influencers.
  * @author
  *   inferno
  */
class ParticleChannels() {

  import ParticleChannels.*

  private var currentId: Int = currentGlobalId

  def newId(): Int = {
    val id = currentId
    currentId += 1
    id
  }

  protected[particles] def resetIds(): Unit =
    currentId = currentGlobalId
}

object ParticleChannels {

  private var currentGlobalId: Int = 0

  def newGlobalId(): Int = {
    val id = currentGlobalId
    currentGlobalId += 1
    id
  }

  // Initializers
  object TextureRegionInitializer extends ChannelInitializer[FloatChannel] {

    override def init(channel: FloatChannel): Unit = {
      var i = 0
      val c = channel.floatData.length
      while (i < c) {
        channel.floatData(i + ParticleChannels.UOffset) = 0
        channel.floatData(i + ParticleChannels.VOffset) = 0
        channel.floatData(i + ParticleChannels.U2Offset) = 1
        channel.floatData(i + ParticleChannels.V2Offset) = 1
        channel.floatData(i + ParticleChannels.HalfWidthOffset) = 0.5f
        channel.floatData(i + ParticleChannels.HalfHeightOffset) = 0.5f
        i += channel.strideSize
      }
    }
  }

  object ColorInitializer extends ChannelInitializer[FloatChannel] {

    override def init(channel: FloatChannel): Unit =
      Arrays.fill(channel.floatData, 0, channel.floatData.length, 1f)
  }

  object ScaleInitializer extends ChannelInitializer[FloatChannel] {

    override def init(channel: FloatChannel): Unit =
      Arrays.fill(channel.floatData, 0, channel.floatData.length, 1f)
  }

  object Rotation2dInitializer extends ChannelInitializer[FloatChannel] {

    override def init(channel: FloatChannel): Unit = {
      var i = 0
      val c = channel.floatData.length
      while (i < c) {
        channel.floatData(i + ParticleChannels.CosineOffset) = 1
        channel.floatData(i + ParticleChannels.SineOffset) = 0
        i += channel.strideSize
      }
    }
  }

  object Rotation3dInitializer extends ChannelInitializer[FloatChannel] {

    override def init(channel: FloatChannel): Unit = {
      var i = 0
      val c = channel.floatData.length
      while (i < c) {
        channel.floatData(i + ParticleChannels.XOffset) = 0
        channel.floatData(i + ParticleChannels.YOffset) = 0
        channel.floatData(i + ParticleChannels.ZOffset) = 0
        channel.floatData(i + ParticleChannels.WOffset) = 1
        i += channel.strideSize
      }
    }
  }

  // Channels
  /** Channels of common use like position, life, color, etc... */
  val Life:     ChannelDescriptor = ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 3)
  val Position: ChannelDescriptor =
    ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 3) // gl units
  val PreviousPosition: ChannelDescriptor =
    ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 3)
  val Color:         ChannelDescriptor = ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 4)
  val TextureRegion: ChannelDescriptor =
    ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 6)
  val Rotation2D:    ChannelDescriptor = ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 2)
  val Rotation3D:    ChannelDescriptor = ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 4)
  val Scale:         ChannelDescriptor = ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 1)
  val ModelInstance: ChannelDescriptor =
    ChannelDescriptor(newGlobalId(), (size: Int) => new Array[sge.graphics.g3d.ModelInstance](size), 1)
  val ParticleController: ChannelDescriptor =
    ChannelDescriptor(
      newGlobalId(),
      (size: Int) => new Array[sge.graphics.g3d.particles.ParticleController](size),
      1
    )
  val Acceleration: ChannelDescriptor =
    ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 3) // gl units/s2
  val AngularVelocity2D: ChannelDescriptor =
    ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 1)
  val AngularVelocity3D: ChannelDescriptor =
    ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 3)
  val Interpolation:  ChannelDescriptor = ChannelDescriptor(-1, (size: Int) => new Array[Float](size), 2)
  val Interpolation4: ChannelDescriptor = ChannelDescriptor(-1, (size: Int) => new Array[Float](size), 4)
  val Interpolation6: ChannelDescriptor = ChannelDescriptor(-1, (size: Int) => new Array[Float](size), 6)

  // Offsets
  /** Offsets to acess a particular value inside a stride of a given channel */
  val CurrentLifeOffset:           Int = 0
  val TotalLifeOffset:             Int = 1
  val LifePercentOffset:           Int = 2
  val RedOffset:                   Int = 0
  val GreenOffset:                 Int = 1
  val BlueOffset:                  Int = 2
  val AlphaOffset:                 Int = 3
  val InterpolationStartOffset:    Int = 0
  val InterpolationDiffOffset:     Int = 1
  val VelocityStrengthStartOffset: Int = 0
  val VelocityStrengthDiffOffset:  Int = 1
  val VelocityThetaStartOffset:    Int = 0
  val VelocityThetaDiffOffset:     Int = 1
  val VelocityPhiStartOffset:      Int = 2
  val VelocityPhiDiffOffset:       Int = 3
  val XOffset:                     Int = 0
  val YOffset:                     Int = 1
  val ZOffset:                     Int = 2
  val WOffset:                     Int = 3
  val UOffset:                     Int = 0
  val VOffset:                     Int = 1
  val U2Offset:                    Int = 2
  val V2Offset:                    Int = 3
  val HalfWidthOffset:             Int = 4
  val HalfHeightOffset:            Int = 5
  val CosineOffset:                Int = 0
  val SineOffset:                  Int = 1
}
