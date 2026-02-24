/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/ETC1.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.Buffer
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

import sge.files.FileHandle
import sge.graphics.Pixmap
import sge.graphics.Pixmap.Format
import sge.math.MathUtils
import sge.utils.BufferUtils
import sge.utils.SgeError
import sge.utils.StreamUtils

/** Class for encoding and decoding ETC1 compressed images. Also provides methods to add a PKM header.
  * @author
  *   mzechner (original implementation)
  */
class ETC1 {
  // Implementation would be in companion object
}

object ETC1 {

  /** The PKM header size in bytes * */
  val PKM_HEADER_SIZE: Int = 16
  val ETC1_RGB8_OES:   Int = 0x00008d64

  /** Class for storing ETC1 compressed image data.
    * @author
    *   mzechner
    */
  final class ETC1Data(
    /** the width in pixels * */
    val width: Int,
    /** the height in pixels * */
    val height: Int,
    /** the optional PKM header and compressed image data * */
    val compressedData: ByteBuffer,
    /** the offset in bytes to the actual compressed data. Might be 16 if this contains a PKM header, 0 otherwise * */
    val dataOffset: Int
  ) extends AutoCloseable {

    checkNPOT()

    def this(pkmFile: FileHandle) = {
      this(
        ETC1.getWidthPKM(ETC1.loadCompressedData(pkmFile), 0),
        ETC1.getHeightPKM(ETC1.loadCompressedData(pkmFile), 0),
        ETC1.loadCompressedData(pkmFile),
        PKM_HEADER_SIZE
      )
    }

    private def checkNPOT(): Unit =
      if (!MathUtils.isPowerOfTwo(width) || !MathUtils.isPowerOfTwo(height)) {
        System.out.println("ETC1Data warning: non-power-of-two ETC1 textures may crash the driver of PowerVR GPUs")
      }

    /** @return whether this ETC1Data has a PKM header */
    def hasPKMHeader(): Boolean =
      dataOffset == 16

    /** Writes the ETC1Data with a PKM header to the given file.
      * @param file
      *   the file.
      */
    def write(file: FileHandle): Unit = {
      var write: DataOutputStream = null
      val buffer       = Array.ofDim[Byte](10 * 1024)
      var writtenBytes = 0
      compressedData.position(0)
      compressedData.limit(compressedData.capacity())
      try {
        write = new DataOutputStream(new GZIPOutputStream(file.write(false)))
        write.writeInt(compressedData.capacity())
        while (writtenBytes != compressedData.capacity()) {
          val bytesToWrite = scala.math.min(compressedData.remaining(), buffer.length)
          compressedData.get(buffer, 0, bytesToWrite)
          write.write(buffer, 0, bytesToWrite)
          writtenBytes += bytesToWrite
        }
      } catch {
        case e: Exception => throw SgeError.FileWriteError(file, "Couldn't write PKM file", Some(e))
      } finally
        StreamUtils.closeQuietly(write)
      compressedData.position(dataOffset)
      compressedData.limit(compressedData.capacity())
    }

    /** Releases the native resources of the ETC1Data instance. */
    def close(): Unit =
      BufferUtils.disposeUnsafeByteBuffer(compressedData)

    override def toString(): String =
      if (hasPKMHeader()) {
        s"${if (ETC1.isValidPKM(compressedData, 0)) "valid" else "invalid"} pkm [${ETC1.getWidthPKM(compressedData, 0)}x${ETC1.getHeightPKM(compressedData, 0)}], compressed: ${compressedData
            .capacity() - ETC1.PKM_HEADER_SIZE}"
      } else {
        s"raw [${width}x${height}], compressed: ${compressedData.capacity() - ETC1.PKM_HEADER_SIZE}"
      }
  }

  private def loadCompressedData(pkmFile: FileHandle): ByteBuffer = {
    val buffer = Array.ofDim[Byte](1024 * 10)
    var in: DataInputStream = null
    try {
      in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(pkmFile.read())))
      val fileSize       = in.readInt()
      val compressedData = BufferUtils.newUnsafeByteBuffer(fileSize)
      var readBytes      = 0
      while ({ readBytes = in.read(buffer); readBytes != -1 })
        compressedData.put(buffer, 0, readBytes)
      compressedData.position(0)
      compressedData.limit(compressedData.capacity())
      compressedData
    } catch {
      case e: Exception => throw SgeError.FileReadError(pkmFile, "Couldn't load pkm file", Some(e))
    } finally
      StreamUtils.closeQuietly(in)
  }

  private def getPixelSize(format: Format): Int =
    if (format == Format.RGB565) 2
    else if (format == Format.RGB888) 3
    else throw SgeError.GraphicsError("Can only handle RGB565 or RGB888 images")

  /** Encodes the image via the ETC1 compression scheme. Only {@link Format#RGB565} and {@link Format#RGB888} are supported.
    * @param pixmap
    *   the {@link Pixmap}
    * @return
    *   the {@link ETC1Data}
    */
  def encodeImage(pixmap: Pixmap): ETC1Data = {
    val pixelSize      = getPixelSize(pixmap.getFormat())
    val compressedData = encodeImage(pixmap.getPixels(), 0, pixmap.getWidth(), pixmap.getHeight(), pixelSize)
    BufferUtils.newUnsafeByteBuffer(compressedData)
    new ETC1Data(pixmap.getWidth(), pixmap.getHeight(), compressedData, 0)
  }

  /** Encodes the image via the ETC1 compression scheme. Only {@link Format#RGB565} and {@link Format#RGB888} are supported. Adds a PKM header in front of the compressed image data.
    * @param pixmap
    *   the {@link Pixmap}
    * @return
    *   the {@link ETC1Data}
    */
  def encodeImagePKM(pixmap: Pixmap): ETC1Data = {
    val pixelSize      = getPixelSize(pixmap.getFormat())
    val compressedData = encodeImagePKM(pixmap.getPixels(), 0, pixmap.getWidth(), pixmap.getHeight(), pixelSize)
    BufferUtils.newUnsafeByteBuffer(compressedData)
    new ETC1Data(pixmap.getWidth(), pixmap.getHeight(), compressedData, 16)
  }

  /** Takes ETC1 compressed image data and converts it to a {@link Format#RGB565} or {@link Format#RGB888} {@link Pixmap} . Does not modify the ByteBuffer's position or limit.
    * @param etc1Data
    *   the {@link ETC1Data} instance
    * @param format
    *   either {@link Format#RGB565} or {@link Format#RGB888}
    * @return
    *   the Pixmap
    */
  def decodeImage(etc1Data: ETC1Data, format: Format): Pixmap = {
    val dataOffset = if (etc1Data.hasPKMHeader()) 16 else 0
    val width      = if (etc1Data.hasPKMHeader()) getWidthPKM(etc1Data.compressedData, 0) else etc1Data.width
    val height     = if (etc1Data.hasPKMHeader()) getHeightPKM(etc1Data.compressedData, 0) else etc1Data.height

    val pixelSize = getPixelSize(format)
    val pixmap    = new Pixmap(width, height, format)
    decodeImage(etc1Data.compressedData, dataOffset, pixmap.getPixels(), 0, width, height, pixelSize)
    pixmap
  }

  /** @param width
    *   the width in pixels
    * @param height
    *   the height in pixels
    * @return
    *   the number of bytes needed to store the compressed data
    */
  def getCompressedDataSize(width: Int, height: Int): Int =
    // Native implementation - placeholder
    ???

  /** Writes a PKM header to the {@link ByteBuffer} . Does not modify the position or limit of the ByteBuffer.
    * @param header
    *   the direct native order {@link ByteBuffer}
    * @param offset
    *   the offset to the header in bytes
    * @param width
    *   the width in pixels
    * @param height
    *   the height in pixels
    */
  def formatHeader(header: ByteBuffer, offset: Int, width: Int, height: Int): Unit =
    // Native implementation - placeholder
    ???

  /** @param header
    *   direct native order {@link ByteBuffer} holding the PKM header
    * @param offset
    *   the offset in bytes to the PKM header from the ByteBuffer's start
    * @return
    *   the width stored in the PKM header
    */
  def getWidthPKM(header: ByteBuffer, offset: Int): Int =
    // Native implementation - placeholder
    ???

  /** @param header
    *   direct native order {@link ByteBuffer} holding the PKM header
    * @param offset
    *   the offset in bytes to the PKM header from the ByteBuffer's start
    * @return
    *   the height stored in the PKM header
    */
  def getHeightPKM(header: ByteBuffer, offset: Int): Int =
    // Native implementation - placeholder
    ???

  /** @param header
    *   direct native order {@link ByteBuffer} holding the PKM header
    * @param offset
    *   the offset in bytes to the PKM header from the ByteBuffer's start
    * @return
    *   the width stored in the PKM header
    */
  def isValidPKM(header: ByteBuffer, offset: Int): Boolean =
    // Native implementation - placeholder
    ???

  /** Decodes the compressed image data to RGB565 or RGB888 pixel data. Does not modify the position or limit of the {@link ByteBuffer} instances.
    * @param compressedData
    *   the compressed image data in a direct native order {@link ByteBuffer}
    * @param offset
    *   the offset in bytes to the image data from the start of the buffer
    * @param decodedData
    *   the decoded data in a direct native order ByteBuffer, must hold width * height * pixelSize bytes.
    * @param offsetDec
    *   the offset in bytes to the decoded image data.
    * @param width
    *   the width in pixels
    * @param height
    *   the height in pixels
    * @param pixelSize
    *   the pixel size, either 2 (RBG565) or 3 (RGB888)
    */
  private def decodeImage(compressedData: ByteBuffer, offset: Int, decodedData: ByteBuffer, offsetDec: Int, width: Int, height: Int, pixelSize: Int): Unit =
    // Native implementation - placeholder
    ???

  /** Encodes the image data given as RGB565 or RGB888. Does not modify the position or limit of the {@link ByteBuffer} .
    * @param imageData
    *   the image data in a direct native order {@link ByteBuffer}
    * @param offset
    *   the offset in bytes to the image data from the start of the buffer
    * @param width
    *   the width in pixels
    * @param height
    *   the height in pixels
    * @param pixelSize
    *   the pixel size, either 2 (RGB565) or 3 (RGB888)
    * @return
    *   a new direct native order ByteBuffer containing the compressed image data
    */
  private def encodeImage(imageData: ByteBuffer, offset: Int, width: Int, height: Int, pixelSize: Int): ByteBuffer =
    // Native implementation - placeholder
    ???

  /** Encodes the image data given as RGB565 or RGB888. Does not modify the position or limit of the {@link ByteBuffer} .
    * @param imageData
    *   the image data in a direct native order {@link ByteBuffer}
    * @param offset
    *   the offset in bytes to the image data from the start of the buffer
    * @param width
    *   the width in pixels
    * @param height
    *   the height in pixels
    * @param pixelSize
    *   the pixel size, either 2 (RGB565) or 3 (RGB888)
    * @return
    *   a new direct native order ByteBuffer containing the compressed image data
    */
  private def encodeImagePKM(imageData: ByteBuffer, offset: Int, width: Int, height: Int, pixelSize: Int): ByteBuffer =
    // Native implementation - placeholder
    ???
}
