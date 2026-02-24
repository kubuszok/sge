/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/TextureAtlasLoader.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package assets
package loaders

import sge.files.FileHandle
import sge.graphics.Texture
import sge.graphics.g2d.TextureAtlas
import sge.graphics.g2d.TextureAtlas.TextureAtlasData
import sge.utils.Nullable
import scala.collection.mutable.ArrayBuffer

/** {@link AssetLoader} to load {@link TextureAtlas} instances. Passing a {@link TextureAtlasParameter} to {@link AssetManager#load(String, Class, AssetLoaderParameters)} allows to specify whether the
  * atlas regions should be flipped on the y-axis or not.
  * @author
  *   mzechner (original implementation)
  */
class TextureAtlasLoader(resolver: FileHandleResolver)(using sge: Sge) extends SynchronousAssetLoader[TextureAtlas, TextureAtlasLoader.TextureAtlasParameter](resolver) {

  private var data: Nullable[TextureAtlasData] = Nullable.empty

  override def load(assetManager: AssetManager, fileName: String, file: FileHandle, parameter: TextureAtlasLoader.TextureAtlasParameter): TextureAtlas = {
    data.foreach { d =>
      for (page <- d.getPages())
        page.textureFile.foreach { tf =>
          val texture = assetManager.get(tf.path().replaceAll("\\\\", "/"), classOf[Texture])
          page.texture = Nullable(texture)
        }
    }

    val atlas = new TextureAtlas(data.orNull)
    data = Nullable.empty
    atlas
  }

  override def getDependencies(fileName: String, file: FileHandle, parameter: TextureAtlasLoader.TextureAtlasParameter): ArrayBuffer[AssetDescriptor[?]] = {
    val atlasFile = file
    val imgDir    = atlasFile.parent()

    if (parameter != null)
      data = Nullable(new TextureAtlasData(atlasFile, imgDir, parameter.flip))
    else
      data = Nullable(new TextureAtlasData(atlasFile, imgDir, false))

    val dependencies = ArrayBuffer.empty[AssetDescriptor[?]]
    data.foreach { d =>
      for (page <- d.getPages()) {
        val params = new TextureLoader.TextureParameter()
        params.format = Nullable(page.format)
        params.genMipMaps = page.useMipMaps
        params.minFilter = page.minFilter
        params.magFilter = page.magFilter
        page.textureFile.foreach { tf =>
          dependencies += new AssetDescriptor[Texture](tf, classOf[Texture], params)
        }
      }
    }
    dependencies
  }
}

object TextureAtlasLoader {
  class TextureAtlasParameter(var flip: Boolean = false) extends AssetLoaderParameters[TextureAtlas]
}
