/*
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package noop

import sge.utils.Nullable

/** A no-op [[sge.Audio]] implementation for headless/testing use. Returns noop instances for all audio resource factory methods.
  */
class NoopAudio extends Audio {

  override def newAudioDevice(samplingRate: Int, isMono: Boolean): audio.AudioDevice =
    NoopAudioDevice(isMono)

  override def newAudioRecorder(samplingRate: Int, isMono: Boolean): audio.AudioRecorder =
    NoopAudioRecorder()

  override def newSound(fileHandle: files.FileHandle): audio.Sound =
    NoopSound()

  override def newMusic(file: files.FileHandle): audio.Music =
    NoopMusic()

  override def switchOutputDevice(deviceIdentifier: Nullable[String]): Boolean = true

  override def getAvailableOutputDevices: Array[String] = Array.empty[String]
}
