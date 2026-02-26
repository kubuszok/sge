/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/ParticleChannels.java
 * Original authors: inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
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
  class TextureRegionInitializer extends ChannelInitializer[FloatChannel] {

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

  object TextureRegionInitializer {
    private lazy val instance: TextureRegionInitializer = new TextureRegionInitializer()

    def get(): TextureRegionInitializer = instance
  }

  class ColorInitializer extends ChannelInitializer[FloatChannel] {

    override def init(channel: FloatChannel): Unit =
      Arrays.fill(channel.floatData, 0, channel.floatData.length, 1f)
  }

  object ColorInitializer {
    private lazy val instance: ColorInitializer = new ColorInitializer()

    def get(): ColorInitializer = instance
  }

  class ScaleInitializer extends ChannelInitializer[FloatChannel] {

    override def init(channel: FloatChannel): Unit =
      Arrays.fill(channel.floatData, 0, channel.floatData.length, 1f)
  }

  object ScaleInitializer {
    private lazy val instance: ScaleInitializer = new ScaleInitializer()

    def get(): ScaleInitializer = instance
  }

  class Rotation2dInitializer extends ChannelInitializer[FloatChannel] {

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

  object Rotation2dInitializer {
    private lazy val instance: Rotation2dInitializer = new Rotation2dInitializer()

    def get(): Rotation2dInitializer = instance
  }

  class Rotation3dInitializer extends ChannelInitializer[FloatChannel] {

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

  object Rotation3dInitializer {
    private lazy val instance: Rotation3dInitializer = new Rotation3dInitializer()

    def get(): Rotation3dInitializer = instance
  }

  // Channels
  /** Channels of common use like position, life, color, etc... */
  val Life:     ChannelDescriptor = new ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 3)
  val Position: ChannelDescriptor =
    new ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 3) // gl units
  val PreviousPosition: ChannelDescriptor =
    new ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 3)
  val Color:         ChannelDescriptor = new ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 4)
  val TextureRegion: ChannelDescriptor =
    new ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 6)
  val Rotation2D:    ChannelDescriptor = new ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 2)
  val Rotation3D:    ChannelDescriptor = new ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 4)
  val Scale:         ChannelDescriptor = new ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 1)
  val ModelInstance: ChannelDescriptor =
    new ChannelDescriptor(newGlobalId(), (size: Int) => new Array[sge.graphics.g3d.ModelInstance](size), 1)
  val ParticleController: ChannelDescriptor =
    new ChannelDescriptor(
      newGlobalId(),
      (size: Int) => new Array[sge.graphics.g3d.particles.ParticleController](size),
      1
    )
  val Acceleration: ChannelDescriptor =
    new ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 3) // gl units/s2
  val AngularVelocity2D: ChannelDescriptor =
    new ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 1)
  val AngularVelocity3D: ChannelDescriptor =
    new ChannelDescriptor(newGlobalId(), (size: Int) => new Array[Float](size), 3)
  val Interpolation:  ChannelDescriptor = new ChannelDescriptor(-1, (size: Int) => new Array[Float](size), 2)
  val Interpolation4: ChannelDescriptor = new ChannelDescriptor(-1, (size: Int) => new Array[Float](size), 4)
  val Interpolation6: ChannelDescriptor = new ChannelDescriptor(-1, (size: Int) => new Array[Float](size), 6)

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
