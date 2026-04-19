/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package scenes
package scene2d
package utils

import sge.Input.Button
import sge.utils.Nullable

/** Tests for ClickListener: initial state, tap square, cancel, pressed/over state. */
class ClickListenerTest extends munit.FunSuite {

  private def ctx(): Sge = SgeTestFixture.testSge()

  // ---------------------------------------------------------------------------
  // Default state
  // ---------------------------------------------------------------------------

  test("ClickListener default state") {
    val cl = ClickListener()
    assertEquals(cl.button, Button(0))
    assertEquals(cl.tapSquareSize, 14f)
    assertEquals(cl.tapCount, 0)
    assert(!cl.pressed)
    assert(!cl.over)
    assertEquals(cl.pressedPointer, -1)
    assertEquals(cl.pressedButton, Button(-1))
    assertEquals(cl.touchDownX, -1f)
    assertEquals(cl.touchDownY, -1f)
  }

  test("ClickListener custom button") {
    val cl = ClickListener(Button(1))
    assertEquals(cl.button, Button(1))
  }

  // ---------------------------------------------------------------------------
  // Tap square
  // ---------------------------------------------------------------------------

  test("inTapSquare returns false when no touch started") {
    val cl = ClickListener()
    assert(!cl.inTapSquare())
    assert(!cl.inTapSquare(0f, 0f))
  }

  test("inTapSquare detects within tap square after touchDown") {
    given Sge = ctx()
    val actor = Actor()
    actor.setSize(200f, 200f)
    val cl = ClickListener()
    actor.addListener(cl)

    val event = InputEvent()
    event.eventType = InputEvent.Type.touchDown
    event.target = Nullable(actor)
    event.listenerActor = Nullable(actor)

    // Simulate touchDown
    val handled = cl.touchDown(event, 50f, 50f, 0, Button(0))
    assert(handled)

    // Within tap square
    assert(cl.inTapSquare(50f, 50f))
    assert(cl.inTapSquare(60f, 60f)) // within 14 pixels

    // Outside tap square
    assert(!cl.inTapSquare(100f, 100f))
  }

  test("invalidateTapSquare resets tap square") {
    val cl = ClickListener()
    val event = InputEvent()

    cl.touchDown(event, 50f, 50f, 0, Button(0))
    assert(cl.inTapSquare())

    cl.invalidateTapSquare()
    assert(!cl.inTapSquare())
    assertEquals(cl.touchDownX, -1f)
    assertEquals(cl.touchDownY, -1f)
  }

  // ---------------------------------------------------------------------------
  // Pressed / over state via touchDown / touchUp
  // ---------------------------------------------------------------------------

  test("touchDown sets pressed state") {
    val cl    = ClickListener()
    val event = InputEvent()

    val handled = cl.touchDown(event, 10f, 20f, 0, Button(0))
    assert(handled)
    assert(cl.pressed)
    assertEquals(cl.pressedPointer, 0)
    assertEquals(cl.pressedButton, Button(0))
    assertEquals(cl.touchDownX, 10f)
    assertEquals(cl.touchDownY, 20f)
  }

  test("touchDown returns false if already pressed") {
    val cl    = ClickListener()
    val event = InputEvent()

    cl.touchDown(event, 10f, 20f, 0, Button(0))
    val second = cl.touchDown(event, 30f, 40f, 1, Button(0))
    assert(!second)
  }

  test("touchDown rejects wrong button") {
    val cl    = ClickListener(Button(0))
    val event = InputEvent()

    // pointer 0 with wrong button
    val handled = cl.touchDown(event, 10f, 20f, 0, Button(1))
    assert(!handled)
    assert(!cl.pressed)
  }

  test("touchDown accepts any button when button is -1") {
    val cl    = ClickListener(Button(-1))
    val event = InputEvent()

    val handled = cl.touchDown(event, 10f, 20f, 0, Button(2))
    assert(handled)
    assert(cl.pressed)
  }

  test("touchUp clears pressed state") {
    given Sge = ctx()
    val actor = Actor()
    actor.setSize(200f, 200f)
    val cl = ClickListener()
    actor.addListener(cl)

    val event = InputEvent()
    event.target = Nullable(actor)
    event.listenerActor = Nullable(actor)

    cl.touchDown(event, 10f, 10f, 0, Button(0))
    assert(cl.pressed)

    cl.touchUp(event, 10f, 10f, 0, Button(0))
    assert(!cl.pressed)
    assertEquals(cl.pressedPointer, -1)
    assertEquals(cl.pressedButton, Button(-1))
  }

  test("touchUp on wrong pointer is ignored") {
    val cl    = ClickListener()
    val event = InputEvent()

    cl.touchDown(event, 10f, 10f, 0, Button(0))
    cl.touchUp(event, 10f, 10f, 1, Button(0)) // wrong pointer
    // Still pressed because touchUp was for a different pointer
    assert(cl.pressed)
  }

  // ---------------------------------------------------------------------------
  // Enter / exit (mouse over state)
  // ---------------------------------------------------------------------------

  test("enter sets over state for pointer -1") {
    val cl    = ClickListener()
    val event = InputEvent()

    cl.enter(event, 10f, 20f, -1, Nullable.empty)
    assert(cl.over)
  }

  test("exit clears over state for pointer -1") {
    val cl    = ClickListener()
    val event = InputEvent()

    cl.enter(event, 10f, 20f, -1, Nullable.empty)
    assert(cl.over)

    cl.exit(event, 10f, 20f, -1, Nullable.empty)
    // over property is _over || _pressed — if not pressed, should not be over
    assert(!cl.pressed)
    // But the over property combines _over and _pressed
    assert(!cl.over)
  }

  test("enter with non-negative pointer does not affect over") {
    val cl    = ClickListener()
    val event = InputEvent()

    cl.enter(event, 10f, 20f, 0, Nullable.empty)
    // pointer != -1, so _over should not change
    assert(!cl.over)
  }

  // ---------------------------------------------------------------------------
  // Cancel
  // ---------------------------------------------------------------------------

  test("cancel clears pressed state") {
    val cl    = ClickListener()
    val event = InputEvent()

    cl.touchDown(event, 10f, 10f, 0, Button(0))
    assert(cl.pressed)

    cl.cancel()
    assert(!cl.pressed)
  }

  test("cancel does nothing when not pressed") {
    val cl = ClickListener()
    cl.cancel() // should not throw
    assert(!cl.pressed)
  }

  // ---------------------------------------------------------------------------
  // Clicked callback
  // ---------------------------------------------------------------------------

  test("touchUp triggers clicked callback") {
    given Sge = ctx()
    val actor = Actor()
    actor.setSize(200f, 200f)
    var clickCount = 0
    val cl = new ClickListener() {
      override def clicked(event: InputEvent, x: Float, y: Float): Unit =
        clickCount += 1
    }
    actor.addListener(cl)

    val event = InputEvent()
    event.target = Nullable(actor)
    event.listenerActor = Nullable(actor)

    cl.touchDown(event, 50f, 50f, 0, Button(0))
    cl.touchUp(event, 50f, 50f, 0, Button(0))
    assertEquals(clickCount, 1)
    assertEquals(cl.tapCount, 1)
  }

  // ---------------------------------------------------------------------------
  // Tap count interval
  // ---------------------------------------------------------------------------

  test("setTapCountInterval updates interval") {
    val cl = ClickListener()
    cl.setTapCountInterval(0.5f)
    // Just verify it doesn't throw; the internal nanos are not publicly accessible
  }

  // ---------------------------------------------------------------------------
  // Visual pressed duration
  // ---------------------------------------------------------------------------

  test("visualPressedDuration companion object default") {
    assertEquals(ClickListener.visualPressedDuration, 0.1f)
  }

  test("tapSquareSize can be changed") {
    val cl = ClickListener()
    cl.tapSquareSize = 20f
    assertEquals(cl.tapSquareSize, 20f)
  }
}
