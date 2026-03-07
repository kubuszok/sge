/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../audio/JavaSoundAudioRecorder.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: JavaSoundAudioRecorder -> DesktopAudioRecorder
 *   Convention: JVM-only (javax.sound.sampled); placed in scalajvm/sge/audio/
 *   Convention: dispose() -> close() via Closeable
 *   Idiom: GdxRuntimeException -> SgeError.InvalidInput
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package audio

import javax.sound.sampled.{ AudioFormat, AudioSystem, TargetDataLine }
import javax.sound.sampled.AudioFormat.Encoding
import sge.utils.SgeError

/** An [[AudioRecorder]] implementation using `javax.sound.sampled`. JVM-only.
  *
  * @param samplingRate
  *   the sampling rate in Hz
  * @param isMono
  *   true for mono, false for stereo
  * @author
  *   mzechner (original implementation)
  */
class DesktopAudioRecorder(samplingRate: Int, isMono: Boolean) extends AudioRecorder {

  private val line: TargetDataLine =
    try {
      val channels  = if (isMono) 1 else 2
      val frameSize = if (isMono) 2 else 4
      val format    = new AudioFormat(Encoding.PCM_SIGNED, samplingRate.toFloat, 16, channels, frameSize, samplingRate.toFloat, false)
      val l         = AudioSystem.getTargetDataLine(format)
      l.open(format, 1024 * 4)
      l.start()
      l
    } catch {
      case ex: Exception => throw SgeError.InvalidInput("Error creating DesktopAudioRecorder.", Some(ex))
    }

  private var buffer: Array[Byte] = new Array[Byte](1024 * 4)

  override def read(samples: Array[Short], offset: Int, numSamples: Int): Unit = {
    if (buffer.length < numSamples * 2) buffer = new Array[Byte](numSamples * 2)

    val toRead    = numSamples * 2
    var bytesRead = 0
    while (bytesRead != toRead)
      bytesRead += line.read(buffer, bytesRead, toRead - bytesRead)

    var i = 0
    var j = 0
    while (i < numSamples * 2) {
      samples(offset + j) = ((buffer(i + 1) << 8) | (buffer(i) & 0xff)).toShort
      i += 2
      j += 1
    }
  }

  override def close(): Unit =
    line.close()
}
