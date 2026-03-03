/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/environment/DirectionalLight.java
 * Original authors: (see AUTHORS file)
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Audit: pass (2026-03-03)
 *   - All 9 methods ported: setDirection x2, set x5, equals x2
 *   - Null checks → Nullable.foreach
 *   - equals(DirectionalLight) uses Nullable.fold for null safety
 */
package sge
package graphics
package g3d
package environment

import scala.annotation.targetName
import scala.language.implicitConversions

import sge.math.Vector3
import sge.utils.Nullable

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
    case other: DirectionalLight => equals(other)
    case _ => false
  }

  @targetName("equalsDirectionalLight")
  def equals(other: Nullable[DirectionalLight]): Boolean =
    other.fold(false)(o => (o eq this) || (color.equals(o.color) && direction.equals(o.direction)))
}
