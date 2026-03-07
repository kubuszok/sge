package sge
package math

class RectangleTest extends munit.FunSuite {

  test("toString") {
    // Use exact float values (binary fractions, non-integer) to avoid JVM/JS formatting differences
    assertEquals(new Rectangle(5.5f, -4.5f, 0.25f, -0.125f).toString(), "[5.5,-4.5,0.25,-0.125]")
  }

  test("fromString") {
    assertEquals(new Rectangle().fromString("[5.0,-4.1,0.03,-0.02]"), new Rectangle(5f, -4.1f, 0.03f, -0.02f))
  }
}
