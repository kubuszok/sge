/*
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Source: backends/gdx-backend-headless/.../mock/audio/MockAudio.java
 *   Renames: MockAudio -> NoopAudio, mock.audio -> noop package
 *   Convention: dispose() removed (Audio trait uses no Closeable); NoopAudioDevice(isMono) passes ctor arg (Java ignores it)
 *   Idiom: Nullable (switchOutputDevice param), split packages
 *   Audited: 2026-03-03
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

  override def availableOutputDevices: Array[String] = Array.empty[String]
}
