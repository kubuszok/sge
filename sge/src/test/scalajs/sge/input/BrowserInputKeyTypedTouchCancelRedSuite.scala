// SGE — RED regression test for ISS-542 (Scala.js / browser only)
//
// DefaultBrowserInput diverges from the GWT original (DefaultGwtInput) and the
// other platforms on two input paths:
//
//  1. keyTyped: the GWT original emits keyTyped from the `keypress` event with the
//     typed character (DefaultGwtInput.java line 753-762: `if (e.getType().equals(
//     "keypress")) { char c = (char)e.getCharCode(); ... processor.keyTyped(c); }`).
//     The SGE port registers NO `keypress` listener and instead emits keyTyped from
//     the `keyup` handler. Held keys therefore give no OS auto-repeat and typed
//     characters arrive only after key release.
//
//  2. touchcancel: the OS/browser delivers `touchcancel` when a touch gesture is
//     interrupted (cheek, system gesture, etc). The SGE InputProcessor — like
//     Android, which posts TOUCH_CANCELLED — exposes a dedicated `touchCancelled`
//     callback for exactly this. The SGE port wires `touchcancel` to the same
//     handler as `touchend`, which posts `touchUp`, so `touchCancelled` never fires
//     on the browser.
//
// This suite pins the correct behavior. It FAILS against the current code:
//   (a) keypress yields nothing / keyup yields the keyTyped, and
//   (b) touchcancel posts touchUp instead of touchCancelled.

package sge
package input

import munit.FunSuite
import org.scalajs.dom.{ Event, HTMLCanvasElement, KeyboardEvent, KeyboardEventInit, document }
import sge.Input.{ Button, Key }
import scala.scalajs.js

class BrowserInputKeyTypedTouchCancelRedSuite extends FunSuite {

  // A recording InputProcessor: appends every callback it receives to `events`,
  // tagged by a stable label so the assertions can inspect what fired.
  final private class Recorder extends InputProcessor {
    val events: scala.collection.mutable.ArrayBuffer[String] =
      scala.collection.mutable.ArrayBuffer.empty

    override def keyDown(keycode:    Key):  Boolean = { events += s"keyDown(${keycode.toInt})"; false }
    override def keyUp(keycode:      Key):  Boolean = { events += s"keyUp(${keycode.toInt})"; false }
    override def keyTyped(character: Char): Boolean = { events += s"keyTyped($character)"; false }

    override def touchDown(screenX: Pixels, screenY: Pixels, pointer: Int, button: Button): Boolean = {
      events += s"touchDown($pointer)"; false
    }
    override def touchUp(screenX: Pixels, screenY: Pixels, pointer: Int, button: Button): Boolean = {
      events += s"touchUp($pointer)"; false
    }
    override def touchCancelled(screenX: Pixels, screenY: Pixels, pointer: Int, button: Button): Boolean = {
      events += s"touchCancelled($pointer)"; false
    }
    override def touchDragged(screenX: Pixels, screenY: Pixels, pointer: Int): Boolean = {
      events += s"touchDragged($pointer)"; false
    }
  }

  // Build a DefaultBrowserInput on a fresh canvas attached to the document so the
  // canvas-level listeners (touch*) and the document-level listeners (key*) both
  // receive dispatched events. Sensors are disabled to keep the test hermetic; the
  // deferred Sge is never dereferenced by any path this test exercises.
  private def newInput(): (HTMLCanvasElement, Recorder) = {
    val canvas: HTMLCanvasElement =
      document.createElement("canvas").asInstanceOf[HTMLCanvasElement]
    document.body.appendChild(canvas)
    val config = new BrowserApplicationConfig()
    config.useAccelerometer = false
    config.useGyroscope = false
    // Sge is only touched by handleDocMouseDown, which this test never triggers.
    lazy val sge: Sge = throw new AssertionError("Sge must not be dereferenced by key/touch handlers")
    val input    = new DefaultBrowserInput(canvas, config)(using sge)
    val recorder = new Recorder
    input.setInputProcessor(recorder)
    (canvas, recorder)
  }

  test("keypress emits keyTyped(char); keyup does NOT emit keyTyped (ISS-542)") {
    val (_, recorder) = newInput()

    // Dispatch a real DOM keypress for the printable char 'a' on the document,
    // matching where DefaultBrowserInput registers its keyboard listeners.
    val pressInit = new KeyboardEventInit {}
    pressInit.key = "a"
    pressInit.keyCode = 65 // 'A' DOM keyCode (also drives keyForCode)
    pressInit.charCode = 97 // 'a' — what the GWT original reads via getCharCode()
    pressInit.bubbles = true
    val press = new KeyboardEvent("keypress", pressInit)
    document.dispatchEvent(press)

    assert(
      recorder.events.contains("keyTyped(a)"),
      s"expected a keypress to produce keyTyped(a); recorded=${recorder.events.toList}"
    )

    // A bare keyup must NOT, by itself, synthesize keyTyped — that is the
    // keypress event's job. Dispatch keyup on a fresh input and assert no keyTyped.
    val (_, recorder2) = newInput()
    val upInit         = new KeyboardEventInit {}
    upInit.key = "b"
    upInit.keyCode = 66
    upInit.bubbles = true
    val up = new KeyboardEvent("keyup", upInit)
    document.dispatchEvent(up)

    assert(
      !recorder2.events.exists(_.startsWith("keyTyped")),
      s"keyup must not emit keyTyped; recorded=${recorder2.events.toList}"
    )
  }

  test("touchcancel routes to touchCancelled, not touchUp (ISS-542)") {
    val (canvas, recorder) = newInput()

    // jsdom does not provide a TouchEvent constructor, so we synthesize the event
    // shape the canvas listener actually reads: a plain Event carrying a
    // `changedTouches` JS array of {identifier, clientX, clientY}. This drives the
    // exact registered touchstart/touchcancel handler paths.
    def touchEvent(kind: String, identifier: Int): Event = {
      val touch   = js.Dynamic.literal(identifier = identifier, clientX = 5.0, clientY = 7.0)
      val touches = js.Array[js.Dynamic](touch)
      val e       = new Event(kind, new org.scalajs.dom.EventInit { bubbles = true })
      e.asInstanceOf[js.Dynamic].changedTouches = touches
      e
    }

    // First register a touch so the cancel has a live pointer in the touch map.
    canvas.dispatchEvent(touchEvent("touchstart", 0))
    assert(
      recorder.events.exists(_.startsWith("touchDown")),
      s"touchstart should have produced a touchDown; recorded=${recorder.events.toList}"
    )

    // Now cancel it. The processor must see touchCancelled and NOT touchUp.
    canvas.dispatchEvent(touchEvent("touchcancel", 0))

    assert(
      recorder.events.contains("touchCancelled(0)"),
      s"touchcancel must produce touchCancelled(0); recorded=${recorder.events.toList}"
    )
    assert(
      !recorder.events.exists(_.startsWith("touchUp")),
      s"touchcancel must NOT produce touchUp; recorded=${recorder.events.toList}"
    )
  }
}
