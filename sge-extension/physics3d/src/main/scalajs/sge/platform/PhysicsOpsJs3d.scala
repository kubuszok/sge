/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: JS stub — throws UnsupportedOperationException
 *   Idiom: split packages
 */
package sge
package platform

/** Scala.js stub for [[PhysicsOps3d]].
  *
  * All methods throw [[UnsupportedOperationException]]. A future implementation may integrate with the Rapier3D WASM build via JavaScript interop.
  */
private[platform] object PhysicsOpsJs3d extends PhysicsOps3d {

  private def unsupported: Nothing =
    throw new UnsupportedOperationException("PhysicsOps3d is not yet available on Scala.js")

  // World lifecycle
  override def createWorld(gravityX:  Float, gravityY: Float, gravityZ: Float):            Long = unsupported
  override def destroyWorld(world:    Long):                                               Unit = unsupported
  override def worldStep(world:       Long, dt:        Float):                             Unit = unsupported
  override def worldSetGravity(world: Long, gx:        Float, gy:       Float, gz: Float): Unit = unsupported
  override def worldGetGravity(world: Long, out:       Array[Float]):                      Unit = unsupported

  // Rigid body
  override def createDynamicBody(world:           Long, x:    Float, y:      Float, z:        Float, qx:       Float, qy:  Float, qz: Float, qw: Float): Long    = unsupported
  override def createStaticBody(world:            Long, x:    Float, y:      Float, z:        Float, qx:       Float, qy:  Float, qz: Float, qw: Float): Long    = unsupported
  override def createKinematicBody(world:         Long, x:    Float, y:      Float, z:        Float, qx:       Float, qy:  Float, qz: Float, qw: Float): Long    = unsupported
  override def destroyBody(world:                 Long, body: Long):                                                                                     Unit    = unsupported
  override def bodyGetPosition(world:             Long, body: Long, out:     Array[Float]):                                                              Unit    = unsupported
  override def bodyGetRotation(world:             Long, body: Long, out:     Array[Float]):                                                              Unit    = unsupported
  override def bodyGetLinearVelocity(world:       Long, body: Long, out:     Array[Float]):                                                              Unit    = unsupported
  override def bodyGetAngularVelocity(world:      Long, body: Long, out:     Array[Float]):                                                              Unit    = unsupported
  override def bodySetPosition(world:             Long, body: Long, x:       Float, y:        Float, z:        Float):                                   Unit    = unsupported
  override def bodySetRotation(world:             Long, body: Long, qx:      Float, qy:       Float, qz:       Float, qw:  Float):                       Unit    = unsupported
  override def bodySetLinearVelocity(world:       Long, body: Long, vx:      Float, vy:       Float, vz:       Float):                                   Unit    = unsupported
  override def bodySetAngularVelocity(world:      Long, body: Long, wx:      Float, wy:       Float, wz:       Float):                                   Unit    = unsupported
  override def bodyApplyForce(world:              Long, body: Long, fx:      Float, fy:       Float, fz:       Float):                                   Unit    = unsupported
  override def bodyApplyImpulse(world:            Long, body: Long, ix:      Float, iy:       Float, iz:       Float):                                   Unit    = unsupported
  override def bodyApplyTorque(world:             Long, body: Long, tx:      Float, ty:       Float, tz:       Float):                                   Unit    = unsupported
  override def bodyApplyForceAtPoint(world:       Long, body: Long, fx:      Float, fy:       Float, fz:       Float, px:  Float, py: Float, pz: Float): Unit    = unsupported
  override def bodyApplyImpulseAtPoint(world:     Long, body: Long, ix:      Float, iy:       Float, iz:       Float, px:  Float, py: Float, pz: Float): Unit    = unsupported
  override def bodySetLinearDamping(world:        Long, body: Long, damping: Float):                                                                     Unit    = unsupported
  override def bodyGetLinearDamping(world:        Long, body: Long):                                                                                     Float   = unsupported
  override def bodySetAngularDamping(world:       Long, body: Long, damping: Float):                                                                     Unit    = unsupported
  override def bodyGetAngularDamping(world:       Long, body: Long):                                                                                     Float   = unsupported
  override def bodySetGravityScale(world:         Long, body: Long, scale:   Float):                                                                     Unit    = unsupported
  override def bodyGetGravityScale(world:         Long, body: Long):                                                                                     Float   = unsupported
  override def bodyIsAwake(world:                 Long, body: Long):                                                                                     Boolean = unsupported
  override def bodyWakeUp(world:                  Long, body: Long):                                                                                     Unit    = unsupported
  override def bodySleep(world:                   Long, body: Long):                                                                                     Unit    = unsupported
  override def bodySetFixedRotation(world:        Long, body: Long, fixed:   Boolean):                                                                   Unit    = unsupported
  override def bodyEnableCcd(world:               Long, body: Long, enable:  Boolean):                                                                   Unit    = unsupported
  override def bodyIsCcdEnabled(world:            Long, body: Long):                                                                                     Boolean = unsupported
  override def bodySetEnabled(world:              Long, body: Long, enabled: Boolean):                                                                   Unit    = unsupported
  override def bodyIsEnabled(world:               Long, body: Long):                                                                                     Boolean = unsupported
  override def bodySetDominanceGroup(world:       Long, body: Long, group:   Int):                                                                       Unit    = unsupported
  override def bodyGetDominanceGroup(world:       Long, body: Long):                                                                                     Int     = unsupported
  override def bodyGetMass(world:                 Long, body: Long):                                                                                     Float   = unsupported
  override def bodyRecomputeMassProperties(world: Long, body: Long):                                                                                     Unit    = unsupported
  override def bodyApplyTorqueImpulse(world:      Long, body: Long, tx:      Float, ty:       Float, tz:       Float):                                   Unit    = unsupported
  override def bodyResetForces(world:             Long, body: Long):                                                                                     Unit    = unsupported
  override def bodyResetTorques(world:            Long, body: Long):                                                                                     Unit    = unsupported
  override def bodyGetType(world:                 Long, body: Long):                                                                                     Int     = unsupported
  override def bodySetEnabledTranslations(world:  Long, body: Long, allowX:  Boolean, allowY: Boolean, allowZ: Boolean):                                 Unit    = unsupported
  override def bodyIsTranslationLockedX(world:    Long, body: Long):                                                                                     Boolean = unsupported
  override def bodyIsTranslationLockedY(world:    Long, body: Long):                                                                                     Boolean = unsupported
  override def bodyIsTranslationLockedZ(world:    Long, body: Long):                                                                                     Boolean = unsupported
  override def bodySetEnabledRotations(world:     Long, body: Long, allowX:  Boolean, allowY: Boolean, allowZ: Boolean):                                 Unit    = unsupported
  override def bodyIsRotationLockedX(world:       Long, body: Long):                                                                                     Boolean = unsupported
  override def bodyIsRotationLockedY(world:       Long, body: Long):                                                                                     Boolean = unsupported
  override def bodyIsRotationLockedZ(world:       Long, body: Long):                                                                                     Boolean = unsupported
  override def bodyGetWorldCenterOfMass(world:    Long, body: Long, out:     Array[Float]):                                                              Unit    = unsupported
  override def bodyGetLocalCenterOfMass(world:    Long, body: Long, out:     Array[Float]):                                                              Unit    = unsupported
  override def bodyGetInertia(world:              Long, body: Long):                                                                                     Float   = unsupported
  override def bodyGetVelocityAtPoint(world:      Long, body: Long, px:      Float, py:       Float, pz:       Float, out: Array[Float]):                Unit    = unsupported

  // Collider
  override def createSphereCollider(world:         Long, body:     Long, radius:      Float):                                                                Long    = unsupported
  override def createBoxCollider(world:            Long, body:     Long, hx:          Float, hy:                 Float, hz:    Float):                       Long    = unsupported
  override def createCapsuleCollider(world:        Long, body:     Long, halfHeight:  Float, radius:             Float):                                     Long    = unsupported
  override def createCylinderCollider(world:       Long, body:     Long, halfHeight:  Float, radius:             Float):                                     Long    = unsupported
  override def createConeCollider(world:           Long, body:     Long, halfHeight:  Float, radius:             Float):                                     Long    = unsupported
  override def createConvexHullCollider(world:     Long, body:     Long, vertices:    Array[Float], vertexCount: Int):                                       Long    = unsupported
  override def createTriMeshCollider(world:        Long, body:     Long, vertices:    Array[Float], vertexCount: Int, indices: Array[Int], indexCount: Int): Long    = unsupported
  override def destroyCollider(world:              Long, collider: Long):                                                                                    Unit    = unsupported
  override def colliderSetDensity(world:           Long, collider: Long, density:     Float):                                                                Unit    = unsupported
  override def colliderSetFriction(world:          Long, collider: Long, friction:    Float):                                                                Unit    = unsupported
  override def colliderSetRestitution(world:       Long, collider: Long, restitution: Float):                                                                Unit    = unsupported
  override def colliderSetSensor(world:            Long, collider: Long, sensor:      Boolean):                                                              Unit    = unsupported
  override def colliderGetDensity(world:           Long, collider: Long):                                                                                    Float   = unsupported
  override def colliderGetFriction(world:          Long, collider: Long):                                                                                    Float   = unsupported
  override def colliderGetRestitution(world:       Long, collider: Long):                                                                                    Float   = unsupported
  override def colliderIsSensor(world:             Long, collider: Long):                                                                                    Boolean = unsupported
  override def colliderSetEnabled(world:           Long, collider: Long, enabled:     Boolean):                                                              Unit    = unsupported
  override def colliderIsEnabled(world:            Long, collider: Long):                                                                                    Boolean = unsupported
  override def colliderGetPositionWrtParent(world: Long, collider: Long, out:         Array[Float]):                                                         Unit    = unsupported
  override def colliderSetPositionWrtParent(world:    Long, collider: Long, x:           Float, y:    Float, z: Float, qx: Float, qy: Float, qz: Float, qw: Float): Unit  = unsupported
  override def colliderGetPosition(world:             Long, collider: Long, out:         Array[Float]):                                                             Unit  = unsupported
  override def colliderGetShapeType(world:            Long, collider: Long):                                                                                        Int   = unsupported
  override def colliderGetAabb(world:                 Long, collider: Long, out:         Array[Float]):                                                             Unit  = unsupported
  override def colliderGetParentBody(world:           Long, collider: Long):                                                                                        Long  = unsupported
  override def colliderGetMass(world:                 Long, collider: Long):                                                                                        Float = unsupported
  override def colliderSetMass(world:                 Long, collider: Long, mass:        Float):                                                                    Unit  = unsupported
  override def colliderSetContactSkin(world:          Long, collider: Long, skin:        Float):                                                                    Unit  = unsupported
  override def colliderSetActiveEvents(world:         Long, collider: Long, flags:       Int):                                                                      Unit  = unsupported
  override def colliderGetActiveEvents(world:         Long, collider: Long):                                                                                        Int   = unsupported
  override def colliderSetActiveCollisionTypes(world: Long, collider: Long, flags:       Int):                                                                      Unit  = unsupported
  override def colliderGetActiveCollisionTypes(world: Long, collider: Long):                                                                                        Int   = unsupported
  override def colliderSetCollisionGroups(world:      Long, collider: Long, memberships: Int, filter: Int):                                                         Unit  = unsupported
  override def colliderGetCollisionGroups(world:      Long, collider: Long, out:         Array[Int]):                                                               Unit  = unsupported
  override def colliderSetSolverGroups(world:         Long, collider: Long, memberships: Int, filter: Int):                                                         Unit  = unsupported
  override def colliderGetSolverGroups(world:         Long, collider: Long, out:         Array[Int]):                                                               Unit  = unsupported

  // Heightfield
  override def createHeightfieldCollider(world: Long, body: Long, heights: Array[Float], nrows: Int, ncols: Int, scaleX: Float, scaleY: Float, scaleZ: Float): Long = unsupported

  // Joints
  override def createFixedJoint(world:     Long, body1: Long, body2: Long):                                                                                                Long = unsupported
  override def createRopeJoint(world:      Long, body1: Long, body2: Long, maxDist:    Float):                                                                             Long = unsupported
  override def createRevoluteJoint(world:  Long, body1: Long, body2: Long, anchorX:    Float, anchorY:   Float, anchorZ: Float, axisX: Float, axisY: Float, axisZ: Float): Long = unsupported
  override def createPrismaticJoint(world: Long, body1: Long, body2: Long, axisX:      Float, axisY:     Float, axisZ:   Float):                                           Long = unsupported
  override def createMotorJoint(world:     Long, body1: Long, body2: Long):                                                                                                Long = unsupported
  override def createSpringJoint(world:    Long, body1: Long, body2: Long, restLength: Float, stiffness: Float, damping: Float):                                           Long = unsupported
  override def destroyJoint(world:         Long, joint: Long):                                                                                                             Unit = unsupported

  // Revolute joint limits and motors
  override def revoluteJointEnableLimits(world:      Long, joint: Long, enable: Boolean):             Unit    = unsupported
  override def revoluteJointSetLimits(world:         Long, joint: Long, lower:  Float, upper: Float): Unit    = unsupported
  override def revoluteJointGetLimits(world:         Long, joint: Long, out:    Array[Float]):        Unit    = unsupported
  override def revoluteJointIsLimitEnabled(world:    Long, joint: Long):                              Boolean = unsupported
  override def revoluteJointEnableMotor(world:       Long, joint: Long, enable: Boolean):             Unit    = unsupported
  override def revoluteJointSetMotorSpeed(world:     Long, joint: Long, speed:  Float):               Unit    = unsupported
  override def revoluteJointSetMaxMotorTorque(world: Long, joint: Long, torque: Float):               Unit    = unsupported
  override def revoluteJointGetMotorSpeed(world:     Long, joint: Long):                              Float   = unsupported
  override def revoluteJointGetAngle(world:          Long, joint: Long):                              Float   = unsupported
  override def revoluteJointGetMaxMotorTorque(world: Long, joint: Long):                              Float   = unsupported

  // Prismatic joint limits and motors
  override def prismaticJointEnableLimits(world:     Long, joint: Long, enable: Boolean):             Unit  = unsupported
  override def prismaticJointSetLimits(world:        Long, joint: Long, lower:  Float, upper: Float): Unit  = unsupported
  override def prismaticJointGetLimits(world:        Long, joint: Long, out:    Array[Float]):        Unit  = unsupported
  override def prismaticJointEnableMotor(world:      Long, joint: Long, enable: Boolean):             Unit  = unsupported
  override def prismaticJointSetMotorSpeed(world:    Long, joint: Long, speed:  Float):               Unit  = unsupported
  override def prismaticJointSetMaxMotorForce(world: Long, joint: Long, force:  Float):               Unit  = unsupported
  override def prismaticJointGetTranslation(world:   Long, joint: Long):                              Float = unsupported
  override def prismaticJointGetMotorSpeed(world:    Long, joint: Long):                              Float = unsupported
  override def prismaticJointGetMaxMotorForce(world: Long, joint: Long):                              Float = unsupported

  // Motor joint
  override def motorJointSetLinearOffset(world:     Long, joint: Long, x:      Float, y: Float, z: Float): Unit  = unsupported
  override def motorJointGetLinearOffset(world:     Long, joint: Long, out:    Array[Float]):              Unit  = unsupported
  override def motorJointSetMaxForce(world:         Long, joint: Long, force:  Float):                     Unit  = unsupported
  override def motorJointSetMaxTorque(world:        Long, joint: Long, torque: Float):                     Unit  = unsupported
  override def motorJointSetCorrectionFactor(world: Long, joint: Long, factor: Float):                     Unit  = unsupported
  override def motorJointGetMaxForce(world:         Long, joint: Long):                                    Float = unsupported
  override def motorJointGetMaxTorque(world:        Long, joint: Long):                                    Float = unsupported
  override def motorJointGetCorrectionFactor(world: Long, joint: Long):                                    Float = unsupported

  // Rope joint
  override def ropeJointSetMaxDistance(world: Long, joint: Long, maxDist: Float): Unit  = unsupported
  override def ropeJointGetMaxDistance(world: Long, joint: Long):                 Float = unsupported

  // Spring joint
  override def springJointSetRestLength(world: Long, joint: Long, restLength: Float):                 Unit  = unsupported
  override def springJointGetRestLength(world: Long, joint: Long):                                    Float = unsupported
  override def springJointSetParams(world:     Long, joint: Long, stiffness:  Float, damping: Float): Unit  = unsupported

  // Queries
  override def rayCast(world: Long, originX: Float, originY: Float, originZ: Float, dirX: Float, dirY: Float, dirZ: Float, maxDist: Float, out: Array[Float]): Boolean = unsupported

  // Additional queries
  override def queryAABB(world:  Long, minX: Float, minY: Float, minZ: Float, maxX:      Float, maxY:             Float, maxZ: Float, outColliders: Array[Long], maxResults: Int): Int = unsupported
  override def queryPoint(world: Long, x:    Float, y:    Float, z:    Float, outBodies: Array[Long], maxResults: Int):                                                            Int = unsupported
  override def rayCastAll(world: Long, originX: Float, originY: Float, originZ: Float, dirX: Float, dirY: Float, dirZ: Float, maxDist: Float, outHits: Array[Float], maxHits: Int): Int = unsupported
  override def projectPoint(world: Long, x: Float, y: Float, z: Float, out: Array[Float]): Boolean = unsupported

  // Contact events
  override def pollContactStartEvents(world: Long, outCollider1: Array[Long], outCollider2: Array[Long], maxEvents: Int): Int = unsupported
  override def pollContactStopEvents(world:  Long, outCollider1: Array[Long], outCollider2: Array[Long], maxEvents: Int): Int = unsupported

  // Contact detail queries
  override def contactPairCount(world:  Long, collider1: Long, collider2: Long):                                    Int = unsupported
  override def contactPairPoints(world: Long, collider1: Long, collider2: Long, out: Array[Float], maxPoints: Int): Int = unsupported

  // Intersection events (sensor overlaps)
  override def pollIntersectionStartEvents(world: Long, outCollider1: Array[Long], outCollider2: Array[Long], maxEvents: Int): Int = unsupported
  override def pollIntersectionStopEvents(world:  Long, outCollider1: Array[Long], outCollider2: Array[Long], maxEvents: Int): Int = unsupported

  // Solver parameters
  override def worldSetNumSolverIterations(world:             Long, iters: Int): Unit = unsupported
  override def worldGetNumSolverIterations(world:             Long):             Int  = unsupported
  override def worldSetNumAdditionalFrictionIterations(world: Long, iters: Int): Unit = unsupported
  override def worldSetNumInternalPgsIterations(world:        Long, iters: Int): Unit = unsupported

  // Contact force events
  override def pollContactForceEvents(world:                Long, outCollider1: Array[Long], outCollider2: Array[Long], outForce: Array[Float], maxEvents: Int): Int   = unsupported
  override def colliderSetContactForceEventThreshold(world: Long, collider:     Long, threshold:           Float):                                               Unit  = unsupported
  override def colliderGetContactForceEventThreshold(world: Long, collider:     Long):                                                                           Float = unsupported

  // Active hooks / one-way direction
  override def colliderSetActiveHooks(world:     Long, collider: Long, flags: Int):                                              Unit    = unsupported
  override def colliderGetActiveHooks(world:     Long, collider: Long):                                                          Int     = unsupported
  override def colliderSetOneWayDirection(world: Long, collider: Long, nx:    Float, ny: Float, nz: Float, allowedAngle: Float): Unit    = unsupported
  override def colliderGetOneWayDirection(world: Long, collider: Long, out:   Array[Float]):                                     Boolean = unsupported
}
