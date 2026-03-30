/*
 * Ported from anim8-gdx - https://github.com/tommyettinger/anim8-gdx
 * Original authors: Tommy Ettinger, Matthias Mann, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Full-color animated PNG encoder with compression.
 * This type of animated PNG supports both full color and a full alpha channel; it
 * does not reduce the colors to match a palette.
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

/** Full-color animated PNG encoder with compression. This type of animated PNG supports both full color and a full alpha channel; it does not reduce the colors to match a palette. If your image does
  * not have a full alpha channel and has 256 or fewer colors, you can use [[AnimatedGif]] or the animated mode of [[PNG8]], which have comparable APIs. An instance can be reused to encode multiple
  * animated PNGs with minimal allocation.
  *
  * The animated PNG (often called APNG) files this produces default to using a high compression level, but this is somewhat slow. You can use [[setCompression]] to set compression to 2, which results
  * in about 10-15% larger files that take about half the time to write.
  */
class AnimatedPNG(initialBufferSize: Int) extends AnimationWriter with AutoCloseable {

  private val SIGNATURE: Array[Byte] = Array(137.toByte, 80, 78, 71, 13, 10, 26, 10)
  private val IHDR = 0x49484452
  private val acTL = 0x6163544c
  private val fcTL = 0x6663544c
  private val IDAT = 0x49444154
  private val fdAT = 0x66644154
  private val IEND = 0x49454e44
  private val COLOR_ARGB:          Byte = 6
  private val COMPRESSION_DEFLATE: Byte = 0
  private val FILTER_NONE:         Byte = 0
  private val INTERLACE_NONE:      Byte = 0

  private val buffer:       ChunkBuffer        = new ChunkBuffer(initialBufferSize)
  private val deflater:     Deflater           = new Deflater()
  private var curLineBytes: Array[Byte] | Null = null
  private var flipY:        Boolean            = true

  /** Creates an AnimatedPNG writer with an initial buffer size of 16384. */
  def this() = this(16384)

  /** If true, the resulting AnimatedPNG is flipped vertically. Default is true. */
  def setFlipY(flipY: Boolean): Unit = this.flipY = flipY

  /** Sets the deflate compression level. Default is [[Deflater.DEFAULT_COMPRESSION]]. */
  def setCompression(level: Int): Unit = deflater.setLevel(level)

  /** Writes an animated PNG file consisting of the given `frames` to the given `file`, at 60 frames per second. */
  override def write(file: FileHandle, frames: Array[Pixmap]): Unit = {
    val output = file.write(false)
    try
      write(output, frames, 60)
    finally
      StreamUtils.closeQuietly(output)
  }

  /** Writes an animated PNG file consisting of the given `frames` to the given `file`, at `fps` frames per second. */
  override def write(file: FileHandle, frames: Array[Pixmap], fps: Int): Unit = {
    val output = file.write(false)
    try
      write(output, frames, fps)
    finally
      StreamUtils.closeQuietly(output)
  }

  /** Writes animated PNG data consisting of the given `frames` to the given `output` stream without closing the stream, at `fps` frames per second.
    */
  override def write(output: OutputStream, frames: Array[Pixmap], fps: Int): Unit = {
    var pixmap         = frames(0)
    val deflaterOutput = new DeflaterOutputStream(buffer, deflater)
    val dataOutput     = new DataOutputStream(output)
    try {
      dataOutput.write(SIGNATURE)
      val width  = pixmap.width.toInt
      val height = pixmap.height.toInt

      buffer.writeInt(IHDR)
      buffer.writeInt(width)
      buffer.writeInt(height)
      buffer.writeByte(8) // 8 bits per component.
      buffer.writeByte(COLOR_ARGB)
      buffer.writeByte(COMPRESSION_DEFLATE)
      buffer.writeByte(FILTER_NONE)
      buffer.writeByte(INTERLACE_NONE)
      buffer.endChunk(dataOutput)

      buffer.writeInt(acTL)
      buffer.writeInt(frames.length)
      buffer.writeInt(0)
      buffer.endChunk(dataOutput)

      val lineLen = width * 4

      var seq = 0
      var fi  = 0
      while (fi < frames.length) {

        buffer.writeInt(fcTL)
        buffer.writeInt(seq)
        seq += 1
        buffer.writeInt(width)
        buffer.writeInt(height)
        buffer.writeInt(0)
        buffer.writeInt(0)
        buffer.writeShort(1)
        buffer.writeShort(fps)
        buffer.writeByte(0)
        buffer.writeByte(0)
        buffer.endChunk(dataOutput)

        if (fi == 0) {
          buffer.writeInt(IDAT)
        } else {
          pixmap = frames(fi)
          buffer.writeInt(fdAT)
          buffer.writeInt(seq)
          seq += 1
        }
        deflater.reset()

        if (curLineBytes == null || curLineBytes.nn.length < lineLen) {
          curLineBytes = new Array[Byte](lineLen)
        }
        val curLine = curLineBytes.nn

        var y = 0
        while (y < height) {
          val py = if (flipY) height - y - 1 else y
          var px = 0
          var x0 = 0
          while (px < width) {
            val pixel = pixmap.getPixel(Pixels(px), Pixels(py))
            curLine(x0) = ((pixel >>> 24) & 0xff).toByte
            x0 += 1
            curLine(x0) = ((pixel >>> 16) & 0xff).toByte
            x0 += 1
            curLine(x0) = ((pixel >>> 8) & 0xff).toByte
            x0 += 1
            curLine(x0) = (pixel & 0xff).toByte
            x0 += 1
            px += 1
          }
          deflaterOutput.write(FILTER_NONE)
          deflaterOutput.write(curLine, 0, lineLen)
          y += 1
        }
        deflaterOutput.finish()
        buffer.endChunk(dataOutput)
        fi += 1
      }
      buffer.writeInt(IEND)
      buffer.endChunk(dataOutput)
      output.flush()
    } catch {
      case e: IOException =>
        System.err.println("anim8: " + e.getMessage)
    }
  }

  /** Disposal should probably be done explicitly, especially if using JRE versions after 8. Note, don't use the same AnimatedPNG object after you call this method.
    */
  override def close(): Unit =
    deflater.end()
}
