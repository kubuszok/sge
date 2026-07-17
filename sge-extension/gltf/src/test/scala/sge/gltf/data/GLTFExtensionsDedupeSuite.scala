/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Coverage suite for ISS-630 — the KHR extension re-encode path has TWO
 * independent dedupe defenses against emitting one extension name twice:
 *   (1) GLTFExtensions.get drops the raw entry once it lazily parses it into a
 *       typed object (GLTFExtensions.scala:54 `rawValues.remove(ext)`), and
 *   (2) gltfExtensionsCodec.encodeValue skips any raw entry whose key was already
 *       emitted as a typed entry (GLTFCodecs.scala:370 typedKeys guard).
 *
 * With only the get()-promotion path exercised, removing EITHER defense is an
 * equivalent mutant (guard (2) masks a missing (1) at encode time, and vice
 * versa). These two tests target each defense at its OWN observation point so
 * each becomes independently mutation-killable:
 *   - Test A pins the encoded output to one occurrence of the key when a typed
 *     entry and a same-named raw entry both exist (kills the guard-(2) mutant).
 *   - Test B pins rawEntries to be EMPTY of the key right after get() promotes it
 *     (kills the remove-(1) mutant, which the encode count alone cannot catch).
 *
 * The current code is correct, so both tests pass today; they close the mutation
 * gap the issue describes rather than reproducing a live bug.
 *
 * Platform scope: shared (src/test/scala) — pure codec / data model.
 */
package sge
package gltf
package data

import sge.gltf.data.GLTFCodecs.given
import sge.gltf.data.extensions.{ KHRLightsPunctual, KHRMaterialsUnlit }
import sge.gltf.loaders.gltf.GLTFJsonParser
import sge.utils.{ readFromString, writeToString }
import sge.utils.given
import lowlevel.Nullable

class GLTFExtensionsDedupeSuite extends munit.FunSuite {

  private def occurrences(haystack: String, needle: String): Int = {
    var count = 0
    var idx   = haystack.indexOf(needle)
    while (idx >= 0) {
      count += 1
      idx = haystack.indexOf(needle, idx + needle.length)
    }
    count
  }

  test("ISS-630 (guard 2): a typed entry and a same-key raw entry encode the key exactly once (GLTFCodecs.scala:370)") {
    val ext = new GLTFExtensions()
    // Typed entry under KHR_materials_unlit ...
    ext.set(KHRMaterialsUnlit.EXT, new KHRMaterialsUnlit())
    // ... plus a stray raw entry under the SAME key (a direct setRaw can leave both maps populated).
    ext.setRaw(KHRMaterialsUnlit.EXT, readFromString[sge.utils.Json]("""{"stray":true}"""))

    val encoded = writeToString(ext)
    assertEquals(
      occurrences(encoded, "\"" + KHRMaterialsUnlit.EXT + "\""),
      1,
      s"the typed entry must win and the raw entry must be skipped (no duplicate object key): $encoded"
    )
  }

  test("ISS-630 (defense 1): GLTFExtensions.get drops the raw entry after promoting it to typed (GLTFExtensions.scala:54)") {
    val gltf =
      GLTFJsonParser.parse(
        """{"asset":{"version":"2.0"},"extensions":{"KHR_lights_punctual":{"lights":[{"type":"point","color":[1,1,1],"intensity":1}]}}}"""
      )
    val exts = gltf.extensions.get

    // Before promotion the extension lives only in the raw map.
    assert(exts.rawEntries.exists(_._1 == KHRLightsPunctual.EXT), "extension must start as a raw entry")
    assert(!exts.typedEntries.exists(_._1 == KHRLightsPunctual.EXT), "extension must not be typed before get()")

    // get() lazily parses it into GLTFLights and must MOVE it out of the raw map.
    val parsed = exts.get(classOf[KHRLightsPunctual.GLTFLights], KHRLightsPunctual.EXT)
    assert(Nullable.isDefined(parsed), "get() must parse the raw KHR_lights_punctual into GLTFLights")

    assert(
      !exts.rawEntries.exists(_._1 == KHRLightsPunctual.EXT),
      "promotion must remove the raw entry (else it would be emitted twice)"
    )
    assert(exts.typedEntries.exists(_._1 == KHRLightsPunctual.EXT), "promoted extension must now be a typed entry")
  }
}
