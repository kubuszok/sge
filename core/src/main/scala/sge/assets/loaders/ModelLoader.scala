/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/ModelLoader.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package assets
package loaders

import scala.util.boundary
import scala.util.boundary.break

import sge.files.FileHandle
import sge.graphics.Texture
import sge.graphics.Texture.{ TextureFilter, TextureWrap }
import sge.graphics.g3d.Model
import sge.graphics.g3d.model.data.{ ModelData, ModelMaterial, ModelTexture }
import sge.graphics.g3d.utils.TextureProvider
import sge.utils.{ DynamicArray, Nullable }

abstract class ModelLoader[P <: ModelLoader.ModelParameters](resolver: FileHandleResolver)(using sge: Sge) extends AsynchronousAssetLoader[Model, P](resolver) {

  protected var items:             DynamicArray[(String, ModelData)] = DynamicArray[(String, ModelData)]()
  protected var defaultParameters: ModelLoader.ModelParameters       = new ModelLoader.ModelParameters()

  /** Directly load the raw model data on the calling thread. */
  def loadModelData(fileHandle: FileHandle, parameters: P): Nullable[ModelData]

  /** Directly load the raw model data on the calling thread. */
  def loadModelData(fileHandle: FileHandle): Nullable[ModelData] =
    loadModelData(fileHandle, null.asInstanceOf[P])

  /** Directly load the model on the calling thread. The model with not be managed by an {@link AssetManager}. */
  def loadModel(fileHandle: FileHandle, textureProvider: TextureProvider, parameters: P): Nullable[Model] = {
    val data = loadModelData(fileHandle, parameters)
    data.fold(Nullable.empty[Model])(d => Nullable(new Model(d, textureProvider)))
  }

  /** Directly load the model on the calling thread. The model with not be managed by an {@link AssetManager}. */
  def loadModel(fileHandle: FileHandle, parameters: P): Nullable[Model] =
    loadModel(fileHandle, new TextureProvider.FileTextureProvider(), parameters)

  /** Directly load the model on the calling thread. The model with not be managed by an {@link AssetManager}. */
  def loadModel(fileHandle: FileHandle, textureProvider: TextureProvider): Nullable[Model] =
    loadModel(fileHandle, textureProvider, null.asInstanceOf[P])

  /** Directly load the model on the calling thread. The model with not be managed by an {@link AssetManager}. */
  def loadModel(fileHandle: FileHandle): Nullable[Model] =
    loadModel(fileHandle, new TextureProvider.FileTextureProvider(), null.asInstanceOf[P])

  override def getDependencies(fileName: String, file: FileHandle, parameters: P): DynamicArray[AssetDescriptor[?]] = {
    val deps = DynamicArray[AssetDescriptor[?]]()
    val data = loadModelData(file, parameters)
    if (data.isEmpty) deps
    else {
      data.foreach { d =>
        val item = (fileName, d)
        items.synchronized {
          items.add(item)
        }
      }

      val textureParameter: TextureLoader.TextureParameter =
        if (Nullable(parameters).isDefined) Nullable(parameters).fold(defaultParameters.textureParameter)(_.textureParameter)
        else defaultParameters.textureParameter

      data.foreach { d =>
        for (modelMaterial <- d.materials)
          Nullable(modelMaterial.textures).foreach { textures =>
            for (modelTexture <- textures)
              deps.add(AssetDescriptor[Texture](modelTexture.fileName, classOf[Texture], Nullable(textureParameter)))
          }
      }
      deps
    }
  }

  override def loadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameters: P): Unit = {}

  override def loadSync(manager: AssetManager, fileName: String, file: FileHandle, parameters: P): Model = boundary {
    var data: Nullable[ModelData] = Nullable.empty
    items.synchronized {
      var i = 0
      while (i < items.size)
        if (items(i)._1.equals(fileName)) {
          data = Nullable(items(i)._2)
          items.removeIndex(i)
        } else {
          i += 1
        }
    }
    if (data.isEmpty) break(null.asInstanceOf[Model])
    data.fold(null.asInstanceOf[Model]) { d =>
      val result = new Model(d, new TextureProvider.AssetTextureProvider(manager))
      // need to remove the textures from the managed disposables, or else ref counting
      // doesn't work!
      val filtered = result.getManagedDisposables
      var idx      = 0
      while (idx < filtered.size)
        filtered(idx) match {
          case _: Texture => filtered.removeIndex(idx)
          case _ => idx += 1
        }
      result
    }
  }
}

object ModelLoader {
  class ModelParameters extends AssetLoaderParameters[Model] {
    var textureParameter: TextureLoader.TextureParameter = {
      val p = new TextureLoader.TextureParameter()
      p.minFilter = TextureFilter.Linear
      p.magFilter = TextureFilter.Linear
      p.wrapU = TextureWrap.Repeat
      p.wrapV = TextureWrap.Repeat
      p
    }
  }
}
