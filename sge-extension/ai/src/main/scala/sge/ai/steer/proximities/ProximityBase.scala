/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/proximities/ProximityBase.java
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
 * Covenant-baseline-loc: 36
 * Covenant-baseline-methods: ProximityBase,_owner,agents,owner,owner_
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package steer
package proximities

import sge.math.Vector

/** `ProximityBase` is the base class for any concrete proximity based on an iterable collection of agents.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
abstract class ProximityBase[T <: Vector[T]](
  private var _owner: Steerable[T],
  /** The collection of the agents handled by this proximity. */
  var agents: Iterable[? <: Steerable[T]]
) extends Proximity[T] {

  override def owner:                        Steerable[T] = _owner
  override def owner_=(owner: Steerable[T]): Unit         = _owner = owner
}
