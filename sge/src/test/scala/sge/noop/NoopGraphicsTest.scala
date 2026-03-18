/*
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package noop

class NoopGraphicsTest extends munit.FunSuite {

  // ---- default dimensions ----

  test("default dimensions are 640x480") {
    val g = NoopGraphics()
    assertEquals(g.width, Pixels(640))
    assertEquals(g.height, Pixels(480))
    assertEquals(g.backBufferWidth, Pixels(640))
    assertEquals(g.backBufferHeight, Pixels(480))
  }

  test("custom dimensions are respected") {
    val g = NoopGraphics(noopWidth = 1920, noopHeight = 1080)
    assertEquals(g.width, Pixels(1920))
    assertEquals(g.height, Pixels(1080))
  }

  // ---- type ----

  test("getType returns Mock") {
    val g = NoopGraphics()
    assertEquals(g.graphicsType, Graphics.GraphicsType.Mock)
  }

  // ---- GL accessors ----

  test("getGL20 throws UnsupportedOperationException") {
    val g = NoopGraphics()
    intercept[UnsupportedOperationException] {
      g.gl20
    }
  }

  test("getGL30/31/32 return empty") {
    val g = NoopGraphics()
    assert(g.gl30.isEmpty)
    assert(g.gl31.isEmpty)
    assert(g.gl32.isEmpty)
    assertEquals(g.gl30Available, false)
    assertEquals(g.gl31Available, false)
    assertEquals(g.gl32Available, false)
  }

  // ---- frame timing ----

  test("initial frameId is 0 and deltaTime is 0") {
    val g = NoopGraphics()
    assertEquals(g.frameId, 0L)
    assertEquals(g.deltaTime, 0.0f)
  }

  test("updateTime increments frameId") {
    val g = NoopGraphics()
    g.updateTime()
    assertEquals(g.frameId, 1L)
    g.updateTime()
    assertEquals(g.frameId, 2L)
  }

  test("updateTime produces non-negative deltaTime") {
    val g = NoopGraphics()
    g.updateTime()
    assert(g.deltaTime >= 0.0f, s"deltaTime should be >= 0, was ${g.deltaTime}")
  }

  // ---- density / PPI ----

  test("density defaults") {
    val g = NoopGraphics()
    assertEquals(g.density, 1.0f)
    assertEquals(g.ppiX, 96.0f)
    assertEquals(g.ppiY, 96.0f)
    assertEquals(g.backBufferScale, 1.0f)
  }

  // ---- continuous rendering ----

  test("continuous rendering is tracked") {
    val g = NoopGraphics()
    assertEquals(g.continuousRendering, true)
    g.setContinuousRendering(false)
    assertEquals(g.continuousRendering, false)
  }

  // ---- display mode / monitor ----

  test("display mode matches dimensions") {
    val g  = NoopGraphics()
    val dm = g.displayMode
    assertEquals(dm.width, 640)
    assertEquals(dm.height, 480)
    assertEquals(dm.refreshRate, 60)
  }

  test("monitor is available") {
    val g = NoopGraphics()
    assertEquals(g.monitor.name, "Noop Monitor")
    assertEquals(g.monitors.length, 1)
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
    assertEquals(g.fullscreen, false)
    assertEquals(g.supportsDisplayModeChange(), false)
    assertEquals(g.supportsExtension("GL_TEST"), false)
  }
}
