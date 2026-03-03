/*
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package noop

import sge.audio.{ Pan, Pitch, SoundId, Volume }

class NoopAudioTest extends munit.FunSuite {

  // ---- NoopAudio factory methods ----

  test("newSound returns a NoopSound") {
    val audio = NoopAudio()
    val sound = audio.newSound(null) // scalastyle:ignore null
    assert(sound.isInstanceOf[NoopSound])
  }

  test("newMusic returns a NoopMusic") {
    val audio = NoopAudio()
    val music = audio.newMusic(null) // scalastyle:ignore null
    assert(music.isInstanceOf[NoopMusic])
  }

  test("newAudioDevice returns a NoopAudioDevice") {
    val audio  = NoopAudio()
    val device = audio.newAudioDevice(44100, true)
    assert(device.isInstanceOf[NoopAudioDevice])
    assert(device.isMono)
  }

  test("newAudioRecorder returns a NoopAudioRecorder") {
    val audio    = NoopAudio()
    val recorder = audio.newAudioRecorder(44100, true)
    assert(recorder.isInstanceOf[NoopAudioRecorder])
  }

  test("getAvailableOutputDevices returns empty array") {
    val audio = NoopAudio()
    assertEquals(audio.getAvailableOutputDevices.length, 0)
  }

  // ---- NoopSound ----

  test("NoopSound.play returns SoundId(0L)") {
    val sound = NoopSound()
    assertEquals(sound.play().toLong, 0L)
    assertEquals(sound.play(Volume.max).toLong, 0L)
    assertEquals(sound.play(Volume.max, Pitch.normal, Pan.center).toLong, 0L)
  }

  test("NoopSound.loop returns SoundId(0L)") {
    val sound = NoopSound()
    assertEquals(sound.loop().toLong, 0L)
    assertEquals(sound.loop(Volume.max).toLong, 0L)
    assertEquals(sound.loop(Volume.max, Pitch.normal, Pan.center).toLong, 0L)
  }

  test("NoopSound methods do not throw") {
    val sound = NoopSound()
    val id    = SoundId(0L)
    sound.stop()
    sound.pause()
    sound.resume()
    sound.stop(id)
    sound.pause(id)
    sound.resume(id)
    sound.setLooping(id, true)
    sound.setPitch(id, Pitch.normal)
    sound.setVolume(id, Volume.max)
    sound.setPan(id, Pan.center, Volume.max)
    sound.close()
  }

  // ---- NoopMusic ----

  test("NoopMusic.playing returns false") {
    val music = NoopMusic()
    assertEquals(music.playing, false)
  }

  test("NoopMusic.volume defaults to Volume.min") {
    val music = NoopMusic()
    assertEquals(music.volume.toFloat, Volume.min.toFloat)
  }

  test("NoopMusic.position defaults to 0") {
    val music = NoopMusic()
    assertEquals(music.position.toFloatSeconds, 0.0f)
  }

  test("NoopMusic.looping is tracked") {
    val music = NoopMusic()
    assertEquals(music.looping, false)
    music.looping = true
    assertEquals(music.looping, true)
  }

  test("NoopMusic.close does not throw") {
    val music = NoopMusic()
    music.play()
    music.pause()
    music.stop()
    music.close()
  }

  // ---- NoopAudioDevice ----

  test("NoopAudioDevice.close does not throw") {
    val device = NoopAudioDevice(isMono = false)
    assertEquals(device.isMono, false)
    assertEquals(device.latency, 0)
    device.writeSamples(Array.empty[Short], 0, 0)
    device.writeSamples(Array.empty[Float], 0, 0)
    device.pause()
    device.resume()
    device.close()
  }

  // ---- NoopAudioRecorder ----

  test("NoopAudioRecorder.close does not throw") {
    val recorder = NoopAudioRecorder()
    recorder.read(Array.empty[Short], 0, 0)
    recorder.close()
  }
}
