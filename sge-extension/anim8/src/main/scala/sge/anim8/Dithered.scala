/*
 * Ported from anim8-gdx - https://github.com/tommyettinger/anim8-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 38
 * Covenant-baseline-methods: Dithered,ditherAlgorithm,ditherAlgorithm_,palette,palette_
 * Covenant-source-reference: com/github/tommyettinger/anim8/Dithered.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 38634cefd749a9a8af4534ca285c8e72437fe181
 */
package sge
package anim8

/** A renderer and/or writer that allows selecting a [[DitherAlgorithm]] for its output. */
trait Dithered {

  /** Gets the PaletteReducer this uses to lower the color count in an image. If the PaletteReducer is null, this should try to assign itself a PaletteReducer when given a new image.
    * @return
    *   the PaletteReducer this uses; may be null
    */
  def palette: PaletteReducer | Null

  /** Sets the PaletteReducer this uses to bring a high-color or different-palette image down to a smaller palette size. If `palette` is null, this should try to assign itself a PaletteReducer when
    * given a new image.
    * @param palette
    *   a PaletteReducer that is often pre-configured with a specific palette; null is usually allowed
    */
  def palette_=(palette: PaletteReducer | Null): Unit

  /** Gets the [[DitherAlgorithm]] this is currently using.
    * @return
    *   which dithering algorithm this currently uses.
    */
  def ditherAlgorithm: DitherAlgorithm

  /** Sets the dither algorithm (or disables it) using an enum constant from [[DitherAlgorithm]]. If this is given null, it instead does nothing.
    * @param ditherAlgorithm
    *   which [[DitherAlgorithm]] to use for upcoming output
    */
  def ditherAlgorithm_=(ditherAlgorithm: DitherAlgorithm): Unit
}
