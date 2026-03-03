/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/TmjMapLoader.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes (audited 2026-03-03):
 *   - All methods match Java 1:1 (load, loadAsync, loadSync, getDependencyAssetDescriptors,
 *     getDependencyFileHandles, collectImageLayerFileHandles, getTileSetDependencyFileHandle,
 *     addStaticTiles)
 *   - Java ObjectMap<String,Texture> → mutable.HashMap[String,Texture]
 *   - Java setOwnedResources(textures.values().toArray()) → DynamicArray construction
 *   - Constructor requires `(using Sge)` (SGE context parameter)
 *   - Split package, braces, no-return conventions satisfied
 */
package sge
package maps
package tiled

import sge.assets.{ AssetDescriptor, AssetManager }
import sge.assets.loaders.{ FileHandleResolver, TextureLoader }
import sge.assets.loaders.resolvers.InternalFileHandleResolver
import sge.files.FileHandle
import sge.graphics.Texture
import sge.graphics.g2d.TextureRegion
import sge.utils.{ DynamicArray, JsonValue, Nullable }

import scala.collection.mutable
import scala.language.implicitConversions

/** @brief
  *   synchronous loader for TMJ maps created with the Tiled tool
  */
class TmjMapLoader(resolver: FileHandleResolver)(using Sge) extends BaseTmjMapLoader[BaseTiledMapLoader.Parameters](resolver) {

  def this()(using Sge) = this(new InternalFileHandleResolver())

  /** Loads the [[TiledMap]] from the given file. The file is resolved via the [[FileHandleResolver]] set in the constructor of this class. By default it will resolve to an internal file. The map will
    * be loaded for a y-up coordinate system.
    *
    * @param fileName
    *   the filename
    * @return
    *   the TiledMap
    */
  def load(fileName: String): TiledMap =
    load(fileName, new BaseTiledMapLoader.Parameters())

  /** Loads the [[TiledMap]] from the given file. The file is resolved via the [[FileHandleResolver]] set in the constructor of this class. By default it will resolve to an internal file.
    *
    * @param fileName
    *   the filename
    * @param parameter
    *   specifies whether to use y-up, generate mip maps etc.
    * @return
    *   the TiledMap
    */
  def load(fileName: String, parameter: BaseTiledMapLoader.Parameters): TiledMap = {
    val tmjFile = resolve(fileName)

    this.root = Nullable(json.parse(tmjFile))

    val textures = mutable.HashMap.empty[String, Texture]

    val textureFiles = getDependencyFileHandles(tmjFile)
    for (textureFile <- textureFiles) {
      val texture = new Texture(textureFile, parameter.generateMipMaps)
      texture.setFilter(parameter.textureMinFilter, parameter.textureMagFilter)
      textures.put(textureFile.path(), texture)
    }

    val map            = loadTiledMap(tmjFile, parameter, new ImageResolver.DirectImageResolver(textures))
    val ownedResources = DynamicArray[AutoCloseable]()
    textures.values.foreach(t => ownedResources.add(t))
    map.setOwnedResources(ownedResources)
    map
  }

  override def loadAsync(
    manager:   AssetManager,
    fileName:  String,
    tmjFile:   FileHandle,
    parameter: BaseTiledMapLoader.Parameters
  ): Unit =
    this.map = loadTiledMap(tmjFile, parameter, new ImageResolver.AssetManagerImageResolver(manager))

  override def loadSync(
    manager:   AssetManager,
    fileName:  String,
    file:      FileHandle,
    parameter: BaseTiledMapLoader.Parameters
  ): TiledMap = map

  override protected def getDependencyAssetDescriptors(
    tmjFile:          FileHandle,
    textureParameter: TextureLoader.TextureParameter
  ): DynamicArray[AssetDescriptor[?]] = {
    val descriptors = DynamicArray[AssetDescriptor[?]]()

    val fileHandles = getDependencyFileHandles(tmjFile)
    for (handle <- fileHandles)
      descriptors.add(new AssetDescriptor[Texture](handle, classOf[Texture], textureParameter))

    descriptors
  }

  protected def getDependencyFileHandles(tmjFile: FileHandle): DynamicArray[FileHandle] = {
    val fileHandles = DynamicArray[FileHandle]()

    // TileSet descriptors
    val r        = root.getOrElse(throw new IllegalStateException("root not initialized"))
    val tileSets = r.get("tilesets")
    tileSets.foreach { ts =>
      for (tileSet <- ts)
        getTileSetDependencyFileHandle(fileHandles, tmjFile, tileSet)
    }

    // ImageLayer descriptors
    r.get("layers").foreach { layers =>
      collectImageLayerFileHandles(layers, tmjFile, fileHandles)
    }

    fileHandles
  }

  private def collectImageLayerFileHandles(
    layers:      JsonValue,
    tmjFile:     FileHandle,
    fileHandles: DynamicArray[FileHandle]
  ): Unit =
    if (Nullable(layers).isDefined) { // scalastyle:ignore
      for (layer <- layers) {
        val layerType = layer.getString("type", Nullable("")).getOrElse("")
        if (layerType == "imagelayer") {
          val source = layer.getString("image", Nullable.empty)
          source.foreach { s =>
            val handle = BaseTiledMapLoader.getRelativeFileHandle(tmjFile, s)
            fileHandles.add(handle)
          }
        } else if (layerType == "group") {
          // Recursively process group layers
          layer.get("layers").foreach(ls => collectImageLayerFileHandles(ls, tmjFile, fileHandles))
        }
      }
    }

  protected def getTileSetDependencyFileHandle(
    tmjFile: FileHandle,
    tileSet: JsonValue
  ): DynamicArray[FileHandle] = {
    val fileHandles = DynamicArray[FileHandle]()
    getTileSetDependencyFileHandle(fileHandles, tmjFile, tileSet)
  }

  protected def getTileSetDependencyFileHandle(
    fileHandles: DynamicArray[FileHandle],
    tmjFile:     FileHandle,
    tileSet:     JsonValue
  ): DynamicArray[FileHandle] = {
    var ts = tileSet
    var tsjFile: FileHandle = tmjFile
    val source = ts.getString("source", Nullable.empty)
    source.foreach { s =>
      tsjFile = BaseTiledMapLoader.getRelativeFileHandle(tmjFile, s)
      ts = json.parse(tsjFile)
    }
    if (ts.has("image")) {
      val imageSource = ts.getString("image", Nullable.empty).getOrElse("")
      val image       = BaseTiledMapLoader.getRelativeFileHandle(tsjFile, imageSource)
      fileHandles.add(image)
    } else {
      val tiles = ts.get("tiles")
      tiles.foreach { t =>
        for (tile <- t) {
          val imageSource = tile.getString("image", Nullable.empty).getOrElse("")
          val image       = BaseTiledMapLoader.getRelativeFileHandle(tsjFile, imageSource)
          fileHandles.add(image)
        }
      }
    }
    fileHandles
  }

  override protected def addStaticTiles(
    tmjFile:       FileHandle,
    imageResolver: ImageResolver,
    tileSet:       TiledMapTileSet,
    element:       JsonValue,
    tiles:         JsonValue,
    name:          String,
    firstgid:      Int,
    tilewidth:     Int,
    tileheight:    Int,
    spacing:       Int,
    margin:        Int,
    source:        String,
    offsetX:       Int,
    offsetY:       Int,
    imageSource:   String,
    imageWidth:    Int,
    imageHeight:   Int,
    image:         FileHandle
  ): Unit = {

    val props = tileSet.getProperties
    if (Nullable(image).isDefined) {
      // One image for the whole tileSet
      val texture = imageResolver.getImage(image.path())

      props.put("imagesource", imageSource)
      props.put("imagewidth", imageWidth:   java.lang.Integer)
      props.put("imageheight", imageHeight: java.lang.Integer)
      props.put("tilewidth", tilewidth:     java.lang.Integer)
      props.put("tileheight", tileheight:   java.lang.Integer)
      props.put("margin", margin:           java.lang.Integer)
      props.put("spacing", spacing:         java.lang.Integer)

      texture.foreach { tex =>
        val stopWidth  = tex.getRegionWidth() - tilewidth
        val stopHeight = tex.getRegionHeight() - tileheight

        var id = firstgid

        var y = margin
        while (y <= stopHeight) {
          var x = margin
          while (x <= stopWidth) {
            val tileRegion = new TextureRegion(tex, x, y, tilewidth, tileheight)
            val tileId     = id
            id += 1
            addStaticTiledMapTile(tileSet, tileRegion, tileId, offsetX.toFloat, offsetY.toFloat)
            x += tilewidth + spacing
          }
          y += tileheight + spacing
        }
      }
    } else {
      // Every tile has its own image source
      var currentImage = image
      for (tile <- tiles) {
        if (tile.has("image")) {
          val imgSource = tile.getString("image", Nullable.empty).getOrElse("")

          if (Nullable(source).isDefined) {
            currentImage = BaseTiledMapLoader.getRelativeFileHandle(
              BaseTiledMapLoader.getRelativeFileHandle(tmjFile, source),
              imgSource
            )
          } else {
            currentImage = BaseTiledMapLoader.getRelativeFileHandle(tmjFile, imgSource)
          }
        }
        val texture = imageResolver.getImage(currentImage.path())
        val tileId  = firstgid + tile.getInt("id", 0)
        texture.foreach(t => addStaticTiledMapTile(tileSet, t, tileId, offsetX.toFloat, offsetY.toFloat))
      }
    }
  }
}
