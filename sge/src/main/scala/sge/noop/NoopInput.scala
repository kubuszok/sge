/*
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Source: backends/gdx-backend-headless/.../mock/input/MockInput.java
 *   Renames: MockInput -> NoopInput
 *   Convention: setInputProcessor stores value (Java ignores); getInputProcessor initializes eagerly (Java lazy null check);
 *     setCursorCatched/isCursorCatched track state (Java ignores); getMaxPointers returns 1 (Java returns 0)
 *   Idiom: split packages
 *   Audited: 2026-03-03
 */
package sge
package noop

import sge.Input.*

/** A no-op [[sge.Input]] implementation for headless/testing use. All input queries return zero/false defaults.
  */
class NoopInput extends Input {

  private var _inputProcessor: InputProcessor = new InputProcessor {}
  private var _cursorCatched:  Boolean        = false

  // ---- accelerometer / gyroscope ----

  override def getAccelerometerX(): Float = 0.0f

  override def getAccelerometerY(): Float = 0.0f

  override def getAccelerometerZ(): Float = 0.0f

  override def getGyroscopeX(): Float = 0.0f

  override def getGyroscopeY(): Float = 0.0f

  override def getGyroscopeZ(): Float = 0.0f

  override def getMaxPointers(): Int = 1

  // ---- pointer position ----

  override def getX(): Pixels = Pixels.zero

  override def getX(pointer: Int): Pixels = Pixels.zero

  override def getDeltaX(): Pixels = Pixels.zero

  override def getDeltaX(pointer: Int): Pixels = Pixels.zero

  override def getY(): Pixels = Pixels.zero

  override def getY(pointer: Int): Pixels = Pixels.zero

  override def getDeltaY(): Pixels = Pixels.zero

  override def getDeltaY(pointer: Int): Pixels = Pixels.zero

  // ---- touch / button / key ----

  override def isTouched(): Boolean = false

  override def justTouched(): Boolean = false

  override def isTouched(pointer: Int): Boolean = false

  override def getPressure(): Float = 0.0f

  override def getPressure(pointer: Int): Float = 0.0f

  override def isButtonPressed(button: Button): Boolean = false

  override def isButtonJustPressed(button: Button): Boolean = false

  override def isKeyPressed(key: Key): Boolean = false

  override def isKeyJustPressed(key: Key): Boolean = false

  // ---- text input / keyboard ----

  override def getTextInput(listener: TextInputListener, title: String, text: String, hint: String): Unit = {}

  override def getTextInput(
    listener: TextInputListener,
    title:    String,
    text:     String,
    hint:     String,
    `type`:   OnscreenKeyboardType
  ): Unit = {}

  override def setOnscreenKeyboardVisible(visible: Boolean): Unit = {}

  override def setOnscreenKeyboardVisible(visible: Boolean, `type`: OnscreenKeyboardType): Unit = {}

  override def openTextInputField(configuration: input.NativeInputConfiguration): Unit = {}

  override def closeTextInputField(sendReturn: Boolean): Unit = {}

  override def setKeyboardHeightObserver(observer: KeyboardHeightObserver): Unit = {}

  // ---- vibration ----

  override def vibrate(milliseconds: Int): Unit = {}

  override def vibrate(milliseconds: Int, fallback: Boolean): Unit = {}

  override def vibrate(milliseconds: Int, amplitude: Int, fallback: Boolean): Unit = {}

  override def vibrate(vibrationType: VibrationType): Unit = {}

  // ---- orientation / rotation ----

  override def getAzimuth(): Float = 0.0f

  override def getPitch(): Float = 0.0f

  override def getRoll(): Float = 0.0f

  override def getRotationMatrix(matrix: Array[Float]): Unit = {}

  override def currentEventTime: sge.utils.Nanos = sge.utils.Nanos.zero

  // ---- catch keys ----

  override def setCatchKey(keycode: Key, catchKey: Boolean): Unit = {}

  override def isCatchKey(keycode: Key): Boolean = false

  // ---- input processor ----

  override def setInputProcessor(processor: InputProcessor): Unit =
    _inputProcessor = processor

  override def getInputProcessor(): InputProcessor = _inputProcessor

  // ---- peripherals ----

  override def isPeripheralAvailable(peripheral: Peripheral): Boolean = false

  override def getRotation(): Int = 0

  override def getNativeOrientation(): Orientation = Orientation.Landscape

  // ---- cursor ----

  override def setCursorCatched(catched: Boolean): Unit =
    _cursorCatched = catched

  override def isCursorCatched(): Boolean = _cursorCatched

  override def setCursorPosition(x: Pixels, y: Pixels): Unit = {}
}
