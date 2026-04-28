/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/LayoutAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: split packages; braces on class
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 41
 * Covenant-baseline-methods: LayoutAction,act,enabled,setTarget
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/LayoutAction.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package scenes
package scene2d
package actions

import sge.scenes.scene2d.utils.Layout
import sge.utils.{ Nullable, Seconds, SgeError }

/** Sets an actor's layout to enabled or disabled. The actor must implement Layout.
  * @author
  *   Nathan Sweet
  */
class LayoutAction extends Action {
  var enabled: Boolean = false

  override def setTarget(actor: Nullable[Actor]): Unit = {
    actor.foreach { a =>
      if (!a.isInstanceOf[Layout]) throw SgeError.InvalidInput("Actor must implement layout: " + a)
    }
    super.setTarget(actor)
  }

  def act(delta: Seconds): Boolean = {
    target.foreach { t =>
      t.asInstanceOf[Layout].layoutEnabled = enabled
    }
    true
  }
}
