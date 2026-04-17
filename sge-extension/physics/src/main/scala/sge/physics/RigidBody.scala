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
  * Provides access to position, velocity, forces, and collider attachment. Bodies are created via [[PhysicsWorld.createBody]] and destroyed via [[PhysicsWorld.destroyBody]].
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

  /** Applies a force at a specific world-space point (generates torque if off-center). */
  def applyForceAtPoint(fx: Float, fy: Float, px: Float, py: Float): Unit =
    world.ops.bodyApplyForceAtPoint(world.handle, handle, fx, fy, px, py)

  /** Applies an impulse at the center of mass (instantaneous velocity change). */
  def applyImpulse(ix: Float, iy: Float): Unit =
    world.ops.bodyApplyImpulse(world.handle, handle, ix, iy)

  /** Applies an impulse at a specific world-space point (generates angular impulse if off-center). */
  def applyImpulseAtPoint(ix: Float, iy: Float, px: Float, py: Float): Unit =
    world.ops.bodyApplyImpulseAtPoint(world.handle, handle, ix, iy, px, py)

  /** Applies a torque (takes effect on next step). */
  def applyTorque(torque: Float): Unit =
    world.ops.bodyApplyTorque(world.handle, handle, torque)

  /** Applies an angular impulse (instantaneous change to angular velocity). */
  def applyTorqueImpulse(impulse: Float): Unit =
    world.ops.bodyApplyTorqueImpulse(world.handle, handle, impulse)

  /** Resets all accumulated forces on this body. */
  def resetForces(): Unit =
    world.ops.bodyResetForces(world.handle, handle)

  /** Resets all accumulated torques on this body. */
  def resetTorques(): Unit =
    world.ops.bodyResetTorques(world.handle, handle)

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

  /** Sets which translation axes are enabled for this body. */
  def setEnabledTranslations(allowX: Boolean, allowY: Boolean): Unit =
    world.ops.bodySetEnabledTranslations(world.handle, handle, allowX, allowY)

  /** Returns true if translation along the X axis is locked. */
  def isTranslationLockedX: Boolean =
    world.ops.bodyIsTranslationLockedX(world.handle, handle)

  /** Returns true if translation along the Y axis is locked. */
  def isTranslationLockedY: Boolean =
    world.ops.bodyIsTranslationLockedY(world.handle, handle)

  /** Returns true if rotation is locked. */
  def isRotationLocked: Boolean =
    world.ops.bodyIsRotationLocked(world.handle, handle)

  /** Gets the dominance group (-127 to 127). Higher dominance bodies push lower ones. */
  def dominanceGroup: Int =
    world.ops.bodyGetDominanceGroup(world.handle, handle)

  /** Sets the dominance group (-127 to 127). */
  def dominanceGroup_=(group: Int): Unit =
    world.ops.bodySetDominanceGroup(world.handle, handle, group)

  /** Gets the world-space center of mass as (x, y). */
  def worldCenterOfMass: (Float, Float) = {
    world.ops.bodyGetWorldCenterOfMass(world.handle, handle, buf2)
    (buf2(0), buf2(1))
  }

  /** Returns true if continuous collision detection is enabled for this body. */
  def isCcdEnabled: Boolean =
    world.ops.bodyIsCcdEnabled(world.handle, handle)

  /** Enables or disables continuous collision detection for this body.
    *
    * CCD prevents fast-moving bodies from tunneling through thin objects.
    */
  def isCcdEnabled_=(enable: Boolean): Unit =
    world.ops.bodyEnableCcd(world.handle, handle, enable)

  /** Gets the velocity at a world-space point on the body as (vx, vy).
    *
    * Useful for computing the velocity of a specific point on a rotating body.
    */
  def velocityAtPoint(px: Float, py: Float): (Float, Float) = {
    world.ops.bodyGetVelocityAtPoint(world.handle, handle, px, py, buf2)
    (buf2(0), buf2(1))
  }

  // ─── Mass properties ──────────────────────────────────────────────────

  /** Returns the total mass of this body (computed from attached colliders). */
  def mass: Float =
    world.ops.bodyGetMass(world.handle, handle)

  /** Returns the angular inertia (moment of inertia) of this body. */
  def inertia: Float =
    world.ops.bodyGetInertia(world.handle, handle)

  /** Returns the local center of mass as (x, y). */
  def localCenterOfMass: (Float, Float) = {
    world.ops.bodyGetLocalCenterOfMass(world.handle, handle, buf2)
    (buf2(0), buf2(1))
  }

  /** Forces recomputation of mass properties from attached colliders. */
  def recomputeMassProperties(): Unit =
    world.ops.bodyRecomputeMassProperties(world.handle, handle)

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
    val ch  = shape match {
      case Shape.Circle(radius)               => ops.createCircleCollider(wh, handle, radius)
      case Shape.Box(halfWidth, halfHeight)   => ops.createBoxCollider(wh, handle, halfWidth, halfHeight)
      case Shape.Capsule(halfHeight, radius)  => ops.createCapsuleCollider(wh, handle, halfHeight, radius)
      case Shape.Polygon(vertices)            => ops.createPolygonCollider(wh, handle, vertices, vertices.length / 2)
      case Shape.Segment(x1, y1, x2, y2)      => ops.createSegmentCollider(wh, handle, x1, y1, x2, y2)
      case Shape.Polyline(vertices)           => ops.createPolylineCollider(wh, handle, vertices, vertices.length / 2)
      case Shape.TriMesh(vertices, indices)   => ops.createTriMeshCollider(wh, handle, vertices, vertices.length / 2, indices, indices.length)
      case Shape.Heightfield(heights, sx, sy) => ops.createHeightfieldCollider(wh, handle, heights, heights.length, sx, sy)
    }
    ops.colliderSetDensity(wh, ch, density)
    ops.colliderSetFriction(wh, ch, friction)
    ops.colliderSetRestitution(wh, ch, restitution)
    Collider(world, ch)
  }
}
