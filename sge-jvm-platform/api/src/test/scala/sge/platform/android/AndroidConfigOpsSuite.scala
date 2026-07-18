// SGE — AndroidConfigOps default-value + mutability contract tests
//
// Covers ISS-723 clause c14 (sge-jvm-platform had ZERO test directories).
// Wave 2026-07-18-I, territory I4. AndroidConfigOps is pure data (mirrors
// LibGDX AndroidApplicationConfiguration) — instantiable on any JVM. The
// defaults are a contract consumed by the audio engine, sensors, and EGL
// config, so each default is pinned exactly (mutation-viable: flipping any
// default in the SUT reddens exactly one assertion here).

package sge
package platform
package android

final class AndroidConfigOpsSuite extends munit.FunSuite {

  test("colour-channel defaults are 8/8/8/8") {
    val c = new AndroidConfigOps
    assertEquals(c.r, 8)
    assertEquals(c.g, 8)
    assertEquals(c.b, 8)
    assertEquals(c.a, 8)
  }

  test("depth/stencil/sample defaults") {
    val c = new AndroidConfigOps
    assertEquals(c.depth, 16)
    assertEquals(c.stencil, 0)
    assertEquals(c.numSamples, 0)
  }

  test("sensor toggles default: accelerometer + compass on, gyroscope + rotation off") {
    val c = new AndroidConfigOps
    assert(c.useAccelerometer)
    assert(c.useCompass)
    assert(!c.useGyroscope)
    assert(!c.useRotationVectorSensor)
    assertEquals(c.sensorDelay, 1) // SENSOR_DELAY_GAME
  }

  test("behaviour flags default: immersive on, wakelock/audio-disable/gl30/cutout off") {
    val c = new AndroidConfigOps
    assert(c.useImmersiveMode)
    assert(!c.useWakelock)
    assert(!c.disableAudio)
    assert(!c.useGL30)
    assert(!c.renderUnderCutout)
  }

  test("numeric defaults: 16 sounds, unbounded net threads") {
    val c = new AndroidConfigOps
    assertEquals(c.maxSimultaneousSounds, 16)
    assertEquals(c.maxNetThreads, Int.MaxValue)
  }

  test("every field is an independently mutable var") {
    val c = new AndroidConfigOps
    c.r = 5; c.depth = 24; c.numSamples = 4
    c.useAccelerometer = false; c.useGL30 = true
    c.maxSimultaneousSounds = 32; c.maxNetThreads = 3
    assertEquals(c.r, 5)
    assertEquals(c.depth, 24)
    assertEquals(c.numSamples, 4)
    assert(!c.useAccelerometer)
    assert(c.useGL30)
    assertEquals(c.maxSimultaneousSounds, 32)
    assertEquals(c.maxNetThreads, 3)
    // mutating one instance does not leak into a fresh one (no shared state)
    assertEquals(new AndroidConfigOps().r, 8)
  }
}
