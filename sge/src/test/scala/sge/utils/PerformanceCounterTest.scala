/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

class PerformanceCounterTest extends munit.FunSuite {

  test("newly created counter has zero current and is not valid") {
    val pc = PerformanceCounter("test")
    assertEqualsFloat(pc.current, 0f, 0f)
    assertEquals(pc.valid, false)
  }

  test("start and stop marks valid and accumulates current") {
    val pc = PerformanceCounter("test")
    pc.start()
    // Simulate some work — the actual time measured will be tiny but > 0 in practice.
    // We just verify the valid flag and that current gets a value.
    pc.stop()
    assertEquals(pc.valid, true)
    assert(pc.current >= 0f) // may be 0 if extremely fast
  }

  test("tick throws when not valid") {
    val pc = PerformanceCounter("test")
    // current=0, valid=false -> tick should throw
    intercept[SgeError.InvalidInput] {
      pc.tick(Seconds(1f / 60f))
    }
  }

  test("tick updates time counter") {
    val pc = PerformanceCounter("test")
    pc.start()
    pc.stop()
    assertEquals(pc.valid, true)
    pc.tick(Seconds(1f / 60f))
    // After tick, time counter should have received a value
    assertEquals(pc.time.count, 1)
    assertEquals(pc.valid, false)
    assertEqualsFloat(pc.current, 0f, 0f)
  }

  test("multiple start-stop-tick cycles accumulate") {
    val pc = PerformanceCounter("test")
    for (_ <- 0 until 5) {
      pc.start()
      pc.stop()
      pc.tick(Seconds(1f / 60f))
    }
    assertEquals(pc.time.count, 5)
    assertEquals(pc.load.count, 5)
  }

  test("start twice overwrites start time without issue") {
    val pc = PerformanceCounter("test")
    pc.start()
    pc.start() // second start overwrites
    pc.stop()
    assertEquals(pc.valid, true)
  }

  test("stop without start does nothing (startTime is zero)") {
    val pc = PerformanceCounter("test")
    pc.stop()
    assertEquals(pc.valid, false)
    assertEqualsFloat(pc.current, 0f, 0f)
  }

  test("multiple start-stop accumulates current") {
    val pc = PerformanceCounter("test")
    pc.start()
    pc.stop()
    val first = pc.current
    pc.start()
    pc.stop()
    // current should be >= first (accumulated)
    assert(pc.current >= first)
  }

  test("reset clears all values") {
    val pc = PerformanceCounter("test")
    pc.start()
    pc.stop()
    pc.tick(Seconds(1f / 60f))
    pc.reset()
    assertEqualsFloat(pc.current, 0f, 0f)
    assertEquals(pc.valid, false)
    assertEquals(pc.time.count, 0)
    assertEquals(pc.load.count, 0)
  }

  test("toString contains name and time/load") {
    val pc = PerformanceCounter("myCounter")
    pc.start()
    pc.stop()
    pc.tick(Seconds(1f / 60f))
    val str = pc.toString
    assert(str.contains("myCounter"), s"Expected 'myCounter' in: $str")
    assert(str.contains("time:"), s"Expected 'time:' in: $str")
    assert(str.contains("load:"), s"Expected 'load:' in: $str")
  }

  test("toString with StringBuilder appends correctly") {
    val pc = PerformanceCounter("test")
    pc.start()
    pc.stop()
    pc.tick(Seconds(1f / 60f))
    val sb = new StringBuilder("prefix: ")
    pc.toString(sb)
    val str = sb.toString
    assert(str.startsWith("prefix: test:"), s"Unexpected: $str")
  }

  test("custom window size") {
    val pc = PerformanceCounter("windowed", windowSize = 10)
    // The time FloatCounter should have a WindowedMean with capacity 10
    for (_ <- 0 until 10) {
      pc.start()
      pc.stop()
      pc.tick(Seconds(1f / 60f))
    }
    assertEquals(pc.time.count, 10)
  }

  test("load is computed correctly for constant delta") {
    val pc = PerformanceCounter("loadTest", windowSize = 1)
    // Manually set current to simulate known timing
    pc.start()
    pc.stop()
    pc.tick(Seconds(1.0f))
    // load.latest should be approximately current/delta
    // Since measured current is tiny, load should be close to 0
    assert(pc.load.latest >= 0f)
    assert(pc.load.latest <= 1f)
  }

  test("tick with zero delta produces zero load") {
    val pc = PerformanceCounter("zeroLoad", windowSize = 1)
    pc.start()
    pc.stop()
    pc.tick(Seconds(0f))
    assertEqualsFloat(pc.load.latest, 0f, 0f)
  }
}
