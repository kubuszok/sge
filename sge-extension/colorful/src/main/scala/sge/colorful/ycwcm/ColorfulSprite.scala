/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful
package ycwcm

/** Data holder for a sprite that stores both a color and a tweak in the YCwCm color space. The color is additive (affecting Y, Cw, Cm channels) and the tweak is multiplicative (affecting Y, Cw, Cm
  * multipliers and contrast).
  *
  * This stores the color and tweak as packed float values that can be used with [[ColorfulBatch]] for rendering.
  */
final case class ColorfulSprite(
  var color: Float = Palette.GRAY,
  var tweak: Float = ColorfulBatch.TWEAK_RESET
) {

  /** Sets the additive color using Y, Cw, Cm, and alpha components. */
  def setColor(Y: Float, Cw: Float, Cm: Float, alpha: Float): Unit =
    color = ColorTools.ycwcm(Y, Cw, Cm, alpha)

  /** Sets the multiplicative tweak using Y, Cw, Cm multipliers and contrast. */
  def setTweak(Y: Float, Cw: Float, Cm: Float, contrast: Float): Unit =
    tweak = ColorTools.ycwcm(Y, Cw, Cm, contrast)

  /** Sets both color and tweak at once. */
  def setTweakedColor(color: Float, tweak: Float): Unit = {
    this.color = color
    this.tweak = tweak
  }

  /** Resets the tweak to have no effect. */
  def resetTweak(): Unit =
    tweak = ColorfulBatch.TWEAK_RESET
}
