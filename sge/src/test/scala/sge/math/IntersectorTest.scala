package sge
package math

import sge.math.collision.{ BoundingBox, Ray }
import sge.utils.Nullable

class IntersectorTest extends munit.FunSuite {

  /** Compares two triangles for equality. Triangles must have the same winding, but may begin with different vertex. Values are epsilon compared, with default tolerance. Triangles are assumed to be
    * valid triangles - no duplicate vertices.
    */
  private def triangleEquals(base: Array[Float], baseOffset: Int, stride: Int, comp: Array[Float]): Boolean = {
    assert(stride >= 3)
    assert(base.length - baseOffset >= 9)
    assert(comp.length == 9)

    var offset = -1
    // Find first comp vertex in base triangle
    var i = 0
    while (i < 3 && offset == -1) {
      val b = baseOffset + i * stride
      if (
        MathUtils.isEqual(base(b), comp(0)) && MathUtils.isEqual(base(b + 1), comp(1))
        && MathUtils.isEqual(base(b + 2), comp(2))
      ) {
        offset = i
      }
      i += 1
    }
    assert(offset != -1, "Triangles do not have common first vertex.")
    // Compare vertices
    i = 0
    while (i < 3) {
      val b = baseOffset + ((offset + i) * stride) % (3 * stride)
      val c = i * stride
      if (
        !MathUtils.isEqual(base(b), comp(c)) || !MathUtils.isEqual(base(b + 1), comp(c + 1))
        || !MathUtils.isEqual(base(b + 2), comp(c + 2))
      ) {
        return false
      }
      i += 1
    }
    true
  }

  test("splitTriangle") {
    val plane = new Plane(new Vector3(1, 0, 0), 0)
    val split = new Intersector.SplitTriangle(3)

    { // All back
      val fTriangle = Array(-10f, 0f, 10f, -1f, 0f, 0f, -12f, 0f, 10f)
      Intersector.splitTriangle(fTriangle, plane, split)
      assert(split.numBack == 1)
      assert(split.numFront == 0)
      assert(split.total == 1)
      assert(triangleEquals(split.back, 0, 3, fTriangle))

      fTriangle(4) = 5f
      assert(!triangleEquals(split.back, 0, 3, fTriangle), "Test is broken")
    }

    { // All front
      val fTriangle = Array(10f, 0f, 10f, 1f, 0f, 0f, 12f, 0f, 10f)
      Intersector.splitTriangle(fTriangle, plane, split)
      assert(split.numBack == 0)
      assert(split.numFront == 1)
      assert(split.total == 1)
      assert(triangleEquals(split.front, 0, 3, fTriangle))
    }

    { // Two back, one front
      val triangle = Array(-10f, 0f, 10f, 10f, 0f, 0f, -10f, 0f, -10f)
      Intersector.splitTriangle(triangle, plane, split)
      assert(split.numBack == 2)
      assert(split.numFront == 1)
      assert(split.total == 3)
      assert(triangleEquals(split.front, 0, 3, Array(0f, 0f, 5f, 10f, 0f, 0f, 0f, 0f, -5f)))

      val firstWay  = Array(Array(-10f, 0f, 10f, 0f, 0f, 5f, 0f, 0f, -5f), Array(-10f, 0f, 10f, 0f, 0f, -5f, -10f, 0f, -10f))
      val secondWay = Array(Array(-10f, 0f, 10f, 0f, 0f, 5f, -10f, 0f, -10f), Array(0f, 0f, 5f, 0f, 0f, -5f, -10f, 0f, -10f))
      val base      = split.back
      val first     = (triangleEquals(base, 0, 3, firstWay(0)) && triangleEquals(base, 9, 3, firstWay(1))) ||
        (triangleEquals(base, 0, 3, firstWay(1)) && triangleEquals(base, 9, 3, firstWay(0)))
      val second = (triangleEquals(base, 0, 3, secondWay(0)) && triangleEquals(base, 9, 3, secondWay(1))) ||
        (triangleEquals(base, 0, 3, secondWay(1)) && triangleEquals(base, 9, 3, secondWay(0)))
      assert(first ^ second, s"Either first or second way must be right (first: $first, second: $second)")
    }

    { // Two front, one back
      val triangle = Array(10f, 0f, 10f, -10f, 0f, 0f, 10f, 0f, -10f)
      Intersector.splitTriangle(triangle, plane, split)
      assert(split.numBack == 1)
      assert(split.numFront == 2)
      assert(split.total == 3)
      assert(triangleEquals(split.back, 0, 3, Array(0f, 0f, 5f, -10f, 0f, 0f, 0f, 0f, -5f)))

      val firstWay  = Array(Array(10f, 0f, 10f, 0f, 0f, 5f, 0f, 0f, -5f), Array(10f, 0f, 10f, 0f, 0f, -5f, 10f, 0f, -10f))
      val secondWay = Array(Array(10f, 0f, 10f, 0f, 0f, 5f, 10f, 0f, -10f), Array(0f, 0f, 5f, 0f, 0f, -5f, 10f, 0f, -10f))
      val base      = split.front
      val first     = (triangleEquals(base, 0, 3, firstWay(0)) && triangleEquals(base, 9, 3, firstWay(1))) ||
        (triangleEquals(base, 0, 3, firstWay(1)) && triangleEquals(base, 9, 3, firstWay(0)))
      val second = (triangleEquals(base, 0, 3, secondWay(0)) && triangleEquals(base, 9, 3, secondWay(1))) ||
        (triangleEquals(base, 0, 3, secondWay(1)) && triangleEquals(base, 9, 3, secondWay(0)))
      assert(first ^ second, s"Either first or second way must be right (first: $first, second: $second)")
    }
  }

  test("intersectSegmentCircle") {
    val circle = new Circle(5f, 5f, 4f)
    // Segment intersects, both segment points outside circle
    var intersects = Intersector.intersectSegmentCircle(new Vector2(0, 1f), new Vector2(12f, 3f), circle, Nullable.empty[Intersector.MinimumTranslationVector])
    assert(intersects)
    // Segment intersects, only one of the points inside circle (and is aligned with center)
    intersects = Intersector.intersectSegmentCircle(new Vector2(0, 5f), new Vector2(2f, 5f), circle, Nullable.empty[Intersector.MinimumTranslationVector])
    assert(intersects)
    // Segment intersects, no points outside circle
    intersects = Intersector.intersectSegmentCircle(new Vector2(5.5f, 6f), new Vector2(7f, 5.5f), circle, Nullable.empty[Intersector.MinimumTranslationVector])
    assert(intersects)
    // Segment doesn't intersect
    intersects = Intersector.intersectSegmentCircle(new Vector2(0f, 6f), new Vector2(0.5f, 2f), circle, Nullable.empty[Intersector.MinimumTranslationVector])
    assert(!intersects)
    // Segment is parallel to Y axis left of circle's center
    val mtv = new Intersector.MinimumTranslationVector()
    intersects = Intersector.intersectSegmentCircle(new Vector2(1.5f, 6f), new Vector2(1.5f, 3f), circle, Nullable(mtv))
    assert(intersects)
    assert(mtv.normal.equals(new Vector2(-1f, 0)))
    assertEqualsFloat(mtv.depth, 0.5f, 0.001f)
    // Segment contains circle center point
    intersects = Intersector.intersectSegmentCircle(new Vector2(4f, 5f), new Vector2(6f, 5f), circle, Nullable(mtv))
    assert(intersects)
    assert(mtv.normal.equals(Vector2(0, 1f)) || mtv.normal.equals(Vector2(0f, -1f)))
    assertEqualsFloat(mtv.depth, 4f, 0.001f)
    // Segment contains circle center point which is the same as the end point
    intersects = Intersector.intersectSegmentCircle(new Vector2(4f, 5f), new Vector2(5f, 5f), circle, Nullable(mtv))
    assert(intersects)
    assert(mtv.normal.equals(Vector2(0, 1f)) || mtv.normal.equals(Vector2(0f, -1f)))
    assertEqualsFloat(mtv.depth, 4f, 0.001f)
  }

  test("intersectPlanes") {
    val NEAR = 0; val FAR = 1; val LEFT = 2; val RIGHT = 3; val TOP = 4; val BOTTOM = 5

    val planes = new Array[Plane](6)
    planes(NEAR) = new Plane(new Vector3(0.0f, 0.0f, 1.0f), -0.1f)
    planes(FAR) = new Plane(new Vector3(0.0f, -0.0f, -1.0f), 99.99771f)
    planes(LEFT) = new Plane(new Vector3(-0.69783056f, 0.0f, 0.71626294f), -9.3877316e-7f)
    planes(RIGHT) = new Plane(new Vector3(0.6978352f, 0.0f, 0.71625835f), -0.0f)
    planes(TOP) = new Plane(new Vector3(0.0f, -0.86602545f, 0.5f), -0.0f)
    planes(BOTTOM) = new Plane(new Vector3(-0.0f, 0.86602545f, 0.5f), -0.0f)

    val intersection = new Vector3()
    Intersector.intersectPlanes(planes(TOP), planes(FAR), planes(LEFT), intersection)
    assertEqualsDouble(intersection.x.toDouble, 102.63903, 0.1)
    assertEqualsDouble(intersection.y.toDouble, 57.7337, 0.1)
    assertEqualsDouble(intersection.z.toDouble, 100.0, 0.1)

    Intersector.intersectPlanes(planes(TOP), planes(FAR), planes(RIGHT), intersection)
    assertEqualsDouble(intersection.x.toDouble, -102.63903, 0.1)
    assertEqualsDouble(intersection.y.toDouble, 57.7337, 0.1)
    assertEqualsDouble(intersection.z.toDouble, 100.0, 0.1)

    Intersector.intersectPlanes(planes(BOTTOM), planes(FAR), planes(LEFT), intersection)
    assertEqualsDouble(intersection.x.toDouble, 102.63903, 0.1)
    assertEqualsDouble(intersection.y.toDouble, -57.7337, 0.1)
    assertEqualsDouble(intersection.z.toDouble, 100.0, 0.1)

    Intersector.intersectPlanes(planes(BOTTOM), planes(FAR), planes(RIGHT), intersection)
    assertEqualsDouble(intersection.x.toDouble, -102.63903, 0.1)
    assertEqualsDouble(intersection.y.toDouble, -57.7337, 0.1)
    assertEqualsDouble(intersection.z.toDouble, 100.0, 0.1)
  }

  test("isPointInTriangle2D") {
    assert(!Intersector.isPointInTriangle(new Vector2(0.1f, 0), new Vector2(0, 0), new Vector2(1, 1), new Vector2(-1, -1)))

    assert(Intersector.isPointInTriangle(new Vector2(0, 0.1f), new Vector2(-1, 1), new Vector2(1, 1), new Vector2(-1, -2)))
  }

  test("isPointInTriangle3D") {
    // 2D ---
    assert(!Intersector.isPointInTriangle(new Vector3(0.1f, 0, 0), new Vector3(0, 0, 0), new Vector3(1, 1, 0), new Vector3(-1, -1, 0)))

    assert(Intersector.isPointInTriangle(new Vector3(0, 0.1f, 0), new Vector3(-1, 1, 0), new Vector3(1, 1, 0), new Vector3(-1, -2, 0)))

    // 3D ---
    assert(
      Intersector.isPointInTriangle(new Vector3(0.2f, 0, 1.25f), new Vector3(-1, 1, 0), new Vector3(1.4f, 0.99f, 2.5f), new Vector3(-1, -2, 0))
    )
    // 1.2f away.
    assert(
      !Intersector.isPointInTriangle(new Vector3(2.6f, 0, 3.75f), new Vector3(-1, 1, 0), new Vector3(1.4f, 0.99f, 2.5f), new Vector3(-1, -2, 0))
    )
    // In an edge.
    assert(Intersector.isPointInTriangle(new Vector3(0, -0.5f, 0.5f), new Vector3(-1, 1, 0), new Vector3(1, 1, 1), new Vector3(-1, -2, 0)))
    // Really close to the edge.
    val epsilon = 0.0000001f // One more 0 will fail.
    val almost1 = 1 - epsilon
    assert(
      !Intersector.isPointInTriangle(new Vector3(0, -0.5f, 0.5f), new Vector3(-1, 1, 0), new Vector3(almost1, 1, 1), new Vector3(-1, -2, 0))
    )

    // A really long distance away.
    assert(!Intersector.isPointInTriangle(new Vector3(199f, 1f, 500f), new Vector3(-1, 1, 0), new Vector3(1, 1, 5f), new Vector3(-1, -2, 0)))

    assert(
      !Intersector.isPointInTriangle(
        new Vector3(-5120.8345f, 8946.126f, -3270.5813f),
        new Vector3(50.008057f, 22.20586f, 124.62208f),
        new Vector3(62.282288f, 22.205864f, 109.665924f),
        new Vector3(70.92052f, 7.205861f, 115.437805f)
      )
    )
  }

  test("intersectPolygons") {
    // Corner case with extremely small overlap polygon
    val intersectionPolygon = new Polygon()
    assert(
      !Intersector.intersectPolygons(
        new Polygon(Array(3200.1453f, 88.00839f, 3233.9087f, 190.34174f, 3266.2905f, 0.0f)),
        new Polygon(Array(3213.0f, 131.0f, 3214.0f, 131.0f, 3214.0f, 130.0f, 3213.0f, 130.0f)),
        Nullable(intersectionPolygon)
      )
    )
    assertEquals(intersectionPolygon.vertexCount, 0)
  }

  test("intersectPolygonsWithVertexLyingOnEdge") {
    val p1 = new Polygon(Array(1f, -1f, 2f, -1f, 2f, -2f, 1f, -2f))
    val p2 = new Polygon(Array(0.5f, -1.5f, 1.5f, -1.5f, 1.5f, -2.5f))

    val intersectionPolygon = new Polygon()
    val checkResult         = Intersector.intersectPolygons(p1, p2, Nullable(intersectionPolygon))

    assert(checkResult)
    assertEquals(intersectionPolygon.vertexCount, 4)
    assertEquals(intersectionPolygon.vertex(0, new Vector2()), new Vector2(1.0f, -2.0f))
    assertEquals(intersectionPolygon.vertex(1, new Vector2()), new Vector2(1.0f, -1.5f))
    assertEquals(intersectionPolygon.vertex(2, new Vector2()), new Vector2(1.5f, -1.5f))
    assertEquals(intersectionPolygon.vertex(3, new Vector2()), new Vector2(1.5f, -2.0f))
  }

  test("intersectPolygonsWithTransformationsOnProvidedResultPolygon") {
    val p1                  = new Polygon(Array(1f, -1f, 2f, -1f, 2f, -2f, 1f, -2f))
    val p2                  = new Polygon(Array(0.5f, -1.5f, 1.5f, -1.5f, 1.5f, -2.5f))
    val intersectionPolygon = new Polygon(new Array[Float](8))
    intersectionPolygon.setScale(5, 5)
    intersectionPolygon.setOrigin(10, 20)
    intersectionPolygon.setPosition(-33, -33)
    intersectionPolygon.setRotation(48)

    val checkResult = Intersector.intersectPolygons(p1, p2, Nullable(intersectionPolygon))

    assert(checkResult)
    val expectedVertices = Array(1f, -2f, 1f, -1.5f, 1.5f, -1.5f, 1.5f, -2f)
    val vertices         = intersectionPolygon.vertices
    for (i <- expectedVertices.indices)
      assertEqualsDouble(vertices(i).toDouble, expectedVertices(i).toDouble, 0.0)
    val transformedVertices = intersectionPolygon.transformedVertices
    for (i <- expectedVertices.indices)
      assertEqualsDouble(transformedVertices(i).toDouble, expectedVertices(i).toDouble, 0.0)
    // verify that the origin has also been reset
    intersectionPolygon.setScale(2, 2)
    val scaledExpected    = Array(2 * 1f, 2 * -2f, 2 * 1f, 2 * -1.5f, 2 * 1.5f, 2 * -1.5f, 2 * 1.5f, 2 * -2f)
    val scaledTransformed = intersectionPolygon.transformedVertices
    for (i <- scaledExpected.indices)
      assertEqualsDouble(scaledTransformed(i).toDouble, scaledExpected(i).toDouble, 0.0)
  }

  test("pointLineSide - left") {
    assertEquals(
      Intersector.pointLineSide(new Vector2(0, 0), new Vector2(1, 0), new Vector2(0.5f, 1)),
      1
    )
  }

  test("pointLineSide - right") {
    assertEquals(
      Intersector.pointLineSide(new Vector2(0, 0), new Vector2(1, 0), new Vector2(0.5f, -1)),
      -1
    )
  }

  test("pointLineSide - on line") {
    assertEquals(
      Intersector.pointLineSide(new Vector2(0, 0), new Vector2(1, 0), new Vector2(0.5f, 0)),
      0
    )
  }

  test("pointLineSide - float overload") {
    assertEquals(Intersector.pointLineSide(0f, 0f, 1f, 0f, 0.5f, 1f), 1)
    assertEquals(Intersector.pointLineSide(0f, 0f, 1f, 0f, 0.5f, -1f), -1)
    assertEquals(Intersector.pointLineSide(0f, 0f, 1f, 0f, 0.5f, 0f), 0)
  }

  test("isPointInPolygon - Vector2 array - inside") {
    val polygon = Array(
      new Vector2(0, 0),
      new Vector2(4, 0),
      new Vector2(4, 4),
      new Vector2(0, 4)
    )
    assert(Intersector.isPointInPolygon(polygon, new Vector2(2, 2)))
  }

  test("isPointInPolygon - Vector2 array - outside") {
    val polygon = Array(
      new Vector2(0, 0),
      new Vector2(4, 0),
      new Vector2(4, 4),
      new Vector2(0, 4)
    )
    assert(!Intersector.isPointInPolygon(polygon, new Vector2(5, 5)))
  }

  test("isPointInPolygon - Vector2 array - triangle") {
    val polygon = Array(
      new Vector2(0, 0),
      new Vector2(4, 0),
      new Vector2(2, 4)
    )
    assert(Intersector.isPointInPolygon(polygon, new Vector2(2, 1)))
    assert(!Intersector.isPointInPolygon(polygon, new Vector2(0, 4)))
  }

  test("isPointInPolygon - float array - inside") {
    // Square: (0,0), (4,0), (4,4), (0,4)
    val polygon = Array(0f, 0f, 4f, 0f, 4f, 4f, 0f, 4f)
    assert(Intersector.isPointInPolygon(polygon, 0, polygon.length, 2f, 2f))
  }

  test("isPointInPolygon - float array - outside") {
    val polygon = Array(0f, 0f, 4f, 0f, 4f, 4f, 0f, 4f)
    assert(!Intersector.isPointInPolygon(polygon, 0, polygon.length, 5f, 5f))
  }

  test("isPointInPolygon - float array - triangle") {
    // Triangle: (0,0), (4,0), (2,4)
    val polygon = Array(0f, 0f, 4f, 0f, 2f, 4f)
    assert(Intersector.isPointInPolygon(polygon, 0, polygon.length, 2f, 1f))
    assert(!Intersector.isPointInPolygon(polygon, 0, polygon.length, 0f, 4f))
  }

  // --- New tests for ported methods ---

  test("intersectRayPlane") {
    val plane        = new Plane(new Vector3(0, 1, 0), new Vector3(0, 0, 0)) // XZ plane at y=0
    val ray          = new Ray(new Vector3(0, 5, 0), new Vector3(0, -1, 0))
    val intersection = new Vector3()
    assert(Intersector.intersectRayPlane(ray, plane, Nullable(intersection)))
    assertEqualsFloat(intersection.x, 0f, 0.001f)
    assertEqualsFloat(intersection.y, 0f, 0.001f)
    assertEqualsFloat(intersection.z, 0f, 0.001f)
  }

  test("intersectRayPlane - ray pointing away") {
    val plane = new Plane(new Vector3(0, 1, 0), new Vector3(0, 0, 0))
    val ray   = new Ray(new Vector3(0, 5, 0), new Vector3(0, 1, 0)) // pointing up, away from plane
    assert(!Intersector.intersectRayPlane(ray, plane, Nullable.empty[Vector3]))
  }

  test("intersectRayTriangle") {
    val ray          = new Ray(new Vector3(0, 0, 5), new Vector3(0, 0, -1))
    val t1           = new Vector3(-1, -1, 0)
    val t2           = new Vector3(1, -1, 0)
    val t3           = new Vector3(0, 1, 0)
    val intersection = new Vector3()
    assert(Intersector.intersectRayTriangle(ray, t1, t2, t3, Nullable(intersection)))
    assertEqualsFloat(intersection.z, 0f, 0.01f)
  }

  test("intersectRayTriangle - miss") {
    val ray = new Ray(new Vector3(5, 5, 5), new Vector3(0, 0, -1))
    val t1  = new Vector3(-1, -1, 0)
    val t2  = new Vector3(1, -1, 0)
    val t3  = new Vector3(0, 1, 0)
    assert(!Intersector.intersectRayTriangle(ray, t1, t2, t3, Nullable.empty[Vector3]))
  }

  test("intersectRaySphere") {
    val ray          = new Ray(new Vector3(0, 0, 10), new Vector3(0, 0, -1))
    val center       = new Vector3(0, 0, 0)
    val intersection = new Vector3()
    assert(Intersector.intersectRaySphere(ray, center, 1f, Nullable(intersection)))
    assertEqualsFloat(intersection.z, 1f, 0.01f)
  }

  test("intersectRaySphere - miss") {
    val ray    = new Ray(new Vector3(5, 5, 10), new Vector3(0, 0, -1))
    val center = new Vector3(0, 0, 0)
    assert(!Intersector.intersectRaySphere(ray, center, 1f, Nullable.empty[Vector3]))
  }

  test("intersectRayBounds") {
    val ray          = new Ray(new Vector3(0, 0, 10), new Vector3(0, 0, -1))
    val box          = new BoundingBox(new Vector3(-1, -1, -1), new Vector3(1, 1, 1))
    val intersection = new Vector3()
    assert(Intersector.intersectRayBounds(ray, box, Nullable(intersection)))
    assertEqualsFloat(intersection.z, 1f, 0.01f)
  }

  test("intersectRayBounds - miss") {
    val ray = new Ray(new Vector3(5, 5, 10), new Vector3(0, 0, -1))
    val box = new BoundingBox(new Vector3(-1, -1, -1), new Vector3(1, 1, 1))
    assert(!Intersector.intersectRayBounds(ray, box, Nullable.empty[Vector3]))
  }

  test("intersectRayBoundsFast") {
    val ray = new Ray(new Vector3(0, 0, 10), new Vector3(0, 0, -1))
    val box = new BoundingBox(new Vector3(-1, -1, -1), new Vector3(1, 1, 1))
    assert(Intersector.intersectRayBoundsFast(ray, box))
  }

  test("intersectSegmentCircle - squareRadius overload") {
    // segment through center of circle at (5,5) with radius 4
    assert(Intersector.intersectSegmentCircle(new Vector2(0, 5), new Vector2(10, 5), new Vector2(5, 5), 16f))
    // segment far away
    assert(!Intersector.intersectSegmentCircle(new Vector2(0, 0), new Vector2(10, 0), new Vector2(5, 5), 1f))
  }

  test("intersectLines") {
    val intersection = new Vector2()
    assert(Intersector.intersectLines(new Vector2(0, 0), new Vector2(1, 1), new Vector2(0, 1), new Vector2(1, 0), Nullable(intersection)))
    assertEqualsFloat(intersection.x, 0.5f, 0.001f)
    assertEqualsFloat(intersection.y, 0.5f, 0.001f)
  }

  test("intersectLines - parallel") {
    assert(!Intersector.intersectLines(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f, Nullable.empty[Vector2]))
  }

  test("intersectSegments") {
    val intersection = new Vector2()
    // Two crossing segments
    assert(Intersector.intersectSegments(new Vector2(0, 0), new Vector2(1, 1), new Vector2(0, 1), new Vector2(1, 0), Nullable(intersection)))
    assertEqualsFloat(intersection.x, 0.5f, 0.001f)
    assertEqualsFloat(intersection.y, 0.5f, 0.001f)
    // Non-crossing segments
    assert(
      !Intersector.intersectSegments(new Vector2(0, 0), new Vector2(1, 0), new Vector2(0, 1), new Vector2(1, 1), Nullable.empty[Vector2])
    )
  }

  test("intersectRectangles") {
    val r1     = new Rectangle(0, 0, 4, 4)
    val r2     = new Rectangle(2, 2, 4, 4)
    val result = new Rectangle()
    assert(Intersector.intersectRectangles(r1, r2, result))
    assertEqualsFloat(result.x, 2f, 0.001f)
    assertEqualsFloat(result.y, 2f, 0.001f)
    assertEqualsFloat(result.width, 2f, 0.001f)
    assertEqualsFloat(result.height, 2f, 0.001f)
  }

  test("intersectRectangles - no overlap") {
    val r1     = new Rectangle(0, 0, 1, 1)
    val r2     = new Rectangle(5, 5, 1, 1)
    val result = new Rectangle()
    assert(!Intersector.intersectRectangles(r1, r2, result))
  }

  test("overlaps - circles") {
    assert(Intersector.overlaps(new Circle(0, 0, 2), new Circle(1, 0, 2)))
    assert(!Intersector.overlaps(new Circle(0, 0, 1), new Circle(5, 5, 1)))
  }

  test("overlaps - rectangles") {
    assert(Intersector.overlaps(new Rectangle(0, 0, 4, 4), new Rectangle(2, 2, 4, 4)))
    assert(!Intersector.overlaps(new Rectangle(0, 0, 1, 1), new Rectangle(5, 5, 1, 1)))
  }

  test("overlaps - circle and rectangle") {
    assert(Intersector.overlaps(new Circle(2, 2, 2), new Rectangle(0, 0, 4, 4)))
    assert(!Intersector.overlaps(new Circle(0, 0, 0.5f), new Rectangle(5, 5, 1, 1)))
  }

  test("overlapConvexPolygons") {
    // Two overlapping squares
    val p1 = new Polygon(Array(0f, 0f, 4f, 0f, 4f, 4f, 0f, 4f))
    val p2 = new Polygon(Array(2f, 2f, 6f, 2f, 6f, 6f, 2f, 6f))
    assert(Intersector.overlapConvexPolygons(p1, p2))

    // Two non-overlapping squares
    val p3 = new Polygon(Array(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f))
    val p4 = new Polygon(Array(5f, 5f, 6f, 5f, 6f, 6f, 5f, 6f))
    assert(!Intersector.overlapConvexPolygons(p3, p4))
  }

  test("overlapConvexPolygons with MTV") {
    val p1  = new Polygon(Array(0f, 0f, 4f, 0f, 4f, 4f, 0f, 4f))
    val p2  = new Polygon(Array(3f, 0f, 7f, 0f, 7f, 4f, 3f, 4f))
    val mtv = new Intersector.MinimumTranslationVector()
    assert(Intersector.overlapConvexPolygons(p1, p2, Nullable(mtv)))
    assert(mtv.depth > 0)
    assert(mtv.normal.length > 0)
  }

  test("intersectRayRay") {
    val scalar = Intersector.intersectRayRay(
      new Vector2(0, 0),
      new Vector2(1, 0),
      new Vector2(0.5f, 1),
      new Vector2(0, -1)
    )
    assertEqualsFloat(scalar, 0.5f, 0.001f)
  }

  test("intersectRayRay - collinear") {
    val scalar = Intersector.intersectRayRay(
      new Vector2(0, 0),
      new Vector2(1, 0),
      new Vector2(1, 0),
      new Vector2(1, 0)
    )
    assert(scalar == Float.PositiveInfinity)
  }

  test("distanceLinePoint") {
    // Distance from point (1, 1) to line from (0, 0) to (2, 0) should be 1
    val d = Intersector.distanceLinePoint(0f, 0f, 2f, 0f, 1f, 1f)
    assertEqualsFloat(d, 1f, 0.001f)
  }

  test("distanceSegmentPoint") {
    // Distance from point (1, 1) to segment from (0, 0) to (2, 0) should be 1
    val d = Intersector.distanceSegmentPoint(0f, 0f, 2f, 0f, 1f, 1f)
    assertEqualsFloat(d, 1f, 0.001f)
  }

  test("nearestSegmentPoint") {
    val nearest = new Vector2()
    Intersector.nearestSegmentPoint(new Vector2(0, 0), new Vector2(2, 0), new Vector2(1, 1), nearest)
    assertEqualsFloat(nearest.x, 1f, 0.001f)
    assertEqualsFloat(nearest.y, 0f, 0.001f)
  }

  test("intersectSegmentPlane") {
    val plane        = new Plane(new Vector3(0, 1, 0), new Vector3(0, 0, 0))
    val intersection = new Vector3()
    assert(Intersector.intersectSegmentPlane(new Vector3(0, 5, 0), new Vector3(0, -5, 0), plane, intersection))
    assertEqualsFloat(intersection.y, 0f, 0.001f)
  }

  test("intersectSegmentPlane - no intersection") {
    val plane        = new Plane(new Vector3(0, 1, 0), new Vector3(0, 0, 0))
    val intersection = new Vector3()
    // Both points above the plane
    assert(!Intersector.intersectSegmentPlane(new Vector3(0, 5, 0), new Vector3(0, 10, 0), plane, intersection))
  }

  test("intersectLinePlane") {
    val plane        = new Plane(new Vector3(0, 1, 0), new Vector3(0, 0, 0))
    val intersection = new Vector3()
    val t            = Intersector.intersectLinePlane(0f, 5f, 0f, 0f, -5f, 0f, plane, Nullable(intersection))
    assert(t >= 0 && t <= 1)
    assertEqualsFloat(intersection.y, 0f, 0.001f)
  }

  test("intersectBoundsPlaneFast") {
    val box   = new BoundingBox(new Vector3(-1, -1, -1), new Vector3(1, 1, 1))
    val plane = new Plane(new Vector3(0, 1, 0), new Vector3(0, 0, 0))
    assert(Intersector.intersectBoundsPlaneFast(box, plane))
  }

  test("intersectSegmentRectangle") {
    val rect = new Rectangle(0, 0, 4, 4)
    assert(Intersector.intersectSegmentRectangle(new Vector2(-1, 2), new Vector2(5, 2), rect))
    assert(!Intersector.intersectSegmentRectangle(new Vector2(-1, 5), new Vector2(5, 5), rect))
  }

  test("intersectLinePolygon") {
    val polygon = new Polygon(Array(0f, 0f, 4f, 0f, 4f, 4f, 0f, 4f))
    assert(Intersector.intersectLinePolygon(new Vector2(-1, 2), new Vector2(5, 2), polygon))
  }

  test("intersectSegmentPolygon") {
    val polygon = new Polygon(Array(0f, 0f, 4f, 0f, 4f, 4f, 0f, 4f))
    assert(Intersector.intersectSegmentPolygon(new Vector2(-1, 2), new Vector2(5, 2), polygon))
    assert(!Intersector.intersectSegmentPolygon(new Vector2(-1, 5), new Vector2(5, 5), polygon))
  }
}
