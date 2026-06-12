/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Scala.js pin of the Double.toString-divergent numeric tokens for ISS-513 — surfacing,
 * not papering over, a real per-platform divergence from gdx.
 *
 * gdx's JsonValue.asStringArray renders whole-valued / negative-zero doubles via
 * java.lang.Double.toString (JsonValue.java:398): on the JVM and Scala Native that yields
 * "100.0" and "-0.0" (see the scalajvm / scalanative copies of this suite). Scala.js's
 * java.lang.Double.toString follows ECMAScript Number-to-string semantics, which drop the
 * trailing ".0" of whole-valued doubles and the sign of negative zero, so BlenderShapeKeys
 * emits "100" and "0" in the browser. This is a Scala.js standard-library property
 * (java.lang.Double.toString(100.0) === "100", java.lang.Double.toString(-0.0) === "0"),
 * not a BlenderShapeKeys porting error; it is pinned here so the divergence is observable
 * and tracked rather than silently assumed. Decimal tokens that are not whole-valued
 * (0.6, 10.5) are identical on all three platforms and are pinned in the shared
 * BlenderShapeKeysSemanticsSuite.
 */
package sge
package gltf
package loaders
package blender

import BlenderShapeKeysTestSupport.{ num, parseSingle, parseSingleEndToEnd }

class BlenderShapeKeysNumberFormatSuite extends munit.FunSuite {

  test("JS: exponent token 1e2 → \"100\" (Scala.js Double.toString drops \".0\"; diverges from gdx \"100.0\")") {
    assertEquals(parseSingle(num("1e2")), "100")
  }

  test("JS: -0.0 token → \"0\" (Scala.js Double.toString drops the negative-zero sign; diverges from gdx \"-0.0\")") {
    assertEquals(parseSingle(num("-0.0")), "0")
  }

  // --- end-to-end through the REAL codec (GLTFJsonParser.parse) — the honest pin of the codec's own number normalisation ---

  test(
    "JS E2E: 1e2 through the codec → \"100\" (BigDecimal.toString = \"1E+2\", contains '+'; Scala.js Double.toString = \"100\")"
  ) {
    // readBigDecimal stores "1E+2" in JsonNumber.value; numberToString's long/double scan MUST accept '+' to classify this
    // as a double. Dropping '+' from the scan set (line 135) makes this render "1E+2" verbatim instead — the surviving
    // mutation N1, killed by this E2E row. Scala.js Double.toString of the whole-valued double yields "100".
    assertEquals(parseSingleEndToEnd("1e2"), "100")
  }

  test(
    "JS E2E: -0.0 through the codec → \"0\" (readBigDecimal drops the negative-zero sign; Scala.js Double.toString(0.0) = \"0\")"
  ) {
    // The shared JsonCodec decodes -0.0 via readBigDecimal, whose BigDecimal.toString is "0.0" (sign dropped) before
    // numberToString runs; Scala.js Double.toString then yields "0". The actual codec divergence from gdx ("-0.0").
    assertEquals(parseSingleEndToEnd("-0.0"), "0")
  }
}
