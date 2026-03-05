/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../audio/Wav.java (inner class WavInputStream)
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Wav.WavInputStream -> WavInputStream (standalone class)
 *   Convention: extracted from backend to core-shared; pure RIFF parser with no audio engine deps
 *   Convention: boundary/break instead of return
 *   Idiom: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package audio

import java.io.{ EOFException, FilterInputStream, IOException }
import sge.files.FileHandle
import sge.utils.StreamUtils
import scala.util.boundary
import scala.util.boundary.break

/** A [[FilterInputStream]] that reads a WAV (RIFF) file, parsing its header to extract format information and positioning the stream at the start of PCM data.
  *
  * Supports PCM (type 0x0001) with 8 or 16-bit depth and IEEE float (type 0x0003) with 32 or 64-bit depth. If the format type is 0x0055 (MP3), the constructor returns early with [[codecType]] set so
  * the caller can delegate to an MP3 decoder.
  *
  * @param file
  *   the WAV file to read
  * @author
  *   Nathan Sweet (original implementation)
  */
class WavInputStream(file: FileHandle) extends FilterInputStream(file.read()) {

  /** Number of audio channels (1 = mono, 2 = stereo). */
  var channels: Int = 0

  /** Bits per sample (8, 16, 32, or 64). */
  var bitDepth: Int = 0

  /** Sample rate in Hz. */
  var sampleRate: Int = 0

  /** Number of data bytes remaining in the data chunk. */
  var dataRemaining: Int = 0

  /** Audio codec type from the fmt chunk (0x0001 = PCM, 0x0003 = IEEE float, 0x0055 = MP3). */
  var codecType: Int = 0

  // Parse the WAV header in the constructor
  try {
    if (read() != 'R' || read() != 'I' || read() != 'F' || read() != 'F')
      throw utils.SgeError.InvalidInput("RIFF header not found: " + file)

    skipFully(4)

    if (read() != 'W' || read() != 'A' || read() != 'V' || read() != 'E')
      throw utils.SgeError.InvalidInput("Invalid wave file header: " + file)

    val fmtChunkLength = seekToChunk('f', 'm', 't', ' ')

    // http://www-mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/WAVE.html
    // http://soundfile.sapp.org/doc/WaveFormat/
    codecType = (read() & 0xff) | ((read() & 0xff) << 8)

    if (codecType == 0x0055) () // Handle MP3 — caller should check codecType and delegate
    else {
      if (codecType != 0x0001 && codecType != 0x0003)
        throw utils.SgeError.InvalidInput(
          "WAV files must be PCM, unsupported format: " + getCodecName(codecType) + " (" + codecType + ")"
        )

      channels = (read() & 0xff) | ((read() & 0xff) << 8)
      sampleRate = (read() & 0xff) | ((read() & 0xff) << 8) | ((read() & 0xff) << 16) | ((read() & 0xff) << 24)

      skipFully(6)

      bitDepth = (read() & 0xff) | ((read() & 0xff) << 8)
      if (codecType == 0x0001) { // PCM
        if (bitDepth != 8 && bitDepth != 16)
          throw utils.SgeError.InvalidInput("PCM WAV files must be 8 or 16-bit: " + bitDepth)
      } else if (codecType == 0x0003) { // Float
        if (bitDepth != 32 && bitDepth != 64)
          throw utils.SgeError.InvalidInput("Floating-point WAV files must be 32 or 64-bit: " + bitDepth)
      }

      skipFully(fmtChunkLength - 16)

      dataRemaining = seekToChunk('d', 'a', 't', 'a')
    }
  } catch {
    case ex: Throwable =>
      StreamUtils.closeQuietly(this)
      throw utils.SgeError.InvalidInput("Error reading WAV file: " + file, Some(ex))
  }

  private def seekToChunk(c1: Char, c2: Char, c3: Char, c4: Char): Int =
    boundary {
      while (true) {
        var found = read() == c1
        found &= read() == c2
        found &= read() == c3
        found &= read() == c4
        val chunkLength =
          (read() & 0xff) | ((read() & 0xff) << 8) | ((read() & 0xff) << 16) | ((read() & 0xff) << 24)
        if (chunkLength == -1) throw IOException("Chunk not found: " + c1.toString + c2 + c3 + c4)
        if (found) break(chunkLength)
        skipFully(chunkLength)
      }
      0 // unreachable, satisfies compiler
    }

  private def skipFully(count: Int): Unit = {
    var remaining = count
    while (remaining > 0) {
      val skipped = in.skip(remaining.toLong)
      if (skipped <= 0) throw EOFException("Unable to skip.")
      remaining -= skipped.toInt
    }
  }

  override def read(buffer: Array[Byte]): Int =
    if (dataRemaining == 0) -1
    else {
      var offset = 0
      boundary {
        while (offset < buffer.length) {
          val length = Math.min(super.read(buffer, offset, buffer.length - offset), dataRemaining)
          if (length == -1) {
            if (offset > 0) break(offset)
            else break(-1)
          }
          offset += length
          dataRemaining -= length
        }
        offset
      }
    }

  /** Returns a human-readable name for the given WAV codec type.
    * @param codecType
    *   16-bit value from the fmt chunk
    * @return
    *   a human-readable codec name
    */
  private def getCodecName(codecType: Int): String =
    codecType match {
      case 0x0002 => "Microsoft ADPCM"
      case 0x0006 => "ITU-T G.711 A-law"
      case 0x0007 => "ITU-T G.711 u-law"
      case 0x0011 => "IMA ADPCM"
      case 0x0022 => "DSP Group TrueSpeech"
      case 0x0031 => "Microsoft GSM 6.10"
      case 0x0040 => "Antex G.721 ADPCM"
      case 0x0070 => "Lernout & Hauspie CELP 4.8kbps"
      case 0x0072 => "Lernout & Hauspie CBS 12kbps"
      case 0xfffe => "Extensible"
      case _      => "Unknown"
    }
}
