package sge
package screen

import sge.graphics.g2d.TextureRegion
import sge.math.Interpolation
import sge.screen.transition.TimedTransition
import sge.utils.{ Nullable, Seconds }

class TimedTransitionSuite extends munit.FunSuite {

  /** Concrete TimedTransition that records the progress values it receives. */
  class TestTimedTransition(
    dur:    Float,
    interp: Nullable[Interpolation] = Nullable.empty
  ) extends TimedTransition(dur, interp) {
    var lastProgress: Float = -1f
    var renderCount:  Int   = 0

    override def render(delta: Seconds, lastScreen: TextureRegion, currScreen: TextureRegion, progress: Float): Unit = {
      lastProgress = progress
      renderCount += 1
    }

    override def resize(width: Pixels, height: Pixels): Unit = ()
    override def close():                               Unit = ()
  }

  // Dummy texture regions (only used as opaque parameters, never rendered)
  private val dummyRegion: TextureRegion = TextureRegion()

  test("progress increases with delta") {
    val t = TestTimedTransition(1.0f)
    t.show()
    t.render(Seconds(0.25f), dummyRegion, dummyRegion)
    assert(t.lastProgress > 0f, s"progress should be > 0, was ${t.lastProgress}")
    val first = t.lastProgress
    t.render(Seconds(0.25f), dummyRegion, dummyRegion)
    assert(t.lastProgress > first, s"progress should increase, was ${t.lastProgress}")
  }

  test("progress clamps at 1.0") {
    val t = TestTimedTransition(0.5f)
    t.show()
    // Overshoot the duration
    t.render(Seconds(1.0f), dummyRegion, dummyRegion)
    assertEqualsFloat(t.lastProgress, 1.0f, 0.001f)
  }

  test("isDone returns true when timePassed >= duration") {
    val t = TestTimedTransition(0.5f)
    t.show()
    assert(!t.isDone)
    t.render(Seconds(0.3f), dummyRegion, dummyRegion)
    assert(!t.isDone)
    t.render(Seconds(0.3f), dummyRegion, dummyRegion)
    assert(t.isDone)
  }

  test("interpolation is applied to progress") {
    // Use an interpolation that squares the input
    val squareInterp: Interpolation = (a: Float) => a * a
    val t = TestTimedTransition(1.0f, Nullable(squareInterp))
    t.show()
    t.render(Seconds(0.5f), dummyRegion, dummyRegion)
    // Without interpolation, progress would be 0.5. With square, it should be 0.25.
    assertEqualsFloat(t.lastProgress, 0.25f, 0.01f)
  }

  test("show() resets timePassed to 0") {
    val t = TestTimedTransition(1.0f)
    t.show()
    t.render(Seconds(0.8f), dummyRegion, dummyRegion)
    assert(t.lastProgress > 0.5f)
    // Reset
    t.show()
    assert(!t.isDone)
    t.render(Seconds(0.1f), dummyRegion, dummyRegion)
    assertEqualsFloat(t.lastProgress, 0.1f, 0.01f)
  }

  test("duration must be positive") {
    intercept[IllegalArgumentException] {
      TestTimedTransition(0f)
    }
    intercept[IllegalArgumentException] {
      TestTimedTransition(-1f)
    }
  }

  private def assertEqualsFloat(actual: Float, expected: Float, delta: Float)(using munit.Location): Unit =
    assert(Math.abs(actual - expected) <= delta, s"expected $expected +/- $delta but got $actual")
}
