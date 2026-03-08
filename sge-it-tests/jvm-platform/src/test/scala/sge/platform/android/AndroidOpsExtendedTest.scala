// SGE — Integration test: Extended Android ops API interfaces
//
// Tests the newly added ops interfaces: ResolutionStrategy, FilesOps,
// AudioEngineOps/SoundOps/MusicOps, HapticsOps.

package sge
package platform
package android

import munit.FunSuite

class AndroidOpsExtendedTest extends FunSuite {

  // ── Resolution Strategies ───────────────────────────────────────────

  test("FillResolutionStrategy returns input dimensions") {
    assertEquals(FillResolutionStrategy.calcMeasures(800, 600), (800, 600))
    assertEquals(FillResolutionStrategy.calcMeasures(1920, 1080), (1920, 1080))
  }

  test("FixedResolutionStrategy ignores available dimensions") {
    val strategy = FixedResolutionStrategy(320, 240)
    assertEquals(strategy.calcMeasures(800, 600), (320, 240))
    assertEquals(strategy.calcMeasures(1920, 1080), (320, 240))
  }

  test("RatioResolutionStrategy maintains aspect ratio - wider") {
    // Available: 800x600 (ratio 1.33), Desired: 16/9 = 1.78
    val strategy = RatioResolutionStrategy(16f / 9f)
    val (w, h)   = strategy.calcMeasures(800, 600)
    // realRatio (1.33) < desiredRatio (1.78) → width-limited
    assertEquals(w, 800)
    // height = round(800 / 1.778) = round(450) = 450
    assert(h < 600, s"Height $h should be less than 600")
    assert(h > 400, s"Height $h should be reasonable")
  }

  test("RatioResolutionStrategy maintains aspect ratio - taller") {
    // Available: 600x800 (ratio 0.75), Desired: 16/9 = 1.78
    val strategy = RatioResolutionStrategy(16f / 9f)
    val (w, h)   = strategy.calcMeasures(600, 800)
    // realRatio (0.75) < desiredRatio (1.78) → width-limited
    assertEquals(w, 600)
    assert(h < 800)
  }

  test("RatioResolutionStrategy factory from width/height") {
    val strategy = RatioResolutionStrategy(1920f, 1080f)
    // Should be the same as RatioResolutionStrategy(1920f / 1080f)
    assertEqualsFloat(strategy.ratio, 1920f / 1080f, 0.001f)
  }

  test("FixedResolutionStrategy is a case class") {
    val a = FixedResolutionStrategy(320, 240)
    val b = FixedResolutionStrategy(320, 240)
    assertEquals(a, b)
  }

  // ── FilesOps trait shape ────────────────────────────────────────────

  test("FilesOps trait has expected methods") {
    val cls = classOf[FilesOps]
    assert(cls.getMethod("openInternal", classOf[String]) != null)
    assert(cls.getMethod("listInternal", classOf[String]) != null)
    assert(cls.getMethod("openInternalFd", classOf[String]) != null)
    assert(cls.getMethod("internalFileLength", classOf[String]) != null)
    assert(cls.getMethod("localStoragePath") != null)
    assert(cls.getMethod("externalStoragePath") != null)
  }

  // ── Audio ops trait shapes ──────────────────────────────────────────

  test("SoundOps trait has expected methods") {
    val cls = classOf[SoundOps]
    assert(cls.getMethod("play", classOf[Float]) != null)
    assert(cls.getMethod("play", classOf[Float], classOf[Float], classOf[Float]) != null)
    assert(cls.getMethod("loop", classOf[Float]) != null)
    assert(cls.getMethod("stop") != null)
    assert(cls.getMethod("pause") != null)
    assert(cls.getMethod("resume") != null)
    assert(cls.getMethod("setPitch", classOf[Long], classOf[Float]) != null)
    assert(cls.getMethod("setVolume", classOf[Long], classOf[Float]) != null)
    assert(cls.getMethod("dispose") != null)
  }

  test("MusicOps trait has expected methods") {
    val cls = classOf[MusicOps]
    assert(cls.getMethod("play") != null)
    assert(cls.getMethod("pause") != null)
    assert(cls.getMethod("stop") != null)
    assert(cls.getMethod("playing") != null)
    assert(cls.getMethod("looping") != null)
    assert(cls.getMethod("volume") != null)
    assert(cls.getMethod("position") != null)
    assert(cls.getMethod("duration") != null)
    assert(cls.getMethod("dispose") != null)
  }

  test("AudioEngineOps trait has expected factory methods") {
    val cls = classOf[AudioEngineOps]
    assert(cls.getMethod("newSoundFromFd", classOf[java.io.FileDescriptor], classOf[Long], classOf[Long]) != null)
    assert(cls.getMethod("newSoundFromPath", classOf[String]) != null)
    assert(cls.getMethod("newMusicFromFd", classOf[java.io.FileDescriptor], classOf[Long], classOf[Long]) != null)
    assert(cls.getMethod("newMusicFromPath", classOf[String]) != null)
    assert(cls.getMethod("newAudioDevice", classOf[Int], classOf[Boolean]) != null)
    assert(cls.getMethod("newAudioRecorder", classOf[Int], classOf[Boolean]) != null)
    assert(cls.getMethod("pauseAll") != null)
    assert(cls.getMethod("resumeAll") != null)
    assert(cls.getMethod("dispose") != null)
  }

  test("AudioDeviceOps trait has expected methods") {
    val cls = classOf[AudioDeviceOps]
    assert(cls.getMethod("isMono") != null)
    assert(cls.getMethod("writeSamples", classOf[Array[Short]], classOf[Int], classOf[Int]) != null)
    assert(cls.getMethod("writeSamples", classOf[Array[Float]], classOf[Int], classOf[Int]) != null)
    assert(cls.getMethod("setVolume", classOf[Float]) != null)
    assert(cls.getMethod("dispose") != null)
  }

  test("AudioRecorderOps trait has expected methods") {
    val cls = classOf[AudioRecorderOps]
    assert(cls.getMethod("read", classOf[Int]) != null)
    assert(cls.getMethod("dispose") != null)
  }

  // ── HapticsOps trait shape ──────────────────────────────────────────

  test("HapticsOps trait has expected methods") {
    val cls = classOf[HapticsOps]
    assert(cls.getMethod("vibrate", classOf[Int]) != null)
    assert(cls.getMethod("vibrateHaptic", classOf[Int]) != null)
    assert(cls.getMethod("vibrateWithIntensity", classOf[Int], classOf[Int], classOf[Boolean]) != null)
    assert(cls.getMethod("hasVibratorAvailable") != null)
    assert(cls.getMethod("hasHapticsSupport") != null)
  }

  // ── AndroidPlatformProvider extended methods ────────────────────────

  test("AndroidPlatformProvider has new factory methods") {
    val cls = classOf[AndroidPlatformProvider]
    assert(cls.getMethod("createFiles", classOf[AnyRef], classOf[Boolean]) != null)
    assert(cls.getMethod("createAudioEngine", classOf[AnyRef], classOf[AndroidConfigOps]) != null)
    assert(cls.getMethod("createHaptics", classOf[AnyRef]) != null)
  }
}
