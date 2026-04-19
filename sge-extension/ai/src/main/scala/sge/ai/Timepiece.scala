/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/Timepiece.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai` -> `sge.ai`
 *   Merged with: `DefaultTimepiece.java`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 57
 * Covenant-baseline-methods: DefaultTimepiece,Timepiece,_deltaTime,_time,deltaTime,time,update
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai

/** Keeps track of time for AI subsystems such as delayed messages and scheduling.
  *
  * @author
  *   davebaol (original implementation)
  */
trait Timepiece {

  /** Returns the current AI time. */
  def time: Float

  /** Returns the time elapsed since the last frame. */
  def deltaTime: Float

  /** Updates the time by the given delta.
    * @param delta
    *   the time elapsed since the last frame
    */
  def update(delta: Float): Unit
}

/** Default [[Timepiece]] implementation that tracks cumulative time and per-frame delta.
  *
  * Delta time is clamped to [[maxDeltaTime]] to prevent AI systems from seeing huge deltas after pauses (tab switches, debugging).
  *
  * @param maxDeltaTime
  *   the maximum delta time; defaults to `Float.PositiveInfinity` (no clamping)
  */
class DefaultTimepiece(val maxDeltaTime: Float = Float.PositiveInfinity) extends Timepiece {

  private var _time:      Float = 0f
  private var _deltaTime: Float = 0f

  override def time: Float = _time

  override def deltaTime: Float = _deltaTime

  override def update(delta: Float): Unit = {
    _deltaTime = scala.math.min(delta, maxDeltaTime)
    _time += _deltaTime
  }
}
