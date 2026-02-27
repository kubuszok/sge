/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/PerformanceCounters.java
 * Original authors: xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge.utils

import sge.math.MathUtils

/** @author xoppa (original implementation) */
class PerformanceCounters {
  private val nano2seconds = MathUtils.nanoToSec

  private var lastTick = 0L
  val counters: DynamicArray[PerformanceCounter] = DynamicArray[PerformanceCounter]()

  def add(name: String, windowSize: Int): PerformanceCounter = {
    val result = new PerformanceCounter(name, windowSize)
    counters.add(result)
    result
  }

  def add(name: String): PerformanceCounter = {
    val result = new PerformanceCounter(name)
    counters.add(result)
    result
  }

  def tick(): Unit = {
    val t = TimeUtils.nanoTime()
    if (lastTick > 0L) tick((t - lastTick) * nano2seconds)
    lastTick = t
  }

  def tick(deltaTime: Float): Unit = {
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
