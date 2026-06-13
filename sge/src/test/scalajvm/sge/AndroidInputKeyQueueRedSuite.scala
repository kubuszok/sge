// SGE — ISS-519 RED suite: Android key events must be QUEUED, not dispatched immediately.
//
// libGDX DefaultAndroidInput queues key events on the UI thread (onKey, lines
// 528-604) and APPLIES them on the render thread in processEvents() (lines
// 415-499): keyDown/keyUp/keyTyped are fired to the processor THERE, and
// keyJustPressed/justPressedKeys are cleared at the START of processEvents
// (lines 421-424) then re-set during the drain (lines 436-437).
//
// The current AndroidInput.onKeyDown/onKeyUp/onKeyTyped dispatch SYNCHRONOUSLY
// on the calling thread and processEvents() has no key queue to drain — so:
//   (a) keys fire on the UI thread (race), and
//   (b) a key pressed before processEvents has its justPressed flag blindly
//       cleared by the next processEvents (line 339) before the frame sees it.
//
// This suite encodes the CORRECT queued contract and therefore FAILS RED
// against the current immediate-dispatch implementation.

package sge

import munit.FunSuite
import sge.Input.*
import sge.platform.android.*

class AndroidInputKeyQueueRedSuite extends FunSuite {

  // ── Stub ops (mirrors AndroidInputTest harness) ─────────────────────

  private class StubSensorOps extends SensorOps {
    override def accelerometerX:                              Float        = 0f
    override def accelerometerY:                              Float        = 0f
    override def accelerometerZ:                              Float        = 0f
    override def gyroscopeX:                                  Float        = 0f
    override def gyroscopeY:                                  Float        = 0f
    override def gyroscopeZ:                                  Float        = 0f
    override def azimuth:                                     Float        = 0f
    override def pitch:                                       Float        = 0f
    override def roll:                                        Float        = 0f
    override def rotationMatrix:                              Array[Float] = Array.fill(16)(0f)
    override def nativeOrientation:                           Int          = 1
    override def hasAccelerometer:                            Boolean      = true
    override def hasGyroscope:                                Boolean      = true
    override def hasCompass:                                  Boolean      = true
    override def hasRotationVector:                           Boolean      = false
    override def registerListeners(config: AndroidConfigOps): Unit         = ()
    override def unregisterListeners():                       Unit         = ()
  }

  private class StubInputMethodOps extends InputMethodOps {
    override def showKeyboard(inputType:    Int):                                                                                               Unit    = ()
    override def hideKeyboard():                                                                                                                Unit    = ()
    override def isKeyboardShown:                                                                                                               Boolean = false
    override def keyboardHeight:                                                                                                                Int     = 0
    override def setKeyboardHeight(height:  Int):                                                                                               Unit    = ()
    override def showTextInputDialog(title: String, text: String, hint: String, maxLength: Int, inputType: Int, callback: InputDialogCallback): Unit    = ()
    override def openNativeTextField(
      text:              String,
      selectionStart:    Int,
      selectionEnd:      Int,
      inputType:         Int,
      maxLength:         Int,
      placeholder:       String,
      maskInput:         Boolean,
      multiLine:         Boolean,
      preventCorrection: Boolean,
      autoComplete:      Array[String],
      onTextChanged:     (String, Int, Int) => Unit,
      onClose:           Boolean => Boolean,
      validator:         String => Boolean
    ):                                                        Unit    = ()
    override def closeNativeTextField(confirmative: Boolean): Unit    = ()
    override def isNativeTextFieldOpen:                       Boolean = false
    override def setView(view:                      AnyRef):  Unit    = ()
  }

  private class StubHapticsOps extends HapticsOps {
    override def vibrate(milliseconds:              Int):                                    Unit    = ()
    override def vibrateHaptic(vibrationType:       Int):                                    Unit    = ()
    override def vibrateWithIntensity(milliseconds: Int, intensity: Int, fallback: Boolean): Unit    = ()
    override def hasVibratorAvailable:                                                       Boolean = true
    override def hasHapticsSupport:                                                          Boolean = true
  }

  private class StubTouchInputOps extends TouchInputOps {
    override def getActionMasked(event: AnyRef):                    Int     = 0
    override def getActionIndex(event:  AnyRef):                    Int     = 0
    override def getPointerCount(event: AnyRef):                    Int     = 1
    override def getPointerId(event:    AnyRef, pointerIndex: Int): Int     = 0
    override def getX(event:            AnyRef, pointerIndex: Int): Int     = 0
    override def getY(event:            AnyRef, pointerIndex: Int): Int     = 0
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

  private def createInput(): AndroidInput =
    AndroidInput(
      AndroidConfigOps(),
      StubSensorOps(),
      StubInputMethodOps(),
      StubHapticsOps(),
      StubTouchInputOps(),
      StubLifecycleOps()
    )

  // ── Recording processor: captures ORDER/timing of key callbacks ─────

  /** Records every key callback the engine fires, in order, so the test can assert that NOTHING is dispatched before processEvents() drains the queue.
    */
  private class RecordingProcessor extends InputProcessor {
    val events: scala.collection.mutable.ArrayBuffer[String] = scala.collection.mutable.ArrayBuffer.empty

    override def keyDown(keycode: Key): Boolean = {
      events += s"keyDown:${keycode.toInt}"
      true
    }
    override def keyUp(keycode: Key): Boolean = {
      events += s"keyUp:${keycode.toInt}"
      true
    }
    override def keyTyped(character: Char): Boolean = {
      events += s"keyTyped:$character"
      true
    }
  }

  // ── Contract 1: keyDown is QUEUED, applied only by processEvents ────

  test("onKeyDown does NOT dispatch keyDown until processEvents drains the queue") {
    val input = createInput()
    val rec   = RecordingProcessor()
    input.setInputProcessor(rec)

    val k = Keys.A.toInt
    input.onKeyDown(k)

    // Discriminating check: on the BUGGY code onKeyDown dispatches synchronously
    // on the calling (UI) thread, so events already contains keyDown here.
    assertEquals(
      rec.events.toList,
      Nil,
      "key events must be QUEUED on the UI thread, not dispatched before processEvents (render thread)"
    )

    input.processEvents()

    assertEquals(rec.events.toList, List(s"keyDown:$k"), "processEvents must drain the queued keyDown to the processor")
    assert(input.isKeyJustPressed(Keys.A), "justPressed must be TRUE in the frame the keyDown is processed")
    assert(input.isKeyPressed(Keys.A), "key must be reported pressed after its down is processed")
  }

  // ── Contract 2: justPressed has a single-frame lifecycle via processEvents ──

  test("isKeyJustPressed is true the frame the queued keyDown is drained, then false next frame") {
    val input = createInput()
    val rec   = RecordingProcessor()
    input.setInputProcessor(rec)

    input.onKeyDown(Keys.SPACE.toInt)

    // BUG (b): justPressed is set immediately on the UI thread by onKeyDown,
    // then BLINDLY CLEARED at the start of the next processEvents — so a key
    // pressed before processEvents is cleared before the frame ever sees it.
    // The correct contract is the opposite: justPressed becomes true precisely
    // WHEN the queued event is drained.
    assert(!input.isKeyJustPressed(Keys.SPACE), "justPressed must not be set before the event is drained by processEvents")

    input.processEvents()
    assert(input.isKeyJustPressed(Keys.SPACE), "justPressed must be TRUE the frame the keyDown is processed")
    assert(input.isKeyPressed(Keys.SPACE), "key must be pressed after keyDown is processed")

    // Second frame with no new events: justPressed clears, pressed remains.
    input.processEvents()
    assert(!input.isKeyJustPressed(Keys.SPACE), "justPressed must clear on the next frame")
    assert(input.isKeyPressed(Keys.SPACE), "key remains pressed until a key-up is processed")
  }

  // ── Contract 3: keyUp is queued; pressed reflects drained state ────

  test("onKeyUp is queued and clears pressed only after processEvents drains it") {
    val input = createInput()
    val rec   = RecordingProcessor()
    input.setInputProcessor(rec)

    input.onKeyDown(Keys.B.toInt)
    input.processEvents()
    assert(input.isKeyPressed(Keys.B))
    rec.events.clear()

    input.onKeyUp(Keys.B.toInt)
    assertEquals(rec.events.toList, Nil, "keyUp must be QUEUED, not dispatched before processEvents")
    // Key is still pressed until the up event is drained on the render thread.
    assert(input.isKeyPressed(Keys.B), "key must remain pressed until the queued key-up is processed")

    input.processEvents()
    assertEquals(rec.events.toList, List(s"keyUp:${Keys.B.toInt}"), "processEvents must drain the queued keyUp")
    assert(!input.isKeyPressed(Keys.B), "key must be released after the queued key-up is processed")
  }

  // ── Contract 1 (typed): keyTyped is queued ─────────────────────────

  test("onKeyTyped does NOT dispatch keyTyped until processEvents drains the queue") {
    val input = createInput()
    val rec   = RecordingProcessor()
    input.setInputProcessor(rec)

    input.onKeyTyped('x')
    assertEquals(rec.events.toList, Nil, "keyTyped must be QUEUED, not dispatched before processEvents")

    input.processEvents()
    assertEquals(rec.events.toList, List("keyTyped:x"), "processEvents must drain the queued keyTyped")
  }

  // ── Ordering: queued events drain in submission order in one frame ──

  test("queued key events drain to the processor in submission order on processEvents") {
    val input = createInput()
    val rec   = RecordingProcessor()
    input.setInputProcessor(rec)

    val k = Keys.A.toInt
    input.onKeyDown(k)
    input.onKeyTyped('a')
    input.onKeyUp(k)

    // Nothing dispatched yet — all three are queued on the UI thread.
    assertEquals(rec.events.toList, Nil, "no key callbacks may fire before processEvents drains the queue")

    input.processEvents()
    assertEquals(
      rec.events.toList,
      List(s"keyDown:$k", "keyTyped:a", s"keyUp:$k"),
      "queued key events must drain in submission order during processEvents"
    )
  }
}
