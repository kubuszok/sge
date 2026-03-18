/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/TideMapLoader.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audited 2026-03-03):
 *   - All methods match Java 1:1 (load, getDependencies, loadMap, loadTileSheets,
 *     loadTileSheet, loadLayer, loadProperties, getRelativeFileHandle)
 *   - Java ObjectMap<String,Texture> → mutable.HashMap[String,Texture]
 *   - Java setOwnedResources(textures.values().toArray()) → DynamicArray construction
 *   - Minor: loadTileSheet skips reading Description child (Java reads it but never uses it)
 *   - Minor: loadTileSheet wraps tile creation inside `texture.foreach` for null safety
 *     (Java does not check null but Nullable ImageResolver.getImage requires it)
 *   - currentTileSet var uses null // scalastyle:ignore (Java interop boundary for Tide format)
 *   - Constructor requires `(using Sge)` (SGE context parameter, replaces Java Gdx.files)
 *   - Split package, braces, no-return conventions satisfied
 *   TODO: test: decode a real .tide file end-to-end through TideMapLoader
 */
package sge
package maps
package tiled

import sge.assets.{ AssetDescriptor, AssetLoaderParameters, AssetManager }
import sge.assets.loaders.{ FileHandleResolver, SynchronousAssetLoader }
import sge.files.FileHandle
import sge.graphics.Texture
import sge.graphics.g2d.TextureRegion
import sge.maps.tiled.TiledMapTileLayer.Cell
import sge.maps.tiled.tiles.{ AnimatedTiledMapTile, StaticTiledMapTile }
import sge.utils.{ DynamicArray, Nullable, XmlReader }

import java.io.IOException
import scala.collection.mutable

class TideMapLoader(resolver: FileHandleResolver)(using Sge) extends SynchronousAssetLoader[TiledMap, TideMapLoader.Parameters](resolver) {

  def this()(using Sge) = this(FileHandleResolver.Internal())

  private val xml:  XmlReader         = XmlReader()
  private var root: XmlReader.Element = scala.compiletime.uninitialized

  def load(fileName: String): TiledMap =
    try {
      val tideFile = resolve(fileName)
      root = xml.parse(tideFile)
      val textures = mutable.HashMap.empty[String, Texture]
      for (textureFile <- loadTileSheets(root, tideFile))
        textures.put(textureFile.path, Texture(textureFile))
      val imageResolver  = new ImageResolver.DirectImageResolver(textures)
      val map            = loadMap(root, tideFile, imageResolver)
      val ownedResources = DynamicArray[AutoCloseable]()
      textures.values.foreach(t => ownedResources.add(t))
      map.setOwnedResources(ownedResources)
      map
    } catch {
      case e: IOException =>
        throw new IllegalArgumentException("Couldn't load tilemap '" + fileName + "'", e)
    }

  override def load(
    assetManager: AssetManager,
    fileName:     String,
    tideFile:     FileHandle,
    parameter:    TideMapLoader.Parameters
  ): TiledMap =
    try
      loadMap(root, tideFile, new ImageResolver.AssetManagerImageResolver(assetManager))
    catch {
      case e: Exception =>
        throw new IllegalArgumentException("Couldn't load tilemap '" + fileName + "'", e)
    }

  override def getDependencies(
    fileName:  String,
    tmxFile:   FileHandle,
    parameter: TideMapLoader.Parameters
  ): DynamicArray[AssetDescriptor[?]] = {
    val dependencies = DynamicArray[AssetDescriptor[?]]()
    try {
      root = xml.parse(tmxFile)
      for (image <- loadTileSheets(root, tmxFile))
        dependencies.add(AssetDescriptor[Texture](image.path))
      dependencies
    } catch {
      case e: IOException =>
        throw new IllegalArgumentException("Couldn't load tilemap '" + fileName + "'", e)
    }
  }

  /** Loads the map data, given the XML root element and an [[ImageResolver]] used to return the tileset Textures
    * @param root
    *   the XML root element
    * @param tmxFile
    *   the Filehandle of the tmx file
    * @param imageResolver
    *   the [[ImageResolver]]
    * @return
    *   the [[TiledMap]]
    */
  private def loadMap(root: XmlReader.Element, tmxFile: FileHandle, imageResolver: ImageResolver): TiledMap = {
    val map        = TiledMap()
    val properties = root.getChildByName("Properties")
    properties.foreach { p =>
      loadProperties(map.properties, p)
    }
    val tilesheets        = root.getChildByName("TileSheets")
    val tilesheetElements = tilesheets.getOrElse(throw new IllegalStateException("missing TileSheets element")).getChildrenByName("TileSheet")
    var ti                = 0
    while (ti < tilesheetElements.size) {
      val tilesheet = tilesheetElements(ti)
      loadTileSheet(map, tilesheet, tmxFile, imageResolver)
      ti += 1
    }
    val layers        = root.getChildByName("Layers")
    val layerElements = layers.getOrElse(throw new IllegalStateException("missing Layers element")).getChildrenByName("Layer")
    var li            = 0
    while (li < layerElements.size) {
      val layer = layerElements(li)
      loadLayer(map, layer)
      li += 1
    }
    map
  }

  /** Loads the tilesets
    * @param root
    *   the root XML element
    * @return
    *   a list of filenames for images containing tiles
    */
  private def loadTileSheets(root: XmlReader.Element, tideFile: FileHandle): DynamicArray[FileHandle] = {
    val images          = DynamicArray[FileHandle]()
    val tilesheets      = root.getChildByName("TileSheets")
    val tilesetElements = tilesheets.getOrElse(throw new IllegalStateException("missing TileSheets element")).getChildrenByName("TileSheet")
    var ti              = 0
    while (ti < tilesetElements.size) {
      val tileset     = tilesetElements(ti)
      val imageSource = tileset.getChildByName("ImageSource")
      val image       = getRelativeFileHandle(tideFile, imageSource.getOrElse(throw new IllegalStateException("missing ImageSource")).text.getOrElse(""))
      images.add(image)
      ti += 1
    }
    images
  }

  private def loadTileSheet(
    map:           TiledMap,
    element:       XmlReader.Element,
    tideFile:      FileHandle,
    imageResolver: ImageResolver
  ): Unit =
    if (element.name == "TileSheet") {
      val id          = element.getAttribute("Id")
      val imageSource = element.getChildByName("ImageSource").getOrElse(throw new IllegalStateException("missing ImageSource")).text.getOrElse("")

      val alignment = element.getChildByName("Alignment").getOrElse(throw new IllegalStateException("missing Alignment"))
      val sheetSize = alignment.getAttribute("SheetSize")
      val tileSize  = alignment.getAttribute("TileSize")
      val margin    = alignment.getAttribute("Margin")
      alignment.getAttribute("Spacing")

      val sheetSizeParts = sheetSize.split(" x ")
      Integer.parseInt(sheetSizeParts(0))
      Integer.parseInt(sheetSizeParts(1))

      val tileSizeParts = tileSize.split(" x ")
      val tileSizeX     = Integer.parseInt(tileSizeParts(0))
      val tileSizeY     = Integer.parseInt(tileSizeParts(1))

      val marginParts = margin.split(" x ")
      val marginX     = Integer.parseInt(marginParts(0))
      val marginY     = Integer.parseInt(marginParts(1))

      val spacingParts = margin.split(" x ")
      val spacingX     = Integer.parseInt(spacingParts(0))
      val spacingY     = Integer.parseInt(spacingParts(1))

      val image   = getRelativeFileHandle(tideFile, imageSource)
      val texture = imageResolver.getImage(image.path)

      val tilesets = map.tileSets
      var firstgid = 1
      for (tileset <- tilesets)
        firstgid += tileset.size

      val tileset = TiledMapTileSet()
      tileset.name = id
      tileset.properties.put("firstgid", firstgid: java.lang.Integer)
      var gid = firstgid

      texture.foreach { tex =>
        val stopWidth  = tex.regionWidth - tileSizeX
        val stopHeight = tex.regionHeight - tileSizeY

        var y = marginY
        while (y <= stopHeight) {
          var x = marginX
          while (x <= stopWidth) {
            val tile: TiledMapTile =
              StaticTiledMapTile(TextureRegion(tex, x, y, tileSizeX, tileSizeY))
            tile.id = gid
            tileset.putTile(gid, tile)
            gid += 1
            x += tileSizeX + spacingX
          }
          y += tileSizeY + spacingY
        }
      }

      val properties = element.getChildByName("Properties")
      properties.foreach { p =>
        loadProperties(tileset.properties, p)
      }

      tilesets.addTileSet(tileset)
    }

  private def loadLayer(map: TiledMap, element: XmlReader.Element): Unit =
    if (element.name == "Layer") {
      val id      = element.getAttribute("Id")
      val visible = element.getAttribute("Visible")

      val dimensions = element.getChildByName("Dimensions").getOrElse(throw new IllegalStateException("missing Dimensions"))
      val layerSize  = dimensions.getAttribute("LayerSize")
      val tileSize   = dimensions.getAttribute("TileSize")

      val layerSizeParts = layerSize.split(" x ")
      val layerSizeX     = Integer.parseInt(layerSizeParts(0))
      val layerSizeY     = Integer.parseInt(layerSizeParts(1))

      val tileSizeParts = tileSize.split(" x ")
      val tileSizeX     = Integer.parseInt(tileSizeParts(0))
      val tileSizeY     = Integer.parseInt(tileSizeParts(1))

      val layer = TiledMapTileLayer(layerSizeX, layerSizeY, tileSizeX, tileSizeY)
      layer.name = id
      layer.visible = visible.equalsIgnoreCase("True")
      val tileArray = element.getChildByName("TileArray").getOrElse(throw new IllegalStateException("missing TileArray"))
      val rows      = tileArray.getChildrenByName("Row")
      val tilesets  = map.tileSets
      var currentTileSet: TiledMapTileSet = null // scalastyle:ignore
      var firstgid = 0
      var row      = 0
      val rowCount = rows.size
      while (row < rowCount) {
        val currentRow = rows(row)
        val y          = rowCount - 1 - row
        var x          = 0
        var child      = 0
        val childCount = currentRow.childCount
        while (child < childCount) {
          val currentChild = currentRow.getChild(child)
          val name         = currentChild.name
          if (name == "TileSheet") {
            currentTileSet = tilesets.getTileSet(currentChild.getAttribute("Ref")).getOrElse(throw new IllegalStateException("missing TileSheet"))
            firstgid = currentTileSet.properties.getAs[Integer]("firstgid").get.intValue()
          } else if (name == "Null") {
            x += currentChild.getIntAttribute("Count", 0)
          } else if (name == "Static") {
            val cell = Cell()
            cell.tile = currentTileSet.getTile(firstgid + currentChild.getIntAttribute("Index", 0))
            layer.setCell(x, y, Nullable(cell))
            x += 1
          } else if (name == "Animated") {
            // Create an AnimatedTile
            val interval        = currentChild.getInt("Interval", 0)
            val frames          = currentChild.getChildByName("Frames").getOrElse(throw new IllegalStateException("missing Frames"))
            val frameTiles      = DynamicArray[StaticTiledMapTile]()
            var frameChild      = 0
            val frameChildCount = frames.childCount
            while (frameChild < frameChildCount) {
              val frame     = frames.getChild(frameChild)
              val frameName = frame.name
              if (frameName == "TileSheet") {
                currentTileSet = tilesets.getTileSet(frame.getAttribute("Ref")).getOrElse(throw new IllegalStateException("missing TileSheet"))
                firstgid = currentTileSet.properties.getAs[Integer]("firstgid").get.intValue()
              } else if (frameName == "Static") {
                frameTiles.add(
                  currentTileSet.getTile(firstgid + frame.getIntAttribute("Index", 0)).getOrElse(throw new IllegalStateException("missing tile")).asInstanceOf[StaticTiledMapTile]
                )
              }
              frameChild += 1
            }
            val cell = Cell()
            cell.tile = Nullable(AnimatedTiledMapTile(interval / 1000f, frameTiles))
            layer.setCell(x, y, Nullable(cell))
            x += 1 // TODO: Reuse existing animated tiles
          }
          child += 1
        }
        row += 1
      }

      val properties = element.getChildByName("Properties")
      properties.foreach { p =>
        loadProperties(layer.properties, p)
      }

      map.layers.add(layer)
    }

  private def loadProperties(properties: MapProperties, element: XmlReader.Element): Unit =
    if (element.name == "Properties") {
      val propertyElements = element.getChildrenByName("Property")
      var pi               = 0
      while (pi < propertyElements.size) {
        val property = propertyElements(pi)
        val key      = property.getAttribute("Key", Nullable.empty).getOrElse("")
        val propType = property.getAttribute("Type", Nullable.empty).getOrElse("")
        val value    = property.text.getOrElse("")

        if (propType == "Int32") {
          properties.put(key, Integer.parseInt(value): java.lang.Integer)
        } else if (propType == "String") {
          properties.put(key, value)
        } else if (propType == "Boolean") {
          properties.put(key, java.lang.Boolean.valueOf(value.equalsIgnoreCase("true")))
        } else {
          properties.put(key, value)
        }
        pi += 1
      }
    }

  private def getRelativeFileHandle(file: FileHandle, path: String): FileHandle =
    BaseTiledMapLoader.getRelativeFileHandle(file, path)
}

object TideMapLoader {

  class Parameters extends AssetLoaderParameters[TiledMap]
}
