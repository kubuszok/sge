/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/lights/SpotLightEx.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Implementation of SpotLight with extra features.
 * WARNING: SpotLight fields cutoffAngle and exponent shouldn't be set manually.
 * Use setConeRad or setConeDeg instead.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 87
 * Covenant-baseline-methods: SpotLightEx,cosInnerAngle,cosOuterAngle,lightAngleOffset,lightAngleScale,range,set,setConeDeg,setConeRad,setDeg,setRad
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package scene3d
package lights

import sge.graphics.Color
import sge.graphics.g3d.environment.SpotLight
import sge.math.{ MathUtils, Vector3 }
import sge.utils.Nullable

class SpotLightEx extends SpotLight {

  /** Optional range in meters. */
  var range: Nullable[Float] = Nullable.empty

  override def set(copyFrom: SpotLight): SpotLight =
    copyFrom match {
      case ex: SpotLightEx =>
        super.set(copyFrom.color, copyFrom.position, copyFrom.direction, copyFrom.intensity, copyFrom.cutoffAngle, copyFrom.exponent)
        this.range = ex.range
        this
      case _ =>
        set(copyFrom.color, copyFrom.position, copyFrom.direction, copyFrom.intensity, copyFrom.cutoffAngle, copyFrom.exponent)
    }

  def setRad(color: Color, position: Vector3, direction: Vector3, intensity: Float, outerConeAngleRad: Float, innerConeAngleRad: Float, range: Nullable[Float]): SpotLightEx = {
    if (color != null) this.color.set(color) // @nowarn — inherited API allows null
    if (position != null) this.position.set(position) // @nowarn — inherited API allows null
    if (direction != null) this.direction.set(direction).nor() // @nowarn — inherited API allows null
    this.intensity = intensity
    setConeRad(outerConeAngleRad, innerConeAngleRad)
    this.range = range
    this
  }

  def setDeg(color: Color, position: Vector3, direction: Vector3, intensity: Float, outerConeAngleDeg: Float, innerConeAngleDeg: Float, range: Nullable[Float]): SpotLightEx =
    setRad(
      color,
      position,
      direction,
      intensity,
      outerConeAngleDeg * MathUtils.degreesToRadians,
      innerConeAngleDeg * MathUtils.degreesToRadians,
      range
    )

  def setConeRad(outerConeAngleRad: Float, innerConeAngleRad: Float): SpotLightEx = {
    // from https://github.com/KhronosGroup/glTF/blob/master/extensions/2.0/Khronos/KHR_lights_punctual/README.md
    val cosOuterAngle    = Math.cos(outerConeAngleRad).toFloat
    val cosInnerAngle    = Math.cos(innerConeAngleRad).toFloat
    val lightAngleScale  = 1.0f / Math.max(0.001f, cosInnerAngle - cosOuterAngle)
    val lightAngleOffset = -cosOuterAngle * lightAngleScale

    // We hack libgdx cutoffAngle and exponent variables to store cached scale/offset values.
    // It's not an issue since libgdx default shader doesn't implement spot lights.
    cutoffAngle = lightAngleOffset
    exponent = lightAngleScale
    this
  }

  def setConeDeg(outerConeAngleDeg: Float, innerConeAngleDeg: Float): SpotLightEx =
    setConeRad(outerConeAngleDeg * MathUtils.degreesToRadians, innerConeAngleDeg * MathUtils.degreesToRadians)
}
