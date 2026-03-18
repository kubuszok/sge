package sge
package math

class PolygonTest extends munit.FunSuite {

  test("zeroRotation") {
    val vertices = Array(0f, 0f, 3f, 0f, 3f, 4f)
    val polygon  = new Polygon(vertices)
    polygon.rotate(0)
    val transformed = polygon.transformedVertices
    val original    = polygon.vertices
    for (i <- transformed.indices)
      assertEqualsDouble(transformed(i).toDouble, original(i).toDouble, 1.0)
  }

  test("360Rotation") {
    val vertices = Array(0f, 0f, 3f, 0f, 3f, 4f)
    val polygon  = new Polygon(vertices)
    polygon.rotate(360)
    val transformed = polygon.transformedVertices
    val original    = polygon.vertices
    for (i <- transformed.indices)
      assertEqualsDouble(transformed(i).toDouble, original(i).toDouble, 1.0)
  }

  test("concavePolygonArea") {
    val concaveVertices = Array(0f, 0f, 2f, 4f, 4f, 0f, 2f, 2f)
    val concavePolygon  = new Polygon(concaveVertices)
    val expectedArea    = 4.0f
    assertEqualsDouble(Math.abs(concavePolygon.area().toDouble), expectedArea.toDouble, 1.0)
  }

  test("triangleArea") {
    val triangleVertices = Array(0f, 0f, 2f, 3f, 4f, 0f)
    val triangle         = new Polygon(triangleVertices)
    val expectedArea     = 6.0f
    assertEqualsDouble(Math.abs(triangle.area().toDouble), expectedArea.toDouble, 1.0)
  }
}
