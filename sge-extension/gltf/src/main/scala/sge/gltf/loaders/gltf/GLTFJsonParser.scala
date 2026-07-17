/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 47
 * Covenant-baseline-methods: GLTFJsonParser,parse
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-07-17
 */
package sge
package gltf
package loaders
package gltf

import com.github.plokhotnyuk.jsoniter_scala.core.{ ReaderConfig, readFromString }

import sge.gltf.data.GLTF
import sge.gltf.data.GLTFCodecs.given
import sge.gltf.loaders.exceptions.GLTFRuntimeException

/** Parses GLTF JSON text into the GLTF data model.
  *
  * Uses jsoniter-scala with Kindlings-derived codecs (defined in [[sge.gltf.data.GLTFCodecs]]) for compile-time codec derivation. The original LibGDX version used reflection-based
  * `Json.fromJson(GLTF.class, json)`.
  */
object GLTFJsonParser {

  /** Reader configuration for GLTF parsing.
    *
    * ISS-626: jsoniter-scala's default [[ReaderConfig]] caps a single JSON string token at maxCharBufSize = 4194304 chars (4 MiB). A spec-valid glTF may embed an entire binary buffer as one base64
    * `data:` URI string — routinely tens of MiB — so the default cap throws `"too long string exceeded maxCharBufSize"` on real-world documents. The original LibGDX JsonReader had no per-string cap.
    * Raise maxCharBufSize to 33554432 chars (32 MiB), matching jsoniter's own default maxBufSize (the byte-buffer ceiling), so a large embedded buffer URI is bounded only by the document's overall
    * size rather than by an arbitrary per-string limit.
    */
  private val readerConfig: ReaderConfig = ReaderConfig.withMaxCharBufSize(33554432)

  def parse(jsonString: String): GLTF =
    try readFromString[GLTF](jsonString, readerConfig)
    catch {
      case e: Exception =>
        throw new GLTFRuntimeException("Failed to parse GLTF JSON: " + e.getMessage, e)
    }
}
