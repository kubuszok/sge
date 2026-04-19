package sge
package math

class PolylineTest extends munit.FunSuite {

  private val eps = 0.01

  test("constructor requires at least 2 points") {
    intercept[IllegalArgumentException] {
      new Polyline(Array(1f, 2f)) // Only 1 point = 2 floats, need at least 4
    }
  }

  test("vertices returns original array") {
    val verts = Array(0f, 0f, 3f, 4f)
    val pl    = new Polyline(verts)
    assert(pl.vertices eq verts)
  }

  test("setPosition and getters") {
    val pl = new Polyline(Array(0f, 0f, 1f, 0f))
    pl.setPosition(5f, 10f)
    assertEqualsDouble(pl.x.toDouble, 5.0, eps)
    assertEqualsDouble(pl.y.toDouble, 10.0, eps)
  }

  test("transformedVertices with no transform matches local") {
    val verts = Array(0f, 0f, 3f, 4f)
    val pl    = new Polyline(verts)
    val tv    = pl.transformedVertices
    for (i <- verts.indices)
      assertEqualsDouble(tv(i).toDouble, verts(i).toDouble, eps)
  }

  test("transformedVertices with position offset") {
    val verts = Array(0f, 0f, 1f, 0f)
    val pl    = new Polyline(verts)
    pl.setPosition(10f, 20f)
    val tv = pl.transformedVertices
    assertEqualsDouble(tv(0).toDouble, 10.0, eps)
    assertEqualsDouble(tv(1).toDouble, 20.0, eps)
    assertEqualsDouble(tv(2).toDouble, 11.0, eps)
    assertEqualsDouble(tv(3).toDouble, 20.0, eps)
  }

  test("length of horizontal segment") {
    val pl = new Polyline(Array(0f, 0f, 3f, 0f))
    assertEqualsDouble(pl.length.toDouble, 3.0, eps)
  }

  test("length of 3-4-5 path") {
    // Two segments: (0,0)->(3,0) length 3, then (3,0)->(3,4) length 4. Total = 7
    val pl = new Polyline(Array(0f, 0f, 3f, 0f, 3f, 4f))
    assertEqualsDouble(pl.length.toDouble, 7.0, eps)
  }

  test("scaledLength with scale factor") {
    val pl = new Polyline(Array(0f, 0f, 2f, 0f))
    pl.setScale(3f, 1f)
    // Scaled length along x: 2 * 3 = 6
    assertEqualsDouble(pl.scaledLength.toDouble, 6.0, eps)
  }

  test("rotation") {
    val pl = new Polyline(Array(0f, 0f, 1f, 0f))
    pl.setRotation(90f)
    assertEqualsDouble(pl.rotation.toDouble, 90.0, eps)

    pl.rotate(45f)
    assertEqualsDouble(pl.rotation.toDouble, 135.0, eps)
  }

  test("setOrigin") {
    val pl = new Polyline(Array(0f, 0f, 1f, 0f))
    pl.setOrigin(0.5f, 0.5f)
    assertEqualsDouble(pl.originX.toDouble, 0.5, eps)
    assertEqualsDouble(pl.originY.toDouble, 0.5, eps)
  }

  test("translate") {
    val pl = new Polyline(Array(0f, 0f, 1f, 0f))
    pl.setPosition(1f, 2f)
    pl.translate(3f, 4f)
    assertEqualsDouble(pl.x.toDouble, 4.0, eps)
    assertEqualsDouble(pl.y.toDouble, 6.0, eps)
  }

  test("boundingRectangle") {
    val pl   = new Polyline(Array(0f, 0f, 4f, 0f, 4f, 3f))
    val rect = pl.boundingRectangle
    assertEqualsDouble(rect.x.toDouble, 0.0, eps)
    assertEqualsDouble(rect.y.toDouble, 0.0, eps)
    assertEqualsDouble(rect.width.toDouble, 4.0, eps)
    assertEqualsDouble(rect.height.toDouble, 3.0, eps)
  }

  test("contains always returns false") {
    val pl = new Polyline(Array(0f, 0f, 4f, 0f, 4f, 3f, 0f, 3f))
    assert(!pl.contains(2f, 1.5f))
    assert(!pl.contains(new Vector2(2f, 1.5f)))
  }

  test("setVertices validates minimum length") {
    val pl = new Polyline(Array(0f, 0f, 1f, 0f))
    intercept[IllegalArgumentException] {
      pl.setVertices(Array(1f, 2f))
    }
  }

  test("scale adds to existing scale") {
    val pl = new Polyline(Array(0f, 0f, 1f, 0f))
    assertEqualsDouble(pl.scaleX.toDouble, 1.0, eps)
    assertEqualsDouble(pl.scaleY.toDouble, 1.0, eps)
    pl.scale(2f)
    assertEqualsDouble(pl.scaleX.toDouble, 3.0, eps)
    assertEqualsDouble(pl.scaleY.toDouble, 3.0, eps)
  }
}
