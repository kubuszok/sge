// SGE — Android audio recorder implementation
//
// Uses android.media.AudioRecord for microphone PCM input.
// Requires RECORD_AUDIO permission.
//
// Migration notes:
//   Source:  com.badlogic.gdx.backends.android.AndroidAudioRecorder
//   Renames: AndroidAudioRecorder → AndroidAudioRecorderImpl
//   Convention: ops interface pattern; _root_.android.* imports
//   Audited: 2026-03-08

package sge
package platform
package android

import _root_.android.media.{AudioFormat, AudioRecord, MediaRecorder}

class AndroidAudioRecorderImpl(samplingRate: Int, isMono: Boolean) extends AudioRecorderOps {

  private val channelConfig = if (isMono) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO
  private val minBufferSize = AudioRecord.getMinBufferSize(samplingRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT)

  @SuppressWarnings(Array("MissingPermission"))
  private val recorder: AudioRecord =
    new AudioRecord(MediaRecorder.AudioSource.MIC, samplingRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT, minBufferSize)

  if (recorder.getState != AudioRecord.STATE_INITIALIZED)
    throw new RuntimeException("Unable to initialize AudioRecorder.\nDo you have the RECORD_AUDIO permission?")

  recorder.startRecording()

  override def read(numSamples: Int): Array[Short] = {
    val samples = new Array[Short](numSamples)
    var read = 0
    while (read != numSamples)
      read += recorder.read(samples, read, numSamples - read)
    samples
  }

  override def dispose(): Unit = {
    recorder.stop()
    recorder.release()
  }
}
