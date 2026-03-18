/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/TextureAtlasLoader.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   TODOs: test: TextureAtlasLoader getDependencies resolves page textures; load assembles atlas (requires GL context)
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package assets
package loaders

import sge.files.FileHandle
import sge.graphics.Texture
import sge.graphics.g2d.TextureAtlas
import sge.graphics.g2d.TextureAtlas.TextureAtlasData
import sge.utils.{ DynamicArray, Nullable, SgeError }

/** {@link AssetLoader} to load {@link TextureAtlas} instances. Passing a {@link TextureAtlasParameter} to {@link AssetManager#load(String, Class, AssetLoaderParameters)} allows to specify whether the
  * atlas regions should be flipped on the y-axis or not.
  * @author
  *   mzechner (original implementation)
  */
class TextureAtlasLoader(resolver: FileHandleResolver)(using Sge) extends SynchronousAssetLoader[TextureAtlas, TextureAtlasLoader.TextureAtlasParameter](resolver) {

  private var data: Nullable[TextureAtlasData] = Nullable.empty

  override def load(assetManager: AssetManager, fileName: String, file: FileHandle, parameter: TextureAtlasLoader.TextureAtlasParameter): TextureAtlas = {
    data.foreach { d =>
      for (page <- d.pages)
        page.textureFile.foreach { tf =>
          val texture = assetManager[Texture](tf.path.replaceAll("\\\\", "/"))
          page.texture = Nullable(texture)
        }
    }

    val atlas = TextureAtlas(data.getOrElse(throw SgeError.SerializationError("TextureAtlasData not loaded")))
    data = Nullable.empty
    atlas
  }

  override def getDependencies(fileName: String, file: FileHandle, parameter: TextureAtlasLoader.TextureAtlasParameter): DynamicArray[AssetDescriptor[?]] = {
    val atlasFile = file
    val imgDir    = atlasFile.parent()

    data = Nullable(TextureAtlasData(atlasFile, imgDir, Nullable(parameter).exists(_.flip)))

    val dependencies = DynamicArray[AssetDescriptor[?]]()
    data.foreach { d =>
      for (page <- d.pages) {
        val params = TextureLoader.TextureParameter()
        params.format = Nullable(page.format)
        params.genMipMaps = page.useMipMaps
        params.minFilter = page.minFilter
        params.magFilter = page.magFilter
        page.textureFile.foreach { tf =>
          dependencies.add(AssetDescriptor[Texture](tf.path, params))
        }
      }
    }
    dependencies
  }
}

object TextureAtlasLoader {
  class TextureAtlasParameter(var flip: Boolean = false) extends AssetLoaderParameters[TextureAtlas] {}
}
