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
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 34
 * Covenant-baseline-methods: EventListener,handle
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/EventListener.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
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
