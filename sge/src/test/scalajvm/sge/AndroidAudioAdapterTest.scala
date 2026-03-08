// SGE — Tests for Android audio adapter wiring
//
// Verifies that AndroidAudio and its sub-adapters (Sound, Music, AudioDevice,
// AudioRecorder) correctly delegate to the ops interfaces.

package sge

import munit.FunSuite
import sge.audio.{ Pan, Pitch, Position, SoundId, Volume }
import sge.platform.android._

class AndroidAudioAdapterTest extends FunSuite {

  // ── Stub SoundOps ──────────────────────────────────────────────────

  private class StubSoundOps extends SoundOps {
    var lastPlayVolume:         Float   = -1f
    var lastPlayPitch:          Float   = -1f
    var lastPlayPan:            Float   = -1f
    var lastLoopVolume:         Float   = -1f
    var lastStopStreamId:       Long    = -1L
    var lastPauseStreamId:      Long    = -1L
    var lastResumeStreamId:     Long    = -1L
    var lastSetPitchStreamId:   Long    = -1L
    var lastSetPitchValue:      Float   = -1f
    var lastSetVolumeStreamId:  Long    = -1L
    var lastSetVolumeValue:     Float   = -1f
    var lastSetLoopingStreamId: Long    = -1L
    var lastSetLoopingValue:    Boolean = false
    var lastSetPanStreamId:     Long    = -1L
    var lastSetPanValue:        Float   = -1f
    var lastSetPanVolume:       Float   = -1f
    var disposed:               Boolean = false
    var stopAllCalled:          Boolean = false
    var pauseAllCalled:         Boolean = false
    var resumeAllCalled:        Boolean = false

    override def play(volume: Float):                           Long = { lastPlayVolume = volume; 42L }
    override def play(volume: Float, pitch: Float, pan: Float): Long = {
      lastPlayVolume = volume; lastPlayPitch = pitch; lastPlayPan = pan; 43L
    }
    override def loop(volume:         Float):                               Long = { lastLoopVolume = volume; 44L }
    override def loop(volume:         Float, pitch:  Float, pan:    Float): Long = { lastLoopVolume = volume; 45L }
    override def stop():                                                    Unit = stopAllCalled = true
    override def stop(streamId:       Long):                                Unit = lastStopStreamId = streamId
    override def pause():                                                   Unit = pauseAllCalled = true
    override def pause(streamId:      Long):                                Unit = lastPauseStreamId = streamId
    override def resume():                                                  Unit = resumeAllCalled = true
    override def resume(streamId:     Long):                                Unit = lastResumeStreamId = streamId
    override def setPitch(streamId:   Long, pitch:   Float):                Unit = { lastSetPitchStreamId = streamId; lastSetPitchValue = pitch }
    override def setVolume(streamId:  Long, volume:  Float):                Unit = { lastSetVolumeStreamId = streamId; lastSetVolumeValue = volume }
    override def setLooping(streamId: Long, looping: Boolean):              Unit = { lastSetLoopingStreamId = streamId; lastSetLoopingValue = looping }
    override def setPan(streamId:     Long, pan:     Float, volume: Float): Unit = { lastSetPanStreamId = streamId; lastSetPanValue = pan; lastSetPanVolume = volume }
    override def dispose():                                                 Unit = disposed = true
  }

  // ── Stub MusicOps ──────────────────────────────────────────────────

  private class StubMusicOps extends MusicOps {
    var _playing:      Boolean    = false
    var _looping:      Boolean    = false
    var _volume:       Float      = 1f
    var _position:     Float      = 0f
    var _duration:     Float      = 120f
    var playCalled:    Boolean    = false
    var pauseCalled:   Boolean    = false
    var stopCalled:    Boolean    = false
    var onCompleteCb:  () => Unit = () => ()
    var disposed:      Boolean    = false
    var wasPlaying:    Boolean    = false
    var lastPan:       Float      = 0f
    var lastPanVolume: Float      = 0f

    override def play():                                            Unit    = { playCalled = true; _playing = true }
    override def pause():                                           Unit    = { pauseCalled = true; _playing = false }
    override def stop():                                            Unit    = { stopCalled = true; _playing = false }
    override def playing:                                           Boolean = _playing
    override def looping:                                           Boolean = _looping
    override def looping_=(isLooping:        Boolean):              Unit    = _looping = isLooping
    override def volume_=(volume:            Float):                Unit    = _volume = volume
    override def volume:                                            Float   = _volume
    override def setPan(pan:                 Float, volume: Float): Unit    = { lastPan = pan; lastPanVolume = volume }
    override def position_=(positionSeconds: Float):                Unit    = _position = positionSeconds
    override def position:                                          Float   = _position
    override def duration:                                          Float   = _duration
    override def onComplete(callback:        () => Unit):           Unit    = onCompleteCb = callback
    override def dispose():                                         Unit    = disposed = true
  }

  // ── Stub AudioDeviceOps ────────────────────────────────────────────

  private class StubAudioDeviceOps extends AudioDeviceOps {
    var _isMono:          Boolean      = true
    var lastShortSamples: Array[Short] = Array.empty
    var lastFloatSamples: Array[Float] = Array.empty
    var lastVolume:       Float        = -1f
    var paused:           Boolean      = false
    var disposed:         Boolean      = false

    override def isMono:                                                            Boolean = _isMono
    override def writeSamples(samples: Array[Short], offset: Int, numSamples: Int): Unit    =
      lastShortSamples = samples.slice(offset, offset + numSamples)
    override def writeSamples(samples: Array[Float], offset: Int, numSamples: Int): Unit =
      lastFloatSamples = samples.slice(offset, offset + numSamples)
    override def setVolume(volume: Float): Unit = lastVolume = volume
    override def pause():                  Unit = paused = true
    override def resume():                 Unit = paused = false
    override def dispose():                Unit = disposed = true
  }

  // ── Stub AudioRecorderOps ──────────────────────────────────────────

  private class StubAudioRecorderOps extends AudioRecorderOps {
    var lastNumSamples: Int     = 0
    var disposed:       Boolean = false

    override def read(numSamples: Int): Array[Short] = {
      lastNumSamples = numSamples
      Array.fill(numSamples)(42.toShort)
    }
    override def dispose(): Unit = disposed = true
  }

  // ── Sound adapter tests ────────────────────────────────────────────

  test("Sound.play() delegates with default volume") {
    val ops   = StubSoundOps()
    val sound = AndroidSoundAdapter(ops)
    val id    = sound.play()
    assertEquals(id.toLong, 42L)
    assertEqualsFloat(ops.lastPlayVolume, 1f, 0.001f)
  }

  test("Sound.play(volume) delegates volume") {
    val ops   = StubSoundOps()
    val sound = AndroidSoundAdapter(ops)
    sound.play(Volume.unsafeMake(0.5f))
    assertEqualsFloat(ops.lastPlayVolume, 0.5f, 0.001f)
  }

  test("Sound.play(volume, pitch, pan) delegates all params") {
    val ops   = StubSoundOps()
    val sound = AndroidSoundAdapter(ops)
    val id    = sound.play(Volume.unsafeMake(0.8f), Pitch.unsafeMake(1.5f), Pan.unsafeMake(-0.5f))
    assertEquals(id.toLong, 43L)
    assertEqualsFloat(ops.lastPlayVolume, 0.8f, 0.001f)
    assertEqualsFloat(ops.lastPlayPitch, 1.5f, 0.001f)
    assertEqualsFloat(ops.lastPlayPan, -0.5f, 0.001f)
  }

  test("Sound.loop(volume) delegates") {
    val ops   = StubSoundOps()
    val sound = AndroidSoundAdapter(ops)
    val id    = sound.loop(Volume.unsafeMake(0.7f))
    assertEquals(id.toLong, 44L)
    assertEqualsFloat(ops.lastLoopVolume, 0.7f, 0.001f)
  }

  test("Sound.stop/pause/resume delegate") {
    val ops   = StubSoundOps()
    val sound = AndroidSoundAdapter(ops)
    sound.stop()
    assert(ops.stopAllCalled)
    sound.pause()
    assert(ops.pauseAllCalled)
    sound.resume()
    assert(ops.resumeAllCalled)
  }

  test("Sound.stop/pause/resume with streamId delegate") {
    val ops   = StubSoundOps()
    val sound = AndroidSoundAdapter(ops)
    sound.stop(SoundId(99L))
    assertEquals(ops.lastStopStreamId, 99L)
    sound.pause(SoundId(100L))
    assertEquals(ops.lastPauseStreamId, 100L)
    sound.resume(SoundId(101L))
    assertEquals(ops.lastResumeStreamId, 101L)
  }

  test("Sound.setPitch delegates") {
    val ops   = StubSoundOps()
    val sound = AndroidSoundAdapter(ops)
    sound.setPitch(SoundId(10L), Pitch.unsafeMake(1.2f))
    assertEquals(ops.lastSetPitchStreamId, 10L)
    assertEqualsFloat(ops.lastSetPitchValue, 1.2f, 0.001f)
  }

  test("Sound.setVolume delegates") {
    val ops   = StubSoundOps()
    val sound = AndroidSoundAdapter(ops)
    sound.setVolume(SoundId(11L), Volume.unsafeMake(0.3f))
    assertEquals(ops.lastSetVolumeStreamId, 11L)
    assertEqualsFloat(ops.lastSetVolumeValue, 0.3f, 0.001f)
  }

  test("Sound.setLooping delegates") {
    val ops   = StubSoundOps()
    val sound = AndroidSoundAdapter(ops)
    sound.setLooping(SoundId(12L), true)
    assertEquals(ops.lastSetLoopingStreamId, 12L)
    assert(ops.lastSetLoopingValue)
  }

  test("Sound.setPan delegates") {
    val ops   = StubSoundOps()
    val sound = AndroidSoundAdapter(ops)
    sound.setPan(SoundId(13L), Pan.unsafeMake(0.5f), Volume.unsafeMake(0.9f))
    assertEquals(ops.lastSetPanStreamId, 13L)
    assertEqualsFloat(ops.lastSetPanValue, 0.5f, 0.001f)
    assertEqualsFloat(ops.lastSetPanVolume, 0.9f, 0.001f)
  }

  test("Sound.close disposes ops") {
    val ops   = StubSoundOps()
    val sound = AndroidSoundAdapter(ops)
    sound.close()
    assert(ops.disposed)
  }

  // ── Music adapter tests ────────────────────────────────────────────

  test("Music play/pause/stop delegate") {
    val ops   = StubMusicOps()
    val music = AndroidMusicAdapter(ops)
    music.play()
    assert(ops.playCalled)
    assert(music.playing)
    music.pause()
    assert(ops.pauseCalled)
    assert(!music.playing)
    music.stop()
    assert(ops.stopCalled)
  }

  test("Music looping property delegates") {
    val ops   = StubMusicOps()
    val music = AndroidMusicAdapter(ops)
    assert(!music.looping)
    music.looping = true
    assert(ops._looping)
    assert(music.looping)
  }

  test("Music volume property delegates") {
    val ops   = StubMusicOps()
    val music = AndroidMusicAdapter(ops)
    assertEqualsFloat(music.volume.toFloat, 1f, 0.001f)
    music.volume = Volume.unsafeMake(0.4f)
    assertEqualsFloat(ops._volume, 0.4f, 0.001f)
  }

  test("Music position property delegates") {
    val ops   = StubMusicOps()
    val music = AndroidMusicAdapter(ops)
    music.position = Position.unsafeMake(30f)
    assertEqualsFloat(ops._position, 30f, 0.001f)
    assertEqualsFloat(music.position.toFloatSeconds, 30f, 0.001f)
  }

  test("Music setPan delegates") {
    val ops   = StubMusicOps()
    val music = AndroidMusicAdapter(ops)
    music.setPan(Pan.unsafeMake(-1f), Volume.unsafeMake(0.5f))
    assertEqualsFloat(ops.lastPan, -1f, 0.001f)
    assertEqualsFloat(ops.lastPanVolume, 0.5f, 0.001f)
  }

  test("Music onComplete callback bridges correctly") {
    val ops            = StubMusicOps()
    val music          = AndroidMusicAdapter(ops)
    var callbackCalled = false
    music.onComplete { m =>
      callbackCalled = true
      assert(m eq music) // should receive the adapter, not the ops
    }
    ops.onCompleteCb() // simulate playback completion
    assert(callbackCalled)
  }

  test("Music.close disposes ops") {
    val ops   = StubMusicOps()
    val music = AndroidMusicAdapter(ops)
    music.close()
    assert(ops.disposed)
  }

  // ── AudioDevice adapter tests ──────────────────────────────────────

  test("AudioDevice.isMono delegates") {
    val ops = StubAudioDeviceOps()
    ops._isMono = false
    val device = AndroidAudioDeviceAdapter(ops)
    assert(!device.isMono)
  }

  test("AudioDevice.writeSamples(short) delegates") {
    val ops     = StubAudioDeviceOps()
    val device  = AndroidAudioDeviceAdapter(ops)
    val samples = Array[Short](1, 2, 3, 4, 5)
    device.writeSamples(samples, 1, 3)
    assertEquals(ops.lastShortSamples.toList, List[Short](2, 3, 4))
  }

  test("AudioDevice.writeSamples(float) delegates") {
    val ops     = StubAudioDeviceOps()
    val device  = AndroidAudioDeviceAdapter(ops)
    val samples = Array[Float](0.1f, 0.2f, 0.3f)
    device.writeSamples(samples, 0, 3)
    assertEquals(ops.lastFloatSamples.length, 3)
  }

  test("AudioDevice.setVolume delegates") {
    val ops    = StubAudioDeviceOps()
    val device = AndroidAudioDeviceAdapter(ops)
    device.setVolume(Volume.unsafeMake(0.6f))
    assertEqualsFloat(ops.lastVolume, 0.6f, 0.001f)
  }

  test("AudioDevice.pause/resume delegate") {
    val ops    = StubAudioDeviceOps()
    val device = AndroidAudioDeviceAdapter(ops)
    device.pause()
    assert(ops.paused)
    device.resume()
    assert(!ops.paused)
  }

  test("AudioDevice.close disposes ops") {
    val ops    = StubAudioDeviceOps()
    val device = AndroidAudioDeviceAdapter(ops)
    device.close()
    assert(ops.disposed)
  }

  // ── AudioRecorder adapter tests ────────────────────────────────────

  test("AudioRecorder.read delegates and copies to output array") {
    val ops      = StubAudioRecorderOps()
    val recorder = AndroidAudioRecorderAdapter(ops)
    val out      = new Array[Short](10)
    recorder.read(out, 2, 5)
    assertEquals(ops.lastNumSamples, 5)
    assertEquals(out(2), 42.toShort)
    assertEquals(out(6), 42.toShort)
    assertEquals(out(0), 0.toShort) // untouched
  }

  test("AudioRecorder.close disposes ops") {
    val ops      = StubAudioRecorderOps()
    val recorder = AndroidAudioRecorderAdapter(ops)
    recorder.close()
    assert(ops.disposed)
  }
}
