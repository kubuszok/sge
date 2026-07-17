// SGE — RED regression test for ISS-779 (Scala.js / browser only)
//
// DefaultBrowserInput.handleWheel (DefaultBrowserInput.scala:371-379) quantizes
// every wheel event to +/-1, discarding the delta magnitude:
//
//   val scrollAmount = if (e.deltaY > 0) 1 else if (e.deltaY < 0) -1 else 0
//   processor.scrolled(0, scrollAmount.toFloat)
//
// The GWT original (DefaultGwtInput.java:719-722) forwards a *velocity* computed
// from the native wheel delta (getMouseWheelVelocity, line 555: wheelDelta / 120,
// / 80, / 3, etc.) so a larger scroll or a trackpad's fractional step produces a
// proportionally larger/smaller amount — not a flat +/-1. On the desktop
// (Lwjgl3) the amount is likewise the continuous float from GLFW. The browser
// port is the outlier: coarse +/-1 steps.
//
// This suite pins the correct contract — the reported scroll amount is
// proportional to the wheel delta magnitude, never clamped to +/-1. It FAILS
// against the current +/-1 quantization.

package sge
package input

import munit.FunSuite
import org.scalajs.dom.{ Event, EventInit, HTMLCanvasElement, document }
import scala.scalajs.js

class BrowserInputWheelVelocityRedSuite extends FunSuite {

  final private class ScrollRecorder extends InputProcessor {
    val amounts: scala.collection.mutable.ArrayBuffer[Float] =
      scala.collection.mutable.ArrayBuffer.empty
    override def scrolled(amountX: Float, amountY: Float): Boolean = { amounts += amountY; false }
  }

  private def newInput(): (HTMLCanvasElement, ScrollRecorder) = {
    val canvas: HTMLCanvasElement =
      document.createElement("canvas").asInstanceOf[HTMLCanvasElement]
    document.body.appendChild(canvas)
    val config = new BrowserApplicationConfig()
    config.useAccelerometer = false
    config.useGyroscope = false
    lazy val sge: Sge = throw new AssertionError("Sge must not be dereferenced by the wheel handler")
    val input    = new DefaultBrowserInput(canvas, config)(using sge)
    val recorder = new ScrollRecorder
    input.setInputProcessor(recorder)
    (canvas, recorder)
  }

  // jsdom lacks a rich WheelEvent constructor, so synthesize the shape handleWheel
  // reads: a plain Event carrying deltaY / deltaX / deltaMode. deltaMode 0 =
  // DOM_DELTA_PIXEL. This drives the exact registered "wheel" handler path.
  private def dispatchWheel(canvas: HTMLCanvasElement, deltaY: Double): Unit = {
    val e = new Event("wheel", new EventInit { bubbles = true; cancelable = true })
    val d = e.asInstanceOf[js.Dynamic]
    d.deltaY = deltaY
    d.deltaX = 0.0
    d.deltaMode = 0
    canvas.dispatchEvent(e)
  }

  test("a large wheel delta scrolls by more than one unit — not clamped to +/-1 (ISS-779)") {
    val (canvas, rec) = newInput()
    // Three notches worth of scroll (a standard notch is ~100px in DOM_DELTA_PIXEL).
    dispatchWheel(canvas, 300.0)
    assertEquals(rec.amounts.size, 1, s"expected exactly one scroll event; got ${rec.amounts.toList}")
    val amount = rec.amounts.head
    assert(amount > 0f, s"scroll down (deltaY>0) must be positive; got $amount")
    assert(
      amount > 1f,
      s"a 3-notch wheel delta must scroll by more than one unit (GWT/GLFW forward the full velocity); got $amount (ISS-779: clamped to +/-1)"
    )
  }

  test("wheel scroll amount is proportional to the delta magnitude (ISS-779)") {
    val (canvasA, recA) = newInput()
    dispatchWheel(canvasA, 100.0)
    val (canvasB, recB) = newInput()
    dispatchWheel(canvasB, 300.0)

    assertEquals(recA.amounts.size, 1)
    assertEquals(recB.amounts.size, 1)
    val small = recA.amounts.head
    val large = recB.amounts.head
    assert(small > 0f && large > 0f, s"both scrolls should be positive; small=$small large=$large")
    // 300 / 100 => the amounts must scale ~3x regardless of the normalization
    // divisor. The current +/-1 quantization makes both exactly 1.0 (ratio 1.0).
    val ratio = large / small
    assert(
      ratio > 2.5f && ratio < 3.5f,
      s"scroll amount must scale with the delta (deltaY 300 vs 100 => ~3x); got small=$small large=$large ratio=$ratio (ISS-779)"
    )
  }

  test("a fractional trackpad delta produces a fractional (sub-unit) scroll amount (ISS-779)") {
    val (canvas, rec) = newInput()
    // A small trackpad step, well under one notch.
    dispatchWheel(canvas, 8.0)
    assertEquals(rec.amounts.size, 1, s"expected one scroll event; got ${rec.amounts.toList}")
    val amount = rec.amounts.head
    assert(amount > 0f, s"positive delta must scroll positively; got $amount")
    assert(
      amount < 1f,
      s"a sub-notch trackpad delta must yield a sub-unit (fractional) scroll amount, not a full +/-1 step; got $amount (ISS-779)"
    )
  }
}
