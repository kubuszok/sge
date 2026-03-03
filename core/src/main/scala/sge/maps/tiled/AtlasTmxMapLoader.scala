/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/AtlasTmxMapLoader.java
 * Original authors: Justin Shapcott, Manuel Bua
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes (audited 2026-03-03):
 *   - All methods match Java 1:1 (load, loadAsync, loadSync, getDependencyAssetDescriptors,
 *     addStaticTiles, getAtlasFileHandle, setTextureFilters)
 *   - Java inner interface AtlasResolver + 2 inner classes → companion object trait + classes
 *   - parseRegionName reuses AtlasTmjMapLoader.parseRegionName (Java: duplicated in each class)
 *   - Java AtlasRegion return → Nullable[TextureRegion] via .map(r => r: TextureRegion)
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
import sge.graphics.g2d.{ TextureAtlas, TextureRegion }
import sge.utils.{ DynamicArray, Nullable, XmlReader }

import scala.language.implicitConversions

/** A TiledMap Loader which loads tiles from a TextureAtlas instead of separate images.
  *
  * It requires a map-level property called 'atlas' with its value being the relative path to the TextureAtlas. The atlas must have in it indexed regions named after the tilesets used in the map. The
  * indexes shall be local to the tileset (not the global id). Strip whitespace and rotation should not be used when creating the atlas.
  *
  * @author
  *   Justin Shapcott
  * @author
  *   Manuel Bua
  */
class AtlasTmxMapLoader(resolver: FileHandleResolver)(using Sge) extends BaseTmxMapLoader[BaseTiledMapLoader.Parameters](resolver) {

  protected var trackedTextures: DynamicArray[Texture] = DynamicArray[Texture]()

  protected var atlasResolver: AtlasTmxMapLoader.AtlasResolver = scala.compiletime.uninitialized

  def this()(using Sge) = this(new InternalFileHandleResolver())

  def load(fileName: String): TiledMap =
    load(fileName, new BaseTiledMapLoader.Parameters())

  def load(fileName: String, parameter: BaseTiledMapLoader.Parameters): TiledMap = {
    val tmxFile = resolve(fileName)

    this.root = Nullable(xml.parse(tmxFile))

    val atlasFileHandle = getAtlasFileHandle(tmxFile)
    val atlas           = new TextureAtlas(atlasFileHandle)
    this.atlasResolver = new AtlasTmxMapLoader.DirectAtlasResolver(atlas)

    val map            = loadTiledMap(tmxFile, parameter, atlasResolver)
    val ownedResources = DynamicArray[AutoCloseable]()
    ownedResources.add(atlas)
    map.setOwnedResources(ownedResources)
    setTextureFilters(parameter.textureMinFilter, parameter.textureMagFilter)
    map
  }

  override def loadAsync(
    manager:   AssetManager,
    fileName:  String,
    tmxFile:   FileHandle,
    parameter: BaseTiledMapLoader.Parameters
  ): Unit = {
    val atlasHandle = getAtlasFileHandle(tmxFile)
    this.atlasResolver = new AtlasTmxMapLoader.AssetManagerAtlasResolver(manager, atlasHandle.path())

    this.map = loadTiledMap(tmxFile, parameter, atlasResolver)
  }

  override def loadSync(
    manager:   AssetManager,
    fileName:  String,
    file:      FileHandle,
    parameter: BaseTiledMapLoader.Parameters
  ): TiledMap = {
    val param = Nullable(parameter)
    param.foreach { p =>
      setTextureFilters(p.textureMinFilter, p.textureMagFilter)
    }

    map
  }

  override protected def getDependencyAssetDescriptors(
    tmxFile:          FileHandle,
    textureParameter: TextureLoader.TextureParameter
  ): DynamicArray[AssetDescriptor[?]] = {
    val descriptors = DynamicArray[AssetDescriptor[?]]()

    // Atlas dependencies
    val atlasFileHandle = getAtlasFileHandle(tmxFile)
    Nullable(atlasFileHandle).foreach { handle =>
      descriptors.add(new AssetDescriptor[TextureAtlas](handle, classOf[TextureAtlas]))
    }

    descriptors
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

    val atlas = atlasResolver.getAtlas()

    for (texture <- atlas.getTextures())
      trackedTextures.add(texture)

    val props = tileSet.getProperties
    props.put("imagesource", imageSource)
    props.put("imagewidth", imageWidth:   java.lang.Integer)
    props.put("imageheight", imageHeight: java.lang.Integer)
    props.put("tilewidth", tilewidth:     java.lang.Integer)
    props.put("tileheight", tileheight:   java.lang.Integer)
    props.put("margin", margin:           java.lang.Integer)
    props.put("spacing", spacing:         java.lang.Integer)

    if (Nullable(imageSource).isDefined && imageSource.nonEmpty) {
      val lastgid = firstgid + ((imageWidth / tilewidth) * (imageHeight / tileheight)) - 1
      for (region <- atlas.findRegions(name))
        // Handle unused tileIds
        if (Nullable(region).isDefined) {
          val tileId = firstgid + region.index
          if (tileId >= firstgid && tileId <= lastgid) {
            addStaticTiledMapTile(tileSet, region, tileId, offsetX.toFloat, offsetY.toFloat)
          }
        }
    }

    // Add tiles with individual image sources
    for (tileElement <- tileElements) {
      val tileId = firstgid + tileElement.getIntAttribute("id", 0)
      val tile   = tileSet.getTile(tileId)
      if (tile.isEmpty) {
        val imageElement = tileElement.getChildByName("image")
        imageElement.foreach { ie =>
          var regionName = ie.getAttribute("source")
          regionName = regionName.substring(0, regionName.lastIndexOf('.'))
          val region = atlas.findRegion(regionName)
          if (region.isEmpty) throw new IllegalArgumentException("Tileset atlasRegion not found: " + regionName)
          region.foreach(r => addStaticTiledMapTile(tileSet, r, tileId, offsetX.toFloat, offsetY.toFloat))
        }
      }
    }
  }

  protected def getAtlasFileHandle(tmxFile: FileHandle): FileHandle = {
    val properties = root.getOrElse(throw new IllegalStateException("root not initialized")).getChildByName("properties")

    var atlasFilePath: Nullable[String] = Nullable.empty
    properties.foreach { props =>
      val propertyElements = props.getChildrenByName("property")
      var pi               = 0
      while (pi < propertyElements.size) {
        val property = propertyElements(pi)
        val name     = property.getAttribute("name")
        if (name.startsWith("atlas")) {
          atlasFilePath = Nullable(property.getAttribute("value"))
        }
        pi += 1
      }
    }
    if (atlasFilePath.isEmpty) {
      throw new IllegalArgumentException("The map is missing the 'atlas' property")
    } else {
      val path       = atlasFilePath.getOrElse("")
      val fileHandle = BaseTiledMapLoader.getRelativeFileHandle(tmxFile, path)
      if (!fileHandle.exists()) {
        throw new IllegalArgumentException("The 'atlas' file could not be found: '" + path + "'")
      }
      fileHandle
    }
  }

  protected def setTextureFilters(min: Texture.TextureFilter, mag: Texture.TextureFilter): Unit = {
    for (texture <- trackedTextures)
      texture.setFilter(min, mag)
    trackedTextures.clear()
  }
}

object AtlasTmxMapLoader {

  trait AtlasResolver extends ImageResolver {
    def getAtlas(): TextureAtlas
  }

  class DirectAtlasResolver(atlas: TextureAtlas) extends AtlasResolver {
    override def getAtlas(): TextureAtlas = atlas

    override def getImage(name: String): Nullable[TextureRegion] = {
      // check for imagelayer and strip if needed
      val regionName = AtlasTmjMapLoader.parseRegionName(name)
      atlas.findRegion(regionName).map(r => r: TextureRegion)
    }
  }

  class AssetManagerAtlasResolver(assetManager: AssetManager, atlasName: String) extends AtlasResolver {
    override def getAtlas(): TextureAtlas =
      assetManager.get(atlasName, classOf[TextureAtlas])

    override def getImage(name: String): Nullable[TextureRegion] = {
      // check for imagelayer and strip if needed
      val regionName = AtlasTmjMapLoader.parseRegionName(name)
      getAtlas().findRegion(regionName).map(r => r: TextureRegion)
    }
  }
}
