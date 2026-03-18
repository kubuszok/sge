/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/influencers/SpawnInfluencer.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audit 2026-03-03):
 * - All public methods faithfully ported (init, allocateChannels, start, activateParticles, copy, save, load).
 * - write/read(Json) omitted (Json serialization not ported).
 * - channel.data[] renamed to channel.floatData() per SGE ParallelArray API.
 * - positionChannel/rotationChannel made public vars (were package-private in Java).
 * - Status: pass
 */
package sge
package graphics
package g3d
package particles
package influencers

import sge.assets.AssetManager
import sge.graphics.g3d.particles.ParallelArray.FloatChannel
import sge.graphics.g3d.particles.{ ParticleChannels, ResourceData }
import sge.graphics.g3d.particles.values.{ PointSpawnShapeValue, SpawnShapeValue }

/** It's an {@link Influencer} which controls where the particles will be spawned.
  * @author
  *   Inferno
  */
class SpawnInfluencer extends Influencer {
  import ParticleControllerComponent.{ TMP_Q, TMP_V1 }

  var spawnShapeValue: SpawnShapeValue = PointSpawnShapeValue()
  var positionChannel: FloatChannel    = scala.compiletime.uninitialized
  var rotationChannel: FloatChannel    = scala.compiletime.uninitialized

  def this(spawnShapeValue: SpawnShapeValue) = {
    this()
    this.spawnShapeValue = spawnShapeValue
  }

  def this(source: SpawnInfluencer) = {
    this()
    spawnShapeValue = source.spawnShapeValue.copy()
  }

  override def init(): Unit =
    spawnShapeValue.init()

  override def allocateChannels(): Unit = {
    positionChannel = controller.particles.addChannel(ParticleChannels.Position)
    rotationChannel = controller.particles.addChannel(ParticleChannels.Rotation3D)
  }

  override def start(): Unit =
    spawnShapeValue.start()

  override def activateParticles(startIndex: Int, count: Int): Unit = {
    var i = startIndex * positionChannel.strideSize
    val c = i + count * positionChannel.strideSize
    while (i < c) {
      spawnShapeValue.spawn(TMP_V1, controller.emitter.percent)
      TMP_V1.mul(controller.transform)
      positionChannel.floatData(i + ParticleChannels.XOffset) = TMP_V1.x
      positionChannel.floatData(i + ParticleChannels.YOffset) = TMP_V1.y
      positionChannel.floatData(i + ParticleChannels.ZOffset) = TMP_V1.z
      i += positionChannel.strideSize
    }

    i = startIndex * rotationChannel.strideSize
    val c2 = i + count * rotationChannel.strideSize
    while (i < c2) {
      controller.transform.rotation(TMP_Q, true)
      rotationChannel.floatData(i + ParticleChannels.XOffset) = TMP_Q.x
      rotationChannel.floatData(i + ParticleChannels.YOffset) = TMP_Q.y
      rotationChannel.floatData(i + ParticleChannels.ZOffset) = TMP_Q.z
      rotationChannel.floatData(i + ParticleChannels.WOffset) = TMP_Q.w
      i += rotationChannel.strideSize
    }
  }

  override def copy(): SpawnInfluencer =
    SpawnInfluencer(this)

  override def save(manager: AssetManager, data: ResourceData[?]): Unit =
    spawnShapeValue.save(manager, data)

  override def load(manager: AssetManager, data: ResourceData[?]): Unit =
    spawnShapeValue.load(manager, data)
}
