package sge

import munit.FunSuite

class FrameSyncTest extends FunSuite {

  test("sync with 0 or negative fps does nothing") {
    val sync = FrameSync()
    // Should not throw or block
    sync.sync(0)
    sync.sync(-1)
    sync.sync(-100)
  }

  test("sync initializes on first call") {
    val sync  = FrameSync()
    val start = System.nanoTime()
    sync.sync(1000) // Very high FPS so it doesn't actually sleep
    val elapsed = System.nanoTime() - start
    // Should complete quickly (under 100ms)
    assert(elapsed < 100_000_000L, s"First sync took too long: ${elapsed}ns")
  }

  test("sync limits frame rate") {
    val sync      = FrameSync()
    val targetFps = 30
    val start     = System.nanoTime()

    // Run 3 frames at 30 FPS — should take ~100ms
    sync.sync(targetFps)
    sync.sync(targetFps)
    sync.sync(targetFps)

    val elapsed   = System.nanoTime() - start
    val elapsedMs = elapsed / 1_000_000L

    // Should take at least ~60ms (2 frame intervals, first frame is immediate)
    // Allow generous tolerance for CI
    assert(elapsedMs >= 30, s"3 frames at 30fps should take >=30ms, took ${elapsedMs}ms")
  }

  test("multiple syncs don't accumulate unbounded delay") {
    val sync  = FrameSync()
    val start = System.nanoTime()

    // Run 5 frames at high FPS
    for (_ <- 0 until 5) sync.sync(500)

    val elapsed   = System.nanoTime() - start
    val elapsedMs = elapsed / 1_000_000L

    // At 500 FPS, 5 frames should take ~10ms, definitely under 200ms
    assert(elapsedMs < 200, s"5 frames at 500fps took ${elapsedMs}ms, expected <200ms")
  }
}
