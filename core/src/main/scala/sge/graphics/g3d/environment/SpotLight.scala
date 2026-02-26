/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/environment/SpotLight.java
 * Original authors: realitix
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package environment

import sge.math.MathUtils
import sge.math.Vector3

/** Note that the default shader doesn't support spot lights, you'll have to supply your own shader to use this class.
  * @author
  *   realitix
  */
class SpotLight extends BaseLight[SpotLight] {
  val position:    Vector3 = new Vector3()
  val direction:   Vector3 = new Vector3()
  var intensity:   Float   = 0f
  var cutoffAngle: Float   = 0f
  var exponent:    Float   = 0f

  def setPosition(positionX: Float, positionY: Float, positionZ: Float): SpotLight = {
    this.position.set(positionX, positionY, positionZ)
    this
  }

  def setPosition(position: Vector3): SpotLight = {
    this.position.set(position)
    this
  }

  def setDirection(directionX: Float, directionY: Float, directionZ: Float): SpotLight = {
    this.direction.set(directionX, directionY, directionZ)
    this
  }

  def setDirection(direction: Vector3): SpotLight = {
    this.direction.set(direction)
    this
  }

  def setIntensity(intensity: Float): SpotLight = {
    this.intensity = intensity
    this
  }

  def setCutoffAngle(cutoffAngle: Float): SpotLight = {
    this.cutoffAngle = cutoffAngle
    this
  }

  def setExponent(exponent: Float): SpotLight = {
    this.exponent = exponent
    this
  }

  def set(copyFrom: SpotLight): SpotLight =
    set(copyFrom.color, copyFrom.position, copyFrom.direction, copyFrom.intensity, copyFrom.cutoffAngle, copyFrom.exponent)

  def set(color: Color, position: Vector3, direction: Vector3, intensity: Float, cutoffAngle: Float, exponent: Float): SpotLight = {
    if (color != null) this.color.set(color)
    if (position != null) this.position.set(position)
    if (direction != null) this.direction.set(direction).nor()
    this.intensity = intensity
    this.cutoffAngle = cutoffAngle
    this.exponent = exponent
    this
  }

  def set(r: Float, g: Float, b: Float, position: Vector3, direction: Vector3, intensity: Float, cutoffAngle: Float, exponent: Float): SpotLight = {
    this.color.set(r, g, b, 1f)
    if (position != null) this.position.set(position)
    if (direction != null) this.direction.set(direction).nor()
    this.intensity = intensity
    this.cutoffAngle = cutoffAngle
    this.exponent = exponent
    this
  }

  def set(color: Color, posX: Float, posY: Float, posZ: Float, dirX: Float, dirY: Float, dirZ: Float, intensity: Float, cutoffAngle: Float, exponent: Float): SpotLight = {
    if (color != null) this.color.set(color)
    this.position.set(posX, posY, posZ)
    this.direction.set(dirX, dirY, dirZ).nor()
    this.intensity = intensity
    this.cutoffAngle = cutoffAngle
    this.exponent = exponent
    this
  }

  def set(r: Float, g: Float, b: Float, posX: Float, posY: Float, posZ: Float, dirX: Float, dirY: Float, dirZ: Float, intensity: Float, cutoffAngle: Float, exponent: Float): SpotLight = {
    this.color.set(r, g, b, 1f)
    this.position.set(posX, posY, posZ)
    this.direction.set(dirX, dirY, dirZ).nor()
    this.intensity = intensity
    this.cutoffAngle = cutoffAngle
    this.exponent = exponent
    this
  }

  def setTarget(target: Vector3): SpotLight = {
    direction.set(target).sub(position).nor()
    this
  }

  override def equals(obj: Any): Boolean = obj match {
    case other: SpotLight => equals(other)
    case _ => false
  }

  def equals(other: SpotLight): Boolean =
    (other != null) && ((other eq this) || (color.equals(other.color) && position.equals(other.position)
      && direction.equals(other.direction) && MathUtils.isEqual(intensity, other.intensity)
      && MathUtils.isEqual(cutoffAngle, other.cutoffAngle) && MathUtils.isEqual(exponent, other.exponent)))
}
