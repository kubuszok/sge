/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/EventListener.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Java interface -> Scala trait (SAM-compatible)
 *   Convention: Split packages; braces on trait body
 *   Idiom: Exact 1:1 port, no behavioral changes
 *   Audited: 2026-03-03
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
