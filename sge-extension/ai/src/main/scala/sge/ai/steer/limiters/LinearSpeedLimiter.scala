/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/limiters/LinearSpeedLimiter.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 32
 * Covenant-baseline-methods: LinearSpeedLimiter,_maxLinearSpeed,maxLinearSpeed,maxLinearSpeed_
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package steer
package limiters

/** A `LinearSpeedLimiter` provides the maximum magnitudes of linear speed. All other methods throw an `UnsupportedOperationException`.
  *
  * @author
  *   davebaol (original implementation)
  */
class LinearSpeedLimiter(
  private var _maxLinearSpeed: Float
) extends NullLimiter {

  /** Returns the maximum linear speed. */
  override def maxLinearSpeed: Float = _maxLinearSpeed

  /** Sets the maximum linear speed. */
  override def maxLinearSpeed_=(value: Float): Unit = _maxLinearSpeed = value
}
