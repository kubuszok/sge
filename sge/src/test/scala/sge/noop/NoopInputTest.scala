/*
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package noop

import sge.Input.{ Button, Key }

class NoopInputTest extends munit.FunSuite {

  // ---- default zero/false values ----

  test("all position getters return 0") {
    val input = NoopInput()
    assertEquals(input.getX(), Pixels.zero)
    assertEquals(input.getX(0), Pixels.zero)
    assertEquals(input.getY(), Pixels.zero)
    assertEquals(input.getY(0), Pixels.zero)
    assertEquals(input.getDeltaX(), Pixels.zero)
    assertEquals(input.getDeltaX(0), Pixels.zero)
    assertEquals(input.getDeltaY(), Pixels.zero)
    assertEquals(input.getDeltaY(0), Pixels.zero)
  }

  test("accelerometer and gyroscope return 0") {
    val input = NoopInput()
    assertEquals(input.getAccelerometerX(), 0.0f)
    assertEquals(input.getAccelerometerY(), 0.0f)
    assertEquals(input.getAccelerometerZ(), 0.0f)
    assertEquals(input.getGyroscopeX(), 0.0f)
    assertEquals(input.getGyroscopeY(), 0.0f)
    assertEquals(input.getGyroscopeZ(), 0.0f)
  }

  test("touch and button queries return false") {
    val input = NoopInput()
    assertEquals(input.isTouched(), false)
    assertEquals(input.justTouched(), false)
    assertEquals(input.isTouched(0), false)
    assertEquals(input.isButtonPressed(Button(0)), false)
    assertEquals(input.isButtonJustPressed(Button(0)), false)
    assertEquals(input.isKeyPressed(Key(0)), false)
    assertEquals(input.isKeyJustPressed(Key(0)), false)
    assertEquals(input.getPressure(), 0.0f)
    assertEquals(input.getPressure(0), 0.0f)
  }

  test("orientation returns defaults") {
    val input = NoopInput()
    assertEquals(input.getRotation(), 0)
    assertEquals(input.getAzimuth(), 0.0f)
    assertEquals(input.getPitch(), 0.0f)
    assertEquals(input.getRoll(), 0.0f)
    assertEquals(input.getNativeOrientation(), Input.Orientation.Landscape)
    assertEquals(input.getMaxPointers(), 1)
    assertEquals(input.currentEventTime, sge.utils.Nanos.zero)
  }

  // ---- input processor round-trip ----

  test("setInputProcessor/getInputProcessor round-trip") {
    val input     = NoopInput()
    val processor = new InputProcessor {
      override def keyDown(keycode: Key): Boolean = true
    }
    input.setInputProcessor(processor)
    assert(input.getInputProcessor() eq processor)
  }

  // ---- cursor catched round-trip ----

  test("setCursorCatched/isCursorCatched round-trip") {
    val input = NoopInput()
    assertEquals(input.isCursorCatched(), false)
    input.setCursorCatched(true)
    assertEquals(input.isCursorCatched(), true)
  }

  // ---- no-op methods don't throw ----

  test("no-op methods do not throw") {
    val input = NoopInput()
    input.vibrate(100)
    input.vibrate(100, true)
    input.vibrate(100, 128, true)
    input.vibrate(Input.VibrationType.LIGHT)
    input.setOnscreenKeyboardVisible(true)
    input.setCatchKey(Key(0), true)
    assertEquals(input.isCatchKey(Key(0)), false)
    input.setCursorPosition(Pixels(10), Pixels(20))
    input.getRotationMatrix(new Array[Float](16))
    assertEquals(input.isPeripheralAvailable(Input.Peripheral.HardwareKeyboard), false)
  }
}
