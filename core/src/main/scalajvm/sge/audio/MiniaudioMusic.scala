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
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package audio

import sge.platform.AudioOps
import sge.utils.Nullable

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

  override def play(): Unit =
    audioOps.playMusic(musicHandle)

  override def pause(): Unit =
    audioOps.pauseMusic(musicHandle)

  override def stop(): Unit =
    audioOps.stopMusic(musicHandle)

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
  def duration: Position =
    Position.unsafeMake(audioOps.getMusicDuration(musicHandle))

  override def onComplete(listener: Music => Unit): Unit =
    _onComplete = Nullable(listener)

  /** Called by the engine during update to fire completion callbacks. */
  private[sge] def fireOnComplete(): Unit =
    _onComplete.foreach(_(this))

  override def close(): Unit = {
    audioOps.disposeMusic(musicHandle)
    engine.forgetMusic(this)
  }
}
