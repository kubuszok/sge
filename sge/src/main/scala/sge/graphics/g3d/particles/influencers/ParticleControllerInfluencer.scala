/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/influencers/ParticleControllerInfluencer.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audit 2026-03-03):
 * - Inner classes Single, Random moved to companion object (Java static classes).
 * - Array<ParticleController> replaced with DynamicArray[ParticleController].
 * - ParticleControllerPool inner class replaced with Pool.Default + lambda.
 * - Random.dispose() renamed to close() per SGE convention.
 * - Null checks replaced with Nullable pattern; data[i] = null replaced with clearSlot(i).
 * - save/load: fully implemented. save uses AssetManager.getAll[ParticleEffect].
 * - load: uses descriptor.fold pattern instead of while-null loop.
 * - Random.init: uses pool.obtain() instead of pool.newObject() (Pool.Default behavior).
 * - Fixes (2026-03-06): save stub completed — AssetManager.getAll now available
 * - Status: pass
 */
package sge
package graphics
package g3d
package particles
package influencers

import sge.assets.AssetManager
import sge.graphics.g3d.particles.ParallelArray.ObjectChannel
import sge.graphics.g3d.particles.ParticleChannels
import sge.graphics.g3d.particles.ParticleController
import sge.graphics.g3d.particles.ResourceData
import sge.utils.{ DynamicArray, Nullable, Pool }

import scala.language.implicitConversions

/** It's an {@link Influencer} which controls which {@link ParticleController} will be assigned to a particle.
  * @author
  *   Inferno
  */
abstract class ParticleControllerInfluencer extends Influencer {

  var templates:                           DynamicArray[ParticleController]  = scala.compiletime.uninitialized
  protected var particleControllerChannel: ObjectChannel[ParticleController] = scala.compiletime.uninitialized

  this.templates = DynamicArray[ParticleController](1)

  def this(templateControllers: ParticleController*) = {
    this()
    this.templates = DynamicArray[ParticleController](templateControllers.length)
    for (tmpl <- templateControllers)
      this.templates.add(tmpl)
  }

  def this(influencer: ParticleControllerInfluencer) = {
    this()
    this.templates = DynamicArray[ParticleController](influencer.templates.size)
    for (tmpl <- influencer.templates)
      this.templates.add(tmpl)
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
    if (Nullable(controller).isDefined) {
      var i = 0
      while (i < controller.particles.size) {
        val ctrl = particleControllerChannel.objectData(i)
        if (Nullable(ctrl).isDefined) {
          ctrl.dispose()
          particleControllerChannel.clearSlot(i)
        }
        i += 1
      }
    }

  override def save(manager: AssetManager, resources: ResourceData[?]): Unit = {
    val data    = resources.createSaveData()
    val effects = manager.getAll[ParticleEffect](DynamicArray[ParticleEffect]())

    val controllers = DynamicArray[ParticleController]()
    controllers.addAll(templates)
    val effectsIndices = DynamicArray[DynamicArray[Int]]()

    var i = 0
    while (i < effects.size && controllers.nonEmpty) {
      val effect            = effects(i)
      val effectControllers = effect.controllers
      val toRemove          = DynamicArray[ParticleController]()
      var indices: Nullable[DynamicArray[Int]] = Nullable.empty
      for (ctrl <- controllers) {
        val index = effectControllers.indexOf(ctrl)
        if (index > -1) {
          if (indices.isEmpty) {
            indices = Nullable(DynamicArray[Int]())
          }
          toRemove.add(ctrl)
          indices.foreach(_.add(index))
        }
      }
      for (r <- toRemove) controllers.removeValue(r)

      indices.foreach { idx =>
        data.saveAsset[ParticleEffect](manager.assetFileName(effect).getOrElse(""))
        effectsIndices.add(idx)
      }
      i += 1
    }
    data.save("indices", effectsIndices.asInstanceOf[AnyRef])
  }

  override def load(manager: AssetManager, resources: ResourceData[?]): Unit = {
    val data = resources.getSaveData()
    val effectsIndices: Nullable[DynamicArray[DynamicArray[Int]]] = data.load("indices")
    effectsIndices.foreach { indices =>
      val indicesIter = indices.iterator
      var keepLoading = true
      while (indicesIter.hasNext && keepLoading) {
        val effectIndices = indicesIter.next()
        val descriptor    = data.loadAsset()
        descriptor.fold {
          keepLoading = false
        } { desc =>
          val effect = manager[ParticleEffect](desc.fileName)
          if (Nullable(effect).isEmpty) throw new RuntimeException("Template is null")
          val effectControllers = effect.controllers
          var j                 = 0
          while (j < effectIndices.size) {
            templates.add(effectControllers(effectIndices(j)))
            j += 1
          }
        }
      }
    }
  }
}

object ParticleControllerInfluencer {

  /** Assigns the first controller of {@link ParticleControllerInfluencer#templates} to the particles. */
  final class Single extends ParticleControllerInfluencer {

    def this(templateControllers: ParticleController*) = {
      this()
      this.templates = DynamicArray[ParticleController](templateControllers.length)
      for (tmpl <- templateControllers)
        this.templates.add(tmpl)
    }

    def this(particleControllerSingle: Single) = {
      this()
      this.templates = DynamicArray[ParticleController](particleControllerSingle.templates.size)
      for (tmpl <- particleControllerSingle.templates)
        this.templates.add(tmpl)
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
      Single(this)
  }

  /** Assigns a random controller of {@link ParticleControllerInfluencer#templates} to the particles. */
  final class Random extends ParticleControllerInfluencer {

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
      this.templates = DynamicArray[ParticleController](templateControllers.length)
      for (tmpl <- templateControllers)
        this.templates.add(tmpl)
      pool = createPool()
    }

    def this(particleControllerRandom: Random) = {
      this()
      this.templates = DynamicArray[ParticleController](particleControllerRandom.templates.size)
      for (tmpl <- particleControllerRandom.templates)
        this.templates.add(tmpl)
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
        particleControllerChannel.clearSlot(i)
        i += 1
      }
    }

    override def copy(): Random =
      Random(this)
  }
}
