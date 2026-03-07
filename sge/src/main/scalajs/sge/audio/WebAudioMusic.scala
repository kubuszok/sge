/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../webaudio/WebAudioAPIMusic.java
 * Original authors: barkholt
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: WebAudioAPIMusic -> WebAudioMusic
 *   Convention: Scala.js only; JSNI -> js.Dynamic; GWT Audio element -> dom.HTMLAudioElement
 *   Convention: OnCompletionListener -> Music => Unit function type
 *   Idiom: Music trait property-style methods (playing, looping, volume, position)
 *   Idiom: dispose() -> close() (Closeable)
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package audio

import org.scalajs.dom
import scala.scalajs.js
import sge.utils.Nullable

/** Web Audio API implementation of [[Music]] using an HTML `<audio>` element for streaming playback, routed through an [[AudioControlGraph]] for volume and pan control.
  *
  * @param audioContext
  *   the Web Audio AudioContext
  * @param audioElement
  *   the HTML audio element for media streaming
  * @param audioControlGraphPool
  *   pool for AudioControlGraph instances
  */
class WebAudioMusic(
  audioContext:          js.Dynamic,
  audioElement:          dom.HTMLAudioElement,
  audioControlGraphPool: AudioControlGraphPool
) extends Music {

  private val audioControlGraph: AudioControlGraph = audioControlGraphPool.obtain()

  private var completionListener: Nullable[Music => Unit] = Nullable.empty

  // Create MediaElementAudioSourceNode and wire it through the control graph
  locally {
    val source = audioContext.createMediaElementSource(audioElement.asInstanceOf[js.Dynamic])
    audioControlGraph.setSource(source)

    audioElement.addEventListener(
      "ended",
      { (_: dom.Event) =>
        completionListener.foreach(_(this))
      }: js.Function1[dom.Event, Unit]
    )
  }

  override def play(): Unit = audioElement.play()

  override def pause(): Unit = audioElement.pause()

  override def stop(): Unit = {
    audioElement.pause()
    audioElement.currentTime = 0
  }

  override def playing: Boolean = !audioElement.paused

  override def looping: Boolean = audioElement.loop

  override def looping_=(isLooping: Boolean): Unit = audioElement.loop = isLooping

  override def volume: Volume = Volume.unsafeMake(audioControlGraph.getVolume())

  override def volume_=(v: Volume): Unit = audioControlGraph.setVolume(v.toFloat)

  override def setPan(pan: Pan, vol: Volume): Unit = {
    audioControlGraph.setPan(pan.toFloat)
    audioControlGraph.setVolume(vol.toFloat)
  }

  override def position: Position = Position.unsafeMake(audioElement.currentTime.toFloat)

  override def position_=(pos: Position): Unit =
    audioElement.currentTime = pos.toFloatSeconds.toDouble

  override def onComplete(listener: Music => Unit): Unit =
    completionListener = Nullable(listener)

  override def close(): Unit = {
    audioElement.pause()
    audioControlGraphPool.free(audioControlGraph)
  }
}
