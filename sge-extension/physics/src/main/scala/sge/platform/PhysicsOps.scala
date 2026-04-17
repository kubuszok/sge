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

  /** Applies a force at a specific world-space point (generates torque if off-center). */
  def bodyApplyForceAtPoint(world: Long, body: Long, fx: Float, fy: Float, px: Float, py: Float): Unit

  /** Applies an impulse at a specific world-space point (generates angular impulse if off-center). */
  def bodyApplyImpulseAtPoint(world: Long, body: Long, ix: Float, iy: Float, px: Float, py: Float): Unit

  /** Applies an angular impulse (instantaneous change to angular velocity). */
  def bodyApplyTorqueImpulse(world: Long, body: Long, impulse: Float): Unit

  /** Resets all accumulated forces on the body. */
  def bodyResetForces(world: Long, body: Long): Unit

  /** Resets all accumulated torques on the body. */
  def bodyResetTorques(world: Long, body: Long): Unit

  /** Gets the linear damping coefficient. */
  def bodyGetLinearDamping(world: Long, body: Long): Float

  /** Gets the angular damping coefficient. */
  def bodyGetAngularDamping(world: Long, body: Long): Float

  /** Gets the gravity scale. */
  def bodyGetGravityScale(world: Long, body: Long): Float

  /** Returns the body type: 0=dynamic, 1=fixed(static), 2=kinematic-position, 3=kinematic-velocity. */
  def bodyGetType(world: Long, body: Long): Int

  /** Enables or disables the body (disabled bodies are removed from simulation). */
  def bodySetEnabled(world: Long, body: Long, enabled: Boolean): Unit

  /** Returns true if the body is enabled. */
  def bodyIsEnabled(world: Long, body: Long): Boolean

  /** Sets which translation axes are enabled for the body. */
  def bodySetEnabledTranslations(world: Long, body: Long, allowX: Boolean, allowY: Boolean): Unit

  /** Returns true if translation along the X axis is locked. */
  def bodyIsTranslationLockedX(world: Long, body: Long): Boolean

  /** Returns true if translation along the Y axis is locked. */
  def bodyIsTranslationLockedY(world: Long, body: Long): Boolean

  /** Returns true if rotation is locked. */
  def bodyIsRotationLocked(world: Long, body: Long): Boolean

  /** Sets the dominance group (-127 to 127). Higher dominance bodies push lower ones. */
  def bodySetDominanceGroup(world: Long, body: Long, group: Int): Unit

  /** Gets the dominance group. */
  def bodyGetDominanceGroup(world: Long, body: Long): Int

  /** Gets the world-space center of mass. Fills `out` with [x, y]. */
  def bodyGetWorldCenterOfMass(world: Long, body: Long, out: Array[Float]): Unit

  /** Enables or disables continuous collision detection for this body. */
  def bodyEnableCcd(world: Long, body: Long, enable: Boolean): Unit

  /** Returns true if CCD is enabled for this body. */
  def bodyIsCcdEnabled(world: Long, body: Long): Boolean

  /** Forces the body to sleep (stop simulating until woken). */
  def bodySleep(world: Long, body: Long): Unit

  /** Gets the velocity at a world-space point on the body. Fills `out` with [vx, vy]. */
  def bodyGetVelocityAtPoint(world: Long, body: Long, px: Float, py: Float, out: Array[Float]): Unit

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

  /** Attaches a segment (edge) collider to a body. Returns a collider handle.
    *
    * @param x1
    *   first endpoint x
    * @param y1
    *   first endpoint y
    * @param x2
    *   second endpoint x
    * @param y2
    *   second endpoint y
    */
  def createSegmentCollider(world: Long, body: Long, x1: Float, y1: Float, x2: Float, y2: Float): Long

  /** Attaches a polyline (chain) collider to a body. Returns a collider handle.
    *
    * @param vertices
    *   flat array of [x0, y0, x1, y1, ...] vertex positions
    * @param vertexCount
    *   number of vertices (half the array length)
    */
  def createPolylineCollider(world: Long, body: Long, vertices: Array[Float], vertexCount: Int): Long

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

  /** Gets the collider's density. */
  def colliderGetDensity(world: Long, collider: Long): Float

  /** Gets the collider's friction coefficient. */
  def colliderGetFriction(world: Long, collider: Long): Float

  /** Gets the collider's restitution (bounciness). */
  def colliderGetRestitution(world: Long, collider: Long): Float

  /** Returns true if the collider is a sensor. */
  def colliderIsSensor(world: Long, collider: Long): Boolean

  /** Enables or disables the collider. */
  def colliderSetEnabled(world: Long, collider: Long, enabled: Boolean): Unit

  /** Returns true if the collider is enabled. */
  def colliderIsEnabled(world: Long, collider: Long): Boolean

  /** Gets the collider position relative to its parent body. Fills `out` with [x, y, angle]. */
  def colliderGetPositionWrtParent(world: Long, collider: Long, out: Array[Float]): Unit

  /** Sets the collider position relative to its parent body. */
  def colliderSetPositionWrtParent(world: Long, collider: Long, x: Float, y: Float, angle: Float): Unit

  /** Gets the collider world position. Fills `out` with [x, y, angle]. */
  def colliderGetPosition(world: Long, collider: Long, out: Array[Float]): Unit

  /** Returns the collider shape type: 0=ball, 1=cuboid, 2=capsule, 3=segment, 4=triangle, 5=trimesh, 6=polyline, 7=heightfield, 8=compound, 9=convex_polygon, -1=unknown.
    */
  def colliderGetShapeType(world: Long, collider: Long): Int

  /** Gets the collider AABB. Fills `out` with [minX, minY, maxX, maxY]. */
  def colliderGetAabb(world: Long, collider: Long, out: Array[Float]): Unit

  /** Gets the parent body handle of a collider. Returns 0 if no parent. */
  def colliderGetParentBody(world: Long, collider: Long): Long

  /** Gets the mass of a collider. */
  def colliderGetMass(world: Long, collider: Long): Float

  /** Sets the mass of a collider (overrides density-based mass). */
  def colliderSetMass(world: Long, collider: Long, mass: Float): Unit

  /** Sets the contact skin (margin around the collider for early contact detection). */
  def colliderSetContactSkin(world: Long, collider: Long, skin: Float): Unit

  /** Sets which events this collider generates (bitmask of ActiveEvents flags). */
  def colliderSetActiveEvents(world: Long, collider: Long, flags: Int): Unit

  /** Gets the active events flags for this collider. */
  def colliderGetActiveEvents(world: Long, collider: Long): Int

  /** Sets which collision types this collider participates in (bitmask of ActiveCollisionTypes). */
  def colliderSetActiveCollisionTypes(world: Long, collider: Long, flags: Int): Unit

  /** Gets the active collision types flags for this collider. */
  def colliderGetActiveCollisionTypes(world: Long, collider: Long): Int

  // ─── New shape types ──────────────────────────────────────────────

  /** Attaches a triangle mesh collider to a body. Returns a collider handle.
    *
    * @param vertices
    *   flat array of [x0, y0, x1, y1, ...] vertex positions
    * @param vertexCount
    *   number of vertices
    * @param indices
    *   flat array of triangle indices [i0, i1, i2, ...]
    * @param indexCount
    *   number of indices (must be a multiple of 3)
    */
  def createTriMeshCollider(world: Long, body: Long, vertices: Array[Float], vertexCount: Int, indices: Array[Int], indexCount: Int): Long

  /** Attaches a heightfield collider to a body. Returns a collider handle.
    *
    * @param heights
    *   array of height values (one per column)
    * @param numCols
    *   number of columns
    * @param scaleX
    *   horizontal scale
    * @param scaleY
    *   vertical scale
    */
  def createHeightfieldCollider(world: Long, body: Long, heights: Array[Float], numCols: Int, scaleX: Float, scaleY: Float): Long

  // ─── Collision filtering ──────────────────────────────────────────

  /** Sets the collision groups for a collider.
    * @param memberships
    *   which groups this collider belongs to (bitmask)
    * @param filter
    *   which groups this collider can collide with (bitmask)
    */
  def colliderSetCollisionGroups(world: Long, collider: Long, memberships: Int, filter: Int): Unit

  /** Gets the collision groups for a collider. Fills `out` with [memberships, filter]. */
  def colliderGetCollisionGroups(world: Long, collider: Long, out: Array[Int]): Unit

  /** Sets the solver groups for a collider (controls force response, not detection).
    * @param memberships
    *   which groups this collider belongs to (bitmask)
    * @param filter
    *   which groups this collider interacts with for force response (bitmask)
    */
  def colliderSetSolverGroups(world: Long, collider: Long, memberships: Int, filter: Int): Unit

  /** Gets the solver groups for a collider. Fills `out` with [memberships, filter]. */
  def colliderGetSolverGroups(world: Long, collider: Long, out: Array[Int]): Unit

  // ─── Joints ───────────────────────────────────────────────────────────

  /** Creates a revolute (hinge) joint at the given world-space anchor. Returns a joint handle. */
  def createRevoluteJoint(world: Long, body1: Long, body2: Long, anchorX: Float, anchorY: Float): Long

  /** Creates a prismatic (slider) joint along the given axis. Returns a joint handle. */
  def createPrismaticJoint(world: Long, body1: Long, body2: Long, axisX: Float, axisY: Float): Long

  /** Creates a fixed (weld) joint between two bodies. Returns a joint handle. */
  def createFixedJoint(world: Long, body1: Long, body2: Long): Long

  /** Creates a rope joint that constrains two bodies to stay within a maximum distance. Returns a joint handle. */
  def createRopeJoint(world: Long, body1: Long, body2: Long, maxDist: Float): Long

  /** Destroys a joint. */
  def destroyJoint(world: Long, joint: Long): Unit

  // ─── Revolute joint limits and motors ─────────────────────────────

  /** Enables or disables angular limits on a revolute joint. */
  def revoluteJointEnableLimits(world: Long, joint: Long, enable: Boolean): Unit

  /** Sets the angular limits (in radians) for a revolute joint. */
  def revoluteJointSetLimits(world: Long, joint: Long, lower: Float, upper: Float): Unit

  /** Gets the angular limits for a revolute joint. Fills `out` with [lower, upper]. */
  def revoluteJointGetLimits(world: Long, joint: Long, out: Array[Float]): Unit

  /** Returns true if the revolute joint has limits enabled. */
  def revoluteJointIsLimitEnabled(world: Long, joint: Long): Boolean

  /** Enables the motor on a revolute joint. */
  def revoluteJointEnableMotor(world: Long, joint: Long, enable: Boolean): Unit

  /** Sets the target velocity for the revolute joint motor (radians/second). */
  def revoluteJointSetMotorSpeed(world: Long, joint: Long, speed: Float): Unit

  /** Sets the maximum torque the revolute joint motor can apply. */
  def revoluteJointSetMaxMotorTorque(world: Long, joint: Long, torque: Float): Unit

  /** Gets the current motor speed setting for a revolute joint. */
  def revoluteJointGetMotorSpeed(world: Long, joint: Long): Float

  /** Gets the current angle of the revolute joint (radians). */
  def revoluteJointGetAngle(world: Long, joint: Long): Float

  // ─── Prismatic joint limits and motors ────────────────────────────

  /** Enables or disables translation limits on a prismatic joint. */
  def prismaticJointEnableLimits(world: Long, joint: Long, enable: Boolean): Unit

  /** Sets the translation limits for a prismatic joint. */
  def prismaticJointSetLimits(world: Long, joint: Long, lower: Float, upper: Float): Unit

  /** Gets the translation limits for a prismatic joint. Fills `out` with [lower, upper]. */
  def prismaticJointGetLimits(world: Long, joint: Long, out: Array[Float]): Unit

  /** Enables the motor on a prismatic joint. */
  def prismaticJointEnableMotor(world: Long, joint: Long, enable: Boolean): Unit

  /** Sets the target velocity for the prismatic joint motor. */
  def prismaticJointSetMotorSpeed(world: Long, joint: Long, speed: Float): Unit

  /** Sets the maximum force the prismatic joint motor can apply. */
  def prismaticJointSetMaxMotorForce(world: Long, joint: Long, force: Float): Unit

  /** Gets the current translation of the prismatic joint. */
  def prismaticJointGetTranslation(world: Long, joint: Long): Float

  // ─── Motor joint ───────────────────────────────────────────────────

  /** Creates a motor joint between two bodies. Returns a joint handle. */
  def createMotorJoint(world: Long, body1: Long, body2: Long): Long

  /** Sets the linear offset target for a motor joint. */
  def motorJointSetLinearOffset(world: Long, joint: Long, x: Float, y: Float): Unit

  /** Gets the linear offset target for a motor joint. Fills `out` with [x, y]. */
  def motorJointGetLinearOffset(world: Long, joint: Long, out: Array[Float]): Unit

  /** Sets the angular offset target for a motor joint (radians). */
  def motorJointSetAngularOffset(world: Long, joint: Long, angle: Float): Unit

  /** Gets the angular offset target for a motor joint (radians). */
  def motorJointGetAngularOffset(world: Long, joint: Long): Float

  /** Sets the maximum force the motor joint can apply. */
  def motorJointSetMaxForce(world: Long, joint: Long, force: Float): Unit

  /** Sets the maximum torque the motor joint can apply. */
  def motorJointSetMaxTorque(world: Long, joint: Long, torque: Float): Unit

  /** Sets the correction factor for the motor joint (0 = no correction, 1 = full correction per step). */
  def motorJointSetCorrectionFactor(world: Long, joint: Long, factor: Float): Unit

  // ─── Revolute joint getters ────────────────────────────────────────

  /** Gets the maximum motor torque for a revolute joint. */
  def revoluteJointGetMaxMotorTorque(world: Long, joint: Long): Float

  // ─── Prismatic joint getters ──────────────────────────────────────

  /** Gets the current motor speed setting for a prismatic joint. */
  def prismaticJointGetMotorSpeed(world: Long, joint: Long): Float

  /** Gets the maximum motor force for a prismatic joint. */
  def prismaticJointGetMaxMotorForce(world: Long, joint: Long): Float

  // ─── Motor joint getters ──────────────────────────────────────────

  /** Gets the maximum force of the motor joint. */
  def motorJointGetMaxForce(world: Long, joint: Long): Float

  /** Gets the maximum torque of the motor joint. */
  def motorJointGetMaxTorque(world: Long, joint: Long): Float

  /** Gets the correction factor for the motor joint. */
  def motorJointGetCorrectionFactor(world: Long, joint: Long): Float

  // ─── Rope joint ───────────────────────────────────────────────────

  /** Sets the maximum distance for a rope joint. */
  def ropeJointSetMaxDistance(world: Long, joint: Long, maxDist: Float): Unit

  /** Gets the maximum distance for a rope joint. */
  def ropeJointGetMaxDistance(world: Long, joint: Long): Float

  // ─── Spring joint ─────────────────────────────────────────────────

  /** Creates a spring joint between two bodies. Returns a joint handle.
    *
    * @param restLength
    *   the natural length of the spring
    * @param stiffness
    *   the spring stiffness (higher = stiffer)
    * @param damping
    *   the spring damping (higher = less oscillation)
    */
  def createSpringJoint(world: Long, body1: Long, body2: Long, restLength: Float, stiffness: Float, damping: Float): Long

  /** Sets the rest length of a spring joint. */
  def springJointSetRestLength(world: Long, joint: Long, restLength: Float): Unit

  /** Gets the rest length of a spring joint. */
  def springJointGetRestLength(world: Long, joint: Long): Float

  /** Sets the stiffness and damping parameters of a spring joint. */
  def springJointSetParams(world: Long, joint: Long, stiffness: Float, damping: Float): Unit

  // ─── Body mass/inertia ────────────────────────────────────────────

  /** Gets the total mass of a rigid body. */
  def bodyGetMass(world: Long, body: Long): Float

  /** Gets the angular inertia of a rigid body. */
  def bodyGetInertia(world: Long, body: Long): Float

  /** Gets the local center of mass. Fills `out` with [x, y]. */
  def bodyGetLocalCenterOfMass(world: Long, body: Long, out: Array[Float]): Unit

  /** Forces recomputation of mass properties from attached colliders. */
  def bodyRecomputeMassProperties(world: Long, body: Long): Unit

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

  /** Finds all colliders intersecting the given axis-aligned bounding box.
    *
    * Fills `outColliders` with collider handles and returns the count of results (capped at `maxResults`).
    */
  def queryAABB(world: Long, minX: Float, minY: Float, maxX: Float, maxY: Float, outColliders: Array[Long], maxResults: Int): Int

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

  // ─── Contact detail queries ───────────────────────────────────────

  /** Returns the number of contact points between two colliders. */
  def contactPairCount(world: Long, collider1: Long, collider2: Long): Int

  /** Gets contact point details between two colliders.
    *
    * Fills `out` with [normalX, normalY, pointX, pointY, penetration] per point (5 floats each). Returns the actual number of points written (capped at `maxPoints`).
    */
  def contactPairPoints(world: Long, collider1: Long, collider2: Long, out: Array[Float], maxPoints: Int): Int

  // ─── Advanced queries ─────────────────────────────────────────────────

  /** Casts a shape and returns the first hit.
    *
    * @param shapeType
    *   0=circle, 1=box, 2=capsule
    * @param shapeParams
    *   shape-dependent: circle=[radius], box=[halfW,halfH], capsule=[halfH,radius]
    * @param out
    *   [hitX, hitY, normalX, normalY, toi, colliderLo, colliderHi] (7 floats)
    * @return
    *   true if a hit was found
    */
  def castShape(
    world:       Long,
    shapeType:   Int,
    shapeParams: Array[Float],
    originX:     Float,
    originY:     Float,
    dirX:        Float,
    dirY:        Float,
    maxDist:     Float,
    out:         Array[Float]
  ): Boolean

  /** Casts a ray and returns ALL hits (up to `maxHits`).
    *
    * Fills `outHits` with 7 floats per hit: [hitX, hitY, normalX, normalY, toi, colliderLo, colliderHi]. Returns the number of hits.
    */
  def rayCastAll(
    world:   Long,
    originX: Float,
    originY: Float,
    dirX:    Float,
    dirY:    Float,
    maxDist: Float,
    outHits: Array[Float],
    maxHits: Int
  ): Int

  /** Projects a point onto the nearest collider surface.
    *
    * Fills `out` with [projX, projY, isInside (1.0 or 0.0), colliderLo, colliderHi] (5 floats). Returns true if a result was found.
    */
  def projectPoint(world: Long, x: Float, y: Float, out: Array[Float]): Boolean

  // ─── Intersection events (sensor overlaps) ────────────────────────────

  /** Polls intersection-start events (sensor overlaps) since the last step. */
  def pollIntersectionStartEvents(world: Long, outCollider1: Array[Long], outCollider2: Array[Long], maxEvents: Int): Int

  /** Polls intersection-stop events (sensor separation) since the last step. */
  def pollIntersectionStopEvents(world: Long, outCollider1: Array[Long], outCollider2: Array[Long], maxEvents: Int): Int

  // ─── Solver parameters ────────────────────────────────────────────────

  /** Sets the number of solver iterations. More iterations = more accuracy but slower. */
  def worldSetNumSolverIterations(world: Long, iters: Int): Unit

  /** Gets the number of solver iterations. */
  def worldGetNumSolverIterations(world: Long): Int

  /** Sets the number of additional friction iterations. */
  def worldSetNumAdditionalFrictionIterations(world: Long, iters: Int): Unit

  /** Sets the number of internal PGS iterations. */
  def worldSetNumInternalPgsIterations(world: Long, iters: Int): Unit

  // ─── Shape intersection ───────────────────────────────────────────────

  /** Tests if a shape at a given position overlaps any collider.
    *
    * @param shapeType
    *   0=circle, 1=box, 2=capsule
    * @param shapeParams
    *   shape-dependent: circle=[radius], box=[halfW,halfH], capsule=[halfH,radius]
    * @param outColliders
    *   array to fill with collider handles
    * @param maxResults
    *   maximum number of results
    * @return
    *   the number of overlapping colliders
    */
  def intersectShape(
    world:        Long,
    shapeType:    Int,
    shapeParams:  Array[Float],
    posX:         Float,
    posY:         Float,
    angle:        Float,
    outColliders: Array[Long],
    maxResults:   Int
  ): Int
}
