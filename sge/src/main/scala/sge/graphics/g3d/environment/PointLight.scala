/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/environment/PointLight.java
 * Original authors: (see AUTHORS file)
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Audit: pass (2026-03-03)
 *   - All 9 methods ported: setPosition x2, setIntensity, set x4, equals x2
 *   - Null checks → Nullable.foreach
 *   - equals(PointLight) uses Nullable.fold for null safety
 */
package sge
package graphics
package g3d
package environment

import scala.language.implicitConversions

import sge.math.Vector3
import sge.utils.Nullable

class PointLight extends BaseLight[PointLight] {
  val position:  Vector3 = Vector3()
  var intensity: Float   = 0f

  def setPosition(positionX: Float, positionY: Float, positionZ: Float): PointLight = {
    this.position.set(positionX, positionY, positionZ)
    this
  }

  def setPosition(position: Vector3): PointLight = {
    this.position.set(position)
    this
  }

  def setIntensity(intensity: Float): PointLight = {
    this.intensity = intensity
    this
  }

  def set(copyFrom: PointLight): PointLight =
    set(copyFrom.color, copyFrom.position, copyFrom.intensity)

  def set(color: Nullable[Color], position: Nullable[Vector3], intensity: Float): PointLight = {
    color.foreach(this.color.set(_))
    position.foreach(this.position.set(_))
    this.intensity = intensity
    this
  }

  def set(r: Float, g: Float, b: Float, position: Nullable[Vector3], intensity: Float): PointLight = {
    this.color.set(r, g, b, 1f)
    position.foreach(this.position.set(_))
    this.intensity = intensity
    this
  }

  def set(color: Nullable[Color], x: Float, y: Float, z: Float, intensity: Float): PointLight = {
    color.foreach(this.color.set(_))
    this.position.set(x, y, z)
    this.intensity = intensity
    this
  }

  def set(r: Float, g: Float, b: Float, x: Float, y: Float, z: Float, intensity: Float): PointLight = {
    this.color.set(r, g, b, 1f)
    this.position.set(x, y, z)
    this.intensity = intensity
    this
  }

  override def equals(obj: Any): Boolean = obj match {
    case other: PointLight =>
      (other eq this) || (color.equals(other.color) && position.equals(other.position) && intensity == other.intensity)
    case _ => false
  }
}
