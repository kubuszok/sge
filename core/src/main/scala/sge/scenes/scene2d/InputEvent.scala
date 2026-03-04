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
 *   TODO: Java-style getters/setters — convert to var or def x/def x_= (11 pairs → vars)
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d

import sge.utils.Nullable
import sge.math.Vector2

/** Event for actor input: touch, mouse, touch/mouse actor enter/exit, mouse scroll, and keyboard events.
  * @see
  *   InputListener
  */
class InputEvent extends Event {
  private var eventType:     InputEvent.Type = scala.compiletime.uninitialized
  private var stageX:        Float           = 0
  private var stageY:        Float           = 0
  private var scrollAmountX: Float           = 0
  private var scrollAmountY: Float           = 0
  private var pointer:       Int             = 0
  private var button:        Int             = 0
  private var keyCode:       Int             = 0
  private var character:     Char            = 0
  private var relatedActor:  Nullable[Actor] = Nullable.empty
  private var touchFocus:    Boolean         = true

  override def reset(): Unit = {
    super.reset()
    relatedActor = Nullable.empty
    button = -1
  }

  /** The stage x coordinate where the event occurred. Valid for: touchDown, touchDragged, touchUp, mouseMoved, enter, and exit.
    */
  def getStageX: Float = stageX

  def setStageX(stageX: Float): Unit =
    this.stageX = stageX

  /** The stage x coordinate where the event occurred. Valid for: touchDown, touchDragged, touchUp, mouseMoved, enter, and exit.
    */
  def getStageY: Float = stageY

  def setStageY(stageY: Float): Unit =
    this.stageY = stageY

  /** The type of input event. */
  def getType: InputEvent.Type = eventType

  def setType(eventType: InputEvent.Type): Unit =
    this.eventType = eventType

  /** The pointer index for the event. The first touch is index 0, second touch is index 1, etc. Always -1 on desktop. Valid for: touchDown, touchDragged, touchUp, enter, and exit.
    */
  def getPointer: Int = pointer

  def setPointer(pointer: Int): Unit =
    this.pointer = pointer

  /** The index for the mouse button pressed. Always 0 on Android. Valid for: touchDown and touchUp.
    * @see
    *   Buttons
    */
  def getButton: Int = button

  def setButton(button: Int): Unit =
    this.button = button

  /** The key code of the key that was pressed. Valid for: keyDown and keyUp. */
  def getKeyCode: Int = keyCode

  def setKeyCode(keyCode: Int): Unit =
    this.keyCode = keyCode

  /** The character for the key that was typed. Valid for: keyTyped. */
  def getCharacter: Char = character

  def setCharacter(character: Char): Unit =
    this.character = character

  /** The amount the mouse was scrolled horizontally. Valid for: scrolled. */
  def getScrollAmountX: Float = scrollAmountX

  /** The amount the mouse was scrolled vertically. Valid for: scrolled. */
  def getScrollAmountY: Float = scrollAmountY

  def setScrollAmountX(scrollAmount: Float): Unit =
    this.scrollAmountX = scrollAmount

  def setScrollAmountY(scrollAmount: Float): Unit =
    this.scrollAmountY = scrollAmount

  /** The actor related to the event. Valid for: enter and exit. For enter, this is the actor being exited, or null. For exit, this is the actor being entered, or null.
    */
  def getRelatedActor: Nullable[Actor] = relatedActor

  /** @param relatedActor May be null. */
  def setRelatedActor(relatedActor: Nullable[Actor]): Unit =
    this.relatedActor = relatedActor

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

  /** If false, {@link InputListener#handle(Event)} will not add the listener to the stage's touch focus when a touch down event is handled. Default is true.
    */
  def getTouchFocus: Boolean = touchFocus

  def setTouchFocus(touchFocus: Boolean): Unit =
    this.touchFocus = touchFocus

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
