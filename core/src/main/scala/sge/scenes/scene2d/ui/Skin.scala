/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Skin.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.files.FileHandle
import sge.graphics.Color
import sge.graphics.Texture
import sge.graphics.g2d.BitmapFont
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
import sge.utils.Nullable
import sge.utils.SgeError

import scala.collection.mutable
import scala.util.boundary
import scala.util.boundary.break

/** A skin stores resources for UI widgets to use (texture regions, ninepatches, fonts, colors, etc). Resources are named and can be looked up by name and type. Resources can be described in JSON.
  * Skin provides useful conversions, such as allowing access to regions in the atlas as ninepatches, sprites, drawables, etc. The get* methods return an instance of the object in the skin. The new*
  * methods return a copy of an instance in the skin. <p> See the <a href="https://libgdx.com/wiki/graphics/2d/scene2d/skin">documentation</a> for more.
  * @author
  *   Nathan Sweet
  */
class Skin() extends AutoCloseable {

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
      _atlas = Nullable(new TextureAtlas(atlasFile))
      _atlas.foreach(addRegions)
    }
    // TODO: load(skinFile) — needs JSON library
  }

  /** Creates a skin containing the resources in the specified skin JSON file and the texture regions from the specified atlas. The atlas is automatically disposed when the skin is disposed.
    */
  def this(skinFile: FileHandle, atlas: TextureAtlas)(using Sge) = {
    this()
    _atlas = Nullable(atlas)
    addRegions(atlas)
    // TODO: load(skinFile) — needs JSON library
  }

  /** Creates a skin containing the texture regions from the specified atlas. The atlas is automatically disposed when the skin is disposed.
    */
  def this(atlas: TextureAtlas) = {
    this()
    _atlas = Nullable(atlas)
    addRegions(atlas)
  }

  /** Adds all resources in the specified skin JSON file. */
  def load(skinFile: FileHandle): Unit =
    // TODO: Implement when a Scala JSON library is chosen.
    // Original used libGDX's Json + ClassReflection, which are intentionally skipped in the migration.
    throw SgeError.InvalidInput("Skin.load() is not yet implemented — needs a Scala JSON library")

  /** Adds all named texture regions from the atlas. The atlas will not be automatically disposed when the skin is disposed. */
  def addRegions(atlas: TextureAtlas): Unit = {
    val regions = atlas.getRegions()
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
    require(name != null, "name cannot be null.")
    require(resource != null, "resource cannot be null.")
    val typeResources = resources.getOrElseUpdate(tpe, mutable.Map.empty)
    typeResources.put(name, resource)
  }

  def remove(name: String, tpe: Class[?]): Unit = {
    require(name != null, "name cannot be null.")
    resources.get(tpe).foreach(_.remove(name))
  }

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
    require(name != null, "name cannot be null.")
    require(tpe != null, "type cannot be null.")

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
    require(name != null, "name cannot be null.")
    require(tpe != null, "type cannot be null.")
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

  def getColor(name: String): Color =
    get(name, classOf[Color])

  def getFont(name: String): BitmapFont =
    get(name, classOf[BitmapFont])

  /** Returns a registered texture region. If no region is found but a texture exists with the name, a region is created from the texture and stored in the skin.
    */
  def getRegion(name: String): TextureRegion = boundary {
    val region = optional(name, classOf[TextureRegion])
    if (region.isDefined) break(region.orNull)

    val texture = optional(name, classOf[Texture])
    if (texture.isEmpty) throw SgeError.InvalidInput("No TextureRegion or Texture registered with name: " + name)
    val newRegion = new TextureRegion(texture.orNull)
    add(name, newRegion, classOf[TextureRegion])
    newRegion
  }

  /** @return an array with the {@link TextureRegion} that have an index != -1, or Nullable.empty if none are found. */
  def getRegions(regionName: String): Nullable[mutable.ArrayBuffer[TextureRegion]] = {
    var regions: Nullable[mutable.ArrayBuffer[TextureRegion]] = Nullable.empty
    var i      = 0
    var region = optional(regionName + "_" + i, classOf[TextureRegion])
    i += 1
    if (region.isDefined) {
      val buf = mutable.ArrayBuffer.empty[TextureRegion]
      while (region.isDefined) {
        buf += region.orNull
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
    if (existing.isDefined) break(existing.orNull)

    val tiled = new TiledDrawable(getRegion(name))
    tiled.setName(Nullable(name))
    if (_scale != 1) {
      scale(tiled)
      tiled.setScale(_scale)
    }
    add(name, tiled, classOf[TiledDrawable])
    tiled
  }

  /** Returns a registered ninepatch. If no ninepatch is found but a region exists with the name, a ninepatch is created from the region and stored in the skin. If the region is an {@link AtlasRegion}
    * then its split {@link AtlasRegion#values} are used, otherwise the ninepatch will have the region as the center patch.
    */
  def getPatch(name: String): NinePatch = boundary {
    val existing = optional(name, classOf[NinePatch])
    if (existing.isDefined) break(existing.orNull)

    try {
      val region = getRegion(name)
      var patch: Nullable[NinePatch] = Nullable.empty
      region match {
        case atlasRegion: AtlasRegion =>
          val splits = atlasRegion.findValue("split")
          splits.foreach { s =>
            val p    = new NinePatch(region, s(0), s(1), s(2), s(3))
            val pads = atlasRegion.findValue("pad")
            pads.foreach(pd => p.setPadding(pd(0).toFloat, pd(1).toFloat, pd(2).toFloat, pd(3).toFloat))
            patch = Nullable(p)
          }
        case _ =>
      }
      if (patch.isEmpty) patch = Nullable(new NinePatch(region))
      if (_scale != 1) patch.orNull.scale(_scale, _scale)
      add(name, patch.orNull, classOf[NinePatch])
      patch.orNull
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
    if (existing.isDefined) break(existing.orNull)

    try {
      val textureRegion = getRegion(name)
      var sprite: Nullable[Sprite] = Nullable.empty
      textureRegion match {
        case region: AtlasRegion =>
          if (region.rotate || region.packedWidth != region.originalWidth || region.packedHeight != region.originalHeight)
            sprite = Nullable(new AtlasSprite(region))
        case _ =>
      }
      if (sprite.isEmpty) sprite = Nullable(new Sprite(textureRegion))
      if (_scale != 1) sprite.orNull.setSize(sprite.orNull.getWidth() * _scale, sprite.orNull.getHeight() * _scale)
      add(name, sprite.orNull, classOf[Sprite])
      sprite.orNull
    } catch {
      case _: SgeError =>
        throw SgeError.InvalidInput("No NinePatch, TextureRegion, or Texture registered with name: " + name)
    }
  }

  /** Returns a registered drawable. If no drawable is found but a region, ninepatch, or sprite exists with the name, then the appropriate drawable is created and stored in the skin.
    */
  def getDrawable(name: String): Drawable = boundary {
    var drawable: Nullable[Drawable] = optional(name, classOf[Drawable])
    if (drawable.isDefined) break(drawable.orNull)

    // Use texture or texture region. If it has splits, use ninepatch. If it has rotation or whitespace stripping, use sprite.
    try {
      val textureRegion = getRegion(name)
      textureRegion match {
        case region: AtlasRegion =>
          if (region.findValue("split").isDefined)
            drawable = Nullable(new NinePatchDrawable(getPatch(name)))
          else if (region.rotate || region.packedWidth != region.originalWidth || region.packedHeight != region.originalHeight)
            drawable = Nullable(new SpriteDrawable(getSprite(name)))
        case _ =>
      }
      if (drawable.isEmpty) {
        val d = new TextureRegionDrawable(textureRegion)
        if (_scale != 1) scale(d)
        drawable = Nullable(d)
      }
    } catch {
      case _: SgeError => // ignored
    }

    // Check for explicit registration of ninepatch, sprite, or tiled drawable.
    if (drawable.isEmpty) {
      val patch = optional(name, classOf[NinePatch])
      if (patch.isDefined)
        drawable = Nullable(new NinePatchDrawable(patch.orNull))
      else {
        val sprite = optional(name, classOf[Sprite])
        if (sprite.isDefined)
          drawable = Nullable(new SpriteDrawable(sprite.orNull))
        else
          throw SgeError.InvalidInput("No Drawable, NinePatch, TextureRegion, Texture, or Sprite registered with name: " + name)
      }
    }

    drawable.orNull match {
      case bd: BaseDrawable => bd.setName(Nullable(name))
      case _ =>
    }

    add(name, drawable.orNull, classOf[Drawable])
    drawable.orNull
  }

  /** Returns the name of the specified style object, or Nullable.empty if it is not in the skin. This compares potentially every style object in the skin of the same type as the specified style,
    * which may be a somewhat expensive operation.
    */
  def find(resource: Any): Nullable[String] = {
    require(resource != null, "style cannot be null.")
    resources.get(resource.getClass) match {
      case Some(typeResources) =>
        typeResources.collectFirst { case (k, v) if v == resource => k } match {
          case Some(name) => Nullable(name)
          case None       => Nullable.empty
        }
      case None => Nullable.empty
    }
  }

  /** Returns a copy of a drawable found in the skin via {@link #getDrawable(String)}. */
  def newDrawable(name: String): Drawable =
    newDrawable(getDrawable(name))

  /** Returns a tinted copy of a drawable found in the skin via {@link #getDrawable(String)}. */
  def newDrawable(name: String, r: Float, g: Float, b: Float, a: Float): Drawable =
    newDrawable(getDrawable(name), new Color(r, g, b, a))

  /** Returns a tinted copy of a drawable found in the skin via {@link #getDrawable(String)}. */
  def newDrawable(name: String, tint: Color): Drawable =
    newDrawable(getDrawable(name), tint)

  /** Returns a copy of the specified drawable. */
  def newDrawable(drawable: Drawable): Drawable =
    drawable match {
      case d: TiledDrawable         => new TiledDrawable(d)
      case d: TextureRegionDrawable => new TextureRegionDrawable(d)
      case d: NinePatchDrawable     => new NinePatchDrawable(d)
      case d: SpriteDrawable        => new SpriteDrawable(d)
      case _ => throw SgeError.InvalidInput("Unable to copy, unknown drawable type: " + drawable.getClass)
    }

  /** Returns a tinted copy of a drawable found in the skin via {@link #getDrawable(String)}. */
  def newDrawable(drawable: Drawable, r: Float, g: Float, b: Float, a: Float): Drawable =
    newDrawable(drawable, new Color(r, g, b, a))

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
          case bd: BaseDrawable => named.setName(Nullable(bd.getName.getOrElse("") + " (" + tint + ")"))
          case _ => named.setName(Nullable(" (" + tint + ")"))
        }
      case _ =>
    }

    result
  }

  /** Scales the drawable's {@link Drawable#getLeftWidth()}, {@link Drawable#getRightWidth()}, {@link Drawable#getBottomHeight()}, {@link Drawable#getTopHeight()}, {@link Drawable#getMinWidth()}, and
    * {@link Drawable#getMinHeight()}.
    */
  def scale(drawable: Drawable): Unit = {
    drawable.setLeftWidth(drawable.getLeftWidth * _scale)
    drawable.setRightWidth(drawable.getRightWidth * _scale)
    drawable.setBottomHeight(drawable.getBottomHeight * _scale)
    drawable.setTopHeight(drawable.getTopHeight * _scale)
    drawable.setMinWidth(drawable.getMinWidth * _scale)
    drawable.setMinHeight(drawable.getMinHeight * _scale)
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
}
