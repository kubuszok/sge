/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: handle-based FFI, platform-agnostic trait
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 525
 * Covenant-baseline-methods: PhysicsOps3d,bodyApplyForce,bodyApplyForceAtPoint,bodyApplyImpulse,bodyApplyImpulseAtPoint,bodyApplyTorque,bodyApplyTorqueImpulse,bodyEnableCcd,bodyGetAngularDamping,bodyGetAngularVelocity,bodyGetDominanceGroup,bodyGetGravityScale,bodyGetInertia,bodyGetLinearDamping,bodyGetLinearVelocity,bodyGetLocalCenterOfMass,bodyGetMass,bodyGetPosition,bodyGetRotation,bodyGetType,bodyGetVelocityAtPoint,bodyGetWorldCenterOfMass,bodyIsAwake,bodyIsCcdEnabled,bodyIsEnabled,bodyIsRotationLockedX,bodyIsRotationLockedY,bodyIsRotationLockedZ,bodyIsTranslationLockedX,bodyIsTranslationLockedY,bodyIsTranslationLockedZ,bodyRecomputeMassProperties,bodyResetForces,bodyResetTorques,bodySetAngularDamping,bodySetAngularVelocity,bodySetDominanceGroup,bodySetEnabled,bodySetEnabledRotations,bodySetEnabledTranslations,bodySetFixedRotation,bodySetGravityScale,bodySetLinearDamping,bodySetLinearVelocity,bodySetPosition,bodySetRotation,bodySleep,bodyWakeUp,colliderGetAabb,colliderGetActiveCollisionTypes,colliderGetActiveEvents,colliderGetActiveHooks,colliderGetCollisionGroups,colliderGetContactForceEventThreshold,colliderGetDensity,colliderGetFriction,colliderGetMass,colliderGetOneWayDirection,colliderGetParentBody,colliderGetPosition,colliderGetPositionWrtParent,colliderGetRestitution,colliderGetShapeType,colliderGetSolverGroups,colliderIsEnabled,colliderIsSensor,colliderSetActiveCollisionTypes,colliderSetActiveEvents,colliderSetActiveHooks,colliderSetCollisionGroups,colliderSetContactForceEventThreshold,colliderSetContactSkin,colliderSetDensity,colliderSetEnabled,colliderSetFriction,colliderSetMass,colliderSetOneWayDirection,colliderSetPositionWrtParent,colliderSetRestitution,colliderSetSensor,colliderSetSolverGroups,contactPairCount,contactPairPoints,createBoxCollider,createCapsuleCollider,createConeCollider,createConvexHullCollider,createCylinderCollider,createDynamicBody,createFixedJoint,createHeightfieldCollider,createKinematicBody,createMotorJoint,createPrismaticJoint,createRevoluteJoint,createRopeJoint,createSphereCollider,createSpringJoint,createStaticBody,createTriMeshCollider,createWorld,destroyBody,destroyCollider,destroyJoint,destroyWorld,motorJointGetCorrectionFactor,motorJointGetLinearOffset,motorJointGetMaxForce,motorJointGetMaxTorque,motorJointSetCorrectionFactor,motorJointSetLinearOffset,motorJointSetMaxForce,motorJointSetMaxTorque,pollContactForceEvents,pollContactStartEvents,pollContactStopEvents,pollIntersectionStartEvents,pollIntersectionStopEvents,prismaticJointEnableLimits,prismaticJointEnableMotor,prismaticJointGetLimits,prismaticJointGetMaxMotorForce,prismaticJointGetMotorSpeed,prismaticJointGetTranslation,prismaticJointSetLimits,prismaticJointSetMaxMotorForce,prismaticJointSetMotorSpeed,projectPoint,queryAABB,queryPoint,rayCast,rayCastAll,revoluteJointEnableLimits,revoluteJointEnableMotor,revoluteJointGetAngle,revoluteJointGetLimits,revoluteJointGetMaxMotorTorque,revoluteJointGetMotorSpeed,revoluteJointIsLimitEnabled,revoluteJointSetLimits,revoluteJointSetMaxMotorTorque,revoluteJointSetMotorSpeed,ropeJointGetMaxDistance,ropeJointSetMaxDistance,springJointGetRestLength,springJointSetParams,springJointSetRestLength,worldGetGravity,worldGetNumSolverIterations,worldSetGravity,worldSetNumAdditionalFrictionIterations,worldSetNumInternalPgsIterations,worldSetNumSolverIterations,worldStep
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-17
 */
package sge
package platform

/** Physics operations for 3D rigid body simulation backed by Rapier3D.
  *
  * All methods use primitives and Long handles to Rust-side Rapier3D objects. Platform implementations delegate to Rust via Panama FFM (JVM) or C ABI (Native).
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

  /** Applies a torque impulse vector (instantaneous change to angular velocity). */
  def bodyApplyTorqueImpulse(world: Long, body: Long, tx: Float, ty: Float, tz: Float): Unit

  /** Resets all accumulated forces on the body. */
  def bodyResetForces(world: Long, body: Long): Unit

  /** Resets all accumulated torques on the body. */
  def bodyResetTorques(world: Long, body: Long): Unit

  /** Returns the body type: 0=dynamic, 1=fixed(static), 2=kinematic-position, 3=kinematic-velocity. */
  def bodyGetType(world: Long, body: Long): Int

  /** Sets which translation axes are enabled for the body. */
  def bodySetEnabledTranslations(world: Long, body: Long, allowX: Boolean, allowY: Boolean, allowZ: Boolean): Unit

  /** Returns true if translation along the X axis is locked. */
  def bodyIsTranslationLockedX(world: Long, body: Long): Boolean

  /** Returns true if translation along the Y axis is locked. */
  def bodyIsTranslationLockedY(world: Long, body: Long): Boolean

  /** Returns true if translation along the Z axis is locked. */
  def bodyIsTranslationLockedZ(world: Long, body: Long): Boolean

  /** Sets which rotation axes are enabled for the body. */
  def bodySetEnabledRotations(world: Long, body: Long, allowX: Boolean, allowY: Boolean, allowZ: Boolean): Unit

  /** Returns true if rotation around the X axis is locked. */
  def bodyIsRotationLockedX(world: Long, body: Long): Boolean

  /** Returns true if rotation around the Y axis is locked. */
  def bodyIsRotationLockedY(world: Long, body: Long): Boolean

  /** Returns true if rotation around the Z axis is locked. */
  def bodyIsRotationLockedZ(world: Long, body: Long): Boolean

  /** Gets the world-space center of mass. Fills `out` with [x, y, z]. */
  def bodyGetWorldCenterOfMass(world: Long, body: Long, out: Array[Float]): Unit

  /** Gets the local center of mass. Fills `out` with [x, y, z]. */
  def bodyGetLocalCenterOfMass(world: Long, body: Long, out: Array[Float]): Unit

  /** Gets the angular inertia of a rigid body. */
  def bodyGetInertia(world: Long, body: Long): Float

  /** Gets the velocity at a world-space point on the body. Fills `out` with [vx, vy, vz]. */
  def bodyGetVelocityAtPoint(world: Long, body: Long, px: Float, py: Float, pz: Float, out: Array[Float]): Unit

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

  /** Gets the collider position relative to its parent body. Fills `out` with [x, y, z, qx, qy, qz, qw]. */
  def colliderGetPositionWrtParent(world: Long, collider: Long, out: Array[Float]): Unit

  /** Sets the collider position relative to its parent body. */
  def colliderSetPositionWrtParent(world: Long, collider: Long, x: Float, y: Float, z: Float, qx: Float, qy: Float, qz: Float, qw: Float): Unit

  /** Gets the collider world position. Fills `out` with [x, y, z, qx, qy, qz, qw]. */
  def colliderGetPosition(world: Long, collider: Long, out: Array[Float]): Unit

  /** Returns the collider shape type: 0=ball, 1=cuboid, 2=capsule, 3=cylinder, 4=cone, 5=convex, 6=trimesh, 7=heightfield, -1=unknown. */
  def colliderGetShapeType(world: Long, collider: Long): Int

  /** Gets the collider AABB. Fills `out` with [minX, minY, minZ, maxX, maxY, maxZ]. */
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

  /** Sets which collision types this collider participates in. */
  def colliderSetActiveCollisionTypes(world: Long, collider: Long, flags: Int): Unit

  /** Gets the active collision types flags for this collider. */
  def colliderGetActiveCollisionTypes(world: Long, collider: Long): Int

  /** Sets the collision groups for a collider. */
  def colliderSetCollisionGroups(world: Long, collider: Long, memberships: Int, filter: Int): Unit

  /** Gets the collision groups for a collider. Fills `out` with [memberships, filter]. */
  def colliderGetCollisionGroups(world: Long, collider: Long, out: Array[Int]): Unit

  /** Sets the solver groups for a collider. */
  def colliderSetSolverGroups(world: Long, collider: Long, memberships: Int, filter: Int): Unit

  /** Gets the solver groups for a collider. Fills `out` with [memberships, filter]. */
  def colliderGetSolverGroups(world: Long, collider: Long, out: Array[Int]): Unit

  // ─── New shape types ──────────────────────────────────────────────

  /** Attaches a 3D heightfield collider to a body. Returns a collider handle.
    *
    * @param heights
    *   row-major array of height values (nrows x ncols)
    * @param nrows
    *   number of rows
    * @param ncols
    *   number of columns
    * @param scaleX
    *   scale along x axis
    * @param scaleY
    *   scale along y axis (height)
    * @param scaleZ
    *   scale along z axis
    */
  def createHeightfieldCollider(world: Long, body: Long, heights: Array[Float], nrows: Int, ncols: Int, scaleX: Float, scaleY: Float, scaleZ: Float): Long

  // ─── Joints ───────────────────────────────────────────────────────────

  /** Creates a fixed (weld) joint between two bodies. Returns a joint handle. */
  def createFixedJoint(world: Long, body1: Long, body2: Long): Long

  /** Creates a rope joint that constrains two bodies to stay within a maximum distance. Returns a joint handle. */
  def createRopeJoint(world: Long, body1: Long, body2: Long, maxDist: Float): Long

  /** Creates a revolute (hinge) joint around a given axis at the given anchor. Returns a joint handle. */
  def createRevoluteJoint(world: Long, body1: Long, body2: Long, anchorX: Float, anchorY: Float, anchorZ: Float, axisX: Float, axisY: Float, axisZ: Float): Long

  /** Creates a prismatic (slider) joint along a given axis. Returns a joint handle. */
  def createPrismaticJoint(world: Long, body1: Long, body2: Long, axisX: Float, axisY: Float, axisZ: Float): Long

  /** Creates a motor joint between two bodies (6-DOF). Returns a joint handle. */
  def createMotorJoint(world: Long, body1: Long, body2: Long): Long

  /** Creates a spring joint between two bodies. Returns a joint handle. */
  def createSpringJoint(world: Long, body1: Long, body2: Long, restLength: Float, stiffness: Float, damping: Float): Long

  /** Destroys a joint. */
  def destroyJoint(world: Long, joint: Long): Unit

  // ─── Revolute joint limits and motors ─────────────────────────────

  def revoluteJointEnableLimits(world:      Long, joint: Long, enable: Boolean):             Unit
  def revoluteJointSetLimits(world:         Long, joint: Long, lower:  Float, upper: Float): Unit
  def revoluteJointGetLimits(world:         Long, joint: Long, out:    Array[Float]):        Unit
  def revoluteJointIsLimitEnabled(world:    Long, joint: Long):                              Boolean
  def revoluteJointEnableMotor(world:       Long, joint: Long, enable: Boolean):             Unit
  def revoluteJointSetMotorSpeed(world:     Long, joint: Long, speed:  Float):               Unit
  def revoluteJointSetMaxMotorTorque(world: Long, joint: Long, torque: Float):               Unit
  def revoluteJointGetMotorSpeed(world:     Long, joint: Long):                              Float
  def revoluteJointGetAngle(world:          Long, joint: Long):                              Float
  def revoluteJointGetMaxMotorTorque(world: Long, joint: Long):                              Float

  // ─── Prismatic joint limits and motors ────────────────────────────

  def prismaticJointEnableLimits(world:     Long, joint: Long, enable: Boolean):             Unit
  def prismaticJointSetLimits(world:        Long, joint: Long, lower:  Float, upper: Float): Unit
  def prismaticJointGetLimits(world:        Long, joint: Long, out:    Array[Float]):        Unit
  def prismaticJointEnableMotor(world:      Long, joint: Long, enable: Boolean):             Unit
  def prismaticJointSetMotorSpeed(world:    Long, joint: Long, speed:  Float):               Unit
  def prismaticJointSetMaxMotorForce(world: Long, joint: Long, force:  Float):               Unit
  def prismaticJointGetTranslation(world:   Long, joint: Long):                              Float
  def prismaticJointGetMotorSpeed(world:    Long, joint: Long):                              Float
  def prismaticJointGetMaxMotorForce(world: Long, joint: Long):                              Float

  // ─── Motor joint ──────────────────────────────────────────────────

  def motorJointSetLinearOffset(world:     Long, joint: Long, x:      Float, y: Float, z: Float): Unit
  def motorJointGetLinearOffset(world:     Long, joint: Long, out:    Array[Float]):              Unit
  def motorJointSetMaxForce(world:         Long, joint: Long, force:  Float):                     Unit
  def motorJointSetMaxTorque(world:        Long, joint: Long, torque: Float):                     Unit
  def motorJointSetCorrectionFactor(world: Long, joint: Long, factor: Float):                     Unit
  def motorJointGetMaxForce(world:         Long, joint: Long):                                    Float
  def motorJointGetMaxTorque(world:        Long, joint: Long):                                    Float
  def motorJointGetCorrectionFactor(world: Long, joint: Long):                                    Float

  // ─── Rope joint ───────────────────────────────────────────────────

  def ropeJointSetMaxDistance(world: Long, joint: Long, maxDist: Float): Unit
  def ropeJointGetMaxDistance(world: Long, joint: Long):                 Float

  // ─── Spring joint ─────────────────────────────────────────────────

  def springJointSetRestLength(world: Long, joint: Long, restLength: Float):                 Unit
  def springJointGetRestLength(world: Long, joint: Long):                                    Float
  def springJointSetParams(world:     Long, joint: Long, stiffness:  Float, damping: Float): Unit

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

  // ─── Additional queries ───────────────────────────────────────────────

  /** Finds all colliders intersecting the given axis-aligned bounding box. */
  def queryAABB(world: Long, minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float, outColliders: Array[Long], maxResults: Int): Int

  /** Finds all bodies whose colliders contain the given point. */
  def queryPoint(world: Long, x: Float, y: Float, z: Float, outBodies: Array[Long], maxResults: Int): Int

  /** Casts a ray and returns ALL hits (up to `maxHits`). Each hit is 9 floats. */
  def rayCastAll(world: Long, originX: Float, originY: Float, originZ: Float, dirX: Float, dirY: Float, dirZ: Float, maxDist: Float, outHits: Array[Float], maxHits: Int): Int

  /** Projects a point onto the nearest collider surface. Fills `out` with [projX, projY, projZ, isInside, colliderLo, colliderHi] (6 floats). Returns true if found. */
  def projectPoint(world: Long, x: Float, y: Float, z: Float, out: Array[Float]): Boolean

  // ─── Contact detail queries ───────────────────────────────────────────

  /** Returns the number of contact points between two colliders. */
  def contactPairCount(world: Long, collider1: Long, collider2: Long): Int

  /** Gets contact point details. Fills `out` with [nx,ny,nz,px,py,pz,penetration] per point (7 floats each). */
  def contactPairPoints(world: Long, collider1: Long, collider2: Long, out: Array[Float], maxPoints: Int): Int

  // ─── Intersection events (sensor overlaps) ────────────────────────────

  def pollIntersectionStartEvents(world: Long, outCollider1: Array[Long], outCollider2: Array[Long], maxEvents: Int): Int
  def pollIntersectionStopEvents(world:  Long, outCollider1: Array[Long], outCollider2: Array[Long], maxEvents: Int): Int

  // ─── Solver parameters ────────────────────────────────────────────────

  def worldSetNumSolverIterations(world:             Long, iters: Int): Unit
  def worldGetNumSolverIterations(world:             Long):             Int
  def worldSetNumAdditionalFrictionIterations(world: Long, iters: Int): Unit
  def worldSetNumInternalPgsIterations(world:        Long, iters: Int): Unit

  // ─── Contact force events ────────────────────────────────────────────

  def pollContactForceEvents(world:                Long, outCollider1: Array[Long], outCollider2: Array[Long], outForce: Array[Float], maxEvents: Int): Int
  def colliderSetContactForceEventThreshold(world: Long, collider:     Long, threshold:           Float):                                               Unit
  def colliderGetContactForceEventThreshold(world: Long, collider:     Long):                                                                           Float

  // ─── Active hooks (contact modification) ──────────────────────────────

  def colliderSetActiveHooks(world:     Long, collider: Long, flags: Int):                                              Unit
  def colliderGetActiveHooks(world:     Long, collider: Long):                                                          Int
  def colliderSetOneWayDirection(world: Long, collider: Long, nx:    Float, ny: Float, nz: Float, allowedAngle: Float): Unit
  def colliderGetOneWayDirection(world: Long, collider: Long, out:   Array[Float]):                                     Boolean
}
