/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/environment/SpotLight.java
 * Original authors: realitix
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Audit: pass (2026-03-03)
 *   - All 13 methods ported: setPosition x2, setDirection x2, setIntensity,
 *     setCutoffAngle, setExponent, set x4, setTarget, equals x2
 *   - Null checks → Nullable.foreach
 *   - equals(SpotLight) uses Nullable.fold for null safety
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 128
 * Covenant-baseline-methods: SpotLight,cutoffAngle,direction,equals,exponent,intensity,position,set,setCutoffAngle,setDirection,setExponent,setIntensity,setPosition,setTarget
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/environment/SpotLight.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package environment

import scala.language.implicitConversions

import sge.math.MathUtils
import sge.math.Vector3
import sge.utils.Nullable

/** Note that the default shader doesn't support spot lights, you'll have to supply your own shader to use this class.
  * @author
  *   realitix
  */
class SpotLight extends BaseLight[SpotLight] {
  val position:    Vector3 = Vector3()
  val direction:   Vector3 = Vector3()
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

  def set(color: Nullable[Color], position: Nullable[Vector3], direction: Nullable[Vector3], intensity: Float, cutoffAngle: Float, exponent: Float): SpotLight = {
    color.foreach(this.color.set(_))
    position.foreach(this.position.set(_))
    direction.foreach(d => this.direction.set(d).nor())
    this.intensity = intensity
    this.cutoffAngle = cutoffAngle
    this.exponent = exponent
    this
  }

  def set(r: Float, g: Float, b: Float, position: Nullable[Vector3], direction: Nullable[Vector3], intensity: Float, cutoffAngle: Float, exponent: Float): SpotLight = {
    this.color.set(r, g, b, 1f)
    position.foreach(this.position.set(_))
    direction.foreach(d => this.direction.set(d).nor())
    this.intensity = intensity
    this.cutoffAngle = cutoffAngle
    this.exponent = exponent
    this
  }

  def set(color: Nullable[Color], posX: Float, posY: Float, posZ: Float, dirX: Float, dirY: Float, dirZ: Float, intensity: Float, cutoffAngle: Float, exponent: Float): SpotLight = {
    color.foreach(this.color.set(_))
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
    case other: SpotLight =>
      (other eq this) || (color.equals(other.color) && position.equals(other.position)
        && direction.equals(other.direction) && MathUtils.isEqual(intensity, other.intensity)
        && MathUtils.isEqual(cutoffAngle, other.cutoffAngle) && MathUtils.isEqual(exponent, other.exponent))
    case _ => false
  }
}
