/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/utils/rays/CentralRayWithWhiskersConfiguration.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package steer
package utils
package rays

import sge.ai.utils.Ray
import sge.math.Vector

/** A `CentralRayWithWhiskersConfiguration` uses a long central ray and two shorter whiskers.
  *
  * A central ray with short whiskers is often the best initial configuration to try but can make it impossible for the character to move down tight passages. Also, it is still susceptible to the
  * corner trap, far less than the parallel configuration though.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class CentralRayWithWhiskersConfiguration[T <: Vector[T]](
  owner: Steerable[T],
  /** The length of the central ray. */
  var rayLength: Float,
  /** The length of the two whiskers (usually shorter than the central ray). */
  var whiskerLength: Float,
  /** The angle in radians of the whiskers from the central ray. */
  var whiskerAngle: Float
) extends RayConfigurationBase[T](owner, 3) {

  override def updateRays(): Array[Ray[T]] = {
    val ownerPosition = owner.position
    val ownerVelocity = owner.linearVelocity

    val velocityAngle = owner.vectorToAngle(ownerVelocity)

    // Update central ray
    rays(0).start.set(ownerPosition)
    rays(0).end.set(ownerVelocity).normalize().scale(rayLength).+(ownerPosition)

    // Update left ray
    rays(1).start.set(ownerPosition)
    owner.angleToVector(rays(1).end, velocityAngle - whiskerAngle).scale(whiskerLength).+(ownerPosition)

    // Update right ray
    rays(2).start.set(ownerPosition)
    owner.angleToVector(rays(2).end, velocityAngle + whiskerAngle).scale(whiskerLength).+(ownerPosition)

    rays
  }
}
