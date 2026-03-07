/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/AddListenerAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; no return; split packages; braces on class
 *   Idiom: target null-access -> target.foreach + listener.foreach
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.{ Nullable, Seconds }

/** Adds a listener to an actor.
  * @author
  *   Nathan Sweet
  */
class AddListenerAction extends Action {
  var listener: Nullable[EventListener] = Nullable.empty
  var capture:  Boolean                 = false

  def act(delta: Seconds): Boolean = {
    target.foreach { t =>
      listener.foreach { l =>
        if (capture) t.addCaptureListener(l)
        else t.addListener(l)
      }
    }
    true
  }

  override def reset(): Unit = {
    super.reset()
    listener = Nullable.empty
  }
}
