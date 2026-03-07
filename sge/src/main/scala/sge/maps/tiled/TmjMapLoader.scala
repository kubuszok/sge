/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/TmjMapLoader.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audited 2026-03-03):
 *   - All methods match Java 1:1 (load, loadAsync, loadSync, getDependencyAssetDescriptors,
 *     getDependencyFileHandles, collectImageLayerFileHandles, getTileSetDependencyFileHandle,
 *     addStaticTiles)
 *   - Java ObjectMap<String,Texture> → mutable.HashMap[String,Texture]
 *   - Java setOwnedResources(textures.values().toArray()) → DynamicArray construction
 *   - Constructor requires `(using Sge)` (SGE context parameter)
 *   - Split package, braces, no-return conventions satisfied
 *   - JsonValue tree walking replaced with jsoniter-scala codec derivation (TmjJson DTOs)
 *   Convention: jsoniter-scala codec derivation replaces JsonValue tree walking
 *   Audited: 2026-03-04
 */
package sge
package maps
package tiled

import sge.assets.{ AssetDescriptor, AssetManager }
import sge.assets.loaders.{ FileHandleResolver, TextureLoader }
import sge.files.FileHandle
import sge.graphics.Texture
import sge.graphics.g2d.TextureRegion
import sge.utils.{ DynamicArray, Nullable, readJson }

import scala.collection.mutable

/** @brief
  *   synchronous loader for TMJ maps created with the Tiled tool
  */
class TmjMapLoader(resolver: FileHandleResolver)(using Sge) extends BaseTmjMapLoader[BaseTiledMapLoader.Parameters](resolver) {

  def this()(using Sge) = this(FileHandleResolver.Internal())

  /** Loads the [[TiledMap]] from the given file. The file is resolved via the [[FileHandleResolver]] set in the constructor of this class. By default it will resolve to an internal file. The map will
    * be loaded for a y-up coordinate system.
    *
    * @param fileName
    *   the filename
    * @return
    *   the TiledMap
    */
  def load(fileName: String): TiledMap =
    load(fileName, BaseTiledMapLoader.Parameters())

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

    this.root = Nullable(tmjFile.readJson[TmjMapJson])

    val textures = mutable.HashMap.empty[String, Texture]

    val textureFiles = getDependencyFileHandles(tmjFile)
    for (textureFile <- textureFiles) {
      val texture = Texture(textureFile, parameter.generateMipMaps)
      texture.setFilter(parameter.textureMinFilter, parameter.textureMagFilter)
      textures.put(textureFile.path(), texture)
    }

    val map            = loadTiledMap(tmjFile, parameter, ImageResolver.DirectImageResolver(textures))
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
    this.map = loadTiledMap(tmjFile, parameter, ImageResolver.AssetManagerImageResolver(manager))

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
    val r = root.getOrElse(throw new IllegalStateException("root not initialized"))
    for (tileSet <- r.tilesets)
      getTileSetDependencyFileHandle(fileHandles, tmjFile, tileSet)

    // ImageLayer descriptors
    collectImageLayerFileHandles(r.layers, tmjFile, fileHandles)

    fileHandles
  }

  private def collectImageLayerFileHandles(
    layers:      List[TmjLayerJson],
    tmjFile:     FileHandle,
    fileHandles: DynamicArray[FileHandle]
  ): Unit =
    for (layer <- layers)
      if (layer.tpe == "imagelayer") {
        layer.image.foreach { s =>
          val handle = BaseTiledMapLoader.getRelativeFileHandle(tmjFile, s)
          fileHandles.add(handle)
        }
      } else if (layer.tpe == "group") {
        // Recursively process group layers
        collectImageLayerFileHandles(layer.layers, tmjFile, fileHandles)
      }

  protected def getTileSetDependencyFileHandle(
    tmjFile: FileHandle,
    tileSet: TmjTilesetRefJson
  ): DynamicArray[FileHandle] = {
    val fileHandles = DynamicArray[FileHandle]()
    getTileSetDependencyFileHandle(fileHandles, tmjFile, tileSet)
  }

  protected def getTileSetDependencyFileHandle(
    fileHandles: DynamicArray[FileHandle],
    tmjFile:     FileHandle,
    tileSet:     TmjTilesetRefJson
  ): DynamicArray[FileHandle] = {
    var ts = tileSet
    var tsjFile: FileHandle = tmjFile
    tileSet.source.foreach { s =>
      tsjFile = BaseTiledMapLoader.getRelativeFileHandle(tmjFile, s)
      ts = tsjFile.readJson[TmjTilesetRefJson]
    }
    if (ts.image.isDefined) {
      val imageSource = ts.image.getOrElse("")
      val image       = BaseTiledMapLoader.getRelativeFileHandle(tsjFile, imageSource)
      fileHandles.add(image)
    } else {
      for (tile <- ts.tiles)
        tile.image.foreach { imgSource =>
          val image = BaseTiledMapLoader.getRelativeFileHandle(tsjFile, imgSource)
          fileHandles.add(image)
        }
    }
    fileHandles
  }

  override protected def addStaticTiles(
    tmjFile:       FileHandle,
    imageResolver: ImageResolver,
    tileSet:       TiledMapTileSet,
    tiles:         List[TmjTileJson],
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

    val props = tileSet.properties
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
        val stopWidth  = tex.regionWidth - tilewidth
        val stopHeight = tex.regionHeight - tileheight

        var id = firstgid

        var y = margin
        while (y <= stopHeight) {
          var x = margin
          while (x <= stopWidth) {
            val tileRegion = TextureRegion(tex, x, y, tilewidth, tileheight)
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
        tile.image.foreach { imgSource =>
          if (source.nonEmpty) {
            currentImage = BaseTiledMapLoader.getRelativeFileHandle(
              BaseTiledMapLoader.getRelativeFileHandle(tmjFile, source),
              imgSource
            )
          } else {
            currentImage = BaseTiledMapLoader.getRelativeFileHandle(tmjFile, imgSource)
          }
        }
        val texture = imageResolver.getImage(currentImage.path())
        val tileId  = firstgid + tile.id
        texture.foreach(t => addStaticTiledMapTile(tileSet, t, tileId, offsetX.toFloat, offsetY.toFloat))
      }
    }
  }
}
