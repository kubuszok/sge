/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * End-to-end numeric coverage for ISS-513 — drives BlenderShapeKeys.parse's
 * numberToString classification through the REAL codec (GLTFJsonParser.parse of a
 * full glTF document, decoded via GLTFExtras' kindlings Json codec → readBigDecimal →
 * JsonNumber.value = BigDecimal.toString). The platform-agnostic semantics suite feeds
 * tokens verbatim via JsonNumber.fromString, which bypasses the codec; that left a hole
 * where numberToString's long/double scan set could lose codec-introduced characters
 * (notably '+' from BigDecimal's "1E+2") without any committed suite turning red. These
 * rows close that hole by exercising the exact decode path the loader takes at runtime.
 *
 * Cross-platform rows only — tokens whose Long.toString / Double.toString form is
 * identical on JVM, Scala Native and Scala.js. The codec-divergent / Double.toString-
 * divergent rows (1e2, -0.0, the >34-digit integer rounding case) are pinned per platform
 * in BlenderShapeKeysNumberFormatSuite (scalajvm/scalanative/scalajs).
 */
package sge
package gltf
package loaders
package blender

import BlenderShapeKeysTestSupport.parseSingleEndToEnd

class BlenderShapeKeysEndToEndSuite extends munit.FunSuite {

  test("E2E: integer 5 → \"5\" through the real codec (Long.toString, JsonValue.java:401)") {
    // BigDecimal.toString("5") = "5"; numberToString classifies long, Long.parseLong/toString → "5".
    assertEquals(parseSingleEndToEnd("5"), "5")
  }

  test("E2E: decimal 0.6 → \"0.6\" through the real codec (Double.toString, JsonValue.java:398)") {
    // BigDecimal.toString("0.6") = "0.6"; numberToString classifies double, Double.parseDouble/toString → "0.6".
    assertEquals(parseSingleEndToEnd("0.6"), "0.6")
  }

  test("E2E: trailing-zero decimal 10.50 → \"10.5\" through the real codec (Double.toString, JsonValue.java:398)") {
    // readBigDecimal preserves scale, BigDecimal.toString = "10.50"; Double.parseDouble/toString normalises to "10.5".
    assertEquals(parseSingleEndToEnd("10.50"), "10.5")
  }

  test(
    "E2E: in-range long 9007199254740993 → \"9007199254740993\" through the real codec (Long.toString, JsonValue.java:401)"
  ) {
    // 2^53 + 1: within Long range and exactly representable as a long but NOT as a double. BigDecimal.toString keeps the
    // digits, numberToString classifies long (no '.'/'e'/'E'), Long.parseLong/toString preserves every digit — proving the
    // long branch, not a double round-trip, renders this token.
    assertEquals(parseSingleEndToEnd("9007199254740993"), "9007199254740993")
  }

  test("E2E: 30-digit integer falls back to the verbatim token through the real codec (JsonReader.java:256-266)") {
    // 30 significant digits < DECIMAL128's 34, so readBigDecimal does NOT round; BigDecimal.toString keeps the plain digits.
    // couldBeLong is true but Long.parseLong overflows, so numberToString returns the raw token verbatim — exactly gdx's
    // string(valueName, value) fallback.
    assertEquals(parseSingleEndToEnd("123456789012345678901234567890"), "123456789012345678901234567890")
  }
}
