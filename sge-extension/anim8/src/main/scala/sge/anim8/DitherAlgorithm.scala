/*
 * Ported from anim8-gdx - https://github.com/tommyettinger/anim8-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 104
 * Covenant-baseline-methods: ALL,DitherAlgorithm,toString
 * Covenant-source-reference: com/github/tommyettinger/anim8/Dithered.java
 * Covenant-verified: 2026-04-19
 */
package sge
package anim8

/** Represents a choice of dithering algorithm to apply when writing a high-color image with a color-limited format. There are a wide variety of options here, all of which offer different tradeoffs
  * for which artifacts they tend to show, and when they show them most often. In general, the highest quality images come from [[PATTERN]] for both animations and many still images, but it is
  * considerably slower than other algorithms here, and it has grid artifacts that can be very noticeable in small palettes. Otherwise, for still images, error diffusion dithers have the highest
  * quality in terms of hue and lightness reproduction, but the artifacts they have can be much more noticeable in animations. For GIFs and animated PNGs (with limited palettes), an ordered dither
  * tends to have slightly worse artifacts, but they don't move between frames, and the movement is what looks especially bad for error diffusion dithers in animations.
  *
  * There are a lot of algorithms here. In most cases, the default [[WREN]] dither, which is an error diffusion type, is a good choice for still images, but if you're handling animations, you'll
  * typically want an ordered dither instead.
  */
enum DitherAlgorithm(val legibleName: String) extends java.lang.Enum[DitherAlgorithm] {

  /** Doesn't dither at all; this generally looks bad unless the palette matches the colors in the image very closely or exactly.
    */
  case NONE extends DitherAlgorithm("None")

  /** Jorge Jimenez' Gradient Interleaved Noise, modified slightly to use as an ordered dither here. */
  case GRADIENT_NOISE extends DitherAlgorithm("GradientNoise")

  /** Thomas Knoll's Pattern Dither (with a 4x4 matrix). */
  case PATTERN extends DitherAlgorithm("Pattern")

  /** Floyd-Steinberg error-diffusion dithering. */
  case DIFFUSION extends DitherAlgorithm("Diffusion")

  /** Ordered dither using a blue-noise pattern that affects lightness. */
  case BLUE_NOISE extends DitherAlgorithm("BlueNoise")

  /** Very similar to [[BLUE_NOISE]] for a still frame, but in an animation this will change wildly from frame to frame.
    */
  case CHAOTIC_NOISE extends DitherAlgorithm("ChaoticNoise")

  /** Subtly alters the error-diffusion dither of [[DIFFUSION]] with a small amount of triangular-distributed blue noise.
    */
  case SCATTER extends DitherAlgorithm("Scatter")

  /** An error diffusion dither that mixes in ordered noise from a triangular-mapped blue noise texture. */
  case NEUE extends DitherAlgorithm("Neue")

  /** An ordered dither built around the lightness-dispersing R2 point sequence, by Martin Roberts. */
  case ROBERTS extends DitherAlgorithm("Roberts")

  /** An error-diffusion dither much like [[NEUE]], except that it adds or subtracts a different error value from each RGB channel, and that it uses translated copies of the R2 dither.
    */
  case WOVEN extends DitherAlgorithm("Woven")

  /** An error-diffusion dither that, like [[NEUE]], starts with [[DIFFUSION Floyd-Steinberg]] dither and adds in blue noise values to break up patterns.
    */
  case DODGY extends DitherAlgorithm("Dodgy")

  /** An intentionally-low-fidelity ordered dither with obvious repeating 2x2 patterns on a regular grid. */
  case LOAF extends DitherAlgorithm("Loaf")

  /** An error-diffusion dither (using Burkes instead of Floyd-Steinberg) that uses offset versions of the R2 sequence and different blue noise textures. This is currently the default dither.
    */
  case WREN extends DitherAlgorithm("Wren")

  /** An error-diffusion dither (using Burkes instead of Floyd-Steinberg) that uses an assortment of patterns to add error to diffuse.
    */
  case OVERBOARD extends DitherAlgorithm("Overboard")

  /** An error-diffusion dither using Burkes dither instead of Floyd-Steinberg. */
  case BURKES extends DitherAlgorithm("Burkes")

  /** An error-diffusion dither based closely on [[BURKES]], but that modifies how much error gets diffused using a per-pixel multiplier obtained from blue noise.
    */
  case OCEANIC extends DitherAlgorithm("Oceanic")

  /** A close relative of [[OCEANIC]], this also incorporates noise into [[BURKES]] to change how each pixel diffuses error, with different noise for each channel.
    */
  case SEASIDE extends DitherAlgorithm("Seaside")

  /** A relative of [[LOAF]], this is another ordered dither, with comparable speed and higher quality than LOAF. */
  case GOURD extends DitherAlgorithm("Gourd")

  /** An ordered dither based on using Blue Noise with a Tent (or triangular-mapped) distribution. */
  case BLUNT extends DitherAlgorithm("Blunt")

  /** An ordered dither that uses a 128x128 triangular-mapped Bayer Matrix. */
  case BANTER extends DitherAlgorithm("Banter")

  /** An ordered dither that works much like [[ROBERTS]], but adds less error when the palette is larger. */
  case MARTEN extends DitherAlgorithm("Marten")

  /** A simple but effective ordered dither related to [[GRADIENT_NOISE]]. */
  case ADDITIVE extends DitherAlgorithm("Additive")

  override def toString: String = legibleName
}

object DitherAlgorithm {

  /** A cached array of all values, to avoid repeatedly allocating new arrays on each call to values(). */
  val ALL: Array[DitherAlgorithm] = DitherAlgorithm.values
}
