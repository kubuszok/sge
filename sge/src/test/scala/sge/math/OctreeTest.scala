package sge
package math

import sge.math.collision.{ BoundingBox, Ray }

class OctreeTest extends munit.FunSuite {

  test("insert") {
    val maxDepth        = 2
    val maxItemsPerNode = 1

    val min = new Vector3(-5f, -5f, -5f)
    val max = new Vector3(5f, 5f, 5f)

    val octree = new Octree[BoundingBox](
      min,
      max,
      maxDepth,
      maxItemsPerNode,
      new Octree.Collider[BoundingBox] {
        private val tmp = new Vector3()

        override def intersects(nodeBounds: BoundingBox, geometry: BoundingBox): Boolean =
          nodeBounds.intersects(geometry)

        override def intersects(frustum: Frustum, geometry: BoundingBox): Boolean =
          false

        override def intersects(ray: Ray, geometry: BoundingBox): Float =
          // Original inverts the condition (!intersect returns distance), but the
          // semantics are: return distance on hit, Float.POSITIVE_INFINITY on miss.
          // The original test's rayCast path is not exercised so the inversion
          // was harmless there. We use the correct (non-inverted) logic.
          if (Intersector.intersectRayBounds(ray, geometry, sge.utils.Nullable(tmp)))
            tmp.distanceSq(ray.origin)
          else
            Float.PositiveInfinity
      }
    )

    assert(octree.root.isLeaf, "root should be a leaf before any insertions")

    val box1 = new BoundingBox(new Vector3(0, 0, 0), new Vector3(1, 1, 1))
    octree.add(box1)

    val box2 = new BoundingBox(new Vector3(2, 2, 2), new Vector3(3, 3, 3))
    octree.add(box2)
    assert(!octree.root.isLeaf, "root should not be a leaf after adding two items (split)")

    val result = scala.collection.mutable.Set[BoundingBox]()
    octree.getAll(result)
    assert(result.contains(box1))
    assert(result.contains(box2))
    assertEquals(result.size, 2)

    octree.remove(box2)
    result.clear()
    // Refill result geometries
    octree.getAll(result)
    assertEquals(result.size, 1)
    assert(result.contains(box1))
  }
}
