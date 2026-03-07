package sge
package math

class Vector4Test extends munit.FunSuite {

  test("toString") {
    // Use exact float values (binary fractions, non-integer) to avoid JVM/JS formatting differences
    assertEquals(new Vector4(-5.5f, 42.5f, 0.25f, -1.75f).toString(), "(-5.5,42.5,0.25,-1.75)")
  }

  test("fromString") {
    assertEquals(new Vector4().fromString("(-5,42.00055,44444.32,-1.9750)"), new Vector4(-5f, 42.00055f, 44444.32f, -1.975f))
  }
}
