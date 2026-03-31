/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package data

import sge.utils.{ Json, Nullable }

/** Holds extra properties from a GLTF JSON element. In the original Java, this implements Json.Serializable; in SGE we store the raw JSON AST instead since SGE uses jsoniter-scala, not LibGDX
  * reflection-based Json.
  */
class GLTFExtras {

  /** The raw JSON value of the "extras" field. */
  var value: Nullable[Json] = Nullable.empty
}
