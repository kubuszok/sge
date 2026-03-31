/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/utils/LightUtils.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package utils

import sge.graphics.g3d.Environment
import sge.graphics.g3d.attributes.{ DirectionalLightsAttribute, PointLightsAttribute, SpotLightsAttribute }
import sge.graphics.g3d.environment.{ BaseLight, DirectionalLight, PointLight, SpotLight }

object LightUtils {

  class LightsInfo {
    var dirLights:   Int = 0
    var pointLights: Int = 0
    var spotLights:  Int = 0
    var miscLights:  Int = 0

    def reset(): Unit = {
      dirLights = 0
      pointLights = 0
      spotLights = 0
      miscLights = 0
    }
  }

  def getLightsInfo(info: LightsInfo, environment: Environment): LightsInfo = {
    info.reset()
    environment.getAs[DirectionalLightsAttribute](DirectionalLightsAttribute.Type).foreach { dla =>
      info.dirLights = dla.lights.size
    }
    environment.getAs[PointLightsAttribute](PointLightsAttribute.Type).foreach { pla =>
      info.pointLights = pla.lights.size
    }
    environment.getAs[SpotLightsAttribute](SpotLightsAttribute.Type).foreach { sla =>
      info.spotLights = sla.lights.size
    }
    info
  }

  def getLightsInfo(info: LightsInfo, lights: Iterable[BaseLight[?]]): LightsInfo = {
    info.reset()
    lights.foreach {
      case _: DirectionalLight => info.dirLights += 1
      case _: PointLight       => info.pointLights += 1
      case _: SpotLight        => info.spotLights += 1
      case _                   => info.miscLights += 1
    }
    info
  }
}
