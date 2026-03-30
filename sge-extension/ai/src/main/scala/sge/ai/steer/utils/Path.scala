/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/utils/Path.java
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

import sge.math.Vector

/** The `Path` for an agent having path following behavior. A path can be shared by multiple path following behaviors because its status is maintained in a [[Path.PathParam]] local to each behavior.
  *
  * The most common type of path is made up of straight line segments, which usually gives reasonably good results while keeping the math simple. However, some driving games use splines to get
  * smoother curved paths, which makes the math more complex.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  * @tparam P
  *   Type of path parameter implementing the [[Path.PathParam]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
trait Path[T <: Vector[T], P <: Path.PathParam] {

  /** Returns a new instance of the path parameter. */
  def createParam(): P

  /** Returns `true` if this path is open; `false` otherwise. */
  def isOpen: Boolean

  /** Returns the length of this path. */
  def length: Float

  /** Returns the first point of this path. */
  def startPoint: T

  /** Returns the last point of this path. */
  def endPoint: T

  /** Maps the given position to the nearest point along the path using the path parameter to ensure coherence and returns the distance of that nearest point from the start of the path.
    * @param position
    *   a location in game space
    * @param param
    *   the path parameter
    * @return
    *   the distance of the nearest point along the path from the start of the path itself.
    */
  def calculateDistance(position: T, param: P): Float

  /** Calculates the target position on the path based on its distance from the start and the path parameter.
    * @param out
    *   the target position to calculate
    * @param param
    *   the path parameter
    * @param targetDistance
    *   the distance of the target position from the start of the path
    */
  def calculateTargetPosition(out: T, param: P, targetDistance: Float): Unit
}

object Path {

  /** A path parameter used by path following behaviors to keep the path status.
    *
    * @author
    *   davebaol (original implementation)
    */
  trait PathParam {

    /** Returns the distance from the start of the path */
    def distance: Float

    /** Sets the distance from the start of the path
      * @param distance
      *   the distance to set
      */
    def distance_=(distance: Float): Unit
  }
}
