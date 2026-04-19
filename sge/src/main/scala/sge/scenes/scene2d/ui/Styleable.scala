/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Styleable.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 25
 * Covenant-baseline-methods: Styleable,setStyle,style
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/ui/Styleable.java
 * Covenant-verified: 2026-04-19
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
  def style: T

  /** Set the current style of the actor */
  def setStyle(style: T): Unit
}
