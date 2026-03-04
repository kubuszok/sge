/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/PixmapIO.java
 * Original authors: mzechner, Nathan Sweet, Matthias Mann
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import sge.utils.Nullable
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.Buffer
import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

import sge.files.FileHandle
import sge.graphics.Pixmap.Format
import sge.graphics.g2d.Gdx2DPixmap
import sge.utils.SgeError
import sge.utils.StreamUtils

/** Writes Pixmaps to various formats.
  * @author
  *   mzechner (original implementation)
  * @author
  *   Nathan Sweet (original implementation)
  */
object PixmapIO {

  /** Writes the {@link Pixmap} to the given file using a custom compression scheme. First three integers define the width, height and format, remaining bytes are zlib compressed pixels. To be able to
    * load the Pixmap to a Texture, use ".cim" as the file suffix. Throws a GdxRuntimeException in case the Pixmap couldn't be written to the file.
    * @param file
    *   the file to write the Pixmap to
    */
  def writeCIM(file: FileHandle, pixmap: Pixmap): Unit =
    CIM.write(file, pixmap)

  /** Reads the {@link Pixmap} from the given file, assuming the Pixmap was written with the {@link PixmapIO#writeCIM(FileHandle, Pixmap)} method. Throws a GdxRuntimeException in case the file
    * couldn't be read.
    * @param file
    *   the file to read the Pixmap from
    */
  def readCIM(file: FileHandle): Pixmap =
    CIM.read(file)

  /** Writes the pixmap as a PNG. See {@link PNG} to write out multiple PNGs with minimal allocation.
    * @param compression
    *   sets the deflate compression level. Default is {@link Deflater#DEFAULT_COMPRESSION}
    * @param flipY
    *   flips the Pixmap vertically if true
    */
  def writePNG(file: FileHandle, pixmap: Pixmap, compression: Int, flipY: Boolean): Unit =
    try {
      val writer = new PNG((pixmap.getWidth() * pixmap.getHeight() * 1.5f).toInt); // Guess at deflated size.
      try {
        writer.setFlipY(flipY);
        writer.setCompression(compression);
        writer.write(file, pixmap);
      } finally
        writer.close();
    } catch {
      case ex: IOException => throw SgeError.GraphicsError("Error writing PNG: " + file, Some(ex));
    }

  /** Writes the pixmap as a PNG with compression. See {@link PNG} to configure the compression level, more efficiently flip the pixmap vertically, and to write out multiple PNGs with minimal
    * allocation.
    */
  def writePNG(file: FileHandle, pixmap: Pixmap): Unit =
    writePNG(file, pixmap, Deflater.DEFAULT_COMPRESSION, false);

  /** @author mzechner */
  private object CIM {
    private val BUFFER_SIZE = 32000;
    private val writeBuffer = new Array[Byte](BUFFER_SIZE);
    private val readBuffer  = new Array[Byte](BUFFER_SIZE);

    def write(file: FileHandle, pixmap: Pixmap): Unit = {
      val deflaterOutputStream = new DeflaterOutputStream(file.write(false));
      val out                  = new DataOutputStream(deflaterOutputStream);
      try {
        out.writeInt(pixmap.getWidth());
        out.writeInt(pixmap.getHeight());
        out.writeInt(toGdx2DPixmapFormat(pixmap.getFormat()));

        val pixelBuf = pixmap.getPixels();
        pixelBuf.asInstanceOf[Buffer].position(0);
        pixelBuf.asInstanceOf[Buffer].limit(pixelBuf.capacity());

        val remainingBytes = pixelBuf.capacity() % BUFFER_SIZE;
        val iterations     = pixelBuf.capacity() / BUFFER_SIZE;

        writeBuffer.synchronized {
          for (_ <- 0 until iterations) {
            pixelBuf.get(writeBuffer);
            out.write(writeBuffer);
          }

          pixelBuf.get(writeBuffer, 0, remainingBytes);
          out.write(writeBuffer, 0, remainingBytes);
        }

        pixelBuf.asInstanceOf[Buffer].position(0);
        pixelBuf.asInstanceOf[Buffer].limit(pixelBuf.capacity());
      } catch {
        case e: Exception => throw SgeError.GraphicsError("Couldn't write Pixmap to file '" + file + "'", Some(e));
      } finally
        StreamUtils.closeQuietly(out)
    }

    def read(file: FileHandle): Pixmap = {
      val in = new DataInputStream(new InflaterInputStream(new BufferedInputStream(file.read())));
      try {
        val width  = in.readInt();
        val height = in.readInt();
        val format = Format.fromGdx2DPixmapFormat(in.readInt());
        val pixmap = new Pixmap(width, height, format);

        val pixelBuf = pixmap.getPixels();
        pixelBuf.asInstanceOf[Buffer].position(0);
        pixelBuf.asInstanceOf[Buffer].limit(pixelBuf.capacity());

        readBuffer.synchronized {
          var readBytes = 0;
          while ({ readBytes = in.read(readBuffer); readBytes > 0 })
            pixelBuf.put(readBuffer, 0, readBytes);
        }

        pixelBuf.asInstanceOf[Buffer].position(0);
        pixelBuf.asInstanceOf[Buffer].limit(pixelBuf.capacity());
        pixmap;
      } catch {
        case e: Exception => throw SgeError.GraphicsError("Couldn't read Pixmap from file '" + file + "'", Some(e));
      } finally
        StreamUtils.closeQuietly(in)
    }

    private def toGdx2DPixmapFormat(format: Format): Int = format match {
      case Format.Alpha          => Gdx2DPixmap.GDX2D_FORMAT_ALPHA
      case Format.Intensity      => Gdx2DPixmap.GDX2D_FORMAT_ALPHA // Map intensity to alpha
      case Format.LuminanceAlpha => Gdx2DPixmap.GDX2D_FORMAT_LUMINANCE_ALPHA
      case Format.RGB565         => Gdx2DPixmap.GDX2D_FORMAT_RGB565
      case Format.RGBA4444       => Gdx2DPixmap.GDX2D_FORMAT_RGBA4444
      case Format.RGB888         => Gdx2DPixmap.GDX2D_FORMAT_RGB888
      case Format.RGBA8888       => Gdx2DPixmap.GDX2D_FORMAT_RGBA8888
    }
  }

  /** PNG encoder with compression. An instance can be reused to encode multiple PNGs with minimal allocation.
    *
    * <pre> Copyright (c) 2007 Matthias Mann - www.matthiasmann.de Copyright (c) 2014 Nathan Sweet
    *
    * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction,
    * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished
    * to do so, subject to the following conditions:
    *
    * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
    *
    * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
    * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
    * OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. </pre>
    *
    * @author
    *   Matthias Mann
    * @author
    *   Nathan Sweet
    */
  class PNG(initialBufferSize: Int = 128 * 128) extends AutoCloseable {
    private val SIGNATURE = Array[Byte](-119, 80, 78, 71, 13, 10, 26, 10)
    private val IHDR      = 0x49484452
    private val IDAT      = 0x49444154
    private val IEND      = 0x49454e44
    private val COLOR_ARGB:          Byte = 6
    private val COMPRESSION_DEFLATE: Byte = 0
    private val FILTER_NONE:         Byte = 0
    private val INTERLACE_NONE:      Byte = 0
    private val PAETH:               Byte = 4

    private val buffer:        ChunkBuffer = new ChunkBuffer(initialBufferSize)
    private val deflater:      Deflater    = new Deflater()
    private var lineOutBytes:  Array[Byte] = scala.compiletime.uninitialized
    private var curLineBytes:  Array[Byte] = scala.compiletime.uninitialized
    private var prevLineBytes: Array[Byte] = scala.compiletime.uninitialized
    private var flipY       = true
    private var lastLineLen = 0

    /** If true, the resulting PNG is flipped vertically. Default is true. */
    def setFlipY(flipY: Boolean): Unit =
      this.flipY = flipY

    /** Sets the deflate compression level. Default is {@link Deflater#DEFAULT_COMPRESSION}. */
    def setCompression(level: Int): Unit =
      deflater.setLevel(level)

    def write(file: FileHandle, pixmap: Pixmap): Unit = {
      val output = file.write(false)
      try
        write(output, pixmap)
      finally
        StreamUtils.closeQuietly(output)
    }

    /** Writes the pixmap to the stream without closing the stream. */
    @throws[IOException]
    def write(output: OutputStream, pixmap: Pixmap): Unit = {
      val deflaterOutput = new DeflaterOutputStream(buffer, deflater);
      val dataOutput     = new DataOutputStream(output);
      dataOutput.write(SIGNATURE);

      buffer.writeInt(IHDR);
      buffer.writeInt(pixmap.getWidth());
      buffer.writeInt(pixmap.getHeight());
      buffer.writeByte(8); // 8 bits per component.
      buffer.writeByte(COLOR_ARGB);
      buffer.writeByte(COMPRESSION_DEFLATE);
      buffer.writeByte(FILTER_NONE);
      buffer.writeByte(INTERLACE_NONE);
      buffer.endChunk(dataOutput);

      buffer.writeInt(IDAT);
      deflater.reset();

      val lineLen = pixmap.getWidth() * 4;

      if (Nullable(lineOutBytes).fold(true)(_.length < lineLen)) {
        lineOutBytes = new Array[Byte](lineLen);
      }
      if (Nullable(curLineBytes).fold(true)(_.length < lineLen)) {
        curLineBytes = new Array[Byte](lineLen);
      }
      if (Nullable(prevLineBytes).fold(true)(_.length < lineLen)) {
        prevLineBytes = new Array[Byte](lineLen);
        for (i <- 0 until lineLen)
          prevLineBytes(i) = 0;
      } else {
        for (i <- 0 until lastLineLen)
          prevLineBytes(i) = 0;
      }

      lastLineLen = lineLen;

      val pixels      = pixmap.getPixels();
      val oldPosition = pixels.position();
      val rgba8888    = pixmap.getFormat() == Format.RGBA8888;
      for (y <- 0 until pixmap.getHeight()) {
        val py = if (flipY) pixmap.getHeight() - y - 1 else y;
        if (rgba8888) {
          pixels.asInstanceOf[Buffer].position(py * lineLen);
          pixels.get(curLineBytes, 0, lineLen);
        } else {
          var x = 0;
          for (px <- 0 until pixmap.getWidth()) {
            val pixel = pixmap.getPixel(px, py);
            curLineBytes(x) = ((pixel >> 24) & 0xff).toByte;
            x += 1;
            curLineBytes(x) = ((pixel >> 16) & 0xff).toByte;
            x += 1;
            curLineBytes(x) = ((pixel >> 8) & 0xff).toByte;
            x += 1;
            curLineBytes(x) = (pixel & 0xff).toByte;
            x += 1;
          }
        }

        lineOutBytes(0) = (curLineBytes(0) - prevLineBytes(0)).toByte;
        lineOutBytes(1) = (curLineBytes(1) - prevLineBytes(1)).toByte;
        lineOutBytes(2) = (curLineBytes(2) - prevLineBytes(2)).toByte;
        lineOutBytes(3) = (curLineBytes(3) - prevLineBytes(3)).toByte;

        for (x <- 4 until lineLen) {
          val a  = curLineBytes(x - 4) & 0xff;
          val b  = prevLineBytes(x) & 0xff;
          val c  = prevLineBytes(x - 4) & 0xff;
          val p  = a + b - c;
          var pa = p - a;
          if (pa < 0) pa = -pa;
          var pb = p - b;
          if (pb < 0) pb = -pb;
          var pc = p - c;
          if (pc < 0) pc = -pc;
          val predictedValue =
            if (pa <= pb && pa <= pc)
              a
            else if (pb <= pc)
              b
            else
              c;
          lineOutBytes(x) = (curLineBytes(x) - predictedValue).toByte;
        }

        deflaterOutput.write(PAETH);
        deflaterOutput.write(lineOutBytes, 0, lineLen);

        val temp = curLineBytes;
        curLineBytes = prevLineBytes;
        prevLineBytes = temp;
      }
      pixels.asInstanceOf[Buffer].position(oldPosition);
      deflaterOutput.finish();
      buffer.endChunk(dataOutput);

      buffer.writeInt(IEND);
      buffer.endChunk(dataOutput);

      output.flush();
    }

    /** Close will happen automatically in finalize but can be done explicitly if desired. */
    def close(): Unit =
      deflater.end();

    private class ChunkBuffer(initialSize: Int)
        extends DataOutputStream({
          val buffer = new ByteArrayOutputStream(initialSize)
          val crc    = new CRC32()
          new CheckedOutputStream(buffer, crc)
        }) {
      private val buffer = new ByteArrayOutputStream(initialSize)
      private val crc    = new CRC32()

      @throws[IOException]
      def endChunk(target: DataOutputStream): Unit = {
        flush()
        target.writeInt(buffer.size() - 4)
        buffer.writeTo(target)
        target.writeInt(crc.getValue().toInt)
        buffer.reset()
        crc.reset()
      }
    }
  }
}
