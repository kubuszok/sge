/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite for ISS-510: KHR_lights_punctual typed decoding + codec round-trip.
 *
 * Problem (a): GLTFCodecs.gltfExtensionsCodec stores KHR_lights_punctual as a
 * raw Json AST (GLTFCodecs.scala:215-223) while the consumer casts the stored
 * object to the typed model: GLTFLoaderBase.loadLights does
 * `exts.get(classOf[KHRLightsPunctual.GLTFLights], KHRLightsPunctual.EXT)`
 * (GLTFLoaderBase.scala:155-160) and getNode does
 * `exts.get(classOf[KHRLightsPunctual.GLTFLightNode], KHRLightsPunctual.EXT)`
 * (GLTFLoaderBase.scala:277-281). GLTFExtensions.get is an unchecked
 * `asInstanceOf[T]` (GLTFExtensions.scala:31-35), so ANY glTF document
 * declaring the extension dies with a ClassCastException the moment the
 * supposedly-typed object is touched. Typed codecs for GLTFLights/GLTFLightNode
 * exist UNUSED at GLTFCodecs.scala:708-730.
 *
 * Original behavior (original-src/gdx-gltf, never fetched):
 *   - net/mgsx/gltf/data/GLTFExtensions.java:28-36 — get(Class<T>, ext) lazily
 *     parses the raw JsonValue into the REQUESTED type per call site, so the
 *     same extension name yields GLTFLights at the root and GLTFLightNode at a
 *     node.
 *   - net/mgsx/gltf/loaders/shared/GLTFLoaderBase.java:189-197 (loadLights)
 *     iterates lightExt.lights; :330-334 reads nodeLight.light.
 *   - net/mgsx/gltf/data/extensions/KHRLightsPunctual.java:29-57 — GLTFLight
 *     carries name/color/intensity/type/range, GLTFLights carries the lights
 *     array, GLTFLightNode carries the light index.
 * The red tests below replicate exactly those consumer call sites against the
 * decoded model and pin the typed values from the JSON, so the fix must
 * actually parse the extension (not merely avoid the crash).
 *
 * Problem (b): ~44 encodeValue implementations in GLTFCodecs.scala are
 * `out.writeNull()` (e.g. gltfCodec:893, gltfAssetCodec:865,
 * gltfNodeCodec:646, gltfExtensionsCodec:236-237), so encoding ANY document
 * produces the literal string "null" — the codecs cannot round-trip a single
 * document despite the Covenant full-port header. The round-trip tests decode
 * a small document, encode it back, and assert specific JSON content survives.
 *
 * Platform scope: shared (src/test/scala) — the codecs and the data model are
 * pure jsoniter-scala code with no GL/filesystem dependency, headless on
 * JVM, JS and Native alike.
 */
package sge
package gltf
package data

import sge.gltf.data.GLTFCodecs.given
import sge.gltf.data.extensions.KHRLightsPunctual
import sge.gltf.loaders.gltf.GLTFJsonParser
import sge.utils.writeToString
import lowlevel.Nullable

class KhrLightsPunctualRedSuite extends munit.FunSuite {

  /** Minimal glTF document declaring KHR_lights_punctual with one point light at the root and a node referencing light 0 — the smallest document that reaches both cast sites
    * (GLTFLoaderBase.scala:155-160 and 277-281).
    */
  private val lightsJson =
    """{
      |  "asset": {"version": "2.0", "generator": "sge-test"},
      |  "extensionsUsed": ["KHR_lights_punctual"],
      |  "extensions": {
      |    "KHR_lights_punctual": {
      |      "lights": [
      |        {"name": "Lamp", "type": "point", "color": [1.0, 0.5, 0.25], "intensity": 40.0, "range": 12.5}
      |      ]
      |    }
      |  },
      |  "scene": 0,
      |  "scenes": [{"name": "Scene", "nodes": [0]}],
      |  "nodes": [
      |    {"name": "LampNode", "extensions": {"KHR_lights_punctual": {"light": 0}}}
      |  ]
      |}""".stripMargin

  // ── GREEN control — harness sanity ──────────────────────────────────

  test("ISS-510 control: plain decode without the extension works (green)") {
    val gltf = GLTFJsonParser.parse("""{"asset":{"version":"2.0","generator":"sge-test"},"scene":0}""")
    assertEquals(gltf.scene, 0)
    assertEquals(gltf.asset.get.version.getOrElse("<missing>"), "2.0")
    assertEquals(gltf.asset.get.generator.getOrElse("<missing>"), "sge-test")
  }

  // ── (b) encode round-trip — encodeValue is writeNull today ──────────

  test("ISS-510: encoding a decoded document preserves asset.version (round-trip)") {
    val gltf    = GLTFJsonParser.parse("""{"asset":{"version":"2.0","generator":"sge-test"},"scene":0}""")
    val encoded = writeToString(gltf)
    // gltfCodec.encodeValue is out.writeNull() (GLTFCodecs.scala:893) — the
    // whole document encodes to the literal "null" today.
    assert(encoded.contains("\"version\":\"2.0\""), s"encoded document lost asset.version: $encoded")
    val reparsed = GLTFJsonParser.parse(encoded)
    assertEquals(reparsed.asset.get.version.getOrElse("<missing>"), "2.0")
    assertEquals(reparsed.asset.get.generator.getOrElse("<missing>"), "sge-test")
  }

  test("ISS-510: encoding a decoded document preserves a node name (round-trip)") {
    val gltf    = GLTFJsonParser.parse("""{"asset":{"version":"2.0"},"nodes":[{"name":"LampNode","translation":[1.0,2.0,3.0]}]}""")
    val encoded = writeToString(gltf)
    assert(encoded.contains("\"LampNode\""), s"encoded document lost the node name: $encoded")
    val reparsed = GLTFJsonParser.parse(encoded)
    assertEquals(reparsed.nodes.get(0).name.getOrElse("<missing>"), "LampNode")
    val t = reparsed.nodes.get(0).translation.get
    assertEqualsFloat(t(0), 1.0f, 0.0001f)
    assertEqualsFloat(t(1), 2.0f, 0.0001f)
    assertEqualsFloat(t(2), 3.0f, 0.0001f)
  }

  test("ISS-510: encoding a decoded document preserves the light intensity (round-trip)") {
    val gltf    = GLTFJsonParser.parse(lightsJson)
    val encoded = writeToString(gltf)
    // gltfExtensionsCodec.encodeValue is also out.writeNull()
    // (GLTFCodecs.scala:236-237) — even with a fixed root codec the extension
    // payload would be dropped, so pin it explicitly.
    assert(encoded.contains("\"intensity\":40"), s"encoded document lost the light intensity: $encoded")
    assert(encoded.contains("KHR_lights_punctual"), s"encoded document lost the extension: $encoded")
  }

  // ── (a) typed extension decoding — CCE today ────────────────────────
  // These run LAST: on Scala.js the ClassCastException surfaces as a fatal
  // UndefinedBehaviorError that aborts the remainder of the suite, so the
  // control and round-trip tests above must already have executed.

  test("ISS-510: root KHR_lights_punctual decodes as typed GLTFLights (GLTFLoaderBase.loadLights cast site)") {
    val gltf = GLTFJsonParser.parse(lightsJson)
    assert(Nullable.isDefined(gltf.extensions), "root extensions must be present")
    val exts = gltf.extensions.get
    // Replicates GLTFLoaderBase.scala:155-160 (original Java: GLTFLoaderBase.java:189-197).
    // Today exts.get returns the raw Json AST cast unchecked to GLTFLights
    // (GLTFExtensions.scala:31-35) — ClassCastException on first typed use.
    val lightExt = exts.get(classOf[KHRLightsPunctual.GLTFLights], KHRLightsPunctual.EXT)
    assert(Nullable.isDefined(lightExt), "KHR_lights_punctual root extension must be present")
    val le: KHRLightsPunctual.GLTFLights = lightExt.get
    assert(Nullable.isDefined(le.lights), "lights array must be parsed")
    val lights = le.lights.get
    // Expected typed values pinned from the JSON above, per the original data
    // model KHRLightsPunctual.java:29-57.
    assertEquals(lights.size, 1)
    val light = lights(0)
    assertEquals(light.name, "Lamp")
    assertEquals(light.`type`.getOrElse("<missing>"), KHRLightsPunctual.GLTFLight.TYPE_POINT)
    assertEqualsFloat(light.color(0), 1.0f, 0.0001f)
    assertEqualsFloat(light.color(1), 0.5f, 0.0001f)
    assertEqualsFloat(light.color(2), 0.25f, 0.0001f)
    assertEqualsFloat(light.intensity, 40.0f, 0.0001f)
    assert(Nullable.isDefined(light.range), "range must be parsed")
    assertEqualsFloat(light.range.get, 12.5f, 0.0001f)
  }

  test("ISS-510: node KHR_lights_punctual decodes as typed GLTFLightNode (GLTFLoaderBase.getNode cast site)") {
    val gltf = GLTFJsonParser.parse(lightsJson)
    val node = gltf.nodes.get(0)
    assertEquals(node.name.getOrElse("<missing>"), "LampNode")
    assert(Nullable.isDefined(node.extensions), "node extensions must be present")
    val exts = node.extensions.get
    // Replicates GLTFLoaderBase.scala:277-281 (original Java: GLTFLoaderBase.java:330-334).
    val nodeLight = exts.get(classOf[KHRLightsPunctual.GLTFLightNode], KHRLightsPunctual.EXT)
    assert(Nullable.isDefined(nodeLight), "KHR_lights_punctual node extension must be present")
    val nl: KHRLightsPunctual.GLTFLightNode = nodeLight.get
    assert(Nullable.isDefined(nl.light), "light index must be parsed")
    assertEquals(nl.light.get, 0)
  }
}
