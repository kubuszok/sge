// SGE — TouchInputOps button-mapping + constant-contract tests
//
// Covers ISS-723 clause c14 (sge-jvm-platform had ZERO test directories).
// Wave 2026-07-18-I, territory I4. TouchInputOps.toSgeButton is the only pure
// branching function in the api layer; the companion constants form a
// cross-module contract (they must match Android MotionEvent flags and sge's
// input button/touch ordinals). Exact-value, mutation-viable assertions.

package sge
package platform
package android

final class TouchInputOpsSuite extends munit.ScalaCheckSuite {

  import org.scalacheck.Prop.forAll

  // ── toSgeButton: exact branch mapping ─────────────────────────────────

  test("toSgeButton maps Android BUTTON flags to SGE button indices") {
    // 0 and 1 both -> LEFT(0): Android reports BUTTON_PRIMARY=1, and legacy
    // touchscreen taps arrive with buttonState 0.
    assertEquals(TouchInputOps.toSgeButton(0), 0) // no-button tap -> LEFT
    assertEquals(TouchInputOps.toSgeButton(1), 0) // BUTTON_PRIMARY  -> LEFT
    assertEquals(TouchInputOps.toSgeButton(2), 1) // BUTTON_SECONDARY-> RIGHT
    assertEquals(TouchInputOps.toSgeButton(4), 2) // BUTTON_TERTIARY -> MIDDLE
    assertEquals(TouchInputOps.toSgeButton(8), 3) // BUTTON_BACK     -> BACK
    assertEquals(TouchInputOps.toSgeButton(16), 4) // BUTTON_FORWARD  -> FORWARD
  }

  test("toSgeButton returns -1 for unrecognised button states") {
    assertEquals(TouchInputOps.toSgeButton(3), -1)
    assertEquals(TouchInputOps.toSgeButton(5), -1)
    assertEquals(TouchInputOps.toSgeButton(6), -1)
    assertEquals(TouchInputOps.toSgeButton(7), -1)
    assertEquals(TouchInputOps.toSgeButton(9), -1)
    assertEquals(TouchInputOps.toSgeButton(32), -1)
    assertEquals(TouchInputOps.toSgeButton(-1), -1)
  }

  property("toSgeButton only ever yields a value in {-1,0,1,2,3,4}") {
    forAll { (state: Int) =>
      val r = TouchInputOps.toSgeButton(state)
      r >= -1 && r <= 4
    }
  }

  property("toSgeButton yields a real button (>=0) exactly for the 6 known states") {
    val known = Set(0, 1, 2, 4, 8, 16)
    forAll { (state: Int) =>
      (TouchInputOps.toSgeButton(state) >= 0) == known.contains(state)
    }
  }

  // ── Companion constant contract ───────────────────────────────────────

  test("MotionEvent action constants hold their documented values") {
    assertEquals(TouchInputOps.ACTION_DOWN, 0)
    assertEquals(TouchInputOps.ACTION_UP, 1)
    assertEquals(TouchInputOps.ACTION_MOVE, 2)
    assertEquals(TouchInputOps.ACTION_CANCEL, 3)
    assertEquals(TouchInputOps.ACTION_OUTSIDE, 4)
    assertEquals(TouchInputOps.ACTION_POINTER_DOWN, 5)
    assertEquals(TouchInputOps.ACTION_POINTER_UP, 6)
    assertEquals(TouchInputOps.ACTION_HOVER_MOVE, 7)
    assertEquals(TouchInputOps.ACTION_SCROLL, 8)
  }

  test("axis + source constants hold their documented values") {
    assertEquals(TouchInputOps.AXIS_VSCROLL, 9)
    assertEquals(TouchInputOps.AXIS_HSCROLL, 10)
    assertEquals(TouchInputOps.SOURCE_CLASS_POINTER, 0x00000002)
  }

  test("SGE touch-event type constants are the distinct ordinals 0..5") {
    val touch = List(
      TouchInputOps.TOUCH_DOWN,
      TouchInputOps.TOUCH_UP,
      TouchInputOps.TOUCH_DRAGGED,
      TouchInputOps.TOUCH_SCROLLED,
      TouchInputOps.TOUCH_MOVED,
      TouchInputOps.TOUCH_CANCELLED
    )
    assertEquals(touch, List(0, 1, 2, 3, 4, 5))
    assertEquals(touch.distinct.size, 6)
  }
}
