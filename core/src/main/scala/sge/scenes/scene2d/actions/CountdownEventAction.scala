/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/CountdownEventAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
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
