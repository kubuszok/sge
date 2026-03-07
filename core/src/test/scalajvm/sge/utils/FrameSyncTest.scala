/*
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

class FrameSyncTest extends munit.FunSuite {

  test("sync(60) blocks for approximately 16ms") {
    val sync = new FrameSync()
    // warm up: first call establishes nextFrame baseline
    sync.sync(60)

    val before = TimeUtils.nanoTime()
    sync.sync(60)
    val elapsed   = TimeUtils.nanoTime() - before
    val elapsedMs = elapsed.toLong / 1000000.0

    // Should be roughly 16.6ms (1000/60). Allow wide tolerance for CI — Windows runners
    // have poor timer precision and can report 140ms+ for a single frame.
    assert(elapsedMs > 5.0, s"sync(60) should block at least 5ms, was ${elapsedMs}ms")
    assert(elapsedMs < 200.0, s"sync(60) should block less than 200ms, was ${elapsedMs}ms")
  }

  test("sync(0) returns immediately") {
    val sync   = new FrameSync()
    val before = TimeUtils.nanoTime()
    sync.sync(0)
    val elapsed   = TimeUtils.nanoTime() - before
    val elapsedMs = elapsed.toLong / 1000000.0

    assert(elapsedMs < 5.0, s"sync(0) should return immediately, took ${elapsedMs}ms")
  }

  test("sync(-1) returns immediately") {
    val sync   = new FrameSync()
    val before = TimeUtils.nanoTime()
    sync.sync(-1)
    val elapsed   = TimeUtils.nanoTime() - before
    val elapsedMs = elapsed.toLong / 1000000.0

    assert(elapsedMs < 5.0, s"sync(-1) should return immediately, took ${elapsedMs}ms")
  }
}
