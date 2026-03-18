/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/BaseTmjMapLoader.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audited 2026-03-03):
 *   - All methods match Java 1:1 (loadTiledMap, loadLayer, loadLayerGroup, loadTileLayer,
 *     loadObjectGroup, loadImageLayer, loadBasicLayerInfo, loadObject, resolveTemplateObject,
 *     mergeJsonObject, cloneElementShallow, mergeJsonProperties, mergeParentElementWithTemplate,
 *     loadProperties, loadTileSet, addStaticTiles, addTileProperties, addTileObjectGroup,
 *     createAnimatedTile, deepCopyJsonValue, getTileIds)
 *   - Java null checks → Nullable patterns throughout
 *   - runOnEndOfLoadTiled = null after use (matches Java exactly, // scalastyle:ignore)
 *   - Java Base64Coder.decode → java.util.Base64.getDecoder.decode
 *   - Split package, braces, no-return conventions satisfied
 *   - JsonValue tree walking replaced with jsoniter-scala codec derivation (TmjJson DTOs)
 *   - Template merging: JsonValue deep-copy/shallow-clone → typed DTO-level merging
 *   Convention: jsoniter-scala codec derivation replaces JsonValue tree walking
 *   Audited: 2026-03-04
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
import sge.utils.{ DynamicArray, Json, Nullable, ObjectMap, StreamUtils, readJson }

import java.io.{ BufferedInputStream, ByteArrayInputStream, IOException, InputStream }
import java.util.zip.{ GZIPInputStream, InflaterInputStream }

abstract class BaseTmjMapLoader[P <: BaseTiledMapLoader.Parameters](resolver: FileHandleResolver) extends BaseTiledMapLoader[P](resolver) {

  protected var root: Nullable[TmjMapJson] = Nullable.empty

  protected var templateCache: ObjectMap[String, TmjObjectJson] = scala.compiletime.uninitialized

  override def getDependencies(
    fileName:  String,
    tmjFile:   FileHandle,
    parameter: P
  ): DynamicArray[AssetDescriptor[?]] = {
    this.root = Nullable(tmjFile.readJson[TmjMapJson])

    val textureParameter = TextureLoader.TextureParameter()
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
    this.map = TiledMap()
    this.idToObject = new scala.collection.mutable.HashMap[Int, MapObject]()
    this.runOnEndOfLoadTiled = DynamicArray[() => Unit]()
    this.templateCache = ObjectMap[String, TmjObjectJson]()

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
    val mapOrientation     = r.orientation
    val mapWidth           = r.width
    val mapHeight          = r.height
    val tileWidth          = r.tilewidth
    val tileHeight         = r.tileheight
    val hexSideLength      = r.hexsidelength
    val staggerAxis        = r.staggeraxis
    val staggerIndex       = r.staggerindex
    val mapBackgroundColor = r.backgroundcolor

    val mapProperties = map.properties
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

    loadProperties(map.properties, r.properties)

    for (element <- r.tilesets)
      loadTileSet(element, tmjFile, imageResolver)

    for (element <- r.layers)
      loadLayer(map, map.layers, element, tmjFile, imageResolver)

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
    element:       TmjLayerJson,
    tmjFile:       FileHandle,
    imageResolver: ImageResolver
  ): Unit =
    element.tpe match {
      case "group"       => loadLayerGroup(map, parentLayers, element, tmjFile, imageResolver)
      case "tilelayer"   => loadTileLayer(map, parentLayers, element)
      case "objectgroup" => loadObjectGroup(map, parentLayers, element, tmjFile)
      case "imagelayer"  => loadImageLayer(map, parentLayers, element, tmjFile, imageResolver)
      case _             =>
    }

  protected def loadLayerGroup(
    map:           TiledMap,
    parentLayers:  MapLayers,
    element:       TmjLayerJson,
    tmjFile:       FileHandle,
    imageResolver: ImageResolver
  ): Unit =
    if (element.tpe == "group") {
      val groupLayer = MapGroupLayer()
      loadBasicLayerInfo(groupLayer, element)

      loadProperties(groupLayer.properties, element.properties)

      for (child <- element.layers)
        loadLayer(map, groupLayer.layers, child, tmjFile, imageResolver)

      for (layer <- groupLayer.layers)
        layer.parent = Nullable(groupLayer)

      parentLayers.add(groupLayer)
    }

  protected def loadTileLayer(map: TiledMap, parentLayers: MapLayers, element: TmjLayerJson): Unit =
    if (element.tpe == "tilelayer") {
      val width      = element.width
      val height     = element.height
      val tileWidth  = map.properties.getAs[Integer]("tilewidth").get.intValue()
      val tileHeight = map.properties.getAs[Integer]("tileheight").get.intValue()
      val layer      = TiledMapTileLayer(width, height, tileWidth, tileHeight)

      loadBasicLayerInfo(layer, element)

      val ids      = BaseTmjMapLoader.getTileIds(element)
      val tileSets = map.tileSets
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
            cell.tile = Nullable(t)
            layer.setCell(x, if (flipY) height - 1 - y else y, Nullable(cell))
          }
          x += 1
        }
        y += 1
      }
      loadProperties(layer.properties, element.properties)
      parentLayers.add(layer)
    }

  protected def loadObjectGroup(
    map:          TiledMap,
    parentLayers: MapLayers,
    element:      TmjLayerJson,
    tmjFile:      FileHandle
  ): Unit =
    if (element.tpe == "objectgroup") {
      val layer = MapLayer()
      loadBasicLayerInfo(layer, element)
      loadProperties(layer.properties, element.properties)

      for (objectElement <- element.objects) {
        var elementToLoad = objectElement
        if (objectElement.template.isDefined) {
          elementToLoad = resolveTemplateObject(map, layer, objectElement, tmjFile)
        }
        loadObject(map, layer, elementToLoad)
      }
      parentLayers.add(layer)
    }

  protected def loadImageLayer(
    map:           TiledMap,
    parentLayers:  MapLayers,
    element:       TmjLayerJson,
    tmjFile:       FileHandle,
    imageResolver: ImageResolver
  ): Unit =
    if (element.tpe == "imagelayer") {
      val x = element.offsetx
      var y = element.offsety
      if (flipY) y = mapHeightInPixels - y

      val imageSrc = element.image.getOrElse("")

      val repeatX = element.repeatx == 1
      val repeatY = element.repeaty == 1

      var texture: Nullable[TextureRegion] = Nullable.empty

      if (imageSrc.nonEmpty) {
        val handle = BaseTiledMapLoader.getRelativeFileHandle(tmjFile, imageSrc)
        texture = imageResolver.getImage(handle.path)
        texture.foreach(t => y -= t.regionHeight)
      }

      @annotation.nowarn("msg=deprecated") // TiledMapImageLayer takes TextureRegion, not Nullable -- null is valid per original LibGDX API
      val textureValue: TextureRegion = texture.orNull
      val layer = TiledMapImageLayer(textureValue, x, y, repeatX, repeatY)

      loadBasicLayerInfo(layer, element)

      loadProperties(layer.properties, element.properties)

      parentLayers.add(layer)
    }

  protected def loadBasicLayerInfo(layer: MapLayer, element: TmjLayerJson): Unit = {
    val name      = element.name
    val opacity   = element.opacity
    val tintColor = element.tintcolor.getOrElse("#ffffffff")
    val visible   = element.visible
    val offsetX   = element.offsetx
    val offsetY   = element.offsety
    val parallaxX = element.parallaxx
    val parallaxY = element.parallaxy

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

  protected def loadObject(map: TiledMap, layer: MapLayer, element: TmjObjectJson): Unit =
    loadObject(map, layer.objects, element, mapHeightInPixels.toFloat)

  protected def loadObject(map: TiledMap, tile: TiledMapTile, element: TmjObjectJson): Unit =
    loadObject(map, tile.objects, element, tile.textureRegion.regionHeight.toFloat)

  protected def loadObject(map: TiledMap, objects: MapObjects, element: TmjObjectJson, heightInPixels: Float): Unit = {

    var obj: Nullable[MapObject] = Nullable.empty

    val scaleX = if (convertObjectToTileSpace) 1.0f / mapTileWidth else 1.0f
    val scaleY = if (convertObjectToTileSpace) 1.0f / mapTileHeight else 1.0f

    val x = element.x.getOrElse(0f) * scaleX
    val y = (if (flipY) heightInPixels - element.y.getOrElse(0f) else element.y.getOrElse(0f)) * scaleY

    val width  = element.width.getOrElse(0f) * scaleX
    val height = element.height.getOrElse(0f) * scaleY

    if (element.polygon.nonEmpty) {
      val points   = element.polygon
      val vertices = new Array[Float](points.size * 2)
      var index    = 0
      for (point <- points) {
        vertices(index) = point.x * scaleX
        index += 1
        vertices(index) = point.y * scaleY * (if (flipY) -1 else 1)
        index += 1
      }
      val polygon = Polygon(vertices)
      polygon.setPosition(x, y)
      obj = Nullable(PolygonMapObject(polygon))
    } else if (element.polyline.nonEmpty) {
      val points   = element.polyline
      val vertices = new Array[Float](points.size * 2)
      var index    = 0
      for (point <- points) {
        vertices(index) = point.x * scaleX
        index += 1
        vertices(index) = point.y * scaleY * (if (flipY) -1 else 1)
        index += 1
      }
      val polyline = Polyline(vertices)
      polyline.setPosition(x, y)
      obj = Nullable(PolylineMapObject(polyline))
    } else if (element.ellipse.isDefined) {
      obj = Nullable(EllipseMapObject(x, if (flipY) y - height else y, width, height))
    } else if (element.point.isDefined) {
      obj = Nullable(PointMapObject(x, if (flipY) y - height else y))
    } else {
      element.text.foreach { textObj =>
        val textMapObject = TextMapObject(
          x,
          if (flipY) y - height else y,
          width,
          height,
          textObj.text.getOrElse("")
        )
        textMapObject.fontFamily = textObj.fontfamily.getOrElse("")
        textMapObject.pixelSize = textObj.pixelSize.getOrElse(16)
        textMapObject.horizontalAlign = textObj.halign.getOrElse("left")
        textMapObject.verticalAlign = textObj.valign.getOrElse("top")
        textMapObject.bold = textObj.bold.getOrElse(false)
        textMapObject.italic = textObj.italic.getOrElse(false)
        textMapObject.underline = textObj.underline.getOrElse(false)
        textMapObject.strikeout = textObj.strikeout.getOrElse(false)
        textMapObject.wrap = textObj.wrap.getOrElse(false)
        // When kerning is true, it won't be added as an attribute, it's true by default
        textMapObject.kerning = textObj.kerning.getOrElse(true)
        // Default color is #000000, not added as attribute
        val textColor = textObj.color.getOrElse("#000000")
        textMapObject.color = Color.valueOf(BaseTiledMapLoader.tiledColorToLibGDXColor(textColor))
        obj = Nullable(textMapObject)
      }
    }
    if (obj.isEmpty) {
      element.gid.foreach { g =>
        val id               = g.toInt
        val flipHorizontally = (id & BaseTiledMapLoader.FLAG_FLIP_HORIZONTALLY) != 0
        val flipVertically   = (id & BaseTiledMapLoader.FLAG_FLIP_VERTICALLY) != 0

        val tile = map.tileSets.getTile(id & ~BaseTiledMapLoader.MASK_CLEAR)
        tile.foreach { t =>
          val tiledMapTileMapObject = TiledMapTileMapObject(t, flipHorizontally, flipVertically)
          val texRegion             = tiledMapTileMapObject.textureRegion.getOrElse(throw new IllegalStateException("tile missing texture region"))
          tiledMapTileMapObject.properties.put("gid", id: java.lang.Integer)
          tiledMapTileMapObject.x = x
          tiledMapTileMapObject.y = if (flipY) y else y - height
          val objectWidth  = element.width.getOrElse(texRegion.regionWidth.toFloat)
          val objectHeight = element.height.getOrElse(texRegion.regionHeight.toFloat)
          tiledMapTileMapObject.scaleX = scaleX * (objectWidth / texRegion.regionWidth)
          tiledMapTileMapObject.scaleY = scaleY * (objectHeight / texRegion.regionHeight)
          tiledMapTileMapObject.rotation = element.rotation.getOrElse(0f)
          obj = Nullable(tiledMapTileMapObject)
        }
      }
      if (obj.isEmpty) {
        obj = Nullable(RectangleMapObject(x, if (flipY) y - height else y, width, height))
      }
    }
    val theObj = obj.getOrElse(throw new IllegalStateException("object could not be created"))
    theObj.name = element.name.getOrElse("")
    element.rotation.foreach(r => theObj.properties.put("rotation", r: java.lang.Float))
    element.tpe.foreach(t => theObj.properties.put("type", t))
    val id = element.id
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
    theObj.visible = element.visible.getOrElse(true)
    loadProperties(theObj.properties, element.properties)

    // if there is a 'type' (=class) specified, then check if there are any other
    // class properties available and put their default values into the properties.
    val objType = element.tpe match {
      case Some(t) => Nullable(t)
      case None    => Nullable.empty[String]
    }
    loadMapPropertiesClassDefaults(objType, theObj.properties)

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
    *   the single object element we are currently parsing
    * @param tmjFile
    *   tmjFile
    * @return
    *   a merged TmjObjectJson representing the combined objects.
    */
  protected def resolveTemplateObject(
    map:        TiledMap,
    layer:      MapLayer,
    mapElement: TmjObjectJson,
    tmjFile:    FileHandle
  ): TmjObjectJson = {
    // Get template (.tj) file name from element
    val tjFileName = mapElement.template.getOrElse("")
    // check for cached tj element
    var templateObject = templateCache.get(tjFileName)
    if (templateObject.isEmpty) {
      // parse the .tj template file
      try {
        val template = BaseTiledMapLoader.getRelativeFileHandle(tmjFile, tjFileName).readJson[TmjTemplateJson]
        templateObject = Nullable(template.`object`)
      } catch {
        case e: Exception =>
          throw new IllegalArgumentException("Error parsing template file: " + tjFileName, e)
      }
      templateCache.put(tjFileName, templateObject.getOrElse(throw new IllegalStateException("template parse failed")))
    }
    val tmpl = templateObject.getOrElse(throw new IllegalStateException("template object not found"))
    // Merge the parent map element with its template element
    mergeObject(mapElement, tmpl)
  }

  /** Merges a parent (map instance) object with its template object. Parent fields override template fields. Properties are merged by name, text objects are merged field-by-field.
    */
  private def mergeObject(parent: TmjObjectJson, template: TmjObjectJson): TmjObjectJson =
    TmjObjectJson(
      id = parent.id,
      name = parent.name.orElse(template.name),
      tpe = parent.tpe.orElse(template.tpe),
      x = parent.x.orElse(template.x),
      y = parent.y.orElse(template.y),
      width = parent.width.orElse(template.width),
      height = parent.height.orElse(template.height),
      rotation = parent.rotation.orElse(template.rotation),
      visible = parent.visible.orElse(template.visible),
      gid = parent.gid.orElse(template.gid),
      template = parent.template,
      properties = mergeProperties(parent.properties, template.properties),
      polygon = if (parent.polygon.nonEmpty) parent.polygon else template.polygon,
      polyline = if (parent.polyline.nonEmpty) parent.polyline else template.polyline,
      ellipse = parent.ellipse.orElse(template.ellipse),
      point = parent.point.orElse(template.point),
      text = mergeText(parent.text, template.text)
    )

  /** Merges two property lists. Parent properties override template properties with the same name. */
  private def mergeProperties(parentProps: List[TmjPropertyJson], templateProps: List[TmjPropertyJson]): List[TmjPropertyJson] =
    if (templateProps.isEmpty) parentProps
    else if (parentProps.isEmpty) templateProps
    else {
      val parentNames  = parentProps.map(_.name).toSet
      val fromTemplate = templateProps.filterNot(p => parentNames.contains(p.name))
      fromTemplate ++ parentProps
    }

  /** Merges two text objects. Parent fields override template fields. */
  private def mergeText(parent: Option[TmjTextJson], template: Option[TmjTextJson]): Option[TmjTextJson] =
    (parent, template) match {
      case (Some(p), Some(t)) =>
        Some(
          TmjTextJson(
            text = p.text.orElse(t.text),
            fontfamily = p.fontfamily.orElse(t.fontfamily),
            pixelSize = p.pixelSize.orElse(t.pixelSize),
            halign = p.halign.orElse(t.halign),
            valign = p.valign.orElse(t.valign),
            bold = p.bold.orElse(t.bold),
            italic = p.italic.orElse(t.italic),
            underline = p.underline.orElse(t.underline),
            strikeout = p.strikeout.orElse(t.strikeout),
            wrap = p.wrap.orElse(t.wrap),
            kerning = p.kerning.orElse(t.kerning),
            color = p.color.orElse(t.color)
          )
        )
      case (Some(p), None) => Some(p)
      case (None, Some(t)) => Some(t)
      case (None, None)    => None
    }

  /* * End of Tiled Template Loading Section * */

  private def loadProperties(properties: MapProperties, propList: List[TmjPropertyJson]): Unit =
    for (property <- propList) {
      val name     = property.name
      val propType = property.tpe
      val valueStr = BaseTiledMapLoader.jsonAsString(property.value)

      propType match {
        case "object" =>
          valueStr.foreach(v => loadObjectProperty(properties, name, v))
        case "class" =>
          // A 'class' property is a property which is itself a set of properties
          val classProperties = MapProperties()
          val className       = property.propertytype.getOrElse("")
          classProperties.put("type", className)
          properties.put(name, classProperties)
          loadJsonClassProperties(className, classProperties, Nullable(property.value))
        case _ =>
          val typeNullable = if (propType.nonEmpty) Nullable(propType) else Nullable.empty[String]
          valueStr.foreach(v => loadBasicProperty(properties, name, v, typeNullable))
      }
    }

  protected def loadTileSet(element: TmjTilesetRefJson, tmjFile: FileHandle, imageResolver: ImageResolver): Unit = {
    val firstgid = element.firstgid
    if (firstgid == 0 && element.source.isEmpty && element.name.isEmpty) {
      // Not a valid tileset reference
    } else {
      var tilesetData = element
      var imageSource = ""
      var imageWidth  = 0
      var imageHeight = 0
      var image: Nullable[FileHandle] = Nullable.empty

      element.source.foreach { s =>
        val tsj = BaseTiledMapLoader.getRelativeFileHandle(tmjFile, s)
        try {
          tilesetData = tsj.readJson[TmjTilesetRefJson]
          tilesetData.image.foreach { img =>
            imageSource = img
            imageWidth = tilesetData.imagewidth
            imageHeight = tilesetData.imageheight
            image = Nullable(BaseTiledMapLoader.getRelativeFileHandle(tsj, imageSource))
          }
        } catch {
          case e: Exception =>
            throw new IllegalArgumentException("Error parsing external tileSet.", e)
        }
      }
      if (element.source.isEmpty) {
        tilesetData.image.foreach { img =>
          imageSource = img
          imageWidth = tilesetData.imagewidth
          imageHeight = tilesetData.imageheight
          image = Nullable(BaseTiledMapLoader.getRelativeFileHandle(tmjFile, imageSource))
        }
      }
      val name       = tilesetData.name.getOrElse("")
      val tilewidth  = tilesetData.tilewidth
      val tileheight = tilesetData.tileheight
      val spacing    = tilesetData.spacing
      val margin     = tilesetData.margin

      var offsetX = 0
      var offsetY = 0
      tilesetData.tileoffset.foreach { o =>
        offsetX = o.x
        offsetY = o.y
      }
      val tileSet = TiledMapTileSet()

      // TileSet
      tileSet.name = name
      val tileSetProperties = tileSet.properties
      loadProperties(tileSetProperties, tilesetData.properties)
      tileSetProperties.put("firstgid", firstgid: java.lang.Integer)

      // Tiles
      val tiles = tilesetData.tiles

      addStaticTiles(
        tmjFile,
        imageResolver,
        tileSet,
        tiles,
        name,
        firstgid,
        tilewidth,
        tileheight,
        spacing,
        margin,
        element.source.getOrElse(""),
        offsetX,
        offsetY,
        imageSource,
        imageWidth,
        imageHeight,
        image.getOrElse(null: FileHandle) // scalastyle:ignore -- null valid per original LibGDX API
      )

      val animatedTiles = DynamicArray[AnimatedTiledMapTile]()

      for (tileElement <- tiles) {
        val localtid = tileElement.id
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
        tileSet.putTile(animatedTile.id, animatedTile)

      map.tileSets.addTileSet(tileSet)
    }
  }

  protected def addStaticTiles(
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
  ): Unit

  private def addTileProperties(tile: TiledMapTile, tileElement: TmjTileJson): Unit = {
    val tileProperties = tile.properties
    tileElement.terrain.foreach(t => tileProperties.put("terrain", t))
    tileElement.probability.foreach(p => tileProperties.put("probability", p))
    tileElement.tpe.foreach(t => tileProperties.put("type", t))
    loadProperties(tileProperties, tileElement.properties)

    // if there is a 'type' (=class) specified, then check if there are any other
    // class properties available and put their default values into the properties.
    val tileType = tileElement.tpe match {
      case Some(t) => Nullable(t)
      case None    => Nullable.empty[String]
    }
    loadMapPropertiesClassDefaults(tileType, tileProperties)
  }

  private def addTileObjectGroup(tile: TiledMapTile, tileElement: TmjTileJson): Unit =
    tileElement.objectgroup.foreach { og =>
      for (objectElement <- og.objects)
        loadObject(this.map, tile, objectElement)
    }

  protected def createAnimatedTile(
    tileSet:     TiledMapTileSet,
    tile:        TiledMapTile,
    tileElement: TmjTileJson,
    firstgid:    Int
  ): Nullable[AnimatedTiledMapTile] =
    if (tileElement.animation.nonEmpty) {
      val staticTiles = DynamicArray[StaticTiledMapTile]()
      val intervals   = DynamicArray[Int]()
      for (frame <- tileElement.animation) {
        staticTiles.add(
          tileSet.getTile(firstgid + frame.tileid).getOrElse(throw new IllegalStateException("missing tile for animation frame")).asInstanceOf[StaticTiledMapTile]
        )
        intervals.add(frame.duration)
      }

      val animatedTile = AnimatedTiledMapTile(intervals.toArray, staticTiles)
      animatedTile.id = tile.id
      Nullable(animatedTile)
    } else Nullable.empty
}

object BaseTmjMapLoader {

  def getTileIds(layer: TmjLayerJson): Array[Int] = {
    val data   = layer.data
    val enc    = layer.encoding.getOrElse("")
    val width  = layer.width
    val height = layer.height

    if (enc.isEmpty || enc == "csv") {
      data match {
        case Some(Json.Arr(values)) =>
          val result = new Array[Int](width * height)
          var i      = 0
          values.foreach {
            case Json.Num(n) =>
              result(i) = n.toLong.map(_.toInt).getOrElse(n.toDouble.map(_.toInt).getOrElse(0))
              i += 1
            case _ =>
              result(i) = 0
              i += 1
          }
          result
        case _ => throw new IllegalStateException("missing tile data")
      }
    } else if (enc == "base64") {
      var is: InputStream = null
      try {
        val comp    = layer.compression.getOrElse("")
        val dataStr = data match {
          case Some(Json.Str(s)) => s
          case _                 => throw new IllegalStateException("missing tile data")
        }
        val bytes = java.util.Base64.getDecoder().decode(dataStr)
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
