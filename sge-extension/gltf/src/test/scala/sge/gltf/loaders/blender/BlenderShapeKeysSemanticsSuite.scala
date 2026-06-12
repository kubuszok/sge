/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Green semantics suite for ISS-513 — pins BlenderShapeKeys.parse's per-element
 * conversion (BlenderShapeKeys.scala elementToString/numberToString) against
 * com.badlogic.gdx.utils.JsonValue.asStringArray (JsonValue.java:387-415) read
 * through JsonReader's number token classification (JsonReader.java:220-266).
 *
 * gdx renders numeric array elements via Long.toString(longValue) /
 * Double.toString(doubleValue) of the value JsonReader parsed (asStringArray reads
 * the *array's* stringValue, which is null for a parsed array — JsonValue.java:398/401
 * — so the source token is never returned). The kindlings AST stores BigDecimal.toString
 * in JsonNumber.value, which diverges from those toString forms (e.g. 1e2 → "1E+2");
 * numberToString re-runs JsonReader's classification over JsonNumber.value to recover
 * gdx's output. These assertions construct the extras Json AST directly (Json.Num/
 * Json.Bool/Json.Null/Json.Arr/Json.Obj/JsonNumber.fromString) so each numeric token is
 * pinned exactly, bypassing the codec's number normalisation where it would erase the
 * token under test (see the -0.0 cases below).
 */
package sge
package gltf
package loaders
package blender

import lowlevel.Nullable
import hearth.kindlings.jsoniterjson.{ Json, JsonNumber, JsonObject }

import BlenderShapeKeysTestSupport.{ meshWithExtras, meshWithExtrasValue, meshWithoutExtras, num, parseSingle }

class BlenderShapeKeysSemanticsSuite extends munit.FunSuite {

  // --- number elements: gdx Long/Double.toString of the parsed value (JsonValue.java:398/401) ---

  test("integer element renders via Long.toString — 5 → \"5\" (JsonValue.java:401)") {
    assertEquals(parseSingle(num("5")), "5")
  }

  test("decimal element renders via Double.toString — 0.6 → \"0.6\" (JsonValue.java:398)") {
    assertEquals(parseSingle(num("0.6")), "0.6")
  }

  test("trailing-zero decimal — 10.50 → \"10.5\" (JsonValue.java:398)") {
    // JsonNumber.value keeps "10.50"; Double.parseDouble/Double.toString normalises to "10.5" on every platform.
    assertEquals(parseSingle(num("10.50")), "10.5")
  }

  // The two toString-divergent number cases (exponent 1e2 → whole-valued double, and -0.0) are pinned per platform
  // in BlenderShapeKeysNumberFormatSuite: java.lang.Double.toString of a whole-valued double / negative zero matches
  // gdx on the JVM and Scala Native ("100.0" / "-0.0") but follows JS Number semantics on Scala.js ("100" / "0").
  // That divergence is real (Scala.js j.l.Double.toString, not BlenderShapeKeys) and is proven, not papered over,
  // by the platform-specific BlenderShapeKeysNumberFormatSuite under scalajvm / scalanative / scalajs.

  test("codec divergence surfaced: parsed -0.0 normalises to \"0.0\" upstream in JsonNumber.fromBigDecimal") {
    // Documents (does not paper over) the residual divergence from gdx: the shared JsonCodec decodes -0.0 through
    // readBigDecimal, which drops the sign, so JsonNumber.value is already "0.0" when elementToString runs. This is a
    // property of the codec (JsonCodec.decode → readBigDecimal → JsonNumber.fromBigDecimal), not of BlenderShapeKeys.
    assertEquals(JsonNumber.fromBigDecimal(BigDecimal("-0.0")).value, "0.0")
  }

  test("integer beyond Long range falls back to the raw token (JsonReader.java:256-266)") {
    // couldBeLong is true but Long.parseLong overflows; gdx falls through to string(valueName, value) leaving the
    // verbatim token as a stringValue element, and numberToString mirrors that with its raw-string fallback.
    assertEquals(parseSingle(num("123456789012345678901234567890")), "123456789012345678901234567890")
  }

  // --- non-number elements (JsonValue.java:394-408) ---

  test("string element returns itself (JsonValue.java:394-396)") {
    assertEquals(parseSingle(Json.fromString("Water")), "Water")
  }

  test("boolean true element → \"true\" (JsonValue.java:403-405)") {
    assertEquals(parseSingle(Json.fromBoolean(true)), "true")
  }

  test("boolean false element → \"false\" (JsonValue.java:403-405)") {
    assertEquals(parseSingle(Json.fromBoolean(false)), "false")
  }

  test("null element keeps a null array entry (JsonValue.java:406-408)") {
    // asStringArray writes null for a nullValue child (JsonValue.java:407); the gdx Array<String> retains it, and the
    // SGE port leaves the pre-initialised null entry in place (BlenderShapeKeys.scala asStringArray mirror).
    val result = BlenderShapeKeys.parse(
      meshWithExtras(Json.Arr(Vector(Json.fromString("Water"), Json.Null, Json.fromString("Mountains"))))
    )
    assert(Nullable.isDefined(result))
    val names = result.get
    assertEquals(names.size, 3)
    assertEquals(names(0), "Water")
    assert(Nullable(names(1)).isEmpty, "null JSON element must leave a null array entry")
    assertEquals(names(2), "Mountains")
  }

  // --- IllegalStateException for nested array/object elements (JsonValue.java:409-410) ---

  test("nested array element throws IllegalStateException (JsonValue.java:409-410)") {
    val ex = intercept[IllegalStateException] {
      BlenderShapeKeys.parse(meshWithExtras(Json.Arr(Vector(Json.Arr(Vector(Json.fromString("x")))))))
    }
    assert(ex.getMessage.contains("array"), ex.getMessage)
  }

  test("nested object element throws IllegalStateException (JsonValue.java:409-410)") {
    val ex = intercept[IllegalStateException] {
      BlenderShapeKeys.parse(meshWithExtras(Json.Arr(Vector(Json.Obj(JsonObject("k" -> Json.fromString("v")))))))
    }
    assert(ex.getMessage.contains("object"), ex.getMessage)
  }

  // --- non-array targetNames → null (BlenderShapeKeys.java:30 `targetNames.isArray()` guard) ---

  test("targetNames as string yields no result (BlenderShapeKeys.java:30/33)") {
    assert(Nullable.isEmpty(BlenderShapeKeys.parse(meshWithExtras(Json.fromString("nope")))))
  }

  test("targetNames as number yields no result (BlenderShapeKeys.java:30/33)") {
    assert(Nullable.isEmpty(BlenderShapeKeys.parse(meshWithExtras(num("5")))))
  }

  test("targetNames as boolean yields no result (BlenderShapeKeys.java:30/33)") {
    assert(Nullable.isEmpty(BlenderShapeKeys.parse(meshWithExtras(Json.fromBoolean(true)))))
  }

  test("targetNames as object yields no result (BlenderShapeKeys.java:30/33)") {
    assert(Nullable.isEmpty(BlenderShapeKeys.parse(meshWithExtras(Json.Obj(JsonObject("a" -> Json.fromString("b")))))))
  }

  test("targetNames as null yields no result (BlenderShapeKeys.java:30/33)") {
    assert(Nullable.isEmpty(BlenderShapeKeys.parse(meshWithExtras(Json.Null))))
  }

  // --- non-object extras value → null (BlenderShapeKeys.java:29 `value.get("targetNames")` on non-object) ---

  test("non-object extras value yields no result (BlenderShapeKeys.java:29)") {
    assert(Nullable.isEmpty(BlenderShapeKeys.parse(meshWithExtrasValue(Json.fromString("scalar extras")))))
  }

  test("extras object without targetNames yields no result (BlenderShapeKeys.java:33)") {
    assert(Nullable.isEmpty(BlenderShapeKeys.parse(meshWithExtrasValue(Json.Obj(JsonObject("other" -> num("1")))))))
  }

  // --- absent extras → null (BlenderShapeKeys.java:28) ---

  test("absent extras yields no result (BlenderShapeKeys.java:28)") {
    assert(Nullable.isEmpty(BlenderShapeKeys.parse(meshWithoutExtras())))
  }
}
