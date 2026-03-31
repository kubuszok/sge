/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/lights/PointLightEx.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package lights

import sge.graphics.Color
import sge.graphics.g3d.environment.PointLight
import sge.math.Vector3
import sge.utils.Nullable

class PointLightEx extends PointLight {

  /** Optional range in meters. */
  var range: Nullable[Float] = Nullable.empty

  override def set(copyFrom: PointLight): PointLight = {
    copyFrom match {
      case ex: PointLightEx =>
        set(copyFrom.color, copyFrom.position, copyFrom.intensity, ex.range)
      case _ =>
        set(copyFrom.color, copyFrom.position, copyFrom.intensity)
    }
  }

  def set(color: Color, position: Vector3, intensity: Float, range: Nullable[Float]): PointLightEx = {
    super.set(color, position, intensity)
    this.range = range
    this
  }
}
