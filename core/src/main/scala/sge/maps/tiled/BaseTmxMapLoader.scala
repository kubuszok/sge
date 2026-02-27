/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/BaseTmxMapLoader.java
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
import sge.maps.tiled.TiledMapTileLayer.Cell
import sge.maps.tiled.objects.TiledMapTileMapObject
import sge.maps.tiled.tiles.{ AnimatedTiledMapTile, StaticTiledMapTile }
import sge.math.{ Polygon, Polyline }
import sge.utils.{ DynamicArray, JsonValue, Nullable, ObjectMap, ObjectSet, StreamUtils, XmlReader }

import java.io.{ BufferedInputStream, ByteArrayInputStream, IOException, InputStream }
import java.util.zip.{ GZIPInputStream, InflaterInputStream }

abstract class BaseTmxMapLoader[P <: BaseTiledMapLoader.Parameters](resolver: FileHandleResolver) extends BaseTiledMapLoader[P](resolver) {

  protected var xml:  XmlReader                   = new XmlReader()
  protected var root: Nullable[XmlReader.Element] = Nullable.empty

  protected var templateCache: ObjectMap[String, XmlReader.Element] = scala.compiletime.uninitialized

  override def getDependencies(
    fileName:  String,
    tmxFile:   FileHandle,
    parameter: P
  ): DynamicArray[AssetDescriptor[?]] = {
    this.root = Nullable(xml.parse(tmxFile))

    val textureParameter = new TextureLoader.TextureParameter()
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
    this.map = new TiledMap()
    this.idToObject = new scala.collection.mutable.HashMap[Int, MapObject]()
    this.runOnEndOfLoadTiled = DynamicArray[() => Unit]()
    this.templateCache = ObjectMap[String, XmlReader.Element]()

    val param = Nullable(parameter)
    if (param.isDefined) {
      this.convertObjectToTileSpace = param.orNull.convertObjectToTileSpace
      this.flipY = param.orNull.flipY
      loadProjectFile(param.orNull.projectFilePath)
    } else {
      this.convertObjectToTileSpace = false
      this.flipY = true
    }

    val r                  = root.orNull
    val mapOrientation     = r.getAttribute("orientation", Nullable.empty)
    val mapWidth           = r.getIntAttribute("width", 0)
    val mapHeight          = r.getIntAttribute("height", 0)
    val tileWidth          = r.getIntAttribute("tilewidth", 0)
    val tileHeight         = r.getIntAttribute("tileheight", 0)
    val hexSideLength      = r.getIntAttribute("hexsidelength", 0)
    val staggerAxis        = r.getAttribute("staggeraxis", Nullable.empty)
    val staggerIndex       = r.getAttribute("staggerindex", Nullable.empty)
    val mapBackgroundColor = r.getAttribute("backgroundcolor", Nullable.empty)

    val mapProperties = map.getProperties
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
    if (properties.isDefined) {
      loadProperties(map.getProperties, properties.orNull)
    }

    val tilesets = r.getChildrenByName("tileset")
    var ti       = 0
    while (ti < tilesets.size) {
      val element = tilesets(ti)
      loadTileSet(element, tmxFile, imageResolver)
      r.removeChild(element)
      ti += 1
    }

    var i = 0
    val j = r.getChildCount
    while (i < j) {
      val element = r.getChild(i)
      loadLayer(map, map.getLayers, element, tmxFile, imageResolver)
      i += 1
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
      val groupLayer = new MapGroupLayer()
      loadBasicLayerInfo(groupLayer, element)

      val properties = element.getChildByName("properties")
      if (properties.isDefined) {
        loadProperties(groupLayer.getProperties, properties.orNull)
      }

      var i = 0
      val j = element.getChildCount
      while (i < j) {
        val child = element.getChild(i)
        loadLayer(map, groupLayer.getLayers, child, tmxFile, imageResolver)
        i += 1
      }

      for (layer <- groupLayer.getLayers)
        layer.setParent(Nullable(groupLayer))

      parentLayers.add(groupLayer)
    }

  protected def loadTileLayer(map: TiledMap, parentLayers: MapLayers, element: XmlReader.Element): Unit =
    if (element.name == "layer") {
      val width      = element.getIntAttribute("width", 0)
      val height     = element.getIntAttribute("height", 0)
      val tileWidth  = map.getProperties.get("tilewidth", classOf[Integer]).intValue()
      val tileHeight = map.getProperties.get("tileheight", classOf[Integer]).intValue()
      val layer      = new TiledMapTileLayer(width, height, tileWidth, tileHeight)

      loadBasicLayerInfo(layer, element)

      val ids      = BaseTmxMapLoader.getTileIds(element, width, height)
      val tilesets = map.getTileSets
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
            cell.setTile(Nullable(t))
            layer.setCell(x, if (flipY) height - 1 - y else y, Nullable(cell))
          }
          x += 1
        }
        y += 1
      }

      val properties = element.getChildByName("properties")
      if (properties.isDefined) {
        loadProperties(layer.getProperties, properties.orNull)
      }
      parentLayers.add(layer)
    }

  protected def loadObjectGroup(
    map:          TiledMap,
    parentLayers: MapLayers,
    element:      XmlReader.Element,
    tmxFile:      FileHandle
  ): Unit =
    if (element.name == "objectgroup") {
      val layer = new MapLayer()
      loadBasicLayerInfo(layer, element)
      val properties = element.getChildByName("properties")
      if (properties.isDefined) {
        loadProperties(layer.getProperties, properties.orNull)
      }

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
        x = java.lang.Float.parseFloat(element.getAttribute("offsetx", Nullable("0")).orNull)
      else
        x = java.lang.Float.parseFloat(element.getAttribute("x", Nullable("0")).orNull)
      if (element.hasAttribute("offsety"))
        y = java.lang.Float.parseFloat(element.getAttribute("offsety", Nullable("0")).orNull)
      else
        y = java.lang.Float.parseFloat(element.getAttribute("y", Nullable("0")).orNull)
      if (flipY) y = mapHeightInPixels - y

      val repeatX = element.getIntAttribute("repeatx", 0) == 1
      val repeatY = element.getIntAttribute("repeaty", 0) == 1

      var texture: Nullable[TextureRegion] = Nullable.empty

      val image = element.getChildByName("image")

      if (image.isDefined) {
        val source = image.orNull.getAttribute("source")
        val handle = BaseTiledMapLoader.getRelativeFileHandle(tmxFile, source)
        texture = imageResolver.getImage(handle.path())
        texture.foreach(t => y -= t.getRegionHeight())
      }

      val layer = new TiledMapImageLayer(texture.orNull, x, y, repeatX, repeatY)

      loadBasicLayerInfo(layer, element)

      val properties = element.getChildByName("properties")
      if (properties.isDefined) {
        loadProperties(layer.getProperties, properties.orNull)
      }

      parentLayers.add(layer)
    }

  protected def loadBasicLayerInfo(layer: MapLayer, element: XmlReader.Element): Unit = {
    val name      = element.getAttribute("name", Nullable.empty).orNull
    val opacity   = java.lang.Float.parseFloat(element.getAttribute("opacity", Nullable("1.0")).orNull)
    val tintColor = element.getAttribute("tintcolor", Nullable("#ffffffff")).orNull
    val visible   = element.getIntAttribute("visible", 1) == 1
    val offsetX   = element.getFloatAttribute("offsetx", 0)
    val offsetY   = element.getFloatAttribute("offsety", 0)
    val parallaxX = element.getFloatAttribute("parallaxx", 1f)
    val parallaxY = element.getFloatAttribute("parallaxy", 1f)

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

  protected def loadObject(map: TiledMap, layer: MapLayer, element: XmlReader.Element): Unit =
    loadObject(map, layer.getObjects, element, mapHeightInPixels.toFloat)

  protected def loadObject(map: TiledMap, tile: TiledMapTile, element: XmlReader.Element): Unit =
    loadObject(map, tile.getObjects, element, tile.getTextureRegion.getRegionHeight().toFloat)

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

      if (element.getChildCount > 0) {
        var child: Nullable[XmlReader.Element] = Nullable.empty
        child = element.getChildByName("polygon")
        if (child.isDefined) {
          val points   = child.orNull.getAttribute("points").split(" ")
          val vertices = new Array[Float](points.length * 2)
          var i        = 0
          while (i < points.length) {
            val point = points(i).split(",")
            vertices(i * 2) = java.lang.Float.parseFloat(point(0)) * scaleX
            vertices(i * 2 + 1) = java.lang.Float.parseFloat(point(1)) * scaleY * (if (flipY) -1 else 1)
            i += 1
          }
          val polygon = new Polygon(vertices)
          polygon.setPosition(x, y)
          obj = Nullable(new PolygonMapObject(polygon))
        } else {
          child = element.getChildByName("polyline")
          if (child.isDefined) {
            val points   = child.orNull.getAttribute("points").split(" ")
            val vertices = new Array[Float](points.length * 2)
            var i        = 0
            while (i < points.length) {
              val point = points(i).split(",")
              vertices(i * 2) = java.lang.Float.parseFloat(point(0)) * scaleX
              vertices(i * 2 + 1) = java.lang.Float.parseFloat(point(1)) * scaleY * (if (flipY) -1 else 1)
              i += 1
            }
            val polyline = new Polyline(vertices)
            polyline.setPosition(x, y)
            obj = Nullable(new PolylineMapObject(polyline))
          } else {
            child = element.getChildByName("ellipse")
            if (child.isDefined) {
              obj = Nullable(new EllipseMapObject(x, if (flipY) y - height else y, width, height))
            } else {
              child = element.getChildByName("point")
              if (child.isDefined) {
                obj = Nullable(new PointMapObject(x, if (flipY) y - height else y))
              } else {
                child = element.getChildByName("text")
                if (child.isDefined) {
                  val textChild     = child.orNull
                  val textMapObject =
                    new TextMapObject(x, if (flipY) y - height else y, width, height, textChild.getText.orNull)
                  textMapObject.setFontFamily(textChild.getAttribute("fontfamily", Nullable("")).orNull)
                  textMapObject.setPixelSize(textChild.getIntAttribute("pixelSize", 16))
                  textMapObject.setHorizontalAlign(textChild.getAttribute("halign", Nullable("left")).orNull)
                  textMapObject.setVerticalAlign(textChild.getAttribute("valign", Nullable("top")).orNull)
                  textMapObject.setBold(textChild.getIntAttribute("bold", 0) == 1)
                  textMapObject.setItalic(textChild.getIntAttribute("italic", 0) == 1)
                  textMapObject.setUnderline(textChild.getIntAttribute("underline", 0) == 1)
                  textMapObject.setStrikeout(textChild.getIntAttribute("strikeout", 0) == 1)
                  textMapObject.setWrap(textChild.getIntAttribute("wrap", 0) == 1)
                  // When kerning is true, it won't be added as an attribute, it's true by default
                  textMapObject.setKerning(textChild.getIntAttribute("kerning", 1) == 1)
                  // Default color is #000000, not added as attribute
                  val textColor = textChild.getAttribute("color", Nullable("#000000")).orNull
                  textMapObject.setColor(Color.valueOf(BaseTiledMapLoader.tiledColorToLibGDXColor(textColor)))
                  obj = Nullable(textMapObject)
                }
              }
            }
          }
        }
      }
      if (obj.isEmpty) {
        val gid = element.getAttribute("gid", Nullable.empty)
        if (gid.isDefined) {
          val id               = java.lang.Long.parseLong(gid.orNull).toInt
          val flipHorizontally = (id & BaseTiledMapLoader.FLAG_FLIP_HORIZONTALLY) != 0
          val flipVertically   = (id & BaseTiledMapLoader.FLAG_FLIP_VERTICALLY) != 0

          val tile = map.getTileSets.getTile(id & ~BaseTiledMapLoader.MASK_CLEAR)
          tile.foreach { t =>
            val tiledMapTileMapObject = new TiledMapTileMapObject(t, flipHorizontally, flipVertically)
            val textureRegion         = tiledMapTileMapObject.getTextureRegion
            tiledMapTileMapObject.getProperties.put("gid", id: java.lang.Integer)
            tiledMapTileMapObject.setX(x)
            tiledMapTileMapObject.setY(if (flipY) y else y - height)
            val objectWidth  = element.getFloatAttribute("width", textureRegion.orNull.getRegionWidth().toFloat)
            val objectHeight = element.getFloatAttribute("height", textureRegion.orNull.getRegionHeight().toFloat)
            tiledMapTileMapObject.setScaleX(scaleX * (objectWidth / textureRegion.orNull.getRegionWidth()))
            tiledMapTileMapObject.setScaleY(scaleY * (objectHeight / textureRegion.orNull.getRegionHeight()))
            tiledMapTileMapObject.setRotation(element.getFloatAttribute("rotation", 0))
            obj = Nullable(tiledMapTileMapObject)
          }
        }
        if (obj.isEmpty) {
          obj = Nullable(new RectangleMapObject(x, if (flipY) y - height else y, width, height))
        }
      }
      val theObj = obj.orNull
      theObj.setName(element.getAttribute("name", Nullable.empty).orNull)
      val rotation = element.getAttribute("rotation", Nullable.empty)
      if (rotation.isDefined) {
        theObj.getProperties.put("rotation", java.lang.Float.parseFloat(rotation.orNull): java.lang.Float)
      }
      val objType = element.getAttribute("type", Nullable.empty)
      objType.foreach(t => theObj.getProperties.put("type", t))
      val id = element.getIntAttribute("id", 0)
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
      theObj.setVisible(element.getIntAttribute("visible", 1) == 1)
      val properties = element.getChildByName("properties")
      if (properties.isDefined) {
        loadProperties(theObj.getProperties, properties.orNull)
      }

      // if there is a 'type' (=class) specified, then check if there are any other
      // class properties available and put their default values into the properties.
      loadMapPropertiesClassDefaults(objType, theObj.getProperties)

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
    val templateObjectElement = templateElement.orNull.getChildByName("object")
    // Merge the parent map element with its template element
    mergeParentElementWithTemplate(mapElement, templateObjectElement.orNull)
  }

  /** Returns a shallow copy of the source element we pass in. */
  protected def cloneElementShallow(sourceElement: XmlReader.Element): XmlReader.Element = {
    // New element for our copy
    val copyElement = new XmlReader.Element(sourceElement.name, Nullable.empty)
    // Get list of attributes from the source element
    val attrs = sourceElement.getAttributes
    if (attrs.isDefined) {
      attrs.orNull.foreachEntry { (key, value) =>
        copyElement.setAttribute(key, value)
      }
    }
    // Checking for text
    if (sourceElement.getText.isDefined) copyElement.setText(sourceElement.getText)
    copyElement
  }

  /** Merges two <properties> tags from a parent and template. Matching properties from the parent will override the template's.
    */
  protected def mergeProperties(
    parentProps:   XmlReader.Element,
    templateProps: XmlReader.Element
  ): XmlReader.Element = {
    if (templateProps == null) return parentProps // scalastyle:ignore
    if (parentProps == null) return templateProps // scalastyle:ignore
    // Create a new merged properties element which will contain a combination of parent and template properties.
    val merged = new XmlReader.Element("properties", Nullable.empty)
    // Set properties from template
    val templateProperties = templateProps.getChildrenByName("property")
    var ti                 = 0
    while (ti < templateProperties.size) {
      merged.addChild(cloneElementShallow(templateProperties(ti)))
      ti += 1
    }
    // Set properties from the parent, matching ones from template will be overridden
    val parentProperties = parentProps.getChildrenByName("property")
    var pi               = 0
    while (pi < parentProperties.size) {
      val property = parentProperties(pi)
      val name     = property.getAttribute("name", Nullable.empty)
      // Find & remove a duplicate by name, if any
      var existing: XmlReader.Element = null // scalastyle:ignore
      var i = 0
      while (i < merged.getChildCount) {
        val child = merged.getChild(i)
        if ("property" == child.name && name.isDefined && name.orNull == child.getAttribute("name", Nullable.empty).orNull) {
          existing = child
        }
        i += 1
      }
      if (existing != null) merged.removeChild(existing)
      merged.addChild(cloneElementShallow(property))
      pi += 1
    }
    merged
  }

  /** Recursively merges a "parent" (map) object element with its referenced template object element. Attributes and properties found in the template are allowed to be overwritten by any matching ones
    * found in its parent element. The returned element is a new detached tree (parent = null) so it can be handed straight to the loadObject() method without issues.
    */
  protected def mergeParentElementWithTemplate(
    parent:   XmlReader.Element,
    template: XmlReader.Element
  ): XmlReader.Element = {
    if (template == null) return parent // scalastyle:ignore
    if (parent == null) return template // scalastyle:ignore
    // Create a new merged element which will contain a combination of parent and template attributes, properties etc...
    val merged = new XmlReader.Element(template.name, Nullable.empty)
    // Set attributes from template
    val templateAttrs = template.getAttributes
    if (templateAttrs.isDefined) {
      templateAttrs.orNull.foreachEntry { (k, v) =>
        merged.setAttribute(k, v)
      }
    }
    // Set attributes from the parent, matching ones from template will be overridden
    val parentAttrs = parent.getAttributes
    if (parentAttrs.isDefined) {
      parentAttrs.orNull.foreachEntry { (k, v) =>
        merged.setAttribute(k, v)
      }
    }
    // Specifically added for TextMapObjects since they are unique compared to other objects.
    val txt =
      if (parent.getText.isDefined && parent.getText.orNull.length > 0) parent.getText else template.getText
    if (txt.isDefined) {
      merged.setText(txt)
    }
    // Handle Child Elements
    // Collect all child tag names that appear in either element
    val tagNames = ObjectSet[String]()
    var i        = 0
    while (i < template.getChildCount) {
      tagNames.add(template.getChild(i).name)
      i += 1
    }
    i = 0
    while (i < parent.getChildCount) {
      tagNames.add(parent.getChild(i).name)
      i += 1
    }

    tagNames.foreach { tag =>
      val mapChild  = parent.getChildByName(tag)
      val tmplChild = template.getChildByName(tag)

      /** Look for properties tags so we can merge those as well. Recursive check if properties is not found. */
      val mergedChild =
        if ("properties" == tag) mergeProperties(mapChild.orNull, tmplChild.orNull)
        else mergeParentElementWithTemplate(mapChild.orNull, tmplChild.orNull)
      merged.addChild(mergedChild)
    }
    merged
  }
  /* * End of Tiled Template Loading Section * */

  protected def loadProperties(properties: MapProperties, element: XmlReader.Element): Unit = {
    if (element == null) return // scalastyle:ignore
    if (element.name == "properties") {
      val propertyElements = element.getChildrenByName("property")
      var pi               = 0
      while (pi < propertyElements.size) {
        val property = propertyElements(pi)
        val name     = property.getAttribute("name", Nullable.empty).orNull
        var value    = getPropertyValue(property)
        val propType = property.getAttribute("type", Nullable.empty)
        if (propType.isDefined && "object" == propType.orNull) {
          loadObjectProperty(properties, name, value)
        } else if (propType.isDefined && "class" == propType.orNull) {
          // A 'class' property is a property which is itself a set of properties
          val classProperties = new MapProperties()
          val className       = property.getAttribute("propertytype")
          classProperties.put("type", className)
          // the actual properties of a 'class' property are stored as a new properties tag
          properties.put(name, classProperties)
          val classPropsElement = property.getChildByName("properties")
          loadClassProperties(className, classProperties, classPropsElement.orNull)
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
    classElement:    XmlReader.Element
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
      val propName  = projectClassMember.name
      val classProp =
        if (classElement == null) null
        else getPropertyByName(classElement, propName)
      projectClassMember.`type` match {
        case "object" =>
          val value =
            if (classProp == null) projectClassMember.defaultValue.flatMap(_.asString()).orNull
            else getPropertyValue(classProp)
          loadObjectProperty(classProperties, propName, value)
        case "class" =>
          // A 'class' property is a property which is itself a set of properties
          val nestedClassProperties = new MapProperties()
          val nestedClassName       = projectClassMember.propertyType.orNull
          nestedClassProperties.put("type", nestedClassName)
          // the actual properties of a 'class' property are stored as a new properties tag
          classProperties.put(propName, nestedClassProperties)
          if (classProp == null) {
            // no class values overridden -> use default class values
            loadJsonClassProperties(nestedClassName, nestedClassProperties, projectClassMember.defaultValue)
          } else {
            loadClassProperties(nestedClassName, nestedClassProperties, classProp)
          }
        case _ =>
          val value =
            if (classProp == null) projectClassMember.defaultValue.flatMap(_.asString()).orNull
            else getPropertyValue(classProp)
          loadBasicProperty(classProperties, propName, value, Nullable(projectClassMember.`type`))
      }
    }
  }

  private def getPropertyValue(classProp: XmlReader.Element): String = {
    val attrValue = classProp.getAttribute("value", Nullable.empty)
    if (attrValue.isDefined) attrValue.orNull else classProp.getText.orNull
  }

  protected def getPropertyByName(classElement: XmlReader.Element, propName: String): XmlReader.Element = {
    // we use getChildrenByNameRecursively here because in case of nested classes,
    // we get an element with a root property (=class) and inside additional property tags for the real
    // class properties. If we just use getChildrenByName we don't get any children for a nested class.
    val properties = classElement.getChildrenByNameRecursively("property")
    var i          = 0
    while (i < properties.size) {
      val property = properties(i)
      if (propName == property.getAttribute("name")) {
        return property // scalastyle:ignore
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
      if (source.isDefined) {
        val tsx = BaseTiledMapLoader.getRelativeFileHandle(tmxFile, source.orNull)
        try {
          el = xml.parse(tsx)
          val imageElement = el.getChildByName("image")
          if (imageElement.isDefined) {
            imageSource = imageElement.orNull.getAttribute("source")
            imageWidth = imageElement.orNull.getIntAttribute("width", 0)
            imageHeight = imageElement.orNull.getIntAttribute("height", 0)
            image = BaseTiledMapLoader.getRelativeFileHandle(tsx, imageSource)
          }
        } catch {
          case e: Exception =>
            throw new IllegalArgumentException("Error parsing external tileset.", e)
        }
      } else {
        val imageElement = el.getChildByName("image")
        if (imageElement.isDefined) {
          imageSource = imageElement.orNull.getAttribute("source")
          imageWidth = imageElement.orNull.getIntAttribute("width", 0)
          imageHeight = imageElement.orNull.getIntAttribute("height", 0)
          image = BaseTiledMapLoader.getRelativeFileHandle(tmxFile, imageSource)
        }
      }
      val name       = el.get("name", Nullable.empty).orNull
      val tilewidth  = el.getIntAttribute("tilewidth", 0)
      val tileheight = el.getIntAttribute("tileheight", 0)
      val spacing    = el.getIntAttribute("spacing", 0)
      val margin     = el.getIntAttribute("margin", 0)

      val offset  = el.getChildByName("tileoffset")
      var offsetX = 0
      var offsetY = 0
      if (offset.isDefined) {
        offsetX = offset.orNull.getIntAttribute("x", 0)
        offsetY = offset.orNull.getIntAttribute("y", 0)
      }
      val tileSet = new TiledMapTileSet()

      // TileSet
      tileSet.setName(name)
      val tileSetProperties = tileSet.getProperties
      val properties        = el.getChildByName("properties")
      if (properties.isDefined) {
        loadProperties(tileSetProperties, properties.orNull)
      }
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
        source.orNull,
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
        tileSet.putTile(animatedTile.getId, animatedTile)

      map.getTileSets.addTileSet(tileSet)
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
    val tileProperties = tile.getProperties
    if (terrain.isDefined) {
      tileProperties.put("terrain", terrain.orNull)
    }
    val probability = tileElement.getAttribute("probability", Nullable.empty)
    if (probability.isDefined) {
      tileProperties.put("probability", probability.orNull)
    }
    val tileType = tileElement.getAttribute("type", Nullable.empty)
    tileType.foreach(t => tileProperties.put("type", t))
    val properties = tileElement.getChildByName("properties")
    if (properties.isDefined) {
      loadProperties(tileProperties, properties.orNull)
    }

    // if there is a 'type' (=class) specified, then check if there are any other
    // class properties available and put their default values into the properties.
    loadMapPropertiesClassDefaults(tileType, tileProperties)
  }

  protected def addTileObjectGroup(tile: TiledMapTile, tileElement: XmlReader.Element): Unit = {
    val objectgroupElement = tileElement.getChildByName("objectgroup")
    if (objectgroupElement.isDefined) {
      val objectElements = objectgroupElement.orNull.getChildrenByName("object")
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
    if (animationElement.isDefined) {
      val staticTiles   = DynamicArray[StaticTiledMapTile]()
      val intervals     = DynamicArray[Int]()
      val frameElements = animationElement.orNull.getChildrenByName("frame")
      var fi            = 0
      while (fi < frameElements.size) {
        val frameElement = frameElements(fi)
        staticTiles.add(tileSet.getTile(firstgid + frameElement.getIntAttribute("tileid", 0)).orNull.asInstanceOf[StaticTiledMapTile])
        intervals.add(frameElement.getIntAttribute("duration", 0))
        fi += 1
      }

      val animatedTile = new AnimatedTiledMapTile(intervals.toArray, staticTiles)
      animatedTile.setId(tile.getId)
      Nullable(animatedTile)
    } else {
      Nullable.empty
    }
  }
}

object BaseTmxMapLoader {

  def getTileIds(element: XmlReader.Element, width: Int, height: Int): Array[Int] = {
    val data     = element.getChildByName("data")
    val encoding = data.orNull.getAttribute("encoding", Nullable.empty)
    if (encoding.isEmpty) { // no 'encoding' attribute means that the encoding is XML
      throw new IllegalArgumentException("Unsupported encoding (XML) for TMX Layer Data")
    }
    val ids = new Array[Int](width * height)
    if (encoding.orNull == "csv") {
      val array = data.orNull.getText.orNull.split(",")
      var i     = 0
      while (i < array.length) {
        ids(i) = java.lang.Long.parseLong(array(i).trim).toInt
        i += 1
      }
    } else if (encoding.orNull == "base64") {
      var is: InputStream = null
      try {
        val compression = data.orNull.getAttribute("compression", Nullable.empty)
        val bytes       = java.util.Base64.getDecoder.decode(data.orNull.getText.orNull)
        if (compression.isEmpty)
          is = new ByteArrayInputStream(bytes)
        else if (compression.orNull == "gzip")
          is = new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes), bytes.length))
        else if (compression.orNull == "zlib")
          is = new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(bytes)))
        else
          throw new IllegalArgumentException("Unrecognised compression (" + compression.orNull + ") for TMX Layer Data")

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
      throw new IllegalArgumentException("Unrecognised encoding (" + encoding.orNull + ") for TMX Layer Data")
    }
    ids
  }
}
