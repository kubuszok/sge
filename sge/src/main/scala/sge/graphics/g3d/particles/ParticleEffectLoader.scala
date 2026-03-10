/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/ParticleEffectLoader.java
 * Original authors: inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - getDependencies: parses JSON AST to extract asset dependencies and caches ResourceData
 * - loadSync: retrieves cached ResourceData, loads resource and batches
 * - save: serialization blocked (needs full particle system JSON write support)
 * - Constructor requires (using Sge) context parameter
 * - items: DynamicArray[(String, ResourceData)] instead of Array<ObjectMap.Entry>
 * - find() private method omitted (used ClassReflection, not called internally)
 * - ParticleEffectSaveParameter: jsonOutputType field omitted (no JsonWriter.OutputType)
 * - ParticleEffectLoadParameter.batches: Nullable[DynamicArray] instead of Array|null
 * - ParticleEffectSaveParameter.batches: Nullable[DynamicArray] instead of Array|null
 */
package sge
package graphics
package g3d
package particles

import _root_.sge.assets.{ AssetDescriptor, AssetLoaderParameters, AssetManager }
import _root_.sge.assets.loaders.{ AsynchronousAssetLoader, FileHandleResolver }
import _root_.sge.files.FileHandle
import _root_.sge.graphics.g3d.particles.batches.ParticleBatch
import _root_.sge.utils.{ DynamicArray, Json, Nullable, readJson }

import hearth.kindlings.jsoniterjson.codec.JsonCodec.given

/** This class can save and load a {@link ParticleEffect}. It should be added as {@link AsynchronousAssetLoader} to the {@link AssetManager} so it will be able to load the effects. It's important to
  * note that the two classes {@link ParticleEffectLoadParameter} and {@link ParticleEffectSaveParameter} should be passed in whenever possible, because when present the batches settings will be
  * loaded automatically. When the load and save parameters are absent, once the effect will be created, one will have to set the required batches manually otherwise the {@link ParticleController}
  * instances contained inside the effect will not be able to render themselves.
  * @author
  *   inferno
  */
class ParticleEffectLoader(resolver: FileHandleResolver)(using Sge) extends AsynchronousAssetLoader[ParticleEffect, ParticleEffectLoader.ParticleEffectLoadParameter](resolver) {

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
    val jsonAst = file.readJson[Json]
    val data    = ResourceData.fromJson[ParticleEffect](jsonAst)
    var assets: DynamicArray[ResourceData.AssetData[?]] = DynamicArray[ResourceData.AssetData[?]]()
    items.synchronized {
      items.add((fileName, data))
      assets = data.getAssets()
    }

    val descriptors = DynamicArray[AssetDescriptor[?]]()
    for (assetData <- assets) {
      // If the asset doesn't exist try to load it from loading effect directory
      if (!resolve(assetData.filename).exists()) {
        assetData.filename = file.parent().child(Sge().files.internal(assetData.filename).name()).path()
      }

      if (assetData.`type` == classOf[ParticleEffect]) {
        descriptors.add(
          AssetDescriptor[ParticleEffect](
            assetData.filename,
            classOf[ParticleEffect],
            Nullable(parameter)
          )
        )
      } else {
        descriptors.add(new AssetDescriptor(assetData.filename, assetData.`type`))
      }
    }

    descriptors
  }

  /** Saves the effect to the given file contained in the passed in parameter. */
  def save(effect: ParticleEffect, parameter: ParticleEffectSaveParameter): Unit = {
    val data = ResourceData[ParticleEffect](effect)

    // effect assets
    effect.save(parameter.manager, data)

    // Batches configurations
    parameter.batches.foreach { batchList =>
      for (batch <- batchList) {
        var shouldSave = false
        for (controller <- effect.controllers)
          if (controller.renderer.isCompatible(batch)) {
            shouldSave = true
          }
        if (shouldSave) batch.save(parameter.manager, data)
      }
    }

    // Blocked: needs Json serialization framework (not yet ported)
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
      scala.util.boundary {
        var i = 0
        while (i < items.size) {
          val entry = items(i)
          if (entry._1 == fileName) {
            effectData = Nullable(entry._2)
            items.removeIndex(i)
            scala.util.boundary.break(())
          }
          i += 1
        }
      }
    }

    val data = effectData.getOrElse(throw new RuntimeException("No ResourceData found for " + fileName))

    data.resource.foreach { res =>
      res.load(manager, data)
    }

    Nullable(parameter).foreach { p =>
      p.batches.foreach { batchList =>
        for (batch <- batchList)
          batch.load(manager, data)
      }
      // setBatch is called with batches (matching Java: effectData.resource.setBatch(parameter.batches))
      p.batches.foreach { batchList =>
        data.resource.foreach(_.setBatch(batchList))
      }
    }

    data.resource.getOrElse(throw new RuntimeException("ResourceData has no resource for " + fileName))
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
