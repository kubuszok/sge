// SGE — shared headless fake AudioOps for wave-D-K desktop audio RED suites.
//
// A pure-Scala stand-in for the miniaudio FFI (sge.platform.AudioOps) that needs no native
// library: initEngine returns a nonzero handle so MiniaudioEngine takes its real (non-noDevice)
// code paths, and createSound/createMusic return nonzero handles so no AudioError is thrown.
// createSound records the (channels, bitDepth, sampleRate) it is handed so a test can assert
// what MiniaudioEngine.newSound actually passes across the FFI boundary (ISS-772).

package sge
package audio

import sge.platform.AudioOps

import scala.collection.mutable.ArrayBuffer

final class WaveDFakeAudioOps extends AudioOps {

  /** Every (channels, bitDepth, sampleRate) triple handed to createSound, in call order. */
  val createSoundArgs: ArrayBuffer[(Int, Int, Int)] = ArrayBuffer.empty

  // ─── Engine lifecycle ───────────────────────────────────────────────
  override def initEngine(simultaneousSources: Int, bufferSize: Int, bufferCount: Int): Long = 1L
  override def shutdownEngine(engineHandle:    Long):                                   Unit = ()
  override def updateEngine(engineHandle:      Long):                                   Unit = ()

  // ─── Sound ──────────────────────────────────────────────────────────
  override def createSound(engineHandle: Long, pcmData: Array[Byte], channels: Int, bitDepth: Int, sampleRate: Int): Long = {
    createSoundArgs += ((channels, bitDepth, sampleRate))
    7L
  }
  override def disposeSound(soundHandle:       Long):                                                           Unit = ()
  override def playSound(soundHandle:          Long, volume:  Float, pitch:  Float, pan: Float, loop: Boolean): Long = 1L
  override def stopSound(instanceId:           Long):                                                           Unit = ()
  override def pauseSound(instanceId:          Long):                                                           Unit = ()
  override def resumeSound(instanceId:         Long):                                                           Unit = ()
  override def stopAllInstances(soundHandle:   Long):                                                           Unit = ()
  override def pauseAllInstances(soundHandle:  Long):                                                           Unit = ()
  override def resumeAllInstances(soundHandle: Long):                                                           Unit = ()
  override def setSoundVolume(instanceId:      Long, volume:  Float):                                           Unit = ()
  override def setSoundPitch(instanceId:       Long, pitch:   Float):                                           Unit = ()
  override def setSoundPan(instanceId:         Long, pan:     Float, volume: Float):                            Unit = ()
  override def setSoundLooping(instanceId:     Long, looping: Boolean):                                         Unit = ()

  // ─── Music ──────────────────────────────────────────────────────────
  override def createMusic(engineHandle:     Long, filePath: String):               Long    = 42L
  override def disposeMusic(musicHandle:     Long):                                 Unit    = ()
  override def playMusic(musicHandle:        Long):                                 Unit    = ()
  override def pauseMusic(musicHandle:       Long):                                 Unit    = ()
  override def stopMusic(musicHandle:        Long):                                 Unit    = ()
  override def isMusicPlaying(musicHandle:   Long):                                 Boolean = false
  override def getMusicVolume(musicHandle:   Long):                                 Float   = 1f
  override def setMusicVolume(musicHandle:   Long, volume:   Float):                Unit    = ()
  override def setMusicPitch(musicHandle:    Long, pitch:    Float):                Unit    = ()
  override def setMusicPan(musicHandle:      Long, pan:      Float, volume: Float): Unit    = ()
  override def isMusicLooping(musicHandle:   Long):                                 Boolean = false
  override def setMusicLooping(musicHandle:  Long, looping:  Boolean):              Unit    = ()
  override def setMusicPosition(musicHandle: Long, position: Float):                Unit    = ()
  override def getMusicPosition(musicHandle: Long):                                 Float   = 0f
  override def getMusicDuration(musicHandle: Long):                                 Float   = 1f

  // ─── AudioDevice ────────────────────────────────────────────────────
  override def createAudioDevice(engineHandle:     Long, sampleRate: Int, isMono:         Boolean):          Long = 9L
  override def disposeAudioDevice(deviceHandle:    Long):                                                    Unit = ()
  override def writeAudioDevice(deviceHandle:      Long, data:       Array[Byte], offset: Int, length: Int): Unit = ()
  override def setAudioDeviceVolume(deviceHandle:  Long, volume:     Float):                                 Unit = ()
  override def pauseAudioDevice(deviceHandle:      Long):                                                    Unit = ()
  override def resumeAudioDevice(deviceHandle:     Long):                                                    Unit = ()
  override def getAudioDeviceLatency(deviceHandle: Long):                                                    Int  = 0

  // ─── Output device ──────────────────────────────────────────────────
  override def getAvailableOutputDevices(engineHandle: Long):                     Array[String] = Array.empty[String]
  override def switchOutputDevice(engineHandle:        Long, deviceName: String): Boolean       = true
}
