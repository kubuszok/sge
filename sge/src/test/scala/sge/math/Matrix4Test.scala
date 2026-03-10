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

  test("mulLeft stores result") {
    // Regression: mulLeft() computed result but never stored it back
    val a = new Matrix4().idt().setTranslation(1, 2, 3)
    val b = new Matrix4().idt().setTranslation(10, 20, 30)
    a.mulLeft(b)
    val pos = a.getTranslation(new Vector3())
    assertEqualsFloat(pos.x, 11f, epsilon)
    assertEqualsFloat(pos.y, 22f, epsilon)
    assertEqualsFloat(pos.z, 33f, epsilon)
  }

  test("mulLeft vs mul order") {
    // A.mulLeft(B) should give B*A (different from A.mul(B) = A*B)
    val scale = new Matrix4().idt().scale(2f, 2f, 2f)
    val trans = new Matrix4().idt().setTranslation(5, 0, 0)
    // scale.mulLeft(trans) = trans * scale: first scale, then translate
    val result = new Matrix4().set(scale).mulLeft(trans)
    val v      = new Vector3(1, 0, 0)
    v.mul(result)
    // (1,0,0) * scale(2) = (2,0,0), then + translate(5,0,0) = (7,0,0)
    assertEqualsFloat(v.x, 7f, epsilon)
  }

  // ---------------------------------------------------------------------------
  // Rotation
  // ---------------------------------------------------------------------------

  test("rotate 90 degrees around Z maps X to Y") {
    val m = new Matrix4().idt().rotate(Vector3(0, 0, 1), 90f)
    val v = new Vector3(1, 0, 0)
    v.rot(m)
    assertEqualsFloat(v.x, 0f, 1e-3f)
    assertEqualsFloat(v.y, 1f, 1e-3f)
    assertEqualsFloat(v.z, 0f, 1e-3f)
  }

  test("rotate 180 degrees around Y maps Z to -Z") {
    val m = new Matrix4().idt().rotate(Vector3(0, 1, 0), 180f)
    val v = new Vector3(0, 0, 1)
    v.rot(m)
    assertEqualsFloat(v.x, 0f, 1e-3f)
    assertEqualsFloat(v.y, 0f, 1e-3f)
    assertEqualsFloat(v.z, -1f, 1e-3f)
  }

  test("setToRotation and getRotation roundtrip") {
    val q  = new Quaternion().setEulerAngles(45f, 30f, 0f)
    val m  = new Matrix4().idt().set(q)
    val q2 = new Quaternion()
    m.getRotation(q2)
    // Quaternion may be negated (represents same rotation)
    val dot = q.x * q2.x + q.y * q2.y + q.z * q2.z + q.w * q2.w
    assert(Math.abs(Math.abs(dot) - 1f) < 1e-3f, s"Quaternion dot product: $dot")
  }

  // ---------------------------------------------------------------------------
  // Scale
  // ---------------------------------------------------------------------------

  test("scale modifies matrix correctly") {
    val m = new Matrix4().idt().scale(2f, 3f, 4f)
    val v = new Vector3(1, 1, 1)
    v.mul(m)
    assertEqualsFloat(v.x, 2f, epsilon)
    assertEqualsFloat(v.y, 3f, epsilon)
    assertEqualsFloat(v.z, 4f, epsilon)
  }

  test("getScale extracts scale factors") {
    val m     = new Matrix4().idt().scale(2f, 3f, 4f)
    val scale = m.getScale(new Vector3())
    assertEqualsFloat(scale.x, 2f, epsilon)
    assertEqualsFloat(scale.y, 3f, epsilon)
    assertEqualsFloat(scale.z, 4f, epsilon)
  }

  // ---------------------------------------------------------------------------
  // Compose: translate + rotate + scale
  // ---------------------------------------------------------------------------

  test("TRS decomposition roundtrip") {
    // Build matrix from translate, rotate, scale
    val pos   = new Vector3(10, 20, 30)
    val rot   = new Quaternion().setEulerAngles(45f, 0f, 0f)
    val scale = new Vector3(2, 2, 2)

    val m = new Matrix4().set(pos, rot, scale)

    // Extract back
    val ePos   = m.getTranslation(new Vector3())
    val eScale = m.getScale(new Vector3())
    val eRot   = new Quaternion()
    m.getRotation(eRot)

    assertEqualsFloat(ePos.x, 10f, 1e-3f)
    assertEqualsFloat(ePos.y, 20f, 1e-3f)
    assertEqualsFloat(ePos.z, 30f, 1e-3f)
    assertEqualsFloat(eScale.x, 2f, 1e-3f)
    assertEqualsFloat(eScale.y, 2f, 1e-3f)
    assertEqualsFloat(eScale.z, 2f, 1e-3f)
  }

  test("transpose of identity is identity") {
    val m = new Matrix4().idt().tra()
    assertMatrixEquals(m, new Matrix4().idt())
  }

  test("det property: det(A * B) = det(A) * det(B)") {
    val a = new Matrix4().idt()
    a.values(Matrix4.M00) = 1; a.values(Matrix4.M01) = 2; a.values(Matrix4.M02) = 0; a.values(Matrix4.M03) = 0
    a.values(Matrix4.M10) = 3; a.values(Matrix4.M11) = 4; a.values(Matrix4.M12) = 0; a.values(Matrix4.M13) = 0
    a.values(Matrix4.M20) = 0; a.values(Matrix4.M21) = 0; a.values(Matrix4.M22) = 1; a.values(Matrix4.M23) = 0
    a.values(Matrix4.M30) = 0; a.values(Matrix4.M31) = 0; a.values(Matrix4.M32) = 0; a.values(Matrix4.M33) = 1

    val b = new Matrix4().idt()
    b.values(Matrix4.M00) = 5; b.values(Matrix4.M01) = 6; b.values(Matrix4.M02) = 0; b.values(Matrix4.M03) = 0
    b.values(Matrix4.M10) = 7; b.values(Matrix4.M11) = 8; b.values(Matrix4.M12) = 0; b.values(Matrix4.M13) = 0

    val ab = new Matrix4().set(a).mul(b)
    assertEqualsFloat(ab.det(), a.det() * b.det(), 1e-2f)
  }
}
