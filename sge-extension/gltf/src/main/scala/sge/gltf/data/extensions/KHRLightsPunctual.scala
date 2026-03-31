/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package data
package extensions

import scala.collection.mutable.ArrayBuffer
import sge.graphics.Color
import sge.gltf.loaders.exceptions.GLTFIllegalException
import sge.gltf.loaders.shared.GLTFTypes
import sge.utils.Nullable

/** [[sge.gltf.data.scene.GLTFNode]] and [[sge.gltf.data.GLTF]] (root) extension. See https://github.com/KhronosGroup/glTF/tree/master/extensions/2.0/Khronos/KHR_lights_punctual
  */
object KHRLightsPunctual {

  val EXT: String = "KHR_lights_punctual"

  class GLTFSpotLight {
    var innerConeAngle: Float = 0f
    var outerConeAngle: Float = scala.math.Pi.toFloat / 4f
  }

  class GLTFLight extends GLTFObject {
    var name:  String       = ""
    var color: Array[Float] = Array(1f, 1f, 1f)

    /** in Candela for point/spot lights : Ev(lx) = Iv(cd) / (d(m))2 in Lux for directional lights : Ev(lx)
      */
    var intensity: Float            = 1f
    var `type`:    Nullable[String] = Nullable.empty

    /** Hint defining a distance cutoff at which the light's intensity may be considered to have reached zero. When null, range is assumed to be infinite.
      */
    var range: Nullable[Float] = Nullable.empty

    var spot: Nullable[GLTFSpotLight] = Nullable.empty
  }

  object GLTFLight {
    val TYPE_DIRECTIONAL: String = "directional"
    val TYPE_POINT:       String = "point"
    val TYPE_SPOT:        String = "spot"
  }

  class GLTFLights {
    var lights: Nullable[ArrayBuffer[GLTFLight]] = Nullable.empty
  }

  class GLTFLightNode {
    var light: Nullable[Int] = Nullable.empty
  }
}
