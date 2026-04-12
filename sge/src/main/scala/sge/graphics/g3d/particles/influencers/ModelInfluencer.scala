/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/influencers/ModelInfluencer.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audit 2026-03-03):
 * - Inner classes Single, Random moved to companion object (Java static classes).
 * - Array<Model> replaced with DynamicArray[Model].
 * - ModelInstancePool inner class replaced with Pool.Default + lambda.
 * - Null check on loaded model removed (load does not throw if model is null).
 * - data[i] = null replaced with clearSlot(i) in Random.killParticles.
 * - write/read(Json) omitted (Json serialization not ported).
 * - All public methods faithfully ported.
 * - Status: pass
 */
package sge
package graphics
package g3d
package particles
package influencers

import sge.assets.AssetManager
import sge.graphics.g3d.{ Model, ModelInstance }
import sge.graphics.g3d.particles.ParallelArray.ObjectChannel
import sge.graphics.g3d.particles.{ ParticleChannels, ResourceData }
import sge.utils.{ DynamicArray, Nullable, Pool }

/** It's an {@link Influencer} which controls which {@link Model} will be assigned to the particles as {@link ModelInstance}.
  * @author
  *   Inferno
  */
abstract class ModelInfluencer extends Influencer {

  var models:       DynamicArray[Model]          = DynamicArray[Model](1)
  var modelChannel: ObjectChannel[ModelInstance] = scala.compiletime.uninitialized

  /** Transport field for JSON serialization: model asset filenames. Since JSON codecs don't have access to AssetManager, this field stores the filenames during serialization/deserialization. The
    * actual models are resolved via AssetManager post-load (or pre-save by the loader).
    */
  var _modelFilenames: Array[String] = Array.empty

  def this(models: Model*) = {
    this()
    for (m <- models) this.models.add(m)
  }

  def this(influencer: ModelInfluencer) = {
    this()
    this.models.addAll(influencer.models)
  }

  override def allocateChannels(): Unit =
    modelChannel = controller.particles.addChannel(ParticleChannels.ModelInstance)

  override def save(manager: AssetManager, resources: ResourceData[?]): Unit = {
    val data = resources.createSaveData()
    for (model <- models)
      data.saveAsset[Model](manager.assetFileName(model).getOrElse(""))
  }

  override def load(manager: AssetManager, resources: ResourceData[?]): Unit = {
    val data       = resources.saveData
    var descriptor = data.loadAsset()
    while (descriptor.isDefined) {
      descriptor.foreach { desc =>
        val model = manager(desc.fileName, desc.`type`).asInstanceOf[Model]
        models.add(model)
      }
      descriptor = data.loadAsset()
    }
  }
}

object ModelInfluencer {

  /** Assigns the first model of {@link ModelInfluencer#models} to the particles. */
  final class Single extends ModelInfluencer {

    def this(influencer: Single) = {
      this()
      this.models.addAll(influencer.models)
    }

    def this(models: Model*) = {
      this()
      for (m <- models) this.models.add(m)
    }

    override def init(): Unit = {
      val first = models(0)
      var i     = 0
      val c     = controller.emitter.maxParticleCount
      while (i < c) {
        modelChannel.objectData(i) = ModelInstance(first)
        i += 1
      }
    }

    override def copy(): Single =
      Single(this)
  }

  /** Assigns a random model of {@link ModelInfluencer#models} to the particles. */
  final class Random extends ModelInfluencer {
    private val pool: Pool[ModelInstance] = Pool.Default[ModelInstance](
      () => {
        val idx = sge.math.MathUtils.random(models.size - 1)
        ModelInstance(models(idx))
      },
      initialCapacity = 16,
      max = Int.MaxValue
    )

    def this(influencer: Random) = {
      this()
      this.models.addAll(influencer.models)
    }

    def this(models: Model*) = {
      this()
      for (m <- models) this.models.add(m)
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
        modelChannel.clearSlot(i)
        i += 1
      }
    }

    override def copy(): Random =
      Random(this)
  }
}
