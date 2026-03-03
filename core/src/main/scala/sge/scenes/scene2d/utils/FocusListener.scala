/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/FocusListener.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - FocusEvent inner class -> companion object nested class
 * - FocusEvent.Type Java enum -> Scala 3 enum
 * - @Null Actor relatedActor -> Nullable[Actor]
 * - switch/case -> Scala pattern matching
 * - All methods faithfully ported
 * - TODO: Java-style getters/setters — FocusEvent.isFocused
 */
package sge
package scenes
package scene2d
package utils

import sge.utils.Nullable

/** Listener for {@link FocusEvent}.
  * @author
  *   Nathan Sweet
  */
abstract class FocusListener extends EventListener {
  def handle(event: Event): Boolean =
    event match {
      case focusEvent: FocusListener.FocusEvent =>
        focusEvent.getType match {
          case FocusListener.FocusEvent.Type.keyboard =>
            keyboardFocusChanged(focusEvent, event.getTarget, focusEvent.isFocused)
          case FocusListener.FocusEvent.Type.scroll =>
            scrollFocusChanged(focusEvent, event.getTarget, focusEvent.isFocused)
        }
        false
      case _ => false
    }

  /** @param actor The event target, which is the actor that emitted the focus event. */
  def keyboardFocusChanged(event: FocusListener.FocusEvent, actor: Actor, focused: Boolean): Unit = {}

  /** @param actor The event target, which is the actor that emitted the focus event. */
  def scrollFocusChanged(event: FocusListener.FocusEvent, actor: Actor, focused: Boolean): Unit = {}
}

object FocusListener {

  /** Fired when an actor gains or loses keyboard or scroll focus. Can be cancelled to prevent losing or gaining focus.
    * @author
    *   Nathan Sweet
    */
  class FocusEvent extends Event {
    private var focused:      Boolean         = false
    private var focusType:    FocusEvent.Type = scala.compiletime.uninitialized
    private var relatedActor: Nullable[Actor] = Nullable.empty

    override def reset(): Unit = {
      super.reset()
      relatedActor = Nullable.empty
    }

    def isFocused: Boolean = focused

    def setFocused(focused: Boolean): Unit =
      this.focused = focused

    def getType: FocusEvent.Type = focusType

    def setType(focusType: FocusEvent.Type): Unit =
      this.focusType = focusType

    /** The actor related to the event. When focus is lost, this is the new actor being focused, or null. When focus is gained, this is the previous actor that was focused, or null.
      */
    def getRelatedActor: Nullable[Actor] = relatedActor

    /** @param relatedActor May be null. */
    def setRelatedActor(relatedActor: Nullable[Actor]): Unit =
      this.relatedActor = relatedActor
  }

  object FocusEvent {

    /** @author Nathan Sweet */
    enum Type {
      case keyboard, scroll
    }
  }
}
