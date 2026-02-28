package sge
package math

class Matrix4Test extends munit.FunSuite {

  private val epsilon = 1e-4f

  private def assertMatrixEquals(a: Matrix4, b: Matrix4, eps: Float = epsilon): Unit =
    for (i <- 0 until 16)
      assert(
        Math.abs(a.values(i) - b.values(i)) < eps,
        s"Matrix mismatch at index $i: ${a.values(i)} vs ${b.values(i)}"
      )

  test("det identity") {
    val m = new Matrix4().idt()
    assertEqualsFloat(m.det(), 1.0f, epsilon)
  }

  test("det singular") {
    val m = new Matrix4() // zero matrix
    for (i <- 0 until 16) m.values(i) = 0f
    assertEqualsFloat(m.det(), 0.0f, epsilon)
  }

  test("inv identity") {
    val m        = new Matrix4().idt().inv()
    val identity = new Matrix4().idt()
    assertMatrixEquals(m, identity)
  }

  test("inv translation") {
    val m = new Matrix4().idt().setTranslation(3, 4, 5)
    m.inv()
    val expected = new Matrix4().idt().setTranslation(-3, -4, -5)
    assertMatrixEquals(m, expected)
  }

  test("inv rotation is transpose") {
    // For an orthogonal matrix (rotation), inverse == transpose
    val m = new Matrix4().idt()
    m.rotate(Vector3(0, 0, 1), 45f)
    val copy = new Matrix4().set(m)
    m.inv()
    val transposed = new Matrix4().set(copy).tra()
    assertMatrixEquals(m, transposed, 1e-3f)
  }

  test("inv arbitrary: M * M.inv() = identity") {
    val m = new Matrix4().idt()
    m.values(Matrix4.M00) = 1; m.values(Matrix4.M01) = 2; m.values(Matrix4.M02) = 3; m.values(Matrix4.M03) = 4
    m.values(Matrix4.M10) = 5; m.values(Matrix4.M11) = 6; m.values(Matrix4.M12) = 7; m.values(Matrix4.M13) = 8
    m.values(Matrix4.M20) = 2; m.values(Matrix4.M21) = 6; m.values(Matrix4.M22) = 4; m.values(Matrix4.M23) = 8
    m.values(Matrix4.M30) = 3; m.values(Matrix4.M31) = 1; m.values(Matrix4.M32) = 7; m.values(Matrix4.M33) = 2
    val mInv     = new Matrix4().set(m).inv()
    val product  = new Matrix4().set(m).mul(mInv)
    val identity = new Matrix4().idt()
    assertMatrixEquals(product, identity, 1e-3f)
  }

  test("inv singular throws") {
    val m = new Matrix4()
    for (i <- 0 until 16) m.values(i) = 0f
    intercept[RuntimeException] {
      m.inv()
    }
  }

  test("setTranslation and getTranslation roundtrip") {
    val m   = new Matrix4().idt().setTranslation(10, 20, 30)
    val pos = m.getTranslation(new Vector3())
    assertEqualsFloat(pos.x, 10f, epsilon)
    assertEqualsFloat(pos.y, 20f, epsilon)
    assertEqualsFloat(pos.z, 30f, epsilon)
  }
}
