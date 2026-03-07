package sge
package math
package collision

class CollisionTest extends munit.FunSuite {

  test("boundingBox") {
    val b1 = new BoundingBox(Vector3.Zero, new Vector3(1, 1, 1))
    val b2 = new BoundingBox(new Vector3(1, 1, 1), new Vector3(2, 2, 2))
    assert(b1.contains(Vector3.Zero))
    assert(b1.contains(b1))
    assert(!b1.contains(b2))
    // Note, in stage the bottom and left sides are inclusive while the right and top sides are exclusive.
  }

  test("orientedBoundingBox") {
    val b1 = new OrientedBoundingBox(new BoundingBox(Vector3.Zero, new Vector3(1, 1, 1)))
    assert(b1.contains(Vector3.Zero))
    assert(b1.contains(b1))
    val b2 = new OrientedBoundingBox(new BoundingBox(new Vector3(1, 1, 1), new Vector3(2, 2, 2)))
    assert(!b1.contains(b2))
  }

  test("orientedBoundingBoxCollision") {
    val b1 = new OrientedBoundingBox(new BoundingBox(Vector3.Zero, new Vector3(1, 1, 1)))
    val b2 = new OrientedBoundingBox(
      new BoundingBox(new Vector3(1 + MathUtils.FLOAT_ROUNDING_ERROR, 1, 1), new Vector3(2, 2, 2))
    )
    assert(!b1.intersects(b2))
    val b3 = new OrientedBoundingBox(new BoundingBox(new Vector3(0.5f, 0.5f, 0.5f), new Vector3(2, 2, 2)))
    assert(b1.intersects(b3))
  }
}
