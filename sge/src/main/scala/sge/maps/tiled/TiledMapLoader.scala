/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/TiledMapLoader.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audited 2026-03-03):
 *   - All methods match Java 1:1 (load, getDependencies, loadAsync, loadSync, usesAtlas)
 *   - Java null parameter checks → Nullable(param).isEmpty
 *   - usesAtlas uses boundary/break for early return
 *   - Constructor requires `(using Sge)` (SGE context parameter)
 *   - Split package, braces, no-return conventions satisfied
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 182
 * Covenant-baseline-methods: TiledMapLoader,atlasTmjMapLoader,atlasTmxMapLoader,extension,file,getDependencies,load,loadAsync,loadSync,param,this,tmjMapLoader,tmxMapLoader,usesAtlas,xmlReader
 * Covenant-source-reference: com/badlogic/gdx/maps/tiled/TiledMapLoader.java
 * Covenant-verified: 2026-04-19
 */
package sge
package maps
package tiled

import sge.assets.{ AssetDescriptor, AssetManager }
import sge.assets.loaders.{ AsynchronousAssetLoader, FileHandleResolver }
import sge.files.FileHandle
import sge.utils.{ DynamicArray, Nullable, XmlReader }

import scala.language.implicitConversions
import scala.util.boundary
import scala.util.boundary.break

/** A universal map loader that automatically delegates to the appropriate underlying map loader [[TmxMapLoader]], [[AtlasTmxMapLoader]], [[TmjMapLoader]], or [[AtlasTmjMapLoader]] based solely on the
  * map file's extension and content. A primary use case is for projects that need to load a mix of TMX and TMJ maps (with or without atlases) using a single loader instance inside an
  * [[AssetManager]]. For TMX and TMJ files, this loader checks for the presence of an `"atlas"` property. If found, it uses an atlas-based loader; otherwise, it falls back to the standard loader.
  */
class TiledMapLoader(resolver: FileHandleResolver)(using Sge) extends AsynchronousAssetLoader[TiledMap, BaseTiledMapLoader.Parameters](resolver) {

  def this()(using Sge) = this(FileHandleResolver.Internal())

  private val tmxMapLoader: TmxMapLoader = TmxMapLoader(resolver)
  private val tmjMapLoader: TmjMapLoader = TmjMapLoader(resolver)

  private val atlasTmxMapLoader: AtlasTmxMapLoader = AtlasTmxMapLoader(resolver)
  private val xmlReader:         XmlReader         = XmlReader()

  private val atlasTmjMapLoader: AtlasTmjMapLoader = AtlasTmjMapLoader(resolver)

  /** Universal synchronous loader. This method is a thin wrapper that picks the correct underlying loader (TMX vs TMJ, atlas vs non-atlas) and then delegates straight through to its synchronous
    * `load(...)` implementation.
    * @param fileName
    *   path to a .tmx or .tmj file
    * @return
    *   a loaded [[TiledMap]]
    */
  def load(fileName: String): TiledMap =
    load(fileName, BaseTiledMapLoader.Parameters())

  /** Universal synchronous loader with custom parameters. Resolves the file and inspects the extension (tmx vs tmj). Check whether the 'atlas' property exists in the map and delegates to the
    * appropriate loader's `load(...)`.
    * @param fileName
    *   path to a .tmx or .tmj file
    * @param parameter
    *   existing Parameters object
    * @return
    *   a loaded [[TiledMap]]
    */
  def load(fileName: String, parameter: BaseTiledMapLoader.Parameters): TiledMap = {
    var param = parameter
    if (Nullable(param).isEmpty) param = BaseTiledMapLoader.Parameters()
    val file      = resolve(fileName)
    val extension = file.extension.toLowerCase
    if (extension == "tmx") {
      if (usesAtlas(file))
        atlasTmxMapLoader.load(fileName, param)
      else
        tmxMapLoader.load(fileName, param)
    } else if (extension == "tmj") {
      if (usesAtlas(file))
        atlasTmjMapLoader.load(fileName, param)
      else
        tmjMapLoader.load(fileName, param)
    } else {
      throw new IllegalArgumentException("Unsupported map format: '" + extension + "' in file: " + fileName)
    }
  }

  override def getDependencies(
    fileName:  String,
    file:      FileHandle,
    parameter: BaseTiledMapLoader.Parameters
  ): DynamicArray[AssetDescriptor[?]] = {
    var param = parameter
    if (Nullable(param).isEmpty) param = BaseTiledMapLoader.Parameters()
    val extension = file.extension.toLowerCase
    if (extension == "tmx") {
      if (usesAtlas(file))
        atlasTmxMapLoader.getDependencies(fileName, file, param)
      else
        tmxMapLoader.getDependencies(fileName, file, param)
    } else if (extension == "tmj") {
      if (usesAtlas(file))
        atlasTmjMapLoader.getDependencies(fileName, file, param)
      else
        tmjMapLoader.getDependencies(fileName, file, param)
    } else {
      throw new IllegalArgumentException("Unsupported map format: " + extension)
    }
  }

  override def loadAsync(
    manager:   AssetManager,
    fileName:  String,
    file:      FileHandle,
    parameter: BaseTiledMapLoader.Parameters
  ): Unit = {
    var param = parameter
    if (Nullable(param).isEmpty) param = BaseTiledMapLoader.Parameters()
    val extension = file.extension.toLowerCase
    if (extension == "tmx") {
      if (usesAtlas(file))
        atlasTmxMapLoader.loadAsync(manager, fileName, file, param)
      else
        tmxMapLoader.loadAsync(manager, fileName, file, param)
    } else if (extension == "tmj") {
      if (usesAtlas(file))
        atlasTmjMapLoader.loadAsync(manager, fileName, file, param)
      else
        tmjMapLoader.loadAsync(manager, fileName, file, param)
    } else {
      throw new IllegalArgumentException("Unsupported map format: " + extension)
    }
  }

  override def loadSync(
    manager:   AssetManager,
    fileName:  String,
    file:      FileHandle,
    parameter: BaseTiledMapLoader.Parameters
  ): TiledMap = {
    var param = parameter
    if (Nullable(param).isEmpty) param = BaseTiledMapLoader.Parameters()
    val extension = file.extension.toLowerCase
    if (extension == "tmx") {
      if (usesAtlas(file))
        atlasTmxMapLoader.loadSync(manager, fileName, file, param)
      else
        tmxMapLoader.loadSync(manager, fileName, file, param)
    } else if (extension == "tmj") {
      if (usesAtlas(file))
        atlasTmjMapLoader.loadSync(manager, fileName, file, param)
      else
        tmjMapLoader.loadSync(manager, fileName, file, param)
    } else {
      throw new IllegalArgumentException("Unsupported map format: " + extension)
    }
  }

  private def usesAtlas(file: FileHandle): Boolean = boundary {
    val extension = file.extension.toLowerCase
    if (extension == "tmx") {
      val root       = xmlReader.parse(file)
      val properties = root.getChildByName("properties")
      properties.foreach { props =>
        val propertyElements = props.getChildrenByName("property")
        var pi               = 0
        while (pi < propertyElements.size) {
          val property = propertyElements(pi)
          val name     = property.getAttribute("name", Nullable("")).getOrElse("")
          if ("atlas" == name) {
            break(true)
          }
          pi += 1
        }
      }
    } else if (extension == "tmj") {
      import sge.utils.readJson
      val map = file.readJson[TmjMapJson]
      for (prop <- map.properties)
        if (prop.name == "atlas") {
          break(true)
        }
    }
    false
  }
}
