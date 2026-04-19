/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/environment/DirectionalLight.java
 * Original authors: (see AUTHORS file)
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Audit: pass (2026-03-03)
 *   - All 9 methods ported: setDirection x2, set x5, equals x2
 *   - Null checks → Nullable.foreach
 *   - equals(DirectionalLight) uses Nullable.fold for null safety
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 70
 * Covenant-baseline-methods: DirectionalLight,direction,equals,set,setDirection
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/environment/DirectionalLight.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package environment

import scala.language.implicitConversions

import sge.math.Vector3
import sge.utils.Nullable

class DirectionalLight extends BaseLight[DirectionalLight] {
  val direction: Vector3 = Vector3()

  def setDirection(directionX: Float, directionY: Float, directionZ: Float): DirectionalLight = {
    this.direction.set(directionX, directionY, directionZ)
    this
  }

  def setDirection(direction: Vector3): DirectionalLight = {
    this.direction.set(direction)
    this
  }

  def set(copyFrom: DirectionalLight): DirectionalLight =
    set(copyFrom.color, copyFrom.direction)

  def set(color: Nullable[Color], direction: Nullable[Vector3]): DirectionalLight = {
    color.foreach(this.color.set(_))
    direction.foreach(d => this.direction.set(d).nor())
    this
  }

  def set(r: Float, g: Float, b: Float, direction: Nullable[Vector3]): DirectionalLight = {
    this.color.set(r, g, b, 1f)
    direction.foreach(d => this.direction.set(d).nor())
    this
  }

  def set(color: Nullable[Color], dirX: Float, dirY: Float, dirZ: Float): DirectionalLight = {
    color.foreach(this.color.set(_))
    this.direction.set(dirX, dirY, dirZ).nor()
    this
  }

  def set(r: Float, g: Float, b: Float, dirX: Float, dirY: Float, dirZ: Float): DirectionalLight = {
    this.color.set(r, g, b, 1f)
    this.direction.set(dirX, dirY, dirZ).nor()
    this
  }

  override def equals(arg0: Any): Boolean = arg0 match {
    case other: DirectionalLight =>
      (other eq this) || (color.equals(other.color) && direction.equals(other.direction))
    case _ => false
  }
}
