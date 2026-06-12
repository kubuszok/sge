/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Committed corpus round-trip suite for ISS-510 (auditor finding 2).
 *
 * The audit established that the typed-entry encode path and the bulk of the
 * GLTFCodecs encoders had ZERO committed coverage: mutating the gltfLight
 * encoder (dropping the "range" field) and regressing the gltfBufferView
 * encoder back to writeNull both survived the full committed suite. The earlier
 * "corpus probe" used original-src/gdx-gltf reference files, which are not
 * compiled and not shipped — the repo has no .gltf fixtures at all.
 *
 * This suite embeds a compact glTF 2.0 corpus as in-repo strings (so it runs
 * headless on JVM, JS and Native with no filesystem) covering the feature
 * surface called out by the auditor — lights including spot + range,
 * bufferViews and accessors including sparse, materials + PBR + texture infos,
 * animations, and morph targets — then asserts decode -> encode -> re-decode
 * preserves every field recursively. Because the assertions read the
 * RE-DECODED model (not substrings), any encoder that drops a field or emits
 * null fails the corresponding assertion:
 *   - drop "range" from gltfLightCodec.encodeValue -> light.range empty on
 *     re-decode -> "spot light range survives" / "point light range survives"
 *     fail.
 *   - regress gltfBufferViewCodec.encodeValue to out.writeNull() ->
 *     re-decoded bufferViews are null -> "bufferView fields survive" fails.
 *
 * It also pins auditor finding 1: after GLTFExtensions.get lazily parses the
 * KHR_lights_punctual root extension into the typed GLTFLights, re-encoding
 * must emit the extension key exactly once (no raw + typed duplicate), which
 * is RFC-8259-undefined and rejected by the glTF validator.
 *
 * Platform scope: shared (src/test/scala) — pure jsoniter-scala codecs and the
 * data model, no GL or filesystem dependency.
 */
package sge
package gltf
package data

import sge.gltf.data.GLTFCodecs.given
import sge.gltf.data.extensions.KHRLightsPunctual
import sge.gltf.loaders.gltf.GLTFJsonParser
import sge.utils.{ given_JsonCodec_Json, writeToString }
import lowlevel.Nullable

class GltfCorpusRoundTripSuite extends munit.FunSuite {

  /** Counts non-overlapping occurrences of `needle` in `haystack`. Used to assert single-key emission. */
  private def countOccurrences(haystack: String, needle: String): Int = {
    var count = 0
    var idx   = haystack.indexOf(needle)
    while (idx >= 0) {
      count += 1
      idx = haystack.indexOf(needle, idx + needle.length)
    }
    count
  }

  /** Decodes, encodes, and re-decodes a document, returning the re-decoded model so assertions read the post-round-trip state. A dropped/nulled encoder field is therefore visible as a missing value.
    */
  private def roundTrip(json: String): GLTF = {
    val gltf    = GLTFJsonParser.parse(json)
    val encoded = writeToString(gltf)
    GLTFJsonParser.parse(encoded)
  }

  /** Like [[roundTrip]] but first promotes the KHR_lights_punctual extension on the root and on every node from its raw JSON AST to the typed model via GLTFExtensions.get — exactly the
    * GLTFLoaderBase.loadLights / getNode call sequence. This forces the TYPED encode path (gltfLightsCodec -> gltfLightCodec, gltfLightNodeCodec) on re-encode; without the promotion the extension
    * stays a raw Json AST and re-encodes through jsonCodec, never touching the typed codecs. Required so the suite exercises (and a mutation in) the typed encoders is observable.
    */
  private def roundTripLightsTyped(json: String): GLTF = {
    val gltf = GLTFJsonParser.parse(json)
    if (Nullable.isDefined(gltf.extensions)) {
      gltf.extensions.get.get(classOf[KHRLightsPunctual.GLTFLights], KHRLightsPunctual.EXT)
    }
    if (Nullable.isDefined(gltf.nodes)) {
      gltf.nodes.get.foreach { node =>
        if (Nullable.isDefined(node.extensions)) {
          node.extensions.get.get(classOf[KHRLightsPunctual.GLTFLightNode], KHRLightsPunctual.EXT)
        }
      }
    }
    val encoded = writeToString(gltf)
    GLTFJsonParser.parse(encoded)
  }

  // ── lights: point + range and spot + range + cone angles ────────────

  private val lightsCorpus =
    """{
      |  "asset": {"version": "2.0", "generator": "sge-corpus"},
      |  "extensionsUsed": ["KHR_lights_punctual"],
      |  "extensions": {
      |    "KHR_lights_punctual": {
      |      "lights": [
      |        {"name": "PointLamp", "type": "point", "color": [1.0, 0.5, 0.25], "intensity": 40.0, "range": 12.5},
      |        {"name": "SpotLamp", "type": "spot", "color": [0.2, 0.4, 0.6], "intensity": 8.0, "range": 7.25,
      |         "spot": {"innerConeAngle": 0.25, "outerConeAngle": 0.75}}
      |      ]
      |    }
      |  },
      |  "nodes": [
      |    {"name": "LampNode", "extensions": {"KHR_lights_punctual": {"light": 1}}}
      |  ]
      |}""".stripMargin

  test("ISS-510 corpus: point light name/type/color/intensity/range survive round-trip") {
    val gltf   = roundTripLightsTyped(lightsCorpus)
    val exts   = gltf.extensions.get
    val le     = exts.get(classOf[KHRLightsPunctual.GLTFLights], KHRLightsPunctual.EXT).get
    val lights = le.lights.get
    assertEquals(lights.size, 2)
    val point = lights(0)
    assertEquals(point.name, "PointLamp")
    assertEquals(point.`type`.getOrElse("<missing>"), KHRLightsPunctual.GLTFLight.TYPE_POINT)
    assertEqualsFloat(point.color(0), 1.0f, 0.0001f)
    assertEqualsFloat(point.color(1), 0.5f, 0.0001f)
    assertEqualsFloat(point.color(2), 0.25f, 0.0001f)
    assertEqualsFloat(point.intensity, 40.0f, 0.0001f)
    // Kills the "drop range from gltfLightCodec" mutation.
    assert(Nullable.isDefined(point.range), "point light range must survive round-trip")
    assertEqualsFloat(point.range.get, 12.5f, 0.0001f)
  }

  test("ISS-510 corpus: spot light range + cone angles survive round-trip") {
    val gltf = roundTripLightsTyped(lightsCorpus)
    val exts = gltf.extensions.get
    val le   = exts.get(classOf[KHRLightsPunctual.GLTFLights], KHRLightsPunctual.EXT).get
    val spot = le.lights.get(1)
    assertEquals(spot.name, "SpotLamp")
    assertEquals(spot.`type`.getOrElse("<missing>"), KHRLightsPunctual.GLTFLight.TYPE_SPOT)
    // Kills the "drop range from gltfLightCodec" mutation on the spot branch too.
    assert(Nullable.isDefined(spot.range), "spot light range must survive round-trip")
    assertEqualsFloat(spot.range.get, 7.25f, 0.0001f)
    assert(Nullable.isDefined(spot.spot), "spot sub-object must survive round-trip")
    assertEqualsFloat(spot.spot.get.innerConeAngle, 0.25f, 0.0001f)
    assertEqualsFloat(spot.spot.get.outerConeAngle, 0.75f, 0.0001f)
  }

  test("ISS-510 corpus: node KHR_lights_punctual light index survives round-trip") {
    val gltf      = roundTripLightsTyped(lightsCorpus)
    val node      = gltf.nodes.get(0)
    val nodeExts  = node.extensions.get
    val nodeLight = nodeExts.get(classOf[KHRLightsPunctual.GLTFLightNode], KHRLightsPunctual.EXT).get
    assert(Nullable.isDefined(nodeLight.light), "node light index must survive round-trip")
    assertEquals(nodeLight.light.get, 1)
  }

  // ── finding 1: single-key emission after the lazy get() typed path ──

  test("ISS-510 corpus: extension key emitted once after get() promotes raw to typed (no duplicate key)") {
    val gltf = GLTFJsonParser.parse(lightsCorpus)
    // Force the lazy parse so the entry exists in BOTH the typed and (pre-fix) raw maps.
    val exts = gltf.extensions.get
    val le   = exts.get(classOf[KHRLightsPunctual.GLTFLights], KHRLightsPunctual.EXT)
    assert(Nullable.isDefined(le), "root KHR_lights_punctual must be present")
    // Encode the root GLTFExtensions object in isolation through its own codec, so the count is over
    // exactly one JSON object and is immune to outer-document field ordering. Before the fix, get()
    // left the raw entry in place, so encodeValue wrote the typed entry AND the raw entry ->
    // {"KHR_lights_punctual":{...},"KHR_lights_punctual":{...}} (RFC-8259-undefined duplicate key).
    val encodedExts = writeToString(exts)
    assertEquals(
      countOccurrences(encodedExts, "\"KHR_lights_punctual\""),
      1,
      s"root extensions emitted the KHR_lights_punctual key more than once (duplicate key): $encodedExts"
    )
    // And the round-trip still preserves the typed payload.
    val re   = roundTrip(lightsCorpus)
    val rele = re.extensions.get.get(classOf[KHRLightsPunctual.GLTFLights], KHRLightsPunctual.EXT).get
    assertEquals(rele.lights.get.size, 2)
  }

  // ── bufferViews + accessors including sparse ────────────────────────

  private val accessorsCorpus =
    """{
      |  "asset": {"version": "2.0"},
      |  "buffers": [{"name": "buf0", "byteLength": 1024, "uri": "data.bin"}],
      |  "bufferViews": [
      |    {"name": "bv0", "buffer": 0, "byteOffset": 16, "byteLength": 256, "byteStride": 12, "target": 34962},
      |    {"name": "bvIndices", "buffer": 0, "byteOffset": 272, "byteLength": 64}
      |  ],
      |  "accessors": [
      |    {"name": "posAcc", "bufferView": 0, "byteOffset": 0, "componentType": 5126, "normalized": false,
      |     "count": 24, "type": "VEC3", "min": [-1.0, -1.0, -1.0], "max": [1.0, 1.0, 1.0],
      |     "sparse": {"count": 3,
      |       "indices": {"bufferView": 1, "byteOffset": 4, "componentType": 5123},
      |       "values": {"bufferView": 1, "byteOffset": 8}}}
      |  ]
      |}""".stripMargin

  test("ISS-510 corpus: bufferView fields survive round-trip") {
    val gltf = roundTrip(accessorsCorpus)
    val bvs  = gltf.bufferViews.get
    assertEquals(bvs.size, 2)
    // Kills the gltfBufferViewCodec.encodeValue regression to out.writeNull(): a nulled encoder
    // yields null bufferViews on re-decode and these reads fail.
    val bv0 = bvs(0)
    assertEquals(bv0.name.getOrElse("<missing>"), "bv0")
    assertEquals(bv0.buffer.get, 0)
    assertEquals(bv0.byteOffset, 16)
    assertEquals(bv0.byteLength, 256)
    assertEquals(bv0.byteStride.get, 12)
    assertEquals(bv0.target.get, 34962)
    val bv1 = bvs(1)
    assertEquals(bv1.name.getOrElse("<missing>"), "bvIndices")
    assertEquals(bv1.byteOffset, 272)
    assertEquals(bv1.byteLength, 64)
    // byteStride/target were absent in the source and must remain absent.
    assert(Nullable.isEmpty(bv1.byteStride), "absent byteStride must stay absent")
    assert(Nullable.isEmpty(bv1.target), "absent target must stay absent")
  }

  test("ISS-510 corpus: accessor + sparse fields survive round-trip") {
    val gltf = roundTrip(accessorsCorpus)
    val acc  = gltf.accessors.get(0)
    assertEquals(acc.name.getOrElse("<missing>"), "posAcc")
    assertEquals(acc.bufferView.get, 0)
    assertEquals(acc.componentType, 5126)
    assertEquals(acc.count, 24)
    assertEquals(acc.`type`.getOrElse("<missing>"), "VEC3")
    val min = acc.min.get
    assertEqualsFloat(min(0), -1.0f, 0.0001f)
    val max = acc.max.get
    assertEqualsFloat(max(2), 1.0f, 0.0001f)
    val sparse = acc.sparse.get
    assertEquals(sparse.count, 3)
    val si = sparse.indices.get
    assertEquals(si.bufferView, 1)
    assertEquals(si.byteOffset, 4)
    assertEquals(si.componentType, 5123)
    val sv = sparse.values.get
    assertEquals(sv.bufferView, 1)
    assertEquals(sv.byteOffset, 8)
  }

  // ── materials + PBR + texture infos ─────────────────────────────────

  private val materialsCorpus =
    """{
      |  "asset": {"version": "2.0"},
      |  "materials": [
      |    {"name": "mat0", "alphaMode": "MASK", "alphaCutoff": 0.4, "doubleSided": true,
      |     "emissiveFactor": [0.1, 0.2, 0.3],
      |     "pbrMetallicRoughness": {
      |       "baseColorFactor": [0.8, 0.7, 0.6, 1.0], "metallicFactor": 0.25, "roughnessFactor": 0.85,
      |       "baseColorTexture": {"index": 2, "texCoord": 1},
      |       "metallicRoughnessTexture": {"index": 3, "texCoord": 0}},
      |     "normalTexture": {"index": 4, "texCoord": 0, "scale": 1.5},
      |     "occlusionTexture": {"index": 5, "texCoord": 0, "strength": 0.9},
      |     "emissiveTexture": {"index": 6, "texCoord": 0}}
      |  ]
      |}""".stripMargin

  test("ISS-510 corpus: material + PBR + texture info fields survive round-trip") {
    val gltf = roundTrip(materialsCorpus)
    val mat  = gltf.materials.get(0)
    assertEquals(mat.name.getOrElse("<missing>"), "mat0")
    assertEquals(mat.alphaMode.getOrElse("<missing>"), "MASK")
    assertEqualsFloat(mat.alphaCutoff.get, 0.4f, 0.0001f)
    assert(mat.doubleSided.get, "doubleSided must survive")
    val ef = mat.emissiveFactor.get
    assertEqualsFloat(ef(1), 0.2f, 0.0001f)
    val pbr = mat.pbrMetallicRoughness.get
    val bcf = pbr.baseColorFactor.get
    assertEqualsFloat(bcf(0), 0.8f, 0.0001f)
    assertEqualsFloat(bcf(3), 1.0f, 0.0001f)
    assertEqualsFloat(pbr.metallicFactor, 0.25f, 0.0001f)
    assertEqualsFloat(pbr.roughnessFactor, 0.85f, 0.0001f)
    assertEquals(pbr.baseColorTexture.get.index.get, 2)
    assertEquals(pbr.baseColorTexture.get.texCoord, 1)
    assertEquals(pbr.metallicRoughnessTexture.get.index.get, 3)
    assertEquals(mat.normalTexture.get.index.get, 4)
    assertEqualsFloat(mat.normalTexture.get.scale, 1.5f, 0.0001f)
    assertEquals(mat.occlusionTexture.get.index.get, 5)
    assertEqualsFloat(mat.occlusionTexture.get.strength, 0.9f, 0.0001f)
    assertEquals(mat.emissiveTexture.get.index.get, 6)
  }

  // ── animations ──────────────────────────────────────────────────────

  private val animationsCorpus =
    """{
      |  "asset": {"version": "2.0"},
      |  "animations": [
      |    {"name": "anim0",
      |     "channels": [{"sampler": 0, "target": {"node": 2, "path": "translation"}}],
      |     "samplers": [{"input": 7, "output": 8, "interpolation": "LINEAR"}]}
      |  ]
      |}""".stripMargin

  test("ISS-510 corpus: animation channel/sampler/target fields survive round-trip") {
    val gltf = roundTrip(animationsCorpus)
    val anim = gltf.animations.get(0)
    assertEquals(anim.name.getOrElse("<missing>"), "anim0")
    val ch = anim.channels.get(0)
    assertEquals(ch.sampler.get, 0)
    val tgt = ch.target.get
    assertEquals(tgt.node.get, 2)
    assertEquals(tgt.path.getOrElse("<missing>"), "translation")
    val smp = anim.samplers.get(0)
    assertEquals(smp.input.get, 7)
    assertEquals(smp.output.get, 8)
    assertEquals(smp.interpolation.getOrElse("<missing>"), "LINEAR")
  }

  // ── meshes + primitives + morph targets ─────────────────────────────

  private val meshesCorpus =
    """{
      |  "asset": {"version": "2.0"},
      |  "meshes": [
      |    {"name": "mesh0", "weights": [0.5, 0.25],
      |     "primitives": [
      |       {"mode": 4, "material": 0, "indices": 9,
      |        "attributes": {"POSITION": 10, "NORMAL": 11},
      |        "targets": [{"POSITION": 12, "NORMAL": 13}, {"POSITION": 14}]}
      |     ]}
      |  ]
      |}""".stripMargin

  test("ISS-510 corpus: mesh primitive + morph target fields survive round-trip") {
    val gltf = roundTrip(meshesCorpus)
    val mesh = gltf.meshes.get(0)
    assertEquals(mesh.name.getOrElse("<missing>"), "mesh0")
    val w = mesh.weights.get
    assertEqualsFloat(w(0), 0.5f, 0.0001f)
    assertEqualsFloat(w(1), 0.25f, 0.0001f)
    val prim = mesh.primitives.get(0)
    assertEquals(prim.mode.get, 4)
    assertEquals(prim.material.get, 0)
    assertEquals(prim.indices.get, 9)
    val attrs = prim.attributes.get
    assertEquals(attrs("POSITION"), 10)
    assertEquals(attrs("NORMAL"), 11)
    val targets = prim.targets.get
    assertEquals(targets.size, 2)
    assertEquals(targets(0)("POSITION"), 12)
    assertEquals(targets(0)("NORMAL"), 13)
    assertEquals(targets(1)("POSITION"), 14)
  }
}
