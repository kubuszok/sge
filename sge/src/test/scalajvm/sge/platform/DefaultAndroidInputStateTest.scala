// SGE — Tests for DefaultAndroidInputState
//
// Verifies per-pointer state management, pointer ID mapping, event queuing,
// and drain semantics.

package sge
package platform

import munit.FunSuite

class DefaultAndroidInputStateTest extends FunSuite {

  test("initial state has all pointers untouched") {
    val state = DefaultAndroidInputState()
    var i     = 0
    while (i < AndroidInputState.NUM_TOUCHES) {
      assert(!state.isTouched(i))
      assertEquals(state.getTouchX(i), 0)
      assertEquals(state.getTouchY(i), 0)
      assertEquals(state.getButton(i), 0)
      assertEqualsFloat(state.getPressure(i), 0f, 0.001f)
      i += 1
    }
  }

  test("set and get per-pointer state") {
    val state = DefaultAndroidInputState()
    state.setTouchX(0, 100)
    state.setTouchY(0, 200)
    state.setDeltaX(0, 5)
    state.setDeltaY(0, -3)
    state.setTouched(0, true)
    state.setButton(0, 1)
    state.setPressure(0, 0.75f)

    assertEquals(state.getTouchX(0), 100)
    assertEquals(state.getTouchY(0), 200)
    assertEquals(state.getDeltaX(0), 5)
    assertEquals(state.getDeltaY(0), -3)
    assert(state.isTouched(0))
    assertEquals(state.getButton(0), 1)
    assertEqualsFloat(state.getPressure(0), 0.75f, 0.001f)
  }

  test("out-of-bounds pointer returns defaults") {
    val state = DefaultAndroidInputState()
    assertEquals(state.getTouchX(AndroidInputState.NUM_TOUCHES), 0)
    assertEquals(state.getTouchY(AndroidInputState.NUM_TOUCHES), 0)
    assertEquals(state.getDeltaX(AndroidInputState.NUM_TOUCHES), 0)
    assertEquals(state.getDeltaY(AndroidInputState.NUM_TOUCHES), 0)
    assert(!state.isTouched(AndroidInputState.NUM_TOUCHES))
    assertEqualsFloat(state.getPressure(AndroidInputState.NUM_TOUCHES), 0f, 0.001f)
  }

  test("out-of-bounds set is silently ignored") {
    val state = DefaultAndroidInputState()
    // Should not throw
    state.setTouchX(AndroidInputState.NUM_TOUCHES, 999)
    state.setTouched(AndroidInputState.NUM_TOUCHES + 5, true)
  }

  // ── Pointer ID mapping ──────────────────────────────────────────────

  test("getFreePointerIndex returns first free slot") {
    val state = DefaultAndroidInputState()
    assertEquals(state.getFreePointerIndex(), 0)

    state.setRealId(0, 100)
    assertEquals(state.getFreePointerIndex(), 1)

    state.setRealId(1, 101)
    assertEquals(state.getFreePointerIndex(), 2)
  }

  test("lookUpPointerIndex finds mapped pointer") {
    val state = DefaultAndroidInputState()
    state.setRealId(3, 777)
    assertEquals(state.lookUpPointerIndex(777), 3)
    assertEquals(state.lookUpPointerIndex(999), -1) // not found
  }

  test("cancelAllPointers resets all pointers") {
    val state = DefaultAndroidInputState()
    state.setRealId(0, 100)
    state.setTouched(0, true)
    state.setButton(0, 1)
    state.setPressure(0, 0.5f)

    state.setRealId(2, 102)
    state.setTouched(2, true)

    state.cancelAllPointers()

    assertEquals(state.lookUpPointerIndex(100), -1)
    assertEquals(state.lookUpPointerIndex(102), -1)
    assert(!state.isTouched(0))
    assert(!state.isTouched(2))
    assertEquals(state.getButton(0), 0)
    assertEqualsFloat(state.getPressure(0), 0f, 0.001f)
  }

  test("getFreePointerIndex returns NUM_TOUCHES when all slots full") {
    val state = DefaultAndroidInputState()
    var i     = 0
    while (i < AndroidInputState.NUM_TOUCHES) {
      state.setRealId(i, i + 1000)
      i += 1
    }
    assertEquals(state.getFreePointerIndex(), AndroidInputState.NUM_TOUCHES)
  }

  // ── Event queuing ──────────────────────────────────────────────────

  test("postTouchEvent adds to queue") {
    val state = DefaultAndroidInputState()
    state.postTouchEvent(0, 50, 100, 0, 0, 12345L)
    state.postTouchEvent(1, 50, 100, 0, 0, 12346L)

    assertEquals(state.touchEvents.size, 2)
  }

  test("postScrollEvent adds to queue") {
    val state = DefaultAndroidInputState()
    state.postScrollEvent(1, -1, 99999L)
    assertEquals(state.scrollEvents.size, 1)
  }

  test("drainTouchEvents returns snapshot and clears queue") {
    val state = DefaultAndroidInputState()
    state.postTouchEvent(0, 10, 20, 0, 0, 1L)
    state.postTouchEvent(1, 30, 40, 0, 0, 2L)

    val events = state.drainTouchEvents()
    assertEquals(events.size, 2)
    assertEquals(events(0).x, 10)
    assertEquals(events(1).x, 30)

    // Original queue is now empty
    assertEquals(state.touchEvents.size, 0)

    // Draining again gives empty
    val events2 = state.drainTouchEvents()
    assertEquals(events2.size, 0)
  }

  test("drainScrollEvents returns snapshot and clears queue") {
    val state = DefaultAndroidInputState()
    state.postScrollEvent(1, 0, 1L)
    state.postScrollEvent(0, -1, 2L)

    val events = state.drainScrollEvents()
    assertEquals(events.size, 2)
    assertEquals(events(0).scrollAmountX, 1)
    assertEquals(events(1).scrollAmountY, -1)

    assertEquals(state.scrollEvents.size, 0)
  }

  // ── Multiple pointers ──────────────────────────────────────────────

  test("multiple pointers track independent state") {
    val state = DefaultAndroidInputState()

    state.setRealId(0, 10)
    state.setTouchX(0, 100)
    state.setTouchY(0, 200)
    state.setTouched(0, true)

    state.setRealId(1, 20)
    state.setTouchX(1, 300)
    state.setTouchY(1, 400)
    state.setTouched(1, true)

    assertEquals(state.lookUpPointerIndex(10), 0)
    assertEquals(state.lookUpPointerIndex(20), 1)
    assertEquals(state.getTouchX(0), 100)
    assertEquals(state.getTouchX(1), 300)
    assert(state.isTouched(0))
    assert(state.isTouched(1))

    // Release first pointer
    state.setRealId(0, -1)
    state.setTouched(0, false)
    assert(!state.isTouched(0))
    assert(state.isTouched(1))
    assertEquals(state.getFreePointerIndex(), 0) // slot 0 is now free
  }
}
