/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: partial-port
 * Covenant-source-reference: gdx-gltf/gltf/src/net/mgsx/gltf/loaders/blender/BlenderShapeKeys.java
 * Covenant-verified: 2026-04-08
 *
 * Partial-port debt:
 *   - targetNames parsing from glMesh.extras requires GLTF JSON codecs (not yet ported).
 */
package sge
package gltf
package loaders
package blender

import sge.gltf.data.geometry.GLTFMesh
import sge.utils.{ DynamicArray, Nullable }

/** Blender stores shape key names in mesh extras.
  *
  * Shape key names are stored in mesh extras as a "targetNames" array. The original Java reads them via LibGDX's JsonValue API; in SGE we use the JSON AST stored in GLTFExtras.
  */
object BlenderShapeKeys {

  /** Blender store shape key names in mesh extras.
    * {{{
    *  "meshes" : [
    *       {
    *         "name" : "Plane",
    *         "extras" : {
    *             "targetNames" : [
    *                 "Water",
    *                 "Mountains"
    *             ]
    *         },
    *         "primitives" : ...,
    *         "weights" : [0.6, 0.3]
    *       }
    *     ]
    * }}}
    */
  def parse(glMesh: GLTFMesh): Nullable[DynamicArray[String]] =
    // targetNames parsing requires GLTF JSON extras; returns empty when extras absent
    glMesh.extras.fold(Nullable.empty[DynamicArray[String]])(_ => Nullable.empty[DynamicArray[String]])
}
