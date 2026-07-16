/*
 * SGE Gauntlet — deterministic PCM WAV fixture generator.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet

import sge.files.FileHandle

/** Generates small deterministic 16-bit mono PCM WAV files (a 440 Hz sine) so audio probes need no binary fixtures in the repo. */
object WavFixture {

  private val SampleRate = 22050

  /** Writes a `seconds`-long 440 Hz sine WAV to `file` and returns it. */
  def write(file: FileHandle, seconds: Float): FileHandle = {
    val sampleCount = (SampleRate * seconds).toInt
    val dataSize    = sampleCount * 2
    val buf         = java.nio.ByteBuffer.allocate(44 + dataSize)
    buf.order(java.nio.ByteOrder.LITTLE_ENDIAN)
    // RIFF header
    buf.put("RIFF".getBytes("US-ASCII"))
    buf.putInt(36 + dataSize)
    buf.put("WAVE".getBytes("US-ASCII"))
    // fmt chunk (PCM, mono, 16-bit)
    buf.put("fmt ".getBytes("US-ASCII"))
    buf.putInt(16)
    buf.putShort(1.toShort)
    buf.putShort(1.toShort)
    buf.putInt(SampleRate)
    buf.putInt(SampleRate * 2)
    buf.putShort(2.toShort)
    buf.putShort(16.toShort)
    // data chunk
    buf.put("data".getBytes("US-ASCII"))
    buf.putInt(dataSize)
    var i = 0
    while (i < sampleCount) {
      val t      = i.toDouble / SampleRate
      val sample = (Math.sin(2.0 * Math.PI * 440.0 * t) * 0.25 * Short.MaxValue).toInt.toShort
      buf.putShort(sample)
      i += 1
    }
    file.writeBytes(buf.array(), false)
    file
  }
}
