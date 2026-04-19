package sge
package math

class Bresenham2Test extends munit.FunSuite {

  test("horizontal line left to right") {
    val b      = new Bresenham2()
    val points = b.line(0, 0, 3, 0)
    assertEquals(points.length, 4)
    assertEquals(points(0), GridPoint2(0, 0))
    assertEquals(points(1), GridPoint2(1, 0))
    assertEquals(points(2), GridPoint2(2, 0))
    assertEquals(points(3), GridPoint2(3, 0))
  }

  test("horizontal line right to left") {
    val b      = new Bresenham2()
    val points = b.line(3, 0, 0, 0)
    assertEquals(points.length, 4)
    assertEquals(points(0), GridPoint2(3, 0))
    assertEquals(points(3), GridPoint2(0, 0))
  }

  test("vertical line bottom to top") {
    val b      = new Bresenham2()
    val points = b.line(0, 0, 0, 3)
    assertEquals(points.length, 4)
    assertEquals(points(0), GridPoint2(0, 0))
    assertEquals(points(3), GridPoint2(0, 3))
  }

  test("diagonal line") {
    val b      = new Bresenham2()
    val points = b.line(0, 0, 3, 3)
    assertEquals(points.length, 4)
    assertEquals(points(0), GridPoint2(0, 0))
    assertEquals(points(3), GridPoint2(3, 3))
  }

  test("single point line") {
    val b      = new Bresenham2()
    val points = b.line(5, 5, 5, 5)
    assertEquals(points.length, 1)
    assertEquals(points(0), GridPoint2(5, 5))
  }

  test("GridPoint2 overload delegates correctly") {
    val b      = new Bresenham2()
    val points = b.line(GridPoint2(1, 2), GridPoint2(4, 6))
    assert(points.length > 0)
    assertEquals(points(0), GridPoint2(1, 2))
    assertEquals(points.last, GridPoint2(4, 6))
  }

  test("steep line produces correct endpoints") {
    val b      = new Bresenham2()
    val points = b.line(0, 0, 1, 5)
    assertEquals(points(0), GridPoint2(0, 0))
    assertEquals(points.last, GridPoint2(1, 5))
    assertEquals(points.length, 6)
  }
}
