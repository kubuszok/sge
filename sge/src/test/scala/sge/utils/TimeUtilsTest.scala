/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

class TimeUtilsTest extends munit.FunSuite {

  test("nanoTime returns positive value") {
    val t = TimeUtils.nanoTime()
    assert(t > Nanos.zero)
  }

  test("millis returns positive value") {
    val t = TimeUtils.millis()
    assert(t > Millis.zero)
  }

  test("nanoTime is monotonically non-decreasing") {
    val t1 = TimeUtils.nanoTime()
    val t2 = TimeUtils.nanoTime()
    assert(t2 >= t1)
  }

  test("millis is monotonically non-decreasing") {
    val t1 = TimeUtils.millis()
    val t2 = TimeUtils.millis()
    assert(t2 >= t1)
  }

  test("nanosToMillis conversion") {
    val nanos  = Nanos(5_000_000L) // 5 ms
    val millis = TimeUtils.nanosToMillis(nanos)
    assertEquals(millis.toLong, 5L)
  }

  test("nanosToMillis truncates sub-millisecond") {
    val nanos  = Nanos(1_500_000L) // 1.5 ms
    val millis = TimeUtils.nanosToMillis(nanos)
    assertEquals(millis.toLong, 1L) // truncated
  }

  test("millisToNanos conversion") {
    val millis = Millis(3L)
    val nanos  = TimeUtils.millisToNanos(millis)
    assertEquals(nanos.toLong, 3_000_000L)
  }

  test("nanosToMillis and millisToNanos round-trip") {
    val original     = Millis(42L)
    val roundTripped = TimeUtils.nanosToMillis(TimeUtils.millisToNanos(original))
    assertEquals(roundTripped.toLong, original.toLong)
  }

  test("timeSinceNanos returns non-negative value") {
    val prev    = TimeUtils.nanoTime()
    val elapsed = TimeUtils.timeSinceNanos(prev)
    assert(elapsed >= Nanos.zero)
  }

  test("timeSinceMillis returns non-negative value") {
    val prev    = TimeUtils.millis()
    val elapsed = TimeUtils.timeSinceMillis(prev)
    assert(elapsed >= Millis.zero)
  }

  test("nanosToMillis of zero is zero") {
    assertEquals(TimeUtils.nanosToMillis(Nanos.zero).toLong, 0L)
  }

  test("millisToNanos of zero is zero") {
    assertEquals(TimeUtils.millisToNanos(Millis.zero).toLong, 0L)
  }

  test("large nanosToMillis conversion") {
    val nanos  = Nanos(1_000_000_000L) // 1 second = 1000 ms
    val millis = TimeUtils.nanosToMillis(nanos)
    assertEquals(millis.toLong, 1000L)
  }
}
