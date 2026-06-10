/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package scenes
package scene2d
package utils

import lowlevel.Nullable

/** Red tests for ISS-501: Selection.choose dropped the three Java early returns.
  *
  * Original (com/badlogic/gdx/scenes/scene2d/utils/Selection.java, choose()):
  *   - `if (required && selected.size == 1) return;` (toggle/ctrl branch)
  *   - `if (selected.size == 1 && selected.contains(item)) return;`
  *   - `if (!selected.add(item) && !modified) return;`
  *
  * In the port these became no-op `()` branches, so control always falls through to `if (fireChangeEvent()) revert() else changed()` — every click on an already-selected item fires a spurious
  * ChangeEvent and invokes changed() (which in ArraySelection/Tree resets shift-range anchors).
  */
class SelectionChooseRedSuite extends munit.FunSuite {

  /** Selection subclass instrumenting the protected changed() hook. */
  final private class CountingSelection(using Sge) extends Selection[String] {
    var changedCalls:                 Int  = 0
    override protected def changed(): Unit =
      changedCalls += 1
  }

  /** Input reporting all ctrl-ish keys (SYM for mac, CONTROL_LEFT/RIGHT elsewhere) as held, so UIUtils.ctrl() returns true on every platform. Everything else delegates to NoopInput. */
  final private class CtrlHeldInput extends Input {
    private val delegate = new sge.noop.NoopInput
    export delegate.{ isKeyPressed as _, * }
    override def isKeyPressed(key: Input.Key): Boolean =
      key == Input.Keys.SYM || key == Input.Keys.CONTROL_LEFT || key == Input.Keys.CONTROL_RIGHT
  }

  /** Selection attached to an Actor whose ChangeListener counts ChangeEvents fired via actor.fire (matches the Java event path). */
  final private class Harness(ctrlHeld: Boolean) {
    given sge: Sge =
      if (ctrlHeld) SgeTestFixture.testSge(input = new CtrlHeldInput)
      else SgeTestFixture.testSge()
    val selection:    CountingSelection = new CountingSelection
    val actor:        Actor             = new Actor()
    var changeEvents: Int               = 0
    actor.addListener(new ChangeListener {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
        changeEvents += 1
    })
    selection.setActor(Nullable(actor))

    def resetCounters(): Unit = {
      changeEvents = 0
      selection.changedCalls = 0
    }
  }

  // ---------------------------------------------------------------------------
  // (1) Java: `if (selected.size == 1 && selected.contains(item)) return;`
  // ---------------------------------------------------------------------------

  test("ISS-501 choose on already-selected single item fires no ChangeEvent and no changed()") {
    val h = new Harness(ctrlHeld = false)
    h.selection.choose("a")
    assertEquals(h.changeEvents, 1, "sanity: initial choose fires one event")
    h.resetCounters()
    // required=false, toggle=false, no ctrl: re-choosing the sole selected item is a no-op in Java
    h.selection.choose("a")
    assertEquals(h.selection.size, 1)
    assert(h.selection.contains(Nullable("a")))
    assertEquals(
      h.changeEvents,
      0,
      "Java early return `if (selected.size == 1 && selected.contains(item)) return;` must prevent a spurious ChangeEvent"
    )
    assertEquals(h.selection.changedCalls, 0, "changed() must not be invoked on a no-op choose")
  }

  // ---------------------------------------------------------------------------
  // (2) Java: `if (required && selected.size == 1) return;` (toggle branch)
  // ---------------------------------------------------------------------------

  test("ISS-501 toggle=true required=true choose(selected) keeps item selected and fires no event") {
    val h = new Harness(ctrlHeld = false)
    h.selection.toggle = true
    h.selection.required = true
    h.selection.choose("a")
    assertEquals(h.changeEvents, 1, "sanity: initial choose fires one event")
    h.resetCounters()
    h.selection.choose("a")
    assertEquals(h.selection.size, 1, "required: the last selected item must stay selected")
    assert(h.selection.contains(Nullable("a")))
    assertEquals(
      h.changeEvents,
      0,
      "Java early return `if (required && selected.size == 1) return;` must prevent a spurious ChangeEvent"
    )
    assertEquals(h.selection.changedCalls, 0, "changed() must not be invoked when required vetoes the deselect")
  }

  // ---------------------------------------------------------------------------
  // (3) Positive pin: a real selection change must still fire exactly one event
  // ---------------------------------------------------------------------------

  test("ISS-501 first-time choose fires exactly one ChangeEvent and one changed()") {
    val h = new Harness(ctrlHeld = false)
    h.selection.choose("a")
    assertEquals(h.selection.size, 1)
    assert(h.selection.contains(Nullable("a")))
    assertEquals(h.changeEvents, 1, "a genuine selection change must fire exactly one ChangeEvent")
    assertEquals(h.selection.changedCalls, 1, "a genuine selection change must invoke changed() exactly once")
    // Replacing the selection is also a genuine change
    h.selection.choose("b")
    assertEquals(h.changeEvents, 2)
    assertEquals(h.selection.changedCalls, 2)
  }

  // ---------------------------------------------------------------------------
  // (4) UIUtils.ctrl() interaction — headlessly controllable via custom Input
  // ---------------------------------------------------------------------------

  test("ISS-501 ctrl-click on sole selected item with required=true keeps selection and fires no event") {
    val h = new Harness(ctrlHeld = true)
    h.selection.required = true
    h.selection.choose("a")
    assertEquals(h.changeEvents, 1, "sanity: initial ctrl-choose fires one event")
    h.resetCounters()
    // ctrl held: `(toggle || UIUtils.ctrl()) && selected.contains(item)` branch;
    // Java early return `if (required && selected.size == 1) return;` applies
    h.selection.choose("a")
    assertEquals(h.selection.size, 1, "required: ctrl-click must not deselect the last item")
    assert(h.selection.contains(Nullable("a")))
    assertEquals(h.changeEvents, 0, "no ChangeEvent when required vetoes the ctrl-deselect")
    assertEquals(h.selection.changedCalls, 0, "no changed() when required vetoes the ctrl-deselect")
  }

  test("ISS-501 ctrl-click deselect without required fires exactly one event (positive pin)") {
    val h = new Harness(ctrlHeld = true)
    h.selection.choose("a")
    h.resetCounters()
    h.selection.choose("a")
    assert(h.selection.isEmpty, "ctrl-click on selected item without required deselects it")
    assertEquals(h.changeEvents, 1, "a genuine deselect must fire exactly one ChangeEvent")
    assertEquals(h.selection.changedCalls, 1)
  }
}
