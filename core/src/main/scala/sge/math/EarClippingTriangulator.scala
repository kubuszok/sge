/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/EarClippingTriangulator.java
 * Original authors: badlogicgames@gmail.com, Nicolas Gramlich (optimizations, collinear edge support), Eric Spitz, Thomas ten Cate (bugfixes, optimizations), Nathan Sweet (rewrite, return indices, no allocation, optimizations)
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * AUDIT: PASS — All methods ported: computeTriangles(3). Private helpers:
 * classifyVertices, isEarTip, areVerticesClockwise. Uses DynamicArray instead of ShortArray.
 */
package sge
package math

import scala.util.boundary
import scala.util.boundary.break

import sge.utils.DynamicArray

/** A simple implementation of the ear cutting algorithm to triangulate simple polygons without holes. For more information: <ul> <li><a
  * href="http://cgm.cs.mcgill.ca/~godfried/teaching/cg-projects/97/Ian/algorithm2.html">http://cgm.cs.mcgill.ca/~godfried/ teaching/cg-projects/97/Ian/algorithm2.html</a></li> <li><a href=
  * "http://www.geometrictools.com/Documentation/TriangulationByEarClipping.pdf">http://www.geometrictools.com/Documentation /TriangulationByEarClipping.pdf</a></li> </ul> If the input polygon is not
  * simple (self-intersects), there will be output but it is of unspecified quality (garbage in, garbage out). <p> If the polygon vertices are very large or very close together then
  * {@link GeometryUtils#isClockwise(float[], int, int)} may not be able to properly assess the winding (because it uses floats). In that case the vertices should be adjusted, eg by finding the
  * smallest X and Y values and subtracting that from each vertex.
  * @author
  *   badlogicgames@gmail.com (original implementation)
  * @author
  *   Nicolas Gramlich (optimizations, collinear edge support) (original implementation)
  * @author
  *   Eric Spitz (original implementation)
  * @author
  *   Thomas ten Cate (bugfixes, optimizations) (original implementation)
  * @author
  *   Nathan Sweet (rewrite, return indices, no allocation, optimizations) (original implementation)
  */
class EarClippingTriangulator {
  private val CONCAVE = -1
  private val CONVEX  = 1

  private val indicesArray = DynamicArray[Short]()
  private var indices:     Array[Short] = scala.compiletime.uninitialized
  private var vertices:    Array[Float] = scala.compiletime.uninitialized
  private var vertexCount: Int          = scala.compiletime.uninitialized
  private val vertexTypes = DynamicArray[Int]()
  private val triangles   = DynamicArray[Short]()

  /** @see #computeTriangles(float[], int, int) */
  def computeTriangles(vertices: Array[Float]): DynamicArray[Short] =
    computeTriangles(vertices, 0, vertices.length)

  /** Triangulates the given (convex or concave) simple polygon to a list of triangle vertices.
    * @param vertices
    *   pairs describing vertices of the polygon, in either clockwise or counterclockwise order.
    * @return
    *   triples of triangle indices in clockwise order. Note the returned array is reused for later calls to the same method.
    */
  def computeTriangles(vertices: Array[Float], offset: Int, count: Int): DynamicArray[Short] = {
    this.vertices = vertices
    val vertexCount = count / 2
    this.vertexCount = vertexCount
    val vertexOffset = offset / 2

    val indicesArray = this.indicesArray
    indicesArray.clear()
    indicesArray.ensureCapacity(vertexCount)
    val indices = new Array[Short](vertexCount)
    this.indices = indices
    if (GeometryUtils.isClockwise(vertices, offset, count)) {
      var i: Short = 0
      while (i < vertexCount) {
        indices(i) = (vertexOffset + i).toShort
        i = (i + 1).toShort
      }
    } else {
      var i = 0
      val n = vertexCount - 1
      while (i < vertexCount) {
        indices(i) = (vertexOffset + n - i).toShort // Reversed.
        i += 1
      }
    }

    val vertexTypes = this.vertexTypes
    vertexTypes.clear()
    vertexTypes.ensureCapacity(vertexCount)
    var i = 0
    val n = vertexCount
    while (i < n) {
      vertexTypes += classifyVertex(i)
      i += 1
    }

    // A polygon with n vertices has a triangulation of n-2 triangles.
    val triangles = this.triangles
    triangles.clear()
    triangles.ensureCapacity(scala.math.max(0, vertexCount - 2) * 3)
    triangulate()
    triangles
  }

  private def triangulate(): Unit = {
    val vertexTypes = this.vertexTypes

    while (vertexCount > 3) {
      val earTipIndex = findEarTip()
      cutEarTip(earTipIndex)

      // The type of the two vertices adjacent to the clipped vertex may have changed.
      val previousIndex = this.previousIndex(earTipIndex)
      val nextIndex     = if (earTipIndex == vertexCount) 0 else earTipIndex
      vertexTypes(previousIndex) = classifyVertex(previousIndex)
      vertexTypes(nextIndex) = classifyVertex(nextIndex)
    }

    if (vertexCount == 3) {
      val triangles = this.triangles
      val indices   = this.indices
      triangles += indices(0)
      triangles += indices(1)
      triangles += indices(2)
    }
  }

  /** @return {@link #CONCAVE} or {@link #CONVEX} */
  private def classifyVertex(index: Int): Int = {
    val indices  = this.indices
    val previous = indices(previousIndex(index)) * 2
    val current  = indices(index) * 2
    val next     = indices(nextIndex(index)) * 2
    val vertices = this.vertices
    computeSpannedAreaSign(vertices(previous), vertices(previous + 1), vertices(current), vertices(current + 1), vertices(next), vertices(next + 1))
  }

  private def findEarTip(): Int = boundary {
    val vertexCount = this.vertexCount
    var i           = 0
    while (i < vertexCount) {
      if (isEarTip(i)) break(i)
      i += 1
    }

    // Desperate mode: if no vertex is an ear tip, we are dealing with a degenerate polygon (e.g. nearly collinear).
    // Note that the input was not necessarily degenerate, but we could have made it so by clipping some valid ears.

    // Idea taken from Martin Held, "FIST: Fast industrial-strength triangulation of polygons", Algorithmica (1998),
    // http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.115.291

    // Return a convex or tangential vertex if one exists.
    val vertexTypes = this.vertexTypes
    i = 0
    while (i < vertexCount) {
      if (vertexTypes(i) != CONCAVE) break(i)
      i += 1
    }
    0 // If all vertices are concave, just return the first one.
  }

  private def isEarTip(earTipIndex: Int): Boolean = boundary {
    val vertexTypes = this.vertexTypes
    if (vertexTypes(earTipIndex) == CONCAVE) break(false)

    val prevIndex = this.previousIndex(earTipIndex)
    val nextIdx   = this.nextIndex(earTipIndex)
    val indices   = this.indices
    val p1        = indices(prevIndex) * 2
    val p2        = indices(earTipIndex) * 2
    val p3        = indices(nextIdx) * 2
    val vertices  = this.vertices
    val p1x       = vertices(p1)
    val p1y       = vertices(p1 + 1)
    val p2x       = vertices(p2)
    val p2y       = vertices(p2 + 1)
    val p3x       = vertices(p3)
    val p3y       = vertices(p3 + 1)

    // Check if any point is inside the triangle formed by previous, current and next vertices.
    // Only consider vertices that are not part of this triangle, or else we'll always find one inside.
    var i = this.nextIndex(this.nextIndex(earTipIndex))
    while (i != prevIndex) {
      // Concave vertices can obviously be inside the candidate ear, but so can tangential vertices
      // if they coincide with one of the triangle's vertices.
      if (vertexTypes(i) != CONVEX) {
        val v  = indices(i) * 2
        val vx = vertices(v)
        val vy = vertices(v + 1)
        // Because the polygon has clockwise winding order, the area sign will be positive if the point is strictly inside.
        // It will be 0 on the edge, which we want to include as well.
        // note: check the edge defined by p1->p3 first since this fails _far_ more then the other 2 checks.
        if (computeSpannedAreaSign(p3x, p3y, p1x, p1y, vx, vy) >= 0) {
          if (computeSpannedAreaSign(p1x, p1y, p2x, p2y, vx, vy) >= 0) {
            if (computeSpannedAreaSign(p2x, p2y, p3x, p3y, vx, vy) >= 0) break(false)
          }
        }
      }
      i = this.nextIndex(i)
    }
    true
  }

  private def cutEarTip(earTipIndex: Int): Unit = {
    val indices   = this.indices
    val triangles = this.triangles

    triangles += indices(previousIndex(earTipIndex))
    triangles += indices(earTipIndex)
    triangles += indices(nextIndex(earTipIndex))

    // Remove from indices array by shifting elements
    for (i <- earTipIndex until vertexCount - 1)
      indices(i) = indices(i + 1)
    // Remove from vertex types
    vertexTypes.removeIndex(earTipIndex)
    vertexCount -= 1
  }

  private def previousIndex(index: Int): Int =
    if (index == 0) vertexCount - 1 else index - 1

  private def nextIndex(index: Int): Int =
    (index + 1) % vertexCount

  private def computeSpannedAreaSign(p1x: Float, p1y: Float, p2x: Float, p2y: Float, p3x: Float, p3y: Float): Int = {
    var area = p1x * (p3y - p2y)
    area += p2x * (p1y - p3y)
    area += p3x * (p2y - p1y)
    scala.math.signum(area).toInt
  }
}
