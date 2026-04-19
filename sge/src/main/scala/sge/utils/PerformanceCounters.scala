/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/PerformanceCounters.java
 * Original authors: xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `Array<PerformanceCounter>` -> `DynamicArray[PerformanceCounter]`
 *   Idiom: split packages
 *   Issues: None
 *   Convention: opaque Seconds for tick(deltaTime) param; opaque Nanos for internal timestamps
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 64
 * Covenant-baseline-methods: PerformanceCounters,add,counters,i,lastTick,nano2seconds,result,t,tick,toString
 * Covenant-source-reference: com/badlogic/gdx/utils/PerformanceCounters.java
 * Covenant-verified: 2026-04-19
 */
package sge
package utils

import sge.math.MathUtils

/** @author xoppa (original implementation) */
final class PerformanceCounters {
  private val nano2seconds = MathUtils.nanoToSec

  private var lastTick = Nanos.zero
  val counters: DynamicArray[PerformanceCounter] = DynamicArray[PerformanceCounter]()

  def add(name: String, windowSize: Int): PerformanceCounter = {
    val result = PerformanceCounter(name, windowSize)
    counters.add(result)
    result
  }

  def add(name: String): PerformanceCounter = {
    val result = PerformanceCounter(name)
    counters.add(result)
    result
  }

  def tick(): Unit = {
    val t = TimeUtils.nanoTime()
    if (lastTick > Nanos.zero) tick(Seconds((t - lastTick).toFloat * nano2seconds))
    lastTick = t
  }

  def tick(deltaTime: Seconds): Unit = {
    var i = 0
    while (i < counters.size) {
      counters(i).tick(deltaTime)
      i += 1
    }
  }

  def toString(sb: StringBuilder): StringBuilder = {
    sb.setLength(0)
    var i = 0
    while (i < counters.size) {
      if (i != 0) sb.append("; ")
      counters(i).toString(sb)
      i += 1
    }
    sb
  }
}
