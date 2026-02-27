package sge
package math

class Vector4Test extends munit.FunSuite {

  test("toString") {
    assertEquals(new Vector4(-5f, 42.00055f, 44444.32f, -1.975f).toString(), "(-5.0,42.00055,44444.32,-1.975)")
  }

  test("fromString") {
    assertEquals(new Vector4().fromString("(-5,42.00055,44444.32,-1.9750)"), new Vector4(-5f, 42.00055f, 44444.32f, -1.975f))
  }
}
