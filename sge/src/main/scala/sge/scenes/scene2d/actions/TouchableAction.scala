/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/TouchableAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: split packages; braces on class
 *   Idiom: target.setTouchable -> target.foreach(_.touchable = ...); uninitialized Touchable field
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 32
 * Covenant-baseline-methods: TouchableAction,act,touchable
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/TouchableAction.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package scenes
package scene2d
package actions

/** Sets the actor's {@link Actor#setTouchable(Touchable) touchability}.
  * @author
  *   Nathan Sweet
  */
class TouchableAction extends Action {
  import sge.utils.Seconds

  var touchable: Touchable = scala.compiletime.uninitialized

  def act(delta: Seconds): Boolean = {
    target.foreach(_.touchable = touchable)
    true
  }
}
