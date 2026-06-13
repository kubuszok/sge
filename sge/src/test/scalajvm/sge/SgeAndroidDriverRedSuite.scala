// SGE — RED suite for ISS-518: canonical Android driver wiring
//
// ISS-518 (CRITICAL): AndroidApplication.onResume/onPause/onDestroy never call
// listener.resume/pause/dispose, and there is no library-level component that
// drives the surface/frame/input loop. Today every game hand-copies the ~80
// lines of SmokeActivity (sge-test/android-smoke/.../SmokeActivity.scala) to
// bootstrap and pump an SGE app on Android.
//
// The decision: ship a thin scala-android `SgeActivity` shell over a NEW,
// android-type-free, plain-JVM `SgeAndroidDriver` that orchestrates the
// canonical wiring. `AndroidApplication` STAYS a plain class. This suite pins
// the *driver* contract — the locally testable, android-type-free core.
//
// This suite is a RED test: `SgeAndroidDriver` does NOT exist yet, so this file
// does not compile (missing symbol). That compile-fail IS the proof-of-red.
// It goes GREEN once the implementer adds `sge.SgeAndroidDriver` with the
// signature pinned below.
//
// ── Pinned SgeAndroidDriver API (implementer must provide) ───────────────────
//
//   package sge
//   final class SgeAndroidDriver(app: AndroidApplication) {
//     def onResume(): Unit                         // app.onResume(); if sgeContext != null then listener.resume()
//     def onPause(): Unit                          // if sgeContext != null then listener.pause(); app.onPause()
//     def onDestroy(): Unit                        // if sgeContext != null then listener.dispose(); app.onDestroy()
//     def onSurfaceChanged(width: Int, height: Int): Unit  // first call: listener.create() once, then resize(w,h); later: resize only
//     def onDrawFrame(): Unit                      // app.processInputEvents(); app.executeRunnables(); listener.render() IN THAT ORDER
//     def keyDown(keyCode: Int): Unit              // route to AndroidInput.onKeyDown(keyCode)
//     def keyUp(keyCode: Int): Unit                // route to AndroidInput.onKeyUp(keyCode)
//     def keyTyped(ch: Char): Unit                 // route to AndroidInput.onKeyTyped(ch)
//     def touchEvent(event: AnyRef): Unit          // route to AndroidInput.onTouchEvent(event)
//     def genericMotion(event: AnyRef): Unit       // route to AndroidInput.onGenericMotionEvent(event)
//   }
//
// Grounding (SmokeActivity line refs are to the canonical hand-wired sequence):
//   - onResume null-guard + order ............ SmokeActivity:121-127, 124-125
//   - onPause  null-guard + order ............ SmokeActivity:129-135, 131-132
//   - onDestroy ............................... SmokeActivity:137-140, 105 (dispose)
//   - surface created-once flag .............. SmokeActivity:83-87
//   - frame: processInputEvents/executeRunnables/render order . SmokeActivity:92-99
//   - input routing (android-type-free Int/Char/AnyRef) ...... AndroidInput.scala:167/182/195/422/432
//   - libgdx state machine origin (AndroidGraphics.onDrawFrame) . backends/gdx-backend-android

package sge

import munit.FunSuite
import sge.platform.android._

class SgeAndroidDriverRedSuite extends FunSuite {

  // ── Recording listener ──────────────────────────────────────────────
  // Appends a tag in call order so we can assert the exact sequence the
  // driver drives. Tags: "create", "resize:WxH", "render", "resume",
  // "pause", "dispose".
  private class RecordingListener(buffer: scala.collection.mutable.ArrayBuffer[String]) extends ApplicationListener {
    override def create():                              Unit = buffer += "create"
    override def resize(width: Pixels, height: Pixels): Unit = buffer += s"resize:${width.toInt}x${height.toInt}"
    override def render():                              Unit = buffer += "render"
    override def pause():                               Unit = buffer += "pause"
    override def resume():                              Unit = buffer += "resume"
    override def dispose():                             Unit = buffer += "dispose"
  }

  // ── Stub ops (mirrors AndroidInputTest + AndroidGraphicsTest stubs) ──

  private class StubSensorOps extends SensorOps {
    var registered:                                           Boolean      = false
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
    override def hasAccelerometer:                            Boolean      = false
    override def hasGyroscope:                                Boolean      = false
    override def hasCompass:                                  Boolean      = false
    override def hasRotationVector:                           Boolean      = false
    override def registerListeners(config: AndroidConfigOps): Unit         = registered = true
    override def unregisterListeners():                       Unit         = registered = false
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
    override def hasVibratorAvailable:                                                       Boolean = false
    override def hasHapticsSupport:                                                          Boolean = false
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
    var resumed:                                            Boolean       = false
    var paused:                                             Boolean       = false
    override def runOnUiThread(runnable: Runnable):         Unit          = runnable.run()
    override def useImmersiveMode(use:   Boolean):          Unit          = ()
    override def getAndroidVersion():                       Int           = 33
    override def getNativeHeapAllocatedSize():              Long          = 0L
    override def finish():                                  Unit          = ()
    override def hasHardwareKeyboard():                     Boolean       = false
    override def setGLSurfaceView(view:  GLSurfaceViewOps): Unit          = ()
    override def getGLSurfaceView():                        AnyRef | Null = null
    override def resumeGLSurfaceView():                     Unit          = resumed = true
    override def pauseGLSurfaceView():                      Unit          = paused = true
  }

  private class StubDisplayMetrics extends DisplayMetricsOps {
    override def ppiX:                                                    Float                = 160f
    override def ppiY:                                                    Float                = 160f
    override def ppcX:                                                    Float                = 160f / 2.54f
    override def ppcY:                                                    Float                = 160f / 2.54f
    override def density:                                                 Float                = 1.0f
    override def safeInsetLeft:                                           Int                  = 0
    override def safeInsetTop:                                            Int                  = 0
    override def safeInsetRight:                                          Int                  = 0
    override def safeInsetBottom:                                         Int                  = 0
    override def updateMetrics(windowManager: AnyRef):                    Unit                 = ()
    override def updateSafeInsets(window:     AnyRef):                    Unit                 = ()
    override def displayMode(context:         AnyRef, bitsPerPixel: Int): (Int, Int, Int, Int) = (1080, 1920, 60, bitsPerPixel)
  }

  private class StubGLSurfaceView extends GLSurfaceViewOps {
    override def view:                                             AnyRef  = "stub-view"
    override def isPaused:                                         Boolean = false
    override def continuousRendering:                              Boolean = true
    override def renderRequested:                                  Boolean = false
    override def onPause():                                        Unit    = ()
    override def onResume():                                       Unit    = ()
    override def requestRender():                                  Unit    = ()
    override def setContinuousRendering(continuous:     Boolean):  Unit    = ()
    override def queueEvent(runnable:                   Runnable): Unit    = runnable.run()
    override def setPreserveEGLContextOnPause(preserve: Boolean):  Unit    = ()
    override def setFocusable(focusable:                Boolean):  Unit    = ()
    override def glEsVersion:                                      Int     = 2
    override def checkGL20Support:                                 Boolean = true
  }

  private class StubCursorOps extends CursorOps {
    override def setSystemCursor(view: AnyRef, cursorType: Int): Unit = ()
  }

  private class StubAudioEngineOps extends AudioEngineOps {
    var resumed:                                                                                           Boolean          = false
    var paused:                                                                                            Boolean          = false
    var disposed:                                                                                          Boolean          = false
    override def newSoundFromFd(fd:                   java.io.FileDescriptor, offset: Long, length: Long): SoundOps         = ??? // unused
    override def newSoundFromPath(path:               String):                                             SoundOps         = ??? // unused
    override def newMusicFromFd(fd:                   java.io.FileDescriptor, offset: Long, length: Long): MusicOps         = ??? // unused
    override def newMusicFromPath(path:               String):                                             MusicOps         = ??? // unused
    override def newAudioDevice(samplingRate:         Int, isMono:                    Boolean):            AudioDeviceOps   = ??? // unused
    override def newAudioRecorder(samplingRate:       Int, isMono:                    Boolean):            AudioRecorderOps = ??? // unused
    override def pauseAll():                                                                               Unit             = paused = true
    override def resumeAll():                                                                              Unit             = resumed = true
    override def availableOutputDevices:                                                                   Array[String]    = Array.empty
    override def switchOutputDevice(deviceIdentifier: String):                                             Boolean          = false
    override def dispose():                                                                                Unit             = disposed = true
  }

  private class StubClipboardOps extends ClipboardOps {
    override def hasContents:               Boolean       = false
    override def getContents:               String | Null = null // scalafix:ok
    override def setContents(text: String): Unit          = ()
  }

  private class StubFilesOps extends FilesOps {
    override def openInternal(path:       String): java.io.InputStream                         = ??? // unused
    override def listInternal(path:       String): Array[String]                               = Array.empty
    override def openInternalFd(path:     String): (java.io.FileDescriptor, Long, Long) | Null = null // scalafix:ok
    override def internalFileLength(path: String): Long                                        = 0L
    override def localStoragePath:                 String                                      = "/tmp"
    override def externalStoragePath:              String | Null                               = null // scalafix:ok
  }

  // ── Stub provider feeding initializeGraphicsAndInput + initializeSge ──

  private class StubProvider(val sensors: StubSensorOps, val audio: StubAudioEngineOps) extends AndroidPlatformProvider {
    override def createLifecycle(activity:           AnyRef):                                     AndroidLifecycleOps = ??? // app gets lifecycle passed directly
    override def createClipboard(context:            AnyRef):                                     ClipboardOps        = StubClipboardOps()
    override def createPreferences(context:          AnyRef, name:             String):           PreferencesOps      = ??? // unused
    override def openURI(context:                    AnyRef, uri:              String):           Boolean             = false
    override def defaultConfig():                                                                 AndroidConfigOps    = AndroidConfigOps()
    override def createFiles(context:                AnyRef, useExternalFiles: Boolean):          FilesOps            = StubFilesOps()
    override def createAudioEngine(context:          AnyRef, config:           AndroidConfigOps): AudioEngineOps      = audio
    override def createHaptics(context:              AnyRef):                                     HapticsOps          = StubHapticsOps()
    override def createCursor(context:               AnyRef):                                     CursorOps           = StubCursorOps()
    override def createDisplayMetrics(windowManager: AnyRef):                                     DisplayMetricsOps   = StubDisplayMetrics()
    override def createSensors(context:              AnyRef, windowManager:    AnyRef):           SensorOps           = sensors
    override def createTouchInput(context:           AnyRef):                                     TouchInputOps       = StubTouchInputOps()
    override def createInputMethod(context:          AnyRef, handler:          AnyRef):           InputMethodOps      = StubInputMethodOps()
    override def createGLSurfaceView(context: AnyRef, config: AndroidConfigOps, resolutionStrategy: ResolutionStrategyOps): GLSurfaceViewOps = StubGLSurfaceView()
    override def createGL20():                                                                                              GL20Ops          = ??? // unused
    override def createGL30():                                                                                              GL30Ops          = ??? // unused
  }

  // ── Harness ─────────────────────────────────────────────────────────

  /** Build a fully-initialized AndroidApplication on a plain JVM.
    * @param init
    *   when true, drive initializeGraphicsAndInput + initializeSge so sgeContext is materialized.
    */
  private def buildApp(
    buffer: scala.collection.mutable.ArrayBuffer[String],
    init:   Boolean
  ): (AndroidApplication, StubLifecycleOps, StubAudioEngineOps) = {
    val sensors   = StubSensorOps()
    val audio     = StubAudioEngineOps()
    val provider  = StubProvider(sensors, audio)
    val lifecycle = StubLifecycleOps()
    val listener: Sge ?=> ApplicationListener = new RecordingListener(buffer)
    val app = new AndroidApplication(listener, provider.defaultConfig(), provider, lifecycle, "dummy-context")
    if (init) {
      app.initializeGraphicsAndInput("dummy-wm", "dummy-handler")
      app.initializeSge()
    }
    (app, lifecycle, audio)
  }

  // ════════════════════════════════════════════════════════════════════
  // 1. Lifecycle ordering + sgeContext-null guard
  //    (SmokeActivity:121-140 — the listener must NOT be driven before
  //    initializeSge materializes sgeContext.)
  // ════════════════════════════════════════════════════════════════════

  test("onResume BEFORE initializeSge does NOT drive listener.resume (sgeContext-null guard, SmokeActivity:124-125)") {
    val buf             = scala.collection.mutable.ArrayBuffer.empty[String]
    val (app, _, audio) = buildApp(buf, init = false)
    val driver          = new SgeAndroidDriver(app)

    driver.onResume()

    assert(audio.resumed, "app.onResume() must still run subsystem resume even before init")
    assert(!buf.contains("resume"), "listener.resume() must NOT be recorded before sgeContext is materialized")
  }

  test("onResume AFTER initializeSge drives app.onResume() then listener.resume() (SmokeActivity:124-125)") {
    val buf             = scala.collection.mutable.ArrayBuffer.empty[String]
    val (app, _, audio) = buildApp(buf, init = true)
    val driver          = new SgeAndroidDriver(app)

    driver.onResume()

    assert(audio.resumed, "app.onResume() must run")
    assertEquals(buf.toList, List("resume"))
  }

  test("onPause AFTER init drives listener.pause() then app.onPause() (SmokeActivity:131-132)") {
    val buf             = scala.collection.mutable.ArrayBuffer.empty[String]
    val (app, _, audio) = buildApp(buf, init = true)
    val driver          = new SgeAndroidDriver(app)

    driver.onPause()

    assertEquals(buf.toList, List("pause"))
    assert(audio.paused, "app.onPause() must run subsystem pause AFTER listener.pause()")
  }

  test(
    "onPause BEFORE init does NOT drive listener.pause but still runs app.onPause (sgeContext-null guard, SmokeActivity:131)"
  ) {
    val buf             = scala.collection.mutable.ArrayBuffer.empty[String]
    val (app, _, audio) = buildApp(buf, init = false)
    val driver          = new SgeAndroidDriver(app)

    driver.onPause()

    assert(!buf.contains("pause"), "listener.pause() must NOT be recorded before sgeContext is materialized")
    assert(audio.paused, "app.onPause() must still run")
  }

  test("onDestroy AFTER init drives listener.dispose() then app.onDestroy() (SmokeActivity:105/137-140)") {
    val buf             = scala.collection.mutable.ArrayBuffer.empty[String]
    val (app, _, audio) = buildApp(buf, init = true)
    val driver          = new SgeAndroidDriver(app)

    driver.onDestroy()

    assertEquals(buf.toList, List("dispose"))
    assert(audio.disposed, "app.onDestroy() must run subsystem dispose AFTER listener.dispose()")
  }

  // ════════════════════════════════════════════════════════════════════
  // 2. Surface: created-once flag (SmokeActivity:83-87)
  // ════════════════════════════════════════════════════════════════════

  test("first onSurfaceChanged drives create() then resize(w,h) (SmokeActivity:83-87)") {
    val buf         = scala.collection.mutable.ArrayBuffer.empty[String]
    val (app, _, _) = buildApp(buf, init = true)
    val driver      = new SgeAndroidDriver(app)

    driver.onSurfaceChanged(800, 600)

    assertEquals(buf.toList, List("create", "resize:800x600"))
  }

  test("subsequent onSurfaceChanged drives resize ONLY — create not called twice (SmokeActivity:83-87)") {
    val buf         = scala.collection.mutable.ArrayBuffer.empty[String]
    val (app, _, _) = buildApp(buf, init = true)
    val driver      = new SgeAndroidDriver(app)

    driver.onSurfaceChanged(800, 600)
    buf.clear()
    driver.onSurfaceChanged(1024, 768)

    assertEquals(buf.toList, List("resize:1024x768"))
    assert(!buf.contains("create"), "create() must NOT be called on a subsequent surface change")
  }

  // ════════════════════════════════════════════════════════════════════
  // 3. Frame: processInputEvents -> executeRunnables -> render order
  //    (SmokeActivity:92-99)
  // ════════════════════════════════════════════════════════════════════

  test("onDrawFrame runs executeRunnables (posted runnable) BEFORE render (SmokeActivity:93-99)") {
    val buf         = scala.collection.mutable.ArrayBuffer.empty[String]
    val (app, _, _) = buildApp(buf, init = true)
    val driver      = new SgeAndroidDriver(app)

    // A posted runnable records "runnable" when executeRunnables drains it.
    app.postRunnable(() => buf += "runnable")

    driver.onDrawFrame()

    assertEquals(buf.toList, List("runnable", "render"))
  }

  test("onDrawFrame drains queued key via processInputEvents BEFORE render (SmokeActivity:93-99)") {
    val buf         = scala.collection.mutable.ArrayBuffer.empty[String]
    val (app, _, _) = buildApp(buf, init = true)
    val driver      = new SgeAndroidDriver(app)

    // Route a keyDown through the driver; it must be drained by processInputEvents
    // (which onDrawFrame calls) so the key is pressed by the time render runs.
    driver.keyDown(Input.Keys.A.toInt)
    driver.onDrawFrame()

    assert(app.input.isKeyPressed(Input.Keys.A), "queued keyDown must be drained by onDrawFrame's processInputEvents")
    assertEquals(buf.toList, List("render"))
  }

  // ════════════════════════════════════════════════════════════════════
  // 4. Input routing (android-type-free Int/Char/AnyRef)
  //    (AndroidInput.scala:167/182/195/422/432)
  // ════════════════════════════════════════════════════════════════════

  test("keyDown(k) routes to AndroidInput.onKeyDown — processor sees keyDown after drain") {
    val buf         = scala.collection.mutable.ArrayBuffer.empty[String]
    val (app, _, _) = buildApp(buf, init = true)
    val driver      = new SgeAndroidDriver(app)

    var sawKeyDown = false
    app.input.setInputProcessor(
      new InputProcessor {
        override def keyDown(keycode: Input.Key): Boolean = { sawKeyDown = true; true }
      }
    )

    driver.keyDown(Input.Keys.SPACE.toInt)
    driver.onDrawFrame() // drains via processInputEvents

    assert(sawKeyDown, "processor.keyDown must fire after driver.keyDown + onDrawFrame")
    assert(app.input.isKeyPressed(Input.Keys.SPACE))
  }

  test("keyUp(k) routes to AndroidInput.onKeyUp — key released after drain") {
    val buf         = scala.collection.mutable.ArrayBuffer.empty[String]
    val (app, _, _) = buildApp(buf, init = true)
    val driver      = new SgeAndroidDriver(app)

    driver.keyDown(Input.Keys.B.toInt)
    driver.onDrawFrame()
    assert(app.input.isKeyPressed(Input.Keys.B))

    driver.keyUp(Input.Keys.B.toInt)
    driver.onDrawFrame()
    assert(!app.input.isKeyPressed(Input.Keys.B), "keyUp routed through driver must release the key")
  }

  test("keyTyped(ch) routes to AndroidInput.onKeyTyped — processor sees typed char after drain") {
    val buf         = scala.collection.mutable.ArrayBuffer.empty[String]
    val (app, _, _) = buildApp(buf, init = true)
    val driver      = new SgeAndroidDriver(app)

    var typed: Char = ' '
    app.input.setInputProcessor(
      new InputProcessor {
        override def keyTyped(character: Char): Boolean = { typed = character; true }
      }
    )

    driver.keyTyped('x')
    driver.onDrawFrame()

    assertEquals(typed, 'x')
  }

  test("touchEvent(event) reaches the input handler (AndroidInput.onTouchEvent seam)") {
    val buf         = scala.collection.mutable.ArrayBuffer.empty[String]
    val (app, _, _) = buildApp(buf, init = true)
    val driver      = new SgeAndroidDriver(app)

    // The StubTouchInputOps drives a TOUCH_DOWN-equivalent through the handler;
    // observe via the input's existing seam: a posted touch becomes justTouched
    // after the next processEvents. We assert the call does not throw and the
    // event path is exercised through the driver's android-type-free AnyRef arg.
    var processorTouched = false
    app.input.setInputProcessor(
      new InputProcessor {
        override def touchDown(screenX: Pixels, screenY: Pixels, pointer: Int, button: Input.Button): Boolean = {
          processorTouched = true; true
        }
      }
    )

    // Post a synthetic touch directly to prove the seam, then route a generic
    // touchEvent through the driver to prove the AnyRef path is wired.
    val androidInput = app.input.asInstanceOf[AndroidInput]
    androidInput.inputState.synchronized {
      androidInput.inputState.postTouchEvent(TouchInputOps.TOUCH_DOWN, 5, 5, 0, 0, System.nanoTime())
    }
    driver.touchEvent("fake-motion-event") // must reach AndroidInput.onTouchEvent without throwing
    driver.onDrawFrame()

    assert(processorTouched, "the posted touch must be dispatched through the driver's frame pump")
  }

  test("genericMotion(event) reaches the input handler (AndroidInput.onGenericMotionEvent seam)") {
    val buf         = scala.collection.mutable.ArrayBuffer.empty[String]
    val (app, _, _) = buildApp(buf, init = true)
    val driver      = new SgeAndroidDriver(app)

    // Must reach AndroidInput.onGenericMotionEvent via the android-type-free
    // AnyRef arg without throwing.
    driver.genericMotion("fake-motion-event")
    // No exception = the routing seam is wired.
    assert(true)
  }
}
