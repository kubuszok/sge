/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/ParticleEffectLoader.java
 * Original authors: inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package particles

import _root_.sge.assets.{ AssetDescriptor, AssetLoaderParameters, AssetManager }
import _root_.sge.assets.loaders.{ AsynchronousAssetLoader, FileHandleResolver }
import _root_.sge.files.FileHandle
import _root_.sge.graphics.g3d.particles.batches.ParticleBatch
import _root_.sge.utils.DynamicArray
import _root_.sge.utils.Nullable

// NOTE: Full implementation requires a JSON serialization bridge (libGDX Json class).
// getDependencies and save are stubbed until a JSON library is integrated.
// See also: G3dModelLoader.scala for a similar pattern.

/** This class can save and load a {@link ParticleEffect}. It should be added as {@link AsynchronousAssetLoader} to the {@link AssetManager} so it will be able to load the effects. It's important to
  * note that the two classes {@link ParticleEffectLoadParameter} and {@link ParticleEffectSaveParameter} should be passed in whenever possible, because when present the batches settings will be
  * loaded automatically. When the load and save parameters are absent, once the effect will be created, one will have to set the required batches manually otherwise the {@link ParticleController}
  * instances contained inside the effect will not be able to render themselves.
  * @author
  *   inferno
  */
class ParticleEffectLoader(resolver: FileHandleResolver)(using sge: Sge) extends AsynchronousAssetLoader[ParticleEffect, ParticleEffectLoader.ParticleEffectLoadParameter](resolver) {

  import ParticleEffectLoader.*

  protected var items: DynamicArray[(String, ResourceData[ParticleEffect])] =
    DynamicArray[(String, ResourceData[ParticleEffect])]()

  override def loadAsync(
    manager:   AssetManager,
    fileName:  String,
    file:      FileHandle,
    parameter: ParticleEffectLoadParameter
  ): Unit = {
    // Nothing to do on async thread
  }

  override def getDependencies(
    fileName:  String,
    file:      FileHandle,
    parameter: ParticleEffectLoadParameter
  ): DynamicArray[AssetDescriptor[?]] = {
    // TODO: Requires Json.fromJson(classOf[ResourceData], file) to deserialize ResourceData
    // Once JSON bridge is available, this should:
    // 1. Deserialize ResourceData[ParticleEffect] from file
    // 2. Cache it in items synchronized list
    // 3. Return asset descriptors for all referenced assets
    val descriptors = DynamicArray[AssetDescriptor[?]]()
    descriptors
  }

  /** Saves the effect to the given file contained in the passed in parameter. */
  def save(effect: ParticleEffect, parameter: ParticleEffectSaveParameter): Unit = {
    val data = new ResourceData[ParticleEffect](effect)

    // effect assets
    effect.save(parameter.manager, data)

    // Batches configurations
    parameter.batches.foreach { batchList =>
      for (batch <- batchList) {
        var shouldSave = false
        for (controller <- effect.getControllers())
          if (controller.renderer.isCompatible(batch)) {
            shouldSave = true
          }
        if (shouldSave) batch.save(parameter.manager, data)
      }
    }

    // TODO: Requires Json library to serialize data to file
    // Once JSON bridge is available:
    // val json = new Json(parameter.jsonOutputType)
    // if (parameter.prettyPrint) parameter.file.writeString(json.prettyPrint(data), false)
    // else json.toJson(data, parameter.file)
  }

  override def loadSync(
    manager:   AssetManager,
    fileName:  String,
    file:      FileHandle,
    parameter: ParticleEffectLoadParameter
  ): ParticleEffect = {
    var effectData: Nullable[ResourceData[ParticleEffect]] = Nullable.empty
    items.synchronized {
      var i = 0
      while (i < items.size) {
        val entry = items(i)
        if (entry._1 == fileName) {
          effectData = Nullable(entry._2)
          items.removeIndex(i)
          i = items.size // break
        }
        i += 1
      }
    }

    effectData.fold {
      throw new RuntimeException("No ResourceData found for " + fileName)
    } { data =>
      data.resource.foreach { res =>
        res.load(manager, data)
      }

      Nullable(parameter).foreach { p =>
        p.batches.foreach { batchList =>
          for (batch <- batchList)
            batch.load(manager, data)
          data.resource.foreach(_.setBatch(batchList))
        }
      }

      data.resource.getOrElse(throw new RuntimeException("ResourceData has no resource for " + fileName))
    }
  }
}

object ParticleEffectLoader {

  class ParticleEffectLoadParameter(
    val batches: Nullable[DynamicArray[ParticleBatch[?]]]
  ) extends AssetLoaderParameters[ParticleEffect] {

    def this(batches: DynamicArray[ParticleBatch[?]]) =
      this(Nullable(batches))
  }

  class ParticleEffectSaveParameter(
    /** Required parameters */
    val file:    FileHandle,
    val manager: AssetManager,
    /** Optional parameters, but should be present to correctly load the settings */
    val batches:     Nullable[DynamicArray[ParticleBatch[?]]] = Nullable.empty,
    val prettyPrint: Boolean = false
  ) extends AssetLoaderParameters[ParticleEffect]
}
