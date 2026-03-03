/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/BaseTmjMapLoader.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package maps
package tiled

import sge.assets.AssetDescriptor
import sge.assets.loaders.{ FileHandleResolver, TextureLoader }
import sge.files.FileHandle
import sge.graphics.Color
import sge.graphics.g2d.TextureRegion
import sge.maps.objects._
import sge.maps.tiled.objects.TiledMapTileMapObject
import sge.maps.tiled.tiles.{ AnimatedTiledMapTile, StaticTiledMapTile }
import sge.math.{ Polygon, Polyline }
import sge.utils.{ DynamicArray, JsonReader, JsonValue, Nullable, ObjectMap, StreamUtils }

import java.io.{ BufferedInputStream, ByteArrayInputStream, IOException, InputStream }
import java.util.zip.{ GZIPInputStream, InflaterInputStream }
import scala.language.implicitConversions

abstract class BaseTmjMapLoader[P <: BaseTiledMapLoader.Parameters](resolver: FileHandleResolver) extends BaseTiledMapLoader[P](resolver) {

  protected var json: JsonReader          = new JsonReader()
  protected var root: Nullable[JsonValue] = Nullable.empty

  protected var templateCache: ObjectMap[String, JsonValue] = scala.compiletime.uninitialized

  override def getDependencies(
    fileName:  String,
    tmjFile:   FileHandle,
    parameter: P
  ): DynamicArray[AssetDescriptor[?]] = {
    this.root = Nullable(json.parse(tmjFile))

    val textureParameter = new TextureLoader.TextureParameter()
    val param            = Nullable(parameter)
    param.foreach { p =>
      textureParameter.genMipMaps = p.generateMipMaps
      textureParameter.minFilter = p.textureMinFilter
      textureParameter.magFilter = p.textureMagFilter
    }

    getDependencyAssetDescriptors(tmjFile, textureParameter)
  }

  /** Loads the map data, given the JSON root element
    *
    * @param tmjFile
    *   the Filehandle of the tmj file
    * @param parameter
    * @param imageResolver
    * @return
    *   the [[TiledMap]]
    */
  override protected def loadTiledMap(tmjFile: FileHandle, parameter: P, imageResolver: ImageResolver): TiledMap = {
    this.map = new TiledMap()
    this.idToObject = new scala.collection.mutable.HashMap[Int, MapObject]()
    this.runOnEndOfLoadTiled = DynamicArray[() => Unit]()
    this.templateCache = ObjectMap[String, JsonValue]()

    val param = Nullable(parameter)
    param.fold {
      this.convertObjectToTileSpace = false
      this.flipY = true
    } { p =>
      this.convertObjectToTileSpace = p.convertObjectToTileSpace
      this.flipY = p.flipY
      loadProjectFile(p.projectFilePath)
    }

    val r                  = root.getOrElse(throw new IllegalStateException("root not initialized"))
    val mapOrientation     = r.getString("orientation", Nullable.empty)
    val mapWidth           = r.getInt("width", 0)
    val mapHeight          = r.getInt("height", 0)
    val tileWidth          = r.getInt("tilewidth", 0)
    val tileHeight         = r.getInt("tileheight", 0)
    val hexSideLength      = r.getInt("hexsidelength", 0)
    val staggerAxis        = r.getString("staggeraxis", Nullable.empty)
    val staggerIndex       = r.getString("staggerindex", Nullable.empty)
    val mapBackgroundColor = r.getString("backgroundcolor", Nullable.empty)

    val mapProperties = map.getProperties
    mapOrientation.foreach(v => mapProperties.put("orientation", v))
    mapProperties.put("width", mapWidth)
    mapProperties.put("height", mapHeight)
    mapProperties.put("tilewidth", tileWidth)
    mapProperties.put("tileheight", tileHeight)
    mapProperties.put("hexsidelength", hexSideLength)
    staggerAxis.foreach(v => mapProperties.put("staggeraxis", v))
    staggerIndex.foreach(v => mapProperties.put("staggerindex", v))
    mapBackgroundColor.foreach(v => mapProperties.put("backgroundcolor", v))
    this.mapTileWidth = tileWidth
    this.mapTileHeight = tileHeight
    this.mapWidthInPixels = mapWidth * tileWidth
    this.mapHeightInPixels = mapHeight * tileHeight

    mapOrientation.foreach { orient =>
      if ("staggered" == orient) {
        if (mapHeight > 1) {
          this.mapWidthInPixels += tileWidth / 2
          this.mapHeightInPixels = mapHeightInPixels / 2 + tileHeight / 2
        }
      }
    }

    val properties = r.get("properties")
    properties.foreach(p => loadProperties(map.getProperties, p))

    val tileSets = r.get("tilesets")
    tileSets.foreach { ts =>
      for (element <- ts)
        loadTileSet(element, tmjFile, imageResolver)
    }

    val layers = r.get("layers")
    layers.foreach { ls =>
      for (element <- ls)
        loadLayer(map, map.getLayers, element, tmjFile, imageResolver)
    }

    // update hierarchical parallax scrolling factors
    // in Tiled the final parallax scrolling factor of a layer is the multiplication of its factor with all its parents
    // 1) get top level groups
    val groups = map.getLayers.getByType(classOf[MapGroupLayer])
    while (groups.nonEmpty) {
      val group = groups.first
      groups.removeIndex(0)

      for (child <- group.getLayers) {
        child.setParallaxX(child.getParallaxX * group.getParallaxX)
        child.setParallaxY(child.getParallaxY * group.getParallaxY)
        child match {
          case g: MapGroupLayer => groups.add(g)
          case _ =>
        }
      }
    }

    for (runnable <- runOnEndOfLoadTiled)
      runnable()
    runOnEndOfLoadTiled = null // scalastyle:ignore

    map
  }

  protected def loadLayer(
    map:           TiledMap,
    parentLayers:  MapLayers,
    element:       JsonValue,
    tmjFile:       FileHandle,
    imageResolver: ImageResolver
  ): Unit = {
    val layerType = element.getString("type", Nullable(":")).getOrElse(":")
    layerType match {
      case "group"       => loadLayerGroup(map, parentLayers, element, tmjFile, imageResolver)
      case "tilelayer"   => loadTileLayer(map, parentLayers, element)
      case "objectgroup" => loadObjectGroup(map, parentLayers, element, tmjFile)
      case "imagelayer"  => loadImageLayer(map, parentLayers, element, tmjFile, imageResolver)
      case _             =>
    }
  }

  protected def loadLayerGroup(
    map:           TiledMap,
    parentLayers:  MapLayers,
    element:       JsonValue,
    tmjFile:       FileHandle,
    imageResolver: ImageResolver
  ): Unit =
    if (element.getString("type", Nullable(":")).getOrElse(":") == "group") {
      val groupLayer = new MapGroupLayer()
      loadBasicLayerInfo(groupLayer, element)

      val properties = element.get("properties")
      properties.foreach(p => loadProperties(groupLayer.getProperties, p))

      val layers = element.get("layers")
      layers.foreach { ls =>
        for (child <- ls)
          loadLayer(map, groupLayer.getLayers, child, tmjFile, imageResolver)
      }

      for (layer <- groupLayer.getLayers)
        layer.setParent(Nullable(groupLayer))

      parentLayers.add(groupLayer)
    }

  protected def loadTileLayer(map: TiledMap, parentLayers: MapLayers, element: JsonValue): Unit =
    if (element.getString("type", Nullable(":")).getOrElse(":") == "tilelayer") {
      val width      = element.getInt("width", 0)
      val height     = element.getInt("height", 0)
      val tileWidth  = map.getProperties.get("tilewidth", classOf[Integer]).intValue()
      val tileHeight = map.getProperties.get("tileheight", classOf[Integer]).intValue()
      val layer      = new TiledMapTileLayer(width, height, tileWidth, tileHeight)

      loadBasicLayerInfo(layer, element)

      val ids      = BaseTmjMapLoader.getTileIds(element, width, height)
      val tileSets = map.getTileSets
      var y        = 0
      while (y < height) {
        var x = 0
        while (x < width) {
          val id               = ids(y * width + x)
          val flipHorizontally = (id & BaseTiledMapLoader.FLAG_FLIP_HORIZONTALLY) != 0
          val flipVertically   = (id & BaseTiledMapLoader.FLAG_FLIP_VERTICALLY) != 0
          val flipDiagonally   = (id & BaseTiledMapLoader.FLAG_FLIP_DIAGONALLY) != 0

          val tile = tileSets.getTile(id & ~BaseTiledMapLoader.MASK_CLEAR)
          tile.foreach { t =>
            val cell = createTileLayerCell(flipHorizontally, flipVertically, flipDiagonally)
            cell.setTile(Nullable(t))
            layer.setCell(x, if (flipY) height - 1 - y else y, Nullable(cell))
          }
          x += 1
        }
        y += 1
      }
      val properties = element.get("properties")
      properties.foreach(p => loadProperties(layer.getProperties, p))
      parentLayers.add(layer)
    }

  protected def loadObjectGroup(
    map:          TiledMap,
    parentLayers: MapLayers,
    element:      JsonValue,
    tmjFile:      FileHandle
  ): Unit =
    if (element.getString("type", Nullable(":")).getOrElse(":") == "objectgroup") {
      val layer = new MapLayer()
      loadBasicLayerInfo(layer, element)
      val properties = element.get("properties")
      properties.foreach(p => loadProperties(layer.getProperties, p))

      val objects = element.get("objects")
      objects.foreach { objs =>
        for (objectElement <- objs) {
          var elementToLoad = objectElement
          if (objectElement.has("template")) {
            elementToLoad = resolveTemplateObject(map, layer, objectElement, tmjFile)
          }
          loadObject(map, layer, elementToLoad)
        }
      }
      parentLayers.add(layer)
    }

  protected def loadImageLayer(
    map:           TiledMap,
    parentLayers:  MapLayers,
    element:       JsonValue,
    tmjFile:       FileHandle,
    imageResolver: ImageResolver
  ): Unit =
    if (element.getString("type", Nullable(":")).getOrElse(":") == "imagelayer") {
      val x = element.getFloat("offsetx", 0)
      var y = element.getFloat("offsety", 0)
      if (flipY) y = mapHeightInPixels - y

      val imageSrc = element.getString("image", Nullable("")).getOrElse("")

      val repeatX = element.getInt("repeatx", 0) == 1
      val repeatY = element.getInt("repeaty", 0) == 1

      var texture: Nullable[TextureRegion] = Nullable.empty

      if (imageSrc.nonEmpty) {
        val handle = BaseTiledMapLoader.getRelativeFileHandle(tmjFile, imageSrc)
        texture = imageResolver.getImage(handle.path())
        texture.foreach(t => y -= t.getRegionHeight())
      }

      @annotation.nowarn("msg=deprecated") // TiledMapImageLayer takes TextureRegion, not Nullable -- null is valid per original LibGDX API
      val textureValue: TextureRegion = texture.orNull
      val layer = new TiledMapImageLayer(textureValue, x, y, repeatX, repeatY)

      loadBasicLayerInfo(layer, element)

      val properties = element.get("properties")
      properties.foreach(p => loadProperties(layer.getProperties, p))

      parentLayers.add(layer)
    }

  protected def loadBasicLayerInfo(layer: MapLayer, element: JsonValue): Unit = {
    val name      = element.getString("name", Nullable.empty).getOrElse("")
    val opacity   = element.getFloat("opacity", 1.0f)
    val tintColor = element.getString("tintcolor", Nullable("#ffffffff")).getOrElse("#ffffffff")
    val visible   = element.getBoolean("visible", true)
    val offsetX   = element.getFloat("offsetx", 0)
    val offsetY   = element.getFloat("offsety", 0)
    val parallaxX = element.getFloat("parallaxx", 1f)
    val parallaxY = element.getFloat("parallaxy", 1f)

    layer.setName(name)
    layer.setOpacity(opacity)
    layer.setVisible(visible)
    layer.setOffsetX(offsetX)
    layer.setOffsetY(offsetY)
    layer.setParallaxX(parallaxX)
    layer.setParallaxY(parallaxY)

    // set layer tint color after converting from #AARRGGBB to #RRGGBBAA
    layer.setTintColor(Color.valueOf(BaseTiledMapLoader.tiledColorToLibGDXColor(tintColor)))
  }

  protected def loadObject(map: TiledMap, layer: MapLayer, element: JsonValue): Unit =
    loadObject(map, layer.getObjects, element, mapHeightInPixels.toFloat)

  protected def loadObject(map: TiledMap, tile: TiledMapTile, element: JsonValue): Unit =
    loadObject(map, tile.getObjects, element, tile.getTextureRegion.getRegionHeight().toFloat)

  protected def loadObject(map: TiledMap, objects: MapObjects, element: JsonValue, heightInPixels: Float): Unit = {

    var obj: Nullable[MapObject] = Nullable.empty

    val scaleX = if (convertObjectToTileSpace) 1.0f / mapTileWidth else 1.0f
    val scaleY = if (convertObjectToTileSpace) 1.0f / mapTileHeight else 1.0f

    val x = element.getFloat("x", 0) * scaleX
    val y = (if (flipY) heightInPixels - element.getFloat("y", 0) else element.getFloat("y", 0)) * scaleY

    val width  = element.getFloat("width", 0) * scaleX
    val height = element.getFloat("height", 0) * scaleY

    if (element.size > 0) {
      var child: Nullable[JsonValue] = Nullable.empty
      child = element.get("polygon")
      if (child.isDefined) {
        child.foreach { c =>
          val vertices = new Array[Float](c.size * 2)
          var index    = 0
          for (point <- c) {
            vertices(index) = point.getFloat("x", 0) * scaleX
            index += 1
            vertices(index) = point.getFloat("y", 0) * scaleY * (if (flipY) -1 else 1)
            index += 1
          }
          val polygon = new Polygon(vertices)
          polygon.setPosition(x, y)
          obj = Nullable(new PolygonMapObject(polygon))
        }
      } else {
        child = element.get("polyline")
        if (child.isDefined) {
          child.foreach { c =>
            val vertices = new Array[Float](c.size * 2)
            var index    = 0
            for (point <- c) {
              vertices(index) = point.getFloat("x", 0) * scaleX
              index += 1
              vertices(index) = point.getFloat("y", 0) * scaleY * (if (flipY) -1 else 1)
              index += 1
            }
            val polyline = new Polyline(vertices)
            polyline.setPosition(x, y)
            obj = Nullable(new PolylineMapObject(polyline))
          }
        } else if (element.get("ellipse").isDefined) {
          obj = Nullable(new EllipseMapObject(x, if (flipY) y - height else y, width, height))
        } else if (element.get("point").isDefined) {
          obj = Nullable(new PointMapObject(x, if (flipY) y - height else y))
        } else {
          child = element.get("text")
          child.foreach { c =>
            val textMapObject = new TextMapObject(
              x,
              if (flipY) y - height else y,
              width,
              height,
              c.getString("text", Nullable("")).getOrElse("")
            )
            textMapObject.setFontFamily(c.getString("fontfamily", Nullable("")).getOrElse(""))
            textMapObject.setPixelSize(c.getInt("pixelSize", 16))
            textMapObject.setHorizontalAlign(c.getString("halign", Nullable("left")).getOrElse("left"))
            textMapObject.setVerticalAlign(c.getString("valign", Nullable("top")).getOrElse("top"))
            textMapObject.setBold(c.getBoolean("bold", false))
            textMapObject.setItalic(c.getBoolean("italic", false))
            textMapObject.setUnderline(c.getBoolean("underline", false))
            textMapObject.setStrikeout(c.getBoolean("strikeout", false))
            textMapObject.setWrap(c.getBoolean("wrap", false))
            // When kerning is true, it won't be added as an attribute, it's true by default
            textMapObject.setKerning(c.getBoolean("kerning", true))
            // Default color is #000000, not added as attribute
            val textColor = c.getString("color", Nullable("#000000")).getOrElse("#000000")
            textMapObject.setColor(Color.valueOf(BaseTiledMapLoader.tiledColorToLibGDXColor(textColor)))
            obj = Nullable(textMapObject)
          }
        }
      }
    }
    if (obj.isEmpty) {
      val gid = element.getString("gid", Nullable.empty)
      gid.foreach { g =>
        val id               = java.lang.Long.parseLong(g).toInt
        val flipHorizontally = (id & BaseTiledMapLoader.FLAG_FLIP_HORIZONTALLY) != 0
        val flipVertically   = (id & BaseTiledMapLoader.FLAG_FLIP_VERTICALLY) != 0

        val tile = map.getTileSets.getTile(id & ~BaseTiledMapLoader.MASK_CLEAR)
        tile.foreach { t =>
          val tiledMapTileMapObject = new TiledMapTileMapObject(t, flipHorizontally, flipVertically)
          val texRegion             = tiledMapTileMapObject.getTextureRegion.getOrElse(throw new IllegalStateException("tile missing texture region"))
          tiledMapTileMapObject.getProperties.put("gid", id: java.lang.Integer)
          tiledMapTileMapObject.setX(x)
          tiledMapTileMapObject.setY(if (flipY) y else y - height)
          val objectWidth  = element.getFloat("width", texRegion.getRegionWidth().toFloat)
          val objectHeight = element.getFloat("height", texRegion.getRegionHeight().toFloat)
          tiledMapTileMapObject.setScaleX(scaleX * (objectWidth / texRegion.getRegionWidth()))
          tiledMapTileMapObject.setScaleY(scaleY * (objectHeight / texRegion.getRegionHeight()))
          tiledMapTileMapObject.setRotation(element.getFloat("rotation", 0))
          obj = Nullable(tiledMapTileMapObject)
        }
      }
      if (obj.isEmpty) {
        obj = Nullable(new RectangleMapObject(x, if (flipY) y - height else y, width, height))
      }
    }
    val theObj = obj.getOrElse(throw new IllegalStateException("object could not be created"))
    theObj.setName(element.getString("name", Nullable.empty).getOrElse(""))
    val rotation = element.getString("rotation", Nullable.empty)
    rotation.foreach(r => theObj.getProperties.put("rotation", java.lang.Float.parseFloat(r): java.lang.Float))
    val objType = element.getString("type", Nullable.empty)
    objType.foreach(t => theObj.getProperties.put("type", t))
    val id = element.getInt("id", 0)
    if (id != 0) {
      theObj.getProperties.put("id", id: java.lang.Integer)
    }
    theObj.getProperties.put("x", x: java.lang.Float)

    theObj match {
      case _: TiledMapTileMapObject => theObj.getProperties.put("y", y: java.lang.Float)
      case _ => theObj.getProperties.put("y", (if (flipY) y - height else y): java.lang.Float)
    }
    theObj.getProperties.put("width", width:   java.lang.Float)
    theObj.getProperties.put("height", height: java.lang.Float)
    theObj.setVisible(element.getBoolean("visible", true))
    val properties = element.get("properties")
    properties.foreach(p => loadProperties(theObj.getProperties, p))

    // if there is a 'type' (=class) specified, then check if there are any other
    // class properties available and put their default values into the properties.
    loadMapPropertiesClassDefaults(objType, theObj.getProperties)

    idToObject.put(id, theObj)
    objects.add(theObj)
  }

  /** Method specifically meant to help resolve template object properties and attributes found in objectgroups. Each template object links to a specific .tj file. Attributes and properties found in
    * the template are allowed to be overwritten by any matching ones found in its parent element. Knowing this, we will merge the two elements together with the parent's props taking precedence and
    * then return the merged value.
    * @param map
    *   TileMap object
    * @param layer
    *   MapLayer object
    * @param mapElement
    *   JsonValue which contains the single json element we are currently parsing
    * @param tmjFile
    *   tmjFile
    * @return
    *   a merged JsonValue representing the combined JsonValues.
    */
  protected def resolveTemplateObject(
    map:        TiledMap,
    layer:      MapLayer,
    mapElement: JsonValue,
    tmjFile:    FileHandle
  ): JsonValue = {
    // Get template (.tj) file name from element
    val tjFileName = mapElement.getString("template", Nullable.empty).getOrElse("")
    // check for cached tj element
    var templateElement = templateCache.get(tjFileName)
    if (templateElement.isEmpty) {
      // parse the .tj template file
      try
        templateElement = Nullable(json.parse(BaseTiledMapLoader.getRelativeFileHandle(tmjFile, tjFileName)))
      catch {
        case e: Exception =>
          throw new IllegalArgumentException("Error parsing template file: " + tjFileName, e)
      }
      templateCache.put(tjFileName, templateElement.getOrElse(throw new IllegalStateException("template parse failed")))
    }
    // Get the root object from the template file
    val te                    = templateElement.getOrElse(throw new IllegalStateException("template element not found"))
    val templateObjectElement = te.get("object")
    // Merge the parent map element with its template element
    mergeParentElementWithTemplate(mapElement, templateObjectElement.getOrElse(throw new IllegalStateException("template object element not found")))
  }

  /** JSON TextMapObjects contain object nodes containing specific text related attributes. Here we merge them, parent attributes will override those found in templates.
    */
  protected def mergeJsonObject(parentObject: Nullable[JsonValue], templateObject: Nullable[JsonValue]): JsonValue =
    if (templateObject.isEmpty) parentObject.getOrElse(throw new IllegalStateException("both json objects absent")) // scalastyle:ignore
    else if (parentObject.isEmpty) templateObject.getOrElse(throw new IllegalStateException("both json objects absent")) // scalastyle:ignore
    else {
      val pObj = parentObject.getOrElse(throw new IllegalStateException("unreachable"))
      val tObj = templateObject.getOrElse(throw new IllegalStateException("unreachable"))
      // Create a new merged element which will contain a combination of parent and template objects
      val merged = new JsonValue(JsonValue.ValueType.`object`)
      // Add children from template
      for (child <- tObj)
        merged.addChild(child.name.getOrElse(""), cloneElementShallow(child))
      // Add or override children from parent
      for (child <- pObj)
        merged.setChild(child.name.getOrElse(""), cloneElementShallow(child))
      merged
    }

  /** Returns a shallow copy of the source JsonValue element we pass in. This method only copies the basic type and value (string, number, boolean, or null) from the source element. It does not clone
    * child element for arrays or objects. If the source element is an array or object, the entire element is copied using JsonValue's copy constructor, resulting in a deep copy for those types.
    */
  protected def cloneElementShallow(src: JsonValue): JsonValue = {
    val clone = src.valueType match {
      case JsonValue.ValueType.stringValue  => new JsonValue(src.asString())
      case JsonValue.ValueType.doubleValue  => new JsonValue(src.asDouble())
      case JsonValue.ValueType.longValue    => new JsonValue(src.asLong())
      case JsonValue.ValueType.booleanValue => new JsonValue(src.asBoolean())
      case JsonValue.ValueType.nullValue    => new JsonValue(Nullable.empty[String])
      // Fallback for a full deep copy for an object/array
      case _ => deepCopyJsonValue(src)
    }
    clone.setName(src.name)
    clone
  }

  /** Merges two properties arrays from a parent and template. Matching properties from the parent will override the template's.
    */
  protected def mergeJsonProperties(parentProps: Nullable[JsonValue], templateProps: Nullable[JsonValue]): JsonValue =
    if (templateProps.isEmpty) parentProps.getOrElse(throw new IllegalStateException("both properties absent")) // scalastyle:ignore
    else if (parentProps.isEmpty) templateProps.getOrElse(throw new IllegalStateException("both properties absent")) // scalastyle:ignore
    else {
      val pProps = parentProps.getOrElse(throw new IllegalStateException("unreachable"))
      val tProps = templateProps.getOrElse(throw new IllegalStateException("unreachable"))
      // Create a new merged properties element which will contain a combination of parent and template properties.
      val merged = new JsonValue(JsonValue.ValueType.array)
      // Set properties from template
      for (property <- tProps)
        merged.addChild(deepCopyJsonValue(property)) // deep copy
      // Set properties from the parent, matching ones from template will be overridden
      for (property <- pProps) {
        val propName = property.getString("name", Nullable.empty)
        if (propName.isDefined) {
          // Search for an existing property with the same name
          for (child <- merged)
            if (propName.getOrElse("") == child.getString("name", Nullable.empty).getOrElse("")) {
              child.removeFromParent()
            }
          merged.addChild(deepCopyJsonValue(property)) // Add or replace with map copy
        }
      }
      merged
    }

  /** Recursively merges a "parent" (map) object element with its referenced template object element. Attributes and properties found in the template are allowed to be overwritten by any matching ones
    * found in its parent element. The returned element is a new detached tree (parent = null) so it can be handed straight to the loadObject() method without issues.
    */
  protected def mergeParentElementWithTemplate(parent: JsonValue, template: JsonValue): JsonValue = {
    // Create a new merged element which will contain a combination of parent and template attributes, properties etc...
    val merged = new JsonValue(JsonValue.ValueType.`object`)
    // Set attributes from template
    for (child <- template)
      merged.addChild(cloneElementShallow(child))
    // Set attributes from the parent, matching ones from template will be overridden
    // Specifically added special case to handle JSON version of TextMapObjects since they are unique compared to other objects.
    for (child <- parent) {
      val key = child.name.getOrElse("")
      key match {
        case "properties" =>
          merged.setChild(key, mergeJsonProperties(Nullable(child), template.get("properties")))
        case "text" =>
          merged.setChild(key, mergeJsonObject(Nullable(child), template.get("text")))
        case _ =>
          merged.setChild(key, cloneElementShallow(child))
      }
    }
    merged
  }

  /** Creates a deep copy of a JsonValue tree, since JsonValue has no copy constructor. */
  private def deepCopyJsonValue(src: JsonValue): JsonValue = {
    val copy = new JsonValue(src.valueType)
    copy.set(src) // copies type, string/double/long values
    copy.setName(src.name)
    // Copy children recursively
    var child = src.child
    while (child.isDefined) {
      val c = child.getOrElse(throw new IllegalStateException("unreachable"))
      copy.addChild(deepCopyJsonValue(c))
      child = c.next
    }
    copy
  }

  /* * End of Tiled Template Loading Section * */

  private def loadProperties(properties: MapProperties, element: JsonValue): Unit =
    if (Nullable(element).isDefined && element.name.fold(false)(_ == "properties")) { // scalastyle:ignore
      for (property <- element) {
        val name     = property.getString("name", Nullable.empty).getOrElse("")
        var value    = property.getString("value", Nullable.empty)
        val propType = property.getString("type", Nullable.empty)
        if (value.isEmpty && propType.fold(true)(_ != "class")) {
          value = property.asString()
        }
        propType.getOrElse("") match {
          case "object" =>
            value.foreach(v => loadObjectProperty(properties, name, v))
          case "class" =>
            // A 'class' property is a property which is itself a set of properties
            val classProperties = new MapProperties()
            val className       = property.getString("propertytype", Nullable.empty).getOrElse("")
            classProperties.put("type", className)
            // the actual properties of a 'class' property are stored as a new properties tag
            properties.put(name, classProperties)
            loadJsonClassProperties(className, classProperties, property.get("value"))
          case _ =>
            value.foreach(v => loadBasicProperty(properties, name, v, propType))
        }
      }
    }

  protected def loadTileSet(element: JsonValue, tmjFile: FileHandle, imageResolver: ImageResolver): Unit = {
    var el = element
    if (el.getString("firstgid", Nullable.empty).isDefined) {
      val firstgid    = el.getInt("firstgid", 1)
      var imageSource = ""
      var imageWidth  = 0
      var imageHeight = 0
      var image: Nullable[FileHandle] = Nullable.empty

      val source = el.getString("source", Nullable.empty)
      source.foreach { s =>
        val tsj = BaseTiledMapLoader.getRelativeFileHandle(tmjFile, s)
        try {
          el = json.parse(tsj)
          if (el.has("image")) {
            imageSource = el.getString("image", Nullable.empty).getOrElse("")
            imageWidth = el.getInt("imagewidth", 0)
            imageHeight = el.getInt("imageheight", 0)
            image = Nullable(BaseTiledMapLoader.getRelativeFileHandle(tsj, imageSource))
          }
        } catch {
          case e: Exception =>
            throw new IllegalArgumentException("Error parsing external tileSet.", e)
        }
      }
      if (source.isEmpty) {
        if (el.has("image")) {
          imageSource = el.getString("image", Nullable.empty).getOrElse("")
          imageWidth = el.getInt("imagewidth", 0)
          imageHeight = el.getInt("imageheight", 0)
          image = Nullable(BaseTiledMapLoader.getRelativeFileHandle(tmjFile, imageSource))
        }
      }
      val name       = el.getString("name", Nullable.empty)
      val tilewidth  = el.getInt("tilewidth", 0)
      val tileheight = el.getInt("tileheight", 0)
      val spacing    = el.getInt("spacing", 0)
      val margin     = el.getInt("margin", 0)

      val offset  = el.get("tileoffset")
      var offsetX = 0
      var offsetY = 0
      offset.foreach { o =>
        offsetX = o.getInt("x", 0)
        offsetY = o.getInt("y", 0)
      }
      val tileSet = new TiledMapTileSet()

      // TileSet
      tileSet.setName(name.getOrElse(""))
      val tileSetProperties = tileSet.getProperties
      val properties        = el.get("properties")
      properties.foreach(p => loadProperties(tileSetProperties, p))
      tileSetProperties.put("firstgid", firstgid: java.lang.Integer)

      // Tiles
      var tiles = el.get("tiles")

      if (tiles.isEmpty) {
        tiles = Nullable(new JsonValue(JsonValue.ValueType.array))
      }

      addStaticTiles(
        tmjFile,
        imageResolver,
        tileSet,
        el,
        tiles.getOrElse(throw new IllegalStateException("unreachable")),
        name.getOrElse(""),
        firstgid,
        tilewidth,
        tileheight,
        spacing,
        margin,
        source.getOrElse(""),
        offsetX,
        offsetY,
        imageSource,
        imageWidth,
        imageHeight,
        image.getOrElse(null: FileHandle) // scalastyle:ignore -- null valid per original LibGDX API
      )

      val animatedTiles = DynamicArray[AnimatedTiledMapTile]()

      for (tileElement <- tiles.getOrElse(throw new IllegalStateException("unreachable"))) {
        val localtid = tileElement.getInt("id", 0)
        val tile     = tileSet.getTile(firstgid + localtid)
        tile.foreach { t =>
          var currentTile  = t
          val animatedTile = createAnimatedTile(tileSet, currentTile, tileElement, firstgid)
          animatedTile.foreach { at =>
            animatedTiles.add(at)
            currentTile = at
          }
          addTileProperties(currentTile, tileElement)
          addTileObjectGroup(currentTile, tileElement)
        }
      }
      // replace original static tiles by animated tiles
      for (animatedTile <- animatedTiles)
        tileSet.putTile(animatedTile.getId, animatedTile)

      map.getTileSets.addTileSet(tileSet)
    }
  }

  protected def addStaticTiles(
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
  ): Unit

  private def addTileProperties(tile: TiledMapTile, tileElement: JsonValue): Unit = {
    val terrain        = tileElement.getString("terrain", Nullable.empty)
    val tileProperties = tile.getProperties
    terrain.foreach(t => tileProperties.put("terrain", t))
    val probability = tileElement.getString("probability", Nullable.empty)
    probability.foreach(p => tileProperties.put("probability", p))
    val tileType = tileElement.getString("type", Nullable.empty)
    tileType.foreach(t => tileProperties.put("type", t))
    val properties = tileElement.get("properties")
    properties.foreach(p => loadProperties(tileProperties, p))

    // if there is a 'type' (=class) specified, then check if there are any other
    // class properties available and put their default values into the properties.
    loadMapPropertiesClassDefaults(tileType, tileProperties)
  }

  private def addTileObjectGroup(tile: TiledMapTile, tileElement: JsonValue): Unit = {
    val objectgroupElement = tileElement.get("objectgroup")
    objectgroupElement.foreach { og =>
      val objects = og.get("objects")
      objects.foreach { objs =>
        for (objectElement <- objs)
          loadObject(this.map, tile, objectElement)
      }
    }
  }

  protected def createAnimatedTile(
    tileSet:     TiledMapTileSet,
    tile:        TiledMapTile,
    tileElement: JsonValue,
    firstgid:    Int
  ): Nullable[AnimatedTiledMapTile] = {
    val animationElement = tileElement.get("animation")
    animationElement.map { ae =>
      val staticTiles = DynamicArray[StaticTiledMapTile]()
      val intervals   = DynamicArray[Int]()
      for (frameValue <- ae) {
        staticTiles.add(
          tileSet.getTile(firstgid + frameValue.getInt("tileid", 0)).getOrElse(throw new IllegalStateException("missing tile for animation frame")).asInstanceOf[StaticTiledMapTile]
        )
        intervals.add(frameValue.getInt("duration", 0))
      }

      val animatedTile = new AnimatedTiledMapTile(intervals.toArray, staticTiles)
      animatedTile.setId(tile.getId)
      animatedTile
    }
  }
}

object BaseTmjMapLoader {

  def getTileIds(element: JsonValue, width: Int, height: Int): Array[Int] = {
    val data     = element.get("data")
    val encoding = element.getString("encoding", Nullable.empty)

    val enc = encoding.getOrElse("")
    if (enc.isEmpty || enc == "csv") {
      data.getOrElse(throw new IllegalStateException("missing tile data")).asIntArray()
    } else if (enc == "base64") {
      var is: InputStream = null
      try {
        val compression = element.getString("compression", Nullable.empty)
        val comp        = compression.getOrElse("")
        val dataValue   = data.getOrElse(throw new IllegalStateException("missing tile data"))
        val bytes       = java.util.Base64.getDecoder.decode(dataValue.asString().getOrElse(""))
        if (comp.isEmpty)
          is = new ByteArrayInputStream(bytes)
        else if (comp == "gzip")
          is = new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes), bytes.length))
        else if (comp == "zlib")
          is = new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(bytes)))
        else
          throw new IllegalArgumentException("Unrecognised compression (" + comp + ") for TMJ Layer Data")

        val temp = new Array[Byte](4)
        val ids  = new Array[Int](width * height)
        var y    = 0
        while (y < height) {
          var x = 0
          while (x < width) {
            var read = is.read(temp)
            while (read < temp.length) {
              val curr = is.read(temp, read, temp.length - read)
              if (curr == -1) read = temp.length // break out
              else read += curr
            }
            if (read != temp.length)
              throw new IllegalArgumentException("Error Reading TMJ Layer Data: Premature end of tile data")
            ids(y * width + x) = BaseTiledMapLoader.unsignedByteToInt(temp(0)) |
              (BaseTiledMapLoader.unsignedByteToInt(temp(1)) << 8) |
              (BaseTiledMapLoader.unsignedByteToInt(temp(2)) << 16) |
              (BaseTiledMapLoader.unsignedByteToInt(temp(3)) << 24)
            x += 1
          }
          y += 1
        }
        ids
      } catch {
        case e: IOException =>
          throw new IllegalArgumentException("Error Reading TMJ Layer Data - IOException: " + e.getMessage)
      } finally
        StreamUtils.closeQuietly(is)
    } else {
      // any other value of 'encoding' is one we're not aware of, probably a feature of a future version of Tiled
      // or another editor
      throw new IllegalArgumentException("Unrecognised encoding (" + enc + ") for TMJ Layer Data")
    }
  }
}
