/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/pfa/PathSmoother.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.pfa` -> `sge.ai.pfa`; `Vector` -> `sge.math.Vector`;
 *     `Ray` -> `sge.ai.utils.Ray`; `RaycastCollisionDetector` -> `sge.ai.utils.RaycastCollisionDetector`;
 *     `TimeUtils` -> `sge.utils.TimeUtils`; `Vector.cpy()` -> `Vector.copy`
 *   Convention: split packages; `null` -> `Nullable`; `return` -> `boundary`/`break`
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package pfa

import sge.ai.utils.Ray
import sge.ai.utils.RaycastCollisionDetector
import sge.math.Vector
import sge.utils.Nullable
import sge.utils.TimeUtils

import scala.util.boundary, boundary.break

/** A `PathSmoother` takes a [[SmoothableGraphPath]] and transforms it by linking directly the nodes that are in line of sight. The smoothed path contains at most as many nodes as the original path.
  * Also, the nodes in the smoothed path are unlikely to have any connections between them (if they were connected in the graph, the pathfinder would have found the smoothed route directly, unless
  * their connections had dramatically large costs).
  *
  * Some world representations are more prone to rough paths than others. For example, tile-based graphs tend to be highly erratic. The final appearance also depends on how characters act on the path.
  * If they are using some kind of path following steering behavior, then the path will be gently smoothed by the steering. It is worth testing your game before assuming the path will need smoothing.
  *
  * For some games, path smoothing is essential to get the AI looking smart. The path smoothing algorithm is rather simple but involves raycast and can be somewhat time-consuming.
  *
  * The algorithm assumes that there is a clear route between any two adjacent nodes in the given path. Although this algorithm produces a smooth path, it doesn't search all possible smoothed paths to
  * find the best one, but the final result is usually much more satisfactory than the original path.
  *
  * @tparam N
  *   Type of node
  * @tparam V
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class PathSmoother[N, V <: Vector[V]](
  val raycastCollisionDetector: RaycastCollisionDetector[V]
) {

  private var ray: Nullable[Ray[V]] = Nullable.empty

  /** Smoothes the given path in place.
    * @param path
    *   the path to smooth
    * @return
    *   the number of nodes removed from the path.
    */
  def smoothPath(path: SmoothableGraphPath[N, V]): Int = {
    val inputPathLength = path.getCount

    // If the path is two nodes long or less, then we can't smooth it
    if (inputPathLength <= 2) {
      0
    } else {
      // Make sure the ray is instantiated
      if (ray.isEmpty) {
        val vec = path.getNodePosition(0)
        ray = Nullable(Ray[V](vec.copy, vec.copy))
      }
      val r = ray.get

      // Keep track of where we are in the smoothed path.
      // We start at 1, because we must always include the start node in the smoothed path.
      var outputIndex = 1

      // Keep track of where we are in the input path
      // We start at 2, because we assume two adjacent
      // nodes will pass the ray cast
      var inputIndex = 2

      // Loop until we find the last item in the input
      while (inputIndex < inputPathLength) {
        // Set the ray
        r.start.set(path.getNodePosition(outputIndex - 1))
        r.end.set(path.getNodePosition(inputIndex))

        // Do the ray cast
        val collides = raycastCollisionDetector.collides(r)

        if (collides) {
          // The ray test failed, swap nodes and consider the next output node
          path.swapNodes(outputIndex, inputIndex - 1)
          outputIndex += 1
        }

        // Consider the next input node
        inputIndex += 1
      }

      // Reached the last input node, always add it to the smoothed path.
      path.swapNodes(outputIndex, inputIndex - 1)
      path.truncatePath(outputIndex + 1)

      // Return the number of removed nodes
      inputIndex - outputIndex - 1
    }
  }

  /** Smoothes in place the path specified by the given request, possibly over multiple consecutive frames.
    * @param request
    *   the path smoothing request
    * @param timeToRun
    *   the time in nanoseconds that this call can use on the current frame
    * @return
    *   `true` if this operation has completed; `false` if more time is needed to complete.
    */
  def smoothPath(request: PathSmootherRequest[N, V], timeToRun: Long): Boolean = {

    var lastTime      = TimeUtils.nanoTime().toLong
    var remainingTime = timeToRun

    val path            = request.path
    val inputPathLength = path.getCount

    // If the path is two nodes long or less, then we can't smooth it
    if (inputPathLength <= 2) {
      true
    } else {
      if (request.isNew) {
        request.isNew = false

        // Make sure the ray is instantiated
        if (ray.isEmpty) {
          val vec = request.path.getNodePosition(0)
          ray = Nullable(Ray[V](vec.copy, vec.copy))
        }

        // Keep track of where we are in the smoothed path.
        // We start at 1, because we must always include the start node in the smoothed path.
        request.outputIndex = 1

        // Keep track of where we are in the input path
        // We start at 2, because we assume two adjacent
        // nodes will pass the ray cast
        request.inputIndex = 2
      }

      val r = ray.get

      boundary {
        // Loop until we find the last item in the input
        while (request.inputIndex < inputPathLength) {

          // Check the available time
          val currentTime = TimeUtils.nanoTime().toLong
          remainingTime -= currentTime - lastTime
          if (remainingTime <= PathFinderQueue.TIME_TOLERANCE) break(false)

          // Set the ray
          r.start.set(path.getNodePosition(request.outputIndex - 1))
          r.end.set(path.getNodePosition(request.inputIndex))

          // Do the ray cast
          val collided = raycastCollisionDetector.collides(r)

          if (collided) {
            // The ray test failed, swap nodes and consider the next output node
            path.swapNodes(request.outputIndex, request.inputIndex - 1)
            request.outputIndex += 1
          }

          // Consider the next input node
          request.inputIndex += 1

          // Store the current time
          lastTime = currentTime
        }

        // Reached the last input node, always add it to the smoothed path
        path.swapNodes(request.outputIndex, request.inputIndex - 1)
        path.truncatePath(request.outputIndex + 1)

        // Smooth completed
        true
      }
    }
  }
}
