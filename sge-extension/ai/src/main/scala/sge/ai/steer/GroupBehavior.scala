/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/GroupBehavior.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 32
 * Covenant-baseline-methods: GroupBehavior,proximity
 * Covenant-source-reference: com/badlogic/gdx/ai/steer/GroupBehavior.java
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 32
 * Covenant-baseline-methods: GroupBehavior,proximity
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package steer

import sge.math.Vector

/** `GroupBehavior` is the base class for the steering behaviors that take into consideration the agents in the game world that are within the immediate area of the owner. This immediate area is
  * defined by a [[Proximity]] that is in charge of finding and processing the owner's neighbors through the given [[Proximity.ProximityCallback]].
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
abstract class GroupBehavior[T <: Vector[T]](
  owner: Steerable[T],
  /** The proximity decides which agents are considered neighbors. */
  var proximity: Proximity[T]
) extends SteeringBehavior[T](owner) {}
