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
    assertEquals(input.x, Pixels.zero)
    assertEquals(input.x(0), Pixels.zero)
    assertEquals(input.y, Pixels.zero)
    assertEquals(input.y(0), Pixels.zero)
    assertEquals(input.deltaX, Pixels.zero)
    assertEquals(input.deltaX(0), Pixels.zero)
    assertEquals(input.deltaY, Pixels.zero)
    assertEquals(input.deltaY(0), Pixels.zero)
  }

  test("accelerometer and gyroscope return 0") {
    val input = NoopInput()
    assertEquals(input.accelerometerX, 0.0f)
    assertEquals(input.accelerometerY, 0.0f)
    assertEquals(input.accelerometerZ, 0.0f)
    assertEquals(input.gyroscopeX, 0.0f)
    assertEquals(input.gyroscopeY, 0.0f)
    assertEquals(input.gyroscopeZ, 0.0f)
  }

  test("touch and button queries return false") {
    val input = NoopInput()
    assertEquals(input.touched, false)
    assertEquals(input.justTouched(), false)
    assertEquals(input.isTouched(0), false)
    assertEquals(input.isButtonPressed(Button(0)), false)
    assertEquals(input.isButtonJustPressed(Button(0)), false)
    assertEquals(input.isKeyPressed(Key(0)), false)
    assertEquals(input.isKeyJustPressed(Key(0)), false)
    assertEquals(input.pressure, 0.0f)
    assertEquals(input.pressure(0), 0.0f)
  }

  test("orientation returns defaults") {
    val input = NoopInput()
    assertEquals(input.rotation, 0)
    assertEquals(input.azimuth, 0.0f)
    assertEquals(input.pitch, 0.0f)
    assertEquals(input.roll, 0.0f)
    assertEquals(input.nativeOrientation, Input.Orientation.Landscape)
    assertEquals(input.maxPointers, 1)
    assertEquals(input.currentEventTime, sge.utils.Nanos.zero)
  }

  // ---- input processor round-trip ----

  test("setInputProcessor/getInputProcessor round-trip") {
    val input     = NoopInput()
    val processor = new InputProcessor {
      override def keyDown(keycode: Key): Boolean = true
    }
    input.setInputProcessor(processor)
    assert(input.inputProcessor eq processor)
  }

  // ---- cursor catched round-trip ----

  test("setCursorCatched/isCursorCatched round-trip") {
    val input = NoopInput()
    assertEquals(input.cursorCatched, false)
    input.setCursorCatched(true)
    assertEquals(input.cursorCatched, true)
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
