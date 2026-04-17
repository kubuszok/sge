/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: handle-based FFI, platform-agnostic trait
 */
package sge
package platform

/** Physics operations for 3D rigid body simulation backed by Rapier3D.
  *
  * All methods use primitives and Long handles to Rust-side Rapier3D objects. Platform implementations delegate to Rust via Panama FFM (JVM), C ABI (Native), or stub (JS).
  *
  * Key differences from 2D [[PhysicsOps]]:
  *   - Gravity is a 3-component vector (gx, gy, gz)
  *   - Body creation takes position (x, y, z) and quaternion rotation (qx, qy, qz, qw)
  *   - Position output is 3 floats, rotation output is 4 floats (quaternion)
  *   - Angular velocity is a 3-component vector (wx, wy, wz)
  *   - Forces, impulses, and torques are 3-component vectors
  *   - Additional 3D shapes: Cylinder, Cone
  *   - Ray cast output is 9 floats (hitXYZ, normalXYZ, toi, colliderLo, colliderHi)
  */
private[sge] trait PhysicsOps3d {

  // ─── World lifecycle ──────────────────────────────────────────────────

  /** Creates a new physics world with the given 3D gravity. Returns a world handle. */
  def createWorld(gravityX: Float, gravityY: Float, gravityZ: Float): Long

  /** Destroys a physics world and all its contents. */
  def destroyWorld(world: Long): Unit

  /** Advances the simulation by `dt` seconds. */
  def worldStep(world: Long, dt: Float): Unit

  /** Sets the world gravity vector. */
  def worldSetGravity(world: Long, gx: Float, gy: Float, gz: Float): Unit

  /** Gets the world gravity vector. Fills `out` with [gx, gy, gz]. */
  def worldGetGravity(world: Long, out: Array[Float]): Unit

  // ─── Rigid body ───────────────────────────────────────────────────────

  /** Creates a dynamic rigid body with position and quaternion rotation. Returns a body handle. */
  def createDynamicBody(world: Long, x: Float, y: Float, z: Float, qx: Float, qy: Float, qz: Float, qw: Float): Long

  /** Creates a static rigid body with position and quaternion rotation. Returns a body handle. */
  def createStaticBody(world: Long, x: Float, y: Float, z: Float, qx: Float, qy: Float, qz: Float, qw: Float): Long

  /** Creates a kinematic rigid body with position and quaternion rotation. Returns a body handle. */
  def createKinematicBody(world: Long, x: Float, y: Float, z: Float, qx: Float, qy: Float, qz: Float, qw: Float): Long

  /** Destroys a rigid body and detaches all its colliders. */
  def destroyBody(world: Long, body: Long): Unit

  /** Gets the body position. Fills `out` with [x, y, z]. */
  def bodyGetPosition(world: Long, body: Long, out: Array[Float]): Unit

  /** Gets the body rotation quaternion. Fills `out` with [qx, qy, qz, qw]. */
  def bodyGetRotation(world: Long, body: Long, out: Array[Float]): Unit

  /** Gets the body linear velocity. Fills `out` with [vx, vy, vz]. */
  def bodyGetLinearVelocity(world: Long, body: Long, out: Array[Float]): Unit

  /** Gets the body angular velocity vector. Fills `out` with [wx, wy, wz]. */
  def bodyGetAngularVelocity(world: Long, body: Long, out: Array[Float]): Unit

  /** Teleports the body to the given position. */
  def bodySetPosition(world: Long, body: Long, x: Float, y: Float, z: Float): Unit

  /** Sets the body rotation as a quaternion. */
  def bodySetRotation(world: Long, body: Long, qx: Float, qy: Float, qz: Float, qw: Float): Unit

  /** Sets the body linear velocity. */
  def bodySetLinearVelocity(world: Long, body: Long, vx: Float, vy: Float, vz: Float): Unit

  /** Sets the body angular velocity vector. */
  def bodySetAngularVelocity(world: Long, body: Long, wx: Float, wy: Float, wz: Float): Unit

  /** Applies a force at the body's center of mass (takes effect on next step). */
  def bodyApplyForce(world: Long, body: Long, fx: Float, fy: Float, fz: Float): Unit

  /** Applies an impulse at the body's center of mass (instantaneous velocity change). */
  def bodyApplyImpulse(world: Long, body: Long, ix: Float, iy: Float, iz: Float): Unit

  /** Applies a torque vector (takes effect on next step). */
  def bodyApplyTorque(world: Long, body: Long, tx: Float, ty: Float, tz: Float): Unit

  /** Applies a force at a specific world-space point (generates torque if off-center). */
  def bodyApplyForceAtPoint(world: Long, body: Long, fx: Float, fy: Float, fz: Float, px: Float, py: Float, pz: Float): Unit

  /** Applies an impulse at a specific world-space point (generates angular impulse if off-center). */
  def bodyApplyImpulseAtPoint(world: Long, body: Long, ix: Float, iy: Float, iz: Float, px: Float, py: Float, pz: Float): Unit

  /** Sets linear damping (velocity decay per second). */
  def bodySetLinearDamping(world: Long, body: Long, damping: Float): Unit

  /** Gets the linear damping coefficient. */
  def bodyGetLinearDamping(world: Long, body: Long): Float

  /** Sets angular damping (angular velocity decay per second). */
  def bodySetAngularDamping(world: Long, body: Long, damping: Float): Unit

  /** Gets the angular damping coefficient. */
  def bodyGetAngularDamping(world: Long, body: Long): Float

  /** Sets gravity scale for this body (1.0 = normal, 0.0 = no gravity). */
  def bodySetGravityScale(world: Long, body: Long, scale: Float): Unit

  /** Gets the gravity scale. */
  def bodyGetGravityScale(world: Long, body: Long): Float

  /** Returns true if the body is awake (simulating). */
  def bodyIsAwake(world: Long, body: Long): Boolean

  /** Wakes up the body so it participates in simulation. */
  def bodyWakeUp(world: Long, body: Long): Unit

  /** Forces the body to sleep (stop simulating until woken). */
  def bodySleep(world: Long, body: Long): Unit

  /** Locks or unlocks rotation for this body. */
  def bodySetFixedRotation(world: Long, body: Long, fixed: Boolean): Unit

  /** Enables or disables continuous collision detection for this body. */
  def bodyEnableCcd(world: Long, body: Long, enable: Boolean): Unit

  /** Returns true if CCD is enabled for this body. */
  def bodyIsCcdEnabled(world: Long, body: Long): Boolean

  /** Enables or disables the body (disabled bodies are removed from simulation). */
  def bodySetEnabled(world: Long, body: Long, enabled: Boolean): Unit

  /** Returns true if the body is enabled. */
  def bodyIsEnabled(world: Long, body: Long): Boolean

  /** Sets the dominance group (-127 to 127). Higher dominance bodies push lower ones. */
  def bodySetDominanceGroup(world: Long, body: Long, group: Int): Unit

  /** Gets the dominance group. */
  def bodyGetDominanceGroup(world: Long, body: Long): Int

  /** Gets the total mass of a rigid body. */
  def bodyGetMass(world: Long, body: Long): Float

  /** Forces recomputation of mass properties from attached colliders. */
  def bodyRecomputeMassProperties(world: Long, body: Long): Unit

  // ─── Collider ─────────────────────────────────────────────────────────

  /** Attaches a sphere collider to a body. Returns a collider handle. */
  def createSphereCollider(world: Long, body: Long, radius: Float): Long

  /** Attaches a box collider to a body. Returns a collider handle.
    *
    * @param hx
    *   half-extent along x axis
    * @param hy
    *   half-extent along y axis
    * @param hz
    *   half-extent along z axis
    */
  def createBoxCollider(world: Long, body: Long, hx: Float, hy: Float, hz: Float): Long

  /** Attaches a capsule collider to a body. Returns a collider handle.
    *
    * The capsule is aligned along the Y axis.
    *
    * @param halfHeight
    *   half the height of the cylindrical section
    * @param radius
    *   the cap radius
    */
  def createCapsuleCollider(world: Long, body: Long, halfHeight: Float, radius: Float): Long

  /** Attaches a cylinder collider to a body. Returns a collider handle.
    *
    * @param halfHeight
    *   half the height of the cylinder
    * @param radius
    *   the cylinder radius
    */
  def createCylinderCollider(world: Long, body: Long, halfHeight: Float, radius: Float): Long

  /** Attaches a cone collider to a body. Returns a collider handle.
    *
    * @param halfHeight
    *   half the height of the cone
    * @param radius
    *   the base radius of the cone
    */
  def createConeCollider(world: Long, body: Long, halfHeight: Float, radius: Float): Long

  /** Attaches a convex hull collider to a body. Returns a collider handle.
    *
    * @param vertices
    *   flat array of [x0, y0, z0, x1, y1, z1, ...] vertex positions
    * @param vertexCount
    *   number of vertices (one third of the array length)
    */
  def createConvexHullCollider(world: Long, body: Long, vertices: Array[Float], vertexCount: Int): Long

  /** Attaches a triangle mesh collider to a body. Returns a collider handle.
    *
    * @param vertices
    *   flat array of [x0, y0, z0, x1, y1, z1, ...] vertex positions
    * @param vertexCount
    *   number of vertices
    * @param indices
    *   flat array of triangle indices [i0, i1, i2, ...]
    * @param indexCount
    *   number of indices (must be a multiple of 3)
    */
  def createTriMeshCollider(world: Long, body: Long, vertices: Array[Float], vertexCount: Int, indices: Array[Int], indexCount: Int): Long

  /** Detaches and destroys a collider. */
  def destroyCollider(world: Long, collider: Long): Unit

  /** Sets the collider's density (affects mass). */
  def colliderSetDensity(world: Long, collider: Long, density: Float): Unit

  /** Sets the collider's friction coefficient. */
  def colliderSetFriction(world: Long, collider: Long, friction: Float): Unit

  /** Sets the collider's restitution (bounciness). */
  def colliderSetRestitution(world: Long, collider: Long, restitution: Float): Unit

  /** Sets whether this collider is a sensor (detects overlap but no physical response). */
  def colliderSetSensor(world: Long, collider: Long, sensor: Boolean): Unit

  // ─── Joints ───────────────────────────────────────────────────────────

  /** Creates a fixed (weld) joint between two bodies. Returns a joint handle. */
  def createFixedJoint(world: Long, body1: Long, body2: Long): Long

  /** Creates a rope joint that constrains two bodies to stay within a maximum distance. Returns a joint handle. */
  def createRopeJoint(world: Long, body1: Long, body2: Long, maxDist: Float): Long

  /** Destroys a joint. */
  def destroyJoint(world: Long, joint: Long): Unit

  // ─── Queries ──────────────────────────────────────────────────────────

  /** Casts a ray and returns the first hit.
    *
    * Fills `out` with [hitX, hitY, hitZ, normalX, normalY, normalZ, toi, colliderHandleLo, colliderHandleHi] (9 floats). Returns true if a hit was found.
    */
  def rayCast(
    world:   Long,
    originX: Float,
    originY: Float,
    originZ: Float,
    dirX:    Float,
    dirY:    Float,
    dirZ:    Float,
    maxDist: Float,
    out:     Array[Float]
  ): Boolean

  // ─── Contact events (polling) ─────────────────────────────────────────

  /** Polls contact-start events since the last step.
    *
    * Fills `outCollider1` and `outCollider2` with collider handle pairs. Returns the event count (capped at `maxEvents`).
    */
  def pollContactStartEvents(world: Long, outCollider1: Array[Long], outCollider2: Array[Long], maxEvents: Int): Int

  /** Polls contact-stop events since the last step.
    *
    * Fills `outCollider1` and `outCollider2` with collider handle pairs. Returns the event count (capped at `maxEvents`).
    */
  def pollContactStopEvents(world: Long, outCollider1: Array[Long], outCollider2: Array[Long], maxEvents: Int): Int
}
