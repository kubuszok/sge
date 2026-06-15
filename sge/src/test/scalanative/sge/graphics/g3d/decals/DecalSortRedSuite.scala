/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-497 (CameraGroupStrategy default decal sorter inverted).
 *
 * Every expected behaviour below is derived by hand-tracing the original
 * com/badlogic/gdx/graphics/g3d/decals/CameraGroupStrategy.java
 * (original-src/libgdx). Java line numbers cited in the test comments refer
 * to that file:
 *   - Default cameraSorter (lines 102-109): `dist1 = camera.position.dst(
 *     o1.position); dist2 = camera.position.dst(o2.position); return (int)
 *     Math.signum(dist2 - dist1);` (line 107). compare(o1, o2) is NEGATIVE
 *     when o1 is FARTHER from the camera (dist1 > dist2 -> dist2 - dist1 < 0),
 *     so farther decals sort FIRST: back-to-front ordering, required for
 *     correct alpha blending of the GROUP_BLEND group. The port
 *     (CameraGroupStrategy.scala lines 72-80) builds the default sorter as
 *     `Ordering.fromLessThan((o1, o2) => Math.signum(dist2 - dist1).toInt >
 *     0)` — lessThan(o1, o2) is true when o2 is farther, i.e. CLOSER decals
 *     sort first — the exact inversion (front-to-back).
 *   - beforeGroup GROUP_BLEND branch (lines 134-138): enables blending,
 *     disables the depth mask and runs `contents.sort(cameraSorter)` — the
 *     public path through which the private sorter is observable.
 *   - GROUP_BLEND is private static final int = 1 (line 85), and
 *     decideGroup (lines 128-130) returns it for any decal whose material is
 *     not opaque, which is how the tests obtain the group id without touching
 *     the private constant.
 *
 * Headless fixture: CameraGroupStrategy's constructor unconditionally calls
 * createDefaultShader() and throws unless ShaderProgram.compiled is true, so
 * plain NoopGL20 (glCreateShader returns 0) cannot construct it. The
 * ShaderCompilingGL20 below extends the NoopGL20 pattern (see
 * SpriteCacheRedSuite) with just enough fake state for ShaderProgram's
 * compile/link checks to succeed: nonzero shader/program handles and
 * GL_TRUE written for GL_COMPILE_STATUS / GL_LINK_STATUS queries. All other
 * pnames leave the (zero-initialised) buffers untouched, so
 * fetchAttributes()/fetchUniforms() see 0 attributes/uniforms and loop zero
 * times. Decals are built with Decal.newDecal(TextureRegion, true) over a
 * dummy Custom-type TextureData texture (Texture construction only calls
 * glGenTexture, a no-op headlessly); hasTransparency = true makes the
 * material non-opaque so decideGroup yields GROUP_BLEND.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer: they encode the original Java semantics, not the port's.
 */
package sge
package graphics
package g3d
package decals

import java.nio.IntBuffer

import sge.graphics.g2d.TextureRegion
import sge.noop.{ NoopGL20, NoopGraphics }
import lowlevel.util.DynamicArray

class DecalSortRedSuite extends munit.FunSuite {

  // --- Headless fixture ------------------------------------------------------

  /** NoopGL20 with shader compilation faked to succeed: ShaderProgram requires nonzero handles from glCreateShader/glCreateProgram (ShaderProgram.scala lines 157-158, 177-178) and a nonzero
    * GL_COMPILE_STATUS / GL_LINK_STATUS readback (lines 162-165, 193-195). Everything else stays a no-op, so fetchAttributes/fetchUniforms read 0 from their zero-initialised buffers.
    */
  final private class ShaderCompilingGL20 extends GL20 {
    private val underlying: GL20 = NoopGL20
    export underlying.{ glCreateProgram as _, glCreateShader as _, glGetProgramiv as _, glGetShaderiv as _, * }

    def glCreateShader(`type`: ShaderType): Int = 1

    def glCreateProgram(): Int = 1

    def glGetShaderiv(shader: Int, pname: Int, params: IntBuffer): Unit =
      if (pname == GL20.GL_COMPILE_STATUS) {
        params.put(0, GL20.GL_TRUE)
        ()
      }

    def glGetProgramiv(program: Int, pname: Int, params: IntBuffer): Unit =
      if (pname == GL20.GL_LINK_STATUS) {
        params.put(0, GL20.GL_TRUE)
        ()
      }
  }

  private def makeSge(): Sge = {
    val fakeGl = new ShaderCompilingGL20
    SgeTestFixture.testSge(graphics = new NoopGraphics() {
      override def gl20: GL20 = fakeGl
    })
  }

  /** Custom-type TextureData: Texture.load() goes straight to consumeCustomData (GLTexture.uploadImageData, Custom branch), never touching Pixmap/gdx2d. Unmanaged, so the texture is not registered
    * with the managed-textures map.
    */
  final private class DummyTextureData extends TextureData {
    def dataType:                                 TextureData.TextureDataType = TextureData.TextureDataType.Custom
    def isPrepared:                               Boolean                     = true
    def prepare():                                Unit                        = ()
    def consumePixmap():                          Pixmap                      = throw new UnsupportedOperationException("dummy texture has no pixmap")
    def disposePixmap:                            Boolean                     = false
    def consumeCustomData(target: TextureTarget): Unit                        = ()
    def width:                                    Int                         = 4
    def height:                                   Int                         = 4
    def getFormat:                                Pixmap.Format               = Pixmap.Format.RGBA8888
    def useMipMaps:                               Boolean                     = false
    def isManaged:                                Boolean                     = false
  }

  /** Transparent decal (hasTransparency = true -> material is not opaque -> decideGroup returns GROUP_BLEND, Java lines 128-130) at the given world position. */
  private def decalAt(region: TextureRegion, x: Float, y: Float, z: Float)(using Sge): Decal = {
    val decal = Decal.newDecal(region, true)
    decal.setPosition(x, y, z)
    decal
  }

  /** Camera at the origin looking down -Z (the Camera default direction is (0, 0, -1)), so a decal at (0, 0, -d) is exactly distance d from the camera. */
  private def originCamera()(using Sge): PerspectiveCamera =
    new PerspectiveCamera(67f, WorldUnits(480f), WorldUnits(320f))

  // --- ISS-497: default cameraSorter must sort decals back-to-front -----------

  test("ISS-497: default sorter must order blended decals farthest-first (Java line 107: (int)Math.signum(dist2 - dist1))") {
    // Java compare(o1, o2) = (int)Math.signum(dist2 - dist1) is negative when
    // o1 is FARTHER, so the GROUP_BLEND sort in beforeGroup (Java lines
    // 134-138) produces back-to-front order: distances (30, 20, 10). The
    // port's Ordering.fromLessThan(... > 0) (CameraGroupStrategy.scala lines
    // 75-79) is the exact inversion and currently yields nearest-first
    // (10, 20, 30), breaking alpha blending of overlapping decals.
    given Sge = makeSge()

    val camera   = originCamera()
    val strategy = new CameraGroupStrategy(camera)
    val region   = new TextureRegion(new Texture(new DummyTextureData))

    val near = decalAt(region, 0f, 0f, -10f)
    val far  = decalAt(region, 0f, 0f, -30f)
    val mid  = decalAt(region, 0f, 0f, -20f)

    val blendGroup = strategy.decideGroup(near)

    val contents = DynamicArray[Decal]()
    contents.add(near)
    contents.add(far)
    contents.add(mid)

    // beforeGroup on the blend group is the public path that runs
    // contents.sort(cameraSorter) (Java line 137 / Scala line 94).
    strategy.beforeGroup(blendGroup, contents)

    val sortedDistances = (0 until contents.size).map(i => camera.position.distance(contents(i).position)).toList
    assertEquals(
      sortedDistances,
      List(30f, 20f, 10f),
      "default cameraSorter must sort GROUP_BLEND decals back-to-front (farthest first, Java line 107), not front-to-back"
    )
    assert(contents(0) eq far, "the farthest decal (distance 30) must be rendered first (Java line 107)")
    assert(contents(1) eq mid, "the middle decal (distance 20) must be rendered second (Java line 107)")
    assert(contents(2) eq near, "the nearest decal (distance 10) must be rendered last (Java line 107)")
  }

  test("ISS-497: equal-distance decals compare equal and both precede a nearer decal (Java line 107: signum(0) == 0)") {
    // Tie pin: two decals at the same distance compare equal (Math.signum(0)
    // == 0, Java line 107) — neither precedes the other — but BOTH must still
    // sort before a nearer decal. Insertion order (30, 10, 30) must become
    // distances (30, 30, 10); the port's inverted sorter currently yields
    // (10, 30, 30).
    given Sge = makeSge()

    val camera   = originCamera()
    val strategy = new CameraGroupStrategy(camera)
    val region   = new TextureRegion(new Texture(new DummyTextureData))

    val farA = decalAt(region, 0f, 0f, -30f)
    val near = decalAt(region, 0f, 0f, -10f)
    val farB = decalAt(region, 0f, 0f, -30f)

    val blendGroup = strategy.decideGroup(near)

    val contents = DynamicArray[Decal]()
    contents.add(farA)
    contents.add(near)
    contents.add(farB)

    strategy.beforeGroup(blendGroup, contents)

    val sortedDistances = (0 until contents.size).map(i => camera.position.distance(contents(i).position)).toList
    assertEquals(
      sortedDistances,
      List(30f, 30f, 10f),
      "equal-distance decals compare equal (signum(0) == 0) and must both precede the nearer decal (back-to-front, Java line 107)"
    )
    assert(contents(2) eq near, "the nearest decal must be rendered last (Java line 107)")
  }
}
