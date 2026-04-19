/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/InputEvent.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: field "type" -> "eventType" (type is reserved in Scala); inner enum Type -> companion object enum
 *   Convention: null -> Nullable[A]; no return statements; split packages; Scala 3 enum
 *   Idiom: Java static inner enum -> companion object enum; Integer.MIN_VALUE -> Int.MinValue
 *   Convention: Int key/button fields → opaque Key/Button types
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 128
 * Covenant-baseline-methods: InputEvent,Type,button,character,eventType,isTouchFocusCancel,keyCode,pointer,relatedActor,reset,scrollAmountX,scrollAmountY,stageX,stageY,toCoordinates,toString,touchFocus
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/InputEvent.java
 * Covenant-verified: 2026-04-19
 */
package sge
package scenes
package scene2d

import sge.Input.{ Button, Key }
import sge.utils.Nullable
import sge.math.Vector2

/** Event for actor input: touch, mouse, touch/mouse actor enter/exit, mouse scroll, and keyboard events.
  * @see
  *   InputListener
  */
class InputEvent extends Event {

  /** The type of input event. */
  var eventType: InputEvent.Type = scala.compiletime.uninitialized

  /** The stage x coordinate where the event occurred. Valid for: touchDown, touchDragged, touchUp, mouseMoved, enter, and exit.
    */
  var stageX: Float = 0

  /** The stage x coordinate where the event occurred. Valid for: touchDown, touchDragged, touchUp, mouseMoved, enter, and exit.
    */
  var stageY: Float = 0

  /** The amount the mouse was scrolled horizontally. Valid for: scrolled. */
  var scrollAmountX: Float = 0

  /** The amount the mouse was scrolled vertically. Valid for: scrolled. */
  var scrollAmountY: Float = 0

  /** The pointer index for the event. The first touch is index 0, second touch is index 1, etc. Always -1 on desktop. Valid for: touchDown, touchDragged, touchUp, enter, and exit.
    */
  var pointer: Int = 0

  /** The index for the mouse button pressed. Always 0 on Android. Valid for: touchDown and touchUp.
    * @see
    *   Buttons
    */
  var button: Button = Button(0)

  /** The key code of the key that was pressed. Valid for: keyDown and keyUp. */
  var keyCode: Key = Key(0)

  /** The character for the key that was typed. Valid for: keyTyped. */
  var character: Char = 0

  /** The actor related to the event. Valid for: enter and exit. For enter, this is the actor being exited, or null. For exit, this is the actor being entered, or null.
    */
  var relatedActor: Nullable[Actor] = Nullable.empty

  /** If false, {@link InputListener#handle(Event)} will not add the listener to the stage's touch focus when a touch down event is handled. Default is true.
    */
  var touchFocus: Boolean = true

  override def reset(): Unit = {
    super.reset()
    relatedActor = Nullable.empty
    button = Button(-1)
  }

  /** Sets actorCoords to this event's coordinates relative to the specified actor.
    * @param actorCoords
    *   Output for resulting coordinates.
    */
  def toCoordinates(actor: Actor, actorCoords: Vector2): Vector2 = {
    actorCoords.set(stageX, stageY)
    actor.stageToLocalCoordinates(actorCoords)
    actorCoords
  }

  /** Returns true if this event is a touchUp triggered by {@link Stage#cancelTouchFocus()}. */
  def isTouchFocusCancel: Boolean = stageX == Int.MinValue || stageY == Int.MinValue

  override def toString: String = eventType.toString
}

object InputEvent {

  /** Types of low-level input events supported by scene2d. */
  enum Type {

    /** A new touch for a pointer on the stage was detected */
    case touchDown

    /** A pointer has stopped touching the stage. */
    case touchUp

    /** A pointer that is touching the stage has moved. */
    case touchDragged

    /** The mouse pointer has moved (without a mouse button being active). */
    case mouseMoved

    /** The mouse pointer or an active touch have entered (i.e., {@link Actor#hit(float, float, boolean) hit}) an actor. */
    case enter

    /** The mouse pointer or an active touch have exited an actor. */
    case exit

    /** The mouse scroll wheel has changed. */
    case scrolled

    /** A keyboard key has been pressed. */
    case keyDown

    /** A keyboard key has been released. */
    case keyUp

    /** A keyboard key has been pressed and released. */
    case keyTyped
  }
}
