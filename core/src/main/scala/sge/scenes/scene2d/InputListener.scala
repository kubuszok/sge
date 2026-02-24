/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/InputListener.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d

import sge.utils.Nullable
import sge.math.Vector2

/** EventListener for low-level input events. Unpacks {@link InputEvent}s and calls the appropriate method. By default the methods here do nothing with the event. Users are expected to override the
  * methods they are interested in, like this:
  *
  * <pre> actor.addListener(new InputListener() { public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) { Gdx.app.log(&quot;Example&quot;, &quot;touch started at
  * (&quot; + x + &quot;, &quot; + y + &quot;)&quot;); return false; }
  *
  * public void touchUp (InputEvent event, float x, float y, int pointer, int button) { Gdx.app.log(&quot;Example&quot;, &quot;touch done at (&quot; + x + &quot;, &quot; + y + &quot;)&quot;); } });
  * </pre>
  */
class InputListener extends EventListener {

  /** Try to handle the given event, if it is an {@link InputEvent}. <p> If the input event is of type {@link InputEvent.Type#touchDown} and {@link InputEvent#getTouchFocus()} is true and
    * {@link #touchDown(InputEvent, float, float, int, int)} returns true (indicating the event was handled) then this listener is added to the stage's
    * {@link Stage#addTouchFocus(EventListener, Actor, Actor, int, int) touch focus} so it will receive all touch dragged events until the next touch up event.
    */
  def handle(e: Event): Boolean =
    e match {
      case event: InputEvent =>
        event.getType match {
          case InputEvent.Type.keyDown  => keyDown(event, event.getKeyCode)
          case InputEvent.Type.keyUp    => keyUp(event, event.getKeyCode)
          case InputEvent.Type.keyTyped => keyTyped(event, event.getCharacter)
          case _                        =>
            event.toCoordinates(event.getListenerActor, InputListener.tmpCoords)

            event.getType match {
              case InputEvent.Type.touchDown =>
                val handled = touchDown(event, InputListener.tmpCoords.x, InputListener.tmpCoords.y, event.getPointer, event.getButton)
                if (handled && event.getTouchFocus) {
                  event.getStage.foreach { stage =>
                    stage.addTouchFocus(this, event.getListenerActor, event.getTarget, event.getPointer, event.getButton)
                  }
                }
                handled
              case InputEvent.Type.touchUp =>
                touchUp(event, InputListener.tmpCoords.x, InputListener.tmpCoords.y, event.getPointer, event.getButton)
                true
              case InputEvent.Type.touchDragged =>
                touchDragged(event, InputListener.tmpCoords.x, InputListener.tmpCoords.y, event.getPointer)
                true
              case InputEvent.Type.mouseMoved =>
                mouseMoved(event, InputListener.tmpCoords.x, InputListener.tmpCoords.y)
              case InputEvent.Type.scrolled =>
                scrolled(event, InputListener.tmpCoords.x, InputListener.tmpCoords.y, event.getScrollAmountX, event.getScrollAmountY)
              case InputEvent.Type.enter =>
                enter(event, InputListener.tmpCoords.x, InputListener.tmpCoords.y, event.getPointer, event.getRelatedActor)
                false
              case InputEvent.Type.exit =>
                exit(event, InputListener.tmpCoords.x, InputListener.tmpCoords.y, event.getPointer, event.getRelatedActor)
                false
              case _ => false
            }
        }
      case _ => false
    }

  /** Called when a mouse button or a finger touch goes down on the actor. If true is returned, this listener will have {@link Stage#addTouchFocus(EventListener, Actor, Actor, int, int) touch focus},
    * so it will receive all touchDragged and touchUp events, even those not over this actor, until touchUp is received. Also when true is returned, the event is {@link Event#handle() handled}.
    * @see
    *   InputEvent
    */
  def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean = false

  /** Called when a mouse button or a finger touch goes up anywhere, but only if touchDown previously returned true for the mouse button or touch. The touchUp event is always
    * {@link Event#handle() handled}.
    * @see
    *   InputEvent
    */
  def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Unit = {}

  /** Called when a mouse button or a finger touch is moved anywhere, but only if touchDown previously returned true for the mouse button or touch. The touchDragged event is always
    * {@link Event#handle() handled}.
    * @see
    *   InputEvent
    */
  def touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int): Unit = {}

  /** Called any time the mouse is moved when a button is not down. This event only occurs on the desktop. When true is returned, the event is {@link Event#handle() handled}.
    * @see
    *   InputEvent
    */
  def mouseMoved(event: InputEvent, x: Float, y: Float): Boolean = false

  /** Called any time the mouse cursor or a finger touch is moved over an actor. On the desktop, this event occurs even when no mouse buttons are pressed (pointer will be -1).
    * @param fromActor
    *   May be null.
    * @see
    *   InputEvent
    */
  def enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Nullable[Actor]): Unit = {}

  /** Called any time the mouse cursor or a finger touch is moved out of an actor. On the desktop, this event occurs even when no mouse buttons are pressed (pointer will be -1).
    * @param toActor
    *   May be null.
    * @see
    *   InputEvent
    */
  def exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Nullable[Actor]): Unit = {}

  /** Called when the mouse wheel has been scrolled. When true is returned, the event is {@link Event#handle() handled}. */
  def scrolled(event: InputEvent, x: Float, y: Float, amountX: Float, amountY: Float): Boolean = false

  /** Called when a key goes down. When true is returned, the event is {@link Event#handle() handled}. */
  def keyDown(event: InputEvent, keycode: Int): Boolean = false

  /** Called when a key goes up. When true is returned, the event is {@link Event#handle() handled}. */
  def keyUp(event: InputEvent, keycode: Int): Boolean = false

  /** Called when a key is typed. When true is returned, the event is {@link Event#handle() handled}.
    * @param character
    *   May be 0 for key typed events that don't map to a character (ctrl, shift, etc).
    */
  def keyTyped(event: InputEvent, character: Char): Boolean = false
}

object InputListener {
  private val tmpCoords: Vector2 = new Vector2()
}
