/*
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Source: backends/gdx-backend-headless/.../mock/audio/MockAudioDevice.java
 *   Renames: MockAudioDevice -> NoopAudioDevice, dispose() -> close()
 *   Convention: isMono is constructor val (Java returns hardcoded false); volume uses opaque Volume type
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 33
 * Covenant-baseline-methods: NoopAudioDevice,close,latency,pause,resume,setVolume,writeSamples
 * Covenant-source-reference: backends/gdx-backend-headless/mock/audio/MockAudioDevice.java
 * Covenant-verified: 2026-04-19
 */
package sge
package noop

import sge.audio.Volume

/** A no-op [[sge.audio.AudioDevice]] implementation for headless/testing use. All write and control methods are no-ops.
  */
final class NoopAudioDevice(override val isMono: Boolean) extends audio.AudioDevice {

  override def writeSamples(samples: Array[Short], offset: Int, numSamples: Int): Unit = {}

  override def writeSamples(samples: Array[Float], offset: Int, numSamples: Int): Unit = {}

  override def latency: Int = 0

  override def setVolume(volume: Volume): Unit = {}

  override def pause(): Unit = {}

  override def resume(): Unit = {}

  override def close(): Unit = {}
}
