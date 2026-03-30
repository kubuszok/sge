/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/utils/Ray.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.utils` -> `sge.ai.utils`; `Vector` -> `sge.math.Vector`
 *   Convention: split packages, public vars instead of public fields
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package utils

import sge.math.Vector

/** A ray is made up of a starting point and an ending point.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class Ray[T <: Vector[T]](
  /** The starting point of this ray. */
  var start: T,
  /** The ending point of this ray. */
  var end: T
) {

  /** Sets this ray from the given ray.
    * @param ray
    *   The ray
    * @return
    *   this ray for chaining.
    */
  def set(ray: Ray[T]): Ray[T] = {
    start.set(ray.start)
    end.set(ray.end)
    this
  }

  /** Sets this ray from the given start and end points.
    * @param start
    *   the starting point of this ray
    * @param end
    *   the ending point of this ray
    * @return
    *   this ray for chaining.
    */
  def set(start: T, end: T): Ray[T] = {
    this.start.set(start)
    this.end.set(end)
    this
  }
}
