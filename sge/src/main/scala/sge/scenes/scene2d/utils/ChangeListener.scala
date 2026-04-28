/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/ChangeListener.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Faithful port, no API changes
 * - ChangeEvent inner class -> companion object nested class
 * - instanceof check -> Scala pattern matching
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 43
 * Covenant-baseline-methods: ChangeEvent,ChangeListener,changed,handle
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/utils/ChangeListener.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package scenes
package scene2d
package utils

/** Listener for {@link ChangeEvent}.
  * @author
  *   Nathan Sweet
  */
abstract class ChangeListener extends EventListener {
  override def handle(event: Event): Boolean =
    event match {
      case ce: ChangeListener.ChangeEvent =>
        event.target.foreach(t => changed(ce, t))
        false
      case _ => false
    }

  /** @param actor The event target, which is the actor that emitted the change event. */
  def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit
}

object ChangeListener {

  /** Fired when something in an actor has changed. This is a generic event, exactly what changed in an actor will vary.
    * @author
    *   Nathan Sweet
    */
  class ChangeEvent extends Event {}
}
