/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/TimeScaleAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: no return; split packages; braces on class
 *   Idiom: action null-check -> action.fold(true)(_.act(delta * scale))
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.Seconds

/** Scales the delta time of an action.
  * @author
  *   Nathan Sweet
  */
class TimeScaleAction extends DelegateAction {
  var scale: Float = 0

  override protected def delegate(delta: Seconds): Boolean =
    action.forall(_.act(delta * scale))
}
