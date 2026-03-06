/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../audio/OpenALLwjgl3Audio.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: OpenALLwjgl3Audio -> MiniaudioEngine
 *   Convention: OpenAL context/device/source management -> miniaudio engine via AudioOps FFI
 *   Convention: OpenALSound -> MiniaudioSound, OpenALMusic -> MiniaudioMusic
 *   Convention: BiFunction<Audio,FileHandle,Sound> extensionToSoundClass -> file-extension dispatch in createSound
 *   Convention: Source pool (idleSources, soundIdToSource, sourceToSoundId) -> native engine handles all instance tracking
 *   Convention: Observer thread for device reconnection -> deferred to native AudioOps
 *   Idiom: split packages; Nullable; DynamicArray for music list
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package audio

import sge.platform.AudioOps
import sge.utils.Nullable

import scala.annotation.nowarn
import scala.collection.mutable.ArrayBuffer

/** Desktop audio engine backed by miniaudio.
  *
  * Manages the lifecycle of sound effects and music streams. The native miniaudio engine is initialized on construction and shut down on [[close]].
  *
  * @param simultaneousSources
  *   maximum number of simultaneous sound sources
  * @param deviceBufferSize
  *   audio device buffer size in samples
  * @param deviceBufferCount
  *   number of audio device buffers
  * @param audioOps
  *   the audio FFI operations
  * @author
  *   Nathan Sweet (original implementation)
  */
class MiniaudioEngine private[sge] (
  simultaneousSources:  Int,
  deviceBufferSize:     Int,
  deviceBufferCount:    Int,
  private val audioOps: AudioOps
) extends DesktopAudio {

  private val engineHandle: Long    = audioOps.initEngine(simultaneousSources, deviceBufferSize, deviceBufferCount)
  private val noDevice:     Boolean = engineHandle == 0L

  private val musicInstances: ArrayBuffer[MiniaudioMusic] = ArrayBuffer.empty
  private val soundInstances: ArrayBuffer[MiniaudioSound] = ArrayBuffer.empty

  // ─── Audio trait ────────────────────────────────────────────────────

  override def newAudioDevice(samplingRate: Int, isMono: Boolean): AudioDevice = {
    if (noDevice) return sge.noop.NoopAudioDevice(isMono)
    val deviceHandle = audioOps.createAudioDevice(engineHandle, samplingRate, isMono)
    if (deviceHandle == 0L) {
      throw sge.utils.SgeError.AudioError("Could not create audio device")
    }
    DesktopAudioDevice(deviceHandle, isMono, audioOps)
  }

  override def newAudioRecorder(samplingRate: Int, isMono: Boolean): AudioRecorder =
    // TODO: implement when AudioRecorder backend is ported
    throw new UnsupportedOperationException("AudioRecorder not yet implemented for miniaudio backend")

  override def newSound(fileHandle: files.FileHandle): Sound = {
    if (noDevice) return sge.noop.NoopSound()
    val pcmData = fileHandle.readBytes()
    // Assume 16-bit stereo 44100Hz — the native engine will decode the actual format from the data.
    // In practice, createSound receives raw file bytes and the native side determines format.
    val soundHandle = audioOps.createSound(engineHandle, pcmData, 2, 16, 44100)
    if (soundHandle == 0L) {
      throw sge.utils.SgeError.AudioError(s"Could not load sound: ${fileHandle.name()}")
    }
    val sound = MiniaudioSound(this, soundHandle, audioOps)
    soundInstances += sound
    sound
  }

  override def newMusic(file: files.FileHandle): Music = {
    if (noDevice) return sge.noop.NoopMusic()
    val musicHandle = audioOps.createMusic(engineHandle, file.path())
    if (musicHandle == 0L) {
      throw sge.utils.SgeError.AudioError(s"Could not load music: ${file.name()}")
    }
    val music = MiniaudioMusic(this, musicHandle, audioOps)
    musicInstances += music
    music
  }

  override def switchOutputDevice(deviceIdentifier: Nullable[String]): Boolean = {
    if (noDevice) return false
    @nowarn("msg=deprecated") // orNull needed at FFI boundary — null means "default device"
    val name = deviceIdentifier.orNull
    audioOps.switchOutputDevice(engineHandle, name)
  }

  override def getAvailableOutputDevices: Array[String] = {
    if (noDevice) return Array.empty[String]
    audioOps.getAvailableOutputDevices(engineHandle)
  }

  // ─── DesktopAudio ──────────────────────────────────────────────────

  override def update(): Unit = {
    if (noDevice) return
    audioOps.updateEngine(engineHandle)
  }

  override def close(): Unit = {
    if (noDevice) return
    // Dispose all tracked music and sounds
    musicInstances.foreach(_.close())
    musicInstances.clear()
    soundInstances.foreach(_.close())
    soundInstances.clear()
    audioOps.shutdownEngine(engineHandle)
  }

  // ─── Internal bookkeeping ──────────────────────────────────────────

  private[sge] def forgetSound(sound: MiniaudioSound): Unit =
    soundInstances -= sound

  private[sge] def forgetMusic(music: MiniaudioMusic): Unit =
    musicInstances -= music
}

object MiniaudioEngine {

  /** Creates a new MiniaudioEngine with default settings.
    * @param audioOps
    *   the audio FFI operations
    */
  def apply(audioOps: AudioOps): MiniaudioEngine =
    new MiniaudioEngine(16, 512, 9, audioOps)
}
