package sge
package gltf
package loaders
package shared

import sge.graphics.GL20
import sge.gltf.data.data.GLTFAccessor
import sge.gltf.loaders.exceptions.GLTFIllegalException
import sge.gltf.loaders.shared.animation.Interpolation
import sge.math.{ Quaternion, Vector3 }
import sge.utils.Nullable

class GLTFTypesSuite extends munit.FunSuite {

  // ── Constants ────────────────────────────────────────────────────────

  test("type constants have expected values") {
    assertEquals(GLTFTypes.TYPE_SCALAR, "SCALAR")
    assertEquals(GLTFTypes.TYPE_VEC2, "VEC2")
    assertEquals(GLTFTypes.TYPE_VEC3, "VEC3")
    assertEquals(GLTFTypes.TYPE_VEC4, "VEC4")
    assertEquals(GLTFTypes.TYPE_MAT2, "MAT2")
    assertEquals(GLTFTypes.TYPE_MAT3, "MAT3")
    assertEquals(GLTFTypes.TYPE_MAT4, "MAT4")
  }

  test("component type constants") {
    assertEquals(GLTFTypes.C_BYTE, 5120)
    assertEquals(GLTFTypes.C_UBYTE, 5121)
    assertEquals(GLTFTypes.C_SHORT, 5122)
    assertEquals(GLTFTypes.C_USHORT, 5123)
    assertEquals(GLTFTypes.C_UINT, 5125)
    assertEquals(GLTFTypes.C_FLOAT, 5126)
  }

  // ── mapPrimitiveMode ─────────────────────────────────────────────────

  test("mapPrimitiveMode: null defaults to GL_TRIANGLES") {
    assertEquals(GLTFTypes.mapPrimitiveMode(Nullable.empty), GL20.GL_TRIANGLES)
  }

  test("mapPrimitiveMode: maps known modes") {
    assertEquals(GLTFTypes.mapPrimitiveMode(Nullable(0)), GL20.GL_POINTS)
    assertEquals(GLTFTypes.mapPrimitiveMode(Nullable(1)), GL20.GL_LINES)
    assertEquals(GLTFTypes.mapPrimitiveMode(Nullable(4)), GL20.GL_TRIANGLES)
    assertEquals(GLTFTypes.mapPrimitiveMode(Nullable(5)), GL20.GL_TRIANGLE_STRIP)
    assertEquals(GLTFTypes.mapPrimitiveMode(Nullable(6)), GL20.GL_TRIANGLE_FAN)
  }

  test("mapPrimitiveMode: throws on invalid mode") {
    intercept[GLTFIllegalException] {
      GLTFTypes.mapPrimitiveMode(Nullable(99))
    }
  }

  // ── accessorTypeSize ─────────────────────────────────────────────────

  test("accessorTypeSize: maps accessor types to element counts") {
    def mkAccessor(t: String): GLTFAccessor = {
      val a = new GLTFAccessor()
      a.`type` = Nullable(t)
      a
    }
    assertEquals(GLTFTypes.accessorTypeSize(mkAccessor("SCALAR")), 1)
    assertEquals(GLTFTypes.accessorTypeSize(mkAccessor("VEC2")), 2)
    assertEquals(GLTFTypes.accessorTypeSize(mkAccessor("VEC3")), 3)
    assertEquals(GLTFTypes.accessorTypeSize(mkAccessor("VEC4")), 4)
    assertEquals(GLTFTypes.accessorTypeSize(mkAccessor("MAT2")), 4)
    assertEquals(GLTFTypes.accessorTypeSize(mkAccessor("MAT3")), 9)
    assertEquals(GLTFTypes.accessorTypeSize(mkAccessor("MAT4")), 16)
  }

  test("accessorTypeSize: throws on null type") {
    val a = new GLTFAccessor()
    intercept[GLTFIllegalException] {
      GLTFTypes.accessorTypeSize(a)
    }
  }

  // ── accessorComponentTypeSize ─────────────────────────────────────────

  test("accessorComponentTypeSize: maps component types to byte sizes") {
    def mkAccessor(ct: Int): GLTFAccessor = {
      val a = new GLTFAccessor()
      a.componentType = ct
      a
    }
    assertEquals(GLTFTypes.accessorComponentTypeSize(mkAccessor(GLTFTypes.C_BYTE)), 1)
    assertEquals(GLTFTypes.accessorComponentTypeSize(mkAccessor(GLTFTypes.C_UBYTE)), 1)
    assertEquals(GLTFTypes.accessorComponentTypeSize(mkAccessor(GLTFTypes.C_SHORT)), 2)
    assertEquals(GLTFTypes.accessorComponentTypeSize(mkAccessor(GLTFTypes.C_USHORT)), 2)
    assertEquals(GLTFTypes.accessorComponentTypeSize(mkAccessor(GLTFTypes.C_UINT)), 4)
    assertEquals(GLTFTypes.accessorComponentTypeSize(mkAccessor(GLTFTypes.C_FLOAT)), 4)
  }

  // ── Vector/Quaternion mapping ─────────────────────────────────────────

  test("map Vector3 from float array") {
    val v = new Vector3()
    GLTFTypes.map(v, Array(1f, 2f, 3f))
    assertEqualsFloat(v.x, 1f, 0.001f)
    assertEqualsFloat(v.y, 2f, 0.001f)
    assertEqualsFloat(v.z, 3f, 0.001f)
  }

  test("map Vector3 from float array with offset") {
    val v = new Vector3()
    GLTFTypes.map(v, Array(0f, 0f, 4f, 5f, 6f), 2)
    assertEqualsFloat(v.x, 4f, 0.001f)
    assertEqualsFloat(v.y, 5f, 0.001f)
    assertEqualsFloat(v.z, 6f, 0.001f)
  }

  test("map Quaternion from float array") {
    val q = new Quaternion()
    GLTFTypes.map(q, Array(0.1f, 0.2f, 0.3f, 0.9f))
    assertEqualsFloat(q.x, 0.1f, 0.001f)
    assertEqualsFloat(q.y, 0.2f, 0.001f)
    assertEqualsFloat(q.z, 0.3f, 0.001f)
    assertEqualsFloat(q.w, 0.9f, 0.001f)
  }

  // ── mapInterpolation ──────────────────────────────────────────────────

  test("mapInterpolation: defaults to LINEAR") {
    assertEquals(GLTFTypes.mapInterpolation(Nullable.empty), Interpolation.LINEAR)
  }

  test("mapInterpolation: maps known types") {
    assertEquals(GLTFTypes.mapInterpolation(Nullable("LINEAR")), Interpolation.LINEAR)
    assertEquals(GLTFTypes.mapInterpolation(Nullable("STEP")), Interpolation.STEP)
    assertEquals(GLTFTypes.mapInterpolation(Nullable("CUBICSPLINE")), Interpolation.CUBICSPLINE)
  }

  test("mapInterpolation: throws on unknown type") {
    intercept[GLTFIllegalException] {
      GLTFTypes.mapInterpolation(Nullable("INVALID"))
    }
  }
}
