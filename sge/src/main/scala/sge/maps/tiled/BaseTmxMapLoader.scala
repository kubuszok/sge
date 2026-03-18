/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/BaseTmxMapLoader.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audited 2026-03-03):
 *   - All methods match Java 1:1 (loadTiledMap, loadLayer, loadLayerGroup, loadTileLayer,
 *     loadObjectGroup, loadImageLayer, loadBasicLayerInfo, loadObject, resolveTemplateObject,
 *     cloneElementShallow, mergeProperties, mergeParentElementWithTemplate, loadProperties,
 *     loadClassProperties, getPropertyByName, loadTileSet, addStaticTiles, addTileProperties,
 *     addTileObjectGroup, createAnimatedTile, getTileIds)
 *   - Java null checks → Nullable patterns throughout
 *   - Java XmlReader.Element returns → Nullable[XmlReader.Element]
 *   - Java IntArray → DynamicArray[Int]; Java Array<T> → DynamicArray[T]
 *   - getPropertyByName returns null via // scalastyle:ignore (Java interop boundary)
 *   - resolveTemplateObject: parsed var uses null // scalastyle:ignore (Java interop)
 *   - runOnEndOfLoadTiled = null after use (matches Java exactly, // scalastyle:ignore)
 *   - Java Base64Coder.decode → java.util.Base64.getDecoder.decode
 *   - boundary/break used for loadProperties, mergeProperties, getPropertyByName
 *   - Split package, braces, no-return conventions satisfied
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
import sge.utils.{ DynamicArray, Nullable, ObjectMap, ObjectSet, StreamUtils, XmlReader }

import java.io.{ BufferedInputStream, ByteArrayInputStream, IOException, InputStream }
import java.util.zip.{ GZIPInputStream, InflaterInputStream }
import scala.language.implicitConversions
import scala.util.boundary
import scala.util.boundary.break

abstract class BaseTmxMapLoader[P <: BaseTiledMapLoader.Parameters](resolver: FileHandleResolver) extends BaseTiledMapLoader[P](resolver) {

  protected var xml:  XmlReader                   = XmlReader()
  protected var root: Nullable[XmlReader.Element] = Nullable.empty

  protected var templateCache: ObjectMap[String, XmlReader.Element] = scala.compiletime.uninitialized

  override def getDependencies(
    fileName:  String,
    tmxFile:   FileHandle,
    parameter: P
  ): DynamicArray[AssetDescriptor[?]] = {
    this.root = Nullable(xml.parse(tmxFile))

    val textureParameter = TextureLoader.TextureParameter()
    val param            = Nullable(parameter)
    param.foreach { p =>
      textureParameter.genMipMaps = p.generateMipMaps
      textureParameter.minFilter = p.textureMinFilter
      textureParameter.magFilter = p.textureMagFilter
    }

    getDependencyAssetDescriptors(tmxFile, textureParameter)
  }

  /** Loads the map data, given the XML root element
    *
    * @param tmxFile
    *   the Filehandle of the tmx file
    * @param parameter
    * @param imageResolver
    * @return
    *   the [[TiledMap]]
    */
  override protected def loadTiledMap(tmxFile: FileHandle, parameter: P, imageResolver: ImageResolver): TiledMap = {
    this.map = TiledMap()
    this.idToObject = new scala.collection.mutable.HashMap[Int, MapObject]()
    this.runOnEndOfLoadTiled = DynamicArray[() => Unit]()
    this.templateCache = ObjectMap[String, XmlReader.Element]()

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
    val mapOrientation     = r.getAttribute("orientation", Nullable.empty)
    val mapWidth           = r.getIntAttribute("width", 0)
    val mapHeight          = r.getIntAttribute("height", 0)
    val tileWidth          = r.getIntAttribute("tilewidth", 0)
    val tileHeight         = r.getIntAttribute("tileheight", 0)
    val hexSideLength      = r.getIntAttribute("hexsidelength", 0)
    val staggerAxis        = r.getAttribute("staggeraxis", Nullable.empty)
    val staggerIndex       = r.getAttribute("staggerindex", Nullable.empty)
    val mapBackgroundColor = r.getAttribute("backgroundcolor", Nullable.empty)

    val mapProperties = map.properties
    mapOrientation.foreach(v => mapProperties.put("orientation", v))
    mapProperties.put("width", mapWidth:              java.lang.Integer)
    mapProperties.put("height", mapHeight:            java.lang.Integer)
    mapProperties.put("tilewidth", tileWidth:         java.lang.Integer)
    mapProperties.put("tileheight", tileHeight:       java.lang.Integer)
    mapProperties.put("hexsidelength", hexSideLength: java.lang.Integer)
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

    val properties = r.getChildByName("properties")
    properties.foreach(p => loadProperties(map.properties, p))

    val tilesets = r.getChildrenByName("tileset")
    var ti       = 0
    while (ti < tilesets.size) {
      val element = tilesets(ti)
      loadTileSet(element, tmxFile, imageResolver)
      r.removeChild(element)
      ti += 1
    }

    var i = 0
    val j = r.childCount
    while (i < j) {
      val element = r.getChild(i)
      loadLayer(map, map.layers, element, tmxFile, imageResolver)
      i += 1
    }

    // update hierarchical parallax scrolling factors
    // in Tiled the final parallax scrolling factor of a layer is the multiplication of its factor with all its parents
    // 1) get top level groups
    val groups = map.layers.byType[MapGroupLayer]
    while (groups.nonEmpty) {
      val group = groups.first
      groups.removeIndex(0)

      for (child <- group.layers) {
        child.parallaxX = child.parallaxX * group.parallaxX
        child.parallaxY = child.parallaxY * group.parallaxY
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
    element:       XmlReader.Element,
    tmxFile:       FileHandle,
    imageResolver: ImageResolver
  ): Unit = {
    val name = element.name
    if (name == "group") loadLayerGroup(map, parentLayers, element, tmxFile, imageResolver)
    else if (name == "layer") loadTileLayer(map, parentLayers, element)
    else if (name == "objectgroup") loadObjectGroup(map, parentLayers, element, tmxFile)
    else if (name == "imagelayer") loadImageLayer(map, parentLayers, element, tmxFile, imageResolver)
  }

  protected def loadLayerGroup(
    map:           TiledMap,
    parentLayers:  MapLayers,
    element:       XmlReader.Element,
    tmxFile:       FileHandle,
    imageResolver: ImageResolver
  ): Unit =
    if (element.name == "group") {
      val groupLayer = MapGroupLayer()
      loadBasicLayerInfo(groupLayer, element)

      val properties = element.getChildByName("properties")
      properties.foreach(p => loadProperties(groupLayer.properties, p))

      var i = 0
      val j = element.childCount
      while (i < j) {
        val child = element.getChild(i)
        loadLayer(map, groupLayer.layers, child, tmxFile, imageResolver)
        i += 1
      }

      for (layer <- groupLayer.layers)
        layer.parent = Nullable(groupLayer)

      parentLayers.add(groupLayer)
    }

  protected def loadTileLayer(map: TiledMap, parentLayers: MapLayers, element: XmlReader.Element): Unit =
    if (element.name == "layer") {
      val width      = element.getIntAttribute("width", 0)
      val height     = element.getIntAttribute("height", 0)
      val tileWidth  = map.properties.getAs[Integer]("tilewidth").get.intValue()
      val tileHeight = map.properties.getAs[Integer]("tileheight").get.intValue()
      val layer      = TiledMapTileLayer(width, height, tileWidth, tileHeight)

      loadBasicLayerInfo(layer, element)

      val ids      = BaseTmxMapLoader.getTileIds(element, width, height)
      val tilesets = map.tileSets
      var y        = 0
      while (y < height) {
        var x = 0
        while (x < width) {
          val id               = ids(y * width + x)
          val flipHorizontally = (id & BaseTiledMapLoader.FLAG_FLIP_HORIZONTALLY) != 0
          val flipVertically   = (id & BaseTiledMapLoader.FLAG_FLIP_VERTICALLY) != 0
          val flipDiagonally   = (id & BaseTiledMapLoader.FLAG_FLIP_DIAGONALLY) != 0

          val tile = tilesets.getTile(id & ~BaseTiledMapLoader.MASK_CLEAR)
          tile.foreach { t =>
            val cell = createTileLayerCell(flipHorizontally, flipVertically, flipDiagonally)
            cell.tile = Nullable(t)
            layer.setCell(x, if (flipY) height - 1 - y else y, Nullable(cell))
          }
          x += 1
        }
        y += 1
      }

      val properties = element.getChildByName("properties")
      properties.foreach(p => loadProperties(layer.properties, p))
      parentLayers.add(layer)
    }

  protected def loadObjectGroup(
    map:          TiledMap,
    parentLayers: MapLayers,
    element:      XmlReader.Element,
    tmxFile:      FileHandle
  ): Unit =
    if (element.name == "objectgroup") {
      val layer = MapLayer()
      loadBasicLayerInfo(layer, element)
      val properties = element.getChildByName("properties")
      properties.foreach(p => loadProperties(layer.properties, p))

      val objectElements = element.getChildrenByName("object")
      var oi             = 0
      while (oi < objectElements.size) {
        val objectElement = objectElements(oi)
        var elementToLoad = objectElement
        if (objectElement.hasAttribute("template")) {
          elementToLoad = resolveTemplateObject(map, layer, objectElement, tmxFile)
        }
        loadObject(map, layer, elementToLoad)
        oi += 1
      }
      parentLayers.add(layer)
    }

  protected def loadImageLayer(
    map:           TiledMap,
    parentLayers:  MapLayers,
    element:       XmlReader.Element,
    tmxFile:       FileHandle,
    imageResolver: ImageResolver
  ): Unit =
    if (element.name == "imagelayer") {
      var x: Float = 0
      var y: Float = 0
      if (element.hasAttribute("offsetx"))
        x = java.lang.Float.parseFloat(element.getAttribute("offsetx", Nullable("0")).getOrElse("0"))
      else
        x = java.lang.Float.parseFloat(element.getAttribute("x", Nullable("0")).getOrElse("0"))
      if (element.hasAttribute("offsety"))
        y = java.lang.Float.parseFloat(element.getAttribute("offsety", Nullable("0")).getOrElse("0"))
      else
        y = java.lang.Float.parseFloat(element.getAttribute("y", Nullable("0")).getOrElse("0"))
      if (flipY) y = mapHeightInPixels - y

      val repeatX = element.getIntAttribute("repeatx", 0) == 1
      val repeatY = element.getIntAttribute("repeaty", 0) == 1

      var texture: Nullable[TextureRegion] = Nullable.empty

      val image = element.getChildByName("image")

      image.foreach { img =>
        val source = img.getAttribute("source")
        val handle = BaseTiledMapLoader.getRelativeFileHandle(tmxFile, source)
        texture = imageResolver.getImage(handle.path)
        texture.foreach(t => y -= t.regionHeight)
      }

      @annotation.nowarn("msg=deprecated") // TiledMapImageLayer takes TextureRegion, not Nullable -- null is valid per original LibGDX API
      val textureValue: TextureRegion = texture.orNull
      val layer = TiledMapImageLayer(textureValue, x, y, repeatX, repeatY)

      loadBasicLayerInfo(layer, element)

      val properties = element.getChildByName("properties")
      properties.foreach(p => loadProperties(layer.properties, p))

      parentLayers.add(layer)
    }

  protected def loadBasicLayerInfo(layer: MapLayer, element: XmlReader.Element): Unit = {
    val name      = element.getAttribute("name", Nullable.empty).getOrElse("")
    val opacity   = java.lang.Float.parseFloat(element.getAttribute("opacity", Nullable("1.0")).getOrElse("1.0"))
    val tintColor = element.getAttribute("tintcolor", Nullable("#ffffffff")).getOrElse("#ffffffff")
    val visible   = element.getIntAttribute("visible", 1) == 1
    val offsetX   = element.getFloatAttribute("offsetx", 0)
    val offsetY   = element.getFloatAttribute("offsety", 0)
    val parallaxX = element.getFloatAttribute("parallaxx", 1f)
    val parallaxY = element.getFloatAttribute("parallaxy", 1f)

    layer.name = name
    layer.opacity = opacity
    layer.visible = visible
    layer.offsetX = offsetX
    layer.offsetY = offsetY
    layer.parallaxX = parallaxX
    layer.parallaxY = parallaxY

    // set layer tint color after converting from #AARRGGBB to #RRGGBBAA
    layer.tintColor = Color.valueOf(BaseTiledMapLoader.tiledColorToLibGDXColor(tintColor))
  }

  protected def loadObject(map: TiledMap, layer: MapLayer, element: XmlReader.Element): Unit =
    loadObject(map, layer.objects, element, mapHeightInPixels.toFloat)

  protected def loadObject(map: TiledMap, tile: TiledMapTile, element: XmlReader.Element): Unit =
    loadObject(map, tile.objects, element, tile.textureRegion.regionHeight.toFloat)

  protected def loadObject(
    map:            TiledMap,
    objects:        MapObjects,
    element:        XmlReader.Element,
    heightInPixels: Float
  ): Unit = {
    if (element.name == "object") {
      var obj: Nullable[MapObject] = Nullable.empty

      val scaleX = if (convertObjectToTileSpace) 1.0f / mapTileWidth else 1.0f
      val scaleY = if (convertObjectToTileSpace) 1.0f / mapTileHeight else 1.0f

      val x = element.getFloatAttribute("x", 0) * scaleX
      val y =
        (if (flipY) heightInPixels - element.getFloatAttribute("y", 0)
         else element.getFloatAttribute("y", 0)) * scaleY

      val width  = element.getFloatAttribute("width", 0) * scaleX
      val height = element.getFloatAttribute("height", 0) * scaleY

      if (element.childCount > 0) {
        var child: Nullable[XmlReader.Element] = Nullable.empty
        child = element.getChildByName("polygon")
        if (child.isDefined) {
          child.foreach { c =>
            val points   = c.getAttribute("points").split(" ")
            val vertices = new Array[Float](points.length * 2)
            var i        = 0
            while (i < points.length) {
              val point = points(i).split(",")
              vertices(i * 2) = java.lang.Float.parseFloat(point(0)) * scaleX
              vertices(i * 2 + 1) = java.lang.Float.parseFloat(point(1)) * scaleY * (if (flipY) -1 else 1)
              i += 1
            }
            val polygon = Polygon(vertices)
            polygon.setPosition(x, y)
            obj = Nullable(PolygonMapObject(polygon))
          }
        } else {
          child = element.getChildByName("polyline")
          if (child.isDefined) {
            child.foreach { c =>
              val points   = c.getAttribute("points").split(" ")
              val vertices = new Array[Float](points.length * 2)
              var i        = 0
              while (i < points.length) {
                val point = points(i).split(",")
                vertices(i * 2) = java.lang.Float.parseFloat(point(0)) * scaleX
                vertices(i * 2 + 1) = java.lang.Float.parseFloat(point(1)) * scaleY * (if (flipY) -1 else 1)
                i += 1
              }
              val polyline = Polyline(vertices)
              polyline.setPosition(x, y)
              obj = Nullable(PolylineMapObject(polyline))
            }
          } else {
            child = element.getChildByName("ellipse")
            if (child.isDefined) {
              obj = Nullable(EllipseMapObject(x, if (flipY) y - height else y, width, height))
            } else {
              child = element.getChildByName("point")
              if (child.isDefined) {
                obj = Nullable(PointMapObject(x, if (flipY) y - height else y))
              } else {
                child = element.getChildByName("text")
                child.foreach { textChild =>
                  val textMapObject =
                    TextMapObject(x, if (flipY) y - height else y, width, height, textChild.text.getOrElse(""))
                  textMapObject.fontFamily = textChild.getAttribute("fontfamily", Nullable("")).getOrElse("")
                  textMapObject.pixelSize = textChild.getIntAttribute("pixelSize", 16)
                  textMapObject.horizontalAlign = textChild.getAttribute("halign", Nullable("left")).getOrElse("left")
                  textMapObject.verticalAlign = textChild.getAttribute("valign", Nullable("top")).getOrElse("top")
                  textMapObject.bold = textChild.getIntAttribute("bold", 0) == 1
                  textMapObject.italic = textChild.getIntAttribute("italic", 0) == 1
                  textMapObject.underline = textChild.getIntAttribute("underline", 0) == 1
                  textMapObject.strikeout = textChild.getIntAttribute("strikeout", 0) == 1
                  textMapObject.wrap = textChild.getIntAttribute("wrap", 0) == 1
                  // When kerning is true, it won't be added as an attribute, it's true by default
                  textMapObject.kerning = textChild.getIntAttribute("kerning", 1) == 1
                  // Default color is #000000, not added as attribute
                  val textColor = textChild.getAttribute("color", Nullable("#000000")).getOrElse("#000000")
                  textMapObject.color = Color.valueOf(BaseTiledMapLoader.tiledColorToLibGDXColor(textColor))
                  obj = Nullable(textMapObject)
                }
              }
            }
          }
        }
      }
      if (obj.isEmpty) {
        val gid = element.getAttribute("gid", Nullable.empty)
        gid.foreach { g =>
          val id               = java.lang.Long.parseLong(g).toInt
          val flipHorizontally = (id & BaseTiledMapLoader.FLAG_FLIP_HORIZONTALLY) != 0
          val flipVertically   = (id & BaseTiledMapLoader.FLAG_FLIP_VERTICALLY) != 0

          val tile = map.tileSets.getTile(id & ~BaseTiledMapLoader.MASK_CLEAR)
          tile.foreach { t =>
            val tiledMapTileMapObject = TiledMapTileMapObject(t, flipHorizontally, flipVertically)
            val texRegion             = tiledMapTileMapObject.textureRegion.getOrElse(throw new IllegalStateException("tile missing texture region"))
            tiledMapTileMapObject.properties.put("gid", id: java.lang.Integer)
            tiledMapTileMapObject.x = x
            tiledMapTileMapObject.y = if (flipY) y else y - height
            val objectWidth  = element.getFloatAttribute("width", texRegion.regionWidth.toFloat)
            val objectHeight = element.getFloatAttribute("height", texRegion.regionHeight.toFloat)
            tiledMapTileMapObject.scaleX = scaleX * (objectWidth / texRegion.regionWidth)
            tiledMapTileMapObject.scaleY = scaleY * (objectHeight / texRegion.regionHeight)
            tiledMapTileMapObject.rotation = element.getFloatAttribute("rotation", 0)
            obj = Nullable(tiledMapTileMapObject)
          }
        }
        if (obj.isEmpty) {
          obj = Nullable(RectangleMapObject(x, if (flipY) y - height else y, width, height))
        }
      }
      val theObj = obj.getOrElse(throw new IllegalStateException("object could not be created"))
      theObj.name = element.getAttribute("name", Nullable.empty).getOrElse("")
      val rotation = element.getAttribute("rotation", Nullable.empty)
      rotation.foreach { r =>
        theObj.properties.put("rotation", java.lang.Float.parseFloat(r): java.lang.Float)
      }
      val objType = element.getAttribute("type", Nullable.empty)
      objType.foreach(t => theObj.properties.put("type", t))
      val id = element.getIntAttribute("id", 0)
      if (id != 0) {
        theObj.properties.put("id", id: java.lang.Integer)
      }
      theObj.properties.put("x", x: java.lang.Float)

      theObj match {
        case _: TiledMapTileMapObject => theObj.properties.put("y", y: java.lang.Float)
        case _ => theObj.properties.put("y", (if (flipY) y - height else y): java.lang.Float)
      }
      theObj.properties.put("width", width:   java.lang.Float)
      theObj.properties.put("height", height: java.lang.Float)
      theObj.visible = element.getIntAttribute("visible", 1) == 1
      val properties = element.getChildByName("properties")
      properties.foreach(p => loadProperties(theObj.properties, p))

      // if there is a 'type' (=class) specified, then check if there are any other
      // class properties available and put their default values into the properties.
      loadMapPropertiesClassDefaults(objType, theObj.properties)

      idToObject.put(id, theObj)
      objects.add(theObj)
    }
  }

  /** Method specifically meant to help resolve template object properties and attributes found in objectgroups. Each template object links to a specific .tx file. Attributes and properties found in
    * the template are allowed to be overwritten by any matching ones found in its parent element. Knowing this, we will merge the two elements together with the parent's props taking precedence and
    * then return the merged value.
    */
  protected def resolveTemplateObject(
    map:        TiledMap,
    layer:      MapLayer,
    mapElement: XmlReader.Element,
    tmxFile:    FileHandle
  ): XmlReader.Element = {
    // Get template (.tx) file name from element
    val txFileName = mapElement.getAttribute("template")
    // check for cached tx element
    var templateElement = templateCache.get(txFileName)
    if (templateElement.isEmpty) {
      val templateFile = BaseTiledMapLoader.getRelativeFileHandle(tmxFile, txFileName)
      // parse the .tx template file
      var parsed: XmlReader.Element = null // scalastyle:ignore
      try
        parsed = xml.parse(templateFile)
      catch {
        case e: Exception =>
          throw new IllegalArgumentException("Error parsing template file: " + txFileName, e)
      }
      templateCache.put(txFileName, parsed)
      templateElement = Nullable(parsed)
    }
    // Get the root object from the template file
    val te                    = templateElement.getOrElse(throw new IllegalStateException("template element not found"))
    val templateObjectElement = te.getChildByName("object")
    // Merge the parent map element with its template element
    mergeParentElementWithTemplate(mapElement, templateObjectElement.getOrElse(throw new IllegalStateException("template object element not found")))
  }

  /** Returns a shallow copy of the source element we pass in. */
  protected def cloneElementShallow(sourceElement: XmlReader.Element): XmlReader.Element = {
    // New element for our copy
    val copyElement = XmlReader.Element(sourceElement.name, Nullable.empty)
    // Get list of attributes from the source element
    val attrs = sourceElement.attributes
    attrs.foreach { a =>
      a.foreachEntry { (key, value) =>
        copyElement.setAttribute(key, value)
      }
    }
    // Checking for text
    if (sourceElement.text.isDefined) copyElement.text = sourceElement.text
    copyElement
  }

  /** Merges two <properties> tags from a parent and template. Matching properties from the parent will override the template's.
    */
  protected def mergeProperties(
    parentProps:   Nullable[XmlReader.Element],
    templateProps: Nullable[XmlReader.Element]
  ): XmlReader.Element = boundary {
    if (templateProps.isEmpty) break(parentProps.getOrElse(throw new IllegalStateException("both properties absent")))
    if (parentProps.isEmpty) break(templateProps.getOrElse(throw new IllegalStateException("both properties absent")))
    val tProps = templateProps.getOrElse(throw new IllegalStateException("unreachable"))
    val pProps = parentProps.getOrElse(throw new IllegalStateException("unreachable"))
    // Create a new merged properties element which will contain a combination of parent and template properties.
    val merged = XmlReader.Element("properties", Nullable.empty)
    // Set properties from template
    val templateProperties = tProps.getChildrenByName("property")
    var ti                 = 0
    while (ti < templateProperties.size) {
      merged.addChild(cloneElementShallow(templateProperties(ti)))
      ti += 1
    }
    // Set properties from the parent, matching ones from template will be overridden
    val parentProperties = pProps.getChildrenByName("property")
    var pi               = 0
    while (pi < parentProperties.size) {
      val property = parentProperties(pi)
      val name     = property.getAttribute("name", Nullable.empty)
      // Find & remove a duplicate by name, if any
      var existing: Nullable[XmlReader.Element] = Nullable.empty
      var i = 0
      while (i < merged.childCount) {
        val child = merged.getChild(i)
        if ("property" == child.name && name.isDefined && name.getOrElse("") == child.getAttribute("name", Nullable.empty).getOrElse("")) {
          existing = Nullable(child)
        }
        i += 1
      }
      existing.foreach(merged.removeChild)
      merged.addChild(cloneElementShallow(property))
      pi += 1
    }
    merged
  }

  /** Recursively merges a "parent" (map) object element with its referenced template object element. Attributes and properties found in the template are allowed to be overwritten by any matching ones
    * found in its parent element. The returned element is a new detached tree (parent = null) so it can be handed straight to the loadObject() method without issues.
    */
  /** Nullable-accepting variant for recursive merging where either child may be absent. */
  private def mergeNullableElements(
    parent:   Nullable[XmlReader.Element],
    template: Nullable[XmlReader.Element]
  ): XmlReader.Element =
    if (template.isEmpty) parent.getOrElse(throw new IllegalStateException("both elements absent"))
    else if (parent.isEmpty) template.getOrElse(throw new IllegalStateException("both elements absent"))
    else
      mergeParentElementWithTemplate(
        parent.getOrElse(throw new IllegalStateException("unreachable")),
        template.getOrElse(throw new IllegalStateException("unreachable"))
      )

  protected def mergeParentElementWithTemplate(
    parent:   XmlReader.Element,
    template: XmlReader.Element
  ): XmlReader.Element = {
    // Create a new merged element which will contain a combination of parent and template attributes, properties etc...
    val merged = XmlReader.Element(template.name, Nullable.empty)
    // Set attributes from template
    val templateAttrs = template.attributes
    templateAttrs.foreach { ta =>
      ta.foreachEntry { (k, v) =>
        merged.setAttribute(k, v)
      }
    }
    // Set attributes from the parent, matching ones from template will be overridden
    val parentAttrs = parent.attributes
    parentAttrs.foreach { pa =>
      pa.foreachEntry { (k, v) =>
        merged.setAttribute(k, v)
      }
    }
    // Specifically added for TextMapObjects since they are unique compared to other objects.
    val txt =
      if (parent.text.isDefined && parent.text.getOrElse("").length > 0) parent.text else template.text
    if (txt.isDefined) {
      merged.text = txt
    }
    // Handle Child Elements
    // Collect all child tag names that appear in either element
    val tagNames = ObjectSet[String]()
    var i        = 0
    while (i < template.childCount) {
      tagNames.add(template.getChild(i).name)
      i += 1
    }
    i = 0
    while (i < parent.childCount) {
      tagNames.add(parent.getChild(i).name)
      i += 1
    }

    tagNames.foreach { tag =>
      val mapChild  = parent.getChildByName(tag)
      val tmplChild = template.getChildByName(tag)

      /** Look for properties tags so we can merge those as well. Recursive check if properties is not found. */
      val mergedChild =
        if ("properties" == tag) mergeProperties(mapChild, tmplChild)
        else mergeNullableElements(mapChild, tmplChild)
      merged.addChild(mergedChild)
    }
    merged
  }
  /* * End of Tiled Template Loading Section * */

  protected def loadProperties(properties: MapProperties, element: XmlReader.Element): Unit = boundary {
    if (Nullable(element).isEmpty) break()
    if (element.name == "properties") {
      val propertyElements = element.getChildrenByName("property")
      var pi               = 0
      while (pi < propertyElements.size) {
        val property = propertyElements(pi)
        val name     = property.getAttribute("name", Nullable.empty).getOrElse("")
        val value    = getPropertyValue(property)
        val propType = property.getAttribute("type", Nullable.empty)
        if (propType.contains("object")) {
          loadObjectProperty(properties, name, value)
        } else if (propType.contains("class")) {
          // A 'class' property is a property which is itself a set of properties
          val classProperties = MapProperties()
          val className       = property.getAttribute("propertytype")
          classProperties.put("type", className)
          // the actual properties of a 'class' property are stored as a new properties tag
          properties.put(name, classProperties)
          val classPropsElement = property.getChildByName("properties")
          loadClassProperties(className, classProperties, classPropsElement)
        } else {
          loadBasicProperty(properties, name, value, propType)
        }
        pi += 1
      }
    }
  }

  protected def loadClassProperties(
    className:       String,
    classProperties: MapProperties,
    classElement:    Nullable[XmlReader.Element]
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
      val propName = projectClassMember.name
      val classProp: Nullable[XmlReader.Element] =
        classElement.flatMap(ce => Nullable(getPropertyByName(ce, propName)))
      projectClassMember.`type` match {
        case "object" =>
          val value =
            if (classProp.isEmpty) projectClassMember.defaultValue.flatMap(BaseTiledMapLoader.jsonAsString).getOrElse("")
            else getPropertyValue(classProp.getOrElse(throw new IllegalStateException("unreachable")))
          loadObjectProperty(classProperties, propName, value)
        case "class" =>
          // A 'class' property is a property which is itself a set of properties
          val nestedClassProperties = MapProperties()
          val nestedClassName       = projectClassMember.propertyType.getOrElse("")
          nestedClassProperties.put("type", nestedClassName)
          // the actual properties of a 'class' property are stored as a new properties tag
          classProperties.put(propName, nestedClassProperties)
          if (classProp.isEmpty) {
            // no class values overridden -> use default class values
            loadJsonClassProperties(nestedClassName, nestedClassProperties, projectClassMember.defaultValue)
          } else {
            loadClassProperties(nestedClassName, nestedClassProperties, classProp)
          }
        case _ =>
          val value =
            if (classProp.isEmpty) projectClassMember.defaultValue.flatMap(BaseTiledMapLoader.jsonAsString).getOrElse("")
            else getPropertyValue(classProp.getOrElse(throw new IllegalStateException("unreachable")))
          loadBasicProperty(classProperties, propName, value, Nullable(projectClassMember.`type`))
      }
    }
  }

  private def getPropertyValue(classProp: XmlReader.Element): String = {
    val attrValue = classProp.getAttribute("value", Nullable.empty)
    attrValue.getOrElse(classProp.text.getOrElse(""))
  }

  protected def getPropertyByName(classElement: XmlReader.Element, propName: String): XmlReader.Element = boundary {
    // we use getChildrenByNameRecursively here because in case of nested classes,
    // we get an element with a root property (=class) and inside additional property tags for the real
    // class properties. If we just use getChildrenByName we don't get any children for a nested class.
    val properties = classElement.getChildrenByNameRecursively("property")
    var i          = 0
    while (i < properties.size) {
      val property = properties(i)
      if (propName == property.getAttribute("name")) {
        break(property)
      }
      i += 1
    }
    null // scalastyle:ignore
  }

  protected def loadTileSet(element: XmlReader.Element, tmxFile: FileHandle, imageResolver: ImageResolver): Unit = {
    if (element.name == "tileset") {
      var el          = element
      val firstgid    = el.getIntAttribute("firstgid", 1)
      var imageSource = ""
      var imageWidth  = 0
      var imageHeight = 0
      var image: FileHandle = null // scalastyle:ignore

      val source = el.getAttribute("source", Nullable.empty)
      source.foreach { s =>
        val tsx = BaseTiledMapLoader.getRelativeFileHandle(tmxFile, s)
        try {
          el = xml.parse(tsx)
          val imageElement = el.getChildByName("image")
          imageElement.foreach { ie =>
            imageSource = ie.getAttribute("source")
            imageWidth = ie.getIntAttribute("width", 0)
            imageHeight = ie.getIntAttribute("height", 0)
            image = BaseTiledMapLoader.getRelativeFileHandle(tsx, imageSource)
          }
        } catch {
          case e: Exception =>
            throw new IllegalArgumentException("Error parsing external tileset.", e)
        }
      }
      if (source.isEmpty) {
        val imageElement = el.getChildByName("image")
        imageElement.foreach { ie =>
          imageSource = ie.getAttribute("source")
          imageWidth = ie.getIntAttribute("width", 0)
          imageHeight = ie.getIntAttribute("height", 0)
          image = BaseTiledMapLoader.getRelativeFileHandle(tmxFile, imageSource)
        }
      }
      val name       = el.get("name", Nullable.empty).getOrElse("")
      val tilewidth  = el.getIntAttribute("tilewidth", 0)
      val tileheight = el.getIntAttribute("tileheight", 0)
      val spacing    = el.getIntAttribute("spacing", 0)
      val margin     = el.getIntAttribute("margin", 0)

      val offset  = el.getChildByName("tileoffset")
      var offsetX = 0
      var offsetY = 0
      offset.foreach { o =>
        offsetX = o.getIntAttribute("x", 0)
        offsetY = o.getIntAttribute("y", 0)
      }
      val tileSet = TiledMapTileSet()

      // TileSet
      tileSet.name = name
      val tileSetProperties = tileSet.properties
      val properties        = el.getChildByName("properties")
      properties.foreach(p => loadProperties(tileSetProperties, p))
      tileSetProperties.put("firstgid", firstgid: java.lang.Integer)

      // Tiles
      val tileElements = el.getChildrenByName("tile")

      addStaticTiles(
        tmxFile,
        imageResolver,
        tileSet,
        el,
        tileElements,
        name,
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
        image
      )

      val animatedTiles = DynamicArray[AnimatedTiledMapTile]()

      {
        var ti = 0
        while (ti < tileElements.size) {
          val tileElement = tileElements(ti)
          val localtid    = tileElement.getIntAttribute("id", 0)
          val tile        = tileSet.getTile(firstgid + localtid)
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
          ti += 1
        }
      }

      // replace original static tiles by animated tiles
      for (animatedTile <- animatedTiles)
        tileSet.putTile(animatedTile.id, animatedTile)

      map.tileSets.addTileSet(tileSet)
    }
  }

  protected def addStaticTiles(
    tmxFile:       FileHandle,
    imageResolver: ImageResolver,
    tileset:       TiledMapTileSet,
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
  ): Unit

  protected def addTileProperties(tile: TiledMapTile, tileElement: XmlReader.Element): Unit = {
    val terrain        = tileElement.getAttribute("terrain", Nullable.empty)
    val tileProperties = tile.properties
    terrain.foreach(t => tileProperties.put("terrain", t))
    val probability = tileElement.getAttribute("probability", Nullable.empty)
    probability.foreach(p => tileProperties.put("probability", p))
    val tileType = tileElement.getAttribute("type", Nullable.empty)
    tileType.foreach(t => tileProperties.put("type", t))
    val properties = tileElement.getChildByName("properties")
    properties.foreach(p => loadProperties(tileProperties, p))

    // if there is a 'type' (=class) specified, then check if there are any other
    // class properties available and put their default values into the properties.
    loadMapPropertiesClassDefaults(tileType, tileProperties)
  }

  protected def addTileObjectGroup(tile: TiledMapTile, tileElement: XmlReader.Element): Unit = {
    val objectgroupElement = tileElement.getChildByName("objectgroup")
    objectgroupElement.foreach { og =>
      val objectElements = og.getChildrenByName("object")
      var oi             = 0
      while (oi < objectElements.size) {
        loadObject(map, tile, objectElements(oi))
        oi += 1
      }
    }
  }

  protected def createAnimatedTile(
    tileSet:     TiledMapTileSet,
    tile:        TiledMapTile,
    tileElement: XmlReader.Element,
    firstgid:    Int
  ): Nullable[AnimatedTiledMapTile] = {
    val animationElement = tileElement.getChildByName("animation")
    animationElement.map { ae =>
      val staticTiles   = DynamicArray[StaticTiledMapTile]()
      val intervals     = DynamicArray[Int]()
      val frameElements = ae.getChildrenByName("frame")
      var fi            = 0
      while (fi < frameElements.size) {
        val frameElement = frameElements(fi)
        staticTiles.add(
          tileSet.getTile(firstgid + frameElement.getIntAttribute("tileid", 0)).getOrElse(throw new IllegalStateException("missing tile for animation frame")).asInstanceOf[StaticTiledMapTile]
        )
        intervals.add(frameElement.getIntAttribute("duration", 0))
        fi += 1
      }

      val animatedTile = AnimatedTiledMapTile(intervals.toArray, staticTiles)
      animatedTile.id = tile.id
      animatedTile
    }
  }
}

object BaseTmxMapLoader {

  def getTileIds(element: XmlReader.Element, width: Int, height: Int): Array[Int] = {
    val d        = element.getChildByName("data").getOrElse(throw new IllegalArgumentException("missing data element"))
    val encoding = d.getAttribute("encoding", Nullable.empty)
    if (encoding.isEmpty) { // no 'encoding' attribute means that the encoding is XML
      throw new IllegalArgumentException("Unsupported encoding (XML) for TMX Layer Data")
    }
    val enc = encoding.getOrElse("")
    val ids = new Array[Int](width * height)
    if (enc == "csv") {
      val array = d.text.getOrElse("").split(",")
      var i     = 0
      while (i < array.length) {
        ids(i) = java.lang.Long.parseLong(array(i).trim).toInt
        i += 1
      }
    } else if (enc == "base64") {
      var is: InputStream = null
      try {
        val compression = d.getAttribute("compression", Nullable.empty)
        val bytes       = java.util.Base64.getDecoder().decode(d.text.getOrElse(""))
        val comp        = compression.getOrElse("")
        if (comp.isEmpty)
          is = new ByteArrayInputStream(bytes)
        else if (comp == "gzip")
          is = new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes), bytes.length))
        else if (comp == "zlib")
          is = new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(bytes)))
        else
          throw new IllegalArgumentException("Unrecognised compression (" + comp + ") for TMX Layer Data")

        val temp = new Array[Byte](4)
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
              throw new IllegalArgumentException("Error Reading TMX Layer Data: Premature end of tile data")
            ids(y * width + x) = BaseTiledMapLoader.unsignedByteToInt(temp(0)) |
              (BaseTiledMapLoader.unsignedByteToInt(temp(1)) << 8) |
              (BaseTiledMapLoader.unsignedByteToInt(temp(2)) << 16) |
              (BaseTiledMapLoader.unsignedByteToInt(temp(3)) << 24)
            x += 1
          }
          y += 1
        }
      } catch {
        case e: IOException =>
          throw new IllegalArgumentException("Error Reading TMX Layer Data - IOException: " + e.getMessage)
      } finally
        StreamUtils.closeQuietly(is)
    } else {
      // any other value of 'encoding' is one we're not aware of, probably a feature of a future version of Tiled
      // or another editor
      throw new IllegalArgumentException("Unrecognised encoding (" + enc + ") for TMX Layer Data")
    }
    ids
  }
}
