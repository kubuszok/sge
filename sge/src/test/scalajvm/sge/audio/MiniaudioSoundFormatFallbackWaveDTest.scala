// SGE — ISS-772 follow-up (wave-D-K bounce #1): pins the non-PCM fallback branches of
// MiniaudioEngine.readSoundFormat.
//
// The frozen red suite (MiniaudioEngineSoundFormatIss772RedSuite) pins the honest-parse path for a
// canonical PCM WAV. This suite mutation-covers the fallback branches added for containers whose PCM
// format cannot be parsed up front — each must hand createSound the documented engine decode-target
// (channels=2, bits=16, rate=44100), never 0/0/0 and never a parse exception:
//   * MP3-in-WAV (fmt codecType 0x0055): WavInputStream early-returns with format fields still 0
//   * ADPCM-in-WAV (fmt codecType 0x0002): WavInputStream rejects it with SgeError.InvalidInput
//   * non-WAV container bytes (OGG magic): no RIFF/WAVE header to parse

package sge
package audio

import munit.FunSuite
import sge.files.{ FileHandle, FileType }

import java.nio.{ ByteBuffer, ByteOrder }

class MiniaudioSoundFormatFallbackWaveDTest extends FunSuite {

  /** Builds the smallest well-formed RIFF/WAVE file whose fmt chunk carries an arbitrary codecType. */
  private def wavBytesWithCodec(codecType: Int, channels: Int, sampleRate: Int): Array[Byte] = {
    val bitsPerSample = 16
    val blockAlign    = channels * bitsPerSample / 8
    val byteRate      = sampleRate * blockAlign
    val dataSize      = 16 * blockAlign
    val buf           = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
    buf.put("RIFF".getBytes("US-ASCII"))
    buf.putInt(36 + dataSize)
    buf.put("WAVE".getBytes("US-ASCII"))
    buf.put("fmt ".getBytes("US-ASCII"))
    buf.putInt(16) // fmt chunk size
    buf.putShort(codecType.toShort)
    buf.putShort(channels.toShort)
    buf.putInt(sampleRate)
    buf.putInt(byteRate)
    buf.putShort(blockAlign.toShort)
    buf.putShort(bitsPerSample.toShort)
    buf.put("data".getBytes("US-ASCII"))
    buf.putInt(dataSize)
    var i = 0
    while (i < dataSize) { buf.put(0.toByte); i += 1 }
    buf.array()
  }

  private def newSoundFromBytes(bytes: Array[Byte], suffix: String): WaveDFakeAudioOps = {
    val ops = new WaveDFakeAudioOps
    val eng = MiniaudioEngine(ops)
    val tmp = java.io.File.createTempFile("sge-iss772-fallback-", suffix)
    tmp.deleteOnExit()
    val out = new java.io.FileOutputStream(tmp)
    try out.write(bytes)
    finally out.close()
    val _ = eng.newSound(new FileHandle(tmp, FileType.Absolute))
    ops
  }

  test("ISS-772: MP3-in-WAV (codecType 0x0055) falls back to the engine decode-target, not 0/0/0") {
    val ops = newSoundFromBytes(wavBytesWithCodec(codecType = 0x0055, channels = 1, sampleRate = 22050), ".wav")
    assertEquals(ops.createSoundArgs.size, 1, "exactly one createSound call for one newSound")
    assertEquals(ops.createSoundArgs.head, (2, 16, 44100), "non-PCM codec in a WAV container must use the engine decode-target")
  }

  test("ISS-772: ADPCM-in-WAV (codecType 0x0002, rejected by WavInputStream) falls back instead of throwing") {
    val ops = newSoundFromBytes(wavBytesWithCodec(codecType = 0x0002, channels = 2, sampleRate = 44100), ".wav")
    assertEquals(
      ops.createSoundArgs.size,
      1,
      "createSound must still be reached — the native decoder decides, matching pre-ISS-772 behavior"
    )
    assertEquals(ops.createSoundArgs.head, (2, 16, 44100), "unparseable WAV codec must use the engine decode-target")
  }

  test("ISS-772: non-WAV container bytes fall back to the engine decode-target") {
    val ogg = "OggS".getBytes("US-ASCII") ++ new Array[Byte](64)
    val ops = newSoundFromBytes(ogg, ".ogg")
    assertEquals(ops.createSoundArgs.size, 1, "exactly one createSound call for one newSound")
    assertEquals(ops.createSoundArgs.head, (2, 16, 44100), "non-WAV container must use the engine decode-target")
  }
}
