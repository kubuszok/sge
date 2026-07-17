// SGE — RED reproducer for ISS-772 (MiniaudioEngine.newSound fabricates the FFI audio format).
//
// The bug (MiniaudioEngine.scala:75-78): newSound reads the raw ENCODED file bytes and hands them
// to AudioOps.createSound together with HARD-CODED params channels=2, bits=16, rate=44100,
// regardless of the file's real format — "relying on the native decoder ignoring them". Those
// params are part of the documented createSound FFI contract (pcmData describes 16-bit stereo
// 44.1kHz PCM), so a mono / non-44.1kHz asset is described dishonestly across the boundary.
//
// This suite loads a genuinely MONO, 22050 Hz WAV and asserts that the channels/sampleRate handed
// to createSound match the ACTUAL file format. It FAILS today because newSound always passes
// (2, 16, 44100). A faithful fix (parse the real format, or redesign the contract so params are
// derived rather than fabricated) makes the recorded args honest and flips this green.

package sge
package audio

import munit.FunSuite
import sge.files.{ FileHandle, FileType }

import java.nio.{ ByteBuffer, ByteOrder }

class MiniaudioEngineSoundFormatIss772RedSuite extends FunSuite {

  /** Builds a minimal valid canonical PCM WAV file (RIFF/WAVE, 16-bit) for the given format. */
  private def wavBytes(channels: Int, sampleRate: Int, frames: Int): Array[Byte] = {
    val bitsPerSample = 16
    val blockAlign    = channels * bitsPerSample / 8
    val byteRate      = sampleRate * blockAlign
    val dataSize      = frames * blockAlign
    val buf           = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
    buf.put("RIFF".getBytes("US-ASCII"))
    buf.putInt(36 + dataSize)
    buf.put("WAVE".getBytes("US-ASCII"))
    buf.put("fmt ".getBytes("US-ASCII"))
    buf.putInt(16) // PCM fmt chunk size
    buf.putShort(1.toShort) // audioFormat = PCM
    buf.putShort(channels.toShort)
    buf.putInt(sampleRate)
    buf.putInt(byteRate)
    buf.putShort(blockAlign.toShort)
    buf.putShort(bitsPerSample.toShort)
    buf.put("data".getBytes("US-ASCII"))
    buf.putInt(dataSize)
    // silent PCM payload — content is irrelevant, only the header format matters
    var i = 0
    while (i < dataSize) { buf.put(0.toByte); i += 1 }
    buf.array()
  }

  private def newSoundFromWav(channels: Int, sampleRate: Int): (WaveDFakeAudioOps, Sound) = {
    val ops = new WaveDFakeAudioOps
    val eng = MiniaudioEngine(ops)
    val tmp = java.io.File.createTempFile("sge-iss772-", ".wav")
    tmp.deleteOnExit()
    val out = new java.io.FileOutputStream(tmp)
    try out.write(wavBytes(channels, sampleRate, frames = 64))
    finally out.close()
    val sound = eng.newSound(new FileHandle(tmp, FileType.Absolute))
    (ops, sound)
  }

  test("ISS-772 (RED): newSound describes a mono 22050 Hz asset honestly to createSound") {
    val (ops, _) = newSoundFromWav(channels = 1, sampleRate = 22050)

    assertEquals(ops.createSoundArgs.size, 1, "exactly one createSound call for one newSound")
    val (channels, _, sampleRate) = ops.createSoundArgs.head

    // The file is mono @ 22050 Hz. newSound currently fabricates channels=2, rate=44100
    // (MiniaudioEngine.scala:78) and hands them across the FFI boundary.
    assertEquals(channels, 1, "createSound must receive the file's real channel count (mono), not the fabricated 2")
    assertEquals(sampleRate, 22050, "createSound must receive the file's real sample rate, not the fabricated 44100")
  }
}
