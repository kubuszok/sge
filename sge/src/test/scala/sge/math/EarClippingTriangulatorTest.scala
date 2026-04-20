package sge
package math

class EarClippingTriangulatorTest extends munit.FunSuite {

  test("triangle returns single triangle") {
    val ect      = new EarClippingTriangulator()
    val vertices = Array(0f, 0f, 4f, 0f, 2f, 3f)
    val result   = ect.computeTriangles(vertices)
    assertEquals(result.size, 3)
  }

  test("square produces two triangles") {
    val ect      = new EarClippingTriangulator()
    val vertices = Array(0f, 0f, 4f, 0f, 4f, 4f, 0f, 4f)
    val result   = ect.computeTriangles(vertices)
    // 4 vertices -> 2 triangles = 6 indices
    assertEquals(result.size, 6)
  }

  test("convex pentagon produces three triangles") {
    val ect = new EarClippingTriangulator()
    // Regular pentagon-like shape
    val vertices = Array(
      2f, 0f, 4f, 1.5f, 3f, 4f, 1f, 4f, 0f, 1.5f
    )
    val result = ect.computeTriangles(vertices)
    // 5 vertices -> 3 triangles = 9 indices
    assertEquals(result.size, 9)
  }

  test("concave polygon produces valid triangulation") {
    val ect = new EarClippingTriangulator()
    // L-shaped concave polygon
    val vertices = Array(
      0f, 0f, 2f, 0f, 2f, 1f, 1f, 1f, 1f, 2f, 0f, 2f
    )
    val result = ect.computeTriangles(vertices)
    // 6 vertices -> 4 triangles = 12 indices
    assertEquals(result.size, 12)
    // All indices should reference valid vertices (0 to 5)
    for (i <- 0 until result.size)
      assert(result(i) >= 0 && result(i) <= 5, s"Index ${result(i)} out of range")
  }

  test("offset and count parameters") {
    val ect = new EarClippingTriangulator()
    // Embed a triangle in a larger array
    val vertices = Array(99f, 99f, 0f, 0f, 4f, 0f, 2f, 3f, 99f, 99f)
    val result   = ect.computeTriangles(vertices, 2, 6)
    assertEquals(result.size, 3)
  }
}
