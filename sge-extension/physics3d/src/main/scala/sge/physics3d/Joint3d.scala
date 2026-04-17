/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: sealed trait hierarchy for joint definitions and handles
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 147
 * Covenant-baseline-methods: Fixed,FixedJoint3d,Joint3d,JointDef3d,Motor,MotorJoint3d,Prismatic,PrismaticJoint3d,Revolute,RevoluteJoint3d,Rope,RopeJoint3d,Spring,SpringJoint3d,angle,correctionFactor,correctionFactor_,enableLimits,enableMotor,handle,isLimitEnabled,limits,limitsBuf,linearOffset,linearOffset_,maxDistance,maxDistance_,maxForce,maxForce_,maxMotorForce,maxMotorForce_,maxMotorTorque,maxMotorTorque_,maxTorque,maxTorque_,motorSpeed,motorSpeed_,offsetBuf,restLength,restLength_,setLimits,setParams,translation,world
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-17
 */
package sge
package physics3d

/** Definition of a 3D joint constraint between two rigid bodies. */
sealed trait JointDef3d

object JointDef3d {

  final case class Fixed(body1: RigidBody3d, body2: RigidBody3d) extends JointDef3d

  final case class Rope(body1: RigidBody3d, body2: RigidBody3d, maxDistance: Float) extends JointDef3d

  /** A revolute (hinge) joint around a given axis at a world-space anchor point.
    *
    * @param anchorX
    *   anchor x in local body1 space
    * @param anchorY
    *   anchor y
    * @param anchorZ
    *   anchor z
    * @param axisX
    *   rotation axis x (will be normalized)
    * @param axisY
    *   rotation axis y
    * @param axisZ
    *   rotation axis z
    */
  final case class Revolute(body1: RigidBody3d, body2: RigidBody3d, anchorX: Float, anchorY: Float, anchorZ: Float, axisX: Float, axisY: Float, axisZ: Float) extends JointDef3d

  /** A prismatic (slider) joint along a given axis. */
  final case class Prismatic(body1: RigidBody3d, body2: RigidBody3d, axisX: Float, axisY: Float, axisZ: Float) extends JointDef3d

  /** A motor joint that drives two bodies toward a target offset (6-DOF). */
  final case class Motor(body1: RigidBody3d, body2: RigidBody3d) extends JointDef3d

  /** A spring joint with configurable stiffness and damping. */
  final case class Spring(body1: RigidBody3d, body2: RigidBody3d, restLength: Float, stiffness: Float, damping: Float) extends JointDef3d
}

/** A handle to a 3D joint constraint in the physics world. */
sealed trait Joint3d {
  private[physics3d] def world:  PhysicsWorld3d
  private[physics3d] def handle: Long
}

class FixedJoint3d private[physics3d] (
  private[physics3d] val world:  PhysicsWorld3d,
  private[physics3d] val handle: Long
) extends Joint3d

class RopeJoint3d private[physics3d] (
  private[physics3d] val world:  PhysicsWorld3d,
  private[physics3d] val handle: Long
) extends Joint3d {

  def maxDistance:             Float = world.ops.ropeJointGetMaxDistance(world.handle, handle)
  def maxDistance_=(d: Float): Unit  = world.ops.ropeJointSetMaxDistance(world.handle, handle, d)
}

class RevoluteJoint3d private[physics3d] (
  private[physics3d] val world:  PhysicsWorld3d,
  private[physics3d] val handle: Long
) extends Joint3d {

  private val limitsBuf = new Array[Float](2)

  def enableLimits(enable: Boolean):             Unit    = world.ops.revoluteJointEnableLimits(world.handle, handle, enable)
  def isLimitEnabled:                            Boolean = world.ops.revoluteJointIsLimitEnabled(world.handle, handle)
  def setLimits(lower:     Float, upper: Float): Unit    = world.ops.revoluteJointSetLimits(world.handle, handle, lower, upper)

  def limits: (Float, Float) = {
    world.ops.revoluteJointGetLimits(world.handle, handle, limitsBuf)
    (limitsBuf(0), limitsBuf(1))
  }

  def enableMotor(enable:      Boolean): Unit  = world.ops.revoluteJointEnableMotor(world.handle, handle, enable)
  def motorSpeed:                        Float = world.ops.revoluteJointGetMotorSpeed(world.handle, handle)
  def motorSpeed_=(speed:      Float):   Unit  = world.ops.revoluteJointSetMotorSpeed(world.handle, handle, speed)
  def maxMotorTorque:                    Float = world.ops.revoluteJointGetMaxMotorTorque(world.handle, handle)
  def maxMotorTorque_=(torque: Float):   Unit  = world.ops.revoluteJointSetMaxMotorTorque(world.handle, handle, torque)
  def angle:                             Float = world.ops.revoluteJointGetAngle(world.handle, handle)
}

class PrismaticJoint3d private[physics3d] (
  private[physics3d] val world:  PhysicsWorld3d,
  private[physics3d] val handle: Long
) extends Joint3d {

  private val limitsBuf = new Array[Float](2)

  def enableLimits(enable: Boolean):             Unit = world.ops.prismaticJointEnableLimits(world.handle, handle, enable)
  def setLimits(lower:     Float, upper: Float): Unit = world.ops.prismaticJointSetLimits(world.handle, handle, lower, upper)

  def limits: (Float, Float) = {
    world.ops.prismaticJointGetLimits(world.handle, handle, limitsBuf)
    (limitsBuf(0), limitsBuf(1))
  }

  def enableMotor(enable:    Boolean): Unit  = world.ops.prismaticJointEnableMotor(world.handle, handle, enable)
  def motorSpeed:                      Float = world.ops.prismaticJointGetMotorSpeed(world.handle, handle)
  def motorSpeed_=(speed:    Float):   Unit  = world.ops.prismaticJointSetMotorSpeed(world.handle, handle, speed)
  def maxMotorForce:                   Float = world.ops.prismaticJointGetMaxMotorForce(world.handle, handle)
  def maxMotorForce_=(force: Float):   Unit  = world.ops.prismaticJointSetMaxMotorForce(world.handle, handle, force)
  def translation:                     Float = world.ops.prismaticJointGetTranslation(world.handle, handle)
}

class MotorJoint3d private[physics3d] (
  private[physics3d] val world:  PhysicsWorld3d,
  private[physics3d] val handle: Long
) extends Joint3d {

  private val offsetBuf = new Array[Float](3)

  def linearOffset: (Float, Float, Float) = {
    world.ops.motorJointGetLinearOffset(world.handle, handle, offsetBuf)
    (offsetBuf(0), offsetBuf(1), offsetBuf(2))
  }

  def linearOffset_=(xyz: (Float, Float, Float)): Unit =
    world.ops.motorJointSetLinearOffset(world.handle, handle, xyz._1, xyz._2, xyz._3)

  def maxForce:                     Float = world.ops.motorJointGetMaxForce(world.handle, handle)
  def maxForce_=(f:         Float): Unit  = world.ops.motorJointSetMaxForce(world.handle, handle, f)
  def maxTorque:                    Float = world.ops.motorJointGetMaxTorque(world.handle, handle)
  def maxTorque_=(t:        Float): Unit  = world.ops.motorJointSetMaxTorque(world.handle, handle, t)
  def correctionFactor:             Float = world.ops.motorJointGetCorrectionFactor(world.handle, handle)
  def correctionFactor_=(f: Float): Unit  = world.ops.motorJointSetCorrectionFactor(world.handle, handle, f)
}

class SpringJoint3d private[physics3d] (
  private[physics3d] val world:  PhysicsWorld3d,
  private[physics3d] val handle: Long
) extends Joint3d {

  def restLength:                                  Float = world.ops.springJointGetRestLength(world.handle, handle)
  def restLength_=(length: Float):                 Unit  = world.ops.springJointSetRestLength(world.handle, handle, length)
  def setParams(stiffness: Float, damping: Float): Unit  = world.ops.springJointSetParams(world.handle, handle, stiffness, damping)
}
