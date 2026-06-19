/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package audio

/** ISS-557 (clause 2): Volume clamping.
  *
  * Volume.unsafeMake does NOT clamp (sge/src/main/scala/sge/audio/Volume.scala:28), and there is no clamping `apply`. The everyday construction path must clamp into the valid range [min, max].
  *
  * Expected clamped values derived from:
  *   - Volume.min = 0 (Volume.scala:30)
  *   - Volume.max = 1 (Volume.scala:31)
  *   - extension toFloat unwraps the opaque Float (Volume.scala:34)
  *
  * This suite is RED today because `Volume.apply(Float)` does not exist (no clamping apply), so it fails to compile / fails the clamp assertions.
  */
class VolumeIss557RedSuite extends munit.FunSuite {

  test("ISS-557 Volume.apply clamps above max to 1.0f") {
    assertEquals(Volume(2.0f).toFloat, 1.0f)
  }

  test("ISS-557 Volume.apply clamps below min to 0.0f") {
    assertEquals(Volume(-1.0f).toFloat, 0.0f)
  }

  test("ISS-557 Volume.apply leaves in-range values untouched") {
    assertEquals(Volume(0.5f).toFloat, 0.5f)
  }
}
