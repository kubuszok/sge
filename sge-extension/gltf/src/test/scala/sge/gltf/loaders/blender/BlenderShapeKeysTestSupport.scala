/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Shared helpers for ISS-513 BlenderShapeKeys semantics tests. Used by the
 * cross-platform BlenderShapeKeysSemanticsSuite and the per-platform
 * BlenderShapeKeysNumberFormatSuite (scalajvm/scalanative/scalajs) that pin the
 * Double.toString-divergent numeric tokens.
 */
package sge
package gltf
package loaders
package blender

import sge.gltf.data.GLTFExtras
import sge.gltf.data.geometry.GLTFMesh
import sge.gltf.loaders.gltf.GLTFJsonParser
import lowlevel.Nullable
import hearth.kindlings.jsoniterjson.{ Json, JsonNumber, JsonObject }

object BlenderShapeKeysTestSupport {

  /** Builds a GLTFMesh whose extras is `{ "targetNames": <targetNames> }`, with the Json AST supplied directly so element tokens are exact. */
  def meshWithExtras(targetNames: Json): GLTFMesh = {
    val mesh   = new GLTFMesh()
    val extras = new GLTFExtras()
    extras.value = Nullable(Json.Obj(JsonObject("targetNames" -> targetNames)))
    mesh.extras = Nullable(extras)
    mesh
  }

  /** Builds a GLTFMesh whose extras value is `<extras>` directly (used for non-object extras and non-array targetNames). */
  def meshWithExtrasValue(extras: Json): GLTFMesh = {
    val mesh = new GLTFMesh()
    val box  = new GLTFExtras()
    box.value = Nullable(extras)
    mesh.extras = Nullable(box)
    mesh
  }

  /** A Json.Num built directly from a token string, preserving the exact token (bypassing codec number normalisation). */
  def num(token: String): Json =
    Json.Num(JsonNumber.fromString(token).getOrElse(throw new AssertionError(s"JsonNumber.fromString($token) returned None")))

  /** A GLTFMesh with no extras at all (exercises BlenderShapeKeys.java:28). */
  def meshWithoutExtras(): GLTFMesh = new GLTFMesh()

  /** Parses a single-element targetNames array and returns the converted string element (raw String; may be null for a JSON null element). */
  def parseSingle(element: Json): String = {
    val result = BlenderShapeKeys.parse(meshWithExtras(Json.Arr(Vector(element))))
    if (Nullable.isEmpty(result)) throw new AssertionError("parse must return an array for a JSON array targetNames")
    val names = result.get
    if (names.size != 1) throw new AssertionError(s"expected a single element, got ${names.size}")
    names(0)
  }

  /** Builds a single-mesh glTF JSON document whose mesh carries `extras.targetNames` holding the given raw numeric token, parses it through the REAL codec (GLTFJsonParser.parse → readFromString[GLTF]
    * → GLTFExtras codec → kindlings Json codec → readBigDecimal → JsonNumber.value = BigDecimal.toString), then runs BlenderShapeKeys.parse and returns the single converted element. Unlike
    * `parseSingle` (which feeds a JsonNumber.fromString token verbatim, bypassing the codec) this exercises the full decode path, so codec number normalisation (e.g. `1e2` → "1E+2", `-0.0` → "0.0",
    * >34-digit rounding) is observable in the output.
    */
  def parseSingleEndToEnd(rawNumericToken: String): String = {
    val json =
      s"""{
         |  "asset": {"version": "2.0"},
         |  "meshes": [
         |    {
         |      "name": "Plane",
         |      "extras": {
         |        "targetNames": [$rawNumericToken]
         |      },
         |      "primitives": [{"attributes": {"POSITION": 0}}]
         |    }
         |  ]
         |}""".stripMargin
    val glMesh = GLTFJsonParser.parse(json).meshes.get(0)
    val result = BlenderShapeKeys.parse(glMesh)
    if (Nullable.isEmpty(result)) throw new AssertionError("parse must return an array for a JSON array targetNames")
    val names = result.get
    if (names.size != 1) throw new AssertionError(s"expected a single element, got ${names.size}")
    names(0)
  }
}
