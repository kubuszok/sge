/*
 * Ported from anim8-gdx - https://github.com/tommyettinger/anim8-gdx
 * Original authors: Matthias Mann, Nathan Sweet, Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * An almost-drop-in replacement for PixmapIO.PNG, optimized for speed at the
 * expense of features. Uses NONE filtering instead of PAETH filtering.
 *
 * Copyright (c) 2007 Matthias Mann - www.matthiasmann.de
 * Copyright (c) 2014 Nathan Sweet
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

import sge.files.FileHandle
import sge.graphics.Pixmap
import sge.utils.StreamUtils

import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

/** An almost-drop-in replacement for PixmapIO.PNG, optimized for speed at the expense of features. This type of PNG supports both full color and a full alpha channel; it does not reduce the colors to
  * match a palette.
  *
  * This class uses NONE filtering instead of PAETH filtering and a lower default compression level (2), which speeds up writing at the expense of file size.
  *
  * @author
  *   Matthias Mann
  * @author
  *   Nathan Sweet
  * @author
  *   Tommy Ettinger
  */
class FastPNG(initialBufferSize: Int) extends AutoCloseable {

  private val SIGNATURE: Array[Byte] = Array(137.toByte, 80, 78, 71, 13, 10, 26, 10)
  private val IHDR = 0x49484452
  private val IDAT = 0x49444154
  private val IEND = 0x49454e44
  private val COLOR_ARGB:          Byte = 6
  private val COMPRESSION_DEFLATE: Byte = 0
  private val FILTER_NONE:         Byte = 0
  private val INTERLACE_NONE:      Byte = 0

  private val buffer:       ChunkBuffer        = new ChunkBuffer(initialBufferSize)
  private val deflater:     Deflater           = new Deflater(2)
  private var curLineBytes: Array[Byte] | Null = null
  private var flipY:        Boolean            = true

  /** Creates a FastPNG writer with an initial buffer size of 1024. */
  def this() = this(1024)

  /** If true, the resulting PNG is flipped vertically. Default is true. */
  def setFlipY(flipY: Boolean): Unit = this.flipY = flipY

  /** Sets the deflate compression level. Default is 2 here instead of the default 6 in PixmapIO.PNG. Using compression level 2 is faster, but doesn't compress quite as well.
    */
  def setCompression(level: Int): Unit = deflater.setLevel(level)

  /** Writes the given Pixmap to the requested FileHandle.
    * @param file
    *   a FileHandle that must be writable
    * @param pixmap
    *   a Pixmap to write as a PNG image
    */
  def write(file: FileHandle, pixmap: Pixmap): Unit = {
    val output = file.write(false)
    try
      write(output, pixmap)
    finally
      StreamUtils.closeQuietly(output)
  }

  /** Writes the given Pixmap as a PNG to the given `output` stream without closing the stream. This can use all 32-bit colors.
    *
    * @param output
    *   the stream to write to; the stream will not be closed
    * @param pixmap
    *   the Pixmap to write
    */
  def write(output: OutputStream, pixmap: Pixmap): Unit = {
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)

      buffer.writeInt(IHDR)
      val width  = pixmap.width.toInt
      val height = pixmap.height.toInt
      buffer.writeInt(width)
      buffer.writeInt(height)
      buffer.writeByte(8) // 8 bits per component.
      buffer.writeByte(COLOR_ARGB)
      buffer.writeByte(COMPRESSION_DEFLATE)
      buffer.writeByte(FILTER_NONE)
      buffer.writeByte(INTERLACE_NONE)
      buffer.endChunk(dataOutput)

      buffer.writeInt(IDAT)
      deflater.reset()

      val lineLen = width * 4
      val curLine =
        if (curLineBytes == null || curLineBytes.nn.length < lineLen) {
          curLineBytes = new Array[Byte](lineLen)
          curLineBytes.nn
        } else {
          curLineBytes.nn
        }

      var y = 0
      while (y < height) {
        val py = if (flipY) height - y - 1 else y
        var px = 0
        var x  = 0
        while (px < width) {
          val pixel = pixmap.getPixel(Pixels(px), Pixels(py))
          curLine(x) = ((pixel >>> 24) & 0xff).toByte
          x += 1
          curLine(x) = ((pixel >>> 16) & 0xff).toByte
          x += 1
          curLine(x) = ((pixel >>> 8) & 0xff).toByte
          x += 1
          curLine(x) = (pixel & 0xff).toByte
          x += 1
          px += 1
        }
        // NONE filtering
        deflaterOutput.write(FILTER_NONE)
        deflaterOutput.write(curLine, 0, lineLen)
        y += 1
      }
      deflaterOutput.finish()
      buffer.endChunk(dataOutput)

      buffer.writeInt(IEND)
      buffer.endChunk(dataOutput)

      output.flush()
    } catch {
      case e: IOException =>
        System.err.println("anim8: " + e.getMessage)
    }
  }

  /** Disposal should probably be done explicitly. Note, don't use the same FastPNG object after you call this method; you'll need to make a new one if you need to write again after closing.
    */
  override def close(): Unit =
    deflater.end()
}
