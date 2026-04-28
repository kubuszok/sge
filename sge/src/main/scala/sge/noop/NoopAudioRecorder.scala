/*
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Source: backends/gdx-backend-headless/.../mock/audio/MockAudioRecorder.java
 *   Renames: MockAudioRecorder -> NoopAudioRecorder, dispose() -> close()
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 19
 * Covenant-baseline-methods: NoopAudioRecorder,close,read
 * Covenant-source-reference: backends/gdx-backend-headless/src/com/badlogic/gdx/backends/headless/mock/audio/MockAudioRecorder.java
 *   Source: backends/gdx-backend-headless/.../mock/audio/MockAudioRecorder.java
 *   Renames: MockAudioRecorder -> NoopAudioRecorder, dispose() -> close()
 *   Idiom: split packages
 *   Audited: 2026-03-03
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 19
 * Covenant-baseline-methods: NoopAudioRecorder,close,read
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package noop

/** A no-op [[sge.audio.AudioRecorder]] implementation for headless/testing use. */
final class NoopAudioRecorder extends audio.AudioRecorder {

  override def read(samples: Array[Short], offset: Int, numSamples: Int): Unit = {}

  override def close(): Unit = {}
}
