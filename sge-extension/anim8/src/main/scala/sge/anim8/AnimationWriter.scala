/*
 * Ported from anim8-gdx - https://github.com/tommyettinger/anim8-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.github.tommyettinger.anim8` -> `sge.anim8`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package anim8

import java.io.OutputStream

import sge.files.FileHandle
import sge.graphics.Pixmap

/** Interface for writing sequences of Pixmaps as animated images.
  *
  * @author
  *   Tommy Ettinger (original implementation)
  */
trait AnimationWriter {

  /** Writes the given frames to a file at the default frame rate (30 fps). */
  def write(file: FileHandle, frames: Array[Pixmap]): Unit

  /** Writes the given frames to a file at the specified frame rate. */
  def write(file: FileHandle, frames: Array[Pixmap], fps: Int): Unit

  /** Writes the given frames to an output stream at the specified frame rate. */
  def write(output: OutputStream, frames: Array[Pixmap], fps: Int): Unit
}
