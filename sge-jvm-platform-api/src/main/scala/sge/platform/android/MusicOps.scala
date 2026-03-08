// SGE — Android music operations interface
//
// Self-contained (JDK types only). Controls streaming music playback.
// Implemented in sge-jvm-platform-android using android.media.MediaPlayer.

package sge
package platform
package android

/** Operations for streaming music playback. Uses only JDK types.
  *
  * All position/duration values are in seconds (float).
  */
trait MusicOps {

  def play():  Unit
  def pause(): Unit
  def stop():  Unit

  def playing: Boolean
  def looping: Boolean

  def looping_=(isLooping: Boolean): Unit
  def volume_=(volume:     Float):   Unit
  def volume:                        Float

  /** Sets the pan and volume. Pan: -1 (left) to 1 (right), 0 = center. */
  def setPan(pan: Float, volume: Float): Unit

  /** Sets the playback position in seconds. */
  def position_=(positionSeconds: Float): Unit

  /** Returns the current playback position in seconds. */
  def position: Float

  /** Returns the total duration in seconds. */
  def duration: Float

  /** Sets a callback invoked when playback completes. The callback receives no arguments. */
  def onComplete(callback: () => Unit): Unit

  /** Releases the music's resources. */
  def dispose(): Unit

  /** Whether the music was playing before a pause-all. Used internally by the audio engine. */
  var wasPlaying: Boolean
}
