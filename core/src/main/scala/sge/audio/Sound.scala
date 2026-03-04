/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/audio/Sound.java
 * Original authors: badlogicgames@gmail.com
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: long return -> SoundId, long soundId param -> SoundId, Disposable -> Closeable
 *   Convention: Java interface -> Scala trait; raw float params replaced with opaque types
 *     (Volume, Pitch, Pan, SoundId); dispose() inherited via Closeable.close()
 *   Idiom: split packages
 *   Audited: 2026-03-03
 */
package sge
package audio

/** <p> A Sound is a short audio clip that can be played numerous times in parallel. It's completely loaded into memory so only load small audio files. Call the {@link #dispose()} method when you're
  * done using the Sound. </p>
  *
  * <p> Sound instances are created via a call to {@link Audio#newSound(FileHandle)} . </p>
  *
  * <p> Calling the {@link #play()} or {@link #play(float)} method will return a long which is an id to that instance of the sound. You can use this id to modify the playback of that sound instance.
  * </p>
  *
  * <p> <b>Note</b>: any values provided will not be clamped, it is the developer's responsibility to do so </p>
  *
  * @author
  *   badlogicgames@gmail.com (original implementation)
  */
trait Sound extends java.io.Closeable {

  /** Plays the sound. If the sound is already playing, it will be played again, concurrently.
    * @return
    *   the id of the sound instance if successful, or -1 on failure.
    */
  def play(): SoundId

  /** Plays the sound. If the sound is already playing, it will be played again, concurrently.
    * @param volume
    *   the volume in the range [0,1]
    * @return
    *   the id of the sound instance if successful, or -1 on failure.
    */
  def play(volume: Volume): SoundId

  /** Plays the sound. If the sound is already playing, it will be played again, concurrently. Note that (with the exception of the web backend) panning only works for mono sounds, not for stereo
    * sounds!
    * @param volume
    *   the volume in the range [0,1]
    * @param pitch
    *   the pitch multiplier, 1 == default, >1 == faster, <1 == slower, the value has to be between 0.5 and 2.0
    * @param pan
    *   panning in the range -1 (full left) to 1 (full right). 0 is center position.
    * @return
    *   the id of the sound instance if successful, or -1 on failure.
    */
  def play(volume: Volume, pitch: Pitch, pan: Pan): SoundId

  /** Plays the sound, looping. If the sound is already playing, it will be played again, concurrently.
    * @return
    *   the id of the sound instance if successful, or -1 on failure.
    */
  def loop(): SoundId

  /** Plays the sound, looping. If the sound is already playing, it will be played again, concurrently. You need to stop the sound via a call to {@link #stop(long)} using the returned id.
    * @param volume
    *   the volume in the range [0, 1]
    * @return
    *   the id of the sound instance if successful, or -1 on failure.
    */
  def loop(volume: Volume): SoundId

  /** Plays the sound, looping. If the sound is already playing, it will be played again, concurrently. You need to stop the sound via a call to {@link #stop(long)} using the returned id. Note that
    * (with the exception of the web backend) panning only works for mono sounds, not for stereo sounds!
    * @param volume
    *   the volume in the range [0,1]
    * @param pitch
    *   the pitch multiplier, 1 == default, >1 == faster, <1 == slower, the value has to be between 0.5 and 2.0
    * @param pan
    *   panning in the range -1 (full left) to 1 (full right). 0 is center position.
    * @return
    *   the id of the sound instance if successful, or -1 on failure.
    */
  def loop(volume: Volume, pitch: Pitch, pan: Pan): SoundId

  /** Stops playing all instances of this sound. */
  def stop(): Unit

  /** Pauses all instances of this sound. */
  def pause(): Unit

  /** Resumes all paused instances of this sound. */
  def resume(): Unit

  /** Stops the sound instance with the given id as returned by {@link #play()} or {@link #play(float)} . If the sound is no longer playing, this has no effect.
    * @param soundId
    *   the sound id
    */
  def stop(soundId: SoundId): Unit

  /** Pauses the sound instance with the given id as returned by {@link #play()} or {@link #play(float)} . If the sound is no longer playing, this has no effect.
    * @param soundId
    *   the sound id
    */
  def pause(soundId: SoundId): Unit

  /** Resumes the sound instance with the given id as returned by {@link #play()} or {@link #play(float)} . If the sound is not paused, this has no effect.
    * @param soundId
    *   the sound id
    */
  def resume(soundId: SoundId): Unit

  /** Sets the sound instance with the given id to be looping. If the sound is no longer playing this has no effect.s
    * @param soundId
    *   the sound id
    * @param looping
    *   whether to loop or not.
    */
  def setLooping(soundId: SoundId, looping: Boolean): Unit

  /** Changes the pitch multiplier of the sound instance with the given id as returned by {@link #play()} or {@link #play(float)} . If the sound is no longer playing, this has no effect.
    * @param soundId
    *   the sound id
    * @param pitch
    *   the pitch multiplier, 1 == default, >1 == faster, <1 == slower, the value has to be between 0.5 and 2.0
    */
  def setPitch(soundId: SoundId, pitch: Pitch): Unit

  /** Changes the volume of the sound instance with the given id as returned by {@link #play()} or {@link #play(float)} . If the sound is no longer playing, this has no effect.
    * @param soundId
    *   the sound id
    * @param volume
    *   the volume in the range 0 (silent) to 1 (max volume).
    */
  def setVolume(soundId: SoundId, volume: Volume): Unit

  /** Sets the panning and volume of the sound instance with the given id as returned by {@link #play()} or {@link #play(float)} . If the sound is no longer playing, this has no effect. Note that
    * panning only works for mono sounds, not for stereo sounds!
    * @param soundId
    *   the sound id
    * @param pan
    *   panning in the range -1 (full left) to 1 (full right). 0 is center position.
    * @param volume
    *   the volume in the range [0,1].
    */
  def setPan(soundId: SoundId, pan: Pan, volume: Volume): Unit
}
