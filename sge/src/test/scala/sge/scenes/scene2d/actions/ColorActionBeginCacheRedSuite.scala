/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package scenes
package scene2d
package actions

import lowlevel.Nullable
import sge.graphics.Color
import sge.utils.Seconds

/** Red test for ISS-776: ColorAction.begin does not cache the resolved color into the `color` field.
  *
  * Original (com/badlogic/gdx/scenes/scene2d/actions/ColorAction.java:31-37):
  * {{{
  *   protected void begin () {
  *     if (color == null) color = target.getColor();   // caches resolved color into the field
  *     startR = color.r; ...
  *   }
  *   public @Null Color getColor () { return color; }
  * }}}
  *
  * When `color` is left unset (null), Java's begin() resolves it to the target actor's color AND writes that instance back into the `color` field, so getColor() returns the actor's color for the
  * remainder of the action's run.
  *
  * The port (ColorAction.scala:41-44) resolves the color into a LOCAL `val c` and never writes it back to `this.color`, so the public `color` getter stays empty while the action runs. This pins the
  * Java behavior; it fails on the port until begin() assigns the resolved color to the field.
  */
class ColorActionBeginCacheRedSuite extends munit.FunSuite {

  private def ctx(): Sge = SgeTestFixture.testSge()

  test("ISS-776 begin() caches the target actor's color into the color field") {
    given Sge = ctx()
    val actor = Actor()
    actor.color.set(0.25f, 0.5f, 0.75f, 1f)

    val action = ColorAction()
    action.setEndColor(Color(1, 1, 1, 1))
    action.duration = Seconds(1f)
    action.setTarget(Nullable(actor))
    // color intentionally left unset (Nullable.empty)
    assert(action.color.isEmpty, "sanity: color starts unset")

    // First act() triggers begin() (delta 0 keeps percent at 0, no completion for a 1s duration).
    action.act(Seconds.zero)

    assert(
      action.color.isDefined,
      "ISS-776: begin() must cache the resolved color into the field so getColor() is populated during the run"
    )
    assert(
      action.color.exists(_ eq actor.color),
      "ISS-776: the cached color must be the target actor's color instance (Java `color = target.getColor()`)"
    )
  }
}
