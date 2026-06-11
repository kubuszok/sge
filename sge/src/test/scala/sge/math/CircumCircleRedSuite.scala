package sge
package math

import lowlevel.util.DynamicArray

/** Red tests for ISS-500 (a): DelaunayTriangulator.circumCircle (DelaunayTriangulator.scala, COMPLETE predicate) uses the point's FULL squared distance to the circumcenter:
  * {{{
  *   else if (xp > xc && rsqr > drsqr) COMPLETE
  * }}}
  * where `rsqr = (xp - xc)^2 + (yp - yc)^2`. The Java original (DelaunayTriangulator.java:248-256) computes
  * {{{
  *   float dx = x2 - xc, dy = y2 - yc;          // line 248-249
  *   float rsqr = dx * dx + dy * dy;            // line 250: radius^2
  *   dx = xp - xc; dx *= dx;                    // lines 252-253: X-distance^2 only
  *   dy = yp - yc;
  *   if (dx + dy * dy - rsqr <= EPSILON) return INSIDE;          // line 255
  *   return xp > xc && dx > rsqr ? COMPLETE : INCOMPLETE;        // line 256
  * }}}
  * i.e. COMPLETE only when the X-distance^2 alone exceeds radius^2. COMPLETE means "no later point can ever fall inside this circumcircle" — sound in Java because points are processed in ascending x,
  * so once `(xp - xc)^2 > r^2` with `xp > xc` every later point is also to the right of the entire circle. The port's full-distance predicate also fires for points ABOVE/BELOW the circle whose x
  * still lies within the circle's x-range; since COMPLETE permanently excludes the triangle from all further circumcircle tests, a later point that falls inside the circle never splits it and the
  * output is not a Delaunay triangulation.
  *
  * Geometry used below (caller order = index order; the triangulator sorts internally by ascending x and, since ISS-499 is fixed, returns caller-order indices):
  *   - A = (-10, 0), B = (-8, 6), C = (-6, -8): all on the circle x^2 + y^2 = 100, so triangle ABC has circumcenter (0, 0) and radius 10.
  *   - P1 = (1, 15): full distance^2 to (0,0) is 1 + 225 = 226 > 100, but X-distance^2 is 1 < 100. Port: 1 > 0 && 226 > 100 -> COMPLETE (ABC permanently excluded). Java: dx = 1 - 0 = 1, dx*dx = 1,
  *     1 > 100 is false -> INCOMPLETE (line 256), so ABC stays testable.
  *   - P2 = (5, 0): distance^2 = 25 < 100, strictly inside ABC's circumcircle. Processed last (largest x). Java removes ABC and re-triangulates; the buggy port skips the completed ABC, keeping a
  *     triangle whose circumcircle strictly contains an input point.
  *
  * The correct triangulation (hand-derived empty-circumcircle checks, confirmed against the Java original executed on this input) is unique because the 5 points are in generic position (no 4
  * cocircular; A, B, C are the only cocircular triple and P1/P2 are strictly outside/inside their circle):
  *   - {A, C, P2}: circumcenter (-2.5, -1.25), r^2 = 57.8125; B at 82.8125, P1 at 276.3 -> empty.
  *   - {A, B, P2}: circumcenter (-2.5, 5/6), r^2 = 56.944; C at 90.3, P1 at 213.0 -> empty.
  *   - {B, P1, P2}: circumcenter (~0.237, ~6.763), r^2 = 68.43; A at 150.5, C at 256.9 -> empty.
  * The alternative diagonal {A, P2, P1} is rejected: its circumcircle (center (-2.5, ~6.03), r^2 = 92.65) strictly contains B (distance^2 = 30.25).
  *
  * The buggy port instead returns {A, B, C} (containing P2 in its circumcircle), {B, C, P2}, {B, P1, P2}.
  *
  * ISS-500 (b) — Java's degenerate-trio guard `if (y2y3 < EPSILON) return INCOMPLETE;` (DelaunayTriangulator.java:226, both |y1-y2| and |y2-y3| below EPSILON) is indeed missing from the port, but no
  * observable failure could be constructed through the public computeTriangles API: the guard only fires for a mesh triangle whose three vertices are pairwise within 1e-6 in y, and under
  * ascending-x insertion such a sliver's third (rightmost) vertex must lie inside a circumcircle through the other two, which for near-collinear points requires an already-degenerate circumcircle
  * (center offset > x*dx/(2*1e-6)) that no sane mesh provides. Empirical probing (33 configurations, including exact x-tie near-duplicate clusters and sorted=true runs that bypass the internal sort)
  * produced identical triangle SETS for the Java original and a port with only (a) fixed in every case, so this suite covers (a) only.
  *
  * Note: output triangle ORDER legitimately differs from Java — the Java fields are unordered gdx arrays (`new ShortArray(false, 16)`, removal swaps from the end) while the port's DynamicArray
  * removals shift. All assertions therefore compare triangle SETS, never sequences.
  */
class CircumCircleRedSuite extends munit.FunSuite {

  private val pA  = (-10f, 0f)
  private val pB  = (-8f, 6f)
  private val pC  = (-6f, -8f)
  private val pP1 = (1f, 15f)
  private val pP2 = (5f, 0f)

  // Caller order A, B, C, P1, P2 = indices 0..4 (already ascending x, so the internal sort is a no-op permutation).
  private val points = Array(pA._1, pA._2, pB._1, pB._2, pC._1, pC._2, pP1._1, pP1._2, pP2._1, pP2._2)

  private val expectedDelaunay = Set(Set(pA, pC, pP2), Set(pA, pB, pP2), Set(pB, pP1, pP2))

  /** Groups a triangle index list into a set of triangles, each triangle being the set of its vertex coordinates dereferenced from `points` (x,y pairs) via the returned indices. */
  private def dereferencedTriangles(points: Array[Float], triangles: DynamicArray[Short]): Set[Set[(Float, Float)]] =
    (0 until triangles.size by 3).map { i =>
      Set(i, i + 1, i + 2).map { j =>
        val p = triangles(j).toInt
        (points(p * 2), points(p * 2 + 1))
      }
    }.toSet

  /** Circumcenter and squared circumradius of a triangle, computed in Double for use as a test oracle. */
  private def circumCircleOf(a: (Float, Float), b: (Float, Float), c: (Float, Float)): (Double, Double, Double) = {
    val ax = a._1.toDouble; val ay = a._2.toDouble
    val bx = b._1.toDouble; val by = b._2.toDouble
    val cx = c._1.toDouble; val cy = c._2.toDouble
    val d  = 2d * (ax * (by - cy) + bx * (cy - ay) + cx * (ay - by))
    val ux = ((ax * ax + ay * ay) * (by - cy) + (bx * bx + by * by) * (cy - ay) + (cx * cx + cy * cy) * (ay - by)) / d
    val uy = ((ax * ax + ay * ay) * (cx - bx) + (bx * bx + by * by) * (ax - cx) + (cx * cx + cy * cy) * (bx - ax)) / d
    (ux, uy, (ax - ux) * (ax - ux) + (ay - uy) * (ay - uy))
  }

  /** Delaunay empty-circumcircle oracle: every returned triangle's circumcircle must contain no other input point strictly inside. Robust against any valid tie-breaking because the inputs used here
    * are in generic position (no 4 cocircular points), where the Delaunay triangulation is unique.
    */
  private def assertDelaunayProperty(points: Array[Float], triangles: DynamicArray[Short]): Unit = {
    val inputPoints = (0 until points.length by 2).map(i => (points(i), points(i + 1)))
    (0 until triangles.size by 3).foreach { i =>
      val verts          = List(i, i + 1, i + 2).map { j =>
        val p = triangles(j).toInt
        (points(p * 2), points(p * 2 + 1))
      }
      val (cx, cy, rsqr) = circumCircleOf(verts(0), verts(1), verts(2))
      inputPoints.filterNot(verts.contains).foreach { case (px, py) =>
        val distSqr = (px - cx) * (px - cx) + (py - cy) * (py - cy)
        assert(
          distSqr > rsqr - 1e-6,
          s"non-Delaunay output: input point ($px, $py) lies strictly inside the circumcircle of triangle $verts (center ($cx, $cy), r^2 = $rsqr, distance^2 = $distSqr)"
        )
      }
    }
  }

  test("ISS-500 red: computeTriangles returns the unique Delaunay triangulation (Java COMPLETE uses X-distance^2 only)") {
    val result = new DelaunayTriangulator().computeTriangles(points, false)
    assertEquals(result.size, 9, "expected exactly 3 triangles (9 indices)")
    assertEquals(
      dereferencedTriangles(points, result),
      expectedDelaunay,
      "triangle set must match the unique Delaunay triangulation; the buggy full-distance COMPLETE predicate wrongly completes {A,B,C} at P1=(1,15) so P2=(5,0) never splits it"
    )
  }

  test("ISS-500 red: every output triangle's circumcircle is empty of other input points") {
    val result = new DelaunayTriangulator().computeTriangles(points, false)
    // With the bug, {A,B,C} (circumcenter (0,0), r = 10) survives even though P2 = (5,0) at distance^2 = 25 < 100 is strictly inside it.
    assertDelaunayProperty(points, result)
  }

  // PINNED-GREEN CONTROL: this test passes at the red SHA on purpose. Without P1 there is no point
  // that is outside ABC's circumcircle yet left of its right edge, so the wrong COMPLETE predicate
  // never fires before P2 = (5,0) (strictly inside, INSIDE in both versions) splits ABC normally.
  // It pins the surrounding incremental machinery so the fix cannot regress the INSIDE path.
  test("ISS-500 control (green at red-sha): without the wrongly-completing point the triangulation is Delaunay") {
    val controlPoints = Array(pA._1, pA._2, pB._1, pB._2, pC._1, pC._2, pP2._1, pP2._2)
    val result        = new DelaunayTriangulator().computeTriangles(controlPoints, false)
    assertEquals(result.size, 6, "expected exactly 2 triangles (6 indices)")
    assertEquals(
      dereferencedTriangles(controlPoints, result),
      Set(Set(pA, pB, pP2), Set(pA, pC, pP2)),
      "the unique Delaunay triangulation of A, B, C, P2 uses the diagonal A-P2 (the alternative diagonal B-C has C inside {A,B,P2}'s circumcircle reflected check: B at 82.8 > 57.8 and C at 90.3 > 56.9 keep both circles empty)"
    )
    assertDelaunayProperty(controlPoints, result)
  }
}
