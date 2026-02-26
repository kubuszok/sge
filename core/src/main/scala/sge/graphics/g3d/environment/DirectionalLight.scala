/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/environment/DirectionalLight.java
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

class DirectionalLight extends BaseLight[DirectionalLight] {
  val direction: Vector3 = new Vector3()

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

  def set(color: Color, direction: Vector3): DirectionalLight = {
    if (color != null) this.color.set(color)
    if (direction != null) this.direction.set(direction).nor()
    this
  }

  def set(r: Float, g: Float, b: Float, direction: Vector3): DirectionalLight = {
    this.color.set(r, g, b, 1f)
    if (direction != null) this.direction.set(direction).nor()
    this
  }

  def set(color: Color, dirX: Float, dirY: Float, dirZ: Float): DirectionalLight = {
    if (color != null) this.color.set(color)
    this.direction.set(dirX, dirY, dirZ).nor()
    this
  }

  def set(r: Float, g: Float, b: Float, dirX: Float, dirY: Float, dirZ: Float): DirectionalLight = {
    this.color.set(r, g, b, 1f)
    this.direction.set(dirX, dirY, dirZ).nor()
    this
  }

  override def equals(arg0: Any): Boolean = arg0 match {
    case other: DirectionalLight => equals(other)
    case _ => false
  }

  def equals(other: DirectionalLight): Boolean =
    (other != null) && ((other eq this) || (color.equals(other.color) && direction.equals(other.direction)))
}
