/*
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Source: backends/gdx-backend-headless/.../mock/audio/MockSound.java
 *   Renames: MockSound -> NoopSound, dispose() -> close()
 *   Convention: returns SoundId(0L) (Java returns 0L); uses opaque Volume/Pitch/Pan/SoundId types
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 53
 * Covenant-baseline-methods: NoopSound,close,loop,pause,play,resume,setLooping,setPan,setPitch,setVolume,stop
 * Covenant-source-reference: backends/gdx-backend-headless/mock/audio/MockSound.java
 * Covenant-verified: 2026-04-19
 */
package sge
package noop

import sge.audio.{ Pan, Pitch, SoundId, Volume }

/** A no-op [[sge.audio.Sound]] implementation for headless/testing use. All play/loop methods return `SoundId(0L)`, all other methods are no-ops.
  */
final class NoopSound extends audio.Sound {

  override def play(): SoundId = SoundId(0L)

  override def play(volume: Volume): SoundId = SoundId(0L)

  override def play(volume: Volume, pitch: Pitch, pan: Pan): SoundId = SoundId(0L)

  override def loop(): SoundId = SoundId(0L)

  override def loop(volume: Volume): SoundId = SoundId(0L)

  override def loop(volume: Volume, pitch: Pitch, pan: Pan): SoundId = SoundId(0L)

  override def stop(): Unit = {}

  override def pause(): Unit = {}

  override def resume(): Unit = {}

  override def stop(soundId: SoundId): Unit = {}

  override def pause(soundId: SoundId): Unit = {}

  override def resume(soundId: SoundId): Unit = {}

  override def setLooping(soundId: SoundId, looping: Boolean): Unit = {}

  override def setPitch(soundId: SoundId, pitch: Pitch): Unit = {}

  override def setVolume(soundId: SoundId, volume: Volume): Unit = {}

  override def setPan(soundId: SoundId, pan: Pan, volume: Volume): Unit = {}

  override def close(): Unit = {}
}
