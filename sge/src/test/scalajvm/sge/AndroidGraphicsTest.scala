// SGE — Unit test: AndroidGraphics
//
// Tests the AndroidGraphics implementation for frame timing, display metrics,
// GL availability, and monitor/display mode stubs.

package sge

import munit.FunSuite
import sge.platform.android._

class AndroidGraphicsTest extends FunSuite {

  // Minimal stubs for ops interfaces
  private def stubConfig(): AndroidConfigOps = {
    val c = AndroidConfigOps()
    c.r = 8; c.g = 8; c.b = 8; c.a = 8
    c.depth = 16; c.stencil = 0; c.numSamples = 0
    c
  }

  private class StubDisplayMetrics extends DisplayMetricsOps {
    var ppiX:                                 Float = 160f
    var ppiY:                                 Float = 160f
    var ppcX:                                 Float = 160f / 2.54f
    var ppcY:                                 Float = 160f / 2.54f
    var density:                              Float = 1.0f
    var safeInsetLeft:                        Int   = 0
    var safeInsetTop:                         Int   = 0
    var safeInsetRight:                       Int   = 0
    var safeInsetBottom:                      Int   = 0
    def updateMetrics(windowManager: AnyRef): Unit  = ()
    def updateSafeInsets(window: AnyRef):     Unit  = {
      safeInsetLeft = 10; safeInsetTop = 20; safeInsetRight = 10; safeInsetBottom = 30
    }
    def displayMode(context: AnyRef, bitsPerPixel: Int): (Int, Int, Int, Int) = (1080, 1920, 60, bitsPerPixel)
  }

  private class StubGLSurfaceView extends GLSurfaceViewOps {
    var view:                                             AnyRef  = "stub-view"
    var isPaused:                                         Boolean = false
    var continuousRendering:                              Boolean = true
    var renderRequested:                                  Boolean = false
    def onPause():                                        Unit    = isPaused = true
    def onResume():                                       Unit    = isPaused = false
    def requestRender():                                  Unit    = renderRequested = true
    def setContinuousRendering(continuous:     Boolean):  Unit    = continuousRendering = continuous
    def queueEvent(runnable:                   Runnable): Unit    = runnable.run()
    def setPreserveEGLContextOnPause(preserve: Boolean):  Unit    = ()
    def setFocusable(focusable:                Boolean):  Unit    = ()
    def glEsVersion:                                      Int     = 2
    def checkGL20Support:                                 Boolean = true
  }

  private class StubCursorOps extends CursorOps {
    var lastView:                                       AnyRef = null // scalafix:ok
    var lastCursorType:                                 Int    = -1
    def setSystemCursor(view: AnyRef, cursorType: Int): Unit   = {
      lastView = view
      lastCursorType = cursorType
    }
  }

  private class StubProvider extends AndroidPlatformProvider {
    def createLifecycle(activity:           AnyRef):                                                                                AndroidLifecycleOps = ???
    def createClipboard(context:            AnyRef):                                                                                ClipboardOps        = ???
    def createPreferences(context:          AnyRef, name:             String):                                                      PreferencesOps      = ???
    def openURI(context:                    AnyRef, uri:              String):                                                      Boolean             = false
    def defaultConfig():                                                                                                            AndroidConfigOps    = AndroidConfigOps()
    def createFiles(context:                AnyRef, useExternalFiles: Boolean):                                                     FilesOps            = ???
    def createAudioEngine(context:          AnyRef, config:           AndroidConfigOps):                                            AudioEngineOps      = ???
    def createHaptics(context:              AnyRef):                                                                                HapticsOps          = ???
    def createCursor(context:               AnyRef):                                                                                CursorOps           = ???
    def createDisplayMetrics(windowManager: AnyRef):                                                                                DisplayMetricsOps   = ???
    def createSensors(context:              AnyRef, windowManager:    AnyRef):                                                      SensorOps           = ???
    def createTouchInput(context:           AnyRef):                                                                                TouchInputOps       = ???
    def createInputMethod(context:          AnyRef, handler:          AnyRef):                                                      InputMethodOps      = ???
    def createGLSurfaceView(context:        AnyRef, config:           AndroidConfigOps, resolutionStrategy: ResolutionStrategyOps): GLSurfaceViewOps    = ???
    def createGL20():                                                                                                               GL20Ops             = ???
    def createGL30():                                                                                                               GL30Ops             = ???
  }

  private def mkGraphics(): (AndroidGraphics, StubDisplayMetrics, StubGLSurfaceView, StubCursorOps) = {
    val dm  = StubDisplayMetrics()
    val glv = StubGLSurfaceView()
    val cur = StubCursorOps()
    val g   = AndroidGraphics(stubConfig(), StubProvider(), dm, glv, cur)
    (g, dm, glv, cur)
  }

  test("graphics type is AndroidGL") {
    val (g, _, _, _) = mkGraphics()
    assertEquals(g.graphicsType, Graphics.GraphicsType.AndroidGL)
  }

  test("isFullscreen always returns true") {
    val (g, _, _, _) = mkGraphics()
    assert(g.fullscreen)
  }

  test("supportsDisplayModeChange returns false") {
    val (g, _, _, _) = mkGraphics()
    assert(!g.supportsDisplayModeChange())
  }

  test("dimensions reflect width/height vars") {
    val (g, _, _, _) = mkGraphics()
    g._width = 1080
    g._height = 1920
    assertEquals(g.width, Pixels(1080))
    assertEquals(g.height, Pixels(1920))
    assertEquals(g.backBufferWidth, Pixels(1080))
    assertEquals(g.backBufferHeight, Pixels(1920))
    assertEquals(g.backBufferScale, 1f)
  }

  test("DPI/density delegates to display metrics ops") {
    val (g, dm, _, _) = mkGraphics()
    dm.ppiX = 320f
    dm.density = 2.0f
    assertEquals(g.ppiX, 320f)
    assertEquals(g.density, 2.0f)
  }

  test("safe insets update from display metrics ops") {
    val (g, _, _, _) = mkGraphics()
    g.updateSafeInsets("window")
    assertEquals(g.safeInsetLeft, Pixels(10))
    assertEquals(g.safeInsetTop, Pixels(20))
    assertEquals(g.safeInsetRight, Pixels(10))
    assertEquals(g.safeInsetBottom, Pixels(30))
  }

  test("frame timing updates correctly") {
    val (g, _, _, _) = mkGraphics()
    // First frame
    g.updateFrameTiming(false)
    assert(g.frameId == 0L)
    assert(g.deltaTime >= 0f)

    // Second frame
    g.updateFrameTiming(false)
    assert(g.frameId == 1L)
  }

  test("frame timing resets delta on resume") {
    val (g, _, _, _) = mkGraphics()
    g.updateFrameTiming(false)
    Thread.sleep(10)
    g.updateFrameTiming(true)
    assertEquals(g.deltaTime, 0f)
  }

  test("continuous rendering delegates to GL surface view") {
    val (g, _, glv, _) = mkGraphics()
    g.setContinuousRendering(false)
    assert(!glv.continuousRendering)
    assert(!g.continuousRendering)

    g.setContinuousRendering(true)
    assert(glv.continuousRendering)
    assert(g.continuousRendering)
  }

  test("requestRendering delegates to GL surface view") {
    val (g, _, glv, _) = mkGraphics()
    g.requestRendering()
    assert(glv.renderRequested)
  }

  test("display mode returns metrics from ops") {
    val (g, _, _, _) = mkGraphics()
    val dm           = g.displayMode
    assertEquals(dm.width, 1080)
    assertEquals(dm.height, 1920)
    assertEquals(dm.refreshRate, 60)
    assertEquals(dm.bitsPerPixel, 32) // 8+8+8+8
  }

  test("monitor stubs return primary monitor") {
    val (g, _, _, _) = mkGraphics()
    val primary      = g.primaryMonitor
    assertEquals(primary.name, "Primary Monitor")
    assertEquals(g.monitor, primary)
    assertEquals(g.monitors.length, 1)
  }

  test("buffer format reflects config") {
    val (g, _, _, _) = mkGraphics()
    val bf           = g.bufferFormat
    assertEquals(bf.r, 8)
    assertEquals(bf.g, 8)
    assertEquals(bf.b, 8)
    assertEquals(bf.a, 8)
    assertEquals(bf.depth, 16)
    assertEquals(bf.stencil, 0)
    assertEquals(bf.samples, 0)
  }

  test("updateBufferFormat changes buffer format") {
    val (g, _, _, _) = mkGraphics()
    g.updateBufferFormat(5, 6, 5, 0, 24, 8, 4, true)
    val bf = g.bufferFormat
    assertEquals(bf.r, 5)
    assertEquals(bf.g, 6)
    assertEquals(bf.b, 5)
    assertEquals(bf.a, 0)
    assertEquals(bf.depth, 24)
    assertEquals(bf.stencil, 8)
    assertEquals(bf.samples, 4)
    assert(bf.coverageSampling)
  }

  test("GL30 not available by default") {
    val (g, _, _, _) = mkGraphics()
    assert(!g.gl30Available)
    assert(!g.gl31Available)
    assert(!g.gl32Available)
    assert(g.gl30.isEmpty)
    assert(g.gl31.isEmpty)
    assert(g.gl32.isEmpty)
  }

  test("setFullscreenMode and setWindowedMode return false") {
    val (g, _, _, _) = mkGraphics()
    assert(!g.setFullscreenMode(Graphics.DisplayMode(1080, 1920, 60, 32)))
    assert(!g.setWindowedMode(Pixels(800), Pixels(600)))
  }

  test("newCursor returns empty") {
    val (g, _, _, _) = mkGraphics()
    assert(g.newCursor(null.asInstanceOf[sge.graphics.Pixmap], Pixels(0), Pixels(0)).isEmpty) // scalafix:ok
  }

  test("setSystemCursor delegates ordinal to cursor ops") {
    val (g, _, glv, cur) = mkGraphics()
    g.setSystemCursor(sge.graphics.Cursor.SystemCursor.Hand)
    assertEquals(cur.lastCursorType, sge.graphics.Cursor.SystemCursor.Hand.ordinal)
    assertEquals(cur.lastView, glv.view)
  }
}
