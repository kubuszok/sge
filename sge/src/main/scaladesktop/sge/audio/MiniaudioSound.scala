/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../audio/OpenALSound.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: OpenALSound -> MiniaudioSound
 *   Convention: OpenAL source/buffer -> miniaudio engine sound via AudioOps FFI
 *   Convention: opaque types (Volume, Pitch, Pan, SoundId) used in public API
 *   Idiom: split packages
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package audio

import sge.platform.AudioOps

/** A short audio clip fully loaded into memory, played via the miniaudio engine.
  *
  * Multiple instances can play concurrently. Each play/loop call returns a [[SoundId]] that can be used to control that specific instance.
  *
  * @param engine
  *   the audio engine that owns this sound
  * @param soundHandle
  *   the native miniaudio sound handle
  * @param audioOps
  *   the audio FFI operations
  * @author
  *   Nathan Sweet (original implementation)
  */
class MiniaudioSound private[sge] (
  private val engine:      MiniaudioEngine,
  private val soundHandle: Long,
  private val audioOps:    AudioOps
) extends Sound {

  override def play(): SoundId =
    SoundId(audioOps.playSound(soundHandle, 1.0f, 1.0f, 0.0f, false))

  override def play(volume: Volume): SoundId =
    SoundId(audioOps.playSound(soundHandle, volume.toFloat, 1.0f, 0.0f, false))

  override def play(volume: Volume, pitch: Pitch, pan: Pan): SoundId =
    SoundId(audioOps.playSound(soundHandle, volume.toFloat, pitch.toFloat, pan.toFloat, false))

  override def loop(): SoundId =
    SoundId(audioOps.playSound(soundHandle, 1.0f, 1.0f, 0.0f, true))

  override def loop(volume: Volume): SoundId =
    SoundId(audioOps.playSound(soundHandle, volume.toFloat, 1.0f, 0.0f, true))

  override def loop(volume: Volume, pitch: Pitch, pan: Pan): SoundId =
    SoundId(audioOps.playSound(soundHandle, volume.toFloat, pitch.toFloat, pan.toFloat, true))

  override def stop(): Unit =
    audioOps.stopAllInstances(soundHandle)

  override def pause(): Unit =
    audioOps.pauseAllInstances(soundHandle)

  override def resume(): Unit =
    audioOps.resumeAllInstances(soundHandle)

  override def stop(soundId: SoundId): Unit =
    audioOps.stopSound(soundId.toLong)

  override def pause(soundId: SoundId): Unit =
    audioOps.pauseSound(soundId.toLong)

  override def resume(soundId: SoundId): Unit =
    audioOps.resumeSound(soundId.toLong)

  override def setLooping(soundId: SoundId, looping: Boolean): Unit =
    audioOps.setSoundLooping(soundId.toLong, looping)

  override def setPitch(soundId: SoundId, pitch: Pitch): Unit =
    audioOps.setSoundPitch(soundId.toLong, pitch.toFloat)

  override def setVolume(soundId: SoundId, volume: Volume): Unit =
    audioOps.setSoundVolume(soundId.toLong, volume.toFloat)

  override def setPan(soundId: SoundId, pan: Pan, volume: Volume): Unit =
    audioOps.setSoundPan(soundId.toLong, pan.toFloat, volume.toFloat)

  override def close(): Unit = {
    audioOps.disposeSound(soundHandle)
    engine.forgetSound(this)
  }
}
