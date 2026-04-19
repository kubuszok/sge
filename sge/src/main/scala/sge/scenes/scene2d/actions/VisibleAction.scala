/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/VisibleAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; no return; split packages; braces on class
 *   Idiom: target.setVisible(visible) -> target.foreach(_.visible = visible)
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 32
 * Covenant-baseline-methods: VisibleAction,act,visible
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/VisibleAction.java
 * Covenant-verified: 2026-04-19
 */
package sge
package scenes
package scene2d
package actions

/** Sets the actor's {@link Actor#setVisible(boolean) visibility}.
  * @author
  *   Nathan Sweet
  */
class VisibleAction extends Action {
  import sge.utils.Seconds

  var visible: Boolean = false

  def act(delta: Seconds): Boolean = {
    target.foreach(_.visible = visible)
    true
  }
}
