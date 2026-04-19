/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 30
 * Covenant-baseline-methods: GLTFJsonParser,parse
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package loaders
package gltf

import sge.gltf.data.GLTF
import sge.gltf.data.GLTFCodecs.given
import sge.gltf.loaders.exceptions.GLTFRuntimeException
import sge.utils.readFromString

/** Parses GLTF JSON text into the GLTF data model.
  *
  * Uses jsoniter-scala with Kindlings-derived codecs (defined in [[sge.gltf.data.GLTFCodecs]]) for compile-time codec derivation. The original LibGDX version used reflection-based
  * `Json.fromJson(GLTF.class, json)`.
  */
object GLTFJsonParser {

  def parse(jsonString: String): GLTF =
    try readFromString[GLTF](jsonString)
    catch {
      case e: Exception =>
        throw new GLTFRuntimeException("Failed to parse GLTF JSON: " + e.getMessage, e)
    }
}
