/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/Seek.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages, Nullable instead of null
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package steer
package behaviors

import sge.ai.utils.Location
import sge.math.Vector
import sge.utils.Nullable

/** `Seek` behavior moves the owner towards the target position. Given a target, this behavior calculates the linear steering acceleration which will direct the agent towards the target as fast as
  * possible.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class Seek[T <: Vector[T]](
  owner: Steerable[T],
  /** The target to seek */
  var target: Nullable[Location[T]] = Nullable.empty[Location[T]]
) extends SteeringBehavior[T](owner) {

  override protected def calculateRealSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T] = {
    // Try to match the position of the character with the position of the target by calculating
    // the direction to the target and by moving toward it as fast as possible.
    steering.linear.set(target.getOrElse(throw new IllegalStateException("target not set")).position).-(owner.position).normalize().scale(getActualLimiter().maxLinearAcceleration)

    // No angular acceleration
    steering.angular = 0

    // Output steering acceleration
    steering
  }
}
