/*
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Source: backends/gdx-backend-headless/.../mock/audio/MockMusic.java
 *   Renames: MockMusic -> NoopMusic, dispose() -> close(), setLooping/isLooping -> looping/looping_=,
 *     setVolume/getVolume -> volume/volume_=, setPosition/getPosition -> position/position_=,
 *     isPlaying -> playing, setOnCompletionListener -> onComplete (Music => Unit SAM)
 *   Convention: tracks looping/volume/position state (Java ignores all); uses opaque Volume/Pan/Position types
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 58
 * Covenant-baseline-methods: NoopMusic,_listener,_looping,_position,_volume,close,looping,looping_,onComplete,pause,play,playing,position,position_,setPan,stop,volume,volume_
 * Covenant-source-reference: backends/gdx-backend-headless/src/com/badlogic/gdx/backends/headless/mock/audio/MockMusic.java
 *   Source: backends/gdx-backend-headless/.../mock/audio/MockMusic.java
 *   Renames: MockMusic -> NoopMusic, dispose() -> close(), setLooping/isLooping -> looping/looping_=,
 *   Convention: tracks looping/volume/position state (Java ignores all); uses opaque Volume/Pan/Position types
 *   Idiom: split packages
 *   Audited: 2026-03-03
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 58
 * Covenant-baseline-methods: NoopMusic,_listener,_looping,_position,_volume,close,looping,looping_,onComplete,pause,play,playing,position,position_,setPan,stop,volume,volume_
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package noop

import sge.audio.{ Pan, Position, Volume }
import scala.annotation.nowarn

/** A no-op [[sge.audio.Music]] implementation for headless/testing use. Tracks looping/volume/position state but never actually plays audio.
  */
final class NoopMusic extends audio.Music {

  private var _looping:  Boolean  = false
  private var _volume:   Volume   = Volume.min
  private var _position: Position = Position.unsafeMake(0.0f)
  @nowarn("msg=not read") // noop implementation, listener stored for potential future use
  private var _listener: audio.Music => Unit = _ => {}

  override def play(): Unit = {}

  override def pause(): Unit = {}

  override def stop(): Unit = {}

  override def playing: Boolean = false

  override def looping: Boolean = _looping

  override def looping_=(isLooping: Boolean): Unit =
    _looping = isLooping

  override def volume: Volume = _volume

  override def volume_=(volume: Volume): Unit =
    _volume = volume

  override def setPan(pan: Pan, volume: Volume): Unit = {}

  override def position: Position = _position

  override def position_=(position: Position): Unit =
    _position = position

  override def onComplete(listener: audio.Music => Unit): Unit =
    _listener = listener

  override def close(): Unit = {}
}
