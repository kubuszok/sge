/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/fma/FormationMember.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.fma` -> `sge.ai.fma`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 33
 * Covenant-baseline-methods: FormationMember,targetLocation
 * Covenant-source-reference: com/badlogic/gdx/ai/fma/FormationMember.java
 *   Renames: `com.badlogic.gdx.ai.fma` -> `sge.ai.fma`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 33
 * Covenant-baseline-methods: FormationMember,targetLocation
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package fma

import sge.ai.utils.Location
import sge.math.Vector

/** Game characters coordinated by a [[Formation]] must implement this trait. Any `FormationMember` has a target location which is the place where it should be in order to stay in formation. This
  * target location is calculated by the formation itself.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
trait FormationMember[T <: Vector[T]] {

  /** Returns the target location of this formation member. */
  def targetLocation: Location[T]
}
