/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/environment/PointLight.java
 * Original authors: (see AUTHORS file)
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package environment

import sge.math.Vector3

class PointLight extends BaseLight[PointLight] {
  val position:  Vector3 = new Vector3()
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

  def set(color: Color, position: Vector3, intensity: Float): PointLight = {
    if (color != null) this.color.set(color)
    if (position != null) this.position.set(position)
    this.intensity = intensity
    this
  }

  def set(r: Float, g: Float, b: Float, position: Vector3, intensity: Float): PointLight = {
    this.color.set(r, g, b, 1f)
    if (position != null) this.position.set(position)
    this.intensity = intensity
    this
  }

  def set(color: Color, x: Float, y: Float, z: Float, intensity: Float): PointLight = {
    if (color != null) this.color.set(color)
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
    case other: PointLight => equals(other)
    case _ => false
  }

  def equals(other: PointLight): Boolean =
    (other != null) && ((other eq this) || (color.equals(other.color) && position.equals(other.position) && intensity == other.intensity))
}
