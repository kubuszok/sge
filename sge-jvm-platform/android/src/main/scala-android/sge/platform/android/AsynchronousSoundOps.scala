// SGE — Asynchronous sound wrapper for Android
//
// Wraps a SoundOps instance, posting play/loop/stop operations to a
// background Handler thread to avoid blocking the main/GL thread.
// Sound IDs are mapped through a circular buffer.
//
// Migration notes:
//   Source: com.badlogic.gdx.backends.android.AsynchronousSound
//   Convention: decorator pattern on SoundOps; Android Handler for threading
//   Audited: 2026-03-08

package sge
package platform
package android

import _root_.android.os.Handler
import java.util.concurrent.atomic.AtomicInteger

/** Asynchronous decorator for [[SoundOps]] that posts playback operations to a background [[Handler]].
  *
  * Play/loop calls return a monotonically increasing sound number instead of the real stream ID. The real stream IDs are stored in a circular buffer so that subsequent stop/pause/resume/set* calls
  * can look up the real ID. Pause/resume (all) and dispose are called synchronously since they don't block.
  *
  * @param sound
  *   the underlying sound to delegate to
  * @param handler
  *   the background Handler to post operations on
  * @param soundIdsCountToSave
  *   the size of the circular ID buffer (typically max simultaneous streams)
  */
class AsynchronousSoundOps(
  private val sound:               SoundOps,
  private val handler:             Handler,
  private val soundIdsCountToSave: Int
) extends SoundOps {

  private val playedSoundsCounter: AtomicInteger = AtomicInteger(0)
  private val soundIds:            Array[Long]   = new Array[Long](soundIdsCountToSave)

  private def saveSoundId(soundNumber: Int, soundId: Long): Unit =
    soundIds(soundNumber % soundIdsCountToSave) = soundId

  private def getSoundId(soundId: Long): Long =
    soundIds(soundId.toInt % soundIdsCountToSave)

  override def play(volume: Float): Long = {
    val soundNumber = playedSoundsCounter.getAndIncrement()
    handler.post { () =>
      val realId = sound.play(volume)
      saveSoundId(soundNumber, realId)
    }
    soundNumber.toLong
  }

  override def play(volume: Float, pitch: Float, pan: Float): Long = {
    val soundNumber = playedSoundsCounter.getAndIncrement()
    handler.post { () =>
      val realId = sound.play(volume, pitch, pan)
      saveSoundId(soundNumber, realId)
    }
    soundNumber.toLong
  }

  override def loop(volume: Float): Long = {
    val soundNumber = playedSoundsCounter.getAndIncrement()
    handler.post { () =>
      val realId = sound.loop(volume)
      saveSoundId(soundNumber, realId)
    }
    soundNumber.toLong
  }

  override def loop(volume: Float, pitch: Float, pan: Float): Long = {
    val soundNumber = playedSoundsCounter.getAndIncrement()
    handler.post { () =>
      val realId = sound.loop(volume, pitch, pan)
      saveSoundId(soundNumber, realId)
    }
    soundNumber.toLong
  }

  override def stop(): Unit = handler.post(() => sound.stop())

  override def stop(streamId: Long): Unit = handler.post { () =>
    val realId = getSoundId(streamId)
    if (realId != -1L) sound.stop(realId)
  }

  // Pause/resume all are called synchronously (don't block)
  override def pause(): Unit = sound.pause()

  override def pause(streamId: Long): Unit = handler.post { () =>
    val realId = getSoundId(streamId)
    if (realId != -1L) sound.pause(realId)
  }

  override def resume(): Unit = sound.resume()

  override def resume(streamId: Long): Unit = handler.post { () =>
    val realId = getSoundId(streamId)
    if (realId != -1L) sound.resume(realId)
  }

  override def setPitch(streamId: Long, pitch: Float): Unit = handler.post { () =>
    val realId = getSoundId(streamId)
    if (realId != -1L) sound.setPitch(realId, pitch)
  }

  override def setVolume(streamId: Long, volume: Float): Unit = handler.post { () =>
    val realId = getSoundId(streamId)
    if (realId != -1L) sound.setVolume(realId, volume)
  }

  override def setLooping(streamId: Long, looping: Boolean): Unit = handler.post { () =>
    val realId = getSoundId(streamId)
    if (realId != -1L) sound.setLooping(realId, looping)
  }

  override def setPan(streamId: Long, pan: Float, volume: Float): Unit = handler.post { () =>
    val realId = getSoundId(streamId)
    if (realId != -1L) sound.setPan(realId, pan, volume)
  }

  override def dispose(): Unit = sound.dispose()
}
