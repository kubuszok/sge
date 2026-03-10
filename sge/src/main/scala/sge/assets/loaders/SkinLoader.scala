/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/assets/loaders/SkinLoader.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   TODOs: test: SkinLoader getDependencies resolves atlas; loadSync applies resources (requires GL context)
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package assets
package loaders

import sge.files.FileHandle
import sge.graphics.g2d.TextureAtlas
import sge.scenes.scene2d.ui.Skin
import sge.utils.{ DynamicArray, Nullable }

import scala.collection.mutable

/** {@link AssetLoader} for {@link Skin} instances. All {@link Texture} and {@link BitmapFont} instances will be loaded as dependencies. Passing a {@link SkinParameter} allows the exact name of the
  * texture associated with the skin to be specified. Otherwise the skin texture is looked up just as with a call to {@link Skin#Skin(com.badlogic.gdx.files.FileHandle)}. A {@link SkinParameter} also
  * allows named resources to be set that will be added to the skin before loading the json file, meaning that they can be referenced from inside the json file itself. This is useful for dynamic
  * resources such as a BitmapFont generated through FreeTypeFontGenerator.
  * @author
  *   Nathan Sweet
  */
class SkinLoader(resolver: FileHandleResolver)(using Sge) extends AsynchronousAssetLoader[Skin, SkinLoader.SkinParameter](resolver) {

  override def getDependencies(
    fileName:  String,
    file:      FileHandle,
    parameter: SkinLoader.SkinParameter
  ): DynamicArray[AssetDescriptor[?]] = {
    val deps  = DynamicArray[AssetDescriptor[?]]()
    val param = Nullable(parameter)
    if (param.forall(_.textureAtlasPath.isEmpty))
      deps.add(AssetDescriptor[TextureAtlas](file.pathWithoutExtension() + ".atlas"))
    else
      param.foreach(_.textureAtlasPath.foreach { path =>
        deps.add(AssetDescriptor[TextureAtlas](path))
      })
    deps
  }

  override def loadAsync(
    manager:   AssetManager,
    fileName:  String,
    file:      FileHandle,
    parameter: SkinLoader.SkinParameter
  ): Unit = {}

  override def loadSync(
    manager:   AssetManager,
    fileName:  String,
    file:      FileHandle,
    parameter: SkinLoader.SkinParameter
  ): Skin = {
    val param            = Nullable(parameter)
    var textureAtlasPath = file.pathWithoutExtension() + ".atlas"
    var resources: Nullable[mutable.Map[String, Any]] = Nullable.empty
    param.foreach { p =>
      p.textureAtlasPath.foreach { path =>
        textureAtlasPath = path
      }
      resources = p.resources
    }
    val atlas = manager[TextureAtlas](textureAtlasPath)
    val skin  = newSkin(atlas)
    resources.foreach { res =>
      for ((key, value) <- res)
        skin.add(key, value)
    }
    skin.load(file)
    skin
  }

  /** Override to allow subclasses of Skin to be loaded or the skin instance to be configured.
    * @param atlas
    *   The TextureAtlas that the skin will use.
    * @return
    *   A new Skin (or subclass of Skin) instance based on the provided TextureAtlas.
    */
  protected def newSkin(atlas: TextureAtlas): Skin =
    Skin(atlas)
}

object SkinLoader {

  class SkinParameter(
    val textureAtlasPath: Nullable[String] = Nullable.empty,
    val resources:        Nullable[mutable.Map[String, Any]] = Nullable.empty
  ) extends AssetLoaderParameters[Skin] {}
}
