// SGE — Android audio recorder operations interface
//
// Self-contained (JDK types only). Provides PCM audio input (microphone).
// Implemented in sge-jvm-platform-android using android.media.AudioRecord.

package sge
package platform
package android

/** PCM audio input (microphone recording). Uses only JDK types. */
trait AudioRecorderOps {

  /** Reads 16-bit PCM samples from the microphone.
    * @param numSamples
    *   number of samples to read
    * @return
    *   array of 16-bit PCM samples
    */
  def read(numSamples: Int): Array[Short]

  /** Releases the recorder's resources. */
  def dispose(): Unit
}
