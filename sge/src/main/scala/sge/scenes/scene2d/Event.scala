/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/Event.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Pool.Poolable (Java interface) -> Pool.Poolable (Scala trait)
 *   Convention: null -> Nullable[A]; no return statements; split packages
 *   Idiom: Fields stored as Nullable; target/listenerActor return Nullable directly
 *   Convention: extends Pool.Poolable — given Poolable[Event] auto-derived via Poolable.fromTrait
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 96
 * Covenant-baseline-methods: Event,bubbles,cancel,cancelled,capture,handle,handled,isCancelled,isHandled,isStopped,listenerActor,reset,stage,stop,stopped,target
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/Event.java
 * Covenant-verified: 2026-04-19
 */
package sge
package scenes
package scene2d

import sge.utils.Nullable
import sge.utils.Pool

/** The base class for all events. <p> By default an event will "bubble" up through an actor's parent's handlers (see {@link #setBubbles(boolean)}). <p> An actor's capture listeners can
  * {@link #stop()} an event to prevent child actors from seeing it. <p> An Event may be marked as "handled" which will end its propagation outside of the Stage (see {@link #handle()}). The default
  * {@link Actor#fire(Event)} will mark events handled if an {@link EventListener} returns true. <p> A cancelled event will be stopped and handled. Additionally, many actors will undo the side-effects
  * of a canceled event. (See {@link #cancel()}.)
  *
  * @see
  *   InputEvent
  * @see
  *   Actor#fire(Event)
  */
class Event extends Pool.Poolable {

  /** The stage for the actor the event was fired on. */
  var stage: Nullable[Stage] = Nullable.empty

  /** Returns the actor that the event originated from. */
  var target: Nullable[Actor] = Nullable.empty

  /** Returns the actor that this listener is attached to. */
  var listenerActor: Nullable[Actor] = Nullable.empty

  /** If true, the event was fired during the capture phase.
    * @see
    *   Actor#fire(Event)
    */
  var capture: Boolean = false // true means event occurred during the capture phase
  /** If true, after the event is fired on the target actor, it will also be fired on each of the parent actors, all the way to the root.
    */
  var bubbles:           Boolean = true // true means propagate to target's parents
  private var handled:   Boolean = false // true means the event was handled (the stage will eat the input)
  private var stopped:   Boolean = false // true means event propagation was stopped
  private var cancelled: Boolean = false // true means propagation was stopped and any action that this event would cause should not happen

  /** Marks this event as handled. This does not affect event propagation inside scene2d, but causes the {@link Stage} {@link InputProcessor} methods to return true, which will eat the event so it is
    * not passed on to the application under the stage.
    */
  def handle(): Unit =
    handled = true

  /** Marks this event cancelled. This {@link #handle() handles} the event and {@link #stop() stops} the event propagation. It also cancels any default action that would have been taken by the code
    * that fired the event. Eg, if the event is for a checkbox being checked, cancelling the event could uncheck the checkbox.
    */
  def cancel(): Unit = {
    cancelled = true
    stopped = true
    handled = true
  }

  /** Marks this event has being stopped. This halts event propagation. Any other listeners on the {@link #getListenerActor() listener actor} are notified, but after that no other listeners are
    * notified.
    */
  def stop(): Unit =
    stopped = true

  def reset(): Unit = {
    stage = Nullable.empty
    target = Nullable.empty
    listenerActor = Nullable.empty
    capture = false
    bubbles = true
    handled = false
    stopped = false
    cancelled = false
  }

  /** {@link #handle()} */
  def isHandled: Boolean = handled

  /** @see #stop() */
  def isStopped: Boolean = stopped

  /** @see #cancel() */
  def isCancelled: Boolean = cancelled
}
