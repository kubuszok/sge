/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/utils/Collision.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.utils` -> `sge.ai.utils`; `Vector` -> `sge.math.Vector`
 *   Convention: split packages, public vars instead of public fields
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 59
 * Covenant-baseline-methods: Collision,normal,point,set
 * Covenant-source-reference: com/badlogic/gdx/ai/utils/Collision.java
 *   Renames: `com.badlogic.gdx.ai.utils` -> `sge.ai.utils`; `Vector` -> `sge.math.Vector`
 *   Convention: split packages, public vars instead of public fields
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 59
 * Covenant-baseline-methods: Collision,normal,point,set
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package utils

import sge.math.Vector

/** A collision is made up of a collision point and the normal at that point of collision.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class Collision[T <: Vector[T]](
  /** The collision point. */
  var point: T,
  /** The normal of this collision. */
  var normal: T
) {

  /** Sets this collision from the given collision.
    * @param collision
    *   The collision
    * @return
    *   this collision for chaining.
    */
  def set(collision: Collision[T]): Collision[T] = {
    point.set(collision.point)
    normal.set(collision.normal)
    this
  }

  /** Sets this collision from the given point and normal.
    * @param point
    *   the collision point
    * @param normal
    *   the normal of this collision
    * @return
    *   this collision for chaining.
    */
  def set(point: T, normal: T): Collision[T] = {
    this.point.set(point)
    this.normal.set(normal)
    this
  }
}
