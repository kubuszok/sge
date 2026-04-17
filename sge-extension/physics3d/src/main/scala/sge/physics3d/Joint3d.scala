/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: sealed trait hierarchy for joint definitions and handles
 */
package sge
package physics3d

/** Definition of a 3D joint constraint between two rigid bodies. */
sealed trait JointDef3d

object JointDef3d {

  /** A fixed (weld) joint that locks two bodies together at their current relative position and orientation.
    *
    * @param body1
    *   the first body
    * @param body2
    *   the second body
    */
  final case class Fixed(body1: RigidBody3d, body2: RigidBody3d) extends JointDef3d

  /** A rope joint that constrains two bodies to stay within a maximum distance.
    *
    * @param body1
    *   the first body
    * @param body2
    *   the second body
    * @param maxDistance
    *   the maximum distance between the bodies' anchor points
    */
  final case class Rope(body1: RigidBody3d, body2: RigidBody3d, maxDistance: Float) extends JointDef3d
}

/** A handle to a 3D joint constraint in the physics world.
  *
  * Joints are created via [[PhysicsWorld3d.createJoint]] and destroyed via [[PhysicsWorld3d.destroyJoint]].
  */
sealed trait Joint3d {
  private[physics3d] def world:  PhysicsWorld3d
  private[physics3d] def handle: Long
}

/** A fixed (weld) joint that locks two bodies together at their current relative position and orientation. */
class FixedJoint3d private[physics3d] (
  private[physics3d] val world:  PhysicsWorld3d,
  private[physics3d] val handle: Long
) extends Joint3d

/** A rope joint that constrains two bodies to stay within a maximum distance.
  *
  * The distance constraint is one-sided: bodies can be closer than `maxDistance` but not farther.
  */
class RopeJoint3d private[physics3d] (
  private[physics3d] val world:  PhysicsWorld3d,
  private[physics3d] val handle: Long
) extends Joint3d
