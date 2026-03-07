package sge
package math

class Vector3Test extends munit.FunSuite {

  test("toString") {
    // Use exact float values (binary fractions, non-integer) to avoid JVM/JS formatting differences
    assertEquals(new Vector3(-5.5f, 42.5f, 0.25f).toString(), "(-5.5,42.5,0.25)")
  }

  test("fromString") {
    assertEquals(new Vector3().fromString("(-5,42.00055,44444.32)"), new Vector3(-5f, 42.00055f, 44444.32f))
  }
}
