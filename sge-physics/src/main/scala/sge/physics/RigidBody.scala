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

/** A handle to a rigid body in the physics world.
  *
  * Provides access to position, velocity, forces, and collider attachment. Bodies are created via
  * [[PhysicsWorld.createBody]] and destroyed via [[PhysicsWorld.destroyBody]].
  */
class RigidBody private[physics] (
  private[physics] val world:  PhysicsWorld,
  private[physics] val handle: Long,
  val bodyType:                BodyType
) {

  /** Scratch buffer reused for 2-float output (position, velocity). */
  private val buf2 = new Array[Float](2)

  /** Returns the world-space position as (x, y). */
  def position: (Float, Float) = {
    world.ops.bodyGetPosition(world.handle, handle, buf2)
    (buf2(0), buf2(1))
  }

  /** Returns the rotation angle in radians. */
  def angle: Float =
    world.ops.bodyGetAngle(world.handle, handle)

  /** Returns the linear velocity as (vx, vy). */
  def linearVelocity: (Float, Float) = {
    world.ops.bodyGetLinearVelocity(world.handle, handle, buf2)
    (buf2(0), buf2(1))
  }

  /** Returns the angular velocity in radians per second. */
  def angularVelocity: Float =
    world.ops.bodyGetAngularVelocity(world.handle, handle)

  /** Teleports the body to the given position. */
  def position_=(pos: (Float, Float)): Unit =
    world.ops.bodySetPosition(world.handle, handle, pos._1, pos._2)

  /** Sets the rotation angle in radians. */
  def angle_=(a: Float): Unit =
    world.ops.bodySetAngle(world.handle, handle, a)

  /** Sets the linear velocity. */
  def linearVelocity_=(vel: (Float, Float)): Unit =
    world.ops.bodySetLinearVelocity(world.handle, handle, vel._1, vel._2)

  /** Sets the angular velocity in radians per second. */
  def angularVelocity_=(omega: Float): Unit =
    world.ops.bodySetAngularVelocity(world.handle, handle, omega)

  /** Applies a force at the center of mass (takes effect on next step). */
  def applyForce(fx: Float, fy: Float): Unit =
    world.ops.bodyApplyForce(world.handle, handle, fx, fy)

  /** Applies an impulse at the center of mass (instantaneous velocity change). */
  def applyImpulse(ix: Float, iy: Float): Unit =
    world.ops.bodyApplyImpulse(world.handle, handle, ix, iy)

  /** Applies a torque (takes effect on next step). */
  def applyTorque(torque: Float): Unit =
    world.ops.bodyApplyTorque(world.handle, handle, torque)

  /** Sets linear damping (velocity decay per second, 0 = no damping). */
  def linearDamping_=(d: Float): Unit =
    world.ops.bodySetLinearDamping(world.handle, handle, d)

  /** Sets angular damping (angular velocity decay per second, 0 = no damping). */
  def angularDamping_=(d: Float): Unit =
    world.ops.bodySetAngularDamping(world.handle, handle, d)

  /** Sets the gravity scale for this body (1.0 = normal, 0.0 = no gravity). */
  def gravityScale_=(s: Float): Unit =
    world.ops.bodySetGravityScale(world.handle, handle, s)

  /** Locks or unlocks rotation for this body. */
  def fixedRotation_=(fixed: Boolean): Unit =
    world.ops.bodySetFixedRotation(world.handle, handle, fixed)

  /** Returns true if the body is awake (actively simulating). */
  def isAwake: Boolean =
    world.ops.bodyIsAwake(world.handle, handle)

  /** Wakes up the body so it participates in simulation. */
  def wakeUp(): Unit =
    world.ops.bodyWakeUp(world.handle, handle)

  /** Attaches a collider with the given shape and material properties to this body.
    *
    * @param shape
    *   the collision shape
    * @param density
    *   the collider density (affects mass), default 1.0
    * @param friction
    *   the friction coefficient, default 0.5
    * @param restitution
    *   the restitution (bounciness), default 0.0
    * @return
    *   the new [[Collider]] handle
    */
  def attachCollider(
    shape:       Shape,
    density:     Float = 1f,
    friction:    Float = 0.5f,
    restitution: Float = 0f
  ): Collider = {
    val ops = world.ops
    val wh  = world.handle
    val ch = shape match {
      case Shape.Circle(radius)              => ops.createCircleCollider(wh, handle, radius)
      case Shape.Box(halfWidth, halfHeight)   => ops.createBoxCollider(wh, handle, halfWidth, halfHeight)
      case Shape.Capsule(halfHeight, radius) => ops.createCapsuleCollider(wh, handle, halfHeight, radius)
      case Shape.Polygon(vertices)           => ops.createPolygonCollider(wh, handle, vertices, vertices.length / 2)
    }
    ops.colliderSetDensity(wh, ch, density)
    ops.colliderSetFriction(wh, ch, friction)
    ops.colliderSetRestitution(wh, ch, restitution)
    Collider(world, ch)
  }
}
