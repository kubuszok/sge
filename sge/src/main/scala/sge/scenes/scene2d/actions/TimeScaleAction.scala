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
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 30
 * Covenant-baseline-methods: TimeScaleAction,delegate,scale
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/TimeScaleAction.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 286f79efa802dd1fbc8deaa073b73230c0321447
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
