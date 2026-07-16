/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red test for ISS-775 (Tooltip.mouseMoved drops the original's
 * getActions().size == 0 guard).
 *
 * Original semantics (original-src/vis-ui/ui/src/main/java/com/kotcrab/vis/ui/
 * widget/Tooltip.java line 297, upstream commit
 * 820300c86a1bd907404217195a9987e5c66d2220 — the commit every visui header
 * references):
 *
 *   public boolean mouseMoved (InputEvent event, float x, float y) {
 *       if (mouseMoveFadeOut && isVisible() && getActions().size == 0) fadeOut();
 *       return false;
 *   }
 *
 * The `getActions().size == 0` clause means: only START a fade-out when no
 * action is currently running. Once a fade-out sequence is installed, further
 * mouse-move events must leave it running untouched so it can finish (and the
 * trailing removeActor() actually removes the tooltip).
 *
 * The port (sge-extension/visui/src/main/scala/sge/visui/widget/Tooltip.scala
 * line 248) reads:
 *
 *   if (tooltip._mouseMoveFadeOut && tooltip.visible) tooltip.doFadeOut()
 *
 * dropping the guard. doFadeOut() clears all actions and installs a fresh
 * fade-out sequence, so with MOUSE_MOVED_FADEOUT enabled every single
 * mouse-move restarts the fade from full opacity — the tooltip never finishes
 * fading and is never removed.
 *
 * Observable consequence (pool-independent): the fade-out sequence installed by
 * doFadeOut() ends in removeActor(), so once the fade finishes the tooltip is
 * removed from its parent. With the guard in place, a running fade keeps
 * accumulating time across mouse-moves and eventually completes, removing the
 * tooltip. Without the guard, every mouse-move calls doFadeOut() again, which
 * clearActions() the in-progress fade and installs a fresh one from time 0, so
 * the accumulated fade time never reaches fadeTime and the tooltip is never
 * removed. (An action-identity assertion is NOT usable: clearActions() frees
 * the sequence back to the action Pool and doFadeOut()'s Actions.sequence(...)
 * re-obtains the very same instance, so identity is preserved even under the
 * bug.)
 *
 * Harness: a real (headless) loaded VisUI skin (VisUITestFixture.headlessSge)
 * so VisTable's `Table(VisUI.getSkin)` base ctor resolves. The tooltip is
 * placed in a parent Group and driven with tooltip.act(dt) to advance the fade,
 * interleaved with mouse-moves. The tooltip's private TooltipInputListener is
 * the sole listener attach() adds to the target; it is driven via its public
 * InputListener.mouseMoved entry point, exactly the dispatch Stage performs.
 *
 * This test is written by the reproducer agent and MUST NOT be modified by the
 * fixer: it encodes the original VisUI semantics, not the port's.
 */
package sge
package visui
package widget

import sge.scenes.scene2d.{ Actor, Group, InputEvent, InputListener }
import sge.utils.Seconds

class TooltipMouseMovedFadeOutRedSuite extends munit.FunSuite {

  override def afterEach(context: AfterEach): Unit = VisUI.dispose()

  test("ISS-775: periodic mouseMoved must not restart the fade-out, so the tooltip still completes fading and is removed") {
    given Sge = VisUITestFixture.headlessSge()
    VisUI.load()

    val tooltip = new Tooltip(new Tooltip.TooltipStyle)
    // MOUSE_MOVED_FADEOUT is a static default copied into the instance flag at
    // construction; enable the instance flag directly so the test does not
    // mutate global static state.
    tooltip.mouseMoveFadeOut = true
    // Actor.visible defaults to true, matching isVisible() in Tooltip.java:297.

    // A parent so the trailing removeActor() has something to remove.
    val parent = new Group()
    parent.addActor(tooltip)

    val target = new Actor()
    tooltip.target = target

    assert(target.listeners.size >= 1, "attach() must add the tooltip's TooltipInputListener to the target (Tooltip.scala:108)")
    val listener = target.listeners(target.listeners.size - 1).asInstanceOf[InputListener]

    def move(): Unit = { val _ = listener.mouseMoved(new InputEvent(), 0f, 0f) }

    // First move installs the fade-out sequence (no action running yet).
    move()
    assertEquals(
      tooltip.actions.size,
      1,
      "first mouseMoved with the flag enabled and no running action must start the fade-out (Tooltip.java:297)"
    )

    // Advance time in fadeTime/2-sized steps, nudging the mouse before each step
    // (as a hovering cursor would). fadeTime defaults to 0.3s, so ~0.9s of total
    // act() budget is far more than enough for a single uninterrupted fade to
    // finish — but under the bug each move restarts it from 0.
    val dt = Seconds(tooltip.fadeTime.toFloat / 2f)
    var i  = 0
    while (i < 6 && tooltip.hasParent) {
      move()
      tooltip.act(dt)
      i += 1
    }

    assert(
      !tooltip.hasParent,
      "with periodic mouse-moves the fade-out must still accumulate time and complete, letting removeActor() remove the tooltip; " +
        "dropping the getActions().size == 0 guard (Tooltip.scala:248) restarts the fade on every move so it never finishes and the tooltip is never removed"
    )
  }
}
