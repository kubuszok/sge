/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original Scala Native implementation of AudioOps
 *   Convention: @extern C FFI to SGE audio bridge (miniaudio wrapper)
 *   Convention: C function naming: sge_audio_*
 *   Convention: Handle types use Long (C int64_t / long long)
 *   Convention: String params allocated in Zone (auto-freed)
 *   Idiom: split packages
 */
package sge
package platform

import java.nio.charset.StandardCharsets

import scala.scalanative.unsafe.*

// The C API uses int64_t for handles, which maps to Scala Long (not CLong).
// CInt is used for non-handle integer parameters.

@link("sge_audio")
@extern
private object AudioOpsC {

  // Engine lifecycle
  def sge_audio_init_engine(simultaneousSources: CInt, bufferSize: CInt, bufferCount: CInt): Long = extern
  def sge_audio_shutdown_engine(engineHandle:    Long):                                      Unit = extern
  def sge_audio_update_engine(engineHandle:      Long):                                      Unit = extern

  // Sound
  def sge_audio_create_sound(
    engineHandle: Long,
    pcmData:      Ptr[Byte],
    dataLen:      CInt,
    channels:     CInt,
    bitDepth:     CInt,
    sampleRate:   CInt
  ):                                                                                                               Long = extern
  def sge_audio_dispose_sound(soundHandle:        Long):                                                           Unit = extern
  def sge_audio_play_sound(soundHandle:           Long, volume:  CFloat, pitch:  CFloat, pan: CFloat, loop: CInt): Long = extern
  def sge_audio_stop_sound(instanceId:            Long):                                                           Unit = extern
  def sge_audio_pause_sound(instanceId:           Long):                                                           Unit = extern
  def sge_audio_resume_sound(instanceId:          Long):                                                           Unit = extern
  def sge_audio_stop_all_instances(soundHandle:   Long):                                                           Unit = extern
  def sge_audio_pause_all_instances(soundHandle:  Long):                                                           Unit = extern
  def sge_audio_resume_all_instances(soundHandle: Long):                                                           Unit = extern
  def sge_audio_set_sound_volume(instanceId:      Long, volume:  CFloat):                                          Unit = extern
  def sge_audio_set_sound_pitch(instanceId:       Long, pitch:   CFloat):                                          Unit = extern
  def sge_audio_set_sound_pan(instanceId:         Long, pan:     CFloat, volume: CFloat):                          Unit = extern
  def sge_audio_set_sound_looping(instanceId:     Long, looping: CInt):                                            Unit = extern

  // Music
  def sge_audio_create_music(engineHandle:      Long, filePath: CString):                Long   = extern
  def sge_audio_dispose_music(musicHandle:      Long):                                   Unit   = extern
  def sge_audio_play_music(musicHandle:         Long):                                   Unit   = extern
  def sge_audio_pause_music(musicHandle:        Long):                                   Unit   = extern
  def sge_audio_stop_music(musicHandle:         Long):                                   Unit   = extern
  def sge_audio_is_music_playing(musicHandle:   Long):                                   CInt   = extern
  def sge_audio_get_music_volume(musicHandle:   Long):                                   CFloat = extern
  def sge_audio_set_music_volume(musicHandle:   Long, volume:   CFloat):                 Unit   = extern
  def sge_audio_set_music_pitch(musicHandle:    Long, pitch:    CFloat):                 Unit   = extern
  def sge_audio_set_music_pan(musicHandle:      Long, pan:      CFloat, volume: CFloat): Unit   = extern
  def sge_audio_is_music_looping(musicHandle:   Long):                                   CInt   = extern
  def sge_audio_set_music_looping(musicHandle:  Long, looping:  CInt):                   Unit   = extern
  def sge_audio_set_music_position(musicHandle: Long, position: CFloat):                 Unit   = extern
  def sge_audio_get_music_position(musicHandle: Long):                                   CFloat = extern
  def sge_audio_get_music_duration(musicHandle: Long):                                   CFloat = extern

  // AudioDevice (raw PCM)
  def sge_audio_create_device(engineHandle:      Long, sampleRate: CInt, isMono:      CInt):               Long = extern
  def sge_audio_dispose_device(deviceHandle:     Long):                                                    Unit = extern
  def sge_audio_write_device(deviceHandle:       Long, data:       Ptr[Byte], offset: CInt, length: CInt): Unit = extern
  def sge_audio_set_device_volume(deviceHandle:  Long, volume:     CFloat):                                Unit = extern
  def sge_audio_pause_device(deviceHandle:       Long):                                                    Unit = extern
  def sge_audio_resume_device(deviceHandle:      Long):                                                    Unit = extern
  def sge_audio_get_device_latency(deviceHandle: Long):                                                    CInt = extern

  // Output device
  def sge_audio_get_output_devices(engineHandle:   Long, count:           Ptr[CInt]): Ptr[Ptr[Byte]] = extern
  def sge_audio_switch_output_device(engineHandle: Long, deviceName:      CString):   CInt           = extern
  def sge_audio_free_output_devices(devices:       Ptr[Ptr[Byte]], count: CInt):      Unit           = extern
}

private[sge] object AudioOpsNative extends AudioOps {

  private val UTF8 = StandardCharsets.UTF_8

  // ─── Engine lifecycle ────────────────────────────────────────────────

  override def initEngine(simultaneousSources: Int, bufferSize: Int, bufferCount: Int): Long =
    AudioOpsC.sge_audio_init_engine(simultaneousSources, bufferSize, bufferCount)

  override def shutdownEngine(engineHandle: Long): Unit =
    AudioOpsC.sge_audio_shutdown_engine(engineHandle)

  override def updateEngine(engineHandle: Long): Unit =
    AudioOpsC.sge_audio_update_engine(engineHandle)

  // ─── Sound ──────────────────────────────────────────────────────────

  override def createSound(engineHandle: Long, pcmData: Array[Byte], channels: Int, bitDepth: Int, sampleRate: Int): Long =
    AudioOpsC.sge_audio_create_sound(engineHandle, pcmData.at(0), pcmData.length, channels, bitDepth, sampleRate)

  override def disposeSound(soundHandle: Long): Unit =
    AudioOpsC.sge_audio_dispose_sound(soundHandle)

  override def playSound(soundHandle: Long, volume: Float, pitch: Float, pan: Float, loop: Boolean): Long =
    AudioOpsC.sge_audio_play_sound(soundHandle, volume, pitch, pan, if (loop) 1 else 0)

  override def stopSound(instanceId: Long): Unit =
    AudioOpsC.sge_audio_stop_sound(instanceId)

  override def pauseSound(instanceId: Long): Unit =
    AudioOpsC.sge_audio_pause_sound(instanceId)

  override def resumeSound(instanceId: Long): Unit =
    AudioOpsC.sge_audio_resume_sound(instanceId)

  override def stopAllInstances(soundHandle: Long): Unit =
    AudioOpsC.sge_audio_stop_all_instances(soundHandle)

  override def pauseAllInstances(soundHandle: Long): Unit =
    AudioOpsC.sge_audio_pause_all_instances(soundHandle)

  override def resumeAllInstances(soundHandle: Long): Unit =
    AudioOpsC.sge_audio_resume_all_instances(soundHandle)

  override def setSoundVolume(instanceId: Long, volume: Float): Unit =
    AudioOpsC.sge_audio_set_sound_volume(instanceId, volume)

  override def setSoundPitch(instanceId: Long, pitch: Float): Unit =
    AudioOpsC.sge_audio_set_sound_pitch(instanceId, pitch)

  override def setSoundPan(instanceId: Long, pan: Float, volume: Float): Unit =
    AudioOpsC.sge_audio_set_sound_pan(instanceId, pan, volume)

  override def setSoundLooping(instanceId: Long, looping: Boolean): Unit =
    AudioOpsC.sge_audio_set_sound_looping(instanceId, if (looping) 1 else 0)

  // ─── Music ──────────────────────────────────────────────────────────

  override def createMusic(engineHandle: Long, filePath: String): Long = {
    val zone = Zone.open()
    try AudioOpsC.sge_audio_create_music(engineHandle, toCString(filePath)(using zone))
    finally zone.close()
  }

  override def disposeMusic(musicHandle: Long): Unit =
    AudioOpsC.sge_audio_dispose_music(musicHandle)

  override def playMusic(musicHandle: Long): Unit =
    AudioOpsC.sge_audio_play_music(musicHandle)

  override def pauseMusic(musicHandle: Long): Unit =
    AudioOpsC.sge_audio_pause_music(musicHandle)

  override def stopMusic(musicHandle: Long): Unit =
    AudioOpsC.sge_audio_stop_music(musicHandle)

  override def isMusicPlaying(musicHandle: Long): Boolean =
    AudioOpsC.sge_audio_is_music_playing(musicHandle) != 0

  override def getMusicVolume(musicHandle: Long): Float =
    AudioOpsC.sge_audio_get_music_volume(musicHandle)

  override def setMusicVolume(musicHandle: Long, volume: Float): Unit =
    AudioOpsC.sge_audio_set_music_volume(musicHandle, volume)

  override def setMusicPitch(musicHandle: Long, pitch: Float): Unit =
    AudioOpsC.sge_audio_set_music_pitch(musicHandle, pitch)

  override def setMusicPan(musicHandle: Long, pan: Float, volume: Float): Unit =
    AudioOpsC.sge_audio_set_music_pan(musicHandle, pan, volume)

  override def isMusicLooping(musicHandle: Long): Boolean =
    AudioOpsC.sge_audio_is_music_looping(musicHandle) != 0

  override def setMusicLooping(musicHandle: Long, looping: Boolean): Unit =
    AudioOpsC.sge_audio_set_music_looping(musicHandle, if (looping) 1 else 0)

  override def setMusicPosition(musicHandle: Long, position: Float): Unit =
    AudioOpsC.sge_audio_set_music_position(musicHandle, position)

  override def getMusicPosition(musicHandle: Long): Float =
    AudioOpsC.sge_audio_get_music_position(musicHandle)

  override def getMusicDuration(musicHandle: Long): Float =
    AudioOpsC.sge_audio_get_music_duration(musicHandle)

  // ─── AudioDevice (raw PCM) ──────────────────────────────────────────

  override def createAudioDevice(engineHandle: Long, sampleRate: Int, isMono: Boolean): Long =
    AudioOpsC.sge_audio_create_device(engineHandle, sampleRate, if (isMono) 1 else 0)

  override def disposeAudioDevice(deviceHandle: Long): Unit =
    AudioOpsC.sge_audio_dispose_device(deviceHandle)

  override def writeAudioDevice(deviceHandle: Long, data: Array[Byte], offset: Int, length: Int): Unit =
    AudioOpsC.sge_audio_write_device(deviceHandle, data.at(offset), 0, length)

  override def setAudioDeviceVolume(deviceHandle: Long, volume: Float): Unit =
    AudioOpsC.sge_audio_set_device_volume(deviceHandle, volume)

  override def pauseAudioDevice(deviceHandle: Long): Unit =
    AudioOpsC.sge_audio_pause_device(deviceHandle)

  override def resumeAudioDevice(deviceHandle: Long): Unit =
    AudioOpsC.sge_audio_resume_device(deviceHandle)

  override def getAudioDeviceLatency(deviceHandle: Long): Int =
    AudioOpsC.sge_audio_get_device_latency(deviceHandle)

  // ─── Output device ──────────────────────────────────────────────────

  override def getAvailableOutputDevices(engineHandle: Long): Array[String] = {
    val count   = stackalloc[CInt]()
    val devices = AudioOpsC.sge_audio_get_output_devices(engineHandle, count)
    if (devices == null || !count <= 0) Array.empty
    else {
      val n      = !count
      val result = Array.tabulate(n)(i => fromCString(devices(i).asInstanceOf[CString], UTF8))
      AudioOpsC.sge_audio_free_output_devices(devices, n)
      result
    }
  }

  override def switchOutputDevice(engineHandle: Long, deviceName: String): Boolean =
    if (deviceName == null) {
      AudioOpsC.sge_audio_switch_output_device(engineHandle, null) != 0
    } else {
      val zone = Zone.open()
      try AudioOpsC.sge_audio_switch_output_device(engineHandle, toCString(deviceName)(using zone)) != 0
      finally zone.close()
    }
}
