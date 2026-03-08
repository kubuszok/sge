/*
 * SGE — Camera + Viewport pipeline integration tests
 *
 * Tests the full chain: Camera setup -> Viewport resize -> coordinate mapping.
 * Verifies that cameras and viewports work together correctly.
 */
package sge
package graphics

import munit.FunSuite
import sge.math.Vector3
import sge.noop.NoopGraphics
import sge.utils.viewport.{ ExtendViewport, FitViewport, ScreenViewport, StretchViewport }

class CameraViewportPipelineTest extends FunSuite {

  // ─── Helpers ─────────────────────────────────────────────────────────

  private def makeContext(w: Int, h: Int): Sge = {
    val graphics = new NoopGraphics(w, h) {
      override def getGL20(): GL20 = NoopGL20
    }
    SgeTestFixture.testSge(graphics = graphics)
  }

  private inline def px(v: Int): Pixels = Pixels(v)

  // ─── OrthographicCamera + StretchViewport ────────────────────────────

  test("ortho + stretch: world center maps to screen center") {
    given sge: Sge = makeContext(800, 600)
    val camera   = OrthographicCamera()
    val viewport = StretchViewport(200f, 150f, camera)
    viewport.update(px(800), px(600))
    camera.update()
    // World (0,0) is center of camera → should map to screen center
    val screen = Vector3(0f, 0f, 0f)
    camera.project(screen)
    assertEqualsFloat(screen.x, 400f, 1f)
    assertEqualsFloat(screen.y, 300f, 1f)
  }

  test("ortho + stretch: screen corners map to world edges") {
    given sge: Sge = makeContext(800, 600)
    val camera   = OrthographicCamera()
    val viewport = StretchViewport(200f, 150f, camera)
    viewport.update(px(800), px(600))
    camera.update()
    // Screen top-left (0, 0) → world should be near (-100, 75)
    val world = Vector3(0f, 0f, 0f)
    camera.unproject(world)
    assertEqualsFloat(world.x, -100f, 1f)
    assertEqualsFloat(world.y, 75f, 1f)
  }

  // ─── OrthographicCamera + FitViewport (letterboxing) ─────────────────

  test("ortho + fit: maintains aspect ratio with gutters") {
    given sge: Sge = makeContext(800, 400)
    val camera   = OrthographicCamera()
    val viewport = FitViewport(200f, 200f, camera) // square world on wide screen
    viewport.update(px(800), px(400))
    camera.update()
    // FitViewport should create pillarboxes (left/right black bars)
    // Viewport should be 400x400 centered at (200, 0)
    assertEquals(viewport.screenX, px(200))
    assertEquals(viewport.screenWidth, px(400))
    assertEquals(viewport.screenHeight, px(400))
  }

  test("ortho + fit: center project/unproject roundtrip") {
    given sge: Sge = makeContext(800, 400)
    val camera   = OrthographicCamera()
    val viewport = FitViewport(200f, 200f, camera)
    viewport.update(px(800), px(400))
    camera.update()
    val v = Vector3(0f, 0f, 0f)
    camera.project(v)
    camera.unproject(v)
    assertEqualsFloat(v.x, 0f, 1f)
    assertEqualsFloat(v.y, 0f, 1f)
  }

  // ─── OrthographicCamera + ScreenViewport ─────────────────────────────

  test("ortho + screen: world units equal pixels") {
    given sge: Sge = makeContext(640, 480)
    val camera   = OrthographicCamera()
    val viewport = ScreenViewport(camera)
    viewport.update(px(640), px(480))
    camera.update()
    // World size should match screen size
    assertEqualsFloat(camera.viewportWidth, 640f, 0.1f)
    assertEqualsFloat(camera.viewportHeight, 480f, 0.1f)
  }

  // ─── Resize ──────────────────────────────────────────────────────────

  test("viewport resize updates camera viewport dimensions") {
    given sge: Sge = makeContext(800, 600)
    val camera   = OrthographicCamera()
    val viewport = StretchViewport(200f, 150f, camera)
    viewport.update(px(800), px(600))
    camera.update()
    // Now resize to 1024x768
    viewport.update(px(1024), px(768))
    camera.update()
    // World dimensions should still be 200x150 for StretchViewport
    assertEqualsFloat(camera.viewportWidth, 200f, 0.1f)
    assertEqualsFloat(camera.viewportHeight, 150f, 0.1f)
  }

  test("extend viewport expands on resize") {
    given sge: Sge = makeContext(800, 400)
    val camera   = OrthographicCamera()
    val viewport = ExtendViewport(200f, 200f, camera)
    viewport.update(px(800), px(400))
    camera.update()
    // ExtendViewport should extend the wider axis
    // 800:400 = 2:1 aspect, min world 200x200 → extends width to 400
    assertEqualsFloat(camera.viewportWidth, 400f, 1f)
    assertEqualsFloat(camera.viewportHeight, 200f, 1f)
  }

  // ─── Zoom ────────────────────────────────────────────────────────────

  test("zoom affects project/unproject") {
    given sge: Sge = makeContext(800, 600)
    val camera   = OrthographicCamera()
    val viewport = StretchViewport(200f, 150f, camera)
    viewport.update(px(800), px(600))
    camera.zoom = 2.0f
    camera.update()
    // With 2x zoom, world visible range doubles: ±200 x ±150
    val world = Vector3(0f, 0f, 0f)
    camera.unproject(world)
    // Top-left at zoom=2 should be (-200, 150)
    assertEqualsFloat(world.x, -200f, 1f)
    assertEqualsFloat(world.y, 150f, 1f)
  }

  // ─── Camera translate + viewport ─────────────────────────────────────

  test("camera translation shifts projected coordinates") {
    given sge: Sge = makeContext(800, 600)
    val camera   = OrthographicCamera()
    val viewport = StretchViewport(200f, 150f, camera)
    viewport.update(px(800), px(600))
    camera.translate(50f, 25f)
    camera.update()
    // Camera center is now at (50, 25)
    // Project world (50, 25) → should be screen center (400, 300)
    val screen = Vector3(50f, 25f, 0f)
    camera.project(screen)
    assertEqualsFloat(screen.x, 400f, 1f)
    assertEqualsFloat(screen.y, 300f, 1f)
  }
}
