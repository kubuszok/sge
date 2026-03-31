/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/freetype/FreeTypeFontGeneratorLoader.java
 * Original authors: Daniel Holderbaum
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
import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
import sge.files.FileHandle
import sge.utils.DynamicArray

/** Makes {@link FreeTypeFontGenerator} manageable via {@link AssetManager}.
  *
  * Register with:
  * {{{
  * assetManager.setLoader(classOf[FreeTypeFontGenerator], new FreeTypeFontGeneratorLoader(InternalFileHandleResolver()))
  * }}}
  *
  * @author
  *   Daniel Holderbaum
  */
class FreeTypeFontGeneratorLoader(resolver: FileHandleResolver)(using Sge)
    extends SynchronousAssetLoader[FreeTypeFontGenerator, FreeTypeFontGeneratorLoader.FreeTypeFontGeneratorParameters](resolver) {

  override def load(
    assetManager: AssetManager,
    fileName:     String,
    file:         FileHandle,
    parameter:    FreeTypeFontGeneratorLoader.FreeTypeFontGeneratorParameters
  ): FreeTypeFontGenerator =
    if (file.extension == "gen")
      FreeTypeFontGenerator(file.sibling(file.nameWithoutExtension))
    else
      FreeTypeFontGenerator(file)

  override def getDependencies(
    fileName:  String,
    file:      FileHandle,
    parameter: FreeTypeFontGeneratorLoader.FreeTypeFontGeneratorParameters
  ): DynamicArray[AssetDescriptor[?]] = DynamicArray[AssetDescriptor[?]]()
}

object FreeTypeFontGeneratorLoader {
  class FreeTypeFontGeneratorParameters extends AssetLoaderParameters[FreeTypeFontGenerator]
}
