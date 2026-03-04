/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/audio/Music.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: isPlaying -> playing, isLooping/setLooping -> looping/looping_=,
 *     getVolume/setVolume -> volume/volume_=, getPosition/setPosition -> position/position_=,
 *     setOnCompletionListener(OnCompletionListener) -> onComplete(Music => Unit),
 *     Disposable -> Closeable
 *   Convention: Java interface -> Scala trait; OnCompletionListener SAM replaced with function type;
 *     raw float params replaced with opaque types (Volume, Pan, Position)
 *   Idiom: split packages
 *   Audited: 2026-03-03
 */
package sge
package audio

/** <p> A Music instance represents a streamed audio file. The interface supports pausing, resuming and so on. When you are done with using the Music instance you have to dispose it via the
  * {@link #dispose()} method. </p>
  *
  * <p> Music instances are created via {@link Audio#newMusic(FileHandle)} . </p>
  *
  * <p> Music instances are automatically paused and resumed when an {@link Application} is paused or resumed. See {@link ApplicationListener} . </p>
  *
  * <p> <b>Note</b>: any values provided will not be clamped, it is the developer's responsibility to do so </p>
  *
  * @author
  *   mzechner (original implementation)
  */
trait Music extends java.io.Closeable {

  /** Starts the play back of the music stream. In case the stream was paused this will resume the play back. In case the music stream is finished playing this will restart the play back.
    */
  def play(): Unit

  /** Pauses the play back. If the music stream has not been started yet or has finished playing a call to this method will be ignored.
    */
  def pause(): Unit

  /** Stops a playing or paused Music instance. Next time play() is invoked the Music will start from the beginning. */
  def stop(): Unit

  /** @return whether this music stream is playing */
  def playing: Boolean

  /** @return whether the music stream is playing. */
  def looping: Boolean

  /** Sets whether the music stream is looping. This can be called at any time, whether the stream is playing.
    *
    * @param isLooping
    *   whether to loop the stream
    */
  def looping_=(isLooping: Boolean): Unit

  /** @return the volume of this music stream. */
  def volume: Volume

  /** Sets the volume of this music stream. The volume must be given in the range [0,1] with 0 being silent and 1 being the maximum volume.
    *
    * @param volume
    */
  def volume_=(volume: Volume): Unit

  /** Sets the panning and volume of this music stream.
    * @param pan
    *   panning in the range -1 (full left) to 1 (full right). 0 is center position.
    * @param volume
    *   the volume in the range [0,1].
    */
  def setPan(pan: Pan, volume: Volume): Unit

  /** Returns the playback position in seconds. */
  def position: Position

  /** Set the playback position in seconds. */
  def position_=(position: Position): Unit

  /** Register a callback to be invoked when the end of a music stream has been reached during playback.
    *
    * @param listener
    *   the callback that will be run.
    */
  def onComplete(listener: Music => Unit): Unit
}
