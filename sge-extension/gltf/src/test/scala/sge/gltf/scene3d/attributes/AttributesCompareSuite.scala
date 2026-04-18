package sge
package gltf
package scene3d
package attributes

import sge.graphics.Color

class AttributesCompareSuite extends munit.FunSuite {

  // ── FogAttribute ─────────────────────────────────────────────────────────

  test("FogAttribute: same values compare as equal") {
    val a = FogAttribute.createFog(0, 1, 2)
    val b = a.copy()
    assertEquals(a.compareTo(b), 0)
  }

  test("FogAttribute: different values compare as not equal") {
    val a = FogAttribute.createFog(0, 1, 2)
    val b = a.copy().asInstanceOf[FogAttribute]
    b.value.x = 4
    assertNotEquals(a.compareTo(b), 0)
  }

  // ── PBRIridescenceAttribute ──────────────────────────────────────────────

  test("PBRIridescenceAttribute: same values compare as equal") {
    val a = PBRIridescenceAttribute(1, 1.5f, 100, 400)
    val b = a.copy()
    assertEquals(a.compareTo(b), 0)
  }

  test("PBRIridescenceAttribute: different values compare as not equal") {
    val a = PBRIridescenceAttribute(1, 1.5f, 100, 400)
    val b = a.copy().asInstanceOf[PBRIridescenceAttribute]
    b.factor = 4
    assertNotEquals(a.compareTo(b), 0)
  }

  // ── PBRVolumeAttribute ───────────────────────────────────────────────────

  test("PBRVolumeAttribute: same values compare as equal") {
    val a = PBRVolumeAttribute(1, 10, Color(Color.WHITE))
    val b = a.copy()
    assertEquals(a.compareTo(b), 0)
  }

  test("PBRVolumeAttribute: different values compare as not equal") {
    val a = PBRVolumeAttribute(1, 10, Color(Color.WHITE))
    val b = a.copy().asInstanceOf[PBRVolumeAttribute]
    b.thicknessFactor = .5f
    assertNotEquals(a.compareTo(b), 0)
  }

  // ── PBRTextureAttribute ──────────────────────────────────────────────────

  test("PBRTextureAttribute: same UV transforms compare as equal") {
    val a = PBRTextureAttribute(PBRTextureAttribute.BaseColorTexture)
    a.offsetU = .2f
    a.offsetV = .3f
    a.scaleU = 2f
    a.scaleV = 3f
    a.rotationUV = 90f
    val b = a.copy()
    assertEquals(a.compareTo(b), 0)
  }

  test("PBRTextureAttribute: different rotationUV compare as not equal") {
    val a = PBRTextureAttribute(PBRTextureAttribute.BaseColorTexture)
    a.offsetU = .2f
    a.offsetV = .3f
    a.scaleU = 2f
    a.scaleV = 3f
    a.rotationUV = 90
    val b = a.copy().asInstanceOf[PBRTextureAttribute]
    b.rotationUV = 180
    assertNotEquals(a.compareTo(b), 0)
  }
}
