/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite for ISS-719 (review-fable, major).
 *
 * GLTFCodecs.khrUnlitCodec (sge-extension/gltf/.../GLTFCodecs.scala:1211-1224)
 * decodes a KHR_materials_unlit object with:
 *
 *   if (in.isNextToken('{')) { while (!in.isNextToken('}')) { in.readKeyAsString(); in.skip() } }
 *
 * The `in.isNextToken('}')` in the loop guard CONSUMES the next token to test
 * whether it is the object's closing brace. For a NON-EMPTY unlit object the
 * consumed token is the opening quote of the next key, but the codec then calls
 * `in.readKeyAsString()` WITHOUT first rolling that token back. Every sibling
 * decoder in the same file rolls the probed token back before re-reading it:
 *   - readFields (GLTFCodecs.scala:181-193) -> in.rollbackToken() at :183
 *   - readNullableObj (GLTFCodecs.scala:126-128) -> in.rollbackToken() at :128
 *   - nullableCodec (GLTFCodecs.scala:47-54) -> in.rollbackToken() at :52
 *   - readNullableFloatArray / readNullableStringIntMap / ... (:84, :152, ...)
 * khrUnlitCodec is the ONE decoder missing the `in.rollbackToken()` call, so a
 * non-empty KHR_materials_unlit corrupts the jsoniter reader: the following
 * `readKeyAsString()` finds no opening quote and the parse throws / mis-reads,
 * which also destroys the parse of any sibling extension after it.
 *
 * Both-side vs original: the original gdx-gltf never streams tokens for this —
 * net/mgsx/gltf/data/GLTFExtensions.java:32 does `json.readValue(type,
 * value.get(ext))` over a fully-materialised libgdx JsonValue DOM, so the unlit
 * object's contents are irrelevant and the sibling extensions always parse. The
 * SGE port reimplements the reader as a streaming jsoniter codec, and this codec
 * dropped the mandatory rollback that the DOM approach never needed.
 *
 * This suite feeds a glTF material whose `extensions` hold a NON-EMPTY
 * KHR_materials_unlit object FOLLOWED by a sibling KHR_materials_emissive_strength
 * extension, and asserts the whole material parses: the unlit extension is
 * present AND the following extension is read correctly. On current code the
 * missing rollback corrupts the reader inside khrUnlitCodec and the parse fails
 * before (or while) reading the sibling. A single inner key is used inside the
 * unlit object so the minimal `in.rollbackToken()` fix makes it pass.
 *
 * Platform scope: shared (src/test/scala) — pure jsoniter-scala codecs and the
 * GLTF data model, no GL or filesystem dependency, runs headless on JVM/JS/Native.
 */
package sge
package gltf
package data

import sge.gltf.data.extensions.{ KHRMaterialsEmissiveStrength, KHRMaterialsUnlit }
import sge.gltf.loaders.gltf.GLTFJsonParser
import lowlevel.Nullable

class GLTFUnlitCodecIss719Suite extends munit.FunSuite {

  // A glTF material whose extensions contain a NON-EMPTY KHR_materials_unlit
  // object followed by another extension. The non-empty unlit object triggers
  // the loop guard `while (!in.isNextToken('}'))` in khrUnlitCodec
  // (GLTFCodecs.scala:1214) to consume the "enabled" key's opening quote; the
  // missing `in.rollbackToken()` then corrupts the following `readKeyAsString()`.
  private val unlitCorpus =
    """{
      |  "asset": {"version": "2.0"},
      |  "materials": [
      |    {
      |      "name": "unlitMat",
      |      "extensions": {
      |        "KHR_materials_unlit": {"enabled": true},
      |        "KHR_materials_emissive_strength": {"emissiveStrength": 2.5}
      |      }
      |    }
      |  ]
      |}""".stripMargin

  test("ISS-719: non-empty KHR_materials_unlit object parses without corrupting the material") {
    // On current code khrUnlitCodec (GLTFCodecs.scala:1211-1224) is missing the
    // in.rollbackToken() that readFields (:183) / readNullableObj (:128) all have,
    // so this parse throws (GLTFRuntimeException wrapping a jsoniter reader error)
    // on the token the unlit codec consumed but never rolled back.
    val gltf = GLTFJsonParser.parse(unlitCorpus)
    assert(Nullable.isDefined(gltf.materials), "materials must be parsed")
    assertEquals(gltf.materials.get.size, 1)
    val mat = gltf.materials.get(0)
    assertEquals(mat.name.getOrElse("<missing>"), "unlitMat")
    assert(Nullable.isDefined(mat.extensions), "material extensions must be parsed")
  }

  test("ISS-719: non-empty KHR_materials_unlit is present as a typed extension") {
    val gltf  = GLTFJsonParser.parse(unlitCorpus)
    val exts  = gltf.materials.get(0).extensions.get
    val unlit = exts.get(classOf[KHRMaterialsUnlit], KHRMaterialsUnlit.EXT)
    assert(Nullable.isDefined(unlit), "KHR_materials_unlit extension must be present after parse")
  }

  test("ISS-719: extension after a non-empty KHR_materials_unlit is parsed correctly (rollback keeps the reader positioned)") {
    // This is the load-bearing assertion: the sibling extension sits AFTER the
    // non-empty unlit object, so it only parses if khrUnlitCodec leaves the
    // reader correctly positioned (i.e. rolls back the token it probed). The
    // missing rollback corrupts the stream and the following extension is lost.
    val gltf     = GLTFJsonParser.parse(unlitCorpus)
    val exts     = gltf.materials.get(0).extensions.get
    val emissive = exts.get(classOf[KHRMaterialsEmissiveStrength], KHRMaterialsEmissiveStrength.EXT)
    assert(Nullable.isDefined(emissive), "extension following the non-empty KHR_materials_unlit must be parsed")
    assertEqualsFloat(emissive.get.emissiveStrength, 2.5f, 0.0001f)
  }
}
