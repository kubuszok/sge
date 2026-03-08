/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../webaudio/WebAudioAPIManager.java
 * Original authors: barkholt
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: WebAudioAPIManager -> WebAudioManager
 *   Convention: Scala.js only; JSNI -> js.Dynamic for Web Audio API
 *   Convention: JavaScriptObject -> js.Dynamic; GWT Timer -> js.timers.setInterval
 *   Convention: AssetDownloader XHR -> fetch API for loading sound data
 *   Idiom: LifecycleListener pause/resume for mute/unmute on focus loss
 *   Idiom: Sound unlock via user interaction events (Web Audio autoplay policy)
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package audio

import org.scalajs.dom
import org.scalajs.dom.document
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js

/** Central manager for Web Audio API sound and music creation.
  *
  * Creates an `AudioContext`, a global volume `GainNode`, and manages the audio unlock flow required by browser autoplay policies. Also implements [[LifecycleListener]] to mute/unmute on application
  * pause/resume.
  */
class WebAudioManager()(using Sge) extends LifecycleListener {

  private val audioContext: js.Dynamic = {
    val AudioContext       = js.Dynamic.global.AudioContext
    val webkitAudioContext = js.Dynamic.global.webkitAudioContext
    val Ctor               = if (!js.isUndefined(AudioContext)) AudioContext else webkitAudioContext
    if (!js.isUndefined(Ctor)) js.Dynamic.newInstance(Ctor)()
    else null // no Web Audio support
  }

  private val globalVolumeNode: js.Dynamic =
    if (audioContext == null) null
    else {
      val node =
        if (!js.isUndefined(audioContext.createGain)) audioContext.createGain()
        else audioContext.createGainNode() // old WebKit/iOS
      node.gain.value = 1.0
      node.connect(audioContext.destination)
      node
    }

  private val audioControlGraphPool: AudioControlGraphPool =
    if (audioContext != null) AudioControlGraphPool(audioContext, globalVolumeNode)
    else null

  // Register as lifecycle listener for mute on pause
  Sge().application.addLifecycleListener(this)

  // Unlock audio context on first user interaction
  if (audioContext != null && isAudioContextLocked) hookUpSoundUnlockers()
  else WebAudioManager.soundUnlocked = true

  private def isAudioContextLocked: Boolean =
    audioContext.state.asInstanceOf[String] != "running"

  private def hookUpSoundUnlockers(): Unit = {
    val eventNames = js.Array(
      "click",
      "contextmenu",
      "auxclick",
      "dblclick",
      "mousedown",
      "mouseup",
      "pointerup",
      "touchend",
      "keydown",
      "keyup",
      "touchstart"
    )
    lazy val unlock: js.Function1[dom.Event, Unit] = { (_: dom.Event) =>
      audioContext.resume()
      WebAudioManager.soundUnlocked = true
      scribe.info("WebAudio: AudioContext unlocked")
      eventNames.foreach(name => document.removeEventListener(name, unlock))
    }
    eventNames.foreach(name => document.addEventListener(name, unlock))
  }

  /** Set the audio output device by sink ID. */
  def setSinkId(sinkId: String): Unit =
    if (audioContext != null && !js.isUndefined(audioContext.setSinkId)) {
      audioContext.setSinkId(sinkId)
    }

  /** Create a new [[WebAudioSound]] from a file handle.
    *
    * The sound data is fetched asynchronously and decoded via `decodeAudioData`.
    */
  def createSound(fileHandle: files.FileHandle): Sound = {
    val sound = WebAudioSound(audioContext, globalVolumeNode, audioControlGraphPool)

    // Fetch the audio data and decode it
    val url = fileHandle match {
      case bfh: files.BrowserFileHandle => bfh.parent().toString + "/" + bfh.name()
      case _ => fileHandle.path()
    }

    dom
      .fetch(url)
      .toFuture
      .flatMap { response =>
        response.arrayBuffer().toFuture
      }
      .foreach { arrayBuf =>
        audioContext.decodeAudioData(
          arrayBuf.asInstanceOf[js.Any],
          { (decodedBuffer: js.Dynamic) =>
            sound.setAudioBuffer(decodedBuffer)
          }: js.Function1[js.Dynamic, Unit],
          { () =>
            scribe.error("WebAudio: decodeAudioData failed")
          }: js.Function0[Unit]
        )
      }

    sound
  }

  /** Create a new [[WebAudioMusic]] from a file handle. */
  def createMusic(fileHandle: files.FileHandle): Music = {
    val url = fileHandle match {
      case bfh: files.BrowserFileHandle => bfh.parent().toString + "/" + bfh.name()
      case _ => fileHandle.path()
    }

    val audioElement = document.createElement("audio").asInstanceOf[dom.HTMLAudioElement]
    audioElement.src = url

    WebAudioMusic(audioContext, audioElement, audioControlGraphPool)
  }

  /** Set the global volume for all sounds and music. */
  def setGlobalVolume(volume: Float): Unit =
    if (globalVolumeNode != null) globalVolumeNode.gain.value = volume.toDouble

  // --- LifecycleListener ---

  override def pause(): Unit =
    // Mute on focus loss
    if (globalVolumeNode != null) globalVolumeNode.disconnect(audioContext.destination)

  override def resume(): Unit =
    // Unmute on focus regain
    if (globalVolumeNode != null) globalVolumeNode.connect(audioContext.destination)

  override def dispose(): Unit = ()
}

object WebAudioManager {
  var soundUnlocked: Boolean = false

  /** Check if the Web Audio API is available in this browser. */
  def isSupported: Boolean = {
    val AudioContext       = js.Dynamic.global.AudioContext
    val webkitAudioContext = js.Dynamic.global.webkitAudioContext
    !js.isUndefined(AudioContext) || !js.isUndefined(webkitAudioContext)
  }
}
