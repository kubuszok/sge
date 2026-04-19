/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/RemoveListenerAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; no return; split packages; braces on class
 *   Idiom: target null-access -> target.foreach + listener.foreach
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 43
 * Covenant-baseline-methods: RemoveListenerAction,act,capture,listener,reset
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/RemoveListenerAction.java
 * Covenant-verified: 2026-04-19
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.{ Nullable, Seconds }

/** Removes a listener from an actor.
  * @author
  *   Nathan Sweet
  */
class RemoveListenerAction extends Action {
  var listener: Nullable[EventListener] = Nullable.empty
  var capture:  Boolean                 = false

  def act(delta: Seconds): Boolean = {
    target.foreach { t =>
      listener.foreach { l =>
        if (capture) t.removeCaptureListener(l)
        else t.removeListener(l)
      }
    }
    true
  }

  override def reset(): Unit = {
    super.reset()
    listener = Nullable.empty
  }
}
