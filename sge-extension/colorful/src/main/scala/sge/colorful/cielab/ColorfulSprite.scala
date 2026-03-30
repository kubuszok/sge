/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful
package cielab

/** Data holder for a sprite that stores both a color and a tweak in the CIELAB color space. The color is additive (affecting L, A, B channels) and the tweak is multiplicative (affecting L, A, B
  * multipliers and contrast).
  *
  * This stores the color and tweak as packed float values that can be used with [[ColorfulBatch]] for rendering.
  */
final case class ColorfulSprite(
  var color: Float = Palette.GRAY,
  var tweak: Float = ColorfulBatch.TWEAK_RESET
) {

  /** Sets the additive color using L, A, B, and alpha components. */
  def setColor(L: Float, A: Float, B: Float, alpha: Float): Unit =
    color = ColorTools.cielab(L, A, B, alpha)

  /** Sets the multiplicative tweak using L, A, B multipliers and contrast. */
  def setTweak(L: Float, A: Float, B: Float, contrast: Float): Unit =
    tweak = ColorTools.cielab(L, A, B, contrast)

  /** Sets both color and tweak at once. */
  def setTweakedColor(color: Float, tweak: Float): Unit = {
    this.color = color
    this.tweak = tweak
  }

  /** Resets the tweak to have no effect. */
  def resetTweak(): Unit =
    tweak = ColorfulBatch.TWEAK_RESET
}
