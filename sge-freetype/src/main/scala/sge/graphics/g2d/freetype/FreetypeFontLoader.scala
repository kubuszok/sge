/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/freetype/FreetypeFontLoader.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Array<T> -> DynamicArray[T]; AssetDescriptor uses Class[T]
 *   Convention: Nullable; using Sge context parameter
 *   Idiom: split packages
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d
package freetype

import sge.Sge
import sge.assets.{ AssetDescriptor, AssetLoaderParameters, AssetManager }
import sge.assets.loaders.{ AsynchronousAssetLoader, FileHandleResolver }
import sge.files.FileHandle
import sge.graphics.g2d.BitmapFont
import sge.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import sge.utils.{ DynamicArray, Nullable }

/** Creates {@link BitmapFont} instances from FreeType font files. Requires a {@link FreeTypeFontLoaderParameter} to be passed to {@link AssetManager#load} which specifies the name of the TTF file as
  * well the parameters used to generate the BitmapFont (size, characters, etc.)
  */
class FreetypeFontLoader(resolver: FileHandleResolver)(using Sge) extends AsynchronousAssetLoader[BitmapFont, FreetypeFontLoader.FreeTypeFontLoaderParameter](resolver) {

  @scala.annotation.nowarn("msg=deprecated") // null check — parameter arrives from AssetManager which may pass null
  override def loadAsync(
    manager:   AssetManager,
    fileName:  String,
    file:      FileHandle,
    parameter: FreetypeFontLoader.FreeTypeFontLoaderParameter
  ): Unit =
    if (parameter == null)
      throw new RuntimeException("FreetypeFontParameter must be set in AssetManager#load to point at a TTF file!")

  @scala.annotation.nowarn("msg=deprecated") // null check — parameter arrives from AssetManager which may pass null
  override def loadSync(
    manager:   AssetManager,
    fileName:  String,
    file:      FileHandle,
    parameter: FreetypeFontLoader.FreeTypeFontLoaderParameter
  ): BitmapFont = {
    if (parameter == null)
      throw new RuntimeException("FreetypeFontParameter must be set in AssetManager#load to point at a TTF file!")
    val generatorOpt = manager.get(parameter.fontFileName + ".gen", classOf[FreeTypeFontGenerator])
    val generator    = generatorOpt.getOrElse(
      throw new RuntimeException(s"FreeTypeFontGenerator not found for: ${parameter.fontFileName}.gen")
    )
    generator.generateFont(parameter.fontParameters)
  }

  override def getDependencies(
    fileName:  String,
    file:      FileHandle,
    parameter: FreetypeFontLoader.FreeTypeFontLoaderParameter
  ): DynamicArray[AssetDescriptor[?]] = {
    val deps = DynamicArray[AssetDescriptor[?]]()
    deps.add(new AssetDescriptor[FreeTypeFontGenerator](parameter.fontFileName + ".gen", classOf[FreeTypeFontGenerator]))
    deps
  }
}

object FreetypeFontLoader {

  class FreeTypeFontLoaderParameter extends AssetLoaderParameters[BitmapFont] {

    /** The name of the TTF file to be used to load the font. */
    var fontFileName: String = ""

    /** The parameters used to generate the font, e.g. size, characters, etc. */
    var fontParameters: FreeTypeFontParameter = FreeTypeFontParameter()
  }
}
