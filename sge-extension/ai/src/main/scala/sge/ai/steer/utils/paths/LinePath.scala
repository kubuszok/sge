/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/utils/paths/LinePath.java
 * Original authors: davebaol, Daniel Holderbaum
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`; `Array` -> `DynamicArray`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package steer
package utils
package paths

import sge.math.{ MathUtils, Vector }
import sge.utils.DynamicArray

import scala.compiletime.uninitialized

/** A `LinePath` is a path for path following behaviors that is made up of a series of waypoints. Each waypoint is connected to the successor with a [[LinePath.Segment]].
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  * @author
  *   Daniel Holderbaum (original implementation)
  */
class LinePath[T <: Vector[T]](waypoints: DynamicArray[T], val isOpen: Boolean = false)(using scala.reflect.ClassTag[LinePath.Segment[T]]) extends Path[T, LinePath.LinePathParam] {

  private var segments:                     DynamicArray[LinePath.Segment[T]] = uninitialized
  private var pathLength:                   Float                             = 0f
  private val nearestPointOnCurrentSegment: T                                 = waypoints.first.copy
  private val nearestPointOnPath:           T                                 = waypoints.first.copy
  private val tmpB:                         T                                 = waypoints.first.copy
  private val tmpC:                         T                                 = waypoints.first.copy

  createPath(waypoints)

  override def length: Float = pathLength

  override def startPoint: T = segments.first.begin

  override def endPoint: T = segments.peek.end

  /** Returns the square distance of the nearest point on line segment `a-b`, from point `c`. Also, the `out` vector is assigned to the nearest point.
    * @param out
    *   the output vector that contains the nearest point on return
    * @param a
    *   the start point of the line segment
    * @param b
    *   the end point of the line segment
    * @param c
    *   the point to calculate the distance from
    */
  def calculatePointSegmentSquareDistance(out: T, a: T, b: T, c: T): Float = {
    out.set(a)
    tmpB.set(b)
    tmpC.set(c)

    val ab     = tmpB.-(a)
    val abLen2 = ab.lengthSq
    if (abLen2 != 0) {
      val t = tmpC.-(a).dot(ab) / abLen2
      out.mulAdd(ab, MathUtils.clamp(t, 0, 1))
    }

    out.distanceSq(c)
  }

  override def createParam(): LinePath.LinePathParam =
    new LinePath.LinePathParam()

  // We pass the last parameter value to the path in order to calculate the current
  // parameter value. This is essential to avoid nasty problems when lines are close together.
  // We should limit the algorithm to only considering areas of the path close to the previous
  // parameter value. The character is unlikely to have moved far, after all.
  // This technique, assuming the new value is close to the old one, is called coherence, and it is a
  // feature of many geometric algorithms.
  // TODO: Currently coherence is not implemented.
  override def calculateDistance(agentCurrPos: T, parameter: LinePath.LinePathParam): Float = {
    // Find the nearest segment
    var smallestDistance2 = Float.PositiveInfinity
    var nearestSegment: LinePath.Segment[T] = segments.first
    var i = 0
    while (i < segments.size) {
      val segment   = segments(i)
      val distance2 = calculatePointSegmentSquareDistance(nearestPointOnCurrentSegment, segment.begin, segment.end, agentCurrPos)

      // first point
      if (distance2 < smallestDistance2) {
        nearestPointOnPath.set(nearestPointOnCurrentSegment)
        smallestDistance2 = distance2
        nearestSegment = segment
        parameter.segmentIndex = i
      }
      i += 1
    }

    // Distance from path start
    val lengthOnPath = nearestSegment.cumulativeLength - nearestPointOnPath.distance(nearestSegment.end)

    parameter.distance = lengthOnPath

    lengthOnPath
  }

  override def calculateTargetPosition(out: T, param: LinePath.LinePathParam, targetDistance0: Float): Unit = {
    var targetDistance = targetDistance0
    if (isOpen) {
      // Open path support
      if (targetDistance < 0) {
        // Clamp target distance to the min
        targetDistance = 0
      } else if (targetDistance > pathLength) {
        // Clamp target distance to the max
        targetDistance = pathLength
      }
    } else {
      // Closed path support
      if (targetDistance < 0) {
        // Backwards
        targetDistance = pathLength + (targetDistance % pathLength)
      } else if (targetDistance > pathLength) {
        // Forward
        targetDistance = targetDistance % pathLength
      }
    }

    // Walk through lines to see on which line we are
    var desiredSegment: LinePath.Segment[T] = segments.first
    var i     = 0
    var found = false
    while (i < segments.size && !found) {
      val segment = segments(i)
      if (segment.cumulativeLength >= targetDistance) {
        desiredSegment = segment
        found = true
      }
      i += 1
    }

    // begin-------targetPos-------end
    val distance = desiredSegment.cumulativeLength - targetDistance

    out.set(desiredSegment.begin).-(desiredSegment.end).scale(distance / desiredSegment.length).+(desiredSegment.end)
  }

  /** Sets up this [[Path]] using the given way points.
    * @param waypoints
    *   The way points of this path.
    * @throws IllegalArgumentException
    *   if `waypoints` has less than two (2) waypoints.
    */
  def createPath(waypoints: DynamicArray[T]): Unit = {
    require(waypoints.size >= 2, "waypoints must contain at least two (2) waypoints")

    segments = DynamicArray[LinePath.Segment[T]](waypoints.size)
    pathLength = 0
    var curr = waypoints.first
    var i    = 1
    while (i <= waypoints.size) {
      val prev = curr
      if (i < waypoints.size) {
        curr = waypoints(i)
      } else if (isOpen) {
        i = waypoints.size + 1 // break: keep the path open
      } else {
        curr = waypoints.first // close the path
      }
      if (i <= waypoints.size) {
        val segment = new LinePath.Segment[T](prev, curr)
        pathLength += segment.length
        segment.cumulativeLength = pathLength
        segments.add(segment)
      }
      i += 1
    }
  }

  /** Returns the segments of this path. */
  def getSegments: DynamicArray[LinePath.Segment[T]] = segments
}

object LinePath {

  /** A `LinePathParam` contains the status of a [[LinePath]].
    *
    * @author
    *   davebaol (original implementation)
    */
  class LinePathParam extends Path.PathParam {
    var segmentIndex:      Int   = 0
    private var _distance: Float = 0f

    override def distance:                    Float = _distance
    override def distance_=(distance: Float): Unit  = _distance = distance
  }

  /** A `Segment` connects two consecutive waypoints of a [[LinePath]].
    *
    * @tparam T
    *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
    *
    * @author
    *   davebaol (original implementation)
    */
  class Segment[T <: Vector[T]](
    val begin: T,
    val end:   T
  ) {
    val length:           Float = begin.distance(end)
    var cumulativeLength: Float = 0f
  }
}
