/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../webaudio/WebAudioAPISound.java
 * Original authors: barkholt
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: WebAudioAPISound -> WebAudioSound
 *   Convention: Scala.js only; JSNI -> js.Dynamic for Web Audio API calls
 *   Convention: JavaScriptObject -> js.Dynamic; IntMap -> mutable.HashMap
 *   Idiom: Sound trait uses opaque types (SoundId, Volume, Pitch, Pan)
 *   Idiom: dispose() -> close() (Closeable)
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package audio

import scala.scalajs.js
import sge.utils.Nullable

/** Web Audio API implementation of [[Sound]].
  *
  * Sounds are loaded asynchronously via `decodeAudioData` and played through an [[AudioControlGraph]] for volume/pan control. Each `play`/`loop` call creates a new `AudioBufferSourceNode` (Web Audio
  * nodes are single-use).
  *
  * @param audioContext
  *   the Web Audio AudioContext
  * @param destinationNode
  *   the global volume GainNode
  * @param audioGraphPool
  *   pool for AudioControlGraph instances
  */
class WebAudioSound(
  audioContext:    js.Dynamic,
  destinationNode: js.Dynamic,
  audioGraphPool:  AudioControlGraphPool
) extends Sound {

  private val activeSounds = scala.collection.mutable.HashMap[Int, js.Dynamic]()
  private val activeGraphs = scala.collection.mutable.HashMap[Int, AudioControlGraph]()

  private var audioBuffer: js.Dynamic = js.undefined.asInstanceOf[js.Dynamic]
  private var nextKey:     Int        = 0

  /** Called when audio data has been decoded. Sets the buffer and resumes any sounds that were requested before decoding finished.
    */
  def setAudioBuffer(buffer: js.Dynamic): Unit = {
    audioBuffer = buffer
    // Resume sounds that were started before decoding finished
    val keys = activeSounds.keys.toArray
    keys.foreach { key =>
      pause(SoundId(key.toLong))
      resumeFrom(key, Nullable(0f))
    }
  }

  private def playInternal(volume: Float, pitch: Float, pan: Float, loop: Boolean): SoundId =
    if (!WebAudioManager.soundUnlocked && isAudioContextLocked) SoundId(-1L)
    else {
      val graph  = audioGraphPool.obtain()
      val source = createBufferSourceNode(loop, pitch)
      graph.setSource(source)
      graph.setPan(pan)
      graph.setVolume(volume)

      val myKey = nextKey
      nextKey += 1

      playSource(source, myKey, 0f)

      activeSounds.put(myKey, source)
      activeGraphs.put(myKey, graph)

      SoundId(myKey.toLong)
    }

  private def createBufferSourceNode(loop: Boolean, pitch: Float): js.Dynamic = {
    val buf =
      if (js.isUndefined(audioBuffer)) {
        // Still loading — create a silent buffer
        audioContext.createBuffer(2, 22050, 44100)
      } else audioBuffer

    val source = audioContext.createBufferSource()
    source.buffer = buf
    source.loop = loop
    if (pitch != 1.0f) source.playbackRate.value = pitch.toDouble
    source
  }

  private def playSource(source: js.Dynamic, key: Int, startOffset: Float): Unit = {
    source.startTime = audioContext.currentTime
    source.onended = { () =>
      soundDone(key)
    }: js.Function0[Unit]

    if (!js.isUndefined(source.start)) source.start(audioContext.currentTime, startOffset.toDouble)
    else source.noteOn(audioContext.currentTime, startOffset.toDouble) // old WebKit
  }

  private def stopSource(source: js.Dynamic): Unit =
    if (!js.isUndefined(source.stop)) source.stop()
    else source.noteOff() // old WebKit

  private def soundDone(key: Int): Unit =
    if (activeSounds.contains(key)) {
      activeSounds.remove(key)
      activeGraphs.remove(key).foreach(audioGraphPool.free)
    }

  private def isAudioContextLocked: Boolean =
    audioContext.state.asInstanceOf[String] != "running"

  // --- Sound trait ---

  override def play(): SoundId = play(Volume.max)

  override def play(volume: Volume): SoundId = play(volume, Pitch.normal, Pan.center)

  override def play(volume: Volume, pitch: Pitch, pan: Pan): SoundId =
    playInternal(volume.toFloat, pitch.toFloat, pan.toFloat, loop = false)

  override def loop(): SoundId = loop(Volume.max)

  override def loop(volume: Volume): SoundId = loop(volume, Pitch.normal, Pan.center)

  override def loop(volume: Volume, pitch: Pitch, pan: Pan): SoundId =
    playInternal(volume.toFloat, pitch.toFloat, pan.toFloat, loop = true)

  override def stop(): Unit = {
    val keys = activeSounds.keys.toArray
    keys.foreach(k => stop(SoundId(k.toLong)))
  }

  override def pause(): Unit = {
    val keys = activeSounds.keys.toArray
    keys.foreach(k => pause(SoundId(k.toLong)))
  }

  override def resume(): Unit = {
    val keys = activeSounds.keys.toArray
    keys.foreach(k => resume(SoundId(k.toLong)))
  }

  override def close(): Unit = {
    stop()
    activeSounds.clear()
    activeGraphs.clear()
  }

  override def stop(soundId: SoundId): Unit = {
    val key = soundId.toLong.toInt
    activeSounds.remove(key).foreach(stopSource)
    activeGraphs.remove(key).foreach(audioGraphPool.free)
  }

  override def pause(soundId: SoundId): Unit = {
    val key = soundId.toLong.toInt
    activeSounds.get(key).foreach { source =>
      // Record pause time then stop (Web Audio has no native pause)
      source.pauseTime = audioContext.currentTime
      stopSource(source)
    }
  }

  override def resume(soundId: SoundId): Unit =
    resumeFrom(soundId.toLong.toInt, Nullable.empty)

  private def resumeFrom(key: Int, from: Nullable[Float]): Unit =
    activeSounds.get(key).foreach { oldSource =>
      activeSounds.remove(key)
      val graph = activeGraphs(key)

      val loop  = oldSource.loop.asInstanceOf[Boolean]
      val pitch = oldSource.playbackRate.value.asInstanceOf[Double].toFloat
      val resumeOffset: Float = from.getOrElse {
        val pt = oldSource.pauseTime.asInstanceOf[Double]
        val st = oldSource.startTime.asInstanceOf[Double]
        (pt - st).toFloat
      }

      val newSource = createBufferSourceNode(loop, pitch)
      graph.setSource(newSource)
      activeSounds.put(key, newSource)

      playSource(newSource, key, resumeOffset)
    }

  override def setLooping(soundId: SoundId, looping: Boolean): Unit = {
    val key = soundId.toLong.toInt
    activeSounds.get(key).foreach { source =>
      source.loop = looping
    }
  }

  override def setPitch(soundId: SoundId, pitch: Pitch): Unit = {
    val key = soundId.toLong.toInt
    activeSounds.get(key).foreach { source =>
      source.playbackRate.value = pitch.toFloat.toDouble
    }
  }

  override def setVolume(soundId: SoundId, volume: Volume): Unit = {
    val key = soundId.toLong.toInt
    activeGraphs.get(key).foreach(_.setVolume(volume.toFloat))
  }

  override def setPan(soundId: SoundId, pan: Pan, volume: Volume): Unit = {
    val key = soundId.toLong.toInt
    activeGraphs.get(key).foreach { graph =>
      graph.setPan(pan.toFloat)
      graph.setVolume(volume.toFloat)
    }
  }
}
