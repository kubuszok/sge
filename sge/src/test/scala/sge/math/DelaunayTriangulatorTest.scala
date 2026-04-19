package sge
package math

class DelaunayTriangulatorTest extends munit.FunSuite {

  test("triangle from three points") {
    // Three non-collinear points forming a simple triangle
    val dt     = new DelaunayTriangulator()
    val points = Array(0f, 0f, 4f, 0f, 2f, 3f)
    val result = dt.computeTriangles(points, false)
    // Should produce exactly one triangle (3 indices)
    assertEquals(result.size, 3)
    // The indices should be 0, 1, 2 in some order
    val indices = (0 until result.size).map(result(_).toInt).toSet
    assertEquals(indices, Set(0, 1, 2))
  }

  test("square produces two triangles") {
    val dt     = new DelaunayTriangulator()
    val points = Array(0f, 0f, 4f, 0f, 4f, 4f, 0f, 4f)
    val result = dt.computeTriangles(points, false)
    // A square should be divided into 2 triangles = 6 indices
    assertEquals(result.size, 6)
    // All indices should be in [0, 3]
    for (i <- 0 until result.size)
      assert(result(i) >= 0 && result(i) <= 3, s"Index ${result(i)} out of range")
  }

  test("fewer than 3 points returns empty") {
    val dt     = new DelaunayTriangulator()
    val points = Array(0f, 0f, 1f, 1f) // 2 points = 4 floats
    val result = dt.computeTriangles(points, false)
    assertEquals(result.size, 0)
  }

  test("five points produces valid triangulation") {
    val dt     = new DelaunayTriangulator()
    val points = Array(0f, 0f, 4f, 0f, 4f, 4f, 0f, 4f, 2f, 2f)
    val result = dt.computeTriangles(points, false)
    // 5 points should produce at least 3 triangles
    assert(result.size >= 9, s"Expected at least 9 indices (3 triangles), got ${result.size}")
    assertEquals(result.size % 3, 0)
  }

  test("sorted flag produces valid result") {
    // Pre-sort by x coordinate
    val dt     = new DelaunayTriangulator()
    val points = Array(0f, 0f, 2f, 3f, 4f, 0f)
    val result = dt.computeTriangles(points, true)
    assertEquals(result.size, 3)
  }
}
