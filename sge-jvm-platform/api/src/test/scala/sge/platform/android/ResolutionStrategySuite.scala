// SGE — Resolution strategy behavioural tests
//
// Covers ISS-723 clause c14 (sge-jvm-platform had ZERO test directories).
// Wave 2026-07-18-I, territory I4. These are the pure-JVM resolution-strategy
// classes from the api module (FillResolutionStrategy, FixedResolutionStrategy,
// RatioResolutionStrategy) — headlessly testable, no Android SDK, no display.
//
// Exact-value assertions (mutation-viable): the RatioResolutionStrategy aspect
// math and rounding are ported from
// com.badlogic.gdx.backends.android.surfaceview.RatioResolutionStrategy.

package sge
package platform
package android

final class ResolutionStrategySuite extends munit.FunSuite {

  // ── FillResolutionStrategy ────────────────────────────────────────────

  test("FillResolutionStrategy returns the available size unchanged") {
    assertEquals(FillResolutionStrategy.calcMeasures(1920, 1080), (1920, 1080))
    assertEquals(FillResolutionStrategy.calcMeasures(0, 0), (0, 0))
    assertEquals(FillResolutionStrategy.calcMeasures(7, 13), (7, 13))
  }

  test("FillResolutionStrategy is a ResolutionStrategyOps") {
    val s: ResolutionStrategyOps = FillResolutionStrategy
    assertEquals(s.calcMeasures(640, 480), (640, 480))
  }

  // ── FixedResolutionStrategy ───────────────────────────────────────────

  test("FixedResolutionStrategy always returns its fixed dimensions") {
    val s = FixedResolutionStrategy(320, 240)
    assertEquals(s.calcMeasures(1920, 1080), (320, 240))
    assertEquals(s.calcMeasures(1, 1), (320, 240))
    assertEquals(s.calcMeasures(0, 0), (320, 240))
  }

  test("FixedResolutionStrategy exposes width/height and case-class equality") {
    val s = FixedResolutionStrategy(800, 600)
    assertEquals(s.width, 800)
    assertEquals(s.height, 600)
    assertEquals(s, FixedResolutionStrategy(800, 600))
    assertNotEquals(s, FixedResolutionStrategy(600, 800))
  }

  // ── RatioResolutionStrategy ───────────────────────────────────────────

  test("RatioResolutionStrategy: realRatio < ratio clamps width, derives height") {
    // 16:9 target; a square (realRatio 1.0 < 1.778) keeps width, shrinks height.
    val s = RatioResolutionStrategy(16f / 9f)
    // height = round(400 / (16/9)) = round(225.0) = 225
    assertEquals(s.calcMeasures(400, 400), (400, 225))
  }

  test("RatioResolutionStrategy: realRatio >= ratio keeps height, derives width") {
    val s = RatioResolutionStrategy(16f / 9f)
    // realRatio = 800/400 = 2.0 >= 1.778 -> height kept, width = round(400*1.778)
    assertEquals(s.calcMeasures(800, 400), (711, 400))
  }

  test("RatioResolutionStrategy: equal ratio takes the >= branch (height kept)") {
    val s = RatioResolutionStrategy(1f) // square target
    // realRatio 1.0 is NOT < 1.0, so else-branch: height=500, width=round(500*1)=500
    assertEquals(s.calcMeasures(500, 500), (500, 500))
  }

  test("RatioResolutionStrategy: rounds the derived dimension (Math.round)") {
    val s = RatioResolutionStrategy(3f)
    // realRatio 1.0 < 3.0 -> width=100, height=round(100/3)=round(33.333)=33
    assertEquals(s.calcMeasures(100, 100), (100, 33))
    // realRatio 5.0 >= 3.0 -> height=100, width=round(100*3)=300
    assertEquals(s.calcMeasures(500, 100), (300, 100))
  }

  test("RatioResolutionStrategy: the (width, height) apply computes ratio = w/h") {
    val s = RatioResolutionStrategy(16f, 9f)
    assertEquals(s.ratio, 16f / 9f)
    // behaves identically to the single-arg form
    assertEquals(s.calcMeasures(800, 400), RatioResolutionStrategy(16f / 9f).calcMeasures(800, 400))
  }

  test("RatioResolutionStrategy: tall target (ratio < 1) derives correctly") {
    val s = RatioResolutionStrategy(0.5f) // 1:2 portrait target
    // realRatio 1.0 >= 0.5 -> height=200, width=round(200*0.5)=100
    assertEquals(s.calcMeasures(200, 200), (100, 200))
    // realRatio 0.25 < 0.5 -> width=100, height=round(100/0.5)=200
    assertEquals(s.calcMeasures(100, 400), (100, 200))
  }
}
