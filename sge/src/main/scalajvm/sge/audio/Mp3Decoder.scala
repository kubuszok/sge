/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../audio/Mp3.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Mp3.Sound/Mp3.Music inner classes -> Mp3Decoder standalone utility
 *   Convention: JVM-only (depends on JLayer/javazoom); placed in scalajvm/sge/audio/
 *   Convention: boundary/break instead of return
 *   Convention: extracted decoder logic from backend Sound/Music wrappers into reusable utility
 *   Idiom: split packages; SgeError instead of GdxRuntimeException
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package audio

import java.io.ByteArrayOutputStream
import javazoom.jl.decoder.{ Bitstream, BitstreamException, Header, MP3Decoder as JLayerDecoder, OutputBuffer }
import sge.files.FileHandle
import sge.utils.SgeError
import scala.util.boundary
import scala.util.boundary.break

/** Decodes MP3 audio files using JLayer (javazoom).
  *
  * Provides two modes of operation:
  *   - [[decodeAll]]: fully decodes an MP3 file into a PCM byte array (for sound effects)
  *   - Streaming via [[Mp3Decoder]] instance: reads PCM chunks incrementally (for music)
  *
  * JVM-only — depends on the JLayer (javazoom.jl) library.
  *
  * @author
  *   Nathan Sweet (original implementation)
  */
class Mp3Decoder private (private val file: FileHandle) extends java.io.Closeable {

  private var bitstream:    Bitstream     = new Bitstream(file.read())
  private var decoder:      JLayerDecoder = new JLayerDecoder()
  private var outputBuffer: OutputBuffer  = scala.compiletime.uninitialized
  private var _setup:       Boolean       = false

  /** Number of audio channels (1 = mono, 2 = stereo). Set after the first frame is read. */
  var channels: Int = 0

  /** Bits per sample (always 16 for MP3 decoded output). */
  val bitDepth: Int = 16

  /** Sample rate in Hz. Set after the first frame is read. */
  var sampleRate: Int = 0

  // Read the first header to determine format
  try {
    val header = bitstream.readFrame()
    if (header == null) throw SgeError.AudioError(s"Empty MP3: ${file.name()}")
    channels = if (header.mode() == Header.SINGLE_CHANNEL) 1 else 2
    outputBuffer = new OutputBuffer(channels, false)
    decoder.setOutputBuffer(outputBuffer)
    sampleRate = header.getSampleRate
    _setup = true
    // Close and reopen so first read() starts from beginning
    bitstream.close()
    bitstream = new Bitstream(file.read())
    decoder = new JLayerDecoder()
    _setup = false
  } catch {
    case e: BitstreamException =>
      throw SgeError.AudioError(s"Error reading MP3 header: ${file.name()}", Some(e))
  }

  /** Reads decoded PCM data into the buffer.
    *
    * @param buffer
    *   the output buffer to fill with PCM data
    * @return
    *   the number of bytes written, or 0 if the stream is exhausted
    */
  def read(buffer: Array[Byte]): Int =
    try
      boundary {
        if (!_setup) {
          // Re-read first header to set up output buffer on fresh stream
          val header = bitstream.readFrame()
          if (header == null) break(0)
          channels = if (header.mode() == Header.SINGLE_CHANNEL) 1 else 2
          outputBuffer = new OutputBuffer(channels, false)
          decoder.setOutputBuffer(outputBuffer)
          sampleRate = header.getSampleRate
          _setup = true
          bitstream.closeFrame()
        }

        var totalLength       = 0
        val minRequiredLength = buffer.length - OutputBuffer.BUFFERSIZE * 2
        while (totalLength <= minRequiredLength) {
          val header = bitstream.readFrame()
          if (header == null) break(totalLength)
          try
            decoder.decodeFrame(header, bitstream)
          catch {
            // JLayer's decoder throws ArrayIndexOutOfBoundsException sometimes
            case _: Exception => ()
          }
          bitstream.closeFrame()

          val length = outputBuffer.reset()
          System.arraycopy(outputBuffer.getBuffer, 0, buffer, totalLength, length)
          totalLength += length
        }
        totalLength
      }
    catch {
      case t: Throwable =>
        reset()
        throw SgeError.AudioError(s"Error reading MP3 audio data: ${file.name()}", Some(t))
    }

  /** Resets the stream to the beginning. */
  def reset(): Unit = {
    try
      bitstream.close()
    catch {
      case _: BitstreamException => ()
    }
    bitstream = new Bitstream(file.read())
    decoder = new JLayerDecoder()
    _setup = false
  }

  override def close(): Unit =
    try
      bitstream.close()
    catch {
      case _: BitstreamException => ()
    }
}

object Mp3Decoder {

  /** Result of fully decoding an MP3 file. */
  final case class DecodedAudio(
    pcmData:    Array[Byte],
    channels:   Int,
    bitDepth:   Int,
    sampleRate: Int
  )

  /** Creates a new streaming Mp3Decoder for the given file.
    * @param file
    *   the MP3 file to decode
    */
  def apply(file: FileHandle): Mp3Decoder = new Mp3Decoder(file)

  /** Fully decodes an MP3 file into a PCM byte array.
    *
    * Suitable for short sound effects that should be loaded entirely into memory.
    *
    * @param file
    *   the MP3 file to decode
    * @return
    *   the decoded audio data with format information
    */
  def decodeAll(file: FileHandle): DecodedAudio = {
    val output    = new ByteArrayOutputStream(4096)
    val bitstream = new Bitstream(file.read())
    val decoder   = new JLayerDecoder()

    try {
      var outputBuffer: OutputBuffer = null
      var sampleRate = -1
      var channels   = -1

      boundary {
        while (true) {
          val header = bitstream.readFrame()
          if (header == null) break()
          if (outputBuffer == null) {
            channels = if (header.mode() == Header.SINGLE_CHANNEL) 1 else 2
            outputBuffer = new OutputBuffer(channels, false)
            decoder.setOutputBuffer(outputBuffer)
            sampleRate = header.getSampleRate
          }
          try
            decoder.decodeFrame(header, bitstream)
          catch {
            // JLayer's decoder throws ArrayIndexOutOfBoundsException sometimes
            case _: Exception => ()
          }
          bitstream.closeFrame()
          output.write(outputBuffer.getBuffer, 0, outputBuffer.reset())
        }
      }

      bitstream.close()

      if (channels == -1) throw SgeError.AudioError(s"Empty MP3: ${file.name()}")

      DecodedAudio(output.toByteArray, channels, 16, sampleRate)
    } catch {
      case e: SgeError  => throw e
      case t: Throwable =>
        throw SgeError.AudioError(s"Error decoding MP3: ${file.name()}", Some(t))
    }
  }
}
