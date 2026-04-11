/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/loaders/gltf/GLTFAssetLoader.java
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: InternalFileHandleResolver -> FileHandleResolver.Internal;
 *     ObjectMap -> HashMap; Array<AssetDescriptor> -> DynamicArray[AssetDescriptor[?]];
 *     null -> Nullable; removeValue(x, true) -> removeValueByRef(x)
 *   Convention: inner class ManagedTextureResolver preserved as private inner class
 *   Idiom: split packages; (using Sge) propagation; boundary/break not needed (no return)
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package loaders
package gltf

import scala.collection.mutable.{ ArrayBuffer, HashMap }
import sge.Sge
import sge.assets.{ AssetDescriptor, AssetLoaderParameters, AssetManager }
import sge.assets.loaders.{ AsynchronousAssetLoader, FileHandleResolver, TextureLoader }
import sge.files.FileHandle
import sge.graphics.{ Pixmap, Texture }
import sge.gltf.data.GLTF
import sge.gltf.data.texture.{ GLTFImage, GLTFSampler, GLTFTexture }
import sge.gltf.loaders.shared.{ GLTFLoaderBase, GLTFTypes, SceneAssetLoaderParameters }
import sge.gltf.loaders.shared.texture.{ ImageResolver, TextureResolver }
import sge.gltf.scene3d.scene.SceneAsset
import sge.utils.{ DynamicArray, Nullable }

class GLTFAssetLoader(resolver: FileHandleResolver)(using sge: Sge)
    extends AsynchronousAssetLoader[SceneAsset, SceneAssetLoaderParameters](resolver) {

  private class ManagedTextureResolver(private val glModel: GLTF) extends TextureResolver {

    private val textureDescriptorsSimple: HashMap[Int, AssetDescriptor[Texture]] = HashMap.empty
    private val textureDescriptorsMipMap: HashMap[Int, AssetDescriptor[Texture]] = HashMap.empty

    private val pixmaps:           HashMap[Int, Pixmap]                         = HashMap.empty
    private val textureParameters: HashMap[Int, TextureLoader.TextureParameter] = HashMap.empty

    override def loadTextures(
      glTextures:    Nullable[ArrayBuffer[GLTFTexture]],
      glSamplers:    Nullable[ArrayBuffer[GLTFSampler]],
      imageResolver: ImageResolver
    )(using Sge): Unit = {}

    def fetch(manager: AssetManager): Unit = {
      for ((key, desc) <- textureDescriptorsSimple) {
        texturesSimple.put(key, manager(desc))
      }
      for ((key, desc) <- textureDescriptorsMipMap) {
        texturesMipmap.put(key, manager(desc))
      }
    }

    def loadTextures(): Unit = {
      for ((index, params) <- textureParameters) {
        val glTexture = glTextures.get(index)
        val pixmap    = pixmaps(glTexture.source.get)
        val texture   = new Texture(pixmap, params.genMipMaps)
        texture.setFilter(params.minFilter, params.magFilter)
        texture.setWrap(params.wrapU, params.wrapV)
        if (params.genMipMaps) {
          texturesMipmap.put(index, texture)
        } else {
          texturesSimple.put(index, texture)
        }
      }
      for ((_, pixmap) <- pixmaps) {
        pixmap.close()
      }
    }

    def getDependencies(deps: DynamicArray[AssetDescriptor[?]]): Unit = {
      this.glTextures = glModel.textures
      this.glSamplers = glModel.samplers
      glTextures.foreach { textures =>
        var i = 0
        while (i < textures.size) {
          val glTexture = textures(i)

          val glImage          = glModel.images.get(glTexture.source.get)
          val imageFile        = GLTFAssetLoader.this.dataFileResolver.get.getImageFile(glImage)
          val textureParameter = new TextureLoader.TextureParameter()
          glTexture.sampler.fold {
            GLTFTypes.mapTextureSampler(textureParameter)
          } { samplerIdx =>
            val sampler = glSamplers.get(samplerIdx)
            if (GLTFTypes.isMipMapFilter(sampler)) {
              textureParameter.genMipMaps = true
            }
            GLTFTypes.mapTextureSampler(textureParameter, sampler)
          }
          if (imageFile.isEmpty) {
            var pixmap = pixmaps.get(glTexture.source.get)
            if (pixmap.isEmpty) {
              val loaded = GLTFAssetLoader.this.dataFileResolver.get.load(glImage)
              pixmaps.put(glTexture.source.get, loaded)
              pixmap = Some(loaded)
            }
            textureParameters.put(i, textureParameter)
          } else {
            val assetDescriptor = new AssetDescriptor[Texture](
              imageFile.get,
              classOf[Texture],
              textureParameter
            )
            deps.add(assetDescriptor)
            if (textureParameter.genMipMaps) {
              textureDescriptorsMipMap.put(glTexture.source.get, assetDescriptor)
            } else {
              textureDescriptorsSimple.put(glTexture.source.get, assetDescriptor)
            }
          }
          i += 1
        }
      }
    }
  }

  private var dataFileResolver: Nullable[SeparatedDataFileResolver] = Nullable.empty
  private var textureResolver:  Nullable[ManagedTextureResolver]    = Nullable.empty

  def this()(using sge: Sge) =
    this(new FileHandleResolver.Internal())

  override def loadAsync(
    manager:   AssetManager,
    fileName:  String,
    file:      FileHandle,
    parameter: SceneAssetLoaderParameters
  ): Unit = {
    textureResolver.foreach(_.fetch(manager))
  }

  override def loadSync(
    manager:   AssetManager,
    fileName:  String,
    file:      FileHandle,
    parameter: SceneAssetLoaderParameters
  ): SceneAsset = {
    val withData = Nullable(parameter).exists(_.withData)

    textureResolver.foreach(_.loadTextures())

    val loader     = new GLTFLoaderBase(textureResolver.map(identity[TextureResolver]))
    val sceneAsset = loader.load(dataFileResolver.get, withData)

    // Delegates texture disposal to AssetManager.
    val deps = manager.dependencies(fileName)
    deps.foreach { depNames =>
      depNames.foreach { depFileName =>
        val dep = manager.get[AnyRef](depFileName, classOf[AnyRef])
        dep.foreach {
          case texture: Texture =>
            sceneAsset.textures.foreach(_.removeValueByRef(texture))
          case _ =>
        }
      }
    }

    this.textureResolver = Nullable.empty
    this.dataFileResolver = Nullable.empty
    sceneAsset
  }

  override def getDependencies(
    fileName:  String,
    file:      FileHandle,
    parameter: SceneAssetLoaderParameters
  ): DynamicArray[AssetDescriptor[?]] = {
    val deps = DynamicArray[AssetDescriptor[?]]()

    val dfr = new SeparatedDataFileResolver()
    dfr.load(file)
    val glModel = dfr.getRoot

    dataFileResolver = Nullable(dfr)

    val mtr = new ManagedTextureResolver(glModel)
    textureResolver = Nullable(mtr)
    mtr.getDependencies(deps)

    deps
  }
}
