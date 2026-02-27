/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/CubemapLoader.java
 * Original authors: mzechner, Vincent Bousquet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package assets
package loaders

import sge.files.FileHandle
import sge.graphics.{ Cubemap, CubemapData }
import sge.graphics.Pixmap.Format
import sge.graphics.Texture.{ TextureFilter, TextureWrap }
import sge.graphics.glutils.KTXTextureData
import sge.utils.{ DynamicArray, Nullable, SgeError }

/** {@link AssetLoader} for {@link Cubemap} instances. The pixel data is loaded asynchronously. The texture is then created on the rendering thread, synchronously. Passing a {@link CubemapParameter}
  * to {@link AssetManager#load(String, Class, AssetLoaderParameters)} allows one to specify parameters as can be passed to the various Cubemap constructors, e.g. filtering and so on.
  * @author
  *   mzechner, Vincent Bousquet (original implementation)
  */
class CubemapLoader(resolver: FileHandleResolver)(using sge: Sge) extends AsynchronousAssetLoader[Cubemap, CubemapLoader.CubemapParameter](resolver) {

  private val info = new CubemapLoader.CubemapLoaderInfo()

  override def loadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameter: CubemapLoader.CubemapParameter): Unit = {
    val param = Nullable(parameter)
    info.filename = fileName
    if (param.fold(true)(_.cubemapData.isEmpty)) {
      var format: Nullable[Format] = Nullable.empty
      var genMipMaps = false
      info.cubemap = Nullable.empty

      param.foreach { p =>
        format = p.format
        info.cubemap = p.cubemap
      }

      if (fileName.contains(".ktx") || fileName.contains(".zktx")) {
        info.data = new KTXTextureData(file, genMipMaps)
      }
    } else {
      param.foreach { p =>
        info.data = p.cubemapData.getOrElse(throw SgeError.InvalidInput("cubemapData is empty"))
        info.cubemap = p.cubemap
      }
    }
    if (!info.data.isPrepared) info.data.prepare()
  }

  override def loadSync(manager: AssetManager, fileName: String, file: FileHandle, parameter: CubemapLoader.CubemapParameter): Cubemap = {
    val cubemapResult: Cubemap = info.cubemap.fold {
      new Cubemap(info.data)
    } { existing =>
      existing.load(info.data)
      existing
    }
    Nullable(parameter).foreach { p =>
      cubemapResult.setFilter(p.minFilter, p.magFilter)
      cubemapResult.setWrap(p.wrapU, p.wrapV)
    }
    cubemapResult
  }

  override def getDependencies(fileName: String, file: FileHandle, parameter: CubemapLoader.CubemapParameter): DynamicArray[AssetDescriptor[?]] =
    DynamicArray[AssetDescriptor[?]]()
}

object CubemapLoader {
  class CubemapLoaderInfo {
    var filename: String            = scala.compiletime.uninitialized
    var data:     CubemapData       = scala.compiletime.uninitialized
    var cubemap:  Nullable[Cubemap] = Nullable.empty
  }

  class CubemapParameter extends AssetLoaderParameters[Cubemap] {

    /** the format of the final Texture. Uses the source images format if null * */
    var format: Nullable[Format] = Nullable.empty

    /** The texture to put the {@link TextureData} in, optional. * */
    var cubemap: Nullable[Cubemap] = Nullable.empty

    /** CubemapData for textures created on the fly, optional. When set, all format and genMipMaps are ignored */
    var cubemapData: Nullable[CubemapData] = Nullable.empty
    var minFilter:   TextureFilter         = TextureFilter.Nearest
    var magFilter:   TextureFilter         = TextureFilter.Nearest
    var wrapU:       TextureWrap           = TextureWrap.ClampToEdge
    var wrapV:       TextureWrap           = TextureWrap.ClampToEdge
  }
}
