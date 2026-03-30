/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/Flee.java
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

/** `Flee` behavior does the opposite of [[Seek]]. It produces a linear steering force that moves the agent away from a target position.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class Flee[T <: Vector[T]](
  owner:  Steerable[T],
  target: Nullable[Location[T]] = Nullable.empty[Location[T]]
) extends Seek[T](owner, target) {

  override protected def calculateRealSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T] = {
    // We just do the opposite of seek, i.e. (owner.position - target.position)
    // instead of (target.position - owner.position)
    steering.linear.set(owner.position).-(target.getOrElse(throw new IllegalStateException("target not set")).position).normalize().scale(getActualLimiter().maxLinearAcceleration)

    // No angular acceleration
    steering.angular = 0

    // Output steering acceleration
    steering
  }
}
