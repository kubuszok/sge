/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../DefaultLwjgl3Input.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: DefaultLwjgl3Input -> DefaultDesktopInput
 *   Merged with: AbstractInput (pressedKeys, justPressedKeys, keyJustPressed, pressedKeyCount, setCatchKey, isCatchKey, isKeyPressed, isKeyJustPressed)
 *   Convention: GLFW callbacks -> Scala lambdas via WindowingOps
 *   Convention: GLFW constants from WindowingOps companion object
 *   Idiom: split packages; no return
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.graphics.glutils.HdpiMode
import sge.input.NativeInputConfiguration
import sge.platform.WindowingOps
import sge.utils.{ Nanos, Nullable }
import scala.collection.mutable

/** Default desktop input implementation using GLFW/SDL3 callbacks via [[WindowingOps]].
  *
  * Registers key, character, scroll, cursor position, and mouse button callbacks on the native window. Buffers events via [[InputEventQueue]] and drains them each frame to the current
  * [[InputProcessor]].
  *
  * Merges LibGDX's `AbstractInput` (key tracking arrays, catch-key set) and `DefaultLwjgl3Input` into a single class.
  *
  * @param window
  *   the desktop window this input is associated with
  * @param windowing
  *   the windowing FFI operations
  * @author
  *   See AUTHORS file (original implementation)
  */
class DefaultDesktopInput private[sge] (
  private[sge] val window: DesktopWindow,
  private val windowing:   WindowingOps
) extends DesktopInput {

  import Input.*
  import WindowingOps.*

  // ─── AbstractInput fields (merged) ──────────────────────────────────
  private val pressedKeys:     Array[Boolean]   = new Array[Boolean](Keys.MAX_KEYCODE.toInt + 1)
  private val justPressedKeys: Array[Boolean]   = new Array[Boolean](Keys.MAX_KEYCODE.toInt + 1)
  private val keysToCatch:     mutable.Set[Key] = mutable.Set.empty
  private var pressedKeyCount: Int              = 0
  private var keyJustPressed:  Boolean          = false

  // ─── DefaultDesktopInput fields ─────────────────────────────────────
  private var _inputProcessor:     Nullable[InputProcessor] = Nullable.empty
  private[sge] val eventQueue:     InputEventQueue          = new InputEventQueue
  private var _mouseX:             Int                      = 0
  private var _mouseY:             Int                      = 0
  private var _mousePressed:       Int                      = 0
  private var _deltaX:             Int                      = 0
  private var _deltaY:             Int                      = 0
  private var _justTouched:        Boolean                  = false
  private val _justPressedButtons: Array[Boolean]           = new Array[Boolean](5)
  private var _lastCharacter:      Char                     = 0
  // Logical mouse position (before HiDPI scaling)
  private var _logicalMouseX: Int = 0
  private var _logicalMouseY: Int = 0

  // ─── GLFW callbacks ─────────────────────────────────────────────────

  private val onKey: (Long, Int, Int, Int, Int) => Unit = { (_, key, _, action, _) =>
    action match {
      case GLFW_PRESS =>
        val gdxKey = getGdxKeyCode(key)
        eventQueue.keyDown(gdxKey, Nanos(System.nanoTime()))
        pressedKeyCount += 1
        keyJustPressed = true
        pressedKeys(gdxKey.toInt) = true
        justPressedKeys(gdxKey.toInt) = true
        window.getGraphics().requestRendering()
        _lastCharacter = 0
        val character = characterForKeyCode(gdxKey)
        if (character != 0) onChar(0L, character.toInt)
      case GLFW_RELEASE =>
        val gdxKey = getGdxKeyCode(key)
        pressedKeyCount -= 1
        pressedKeys(gdxKey.toInt) = false
        window.getGraphics().requestRendering()
        eventQueue.keyUp(gdxKey, Nanos(System.nanoTime()))
      case GLFW_REPEAT =>
        if (_lastCharacter != 0) {
          window.getGraphics().requestRendering()
          eventQueue.keyTyped(_lastCharacter, Nanos(System.nanoTime()))
        }
      case _ => ()
    }
  }

  private val onChar: (Long, Int) => Unit = { (_, codepoint) =>
    if ((codepoint & 0xff00) != 0xf700) {
      _lastCharacter = codepoint.toChar
      window.getGraphics().requestRendering()
      eventQueue.keyTyped(codepoint.toChar, Nanos(System.nanoTime()))
    }
  }

  private val onScroll: (Long, Double, Double) => Unit = { (_, scrollX, scrollY) =>
    window.getGraphics().requestRendering()
    eventQueue.scrolled(-scrollX.toFloat, -scrollY.toFloat, Nanos(System.nanoTime()))
  }

  private val onCursorPos: (Long, Double, Double) => Unit = { (_, x, y) =>
    _deltaX = x.toInt - _logicalMouseX
    _deltaY = y.toInt - _logicalMouseY
    _mouseX = x.toInt
    _mouseY = y.toInt
    _logicalMouseX = x.toInt
    _logicalMouseY = y.toInt

    if (window.config.hdpiMode == HdpiMode.Pixels) {
      val graphics = window.getGraphics()
      val xScale   = graphics.getBackBufferWidth().toFloat / graphics.getLogicalWidth().toFloat
      val yScale   = graphics.getBackBufferHeight().toFloat / graphics.getLogicalHeight().toFloat
      _deltaX = (_deltaX * xScale).toInt
      _deltaY = (_deltaY * yScale).toInt
      _mouseX = (_mouseX * xScale).toInt
      _mouseY = (_mouseY * yScale).toInt
    }

    window.getGraphics().requestRendering()
    val time = Nanos(System.nanoTime())
    if (_mousePressed > 0) {
      eventQueue.touchDragged(Pixels(_mouseX), Pixels(_mouseY), 0, time)
    } else {
      eventQueue.mouseMoved(Pixels(_mouseX), Pixels(_mouseY), time)
    }
  }

  private val onMouseButton: (Long, Int, Int, Int) => Unit = { (_, button, action, _) =>
    val gdxButton = toGdxButton(button)
    if (button == -1 || gdxButton != Button(-1)) {
      val time = Nanos(System.nanoTime())
      if (action == GLFW_PRESS) {
        _mousePressed += 1
        _justTouched = true
        _justPressedButtons(gdxButton.toInt) = true
        window.getGraphics().requestRendering()
        eventQueue.touchDown(Pixels(_mouseX), Pixels(_mouseY), 0, gdxButton, time)
      } else {
        _mousePressed = scala.math.max(0, _mousePressed - 1)
        window.getGraphics().requestRendering()
        eventQueue.touchUp(Pixels(_mouseX), Pixels(_mouseY), 0, gdxButton, time)
      }
    }
  }

  private def toGdxButton(button: Int): Button = button match {
    case 0 => Buttons.LEFT
    case 1 => Buttons.RIGHT
    case 2 => Buttons.MIDDLE
    case 3 => Buttons.BACK
    case 4 => Buttons.FORWARD
    case _ => Button(-1)
  }

  // ─── Construction ───────────────────────────────────────────────────
  windowHandleChanged(window.getWindowHandle())

  // ─── DesktopInput lifecycle ─────────────────────────────────────────

  override def windowHandleChanged(windowHandle: Long): Unit = {
    resetPollingStates()
    windowing.setKeyCallback(window.getWindowHandle(), onKey)
    windowing.setCharCallback(window.getWindowHandle(), onChar)
    windowing.setScrollCallback(window.getWindowHandle(), onScroll)
    windowing.setCursorPosCallback(window.getWindowHandle(), onCursorPos)
    windowing.setMouseButtonCallback(window.getWindowHandle(), onMouseButton)
  }

  override def update(): Unit =
    eventQueue.drain(_inputProcessor)

  override def prepareNext(): Unit = {
    if (_justTouched) {
      _justTouched = false
      var i = 0
      while (i < _justPressedButtons.length) {
        _justPressedButtons(i) = false
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
    _deltaX = 0
    _deltaY = 0
  }

  override def resetPollingStates(): Unit = {
    _justTouched = false
    keyJustPressed = false
    var i = 0
    while (i < justPressedKeys.length) {
      justPressedKeys(i) = false
      i += 1
    }
    i = 0
    while (i < _justPressedButtons.length) {
      _justPressedButtons(i) = false
      i += 1
    }
    eventQueue.drain(Nullable.empty)
  }

  @scala.annotation.nowarn("msg=deprecated") // null — GLFW FFI interop: passing null unregisters the callback
  override def close(): Unit = {
    windowing.setKeyCallback(window.getWindowHandle(), null)
    windowing.setCharCallback(window.getWindowHandle(), null)
    windowing.setScrollCallback(window.getWindowHandle(), null)
    windowing.setCursorPosCallback(window.getWindowHandle(), null)
    windowing.setMouseButtonCallback(window.getWindowHandle(), null)
  }

  // ─── Input: pointer/touch ───────────────────────────────────────────

  override def getMaxPointers(): Int = 1

  override def getX(): Pixels = Pixels(_mouseX)

  override def getX(pointer: Int): Pixels = if (pointer == 0) Pixels(_mouseX) else Pixels.zero

  override def getDeltaX(): Pixels = Pixels(_deltaX)

  override def getDeltaX(pointer: Int): Pixels = if (pointer == 0) Pixels(_deltaX) else Pixels.zero

  override def getY(): Pixels = Pixels(_mouseY)

  override def getY(pointer: Int): Pixels = if (pointer == 0) Pixels(_mouseY) else Pixels.zero

  override def getDeltaY(): Pixels = Pixels(_deltaY)

  override def getDeltaY(pointer: Int): Pixels = if (pointer == 0) Pixels(_deltaY) else Pixels.zero

  override def isTouched(): Boolean = {
    val handle = window.getWindowHandle()
    windowing.getMouseButton(handle, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS ||
    windowing.getMouseButton(handle, GLFW_MOUSE_BUTTON_2) == GLFW_PRESS ||
    windowing.getMouseButton(handle, GLFW_MOUSE_BUTTON_3) == GLFW_PRESS ||
    windowing.getMouseButton(handle, GLFW_MOUSE_BUTTON_4) == GLFW_PRESS ||
    windowing.getMouseButton(handle, GLFW_MOUSE_BUTTON_5) == GLFW_PRESS
  }

  override def justTouched(): Boolean = _justTouched

  override def isTouched(pointer: Int): Boolean = if (pointer == 0) isTouched() else false

  override def getPressure(): Float = getPressure(0)

  override def getPressure(pointer: Int): Float = if (isTouched(pointer)) 1f else 0f

  override def isButtonPressed(button: Button): Boolean =
    windowing.getMouseButton(window.getWindowHandle(), button.toInt) == GLFW_PRESS

  override def isButtonJustPressed(button: Button): Boolean =
    if (button.toInt < 0 || button.toInt >= _justPressedButtons.length) false
    else _justPressedButtons(button.toInt)

  // ─── Input: keyboard (merged from AbstractInput) ────────────────────

  override def isKeyPressed(key: Key): Boolean =
    if (key == Keys.ANY_KEY) pressedKeyCount > 0
    else if (key.toInt < 0 || key.toInt > Keys.MAX_KEYCODE.toInt) false
    else pressedKeys(key.toInt)

  override def isKeyJustPressed(key: Key): Boolean =
    if (key == Keys.ANY_KEY) keyJustPressed
    else if (key.toInt < 0 || key.toInt > Keys.MAX_KEYCODE.toInt) false
    else justPressedKeys(key.toInt)

  override def setCatchKey(keycode: Key, catchKey: Boolean): Unit =
    if (!catchKey) keysToCatch.remove(keycode)
    else keysToCatch.add(keycode)

  override def isCatchKey(keycode: Key): Boolean = keysToCatch.contains(keycode)

  // ─── Input: text input ──────────────────────────────────────────────

  override def getTextInput(listener: TextInputListener, title: String, text: String, hint: String): Unit =
    getTextInput(listener, title, text, hint, OnscreenKeyboardType.Default)

  override def getTextInput(
    listener: TextInputListener,
    title:    String,
    text:     String,
    hint:     String,
    `type`:   OnscreenKeyboardType
  ): Unit =
    // FIXME getTextInput does nothing on desktop
    listener.canceled()

  override def currentEventTime: Nanos = eventQueue.currentEventTime

  override def setInputProcessor(processor: InputProcessor): Unit =
    _inputProcessor = Nullable(processor)

  @scala.annotation.nowarn("msg=deprecated") // orNull at trait boundary — Input.getInputProcessor returns InputProcessor
  override def getInputProcessor(): InputProcessor = _inputProcessor.orNull

  // ─── Input: cursor ──────────────────────────────────────────────────

  override def setCursorCatched(catched: Boolean): Unit =
    windowing.setInputMode(
      window.getWindowHandle(),
      GLFW_CURSOR,
      if (catched) GLFW_CURSOR_DISABLED else GLFW_CURSOR_NORMAL
    )

  override def isCursorCatched(): Boolean =
    windowing.getInputMode(window.getWindowHandle(), GLFW_CURSOR) == GLFW_CURSOR_DISABLED

  override def setCursorPosition(x: Pixels, y: Pixels): Unit = {
    var cx = x.toInt
    var cy = y.toInt
    if (window.config.hdpiMode == HdpiMode.Pixels) {
      val graphics = window.getGraphics()
      val xScale   = graphics.getLogicalWidth().toFloat / graphics.getBackBufferWidth().toFloat
      val yScale   = graphics.getLogicalHeight().toFloat / graphics.getBackBufferHeight().toFloat
      cx = (cx * xScale).toInt
      cy = (cy * yScale).toInt
    }
    windowing.setCursorPos(window.getWindowHandle(), cx.toDouble, cy.toDouble)
    onCursorPos(window.getWindowHandle(), cx.toDouble, cy.toDouble)
  }

  // ─── Input: key code mapping ────────────────────────────────────────

  protected def characterForKeyCode(key: Key): Char = key match {
    case Keys.BACKSPACE    => 8
    case Keys.TAB          => '\t'
    case Keys.FORWARD_DEL  => 127
    case Keys.NUMPAD_ENTER => '\n'
    case Keys.ENTER        => '\n'
    case _                 => 0
  }

  def getGdxKeyCode(lwjglKeyCode: Int): Key = lwjglKeyCode match {
    case GLFW_KEY_SPACE         => Keys.SPACE
    case GLFW_KEY_APOSTROPHE    => Keys.APOSTROPHE
    case GLFW_KEY_COMMA         => Keys.COMMA
    case GLFW_KEY_MINUS         => Keys.MINUS
    case GLFW_KEY_PERIOD        => Keys.PERIOD
    case GLFW_KEY_SLASH         => Keys.SLASH
    case GLFW_KEY_0             => Keys.NUM_0
    case GLFW_KEY_1             => Keys.NUM_1
    case GLFW_KEY_2             => Keys.NUM_2
    case GLFW_KEY_3             => Keys.NUM_3
    case GLFW_KEY_4             => Keys.NUM_4
    case GLFW_KEY_5             => Keys.NUM_5
    case GLFW_KEY_6             => Keys.NUM_6
    case GLFW_KEY_7             => Keys.NUM_7
    case GLFW_KEY_8             => Keys.NUM_8
    case GLFW_KEY_9             => Keys.NUM_9
    case GLFW_KEY_SEMICOLON     => Keys.SEMICOLON
    case GLFW_KEY_EQUAL         => Keys.EQUALS
    case GLFW_KEY_A             => Keys.A
    case GLFW_KEY_B             => Keys.B
    case GLFW_KEY_C             => Keys.C
    case GLFW_KEY_D             => Keys.D
    case GLFW_KEY_E             => Keys.E
    case GLFW_KEY_F             => Keys.F
    case GLFW_KEY_G             => Keys.G
    case GLFW_KEY_H             => Keys.H
    case GLFW_KEY_I             => Keys.I
    case GLFW_KEY_J             => Keys.J
    case GLFW_KEY_K             => Keys.K
    case GLFW_KEY_L             => Keys.L
    case GLFW_KEY_M             => Keys.M
    case GLFW_KEY_N             => Keys.N
    case GLFW_KEY_O             => Keys.O
    case GLFW_KEY_P             => Keys.P
    case GLFW_KEY_Q             => Keys.Q
    case GLFW_KEY_R             => Keys.R
    case GLFW_KEY_S             => Keys.S
    case GLFW_KEY_T             => Keys.T
    case GLFW_KEY_U             => Keys.U
    case GLFW_KEY_V             => Keys.V
    case GLFW_KEY_W             => Keys.W
    case GLFW_KEY_X             => Keys.X
    case GLFW_KEY_Y             => Keys.Y
    case GLFW_KEY_Z             => Keys.Z
    case GLFW_KEY_LEFT_BRACKET  => Keys.LEFT_BRACKET
    case GLFW_KEY_BACKSLASH     => Keys.BACKSLASH
    case GLFW_KEY_RIGHT_BRACKET => Keys.RIGHT_BRACKET
    case GLFW_KEY_GRAVE_ACCENT  => Keys.GRAVE
    case GLFW_KEY_WORLD_1       => Keys.WORLD_1
    case GLFW_KEY_WORLD_2       => Keys.WORLD_2
    case GLFW_KEY_ESCAPE        => Keys.ESCAPE
    case GLFW_KEY_ENTER         => Keys.ENTER
    case GLFW_KEY_TAB           => Keys.TAB
    case GLFW_KEY_BACKSPACE     => Keys.BACKSPACE
    case GLFW_KEY_INSERT        => Keys.INSERT
    case GLFW_KEY_DELETE        => Keys.FORWARD_DEL
    case GLFW_KEY_RIGHT         => Keys.RIGHT
    case GLFW_KEY_LEFT          => Keys.LEFT
    case GLFW_KEY_DOWN          => Keys.DOWN
    case GLFW_KEY_UP            => Keys.UP
    case GLFW_KEY_PAGE_UP       => Keys.PAGE_UP
    case GLFW_KEY_PAGE_DOWN     => Keys.PAGE_DOWN
    case GLFW_KEY_HOME          => Keys.HOME
    case GLFW_KEY_END           => Keys.END
    case GLFW_KEY_CAPS_LOCK     => Keys.CAPS_LOCK
    case GLFW_KEY_SCROLL_LOCK   => Keys.SCROLL_LOCK
    case GLFW_KEY_PRINT_SCREEN  => Keys.PRINT_SCREEN
    case GLFW_KEY_PAUSE         => Keys.PAUSE
    case GLFW_KEY_F1            => Keys.F1
    case GLFW_KEY_F2            => Keys.F2
    case GLFW_KEY_F3            => Keys.F3
    case GLFW_KEY_F4            => Keys.F4
    case GLFW_KEY_F5            => Keys.F5
    case GLFW_KEY_F6            => Keys.F6
    case GLFW_KEY_F7            => Keys.F7
    case GLFW_KEY_F8            => Keys.F8
    case GLFW_KEY_F9            => Keys.F9
    case GLFW_KEY_F10           => Keys.F10
    case GLFW_KEY_F11           => Keys.F11
    case GLFW_KEY_F12           => Keys.F12
    case GLFW_KEY_F13           => Keys.F13
    case GLFW_KEY_F14           => Keys.F14
    case GLFW_KEY_F15           => Keys.F15
    case GLFW_KEY_F16           => Keys.F16
    case GLFW_KEY_F17           => Keys.F17
    case GLFW_KEY_F18           => Keys.F18
    case GLFW_KEY_F19           => Keys.F19
    case GLFW_KEY_F20           => Keys.F20
    case GLFW_KEY_F21           => Keys.F21
    case GLFW_KEY_F22           => Keys.F22
    case GLFW_KEY_F23           => Keys.F23
    case GLFW_KEY_F24           => Keys.F24
    case GLFW_KEY_F25           => Keys.UNKNOWN
    case GLFW_KEY_NUM_LOCK      => Keys.NUM_LOCK
    case GLFW_KEY_KP_0          => Keys.NUMPAD_0
    case GLFW_KEY_KP_1          => Keys.NUMPAD_1
    case GLFW_KEY_KP_2          => Keys.NUMPAD_2
    case GLFW_KEY_KP_3          => Keys.NUMPAD_3
    case GLFW_KEY_KP_4          => Keys.NUMPAD_4
    case GLFW_KEY_KP_5          => Keys.NUMPAD_5
    case GLFW_KEY_KP_6          => Keys.NUMPAD_6
    case GLFW_KEY_KP_7          => Keys.NUMPAD_7
    case GLFW_KEY_KP_8          => Keys.NUMPAD_8
    case GLFW_KEY_KP_9          => Keys.NUMPAD_9
    case GLFW_KEY_KP_DECIMAL    => Keys.NUMPAD_DOT
    case GLFW_KEY_KP_DIVIDE     => Keys.NUMPAD_DIVIDE
    case GLFW_KEY_KP_MULTIPLY   => Keys.NUMPAD_MULTIPLY
    case GLFW_KEY_KP_SUBTRACT   => Keys.NUMPAD_SUBTRACT
    case GLFW_KEY_KP_ADD        => Keys.NUMPAD_ADD
    case GLFW_KEY_KP_ENTER      => Keys.NUMPAD_ENTER
    case GLFW_KEY_KP_EQUAL      => Keys.NUMPAD_EQUALS
    case GLFW_KEY_LEFT_SHIFT    => Keys.SHIFT_LEFT
    case GLFW_KEY_LEFT_CONTROL  => Keys.CONTROL_LEFT
    case GLFW_KEY_LEFT_ALT      => Keys.ALT_LEFT
    case GLFW_KEY_LEFT_SUPER    => Keys.SYM
    case GLFW_KEY_RIGHT_SHIFT   => Keys.SHIFT_RIGHT
    case GLFW_KEY_RIGHT_CONTROL => Keys.CONTROL_RIGHT
    case GLFW_KEY_RIGHT_ALT     => Keys.ALT_RIGHT
    case GLFW_KEY_RIGHT_SUPER   => Keys.SYM
    case GLFW_KEY_MENU          => Keys.MENU
    case _                      => Keys.UNKNOWN
  }

  // ─── Input: stubs (desktop has no sensors/vibration/on-screen keyboard) ─

  override def getAccelerometerX(): Float = 0f
  override def getAccelerometerY(): Float = 0f
  override def getAccelerometerZ(): Float = 0f

  override def getGyroscopeX(): Float = 0f
  override def getGyroscopeY(): Float = 0f
  override def getGyroscopeZ(): Float = 0f

  override def isPeripheralAvailable(peripheral: Peripheral): Boolean =
    peripheral == Peripheral.HardwareKeyboard

  override def getRotation(): Int = 0

  override def getNativeOrientation(): Orientation = Orientation.Landscape

  override def setOnscreenKeyboardVisible(visible: Boolean):                               Unit = ()
  override def setOnscreenKeyboardVisible(visible: Boolean, `type`: OnscreenKeyboardType): Unit = ()

  override def openTextInputField(configuration:   NativeInputConfiguration): Unit = ()
  override def closeTextInputField(sendReturn:     Boolean):                  Unit = ()
  override def setKeyboardHeightObserver(observer: KeyboardHeightObserver):   Unit = ()

  override def vibrate(milliseconds:  Int):                                    Unit = ()
  override def vibrate(milliseconds:  Int, fallback:  Boolean):                Unit = ()
  override def vibrate(milliseconds:  Int, amplitude: Int, fallback: Boolean): Unit = ()
  override def vibrate(vibrationType: VibrationType):                          Unit = ()

  override def getAzimuth():                            Float = 0f
  override def getPitch():                              Float = 0f
  override def getRoll():                               Float = 0f
  override def getRotationMatrix(matrix: Array[Float]): Unit  = ()
}
