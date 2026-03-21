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
class Joint private[physics] (
  private[physics] val world:  PhysicsWorld,
  private[physics] val handle: Long
)
