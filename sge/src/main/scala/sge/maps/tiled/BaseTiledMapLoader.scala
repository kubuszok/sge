/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/BaseTiledMapLoader.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audited 2026-03-03):
 *   - All methods, fields, inner types (Parameters, ProjectClassMember) match Java 1:1
 *   - Java IntMap<MapObject> → mutable.HashMap[Int, MapObject]
 *   - Java Array<Runnable> → DynamicArray[() => Unit]
 *   - Java ObjectMap<String, Array<ProjectClassMember>> → ObjectMap[String, DynamicArray[...]]
 *   - Java null fields → Nullable[A]; early returns → boundary/break
 *   - Java GdxRuntimeException → IllegalArgumentException/IllegalStateException
 *   - loadProjectFile: extra foreach nesting matches Java continue-based loop
 *   - loadObjectProperty: Java Runnable → Scala () => Unit lambda
 *   - Fixed: loadProjectFile now calls `projectClassMembers.add(projectClassMember)` inside for loop
 *   - Minor: Javadoc for tiledColorToLibGDXColor is misplaced above loadMapPropertiesClassDefaults
 *   - Split package, braces, no-return conventions satisfied
 *   - JsonValue tree walking replaced with jsoniter-scala codec derivation (TiledProjectJson)
 *   Convention: jsoniter-scala codec derivation replaces JsonValue tree walking
 *   Audited: 2026-03-04
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 403
 * Covenant-baseline-methods: BaseTiledMapLoader,FLAG_FLIP_DIAGONALLY,FLAG_FLIP_HORIZONTALLY,FLAG_FLIP_VERTICALLY,MASK_CLEAR,Parameters,ProjectClassMember,addStaticTiledMapTile,alpha,castProperty,castValue,cell,classInfo,classMembers,color,convertObjectToTileSpace,createTileLayerCell,defaultValue,flipY,forceTextureFilters,generateMipMaps,getDependencyAssetDescriptors,getIdToObject,getRelativeFileHandle,idToObject,jsonAsString,jsonGetField,loadBasicProperty,loadJsonClassProperties,loadMapPropertiesClassDefaults,loadObjectProperty,loadProjectFile,loadTiledMap,map,mapHeightInPixels,mapTileHeight,mapTileWidth,mapWidthInPixels,members,name,pci,project,projectClassInfo,projectFile,projectFilePath,propertyType,result,runOnEndOfLoadTiled,t,textureMagFilter,textureMinFilter,tile,tiledColorToLibGDXColor,toString,tokens,unsignedByteToInt
 * Covenant-source-reference: com/badlogic/gdx/maps/tiled/BaseTiledMapLoader.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 323c1515ddf3855b6706caf47050acd9d5f7a057
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
import sge.utils.{ DynamicArray, Json, Nullable, ObjectMap, readJson }

import scala.collection.mutable
import scala.util.boundary
import scala.util.boundary.break

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
    val t = `type`.getOrElse("string")
    if (t == "string" || t == "file") value
    else if (t == "int") Integer.valueOf(value)
    else if (t == "float") java.lang.Float.valueOf(value)
    else if (t == "bool") java.lang.Boolean.valueOf(value)
    else if (t == "color")
      // produce color after converting from #AARRGGBB to #RRGGBBAA
      Color.valueOf(BaseTiledMapLoader.tiledColorToLibGDXColor(value))
    else
      throw new IllegalArgumentException(
        "Wrong type given for property " + name + ", given : " + t
          + ", supported : string, file, bool, int, float, color"
      )
  }

  protected def createTileLayerCell(
    flipHorizontally: Boolean,
    flipVertically:   Boolean,
    flipDiagonally:   Boolean
  ): TiledMapTileLayer.Cell = {
    val cell = TiledMapTileLayer.Cell()
    if (flipDiagonally) {
      if (flipHorizontally && flipVertically) {
        cell.flipHorizontally = true
        cell.rotation = TiledMapTileLayer.Cell.ROTATE_270
      } else if (flipHorizontally) {
        cell.rotation = TiledMapTileLayer.Cell.ROTATE_270
      } else if (flipVertically) {
        cell.rotation = TiledMapTileLayer.Cell.ROTATE_90
      } else {
        cell.flipVertically = true
        cell.rotation = TiledMapTileLayer.Cell.ROTATE_270
      }
    } else {
      cell.flipHorizontally = flipHorizontally
      cell.flipVertically = flipVertically
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
    val tile: TiledMapTile = StaticTiledMapTile(textureRegion)
    tile.id = tileId
    tile.offsetX = offsetX
    tile.offsetY = if (flipY) -offsetY else offsetY
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
  protected def loadProjectFile(projectFilePath: Nullable[String]): Unit = boundary {
    val classInfo = ObjectMap[String, DynamicArray[BaseTiledMapLoader.ProjectClassMember]]()
    projectClassInfo = Nullable(classInfo)
    if (projectFilePath.isEmpty || projectFilePath.getOrElse("").trim.isEmpty) {
      break()
    }

    val projectFile = resolve(projectFilePath.getOrElse(""))
    val project     = projectFile.readJson[TiledProjectJson]

    if (project.propertyTypes.isEmpty) {
      // no custom property types in project -> nothing to parse
      break()
    }

    for (propertyType <- project.propertyTypes)
      if ("class" == propertyType.tpe) {
        val className = propertyType.name
        if (propertyType.members.nonEmpty) {
          val projectClassMembers = DynamicArray[BaseTiledMapLoader.ProjectClassMember]()
          classInfo.put(className, projectClassMembers)
          for (member <- propertyType.members) {
            val projectClassMember = BaseTiledMapLoader.ProjectClassMember()
            projectClassMember.name = member.name
            projectClassMember.`type` = member.tpe
            projectClassMember.propertyType = member.propertyType match {
              case Some(s) => Nullable(s)
              case None    => Nullable.empty
            }
            projectClassMember.defaultValue = member.value match {
              case Some(v) => Nullable(v)
              case None    => Nullable.empty
            }
            projectClassMembers.add(projectClassMember)
          }
        }
      }
  }

  protected def loadJsonClassProperties(
    className:       String,
    classProperties: MapProperties,
    classElement:    Nullable[Json]
  ): Unit = {
    val pci = projectClassInfo.getOrElse(
      throw new IllegalStateException(
        "No class information loaded to support class properties. Did you set the 'projectFilePath' parameter?"
      )
    )
    if (pci.isEmpty) {
      throw new IllegalStateException(
        "No class information available. Did you set the correct Tiled project path in the 'projectFilePath' parameter?"
      )
    }
    val members = pci
      .get(className)
      .getOrElse(
        throw new IllegalStateException(
          "There is no class with name '" + className + "' in given Tiled project file."
        )
      )

    for (projectClassMember <- members) {
      val propName     = projectClassMember.name
      val classPropRaw = classElement.flatMap(ce => BaseTiledMapLoader.jsonGetField(ce, propName))
      projectClassMember.`type` match {
        case "object" =>
          val value =
            if (classPropRaw.isEmpty) projectClassMember.defaultValue.flatMap(BaseTiledMapLoader.jsonAsString)
            else classPropRaw.flatMap(BaseTiledMapLoader.jsonAsString)
          value.foreach(v => loadObjectProperty(classProperties, propName, v))
        case "class" =>
          // A 'class' property is a property which is itself a set of properties
          val classProp             = if (classPropRaw.isEmpty) projectClassMember.defaultValue else classPropRaw
          val nestedClassProperties = MapProperties()
          val nestedClassName       = projectClassMember.propertyType.getOrElse("")
          nestedClassProperties.put("type", nestedClassName)
          // the actual properties of a 'class' property are stored as a new properties tag
          classProperties.put(propName, nestedClassProperties)
          loadJsonClassProperties(nestedClassName, nestedClassProperties, classProp)
        case _ =>
          val value =
            if (classPropRaw.isEmpty) projectClassMember.defaultValue.flatMap(BaseTiledMapLoader.jsonAsString)
            else classPropRaw.flatMap(BaseTiledMapLoader.jsonAsString)
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
  protected def loadMapPropertiesClassDefaults(className: Nullable[String], mapProperties: MapProperties): Unit = boundary {
    if (projectClassInfo.isEmpty) {
      System.err.println(
        "[TiledMapLoader] WARN: There is at least one property of type class or an object with a class defined. "
          + "Use the 'projectFilePath' parameter to correctly load the default values of a class."
      )
      // to avoid spamming the warning message we can set an empty ObjectMap as projectClassInfo
      this.projectClassInfo = Nullable(ObjectMap[String, DynamicArray[BaseTiledMapLoader.ProjectClassMember]]())
      break()
    }
    val pci = projectClassInfo.getOrElse(throw new IllegalStateException("unreachable"))
    if (className.isEmpty || !pci.containsKey(className.getOrElse(""))) {
      break()
    }

    val classMembers = pci.get(className.getOrElse(""))
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
            val nestedClassProperties = MapProperties()
            val nestedClassName       = classMember.propertyType.getOrElse("")
            nestedClassProperties.put("type", nestedClassName)
            mapProperties.put(propName, nestedClassProperties)
            loadJsonClassProperties(nestedClassName, nestedClassProperties, classMember.defaultValue)
          } else {
            classMember.defaultValue.foreach { dv =>
              val value = BaseTiledMapLoader.jsonAsString(dv)
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
    var name:         String           = scala.compiletime.uninitialized
    var `type`:       String           = scala.compiletime.uninitialized
    var propertyType: Nullable[String] = Nullable.empty
    var defaultValue: Nullable[Json]   = Nullable.empty

    override def toString: String =
      "ProjectClassMember{" +
        "name='" + name + "'" +
        ", type='" + `type` + "'" +
        ", propertyType='" + propertyType.getOrElse("") + "'" +
        ", defaultValue=" + defaultValue.map(_.toString).getOrElse("") + "}"
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

  /** Looks up a field in a JSON object by name. */
  protected[tiled] def jsonGetField(json: Json, name: String): Nullable[Json] = json match {
    case Json.Obj(fields) =>
      var result: Nullable[Json] = Nullable.empty
      fields.fields.foreach { case (k, v) =>
        if (k == name) result = Nullable(v)
      }
      result
    case _ => Nullable.empty
  }

  /** Extracts a string representation from a simple JSON value. */
  protected[tiled] def jsonAsString(json: Json): Nullable[String] = json match {
    case Json.Str(s)  => Nullable(s)
    case Json.Num(n)  => Nullable(n.value)
    case Json.Bool(b) => Nullable(b.toString)
    case _            => Nullable.empty
  }
}
