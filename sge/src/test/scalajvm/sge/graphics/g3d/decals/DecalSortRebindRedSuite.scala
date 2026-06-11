/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-497 bounce 1 (default cameraSorter must follow camera rebinding).
 *
 * Every expected behaviour below is derived by hand-tracing the original
 * com/badlogic/gdx/graphics/g3d/decals/CameraGroupStrategy.java
 * (original-src/libgdx). Java line numbers cited in the test comments refer
 * to that file:
 *   - Default cameraSorter (lines 102-109): the anonymous Comparator
 *     deliberately reads `CameraGroupStrategy.this.camera.position.dst(...)`
 *     (lines 105-106) — the LIVE field, not the constructor parameter — so
 *     the default sorter always measures distances against whatever camera
 *     the strategy currently holds.
 *   - setCamera (lines 119-121) replaces that field: after
 *     `strategy.setCamera(cam2)` the very same default sorter must order the
 *     GROUP_BLEND decals back-to-front AGAINST cam2.
 *   - The port's secondary constructor (CameraGroupStrategy.scala lines
 *     74-82 at fix-1, commit 4ba94d2f) builds the default sorter as a SAM
 *     lambda that captures the constructor PARAMETER `camera` instead of
 *     reading the live `this.camera` field, so after a rebind through the
 *     public setter (`setCamera(cam2)` / `strategy.camera = cam2`) the
 *     default sorter still sorts against the ORIGINAL cam1 — observable
 *     wrong blend order through the same public beforeGroup path.
 *   - beforeGroup GROUP_BLEND branch (lines 134-138) runs
 *     `contents.sort(cameraSorter)` — the public path through which the
 *     private sorter is observable; the group id is obtained from
 *     decideGroup (lines 128-130) on a transparent decal, exactly as in
 *     DecalSortRedSuite.
 *
 * Headless fixture: identical to DecalSortRedSuite (frozen at red-1,
 * 27b47824). CameraGroupStrategy's constructor unconditionally calls
 * createDefaultShader() and throws unless ShaderProgram.compiled is true, so
 * plain NoopGL20 (glCreateShader returns 0) cannot construct it. The
 * ShaderCompilingGL20 below extends the NoopGL20 pattern with just enough
 * fake state for ShaderProgram's compile/link checks to succeed: nonzero
 * shader/program handles and GL_TRUE written for GL_COMPILE_STATUS /
 * GL_LINK_STATUS queries. All other pnames leave the (zero-initialised)
 * buffers untouched, so fetchAttributes()/fetchUniforms() see 0
 * attributes/uniforms and loop zero times. Decals are built with
 * Decal.newDecal(TextureRegion, true) over a dummy Custom-type TextureData
 * texture (Texture construction only calls glGenTexture, a no-op
 * headlessly); hasTransparency = true makes the material non-opaque so
 * decideGroup yields GROUP_BLEND.
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
import sge.noop.NoopGraphics
import lowlevel.util.DynamicArray

class DecalSortRebindRedSuite extends munit.FunSuite {

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

  /** Camera repositioned to (0, 0, -40), BEHIND every decal of these tests, looking back up +Z at them. The direction is irrelevant to the sort (Java lines 105-107 use positions only); the position
    * is chosen so that the decal FARTHEST from cam2 is exactly the decal NEAREST to cam1, flipping the expected back-to-front order:
    *   - near at (0, 0, -10): cam1 -> 10, cam2 -> |-10 - (-40)| = 30
    *   - mid at (0, 0, -15): cam1 -> 15, cam2 -> |-15 - (-40)| = 25
    *   - far at (0, 0, -30): cam1 -> 30, cam2 -> |-30 - (-40)| = 10
    */
  private def behindCamera()(using Sge): PerspectiveCamera = {
    val cam2 = originCamera()
    cam2.position.set(0f, 0f, -40f)
    cam2.direction.set(0f, 0f, 1f)
    cam2
  }

  // --- ISS-497 bounce 1: default sorter must follow camera rebinding ----------

  test(
    "ISS-497: default sorter must follow setCamera rebinding (Java lines 105-106 read the live field, lines 119-121 replace it)"
  ) {
    // Java's anonymous Comparator reads CameraGroupStrategy.this.camera
    // (lines 105-106), so after setCamera(cam2) (lines 119-121) the
    // GROUP_BLEND sort in beforeGroup (lines 134-138) must be back-to-front
    // AGAINST cam2: order (near, mid, far) = cam2 distances (30, 25, 10).
    // The port's secondary constructor (CameraGroupStrategy.scala lines
    // 74-82 at fix-1) captured the ctor PARAMETER cam1 in the SAM lambda, so
    // it still yields cam1 back-to-front (far, mid, near) = cam2 distances
    // (10, 25, 30) — the exact reverse.
    given Sge = makeSge()

    val cam1     = originCamera()
    val strategy = new CameraGroupStrategy(cam1)
    val region   = new TextureRegion(new Texture(new DummyTextureData))

    // Distances from cam1 at the origin / cam2 at (0, 0, -40) — see behindCamera().
    val near = decalAt(region, 0f, 0f, -10f) // cam1: 10, cam2: 30
    val mid  = decalAt(region, 0f, 0f, -15f) // cam1: 15, cam2: 25
    val far  = decalAt(region, 0f, 0f, -30f) // cam1: 30, cam2: 10

    val blendGroup = strategy.decideGroup(near)

    val contents = DynamicArray[Decal]()
    contents.add(mid)
    contents.add(far)
    contents.add(near)

    // Pre-rebind sanity (NOT the red assertion): with cam1 still bound the
    // sorter must produce cam1 back-to-front order (far, mid, near) = cam1
    // distances (30, 15, 10). This passes at fix-1 and pins the fixture.
    strategy.beforeGroup(blendGroup, contents)
    val cam1Distances = (0 until contents.size).map(i => cam1.position.distance(contents(i).position)).toList
    assertEquals(
      cam1Distances,
      List(30f, 15f, 10f),
      "pre-rebind sanity: default sorter sorts GROUP_BLEND decals back-to-front against cam1 (Java line 107)"
    )

    // Rebind through the public setter (Java lines 119-121).
    val cam2 = behindCamera()
    strategy.setCamera(cam2)

    // Scramble insertion order again so that neither the stale-cam1 order
    // (far, mid, near) nor the expected cam2 order (near, mid, far) equals
    // the insertion order — the sort has to actively reorder either way.
    contents.clear()
    contents.add(mid)
    contents.add(near)
    contents.add(far)

    strategy.beforeGroup(blendGroup, contents)

    val cam2Distances = (0 until contents.size).map(i => cam2.position.distance(contents(i).position)).toList
    assertEquals(
      cam2Distances,
      List(30f, 25f, 10f),
      "after setCamera (Java lines 119-121) the default sorter must sort back-to-front against the NEW camera (Java lines 105-106 read the live field), not the construction-time one"
    )
    assert(
      contents(0) eq near,
      "the decal farthest from cam2 (distance 30) must be rendered first after rebinding (Java lines 105-107)"
    )
    assert(contents(1) eq mid, "the decal at cam2 distance 25 must be rendered second after rebinding (Java lines 105-107)")
    assert(contents(2) eq far, "the decal nearest to cam2 (distance 10) must be rendered last after rebinding (Java lines 105-107)")
  }

  test(
    "ISS-497: default sorter must follow direct camera reassignment (the port's public var is the same live field Java lines 105-106 read)"
  ) {
    // The port exposes the camera as a public `var camera: Camera` (the
    // no-logic setCamera/getCamera pair maps onto it), so
    // `strategy.camera = cam2` is the second public spelling of Java's
    // setCamera (lines 119-121). The faithful port reads the live field in
    // the default sorter (Java lines 105-106), so this path must follow the
    // rebind too — a fix that only patches setCamera is not faithful.
    given Sge = makeSge()

    val cam1     = originCamera()
    val strategy = new CameraGroupStrategy(cam1)
    val region   = new TextureRegion(new Texture(new DummyTextureData))

    // Distances from cam1 at the origin / cam2 at (0, 0, -40) — see behindCamera().
    val near = decalAt(region, 0f, 0f, -10f) // cam1: 10, cam2: 30
    val far  = decalAt(region, 0f, 0f, -30f) // cam1: 30, cam2: 10

    val blendGroup = strategy.decideGroup(near)

    val cam2 = behindCamera()
    strategy.camera = cam2

    // Insertion order (far, near) is exactly the stale-cam1 back-to-front
    // order (cam1 distances 30, 10), so the stale capture leaves it
    // untouched; per Java the sort against cam2 must flip it to (near, far)
    // = cam2 distances (30, 10).
    val contents = DynamicArray[Decal]()
    contents.add(far)
    contents.add(near)

    strategy.beforeGroup(blendGroup, contents)

    val cam2Distances = (0 until contents.size).map(i => cam2.position.distance(contents(i).position)).toList
    assertEquals(
      cam2Distances,
      List(30f, 10f),
      "after `strategy.camera = cam2` the default sorter must sort back-to-front against the NEW camera (Java lines 105-106 read the live field)"
    )
    assert(
      contents(0) eq near,
      "the decal farthest from cam2 (distance 30) must be rendered first after reassignment (Java lines 105-107)"
    )
    assert(
      contents(1) eq far,
      "the decal nearest to cam2 (distance 10) must be rendered last after reassignment (Java lines 105-107)"
    )
  }
}
