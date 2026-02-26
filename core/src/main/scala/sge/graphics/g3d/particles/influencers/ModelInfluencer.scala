/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/influencers/ModelInfluencer.java
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

import scala.collection.mutable.ArrayBuffer

import sge.assets.AssetManager
import sge.graphics.g3d.{ Model, ModelInstance }
import sge.graphics.g3d.particles.ParallelArray.ObjectChannel
import sge.graphics.g3d.particles.{ ParticleChannels, ResourceData }
import sge.graphics.g3d.particles.ResourceData.SaveData
import sge.utils.{ Nullable, Pool }

/** It's an {@link Influencer} which controls which {@link Model} will be assigned to the particles as {@link ModelInstance}.
  * @author
  *   Inferno
  */
abstract class ModelInfluencer extends Influencer {

  var models:       ArrayBuffer[Model]           = new ArrayBuffer[Model](1)
  var modelChannel: ObjectChannel[ModelInstance] = scala.compiletime.uninitialized

  def this(models: Model*) = {
    this()
    this.models ++= models
  }

  def this(influencer: ModelInfluencer) = {
    this()
    this.models ++= influencer.models
  }

  override def allocateChannels(): Unit =
    modelChannel = controller.particles.addChannel(ParticleChannels.ModelInstance)

  override def save(manager: AssetManager, resources: ResourceData[?]): Unit = {
    val data = resources.createSaveData()
    for (model <- models)
      data.saveAsset(manager.getAssetFileName(model), classOf[Model])
  }

  override def load(manager: AssetManager, resources: ResourceData[?]): Unit = {
    val data       = resources.getSaveData()
    var descriptor = data.loadAsset()
    while (descriptor.isDefined) {
      descriptor.foreach { desc =>
        val model = manager.get(desc.fileName, desc.`type`).asInstanceOf[Model]
        models += model
      }
      descriptor = data.loadAsset()
    }
  }
}

object ModelInfluencer {

  /** Assigns the first model of {@link ModelInfluencer#models} to the particles. */
  class Single extends ModelInfluencer {

    def this(influencer: Single) = {
      this()
      this.models ++= influencer.models
    }

    def this(models: Model*) = {
      this()
      this.models ++= models
    }

    override def init(): Unit = {
      val first = models(0)
      var i     = 0
      val c     = controller.emitter.maxParticleCount
      while (i < c) {
        modelChannel.objectData(i) = new ModelInstance(first)
        i += 1
      }
    }

    override def copy(): Single =
      new Single(this)
  }

  /** Assigns a random model of {@link ModelInfluencer#models} to the particles. */
  class Random extends ModelInfluencer {
    private val pool: Pool[ModelInstance] = new Pool.Default[ModelInstance](
      () => {
        val idx = sge.math.MathUtils.random(models.size - 1)
        new ModelInstance(models(idx))
      },
      initialCapacity = 16,
      max = Int.MaxValue
    )

    def this(influencer: Random) = {
      this()
      this.models ++= influencer.models
    }

    def this(models: Model*) = {
      this()
      this.models ++= models
    }

    override def init(): Unit =
      pool.clear()

    override def activateParticles(startIndex: Int, count: Int): Unit = {
      var i = startIndex
      val c = startIndex + count
      while (i < c) {
        modelChannel.objectData(i) = pool.obtain()
        i += 1
      }
    }

    override def killParticles(startIndex: Int, count: Int): Unit = {
      var i = startIndex
      val c = startIndex + count
      while (i < c) {
        pool.free(modelChannel.objectData(i))
        modelChannel.objectData(i) = null.asInstanceOf[ModelInstance]
        i += 1
      }
    }

    override def copy(): Random =
      new Random(this)
  }
}
