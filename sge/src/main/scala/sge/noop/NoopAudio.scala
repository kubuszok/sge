/*
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Source: backends/gdx-backend-headless/.../mock/audio/MockAudio.java
 *   Renames: MockAudio -> NoopAudio, mock.audio -> noop package
 *   Convention: dispose() removed (Audio trait uses no Closeable); NoopAudioDevice(isMono) passes ctor arg (Java ignores it)
 *   Idiom: Nullable (switchOutputDevice param), split packages
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 35
 * Covenant-baseline-methods: NoopAudio,availableOutputDevices,newAudioDevice,newAudioRecorder,newMusic,newSound,switchOutputDevice
 * Covenant-source-reference: backends/gdx-backend-headless/src/com/badlogic/gdx/backends/headless/mock/audio/MockAudio.java
 *   Source: backends/gdx-backend-headless/.../mock/audio/MockAudio.java
 *   Renames: MockAudio -> NoopAudio, mock.audio -> noop package
 *   Convention: dispose() removed (Audio trait uses no Closeable); NoopAudioDevice(isMono) passes ctor arg (Java ignores it)
 *   Idiom: Nullable (switchOutputDevice param), split packages
 *   Audited: 2026-03-03
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 35
 * Covenant-baseline-methods: NoopAudio,availableOutputDevices,newAudioDevice,newAudioRecorder,newMusic,newSound,switchOutputDevice
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 4c9b9758fbe5efe39126a52fee8f9d5fb4119b31
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
