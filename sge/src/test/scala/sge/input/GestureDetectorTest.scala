/*
 * SGE — GestureDetector unit tests
 *
 * Tests gesture recognition: tap, long press, fling, pan, zoom, pinch.
 * Uses noop Sge context with custom Input to control timing.
 */
package sge
package input

import munit.FunSuite
import sge.Input.Button
import sge.math.Vector2
import sge.utils.{ Seconds, Timer }

class GestureDetectorTest extends FunSuite {

  override def afterAll(): Unit = {
    Timer.disposeThread()
    super.afterAll()
  }

  // ─── Helpers ─────────────────────────────────────────────────────────

  private val btn0 = Button(0)

  /** Captures gesture callbacks for verification. */
  private class RecordingListener extends GestureDetector.GestureAdapter {
    var touchDownCount: Int = 0
    var tapCount:       Int = 0
    var lastTapCount:   Int = 0
    var longPressCount: Int = 0
    var flingCount:     Int = 0
    var panCount:       Int = 0
    var panStopCount:   Int = 0
    var zoomCount:      Int = 0
    var pinchCount:     Int = 0
    var pinchStopCount: Int = 0

    var lastFlingVX: Float = 0f
    var lastFlingVY: Float = 0f
    var lastPanX:    Float = 0f
    var lastPanY:    Float = 0f
    var lastPanDX:   Float = 0f
    var lastPanDY:   Float = 0f

    override def touchDown(x: Float, y: Float, pointer: Int, button: Button): Boolean = {
      touchDownCount += 1; false
    }

    override def tap(x: Float, y: Float, count: Int, button: Button): Boolean = {
      tapCount += 1; lastTapCount = count; true
    }

    override def longPress(x: Float, y: Float): Boolean = {
      longPressCount += 1; true
    }

    override def fling(velocityX: Float, velocityY: Float, button: Button): Boolean = {
      flingCount += 1; lastFlingVX = velocityX; lastFlingVY = velocityY; true
    }

    override def pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean = {
      panCount += 1; lastPanX = x; lastPanY = y; lastPanDX = deltaX; lastPanDY = deltaY; true
    }

    override def panStop(x: Float, y: Float, pointer: Int, button: Button): Boolean = {
      panStopCount += 1; true
    }

    override def zoom(initialDistance: Float, distance: Float): Boolean = {
      zoomCount += 1; true
    }

    override def pinch(initialPointer1: Vector2, initialPointer2: Vector2, pointer1: Vector2, pointer2: Vector2): Boolean = {
      pinchCount += 1; true
    }

    override def pinchStop(): Unit =
      pinchStopCount += 1
  }

  private def makeContext(): Sge =
    SgeTestFixture.testSge()

  // ─── Tap ─────────────────────────────────────────────────────────────

  test("single tap within tap square fires tap callback") {
    given Sge    = makeContext()
    val listener = RecordingListener()
    val detector = GestureDetector(listener = listener)
    detector.touchDown(100f, 100f, 0, btn0)
    detector.touchUp(101f, 101f, 0, btn0)
    assertEquals(listener.tapCount, 1)
    assertEquals(listener.lastTapCount, 1)
  }

  test("touchDown callback always fires on pointer 0") {
    given Sge    = makeContext()
    val listener = RecordingListener()
    val detector = GestureDetector(listener = listener)
    detector.touchDown(100f, 100f, 0, btn0)
    assertEquals(listener.touchDownCount, 1)
  }

  test("pointer > 1 is ignored") {
    given Sge    = makeContext()
    val listener = RecordingListener()
    val detector = GestureDetector(listener = listener)
    val result   = detector.touchDown(100f, 100f, 2, btn0)
    assertEquals(result, false)
    assertEquals(listener.touchDownCount, 0)
  }

  // ─── Pan ─────────────────────────────────────────────────────────────

  test("drag outside tap square triggers pan") {
    given Sge    = makeContext()
    val listener = RecordingListener()
    val detector = GestureDetector(listener = listener)
    detector.touchDown(100f, 100f, 0, btn0)
    // Move far outside the 20px tap square
    detector.touchDragged(200f, 200f, 0)
    assert(listener.panCount > 0, "Expected pan callback")
    assert(detector.isPanning(), "Expected isPanning() to be true")
  }

  test("drag within tap square does not trigger pan") {
    given Sge    = makeContext()
    val listener = RecordingListener()
    val detector = GestureDetector(listener = listener)
    detector.touchDown(100f, 100f, 0, btn0)
    // Stay within 20px tap rectangle
    detector.touchDragged(105f, 105f, 0)
    assertEquals(listener.panCount, 0)
    assertEquals(detector.isPanning(), false)
  }

  test("pan then touchUp fires panStop") {
    given Sge    = makeContext()
    val listener = RecordingListener()
    val detector = GestureDetector(listener = listener)
    detector.touchDown(100f, 100f, 0, btn0)
    detector.touchDragged(200f, 200f, 0)
    detector.touchUp(200f, 200f, 0, btn0)
    assertEquals(listener.panStopCount, 1)
  }

  // ─── Fling ───────────────────────────────────────────────────────────

  test("quick drag and release fires fling") {
    given Sge    = makeContext()
    val listener = RecordingListener()
    // Use a very large maxFlingDelay so fling is not time-gated
    val detector = GestureDetector(
      maxFlingDelay = Seconds(Integer.MAX_VALUE),
      listener = listener
    )
    detector.touchDown(100f, 100f, 0, btn0)
    detector.touchDragged(200f, 100f, 0)
    detector.touchUp(200f, 100f, 0, btn0)
    assertEquals(listener.flingCount, 1)
  }

  // ─── Cancel / Reset ──────────────────────────────────────────────────

  test("cancel suppresses further gesture events") {
    given Sge    = makeContext()
    val listener = RecordingListener()
    val detector = GestureDetector(listener = listener)
    detector.touchDown(100f, 100f, 0, btn0)
    detector.cancel()
    // After cancel, touchDragged and touchUp should not fire pan/tap
    detector.touchDragged(200f, 200f, 0)
    detector.touchUp(200f, 200f, 0, btn0)
    assertEquals(listener.panCount, 0)
    assertEquals(listener.tapCount, 0)
  }

  test("reset clears internal state") {
    given Sge    = makeContext()
    val listener = RecordingListener()
    val detector = GestureDetector(listener = listener)
    detector.touchDown(100f, 100f, 0, btn0)
    detector.touchDragged(200f, 200f, 0)
    detector.reset()
    assertEquals(detector.isPanning(), false)
  }

  // ─── touchCancelled ──────────────────────────────────────────────────

  test("touchCancelled cancels gesture detection") {
    given Sge    = makeContext()
    val listener = RecordingListener()
    val detector = GestureDetector(listener = listener)
    detector.touchDown(100f, 100f, 0, btn0)
    detector.touchCancelled(Pixels(100), Pixels(100), 0, btn0)
    // After cancel, longPressFired should be true (cancel sets it)
    assert(detector.longPressFired)
  }

  // ─── Tap square size ─────────────────────────────────────────────────

  test("setTapSquareSize changes tap detection area") {
    given Sge    = makeContext()
    val listener = RecordingListener()
    val detector = GestureDetector(listener = listener)
    // Shrink tap square to 5px
    detector.setTapSquareSize(5f)
    detector.touchDown(100f, 100f, 0, btn0)
    // Move 10px — outside 5px tap square, so should trigger pan
    detector.touchDragged(110f, 100f, 0)
    assert(listener.panCount > 0, "Expected pan with smaller tap square")
  }

  // ─── invalidateTapSquare ─────────────────────────────────────────────

  test("invalidateTapSquare forces pan mode") {
    given Sge    = makeContext()
    val listener = RecordingListener()
    val detector = GestureDetector(listener = listener)
    detector.touchDown(100f, 100f, 0, btn0)
    detector.invalidateTapSquare()
    // Even a small drag should trigger pan now
    detector.touchDragged(101f, 101f, 0)
    assert(listener.panCount > 0, "Expected pan after invalidateTapSquare")
  }

  // ─── Pixels-based InputProcessor overrides ───────────────────────────

  test("Pixels-based touchDown delegates to Float touchDown") {
    given Sge    = makeContext()
    val listener = RecordingListener()
    val detector = GestureDetector(listener = listener)
    // Call the InputProcessor Pixels override directly
    detector.touchDown(Pixels(50), Pixels(60), 0, btn0)
    assertEquals(listener.touchDownCount, 1)
  }

  test("Pixels-based touchDragged delegates correctly") {
    given Sge    = makeContext()
    val listener = RecordingListener()
    val detector = GestureDetector(listener = listener)
    detector.touchDown(100f, 100f, 0, btn0)
    detector.touchDragged(Pixels(200), Pixels(200), 0)
    assert(listener.panCount > 0)
  }
}
