/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/RemoveListenerAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; no return; split packages; braces on class
 *   Idiom: target null-access -> target.foreach + listener.foreach
 *   TODO: Java-style getters/setters -- setListener, setCapture
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.Nullable

/** Removes a listener from an actor.
  * @author
  *   Nathan Sweet
  */
class RemoveListenerAction extends Action {
  private var listener: Nullable[EventListener] = Nullable.empty
  private var capture:  Boolean                 = false

  def act(delta: Float): Boolean = {
    target.foreach { t =>
      listener.foreach { l =>
        if (capture) t.removeCaptureListener(l)
        else t.removeListener(l)
      }
    }
    true
  }

  def getListener: Nullable[EventListener] = listener

  def setListener(listener: EventListener): Unit = this.listener = Nullable(listener)

  def getCapture: Boolean = capture

  def setCapture(capture: Boolean): Unit = this.capture = capture

  override def reset(): Unit = {
    super.reset()
    listener = Nullable.empty
  }
}
