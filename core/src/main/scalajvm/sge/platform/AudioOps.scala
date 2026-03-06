/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   SGE-original platform abstraction for audio engine (miniaudio)
 *   Convention: trait defines FFI contract; JVM uses Panama, Native uses @extern
 *   Idiom: split packages
 */
package sge
package platform

/** Audio engine operations (miniaudio). Defines the FFI contract for initializing the audio system, playing sounds, and streaming music.
  *
  * Platform implementations:
  *   - JVM: Panama downcall handles to miniaudio shared library
  *   - Native: @extern C FFI to miniaudio
  *
  * Methods are added incrementally as backend files are ported.
  */
private[sge] trait AudioOps {

  // ─── Engine lifecycle ──────────────────────────────────────────────────

  /** Initializes the audio engine.
    * @param simultaneousSources
    *   maximum number of simultaneous sound sources
    * @param bufferSize
    *   audio device buffer size in samples
    * @param bufferCount
    *   number of audio device buffers
    * @return
    *   a native engine handle, or 0 on failure
    */
  def initEngine(simultaneousSources: Int, bufferSize: Int, bufferCount: Int): Long

  /** Shuts down the audio engine and frees all resources. */
  def shutdownEngine(engineHandle: Long): Unit

  /** Updates the audio engine state (call once per frame). */
  def updateEngine(engineHandle: Long): Unit

  // ─── Sound (fully loaded into memory) ──────────────────────────────────

  /** Creates a sound from PCM data loaded in memory.
    * @param engineHandle
    *   the audio engine handle
    * @param pcmData
    *   raw PCM audio data
    * @param channels
    *   number of audio channels
    * @param bitDepth
    *   bits per sample
    * @param sampleRate
    *   samples per second
    * @return
    *   a native sound handle, or 0 on failure
    */
  def createSound(engineHandle: Long, pcmData: Array[Byte], channels: Int, bitDepth: Int, sampleRate: Int): Long

  /** Disposes of a sound and frees its memory. */
  def disposeSound(soundHandle: Long): Unit

  /** Plays a sound.
    * @return
    *   an instance ID for controlling this playback, or -1 on failure
    */
  def playSound(soundHandle: Long, volume: Float, pitch: Float, pan: Float, loop: Boolean): Long

  /** Stops a sound instance. */
  def stopSound(instanceId: Long): Unit

  /** Pauses a sound instance. */
  def pauseSound(instanceId: Long): Unit

  /** Resumes a paused sound instance. */
  def resumeSound(instanceId: Long): Unit

  /** Stops all instances of a sound. */
  def stopAllInstances(soundHandle: Long): Unit

  /** Pauses all instances of a sound. */
  def pauseAllInstances(soundHandle: Long): Unit

  /** Resumes all paused instances of a sound. */
  def resumeAllInstances(soundHandle: Long): Unit

  /** Sets the volume of a sound instance. */
  def setSoundVolume(instanceId: Long, volume: Float): Unit

  /** Sets the pitch of a sound instance. */
  def setSoundPitch(instanceId: Long, pitch: Float): Unit

  /** Sets the pan and volume of a sound instance. */
  def setSoundPan(instanceId: Long, pan: Float, volume: Float): Unit

  /** Sets whether a sound instance loops. */
  def setSoundLooping(instanceId: Long, looping: Boolean): Unit

  // ─── Music (streamed from disk) ────────────────────────────────────────

  /** Creates a music stream from a file path.
    * @return
    *   a native music handle, or 0 on failure
    */
  def createMusic(engineHandle: Long, filePath: String): Long

  /** Disposes of a music stream. */
  def disposeMusic(musicHandle: Long): Unit

  /** Starts or resumes music playback. */
  def playMusic(musicHandle: Long): Unit

  /** Pauses music playback. */
  def pauseMusic(musicHandle: Long): Unit

  /** Stops music playback and rewinds. */
  def stopMusic(musicHandle: Long): Unit

  /** Returns true if the music is currently playing. */
  def isMusicPlaying(musicHandle: Long): Boolean

  /** Gets the music volume (0.0 to 1.0). */
  def getMusicVolume(musicHandle: Long): Float

  /** Sets the music volume (0.0 to 1.0). */
  def setMusicVolume(musicHandle: Long, volume: Float): Unit

  /** Sets the music pitch multiplier. */
  def setMusicPitch(musicHandle: Long, pitch: Float): Unit

  /** Sets the music pan (-1.0 left, 0.0 center, 1.0 right) and volume. */
  def setMusicPan(musicHandle: Long, pan: Float, volume: Float): Unit

  /** Returns true if the music is set to loop. */
  def isMusicLooping(musicHandle: Long): Boolean

  /** Sets whether the music loops. */
  def setMusicLooping(musicHandle: Long, looping: Boolean): Unit

  /** Sets the music playback position in seconds. */
  def setMusicPosition(musicHandle: Long, position: Float): Unit

  /** Gets the current playback position in seconds. */
  def getMusicPosition(musicHandle: Long): Float

  /** Gets the total duration of the music in seconds. */
  def getMusicDuration(musicHandle: Long): Float

  // ─── AudioDevice (raw PCM output) ─────────────────────────────────────

  /** Creates an audio device for raw PCM output.
    * @param engineHandle
    *   the audio engine handle
    * @param sampleRate
    *   samples per second
    * @param isMono
    *   true for mono, false for stereo
    * @return
    *   a native audio device handle, or 0 on failure
    */
  def createAudioDevice(engineHandle: Long, sampleRate: Int, isMono: Boolean): Long

  /** Disposes of an audio device. */
  def disposeAudioDevice(deviceHandle: Long): Unit

  /** Writes raw PCM bytes to an audio device. Blocks until the data has been queued.
    * @param deviceHandle
    *   the audio device handle
    * @param data
    *   raw PCM byte data (16-bit signed, little-endian)
    * @param offset
    *   byte offset into the data array
    * @param length
    *   number of bytes to write
    */
  def writeAudioDevice(deviceHandle: Long, data: Array[Byte], offset: Int, length: Int): Unit

  /** Sets the volume of an audio device. */
  def setAudioDeviceVolume(deviceHandle: Long, volume: Float): Unit

  /** Pauses an audio device. */
  def pauseAudioDevice(deviceHandle: Long): Unit

  /** Resumes an audio device. */
  def resumeAudioDevice(deviceHandle: Long): Unit

  /** Returns the latency of an audio device in samples. */
  def getAudioDeviceLatency(deviceHandle: Long): Int

  // ─── Output device ─────────────────────────────────────────────────────

  /** Returns the names of available audio output devices. */
  def getAvailableOutputDevices(engineHandle: Long): Array[String]

  /** Switches audio output to the specified device.
    * @param deviceName
    *   the device name, or null for default
    * @return
    *   true on success
    */
  def switchOutputDevice(engineHandle: Long, deviceName: String): Boolean
}
