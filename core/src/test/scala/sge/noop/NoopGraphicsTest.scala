/*
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package noop

class NoopGraphicsTest extends munit.FunSuite {

  // ---- default dimensions ----

  test("default dimensions are 640x480") {
    val g = NoopGraphics()
    assertEquals(g.getWidth(), 640)
    assertEquals(g.getHeight(), 480)
    assertEquals(g.getBackBufferWidth(), 640)
    assertEquals(g.getBackBufferHeight(), 480)
  }

  test("custom dimensions are respected") {
    val g = NoopGraphics(noopWidth = 1920, noopHeight = 1080)
    assertEquals(g.getWidth(), 1920)
    assertEquals(g.getHeight(), 1080)
  }

  // ---- type ----

  test("getType returns Mock") {
    val g = NoopGraphics()
    assertEquals(g.getType(), Graphics.GraphicsType.Mock)
  }

  // ---- GL accessors ----

  test("getGL20 throws UnsupportedOperationException") {
    val g = NoopGraphics()
    intercept[UnsupportedOperationException] {
      g.getGL20()
    }
  }

  test("getGL30/31/32 return empty") {
    val g = NoopGraphics()
    assert(g.getGL30().isEmpty)
    assert(g.getGL31().isEmpty)
    assert(g.getGL32().isEmpty)
    assertEquals(g.isGL30Available(), false)
    assertEquals(g.isGL31Available(), false)
    assertEquals(g.isGL32Available(), false)
  }

  // ---- frame timing ----

  test("initial frameId is 0 and deltaTime is 0") {
    val g = NoopGraphics()
    assertEquals(g.getFrameId(), 0L)
    assertEquals(g.getDeltaTime(), 0.0f)
  }

  test("updateTime increments frameId") {
    val g = NoopGraphics()
    g.updateTime()
    assertEquals(g.getFrameId(), 1L)
    g.updateTime()
    assertEquals(g.getFrameId(), 2L)
  }

  test("updateTime produces non-negative deltaTime") {
    val g = NoopGraphics()
    g.updateTime()
    assert(g.getDeltaTime() >= 0.0f, s"deltaTime should be >= 0, was ${g.getDeltaTime()}")
  }

  // ---- density / PPI ----

  test("density defaults") {
    val g = NoopGraphics()
    assertEquals(g.getDensity(), 1.0f)
    assertEquals(g.getPpiX(), 96.0f)
    assertEquals(g.getPpiY(), 96.0f)
    assertEquals(g.getBackBufferScale(), 1.0f)
  }

  // ---- continuous rendering ----

  test("continuous rendering is tracked") {
    val g = NoopGraphics()
    assertEquals(g.isContinuousRendering(), true)
    g.setContinuousRendering(false)
    assertEquals(g.isContinuousRendering(), false)
  }

  // ---- display mode / monitor ----

  test("display mode matches dimensions") {
    val g  = NoopGraphics()
    val dm = g.getDisplayMode()
    assertEquals(dm.width, 640)
    assertEquals(dm.height, 480)
    assertEquals(dm.refreshRate, 60)
  }

  test("monitor is available") {
    val g = NoopGraphics()
    assertEquals(g.getMonitor().name, "Noop Monitor")
    assertEquals(g.getMonitors().length, 1)
  }

  // ---- no-op methods don't throw ----

  test("window no-ops do not throw") {
    val g = NoopGraphics()
    g.setTitle("test")
    g.setUndecorated(true)
    g.setResizable(false)
    g.setVSync(true)
    g.setForegroundFPS(60)
    g.requestRendering()
    assertEquals(g.isFullscreen(), false)
    assertEquals(g.supportsDisplayModeChange(), false)
    assertEquals(g.supportsExtension("GL_TEST"), false)
  }
}
