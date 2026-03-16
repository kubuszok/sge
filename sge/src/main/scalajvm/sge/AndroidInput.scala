/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-android/.../DefaultAndroidInput.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: DefaultAndroidInput -> AndroidInput
 *   Convention: delegates to ops interfaces; no Activity subclass needed in sge core
 *   Idiom: split packages; Nullable; opaque types (Key, Button, Pixels, Nanos)
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.Input.*
import sge.platform.{ AndroidInputState, AndroidMouseHandler, AndroidTouchHandler, DefaultAndroidInputState }
import sge.platform.android.{ AndroidConfigOps, HapticsOps, InputDialogCallback, InputMethodOps, SensorOps, TouchInputOps }
import sge.utils.{ Nanos, Nullable }

/** An implementation of [[Input]] for Android.
  *
  * Delegates sensor queries to [[SensorOps]], touch/mouse to [[AndroidInputState]] (mutated by [[AndroidTouchHandler]]/[[AndroidMouseHandler]]), keyboard to [[InputMethodOps]], and haptics to
  * [[HapticsOps]].
  *
  * @param config
  *   the Android application configuration
  * @param sensorOps
  *   sensor operations
  * @param inputMethodOps
  *   input method (keyboard/dialog) operations
  * @param hapticsOps
  *   haptic feedback operations
  * @param touchInputOps
  *   touch event data extraction
  * @param lifecycleOps
  *   lifecycle operations (for hardware keyboard check)
  */
class AndroidInput(
  private val config:         AndroidConfigOps,
  private val sensorOps:      SensorOps,
  private val inputMethodOps: InputMethodOps,
  private val hapticsOps:     HapticsOps,
  private val touchInputOps:  TouchInputOps,
  private val lifecycleOps:   sge.platform.android.AndroidLifecycleOps
) extends Input {

  // ── Handlers ────────────────────────────────────────────────────────

  private[sge] val inputState:   DefaultAndroidInputState = DefaultAndroidInputState()
  private[sge] val touchHandler: AndroidTouchHandler      = AndroidTouchHandler(touchInputOps)
  private[sge] val mouseHandler: AndroidMouseHandler      = AndroidMouseHandler(touchInputOps)

  // ── Key state ──────────────────────────────────────────────────────

  private val pressedKeys:       Array[Boolean] = new Array[Boolean](256)
  private val justPressedKeys:   Array[Boolean] = new Array[Boolean](256)
  private var keyCount:          Int            = 0
  private var _justTouched:      Boolean        = false
  private var _currentEventTime: Long           = 0L

  // ── Caught keys ────────────────────────────────────────────────────

  private val caughtKeys: Array[Boolean] = new Array[Boolean](256)

  // ── Input processor ────────────────────────────────────────────────

  @volatile private var _inputProcessor: InputProcessor = scala.compiletime.uninitialized

  // ── Keyboard height observer ───────────────────────────────────────

  @volatile private var _keyboardHeightObserver: KeyboardHeightObserver = scala.compiletime.uninitialized

  // ── Sensor queries ─────────────────────────────────────────────────

  override def getAccelerometerX(): Float = sensorOps.accelerometerX
  override def getAccelerometerY(): Float = sensorOps.accelerometerY
  override def getAccelerometerZ(): Float = sensorOps.accelerometerZ

  override def getGyroscopeX(): Float = sensorOps.gyroscopeX
  override def getGyroscopeY(): Float = sensorOps.gyroscopeY
  override def getGyroscopeZ(): Float = sensorOps.gyroscopeZ

  override def getAzimuth(): Float = sensorOps.azimuth
  override def getPitch():   Float = sensorOps.pitch
  override def getRoll():    Float = sensorOps.roll

  override def getRotationMatrix(matrix: Array[Float]): Unit = {
    val rm = sensorOps.rotationMatrix
    System.arraycopy(rm, 0, matrix, 0, Math.min(rm.length, matrix.length))
  }

  // ── Pointer / touch queries ────────────────────────────────────────

  override def getMaxPointers(): Int = AndroidInputState.NUM_TOUCHES

  override def getX():             Pixels = Pixels(inputState.getTouchX(0))
  override def getX(pointer: Int): Pixels = Pixels(inputState.getTouchX(pointer))

  override def getDeltaX():             Pixels = Pixels(inputState.getDeltaX(0))
  override def getDeltaX(pointer: Int): Pixels = Pixels(inputState.getDeltaX(pointer))

  override def getY():             Pixels = Pixels(inputState.getTouchY(0))
  override def getY(pointer: Int): Pixels = Pixels(inputState.getTouchY(pointer))

  override def getDeltaY():             Pixels = Pixels(inputState.getDeltaY(0))
  override def getDeltaY(pointer: Int): Pixels = Pixels(inputState.getDeltaY(pointer))

  override def isTouched(): Boolean = inputState.synchronized {
    var i = 0
    while (i < AndroidInputState.NUM_TOUCHES) {
      if (inputState.isTouched(i)) return true
      i += 1
    }
    false
  }

  override def isTouched(pointer: Int): Boolean = inputState.isTouched(pointer)

  override def justTouched(): Boolean = _justTouched

  override def getPressure():             Float = inputState.getPressure(0)
  override def getPressure(pointer: Int): Float = inputState.getPressure(pointer)

  override def isButtonPressed(button: Button): Boolean = inputState.synchronized {
    val b = button.toInt
    var i = 0
    while (i < AndroidInputState.NUM_TOUCHES) {
      if (inputState.isTouched(i) && inputState.getButton(i) == b) return true
      i += 1
    }
    false
  }

  override def isButtonJustPressed(button: Button): Boolean =
    // Simplified: check if just touched with the given button on pointer 0
    _justTouched && inputState.getButton(0) == button.toInt

  // ── Key state ──────────────────────────────────────────────────────

  override def isKeyPressed(key: Key): Boolean = {
    val k = key.toInt
    if (k == Keys.ANY_KEY.toInt) keyCount > 0
    else if (k >= 0 && k < 256) pressedKeys(k)
    else false
  }

  override def isKeyJustPressed(key: Key): Boolean = {
    val k = key.toInt
    if (k == Keys.ANY_KEY.toInt) keyCount > 0
    else if (k >= 0 && k < 256) justPressedKeys(k)
    else false
  }

  /** Called from the key event handler to register a key press. */
  private[sge] def onKeyDown(keycode: Int): Boolean = {
    if (keycode >= 0 && keycode < 256) {
      if (!pressedKeys(keycode)) {
        pressedKeys(keycode) = true
        keyCount += 1
      }
      justPressedKeys(keycode) = true
    }
    val processor = _inputProcessor
    if (processor != null) processor.keyDown(Key(keycode)) // scalafix:ok
    else false
  }

  /** Called from the key event handler to register a key release. */
  private[sge] def onKeyUp(keycode: Int): Boolean = {
    if (keycode >= 0 && keycode < 256) {
      if (pressedKeys(keycode)) {
        pressedKeys(keycode) = false
        keyCount -= 1
      }
    }
    val processor = _inputProcessor
    if (processor != null) processor.keyUp(Key(keycode)) // scalafix:ok
    else false
  }

  /** Called from the key event handler to register a typed character. */
  private[sge] def onKeyTyped(character: Char): Boolean = {
    val processor = _inputProcessor
    if (processor != null) processor.keyTyped(character) // scalafix:ok
    else false
  }

  // ── Text input / keyboard ──────────────────────────────────────────

  override def getTextInput(listener: TextInputListener, title: String, text: String, hint: String): Unit =
    getTextInput(listener, title, text, hint, OnscreenKeyboardType.Default)

  override def getTextInput(listener: TextInputListener, title: String, text: String, hint: String, `type`: OnscreenKeyboardType): Unit =
    inputMethodOps.showTextInputDialog(
      title,
      text,
      hint,
      0, // no max length
      onscreenKeyboardTypeToAndroidInputType(`type`),
      new InputDialogCallback {
        override def onInput(text: String): Unit = listener.input(text)
        override def onCancel():            Unit = listener.canceled()
      }
    )

  override def setOnscreenKeyboardVisible(visible: Boolean): Unit =
    setOnscreenKeyboardVisible(visible, OnscreenKeyboardType.Default)

  override def setOnscreenKeyboardVisible(visible: Boolean, `type`: OnscreenKeyboardType): Unit =
    if (visible) inputMethodOps.showKeyboard(onscreenKeyboardTypeToAndroidInputType(`type`))
    else inputMethodOps.hideKeyboard()

  override def openTextInputField(configuration: input.NativeInputConfiguration): Unit = {
    configuration.validate()
    val wrapper   = configuration.getTextInputWrapper()
    val text      = wrapper.getText()
    val selStart  = wrapper.getSelectionStart()
    val selEnd    = wrapper.getSelectionEnd()
    val inputType = onscreenKeyboardTypeToAndroidInputType(configuration.getType())
    val maxLen    = configuration.getMaxLength().getOrElse(0)
    val hint      = configuration.getPlaceholder()
    val mask      = configuration.isMaskInput()
    val multi     = configuration.isMultiLine()
    val noCorrect = configuration.isPreventCorrection()
    val autoComp  = configuration.getAutoComplete()
    val validator = configuration.getValidator()
    val closeCb   = configuration.getCloseCallback()

    inputMethodOps.openNativeTextField(
      text,
      selStart,
      selEnd,
      inputType,
      maxLen,
      hint,
      mask,
      multi,
      noCorrect,
      if (autoComp.isDefined) autoComp.get else null, // scalafix:ok — Java interop boundary
      (t, ss, se) => wrapper.writeResults(t, ss, se),
      confirmative =>
        if (closeCb != null) closeCb.onClose(confirmative) // scalafix:ok — may be uninitialized
        else false,
      if (validator != null) s => validator.validate(s) else null // scalafix:ok — may be uninitialized
    )
  }

  override def closeTextInputField(isConfirmative: Boolean, callback: Nullable[input.NativeInputConfiguration.NativeInputCloseCallback]): Unit = {
    inputMethodOps.closeNativeTextField(isConfirmative)
    callback.foreach(cb => cb.onClose(isConfirmative))
  }

  override def isTextInputFieldOpened(): Boolean =
    inputMethodOps.isNativeTextFieldOpen

  override def setKeyboardHeightObserver(observer: KeyboardHeightObserver): Unit =
    _keyboardHeightObserver = observer

  /** Notify the keyboard height observer of a height change. Called from the platform layer. */
  private[sge] def notifyKeyboardHeight(height: Int): Unit = {
    val obs = _keyboardHeightObserver
    if (obs != null) obs.onKeyboardHeightChanged(height) // scalafix:ok
  }

  // ── Haptics ────────────────────────────────────────────────────────

  override def vibrate(milliseconds: Int): Unit =
    hapticsOps.vibrate(milliseconds)

  override def vibrate(milliseconds: Int, fallback: Boolean): Unit =
    if (hapticsOps.hasHapticsSupport || fallback) hapticsOps.vibrate(milliseconds)

  override def vibrate(milliseconds: Int, amplitude: Int, fallback: Boolean): Unit =
    hapticsOps.vibrateWithIntensity(milliseconds, amplitude, fallback)

  override def vibrate(vibrationType: VibrationType): Unit =
    hapticsOps.vibrateHaptic(vibrationType.ordinal)

  // ── Compass ────────────────────────────────────────────────────────

  override def currentEventTime: Nanos = Nanos(_currentEventTime)

  // ── Caught keys ────────────────────────────────────────────────────

  override def setCatchKey(keycode: Key, catchKey: Boolean): Unit = {
    val k = keycode.toInt
    if (k >= 0 && k < 256) caughtKeys(k) = catchKey
  }

  override def isCatchKey(keycode: Key): Boolean = {
    val k = keycode.toInt
    k >= 0 && k < 256 && caughtKeys(k)
  }

  // ── Input processor ────────────────────────────────────────────────

  override def setInputProcessor(processor: InputProcessor): Unit =
    _inputProcessor = processor

  override def getInputProcessor(): InputProcessor = _inputProcessor

  // ── Peripherals ────────────────────────────────────────────────────

  override def isPeripheralAvailable(peripheral: Peripheral): Boolean = peripheral match {
    case Peripheral.HardwareKeyboard => lifecycleOps.hasHardwareKeyboard()
    case Peripheral.OnscreenKeyboard => true
    case Peripheral.MultitouchScreen => touchInputOps.supportsMultitouch
    case Peripheral.Accelerometer    => sensorOps.hasAccelerometer
    case Peripheral.Compass          => sensorOps.hasCompass
    case Peripheral.Vibrator         => hapticsOps.hasVibratorAvailable
    case Peripheral.HapticFeedback   => hapticsOps.hasHapticsSupport
    case Peripheral.Gyroscope        => sensorOps.hasGyroscope
    case Peripheral.RotationVector   => sensorOps.hasRotationVector
    case Peripheral.Pressure         => true // Android supports pressure natively
  }

  // ── Rotation / orientation ─────────────────────────────────────────

  override def getRotation(): Int = 0 // requires WindowManager — needs wiring later

  override def getNativeOrientation(): Orientation =
    if (sensorOps.nativeOrientation == 0) Orientation.Landscape
    else Orientation.Portrait

  // ── Cursor (no-op on Android) ──────────────────────────────────────

  override def setCursorCatched(catched: Boolean):           Unit    = ()
  override def isCursorCatched():                            Boolean = false
  override def setCursorPosition(x:      Pixels, y: Pixels): Unit    = ()

  // ── Per-frame processing ───────────────────────────────────────────

  /** Process queued touch/scroll events through the input processor. Call once per frame from the render loop. */
  private[sge] def processEvents(): Unit = {
    // Clear just-pressed state
    java.util.Arrays.fill(justPressedKeys, false)
    _justTouched = false

    val processor = _inputProcessor

    inputState.synchronized {
      val touchEvents  = inputState.drainTouchEvents()
      val scrollEvents = inputState.drainScrollEvents()

      touchEvents.foreach { e =>
        _currentEventTime = e.timeStamp
        e.eventType match {
          case TouchInputOps.TOUCH_DOWN =>
            _justTouched = true
            if (processor != null) processor.touchDown(Pixels(e.x), Pixels(e.y), e.pointer, Input.Button(e.button)) // scalafix:ok

          case TouchInputOps.TOUCH_UP =>
            if (processor != null) processor.touchUp(Pixels(e.x), Pixels(e.y), e.pointer, Input.Button(e.button)) // scalafix:ok

          case TouchInputOps.TOUCH_DRAGGED =>
            if (processor != null) processor.touchDragged(Pixels(e.x), Pixels(e.y), e.pointer) // scalafix:ok

          case TouchInputOps.TOUCH_MOVED =>
            if (processor != null) processor.mouseMoved(Pixels(e.x), Pixels(e.y)) // scalafix:ok

          case TouchInputOps.TOUCH_CANCELLED =>
            if (processor != null) processor.touchCancelled(Pixels(e.x), Pixels(e.y), e.pointer, Input.Button(e.button)) // scalafix:ok

          case _ => ()
        }
      }

      scrollEvents.foreach { e =>
        _currentEventTime = e.timeStamp
        if (processor != null) processor.scrolled(e.scrollAmountX.toFloat, e.scrollAmountY.toFloat) // scalafix:ok
      }
    }
  }

  /** Forward a touch event from the Android View to the touch handler.
    *
    * The host Activity must call this from its `dispatchTouchEvent` or an `OnTouchListener` wired to the GL surface view.
    *
    * @param event
    *   the Android `MotionEvent` (as `AnyRef`)
    */
  def onTouchEvent(event: AnyRef): Unit =
    touchHandler.onTouch(event, inputState)

  /** Forward a generic motion event (mouse hover/scroll) from the Android View.
    *
    * @param event
    *   the Android `MotionEvent` (as `AnyRef`)
    * @return
    *   true if the event was handled
    */
  def onGenericMotionEvent(event: AnyRef): Boolean =
    mouseHandler.onGenericMotion(event, inputState)

  /** Register sensor listeners based on configuration. */
  private[sge] def registerSensors(): Unit = sensorOps.registerListeners(config)

  /** Unregister sensor listeners. */
  private[sge] def unregisterSensors(): Unit = sensorOps.unregisterListeners()

  // ── Keyboard type mapping ──────────────────────────────────────────

  private def onscreenKeyboardTypeToAndroidInputType(`type`: OnscreenKeyboardType): Int = `type` match {
    case OnscreenKeyboardType.Default   => 0x00000001 // TYPE_CLASS_TEXT
    case OnscreenKeyboardType.NumberPad => 0x00000002 // TYPE_CLASS_NUMBER
    case OnscreenKeyboardType.PhonePad  => 0x00000003 // TYPE_CLASS_PHONE
    case OnscreenKeyboardType.Email     => 0x00000001 | 0x00000020 // TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_EMAIL_ADDRESS
    case OnscreenKeyboardType.Password  => 0x00000001 | 0x00000080 // TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD
    case OnscreenKeyboardType.URI       => 0x00000001 | 0x00000010 // TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_URI
  }
}
