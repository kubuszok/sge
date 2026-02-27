package sge
package math

class Shape2DTest extends munit.FunSuite {

  test("circle") {
    val c1 = new Circle(0, 0, 1)
    val c2 = new Circle(0, 0, 1)
    val c3 = new Circle(2, 0, 1)
    val c4 = new Circle(0, 0, 2)

    assert(c1.overlaps(c1))
    assert(c1.overlaps(c2))
    assert(!c1.overlaps(c3))
    assert(c1.overlaps(c4))
    assert(c4.overlaps(c1))
    assert(c1.contains(0, 1))
    assert(!c1.contains(0, 2))
    assert(c1.contains(c1))
    assert(!c1.contains(c4))
    assert(c4.contains(c1))
  }

  test("rectangle") {
    val r1 = new Rectangle(0, 0, 1, 1)
    val r2 = new Rectangle(1, 0, 2, 1)
    assert(r1.overlaps(r1))
    assert(!r1.overlaps(r2))
    assert(r1.contains(0, 0))
  }
}
