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

/** Default [[Timepiece]] implementation that tracks cumulative time and per-frame delta. */
class DefaultTimepiece extends Timepiece {

  private var _time:      Float = 0f
  private var _deltaTime: Float = 0f

  override def time: Float = _time

  override def deltaTime: Float = _deltaTime

  override def update(delta: Float): Unit = {
    _deltaTime = delta
    _time += delta
  }
}
