/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/fma/SlotAssignment.java
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
 * Covenant-baseline-loc: 27
 * Covenant-baseline-methods: SlotAssignment
 * Covenant-source-reference: com/badlogic/gdx/ai/fma/SlotAssignment.java
 *   Renames: `com.badlogic.gdx.ai.fma` -> `sge.ai.fma`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 27
 * Covenant-baseline-methods: SlotAssignment
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package fma

import sge.math.Vector

/** A `SlotAssignment` instance represents the assignment of a single [[FormationMember]] to its slot in the [[Formation]].
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class SlotAssignment[T <: Vector[T]](var member: FormationMember[T], var slotNumber: Int = 0) {}
