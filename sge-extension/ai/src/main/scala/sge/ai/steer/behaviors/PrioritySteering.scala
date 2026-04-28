/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/PrioritySteering.java
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
 * Covenant-baseline-loc: 86
 * Covenant-baseline-methods: PrioritySteering,add,behaviors,calculateRealSteering,epsilon,epsilonSquared,i,n,selectedBehaviorIndex
 * Covenant-source-reference: com/badlogic/gdx/ai/steer/behaviors/PrioritySteering.java
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`; `Array` -> `DynamicArray`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 86
 * Covenant-baseline-methods: PrioritySteering,add,behaviors,calculateRealSteering,epsilon,epsilonSquared,i,n,selectedBehaviorIndex
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

/** The `PrioritySteering` behavior iterates through the behaviors and returns the first non zero steering. It makes sense since certain steering behaviors only request an acceleration in particular
  * conditions.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class PrioritySteering[T <: Vector[T]](
  owner: Steerable[T],
  /** The threshold of the steering acceleration magnitude below which a steering behavior is considered to have given no output. */
  var epsilon: Float = 0.001f
)(using ClassTag[SteeringBehavior[T]])
    extends SteeringBehavior[T](owner) {

  /** The list of steering behaviors in priority order. */
  val behaviors: DynamicArray[SteeringBehavior[T]] = DynamicArray[SteeringBehavior[T]]()

  /** The index of the behavior whose acceleration has been returned by the last evaluation of this priority steering. */
  var selectedBehaviorIndex: Int = -1

  /** Adds the specified behavior to the priority list.
    * @param behavior
    *   the behavior to add
    * @return
    *   this behavior for chaining.
    */
  def add(behavior: SteeringBehavior[T]): PrioritySteering[T] = {
    behaviors.add(behavior)
    this
  }

  override protected def calculateRealSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T] = boundary {
    // We'll need epsilon squared later.
    val epsilonSquared = epsilon * epsilon

    // Go through the behaviors until one has a large enough acceleration
    val n = behaviors.size
    selectedBehaviorIndex = -1
    var i = 0
    while (i < n) {
      selectedBehaviorIndex = i

      val behavior = behaviors(i)

      // Calculate the behavior's steering
      behavior.calculateSteering(steering)

      // If we're above the threshold return the current steering
      if (steering.calculateSquareMagnitude() > epsilonSquared) {
        break(steering)
      }
      i += 1
    }

    // If we get here, it means that no behavior had a large enough acceleration,
    // so return the small acceleration from the final behavior or zero if there are
    // no behaviors in the list.
    if (n > 0) steering else steering.setZero()
  }
}
