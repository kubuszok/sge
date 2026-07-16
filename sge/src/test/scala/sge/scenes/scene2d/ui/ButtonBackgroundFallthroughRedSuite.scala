/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package scenes
package scene2d
package ui

import sge.scenes.scene2d.utils.{ BaseDrawable, Drawable }
import lowlevel.Nullable

/** Red tests for ISS-758: Button.backgroundDrawable (Button.scala lines 152-196) lost the original fall-through cascade.
  *
  * Original com/badlogic/gdx/scenes/scene2d/ui/Button.java (original-src/libgdx, commit a729bf1f0de099ebcc60562d72f008157677b559), getBackgroundDrawable lines 189-210 is a CASCADE of independent
  * guarded returns: the pressed block (191-194), the over block (195-201), the checked/focused block (202-208) and the final `return style.up` (209) each FALL THROUGH to the next when their specific
  * drawable is null. In particular for a CHECKED + OVER button with no checkedOver drawable, line 197 does not return, so control falls to line 203-206: checkedFocused -> checked -> over -> up. The
  * port instead terminates the over-and-checked branch with `else _style.up` (Button.scala lines 158-160 and 178-180), skipping the checked/over/focused fallbacks entirely (the focused fallback of
  * Java line 208 is skipped by the same lost fall-through; it needs Stage keyboard focus and is not reproducible headless, so these tests pin the cascade through the over-state variants).
  * TextButton.scala lines 76-97 shows the correct boundary/break spelling of the same cascade.
  *
  * Mouse-over is simulated the way Stage delivers it: ClickListener.enter with pointer == -1 (ClickListener.java enter; port ClickListener.scala line 101-102 sets _over = true).
  *
  * These tests are written by the reproducer agent and MUST NOT be modified by the fixer: they encode the original Java semantics, not the port's.
  */
class ButtonBackgroundFallthroughRedSuite extends munit.FunSuite {

  private given Sge = SgeTestFixture.testSge()

  /** Exposes the protected backgroundDrawable state machine under test. */
  final private class ProbeButton(style: Button.ButtonStyle)(using Sge) extends Button(style) {
    def currentBackground: Nullable[Drawable] = backgroundDrawable
  }

  private def named(n: String): Nullable[Drawable] = {
    val d = new BaseDrawable()
    d.name = Nullable(n)
    Nullable(d)
  }

  private def nameOf(bg: Nullable[Drawable]): String =
    bg.fold("<none>") {
      case b: BaseDrawable => b.name.getOrElse("<unnamed>")
      case _ => "<not-a-BaseDrawable>"
    }

  /** Sets the over state exactly like Stage's mouseMoved dispatch: enter with pointer -1. */
  private def simulateMouseOver(btn: Button): Unit =
    btn.clickListener.enter(new InputEvent(), 0f, 0f, -1, Nullable.empty[Actor])

  private def checkedOverButton(style: Button.ButtonStyle): ProbeButton = {
    val btn = new ProbeButton(style)
    btn.programmaticChangeEvents = false
    btn.setChecked(true)
    simulateMouseOver(btn)
    assert(btn.isChecked, "precondition: the button is checked")
    assert(btn.isOver, "precondition: the button is hovered (ClickListener.enter with pointer -1)")
    assert(!btn.isPressed, "precondition: the button is not pressed")
    btn
  }

  test("ISS-758: checked + over without checkedOver falls through to style.checked") {
    // Java: over block (lines 195-201) -> checkedOver == null, NO return ->
    // checked block (lines 203-206) -> style.checked != null -> return
    // style.checked. The port returns `up` from the over-and-checked branch
    // (Button.scala lines 158-160) instead of falling through.
    val style = new Button.ButtonStyle(named("up"), Nullable.empty, named("checked"))
    val btn   = checkedOverButton(style)
    assertEquals(
      nameOf(btn.currentBackground),
      "checked",
      "checked+over with no checkedOver must fall through to style.checked (Button.java lines 197 -> 205), not stop at style.up"
    )
  }

  test("ISS-758: checked + over prefers style.checked over the generic over drawable") {
    // Java: the over block's checked arm (line 197) only considers
    // checkedOver — style.over is for UNchecked buttons (line 199). With
    // checkedOver null control falls to line 205 and returns style.checked.
    // The port again returns `up`.
    val style = new Button.ButtonStyle(named("up"), Nullable.empty, named("checked"))
    style.over = named("over")
    val btn = checkedOverButton(style)
    assertEquals(
      nameOf(btn.currentBackground),
      "checked",
      "checked+over with style.over but no checkedOver must still return style.checked (Button.java line 205)"
    )
  }

  test("ISS-758: checked + over without checked drawable falls back to style.over") {
    // Java: over block -> checkedOver == null -> fall through; checked block
    // -> checkedFocused/checked == null -> line 206 `if (isOver() &&
    // style.over != null) return style.over`. The port returns `up`.
    val style = new Button.ButtonStyle(named("up"), Nullable.empty, Nullable.empty)
    style.over = named("over")
    val btn = checkedOverButton(style)
    assertEquals(
      nameOf(btn.currentBackground),
      "over",
      "checked+over with no checked/checkedOver drawable must fall back to style.over (Button.java line 206)"
    )
  }
}
