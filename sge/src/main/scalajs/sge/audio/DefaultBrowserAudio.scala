/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../DefaultGwtAudio.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: DefaultGwtAudio -> DefaultBrowserAudio
 *   Convention: Scala.js only; delegates to WebAudioManager for Sound/Music creation
 *   Convention: JSNI getUserMedia/fetchAvailableOutputDevices -> js.Dynamic navigator.mediaDevices
 *   Convention: GWT Timer observer -> js.timers.setInterval; DeviceListener SAM -> function type
 *   Idiom: AudioDevice/AudioRecorder not supported in browser — throws SgeError.Unsupported (ISS-771)
 *   Idiom: Output device enumeration via navigator.mediaDevices.enumerateDevices
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package audio

import lowlevel.Nullable

import scala.collection.mutable
import scala.scalajs.js

/** Browser implementation of [[BrowserAudio]] using the Web Audio API.
  *
  * Sound effects use `decodeAudioData` + `AudioBufferSourceNode`, music uses `HTMLAudioElement` routed through `createMediaElementSource`.
  *
  * Output-device selection mirrors `DefaultGwtAudio`: when [[BrowserApplicationConfig.fetchAvailableOutputDevices]] is enabled, the user is asked for audio permission (labels are only exposed after
  * `getUserMedia` consent) and available `audiooutput` devices are polled every second via `navigator.mediaDevices.enumerateDevices()`.
  */
class DefaultBrowserAudio(application: Application, config: BrowserApplicationConfig) extends BrowserAudio {

  private val webAudioManager: WebAudioManager = WebAudioManager(application)

  /** Maps user-facing device labels to browser sink ids — `DefaultGwtAudio.outputDeviceLabelsIds`. Refreshed by the polling observer below. */
  private val outputDeviceLabelsIds: mutable.LinkedHashMap[String, String] = mutable.LinkedHashMap.empty

  // Mirror the DefaultGwtAudio constructor: when the config opts in, request audio permission and
  // start a repeating 1s observer (GWT Timer.scheduleRepeating(1000)) that re-enumerates devices.
  locally {
    if (config.fetchAvailableOutputDevices) {
      getUserMedia()
      js.timers.setInterval(1000d) {
        fetchAvailableOutputDevices { (ids, labels) =>
          outputDeviceLabelsIds.clear()
          var i = 0
          while (i < ids.length) {
            outputDeviceLabelsIds.put(labels(i), ids(i))
            i += 1
          }
        }
      }
    }
  }

  override def newAudioDevice(samplingRate: Int, isMono: Boolean): AudioDevice =
    throw utils.SgeError.Unsupported("AudioDevice not supported by browser backend", None)

  override def newAudioRecorder(samplingRate: Int, isMono: Boolean): AudioRecorder =
    throw utils.SgeError.Unsupported("AudioRecorder not supported by browser backend", None)

  override def newSound(fileHandle: files.FileHandle): Sound =
    webAudioManager.createSound(fileHandle)

  override def newMusic(file: files.FileHandle): Music =
    webAudioManager.createMusic(file)

  override def switchOutputDevice(deviceIdentifier: Nullable[String]): Boolean = {
    // Mirror DefaultGwtAudio.switchOutputDevice: honour the "speaker-selection" feature policy when
    // the browser advertises it; when the policy denies the feature, report false without switching.
    val features = BrowserFeaturePolicy.features()
    val allowed  = features.fold(true)(f => !f.contains("speaker-selection")) ||
      BrowserFeaturePolicy.allowsFeature("speaker-selection")
    if (allowed) {
      // Empty string = the browser default sink (the original maps a null label to ""). An unknown
      // label also falls back to the default sink: the original passes ObjectMap#get's null straight
      // into setSinkId; SGE does not pass null across the JS boundary.
      val sinkId = deviceIdentifier.fold("")(label => outputDeviceLabelsIds.getOrElse(label, ""))
      webAudioManager.setSinkId(sinkId)
      true
    } else {
      false
    }
  }

  override def availableOutputDevices: Array[String] = outputDeviceLabelsIds.keysIterator.toArray

  override def close(): Unit = ()

  /** Requests audio-capture permission so `enumerateDevices` exposes device labels — the JSNI `getUserMedia` in the original. */
  private def getUserMedia(): Unit = {
    js.Dynamic.global.navigator.mediaDevices.getUserMedia(js.Dynamic.literal("audio" -> true))
    ()
  }

  /** Enumerates `audiooutput` devices (deviceId present and not the synthetic "default" entry) and hands their (ids, labels) to the listener — the JSNI `fetchAvailableOutputDevices` in the original.
    */
  private def fetchAvailableOutputDevices(listener: (Array[String], Array[String]) => Unit): Unit = {
    val callback: js.Function1[js.Array[js.Dynamic], Unit] = { (devices: js.Array[js.Dynamic]) =>
      val dev = devices.toArray.filter { device =>
        // device.deviceId && device.kind === 'audiooutput' && device.deviceId !== 'default'
        js.DynamicImplicits.truthValue(device.deviceId) &&
        device.kind.asInstanceOf[String] == "audiooutput" &&
        device.deviceId.asInstanceOf[String] != "default"
      }
      listener(dev.map(_.deviceId.asInstanceOf[String]), dev.map(_.label.asInstanceOf[String]))
    }
    js.Dynamic.global.navigator.mediaDevices.enumerateDevices().`then`(callback)
    ()
  }
}
