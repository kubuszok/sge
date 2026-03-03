/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/ColorAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; no return; split packages; braces on class
 *   Renames: 4 separate startR/startG/startB/startA floats -> startColor: Color object
 *   Idiom: null color fallback -> Nullable.getOrElse(target.fold(...)(_.getColor))
 *   TODO: Java-style getters/setters -- getColor/setColor, getEndColor/setEndColor
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.Nullable
import sge.graphics.Color

/** Sets the actor's color (or a specified color), from the current to the new color. Note this action transitions from the color at the time the action starts to the specified color.
  * @author
  *   Nathan Sweet
  */
class ColorAction extends TemporalAction {
  private val startColor: Color           = new Color()
  private var color:      Nullable[Color] = Nullable.empty
  private val endColor:   Color           = new Color()

  override protected def begin(): Unit = {
    val c = this.color.getOrElse(target.fold(new Color())(_.getColor))
    startColor.set(c)
  }

  override protected def update(percent: Float): Unit = {
    val c = this.color.getOrElse(target.fold(new Color())(_.getColor))
    if (percent == 0)
      c.set(startColor)
    else if (percent == 1)
      c.set(endColor)
    else {
      val r = startColor.r + (endColor.r - startColor.r) * percent
      val g = startColor.g + (endColor.g - startColor.g) * percent
      val b = startColor.b + (endColor.b - startColor.b) * percent
      val a = startColor.a + (endColor.a - startColor.a) * percent
      c.set(r, g, b, a)
    }
  }

  override def reset(): Unit = {
    super.reset()
    color = Nullable.empty
  }

  def getColor: Nullable[Color] = color

  def setColor(color: Nullable[Color]): Unit = this.color = color

  def getEndColor: Color = endColor

  def setEndColor(color: Color): Unit = endColor.set(color)
}
