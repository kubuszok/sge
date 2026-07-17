/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../audio/OpenALLwjgl3Audio.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: OpenALLwjgl3Audio -> MiniaudioEngine
 *   Convention: OpenAL context/device/source management -> miniaudio engine via AudioOps FFI
 *   Convention: OpenALSound -> MiniaudioSound, OpenALMusic -> MiniaudioMusic
 *   Convention: BiFunction<Audio,FileHandle,Sound> extensionToSoundClass -> file-extension dispatch in createSound
 *   Convention: Source pool (idleSources, soundIdToSource, sourceToSoundId) -> native engine handles all instance tracking
 *   Convention: Observer thread for device reconnection -> deferred to native AudioOps
 *   Idiom: split packages; Nullable; DynamicArray for music list
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package audio

import sge.platform.AudioOps
import lowlevel.Nullable

import scala.annotation.nowarn
import scala.collection.mutable.ArrayBuffer

/** Desktop audio engine backed by miniaudio.
  *
  * Manages the lifecycle of sound effects and music streams. The native miniaudio engine is initialized on construction and shut down on [[close]].
  *
  * @param simultaneousSources
  *   maximum number of simultaneous sound sources
  * @param deviceBufferSize
  *   audio device buffer size in samples
  * @param deviceBufferCount
  *   number of audio device buffers
  * @param audioOps
  *   the audio FFI operations
  * @author
  *   Nathan Sweet (original implementation)
  */
class MiniaudioEngine private[sge] (
  simultaneousSources:         Int,
  deviceBufferSize:            Int,
  deviceBufferCount:           Int,
  private val audioOps:        AudioOps,
  private val recorderFactory: (Int, Boolean) => AudioRecorder = (_, _) => throw sge.utils.SgeError.Unsupported("AudioRecorder not available on this platform")
) extends DesktopAudio {

  private val engineHandle: Long    = audioOps.initEngine(simultaneousSources, deviceBufferSize, deviceBufferCount)
  private val noDevice:     Boolean = engineHandle == 0L

  private val musicInstances: ArrayBuffer[MiniaudioMusic] = ArrayBuffer.empty
  private val soundInstances: ArrayBuffer[MiniaudioSound] = ArrayBuffer.empty

  // ─── Instance-registry lock (ISS-810) ───────────────────────────────
  //
  // musicInstances/soundInstances are mutated CROSS-THREAD by design: a loader thread appends via
  // newSound/newMusic (`+=`) while the render thread iterates musicInstances in update() and an
  // instance's close() removes itself via forgetSound/forgetMusic (`-=`). scala.collection.mutable.
  // ArrayBuffer is not thread-safe — its addOne does `array(size0) = elem; size0 += 1` with no
  // atomicity, so racing appends can overwrite a slot (losing an element) or throw while the backing
  // array is being grown. We therefore guard EVERY mutation and read of the two buffers on this
  // dedicated monitor, the same remedy the Pool free-list (ISS-603) and PoolManager map (ISS-803)
  // took (see sge.utils.Pool.lock / sge.utils.PoolManager).
  //
  // LOCKING DISCIPLINE: the monitor is a dedicated private object (never exposed, so external code
  // cannot interfere) and guards ONLY the buffer structure. Foreign code never runs under it, so the
  // per-frame render path stays cheap and there is no lock-ordering risk:
  //   - update() re-checks length + fetches items(i) UNDER the lock, then releases it before calling
  //     music.update() (native polling + the user completion listener);
  //   - close() snapshots + clears BOTH buffers atomically under the lock, then disposes the
  //     survivors OUTSIDE it (their forgetMusic/forgetSound re-acquire it for a now-no-op removal).
  private val instancesLock = new AnyRef

  // ─── Audio trait ────────────────────────────────────────────────────

  override def newAudioDevice(samplingRate: Int, isMono: Boolean): AudioDevice =
    if (noDevice) sge.noop.NoopAudioDevice(isMono)
    else {
      val deviceHandle = audioOps.createAudioDevice(engineHandle, samplingRate, isMono)
      if (deviceHandle == 0L) {
        throw sge.utils.SgeError.AudioError("Could not create audio device")
      }
      DesktopAudioDevice(deviceHandle, isMono, audioOps)
    }

  override def newAudioRecorder(samplingRate: Int, isMono: Boolean): AudioRecorder =
    recorderFactory(samplingRate, isMono)

  override def newSound(fileHandle: files.FileHandle): Sound =
    if (noDevice) sge.noop.NoopSound()
    else {
      val pcmData = fileHandle.readBytes()
      // Describe the stream to createSound honestly (ISS-772) instead of fabricating a fixed
      // channels=2/bits=16/rate=44100. The native miniaudio decoder consumes the encoded bytes and
      // re-derives the format, but the AudioOps.createSound FFI contract documents these parameters
      // as describing the stream, so they must match the asset's real format.
      val (channels, bitDepth, sampleRate) = readSoundFormat(fileHandle, pcmData)
      val soundHandle                      = audioOps.createSound(engineHandle, pcmData, channels, bitDepth, sampleRate)
      if (soundHandle == 0L) {
        throw sge.utils.SgeError.AudioError(s"Could not load sound: ${fileHandle.name}")
      }
      val sound = MiniaudioSound(this, soundHandle, audioOps)
      instancesLock.synchronized(soundInstances += sound)
      sound
    }

  override def newMusic(file: files.FileHandle): Music =
    if (noDevice) sge.noop.NoopMusic()
    else {
      // Miniaudio needs a real filesystem path. For Internal/Classpath files that
      // may be inside a JAR, resolve via file() first; if that doesn't exist on
      // disk, extract from the classpath to a temporary file.
      val resolvedPath = {
        val f = file.file
        if (f.exists()) f.getAbsolutePath
        else {
          val tmp = java.io.File.createTempFile("sge-music-", "-" + file.name)
          tmp.deleteOnExit()
          val in  = file.read()
          val out = new java.io.FileOutputStream(tmp)
          try {
            val buf = new Array[Byte](8192)
            var n   = in.read(buf)
            while (n >= 0) { out.write(buf, 0, n); n = in.read(buf) }
          } finally { in.close(); out.close() }
          tmp.getAbsolutePath
        }
      }
      val musicHandle = audioOps.createMusic(engineHandle, resolvedPath)
      if (musicHandle == 0L) {
        throw sge.utils.SgeError.AudioError(s"Could not load music: ${file.name}")
      }
      val music = MiniaudioMusic(this, musicHandle, audioOps)
      instancesLock.synchronized(musicInstances += music)
      music
    }

  override def switchOutputDevice(deviceIdentifier: Nullable[String]): Boolean =
    if (noDevice) false
    else {
      @nowarn("msg=deprecated") // orNull needed at FFI boundary — null means "default device"
      val name = deviceIdentifier.orNull
      audioOps.switchOutputDevice(engineHandle, name)
    }

  override def availableOutputDevices: Array[String] =
    if (noDevice) Array.empty[String]
    else audioOps.getAvailableOutputDevices(engineHandle)

  // ─── DesktopAudio ──────────────────────────────────────────────────

  override def update(): Unit =
    if (!noDevice) {
      audioOps.updateEngine(engineHandle)
      // Mirror OpenALLwjgl3Audio.update(): after advancing the engine, drive every music instance
      // so a naturally-finished stream fires its completion listener (ISS-760). Index iteration
      // matches the original ("for (int i = 0; i < music.size; i++) music.items[i].update()"); a
      // listener that closes its track during the callback shrinks musicInstances, and the
      // re-checked bound keeps the walk in range. Each iteration re-checks the length AND fetches
      // items(i) UNDER instancesLock (atomically, as one Option) so a concurrent newMusic append
      // cannot resize the backing array under the read (ISS-810); music.update() then runs OUTSIDE
      // the lock (see the locking-discipline note), so no foreign code executes while the lock is held.
      var i       = 0
      var running = true
      while (running)
        instancesLock.synchronized {
          if (i < musicInstances.length) Some(musicInstances(i)) else None
        } match {
          case Some(music) => music.update(); i += 1
          case None        => running = false
        }
    }

  override def close(): Unit =
    if (!noDevice) {
      // Snapshot before iterating — close() calls forgetMusic/forgetSound which mutates the buffer.
      // Snapshot + clear of BOTH buffers run atomically under instancesLock (ISS-810) so a concurrent
      // loader append cannot interleave with the teardown; the survivors are then closed OUTSIDE the
      // lock (their forgetMusic/forgetSound re-acquire it for a now-no-op removal), keeping native
      // dispose off the lock. Verbatim operation order preserved: snapshot music, snapshot sounds,
      // clear music, clear sounds, close music, close sounds, shutdown engine.
      val (music, sounds) = instancesLock.synchronized {
        val music  = musicInstances.toList
        val sounds = soundInstances.toList
        musicInstances.clear()
        soundInstances.clear()
        (music, sounds)
      }
      music.foreach(_.close())
      sounds.foreach(_.close())
      audioOps.shutdownEngine(engineHandle)
    }

  // ─── Internal bookkeeping ──────────────────────────────────────────

  /** Parses the real `(channels, bitDepth, sampleRate)` of a sound asset so createSound receives honest parameters (ISS-772).
    *
    * WAV (RIFF/WAVE) assets are parsed via [[WavInputStream]], which reads the format directly from the `fmt ` chunk. A non-WAV container (e.g. OGG/MP3), a non-PCM codec inside a WAV container (e.g.
    * MP3-in-WAV, codecType 0x0055), or a WAV that WavInputStream cannot parse has no single fixed PCM format — the native miniaudio decoder selects an output format at decode time — so we describe
    * those with the engine's decode target (stereo / 16-bit / 44.1kHz); the encoded bytes still carry the true format for the decoder to honour.
    */
  private def readSoundFormat(fileHandle: files.FileHandle, pcmData: Array[Byte]): (Int, Int, Int) =
    if (isWav(pcmData)) {
      try {
        val wav = new WavInputStream(fileHandle)
        try
          // MP3-in-WAV (fmt codecType 0x0055): WavInputStream returns early without populating
          // channels/bitDepth/sampleRate (all 0), so a non-PCM/non-float codec falls back to the
          // engine decode-target rather than describing the stream as 0-channel garbage.
          if (wav.codecType == 0x0001 || wav.codecType == 0x0003) (wav.channels, wav.bitDepth, wav.sampleRate)
          else (2, 16, 44100)
        finally
          sge.utils.StreamUtils.closeQuietly(wav)
      } catch {
        // WavInputStream rejects codecs it cannot parse (ADPCM, A-law, ...) and malformed headers
        // with InvalidInput. The native miniaudio decoder may still handle such files, so preserve
        // the pre-ISS-772 behavior: hand the bytes over with the engine decode-target and let the
        // decoder (or the createSound == 0 -> AudioError path) decide.
        case _: sge.utils.SgeError.InvalidInput => (2, 16, 44100)
      }
    } else {
      (2, 16, 44100)
    }

  /** Returns true when `data` begins with a RIFF/WAVE container header (`"RIFF"` at offset 0 and `"WAVE"` at offset 8).
    */
  private def isWav(data: Array[Byte]): Boolean =
    data.length >= 12 &&
      data(0) == 'R'.toByte && data(1) == 'I'.toByte && data(2) == 'F'.toByte && data(3) == 'F'.toByte &&
      data(8) == 'W'.toByte && data(9) == 'A'.toByte && data(10) == 'V'.toByte && data(11) == 'E'.toByte

  private[sge] def forgetSound(sound: MiniaudioSound): Unit =
    instancesLock.synchronized(soundInstances -= sound)

  private[sge] def forgetMusic(music: MiniaudioMusic): Unit =
    instancesLock.synchronized(musicInstances -= music)
}

object MiniaudioEngine {

  /** Creates a new MiniaudioEngine with default settings.
    * @param audioOps
    *   the audio FFI operations
    * @param recorderFactory
    *   factory for creating [[AudioRecorder]] instances (platform-specific)
    */
  def apply(audioOps: AudioOps, recorderFactory: (Int, Boolean) => AudioRecorder = (_, _) => throw sge.utils.SgeError.Unsupported("AudioRecorder not available on this platform")): MiniaudioEngine =
    new MiniaudioEngine(16, 512, 9, audioOps, recorderFactory)
}
