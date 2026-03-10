/*
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Source: backends/gdx-backend-headless/.../mock/audio/MockAudioRecorder.java
 *   Renames: MockAudioRecorder -> NoopAudioRecorder, dispose() -> close()
 *   Idiom: split packages
 *   Audited: 2026-03-03
 */
package sge
package noop

/** A no-op [[sge.audio.AudioRecorder]] implementation for headless/testing use. */
final class NoopAudioRecorder extends audio.AudioRecorder {

  override def read(samples: Array[Short], offset: Int, numSamples: Int): Unit = {}

  override def close(): Unit = {}
}
