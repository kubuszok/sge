// SGE — Android audio device operations interface
//
// Self-contained (JDK types only). Provides PCM audio output.
// Implemented in sge-jvm-platform-android using android.media.AudioTrack.

package sge
package platform
package android

/** PCM audio output device. Uses only JDK types.
  *
  * Writes raw PCM samples to the audio hardware.
  */
trait AudioDeviceOps {

  /** Whether this device outputs mono audio. */
  def isMono: Boolean

  /** Writes 16-bit PCM samples. For stereo, samples are interleaved (L, R, L, R, ...). */
  def writeSamples(samples: Array[Short], offset: Int, numSamples: Int): Unit

  /** Writes float PCM samples in [-1, 1]. For stereo, samples are interleaved. */
  def writeSamples(samples: Array[Float], offset: Int, numSamples: Int): Unit

  /** Sets the output volume. Range: [0, 1]. */
  def setVolume(volume: Float): Unit

  /** Pauses audio output. */
  def pause(): Unit

  /** Resumes audio output. */
  def resume(): Unit

  /** Releases the device's resources. */
  def dispose(): Unit
}
