// SGE — Android sound operations implementation
//
// Uses android.media.SoundPool for short sound effect playback.
//
// Migration notes:
//   Source:  com.badlogic.gdx.backends.android.AndroidSound
//   Renames: AndroidSound → AndroidSoundOpsImpl
//   Convention: ops interface pattern; _root_.android.* imports
//   Audited: 2026-03-08

package sge
package platform
package android

import _root_.android.media.{ AudioManager, SoundPool }

class AndroidSoundOpsImpl(soundPool: SoundPool, manager: AudioManager, soundId: Int) extends SoundOps {

  private val MaxStreams = 8
  // Ring buffer of active stream IDs (most recent first)
  private val streamIds   = new Array[Int](MaxStreams)
  private var streamCount = 0

  private def pushStream(id: Int): Unit =
    if (streamCount == MaxStreams) {
      // Shift all elements left, dropping the oldest
      System.arraycopy(streamIds, 0, streamIds, 0, MaxStreams - 1)
      streamIds(MaxStreams - 1) = id
    } else {
      // Insert at end
      streamIds(streamCount) = id
      streamCount += 1
    }

  private def panToVolumes(pan: Float, volume: Float): (Float, Float) = {
    var left  = volume
    var right = volume
    if (pan < 0) right *= (1 - Math.abs(pan))
    else if (pan > 0) left *= (1 - Math.abs(pan))
    (left, right)
  }

  override def play(volume: Float): Long = {
    val streamId = soundPool.play(soundId, volume, volume, 1, 0, 1f)
    if (streamId == 0) return -1L
    pushStream(streamId)
    streamId.toLong
  }

  override def play(volume: Float, pitch: Float, pan: Float): Long = {
    val (left, right) = panToVolumes(pan, volume)
    val streamId      = soundPool.play(soundId, left, right, 1, 0, pitch)
    if (streamId == 0) return -1L
    pushStream(streamId)
    streamId.toLong
  }

  override def loop(volume: Float): Long = {
    val streamId = soundPool.play(soundId, volume, volume, 2, -1, 1f)
    if (streamId == 0) return -1L
    pushStream(streamId)
    streamId.toLong
  }

  override def loop(volume: Float, pitch: Float, pan: Float): Long = {
    val (left, right) = panToVolumes(pan, volume)
    val streamId      = soundPool.play(soundId, left, right, 2, -1, pitch)
    if (streamId == 0) return -1L
    pushStream(streamId)
    streamId.toLong
  }

  override def stop(): Unit = {
    var i = 0
    while (i < streamCount) {
      soundPool.stop(streamIds(i))
      i += 1
    }
  }

  override def stop(streamId: Long): Unit =
    soundPool.stop(streamId.toInt)

  override def pause(): Unit =
    soundPool.autoPause()

  override def pause(streamId: Long): Unit =
    soundPool.pause(streamId.toInt)

  override def resume(): Unit =
    soundPool.autoResume()

  override def resume(streamId: Long): Unit =
    soundPool.resume(streamId.toInt)

  override def setPitch(streamId: Long, pitch: Float): Unit =
    soundPool.setRate(streamId.toInt, pitch)

  override def setVolume(streamId: Long, volume: Float): Unit =
    soundPool.setVolume(streamId.toInt, volume, volume)

  override def setLooping(streamId: Long, looping: Boolean): Unit = {
    val id = streamId.toInt
    soundPool.pause(id)
    soundPool.setLoop(id, if (looping) -1 else 0)
    soundPool.setPriority(id, if (looping) 2 else 1)
    soundPool.resume(id)
  }

  override def setPan(streamId: Long, pan: Float, volume: Float): Unit = {
    val (left, right) = panToVolumes(pan, volume)
    soundPool.setVolume(streamId.toInt, left, right)
  }

  override def dispose(): Unit =
    soundPool.unload(soundId)
}
