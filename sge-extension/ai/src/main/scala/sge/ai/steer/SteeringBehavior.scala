/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/SteeringBehavior.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages, Nullable instead of null, public vars where appropriate
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package steer

import sge.ai.utils.Location
import sge.math.Vector
import sge.utils.Nullable

/** A `SteeringBehavior` calculates the linear and/or angular accelerations to be applied to its owner.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
abstract class SteeringBehavior[T <: Vector[T]](
  /** The owner of this steering behavior */
  var owner: Steerable[T],
  /** The limiter of this steering behavior */
  var limiter: Nullable[Limiter] = Nullable.empty[Limiter],
  /** A flag indicating whether this steering behavior is enabled or not. */
  var enabled: Boolean = true
) {

  /** If this behavior is enabled calculates the steering acceleration and writes it to the given steering output. If it is disabled the steering output is set to zero.
    * @param steering
    *   the steering acceleration to be calculated.
    * @return
    *   the calculated steering acceleration for chaining.
    */
  def calculateSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T] =
    if (enabled) calculateRealSteering(steering) else steering.setZero()

  /** Calculates the steering acceleration produced by this behavior and writes it to the given steering output.
    *
    * This method is called by [[calculateSteering]] when this steering behavior is enabled.
    * @param steering
    *   the steering acceleration to be calculated.
    * @return
    *   the calculated steering acceleration for chaining.
    */
  protected def calculateRealSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T]

  /** Returns the actual limiter of this steering behavior. */
  protected def getActualLimiter(): Limiter =
    limiter.getOrElse(owner)

  /** Utility method that creates a new vector.
    *
    * This method is used internally to instantiate vectors of the correct type parameter `T`. This technique keeps the API simple and makes the API easier to use with the GWT backend because avoids
    * the use of reflection.
    *
    * @param location
    *   the location whose position is used to create the new vector
    * @return
    *   the newly created vector
    */
  protected def newVector(location: Location[T]): T =
    location.position.copy.setZero()
}
