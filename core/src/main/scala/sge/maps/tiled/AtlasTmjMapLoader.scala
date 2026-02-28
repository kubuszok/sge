/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/AtlasTmjMapLoader.java
 * Original authors: Justin Shapcott, Manuel Bua
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
import sge.graphics.g2d.{ TextureAtlas, TextureRegion }
import sge.utils.{ DynamicArray, JsonValue, Nullable }

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
class AtlasTmjMapLoader(resolver: FileHandleResolver)(using sge: Sge) extends BaseTmjMapLoader[BaseTiledMapLoader.Parameters](resolver) {

  protected var trackedTextures: DynamicArray[Texture] = DynamicArray[Texture]()

  protected var atlasResolver: AtlasTmjMapLoader.AtlasResolver = scala.compiletime.uninitialized

  def this()(using sge: Sge) = this(new InternalFileHandleResolver())

  def load(fileName: String): TiledMap =
    load(fileName, new BaseTiledMapLoader.Parameters())

  def load(fileName: String, parameter: BaseTiledMapLoader.Parameters): TiledMap = {
    val tmjFile = resolve(fileName)

    this.root = Nullable(json.parse(tmjFile))

    val atlasFileHandle = getAtlasFileHandle(tmjFile)
    val atlas           = new TextureAtlas(atlasFileHandle)
    this.atlasResolver = new AtlasTmjMapLoader.DirectAtlasResolver(atlas)

    val map            = loadTiledMap(tmjFile, parameter, atlasResolver)
    val ownedResources = DynamicArray[AutoCloseable]()
    ownedResources.add(atlas)
    map.setOwnedResources(ownedResources)
    setTextureFilters(parameter.textureMinFilter, parameter.textureMagFilter)
    map
  }

  override def loadAsync(
    manager:   AssetManager,
    fileName:  String,
    tmjFile:   FileHandle,
    parameter: BaseTiledMapLoader.Parameters
  ): Unit = {
    val atlasHandle = getAtlasFileHandle(tmjFile)
    this.atlasResolver = new AtlasTmjMapLoader.AssetManagerAtlasResolver(manager, atlasHandle.path())

    this.map = loadTiledMap(tmjFile, parameter, atlasResolver)
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
    for (tileElement <- tiles) {
      val tileId = firstgid + tileElement.getInt("id", 0)
      val tile   = tileSet.getTile(tileId)
      if (tile.isEmpty) {
        val imageElement = tileElement.get("image")
        imageElement.foreach { ie =>
          var regionName = ie.asString().orNull
          regionName = regionName.substring(0, regionName.lastIndexOf('.'))
          val region = atlas.findRegion(regionName)
          if (region.isEmpty) throw new IllegalArgumentException("Tileset atlasRegion not found: " + regionName)
          region.foreach(r => addStaticTiledMapTile(tileSet, r, tileId, offsetX.toFloat, offsetY.toFloat))
        }
      }
    }
  }

  protected def getAtlasFileHandle(tmjFile: FileHandle): FileHandle = {
    val properties = root.orNull.get("properties")

    var atlasFilePath: Nullable[String] = Nullable.empty
    properties.foreach { props =>
      for (property <- props) {
        val name = property.getString("name", Nullable("")).orNull
        if (name.startsWith("atlas")) {
          atlasFilePath = property.getString("value", Nullable(""))
        }
      }
    }

    if (atlasFilePath.isEmpty || atlasFilePath.orNull.isEmpty) {
      throw new IllegalArgumentException("The map is missing the 'atlas' property")
    } else {
      val fileHandle = BaseTiledMapLoader.getRelativeFileHandle(tmjFile, atlasFilePath.orNull)
      if (!fileHandle.exists()) {
        throw new IllegalArgumentException("The 'atlas' file could not be found: '" + atlasFilePath.orNull + "'")
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

object AtlasTmjMapLoader {

  trait AtlasResolver extends ImageResolver {
    def getAtlas(): TextureAtlas
  }

  class DirectAtlasResolver(atlas: TextureAtlas) extends AtlasResolver {
    override def getAtlas(): TextureAtlas = atlas

    override def getImage(name: String): Nullable[TextureRegion] = {
      // check for imagelayer and strip if needed
      val regionName = parseRegionName(name)
      atlas.findRegion(regionName).map(r => r: TextureRegion)
    }
  }

  class AssetManagerAtlasResolver(assetManager: AssetManager, atlasName: String) extends AtlasResolver {
    override def getAtlas(): TextureAtlas =
      assetManager.get(atlasName, classOf[TextureAtlas])

    override def getImage(name: String): Nullable[TextureRegion] = {
      // check for imagelayer and strip if needed
      val regionName = parseRegionName(name)
      getAtlas().findRegion(regionName).map(r => r: TextureRegion)
    }
  }

  /** Parse incoming region name to check for 'atlas_imagelayer' within the String These are regions representing Image Layers that have been packed into the atlas ImageLayer Image names include the
    * relative assets path, so it must be stripped.
    * @param name
    *   Name to check
    * @return
    *   The name of the region to pass into an atlas
    */
  def parseRegionName(name: String): String =
    if (name.contains("atlas_imagelayer")) {
      // Find the last '/' in the path
      val lastSlash = name.lastIndexOf('/')
      // If we found a slash, return everything after it which should be our region name
      // If no slashes found return entire string
      if (lastSlash >= 0) name.substring(lastSlash + 1) else name
    } else {
      name
    }
}
