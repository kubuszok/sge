/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/InputListener.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: implements -> extends; static tmpCoords -> local variable (was companion object val, shared state bug)
 *   Convention: null -> Nullable[A]; no return statements; split packages
 *   Idiom: Java switch -> Scala match; instanceof+cast -> pattern match; stage() null-check -> Nullable.foreach
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d

import sge.Input.{ Button, Key }
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
        event.eventType match {
          case InputEvent.Type.keyDown  => keyDown(event, event.keyCode)
          case InputEvent.Type.keyUp    => keyUp(event, event.keyCode)
          case InputEvent.Type.keyTyped => keyTyped(event, event.character)
          case _                        =>
            val coords = Vector2()
            event.listenerActor.foreach(la => event.toCoordinates(la, coords))

            event.eventType match {
              case InputEvent.Type.touchDown =>
                val handled = touchDown(event, coords.x, coords.y, event.pointer, event.button)
                if (handled && event.touchFocus) {
                  event.stage.foreach { stage =>
                    event.listenerActor.foreach { la =>
                      event.target.foreach { t =>
                        stage.addTouchFocus(this, la, t, event.pointer, event.button)
                      }
                    }
                  }
                }
                handled
              case InputEvent.Type.touchUp =>
                touchUp(event, coords.x, coords.y, event.pointer, event.button)
                true
              case InputEvent.Type.touchDragged =>
                touchDragged(event, coords.x, coords.y, event.pointer)
                true
              case InputEvent.Type.mouseMoved =>
                mouseMoved(event, coords.x, coords.y)
              case InputEvent.Type.scrolled =>
                scrolled(event, coords.x, coords.y, event.scrollAmountX, event.scrollAmountY)
              case InputEvent.Type.enter =>
                enter(event, coords.x, coords.y, event.pointer, event.relatedActor)
                false
              case InputEvent.Type.exit =>
                exit(event, coords.x, coords.y, event.pointer, event.relatedActor)
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
  def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Button): Boolean = false

  /** Called when a mouse button or a finger touch goes up anywhere, but only if touchDown previously returned true for the mouse button or touch. The touchUp event is always
    * {@link Event#handle() handled}.
    * @see
    *   InputEvent
    */
  def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Button): Unit = {}

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
  def keyDown(event: InputEvent, keycode: Key): Boolean = false

  /** Called when a key goes up. When true is returned, the event is {@link Event#handle() handled}. */
  def keyUp(event: InputEvent, keycode: Key): Boolean = false

  /** Called when a key is typed. When true is returned, the event is {@link Event#handle() handled}.
    * @param character
    *   May be 0 for key typed events that don't map to a character (ctrl, shift, etc).
    */
  def keyTyped(event: InputEvent, character: Char): Boolean = false
}

object InputListener {}
