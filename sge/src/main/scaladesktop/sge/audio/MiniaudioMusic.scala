/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../audio/OpenALMusic.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: OpenALMusic -> MiniaudioMusic
 *   Convention: OpenAL streaming buffer management -> miniaudio engine music via AudioOps FFI
 *   Convention: opaque types (Volume, Pitch, Pan, Position) used in public API
 *   Convention: OnCompletionListener SAM -> (Music => Unit) function type
 *   Idiom: split packages; Nullable for completion listener
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package audio

import sge.platform.AudioOps
import lowlevel.Nullable

/** A streaming music track played via the miniaudio engine.
  *
  * Unlike [[MiniaudioSound]], music is streamed from disk rather than fully loaded into memory. Only one instance plays at a time.
  *
  * @param engine
  *   the audio engine that owns this music
  * @param musicHandle
  *   the native miniaudio music handle
  * @param audioOps
  *   the audio FFI operations
  * @author
  *   Nathan Sweet (original implementation)
  */
class MiniaudioMusic private[sge] (
  private val engine:      MiniaudioEngine,
  private val musicHandle: Long,
  private val audioOps:    AudioOps
) extends Music {

  private var _onComplete: Nullable[Music => Unit] = Nullable.empty

  // Completion detection state (ISS-760). The shipped miniaudio provider libraries expose no
  // "at-end" symbol (ma_sound_at_end lives in the separate sge-native-providers repo and cannot
  // be added here), so natural end-of-stream is detected via a played -> stopped transition where
  // the cursor has reached the track duration. `playbackStarted` arms detection while the stream is
  // active; `completed` is a one-shot latch so the listener fires exactly once per playback.
  private var playbackStarted: Boolean = false
  private var completed:       Boolean = false

  override def play(): Unit = {
    // A fresh playback re-arms detection so a replayed track can complete again.
    completed = false
    playbackStarted = true
    audioOps.playMusic(musicHandle)
  }

  override def pause(): Unit =
    // A user pause leaves the cursor mid-track (position < duration), so it is not mistaken for a
    // natural end by update(); detection stays armed and resumes when play() is called again.
    audioOps.pauseMusic(musicHandle)

  override def stop(): Unit = {
    // A user stop rewinds the cursor to 0, distinguishing it from a natural end; disarm detection.
    playbackStarted = false
    audioOps.stopMusic(musicHandle)
  }

  override def playing: Boolean =
    audioOps.isMusicPlaying(musicHandle)

  override def looping: Boolean =
    audioOps.isMusicLooping(musicHandle)

  override def looping_=(isLooping: Boolean): Unit =
    audioOps.setMusicLooping(musicHandle, isLooping)

  override def volume: Volume =
    Volume.unsafeMake(audioOps.getMusicVolume(musicHandle))

  override def volume_=(volume: Volume): Unit =
    audioOps.setMusicVolume(musicHandle, volume.toFloat)

  override def setPan(pan: Pan, volume: Volume): Unit =
    audioOps.setMusicPan(musicHandle, pan.toFloat, volume.toFloat)

  override def position: Position =
    Position.unsafeMake(audioOps.getMusicPosition(musicHandle))

  override def position_=(position: Position): Unit =
    audioOps.setMusicPosition(musicHandle, position.toFloatSeconds)

  /** Returns the total duration of the music in seconds. */
  override def duration: Position =
    Position.unsafeMake(audioOps.getMusicDuration(musicHandle))

  override def onComplete(listener: Music => Unit): Unit =
    _onComplete = Nullable(listener)

  /** Called by the engine during update to fire completion callbacks. */
  private[sge] def fireOnComplete(): Unit =
    _onComplete.foreach(_(this))

  /** Drives completion detection for one frame, mirroring `OpenALMusic.update()`.
    *
    * Invoked once per frame by [[MiniaudioEngine.update]] (which is itself driven by the application loop's per-frame `audio.update()` tick, matching `OpenALLwjgl3Audio.update()`). When the stream
    * reaches its natural end — it was playing and is now stopped with the cursor at the track duration — this stops the track and fires the completion listener exactly once. A user stop() (cursor
    * rewound to 0) or pause() (cursor mid-track) is not a natural end and does not trigger completion; looping tracks never report stopped and so never complete.
    */
  private[sge] def update(): Unit =
    if (!completed) {
      if (audioOps.isMusicPlaying(musicHandle)) {
        playbackStarted = true
      } else if (playbackStarted) {
        val position = audioOps.getMusicPosition(musicHandle)
        val duration = audioOps.getMusicDuration(musicHandle)
        // duration 0/unknown => completion is NEVER detected here — a behavioral delta vs LibGDX,
        // whose OpenAL backend detects end by buffer exhaustion (duration-free). Closing this gap
        // needs an at-end symbol from the provider repo (see the mechanism note above).
        if (duration > 0f && position >= duration) {
          completed = true
          // Mirror OpenALMusic.update(): stop the exhausted stream, then notify the listener.
          stop()
          fireOnComplete()
        }
      }
    }

  override def close(): Unit = {
    audioOps.disposeMusic(musicHandle)
    engine.forgetMusic(this)
  }
}
