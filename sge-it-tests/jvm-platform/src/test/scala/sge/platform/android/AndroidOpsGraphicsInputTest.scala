// SGE — Integration test: Graphics, input, cursor ops API interfaces
//
// Tests the newly added ops interfaces: CursorOps, DisplayMetricsOps,
// SensorOps, InputMethodOps, GLSurfaceViewOps.

package sge
package platform
package android

import munit.FunSuite

class AndroidOpsGraphicsInputTest extends FunSuite {

  // ── CursorOps ─────────────────────────────────────────────────────────

  test("CursorOps trait has expected methods") {
    val cls = classOf[CursorOps]
    assert(cls.getMethod("setSystemCursor", classOf[AnyRef], classOf[Int]) != null)
  }

  test("CursorOps constants are defined") {
    assertEquals(CursorOps.Arrow, 0)
    assertEquals(CursorOps.Ibeam, 1)
    assertEquals(CursorOps.Crosshair, 2)
    assertEquals(CursorOps.Hand, 3)
    assertEquals(CursorOps.HorizontalResize, 4)
    assertEquals(CursorOps.VerticalResize, 5)
    assertEquals(CursorOps.NWSEResize, 6)
    assertEquals(CursorOps.NESWResize, 7)
    assertEquals(CursorOps.AllResize, 8)
    assertEquals(CursorOps.NotAllowed, 9)
    assertEquals(CursorOps.None, 10)
  }

  // ── DisplayMetricsOps ─────────────────────────────────────────────────

  test("DisplayMetricsOps trait has expected methods") {
    val cls = classOf[DisplayMetricsOps]
    assert(cls.getMethod("ppiX") != null)
    assert(cls.getMethod("ppiY") != null)
    assert(cls.getMethod("ppcX") != null)
    assert(cls.getMethod("ppcY") != null)
    assert(cls.getMethod("density") != null)
    assert(cls.getMethod("safeInsetLeft") != null)
    assert(cls.getMethod("safeInsetTop") != null)
    assert(cls.getMethod("safeInsetRight") != null)
    assert(cls.getMethod("safeInsetBottom") != null)
    assert(cls.getMethod("updateMetrics", classOf[AnyRef]) != null)
    assert(cls.getMethod("updateSafeInsets", classOf[AnyRef]) != null)
    assert(cls.getMethod("displayMode", classOf[AnyRef], classOf[Int]) != null)
  }

  // ── SensorOps ─────────────────────────────────────────────────────────

  test("SensorOps trait has expected methods") {
    val cls = classOf[SensorOps]
    assert(cls.getMethod("registerListeners", classOf[AndroidConfigOps]) != null)
    assert(cls.getMethod("unregisterListeners") != null)
    assert(cls.getMethod("accelerometerX") != null)
    assert(cls.getMethod("accelerometerY") != null)
    assert(cls.getMethod("accelerometerZ") != null)
    assert(cls.getMethod("gyroscopeX") != null)
    assert(cls.getMethod("gyroscopeY") != null)
    assert(cls.getMethod("gyroscopeZ") != null)
    assert(cls.getMethod("azimuth") != null)
    assert(cls.getMethod("pitch") != null)
    assert(cls.getMethod("roll") != null)
    assert(cls.getMethod("rotationMatrix") != null)
    assert(cls.getMethod("nativeOrientation") != null)
    assert(cls.getMethod("hasAccelerometer") != null)
    assert(cls.getMethod("hasGyroscope") != null)
    assert(cls.getMethod("hasCompass") != null)
    assert(cls.getMethod("hasRotationVector") != null)
  }

  // ── InputMethodOps ────────────────────────────────────────────────────

  test("InputMethodOps trait has expected methods") {
    val cls = classOf[InputMethodOps]
    assert(cls.getMethod("showKeyboard", classOf[Int]) != null)
    assert(cls.getMethod("hideKeyboard") != null)
    assert(cls.getMethod("isKeyboardShown") != null)
    assert(cls.getMethod("keyboardHeight") != null)
    assert(cls.getMethod("setKeyboardHeight", classOf[Int]) != null)
    assert(
      cls.getMethod(
        "showTextInputDialog",
        classOf[String],
        classOf[String],
        classOf[String],
        classOf[Int],
        classOf[Int],
        classOf[InputDialogCallback]
      ) != null
    )
    assert(cls.getMethod("setView", classOf[AnyRef]) != null)
  }

  test("InputDialogCallback trait has expected methods") {
    val cls = classOf[InputDialogCallback]
    assert(cls.getMethod("onInput", classOf[String]) != null)
    assert(cls.getMethod("onCancel") != null)
  }

  // ── GLSurfaceViewOps ──────────────────────────────────────────────────

  test("GLSurfaceViewOps trait has expected methods") {
    val cls = classOf[GLSurfaceViewOps]
    assert(cls.getMethod("view") != null)
    assert(cls.getMethod("onPause") != null)
    assert(cls.getMethod("onResume") != null)
    assert(cls.getMethod("requestRender") != null)
    assert(cls.getMethod("setContinuousRendering", classOf[Boolean]) != null)
    assert(cls.getMethod("queueEvent", classOf[Runnable]) != null)
    assert(cls.getMethod("setPreserveEGLContextOnPause", classOf[Boolean]) != null)
    assert(cls.getMethod("setFocusable", classOf[Boolean]) != null)
    assert(cls.getMethod("glEsVersion") != null)
    assert(cls.getMethod("checkGL20Support") != null)
  }

  // ── AndroidPlatformProvider extended methods ──────────────────────────

  test("AndroidPlatformProvider has graphics/input factory methods") {
    val cls = classOf[AndroidPlatformProvider]
    assert(cls.getMethod("createCursor", classOf[AnyRef]) != null)
    assert(cls.getMethod("createDisplayMetrics", classOf[AnyRef]) != null)
    assert(cls.getMethod("createSensors", classOf[AnyRef], classOf[AnyRef]) != null)
    assert(cls.getMethod("createInputMethod", classOf[AnyRef], classOf[AnyRef]) != null)
    assert(
      cls.getMethod(
        "createGLSurfaceView",
        classOf[AnyRef],
        classOf[AndroidConfigOps],
        classOf[ResolutionStrategyOps]
      ) != null
    )
  }

  // ── Stub implementations ──────────────────────────────────────────────

  test("stub CursorOps implementation works") {
    var lastCursorType = -1
    val stub           = new CursorOps {
      override def setSystemCursor(view: AnyRef, cursorType: Int): Unit =
        lastCursorType = cursorType
    }
    stub.setSystemCursor("fakeView", CursorOps.Hand)
    assertEquals(lastCursorType, CursorOps.Hand)
  }

  test("stub InputDialogCallback works") {
    var result: String = null
    var cancelled = false
    val callback  = new InputDialogCallback {
      override def onInput(text: String): Unit = result = text
      override def onCancel():            Unit = cancelled = true
    }
    callback.onInput("hello")
    assertEquals(result, "hello")
    assert(!cancelled)
    callback.onCancel()
    assert(cancelled)
  }
}
