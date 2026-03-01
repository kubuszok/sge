// SGE Native Ops — BufferOps shared test suite
//
// Tests run on all platforms (JVM, JS, Native) using the platform-specific
// BufferOps implementation provided by PlatformOps.

package sge
package platform

class BufferOpsSuite extends munit.FunSuite {

  val ops: BufferOps = PlatformOps.buffer

  private val Eps = 1e-6f

  // ─── Identity matrices ─────────────────────────────────────────────────

  // 4x4 identity matrix (column-major)
  private val identity4x4: Array[Float] = Array(
    1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1
  )

  // 3x3 identity matrix (column-major)
  private val identity3x3: Array[Float] = Array(
    1, 0, 0, 0, 1, 0, 0, 0, 1
  )

  // Translation matrix (translate by 10, 20, 30) — column-major 4x4
  private val translate10_20_30: Array[Float] = Array(
    1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 10, 20, 30, 1
  )

  // Scale matrix (scale by 2, 3, 4) — column-major 3x3
  private val scale2_3_4: Array[Float] = Array(
    2, 0, 0, 0, 3, 0, 0, 0, 4
  )

  private def assertFloatsEqual(actual: Float, expected: Float, msg: String = ""): Unit =
    assert(
      Math.abs(actual - expected) < Eps,
      s"$msg expected=$expected, actual=$actual, diff=${Math.abs(actual - expected)}"
    )

  // ─── Copy ─────────────────────────────────────────────────────────────

  test("copy bytes round-trip") {
    val src = Array[Byte](1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val dst = new Array[Byte](10)
    ops.copy(src, 2, dst, 3, 5)
    // dst[3..8] should be src[2..7] = {3, 4, 5, 6, 7}
    assertEquals(dst(3), 3.toByte)
    assertEquals(dst(4), 4.toByte)
    assertEquals(dst(5), 5.toByte)
    assertEquals(dst(6), 6.toByte)
    assertEquals(dst(7), 7.toByte)
    // Surrounding bytes unchanged
    assertEquals(dst(0), 0.toByte)
    assertEquals(dst(1), 0.toByte)
    assertEquals(dst(2), 0.toByte)
    assertEquals(dst(8), 0.toByte)
    assertEquals(dst(9), 0.toByte)
  }

  test("copy floats round-trip") {
    val src = Array[Float](1.5f, 2.5f, 3.5f, 4.5f, 5.5f)
    val dst = new Array[Float](5)
    ops.copy(src, 1, dst, 0, 3)
    // dst[0..3] should be src[1..4] = {2.5, 3.5, 4.5}
    assertFloatsEqual(dst(0), 2.5f)
    assertFloatsEqual(dst(1), 3.5f)
    assertFloatsEqual(dst(2), 4.5f)
    // Remaining unchanged
    assertFloatsEqual(dst(3), 0.0f)
    assertFloatsEqual(dst(4), 0.0f)
  }

  test("copy bytes full array") {
    val src = Array[Byte](10, 20, 30, 40, 50)
    val dst = new Array[Byte](5)
    ops.copy(src, 0, dst, 0, 5)
    var i = 0
    while (i < 5) {
      assertEquals(dst(i), src(i))
      i += 1
    }
  }

  // ─── Transform V4M4 ──────────────────────────────────────────────────

  test("transformV4M4 identity") {
    val data = Array[Float](1.0f, 2.0f, 3.0f, 1.0f)
    ops.transformV4M4(data, 4, 1, identity4x4, 0)
    assertFloatsEqual(data(0), 1.0f, "x")
    assertFloatsEqual(data(1), 2.0f, "y")
    assertFloatsEqual(data(2), 3.0f, "z")
    assertFloatsEqual(data(3), 1.0f, "w")
  }

  test("transformV4M4 translation") {
    val data = Array[Float](1.0f, 2.0f, 3.0f, 1.0f)
    ops.transformV4M4(data, 4, 1, translate10_20_30, 0)
    assertFloatsEqual(data(0), 11.0f, "x")
    assertFloatsEqual(data(1), 22.0f, "y")
    assertFloatsEqual(data(2), 33.0f, "z")
    assertFloatsEqual(data(3), 1.0f, "w")
  }

  // ─── Transform V3M4 ──────────────────────────────────────────────────

  test("transformV3M4 identity") {
    val data = Array[Float](5.0f, 6.0f, 7.0f)
    ops.transformV3M4(data, 3, 1, identity4x4, 0)
    assertFloatsEqual(data(0), 5.0f, "x")
    assertFloatsEqual(data(1), 6.0f, "y")
    assertFloatsEqual(data(2), 7.0f, "z")
  }

  test("transformV3M4 known values (translation)") {
    // Vec3 (1, 2, 3) x translate(10, 20, 30) = (11, 22, 33) with implicit w=1
    val data = Array[Float](1.0f, 2.0f, 3.0f)
    ops.transformV3M4(data, 3, 1, translate10_20_30, 0)
    assertFloatsEqual(data(0), 11.0f, "x")
    assertFloatsEqual(data(1), 22.0f, "y")
    assertFloatsEqual(data(2), 33.0f, "z")
  }

  // ─── Transform V2M4 ──────────────────────────────────────────────────

  test("transformV2M4 identity") {
    val data = Array[Float](3.0f, 4.0f)
    ops.transformV2M4(data, 2, 1, identity4x4, 0)
    assertFloatsEqual(data(0), 3.0f, "x")
    assertFloatsEqual(data(1), 4.0f, "y")
  }

  test("transformV2M4 translation") {
    // Vec2 (1, 2) x translate(10, 20, 30) = (11, 22) with implicit z=0, w=1
    val data = Array[Float](1.0f, 2.0f)
    ops.transformV2M4(data, 2, 1, translate10_20_30, 0)
    assertFloatsEqual(data(0), 11.0f, "x")
    assertFloatsEqual(data(1), 22.0f, "y")
  }

  // ─── Transform V3M3 ──────────────────────────────────────────────────

  test("transformV3M3 identity") {
    val data = Array[Float](1.0f, 2.0f, 3.0f)
    ops.transformV3M3(data, 3, 1, identity3x3, 0)
    assertFloatsEqual(data(0), 1.0f, "x")
    assertFloatsEqual(data(1), 2.0f, "y")
    assertFloatsEqual(data(2), 3.0f, "z")
  }

  test("transformV3M3 scale") {
    // Vec3 (1, 2, 3) x scale(2, 3, 4) = (2, 6, 12)
    val data = Array[Float](1.0f, 2.0f, 3.0f)
    ops.transformV3M3(data, 3, 1, scale2_3_4, 0)
    assertFloatsEqual(data(0), 2.0f, "x")
    assertFloatsEqual(data(1), 6.0f, "y")
    assertFloatsEqual(data(2), 12.0f, "z")
  }

  // ─── Transform V2M3 ──────────────────────────────────────────────────

  test("transformV2M3 identity") {
    val data = Array[Float](5.0f, 7.0f)
    ops.transformV2M3(data, 2, 1, identity3x3, 0)
    assertFloatsEqual(data(0), 5.0f, "x")
    assertFloatsEqual(data(1), 7.0f, "y")
  }

  test("transformV2M3 translation via 3x3") {
    // 3x3 matrix that translates by (10, 20):
    // [1, 0, 0,  0, 1, 0,  10, 20, 1] (column-major)
    // For V2M3: result.x = x*m0 + y*m3 + m6, result.y = x*m1 + y*m4 + m7
    val translate2d: Array[Float] = Array(
      1, 0, 0, 0, 1, 0, 10, 20, 1
    )
    val data = Array[Float](3.0f, 4.0f)
    ops.transformV2M3(data, 2, 1, translate2d, 0)
    assertFloatsEqual(data(0), 13.0f, "x")
    assertFloatsEqual(data(1), 24.0f, "y")
  }

  // ─── Multi-vertex transform with stride ────────────────────────────────

  test("multi-vertex transform with stride") {
    // 3 vertices with stride 5 (3 position floats + 2 extra floats)
    // Layout: [x, y, z, u, v, x, y, z, u, v, x, y, z, u, v]
    val data = Array[Float](
      1.0f, 0.0f, 0.0f, 0.1f, 0.2f, 0.0f, 1.0f, 0.0f, 0.3f, 0.4f, 0.0f, 0.0f, 1.0f, 0.5f, 0.6f
    )
    ops.transformV3M4(data, 5, 3, translate10_20_30, 0)

    // Vertex 0: (1,0,0) -> (11, 20, 30)
    assertFloatsEqual(data(0), 11.0f, "v0.x")
    assertFloatsEqual(data(1), 20.0f, "v0.y")
    assertFloatsEqual(data(2), 30.0f, "v0.z")
    // UV should be untouched
    assertFloatsEqual(data(3), 0.1f, "v0.u")
    assertFloatsEqual(data(4), 0.2f, "v0.v")

    // Vertex 1: (0,1,0) -> (10, 21, 30)
    assertFloatsEqual(data(5), 10.0f, "v1.x")
    assertFloatsEqual(data(6), 21.0f, "v1.y")
    assertFloatsEqual(data(7), 30.0f, "v1.z")
    assertFloatsEqual(data(8), 0.3f, "v1.u")
    assertFloatsEqual(data(9), 0.4f, "v1.v")

    // Vertex 2: (0,0,1) -> (10, 20, 31)
    assertFloatsEqual(data(10), 10.0f, "v2.x")
    assertFloatsEqual(data(11), 20.0f, "v2.y")
    assertFloatsEqual(data(12), 31.0f, "v2.z")
    assertFloatsEqual(data(13), 0.5f, "v2.u")
    assertFloatsEqual(data(14), 0.6f, "v2.v")
  }

  test("transformV3M4 with offset") {
    // Offset of 2 floats — vertices start at index 2
    val data = Array[Float](
      99.0f, 99.0f, // padding
      1.0f, 2.0f, 3.0f // the vertex
    )
    ops.transformV3M4(data, 3, 1, translate10_20_30, 2)
    // Padding should be untouched
    assertFloatsEqual(data(0), 99.0f, "pad0")
    assertFloatsEqual(data(1), 99.0f, "pad1")
    // Vertex should be translated
    assertFloatsEqual(data(2), 11.0f, "x")
    assertFloatsEqual(data(3), 22.0f, "y")
    assertFloatsEqual(data(4), 33.0f, "z")
  }

  // ─── Find ─────────────────────────────────────────────────────────────

  test("find exact match") {
    val vertex   = Array[Float](3.0f, 4.0f, 5.0f)
    val vertices = Array[Float](
      1.0f, 2.0f, 3.0f, // vertex 0
      3.0f, 4.0f, 5.0f, // vertex 1 — match
      6.0f, 7.0f, 8.0f // vertex 2
    )
    assertEquals(ops.find(vertex, 0, 3, vertices, 0, 3), 1L)
  }

  test("find not found") {
    val vertex   = Array[Float](9.0f, 9.0f, 9.0f)
    val vertices = Array[Float](
      1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f
    )
    assertEquals(ops.find(vertex, 0, 3, vertices, 0, 3), -1L)
  }

  test("find first match among duplicates") {
    val vertex   = Array[Float](1.0f, 2.0f)
    val vertices = Array[Float](
      1.0f, 2.0f, // vertex 0 — first match
      3.0f, 4.0f, 1.0f, 2.0f // vertex 2 — also matches
    )
    assertEquals(ops.find(vertex, 0, 2, vertices, 0, 3), 0L)
  }

  test("find with vertex offset") {
    // Search vertex starts at offset 1 in the vertex array
    val vertex   = Array[Float](99.0f, 3.0f, 4.0f, 5.0f)
    val vertices = Array[Float](
      1.0f, 2.0f, 3.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f
    )
    assertEquals(ops.find(vertex, 1, 3, vertices, 0, 3), 1L)
  }

  test("find epsilon match") {
    val vertex   = Array[Float](1.0f, 2.0f, 3.0f)
    val vertices = Array[Float](
      1.01f, 2.01f, 3.01f, 5.0f, 6.0f, 7.0f
    )
    // Should find with epsilon 0.02, since diff = 0.01 per component
    assertEquals(ops.find(vertex, 0, 3, vertices, 0, 2, 0.02f), 0L)
  }

  test("find epsilon not found") {
    val vertex   = Array[Float](1.0f, 2.0f, 3.0f)
    val vertices = Array[Float](
      1.5f, 2.5f, 3.5f, 4.0f, 5.0f, 6.0f
    )
    // Should not find with epsilon 0.1, since diff = 0.5 per component
    assertEquals(ops.find(vertex, 0, 3, vertices, 0, 2, 0.1f), -1L)
  }

  test("find empty search") {
    val vertex   = Array[Float](1.0f, 2.0f)
    val vertices = new Array[Float](0)
    assertEquals(ops.find(vertex, 0, 2, vertices, 0, 0), -1L)
  }
}
