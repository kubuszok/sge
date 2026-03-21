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
package platform

/** Physics operations for 2D rigid body simulation backed by Rapier2D.
  *
  * All methods use primitives and Long handles to Rust-side Rapier2D objects. Platform implementations delegate to Rust via Panama FFM (JVM), C ABI (Native), or stub (JS).
  */
private[sge] trait PhysicsOps {

  // ─── World lifecycle ──────────────────────────────────────────────────

  /** Creates a new physics world with the given gravity. Returns a world handle. */
  def createWorld(gravityX: Float, gravityY: Float): Long

  /** Destroys a physics world and all its contents. */
  def destroyWorld(world: Long): Unit

  /** Advances the simulation by `dt` seconds. */
  def worldStep(world: Long, dt: Float): Unit

  /** Sets the world gravity vector. */
  def worldSetGravity(world: Long, gx: Float, gy: Float): Unit

  /** Gets the world gravity vector. Fills `out` with [gx, gy]. */
  def worldGetGravity(world: Long, out: Array[Float]): Unit

  // ─── Rigid body ───────────────────────────────────────────────────────

  /** Creates a dynamic rigid body. Returns a body handle. */
  def createDynamicBody(world: Long, x: Float, y: Float, angle: Float): Long

  /** Creates a static rigid body. Returns a body handle. */
  def createStaticBody(world: Long, x: Float, y: Float, angle: Float): Long

  /** Creates a kinematic rigid body. Returns a body handle. */
  def createKinematicBody(world: Long, x: Float, y: Float, angle: Float): Long

  /** Destroys a rigid body and detaches all its colliders. */
  def destroyBody(world: Long, body: Long): Unit

  /** Gets the body position. Fills `out` with [x, y]. */
  def bodyGetPosition(world: Long, body: Long, out: Array[Float]): Unit

  /** Gets the body rotation angle in radians. */
  def bodyGetAngle(world: Long, body: Long): Float

  /** Gets the body linear velocity. Fills `out` with [vx, vy]. */
  def bodyGetLinearVelocity(world: Long, body: Long, out: Array[Float]): Unit

  /** Gets the body angular velocity in radians/second. */
  def bodyGetAngularVelocity(world: Long, body: Long): Float

  /** Teleports the body to the given position. */
  def bodySetPosition(world: Long, body: Long, x: Float, y: Float): Unit

  /** Sets the body rotation angle in radians. */
  def bodySetAngle(world: Long, body: Long, angle: Float): Unit

  /** Sets the body linear velocity. */
  def bodySetLinearVelocity(world: Long, body: Long, vx: Float, vy: Float): Unit

  /** Sets the body angular velocity in radians/second. */
  def bodySetAngularVelocity(world: Long, body: Long, omega: Float): Unit

  /** Applies a force at the body's center of mass (takes effect on next step). */
  def bodyApplyForce(world: Long, body: Long, fx: Float, fy: Float): Unit

  /** Applies an impulse at the body's center of mass (instantaneous velocity change). */
  def bodyApplyImpulse(world: Long, body: Long, ix: Float, iy: Float): Unit

  /** Applies a torque (takes effect on next step). */
  def bodyApplyTorque(world: Long, body: Long, torque: Float): Unit

  /** Sets linear damping (velocity decay per second). */
  def bodySetLinearDamping(world: Long, body: Long, damping: Float): Unit

  /** Sets angular damping (angular velocity decay per second). */
  def bodySetAngularDamping(world: Long, body: Long, damping: Float): Unit

  /** Sets gravity scale for this body (1.0 = normal, 0.0 = no gravity). */
  def bodySetGravityScale(world: Long, body: Long, scale: Float): Unit

  /** Returns true if the body is awake (simulating). */
  def bodyIsAwake(world: Long, body: Long): Boolean

  /** Wakes up the body so it participates in simulation. */
  def bodyWakeUp(world: Long, body: Long): Unit

  /** Locks or unlocks rotation for this body. */
  def bodySetFixedRotation(world: Long, body: Long, fixed: Boolean): Unit

  // ─── Collider ─────────────────────────────────────────────────────────

  /** Attaches a circle collider to a body. Returns a collider handle. */
  def createCircleCollider(world: Long, body: Long, radius: Float): Long

  /** Attaches a box collider to a body. Returns a collider handle. */
  def createBoxCollider(world: Long, body: Long, halfWidth: Float, halfHeight: Float): Long

  /** Attaches a capsule collider to a body. Returns a collider handle. */
  def createCapsuleCollider(world: Long, body: Long, halfHeight: Float, radius: Float): Long

  /** Attaches a convex polygon collider to a body. Returns a collider handle.
    *
    * @param vertices
    *   flat array of [x0, y0, x1, y1, ...] vertex positions
    * @param vertexCount
    *   number of vertices (half the array length)
    */
  def createPolygonCollider(world: Long, body: Long, vertices: Array[Float], vertexCount: Int): Long

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

  /** Creates a revolute (hinge) joint at the given world-space anchor. Returns a joint handle. */
  def createRevoluteJoint(world: Long, body1: Long, body2: Long, anchorX: Float, anchorY: Float): Long

  /** Creates a prismatic (slider) joint along the given axis. Returns a joint handle. */
  def createPrismaticJoint(world: Long, body1: Long, body2: Long, axisX: Float, axisY: Float): Long

  /** Creates a fixed (weld) joint between two bodies. Returns a joint handle. */
  def createFixedJoint(world: Long, body1: Long, body2: Long): Long

  /** Destroys a joint. */
  def destroyJoint(world: Long, joint: Long): Unit

  // ─── Queries ──────────────────────────────────────────────────────────

  /** Casts a ray and returns the first hit.
    *
    * Fills `out` with [hitX, hitY, normalX, normalY, toi, bodyHandleLo, bodyHandleHi] (7 floats). Returns true if a hit was found.
    */
  def rayCast(
    world:   Long,
    originX: Float,
    originY: Float,
    dirX:    Float,
    dirY:    Float,
    maxDist: Float,
    out:     Array[Float]
  ): Boolean

  /** Finds all bodies whose colliders contain the given point.
    *
    * Fills `outBodies` with body handles and returns the count of results (capped at `maxResults`).
    */
  def queryPoint(world: Long, x: Float, y: Float, outBodies: Array[Long], maxResults: Int): Int

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
