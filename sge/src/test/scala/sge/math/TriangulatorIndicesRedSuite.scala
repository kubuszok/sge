package sge
package math

import lowlevel.util.DynamicArray

/** Red tests for ISS-499: DelaunayTriangulator.sort (DelaunayTriangulator.scala:304) and ConvexHull.sort/sortWithIndices (ConvexHull.scala:213/:291) pass `originalIndices.toArray` into the quicksort
  * partition. lls `DynamicArray.toArray` returns a FRESH COPY on every call (DynamicArray.scala:572 `mk.copyOfRange(_items, 0, _size)`), so every index swap performed inside the partition is
  * discarded and `originalIndices` stays the identity permutation.
  *
  * The Java originals operate on the LIVE backing array instead:
  *   - DelaunayTriangulator.java:265 `short[] originalIndicesArray = originalIndices.items;` is taken once, line 278 passes it to `quicksortPartition`, whose swaps (lines 312-314, 325-327) mutate the
  *     shared array; lines 201-203 then map each triangle index through it: `trianglesArray[i] = (short)(originalIndicesArray[trianglesArray[i] / 2] * 2);`
  *   - ConvexHull.java:230 `short[] originalIndicesArray = originalIndices.items;`, line 243 passes it to `quicksortPartitionWithIndices` (swaps at lines 284-286, 296-298), and lines 147-150 map each
  *     hull index: `indicesArray[i] = originalIndicesArray[indicesArray[i]];`
  *
  * Consequence of the SGE bug: `computeTriangles(points, sorted = false)` and `computeIndices(points, ..., sorted = false, ...)` return indices that reference the INTERNALLY SORTED copy of the
  * points, not the caller's input order. The existing DelaunayTriangulatorTest/ConvexHullTest suites only assert index sets/counts, which masks the wrong permutation. These tests dereference the
  * returned indices into the ORIGINAL input array and assert the resulting coordinates, which fails while the bug is present.
  */
class TriangulatorIndicesRedSuite extends munit.FunSuite {

  /** Groups a triangle index list into a set of triangles, each triangle being the set of its vertex coordinates dereferenced from `points` (x,y pairs) via the returned indices.
    */
  private def dereferencedTriangles(points: Array[Float], triangles: DynamicArray[Short]): Set[Set[(Float, Float)]] =
    (0 until triangles.size by 3).map { i =>
      Set(i, i + 1, i + 2).map { j =>
        val p = triangles(j).toInt
        (points(p * 2), points(p * 2 + 1))
      }
    }.toSet

  /** Dereferences hull indices into `points` (x,y pairs), preserving the walk order. */
  private def dereferencedHull(points: Array[Float], indices: DynamicArray[Int]): List[(Float, Float)] =
    (0 until indices.size).map { i =>
      val p = indices(i)
      (points(p * 2), points(p * 2 + 1))
    }.toList

  // Shared geometry: triangle A-B-C with interior point D. The triangulation of a triangle
  // plus a single interior point is UNIQUE (the fan around the interior point), so the
  // expected output does not depend on any Delaunay tie-breaking:
  //   A = (0, 0), B = (10, 0), C = (5, 10), D = (4, 4)
  //   expected triangles (as vertex sets): {A,B,D}, {B,C,D}, {C,A,D}
  private val dA          = (0f, 0f)
  private val dB          = (10f, 0f)
  private val dC          = (5f, 10f)
  private val dD          = (4f, 4f)
  private val expectedFan = Set(Set(dA, dB, dD), Set(dB, dC, dD), Set(dC, dA, dD))

  test("ISS-499 red: DelaunayTriangulator.computeTriangles(sorted=false) indices reference the original input order") {
    // Input order is a deliberate non-trivial permutation of the x-sorted order (no fixed points):
    //   idx 0: B = (10, 0)
    //   idx 1: C = (5, 10)
    //   idx 2: A = (0, 0)
    //   idx 3: D = (4, 4)
    // x-sorted order: A(x=0), D(x=4), C(x=5), B(x=10), so the correct sorted-position ->
    // original-index permutation is [2, 3, 1, 0] (Java DelaunayTriangulator.java:201-203).
    // With the bug the permutation stays the identity, so the returned indices are sorted
    // positions: dereferencing them into the original array yields the bogus triangle
    // {B, C, A} (the outer triangle, which no valid triangulation of these 4 points contains)
    // and the expected triangle {A, B, D} is missing.
    val points = Array(10f, 0f, 5f, 10f, 0f, 0f, 4f, 4f)
    val result = new DelaunayTriangulator().computeTriangles(points, false)
    assertEquals(result.size, 9, "expected exactly 3 triangles (9 indices)")
    assertEquals(
      dereferencedTriangles(points, result),
      expectedFan,
      "triangle indices must dereference into the ORIGINAL (unsorted) point array as the unique fan around the interior point D"
    )
  }

  test("ISS-499 red: ConvexHull.computeIndices(sorted=false) hull indices reference the original input order") {
    // Hull A-B-C-D (counter-clockwise) with interior point E:
    //   A = (0, 0), B = (5, 0), C = (4, 4), D = (1, 4), E = (2, 2)
    // Input order is a deliberate non-trivial permutation of the x-sorted order:
    //   idx 0: C = (4, 4)
    //   idx 1: A = (0, 0)
    //   idx 2: E = (2, 2)
    //   idx 3: B = (5, 0)
    //   idx 4: D = (1, 4)
    // x-sorted order: A(0), D(1), E(2), C(4), B(5) -> correct sorted-position ->
    // original-index permutation [1, 4, 2, 0, 3] (Java ConvexHull.java:147-150).
    // The monotone chain on the sorted copy walks A -> B -> C -> D -> A (counter-clockwise,
    // closed), i.e. sorted positions [0, 4, 3, 1, 0]; mapped through the permutation the
    // correct result is original indices [1, 3, 0, 4, 1]. With the bug the identity map is
    // applied and [0, 4, 3, 1, 0] is returned, which dereferences into the original array
    // as (4,4), (1,4), (5,0), (0,0), (4,4) — not a counter-clockwise hull walk.
    val points = Array(4f, 4f, 0f, 0f, 2f, 2f, 5f, 0f, 1f, 4f)
    val result = new ConvexHull().computeIndices(points, false, false)
    assertEquals(
      dereferencedHull(points, result),
      List((0f, 0f), (5f, 0f), (4f, 4f), (1f, 4f), (0f, 0f)),
      "hull indices must dereference into the ORIGINAL (unsorted) point array as the closed counter-clockwise hull walk A, B, C, D, A"
    )
  }

  // PINNED-GREEN CONTROL: this test passes at the red SHA on purpose. The sorted=true path
  // never touches originalIndices (Java DelaunayTriangulator.java:200 / ConvexHull.java:146
  // guard the remapping with `if (!sorted)`), so it is unaffected by ISS-499. It pins the
  // behavior so the fix for the unsorted path cannot regress the sorted path.
  test("ISS-499 control (green at red-sha): sorted=true paths are unaffected") {
    // Delaunay: same geometry as above, already sorted by x: A, D, C, B.
    val delaunayPoints = Array(0f, 0f, 4f, 4f, 5f, 10f, 10f, 0f)
    val triangles      = new DelaunayTriangulator().computeTriangles(delaunayPoints, true)
    assertEquals(triangles.size, 9, "expected exactly 3 triangles (9 indices)")
    assertEquals(
      dereferencedTriangles(delaunayPoints, triangles),
      expectedFan,
      "sorted=true triangle indices must dereference as the unique fan around the interior point D"
    )

    // ConvexHull: same geometry as above, already sorted by x: A, D, E, C, B.
    val hullPoints = Array(0f, 0f, 1f, 4f, 2f, 2f, 4f, 4f, 5f, 0f)
    val indices    = new ConvexHull().computeIndices(hullPoints, true, false)
    assertEquals(
      dereferencedHull(hullPoints, indices),
      List((0f, 0f), (5f, 0f), (4f, 4f), (1f, 4f), (0f, 0f)),
      "sorted=true hull indices must dereference as the closed counter-clockwise hull walk A, B, C, D, A"
    )
  }
}
