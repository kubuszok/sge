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

  /** Sets the target motor speed. */
  def motorSpeed_=(speed: Float): Unit =
    world.ops.prismaticJointSetMotorSpeed(world.handle, handle, speed)

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
