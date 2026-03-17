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
import scala.scalajs.js

/** Central manager for Web Audio API sound and music creation.
  *
  * Creates an `AudioContext`, a global volume `GainNode`, and manages the audio unlock flow required by browser autoplay policies. Also implements [[LifecycleListener]] to mute/unmute on application
  * pause/resume.
  */
class WebAudioManager()(using Sge) extends LifecycleListener {

  private val audioContext: js.Dynamic = {
    val hasStandard = js.typeOf(js.Dynamic.global.AudioContext) != "undefined"
    val hasWebkit   = js.typeOf(js.Dynamic.global.webkitAudioContext) != "undefined"
    if (hasStandard) js.Dynamic.newInstance(js.Dynamic.global.AudioContext)()
    else if (hasWebkit) js.Dynamic.newInstance(js.Dynamic.global.webkitAudioContext)()
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
    * The sound data is read from the preloaded cache (via BrowserFileHandle) and decoded via `decodeAudioData`.
    */
  def createSound(fileHandle: files.FileHandle): Sound = {
    val sound = WebAudioSound(audioContext, globalVolumeNode, audioControlGraphPool)

    // Read audio bytes from the preloaded cache and decode
    val bytes = fileHandle.readBytes()
    val int8  = new js.typedarray.Int8Array(bytes.length)
    var i     = 0
    while (i < bytes.length) {
      int8(i) = bytes(i)
      i += 1
    }
    val arrayBuf = int8.buffer

    audioContext.decodeAudioData(
      arrayBuf.asInstanceOf[js.Any],
      { (decodedBuffer: js.Dynamic) =>
        sound.setAudioBuffer(decodedBuffer)
      }: js.Function1[js.Dynamic, Unit],
      { () =>
        scribe.error("WebAudio: decodeAudioData failed for " + fileHandle.path())
      }: js.Function0[Unit]
    )

    sound
  }

  /** Create a new [[WebAudioMusic]] from a file handle. */
  def createMusic(fileHandle: files.FileHandle): Music = {
    // For music, create a Blob URL from cached bytes — streaming via <audio> element
    val bytes = fileHandle.readBytes()
    val int8  = new js.typedarray.Int8Array(bytes.length)
    var i     = 0
    while (i < bytes.length) {
      int8(i) = bytes(i)
      i += 1
    }
    val blob         = new dom.Blob(js.Array(int8.buffer), js.Dynamic.literal("type" -> mimeForAudio(fileHandle.path())).asInstanceOf[dom.BlobPropertyBag])
    val blobUrl      = dom.URL.createObjectURL(blob)
    val audioElement = document.createElement("audio").asInstanceOf[dom.HTMLAudioElement]
    audioElement.src = blobUrl

    WebAudioMusic(audioContext, audioElement, audioControlGraphPool)
  }

  private def mimeForAudio(path: String): String = {
    val lower = path.toLowerCase
    if (lower.endsWith(".ogg")) "audio/ogg"
    else if (lower.endsWith(".mp3")) "audio/mpeg"
    else if (lower.endsWith(".wav")) "audio/wav"
    else "audio/mpeg"
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
  def isSupported: Boolean =
    js.typeOf(js.Dynamic.global.AudioContext) != "undefined" ||
      js.typeOf(js.Dynamic.global.webkitAudioContext) != "undefined"
}
