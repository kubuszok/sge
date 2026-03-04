/*
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package audio

class AudioUtilsTest extends munit.FunSuite {

  private val Eps = 0.001f

  private def assertClose(actual: Float, expected: Float, label: String): Unit =
    assert(scala.math.abs(actual - expected) < Eps, s"$label: expected $expected, got $actual")

  // ---- center pan ----

  test("center pan with max volume produces equal channels") {
    val (left, right) = AudioUtils.panToStereoVolumes(Pan.center, Volume.max)
    assertClose(left, 1.0f, "left")
    assertClose(right, 1.0f, "right")
  }

  // ---- full left ----

  test("full left pan with max volume produces left=1, right=0") {
    val (left, right) = AudioUtils.panToStereoVolumes(Pan.maxLeft, Volume.max)
    assertClose(left, 1.0f, "left")
    assertClose(right, 0.0f, "right")
  }

  // ---- full right ----

  test("full right pan with max volume produces left=0, right=1") {
    val (left, right) = AudioUtils.panToStereoVolumes(Pan.maxRight, Volume.max)
    assertClose(left, 0.0f, "left")
    assertClose(right, 1.0f, "right")
  }

  // ---- zero volume ----

  test("zero volume produces silent channels regardless of pan") {
    val (left, right) = AudioUtils.panToStereoVolumes(Pan.center, Volume.min)
    assertClose(left, 0.0f, "left")
    assertClose(right, 0.0f, "right")
  }

  // ---- volumes never exceed input volume ----

  test("stereo volumes never exceed input volume") {
    val pans    = List(-1.0f, -0.5f, 0.0f, 0.5f, 1.0f)
    val volumes = List(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)
    for {
      p <- pans
      v <- volumes
    } {
      val pan           = Pan.unsafeMake(p)
      val volume        = Volume.unsafeMake(v)
      val (left, right) = AudioUtils.panToStereoVolumes(pan, volume)
      assert(left >= 0.0f && left <= v + Eps, s"left=$left out of range for pan=$p, vol=$v")
      assert(right >= 0.0f && right <= v + Eps, s"right=$right out of range for pan=$p, vol=$v")
    }
  }
}
