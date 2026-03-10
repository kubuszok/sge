/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Skin.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: null -> Nullable; Disposable -> AutoCloseable; ObjectMap -> scala.collection.mutable.Map; GdxRuntimeException -> SgeError; boundary/break
 *   Renames: JsonValue tree walking -> kindlings Json AST; reflection-based field setting -> SkinStyleReader type class
 *   Idiom: split packages
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import hearth.kindlings.jsoniterjson.codec.JsonCodec.given

import sge.files.FileHandle
import sge.graphics.Color
import sge.graphics.Texture
import sge.graphics.g2d.BitmapFont
import sge.graphics.g2d.BitmapFontData
import sge.graphics.g2d.NinePatch
import sge.graphics.g2d.Sprite
import sge.graphics.g2d.TextureAtlas
import sge.graphics.g2d.TextureAtlas.AtlasRegion
import sge.graphics.g2d.TextureAtlas.AtlasSprite
import sge.graphics.g2d.TextureRegion
import sge.scenes.scene2d.utils.BaseDrawable
import sge.scenes.scene2d.utils.Drawable
import sge.scenes.scene2d.utils.NinePatchDrawable
import sge.scenes.scene2d.utils.SpriteDrawable
import sge.scenes.scene2d.utils.TextureRegionDrawable
import sge.scenes.scene2d.utils.TiledDrawable
import sge.utils.DynamicArray
import sge.utils.Json
import sge.utils.Nullable
import sge.utils.SgeError
import sge.utils.readJson

import scala.reflect.ClassTag

import scala.collection.mutable
import scala.util.boundary
import scala.util.boundary.break

/** A skin stores resources for UI widgets to use (texture regions, ninepatches, fonts, colors, etc). Resources are named and can be looked up by name and type. Resources can be described in JSON.
  * Skin provides useful conversions, such as allowing access to regions in the atlas as ninepatches, sprites, drawables, etc. The get* methods return an instance of the object in the skin. The new*
  * methods return a copy of an instance in the skin. <p> See the <a href="https://libgdx.com/wiki/graphics/2d/scene2d/skin">documentation</a> for more.
  * @author
  *   Nathan Sweet
  */
class Skin()(using Sge) extends AutoCloseable {

  val resources:      mutable.Map[Class[?], mutable.Map[String, Any]] = mutable.Map.empty
  private var _atlas: Nullable[TextureAtlas]                          = Nullable.empty
  private var _scale: Float                                           = 1f

  /** Creates a skin containing the resources in the specified skin JSON file. If a file in the same directory with a ".atlas" extension exists, it is loaded as a {@link TextureAtlas} and the texture
    * regions added to the skin. The atlas is automatically disposed when the skin is disposed.
    */
  def this(skinFile: FileHandle)(using Sge) = {
    this()
    val atlasFile = skinFile.sibling(skinFile.nameWithoutExtension() + ".atlas")
    if (atlasFile.exists()) {
      _atlas = Nullable(TextureAtlas(atlasFile))
      _atlas.foreach(addRegions)
    }
    load(skinFile)
  }

  /** Creates a skin containing the resources in the specified skin JSON file and the texture regions from the specified atlas. The atlas is automatically disposed when the skin is disposed.
    */
  def this(skinFile: FileHandle, atlas: TextureAtlas)(using Sge) = {
    this()
    _atlas = Nullable(atlas)
    addRegions(atlas)
    load(skinFile)
  }

  /** Creates a skin containing the texture regions from the specified atlas. The atlas is automatically disposed when the skin is disposed.
    */
  def this(atlas: TextureAtlas)(using Sge) = {
    this()
    _atlas = Nullable(atlas)
    addRegions(atlas)
  }

  /** Adds all resources in the specified skin JSON file. */
  def load(skinFile: FileHandle): Unit =
    try {
      val root = skinFile.readJson[Json]
      root match {
        case Json.Obj(rootObj) =>
          rootObj.fields.foreach { case (typeName, typeValue) =>
            Skin.resolveClass(typeName) match {
              case Some(tpe) => readNamedObjects(tpe, typeValue, skinFile)
              case None      => () // Unknown type, skip
            }
          }
        case _ => ()
      }
    } catch {
      case ex: SgeError  => throw ex
      case ex: Exception =>
        throw SgeError.InvalidInput("Error reading skin file: " + skinFile + ": " + ex.getMessage)
    }

  private def readNamedObjects(tpe: Class[?], valueMap: Json, skinFile: FileHandle): Unit = {
    val addType = if (tpe == classOf[Skin.TintedDrawable]) classOf[Drawable] else tpe
    valueMap match {
      case Json.Obj(obj) =>
        obj.fields.foreach { case (entryName, entryValue) =>
          try {
            val resource = readValue(tpe, entryName, entryValue, skinFile)
            if (Nullable(resource).isDefined) {
              add(entryName, resource, addType)
              if (addType != classOf[Drawable] && classOf[Drawable].isAssignableFrom(addType))
                add(entryName, resource, classOf[Drawable])
            }
          } catch {
            case ex: Exception =>
              throw SgeError.InvalidInput("Error reading " + tpe.getSimpleName + ": " + entryName + ": " + ex.getMessage)
          }
        }
      case _ => ()
    }
  }

  /** Reads a single value of the specified type from JSON. For string JSON values referencing named resources, looks them up in this skin. For Color, BitmapFont, and TintedDrawable, uses explicit
    * readers. For all other types (typically widget style classes), dispatches to SkinStyleReader.
    */
  private def readValue(tpe: Class[?], entryName: String, json: Json, skinFile: FileHandle): Any =
    json match {
      case Json.Str(s) if !classOf[CharSequence].isAssignableFrom(tpe) =>
        get(s, tpe.asInstanceOf[Class[Any]])
      case _ if tpe == classOf[Color]               => readColor(json)
      case _ if tpe == classOf[BitmapFont]          => readBitmapFont(json, skinFile)
      case _ if tpe == classOf[Skin.TintedDrawable] => readTintedDrawable(entryName, json)
      case _                                        => readStyleObject(tpe, json)
    }

  /** Reads a Color from JSON. Supports string references, hex notation, and r/g/b/a components. */
  private def readColor(json: Json): Color = json match {
    case Json.Str(name) => get(name, classOf[Color])
    case _: Json.Obj =>
      Skin.getField(json, "hex") match {
        case Some(Json.Str(hex)) => Color.valueOf(hex)
        case _                   =>
          val r = Skin.getFloatField(json, "r", 0f)
          val g = Skin.getFloatField(json, "g", 0f)
          val b = Skin.getFloatField(json, "b", 0f)
          val a = Skin.getFloatField(json, "a", 1f)
          Color(r, g, b, a)
      }
    case _ => throw SgeError.InvalidInput("Invalid color JSON")
  }

  /** Reads a BitmapFont from JSON. */
  private def readBitmapFont(json: Json, skinFile: FileHandle): BitmapFont = {
    val path            = Skin.getStringField(json, "file", "")
    val scaledSize      = Skin.getFloatField(json, "scaledSize", -1f)
    val flip            = Skin.getBoolField(json, "flip", false)
    val markupEnabled   = Skin.getBoolField(json, "markupEnabled", false)
    val useIntPositions = Skin.getBoolField(json, "useIntegerPositions", true)

    var fontFile = skinFile.parent().child(path)
    if (!fontFile.exists()) fontFile = Sge().files.internal(path)
    if (!fontFile.exists()) throw SgeError.InvalidInput("Font file not found: " + fontFile)

    // Use a region with the same name as the font, else use a PNG file in the same directory as the FNT file.
    val regionName = fontFile.nameWithoutExtension()
    try {
      val font: BitmapFont = {
        val regions = getRegions(regionName)
        if (regions.isDefined)
          BitmapFont(BitmapFontData(Nullable(fontFile), flip), regions, true)
        else {
          val region = optional(regionName, classOf[TextureRegion])
          if (region.isDefined)
            BitmapFont(fontFile, region, flip)
          else {
            val imageFile = fontFile.parent().child(regionName + ".png")
            if (imageFile.exists())
              BitmapFont(fontFile, imageFile, flip)
            else
              BitmapFont(fontFile, flip)
          }
        }
      }
      font.data.markupEnabled = markupEnabled
      font.integerPositions = useIntPositions
      // Scaled size is the desired cap height to scale the font to.
      if (scaledSize != -1) {
        val s = scaledSize / font.capHeight
        font.data.scaleX = s
        font.data.scaleY = s
      }
      font
    } catch {
      case ex: RuntimeException =>
        throw SgeError.InvalidInput("Error loading bitmap font: " + fontFile + ": " + ex.getMessage)
    }
  }

  /** Reads a TintedDrawable from JSON. Returns the tinted Drawable. */
  private def readTintedDrawable(entryName: String, json: Json): Drawable = {
    val drawableName = Skin.getStringField(json, "name", "")
    val colorJson    = Skin.getField(json, "color").getOrElse(throw SgeError.InvalidInput("TintedDrawable requires 'color'"))
    val color        = readColor(colorJson)
    val drawable     = newDrawable(drawableName, color)
    drawable match {
      case named: BaseDrawable =>
        named.name = Nullable(entryName + " (" + drawableName + ", " + color + ")")
      case _ =>
    }
    drawable
  }

  /** Reads a style object from JSON using SkinStyleReader type class dispatch. Supports "parent" field for style inheritance. */
  private[ui] def readStyleObject(tpe: Class[?], json: Json): Any = {
    val reader = SkinStyleReader.registry.getOrElse(tpe, throw SgeError.InvalidInput("No style reader registered for: " + tpe.getName)).asInstanceOf[SkinStyleReader[Any]]
    val obj    = reader.create()

    // Handle parent field: copy all fields from the named parent resource.
    // Uses a compile-time type hierarchy instead of getSuperclass() for Scala.js compatibility.
    Skin.getField(json, "parent").foreach {
      case Json.Str(parentName) =>
        val typesToTry = tpe :: Skin.styleParentTypes.getOrElse(tpe, Nil)
        var found      = false
        for (parentType <- typesToTry if !found)
          try {
            val parentObj = get(parentName, parentType.asInstanceOf[Class[Any]])
            reader.copyFrom(parentObj, obj)
            found = true
          } catch {
            case _: SgeError => ()
          }
        if (!found)
          throw SgeError.InvalidInput("Unable to find parent resource with name: " + parentName)
      case _ => ()
    }

    // Set fields from JSON.
    json match {
      case Json.Obj(fields) =>
        fields.fields.foreach { case (fieldName, fieldValue) =>
          if (fieldName != "parent")
            reader.setField(obj, fieldName, fieldValue, this, readColor, readStyleObject)
        }
      case _ => ()
    }
    obj
  }

  /** Adds all named texture regions from the atlas. The atlas will not be automatically disposed when the skin is disposed. */
  def addRegions(atlas: TextureAtlas): Unit = {
    val regions = atlas.regions
    var i       = 0
    val n       = regions.length
    while (i < n) {
      val region = regions(i)
      var name   = region.name
      if (region.index != -1) {
        name = name + "_" + region.index
      }
      add(name, region, classOf[TextureRegion])
      i += 1
    }
  }

  def add(name: String, resource: Any): Unit =
    add(name, resource, resource.getClass)

  def add(name: String, resource: Any, tpe: Class[?]): Unit = {
    val typeResources = resources.getOrElseUpdate(tpe, mutable.Map.empty)
    typeResources.put(name, resource)
  }

  def remove(name: String, tpe: Class[?]): Unit =
    resources.get(tpe).foreach(_.remove(name))

  /** Returns a resource named "default" for the specified type.
    * @throws SgeError
    *   if the resource was not found.
    */
  def get[T](tpe: Class[T]): T =
    get("default", tpe)

  /** Returns a named resource of the specified type.
    * @throws SgeError
    *   if the resource was not found.
    */
  def get[T](name: String, tpe: Class[T]): T = boundary {
    if (tpe == classOf[Drawable]) break(getDrawable(name).asInstanceOf[T])
    if (tpe == classOf[TextureRegion]) break(getRegion(name).asInstanceOf[T])
    if (tpe == classOf[NinePatch]) break(getPatch(name).asInstanceOf[T])
    if (tpe == classOf[Sprite]) break(getSprite(name).asInstanceOf[T])

    val typeResources = resources.get(tpe)
    if (typeResources.isEmpty)
      throw SgeError.InvalidInput("No " + tpe.getName + " registered with name: " + name)
    val resource = typeResources.get.get(name)
    if (resource.isEmpty)
      throw SgeError.InvalidInput("No " + tpe.getName + " registered with name: " + name)
    resource.get.asInstanceOf[T]
  }

  /** Returns a named resource of the specified type.
    * @return
    *   Nullable.empty if not found.
    */
  def optional[T](name: String, tpe: Class[T]): Nullable[T] = {
    val typeResources = resources.get(tpe)
    if (typeResources.isEmpty) Nullable.empty
    else {
      typeResources.get.get(name) match {
        case Some(v) => Nullable(v.asInstanceOf[T])
        case None    => Nullable.empty
      }
    }
  }

  def has(name: String, tpe: Class[?]): Boolean =
    resources.get(tpe) match {
      case Some(typeResources) => typeResources.contains(name)
      case None                => false
    }

  /** Returns the name to resource mapping for the specified type, or Nullable.empty if no resources of that type exist. */
  def getAll[T](tpe: Class[T]): Nullable[mutable.Map[String, T]] =
    resources.get(tpe) match {
      case Some(m) => Nullable(m.asInstanceOf[mutable.Map[String, T]])
      case None    => Nullable.empty
    }

  /** Returns a resource named "default" for the specified type. */
  def get[T: ClassTag]: T =
    get("default", summon[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]])

  /** Returns a named resource of the specified type. */
  def get[T: ClassTag](name: String): T =
    get(name, summon[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]])

  /** Returns a named resource of the specified type, or Nullable.empty if not found. */
  def optional[T: ClassTag](name: String): Nullable[T] =
    optional(name, summon[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]])

  /** Whether a resource with the specified name and type exists. */
  def has[T: ClassTag](name: String): Boolean =
    has(name, summon[ClassTag[T]].runtimeClass)

  /** Returns the name to resource mapping for the specified type, or Nullable.empty if no resources of that type exist. */
  def getAll[T: ClassTag]: Nullable[mutable.Map[String, T]] =
    getAll(summon[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]])

  def getColor(name: String): Color =
    get(name, classOf[Color])

  def getFont(name: String): BitmapFont =
    get(name, classOf[BitmapFont])

  /** Returns a registered texture region. If no region is found but a texture exists with the name, a region is created from the texture and stored in the skin.
    */
  def getRegion(name: String): TextureRegion = boundary {
    val region = optional(name, classOf[TextureRegion])
    region.foreach(r => break(r))

    val texture   = optional(name, classOf[Texture])
    val tex       = texture.getOrElse(throw SgeError.InvalidInput("No TextureRegion or Texture registered with name: " + name))
    val newRegion = TextureRegion(tex)
    add(name, newRegion, classOf[TextureRegion])
    newRegion
  }

  /** @return an array with the {@link TextureRegion} that have an index != -1, or Nullable.empty if none are found. */
  def getRegions(regionName: String): Nullable[DynamicArray[TextureRegion]] = {
    var regions: Nullable[DynamicArray[TextureRegion]] = Nullable.empty
    var i      = 0
    var region = optional(regionName + "_" + i, classOf[TextureRegion])
    i += 1
    if (region.isDefined) {
      val buf = DynamicArray[TextureRegion]()
      while (region.isDefined) {
        region.foreach(buf.add(_))
        region = optional(regionName + "_" + i, classOf[TextureRegion])
        i += 1
      }
      regions = Nullable(buf)
    }
    regions
  }

  /** Returns a registered tiled drawable. If no tiled drawable is found but a region exists with the name, a tiled drawable is created from the region and stored in the skin.
    */
  def getTiledDrawable(name: String): TiledDrawable = boundary {
    val existing = optional(name, classOf[TiledDrawable])
    existing.foreach(e => break(e))

    val tiled = TiledDrawable(getRegion(name))
    tiled.name = Nullable(name)
    if (_scale != 1) {
      scale(tiled)
      tiled.scale = _scale
    }
    add(name, tiled, classOf[TiledDrawable])
    tiled
  }

  /** Returns a registered ninepatch. If no ninepatch is found but a region exists with the name, a ninepatch is created from the region and stored in the skin. If the region is an {@link AtlasRegion}
    * then its split {@link AtlasRegion#values} are used, otherwise the ninepatch will have the region as the center patch.
    */
  def getPatch(name: String): NinePatch = boundary {
    val existing = optional(name, classOf[NinePatch])
    existing.foreach(e => break(e))

    try {
      val region = getRegion(name)
      var patch: Nullable[NinePatch] = Nullable.empty
      region match {
        case atlasRegion: AtlasRegion =>
          val splits = atlasRegion.findValue("split")
          splits.foreach { s =>
            val p    = NinePatch(region, s(0), s(1), s(2), s(3))
            val pads = atlasRegion.findValue("pad")
            pads.foreach(pd => p.setPadding(pd(0).toFloat, pd(1).toFloat, pd(2).toFloat, pd(3).toFloat))
            patch = Nullable(p)
          }
        case _ =>
      }
      val result = patch.getOrElse(NinePatch(region))
      if (_scale != 1) result.scale(_scale, _scale)
      add(name, result, classOf[NinePatch])
      result
    } catch {
      case _: SgeError =>
        throw SgeError.InvalidInput("No NinePatch, TextureRegion, or Texture registered with name: " + name)
    }
  }

  /** Returns a registered sprite. If no sprite is found but a region exists with the name, a sprite is created from the region and stored in the skin. If the region is an {@link AtlasRegion} then an
    * {@link AtlasSprite} is used if the region has been whitespace stripped or packed rotated 90 degrees.
    */
  def getSprite(name: String): Sprite = boundary {
    val existing = optional(name, classOf[Sprite])
    existing.foreach(e => break(e))

    try {
      val textureRegion = getRegion(name)
      var sprite: Nullable[Sprite] = Nullable.empty
      textureRegion match {
        case region: AtlasRegion =>
          if (region.rotate || region.packedWidth != region.originalWidth || region.packedHeight != region.originalHeight)
            sprite = Nullable(AtlasSprite(region))
        case _ =>
      }
      val result = sprite.getOrElse(Sprite(textureRegion))
      if (_scale != 1) result.setSize(result.width * _scale, result.height * _scale)
      add(name, result, classOf[Sprite])
      result
    } catch {
      case _: SgeError =>
        throw SgeError.InvalidInput("No NinePatch, TextureRegion, or Texture registered with name: " + name)
    }
  }

  /** Returns a registered drawable. If no drawable is found but a region, ninepatch, or sprite exists with the name, then the appropriate drawable is created and stored in the skin.
    */
  def getDrawable(name: String): Drawable = boundary {
    var drawable: Nullable[Drawable] = optional(name, classOf[Drawable])
    drawable.foreach(d => break(d))

    // Use texture or texture region. If it has splits, use ninepatch. If it has rotation or whitespace stripping, use sprite.
    try {
      val textureRegion = getRegion(name)
      textureRegion match {
        case region: AtlasRegion =>
          if (region.findValue("split").isDefined)
            drawable = Nullable(NinePatchDrawable(getPatch(name)))
          else if (region.rotate || region.packedWidth != region.originalWidth || region.packedHeight != region.originalHeight)
            drawable = Nullable(SpriteDrawable(getSprite(name)))
        case _ =>
      }
      if (drawable.isEmpty) {
        val d = TextureRegionDrawable(textureRegion)
        if (_scale != 1) scale(d)
        drawable = Nullable(d)
      }
    } catch {
      case _: SgeError => // ignored
    }

    // Check for explicit registration of ninepatch, sprite, or tiled drawable.
    if (drawable.isEmpty) {
      val patch = optional(name, classOf[NinePatch])
      patch.foreach { p =>
        drawable = Nullable(NinePatchDrawable(p))
      }
      if (drawable.isEmpty) {
        val sprite = optional(name, classOf[Sprite])
        sprite.foreach { s =>
          drawable = Nullable(SpriteDrawable(s))
        }
        if (drawable.isEmpty)
          throw SgeError.InvalidInput("No Drawable, NinePatch, TextureRegion, Texture, or Sprite registered with name: " + name)
      }
    }

    val result = drawable.getOrElse(throw SgeError.InvalidInput("No Drawable registered with name: " + name))
    result match {
      case bd: BaseDrawable => bd.name = Nullable(name)
      case _ =>
    }

    add(name, result, classOf[Drawable])
    result
  }

  /** Returns the name of the specified style object, or Nullable.empty if it is not in the skin. This compares potentially every style object in the skin of the same type as the specified style,
    * which may be a somewhat expensive operation.
    */
  def find(resource: Any): Nullable[String] =
    resources.get(resource.getClass) match {
      case Some(typeResources) =>
        typeResources.collectFirst { case (k, v) if v == resource => k } match {
          case Some(name) => Nullable(name)
          case None       => Nullable.empty
        }
      case None => Nullable.empty
    }

  /** Returns a copy of a drawable found in the skin via {@link #getDrawable(String)}. */
  def newDrawable(name: String): Drawable =
    newDrawable(getDrawable(name))

  /** Returns a tinted copy of a drawable found in the skin via {@link #getDrawable(String)}. */
  def newDrawable(name: String, r: Float, g: Float, b: Float, a: Float): Drawable =
    newDrawable(getDrawable(name), Color(r, g, b, a))

  /** Returns a tinted copy of a drawable found in the skin via {@link #getDrawable(String)}. */
  def newDrawable(name: String, tint: Color): Drawable =
    newDrawable(getDrawable(name), tint)

  /** Returns a copy of the specified drawable. */
  def newDrawable(drawable: Drawable): Drawable =
    drawable match {
      case d: TiledDrawable         => TiledDrawable(d)
      case d: TextureRegionDrawable => TextureRegionDrawable(d)
      case d: NinePatchDrawable     => NinePatchDrawable(d)
      case d: SpriteDrawable        => SpriteDrawable(d)
      case _ => throw SgeError.InvalidInput("Unable to copy, unknown drawable type: " + drawable.getClass)
    }

  /** Returns a tinted copy of a drawable found in the skin via {@link #getDrawable(String)}. */
  def newDrawable(drawable: Drawable, r: Float, g: Float, b: Float, a: Float): Drawable =
    newDrawable(drawable, Color(r, g, b, a))

  /** Returns a tinted copy of a drawable found in the skin via {@link #getDrawable(String)}. */
  def newDrawable(drawable: Drawable, tint: Color): Drawable = {
    val result: Drawable = drawable match {
      case d: TextureRegionDrawable => d.tint(tint)
      case d: NinePatchDrawable     => d.tint(tint)
      case d: SpriteDrawable        => d.tint(tint)
      case _ => throw SgeError.InvalidInput("Unable to copy, unknown drawable type: " + drawable.getClass)
    }

    result match {
      case named: BaseDrawable =>
        drawable match {
          case bd: BaseDrawable => named.name = Nullable(bd.name.getOrElse("") + " (" + tint + ")")
          case _ => named.name = Nullable(" (" + tint + ")")
        }
      case _ =>
    }

    result
  }

  /** Scales the drawable's {@link Drawable#getLeftWidth()}, {@link Drawable#getRightWidth()}, {@link Drawable#getBottomHeight()}, {@link Drawable#getTopHeight()}, {@link Drawable#getMinWidth()}, and
    * {@link Drawable#getMinHeight()}.
    */
  def scale(drawable: Drawable): Unit = {
    drawable.leftWidth = drawable.leftWidth * _scale
    drawable.rightWidth = drawable.rightWidth * _scale
    drawable.bottomHeight = drawable.bottomHeight * _scale
    drawable.topHeight = drawable.topHeight * _scale
    drawable.minWidth = drawable.minWidth * _scale
    drawable.minHeight = drawable.minHeight * _scale
  }

  /** The scale used to size drawables created by this skin. <p> This can be useful when scaling an entire UI (eg with a stage's viewport) then using an atlas with images whose resolution matches the
    * UI scale. The skin can then be scaled the opposite amount so that the larger or smaller images are drawn at the original size. For example, if the UI is scaled 2x, the atlas would have images
    * that are twice the size, then the skin's scale would be set to 0.5.
    */
  def setScale(scale: Float): Unit =
    _scale = scale

  /** Sets the style on the actor to disabled or enabled. This is done by appending "-disabled" to the style name when enabled is false, and removing "-disabled" from the style name when enabled is
    * true. If the style was not found in the skin, an exception is thrown.
    */
  def setEnabled[V](styleable: Styleable[V], enabled: Boolean): Unit = {
    val style = styleable.getStyle
    val name  = find(style)
    name.foreach { n =>
      val newName  = n.replace("-disabled", "") + (if (enabled) "" else "-disabled")
      val newStyle = get(newName, style.asInstanceOf[AnyRef].getClass.asInstanceOf[Class[V]])
      styleable.setStyle(newStyle)
    }
  }

  /** Returns the {@link TextureAtlas} passed to this skin constructor, or Nullable.empty. */
  def getAtlas: Nullable[TextureAtlas] = _atlas

  /** Disposes the {@link TextureAtlas} and all {@link AutoCloseable} resources in the skin. */
  override def close(): Unit = {
    _atlas.foreach(_.close())
    for (entry <- resources.values)
      for (resource <- entry.values)
        resource match {
          case c: AutoCloseable => c.close()
          case _ =>
        }
  }
}

object Skin {

  /** @author Nathan Sweet */
  class TintedDrawable {
    var name:  String = scala.compiletime.uninitialized
    var color: Color  = scala.compiletime.uninitialized
  }

  /** Maps type name strings to Class objects for JSON type resolution. Hardcoded instead of using getSimpleName/Class.forName for Scala.js/Native compatibility. Includes short names, LibGDX
    * fully-qualified names, and SGE fully-qualified names.
    */
  private lazy val classTagMap: Map[String, Class[?]] = Map(
    // Short names (used in modern skin JSON files)
    "BitmapFont" -> classOf[BitmapFont],
    "Color" -> classOf[Color],
    "TintedDrawable" -> classOf[TintedDrawable],
    "NinePatchDrawable" -> classOf[NinePatchDrawable],
    "SpriteDrawable" -> classOf[SpriteDrawable],
    "TextureRegionDrawable" -> classOf[TextureRegionDrawable],
    "TiledDrawable" -> classOf[TiledDrawable],
    "ButtonStyle" -> classOf[Button.ButtonStyle],
    "CheckBoxStyle" -> classOf[CheckBox.CheckBoxStyle],
    "ImageButtonStyle" -> classOf[ImageButton.ImageButtonStyle],
    "ImageTextButtonStyle" -> classOf[ImageTextButton.ImageTextButtonStyle],
    "LabelStyle" -> classOf[Label.LabelStyle],
    "ListStyle" -> classOf[SgeList.ListStyle],
    "ProgressBarStyle" -> classOf[ProgressBar.ProgressBarStyle],
    "ScrollPaneStyle" -> classOf[ScrollPane.ScrollPaneStyle],
    "SelectBoxStyle" -> classOf[SelectBox.SelectBoxStyle],
    "SliderStyle" -> classOf[Slider.SliderStyle],
    "SplitPaneStyle" -> classOf[SplitPane.SplitPaneStyle],
    "TextButtonStyle" -> classOf[TextButton.TextButtonStyle],
    "TextFieldStyle" -> classOf[TextField.TextFieldStyle],
    "TextTooltipStyle" -> classOf[TextTooltip.TextTooltipStyle],
    "TouchpadStyle" -> classOf[Touchpad.TouchpadStyle],
    "TreeStyle" -> classOf[Tree.TreeStyle],
    "WindowStyle" -> classOf[Window.WindowStyle],
    // LibGDX fully-qualified names (for compatibility with existing skin JSON files)
    "com.badlogic.gdx.graphics.g2d.BitmapFont" -> classOf[BitmapFont],
    "com.badlogic.gdx.graphics.Color" -> classOf[Color],
    "com.badlogic.gdx.scenes.scene2d.ui.Skin$TintedDrawable" -> classOf[TintedDrawable],
    "com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable" -> classOf[NinePatchDrawable],
    "com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable" -> classOf[SpriteDrawable],
    "com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable" -> classOf[TextureRegionDrawable],
    "com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable" -> classOf[TiledDrawable],
    "com.badlogic.gdx.scenes.scene2d.ui.Button$ButtonStyle" -> classOf[Button.ButtonStyle],
    "com.badlogic.gdx.scenes.scene2d.ui.CheckBox$CheckBoxStyle" -> classOf[CheckBox.CheckBoxStyle],
    "com.badlogic.gdx.scenes.scene2d.ui.ImageButton$ImageButtonStyle" -> classOf[ImageButton.ImageButtonStyle],
    "com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton$ImageTextButtonStyle" -> classOf[ImageTextButton.ImageTextButtonStyle],
    "com.badlogic.gdx.scenes.scene2d.ui.Label$LabelStyle" -> classOf[Label.LabelStyle],
    "com.badlogic.gdx.scenes.scene2d.ui.List$ListStyle" -> classOf[SgeList.ListStyle],
    "com.badlogic.gdx.scenes.scene2d.ui.ProgressBar$ProgressBarStyle" -> classOf[ProgressBar.ProgressBarStyle],
    "com.badlogic.gdx.scenes.scene2d.ui.ScrollPane$ScrollPaneStyle" -> classOf[ScrollPane.ScrollPaneStyle],
    "com.badlogic.gdx.scenes.scene2d.ui.SelectBox$SelectBoxStyle" -> classOf[SelectBox.SelectBoxStyle],
    "com.badlogic.gdx.scenes.scene2d.ui.Slider$SliderStyle" -> classOf[Slider.SliderStyle],
    "com.badlogic.gdx.scenes.scene2d.ui.SplitPane$SplitPaneStyle" -> classOf[SplitPane.SplitPaneStyle],
    "com.badlogic.gdx.scenes.scene2d.ui.TextButton$TextButtonStyle" -> classOf[TextButton.TextButtonStyle],
    "com.badlogic.gdx.scenes.scene2d.ui.TextField$TextFieldStyle" -> classOf[TextField.TextFieldStyle],
    "com.badlogic.gdx.scenes.scene2d.ui.TextTooltip$TextTooltipStyle" -> classOf[TextTooltip.TextTooltipStyle],
    "com.badlogic.gdx.scenes.scene2d.ui.Touchpad$TouchpadStyle" -> classOf[Touchpad.TouchpadStyle],
    "com.badlogic.gdx.scenes.scene2d.ui.Tree$TreeStyle" -> classOf[Tree.TreeStyle],
    "com.badlogic.gdx.scenes.scene2d.ui.Window$WindowStyle" -> classOf[Window.WindowStyle]
  )

  /** Style class inheritance hierarchy for parent style lookup. Maps each subclass to its parent types (in order). Replaces getSuperclass() for Scala.js compatibility.
    */
  private val styleParentTypes: Map[Class[?], List[Class[?]]] = Map(
    classOf[TextButton.TextButtonStyle] -> List(classOf[Button.ButtonStyle]),
    classOf[CheckBox.CheckBoxStyle] -> List(classOf[TextButton.TextButtonStyle], classOf[Button.ButtonStyle]),
    classOf[ImageButton.ImageButtonStyle] -> List(classOf[Button.ButtonStyle]),
    classOf[ImageTextButton.ImageTextButtonStyle] -> List(classOf[TextButton.TextButtonStyle], classOf[Button.ButtonStyle]),
    classOf[Slider.SliderStyle] -> List(classOf[ProgressBar.ProgressBarStyle])
  )

  /** Resolves a type name from skin JSON to a Class. Uses a hardcoded map instead of Class.forName for Scala.js/Native compatibility.
    */
  private def resolveClass(name: String): Option[Class[?]] =
    classTagMap.get(name)

  // ---------------------------------------------------------------------------
  // JSON field access helpers
  // ---------------------------------------------------------------------------

  /** Extracts a named field from a Json.Obj. */
  private[ui] def getField(json: Json, name: String): Option[Json] = json match {
    case Json.Obj(obj) =>
      var result: Option[Json] = None
      obj.fields.foreach { case (k, v) => if (k == name) result = Some(v) }
      result
    case _ => None
  }

  /** Extracts a String field with a default. */
  private def getStringField(json: Json, name: String, default: String): String =
    getField(json, name) match {
      case Some(Json.Str(s)) => s
      case _                 => default
    }

  /** Extracts a Float field with a default. */
  private def getFloatField(json: Json, name: String, default: Float): Float =
    getField(json, name) match {
      case Some(Json.Num(n)) => n.toDouble.map(_.toFloat).getOrElse(default)
      case _                 => default
    }

  /** Extracts a Boolean field with a default. */
  private def getBoolField(json: Json, name: String, default: Boolean): Boolean =
    getField(json, name) match {
      case Some(Json.Bool(b)) => b
      case _                  => default
    }
}
