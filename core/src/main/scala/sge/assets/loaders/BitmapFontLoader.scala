/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/BitmapFontLoader.java
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
import sge.graphics.Texture.TextureFilter
import sge.graphics.g2d.{ BitmapFont, BitmapFontData, TextureAtlas, TextureRegion }
import sge.utils.{ DynamicArray, Nullable, SgeError }
import scala.util.boundary

/** {@link AssetLoader} for {@link BitmapFont} instances. Loads the font description file (.fnt) asynchronously, loads the {@link Texture} containing the glyphs as a dependency. The
  * {@link BitmapFontParameter} allows you to set things like texture filters or whether to flip the glyphs vertically.
  * @author
  *   mzechner (original implementation)
  */
class BitmapFontLoader(resolver: FileHandleResolver)(using Sge) extends AsynchronousAssetLoader[BitmapFont, BitmapFontLoader.BitmapFontParameter](resolver) {

  private var data: Nullable[BitmapFontData] = Nullable.empty

  override def getDependencies(fileName: String, file: FileHandle, parameter: BitmapFontLoader.BitmapFontParameter): DynamicArray[AssetDescriptor[?]] = boundary {
    val param = Nullable(parameter)
    val deps  = DynamicArray[AssetDescriptor[?]]()
    param.foreach { p =>
      if (p.bitmapFontData.isDefined) {
        data = p.bitmapFontData
        boundary.break(deps)
      }
    }

    data = Nullable(new BitmapFontData(Nullable(file), param.fold(false)(_.flip)))
    if (param.fold(false)(_.atlasName.isDefined)) {
      param.foreach(_.atlasName.foreach { atlasName =>
        deps.add(new AssetDescriptor[TextureAtlas](atlasName, classOf[TextureAtlas]))
      })
    } else {
      data.foreach { d =>
        d.imagePaths.foreach { paths =>
          for (i <- 0 until paths.length) {
            val path     = paths(i)
            val resolved = resolve(path)

            val textureParams = new TextureLoader.TextureParameter()

            param.foreach { p =>
              textureParams.genMipMaps = p.genMipMaps
              textureParams.minFilter = p.minFilter
              textureParams.magFilter = p.magFilter
            }

            deps.add(new AssetDescriptor[Texture](resolved, classOf[Texture], textureParams))
          }
        }
      }
    }

    deps
  }

  override def loadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameter: BitmapFontLoader.BitmapFontParameter): Unit = {}

  override def loadSync(manager: AssetManager, fileName: String, file: FileHandle, parameter: BitmapFontLoader.BitmapFontParameter): BitmapFont = {
    val param = Nullable(parameter)
    if (param.fold(false)(_.atlasName.isDefined)) {
      val d         = data.getOrElse(throw SgeError.GraphicsError("BitmapFontData not loaded"))
      val atlasName = param.getOrElse(throw SgeError.GraphicsError("parameter required")).atlasName.getOrElse(throw SgeError.GraphicsError("atlasName required"))
      val atlas     = manager.get(atlasName, classOf[TextureAtlas])
      val paths     = d.imagePaths.getOrElse(throw SgeError.GraphicsError("BitmapFontData has no image paths"))
      val name      = file.sibling(paths(0)).nameWithoutExtension()
      val region    = atlas.findRegion(name)

      region.fold {
        throw SgeError.GraphicsError("Could not find font region " + name + " in atlas " + atlasName)
      } { r =>
        new BitmapFont(file, Nullable(r))
      }
    } else {
      val d        = data.getOrElse(throw SgeError.GraphicsError("BitmapFontData not loaded"))
      val imgPaths = d.imagePaths.getOrElse(throw SgeError.GraphicsError("BitmapFontData has no image paths"))
      val n        = imgPaths.length
      val regs     = DynamicArray[TextureRegion]()
      for (i <- 0 until n)
        regs.add(new TextureRegion(manager.get(imgPaths(i), classOf[Texture])))
      new BitmapFont(d, Nullable(regs), true)
    }
  }
}

object BitmapFontLoader {

  /** Parameter to be passed to {@link AssetManager#load(String, Class, AssetLoaderParameters)} if additional configuration is necessary for the {@link BitmapFont}.
    * @author
    *   mzechner (original implementation)
    */
  class BitmapFontParameter extends AssetLoaderParameters[BitmapFont] {

    /** Flips the font vertically if {@code true}. Defaults to {@code false}. * */
    var flip: Boolean = false

    /** Generates mipmaps for the font if {@code true}. Defaults to {@code false}. * */
    var genMipMaps: Boolean = false

    /** The {@link TextureFilter} to use when scaling down the {@link BitmapFont}. Defaults to {@link TextureFilter#Nearest}. */
    var minFilter: TextureFilter = TextureFilter.Nearest

    /** The {@link TextureFilter} to use when scaling up the {@link BitmapFont}. Defaults to {@link TextureFilter#Nearest}. */
    var magFilter: TextureFilter = TextureFilter.Nearest

    /** optional {@link BitmapFontData} to be used instead of loading the {@link Texture} directly. Use this if your font is embedded in a Skin.
      */
    var bitmapFontData: Nullable[BitmapFontData] = Nullable.empty

    /** The name of the {@link TextureAtlas} to load the {@link BitmapFont} itself from. Optional; if empty, will look for a separate image
      */
    var atlasName: Nullable[String] = Nullable.empty
  }
}
