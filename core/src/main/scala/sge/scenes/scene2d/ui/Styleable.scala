/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Styleable.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

/** This interface marks an Actor as Styleable
  * @tparam T
  *   The Style object type
  */
trait Styleable[T] {

  /** Get the current style of the actor */
  def getStyle: T

  /** Set the current style of the actor */
  def setStyle(style: T): Unit
}
