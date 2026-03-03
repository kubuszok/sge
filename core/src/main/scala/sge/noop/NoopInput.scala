/*
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
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

  override def getX(): Int = 0

  override def getX(pointer: Int): Int = 0

  override def getDeltaX(): Int = 0

  override def getDeltaX(pointer: Int): Int = 0

  override def getY(): Int = 0

  override def getY(pointer: Int): Int = 0

  override def getDeltaY(): Int = 0

  override def getDeltaY(pointer: Int): Int = 0

  // ---- touch / button / key ----

  override def isTouched(): Boolean = false

  override def justTouched(): Boolean = false

  override def isTouched(pointer: Int): Boolean = false

  override def getPressure(): Float = 0.0f

  override def getPressure(pointer: Int): Float = 0.0f

  override def isButtonPressed(button: Int): Boolean = false

  override def isButtonJustPressed(button: Int): Boolean = false

  override def isKeyPressed(key: Int): Boolean = false

  override def isKeyJustPressed(key: Int): Boolean = false

  // ---- text input / keyboard ----

  override def getTextInput(listener: TextInputListener, title: String, text: String, hint: String): Unit = {}

  override def getTextInput(
    listener: TextInputListener,
    title:    String,
    text:     String,
    hint:     String,
    `type`:   OnscreenKeyboardType.OnscreenKeyboardType
  ): Unit = {}

  override def setOnscreenKeyboardVisible(visible: Boolean): Unit = {}

  override def setOnscreenKeyboardVisible(visible: Boolean, `type`: OnscreenKeyboardType.OnscreenKeyboardType): Unit = {}

  override def openTextInputField(configuration: input.NativeInputConfiguration): Unit = {}

  override def closeTextInputField(sendReturn: Boolean): Unit = {}

  override def setKeyboardHeightObserver(observer: KeyboardHeightObserver): Unit = {}

  // ---- vibration ----

  override def vibrate(milliseconds: Int): Unit = {}

  override def vibrate(milliseconds: Int, fallback: Boolean): Unit = {}

  override def vibrate(milliseconds: Int, amplitude: Int, fallback: Boolean): Unit = {}

  override def vibrate(vibrationType: VibrationType.VibrationType): Unit = {}

  // ---- orientation / rotation ----

  override def getAzimuth(): Float = 0.0f

  override def getPitch(): Float = 0.0f

  override def getRoll(): Float = 0.0f

  override def getRotationMatrix(matrix: Array[Float]): Unit = {}

  override def getCurrentEventTime(): Long = 0L

  // ---- catch keys ----

  override def setCatchKey(keycode: Int, catchKey: Boolean): Unit = {}

  override def isCatchKey(keycode: Int): Boolean = false

  // ---- input processor ----

  override def setInputProcessor(processor: InputProcessor): Unit =
    _inputProcessor = processor

  override def getInputProcessor(): InputProcessor = _inputProcessor

  // ---- peripherals ----

  override def isPeripheralAvailable(peripheral: Peripheral.Peripheral): Boolean = false

  override def getRotation(): Int = 0

  override def getNativeOrientation(): Orientation.Orientation = Orientation.Landscape

  // ---- cursor ----

  override def setCursorCatched(catched: Boolean): Unit =
    _cursorCatched = catched

  override def isCursorCatched(): Boolean = _cursorCatched

  override def setCursorPosition(x: Int, y: Int): Unit = {}
}
