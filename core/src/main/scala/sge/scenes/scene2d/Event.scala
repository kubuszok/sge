/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/Event.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
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
  private var stage:         Nullable[Stage] = Nullable.empty
  private var targetActor:   Nullable[Actor] = Nullable.empty
  private var listenerActor: Nullable[Actor] = Nullable.empty
  private var capture:       Boolean         = false // true means event occurred during the capture phase
  private var bubbles:       Boolean         = true // true means propagate to target's parents
  private var handled:       Boolean         = false // true means the event was handled (the stage will eat the input)
  private var stopped:       Boolean         = false // true means event propagation was stopped
  private var cancelled:     Boolean         = false // true means propagation was stopped and any action that this event would cause should not happen

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
    targetActor = Nullable.empty
    listenerActor = Nullable.empty
    capture = false
    bubbles = true
    handled = false
    stopped = false
    cancelled = false
  }

  /** Returns the actor that the event originated from. */
  def getTarget: Actor = targetActor.getOrElse(null)

  def setTarget(targetActor: Actor): Unit =
    this.targetActor = Nullable(targetActor)

  /** Returns the actor that this listener is attached to. */
  def getListenerActor: Actor = listenerActor.getOrElse(null)

  def setListenerActor(listenerActor: Actor): Unit =
    this.listenerActor = Nullable(listenerActor)

  def getBubbles: Boolean = bubbles

  /** If true, after the event is fired on the target actor, it will also be fired on each of the parent actors, all the way to the root.
    */
  def setBubbles(bubbles: Boolean): Unit =
    this.bubbles = bubbles

  /** {@link #handle()} */
  def isHandled: Boolean = handled

  /** @see #stop() */
  def isStopped: Boolean = stopped

  /** @see #cancel() */
  def isCancelled: Boolean = cancelled

  def setCapture(capture: Boolean): Unit =
    this.capture = capture

  /** If true, the event was fired during the capture phase.
    * @see
    *   Actor#fire(Event)
    */
  def isCapture: Boolean = capture

  def setStage(stage: Stage): Unit =
    this.stage = Nullable(stage)

  /** The stage for the actor the event was fired on. */
  def getStage: Nullable[Stage] = stage
}
