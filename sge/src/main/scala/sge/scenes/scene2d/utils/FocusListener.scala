/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/FocusListener.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - FocusEvent inner class -> companion object nested class
 * - FocusEvent.Type Java enum -> Scala 3 enum
 * - @Null Actor relatedActor -> Nullable[Actor]
 * - switch/case -> Scala pattern matching
 * - All methods faithfully ported
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 75
 * Covenant-baseline-methods: FocusEvent,FocusListener,Type,focusType,focused,handle,keyboardFocusChanged,relatedActor,reset,scrollFocusChanged
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/utils/FocusListener.java
 * Covenant-verified: 2026-04-19
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
        focusEvent.focusType match {
          case FocusListener.FocusEvent.Type.keyboard =>
            event.target.foreach(t => keyboardFocusChanged(focusEvent, t, focusEvent.focused))
          case FocusListener.FocusEvent.Type.scroll =>
            event.target.foreach(t => scrollFocusChanged(focusEvent, t, focusEvent.focused))
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
    var focused:   Boolean         = false
    var focusType: FocusEvent.Type = scala.compiletime.uninitialized

    /** The actor related to the event. When focus is lost, this is the new actor being focused, or null. When focus is gained, this is the previous actor that was focused, or null.
      */
    var relatedActor: Nullable[Actor] = Nullable.empty

    override def reset(): Unit = {
      super.reset()
      relatedActor = Nullable.empty
    }
  }

  object FocusEvent {

    /** @author Nathan Sweet */
    enum Type {
      case keyboard, scroll
    }
  }
}
