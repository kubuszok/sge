/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/AtlasTmjMapLoader.java
 * Original authors: Justin Shapcott, Manuel Bua
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audited 2026-03-03):
 *   - All methods match Java 1:1 (load, loadAsync, loadSync, getDependencyAssetDescriptors,
 *     addStaticTiles, getAtlasFileHandle, setTextureFilters, parseRegionName)
 *   - Java inner interface AtlasResolver + 2 inner classes → companion object trait + classes
 *   - parseRegionName placed in companion object (Java: static package-private method)
 *   - Java AtlasRegion return → Nullable[TextureRegion] via .map(r => r: TextureRegion)
 *   - Constructor requires `(using Sge)` (SGE context parameter)
 *   - Split package, braces, no-return conventions satisfied
 *
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
import sge.graphics.g2d.{ TextureAtlas, TextureRegion }
import sge.utils.{ DynamicArray, Nullable, readJson }

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
class AtlasTmjMapLoader(resolver: FileHandleResolver)(using Sge) extends BaseTmjMapLoader[BaseTiledMapLoader.Parameters](resolver) {

  def this()(using Sge) = this(FileHandleResolver.Internal())

  protected var trackedTextures: DynamicArray[Texture] = DynamicArray[Texture]()

  protected var atlasResolver: AtlasTmjMapLoader.AtlasResolver = scala.compiletime.uninitialized

  def load(fileName: String): TiledMap =
    load(fileName, BaseTiledMapLoader.Parameters())

  def load(fileName: String, parameter: BaseTiledMapLoader.Parameters): TiledMap = {
    val tmjFile = resolve(fileName)

    this.root = Nullable(tmjFile.readJson[TmjMapJson])

    val atlasFileHandle = getAtlasFileHandle(tmjFile)
    val atlas           = TextureAtlas(atlasFileHandle)
    this.atlasResolver = AtlasTmjMapLoader.DirectAtlasResolver(atlas)

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
    this.atlasResolver = AtlasTmjMapLoader.AssetManagerAtlasResolver(manager, atlasHandle.path())

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
      descriptors.add(AssetDescriptor[TextureAtlas](handle))
    }

    descriptors
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

    val atlas = atlasResolver.getAtlas()

    for (texture <- atlas.textures)
      trackedTextures.add(texture)

    val props = tileSet.properties
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
    for (tile <- tiles) {
      val tileId   = firstgid + tile.id
      val existing = tileSet.getTile(tileId)
      if (existing.isEmpty) {
        tile.image.foreach { imgSource =>
          var regionName = imgSource
          regionName = regionName.substring(0, regionName.lastIndexOf('.'))
          val region = atlas.findRegion(regionName)
          if (region.isEmpty) throw new IllegalArgumentException("Tileset atlasRegion not found: " + regionName)
          region.foreach(r => addStaticTiledMapTile(tileSet, r, tileId, offsetX.toFloat, offsetY.toFloat))
        }
      }
    }
  }

  protected def getAtlasFileHandle(tmjFile: FileHandle): FileHandle = {
    val r = root.getOrElse(throw new IllegalStateException("root not initialized"))

    var atlasFilePath: Nullable[String] = Nullable.empty
    for (property <- r.properties)
      if (property.name.startsWith("atlas")) {
        property.value match {
          case sge.utils.Json.Str(s) => atlasFilePath = Nullable(s)
          case _                     => ()
        }
      }

    if (atlasFilePath.isEmpty || atlasFilePath.getOrElse("").isEmpty) {
      throw new IllegalArgumentException("The map is missing the 'atlas' property")
    } else {
      val path       = atlasFilePath.getOrElse("")
      val fileHandle = BaseTiledMapLoader.getRelativeFileHandle(tmjFile, path)
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
      assetManager[TextureAtlas](atlasName)

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
