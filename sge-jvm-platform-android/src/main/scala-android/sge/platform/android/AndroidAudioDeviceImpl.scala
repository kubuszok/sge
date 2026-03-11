// SGE — Android audio device implementation
//
// Uses android.media.AudioTrack for raw PCM audio output.
//
// Migration notes:
//   Source:  com.badlogic.gdx.backends.android.AndroidAudioDevice
//   Renames: AndroidAudioDevice → AndroidAudioDeviceImpl
//   Convention: ops interface pattern; _root_.android.* imports
//   Audited: 2026-03-08

package sge
package platform
package android

import _root_.android.media.{ AudioAttributes, AudioFormat, AudioManager, AudioTrack }

class AndroidAudioDeviceImpl(samplingRate: Int, val isMono: Boolean) extends AudioDeviceOps {

  private val channelMask = if (isMono) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
  private val encoding    = AudioFormat.ENCODING_PCM_16BIT
  private val minSize     = AudioTrack.getMinBufferSize(samplingRate, channelMask, encoding)

  private val track: AudioTrack = {
    val attrs = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    val fmt   = new AudioFormat.Builder().setSampleRate(samplingRate).setChannelMask(channelMask).setEncoding(encoding).build()
    val t     = new AudioTrack(attrs, fmt, minSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
    t.play()
    t
  }

  private var buffer = new Array[Short](1024)

  override def writeSamples(samples: Array[Short], offset: Int, numSamples: Int): Unit = {
    var written = track.write(samples, offset, numSamples)
    while (written != numSamples)
      written += track.write(samples, offset + written, numSamples - written)
  }

  override def writeSamples(samples: Array[Float], offset: Int, numSamples: Int): Unit = {
    if (buffer.length < samples.length) buffer = new Array[Short](samples.length)
    var i     = offset
    var j     = 0
    val bound = offset + numSamples
    while (i < bound) {
      var fValue = samples(i)
      if (fValue > 1f) fValue = 1f
      if (fValue < -1f) fValue = -1f
      buffer(j) = (fValue * Short.MaxValue).toShort
      i += 1
      j += 1
    }
    var written = track.write(buffer, 0, numSamples)
    while (written != numSamples)
      written += track.write(buffer, written, numSamples - written)
  }

  override def setVolume(volume: Float): Unit =
    track.setVolume(volume)

  override def pause(): Unit =
    if (track.getPlayState == AudioTrack.PLAYSTATE_PLAYING) track.pause()

  override def resume(): Unit =
    if (track.getPlayState == AudioTrack.PLAYSTATE_PAUSED) track.play()

  override def dispose(): Unit = {
    track.stop()
    track.release()
  }
}
