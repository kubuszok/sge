/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/SteeringAcceleration.java
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
 * Covenant-baseline-loc: 96
 * Covenant-baseline-methods: SteeringAcceleration,add,angular,calculateMagnitude,calculateSquareMagnitude,isZero,linear,mulAdd,scl,setZero
 * Covenant-source-reference: com/badlogic/gdx/ai/steer/SteeringAcceleration.java
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 96
 * Covenant-baseline-methods: SteeringAcceleration,add,angular,calculateMagnitude,calculateSquareMagnitude,isZero,linear,mulAdd,scl,setZero
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package steer

import sge.math.Vector

/** `SteeringAcceleration` is a movement requested by the steering system. It is made up of two components, linear and angular acceleration.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class SteeringAcceleration[T <: Vector[T]](
  /** The linear component of this steering acceleration. */
  var linear: T,
  /** The angular component of this steering acceleration. */
  var angular: Float = 0f
) {

  /** Returns `true` if both linear and angular components of this steering acceleration are zero; `false` otherwise. */
  def isZero: Boolean =
    angular == 0 && linear.isZero

  /** Zeros the linear and angular components of this steering acceleration.
    * @return
    *   this steering acceleration for chaining
    */
  def setZero(): SteeringAcceleration[T] = {
    linear.setZero()
    angular = 0f
    this
  }

  /** Adds the given steering acceleration to this steering acceleration.
    *
    * @param steering
    *   the steering acceleration
    * @return
    *   this steering acceleration for chaining
    */
  def add(steering: SteeringAcceleration[T]): SteeringAcceleration[T] = {
    linear.+(steering.linear)
    angular += steering.angular
    this
  }

  /** Scales this steering acceleration by the specified scalar.
    *
    * @param scalar
    *   the scalar
    * @return
    *   this steering acceleration for chaining
    */
  def scl(scalar: Float): SteeringAcceleration[T] = {
    linear.scale(scalar)
    angular *= scalar
    this
  }

  /** First scale a supplied steering acceleration, then add it to this steering acceleration.
    *
    * @param steering
    *   the steering acceleration
    * @param scalar
    *   the scalar
    * @return
    *   this steering acceleration for chaining
    */
  def mulAdd(steering: SteeringAcceleration[T], scalar: Float): SteeringAcceleration[T] = {
    linear.mulAdd(steering.linear, scalar)
    angular += steering.angular * scalar
    this
  }

  /** Returns the square of the magnitude of this steering acceleration. This includes the angular component. */
  def calculateSquareMagnitude(): Float =
    linear.lengthSq + angular * angular

  /** Returns the magnitude of this steering acceleration. This includes the angular component. */
  def calculateMagnitude(): Float =
    Math.sqrt(calculateSquareMagnitude()).toFloat
}
