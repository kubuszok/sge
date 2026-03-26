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
 *   Audited: 2026-03-08
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
  simultaneousSources:         Int,
  deviceBufferSize:            Int,
  deviceBufferCount:           Int,
  private val audioOps:        AudioOps,
  private val recorderFactory: (Int, Boolean) => AudioRecorder = (_, _) => throw new UnsupportedOperationException("AudioRecorder not available on this platform")
) extends DesktopAudio {

  private val engineHandle: Long    = audioOps.initEngine(simultaneousSources, deviceBufferSize, deviceBufferCount)
  private val noDevice:     Boolean = engineHandle == 0L

  private val musicInstances: ArrayBuffer[MiniaudioMusic] = ArrayBuffer.empty
  private val soundInstances: ArrayBuffer[MiniaudioSound] = ArrayBuffer.empty

  // ─── Audio trait ────────────────────────────────────────────────────

  override def newAudioDevice(samplingRate: Int, isMono: Boolean): AudioDevice =
    if (noDevice) sge.noop.NoopAudioDevice(isMono)
    else {
      val deviceHandle = audioOps.createAudioDevice(engineHandle, samplingRate, isMono)
      if (deviceHandle == 0L) {
        throw sge.utils.SgeError.AudioError("Could not create audio device")
      }
      DesktopAudioDevice(deviceHandle, isMono, audioOps)
    }

  override def newAudioRecorder(samplingRate: Int, isMono: Boolean): AudioRecorder =
    recorderFactory(samplingRate, isMono)

  override def newSound(fileHandle: files.FileHandle): Sound =
    if (noDevice) sge.noop.NoopSound()
    else {
      val pcmData = fileHandle.readBytes()
      // Assume 16-bit stereo 44100Hz — the native engine will decode the actual format from the data.
      // In practice, createSound receives raw file bytes and the native side determines format.
      val soundHandle = audioOps.createSound(engineHandle, pcmData, 2, 16, 44100)
      if (soundHandle == 0L) {
        throw sge.utils.SgeError.AudioError(s"Could not load sound: ${fileHandle.name}")
      }
      val sound = MiniaudioSound(this, soundHandle, audioOps)
      soundInstances += sound
      sound
    }

  override def newMusic(file: files.FileHandle): Music =
    if (noDevice) sge.noop.NoopMusic()
    else {
      // Miniaudio needs a real filesystem path. For Internal/Classpath files that
      // may be inside a JAR, resolve via file() first; if that doesn't exist on
      // disk, extract from the classpath to a temporary file.
      val resolvedPath = {
        val f = file.file
        if (f.exists()) f.getAbsolutePath
        else {
          val tmp = java.io.File.createTempFile("sge-music-", "-" + file.name)
          tmp.deleteOnExit()
          val in  = file.read()
          val out = new java.io.FileOutputStream(tmp)
          try {
            val buf = new Array[Byte](8192)
            var n   = in.read(buf)
            while (n >= 0) { out.write(buf, 0, n); n = in.read(buf) }
          } finally { in.close(); out.close() }
          tmp.getAbsolutePath
        }
      }
      val musicHandle = audioOps.createMusic(engineHandle, resolvedPath)
      if (musicHandle == 0L) {
        throw sge.utils.SgeError.AudioError(s"Could not load music: ${file.name}")
      }
      val music = MiniaudioMusic(this, musicHandle, audioOps)
      musicInstances += music
      music
    }

  override def switchOutputDevice(deviceIdentifier: Nullable[String]): Boolean =
    if (noDevice) false
    else {
      @nowarn("msg=deprecated") // orNull needed at FFI boundary — null means "default device"
      val name = deviceIdentifier.orNull
      audioOps.switchOutputDevice(engineHandle, name)
    }

  override def availableOutputDevices: Array[String] =
    if (noDevice) Array.empty[String]
    else audioOps.getAvailableOutputDevices(engineHandle)

  // ─── DesktopAudio ──────────────────────────────────────────────────

  override def update(): Unit =
    if (!noDevice) audioOps.updateEngine(engineHandle)

  override def close(): Unit =
    if (!noDevice) {
      // Snapshot before iterating — close() calls forgetMusic/forgetSound which mutates the buffer
      val music  = musicInstances.toList
      val sounds = soundInstances.toList
      musicInstances.clear()
      soundInstances.clear()
      music.foreach(_.close())
      sounds.foreach(_.close())
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
    * @param recorderFactory
    *   factory for creating [[AudioRecorder]] instances (platform-specific)
    */
  def apply(audioOps: AudioOps, recorderFactory: (Int, Boolean) => AudioRecorder = (_, _) => throw new UnsupportedOperationException("AudioRecorder not available on this platform")): MiniaudioEngine =
    new MiniaudioEngine(16, 512, 9, audioOps, recorderFactory)
}
