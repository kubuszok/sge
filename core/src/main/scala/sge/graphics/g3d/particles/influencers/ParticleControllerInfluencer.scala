/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/influencers/ParticleControllerInfluencer.java
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
import sge.graphics.g3d.particles.ParallelArray.ObjectChannel
import sge.graphics.g3d.particles.ParticleChannels
import sge.graphics.g3d.particles.ParticleController
import sge.graphics.g3d.particles.ResourceData
import sge.utils.{ DynamicArray, Nullable, Pool }

/** It's an {@link Influencer} which controls which {@link ParticleController} will be assigned to a particle.
  * @author
  *   Inferno
  */
abstract class ParticleControllerInfluencer extends Influencer {

  var templates:                           ArrayBuffer[ParticleController]   = scala.compiletime.uninitialized
  protected var particleControllerChannel: ObjectChannel[ParticleController] = scala.compiletime.uninitialized

  this.templates = new ArrayBuffer[ParticleController](1)

  def this(templateControllers: ParticleController*) = {
    this()
    this.templates = new ArrayBuffer[ParticleController](templateControllers.length)
    for (tmpl <- templateControllers)
      this.templates += tmpl
  }

  def this(influencer: ParticleControllerInfluencer) = {
    this()
    this.templates = new ArrayBuffer[ParticleController](influencer.templates.size)
    for (tmpl <- influencer.templates)
      this.templates += tmpl
  }

  override def allocateChannels(): Unit =
    particleControllerChannel = controller.particles.addChannel(ParticleChannels.ParticleController)

  override def end(): Unit = {
    var i = 0
    while (i < controller.particles.size) {
      particleControllerChannel.objectData(i).end()
      i += 1
    }
  }

  override def close(): Unit =
    if (controller != null) {
      var i = 0
      while (i < controller.particles.size) {
        val ctrl = particleControllerChannel.objectData(i)
        if (ctrl != null) {
          ctrl.dispose()
          particleControllerChannel.objectData(i) = null.asInstanceOf[ParticleController]
        }
        i += 1
      }
    }

  override def save(manager: AssetManager, resources: ResourceData[?]): Unit = {
    val data    = resources.createSaveData()
    val effects = new ArrayBuffer[ParticleEffect]()
    // NOTE: AssetManager.getAll not yet available; save is a stub for now

    val controllers = new ArrayBuffer[ParticleController]()
    controllers ++= templates
    val effectsIndices = new ArrayBuffer[DynamicArray[Int]]()

    var i = 0
    while (i < effects.size && controllers.nonEmpty) {
      val effect            = effects(i)
      val effectControllers = effect.getControllers()
      val toRemove          = new ArrayBuffer[ParticleController]()
      var indices: Nullable[DynamicArray[Int]] = Nullable.empty
      for (ctrl <- controllers) {
        val index = effectControllers.indexOf(ctrl)
        if (index > -1) {
          if (indices.isEmpty) {
            indices = Nullable(DynamicArray[Int]())
          }
          toRemove += ctrl
          indices.foreach(_.add(index))
        }
      }
      controllers --= toRemove

      indices.foreach { idx =>
        data.saveAsset(manager.getAssetFileName(effect), classOf[ParticleEffect])
        effectsIndices += idx
      }
      i += 1
    }
    data.save("indices", effectsIndices.asInstanceOf[AnyRef])
  }

  override def load(manager: AssetManager, resources: ResourceData[?]): Unit = {
    val data = resources.getSaveData()
    val effectsIndices: Nullable[ArrayBuffer[DynamicArray[Int]]] = data.load("indices")
    effectsIndices.foreach { indices =>
      val indicesIter = indices.iterator
      var keepLoading = true
      while (indicesIter.hasNext && keepLoading) {
        val effectIndices = indicesIter.next()
        val descriptor    = data.loadAsset()
        descriptor.fold {
          keepLoading = false
        } { desc =>
          val effect = manager.get(desc.fileName, classOf[ParticleEffect])
          if (effect == null) throw new RuntimeException("Template is null")
          val effectControllers = effect.getControllers()
          var j                 = 0
          while (j < effectIndices.size) {
            templates += effectControllers(effectIndices(j))
            j += 1
          }
        }
      }
    }
  }
}

object ParticleControllerInfluencer {

  /** Assigns the first controller of {@link ParticleControllerInfluencer#templates} to the particles. */
  class Single extends ParticleControllerInfluencer {

    def this(templateControllers: ParticleController*) = {
      this()
      this.templates = new ArrayBuffer[ParticleController](templateControllers.length)
      for (tmpl <- templateControllers)
        this.templates += tmpl
    }

    def this(particleControllerSingle: Single) = {
      this()
      this.templates = new ArrayBuffer[ParticleController](particleControllerSingle.templates.size)
      for (tmpl <- particleControllerSingle.templates)
        this.templates += tmpl
    }

    override def init(): Unit = {
      val first = templates(0)
      var i     = 0
      val c     = controller.particles.capacity
      while (i < c) {
        val copy = first.copy()
        copy.init()
        particleControllerChannel.objectData(i) = copy
        i += 1
      }
    }

    override def activateParticles(startIndex: Int, count: Int): Unit = {
      var i = startIndex
      val c = startIndex + count
      while (i < c) {
        particleControllerChannel.objectData(i).start()
        i += 1
      }
    }

    override def killParticles(startIndex: Int, count: Int): Unit = {
      var i = startIndex
      val c = startIndex + count
      while (i < c) {
        particleControllerChannel.objectData(i).end()
        i += 1
      }
    }

    override def copy(): Single =
      new Single(this)
  }

  /** Assigns a random controller of {@link ParticleControllerInfluencer#templates} to the particles. */
  class Random extends ParticleControllerInfluencer {

    private var pool: Pool[ParticleController] = createPool()

    private def createPool(): Pool[ParticleController] = {
      val self = this
      new Pool.Default[ParticleController](() => {
        val ctrl = self.templates(sge.math.MathUtils.random(self.templates.size - 1)).copy()
        ctrl.init()
        ctrl
      }) {
        override def clear(): Unit = {
          // Dispose every allocated instance because the templates may be changed
          var i    = 0
          val free = getFree
          while (i < free) {
            obtain().dispose()
            i += 1
          }
          super.clear()
        }
      }
    }

    def this(templateControllers: ParticleController*) = {
      this()
      this.templates = new ArrayBuffer[ParticleController](templateControllers.length)
      for (tmpl <- templateControllers)
        this.templates += tmpl
      pool = createPool()
    }

    def this(particleControllerRandom: Random) = {
      this()
      this.templates = new ArrayBuffer[ParticleController](particleControllerRandom.templates.size)
      for (tmpl <- particleControllerRandom.templates)
        this.templates += tmpl
      pool = createPool()
    }

    override def init(): Unit = {
      pool.clear()
      // Allocate the new instances
      var i = 0
      while (i < controller.emitter.maxParticleCount) {
        pool.free(pool.obtain())
        i += 1
      }
    }

    override def close(): Unit = {
      pool.clear()
      super.close()
    }

    override def activateParticles(startIndex: Int, count: Int): Unit = {
      var i = startIndex
      val c = startIndex + count
      while (i < c) {
        val ctrl = pool.obtain()
        ctrl.start()
        particleControllerChannel.objectData(i) = ctrl
        i += 1
      }
    }

    override def killParticles(startIndex: Int, count: Int): Unit = {
      var i = startIndex
      val c = startIndex + count
      while (i < c) {
        val ctrl = particleControllerChannel.objectData(i)
        ctrl.end()
        pool.free(ctrl)
        particleControllerChannel.objectData(i) = null.asInstanceOf[ParticleController]
        i += 1
      }
    }

    override def copy(): Random =
      new Random(this)
  }
}
