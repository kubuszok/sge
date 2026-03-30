/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful
package rgb

/** Data holder for a sprite that stores both a color and a tweak in the RGB color space. The color is additive (affecting R, G, B channels) and the tweak is multiplicative (affecting R, G, B
  * multipliers and contrast).
  */
final case class ColorfulSprite(
  var color: Float = ColorTools.rgb(0.5f, 0.5f, 0.5f, 1f),
  var tweak: Float = ColorfulBatch.TWEAK_RESET
) {

  /** Sets the additive color using R, G, B, and alpha components. */
  def setColor(r: Float, g: Float, b: Float, alpha: Float): Unit =
    color = ColorTools.rgb(r, g, b, alpha)

  /** Sets the multiplicative tweak using R, G, B multipliers and contrast. */
  def setTweak(r: Float, g: Float, b: Float, contrast: Float): Unit =
    tweak = ColorTools.rgb(r, g, b, contrast)

  /** Sets both color and tweak at once. */
  def setTweakedColor(color: Float, tweak: Float): Unit = {
    this.color = color
    this.tweak = tweak
  }

  /** Resets the tweak to have no effect. */
  def resetTweak(): Unit =
    tweak = ColorfulBatch.TWEAK_RESET
}
