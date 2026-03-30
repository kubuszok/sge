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
