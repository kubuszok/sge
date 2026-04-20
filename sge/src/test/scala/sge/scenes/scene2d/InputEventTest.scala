/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package scenes
package scene2d

import sge.Input.{ Button, Key }
import sge.utils.Nullable

/** Tests for InputEvent: property access, type enum, reset, isTouchFocusCancel. */
class InputEventTest extends munit.FunSuite {

  private def ctx(): Sge = SgeTestFixture.testSge()

  // ---------------------------------------------------------------------------
  // Default state
  // ---------------------------------------------------------------------------

  test("InputEvent default state") {
    val event = InputEvent()
    assertEquals(event.stageX, 0f)
    assertEquals(event.stageY, 0f)
    assertEquals(event.scrollAmountX, 0f)
    assertEquals(event.scrollAmountY, 0f)
    assertEquals(event.pointer, 0)
    assertEquals(event.button, Button(0))
    assertEquals(event.keyCode, Key(0))
    assertEquals(event.character, '\u0000')
    assert(event.relatedActor.isEmpty)
    assert(event.touchFocus)
  }

  // ---------------------------------------------------------------------------
  // Setting properties
  // ---------------------------------------------------------------------------

  test("InputEvent properties can be set") {
    given Sge = ctx()
    val event = InputEvent()
    event.stageX = 100f
    event.stageY = 200f
    event.pointer = 2
    event.button = Button(1)
    event.keyCode = Key(42)
    event.character = 'A'
    event.scrollAmountX = 1.5f
    event.scrollAmountY = -2.5f
    event.touchFocus = false

    val related = Actor()
    event.relatedActor = Nullable(related)

    assertEquals(event.stageX, 100f)
    assertEquals(event.stageY, 200f)
    assertEquals(event.pointer, 2)
    assertEquals(event.button, Button(1))
    assertEquals(event.keyCode, Key(42))
    assertEquals(event.character, 'A')
    assertEquals(event.scrollAmountX, 1.5f)
    assertEquals(event.scrollAmountY, -2.5f)
    assert(!event.touchFocus)
    assert(event.relatedActor.exists(_ eq related))
  }

  // ---------------------------------------------------------------------------
  // Event type enum
  // ---------------------------------------------------------------------------

  test("InputEvent.Type enum has all expected values") {
    val types = InputEvent.Type.values
    assertEquals(types.length, 10)
    assert(types.contains(InputEvent.Type.touchDown))
    assert(types.contains(InputEvent.Type.touchUp))
    assert(types.contains(InputEvent.Type.touchDragged))
    assert(types.contains(InputEvent.Type.mouseMoved))
    assert(types.contains(InputEvent.Type.enter))
    assert(types.contains(InputEvent.Type.exit))
    assert(types.contains(InputEvent.Type.scrolled))
    assert(types.contains(InputEvent.Type.keyDown))
    assert(types.contains(InputEvent.Type.keyUp))
    assert(types.contains(InputEvent.Type.keyTyped))
  }

  test("InputEvent eventType can be assigned") {
    val event = InputEvent()
    event.eventType = InputEvent.Type.touchDown
    assertEquals(event.eventType, InputEvent.Type.touchDown)
  }

  // ---------------------------------------------------------------------------
  // Reset
  // ---------------------------------------------------------------------------

  test("InputEvent reset clears relatedActor and button") {
    given Sge   = ctx()
    val event   = InputEvent()
    val related = Actor()
    event.relatedActor = Nullable(related)
    event.button = Button(2)
    event.stageX = 50f
    event.stageY = 60f

    event.reset()

    assert(event.relatedActor.isEmpty)
    assertEquals(event.button, Button(-1))
    // Inherited Event fields are also reset
    assert(event.target.isEmpty)
    assert(event.stage.isEmpty)
    assert(!event.isStopped)
    assert(!event.isCancelled)
    assert(!event.isHandled)
    assert(event.bubbles)
    assert(!event.capture)
  }

  // ---------------------------------------------------------------------------
  // isTouchFocusCancel
  // ---------------------------------------------------------------------------

  test("isTouchFocusCancel returns true when stageX is Int.MinValue") {
    val event = InputEvent()
    event.stageX = Int.MinValue.toFloat
    event.stageY = 0f
    assert(event.isTouchFocusCancel)
  }

  test("isTouchFocusCancel returns true when stageY is Int.MinValue") {
    val event = InputEvent()
    event.stageX = 0f
    event.stageY = Int.MinValue.toFloat
    assert(event.isTouchFocusCancel)
  }

  test("isTouchFocusCancel returns false for normal coordinates") {
    val event = InputEvent()
    event.stageX = 100f
    event.stageY = 200f
    assert(!event.isTouchFocusCancel)
  }

  // ---------------------------------------------------------------------------
  // toString
  // ---------------------------------------------------------------------------

  test("toString returns the event type name") {
    val event = InputEvent()
    event.eventType = InputEvent.Type.keyDown
    assertEquals(event.toString, "keyDown")
  }

  // ---------------------------------------------------------------------------
  // Inheritance: Event flags work on InputEvent
  // ---------------------------------------------------------------------------

  test("InputEvent inherits Event stop/cancel/handle") {
    val event = InputEvent()

    assert(!event.isHandled)
    event.handle()
    assert(event.isHandled)

    assert(!event.isStopped)
    event.stop()
    assert(event.isStopped)

    val event2 = InputEvent()
    event2.cancel()
    assert(event2.isCancelled)
    assert(event2.isStopped)
    assert(event2.isHandled)
  }
}
