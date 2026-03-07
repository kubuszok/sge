/*
 * SGE - Scala Game Engine
 * copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (JSON DTO case classes for .tiled-project format)
 *   Convention: mirrors .tiled-project JSON structure for jsoniter-scala codec derivation
 *   Idiom: split packages, final case class, Option for optional fields
 */
package sge
package maps
package tiled

import com.github.plokhotnyuk.jsoniter_scala.macros.named
import hearth.kindlings.jsoniterjson.codec.JsonCodec.given
import sge.utils.Json

/** JSON DTO case classes for Tiled project files (.tiled-project).
  *
  * Used to load class property definitions (property type metadata).
  */
final case class TiledProjectJson(
  propertyTypes: List[TiledProjectPropertyTypeJson] = Nil
)

final case class TiledProjectPropertyTypeJson(
  name:               String = "",
  @named("type") tpe: String = "",
  members:            List[TiledProjectMemberJson] = Nil
)

/** A member of a Tiled project class definition. The `value` field is polymorphic (string, number, boolean, or object). */
final case class TiledProjectMemberJson(
  name:               String = "",
  @named("type") tpe: String = "",
  propertyType:       Option[String] = None,
  value:              Option[Json] = None
)

object TiledProjectJson {
  given codec: sge.utils.JsonCodec[TiledProjectJson] = sge.utils.JsonCodec.make
}
