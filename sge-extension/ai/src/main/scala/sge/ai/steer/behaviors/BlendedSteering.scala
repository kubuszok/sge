/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/BlendedSteering.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`; `Array` -> `DynamicArray`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 132
 * Covenant-baseline-methods: BehaviorAndWeight,BlendedSteering,actualLimiter,add,behavior,calculateRealSteering,get,i,len,list,remove,steering,weight
 * Covenant-source-reference: com/badlogic/gdx/ai/steer/behaviors/BlendedSteering.java
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`; `Array` -> `DynamicArray`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 132
 * Covenant-baseline-methods: BehaviorAndWeight,BlendedSteering,actualLimiter,add,behavior,calculateRealSteering,get,i,len,list,remove,steering,weight
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package steer
package behaviors

import sge.math.Vector
import sge.utils.DynamicArray

import scala.reflect.ClassTag
import scala.util.boundary
import boundary.break

/** This combination behavior simply sums up all the behaviors, applies their weights, and truncates the result before returning. There are no constraints on the blending weights; they don't have to
  * sum to one, for example, and rarely do.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class BlendedSteering[T <: Vector[T]](
  owner: Steerable[T]
)(using ClassTag[BlendedSteering.BehaviorAndWeight[T]])
    extends SteeringBehavior[T](owner) {

  /** The list of behaviors and their corresponding blending weights. */
  val list: DynamicArray[BlendedSteering.BehaviorAndWeight[T]] = DynamicArray[BlendedSteering.BehaviorAndWeight[T]]()

  private val steering: SteeringAcceleration[T] = new SteeringAcceleration[T](newVector(owner))

  /** Adds a steering behavior and its weight to the list.
    * @param behavior
    *   the steering behavior to add
    * @param weight
    *   the weight of the behavior
    * @return
    *   this behavior for chaining.
    */
  def add(behavior: SteeringBehavior[T], weight: Float): BlendedSteering[T] =
    add(new BlendedSteering.BehaviorAndWeight[T](behavior, weight))

  /** Adds a steering behavior and its weight to the list.
    * @param item
    *   the steering behavior and its weight
    * @return
    *   this behavior for chaining.
    */
  def add(item: BlendedSteering.BehaviorAndWeight[T]): BlendedSteering[T] = {
    item.behavior.owner = owner
    list.add(item)
    this
  }

  /** Removes a steering behavior from the list.
    * @param item
    *   the steering behavior to remove
    */
  def remove(item: BlendedSteering.BehaviorAndWeight[T]): Unit =
    list.removeValue(item)

  /** Removes a steering behavior from the list.
    * @param behavior
    *   the steering behavior to remove
    */
  def remove(behavior: SteeringBehavior[T]): Unit = boundary {
    var i = 0
    while (i < list.size) {
      if (list(i).behavior eq behavior) {
        list.removeIndex(i)
        break(())
      }
      i += 1
    }
  }

  /** Returns the weighted behavior at the specified index.
    * @param index
    *   the index of the weighted behavior to return
    */
  def get(index: Int): BlendedSteering.BehaviorAndWeight[T] = list(index)

  override protected def calculateRealSteering(blendedSteering: SteeringAcceleration[T]): SteeringAcceleration[T] = {
    // Clear the output to start with
    blendedSteering.setZero()

    // Go through all the behaviors
    val len = list.size
    var i   = 0
    while (i < len) {
      val bw = list(i)

      // Calculate the behavior's steering
      bw.behavior.calculateSteering(steering)

      // Scale and add the steering to the accumulator
      blendedSteering.mulAdd(steering, bw.weight)

      i += 1
    }

    val actualLimiter = getActualLimiter()

    // Crop the result
    blendedSteering.linear.limit(actualLimiter.maxLinearAcceleration)
    if (blendedSteering.angular > actualLimiter.maxAngularAcceleration) {
      blendedSteering.angular = actualLimiter.maxAngularAcceleration
    }

    blendedSteering
  }
}

object BlendedSteering {

  class BehaviorAndWeight[T <: Vector[T]](
    var behavior: SteeringBehavior[T],
    var weight:   Float
  ) {}
}
