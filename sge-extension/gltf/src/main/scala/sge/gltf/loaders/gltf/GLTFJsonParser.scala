/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package loaders
package gltf

import sge.gltf.data.GLTF
import sge.gltf.loaders.exceptions.GLTFRuntimeException

/** Parses GLTF JSON text into the GLTF data model.
  *
  * TODO: Implement full JSON-to-GLTF deserialization. The LibGDX version used reflection-based `Json.fromJson(GLTF.class, json)`. SGE uses jsoniter-scala which requires compile-time codec derivation.
  * Since the GLTF data model uses mutable `var` fields and `Nullable`, a custom codec or manual parser is needed. This is a placeholder that will be completed when the full scene3d subsystem is
  * ported.
  */
object GLTFJsonParser {

  def parse(jsonString: String): GLTF =
    // TODO: implement JSON parsing for GLTF data model
    throw new GLTFRuntimeException("GLTF JSON parsing not yet implemented — requires custom jsoniter-scala codecs for the mutable GLTF data model")
}
