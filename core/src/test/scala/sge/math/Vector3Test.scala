package sge
package math

class Vector3Test extends munit.FunSuite {

  test("toString") {
    assertEquals(new Vector3(-5f, 42.00055f, 44444.32f).toString(), "(-5.0,42.00055,44444.32)")
  }

  test("fromString") {
    assertEquals(new Vector3().fromString("(-5,42.00055,44444.32)"), new Vector3(-5f, 42.00055f, 44444.32f))
  }
}
