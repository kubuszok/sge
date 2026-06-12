/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * JVM pin of the Double.toString-divergent numeric tokens for ISS-513. On the JVM,
 * java.lang.Double.toString of a whole-valued double / negative zero matches gdx's
 * JsonValue.asStringArray (JsonValue.java:398) byte-for-byte: 1e2 → "100.0", -0.0 → "-0.0".
 * Scala.js produces "100"/"0" instead (see the scalajs copy of this suite); that divergence
 * is a Scala.js j.l.Double.toString property, surfaced here rather than hidden.
 */
package sge
package gltf
package loaders
package blender

import BlenderShapeKeysTestSupport.{ num, parseSingle, parseSingleEndToEnd }

class BlenderShapeKeysNumberFormatSuite extends munit.FunSuite {

  test("JVM: exponent token 1e2 → \"100.0\" via Double.toString (JsonValue.java:398, matches gdx)") {
    // BigDecimal.toString of 1e2 is "1E+2"; numberToString re-parses as double and emits Double.toString = "100.0".
    assertEquals(parseSingle(num("1e2")), "100.0")
  }

  test("JVM: -0.0 token → \"-0.0\" via Double.toString (JsonValue.java:398, matches gdx)") {
    // Constructed via JsonNumber.fromString so the "-0.0" token BYPASSES the codec; numberToString → Double.toString(-0.0) = "-0.0".
    assertEquals(parseSingle(num("-0.0")), "-0.0")
  }

  // --- end-to-end through the REAL codec (GLTFJsonParser.parse) — the honest pin of the codec's own number normalisation ---

  test(
    "JVM E2E: 1e2 through the codec → \"100.0\" (BigDecimal.toString = \"1E+2\", contains '+'; Double.toString = \"100.0\")"
  ) {
    // readBigDecimal stores "1E+2" in JsonNumber.value; numberToString's long/double scan MUST accept '+' to classify this
    // as a double, then Double.parseDouble/toString → "100.0". Dropping '+' from the scan set (line 135) makes this render
    // "1E+2" verbatim instead — the surviving mutation N1, killed by this E2E row.
    assertEquals(parseSingleEndToEnd("1e2"), "100.0")
  }

  test(
    "JVM E2E: -0.0 through the codec → \"0.0\" (readBigDecimal drops the negative-zero sign; Double.toString(0.0) = \"0.0\")"
  ) {
    // The shared JsonCodec decodes -0.0 via readBigDecimal, whose BigDecimal.toString is "0.0" (sign dropped) before
    // numberToString runs. This is the actual codec divergence from gdx (gdx renders Double.toString(-0.0) = "-0.0").
    assertEquals(parseSingleEndToEnd("-0.0"), "0.0")
  }

  test(
    "JVM E2E: 40-digit integer → \"1.2345678901234568E39\" (>34-sig-digit codec rounding double-rendered; diverges from gdx's verbatim token)"
  ) {
    // gdx classifies the digits-only token as couldBeLong, overflows Long.parseLong and renders the 40-digit token VERBATIM
    // (JsonReader.java:256-266). The port's codec rounds it in DECIMAL128 to "1.234567890123456789012345678901235E+39"
    // (34 sig digits) before numberToString sees it; the '.'/'E'/'+' then route it through Double.parseDouble/toString to
    // "1.2345678901234568E39". This divergence is a property of readBigDecimal's DECIMAL128 rounding, not BlenderShapeKeys.
    assertEquals(parseSingleEndToEnd("1234567890123456789012345678901234567890"), "1.2345678901234568E39")
  }
}
