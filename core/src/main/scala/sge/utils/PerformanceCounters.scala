/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/PerformanceCounters.java
 * Original authors: xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge.utils

import scala.collection.mutable.ArrayBuffer
import sge.math.MathUtils

/** @author xoppa (original implementation) */
class PerformanceCounters {
  private val nano2seconds = MathUtils.nanoToSec

  private var lastTick = 0L
  val counters: ArrayBuffer[PerformanceCounter] = ArrayBuffer.empty[PerformanceCounter]

  def add(name: String, windowSize: Int): PerformanceCounter = {
    val result = new PerformanceCounter(name, windowSize)
    counters += result
    result
  }

  def add(name: String): PerformanceCounter = {
    val result = new PerformanceCounter(name)
    counters += result
    result
  }

  def tick(): Unit = {
    val t = TimeUtils.nanoTime()
    if (lastTick > 0L) tick((t - lastTick) * nano2seconds)
    lastTick = t
  }

  def tick(deltaTime: Float): Unit =
    for (i <- counters.indices)
      counters(i).tick(deltaTime)

  def toString(sb: StringBuilder): StringBuilder = {
    sb.setLength(0)
    for (i <- counters.indices) {
      if (i != 0) sb.append("; ")
      counters(i).toString(sb)
    }
    sb
  }
}
