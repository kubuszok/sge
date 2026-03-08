/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils
package viewport

import sge.graphics.{ GL20, OrthographicCamera }
import sge.noop.NoopGraphics

class ViewportTest extends munit.FunSuite {

  private val epsilon = 0.01f

  private def assertEqualsFloat(actual: Float, expected: Float, delta: Float): Unit =
    assert(Math.abs(actual - expected) < delta, s"expected $expected but got $actual (delta=$delta)")

  private def makeContext(w: Int, h: Int): Sge = {
    val graphics = new NoopGraphics(w, h) {
      override def getGL20(): GL20 = NoopGL20
    }
    SgeTestFixture.testSge(graphics = graphics)
  }

  // ---- ScreenViewport ----

  test("ScreenViewport update sets world to screen size") {
    given Sge = makeContext(800, 600)
    val vp    = ScreenViewport()
    vp.update(Pixels(800), Pixels(600))
    assertEqualsFloat(vp.worldWidth, 800f, epsilon)
    assertEqualsFloat(vp.worldHeight, 600f, epsilon)
  }

  test("ScreenViewport screenBounds fill the screen") {
    given Sge = makeContext(800, 600)
    val vp    = ScreenViewport()
    vp.update(Pixels(800), Pixels(600))
    assertEquals(vp.screenX.toInt, 0)
    assertEquals(vp.screenY.toInt, 0)
    assertEquals(vp.screenWidth.toInt, 800)
    assertEquals(vp.screenHeight.toInt, 600)
  }

  test("ScreenViewport unitsPerPixel=0.5 halves world size") {
    given Sge = makeContext(800, 600)
    val vp    = ScreenViewport()
    vp.unitsPerPixel = 0.5f
    vp.update(Pixels(800), Pixels(600))
    assertEqualsFloat(vp.worldWidth, 400f, epsilon)
    assertEqualsFloat(vp.worldHeight, 300f, epsilon)
  }

  test("ScreenViewport unitsPerPixel=2 doubles world size") {
    given Sge = makeContext(800, 600)
    val vp    = ScreenViewport()
    vp.unitsPerPixel = 2f
    vp.update(Pixels(800), Pixels(600))
    assertEqualsFloat(vp.worldWidth, 1600f, epsilon)
    assertEqualsFloat(vp.worldHeight, 1200f, epsilon)
  }

  // ---- StretchViewport ----

  test("StretchViewport stretches to fill screen") {
    given Sge = makeContext(800, 600)
    val vp    = StretchViewport(100f, 100f)
    vp.update(Pixels(800), Pixels(600))
    assertEquals(vp.screenX.toInt, 0)
    assertEquals(vp.screenY.toInt, 0)
    assertEquals(vp.screenWidth.toInt, 800)
    assertEquals(vp.screenHeight.toInt, 600)
  }

  test("StretchViewport preserves world size") {
    given Sge = makeContext(800, 600)
    val vp    = StretchViewport(100f, 100f)
    vp.update(Pixels(800), Pixels(600))
    assertEqualsFloat(vp.worldWidth, 100f, epsilon)
    assertEqualsFloat(vp.worldHeight, 100f, epsilon)
  }

  test("StretchViewport no gutters") {
    given Sge = makeContext(800, 600)
    val vp    = StretchViewport(100f, 100f)
    vp.update(Pixels(800), Pixels(600))
    assertEquals(vp.getLeftGutterWidth().toInt, 0)
    assertEquals(vp.getBottomGutterHeight().toInt, 0)
  }

  // ---- FitViewport ----

  test("FitViewport letterboxes wider screen (100x100 on 800x600)") {
    given Sge = makeContext(800, 600)
    val vp    = FitViewport(100f, 100f)
    vp.update(Pixels(800), Pixels(600))
    // Fit scales 100x100 to 600x600 (limited by height)
    assertEquals(vp.screenWidth.toInt, 600)
    assertEquals(vp.screenHeight.toInt, 600)
  }

  test("FitViewport preserves world size") {
    given Sge = makeContext(800, 600)
    val vp    = FitViewport(100f, 100f)
    vp.update(Pixels(800), Pixels(600))
    assertEqualsFloat(vp.worldWidth, 100f, epsilon)
    assertEqualsFloat(vp.worldHeight, 100f, epsilon)
  }

  test("FitViewport left gutter is correct") {
    given Sge = makeContext(800, 600)
    val vp    = FitViewport(100f, 100f)
    vp.update(Pixels(800), Pixels(600))
    // Viewport is 600x600 centered: left gutter = (800-600)/2 = 100
    assertEquals(vp.getLeftGutterWidth().toInt, 100)
    assertEquals(vp.screenX.toInt, 100)
  }

  test("FitViewport with taller screen pillarboxes (100x100 on 600x800)") {
    given Sge = makeContext(600, 800)
    val vp    = FitViewport(100f, 100f)
    vp.update(Pixels(600), Pixels(800))
    // Fit scales 100x100 to 600x600 (limited by width)
    assertEquals(vp.screenWidth.toInt, 600)
    assertEquals(vp.screenHeight.toInt, 600)
    // Top/bottom gutters: (800-600)/2 = 100
    assertEquals(vp.getBottomGutterHeight().toInt, 100)
  }

  // ---- FillViewport ----

  test("FillViewport fills screen (100x100 on 800x600)") {
    given Sge = makeContext(800, 600)
    val vp    = FillViewport(100f, 100f)
    vp.update(Pixels(800), Pixels(600))
    // Fill scales 100x100 to 800x800 (limited by width, which is larger)
    assertEquals(vp.screenWidth.toInt, 800)
    assertEquals(vp.screenHeight.toInt, 800)
  }

  test("FillViewport preserves world size") {
    given Sge = makeContext(800, 600)
    val vp    = FillViewport(100f, 100f)
    vp.update(Pixels(800), Pixels(600))
    assertEqualsFloat(vp.worldWidth, 100f, epsilon)
    assertEqualsFloat(vp.worldHeight, 100f, epsilon)
  }

  test("FillViewport negative screen offset (overflows)") {
    given Sge = makeContext(800, 600)
    val vp    = FillViewport(100f, 100f)
    vp.update(Pixels(800), Pixels(600))
    // screenY = (600-800)/2 = -100
    assertEquals(vp.screenY.toInt, -100)
    assertEquals(vp.screenX.toInt, 0)
  }

  // ---- ExtendViewport ----

  test("ExtendViewport extends shorter dimension") {
    given Sge = makeContext(800, 600)
    val vp    = ExtendViewport(100f, 100f)
    vp.update(Pixels(800), Pixels(600))
    // min 100x100 on 800x600: fit gives 600x600, then extends width to fill 800
    // world width should be > 100
    assert(vp.worldWidth > 100f, s"world width ${vp.worldWidth} should be > 100")
    assertEqualsFloat(vp.worldHeight, 100f, epsilon)
  }

  test("ExtendViewport fills screen with extended viewport") {
    given Sge = makeContext(800, 600)
    val vp    = ExtendViewport(100f, 100f)
    vp.update(Pixels(800), Pixels(600))
    // After extension, viewport should fill screen
    assertEquals(vp.screenWidth.toInt, 800)
    assertEquals(vp.screenHeight.toInt, 600)
  }

  test("ExtendViewport with max limits extension") {
    given Sge = makeContext(800, 600)
    val vp    = ExtendViewport(100f, 100f, 120f, 120f)
    vp.update(Pixels(800), Pixels(600))
    // worldWidth should be limited to maxWidth=120
    assert(vp.worldWidth <= 120f + epsilon, s"world width ${vp.worldWidth} should be <= 120")
  }

  test("ExtendViewport square screen keeps min world size") {
    given Sge = makeContext(600, 600)
    val vp    = ExtendViewport(100f, 100f)
    vp.update(Pixels(600), Pixels(600))
    assertEqualsFloat(vp.worldWidth, 100f, epsilon)
    assertEqualsFloat(vp.worldHeight, 100f, epsilon)
  }

  // ---- ScalingViewport with Scaling.none ----

  test("ScalingViewport none: viewport is world size centered") {
    given Sge = makeContext(800, 600)
    val vp    = ScalingViewport(Scaling.none, 100f, 100f)
    vp.update(Pixels(800), Pixels(600))
    assertEquals(vp.screenWidth.toInt, 100)
    assertEquals(vp.screenHeight.toInt, 100)
    // Centered: screenX = (800-100)/2 = 350
    assertEquals(vp.screenX.toInt, 350)
    // screenY = (600-100)/2 = 250
    assertEquals(vp.screenY.toInt, 250)
  }

  test("ScalingViewport none: gutters exist") {
    given Sge = makeContext(800, 600)
    val vp    = ScalingViewport(Scaling.none, 100f, 100f)
    vp.update(Pixels(800), Pixels(600))
    assertEquals(vp.getLeftGutterWidth().toInt, 350)
    assertEquals(vp.getBottomGutterHeight().toInt, 250)
  }

  // ---- Viewport camera interaction ----

  test("Viewport apply with centerCamera positions camera at world center") {
    given Sge = makeContext(800, 600)
    val vp    = StretchViewport(200f, 100f)
    vp.update(Pixels(800), Pixels(600), centerCamera = true)
    assertEqualsFloat(vp.camera.position.x, 100f, epsilon)
    assertEqualsFloat(vp.camera.position.y, 50f, epsilon)
  }

  test("Viewport update sets camera viewport dimensions") {
    given Sge = makeContext(800, 600)
    val vp    = StretchViewport(200f, 150f)
    vp.update(Pixels(800), Pixels(600))
    assertEqualsFloat(vp.camera.viewportWidth, 200f, epsilon)
    assertEqualsFloat(vp.camera.viewportHeight, 150f, epsilon)
  }

  test("Viewport with custom camera uses that camera") {
    given Sge = makeContext(800, 600)
    val cam   = OrthographicCamera()
    val vp    = StretchViewport(100f, 100f, cam)
    vp.update(Pixels(800), Pixels(600))
    assert(vp.camera eq cam, "viewport should use the provided camera")
  }
}
