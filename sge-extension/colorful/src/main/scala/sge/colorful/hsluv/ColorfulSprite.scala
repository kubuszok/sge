/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful
package hsluv

/** Data holder for a sprite that stores both a color and a tweak in the HSLuv color space. The color is additive (affecting H, S, L channels) and the tweak is multiplicative.
  *
  * This stores the color and tweak as packed float values that can be used with [[ColorfulBatch]] for rendering.
  */
final case class ColorfulSprite(
  var color: Float = Palette.GRAY,
  var tweak: Float = ColorfulBatch.TWEAK_RESET
) {

  /** Sets the additive color using H, S, L, and alpha components. */
  def setColor(H: Float, S: Float, L: Float, alpha: Float): Unit =
    color = ColorTools.hsluv(H, S, L, alpha)

  /** Sets the multiplicative tweak using H, S, L multipliers and contrast. */
  def setTweak(H: Float, S: Float, L: Float, contrast: Float): Unit =
    tweak = ColorTools.hsluv(H, S, L, contrast)

  /** Sets both color and tweak at once. */
  def setTweakedColor(color: Float, tweak: Float): Unit = {
    this.color = color
    this.tweak = tweak
  }

  /** Resets the tweak to have no effect. */
  def resetTweak(): Unit =
    tweak = ColorfulBatch.TWEAK_RESET
}
