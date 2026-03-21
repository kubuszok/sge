// SGE — Android music operations implementation
//
// Uses android.media.MediaPlayer for streaming music playback.
//
// Migration notes:
//   Source:  com.badlogic.gdx.backends.android.AndroidMusic
//   Renames: AndroidMusic → AndroidMusicOpsImpl
//   Convention: ops interface pattern; _root_.android.* imports; completion callback uses Runnable
//   Audited: 2026-03-08

package sge
package platform
package android

import _root_.android.media.MediaPlayer

class AndroidMusicOpsImpl(
  private val onDispose:    AndroidMusicOpsImpl => Unit,
  private val postRunnable: Runnable => Unit,
  private var player:       MediaPlayer | Null
) extends MusicOps
    with MediaPlayer.OnCompletionListener {

  private var isPrepared = true
  var wasPlaying:                 Boolean             = false
  private var _volume:            Float               = 1f
  private var completionCallback: (() => Unit) | Null = null

  if (player != null) {
    player.nn.setOnCompletionListener(this)
  }

  override def play(): Unit = {
    val p = player
    if (p == null) return
    try {
      if (!isPrepared) {
        p.prepare()
        isPrepared = true
      }
      p.start()
    } catch {
      case _: (IllegalStateException | java.io.IOException) => ()
    }
  }

  override def pause(): Unit = {
    val p = player
    if (p == null) return
    try
      if (p.isPlaying) p.pause()
    catch {
      case _: IllegalStateException => ()
    }
    wasPlaying = false
  }

  override def stop(): Unit = {
    val p = player
    if (p == null) return
    p.stop()
    isPrepared = false
  }

  override def playing: Boolean = {
    val p = player
    if (p == null) return false
    try p.isPlaying
    catch { case _: IllegalStateException => false }
  }

  override def looping: Boolean = {
    val p = player
    if (p == null) return false
    try p.isLooping
    catch { case _: IllegalStateException => false }
  }

  override def looping_=(isLooping: Boolean): Unit = {
    val p = player
    if (p != null) p.setLooping(isLooping)
  }

  override def volume_=(v: Float): Unit = {
    val p = player
    if (p != null) {
      p.setVolume(v, v)
      _volume = v
    }
  }

  override def volume: Float = _volume

  override def setPan(pan: Float, vol: Float): Unit = {
    val p = player
    if (p == null) return
    var left  = vol
    var right = vol
    if (pan < 0) right *= (1 - Math.abs(pan))
    else if (pan > 0) left *= (1 - Math.abs(pan))
    p.setVolume(left, right)
    _volume = vol
  }

  override def position_=(positionSeconds: Float): Unit = {
    val p = player
    if (p == null) return
    try {
      if (!isPrepared) {
        p.prepare()
        isPrepared = true
      }
      p.seekTo((positionSeconds * 1000).toInt)
    } catch {
      case _: (IllegalStateException | java.io.IOException) => ()
    }
  }

  override def position: Float = {
    val p = player
    if (p == null) 0f
    else p.getCurrentPosition / 1000f
  }

  override def duration: Float = {
    val p = player
    if (p == null) 0f
    else p.getDuration / 1000f
  }

  override def onComplete(callback: () => Unit): Unit =
    completionCallback = callback

  override def dispose(): Unit = {
    val p = player
    if (p == null) return
    try p.release()
    catch { case _: Throwable => () }
    finally {
      player = null
      completionCallback = null
      onDispose(this)
    }
  }

  // MediaPlayer.OnCompletionListener
  override def onCompletion(mp: MediaPlayer): Unit = {
    val cb = completionCallback
    if (cb != null) {
      postRunnable { () =>
        val cb2 = completionCallback
        if (cb2 != null) cb2()
      }
    }
  }
}
