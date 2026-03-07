/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../DefaultGwtInput.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: DefaultGwtInput -> DefaultBrowserInput
 *   Merged with: AbstractInput (inlined pressedKeys/justPressedKeys/keysToCatch)
 *   Convention: Scala.js only; uses scalajs-dom instead of GWT JSNI
 *   Convention: DOM KeyboardEvent.keyCode for key mapping (same numeric codes as GWT KeyCodes)
 *   Convention: Pointer Lock API via scalajs-dom (replaces JSNI pointer lock)
 *   Idiom: GdxRuntimeException -> SgeError; Gdx.graphics -> Sge().graphics
 *   Idiom: Accelerometer/Gyroscope via Generic Sensor API (BrowserAccelerometer/BrowserGyroscope)
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package input

import org.scalajs.dom
import org.scalajs.dom.{ HTMLCanvasElement, KeyboardEvent, MouseEvent, Touch, TouchEvent, document, window }
import sge.Input.*
import scala.compiletime.uninitialized
import scala.scalajs.js

/** Full browser input implementation using DOM events.
  *
  * Handles keyboard, mouse, and touch input by hooking DOM event listeners on the canvas element and document. Key codes are mapped from DOM `keyCode` values to SGE [[Input.Keys]] constants.
  *
  * @param canvas
  *   the HTML canvas element to listen on
  * @param config
  *   the browser application configuration
  */
class DefaultBrowserInput(canvas: HTMLCanvasElement, config: BrowserApplicationConfig)(using Sge) extends BrowserInput {

  import DefaultBrowserInput.*

  // --- AbstractInput fields (inlined) ---
  private val pressedKeys:     Array[Boolean]                    = new Array[Boolean](Keys.MAX_KEYCODE.toInt + 1)
  private val justPressedKeys: Array[Boolean]                    = new Array[Boolean](Keys.MAX_KEYCODE.toInt + 1)
  private val keysToCatch:     scala.collection.mutable.Set[Key] = scala.collection.mutable.HashSet()
  private var pressedKeyCount: Int                               = 0
  private var keyJustPressed:  Boolean                           = false

  // --- Touch/mouse state ---
  private val touchMap:              scala.collection.mutable.Map[Int, Int] = scala.collection.mutable.HashMap()
  private val touched:               Array[Boolean]                         = new Array[Boolean](MaxTouches)
  private val touchXArr:             Array[Int]                             = new Array[Int](MaxTouches)
  private val touchYArr:             Array[Int]                             = new Array[Int](MaxTouches)
  private val deltaXArr:             Array[Int]                             = new Array[Int](MaxTouches)
  private val deltaYArr:             Array[Int]                             = new Array[Int](MaxTouches)
  private val pressedButtons:        scala.collection.mutable.Set[Button]   = scala.collection.mutable.HashSet()
  private val justPressedButtonsArr: Array[Boolean]                         = new Array[Boolean](5)
  private var _justTouched:          Boolean                                = false
  private var currentEventTimeStamp: sge.utils.Nanos                        = sge.utils.Nanos(0L)
  private var processor:             InputProcessor                         = uninitialized
  private var hasFocus:              Boolean                                = true

  // --- Sensor state ---
  private var accelerometer: BrowserAccelerometer = scala.compiletime.uninitialized
  private var gyroscope:     BrowserGyroscope     = scala.compiletime.uninitialized

  // Hook events on construction
  hookEvents()
  setCatchKey(Keys.BACKSPACE, true)
  setupSensors()

  // --- Input trait implementation ---

  override def getAccelerometerX(): Float = if (accelerometer != null) accelerometer.x.toFloat else 0f
  override def getAccelerometerY(): Float = if (accelerometer != null) accelerometer.y.toFloat else 0f
  override def getAccelerometerZ(): Float = if (accelerometer != null) accelerometer.z.toFloat else 0f
  override def getGyroscopeX():     Float = if (gyroscope != null) gyroscope.x.toFloat else 0f
  override def getGyroscopeY():     Float = if (gyroscope != null) gyroscope.y.toFloat else 0f
  override def getGyroscopeZ():     Float = if (gyroscope != null) gyroscope.z.toFloat else 0f

  override def getMaxPointers(): Int = MaxTouches

  override def getX():                  Pixels = Pixels(touchXArr(0))
  override def getX(pointer:      Int): Pixels = Pixels(touchXArr(pointer))
  override def getDeltaX():             Pixels = Pixels(deltaXArr(0))
  override def getDeltaX(pointer: Int): Pixels = Pixels(deltaXArr(pointer))
  override def getY():                  Pixels = Pixels(touchYArr(0))
  override def getY(pointer:      Int): Pixels = Pixels(touchYArr(pointer))
  override def getDeltaY():             Pixels = Pixels(deltaYArr(0))
  override def getDeltaY(pointer: Int): Pixels = Pixels(deltaYArr(pointer))

  override def isTouched(): Boolean = {
    import scala.util.boundary, boundary.break
    boundary {
      var i = 0
      while (i < MaxTouches) {
        if (touched(i)) break(true)
        i += 1
      }
      false
    }
  }

  override def justTouched(): Boolean = _justTouched

  override def isTouched(pointer: Int): Boolean = touched(pointer)

  override def isButtonPressed(button: Button): Boolean =
    pressedButtons.contains(button) && touched(0)

  override def isButtonJustPressed(button: Button): Boolean =
    if (button.toInt < 0 || button.toInt >= justPressedButtonsArr.length) false
    else justPressedButtonsArr(button.toInt)

  override def getPressure():             Float = getPressure(0)
  override def getPressure(pointer: Int): Float = if (isTouched(pointer)) 1f else 0f

  override def isKeyPressed(key: Key): Boolean =
    if (key == Keys.ANY_KEY) pressedKeyCount > 0
    else if (key.toInt < 0 || key.toInt > Keys.MAX_KEYCODE.toInt) false
    else pressedKeys(key.toInt)

  override def isKeyJustPressed(key: Key): Boolean =
    if (key == Keys.ANY_KEY) keyJustPressed
    else if (key.toInt < 0 || key.toInt > Keys.MAX_KEYCODE.toInt) false
    else justPressedKeys(key.toInt)

  override def getTextInput(listener: TextInputListener, title: String, text: String, hint: String): Unit =
    getTextInput(listener, title, text, hint, OnscreenKeyboardType.Default)

  override def getTextInput(
    listener: TextInputListener,
    title:    String,
    text:     String,
    hint:     String,
    `type`:   OnscreenKeyboardType
  ): Unit = {
    // Use browser prompt as a simple fallback
    val result = window.prompt(title, text)
    if (result != null) listener.input(result)
    else listener.canceled()
  }

  override def setOnscreenKeyboardVisible(visible: Boolean):                                 Unit            = ()
  override def setOnscreenKeyboardVisible(visible: Boolean, `type`: OnscreenKeyboardType):   Unit            = ()
  override def openTextInputField(configuration:   NativeInputConfiguration):                Unit            = ()
  override def closeTextInputField(sendReturn:     Boolean):                                 Unit            = ()
  override def setKeyboardHeightObserver(observer: KeyboardHeightObserver):                  Unit            = ()
  override def vibrate(milliseconds:               Int):                                     Unit            = ()
  override def vibrate(milliseconds:               Int, fallback:   Boolean):                Unit            = ()
  override def vibrate(milliseconds:               Int, amplitude:  Int, fallback: Boolean): Unit            = ()
  override def vibrate(vibrationType:              VibrationType):                           Unit            = ()
  override def getAzimuth():                                                                 Float           = 0f
  override def getPitch():                                                                   Float           = 0f
  override def getRoll():                                                                    Float           = 0f
  override def getRotationMatrix(matrix:           Array[Float]):                            Unit            = ()
  override def currentEventTime:                                                             sge.utils.Nanos = currentEventTimeStamp

  override def setCatchKey(keycode: Key, catchKey: Boolean): Unit =
    if (catchKey) keysToCatch.add(keycode) else keysToCatch.remove(keycode)

  override def isCatchKey(keycode: Key): Boolean = keysToCatch.contains(keycode)

  override def setInputProcessor(proc: InputProcessor): Unit           = processor = proc
  override def getInputProcessor():                     InputProcessor = processor

  override def isPeripheralAvailable(peripheral: Peripheral): Boolean =
    peripheral match {
      case Peripheral.HardwareKeyboard => !isMobileDevice
      case Peripheral.MultitouchScreen => isTouchScreen
      case Peripheral.OnscreenKeyboard => isMobileDevice
      case Peripheral.Accelerometer    =>
        BrowserAccelerometer.isSupported && isAccelerometerPresent && BrowserFeaturePolicy.allowsFeature(
          BrowserAccelerometer.Permission
        )
      case Peripheral.Gyroscope =>
        BrowserGyroscope.isSupported && isGyroscopePresent && BrowserFeaturePolicy.allowsFeature(
          BrowserGyroscope.Permission
        )
      case _ => false
    }

  override def getRotation(): Int = {
    val screen      = window.screen
    val orientation = screen.asInstanceOf[js.Dynamic].orientation
    if (js.isUndefined(orientation)) 0
    else {
      val angle = orientation.asInstanceOf[js.Dynamic].angle
      if (js.isUndefined(angle)) 0
      else angle.asInstanceOf[Int]
    }
  }

  override def getNativeOrientation(): Orientation = Orientation.Landscape

  override def setCursorCatched(catched: Boolean): Unit =
    if (catched) canvas.asInstanceOf[js.Dynamic].requestPointerLock()
    else document.asInstanceOf[js.Dynamic].exitPointerLock()

  override def isCursorCatched(): Boolean =
    document.asInstanceOf[js.Dynamic].pointerLockElement.asInstanceOf[Any] == canvas

  override def setCursorPosition(x: Pixels, y: Pixels): Unit = () // not possible in browser

  // --- BrowserInput lifecycle ---

  override def reset(): Unit = {
    if (_justTouched) {
      _justTouched = false
      var i = 0
      while (i < justPressedButtonsArr.length) {
        justPressedButtonsArr(i) = false
        i += 1
      }
    }
    if (keyJustPressed) {
      keyJustPressed = false
      var i = 0
      while (i < justPressedKeys.length) {
        justPressedKeys(i) = false
        i += 1
      }
    }
  }

  // --- Sensor setup ---

  private def setupSensors(): Unit = {
    if (config.useAccelerometer && BrowserFeaturePolicy.allowsFeature(BrowserAccelerometer.Permission)) {
      if (BrowserAccelerometer.isSupported) {
        setupAccelerometer()
      } else {
        BrowserPermissions.queryPermission(
          BrowserAccelerometer.Permission,
          new BrowserPermissions.PermissionResult {
            override def granted(): Unit = setupAccelerometer()
            override def denied():  Unit = ()
            override def prompt():  Unit = setupAccelerometer()
          }
        )
      }
    }
    if (config.useGyroscope) {
      if (BrowserGyroscope.isSupported) {
        setupGyroscope()
      } else {
        BrowserPermissions.queryPermission(
          BrowserGyroscope.Permission,
          new BrowserPermissions.PermissionResult {
            override def granted(): Unit = setupGyroscope()
            override def denied():  Unit = ()
            override def prompt():  Unit = setupGyroscope()
          }
        )
      }
    }
  }

  private def setupAccelerometer(): Unit =
    if (BrowserAccelerometer.isSupported && BrowserFeaturePolicy.allowsFeature(BrowserAccelerometer.Permission)) {
      if (accelerometer == null) accelerometer = BrowserAccelerometer.getInstance()
      if (!accelerometer.activated) accelerometer.start()
    }

  private def setupGyroscope(): Unit =
    if (BrowserGyroscope.isSupported && BrowserFeaturePolicy.allowsFeature(BrowserGyroscope.Permission)) {
      if (gyroscope == null) gyroscope = BrowserGyroscope.getInstance()
      if (!gyroscope.activated) gyroscope.start()
    }

  private def isAccelerometerPresent: Boolean =
    getAccelerometerX() != 0f || getAccelerometerY() != 0f || getAccelerometerZ() != 0f

  private def isGyroscopePresent: Boolean =
    getGyroscopeX() != 0f || getGyroscopeY() != 0f || getGyroscopeZ() != 0f

  // --- Event hooking ---

  private def hookEvents(): Unit = {
    canvas.addEventListener("mousedown", (e: MouseEvent) => handleMouseDown(e), true)
    document.addEventListener("mousedown", (e: MouseEvent) => handleDocMouseDown(e), true)
    canvas.addEventListener("mouseup", (e: MouseEvent) => handleMouseUp(e), true)
    document.addEventListener("mouseup", (e: MouseEvent) => handleMouseUp(e), true)
    canvas.addEventListener("mousemove", (e: MouseEvent) => handleMouseMove(e), true)
    document.addEventListener("mousemove", (e: MouseEvent) => handleMouseMove(e), true)
    canvas.addEventListener("wheel", (e: dom.WheelEvent) => handleWheel(e), true)
    document.addEventListener("keydown", (e: KeyboardEvent) => handleKeyDown(e), false)
    document.addEventListener("keyup", (e: KeyboardEvent) => handleKeyUp(e), false)
    window.addEventListener("blur", (_: dom.Event) => handleBlur(), false)
    canvas.addEventListener("touchstart", (e: TouchEvent) => handleTouchStart(e), true)
    canvas.addEventListener("touchmove", (e: TouchEvent) => handleTouchMove(e), true)
    canvas.addEventListener("touchcancel", (e: TouchEvent) => handleTouchEnd(e), true)
    canvas.addEventListener("touchend", (e: TouchEvent) => handleTouchEnd(e), true)
  }

  // --- Mouse handlers ---

  private def handleMouseDown(e: MouseEvent): Unit = {
    val button = getButton(e.button.toInt)
    if (!pressedButtons.contains(button)) {
      hasFocus = true
      _justTouched = true
      touched(0) = true
      pressedButtons.add(button)
      if (button.toInt < justPressedButtonsArr.length) justPressedButtonsArr(button.toInt) = true
      deltaXArr(0) = 0
      deltaYArr(0) = 0
      if (isCursorCatched()) {
        touchXArr(0) += movementX(e)
        touchYArr(0) += movementY(e)
      } else {
        touchXArr(0) = getRelativeX(e)
        touchYArr(0) = getRelativeY(e)
      }
      currentEventTimeStamp = utils.TimeUtils.nanoTime()
      if (processor != null) processor.touchDown(Pixels(touchXArr(0)), Pixels(touchYArr(0)), 0, button)
    }
  }

  private def handleDocMouseDown(e: MouseEvent): Unit =
    // Track focus loss when clicking outside canvas
    if (e.target != canvas) {
      val mx = getRelativeX(e)
      val my = getRelativeY(e)
      if (mx < 0 || mx > Sge().graphics.getWidth().toInt || my < 0 || my > Sge().graphics.getHeight().toInt) {
        hasFocus = false
      }
    }

  private def handleMouseMove(e: MouseEvent): Unit = {
    if (isCursorCatched()) {
      deltaXArr(0) = movementX(e)
      deltaYArr(0) = movementY(e)
      touchXArr(0) += movementX(e)
      touchYArr(0) += movementY(e)
    } else {
      deltaXArr(0) = getRelativeX(e) - touchXArr(0)
      deltaYArr(0) = getRelativeY(e) - touchYArr(0)
      touchXArr(0) = getRelativeX(e)
      touchYArr(0) = getRelativeY(e)
    }
    currentEventTimeStamp = utils.TimeUtils.nanoTime()
    if (processor != null) {
      if (touched(0)) processor.touchDragged(Pixels(touchXArr(0)), Pixels(touchYArr(0)), 0)
      else processor.mouseMoved(Pixels(touchXArr(0)), Pixels(touchYArr(0)))
    }
  }

  private def handleMouseUp(e: MouseEvent): Unit = {
    val button = getButton(e.button.toInt)
    if (pressedButtons.contains(button)) {
      pressedButtons.remove(button)
      touched(0) = pressedButtons.nonEmpty
      if (isCursorCatched()) {
        deltaXArr(0) = movementX(e)
        deltaYArr(0) = movementY(e)
        touchXArr(0) += movementX(e)
        touchYArr(0) += movementY(e)
      } else {
        deltaXArr(0) = getRelativeX(e) - touchXArr(0)
        deltaYArr(0) = getRelativeY(e) - touchYArr(0)
        touchXArr(0) = getRelativeX(e)
        touchYArr(0) = getRelativeY(e)
      }
      currentEventTimeStamp = utils.TimeUtils.nanoTime()
      touched(0) = false
      if (processor != null) processor.touchUp(Pixels(touchXArr(0)), Pixels(touchYArr(0)), 0, button)
    }
  }

  private def handleWheel(e: dom.WheelEvent): Unit = {
    if (processor != null) {
      // deltaY is positive for scroll down, normalize to integer scroll units
      val scrollAmount = if (e.deltaY > 0) 1 else if (e.deltaY < 0) -1 else 0
      processor.scrolled(0, scrollAmount.toFloat)
    }
    currentEventTimeStamp = utils.TimeUtils.nanoTime()
    e.preventDefault()
  }

  // --- Keyboard handlers ---

  private def handleKeyDown(e: KeyboardEvent): Unit =
    if (hasFocus) {
      val code = keyForCode(e.keyCode, e.location)
      if (isCatchKey(code)) e.preventDefault()
      if (code == Keys.BACKSPACE) {
        if (processor != null) {
          processor.keyDown(code)
          processor.keyTyped('\b')
        }
      } else {
        if (!pressedKeys(code.toInt)) {
          pressedKeyCount += 1
          pressedKeys(code.toInt) = true
          keyJustPressed = true
          justPressedKeys(code.toInt) = true
          if (processor != null) processor.keyDown(code)
        }
      }
    }

  private def handleKeyUp(e: KeyboardEvent): Unit =
    if (!hasFocus) {
      // Release all pressed keys on blur
      if (pressedKeyCount > 0) releaseAllKeys()
    } else {
      val code = keyForCode(e.keyCode, e.location)
      if (isCatchKey(code)) e.preventDefault()
      // Tab doesn't fire keypress in most browsers, so we emulate keyTyped here
      if (processor != null && code == Keys.TAB) processor.keyTyped('\t')
      // Also forward keypress for printable characters via the key string
      if (e.key.length == 1 && code != Keys.BACKSPACE) {
        if (processor != null) processor.keyTyped(e.key.charAt(0))
      }
      if (pressedKeys(code.toInt)) {
        pressedKeyCount -= 1
        pressedKeys(code.toInt) = false
      }
      if (processor != null) processor.keyUp(code)
    }

  private def handleBlur(): Unit =
    if (pressedKeyCount > 0) releaseAllKeys()

  private def releaseAllKeys(): Unit = {
    var i = 0
    while (i <= Keys.MAX_KEYCODE.toInt) {
      if (pressedKeys(i)) {
        pressedKeys(i) = false
        pressedKeyCount -= 1
        if (processor != null) processor.keyUp(Key(i))
      }
      i += 1
    }
  }

  // --- Touch handlers ---

  private def handleTouchStart(e: TouchEvent): Unit = {
    _justTouched = true
    val touches = e.changedTouches
    var i       = 0
    while (i < touches.length) {
      val touch   = touches(i)
      val real    = touch.identifier.toInt
      val touchId = getAvailablePointer()
      if (touchId >= 0) {
        touchMap.put(real, touchId)
        touched(touchId) = true
        touchXArr(touchId) = getRelativeTouchX(touch)
        touchYArr(touchId) = getRelativeTouchY(touch)
        deltaXArr(touchId) = 0
        deltaYArr(touchId) = 0
        if (processor != null) processor.touchDown(Pixels(touchXArr(touchId)), Pixels(touchYArr(touchId)), touchId, Buttons.LEFT)
      }
      i += 1
    }
    currentEventTimeStamp = utils.TimeUtils.nanoTime()
    e.preventDefault()
  }

  private def handleTouchMove(e: TouchEvent): Unit = {
    val touches = e.changedTouches
    var i       = 0
    while (i < touches.length) {
      val touch = touches(i)
      val real  = touch.identifier.toInt
      touchMap.get(real).foreach { touchId =>
        deltaXArr(touchId) = getRelativeTouchX(touch) - touchXArr(touchId)
        deltaYArr(touchId) = getRelativeTouchY(touch) - touchYArr(touchId)
        touchXArr(touchId) = getRelativeTouchX(touch)
        touchYArr(touchId) = getRelativeTouchY(touch)
        if (processor != null) processor.touchDragged(Pixels(touchXArr(touchId)), Pixels(touchYArr(touchId)), touchId)
      }
      i += 1
    }
    currentEventTimeStamp = utils.TimeUtils.nanoTime()
    e.preventDefault()
  }

  private def handleTouchEnd(e: TouchEvent): Unit = {
    val touches = e.changedTouches
    var i       = 0
    while (i < touches.length) {
      val touch = touches(i)
      val real  = touch.identifier.toInt
      touchMap.get(real).foreach { touchId =>
        touchMap.remove(real)
        touched(touchId) = false
        deltaXArr(touchId) = getRelativeTouchX(touch) - touchXArr(touchId)
        deltaYArr(touchId) = getRelativeTouchY(touch) - touchYArr(touchId)
        touchXArr(touchId) = getRelativeTouchX(touch)
        touchYArr(touchId) = getRelativeTouchY(touch)
        if (processor != null) processor.touchUp(Pixels(touchXArr(touchId)), Pixels(touchYArr(touchId)), touchId, Buttons.LEFT)
      }
      i += 1
    }
    currentEventTimeStamp = utils.TimeUtils.nanoTime()
    e.preventDefault()
  }

  // --- Helpers ---

  private def getButton(button: Int): Button =
    button match {
      case 0 => Buttons.LEFT
      case 2 => Buttons.RIGHT
      case 1 => Buttons.MIDDLE
      case _ => Buttons.LEFT
    }

  private def getRelativeX(e: MouseEvent): Int = {
    val rect   = canvas.getBoundingClientRect()
    val scaleX = canvas.width.toFloat / canvas.clientWidth.toFloat
    Math.round(scaleX * (e.clientX - rect.left).toFloat)
  }

  private def getRelativeY(e: MouseEvent): Int = {
    val rect   = canvas.getBoundingClientRect()
    val scaleY = canvas.height.toFloat / canvas.clientHeight.toFloat
    Math.round(scaleY * (e.clientY - rect.top).toFloat)
  }

  private def getRelativeTouchX(touch: Touch): Int = {
    val rect   = canvas.getBoundingClientRect()
    val scaleX = canvas.width.toFloat / canvas.clientWidth.toFloat
    Math.round(scaleX * (touch.clientX - rect.left).toFloat)
  }

  private def getRelativeTouchY(touch: Touch): Int = {
    val rect   = canvas.getBoundingClientRect()
    val scaleY = canvas.height.toFloat / canvas.clientHeight.toFloat
    Math.round(scaleY * (touch.clientY - rect.top).toFloat)
  }

  private def movementX(e: MouseEvent): Int =
    e.asInstanceOf[js.Dynamic].movementX.asInstanceOf[js.UndefOr[Double]].fold(0)(_.toInt)

  private def movementY(e: MouseEvent): Int =
    e.asInstanceOf[js.Dynamic].movementY.asInstanceOf[js.UndefOr[Double]].fold(0)(_.toInt)

  private def getAvailablePointer(): Int = {
    import scala.util.boundary, boundary.break
    boundary {
      var i = 0
      while (i < MaxTouches) {
        if (!touchMap.values.exists(_ == i)) break(i)
        i += 1
      }
      -1
    }
  }

  private def isMobileDevice: Boolean = {
    val ua = window.navigator.userAgent.toLowerCase
    ua.contains("android") || ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod")
  }

  private def isTouchScreen: Boolean =
    js.typeOf(js.Dynamic.global.ontouchstart) != "undefined" ||
      window.navigator.asInstanceOf[js.Dynamic].maxTouchPoints.asInstanceOf[js.UndefOr[Int]].fold(0)(identity) > 0
}

object DefaultBrowserInput {
  private val MaxTouches = 20

  // Key location constants (from DOM KeyboardEvent.location)
  private val LocationRight  = 2
  private val LocationNumpad = 3

  // DOM keyCode constants (these match the standard DOM KeyboardEvent.keyCode values)
  // Note: keyCode is deprecated in DOM spec but still widely supported and needed for game input
  private val KeyPause                  = 19
  private val KeyCapsLock               = 20
  private val KeySpace                  = 32
  private val KeyInsert                 = 45
  private val Key0                      = 48
  private val Key9                      = 57
  private val KeyA                      = 65
  private val KeyZ                      = 90
  private val KeyLeftWindowKey          = 91
  private val KeyRightWindowKey         = 92
  private val KeyNumpad0                = 96
  private val KeyNumpad9                = 105
  private val KeyMultiply               = 106
  private val KeyAdd                    = 107
  private val KeySubtract               = 109
  private val KeyDecimalPoint           = 110
  private val KeyDivide                 = 111
  private val KeyF1                     = 112
  private val KeyF24                    = 135
  private val KeyNumLock                = 144
  private val KeyScrollLock             = 145
  private val KeyAudioVolumeDown        = 174
  private val KeyAudioVolumeUp          = 175
  private val KeyMediaTrackNext         = 176
  private val KeyMediaTrackPrevious     = 177
  private val KeyMediaStop              = 178
  private val KeyMediaPlayPause         = 179
  private val KeyAudioVolumeDownFirefox = 182
  private val KeyAudioVolumeUpFirefox   = 183
  private val KeySemicolon              = 186
  private val KeyEquals                 = 187
  private val KeyComma                  = 188
  private val KeyDash                   = 189
  private val KeyPeriod                 = 190
  private val KeyForwardSlash           = 191
  private val KeyGraveAccent            = 192
  private val KeyOpenBracket            = 219
  private val KeyBackslash              = 220
  private val KeyCloseBracket           = 221
  private val KeySingleQuote            = 222

  // DOM KeyCodes that match com.google.gwt.event.dom.client.KeyCodes constants
  private val KeyCodeAlt         = 18
  private val KeyCodeBackspace   = 8
  private val KeyCodeCtrl        = 17
  private val KeyCodeDelete      = 46
  private val KeyCodeDown        = 40
  private val KeyCodeEnd         = 35
  private val KeyCodeEnter       = 13
  private val KeyCodeEscape      = 27
  private val KeyCodeHome        = 36
  private val KeyCodeLeft        = 37
  private val KeyCodePageDown    = 34
  private val KeyCodePageUp      = 33
  private val KeyCodeRight       = 39
  private val KeyCodeShift       = 16
  private val KeyCodeTab         = 9
  private val KeyCodeUp          = 38
  private val KeyCodePrintScreen = 44

  /** Map a DOM keyCode + location to an SGE Keys constant. */
  def keyForCode(keyCode: Int, location: Int): Key =
    keyCode match {
      case KeyCodeAlt         => if (location == LocationRight) Keys.ALT_RIGHT else Keys.ALT_LEFT
      case KeyCodeBackspace   => Keys.BACKSPACE
      case KeyCodeCtrl        => if (location == LocationRight) Keys.CONTROL_RIGHT else Keys.CONTROL_LEFT
      case KeyCodeDelete      => Keys.FORWARD_DEL
      case KeyCodeDown        => Keys.DOWN
      case KeyCodeEnd         => Keys.END
      case KeyCodeEnter       => if (location == LocationNumpad) Keys.NUMPAD_ENTER else Keys.ENTER
      case KeyCodeEscape      => Keys.ESCAPE
      case KeyCodeHome        => Keys.HOME
      case KeyCodeLeft        => Keys.LEFT
      case KeyCodePageDown    => Keys.PAGE_DOWN
      case KeyCodePageUp      => Keys.PAGE_UP
      case KeyCodeRight       => Keys.RIGHT
      case KeyCodeShift       => if (location == LocationRight) Keys.SHIFT_RIGHT else Keys.SHIFT_LEFT
      case KeyCodeTab         => Keys.TAB
      case KeyCodeUp          => Keys.UP
      case KeyCodePrintScreen => Keys.PRINT_SCREEN

      case KeyPause    => Keys.PAUSE
      case KeyCapsLock => Keys.CAPS_LOCK
      case KeySpace    => Keys.SPACE
      case KeyInsert   => Keys.INSERT

      case c if c >= Key0 && c <= Key9 => Key(Keys.NUM_0.toInt + (c - Key0))
      case c if c >= KeyA && c <= KeyZ => Key(Keys.A.toInt + (c - KeyA))

      case KeyLeftWindowKey  => Keys.UNKNOWN
      case KeyRightWindowKey => Keys.UNKNOWN

      case c if c >= KeyNumpad0 && c <= KeyNumpad9 => Key(Keys.NUMPAD_0.toInt + (c - KeyNumpad0))
      case KeyMultiply                             => Keys.NUMPAD_MULTIPLY
      case KeyAdd                                  => Keys.NUMPAD_ADD
      case KeySubtract                             => Keys.NUMPAD_SUBTRACT
      case KeyDecimalPoint                         => Keys.NUMPAD_DOT
      case KeyDivide                               => Keys.NUMPAD_DIVIDE

      case c if c >= KeyF1 && c <= KeyF24 => Key(Keys.F1.toInt + (c - KeyF1))

      case KeyNumLock    => Keys.NUM_LOCK
      case KeyScrollLock => Keys.SCROLL_LOCK

      case KeyAudioVolumeDown | KeyAudioVolumeDownFirefox => Keys.VOLUME_DOWN
      case KeyAudioVolumeUp | KeyAudioVolumeUpFirefox     => Keys.VOLUME_UP
      case KeyMediaTrackNext                              => Keys.MEDIA_NEXT
      case KeyMediaTrackPrevious                          => Keys.MEDIA_PREVIOUS
      case KeyMediaStop                                   => Keys.MEDIA_STOP
      case KeyMediaPlayPause                              => Keys.MEDIA_PLAY_PAUSE

      case KeySemicolon    => Keys.SEMICOLON
      case KeyEquals       => Keys.EQUALS
      case KeyComma        => Keys.COMMA
      case KeyDash         => Keys.MINUS
      case KeyPeriod       => Keys.PERIOD
      case KeyForwardSlash => Keys.SLASH
      case KeyGraveAccent  => Keys.UNKNOWN
      case KeyOpenBracket  => Keys.LEFT_BRACKET
      case KeyBackslash    => Keys.BACKSLASH
      case KeyCloseBracket => Keys.RIGHT_BRACKET
      case KeySingleQuote  => Keys.APOSTROPHE

      case _ => Keys.UNKNOWN
    }
}
