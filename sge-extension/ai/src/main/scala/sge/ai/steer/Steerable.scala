/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/Steerable.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages, def+setter pairs instead of getX/setX/isX
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 50
 * Covenant-baseline-methods: Steerable,angularVelocity,boundingRadius,linearVelocity,tagged,tagged_
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package steer

import sge.ai.utils.Location
import sge.math.Vector

/** A `Steerable` is a [[Location]] that gives access to the character's data required by steering system.
  *
  * Notice that there is nothing to connect the direction that a Steerable is moving and the direction it is facing. For instance, a character can be oriented along the x-axis but be traveling
  * directly along the y-axis.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
trait Steerable[T <: Vector[T]] extends Location[T] with Limiter {

  /** Returns the vector indicating the linear velocity of this Steerable. */
  def linearVelocity: T

  /** Returns the float value indicating the the angular velocity in radians of this Steerable. */
  def angularVelocity: Float

  /** Returns the bounding radius of this Steerable. */
  def boundingRadius: Float

  /** Returns `true` if this Steerable is tagged; `false` otherwise. */
  def tagged: Boolean

  /** Tag/untag this Steerable. This is a generic flag utilized in a variety of ways.
    * @param tagged
    *   the boolean value to set
    */
  def tagged_=(tagged: Boolean): Unit
}
