/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/fma/FormationPattern.java
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
 * Covenant-baseline-loc: 52
 * Covenant-baseline-methods: FormationPattern,calculateSlotLocation,numberOfSlots,numberOfSlots_,supportsSlots
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package fma

import sge.ai.utils.Location
import sge.math.Vector

/** The `FormationPattern` trait represents the shape of a formation and generates the slot offsets, relative to its anchor point. Since formations can be scalable the pattern must be able to
  * determine if a given number of slots is supported.
  *
  * Each particular pattern (such as a V, wedge, circle) needs its own instance of a class that implements this `FormationPattern` trait.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
trait FormationPattern[T <: Vector[T]] {

  /** Returns the number of slots. */
  def numberOfSlots: Int

  /** Sets the number of slots.
    * @param numberOfSlots
    *   the number of slots to set
    */
  def numberOfSlots_=(numberOfSlots: Int): Unit

  /** Returns the location of the given slot index. */
  def calculateSlotLocation(outLocation: Location[T], slotNumber: Int): Location[T]

  /** Returns true if the pattern can support the given number of slots
    * @param slotCount
    *   the number of slots
    * @return
    *   `true` if this pattern can support the given number of slots; `false` otherwise.
    */
  def supportsSlots(slotCount: Int): Boolean
}
