/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../DefaultGwtAudio.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: DefaultGwtAudio -> DefaultBrowserAudio
 *   Convention: Scala.js only; delegates to WebAudioManager for Sound/Music creation
 *   Idiom: AudioDevice/AudioRecorder not supported in browser — throws SgeError
 *   Idiom: Output device enumeration via navigator.mediaDevices.enumerateDevices
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package audio

import sge.utils.Nullable

/** Browser implementation of [[BrowserAudio]] using the Web Audio API.
  *
  * Sound effects use `decodeAudioData` + `AudioBufferSourceNode`, music uses `HTMLAudioElement` routed through `createMediaElementSource`.
  */
class DefaultBrowserAudio(application: Application) extends BrowserAudio {

  private val webAudioManager: WebAudioManager = WebAudioManager(application)

  override def newAudioDevice(samplingRate: Int, isMono: Boolean): AudioDevice =
    throw utils.SgeError.InvalidInput("AudioDevice not supported by browser backend", None)

  override def newAudioRecorder(samplingRate: Int, isMono: Boolean): AudioRecorder =
    throw utils.SgeError.InvalidInput("AudioRecorder not supported by browser backend", None)

  override def newSound(fileHandle: files.FileHandle): Sound =
    webAudioManager.createSound(fileHandle)

  override def newMusic(file: files.FileHandle): Music =
    webAudioManager.createMusic(file)

  override def switchOutputDevice(deviceIdentifier: Nullable[String]): Boolean = false

  override def availableOutputDevices: Array[String] = Array.empty

  override def close(): Unit = ()
}
