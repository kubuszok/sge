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
    // Exact Bresenham sequence for (1,2)→(4,6) (Bresenham2.java:65-108).
    // w=3, h=4 → h is longest (dominant axis Y): dx1=dy1=1, dx2=0, dy2=1,
    // shortest2=6, longest2=8. numerator trace over i=0..4:
    //   (1,2) num 6>4 → -2, step dx1/dy1 → (2,3)
    //   (2,3) num 4≯4 → step dx2/dy2 → (2,4)
    //   (2,4) num 10>4 → 2, step dx1/dy1 → (3,5)
    //   (3,5) num 8>4 → 0, step dx1/dy1 → (4,6)
    //   (4,6) emitted. Full sequence: (1,2),(2,3),(2,4),(3,5),(4,6).
    // ISS-724 c4, wave 2026-07-18-G territory G3.
    assertEquals(points.length, 5)
    assertEquals(points(0), GridPoint2(1, 2))
    assertEquals(points(1), GridPoint2(2, 3))
    assertEquals(points(2), GridPoint2(2, 4))
    assertEquals(points(3), GridPoint2(3, 5))
    assertEquals(points(4), GridPoint2(4, 6))
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
