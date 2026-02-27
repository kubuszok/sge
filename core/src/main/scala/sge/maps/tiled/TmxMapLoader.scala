/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/TmxMapLoader.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
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
import sge.utils.{ DynamicArray, Nullable, XmlReader }

import scala.collection.mutable

/** @brief
  *   synchronous loader for TMX maps created with the Tiled tool
  */
class TmxMapLoader(resolver: FileHandleResolver)(using sge: Sge) extends BaseTmxMapLoader[BaseTiledMapLoader.Parameters](resolver) {

  def this()(using sge: Sge) = this(new InternalFileHandleResolver())

  /** Loads the [[TiledMap]] from the given file. The file is resolved via the [[FileHandleResolver]] set in the constructor of this class. By default it will resolve to an internal file. The map will
    * be loaded for a y-up coordinate system.
    * @param fileName
    *   the filename
    * @return
    *   the TiledMap
    */
  def load(fileName: String): TiledMap =
    load(fileName, new BaseTiledMapLoader.Parameters())

  /** Loads the [[TiledMap]] from the given file. The file is resolved via the [[FileHandleResolver]] set in the constructor of this class. By default it will resolve to an internal file.
    * @param fileName
    *   the filename
    * @param parameter
    *   specifies whether to use y-up, generate mip maps etc.
    * @return
    *   the TiledMap
    */
  def load(fileName: String, parameter: BaseTiledMapLoader.Parameters): TiledMap = {
    val tmxFile = resolve(fileName)

    this.root = Nullable(xml.parse(tmxFile))

    val textures = mutable.HashMap.empty[String, Texture]

    val textureFiles = getDependencyFileHandles(tmxFile)
    for (textureFile <- textureFiles) {
      val texture = new Texture(textureFile, parameter.generateMipMaps)
      texture.setFilter(parameter.textureMinFilter, parameter.textureMagFilter)
      textures.put(textureFile.path(), texture)
    }

    val map            = loadTiledMap(tmxFile, parameter, new ImageResolver.DirectImageResolver(textures))
    val ownedResources = DynamicArray[AutoCloseable]()
    textures.values.foreach(t => ownedResources.add(t))
    map.setOwnedResources(ownedResources)
    map
  }

  override def loadAsync(
    manager:   AssetManager,
    fileName:  String,
    tmxFile:   FileHandle,
    parameter: BaseTiledMapLoader.Parameters
  ): Unit =
    this.map = loadTiledMap(tmxFile, parameter, new ImageResolver.AssetManagerImageResolver(manager))

  override def loadSync(
    manager:   AssetManager,
    fileName:  String,
    file:      FileHandle,
    parameter: BaseTiledMapLoader.Parameters
  ): TiledMap = map

  override protected def getDependencyAssetDescriptors(
    tmxFile:          FileHandle,
    textureParameter: TextureLoader.TextureParameter
  ): DynamicArray[AssetDescriptor[?]] = {
    val descriptors = DynamicArray[AssetDescriptor[?]]()

    val fileHandles = getDependencyFileHandles(tmxFile)
    for (handle <- fileHandles)
      descriptors.add(new AssetDescriptor[Texture](handle, classOf[Texture], textureParameter))

    descriptors
  }

  protected def getDependencyFileHandles(tmxFile: FileHandle): DynamicArray[FileHandle] = {
    val fileHandles = DynamicArray[FileHandle]()

    // TileSet descriptors
    val tilesetElements = root.orNull.getChildrenByNameRecursively("tileset")
    var ti              = 0
    while (ti < tilesetElements.size) {
      getTileSetDependencyFileHandle(fileHandles, tmxFile, tilesetElements(ti))
      ti += 1
    }

    // ImageLayer descriptors
    val imageLayerElements = root.orNull.getChildrenByNameRecursively("imagelayer")
    var ii                 = 0
    while (ii < imageLayerElements.size) {
      val imageLayer = imageLayerElements(ii)
      val image      = imageLayer.getChildByName("image")
      if (image.isDefined) {
        val source = image.orNull.getAttribute("source", Nullable.empty)
        if (source.isDefined) {
          val handle = BaseTiledMapLoader.getRelativeFileHandle(tmxFile, source.orNull)
          fileHandles.add(handle)
        }
      }
      ii += 1
    }

    fileHandles
  }

  protected def getTileSetDependencyFileHandle(
    tmxFile: FileHandle,
    tileset: XmlReader.Element
  ): DynamicArray[FileHandle] = {
    val fileHandles = DynamicArray[FileHandle]()
    getTileSetDependencyFileHandle(fileHandles, tmxFile, tileset)
  }

  protected def getTileSetDependencyFileHandle(
    fileHandles: DynamicArray[FileHandle],
    tmxFile:     FileHandle,
    tileset:     XmlReader.Element
  ): DynamicArray[FileHandle] = {
    var ts = tileset
    var tsxFile: FileHandle = tmxFile
    val source = ts.getAttribute("source", Nullable.empty)
    if (source.isDefined) {
      tsxFile = BaseTiledMapLoader.getRelativeFileHandle(tmxFile, source.orNull)
      ts = xml.parse(tsxFile)
    }
    val imageElement = ts.getChildByName("image")
    if (imageElement.isDefined) {
      val imageSource = imageElement.orNull.getAttribute("source")
      val image       = BaseTiledMapLoader.getRelativeFileHandle(tsxFile, imageSource)
      fileHandles.add(image)
    } else {
      val tileElements = ts.getChildrenByName("tile")
      var tei          = 0
      while (tei < tileElements.size) {
        val tile        = tileElements(tei)
        val imageSource = tile.getChildByName("image").orNull.getAttribute("source")
        val image       = BaseTiledMapLoader.getRelativeFileHandle(tsxFile, imageSource)
        fileHandles.add(image)
        tei += 1
      }
    }
    fileHandles
  }

  override protected def addStaticTiles(
    tmxFile:       FileHandle,
    imageResolver: ImageResolver,
    tileSet:       TiledMapTileSet,
    element:       XmlReader.Element,
    tileElements:  DynamicArray[XmlReader.Element],
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
    if (image != null) {
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
      for (tileElement <- tileElements) {
        val imageElement = tileElement.getChildByName("image")
        if (imageElement.isDefined) {
          val imgSource = imageElement.orNull.getAttribute("source")

          if (source != null) {
            currentImage = BaseTiledMapLoader.getRelativeFileHandle(
              BaseTiledMapLoader.getRelativeFileHandle(tmxFile, source),
              imgSource
            )
          } else {
            currentImage = BaseTiledMapLoader.getRelativeFileHandle(tmxFile, imgSource)
          }
        }
        val texture = imageResolver.getImage(currentImage.path())
        val tileId  = firstgid + tileElement.getIntAttribute("id", 0)
        texture.foreach(t => addStaticTiledMapTile(tileSet, t, tileId, offsetX.toFloat, offsetY.toFloat))
      }
    }
  }
}
