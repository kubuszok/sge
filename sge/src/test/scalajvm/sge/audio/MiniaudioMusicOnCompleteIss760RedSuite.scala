// SGE — RED reproducer for ISS-760 (Music.onComplete NEVER fires on desktop JVM+Native)
//
// Contract (grounded in the libgdx lwjgl3/OpenAL backend, local reference original-src/libgdx):
//   * Lwjgl3Application.java:169 — the application loop calls audio.update() every frame.
//   * OpenALLwjgl3Audio.java:314 update() — "for (int i = 0; i < music.size; i++) music.items[i].update();"
//   * OpenALMusic.java:234 update() — when the stream is exhausted
//     ("if (end && alGetSourcei(sourceID, AL_BUFFERS_QUEUED) == 0)") it calls
//     "stop(); if (onCompletionListener != null) onCompletionListener.onCompletion(this);"
//   => completion is DRIVEN BY THE PER-FRAME AUDIO UPDATE TICK.
//
// SGE desktop port status (the bug):
//   * DesktopApplication.scala:152 calls _audio.update() every frame — the tick EXISTS.
//   * MiniaudioEngine.update() (MiniaudioEngine.scala:132) only forwards to
//     audioOps.updateEngine(engineHandle); it never inspects its musicInstances and never
//     calls MiniaudioMusic.fireOnComplete().
//   * MiniaudioMusic.fireOnComplete (MiniaudioMusic.scala:85) has ZERO callers anywhere.
//   => a listener registered via Music.onComplete can never fire on JVM or Native.
//   JS (WebAudioMusic.scala:49 "ended" event listener) and Android
//   (AndroidApplication.scala:458) both wire completion; desktop does not.
//
// This suite stubs AudioOps (no native library, headless-safe): the failure below is caused
// exclusively by the MISSING PLUMBING between the engine tick and fireOnComplete, not by any
// test-environment limitation — the green sanity test proves the callback machinery itself works.

package sge
package audio

import munit.FunSuite
import sge.files.{ FileHandle, FileType }
import sge.platform.AudioOps

class MiniaudioMusicOnCompleteIss760RedSuite extends FunSuite {

  /** Minimal fake AudioOps: nonzero engine/music handles so MiniaudioEngine takes the real (non-noDevice) code paths; music playback state is driven by the test to simulate the miniaudio decoder
    * reaching end-of-stream (a natural end, not a user stop()).
    */
  final private class FakeAudioOps extends AudioOps {
    var playing:           Boolean = false
    var position:          Float   = 0f
    var updateEngineCalls: Int     = 0

    // ─── Engine lifecycle ───────────────────────────────────────────────
    override def initEngine(simultaneousSources: Int, bufferSize: Int, bufferCount: Int): Long = 1L
    override def shutdownEngine(engineHandle:    Long):                                   Unit = ()
    override def updateEngine(engineHandle:      Long):                                   Unit = updateEngineCalls += 1

    // ─── Sound ──────────────────────────────────────────────────────────
    override def createSound(engineHandle:       Long, pcmData: Array[Byte], channels: Int, bitDepth: Int, sampleRate: Int):     Long = 7L
    override def disposeSound(soundHandle:       Long):                                                                          Unit = ()
    override def playSound(soundHandle:          Long, volume:  Float, pitch:          Float, pan:    Float, loop:     Boolean): Long = 1L
    override def stopSound(instanceId:           Long):                                                                          Unit = ()
    override def pauseSound(instanceId:          Long):                                                                          Unit = ()
    override def resumeSound(instanceId:         Long):                                                                          Unit = ()
    override def stopAllInstances(soundHandle:   Long):                                                                          Unit = ()
    override def pauseAllInstances(soundHandle:  Long):                                                                          Unit = ()
    override def resumeAllInstances(soundHandle: Long):                                                                          Unit = ()
    override def setSoundVolume(instanceId:      Long, volume:  Float):                                                          Unit = ()
    override def setSoundPitch(instanceId:       Long, pitch:   Float):                                                          Unit = ()
    override def setSoundPan(instanceId:         Long, pan:     Float, volume:         Float):                                   Unit = ()
    override def setSoundLooping(instanceId:     Long, looping: Boolean):                                                        Unit = ()

    // ─── Music ──────────────────────────────────────────────────────────
    override def createMusic(engineHandle:     Long, filePath: String):               Long    = 42L
    override def disposeMusic(musicHandle:     Long):                                 Unit    = ()
    override def playMusic(musicHandle:        Long):                                 Unit    = playing = true
    override def pauseMusic(musicHandle:       Long):                                 Unit    = playing = false
    override def stopMusic(musicHandle:        Long):                                 Unit    = { playing = false; position = 0f }
    override def isMusicPlaying(musicHandle:   Long):                                 Boolean = playing
    override def getMusicVolume(musicHandle:   Long):                                 Float   = 1f
    override def setMusicVolume(musicHandle:   Long, volume:   Float):                Unit    = ()
    override def setMusicPitch(musicHandle:    Long, pitch:    Float):                Unit    = ()
    override def setMusicPan(musicHandle:      Long, pan:      Float, volume: Float): Unit    = ()
    override def isMusicLooping(musicHandle:   Long):                                 Boolean = false
    override def setMusicLooping(musicHandle:  Long, looping:  Boolean):              Unit    = ()
    override def setMusicPosition(musicHandle: Long, position: Float):                Unit    = this.position = position
    override def getMusicPosition(musicHandle: Long):                                 Float   = position
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

  /** Creates a real MiniaudioEngine over the fake ops and registers one music track via the public newMusic path (so the engine's own musicInstances bookkeeping sees it). */
  private def newEngineWithMusic(): (MiniaudioEngine, FakeAudioOps, Music) = {
    val ops    = new FakeAudioOps
    val engine = MiniaudioEngine(ops)
    val tmp    = java.io.File.createTempFile("sge-iss760-", ".ogg")
    tmp.deleteOnExit()
    val music = engine.newMusic(new FileHandle(tmp, FileType.Absolute))
    (engine, ops, music)
  }

  test("sanity (GREEN): fireOnComplete itself invokes the registered listener") {
    val (_, _, music) = newEngineWithMusic()
    var fired         = 0
    music.onComplete(_ => fired += 1)
    music match {
      case m: MiniaudioMusic => m.fireOnComplete()
      case other => fail(s"expected MiniaudioMusic from newMusic, got ${other.getClass}")
    }
    assertEquals(fired, 1, "callback machinery works when fireOnComplete is invoked directly")
  }

  test("ISS-760 (RED): onComplete fires via the per-frame engine update tick when the track ends") {
    val (engine, ops, music) = newEngineWithMusic()
    var fired                = 0
    music.onComplete(_ => fired += 1)

    music.play()
    assert(music.playing, "precondition: music reports playing after play()")

    // Simulate the miniaudio decoder reaching end-of-stream: the native sound transitions to
    // stopped by itself (natural end — NOT a user stop()/pause()), position sits at duration.
    ops.playing = false
    ops.position = 1f

    // Drive the per-frame tick exactly as the application loop does
    // (DesktopApplication.scala:152 -> _audio.update()). In libgdx this tick
    // (OpenALLwjgl3Audio.update -> OpenALMusic.update) detects end-of-stream and calls
    // onCompletionListener.onCompletion(this). Several frames, as completion detection
    // is allowed to need more than one tick.
    var i = 0
    while (i < 10) {
      engine.update()
      i += 1
    }

    assertEquals(ops.updateEngineCalls, 10, "the engine tick reached the FFI layer")
    assertEquals(
      fired,
      1,
      "onComplete listener must fire exactly once after the track finishes " +
        "(ISS-760: no code path on desktop ever calls MiniaudioMusic.fireOnComplete)"
    )
  }
}
