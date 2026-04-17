/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: handle-based FFI wrapper with 3D vectors and quaternions
 */
package sge
package physics3d

/** A handle to a 3D rigid body in the physics world.
  *
  * Provides access to position, rotation (quaternion), velocity, forces, and collider attachment. Bodies are created via [[PhysicsWorld3d.createBody]] and destroyed via
  * [[PhysicsWorld3d.destroyBody]].
  */
class RigidBody3d private[physics3d] (
  private[physics3d] val world:  PhysicsWorld3d,
  private[physics3d] val handle: Long,
  val bodyType:                  BodyType3d
) {

  /** Scratch buffer reused for 3-float output (position, velocity). */
  private val buf3 = new Array[Float](3)

  /** Scratch buffer reused for 4-float output (quaternion). */
  private val buf4 = new Array[Float](4)

  // ─── Position & rotation ──────────────────────────────────────────────

  /** Returns the world-space position as (x, y, z). */
  def position: (Float, Float, Float) = {
    world.ops.bodyGetPosition(world.handle, handle, buf3)
    (buf3(0), buf3(1), buf3(2))
  }

  /** Teleports the body to the given position. */
  def position_=(pos: (Float, Float, Float)): Unit =
    world.ops.bodySetPosition(world.handle, handle, pos._1, pos._2, pos._3)

  /** Returns the rotation quaternion as (qx, qy, qz, qw). */
  def rotation: (Float, Float, Float, Float) = {
    world.ops.bodyGetRotation(world.handle, handle, buf4)
    (buf4(0), buf4(1), buf4(2), buf4(3))
  }

  /** Sets the rotation as a quaternion (qx, qy, qz, qw). */
  def rotation_=(q: (Float, Float, Float, Float)): Unit =
    world.ops.bodySetRotation(world.handle, handle, q._1, q._2, q._3, q._4)

  // ─── Velocity ─────────────────────────────────────────────────────────

  /** Returns the linear velocity as (vx, vy, vz). */
  def linearVelocity: (Float, Float, Float) = {
    world.ops.bodyGetLinearVelocity(world.handle, handle, buf3)
    (buf3(0), buf3(1), buf3(2))
  }

  /** Sets the linear velocity. */
  def linearVelocity_=(vel: (Float, Float, Float)): Unit =
    world.ops.bodySetLinearVelocity(world.handle, handle, vel._1, vel._2, vel._3)

  /** Returns the angular velocity as (wx, wy, wz). */
  def angularVelocity: (Float, Float, Float) = {
    world.ops.bodyGetAngularVelocity(world.handle, handle, buf3)
    (buf3(0), buf3(1), buf3(2))
  }

  /** Sets the angular velocity vector. */
  def angularVelocity_=(omega: (Float, Float, Float)): Unit =
    world.ops.bodySetAngularVelocity(world.handle, handle, omega._1, omega._2, omega._3)

  // ─── Forces & impulses ────────────────────────────────────────────────

  /** Applies a force at the center of mass (takes effect on next step). */
  def applyForce(fx: Float, fy: Float, fz: Float): Unit =
    world.ops.bodyApplyForce(world.handle, handle, fx, fy, fz)

  /** Applies an impulse at the center of mass (instantaneous velocity change). */
  def applyImpulse(ix: Float, iy: Float, iz: Float): Unit =
    world.ops.bodyApplyImpulse(world.handle, handle, ix, iy, iz)

  /** Applies a torque vector (takes effect on next step). */
  def applyTorque(tx: Float, ty: Float, tz: Float): Unit =
    world.ops.bodyApplyTorque(world.handle, handle, tx, ty, tz)

  /** Applies a force at a specific world-space point (generates torque if off-center). */
  def applyForceAtPoint(fx: Float, fy: Float, fz: Float, px: Float, py: Float, pz: Float): Unit =
    world.ops.bodyApplyForceAtPoint(world.handle, handle, fx, fy, fz, px, py, pz)

  /** Applies an impulse at a specific world-space point (generates angular impulse if off-center). */
  def applyImpulseAtPoint(ix: Float, iy: Float, iz: Float, px: Float, py: Float, pz: Float): Unit =
    world.ops.bodyApplyImpulseAtPoint(world.handle, handle, ix, iy, iz, px, py, pz)

  // ─── Damping & gravity ────────────────────────────────────────────────

  /** Gets the linear damping coefficient. */
  def linearDamping: Float =
    world.ops.bodyGetLinearDamping(world.handle, handle)

  /** Sets linear damping (velocity decay per second, 0 = no damping). */
  def linearDamping_=(d: Float): Unit =
    world.ops.bodySetLinearDamping(world.handle, handle, d)

  /** Gets the angular damping coefficient. */
  def angularDamping: Float =
    world.ops.bodyGetAngularDamping(world.handle, handle)

  /** Sets angular damping (angular velocity decay per second, 0 = no damping). */
  def angularDamping_=(d: Float): Unit =
    world.ops.bodySetAngularDamping(world.handle, handle, d)

  /** Gets the gravity scale for this body. */
  def gravityScale: Float =
    world.ops.bodyGetGravityScale(world.handle, handle)

  /** Sets the gravity scale for this body (1.0 = normal, 0.0 = no gravity). */
  def gravityScale_=(s: Float): Unit =
    world.ops.bodySetGravityScale(world.handle, handle, s)

  // ─── State ────────────────────────────────────────────────────────────

  /** Locks or unlocks rotation for this body. */
  def fixedRotation_=(fixed: Boolean): Unit =
    world.ops.bodySetFixedRotation(world.handle, handle, fixed)

  /** Returns true if the body is awake (actively simulating). */
  def isAwake: Boolean =
    world.ops.bodyIsAwake(world.handle, handle)

  /** Wakes up the body so it participates in simulation. */
  def wakeUp(): Unit =
    world.ops.bodyWakeUp(world.handle, handle)

  /** Forces the body to sleep (stop simulating until woken by contact or explicit wake). */
  def sleep(): Unit =
    world.ops.bodySleep(world.handle, handle)

  /** Returns true if this body is enabled (participating in simulation). */
  def isEnabled: Boolean =
    world.ops.bodyIsEnabled(world.handle, handle)

  /** Enables or disables this body. Disabled bodies are removed from simulation. */
  def isEnabled_=(enabled: Boolean): Unit =
    world.ops.bodySetEnabled(world.handle, handle, enabled)

  /** Returns true if continuous collision detection is enabled for this body. */
  def isCcdEnabled: Boolean =
    world.ops.bodyIsCcdEnabled(world.handle, handle)

  /** Enables or disables continuous collision detection for this body.
    *
    * CCD prevents fast-moving bodies from tunneling through thin objects.
    */
  def isCcdEnabled_=(enable: Boolean): Unit =
    world.ops.bodyEnableCcd(world.handle, handle, enable)

  /** Gets the dominance group (-127 to 127). Higher dominance bodies push lower ones. */
  def dominanceGroup: Int =
    world.ops.bodyGetDominanceGroup(world.handle, handle)

  /** Sets the dominance group (-127 to 127). */
  def dominanceGroup_=(group: Int): Unit =
    world.ops.bodySetDominanceGroup(world.handle, handle, group)

  // ─── Mass properties ──────────────────────────────────────────────────

  /** Returns the total mass of this body (computed from attached colliders). */
  def mass: Float =
    world.ops.bodyGetMass(world.handle, handle)

  /** Forces recomputation of mass properties from attached colliders. */
  def recomputeMassProperties(): Unit =
    world.ops.bodyRecomputeMassProperties(world.handle, handle)

  // ─── Collider attachment ──────────────────────────────────────────────

  /** Attaches a collider with the given 3D shape and material properties to this body.
    *
    * @param shape
    *   the 3D collision shape
    * @param density
    *   the collider density (affects mass), default 1.0
    * @param friction
    *   the friction coefficient, default 0.5
    * @param restitution
    *   the restitution (bounciness), default 0.0
    * @return
    *   the new [[Collider3d]] handle
    */
  def attachCollider(
    shape:       Shape3d,
    density:     Float = 1f,
    friction:    Float = 0.5f,
    restitution: Float = 0f
  ): Collider3d = {
    val ops = world.ops
    val wh  = world.handle
    val ch  = shape match {
      case Shape3d.Sphere(radius)                => ops.createSphereCollider(wh, handle, radius)
      case Shape3d.Box(hx, hy, hz)               => ops.createBoxCollider(wh, handle, hx, hy, hz)
      case Shape3d.Capsule(halfHeight, radius)   => ops.createCapsuleCollider(wh, handle, halfHeight, radius)
      case Shape3d.Cylinder(halfHeight, radius)  => ops.createCylinderCollider(wh, handle, halfHeight, radius)
      case Shape3d.Cone(halfHeight, radius)      => ops.createConeCollider(wh, handle, halfHeight, radius)
      case Shape3d.ConvexHull(vertices)          => ops.createConvexHullCollider(wh, handle, vertices, vertices.length / 3)
      case Shape3d.TriMesh(vertices, indices)    => ops.createTriMeshCollider(wh, handle, vertices, vertices.length / 3, indices, indices.length)
    }
    ops.colliderSetDensity(wh, ch, density)
    ops.colliderSetFriction(wh, ch, friction)
    ops.colliderSetRestitution(wh, ch, restitution)
    Collider3d(world, ch)
  }
}
