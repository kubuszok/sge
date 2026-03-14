/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original JVM implementation of AudioOps
 *   Convention: Panama FFM downcall handles to miniaudio shared library
 *   Convention: String params allocated in confined arenas (auto-freed)
 *   Idiom: split packages
 *   Audited: 2026-03-08
 */
package sge
package platform

import java.lang.foreign.*
import java.lang.foreign.ValueLayout.*
import java.lang.invoke.MethodHandle

/** JVM implementation of [[AudioOps]] via Panama FFM downcall handles to the miniaudio shared library.
  *
  * Each method creates a downcall handle to the corresponding miniaudio C function exposed by the SGE native bridge. The bridge wraps miniaudio's engine/sound/music APIs into a flat C interface
  * suitable for FFI.
  *
  * @param lib
  *   a `SymbolLookup` for the SGE audio bridge library (e.g. libsge_audio.dylib)
  */
class AudioOpsJvm(lib: SymbolLookup) extends AudioOps {

  private val linker: Linker = Linker.nativeLinker()

  private def h(name: String, desc: FunctionDescriptor): MethodHandle =
    linker.downcallHandle(
      lib.find(name).orElseThrow(() => new UnsatisfiedLinkError(s"Audio symbol not found: $name")),
      desc
    )

  // ─── Layout aliases ────────────────────────────────────────────────────

  private val I: ValueLayout.OfInt   = JAVA_INT
  private val F: ValueLayout.OfFloat = JAVA_FLOAT
  private val J: ValueLayout.OfLong  = JAVA_LONG
  private val P: AddressLayout       = ADDRESS

  // ─── Method handles (lazy to avoid loading unused symbols) ──────────

  // Engine lifecycle
  private lazy val hInitEngine     = h("sge_audio_init_engine", FunctionDescriptor.of(J, I, I, I))
  private lazy val hShutdownEngine = h("sge_audio_shutdown_engine", FunctionDescriptor.ofVoid(J))
  private lazy val hUpdateEngine   = h("sge_audio_update_engine", FunctionDescriptor.ofVoid(J))

  // Sound
  private lazy val hCreateSound        = h("sge_audio_create_sound", FunctionDescriptor.of(J, J, P, I, I, I, I))
  private lazy val hDisposeSound       = h("sge_audio_dispose_sound", FunctionDescriptor.ofVoid(J))
  private lazy val hPlaySound          = h("sge_audio_play_sound", FunctionDescriptor.of(J, J, F, F, F, I))
  private lazy val hStopSound          = h("sge_audio_stop_sound", FunctionDescriptor.ofVoid(J))
  private lazy val hPauseSound         = h("sge_audio_pause_sound", FunctionDescriptor.ofVoid(J))
  private lazy val hResumeSound        = h("sge_audio_resume_sound", FunctionDescriptor.ofVoid(J))
  private lazy val hStopAllInstances   = h("sge_audio_stop_all_instances", FunctionDescriptor.ofVoid(J))
  private lazy val hPauseAllInstances  = h("sge_audio_pause_all_instances", FunctionDescriptor.ofVoid(J))
  private lazy val hResumeAllInstances = h("sge_audio_resume_all_instances", FunctionDescriptor.ofVoid(J))
  private lazy val hSetSoundVolume     = h("sge_audio_set_sound_volume", FunctionDescriptor.ofVoid(J, F))
  private lazy val hSetSoundPitch      = h("sge_audio_set_sound_pitch", FunctionDescriptor.ofVoid(J, F))
  private lazy val hSetSoundPan        = h("sge_audio_set_sound_pan", FunctionDescriptor.ofVoid(J, F, F))
  private lazy val hSetSoundLooping    = h("sge_audio_set_sound_looping", FunctionDescriptor.ofVoid(J, I))

  // Music
  private lazy val hCreateMusic      = h("sge_audio_create_music", FunctionDescriptor.of(J, J, P))
  private lazy val hDisposeMusic     = h("sge_audio_dispose_music", FunctionDescriptor.ofVoid(J))
  private lazy val hPlayMusic        = h("sge_audio_play_music", FunctionDescriptor.ofVoid(J))
  private lazy val hPauseMusic       = h("sge_audio_pause_music", FunctionDescriptor.ofVoid(J))
  private lazy val hStopMusic        = h("sge_audio_stop_music", FunctionDescriptor.ofVoid(J))
  private lazy val hIsMusicPlaying   = h("sge_audio_is_music_playing", FunctionDescriptor.of(I, J))
  private lazy val hGetMusicVolume   = h("sge_audio_get_music_volume", FunctionDescriptor.of(F, J))
  private lazy val hSetMusicVolume   = h("sge_audio_set_music_volume", FunctionDescriptor.ofVoid(J, F))
  private lazy val hSetMusicPitch    = h("sge_audio_set_music_pitch", FunctionDescriptor.ofVoid(J, F))
  private lazy val hSetMusicPan      = h("sge_audio_set_music_pan", FunctionDescriptor.ofVoid(J, F, F))
  private lazy val hIsMusicLooping   = h("sge_audio_is_music_looping", FunctionDescriptor.of(I, J))
  private lazy val hSetMusicLooping  = h("sge_audio_set_music_looping", FunctionDescriptor.ofVoid(J, I))
  private lazy val hSetMusicPosition = h("sge_audio_set_music_position", FunctionDescriptor.ofVoid(J, F))
  private lazy val hGetMusicPosition = h("sge_audio_get_music_position", FunctionDescriptor.of(F, J))
  private lazy val hGetMusicDuration = h("sge_audio_get_music_duration", FunctionDescriptor.of(F, J))

  // AudioDevice (raw PCM)
  private lazy val hCreateAudioDevice  = h("sge_audio_create_device", FunctionDescriptor.of(J, J, I, I))
  private lazy val hDisposeAudioDevice = h("sge_audio_dispose_device", FunctionDescriptor.ofVoid(J))
  private lazy val hWriteAudioDevice   = h("sge_audio_write_device", FunctionDescriptor.ofVoid(J, P, I, I))
  private lazy val hSetDeviceVolume    = h("sge_audio_set_device_volume", FunctionDescriptor.ofVoid(J, F))
  private lazy val hPauseDevice        = h("sge_audio_pause_device", FunctionDescriptor.ofVoid(J))
  private lazy val hResumeDevice       = h("sge_audio_resume_device", FunctionDescriptor.ofVoid(J))
  private lazy val hGetDeviceLatency   = h("sge_audio_get_device_latency", FunctionDescriptor.of(I, J))

  // Output device
  private lazy val hGetOutputDevices   = h("sge_audio_get_output_devices", FunctionDescriptor.of(P, J, P))
  private lazy val hSwitchOutputDevice = h("sge_audio_switch_output_device", FunctionDescriptor.of(I, J, P))
  private lazy val hFreeOutputDevices  = h("sge_audio_free_output_devices", FunctionDescriptor.ofVoid(P, I))

  // ─── Engine lifecycle ──────────────────────────────────────────────────

  override def initEngine(simultaneousSources: Int, bufferSize: Int, bufferCount: Int): Long =
    hInitEngine.invoke(simultaneousSources, bufferSize, bufferCount).asInstanceOf[Long]

  override def shutdownEngine(engineHandle: Long): Unit =
    hShutdownEngine.invoke(engineHandle)

  override def updateEngine(engineHandle: Long): Unit =
    hUpdateEngine.invoke(engineHandle)

  // ─── Sound ──────────────────────────────────────────────────────────────

  override def createSound(engineHandle: Long, pcmData: Array[Byte], channels: Int, bitDepth: Int, sampleRate: Int): Long = {
    val arena = Arena.ofConfined()
    try {
      val seg = arena.allocate(pcmData.length.toLong)
      MemorySegment.copy(pcmData, 0, seg, JAVA_BYTE, 0, pcmData.length)
      hCreateSound.invoke(engineHandle, seg, pcmData.length, channels, bitDepth, sampleRate).asInstanceOf[Long]
    } finally arena.close()
  }

  override def disposeSound(soundHandle: Long): Unit =
    hDisposeSound.invoke(soundHandle)

  override def playSound(soundHandle: Long, volume: Float, pitch: Float, pan: Float, loop: Boolean): Long =
    hPlaySound.invoke(soundHandle, volume, pitch, pan, if (loop) 1 else 0).asInstanceOf[Long]

  override def stopSound(instanceId: Long): Unit =
    hStopSound.invoke(instanceId)

  override def pauseSound(instanceId: Long): Unit =
    hPauseSound.invoke(instanceId)

  override def resumeSound(instanceId: Long): Unit =
    hResumeSound.invoke(instanceId)

  override def stopAllInstances(soundHandle: Long): Unit =
    hStopAllInstances.invoke(soundHandle)

  override def pauseAllInstances(soundHandle: Long): Unit =
    hPauseAllInstances.invoke(soundHandle)

  override def resumeAllInstances(soundHandle: Long): Unit =
    hResumeAllInstances.invoke(soundHandle)

  override def setSoundVolume(instanceId: Long, volume: Float): Unit =
    hSetSoundVolume.invoke(instanceId, volume)

  override def setSoundPitch(instanceId: Long, pitch: Float): Unit =
    hSetSoundPitch.invoke(instanceId, pitch)

  override def setSoundPan(instanceId: Long, pan: Float, volume: Float): Unit =
    hSetSoundPan.invoke(instanceId, pan, volume)

  override def setSoundLooping(instanceId: Long, looping: Boolean): Unit =
    hSetSoundLooping.invoke(instanceId, if (looping) 1 else 0)

  // ─── Music ──────────────────────────────────────────────────────────────

  override def createMusic(engineHandle: Long, filePath: String): Long = {
    val arena = Arena.ofConfined()
    try
      hCreateMusic.invoke(engineHandle, arena.allocateFrom(filePath)).asInstanceOf[Long]
    finally arena.close()
  }

  override def disposeMusic(musicHandle: Long): Unit =
    hDisposeMusic.invoke(musicHandle)

  override def playMusic(musicHandle: Long): Unit =
    hPlayMusic.invoke(musicHandle)

  override def pauseMusic(musicHandle: Long): Unit =
    hPauseMusic.invoke(musicHandle)

  override def stopMusic(musicHandle: Long): Unit =
    hStopMusic.invoke(musicHandle)

  override def isMusicPlaying(musicHandle: Long): Boolean = {
    val result = hIsMusicPlaying.invoke(musicHandle).asInstanceOf[Int]
    result != 0
  }

  override def getMusicVolume(musicHandle: Long): Float =
    hGetMusicVolume.invoke(musicHandle).asInstanceOf[Float]

  override def setMusicVolume(musicHandle: Long, volume: Float): Unit =
    hSetMusicVolume.invoke(musicHandle, volume)

  override def setMusicPitch(musicHandle: Long, pitch: Float): Unit =
    hSetMusicPitch.invoke(musicHandle, pitch)

  override def setMusicPan(musicHandle: Long, pan: Float, volume: Float): Unit =
    hSetMusicPan.invoke(musicHandle, pan, volume)

  override def isMusicLooping(musicHandle: Long): Boolean = {
    val result = hIsMusicLooping.invoke(musicHandle).asInstanceOf[Int]
    result != 0
  }

  override def setMusicLooping(musicHandle: Long, looping: Boolean): Unit =
    hSetMusicLooping.invoke(musicHandle, if (looping) 1 else 0)

  override def setMusicPosition(musicHandle: Long, position: Float): Unit =
    hSetMusicPosition.invoke(musicHandle, position)

  override def getMusicPosition(musicHandle: Long): Float =
    hGetMusicPosition.invoke(musicHandle).asInstanceOf[Float]

  override def getMusicDuration(musicHandle: Long): Float =
    hGetMusicDuration.invoke(musicHandle).asInstanceOf[Float]

  // ─── AudioDevice (raw PCM) ──────────────────────────────────────────────

  override def createAudioDevice(engineHandle: Long, sampleRate: Int, isMono: Boolean): Long =
    hCreateAudioDevice.invoke(engineHandle, sampleRate, if (isMono) 1 else 0).asInstanceOf[Long]

  override def disposeAudioDevice(deviceHandle: Long): Unit =
    hDisposeAudioDevice.invoke(deviceHandle)

  override def writeAudioDevice(deviceHandle: Long, data: Array[Byte], offset: Int, length: Int): Unit = {
    val arena = Arena.ofConfined()
    try {
      val seg = arena.allocate(length.toLong)
      MemorySegment.copy(data, offset, seg, JAVA_BYTE, 0, length)
      hWriteAudioDevice.invoke(deviceHandle, seg, 0, length)
    } finally arena.close()
  }

  override def setAudioDeviceVolume(deviceHandle: Long, volume: Float): Unit =
    hSetDeviceVolume.invoke(deviceHandle, volume)

  override def pauseAudioDevice(deviceHandle: Long): Unit =
    hPauseDevice.invoke(deviceHandle)

  override def resumeAudioDevice(deviceHandle: Long): Unit =
    hResumeDevice.invoke(deviceHandle)

  override def getAudioDeviceLatency(deviceHandle: Long): Int =
    hGetDeviceLatency.invoke(deviceHandle).asInstanceOf[Int]

  // ─── Output device ──────────────────────────────────────────────────────

  override def getAvailableOutputDevices(engineHandle: Long): Array[String] = {
    val arena = Arena.ofConfined()
    try {
      val countSeg = arena.allocate(I)
      val result   = hGetOutputDevices.invoke(engineHandle, countSeg).asInstanceOf[MemorySegment]
      if (result == MemorySegment.NULL || result.address() == 0L) Array.empty
      else {
        val count = countSeg.get(I, 0)
        if (count <= 0) Array.empty
        else {
          val ptrs    = result.reinterpret(P.byteSize() * count.toLong)
          val devices = Array.tabulate(count) { i =>
            val strPtr = ptrs.getAtIndex(P, i.toLong)
            strPtr.reinterpret(Long.MaxValue).getString(0)
          }
          hFreeOutputDevices.invoke(result, count)
          devices
        }
      }
    } finally arena.close()
  }

  override def switchOutputDevice(engineHandle: Long, deviceName: String): Boolean = {
    val arena = Arena.ofConfined()
    try {
      val namePtr = if (deviceName == null) MemorySegment.NULL else arena.allocateFrom(deviceName)
      val result  = hSwitchOutputDevice.invoke(engineHandle, namePtr).asInstanceOf[Int]
      result != 0
    } finally arena.close()
  }
}

object AudioOpsJvm {

  /** Creates an AudioOpsJvm from the SGE audio bridge library loaded from the system library path.
    * @param libName
    *   the library name (e.g. "sge_audio", "sge_native_ops")
    */
  def apply(libName: String = "sge_audio"): AudioOpsJvm = {
    val found  = NativeLibLoader.load(libName)
    val lookup = SymbolLookup.libraryLookup(found, Arena.global())
    new AudioOpsJvm(lookup)
  }
}
