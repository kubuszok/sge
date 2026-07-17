/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red test — ISS-734 umbrella clause c7 (core minors #2), wave 2026-07-17-F,
 * territory X. Reproducer-authored: MUST NOT be modified by the fixer; it
 * encodes the ORIGINAL LibGDX semantics
 * (com/badlogic/gdx/math/collision/BoundingBox.java, original-src/libgdx), not
 * the port's.
 *
 * DEFECT. The port's intersects (BoundingBox.scala lines 361-366) adds an EXTRA
 * validity guard on the ARGUMENT box:
 *
 *   def intersects(bounds: BoundingBox): Boolean =
 *     if (!isValid() || !bounds.isValid()) false
 *     else !( min.x > bounds.max.x || ... )
 *
 * The original BoundingBox.java lines 301-315 guards ONLY `this`:
 *
 *   public boolean intersects (BoundingBox b) {
 *     if (!isValid()) return false;
 *     // SAT test on center (cnt) / half-dimensions (dim) of THIS and b
 *     float lx = Math.abs(this.cnt.x - b.cnt.x);
 *     float sumx = (this.dim.x / 2f) + (b.dim.x / 2f);
 *     ... return (lx <= sumx && ly <= sumy && lz <= sumz);
 *   }
 *
 * The original NEVER checks b.isValid(). For a valid `this` and an `b` that is
 * invalid on one axis but whose center/half-extents still satisfy the SAT
 * overlap on all three axes, the original returns TRUE. The port short-circuits
 * to FALSE on the extra `!bounds.isValid()` guard.
 *
 * (The port's min/max overlap formula is otherwise equivalent to the original's
 * SAT formula for valid boxes — the migration note at BoundingBox.scala:15
 * acknowledges this. The only behavioral divergence is the extra guard.)
 */
package sge
package math
package collision

import sge.math.Vector3

class BoundingBoxIntersectsIss734RedSuite extends munit.FunSuite {

  test("ISS-734 c7: intersects guards only `this`, not the argument box (BoundingBox.java:301-302)") {
    // a: a large valid box centered at the origin. cnt=(0,0,0), dim=(100,100,100).
    val a = new BoundingBox().set(new Vector3(-50f, -50f, -50f), new Vector3(50f, 50f, 50f))
    assert(a.isValid(), "precondition: a is valid")

    // b: INVALID on the x-axis (min.x=1 > max.x=-1) but its center is the origin
    // and its (signed) half-extents keep the SAT overlap satisfied on all axes.
    // Built by mutating min/max directly (set(...) would normalize) + update().
    val b = new BoundingBox()
    b.min.set(1f, -1f, -1f)
    b.max.set(-1f, 1f, 1f)
    b.update() // recomputes cnt=(0,0,0), dim=(-2,2,2)
    assert(!b.isValid(), "precondition: b is invalid on the x-axis (min.x=1 > max.x=-1)")

    // Original SAT (BoundingBox.java:306-315) with `this`=a valid:
    //   lx=|0-0|=0, sumx=50 + (-2)/2 = 49  -> 0 <= 49  true
    //   ly=0,       sumy=50 + 1     = 51   -> true
    //   lz=0,       sumz=50 + 1     = 51   -> true
    //   => intersects returns TRUE (b.isValid() is never consulted).
    assertEquals(
      a.intersects(b),
      true,
      "intersects must guard only `this` (BoundingBox.java:301-302), not the argument; " +
        "the port's extra `!bounds.isValid()` guard (BoundingBox.scala:362) wrongly short-circuits to false"
    )
  }

  test("ISS-734 c7 (control): two ordinary valid boxes intersect / miss as expected") {
    // Positive control — must pass on both port and fixed version.
    val a           = new BoundingBox().set(new Vector3(0f, 0f, 0f), new Vector3(4f, 4f, 4f))
    val overlapping = new BoundingBox().set(new Vector3(3f, 3f, 3f), new Vector3(6f, 6f, 6f))
    val separated   = new BoundingBox().set(new Vector3(10f, 10f, 10f), new Vector3(12f, 12f, 12f))
    assertEquals(a.intersects(overlapping), true, "overlapping valid boxes intersect")
    assertEquals(a.intersects(separated), false, "separated valid boxes do not intersect")
  }
}
