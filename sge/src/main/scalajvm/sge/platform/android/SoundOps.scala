// SGE — Android sound operations interface
//
// Self-contained (JDK types only). Controls a loaded sound effect.
// Implemented in sge-jvm-platform-android using android.media.SoundPool.

package sge
package platform
package android

/** Operations for a loaded sound effect. Uses only JDK types.
  *
  * Sound IDs (Long) are stream identifiers returned by play/loop methods. A value of -1 indicates an error.
  */
trait SoundOps {

  /** Plays the sound at the given volume.
    * @return
    *   stream ID for further control, or -1 on error
    */
  def play(volume: Float): Long

  /** Plays the sound with volume, pitch, and pan.
    * @return
    *   stream ID, or -1 on error
    */
  def play(volume: Float, pitch: Float, pan: Float): Long

  /** Loops the sound at the given volume.
    * @return
    *   stream ID, or -1 on error
    */
  def loop(volume: Float): Long

  /** Loops the sound with volume, pitch, and pan.
    * @return
    *   stream ID, or -1 on error
    */
  def loop(volume: Float, pitch: Float, pan: Float): Long

  /** Stops all streams of this sound. */
  def stop(): Unit

  /** Stops a specific stream. */
  def stop(streamId: Long): Unit

  /** Pauses all streams of this sound. */
  def pause(): Unit

  /** Pauses a specific stream. */
  def pause(streamId: Long): Unit

  /** Resumes all paused streams of this sound. */
  def resume(): Unit

  /** Resumes a specific paused stream. */
  def resume(streamId: Long): Unit

  /** Sets the pitch of a specific stream. */
  def setPitch(streamId: Long, pitch: Float): Unit

  /** Sets the volume of a specific stream. */
  def setVolume(streamId: Long, volume: Float): Unit

  /** Sets whether a specific stream loops. */
  def setLooping(streamId: Long, looping: Boolean): Unit

  /** Sets the pan and volume of a specific stream. Pan: -1 (left) to 1 (right), 0 = center. */
  def setPan(streamId: Long, pan: Float, volume: Float): Unit

  /** Releases the sound's resources. */
  def dispose(): Unit
}
