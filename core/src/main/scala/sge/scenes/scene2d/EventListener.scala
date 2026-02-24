/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/EventListener.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d

/** Low level interface for receiving events. Typically there is a listener class for each specific event class.
  * @see
  *   InputListener
  * @see
  *   InputEvent
  * @author
  *   Nathan Sweet
  */
trait EventListener {

  /** Try to handle the given event, if it is applicable.
    * @return
    *   true if the event should be considered {@link Event#handle() handled} by scene2d.
    */
  def handle(event: Event): Boolean
}
