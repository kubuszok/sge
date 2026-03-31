/*
 * Ported from anim8-gdx - https://github.com/tommyettinger/anim8-gdx
 * Original authors: Matthias Mann, Nathan Sweet, Tommy Ettinger (PNG-8 parts only)
 * Licensed under the Apache License, Version 2.0
 *
 * PNG-8 encoder with compression; defaults to using FastPalette when possible.
 *
 * Copyright (c) 2007 Matthias Mann - www.matthiasmann.de
 * Copyright (c) 2014 Nathan Sweet
 * Copyright (c) 2018 Tommy Ettinger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package anim8

import sge.graphics.Pixmap

import java.io.OutputStream

/** PNG-8 encoder with compression; can write animated and non-animated PNG images in indexed-mode. This is a subclass of [[PNG8]] that defaults to using a [[FastPalette]] when possible and uses a
  * lower compression level (2) for faster writes.
  *
  * @author
  *   Matthias Mann
  * @author
  *   Nathan Sweet
  * @author
  *   Tommy Ettinger (PNG-8 parts only)
  */
class FastPNG8(initialBufferSize: Int) extends PNG8(initialBufferSize) {

  setCompression(2)

  def this() =
    this(128 * 128)

  /** Writes the pixmap to the stream without closing the stream, optionally computing an 8-bit palette from the given Pixmap. If [[palette]] is null (the default), this will compute a palette from
    * the given Pixmap regardless of computePalette.
    *
    * @param output
    *   an OutputStream that will not be closed
    * @param pixmap
    *   a Pixmap to write to the given output stream
    * @param computePalette
    *   if true, this will analyze the Pixmap and use the most common colors
    * @param dither
    *   true if this should dither colors that can't be represented exactly
    * @param threshold
    *   the analysis threshold to use if computePalette is true
    */
  override def write(output: OutputStream, pixmap: Pixmap, computePalette: Boolean, dither: Boolean, threshold: Int): Unit = {
    val clearPalette = palette == null
    if (clearPalette) {
      palette = new FastPalette(pixmap, threshold)
    } else if (computePalette) {
      palette.nn.analyze(pixmap, threshold)
    }
    palette.nn.setDitherStrength(ditherStrength)

    if (dither) {
      writeDithered(output, pixmap)
    } else {
      writeSolid(output, pixmap)
    }
    if (clearPalette) palette = null
  }

  /** Writes the given frames as an animated PNG-8, with optional dithering. Uses [[FastPalette]] when palette is null.
    *
    * @param output
    *   an OutputStream that will not be closed
    * @param frames
    *   a Pixmap Array to write as a sequence of frames to the given output stream
    * @param fps
    *   how many frames per second the animation should run at
    * @param dither
    *   true if this should use the current dither algorithm; false to not dither
    */
  override def write(output: OutputStream, frames: Array[Pixmap], fps: Int, dither: Boolean): Unit = {
    val clearPalette = palette == null
    if (clearPalette) {
      palette = new FastPalette(frames)
    }
    palette.nn.setDitherStrength(ditherStrength)
    if (dither) {
      write(output, frames, fps)
    } else {
      writeSolid(output, frames, fps)
    }
    if (clearPalette) palette = null
  }
}
