/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/TextureLoader.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package assets
package loaders

import sge.files.FileHandle
import sge.graphics.Pixmap.Format
import sge.graphics.Texture
import sge.graphics.Texture.TextureFilter
import sge.graphics.Texture.TextureWrap
import sge.graphics.TextureData
import sge.utils.{ Nullable, SgeError }
import scala.collection.mutable.ArrayBuffer

/** {@link AssetLoader} for {@link Texture} instances. The pixel data is loaded asynchronously. The texture is then created on the rendering thread, synchronously. Passing a {@link TextureParameter}
  * to {@link AssetManager#load(String, Class, AssetLoaderParameters)} allows one to specify parameters as can be passed to the various Texture constructors, e.g. filtering, whether to generate
  * mipmaps and so on.
  * @author
  *   mzechner
  */
class TextureLoader(resolver: FileHandleResolver)(using sge: Sge) extends AsynchronousAssetLoader[Texture, TextureLoader.TextureParameter](resolver) {

  private val info = new TextureLoader.TextureLoaderInfo()

  override def loadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameter: TextureLoader.TextureParameter): Unit = {
    val param = Nullable(parameter)
    info.filename = fileName
    if (param.fold(true)(_.textureData.isEmpty)) {
      var format: Nullable[Format] = Nullable.empty
      var genMipMaps = false
      info.texture = Nullable.empty

      param.foreach { p =>
        format = p.format
        genMipMaps = p.genMipMaps
        info.texture = p.texture
      }

      info.data = TextureData.Factory.loadFromFile(file, format, genMipMaps)
    } else {
      param.foreach { p =>
        info.data = p.textureData.getOrElse(throw SgeError.InvalidInput("textureData is empty"))
        info.texture = p.texture
      }
    }
    if (!info.data.isPrepared) info.data.prepare()
  }

  override def loadSync(manager: AssetManager, fileName: String, file: FileHandle, parameter: TextureLoader.TextureParameter): Texture = {
    val texture = info.texture.fold {
      new Texture(info.data)
    } { existing =>
      existing.load(info.data)
      existing
    }
    Nullable(parameter).foreach { p =>
      texture.setFilter(p.minFilter, p.magFilter)
      texture.setWrap(p.wrapU, p.wrapV)
    }
    texture
  }

  override def getDependencies(fileName: String, file: FileHandle, parameter: TextureLoader.TextureParameter): ArrayBuffer[AssetDescriptor[?]] =
    ArrayBuffer.empty
}

object TextureLoader {
  class TextureLoaderInfo {
    var filename: String            = scala.compiletime.uninitialized
    var data:     TextureData       = scala.compiletime.uninitialized
    var texture:  Nullable[Texture] = Nullable.empty
  }

  class TextureParameter extends AssetLoaderParameters[Texture] {

    /** the format of the final Texture. Uses the source images format if null * */
    var format: Nullable[Format] = Nullable.empty

    /** whether to generate mipmaps * */
    var genMipMaps: Boolean = false

    /** The texture to put the {@link TextureData} in, optional. * */
    var texture: Nullable[Texture] = Nullable.empty

    /** TextureData for textures created on the fly, optional. When set, all format and genMipMaps are ignored */
    var textureData: Nullable[TextureData] = Nullable.empty
    var minFilter:   TextureFilter         = TextureFilter.Nearest
    var magFilter:   TextureFilter         = TextureFilter.Nearest
    var wrapU:       TextureWrap           = TextureWrap.ClampToEdge
    var wrapV:       TextureWrap           = TextureWrap.ClampToEdge
  }
}
