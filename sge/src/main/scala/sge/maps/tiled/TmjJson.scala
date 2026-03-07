/*
 * SGE - Scala Game Engine
 * copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (JSON DTO case classes for .tmj / .tsj / .tj formats)
 *   Convention: mirrors TMJ JSON structure for jsoniter-scala codec derivation
 *   Idiom: split packages, final case class, Option for optional fields
 */
package sge
package maps
package tiled

import com.github.plokhotnyuk.jsoniter_scala.macros.{ CodecMakerConfig, named }
import hearth.kindlings.jsoniterjson.codec.JsonCodec.given
import sge.utils.Json

/** JSON DTO case classes for the TMJ tiled map format (.tmj / .tsj / .tj files).
  *
  * These match the raw JSON structure exactly so jsoniter-scala can derive codecs. The loader transforms these DTOs into the mutable domain objects.
  */

/** Top-level .tmj map structure. */
final case class TmjMapJson(
  orientation:     Option[String] = None,
  width:           Int = 0,
  height:          Int = 0,
  tilewidth:       Int = 0,
  tileheight:      Int = 0,
  hexsidelength:   Int = 0,
  staggeraxis:     Option[String] = None,
  staggerindex:    Option[String] = None,
  backgroundcolor: Option[String] = None,
  properties:      List[TmjPropertyJson] = Nil,
  tilesets:        List[TmjTilesetRefJson] = Nil,
  layers:          List[TmjLayerJson] = Nil
)

/** A property entry (name/type/value). The `value` is polymorphic (string, number, boolean, or object). */
final case class TmjPropertyJson(
  name:               String = "",
  @named("type") tpe: String = "",
  value:              Json = Json.Null,
  propertytype:       Option[String] = None
)

/** Tileset reference — can be inline (all fields present) or external (only firstgid + source). Also used to parse standalone .tsj tileset files. */
final case class TmjTilesetRefJson(
  firstgid:    Int = 0,
  source:      Option[String] = None,
  name:        Option[String] = None,
  image:       Option[String] = None,
  imagewidth:  Int = 0,
  imageheight: Int = 0,
  tilewidth:   Int = 0,
  tileheight:  Int = 0,
  spacing:     Int = 0,
  margin:      Int = 0,
  tileoffset:  Option[TmjTileOffsetJson] = None,
  properties:  List[TmjPropertyJson] = Nil,
  tiles:       List[TmjTileJson] = Nil
)

final case class TmjTileOffsetJson(
  x: Int = 0,
  y: Int = 0
)

final case class TmjTileJson(
  id:                 Int = 0,
  @named("type") tpe: Option[String] = None,
  terrain:            Option[String] = None,
  probability:        Option[String] = None,
  properties:         List[TmjPropertyJson] = Nil,
  animation:          List[TmjAnimFrameJson] = Nil,
  objectgroup:        Option[TmjObjectGroupJson] = None,
  image:              Option[String] = None
)

final case class TmjAnimFrameJson(
  tileid:   Int = 0,
  duration: Int = 0
)

/** Inline object group (found within a tile definition). */
final case class TmjObjectGroupJson(
  objects: List[TmjObjectJson] = Nil
)

/** A layer in the map. All layer types share one flat case class. The `tpe` field discriminates: "tilelayer", "objectgroup", "imagelayer", "group". */
final case class TmjLayerJson(
  // Common fields
  name:               String = "",
  @named("type") tpe: String = "",
  opacity:            Float = 1.0f,
  tintcolor:          Option[String] = None,
  visible:            Boolean = true,
  offsetx:            Float = 0f,
  offsety:            Float = 0f,
  parallaxx:          Float = 1f,
  parallaxy:          Float = 1f,
  properties:         List[TmjPropertyJson] = Nil,
  // Tile layer
  width:       Int = 0,
  height:      Int = 0,
  data:        Option[Json] = None, // int array (CSV) or base64 string
  encoding:    Option[String] = None,
  compression: Option[String] = None,
  // Object group
  objects: List[TmjObjectJson] = Nil,
  // Image layer
  image:   Option[String] = None,
  repeatx: Int = 0,
  repeaty: Int = 0,
  // Group layer (recursive)
  layers: List[TmjLayerJson] = Nil
)

/** A map object. Fields use `Option` to support template merging where absent fields should fall back to the template's value. */
final case class TmjObjectJson(
  id:                 Int = 0,
  name:               Option[String] = None,
  @named("type") tpe: Option[String] = None,
  x:                  Option[Float] = None,
  y:                  Option[Float] = None,
  width:              Option[Float] = None,
  height:             Option[Float] = None,
  rotation:           Option[Float] = None,
  visible:            Option[Boolean] = None,
  gid:                Option[Long] = None,
  template:           Option[String] = None,
  properties:         List[TmjPropertyJson] = Nil,
  polygon:            List[TmjPointJson] = Nil,
  polyline:           List[TmjPointJson] = Nil,
  ellipse:            Option[Boolean] = None,
  point:              Option[Boolean] = None,
  text:               Option[TmjTextJson] = None
)

final case class TmjPointJson(
  x: Float = 0f,
  y: Float = 0f
)

/** Text properties for a TextMapObject. Fields are `Option` to support template merging. */
final case class TmjTextJson(
  text:       Option[String] = None,
  fontfamily: Option[String] = None,
  pixelSize:  Option[Int] = None,
  halign:     Option[String] = None,
  valign:     Option[String] = None,
  bold:       Option[Boolean] = None,
  italic:     Option[Boolean] = None,
  underline:  Option[Boolean] = None,
  strikeout:  Option[Boolean] = None,
  wrap:       Option[Boolean] = None,
  kerning:    Option[Boolean] = None,
  color:      Option[String] = None
)

/** Template file (.tj) structure. */
final case class TmjTemplateJson(
  @named("type") tpe: String = "",
  `object`:           TmjObjectJson = TmjObjectJson()
)

object TmjMapJson {
  given codec: sge.utils.JsonCodec[TmjMapJson] = sge.utils.JsonCodec.make(
    CodecMakerConfig.withAllowRecursiveTypes(true)
  )
}

object TmjTilesetRefJson {
  given codec: sge.utils.JsonCodec[TmjTilesetRefJson] = sge.utils.JsonCodec.make
}

object TmjTemplateJson {
  given codec: sge.utils.JsonCodec[TmjTemplateJson] = sge.utils.JsonCodec.make
}
