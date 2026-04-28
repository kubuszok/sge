/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/steer/behaviors/Jump.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`; `GdxAI.getTimepiece()` -> `(using Timepiece)`
 *   Convention: split packages, Nullable instead of null
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 270
 * Covenant-baseline-methods: GravityComponentHandler,Jump,JumpCallback,JumpDescriptor,JumpTarget,airborneTime,calculateAirborneTimeAndVelocity,calculateRealSteering,calculateTarget,callback,checkAirborneTimeAndCalculateVelocity,delta,g,getComponent,gravity,gravityComponentHandler,isJumpAchievable,jumpDescriptor,jumpTarget,landingPosition,linearVelocity,maxVerticalVelocity,planarVelocity,position,reportAchievability,set,setComponent,setJumpDescriptor,setTakeoffTolerance,sqrtTerm,takeoff,takeoffPosition,takeoffPositionTolerance,takeoffVelocityTolerance,targetLinearVelocity,targetPosition,time
 * Covenant-source-reference: com/badlogic/gdx/ai/steer/behaviors/Jump.java
 *   Renames: `com.badlogic.gdx.ai.steer` -> `sge.ai.steer`; `GdxAI.getTimepiece()` -> `(using Timepiece)`
 *   Convention: split packages, Nullable instead of null
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 270
 * Covenant-baseline-methods: GravityComponentHandler,Jump,JumpCallback,JumpDescriptor,JumpTarget,airborneTime,calculateAirborneTimeAndVelocity,calculateRealSteering,calculateTarget,callback,checkAirborneTimeAndCalculateVelocity,delta,g,getComponent,gravity,gravityComponentHandler,isJumpAchievable,jumpDescriptor,jumpTarget,landingPosition,linearVelocity,maxVerticalVelocity,planarVelocity,position,reportAchievability,set,setComponent,setJumpDescriptor,setTakeoffTolerance,sqrtTerm,takeoff,takeoffPosition,takeoffPositionTolerance,takeoffVelocityTolerance,targetLinearVelocity,targetPosition,time
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package steer
package behaviors

import sge.math.{ Epsilon, Vector }
import sge.utils.Nullable

import scala.compiletime.uninitialized

/** First the `Jump` behavior calculates the linear velocity required to achieve the jump. If the calculated velocity doesn't exceed the maximum linear velocity the jump is achievable; otherwise it's
  * not. In either cases, the given callback gets informed through the `reportAchievability` method. Also, if the jump is achievable the run up phase begins and the `Jump` behavior will start to
  * produce the linear acceleration required to match the calculated velocity.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
class Jump[T <: Vector[T]](
  owner: Steerable[T],
  /** The jump descriptor to use */
  var jumpDescriptor: Jump.JumpDescriptor[T],
  /** The gravity vector to use. Notice that this behavior only supports gravity along a single axis. */
  var gravity:                 T,
  var gravityComponentHandler: Jump.GravityComponentHandler[T],
  var callback:                Jump.JumpCallback
) extends MatchVelocity[T](owner) {

  /** The maximum vertical component of jump velocity, where "vertical" stands for the axis where gravity operates. */
  var maxVerticalVelocity: Float = 0f

  var takeoffPositionTolerance: Float = 0f
  var takeoffVelocityTolerance: Float = 0f

  /** Keeps track of whether the jump is achievable */
  private var isJumpAchievable: Boolean = false

  var airborneTime: Float = 0f

  private val jumpTarget:     Jump.JumpTarget[T] = new Jump.JumpTarget[T](owner)
  private val planarVelocity: T                  = newVector(owner)

  // Initialize target to empty; it will be calculated on first steering
  target = Nullable.empty

  /** Sets the jump descriptor to use.
    * @param jumpDescriptor
    *   the jump descriptor to set
    * @return
    *   this behavior for chaining.
    */
  def setJumpDescriptor(jumpDescriptor: Jump.JumpDescriptor[T]): Jump[T] = {
    this.jumpDescriptor = jumpDescriptor
    this.target = Nullable.empty
    this.isJumpAchievable = false
    this
  }

  /** Sets the tolerance used to check if the owner has reached the takeoff location with the required velocity.
    * @param takeoffTolerance
    *   the takeoff tolerance for both position and velocity
    * @return
    *   this behavior for chaining.
    */
  def setTakeoffTolerance(takeoffTolerance: Float): Jump[T] = {
    takeoffPositionTolerance = takeoffTolerance
    takeoffVelocityTolerance = takeoffTolerance
    this
  }

  override protected def calculateRealSteering(steering: SteeringAcceleration[T]): SteeringAcceleration[T] = {
    // Check if we have a trajectory, and create one if not.
    if (target.isEmpty) {
      val calculated = calculateTarget()
      target = Nullable(calculated)
      callback.reportAchievability(isJumpAchievable)
    }

    // If the trajectory is zero, return no steering acceleration
    if (!isJumpAchievable) {
      steering.setZero()
    } else {
      val tgt = target.getOrElse(throw new IllegalStateException())

      // Check if the owner has reached target position and velocity with acceptable tolerance
      if (owner.position.epsilonEquals(tgt.position)(using Epsilon(takeoffPositionTolerance))) {
        if (owner.linearVelocity.epsilonEquals(tgt.linearVelocity)(using Epsilon(takeoffVelocityTolerance))) {
          isJumpAchievable = false
          // Perform the jump, and return no steering (the owner is airborne, no need to steer).
          callback.takeoff(maxVerticalVelocity, airborneTime)
          steering.setZero()
        } else {
          // Delegate to MatchVelocity
          super.calculateRealSteering(steering)
        }
      } else {
        // Delegate to MatchVelocity
        super.calculateRealSteering(steering)
      }
    }
  }

  /** Works out the trajectory calculation. */
  private def calculateTarget(): Steerable[T] = {
    this.jumpTarget.targetPosition = jumpDescriptor.takeoffPosition
    this.airborneTime = calculateAirborneTimeAndVelocity(jumpTarget.targetLinearVelocity, jumpDescriptor, getActualLimiter().maxLinearSpeed)
    this.isJumpAchievable = airborneTime >= 0
    jumpTarget
  }

  /** Returns the airborne time and sets the `outVelocity` vector to the airborne planar velocity required to achieve the jump. If the jump is not achievable -1 is returned and the `outVelocity`
    * vector remains unchanged.
    * @param outVelocity
    *   the output vector where the airborne planar velocity is calculated
    * @param jumpDescriptor
    *   the jump descriptor
    * @param maxLinearSpeed
    *   the maximum linear speed that can be used to achieve the jump
    * @return
    *   the time of flight or -1 if the jump is not achievable using the given max linear speed.
    */
  def calculateAirborneTimeAndVelocity(outVelocity: T, jumpDescriptor: Jump.JumpDescriptor[T], maxLinearSpeed: Float): Float = {
    val g = gravityComponentHandler.getComponent(gravity)

    // Calculate the first jump time
    val sqrtTerm = Math
      .sqrt(
        2f * g * gravityComponentHandler.getComponent(jumpDescriptor.delta)
          + maxVerticalVelocity * maxVerticalVelocity
      )
      .toFloat
    var time = (-maxVerticalVelocity + sqrtTerm) / g

    // Check if we can use it
    if (!checkAirborneTimeAndCalculateVelocity(outVelocity, time, jumpDescriptor, maxLinearSpeed)) {
      // Otherwise try the other time
      time = (-maxVerticalVelocity - sqrtTerm) / g
      if (!checkAirborneTimeAndCalculateVelocity(outVelocity, time, jumpDescriptor, maxLinearSpeed)) {
        -1f // Unachievable jump
      } else {
        time // Achievable jump
      }
    } else {
      time // Achievable jump
    }
  }

  private def checkAirborneTimeAndCalculateVelocity(outVelocity: T, time: Float, jumpDescriptor: Jump.JumpDescriptor[T], maxLinearSpeed: Float): Boolean = {
    // Calculate the planar velocity
    planarVelocity.set(jumpDescriptor.delta).scale(1f / time)
    gravityComponentHandler.setComponent(planarVelocity, 0f)

    // Check the planar linear speed
    if (planarVelocity.lengthSq < maxLinearSpeed * maxLinearSpeed) {
      // We have a valid solution, so store it by merging vertical and non-vertical axes
      val verticalValue = gravityComponentHandler.getComponent(outVelocity)
      gravityComponentHandler.setComponent(outVelocity.set(planarVelocity), verticalValue)
      true
    } else {
      false
    }
  }
}

object Jump {

  /** A `JumpTarget` is used internally by Jump to represent the takeoff target. */
  private class JumpTarget[T <: Vector[T]](other: Steerable[T]) extends SteerableAdapter[T] {
    var targetPosition:       T = uninitialized
    val targetLinearVelocity: T = other.position.copy.setZero()

    override def position:       T = targetPosition
    override def linearVelocity: T = targetLinearVelocity
  }

  /** A `JumpDescriptor` contains jump information like the take-off and the landing position.
    *
    * @tparam T
    *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
    *
    * @author
    *   davebaol (original implementation)
    */
  class JumpDescriptor[T <: Vector[T]](
    /** The position of the takeoff pad */
    var takeoffPosition: T,
    /** The position of the landing pad */
    var landingPosition: T
  ) {

    /** The change in position from takeoff to landing. This is calculated from the other values. */
    val delta: T = landingPosition.copy
    set(takeoffPosition, landingPosition)

    /** Sets this `JumpDescriptor` from the given takeoff and landing positions.
      * @param takeoffPosition
      *   the position of the takeoff pad
      * @param landingPosition
      *   the position of the landing pad
      */
    def set(takeoffPosition: T, landingPosition: T): Unit = {
      this.takeoffPosition.set(takeoffPosition)
      this.landingPosition.set(landingPosition)
      this.delta.set(landingPosition).-(takeoffPosition)
    }
  }

  /** A `GravityComponentHandler` is aware of the axis along which the gravity acts.
    *
    * @tparam T
    *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
    *
    * @author
    *   davebaol (original implementation)
    */
  trait GravityComponentHandler[T <: Vector[T]] {

    /** Returns the component of the given vector along which the gravity operates.
      * @param vector
      *   the vector
      * @return
      *   the value of the component affected by gravity.
      */
    def getComponent(vector: T): Float

    /** Sets the component of the given vector along which the gravity operates.
      * @param vector
      *   the vector
      * @param value
      *   the value of the component affected by gravity
      */
    def setComponent(vector: T, value: Float): Unit
  }

  /** The `JumpCallback` allows you to know whether a jump is achievable and when to jump.
    *
    * @author
    *   davebaol (original implementation)
    */
  trait JumpCallback {

    /** Reports whether the jump is achievable or not.
      * @param achievable
      *   whether the jump is achievable or not.
      */
    def reportAchievability(achievable: Boolean): Unit

    /** This method is called to notify that both the position and velocity of the character are good enough to jump.
      * @param maxVerticalVelocity
      *   the velocity to set along the vertical axis to achieve the jump
      * @param time
      *   the duration of the jump
      */
    def takeoff(maxVerticalVelocity: Float, time: Float): Unit
  }
}
