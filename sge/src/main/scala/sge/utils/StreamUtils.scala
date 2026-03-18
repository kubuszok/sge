/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/StreamUtils.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `@Null String charset` -> `Nullable[String]`
 *   Convention: `null` -> `Nullable`; `IOException` throws declarations omitted (not required in Scala)
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.StringWriter
import java.nio.ByteBuffer

/** Provides utility methods to copy streams. */
object StreamUtils {

  val DEFAULT_BUFFER_SIZE = 4096
  val EMPTY_BYTES         = Array.empty[Byte]

  /** Allocates a {@value #DEFAULT_BUFFER_SIZE} byte[] for use as a temporary buffer and calls {@link #copyStream(InputStream, OutputStream, byte[])} .
    */
  def copyStream(input: InputStream, output: OutputStream): Unit =
    copyStream(input, output, new Array[Byte](DEFAULT_BUFFER_SIZE))

  /** Allocates a byte[] of the specified size for use as a temporary buffer and calls {@link #copyStream(InputStream, OutputStream, byte[])} .
    */
  def copyStream(input: InputStream, output: OutputStream, bufferSize: Int): Unit =
    copyStream(input, output, new Array[Byte](bufferSize))

  /** Copy the data from an {@link InputStream} to an {@link OutputStream} , using the specified byte[] as a temporary buffer. The stream is not closed.
    */
  def copyStream(input: InputStream, output: OutputStream, buffer: Array[Byte]): Unit = {
    var bytesRead: Int = 0
    while ({ bytesRead = input.read(buffer); bytesRead != -1 })
      output.write(buffer, 0, bytesRead)
  }

  /** Allocates a {@value #DEFAULT_BUFFER_SIZE} byte[] for use as a temporary buffer and calls {@link #copyStream(InputStream, OutputStream, byte[])} .
    */
  def copyStream(input: InputStream, output: ByteBuffer): Unit =
    copyStream(input, output, new Array[Byte](DEFAULT_BUFFER_SIZE))

  /** Allocates a byte[] of the specified size for use as a temporary buffer and calls {@link #copyStream(InputStream, ByteBuffer, byte[])} .
    */
  def copyStream(input: InputStream, output: ByteBuffer, bufferSize: Int): Unit =
    copyStream(input, output, new Array[Byte](bufferSize))

  /** Copy the data from an {@link InputStream} to a {@link ByteBuffer} , using the specified byte[] as a temporary buffer. The buffer's limit is increased by the number of bytes copied, the position
    * is left unchanged. The stream is not closed.
    * @param output
    *   Must be a direct Buffer with native byte order and the buffer MUST be large enough to hold all the bytes in the stream. No error checking is performed.
    * @return
    *   the number of bytes copied.
    */
  def copyStream(input: InputStream, output: ByteBuffer, buffer: Array[Byte]): Int = {
    val startPosition = output.position()
    var total:     Int = 0
    var bytesRead: Int = 0
    while ({ bytesRead = input.read(buffer); bytesRead != -1 }) {
      BufferUtils.copy(buffer, 0, output, bytesRead)
      total += bytesRead
      output.position(startPosition + total)
    }
    output.position(startPosition)
    total
  }

  /** Copy the data from an {@link InputStream} to a byte array. The stream is not closed. */
  def copyStreamToByteArray(input: InputStream): Array[Byte] =
    copyStreamToByteArray(input, input.available())

  /** Copy the data from an {@link InputStream} to a byte array. The stream is not closed.
    * @param estimatedSize
    *   Used to allocate the output byte[] to possibly avoid an array copy.
    */
  def copyStreamToByteArray(input: InputStream, estimatedSize: Int): Array[Byte] = {
    val baos = OptimizedByteArrayOutputStream(Math.max(0, estimatedSize))
    copyStream(input, baos)
    baos.toByteArray()
  }

  /** Calls {@link #copyStreamToString(InputStream, int, String)} using the input's {@link InputStream#available() available} size and the platform's default charset.
    */
  def copyStreamToString(input: InputStream): String =
    copyStreamToString(input, input.available(), Nullable.empty)

  /** Calls {@link #copyStreamToString(InputStream, int, String)} using the platform's default charset. */
  def copyStreamToString(input: InputStream, estimatedSize: Int): String =
    copyStreamToString(input, estimatedSize, Nullable.empty)

  /** Copy the data from an {@link InputStream} to a string using the specified charset.
    * @param estimatedSize
    *   Used to allocate the output buffer to possibly avoid an array copy.
    * @param charset
    *   May be null to use the platform's default charset.
    */
  def copyStreamToString(input: InputStream, estimatedSize: Int, charset: Nullable[String]): String = {
    val reader = charset.fold(new InputStreamReader(input))(charset => new InputStreamReader(input, charset))
    val writer = new StringWriter(0 max estimatedSize)
    val buffer = new Array[Char](DEFAULT_BUFFER_SIZE)
    var charsRead: Int = 0;
    while ({ charsRead = reader.read(buffer); charsRead != -1 })
      writer.write(buffer, 0, charsRead)
    writer.toString()
  }

  /** Close and ignore all errors. */
  def closeQuietly(c: Closeable): Unit =
    Nullable(c).foreach { c =>
      try c.close()
      catch {
        case e: Error     => throw e // Never swallow OOM, SOE, etc.
        case _: Exception => // Intentionally ignored per closeQuietly contract
      }
    }

  /** A ByteArrayOutputStream which avoids copying of the byte array if possible. */
  final class OptimizedByteArrayOutputStream(initialSize: Int) extends ByteArrayOutputStream(initialSize) {

    override def toByteArray: Array[Byte] =
      if (count == buf.length) buf
      else super.toByteArray

    def buffer: Array[Byte] =
      buf
  }
}
