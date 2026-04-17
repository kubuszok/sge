/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (2D physics API backed by Rapier2D)
 *   Convention: handle-based FFI, platform-agnostic trait
 *   Audited: 2026-03-08
 */
package sge
package physics

/** Definition of a joint constraint between two rigid bodies. */
sealed trait JointDef

object JointDef {

  /** A revolute (hinge) joint that constrains two bodies to rotate around a shared world-space anchor point.
    *
    * @param body1
    *   the first body
    * @param body2
    *   the second body
    * @param anchorX
    *   world-space x coordinate of the anchor
    * @param anchorY
    *   world-space y coordinate of the anchor
    */
  final case class Revolute(body1: RigidBody, body2: RigidBody, anchorX: Float, anchorY: Float) extends JointDef

  /** A prismatic (slider) joint that constrains two bodies to translate along a given axis.
    *
    * @param body1
    *   the first body
    * @param body2
    *   the second body
    * @param axisX
    *   x component of the slide axis (will be normalized by Rapier)
    * @param axisY
    *   y component of the slide axis (will be normalized by Rapier)
    */
  final case class Prismatic(body1: RigidBody, body2: RigidBody, axisX: Float, axisY: Float) extends JointDef

  /** A fixed (weld) joint that locks two bodies together at their current relative position and angle.
    *
    * @param body1
    *   the first body
    * @param body2
    *   the second body
    */
  final case class Fixed(body1: RigidBody, body2: RigidBody) extends JointDef

  /** A rope joint that constrains two bodies to stay within a maximum distance.
    *
    * @param body1
    *   the first body
    * @param body2
    *   the second body
    * @param maxDistance
    *   the maximum distance between the bodies' anchor points
    */
  final case class Rope(body1: RigidBody, body2: RigidBody, maxDistance: Float) extends JointDef

  /** A motor joint that drives two bodies toward a target relative position and angle.
    *
    * @param body1
    *   the first body
    * @param body2
    *   the second body
    */
  final case class Motor(body1: RigidBody, body2: RigidBody) extends JointDef

  /** A mouse (drag) joint that pulls a body toward a world-space target position.
    *
    * Internally implemented using a kinematic anchor body and a motor joint. The anchor body is created at the target position and hidden from the game. Moving the target teleports the anchor body,
    * and the motor joint applies forces to pull the dragged body toward it.
    *
    * @param body
    *   the body to drag
    * @param targetX
    *   initial world-space x coordinate of the drag target
    * @param targetY
    *   initial world-space y coordinate of the drag target
    */
  final case class Mouse(body: RigidBody, targetX: Float, targetY: Float) extends JointDef

  /** A spring joint that connects two bodies with a spring-damper constraint.
    *
    * The spring pulls the bodies toward the rest length distance, with configurable stiffness and damping.
    *
    * @param body1
    *   the first body
    * @param body2
    *   the second body
    * @param restLength
    *   the natural length of the spring
    * @param stiffness
    *   the spring stiffness (higher = stiffer)
    * @param damping
    *   the spring damping (higher = less oscillation)
    */
  final case class Spring(body1: RigidBody, body2: RigidBody, restLength: Float, stiffness: Float, damping: Float) extends JointDef
}

/** A handle to a joint constraint in the physics world.
  *
  * Joints are created via [[PhysicsWorld.createJoint]] and destroyed via [[PhysicsWorld.destroyJoint]].
  */
sealed trait Joint {
  private[physics] def world:  PhysicsWorld
  private[physics] def handle: Long
}

/** A revolute (hinge) joint that constrains two bodies to rotate around a shared anchor point.
  *
  * Supports angular limits and motor control.
  */
class RevoluteJoint private[physics] (
  private[physics] val world:  PhysicsWorld,
  private[physics] val handle: Long
) extends Joint {

  private val limitsBuf = new Array[Float](2)

  /** Enables or disables angular limits for this joint. */
  def enableLimits(enable: Boolean): Unit =
    world.ops.revoluteJointEnableLimits(world.handle, handle, enable)

  /** Returns true if angular limits are enabled. */
  def isLimitEnabled: Boolean =
    world.ops.revoluteJointIsLimitEnabled(world.handle, handle)

  /** Sets the angular limits (in radians). */
  def setLimits(lower: Float, upper: Float): Unit =
    world.ops.revoluteJointSetLimits(world.handle, handle, lower, upper)

  /** Gets the angular limits as (lower, upper) in radians. */
  def limits: (Float, Float) = {
    world.ops.revoluteJointGetLimits(world.handle, handle, limitsBuf)
    (limitsBuf(0), limitsBuf(1))
  }

  /** Enables or disables the motor for this joint. */
  def enableMotor(enable: Boolean): Unit =
    world.ops.revoluteJointEnableMotor(world.handle, handle, enable)

  /** Sets the target motor speed (radians/second). */
  def motorSpeed_=(speed: Float): Unit =
    world.ops.revoluteJointSetMotorSpeed(world.handle, handle, speed)

  /** Gets the current motor speed setting. */
  def motorSpeed: Float =
    world.ops.revoluteJointGetMotorSpeed(world.handle, handle)

  /** Gets the maximum motor torque. */
  def maxMotorTorque: Float =
    world.ops.revoluteJointGetMaxMotorTorque(world.handle, handle)

  /** Sets the maximum motor torque. */
  def maxMotorTorque_=(torque: Float): Unit =
    world.ops.revoluteJointSetMaxMotorTorque(world.handle, handle, torque)

  /** Gets the current joint angle (radians). */
  def angle: Float =
    world.ops.revoluteJointGetAngle(world.handle, handle)
}

/** A prismatic (slider) joint that constrains two bodies to translate along a given axis.
  *
  * Supports translation limits and motor control.
  */
class PrismaticJoint private[physics] (
  private[physics] val world:  PhysicsWorld,
  private[physics] val handle: Long
) extends Joint {

  private val limitsBuf = new Array[Float](2)

  /** Enables or disables translation limits for this joint. */
  def enableLimits(enable: Boolean): Unit =
    world.ops.prismaticJointEnableLimits(world.handle, handle, enable)

  /** Sets the translation limits. */
  def setLimits(lower: Float, upper: Float): Unit =
    world.ops.prismaticJointSetLimits(world.handle, handle, lower, upper)

  /** Gets the translation limits as (lower, upper). */
  def limits: (Float, Float) = {
    world.ops.prismaticJointGetLimits(world.handle, handle, limitsBuf)
    (limitsBuf(0), limitsBuf(1))
  }

  /** Enables or disables the motor for this joint. */
  def enableMotor(enable: Boolean): Unit =
    world.ops.prismaticJointEnableMotor(world.handle, handle, enable)

  /** Gets the current motor speed setting. */
  def motorSpeed: Float =
    world.ops.prismaticJointGetMotorSpeed(world.handle, handle)

  /** Sets the target motor speed. */
  def motorSpeed_=(speed: Float): Unit =
    world.ops.prismaticJointSetMotorSpeed(world.handle, handle, speed)

  /** Gets the maximum motor force. */
  def maxMotorForce: Float =
    world.ops.prismaticJointGetMaxMotorForce(world.handle, handle)

  /** Sets the maximum motor force. */
  def maxMotorForce_=(force: Float): Unit =
    world.ops.prismaticJointSetMaxMotorForce(world.handle, handle, force)

  /** Gets the current translation of the joint. */
  def translation: Float =
    world.ops.prismaticJointGetTranslation(world.handle, handle)
}

/** A fixed (weld) joint that locks two bodies together at their current relative position and angle. */
class FixedJoint private[physics] (
  private[physics] val world:  PhysicsWorld,
  private[physics] val handle: Long
) extends Joint

/** A rope joint that constrains two bodies to stay within a maximum distance.
  *
  * The distance constraint is one-sided: bodies can be closer than `maxDistance` but not farther.
  */
class RopeJoint private[physics] (
  private[physics] val world:  PhysicsWorld,
  private[physics] val handle: Long
) extends Joint {

  /** Gets the maximum distance for this rope joint. */
  def maxDistance: Float =
    world.ops.ropeJointGetMaxDistance(world.handle, handle)

  /** Sets the maximum distance for this rope joint. */
  def maxDistance_=(d: Float): Unit =
    world.ops.ropeJointSetMaxDistance(world.handle, handle, d)
}

/** A motor joint that drives two bodies toward a target relative position and angle.
  *
  * The motor applies forces/torques to move body2 toward a target offset relative to body1. The correction factor controls how aggressively the motor corrects position errors.
  */
class MotorJoint private[physics] (
  private[physics] val world:  PhysicsWorld,
  private[physics] val handle: Long
) extends Joint {

  private val offsetBuf = new Array[Float](2)

  /** Gets the linear offset target as (x, y). */
  def linearOffset: (Float, Float) = {
    world.ops.motorJointGetLinearOffset(world.handle, handle, offsetBuf)
    (offsetBuf(0), offsetBuf(1))
  }

  /** Sets the linear offset target. */
  def linearOffset_=(xy: (Float, Float)): Unit =
    world.ops.motorJointSetLinearOffset(world.handle, handle, xy._1, xy._2)

  /** Gets the angular offset target (radians). */
  def angularOffset: Float =
    world.ops.motorJointGetAngularOffset(world.handle, handle)

  /** Sets the angular offset target (radians). */
  def angularOffset_=(a: Float): Unit =
    world.ops.motorJointSetAngularOffset(world.handle, handle, a)

  /** Gets the maximum force the motor can apply. */
  def maxForce: Float =
    world.ops.motorJointGetMaxForce(world.handle, handle)

  /** Sets the maximum force the motor can apply. */
  def maxForce_=(f: Float): Unit =
    world.ops.motorJointSetMaxForce(world.handle, handle, f)

  /** Gets the maximum torque the motor can apply. */
  def maxTorque: Float =
    world.ops.motorJointGetMaxTorque(world.handle, handle)

  /** Sets the maximum torque the motor can apply. */
  def maxTorque_=(t: Float): Unit =
    world.ops.motorJointSetMaxTorque(world.handle, handle, t)

  /** Gets the correction factor. */
  def correctionFactor: Float =
    world.ops.motorJointGetCorrectionFactor(world.handle, handle)

  /** Sets the correction factor (0 = no correction, 1 = full correction per step). */
  def correctionFactor_=(f: Float): Unit =
    world.ops.motorJointSetCorrectionFactor(world.handle, handle, f)
}

/** A spring joint that connects two bodies with a spring-damper constraint.
  *
  * The spring pulls the bodies toward the rest length distance, with configurable stiffness and damping.
  */
class SpringJoint private[physics] (
  private[physics] val world:  PhysicsWorld,
  private[physics] val handle: Long
) extends Joint {

  /** Gets the rest length of the spring. */
  def restLength: Float =
    world.ops.springJointGetRestLength(world.handle, handle)

  /** Sets the rest length of the spring. */
  def restLength_=(length: Float): Unit =
    world.ops.springJointSetRestLength(world.handle, handle, length)

  /** Sets the stiffness and damping parameters of the spring. */
  def setParams(stiffness: Float, damping: Float): Unit =
    world.ops.springJointSetParams(world.handle, handle, stiffness, damping)
}

/** A mouse (drag) joint that pulls a body toward a world-space target position.
  *
  * Internally implemented as a kinematic anchor body combined with a motor joint. The anchor body is created at the target position and is invisible to the game. Moving the target teleports the
  * anchor body, and the motor joint pulls the dragged body toward it.
  *
  * Created via [[PhysicsWorld.createJoint]] with a [[JointDef.Mouse]] definition.
  */
class MouseJoint private[physics] (
  private[physics] val world:        PhysicsWorld,
  private[physics] val handle:       Long,
  private[physics] val anchorHandle: Long
) extends Joint {

  private val posBuf = new Array[Float](2)

  /** Gets the current target position as (x, y).
    *
    * Reads the position of the internal kinematic anchor body.
    */
  def target: (Float, Float) = {
    world.ops.bodyGetPosition(world.handle, anchorHandle, posBuf)
    (posBuf(0), posBuf(1))
  }

  /** Sets the target position, teleporting the internal anchor body.
    *
    * The dragged body will be pulled toward this position by the motor joint.
    */
  def target_=(pos: (Float, Float)): Unit =
    world.ops.bodySetPosition(world.handle, anchorHandle, pos._1, pos._2)

  /** Sets the maximum force the motor joint can apply to pull the body toward the target.
    *
    * Higher values make the joint stiffer and more responsive.
    */
  def maxForce_=(f: Float): Unit =
    world.ops.motorJointSetMaxForce(world.handle, handle, f)

  /** Sets the correction factor for the motor joint (0 = no correction, 1 = full correction per step).
    *
    * Controls how aggressively the dragged body tracks the target. Higher values make the body snap to the target faster but can cause instability.
    */
  def correctionFactor_=(f: Float): Unit =
    world.ops.motorJointSetCorrectionFactor(world.handle, handle, f)
}
