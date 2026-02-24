/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/EventAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.Nullable

/** Waits for an event to be fired by the target actor, then executes.
  * @author
  *   Nathan Sweet
  */
abstract class EventAction[T <: Event](val eventClass: Class[? <: T]) extends Action {
  var result: Boolean = false
  var active: Boolean = false

  private val listener: EventListener = new EventListener {
    def handle(event: Event): Boolean =
      if (!active || !eventClass.isInstance(event)) false
      else {
        result = EventAction.this.handle(eventClass.cast(event))
        result
      }
  }

  override def restart(): Unit = {
    result = false
    active = false
  }

  override def setTarget(newTarget: Nullable[Actor]): Unit = {
    target.foreach(_.removeListener(listener))
    super.setTarget(newTarget)
    newTarget.foreach(_.addListener(listener))
  }

  /** Called when the specific type of event occurs on the actor.
    * @return
    *   true if the event should be considered handled and this EventAction considered complete.
    */
  def handle(event: T): Boolean

  def act(delta: Float): Boolean = {
    active = true
    result
  }

  def isActive: Boolean = active

  def setActive(active: Boolean): Unit = this.active = active
}
