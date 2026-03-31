/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package data
package geometry

import scala.collection.mutable.HashMap

/** A morph target is a map of attribute names to accessor indices. In the original Java, this extends ObjectMap and implements Json.Serializable; in SGE we use a simple HashMap since SGE uses
  * jsoniter-scala, not LibGDX Json.
  */
class GLTFMorphTarget extends HashMap[String, Int]
