/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful
package ipt_hq

/** Data holder for a sprite that stores both a color and a tweak in the IPT_HQ color space. The color is additive (affecting I, P, T channels) and the tweak is multiplicative (affecting I, P, T
  * multipliers and contrast).
  *
  * This stores the color and tweak as packed float values that can be used with [[ColorfulBatch]] for rendering.
  */
final case class ColorfulSprite(
  var color: Float = Palette.GRAY,
  var tweak: Float = ColorfulBatch.TWEAK_RESET
) {

  /** Sets the additive color using I, P, T, and alpha components. */
  def setColor(I: Float, P: Float, T: Float, alpha: Float): Unit =
    color = ColorTools.ipt(I, P, T, alpha)

  /** Sets the multiplicative tweak using I, P, T multipliers and contrast. */
  def setTweak(I: Float, P: Float, T: Float, contrast: Float): Unit =
    tweak = ColorTools.ipt(I, P, T, contrast)

  /** Sets both color and tweak at once. */
  def setTweakedColor(color: Float, tweak: Float): Unit = {
    this.color = color
    this.tweak = tweak
  }

  /** Resets the tweak to have no effect. */
  def resetTweak(): Unit =
    tweak = ColorfulBatch.TWEAK_RESET
}
