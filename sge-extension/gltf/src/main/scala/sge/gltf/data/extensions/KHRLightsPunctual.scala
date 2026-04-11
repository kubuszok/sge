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
import sge.graphics.g3d.environment.BaseLight
import sge.gltf.loaders.exceptions.GLTFIllegalException
import sge.gltf.loaders.shared.GLTFTypes
import sge.gltf.scene3d.lights.{ DirectionalLightEx, PointLightEx, SpotLightEx }
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

  def map(light: GLTFLight): BaseLight[?] = {
    val lightType = light.`type`.getOrElse(throw new GLTFIllegalException("light type is null"))
    if (GLTFLight.TYPE_DIRECTIONAL == lightType) {
      val dl = new DirectionalLightEx()
      dl.baseColor.set(GLTFTypes.mapColor(Nullable(light.color), Color(Color.WHITE)))
      dl.intensity = light.intensity
      dl
    } else if (GLTFLight.TYPE_POINT == lightType) {
      val pl = new PointLightEx()
      pl.color.set(GLTFTypes.mapColor(Nullable(light.color), Color(Color.WHITE)))
      // Blender exported intensity is the raw value in Watts
      // GLTF spec. states it's in Candela which is lumens per square radian (lm/sr).
      // adjustement is made empirically here (comparing with Blender rendering)
      // TODO find if it's a GLTF Blender exporter issue and find the right conversion.
      pl.intensity = light.intensity / 10f
      pl.range = light.range
      pl
    } else if (GLTFLight.TYPE_SPOT == lightType) {
      val sl = new SpotLightEx()
      if (light.spot.isEmpty) throw new GLTFIllegalException("spot property required for spot light type")
      sl.color.set(GLTFTypes.mapColor(Nullable(light.color), Color(Color.WHITE)))
      // same hack as point lights (see point light above)
      sl.intensity = light.intensity / 10f
      sl.range = light.range
      sl.setConeRad(light.spot.get.outerConeAngle, light.spot.get.innerConeAngle)
      sl
    } else {
      throw new GLTFIllegalException("unsupported light type " + lightType)
    }
  }
}
