/*
 * SGE - Scala Game Engine
 * copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (JSON DTO case classes for .tiled-project format)
 *   Convention: mirrors .tiled-project JSON structure for jsoniter-scala codec derivation
 *   Idiom: split packages, final case class, Option for optional fields
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 42
 * Covenant-baseline-methods: TiledProjectJson,TiledProjectMemberJson,TiledProjectPropertyTypeJson
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package maps
package tiled

import sge.utils.{ Json, fieldName }
import sge.utils.given

/** JSON DTO case classes for Tiled project files (.tiled-project).
  *
  * Used to load class property definitions (property type metadata).
  */
final case class TiledProjectJson(
  propertyTypes: List[TiledProjectPropertyTypeJson] = Nil
)

final case class TiledProjectPropertyTypeJson(
  name:                   String = "",
  @fieldName("type") tpe: String = "",
  members:                List[TiledProjectMemberJson] = Nil
)

/** A member of a Tiled project class definition. The `value` field is polymorphic (string, number, boolean, or object). */
final case class TiledProjectMemberJson(
  name:                   String = "",
  @fieldName("type") tpe: String = "",
  propertyType:           Option[String] = None,
  value:                  Option[Json] = None
)

object TiledProjectJson {
  given codec: sge.utils.JsonCodec[TiledProjectJson] = sge.utils.JsonCodec.derive[TiledProjectJson]
}
