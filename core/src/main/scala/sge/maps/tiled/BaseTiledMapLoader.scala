/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/BaseTiledMapLoader.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package maps
package tiled

import sge.assets.AssetDescriptor
import sge.assets.loaders.{ AsynchronousAssetLoader, FileHandleResolver, TextureLoader }
import sge.files.FileHandle
import sge.graphics.Color
import sge.graphics.g2d.TextureRegion
import sge.maps.tiled.tiles.StaticTiledMapTile
import sge.utils.{ JsonReader, JsonValue, Nullable, ObjectMap }

import sge.utils.DynamicArray

import scala.collection.mutable

abstract class BaseTiledMapLoader[P <: BaseTiledMapLoader.Parameters](resolver: FileHandleResolver) extends AsynchronousAssetLoader[TiledMap, P](resolver) {

  protected var convertObjectToTileSpace: Boolean = false
  protected var flipY:                    Boolean = true

  protected var mapTileWidth:      Int = 0
  protected var mapTileHeight:     Int = 0
  protected var mapWidthInPixels:  Int = 0
  protected var mapHeightInPixels: Int = 0

  protected var map:                 TiledMap                        = scala.compiletime.uninitialized
  protected var idToObject:          mutable.HashMap[Int, MapObject] = scala.compiletime.uninitialized
  protected var runOnEndOfLoadTiled: DynamicArray[() => Unit]        = scala.compiletime.uninitialized

  /** Optional Tiled project class information. Key is the classname and value is an array of class members (=class properties)
    */
  protected var projectClassInfo: Nullable[ObjectMap[String, DynamicArray[BaseTiledMapLoader.ProjectClassMember]]] =
    Nullable.empty

  /** Meant to be called within getDependencies() of a child class */
  protected def getDependencyAssetDescriptors(
    mapFile:          FileHandle,
    textureParameter: TextureLoader.TextureParameter
  ): DynamicArray[AssetDescriptor[?]]

  /** Loads the map data, given the root element
    *
    * @param mapFile
    *   the Filehandle of the map file, .tmx or .tmj supported
    * @param parameter
    * @param imageResolver
    * @return
    *   the [[TiledMap]]
    */
  protected def loadTiledMap(mapFile: FileHandle, parameter: P, imageResolver: ImageResolver): TiledMap

  /** Gets a map of the object ids to the [[MapObject]] instances. Returns null if [[loadTiledMap]] has not been called yet.
    *
    * @return
    *   the map of the ids to [[MapObject]], or null if [[loadTiledMap]] method has not been called yet.
    */
  def getIdToObject: Nullable[mutable.HashMap[Int, MapObject]] = Nullable(idToObject)

  protected def castProperty(name: String, value: String, `type`: Nullable[String]): Any = {
    val t = `type`
    if (t.isEmpty || t.orNull == "string" || t.orNull == "file") value
    else if (t.orNull == "int") Integer.valueOf(value)
    else if (t.orNull == "float") java.lang.Float.valueOf(value)
    else if (t.orNull == "bool") java.lang.Boolean.valueOf(value)
    else if (t.orNull == "color")
      // return color after converting from #AARRGGBB to #RRGGBBAA
      Color.valueOf(BaseTiledMapLoader.tiledColorToLibGDXColor(value))
    else
      throw new IllegalArgumentException(
        "Wrong type given for property " + name + ", given : " + t.orNull
          + ", supported : string, file, bool, int, float, color"
      )
  }

  protected def createTileLayerCell(
    flipHorizontally: Boolean,
    flipVertically:   Boolean,
    flipDiagonally:   Boolean
  ): TiledMapTileLayer.Cell = {
    val cell = new TiledMapTileLayer.Cell()
    if (flipDiagonally) {
      if (flipHorizontally && flipVertically) {
        cell.setFlipHorizontally(true)
        cell.setRotation(TiledMapTileLayer.Cell.ROTATE_270)
      } else if (flipHorizontally) {
        cell.setRotation(TiledMapTileLayer.Cell.ROTATE_270)
      } else if (flipVertically) {
        cell.setRotation(TiledMapTileLayer.Cell.ROTATE_90)
      } else {
        cell.setFlipVertically(true)
        cell.setRotation(TiledMapTileLayer.Cell.ROTATE_270)
      }
    } else {
      cell.setFlipHorizontally(flipHorizontally)
      cell.setFlipVertically(flipVertically)
    }
    cell
  }

  protected def addStaticTiledMapTile(
    tileSet:       TiledMapTileSet,
    textureRegion: TextureRegion,
    tileId:        Int,
    offsetX:       Float,
    offsetY:       Float
  ): Unit = {
    val tile: TiledMapTile = new StaticTiledMapTile(textureRegion)
    tile.setId(tileId)
    tile.setOffsetX(offsetX)
    tile.setOffsetY(if (flipY) -offsetY else offsetY)
    tileSet.putTile(tileId, tile)
  }

  protected def loadObjectProperty(properties: MapProperties, name: String, value: String): Unit =
    // Wait until the end of [loadTiledMap] to fetch the object
    try {
      // Value should be the id of the object being pointed to
      val id = Integer.parseInt(value)
      // Create closure to fetch object and add it to props
      val fetch: () => Unit = () => {
        val obj = idToObject.get(id)
        obj.foreach(o => properties.put(name, o))
      }
      // closure should not run until the end of [loadTiledMap]
      runOnEndOfLoadTiled.add(fetch)
    } catch {
      case exception: Exception =>
        throw new IllegalArgumentException(
          "Error parsing property [" + name + "] of type \"object\" with value: [" + value + "]",
          exception
        )
    }

  protected def loadBasicProperty(properties: MapProperties, name: String, value: String, `type`: Nullable[String]): Unit = {
    val castValue = castProperty(name, value, `type`)
    properties.put(name, castValue)
  }

  /** Parses the given Tiled project file for class property information. A class can have multiple members. Refer to [[BaseTiledMapLoader.ProjectClassMember]].
    */
  protected def loadProjectFile(projectFilePath: Nullable[String]): Unit = {
    val classInfo = ObjectMap[String, DynamicArray[BaseTiledMapLoader.ProjectClassMember]]()
    projectClassInfo = Nullable(classInfo)
    if (projectFilePath.isEmpty || projectFilePath.orNull.trim.isEmpty) {
      return // scalastyle:ignore
    }

    val projectFile   = resolve(projectFilePath.orNull)
    val projectRoot   = new JsonReader().parse(projectFile)
    val propertyTypes = projectRoot.get("propertyTypes")
    if (propertyTypes.isEmpty) {
      // no custom property types in project -> nothing to parse
      return // scalastyle:ignore
    }

    for (propertyType <- propertyTypes.orNull)
      if ("class" == propertyType.getString("type", Nullable.empty).orNull) {
        val className = propertyType.getString("name", Nullable.empty).orNull
        val members   = propertyType.get("members")
        if (members.isDefined && !members.orNull.isEmpty) {
          val projectClassMembers = DynamicArray[BaseTiledMapLoader.ProjectClassMember]()
          classInfo.put(className, projectClassMembers)
          for (member <- members.orNull) {
            val projectClassMember = new BaseTiledMapLoader.ProjectClassMember()
            projectClassMember.name = member.getString("name", Nullable.empty).orNull
            projectClassMember.`type` = member.getString("type", Nullable.empty).orNull
            projectClassMember.propertyType = member.getString("propertyType", Nullable.empty)
            projectClassMember.defaultValue = member.get("value")
          }
        }
      }
  }

  protected def loadJsonClassProperties(
    className:       String,
    classProperties: MapProperties,
    classElement:    Nullable[JsonValue]
  ): Unit = {
    if (projectClassInfo.isEmpty) {
      throw new IllegalStateException(
        "No class information loaded to support class properties. Did you set the 'projectFilePath' parameter?"
      )
    }
    if (projectClassInfo.orNull.isEmpty) {
      throw new IllegalStateException(
        "No class information available. Did you set the correct Tiled project path in the 'projectFilePath' parameter?"
      )
    }
    val projectClassMembers = projectClassInfo.orNull.get(className)
    if (projectClassMembers.isEmpty) {
      throw new IllegalStateException(
        "There is no class with name '" + className + "' in given Tiled project file."
      )
    }

    for (projectClassMember <- projectClassMembers.orNull) {
      val propName     = projectClassMember.name
      val classPropRaw = classElement.flatMap(_.get(propName))
      projectClassMember.`type` match {
        case "object" =>
          val value =
            if (classPropRaw.isEmpty) projectClassMember.defaultValue.flatMap(_.asString())
            else classPropRaw.flatMap(_.asString())
          value.foreach(v => loadObjectProperty(classProperties, propName, v))
        case "class" =>
          // A 'class' property is a property which is itself a set of properties
          val classProp             = if (classPropRaw.isEmpty) projectClassMember.defaultValue else classPropRaw
          val nestedClassProperties = new MapProperties()
          val nestedClassName       = projectClassMember.propertyType.orNull
          nestedClassProperties.put("type", nestedClassName)
          // the actual properties of a 'class' property are stored as a new properties tag
          classProperties.put(propName, nestedClassProperties)
          loadJsonClassProperties(nestedClassName, nestedClassProperties, classProp)
        case _ =>
          val value =
            if (classPropRaw.isEmpty) projectClassMember.defaultValue.flatMap(_.asString())
            else classPropRaw.flatMap(_.asString())
          value.foreach(v => loadBasicProperty(classProperties, propName, v, Nullable(projectClassMember.`type`)))
      }
    }
  }

  /** Converts Tiled's color format #AARRGGBB to a libGDX appropriate #RRGGBBAA The Tiled Map Editor uses the color format #AARRGGBB But note, if the alpha of the color is set to 255, Tiled does not
    * include it as part of the color code in the .tmx ex. Red (r:255,g:0,b:0,a:255) becomes #ff0000, Red (r:255,g:0,b:0,a:127) becomes #7fff0000
    *
    * @param tiledColor
    *   A String representing a color in Tiled's #AARRGGBB format
    * @return
    *   A String representing the color in the #RRGGBBAA format
    */
  protected def loadMapPropertiesClassDefaults(className: Nullable[String], mapProperties: MapProperties): Unit = {
    if (projectClassInfo.isEmpty) {
      System.err.println(
        "[TiledMapLoader] WARN: There is at least one property of type class or an object with a class defined. "
          + "Use the 'projectFilePath' parameter to correctly load the default values of a class."
      )
      // to avoid spamming the warning message we can set an empty ObjectMap as projectClassInfo
      this.projectClassInfo = Nullable(ObjectMap[String, DynamicArray[BaseTiledMapLoader.ProjectClassMember]]())
      return // scalastyle:ignore
    }
    if (className.isEmpty || !projectClassInfo.orNull.containsKey(className.orNull)) {
      return // scalastyle:ignore
    }

    val classMembers = projectClassInfo.orNull.get(className.orNull)
    classMembers.foreach { members =>
      for (classMember <- members) {
        val propName = classMember.name
        if (!mapProperties.containsKey(propName)) {
          if ("class" == classMember.`type`) {
            // Load default class values.
            // This happens e.g. if a user has a "project class" that has another class property (=nested classes)
            // and assigns it as a "class" to an object/tile without overruling any of its values.
            // In that case we need to load the default values of the class
            // which are stored inside the 'projectClassInfo' field.
            val nestedClassProperties = new MapProperties()
            val nestedClassName       = classMember.propertyType.orNull
            nestedClassProperties.put("type", nestedClassName)
            mapProperties.put(propName, nestedClassProperties)
            loadJsonClassProperties(classMember.propertyType.orNull, nestedClassProperties, classMember.defaultValue)
          } else {
            classMember.defaultValue.foreach { dv =>
              val value = dv.asString()
              value.foreach { v =>
                if ("object" == classMember.`type`) {
                  loadObjectProperty(mapProperties, propName, v)
                } else {
                  loadBasicProperty(mapProperties, propName, v, Nullable(classMember.`type`))
                }
              }
            }
          }
        }
      }
    }
  }
}

object BaseTiledMapLoader {

  class Parameters extends sge.assets.AssetLoaderParameters[TiledMap] {

    /** generate mipmaps? */
    var generateMipMaps: Boolean = false

    /** The TextureFilter to use for minification */
    var textureMinFilter: sge.graphics.Texture.TextureFilter = sge.graphics.Texture.TextureFilter.Nearest

    /** The TextureFilter to use for magnification */
    var textureMagFilter: sge.graphics.Texture.TextureFilter = sge.graphics.Texture.TextureFilter.Nearest

    /** Whether to convert the objects' pixel position and size to the equivalent in tile space. */
    var convertObjectToTileSpace: Boolean = false

    /** Whether to flip all Y coordinates so that Y positive is up. All libGDX renderers require flipped Y coordinates, and thus flipY set to true. This parameter is included for non-rendering related
      * purposes of TMX files, or custom renderers.
      */
    var flipY: Boolean = true

    /** Path to Tiled project file. Needed when using class properties. */
    var projectFilePath: Nullable[String] = Nullable.empty

    /** force texture filters? */
    var forceTextureFilters: Boolean = false
  }

  /** Representation of a single Tiled class property. A property has:
    *   - a property `name`
    *   - a property `type` like string, int, ...
    *   - an optional `propertyType` for class and enum types to refer to a specific class/enum
    *   - a `defaultValue`
    */
  class ProjectClassMember {
    var name:         String              = scala.compiletime.uninitialized
    var `type`:       String              = scala.compiletime.uninitialized
    var propertyType: Nullable[String]    = Nullable.empty
    var defaultValue: Nullable[JsonValue] = Nullable.empty

    override def toString: String =
      "ProjectClassMember{" +
        "name='" + name + "'" +
        ", type='" + `type` + "'" +
        ", propertyType='" + propertyType.orNull + "'" +
        ", defaultValue=" + defaultValue.orNull + "}"
  }

  protected[tiled] val FLAG_FLIP_HORIZONTALLY: Int = 0x80000000
  protected[tiled] val FLAG_FLIP_VERTICALLY:   Int = 0x40000000
  protected[tiled] val FLAG_FLIP_DIAGONALLY:   Int = 0x20000000
  protected[tiled] val MASK_CLEAR:             Int = 0xe0000000

  protected[tiled] def unsignedByteToInt(b: Byte): Int = b & 0xff

  protected[tiled] def getRelativeFileHandle(file: FileHandle, path: String): FileHandle = {
    val tokens = path.split("[/\\\\]+")
    var result = file.parent()
    for (token <- tokens)
      if (token == "..")
        result = result.parent()
      else
        result = result.child(token)
    result
  }

  /** Converts Tiled's color format #AARRGGBB to a libGDX appropriate #RRGGBBAA */
  def tiledColorToLibGDXColor(tiledColor: String): String = {
    val alpha = if (tiledColor.length == 9) tiledColor.substring(1, 3) else "ff"
    val color = if (tiledColor.length == 9) tiledColor.substring(3) else tiledColor.substring(1)
    color + alpha
  }
}
