/*
 * Ported from anim8-gdx - https://github.com/tommyettinger/anim8-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * GIF encoder using standard LZW compression; defaults to using FastPalette
 * and has fastAnalysis enabled. Based on Nick Badal's Android port of
 * Alessandro La Rossa's J2ME port of Kevin Weiner's pure Java animated GIF
 * encoder. The original has no copyright asserted.
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 72
 * Covenant-baseline-methods: FastGif,i,write
 * Covenant-source-reference: com/github/tommyettinger/anim8/FastGif.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 38634cefd749a9a8af4534ca285c8e72437fe181
 */
package sge
package anim8

import scala.util.boundary
import scala.util.boundary.break

import java.io.OutputStream

import sge.graphics.Pixmap

/** GIF encoder using standard LZW compression; can write animated and non-animated GIF images. This is a subclass of [[AnimatedGif]] that defaults to using a [[FastPalette]] when possible and
  * defaults to having [[AnimatedGif.fastAnalysis fastAnalysis]] enabled. An instance can be reused to encode multiple GIFs with minimal allocation.
  *
  * You can configure the target palette and how this can dither colors via the [[palette]] field, which is a [[PaletteReducer]] object that defaults to null and can be reused. If you assign a
  * PaletteReducer to palette, the methods [[PaletteReducer.exact(Array[Int])* PaletteReducer.exact]] or [[PaletteReducer.analyze(Pixmap)* PaletteReducer.analyze]] can be used to make the target
  * palette match a specific set of colors or the colors in an existing image. If palette is null, this will use a [[FastPalette]] subclass of PaletteReducer, and will compute a palette for each GIF
  * that closely fits its set of given animation frames.
  */
class FastGif extends AnimatedGif {

  /** Writes the given Pixmap values in `frames`, in order, to an animated GIF in the OutputStream `output`. The resulting GIF will play back at `fps` frames per second. If [[palette]] is null,
    * [[AnimatedGif.fastAnalysis fastAnalysis]] is set to false, and frames contains 2 or more Pixmaps, then this will make a palette for the first frame using [[FastPalette.analyze]], then reuse that
    * FastPalette but recompute a different analyzed palette for each subsequent frame.
    *
    * @param output
    *   the OutputStream to write to; will not be closed by this method
    * @param frames
    *   an Array of Pixmap frames that should all be the same size, to be written in order
    * @param fps
    *   how many frames (from `frames`) to play back per second
    */
  override def write(output: OutputStream, frames: Array[Pixmap], fps: Int): Unit = boundary {
    if (frames == null || frames.isEmpty) {
      // noinspection: null check needed for Java interop @nowarn
      break(())
    }
    _clearPalette = palette == null
    if (_clearPalette) {
      if (fastAnalysis && frames.length > 1) {
        palette = new FastPalette()
        palette.nn.analyzeFast(frames(0), 300, 256)
      } else {
        palette = new FastPalette(frames(0))
      }
    }
    if (!start(output)) {
      break(())
    }
    setFrameRate(fps.toFloat)
    var i = 0
    while (i < frames.length) {
      addFrame(frames(i))
      i += 1
    }
    finish()
    if (_clearPalette) {
      palette = null
    }
  }
}
