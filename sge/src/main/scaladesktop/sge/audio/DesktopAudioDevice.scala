/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../audio/OpenALAudioDevice.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: OpenALAudioDevice -> DesktopAudioDevice
 *   Convention: OpenAL buffer management -> miniaudio engine audio device via AudioOps FFI
 *   Convention: opaque types (Volume) used in public API
 *   Idiom: split packages
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package audio

import sge.math.MathUtils
import sge.platform.AudioOps

/** A raw PCM audio output device backed by miniaudio.
  *
  * Writes 16-bit signed PCM data to the audio device. The device blocks when its internal buffers are full.
  *
  * @param deviceHandle
  *   the native audio device handle
  * @param _isMono
  *   true for mono, false for stereo
  * @param audioOps
  *   the audio FFI operations
  * @author
  *   Nathan Sweet (original implementation)
  */
class DesktopAudioDevice private[sge] (
  private val deviceHandle: Long,
  private val _isMono:      Boolean,
  private val audioOps:     AudioOps
) extends AudioDevice {

  private var bytes: Array[Byte] = scala.compiletime.uninitialized

  override def isMono: Boolean = _isMono

  @scala.annotation.nowarn("msg=deprecated") // null check — bytes is uninitialized (null) until first write
  override def writeSamples(samples: Array[Short], offset: Int, numSamples: Int): Unit = {
    if (bytes == null || bytes.length < numSamples * 2) bytes = new Array[Byte](numSamples * 2)
    val end = scala.math.min(offset + numSamples, samples.length)
    var i   = offset
    var ii  = 0
    while (i < end) {
      val sample = samples(i)
      bytes(ii) = (sample & 0xff).toByte
      ii += 1
      bytes(ii) = ((sample >> 8) & 0xff).toByte
      ii += 1
      i += 1
    }
    audioOps.writeAudioDevice(deviceHandle, bytes, 0, numSamples * 2)
  }

  @scala.annotation.nowarn("msg=deprecated") // null check — bytes is uninitialized (null) until first write
  override def writeSamples(samples: Array[Float], offset: Int, numSamples: Int): Unit = {
    if (bytes == null || bytes.length < numSamples * 2) bytes = new Array[Byte](numSamples * 2)
    val end = scala.math.min(offset + numSamples, samples.length)
    var i   = offset
    var ii  = 0
    while (i < end) {
      val floatSample = MathUtils.clamp(samples(i), -1.0f, 1.0f)
      val intSample   = (floatSample * 32767).toInt
      bytes(ii) = (intSample & 0xff).toByte
      ii += 1
      bytes(ii) = ((intSample >> 8) & 0xff).toByte
      ii += 1
      i += 1
    }
    audioOps.writeAudioDevice(deviceHandle, bytes, 0, numSamples * 2)
  }

  override def latency: Int =
    audioOps.getAudioDeviceLatency(deviceHandle)

  override def setVolume(volume: Volume): Unit =
    audioOps.setAudioDeviceVolume(deviceHandle, volume.toFloat)

  override def pause(): Unit =
    audioOps.pauseAudioDevice(deviceHandle)

  override def resume(): Unit =
    audioOps.resumeAudioDevice(deviceHandle)

  override def close(): Unit =
    audioOps.disposeAudioDevice(deviceHandle)
}
