/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/utils/RaycastCollisionDetector.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.utils` -> `sge.ai.utils`; `Vector` -> `sge.math.Vector`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 47
 * Covenant-baseline-methods: RaycastCollisionDetector,collides,findCollision
 * Covenant-source-reference: com/badlogic/gdx/ai/utils/RaycastCollisionDetector.java
 *   Renames: `com.badlogic.gdx.ai.utils` -> `sge.ai.utils`; `Vector` -> `sge.math.Vector`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 47
 * Covenant-baseline-methods: RaycastCollisionDetector,collides,findCollision
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package utils

import sge.math.Vector

/** Finds the closest intersection between a ray and any object in the game world.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
trait RaycastCollisionDetector[T <: Vector[T]] {

  /** Casts the given ray to test if it collides with any objects in the game world.
    * @param ray
    *   the ray to cast.
    * @return
    *   `true` in case of collision; `false` otherwise.
    */
  def collides(ray: Ray[T]): Boolean

  /** Find the closest collision between the given input ray and the objects in the game world. In case of collision, `outputCollision` will contain the collision point and the normal vector of the
    * obstacle at the point of collision.
    * @param outputCollision
    *   the output collision.
    * @param inputRay
    *   the ray to cast.
    * @return
    *   `true` in case of collision; `false` otherwise.
    */
  def findCollision(outputCollision: Collision[T], inputRay: Ray[T]): Boolean
}
