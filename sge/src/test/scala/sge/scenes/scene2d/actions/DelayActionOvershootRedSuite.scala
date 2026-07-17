/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package scenes
package scene2d
package actions

import lowlevel.Nullable
import sge.utils.Seconds

/** Red test for ISS-776 (DelayAction facet): the completing frame clamps `time` to `duration`, discarding the overshoot that the original preserves.
  *
  * Original (com/badlogic/gdx/scenes/scene2d/actions/DelayAction.java:31-39):
  * {{{
  *   protected boolean delegate (float delta) {
  *     if (time < duration) {
  *       time += delta;                     // time can exceed duration...
  *       if (time < duration) return false;
  *       delta = time - duration;           // ...and is LEFT overshot; only the forwarded delta is trimmed
  *     }
  *     if (action == null) return true;
  *     return action.act(delta);
  *   }
  *   public float getTime () { return time; }
  * }}}
  *
  * On the completing frame Java advances `time` past `duration` (e.g. time = 1.5 for a 1.0 duration) and only the delta forwarded to the wrapped action is trimmed to the overshoot; `time` itself
  * stays at 1.5, so getTime() reports the overshoot.
  *
  * The port (DelayAction.scala:44-46) executes `time = duration`, clamping `time` to exactly the duration and losing the overshoot. This pins the Java behavior; it fails on the port until the clamp
  * is removed.
  */
class DelayActionOvershootRedSuite extends munit.FunSuite {

  test("ISS-776 completing frame preserves the overshoot in time (no clamp to duration)") {
    val delay = DelayAction(Seconds(1f))
    // No wrapped action: delegate falls through to `action.forall(...)` == true, isolating the time math.
    assert(delay.action.isEmpty, "sanity: no wrapped action")

    // One frame overshoots the delay by 0.5s.
    val done = delay.act(Seconds(1.5f))
    assert(done, "the delay is complete after the overshooting frame")

    assertEquals(
      delay.time.toFloat,
      1.5f,
      "ISS-776: Java leaves time at the overshot value (1.5); the port clamps it to duration (1.0)"
    )
  }
}
