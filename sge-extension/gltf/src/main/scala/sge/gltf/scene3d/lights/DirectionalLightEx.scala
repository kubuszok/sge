/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/lights/DirectionalLightEx.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package lights

import sge.graphics.Color
import sge.graphics.g3d.environment.DirectionalLight
import sge.math.Vector3
import sge.utils.Nullable

class DirectionalLightEx extends DirectionalLight {

  /** base color clamped */
  val baseColor: Color = Color(Color.WHITE)

  /** light intensity in lux (lm/m2) */
  var intensity: Float = 1f

  override def set(copyFrom: DirectionalLight): DirectionalLight =
    copyFrom match {
      case ex: DirectionalLightEx =>
        set(ex.baseColor, copyFrom.direction, ex.intensity)
      case _ =>
        set(copyFrom.color, copyFrom.direction, 1f)
    }

  def set(baseColor: Color, direction: Vector3, intensity: Float): DirectionalLightEx = {
    this.intensity = intensity
    this.baseColor.set(baseColor)
    this.direction.set(direction)
    updateColor()
    this
  }

  override def set(color: Nullable[Color], direction: Nullable[Vector3]): DirectionalLightEx = {
    color.foreach(c => this.baseColor.set(c))
    direction.foreach(d => this.direction.set(d).nor())
    this
  }

  override def set(r: Float, g: Float, b: Float, direction: Nullable[Vector3]): DirectionalLightEx = {
    this.baseColor.set(r, g, b, 1f)
    direction.foreach(d => this.direction.set(d).nor())
    this
  }

  override def set(color: Nullable[Color], dirX: Float, dirY: Float, dirZ: Float): DirectionalLightEx = {
    color.foreach(c => this.baseColor.set(c))
    this.direction.set(dirX, dirY, dirZ).nor()
    this
  }

  override def set(r: Float, g: Float, b: Float, dirX: Float, dirY: Float, dirZ: Float): DirectionalLightEx = {
    this.baseColor.set(r, g, b, 1f).clamp()
    this.direction.set(dirX, dirY, dirZ).nor()
    this
  }

  def updateColor(): Unit = {
    this.color.r = baseColor.r * intensity
    this.color.g = baseColor.g * intensity
    this.color.b = baseColor.b * intensity
  }

  override def equals(other: Any): Boolean = other match {
    case ex: DirectionalLightEx =>
      (ex eq this) || (baseColor.equals(ex.baseColor) && java.lang.Float.compare(intensity, ex.intensity) == 0 && direction.equals(ex.direction))
    case _ => false
  }
}
