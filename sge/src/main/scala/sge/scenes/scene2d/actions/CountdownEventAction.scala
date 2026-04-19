/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/CountdownEventAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: split packages; braces on class
 *   Renames: package-private count field -> val count (constructor param)
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 30
 * Covenant-baseline-methods: CountdownEventAction,current,handle
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/CountdownEventAction.java
 * Covenant-verified: 2026-04-19
 */
package sge
package scenes
package scene2d
package actions

/** An EventAction that is complete once it receives a number of events.
  * @author
  *   Nathan Sweet
  */
class CountdownEventAction[T <: Event](eventClass: Class[? <: T], val count: Int) extends EventAction[T](eventClass) {
  private var current: Int = 0

  def handle(event: T): Boolean = {
    current += 1
    current >= count
  }
}
