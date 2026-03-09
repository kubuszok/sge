// SGE — Tests for AndroidInput adapter wiring
//
// Verifies that AndroidInput correctly delegates to ops interfaces
// and processes touch/scroll events through the InputProcessor.

package sge

import munit.FunSuite
import sge.Input.*
import sge.platform.android.*

class AndroidInputTest extends FunSuite {

  // ── Stub ops ────────────────────────────────────────────────────────

  private class StubSensorOps extends SensorOps {
    var accX:       Float   = 1.5f
    var accY:       Float   = 2.5f
    var accZ:       Float   = 9.8f
    var gyroX:      Float   = 0.1f
    var gyroY:      Float   = 0.2f
    var gyroZ:      Float   = 0.3f
    var _azimuth:   Float   = 45f
    var _pitch:     Float   = 10f
    var _roll:      Float   = 5f
    var registered: Boolean = false

    override def accelerometerX:                              Float        = accX
    override def accelerometerY:                              Float        = accY
    override def accelerometerZ:                              Float        = accZ
    override def gyroscopeX:                                  Float        = gyroX
    override def gyroscopeY:                                  Float        = gyroY
    override def gyroscopeZ:                                  Float        = gyroZ
    override def azimuth:                                     Float        = _azimuth
    override def pitch:                                       Float        = _pitch
    override def roll:                                        Float        = _roll
    override def rotationMatrix:                              Array[Float] = Array.fill(16)(0f)
    override def nativeOrientation:                           Int          = 1 // portrait
    override def hasAccelerometer:                            Boolean      = true
    override def hasGyroscope:                                Boolean      = true
    override def hasCompass:                                  Boolean      = true
    override def hasRotationVector:                           Boolean      = false
    override def registerListeners(config: AndroidConfigOps): Unit         = registered = true
    override def unregisterListeners():                       Unit         = registered = false
  }

  private class StubInputMethodOps extends InputMethodOps {
    var keyboardShown:   Boolean = false
    var lastInputType:   Int     = -1
    var _keyboardHeight: Int     = 0

    override def showKeyboard(inputType: Int): Unit = {
      keyboardShown = true
      lastInputType = inputType
    }
    override def hideKeyboard():                                                                                                                Unit    = keyboardShown = false
    override def isKeyboardShown:                                                                                                               Boolean = keyboardShown
    override def keyboardHeight:                                                                                                                Int     = _keyboardHeight
    override def setKeyboardHeight(height: Int):                                                                                                Unit    = _keyboardHeight = height
    override def showTextInputDialog(title: String, text: String, hint: String, maxLength: Int, inputType: Int, callback: InputDialogCallback): Unit    =
      callback.onInput("test-result")
    override def setView(view: AnyRef): Unit = ()
  }

  private class StubHapticsOps extends HapticsOps {
    var lastMillis:        Int = -1
    var lastVibrationType: Int = -1
    var lastIntensity:     Int = -1

    override def vibrate(milliseconds:        Int):                                          Unit = lastMillis = milliseconds
    override def vibrateHaptic(vibrationType: Int):                                          Unit = lastVibrationType = vibrationType
    override def vibrateWithIntensity(milliseconds: Int, intensity: Int, fallback: Boolean): Unit = {
      lastMillis = milliseconds
      lastIntensity = intensity
    }
    override def hasVibratorAvailable: Boolean = true
    override def hasHapticsSupport:    Boolean = true
  }

  private class StubTouchInputOps extends TouchInputOps {
    override def getActionMasked(event: AnyRef):                    Int     = 0
    override def getActionIndex(event:  AnyRef):                    Int     = 0
    override def getPointerCount(event: AnyRef):                    Int     = 1
    override def getPointerId(event:    AnyRef, pointerIndex: Int): Int     = 0
    override def getX(event:            AnyRef, pointerIndex: Int): Int     = 100
    override def getY(event:            AnyRef, pointerIndex: Int): Int     = 200
    override def getPressure(event:     AnyRef, pointerIndex: Int): Float   = 1.0f
    override def getButtonState(event:  AnyRef):                    Int     = 0
    override def getSource(event:       AnyRef):                    Int     = 0
    override def getAxisValue(event:    AnyRef, axis:         Int): Float   = 0f
    override def supportsMultitouch:                                Boolean = true
  }

  private class StubLifecycleOps extends AndroidLifecycleOps {
    override def runOnUiThread(runnable: Runnable):         Unit          = runnable.run()
    override def useImmersiveMode(use:   Boolean):          Unit          = ()
    override def getAndroidVersion():                       Int           = 33
    override def getNativeHeapAllocatedSize():              Long          = 0L
    override def finish():                                  Unit          = ()
    override def hasHardwareKeyboard():                     Boolean       = false
    override def setGLSurfaceView(view:  GLSurfaceViewOps): Unit          = ()
    override def getGLSurfaceView():                        AnyRef | Null = null
    override def resumeGLSurfaceView():                     Unit          = ()
    override def pauseGLSurfaceView():                      Unit          = ()
  }

  private def createInput(): (AndroidInput, StubSensorOps, StubInputMethodOps, StubHapticsOps) = {
    val config      = AndroidConfigOps()
    val sensors     = StubSensorOps()
    val inputMethod = StubInputMethodOps()
    val haptics     = StubHapticsOps()
    val touch       = StubTouchInputOps()
    val lifecycle   = StubLifecycleOps()
    val input       = AndroidInput(config, sensors, inputMethod, haptics, touch, lifecycle)
    (input, sensors, inputMethod, haptics)
  }

  // ── Sensor delegation ──────────────────────────────────────────────

  test("accelerometer values delegate to SensorOps") {
    val (input, sensors, _, _) = createInput()
    assertEqualsFloat(input.getAccelerometerX(), 1.5f, 0.001f)
    assertEqualsFloat(input.getAccelerometerY(), 2.5f, 0.001f)
    assertEqualsFloat(input.getAccelerometerZ(), 9.8f, 0.001f)
    sensors.accX = 3.0f
    assertEqualsFloat(input.getAccelerometerX(), 3.0f, 0.001f)
  }

  test("gyroscope values delegate to SensorOps") {
    val (input, _, _, _) = createInput()
    assertEqualsFloat(input.getGyroscopeX(), 0.1f, 0.001f)
    assertEqualsFloat(input.getGyroscopeY(), 0.2f, 0.001f)
    assertEqualsFloat(input.getGyroscopeZ(), 0.3f, 0.001f)
  }

  test("compass values delegate to SensorOps") {
    val (input, _, _, _) = createInput()
    assertEqualsFloat(input.getAzimuth(), 45f, 0.001f)
    assertEqualsFloat(input.getPitch(), 10f, 0.001f)
    assertEqualsFloat(input.getRoll(), 5f, 0.001f)
  }

  test("native orientation returns portrait when SensorOps says 1") {
    val (input, _, _, _) = createInput()
    assertEquals(input.getNativeOrientation(), Orientation.Portrait)
  }

  // ── Key state ──────────────────────────────────────────────────────

  test("isKeyPressed returns false initially") {
    val (input, _, _, _) = createInput()
    assert(!input.isKeyPressed(Keys.A))
    assert(!input.isKeyPressed(Keys.ANY_KEY))
  }

  test("onKeyDown/onKeyUp track key state") {
    val (input, _, _, _) = createInput()
    input.onKeyDown(Keys.A.toInt)
    assert(input.isKeyPressed(Keys.A))
    assert(input.isKeyPressed(Keys.ANY_KEY))
    assert(input.isKeyJustPressed(Keys.A))

    input.onKeyUp(Keys.A.toInt)
    assert(!input.isKeyPressed(Keys.A))
    assert(!input.isKeyPressed(Keys.ANY_KEY))
  }

  test("processEvents clears just-pressed state") {
    val (input, _, _, _) = createInput()
    input.onKeyDown(Keys.SPACE.toInt)
    assert(input.isKeyJustPressed(Keys.SPACE))

    input.processEvents()
    assert(!input.isKeyJustPressed(Keys.SPACE))
    // But the key is still pressed (held down)
    assert(input.isKeyPressed(Keys.SPACE))
  }

  // ── Caught keys ────────────────────────────────────────────────────

  test("setCatchKey/isCatchKey") {
    val (input, _, _, _) = createInput()
    assert(!input.isCatchKey(Keys.BACK))
    input.setCatchKey(Keys.BACK, true)
    assert(input.isCatchKey(Keys.BACK))
    input.setCatchKey(Keys.BACK, false)
    assert(!input.isCatchKey(Keys.BACK))
  }

  // ── Keyboard visibility ────────────────────────────────────────────

  test("setOnscreenKeyboardVisible shows/hides keyboard") {
    val (input, _, inputMethod, _) = createInput()
    input.setOnscreenKeyboardVisible(true)
    assert(inputMethod.keyboardShown)
    input.setOnscreenKeyboardVisible(false)
    assert(!inputMethod.keyboardShown)
  }

  test("setOnscreenKeyboardVisible with type passes correct input type") {
    val (input, _, inputMethod, _) = createInput()
    input.setOnscreenKeyboardVisible(true, OnscreenKeyboardType.NumberPad)
    assert(inputMethod.keyboardShown)
    assertEquals(inputMethod.lastInputType, 0x00000002) // TYPE_CLASS_NUMBER
  }

  // ── Text input ─────────────────────────────────────────────────────

  test("getTextInput delegates to InputMethodOps") {
    val (input, _, _, _) = createInput()
    var result: String = ""
    input.getTextInput(
      new TextInputListener {
        override def input(text: String): Unit = result = text
        override def canceled():          Unit = ()
      },
      "Title",
      "initial",
      "hint"
    )
    assertEquals(result, "test-result")
  }

  // ── Haptics ────────────────────────────────────────────────────────

  test("vibrate delegates to HapticsOps") {
    val (input, _, _, haptics) = createInput()
    input.vibrate(200)
    assertEquals(haptics.lastMillis, 200)
  }

  test("vibrate with amplitude delegates to HapticsOps") {
    val (input, _, _, haptics) = createInput()
    input.vibrate(300, 128, true)
    assertEquals(haptics.lastMillis, 300)
    assertEquals(haptics.lastIntensity, 128)
  }

  test("vibrate with VibrationType delegates to HapticsOps") {
    val (input, _, _, haptics) = createInput()
    input.vibrate(VibrationType.HEAVY)
    assertEquals(haptics.lastVibrationType, 2) // HEAVY.ordinal
  }

  // ── Peripheral availability ────────────────────────────────────────

  test("isPeripheralAvailable checks correct ops") {
    val (input, _, _, _) = createInput()
    assert(input.isPeripheralAvailable(Peripheral.Accelerometer))
    assert(input.isPeripheralAvailable(Peripheral.Gyroscope))
    assert(input.isPeripheralAvailable(Peripheral.Compass))
    assert(!input.isPeripheralAvailable(Peripheral.RotationVector))
    assert(input.isPeripheralAvailable(Peripheral.Vibrator))
    assert(input.isPeripheralAvailable(Peripheral.HapticFeedback))
    assert(input.isPeripheralAvailable(Peripheral.MultitouchScreen))
    assert(input.isPeripheralAvailable(Peripheral.OnscreenKeyboard))
    assert(!input.isPeripheralAvailable(Peripheral.HardwareKeyboard))
  }

  // ── Touch event processing ─────────────────────────────────────────

  test("touch events are dispatched to InputProcessor") {
    val (input, _, _, _) = createInput()
    var touchDownCalled  = false
    var touchUpCalled    = false

    input.setInputProcessor(
      new InputProcessor {
        override def touchDown(screenX: Pixels, screenY: Pixels, pointer: Int, button: Button): Boolean = {
          touchDownCalled = true
          assertEquals(screenX.toInt, 50)
          assertEquals(screenY.toInt, 100)
          true
        }
        override def touchUp(screenX: Pixels, screenY: Pixels, pointer: Int, button: Button): Boolean = {
          touchUpCalled = true
          true
        }
      }
    )

    // Simulate events via input state
    input.inputState.synchronized {
      input.inputState.postTouchEvent(TouchInputOps.TOUCH_DOWN, 50, 100, 0, 0, System.nanoTime())
      input.inputState.postTouchEvent(TouchInputOps.TOUCH_UP, 50, 100, 0, 0, System.nanoTime())
    }

    input.processEvents()
    assert(touchDownCalled, "touchDown should have been called")
    assert(touchUpCalled, "touchUp should have been called")
  }

  test("scroll events are dispatched to InputProcessor") {
    val (input, _, _, _) = createInput()
    var scrollCalled     = false
    var scrollX: Float = 0f
    var scrollY: Float = 0f

    input.setInputProcessor(
      new InputProcessor {
        override def scrolled(amountX: Float, amountY: Float): Boolean = {
          scrollCalled = true
          scrollX = amountX
          scrollY = amountY
          true
        }
      }
    )

    input.inputState.synchronized {
      input.inputState.postScrollEvent(1, -1, System.nanoTime())
    }

    input.processEvents()
    assert(scrollCalled, "scrolled should have been called")
    assertEqualsFloat(scrollX, 1f, 0.001f)
    assertEqualsFloat(scrollY, -1f, 0.001f)
  }

  test("justTouched returns true after TOUCH_DOWN event in same frame") {
    val (input, _, _, _) = createInput()

    input.inputState.synchronized {
      input.inputState.postTouchEvent(TouchInputOps.TOUCH_DOWN, 10, 20, 0, 0, System.nanoTime())
    }

    input.processEvents()
    assert(input.justTouched())

    // Next frame clears it
    input.processEvents()
    assert(!input.justTouched())
  }

  // ── Sensor registration ────────────────────────────────────────────

  test("registerSensors delegates to SensorOps") {
    val (input, sensors, _, _) = createInput()
    assert(!sensors.registered)
    input.registerSensors()
    assert(sensors.registered)
    input.unregisterSensors()
    assert(!sensors.registered)
  }

  // ── Cursor (no-op on Android) ──────────────────────────────────────

  test("cursor methods are no-ops") {
    val (input, _, _, _) = createInput()
    input.setCursorCatched(true)
    assert(!input.isCursorCatched())
    input.setCursorPosition(Pixels(0), Pixels(0))
    // no crash = pass
  }
}
